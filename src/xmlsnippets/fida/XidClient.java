//*******************************{begin:header}******************************//
//     XML Processing Snippets - https://code.google.com/p/xml-snippets/     //
//***************************************************************************//
//
//      xml-snippets:   XML Processing Snippets 
//                      with Some Theoretical Considerations
//
//      Copyright (C) 2012 Jani Hautamaki <jani.hautamaki@hotmail.com>
//
//      Licensed under the terms of GNU General Public License v3.
//
//      You should have received a copy of the GNU General Public License v3
//      along with this program as the file LICENSE.txt; if not, please see
//      http://www.gnu.org/licenses/gpl-3.0.html
//
//********************************{end:header}*******************************//

package xmlsnippets.fida;

// java core imports
import java.io.File;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Stack;
import java.util.Set;
import java.util.HashSet;
// jdom imports
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Namespace;
// xmlsnippets imports
import xmlsnippets.core.Xid;
import xmlsnippets.core.XidString;
import xmlsnippets.core.XidIdentification;
import xmlsnippets.core.ContentualEq;
import xmlsnippets.core.Normalization;
import xmlsnippets.util.XMLFileHelper;
import xmlsnippets.util.XPathIdentification;
import xmlsnippets.util.XPathDebugger;
import xmlsnippets.util.Digest;
import xmlsnippets.util.NamespacesBubbler;
import xmlsnippets.util.FileHelper;

// fida impotrs
import xmlsnippets.fida.Fida;
import xmlsnippets.fida.FidaXML;

public class XidClient
{
    
    // CONSTANTS
    //===========
    
    /**
     * Exit code for succesful run.
     */
    public static final int EXIT_SUCCESS = 0;
    
    /**
     * Exit code for unsuccesful run.
     */
    public static final int EXIT_FAILURE = 1;
    
    /**
     * Default repository file name for the Fida serialization
     */
    public static final String DEFAULT_FIDA_REPOSITORY = "fida.xml";
    
    // MEMBER VARIABLES
    //==================
    
    // CLASS METHODS
    //===============
    
    /*
     * A helper class to capture the command-line
     */
    public static class CmdArgs {
        public static final int BUBBLE_PRUDENT = 0;
        public static final int BUBBLE_GREEDY = 1;
        public static final int BUBBLE_NONE = 2;
        public boolean debug_flag = false;
        public boolean addall_flag = false;
        public boolean unrev_flag = false;
        public int bubble = BUBBLE_PRUDENT;
        public boolean removeall_flag = false;
        public String repo_filename = DEFAULT_FIDA_REPOSITORY;
        public List<String> rest_args = new LinkedList<String>();
        public String command_arg = null;
    } // class CmdArgs
    
    // BRIDGE TO THE BACKEND REPOSITORY DATA STRUCTURE
    //================================================
    
    /**
     * The background data structure
     */
    private static Fida.Repository g_fida = null;
    
    /** 
     * The details how the interface {@code AbstractRepository} 
     * maps (or identifies) into the terms of the implementation 
     * {@code Fida.Repository}. This is the "glue" between those two.
     */
    public static class FidaRepository
        implements AbstractRepository
    {
        // MEMBER VARIABLES
        //==================
        
        /** The implementing repository object. */
        private Fida.Repository db;
        
        // CONSTRUCTORS
        //==============
        
        /** The constructor; no validation to the parameters. */
        public FidaRepository(Fida.Repository db) {
            this.db = db;
        } // ctor
        
        // INTERFACE IMPLEMENTATION
        //==========================
        
        @Override
        public Fida.Node get_node(Xid user_xid) {
            return db.state.externals.get(user_xid);
        } // get_node()
        
        @Override
        public void set_new_revision(Xid xid) {
            // Copy the repository state into the element's rev.
            xid.rev = db.item_xid.rev;

            // Stamp the secondary revision number to the element.
            // If the repository is not using secondary revisioning,
            // then this will reset the element's secondary revisioning to.
            xid.v_major = db.item_xid.v_major;
            xid.v_minor = db.item_xid.v_minor;
        } // set_new_revision()
        
        @Override
        public Xid generate_xid(String typename) {
            // Use the repository's revision as the revision
            int rev = db.item_xid.rev;
            // Generate the xid as a combination of the type name and 
            // a unique, unused internal uid identifier.
            String id = String.format(
                "#%s!%08x", typename, db.state.new_uid());
            
            return new Xid(id, rev);
        } // generate_xid()

        @Override
        public Fida.Node add_node(Element payload, Fida.Node prev) {
            // Make sure that the payload parameter is good
            if (payload == null) {
                throw new IllegalArgumentException("Payload is null");
            } // if: null payload
            
            // Dig out the payload xid. Returns null if there is none.
            Xid payload_xid = XidIdentification.get_xid(payload);
            
            // Make sure that the payload has a xid.
            if (payload_xid == null) {
                throw new IllegalArgumentException(
                    "Payload XML element does not have xid");
            } // if: no xid
            
            // Okay, good to go, almost.
            
            // One final check, verify that the xid is unused.
            if (db.state.externals.get(payload_xid) != null) {
                throw new RuntimeException(String.format(
                    "Attempting to add a payload with xid=%s which is already taken",
                    XidString.serialize(payload_xid)));
            } // if: xid taken already
            
            // Everything is fine.
            
            // Create a new Fida.Node. 
            // It is used as the return value too.
            Fida.Node node = new Fida.Node();
            
            // Assign a xid and uid. 
            // TODO: replace with a method call to this class.
            node.item_xid = this.generate_xid("node");
            
            // Set link to the previous (if any)
            node.prev = prev;
            
            // Set payload content
            node.payload_element = payload;
            
            // Record the payload xid to the node object
            node.payload_xid = payload_xid;
            
            // Link the administrative node to the next commit
            node.parent_commit = db.next_commit;
            
            // Add the administrative node to the nodes set of the next commit
            g_fida.next_commit.nodes.add(node);
            
            // Remember to put the payload element's xid into the externals
            // hash map so that it is marked as taken and it can be resolved.
            db.state.externals.put(payload_xid, node);
            
            // Return the created administrative node
            return node;
        } // add_node()
        
        @Override
        public Fida.Node get_latest_leaser(String id) {
            // Return variable
            Fida.Node rval = null;
            
            // Loop through all root nodes in the current tree.
            for (Fida.File ff: db.state.tree) {
                // TODO: Use db instead of g_fida. Replace the method call.
                Element e = resolve_payload_xid(ff.root_xid);
                
                rval = get_latest_leaser(e, id);
                if (rval != null) {
                    break;
                } // if
            } // for: each file
            
            return rval;
        } // get_latest_leaser()
        
        // HELPER METHODS
        //================
        
        private Fida.Node get_latest_leaser(Element elem, String id) {
            // Return variable
            Fida.Node rval = null;
            
            // Loop through all child elements of the current element
            // and call this method recursively on them.
            for (Object obj : elem.getContent()) {
                // If not a child, skip
                if ((obj instanceof Element) == false) {
                    continue;
                }
                
                // Cast
                Element c = (Element) obj;
                
                // The target element into which the recursion is made
                Element target = null;
                
                // If the element is an inclusion-by-xid, this returns
                // the xid reference.
                Xid ref_xid = get_ref_xid(c);
                
                // If the element is an inclusion-by-xid, the resolved
                // payload XML element of the xid reference is the target
                // of the recursion, otherwise it is the current child 
                // element itself.
                if (ref_xid != null) {
                    // TODO: replace this method call
                    target = resolve_payload_xid(ref_xid);
                } else {
                    target = c;
                } // if-else
                
                // Recurse
                Fida.Node match = null;
                match = get_latest_leaser(target, id);
                
                // If a leaser of the lifeline designator was found
                // from the recursion target, and it is newer than
                // the currently latest leaser, update the return value.
                if (match != null) {
                    if (rval == null) {
                        rval = match;
                    } else if (rval.payload_xid.rev < match.payload_xid.rev) {
                        // Update
                        rval = match;
                    } // if
                } // if: recursion target had a leaser
            } // for: each child element
            
            // Process the current XML element
            //=================================
            
            // See if the current payload element has a matching id.
            Xid xid = XidIdentification.get_xid(elem);
            
            // See if the current payload XML is identified and if it is,
            // then determine whether it uses the same lifeline designator
            // that is being requested.
            if ((xid != null) && xid.id.equals(id)) {
                // Yes, the lifeline designator matches.
                
                // If this instance of the data object is not the latest 
                // instance available in its lifeline, then the presence of 
                // this xid cannot be used to conclude whether the lifeline
                // designator is currently leased or not. (For instance, data 
                // object's newer revision, which is not present in the current
                // tree, may use a different lifeline designator).
                
                // Get the administrative entry
                Fida.Node match = get_node(xid);
                
                // If the data object does NOT have successors, then this
                // data object may be the current leaser of the lifeline
                // designator.
                if (match.next.size() == 0) {
                    // The object does not have any successors.
                    // This was the leaser of the lifeline designator
                    // at that time. If that time was a newer point in time
                    // than the previous match, then the return value is
                    // updated. Also, if there was no previous match,
                    // this forms the base line.
                    if ((rval == null) 
                        || (rval.payload_xid.rev < match.payload_xid.rev))
                    {
                        rval = match;
                    } // if update
                } // if: does not have a next.
            } // if: the data object has the specified lifeline designator.
            
            return rval;
        } // get_latest_leaser()
    } // class FidaRepository
    
    //========================================================================
    
    /**
     * Creates a new repository with the given file name,
     * and repository name. 
     */
    public static void create_fida_repository(
        String filename, 
        String reponame
    ) {
        if (g_fida != null) {
            throw new RuntimeException(String.format(
                "Repository already created or read"));
        }
        
        g_fida = new Fida.Repository();
        g_fida.file = new File(filename);
        g_fida.item_xid = new Xid(String.format(
            "#repository!%s", reponame), 0);
        
    } // create_fida_repository()
    
    /**
     * Parse g_fida from a specified file.
     * @param file specified the location of the repository file.
     */
    public static void read_fida_repository(File file) {
        
        if ((file.isFile() == false) || (file.exists() == false)) {
            throw new RuntimeException(String.format(
                "Not a file or does not exist: %s", file.getPath()));
        } // if
        
        g_fida = FidaXML.deserialize(file);
    } // read_fida_repository()
    
    /**
     * Re-serializes the g_fida repository back to disk.
     */
    public static void write_fida_repository() {
        if (g_fida == null) {
            throw new RuntimeException(String.format(
                "Read the repository first!"));
        }
        FidaXML.serialize(g_fida);
    } // write_fida_repository()
    
    /**
     * Generates an internal xid with an uid, and records that uid as used.
     */
    public static Xid generate_xid(String typename) {
        int rev = g_fida.item_xid.rev;
        String id = String.format("#%s!%08x", 
            typename, g_fida.state.new_uid());
        
        return new Xid(id, rev);
    } // generate_xid()
    
    /**
     * Gets a file from the currently active tree.
     *
     */
    public static boolean is_already_tracked(File file) {
        return get_tracked_file(file) != null;
    } // is_already_tracked()
    
    /**
     * Gets a tracked file
     */
    public static Fida.File get_tracked_file(File file) {
        try {
            file = FileHelper.getRelativePath(
                g_fida.file.getParentFile(), file);
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "%s: getRelativePath() failed", file.getPath()), ex);
        } // try-catch
        
        try {
            // First, convert the file path into a relative form
            // relative to the repository base. May throw!
            
            List<Fida.File> tree = g_fida.state.tree;
            
            for (Fida.File ff : tree) {
                File tracked = new File(ff.path);
                // Get canonial file
                if (tracked.equals(file)) {
                    return ff;
                }

            } // for
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        } // try-catch
        
        return null;
    } // get_tracked_file

    
    /**
     * Creates and allocates a new commit object.
     * Most importantly, the commit object is assigned to the varibale
     * g_fida.next_commit. This is used in populate() to known where to
     * put the new Fida.Node objects generated.
     *
     * How it works is: 
     *      
     *      populate() calls add_fida_node()
     * 
     *      add_fida_node() looks up the value g_fida.next_commit
     *      and adds the node there.
     *
     */
    public static Fida.Commit allocate_commit() {
        
        // Create a new commit object.
        Fida.Commit next_commit = new Fida.Commit();
        
        // Assign a proper xid and uid to it.
        next_commit.item_xid = generate_xid("commit");
        
        // Set the user name. TODO: Something more sensible
        // could certainly be used here.
        next_commit.author = System.getProperty("user.name");

        // The date field was automatically set in the constructor.
        // next_commit.date = new Date()
        
        // Make the repository conscious about the next commit
        g_fida.next_commit = next_commit;
        
        return next_commit;
    } // allocate_commit()
    
    /**
     * This functions can cause the repositorys state / current revision 
     * number to jump abruply and in discontinous manner.
     * What is being asserted is that the newest revision number is
     * always the greatest, so that the rev+1 is always non-existent
     * in the repository.
     * 
     * @param rev Revision number which must be guaranteed to be past number.
     */
    public static void update_repository_revision(Xid xid) {
        if (g_fida.item_xid.rev < xid.rev) {
            g_fida.item_xid.rev = xid.rev;
        }
    } // update_repository_revision()
    
    public static void update_repository_revspec(Xid xid) {
        if (xid.has_version()) {
            System.out.printf("Payload xid: %s\n", XidString.serialize(xid));
            if ((xid.v_major > g_fida.item_xid.v_major)
                || ((xid.v_major == g_fida.item_xid.v_major)
                && (xid.v_minor > g_fida.item_xid.v_minor)))
            {
                g_fida.item_xid.v_major = xid.v_major;
                g_fida.item_xid.v_minor = xid.v_minor;
            }
        } // if
    } // update_repository_revspec()
    
    
    
    
    
    
    //========================================================================
    // MAIN
    //========================================================================

    //========================================================================
    // parse_arguments()
    //========================================================================
    
    protected static CmdArgs parse_arguments(String[] args) {
        CmdArgs rval = new CmdArgs();
        
        for (int i = 0; i < args.length; i++) {
            String carg = args[i];
            // See if the current argument is a swtich
            if (carg.charAt(0) == '-') {
                String option = carg.substring(1);
                if (option.equals("f")) {
                    // Override default xid store file
                    i++;
                    expect_arg(args, i);
                    rval.repo_filename = args[i];
                }
                else if (option.equals("debug")) {
                    rval.debug_flag = true;
                }
                else if (option.equals("force")) {
                    rval.addall_flag = true;
                }
                else if (option.equals("unrev")) {
                    rval.unrev_flag = true;
                }
                else if (option.equals("removeall")) {
                    rval.removeall_flag = true;
                }
                else if (option.equals("greedy")) {
                    rval.bubble = CmdArgs.BUBBLE_GREEDY;
                }
                else if (option.equals("prudent")) {
                    rval.bubble = CmdArgs.BUBBLE_PRUDENT;
                }
                else if (option.equals("nobubble")) {
                    rval.bubble = CmdArgs.BUBBLE_NONE;
                }
                else {
                    // Unrecognized
                    throw new RuntimeException(String.format(
                        "Unrecognized option: %s\n", carg));
                } // if-else
            } else {
                // Otherwise, it is not an option.
                // If there has not been a command yet, assume it is a command
                if (rval.command_arg == null) {
                    rval.command_arg = carg;
                } else {
                    // Otherwise it is a file parameter
                    rval.rest_args.add(carg);
                } // if-else: command already specified?
            } // if-else
        } // for: each arg
        
        return rval;
    } // parse_arguments()

    protected static void expect_arg(String[] args, int i) {
        if (i >= args.length) {
            // Index out of bounds
            throw new RuntimeException(String.format(
                "Error: expected an argument after: %s", args.length));
        }
    } // expect_arg()
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.printf("No arguments\n");
            System.exit(EXIT_SUCCESS);
        }
        
        CmdArgs cmd_args = null;
        try {
            cmd_args = parse_arguments(args);
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if (msg != null) {
                System.out.printf("%s\n", ex.getMessage());
            } else {
                ex.printStackTrace();
            }
            System.exit(EXIT_FAILURE);
        } // try-catch

        // For convenience, and to avoid double dot expressions.
        String command = cmd_args.command_arg;
        
        try {
            if (command == null) {
                throw new RuntimeException(String.format(
                    "Error: no command"));
            }
            else if (command.equals("help")) {
                display_help();
                System.exit(EXIT_SUCCESS);
            }
            else if (command.equals("init")) {
                
                create_fida_repository(cmd_args.repo_filename, "unnamed");
                write_fida_repository();
                System.out.printf("Created: %s\n", g_fida.file.getPath());
                
                System.exit(EXIT_SUCCESS);
            } // if-else
            
            // Discover fida repository by recursion
            File repo_file = new File(cmd_args.repo_filename);
            if (repo_file.isAbsolute() == false) {
                // Attempt discovery. Get current cwd
                File cwd = new File(System.getProperty("user.dir"));
                // Then find the specified repository file name
                // by ascending.
                repo_file = FileHelper.discoverFileByAscendingDirs(
                    cwd, cmd_args.repo_filename);
                
                if (repo_file == null) {
                    throw new RuntimeException(String.format(
                        "Error: Cannot locate repository database \"%s\"",
                        cmd_args.repo_filename));
                } // if: not found
                
            } // if: not absolute file name
            
            
            // At this stage, read the FIDA repository file
            read_fida_repository(repo_file);
            System.out.printf("Parsed: %s\n", g_fida.file.getPath());
            
            // Increase the revision number
            g_fida.item_xid.rev++;

            if (cmd_args.addall_flag == true) {
                System.out.printf("Warning: allow_unknowns=true.\n");
                g_fida.state.allow_unknowns = true;
            }
            if (cmd_args.unrev_flag == true) {
                System.out.printf("Warning: unrev_unknowns=true.\n");
                g_fida.state.unrev_unknowns = true;
            }
            
            if (command.equals("add")) {
                if (cmd_args.rest_args.size() == 0) {
                    throw new RuntimeException("No files to add");
                }
                add_files(cmd_args.rest_args);
            }
            else if (command.equals("remove")) {
                remove_files(cmd_args.rest_args);
            }
            else if (command.equals("update")) {
                update_files();
            }
            else if (command.equals("status")) {
                display_status();
            }
            else if (command.equals("rebuild")) {
                rebuild_file(cmd_args.rest_args, cmd_args.bubble);
            }
            else if (command.equals("output")) {
                output_xid(cmd_args.rest_args, cmd_args.bubble);
            }
            else if (command.equals("output2")) {
                output2_xid(cmd_args.rest_args);
            }
            else if (command.equals("fileinfo")) {
                get_file_info(cmd_args.rest_args);
            }
            else if (command.equals("commitinfo")) {
                get_commit_info(cmd_args.rest_args);
            }
            else if (command.equals("debug")) {
                // Execute debug mode
                System.out.printf("Debug (unimplemented).\n");
            }
            else if (command.equals("check")) {
                System.out.printf("Check (unimplemented).\n");
            }
            else if (command.equals("tree")) {
                display_tree();
            }
            else if (command.equals("lifelines")) {
                display_lifelines();
            }
            else if (command.equals("setversion")) {
                set_repository_version(cmd_args.rest_args);
            }
            else if (command.equals("incversion")) {
                inc_repository_version(cmd_args.rest_args);
            }
            else if (command.equals("migrate")) {
                migrate_files(cmd_args.rest_args);
            }
            else {
                throw new RuntimeException(String.format(
                    "Error: unknown command \"%s\"", command));
            } // if-else known command?
            
            // Re-serialize the ingested files and the repository
            //====================================================
            
            if ((g_fida.state.modified == true) 
                && (g_fida.next_commit != null))
            {
                // Rewrite the updated files, if any
                // This also updates the file digest values which must be
                // done PRIOR to the serialization of the repository itself.
                for (Fida.File rewriteff : g_fida.next_commit.layout) {
                    if (rewriteff.action == Fida.ACTION_FILE_REMOVED) {
                        // Delete the file?
                        continue;
                    } // if
                    
                    File f = new File(g_fida.file.getParent(), rewriteff.path);
                    //File f = new File(rewriteff.path);
                    Document doc = rewriteff.doc;
                    try {
                        XMLFileHelper.serialize_document_verbatim(doc, f);
                    } catch(Exception ex) {
                        throw new RuntimeException(ex);
                    } // try-catch
                    
                    // Calculate the digest for the updated file
                    
                    try {
                        rewriteff.digest = Digest.create("md5", f);
                    } catch(Exception ex) {
                        throw new RuntimeException(String.format(
                            "%s: cannot calculate digest; %s\n", 
                            f.getPath(), ex.getMessage()), ex);
                    } // try-catch
                } // for: each file the commit set
            }
            // This is separate from the above for the reason that
            // there might be commands which simply configure 
            // repository settings, and do not actually create a commit
            if (g_fida.state.modified == true) {
            
                // Re-serialize the repository
                write_fida_repository();
                // Mark the state unmodified
                g_fida.state.modified = false;
                // Remove the next_commit
                g_fida.next_commit = null;
                
                // Done
                System.out.printf("Committed revision %d\n", 
                    g_fida.item_xid.rev);
            } // if
            
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if ((msg == null) || (cmd_args.debug_flag == true)) {
                ex.printStackTrace();
            }
            else if (msg != null) {
                System.out.printf("%s\n", msg);
            } // if-else
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        // Otherwise, inspect the command
        System.exit(EXIT_SUCCESS);
    } // main()

    public static void display_help() {
        System.out.printf("Fida (C) 2012 Jani Hautamaki\n");
        System.out.printf("\n");
        System.out.printf("Synopsis:\n");
        System.out.printf("    fida <command> [arguments_or_options]\n");
        System.out.printf("\n");
        System.out.printf("Options:\n");
        System.out.printf("    -f <file>                      specified the file to use as repository\n");
        System.out.printf("    -force                         allows unknown xids to be ingested\n");
        System.out.printf("    -unrev                         allows unknown xids by unrevisioning\n");
        System.out.printf("    -debug                         stack trace on exception\n");
        System.out.printf("    -greedy                        bubble namespace decls greedily\n");
        System.out.printf("    -prudent                       bubble namespace decls prudently\n");
        System.out.printf("    -nobubble                      disable bubbling\n");
        System.out.printf("\n");
        System.out.printf("Commands:\n");
        System.out.printf("\n");
        System.out.printf("  Managing files:\n");
        System.out.printf("    init                           creates a new repository\n");
        System.out.printf("    add <file1> [file2] ...        start tracking files\n");
        System.out.printf("    remove <file1> [file2] ...     drop files from tracking\n");
        System.out.printf("    update <file1> [file2] ...     update repository\n");
        System.out.printf("    migrate                        migrate tracked files\n");
        System.out.printf("\n");
        System.out.printf("  Secondary versioning:\n");
        System.out.printf("    setversion <major.minor>       sets the repository version\n");
        System.out.printf("    incversion major | minor       increases major or minor\n");
        System.out.printf("\n");
        System.out.printf("  Miscellaneous:\n");
        System.out.printf("    status                         displaystatus of tracked files\n");
        System.out.printf("    fileinfo <rev> <path>          display file record details\n");
        System.out.printf("    commitinfo <rev>               display commit details\n");
        System.out.printf("    tree                           display currently tracked files\n");
        System.out.printf("    lifelines                      display lifelines of the XML elements\n");
        System.out.printf("\n");
        System.out.printf("    rebuild <rev> <path> <file>    rebuilds archived file record to a file\n");
        System.out.printf("    output <xid>                   rebuilds the given xid on screen\n");
        System.out.printf("    output2 <xid>                  displays the given xid on screen\n");
    } // display_help()

    
    //=========================================================================
    // Remove files
    //=========================================================================
    
    public static void remove_files(List<String> filenames) {
        
        // Create a new commit object
        Fida.Commit next_commit = allocate_commit();
        
        // For each argument file name
        for (String fname : filenames) {
            
            File file = new File(fname);
            
            Fida.File ff = get_tracked_file(file);
            if (ff == null) {
                throw new RuntimeException(String.format(
                    "%s: file not tracked", file.getPath()));
            } // if
            
            // Otherwise create a deletion entry
            Fida.File del = new Fida.File();
            
            next_commit.layout.add(del);
            del.parent_commit = next_commit;
            
            // Assign a unique id
            del.item_xid = generate_xid("file");
            // Make it a successor of the previous
            del.prev = ff;
            // Set the action
            del.action = Fida.ACTION_FILE_REMOVED;
            // Just copy
            del.path = ff.path;
            del.digest = ff.digest;
            del.root_xid = ff.root_xid;
        } // for
        
        commit_files(next_commit);
    } // remove_files()
    
    //=========================================================================
    // Update tracked files
    //=========================================================================
    
    public static void update_files() {
        // Create commit object
        Fida.Commit next_commit = allocate_commit();
        
        List<Fida.File> tree = g_fida.state.tree;
        for (Fida.File ff : tree) {
            
            // Create a new File object by combining the repository db
            // directory with the relative path
            File file = new File(g_fida.file.getParent(), ff.path);
            
            if ((file.isFile() == false) || (file.exists() == false)) {
                // Abort
                throw new RuntimeException(String.format(
                    "%s: File disappeared", file.getPath()));
            } // if

            // Calculate the digest
            Digest curdigest = null;
            try {
                curdigest = Digest.create("md5", file);
            } catch(Exception ex) {
                throw new RuntimeException(String.format(
                    "%s: cannot calculate digest; %s\n", 
                    ff.path, ex.getMessage()), ex);
            } // try-catch
            
            if (curdigest.equals(ff.digest)) {
                System.out.printf("Unmodified %s\n", ff.path);
                continue;
            } // if
            
            // Otherwise an update is needed
            // Otherwise create an update entry
            Fida.File newff = new Fida.File();
            next_commit.layout.add(newff);
            
            // Assign a unique id
            newff.item_xid = generate_xid("file");
            // Make it a successor of the previous
            newff.prev = ff;
            // Set the action
            newff.action = Fida.ACTION_FILE_UPDATED;
            // Just copy
            newff.path = ff.path;
            
            // Re-calcualte the digest AFTER the file has been modified!!
            newff.digest = null;
        } // for: each file
        
        // COMMIT FILES
        //==============
        
        commit_files(next_commit);

        // re-serialize the ingested files.
        // It is done later, but I don't know it would be more clear
        // to do it here with a function call...
        
    } // update_files()

    //=========================================================================
    // Add files
    //=========================================================================
    
    public static void add_files(List<String> filenames) {
        
        Map<File, Document> added_files = new LinkedHashMap<File, Document>();
        Map<File, List<Stack<Xid>>> unexpands_map 
            = new LinkedHashMap<File, List<Stack<Xid>>>();

        // Allocate commit object; this makes the allocated commit object
        // known globally at g_fida.next_commit. The populate() method used
        // that knowledge.
        Fida.Commit next_commit = allocate_commit();
        
        for (String fname : filenames) {
            
            File file = new File(fname);
            if ((file.isFile() == false) || (file.exists() == false)) {
                // Abort
                throw new RuntimeException(String.format(
                    "Not a file or does not exist: %s", file.getPath()));
            } // if
            
            // Convert the path into relative form relative to repo basedir
            try {
                file = FileHelper.getRelativePath(
                    g_fida.file.getParentFile(), file);
            } catch(Exception ex) {
                throw new RuntimeException(String.format(
                    "%s: getRelativePath() failed", fname), ex);
            }
            
            if (is_already_tracked(file)) {
                throw new RuntimeException(String.format(
                    "%s: already tracked!", file.getPath()));
            }
            
            // Create new Fida.File object to record the details of
            // the newly added file.
            Fida.File ff = new Fida.File();
            
            // Set xid
            ff.item_xid = generate_xid("file");
            
            // Record the path (relative form)
            ff.path = file.getPath();
            
            // Mark proper action
            ff.action = Fida.ACTION_FILE_ADDED;
            
            // Add the newly created Fida.File to the Fida.Commit object
            // it belongs to.
            next_commit.layout.add(ff);
            ff.parent_commit = next_commit;
            
            // TODO: Preprocess the XML document. That is, expand
            // relative @ids, missing rev data from new xids, and so on.
            
        } // for: each file name
        
        commit_files(next_commit);
        
        // Rewrite the updated files
    } // add_files()
    
    //=========================================================================
    // commit_files() - this is used by "add", "update" and "remove"
    //=========================================================================
    // Finish the commit if succesfully processed 
    // and add it to repository db.
    public static void commit_files(Fida.Commit next_commit) {
        
        // Create link
        FidaRepository db = new FidaRepository(g_fida);
        
        // Set update options
        UpdateLogic.g_opt_unrev_unknowns = g_fida.state.unrev_unknowns;
        UpdateLogic.g_opt_ingest_unknowns = g_fida.state.allow_unknowns;
        
        
        // Preprocess the whole set
        for (Fida.File ff : next_commit.layout) {
            
            // Don't attempt to process removed files
            if (ff.action == Fida.ACTION_FILE_REMOVED) {
                continue;
            }

            // The digest should be calculated AFTER the repository
            // has been updated and AFTER the ingested files are updated
            // to the disk with updated revision numbers. Otherwise,
            //  the digest values won't be corrent
            
            System.out.printf("Processing file %s\n", ff.path);
            
            // Attempt to read the XML document
            Document doc = null;
            try {
                // TODO:
                // Read file's XML encoding directive 
                // and put into into Fida.File object.
                
                // The String ff.path is a relative path to the repo basedir
                File source = new File(g_fida.file.getParentFile(), ff.path);
                // Attempt reading the XML document. This may throw.
                doc = XMLFileHelper.deserialize_document(source);
                
            } catch(Exception ex) {
                // Bubble up the message
                throw new RuntimeException(ex.getMessage(), ex);
            } // try-catch
            
            // If this point is reached, the file is a well-formed XML doc.
            // We might as well record it already to the Fida.File object.
            ff.doc = doc;

            // Pick the root to a local variable for convenience.
            Element root = doc.getRootElement();
            
            // Preprocess the document
            //=========================
            
            preprocess(root);
            
        } //  for: each ff

        // If and only if all files are preprocessed correctly,
        // may the commit proceed to populating the database in memory.
        
        // Process each document against revision database.
        for (Fida.File ff : next_commit.layout) {
            
            // Don't attempt to process removed files
            if (ff.action == Fida.ACTION_FILE_REMOVED) {
                continue;
            } // if: file removed
            
            // Pick the parsed file to a local variable for convenience,
            // and also pick the root element to a local var for convenience.
            Document doc = ff.doc;
            Element root = doc.getRootElement();
            
            // Data structure for the manifestation details
            Map<Element, List<Stack<Xid>>> manifestations_map
                = new LinkedHashMap<Element, List<Stack<Xid>>>();
            
            // Process the XML document; this method call will do the horse
            // work for revision control
            UpdateLogic.ingest(db, root, manifestations_map);
            
            // Get the root xid. An XML document root MUST have a xid,
            // or otherwise it is an error. The xid must be discovered
            // AFTER the population() call, because it may change 
            // the xid's revision (or even name)
            ff.root_xid = XidIdentification.get_xid(root);
            if (ff.root_xid == null) {
                throw new RuntimeException(String.format(
                    "%s: the root element must have a xid!", ff.path));
            } // if: no root xid

            // If the file has specific manifestation details, record them 
            // to the Fida.File object. If there is none map.get() returns 
            // null, and consequently ff.manifestation is set to null too.
            ff.manifestation = manifestations_map.get(null);
            
            // Remove the following comment to see how the file looks
            // after the population procedure. The xid revs should have
            // been filled with proper values.
            //XPathDebugger.debug(doc);
        } // for: each ff
        
        
        // Check all ref_xid values and make sure that their targets exists.
        //====================================================================
        // All possible ids should be known by now, so unknown targets
        // are not allowed anymore.
        validate_ref_xids(next_commit);

        // See if anything was actually updated at all.
        if ((next_commit.nodes.size() == 0) 
            && (next_commit.layout.size() == 0))
        {
            // Nothing was updated. Do not continue.
            return;
        } // if: no modifications
        
        // Create a commit set
        //====================================================================
        
        // The commit set is proper. It can be added to the repository
        // for serialization
        g_fida.commits.add(next_commit);
        // Make the newest commit the head commit
        g_fida.state.head = next_commit;
        // Mark the repository modified
        g_fida.state.modified = true;

        
    } // process_commit_files()

    //=========================================================================
    // Preprocess element recursively
    //=========================================================================
    
    /**
     * TODO: Add a helper class which captures various preferences regarding
     * the behaviour of the preprocessing.
     */
    public static void preprocess(Element elem) {
        
        // Depth-first recursion
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            } // if: not Element
            
            Element child = (Element) obj;
            preprocess(child);
        } // for
        
        
        // 1. preprocess the newly added elements by correcting their xid
        
        // Get the xid of the element, and allow missing rev
        Xid prexid = XidIdentification.get_xid(elem, true); 
        
        if (prexid == null) {
            // The element does not have any identification at all.
            return;
        }
        
        // TODO:
        // Implement here the option -removeall
        
        
        // Revision is missing. Automatically convert it into 
        // an unassigned revision. This is the mechanism which creates 
        // new identities for XML elements.
        if (prexid.rev == Xid.REV_MISSING) {
            // Transform missing revision into an unassigned revision.
            prexid.rev = Xid.REV_UNASSIGNED;
            // Reflect the revision back to the XML element.
            XidIdentification.set_xid(elem, prexid);
        }
        
        // This finished the pre-processing part of the XML element.
        // The-preprocessing could be done PRIOR to populating.
        
        // The element should now have a valid xid, either as (@id, @rev) pair
        // or as @xid. However, it is still possible, that there exists
        // all three. Also, it is possible for the xid's id or rev part
        // to contain invalid values. The following call will sort it out.
        Xid xid = XidIdentification.get_xid(elem);
        
        // If -force flag is in activation, scan for the highest rev number,
        // and update the repository's state to correspond the highest revnum
        // BEFORE repository/tracked files are updated. Otherwise the
        // new xid's could possible receive inconsistent revision numbers.
        // way to ingest unknown revisions! 
        if (g_fida.state.allow_unknowns) {
            update_repository_revision(xid);
            // Scan for the highest revspec?
            //update_repository_revspec(xid);
        } // if
    } // preprocess()

    //=========================================================================
    // Display tree
    //=========================================================================
    
    public static void display_tree() {
        List<Fida.File> tree = g_fida.state.tree;
        System.out.printf("Currently tracked files\n");
        for (Fida.File ff : tree) {
            System.out.printf("r%-10d %s\n",
                ff.item_xid.rev, ff.path);
        }
        System.out.printf("Total %d files. Current revision: %d\n", 
            tree.size(), g_fida.item_xid.rev-1);
    } // display_tree()

    //=========================================================================
    // Display status
    //=========================================================================

    public static void display_status() {
        List<Fida.File> tree = g_fida.state.tree;
        
        for (Fida.File ff : tree) {
            // Combine repo base + relative path
            File file = new File(g_fida.file.getParentFile(), ff.path);
            String status = null;
            
            if ((file.isFile() == false) || (file.exists() == false)) {
                // File has disappeared
                status = "!!";
            } else {
                // Calculate the digest
                Digest curdigest = null;
                try {
                    curdigest = Digest.create("md5", file);
                } catch(Exception ex) {
                    throw new RuntimeException(String.format(
                        "%s: cannot calculate digest; %s\n", 
                        ff.path, ex.getMessage()), ex);
                } // try-catch
                
                if (curdigest.equals(ff.digest)) {
                    status = "OK";
                } else {
                    status = "M";
                } // if-else
            } // if-else
            
            System.out.printf("r%-5d %-2s   %s\n",
                ff.item_xid.rev, status, ff.path);
            
        } // for: each currently tracked file
        System.out.printf("Current revision %d\n", g_fida.item_xid.rev-1);
    } // display_status()

    
    //=========================================================================
    // Get file information
    //=========================================================================
    
    public static void get_file_info(List<String> args) {
        if (args.size() != 2) {
            throw new RuntimeException(String.format(
                "Incorrect number of arguments. Expected: <rev> <path>"));
        }
        
        // Get the revision string
        String revstring = args.get(0);
        // Get the path string
        String path = args.get(1);
        // Convert the revision string into an integer
        int rev = deserialize_revstring(revstring);
        
        Fida.File nearest = get_nearest_file(rev, path);

        if (nearest == null) {
            System.out.printf("%s: file not known\n", path);
        }
        else if (nearest.action == Fida.ACTION_FILE_REMOVED) {
            System.out.printf("File was removed at revision %d\n", nearest.item_xid.rev);
        } else {
            System.out.printf("Nearest match is xid=%s\n", XidString.serialize(nearest.item_xid));
            display_file_info(nearest);
        } // if-else
    } // get_fileinfo();
    
    // TODO:
    // The Date serialization depends on FidaXML class, which is not good. 
    // The date methods are more general and they should be separated from 
    // FidaXML into their own class.
    
    public static void display_file_info(Fida.File ff) {
        
        Fida.File earliest = ff;
        while (earliest.prev != null) {
            earliest = earliest.prev;
        }
        Fida.File latest = ff;
        while (latest.next.size() > 0) {
            latest = latest.next.get(0);
        }  // while
        
        System.out.printf("File details\n");
        System.out.printf("   File xid:         %s\n", XidString.serialize(ff.item_xid));
        System.out.printf("   Path:             %s\n", ff.path);
        System.out.printf("   Digest:           %s\n", ff.digest.toString());
        System.out.printf("   Unexpands:        %d\n", 
            ff.manifestation != null ? ff.manifestation.size() : 0);
        System.out.printf("   Previous xid:     %s\n",
            ff.prev != null ? XidString.serialize(ff.prev.item_xid) : "<no previous>");
        System.out.printf("   Next xid:         %s\n",
            ff.next.size() != 0 ? XidString.serialize(ff.next.get(0).item_xid) : "<no next>");
        System.out.printf("   Commit xid:       %s\n",
            XidString.serialize(ff.parent_commit.item_xid));
        System.out.printf("   Commit date:      %s\n", FidaXML.serialize_date(ff.parent_commit.date));
        System.out.printf("   Commit author:    %s\n", ff.parent_commit.author);
        System.out.printf("   Earliest rev:     %s\n",
            earliest == ff ? "<this>" : XidString.serialize(earliest.item_xid));
        System.out.printf("   Latest rev:       %s\n",
            latest == ff ? "<this>" : XidString.serialize(latest.item_xid));
    } // display_file_info()
    
    //=========================================================================
    // Get commit information
    //=========================================================================
    
    public static void get_commit_info(List<String> args) {
        if (args.size() != 1) {
            throw new RuntimeException(String.format(
                "Incorrect number of arguments. Expected: <rev>"));
        }
        // Get the revision strings
        String revstring = args.get(0);
        // Convert the revision string into an integer
        int rev = deserialize_revstring(revstring);
        
        // Find the nearest commit object
        Fida.Commit nearest = get_nearest_commit(rev);
        // See if there was any
        if (nearest == null) {
            System.out.printf("r%d: commit not known\n", revstring);
        }
        else {
            System.out.printf("Nearest match is xid=%s\n", XidString.serialize(nearest.item_xid));
            display_commit_info(nearest);
        } // if-else
    } // get_fileinfo();
    
    public static void display_commit_info(Fida.Commit commit) {
        System.out.printf("Commit details\n");
        System.out.printf("   Commit xid:       %s\n", XidString.serialize(commit.item_xid));
        System.out.printf("   Date:             %s\n", FidaXML.serialize_date(commit.date));
        System.out.printf("   Author:           %s\n", commit.author);     
        System.out.printf("   Layout:           %d files\n", commit.layout.size());
        System.out.printf("   Nodes:            %d nodes\n", commit.nodes.size());
        System.out.printf("\n");
        
        // Display individual files
        int count;
        count = 0;
        for (Fida.File ff : commit.layout) {
            count++;
            System.out.printf("   File #%-3d         r%-3d  %s\n", count, ff.item_xid.rev, ff.path);
        } // for: each file
        System.out.printf("\n");
        // Display individual nodes
        count = 0;
        for (Fida.Node node : commit.nodes) {
            count++;
            System.out.printf("   Node #%-3d         %s\n", count, XidString.serialize(node.payload_xid));
        } // for: each node
    } // display_commit_info()

    //=========================================================================
    // Display lifelines
    //=========================================================================
    
    /**
     * Displays lifelines of all payload elements
     */
    public static void display_lifelines() {
        // First, gather all nodes
        List<Fida.Node> nodes = new LinkedList<Fida.Node>();
        
        for (Fida.Commit commit : g_fida.commits) {
            for (Fida.Node n : commit.nodes) {
                nodes.add(n);
            } // for
        } // for
        
        int count = 0;
        while (nodes.size() > 0) {
            
            // Seek first non-successor
            Fida.Node node = null;
            for (Fida.Node n : nodes) {
                if (n.prev == null) {
                    node = n;
                    break;
                }
            } // for
            
            // New lifeline discovered
            count++;
            
            // Start traversing
            StringBuilder sb = new StringBuilder();
            int len = 0;
            int last_rev = 0;
            do {
                // Remove the node that is being handled next.
                nodes.remove(node);
                
                if (len > 0) {
                    sb.append(" > ");
                }
                
                if (node.prev != null) {
                    // This is a successor node
                    String prev_id = node.prev.payload_xid.id;
                    String cur_id = node.payload_xid.id;
                    
                    if (prev_id.equals(cur_id)) {
                        sb.append(String.format("r%d", node.payload_xid.rev));
                    } else {
                        sb.append(String.format("%s", 
                            XidString.serialize(node.payload_xid)));
                    }
                } else {
                    // This is the grand-parent node
                    sb.append(String.format("%s", 
                        XidString.serialize(node.payload_xid)));
                }
                
                // Length of the lifeline increases
                len++;
                last_rev = node.payload_xid.rev;
                
                if (node.next.size() > 0) {
                    node = node.next.get(0);
                } else {
                    node = null;
                }
                
            } while (node != null);
            System.out.printf("%-3d   %s\n", count, sb.toString());
        } // while: nodes unempty
    } // display_lifelines()

    //=========================================================================
    // Output xid (rebuild a payload element)
    //=========================================================================

    public static void output_xid(
        List<String> args, 
        int bubble
    ) {
        if (args.size() != 1) {
            throw new RuntimeException(String.format(
                "Incorrect number of arguments"));
        }
        
        String xidstring = args.get(0);
        
        Xid xid = XidString.deserialize(xidstring);
        
        // This throws if not found
        Element elem = resolve_payload_xid(xid);
        
        // Denormalize. No special denormalization
        FidaRepository db = new FidaRepository(g_fida);
        Element copy = denormalize(db, elem, null, null);
        
        // ====================================================
        // Attempt to bubble the namespace declarations upwards
        // ====================================================
        if (bubble == CmdArgs.BUBBLE_GREEDY) {
            NamespacesBubbler.bubble_namespaces_greedy(copy);
        } else if (bubble == CmdArgs.BUBBLE_PRUDENT) {
            NamespacesBubbler.bubble_namespaces_prudent(copy);
        } else {
            // no bubbling
        } // if-else
        
        // Display on screen, exploit XPathDebugger for that...
        XPathDebugger debugger = new XPathDebugger();
        debugger.output_element(copy);
    } // output_xid()
    
    //=========================================================================
    // Output xid (no denormalization)
    //=========================================================================
    
    public static void output2_xid(List<String> args) {
        if (args.size() != 1) {
            throw new RuntimeException(String.format(
                "Incorrect number of arguments"));
        }
        
        String xidstring = args.get(0);
        
        Xid xid = XidString.deserialize(xidstring);

        // Wrap the global repository into abstraction layer
        FidaRepository db = new FidaRepository(g_fida);
        
        // Get the administrative node corresponding to xid
        Fida.Node node = db.get_node(xid);

        // Display on screen, exploit XPathDebugger for that...
        XPathDebugger debugger = new XPathDebugger();
        debugger.output_element(node.payload_element);
    } // output2_xid()
    
    
    //=========================================================================
    // Set repository major and minor versions
    //=========================================================================

    public static void set_repository_version(List<String> args) {
        if (args.size() == 0) {
            System.out.printf("Repository current version: %s\n",
                XidString.serialize(g_fida.item_xid));
            return;
        }
        
        String vstring = args.get(0);
        int v_major;
        int v_minor;
        int k = vstring.indexOf('.');
        if (k == -1) {
            throw new RuntimeException(String.format(
                "There is no dot present in the <major.minor> string: %s", vstring));
        }
        
        try {
            v_major = Integer.parseInt(vstring.substring(0, k));
            v_minor = Integer.parseInt(vstring.substring(k+1));
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "Unable to convert into integers: %s", vstring));
        } // try-catch
        
        if ((v_major < 0) || (v_minor < 0)) {
            throw new RuntimeException(String.format(
                "Both major and minor have to be non-negative!"));
        }
        
        if ((v_major < g_fida.item_xid.v_major)
            || ((v_major == g_fida.item_xid.v_major) 
            && (v_minor < g_fida.item_xid.v_minor)))
        {
            throw new RuntimeException(String.format(
                "The revision cannot be less than the current: %s",
                XidString.serialize(g_fida.item_xid)));
        } // if
        
        g_fida.item_xid.v_major = v_major;
        g_fida.item_xid.v_minor = v_minor;
        
        // MARK THE REPOSITORY AS MODIFIED!
        g_fida.state.modified = true;
        
        System.out.printf("Repository version set: %s\n", vstring);
    } // set_repository_version

    //=========================================================================
    // Increase repository major or minor version
    //=========================================================================

    public static void inc_repository_version(List<String> args) {
        boolean incminor = true; // false implies incmajor
        
        if (g_fida.item_xid.has_version() == false) {
            throw new RuntimeException(String.format(
                "Set the repository version first with setversion"));
        }
        
        if (args.size() == 0) {
            System.out.printf("Defaults to incversion minor\n");
        } else {
            String s = args.get(0);
            if (s.equals("major")) {
                incminor = false;
            } else if (s.equals("minor")) {
                incminor = true;
            } else {
                throw new RuntimeException(String.format(
                    "The argument should be either \'minor\' or \'major\'"));
            }
        } // if-else
        
        if (incminor == true) {
            g_fida.item_xid.v_minor++;
        } else {
            g_fida.item_xid.v_major++;
        }
        
        // MARK THE REPOSITORY AS MODIFIED!
        g_fida.state.modified = true;
        
        System.out.printf("Repository version increased: %d.%d\n",
            g_fida.item_xid.v_major, g_fida.item_xid.v_minor);
        
    } // inc_repository_version
    
    
    //=========================================================================
    // Rebuild a file
    //=========================================================================
    
    public static void rebuild_file(
        List<String> args, 
        int bubble
    ) {
        if (args.size() < 3) {
            throw new RuntimeException(String.format(
                "Incorrect number of arguments. Expected: <rev> <path> <new_name>"));
        }
        
        String revstring = args.get(0);
        String path = args.get(1);
        String newname = args.get(2);
        // Convert revstring into an integer
        int rev = deserialize_revstring(revstring);
        
        // TODO:
        // Seek out the file's manifestation at <rev>.
        Fida.File nearest = get_nearest_file(rev, path);
        
        if (nearest == null) {
            System.out.printf("%s: file not known\n", path);
        }
        else if (nearest.action == Fida.ACTION_FILE_REMOVED) {
            System.out.printf("File was removed at revision %d\n", nearest.item_xid.rev);
        } else {
            // Continue to rebuild
            System.out.printf("Nearest match is xid=%s\n", XidString.serialize(nearest.item_xid));
            System.out.printf("Rebuilding\n");
            
            build_manifestation(nearest, newname, bubble);
            
        } // if-else
    } // rebuild_file();
    
    public static void build_manifestation(
        Fida.File ff, 
        String filename,
        int bubble
    ) {
        List<Stack<Xid>> manifestation = ff.manifestation;
        Element root = null;
        
        root = resolve_payload_xid(ff.root_xid);
        
        // Build
        
        FidaRepository db = new FidaRepository(g_fida);
        Element newroot = denormalize(db, root, manifestation, null);
        
        // ====================================================
        // Attempt to bubble the namespace declarations upwards
        // ====================================================
        if (bubble == CmdArgs.BUBBLE_GREEDY) {
            NamespacesBubbler.bubble_namespaces_greedy(newroot);
        } else if (bubble == CmdArgs.BUBBLE_PRUDENT) {
            NamespacesBubbler.bubble_namespaces_prudent(newroot);
        } else {
            // no bubbling
        } // if-else
        
        Document doc = new Document(newroot);
        
        try {
            File file = new File(filename);
            XMLFileHelper.serialize_document_formatted(doc, file);
            System.out.printf("Created: %s\n", file.getPath());
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    } // build_manifestation()

    //=========================================================================
    // Denormalization of a payload XML element.
    // These pair with normalization, and they are more generic. 
    // They should be separated and put into core package.
    //=========================================================================
    
    public static Element denormalize(
        AbstractRepository db,
        Element elem,
        List<Stack<Xid>> manifestation,
        Map<Fida.Node, Fida.Node> migration
    ) {
        Element rval = null;

        // Create an initial copy
        rval = new Element(elem.getName(), elem.getNamespace());
        
        // Clone attributes
        List attributes = elem.getAttributes();
        for (Object obj : attributes) {
            Attribute a_orig = (Attribute) obj;
            Attribute a_copy = (Attribute) a_orig.clone();
            rval.setAttribute(a_copy);
        } // for: each attr
        
        // Clone content
        List content = elem.getContent();
        for (Object obj : content) {
            
            if (obj instanceof Element) {
                // This is a child element. It needs more careful inspection.
                Element child = (Element) obj;
                // A copy the child is made in normalize_child()
                // and the copy is then added as a content to the current
                // element. It does not matter whether the copy will be 
                // a referencing copy or a normalized copy. 
                rval.addContent(
                    denormalize_child(db, child, manifestation, migration));
            } 
            else {
                // Either Text, Comment, CDATA or something similar.
                // Just make an identical copy of it.
                Content content_orig = (Content) obj;
                Content content_copy = (Content) content_orig.clone();
                rval.addContent(content_copy);
            } // if-else: instance of Element
        } // for: each content object
        
        return rval;
    } // denormalize()
    
    @SuppressWarnings("unchecked")
    private static Element denormalize_child(
        AbstractRepository db,
        Element child,
        List<Stack<Xid>> manifestation,
        Map<Fida.Node, Fida.Node> migration
    ) {
        // Return variable
        Element rval = null;
        
        // Attempt to pick ref_xid, if any
        //Xid xid = XidIdentification.get_ref_xid(child);
        Xid ref_xid = get_ref_xid(child);

        // Filter the manifestation at the same time.
        // Defaults to the one that was passed in.
        List<Stack<Xid>> next_manifestation = manifestation;
        
        // Whether the child is to be expanded or not.
        boolean expand = true;
        
        if (ref_xid != null) {
            // Inclusion-by-xid.
            // It must have a link_xid attribute
            String value = child.getAttributeValue("link_xid");
            
            if (value == null) {
                // No idea how to tell more precisely where we are.
                throw new RuntimeException(String.format(
                    "ref_xid element does not have a link_xid attribute"));
            } // if
            
            // Pick the link xid
            Xid link_xid = XidString.deserialize(value);
            // Strip out those entries from the manifestation which do not
            // have this as the top.
            
            if (manifestation != null) {
                // TODO:
                // Separate filtering procedure somewhere else.
                next_manifestation = new LinkedList<Stack<Xid>>();
                for (Stack<Xid> unexpand : manifestation) {
                    if (unexpand.peek().equals(link_xid) == false) {
                        // Filter out
                        continue;
                    } // if
                    
                    // Copy...
                    Stack<Xid> copy = (Stack<Xid>) unexpand.clone();
                    // ..and pop
                    copy.pop();
                    
                    // If became an empty stack, then this is exactly
                    // the link_xid that should not be expanded
                    if (copy.empty() == true) {
                        expand = false;
                        System.out.printf("Not expanding: %s\n", XidString.serialize(link_xid));
                        //next_manifestation = null;
                        //break;
                    } else {
                        // If link_xids were left into the unexpand guide,
                        // then add it to the filter result
                        next_manifestation.add(copy);
                    } // if-else
                } // for: each unexpand
            } // if: there is a manifestation
            
            // Make the expansion only if the it was decided to do.
            
            if (expand == true) {
                // Jump to a different child. 
                // First, get the administrative node of the target xid
                Fida.Node target_node = db.get_node(ref_xid);
                
                // Verify that the target xid was found
                if (target_node == null) {
                    throw new RuntimeException(String.format(
                        "Cannot resolve payload xid=%s", XidString.serialize(ref_xid)));
                }
                
                // 1. Apply manifestation mapping
                // TODO: Apply manifestation mapping here
                // 2. Apply migration mapping
                if ((migration != null) && (target_node.next.size() > 0)) {
                    // Can be migrated. See if this administrative node
                    // has already been migrated
                    Fida.Node mig_node = migration.get(target_node);
                    
                    if (mig_node == null) {
                        // Not yet migrated. Seek the newest one
                        mig_node = target_node;
                        while (mig_node.next.size() > 0) {
                            mig_node = mig_node.next.get(0);
                        } // while: has next
                        // record the redirection
                        migration.put(target_node, mig_node);
                    } 
                    // Re-target
                    target_node = mig_node;
                } // if: migration is to be applied

                // Assign the payload element as the next child
                child = target_node.payload_element;
                /*
                System.out.printf("dereferencing %s\n",
                    XidString.serialize(target_node.payload_xid));
                */
            } // if: the inclusion-by-xid is going to be expanded
        } // if: inclusion-by-xid
        
        rval = denormalize(db, child, next_manifestation, null);
        
        if (expand == false) {
            // Strip out the link_xid information from the created copy
            rval.removeAttribute("link_xid");
        }
        
        return rval;
    } // denormalize_child()

    //=========================================================================
    // Migrate files: primary method (a proper graph implementation)
    //=========================================================================
    
    /**
     * @param args unused
     */
    public static void migrate_files2(List<String> args) {
        // PHASE 1: Read all files in the current tree
        //*********************************************
        
    }
    
    public static Fida.Commit read_tree(Fida.Repository fidaRepo) {
        // Create link
        FidaRepository db = new FidaRepository(fidaRepo);
        
        // Allocate a commit
        Fida.Commit next_commit = allocate_commit();
        
        // For convenience
        List<Fida.File> tree = fidaRepo.state.tree;
        
        // Migrate all files
        for (Fida.File ff : tree) {
            // "ff.getPath(repo)"
            File file = new File(fidaRepo.file.getParent(), ff.path);
            if ((file.isFile() == false) || (file.exists() == false)) {
                // Abort
                throw new RuntimeException(String.format(
                    "%s: File disappeared", file.getPath()));
            } // if
            
            // Calculate the digest
            Digest curdigest = null;
            try {
                curdigest = Digest.create("md5", file);
            } catch(Exception ex) {
                throw new RuntimeException(String.format(
                    "%s: cannot calculate digest; %s\n", 
                    ff.path, ex.getMessage()), ex);
            } // try-catch
            
            if (curdigest.equals(ff.digest) == false) {
                throw new RuntimeException(String.format(
                    "%s: Uncommited changes", ff.path));
            } // if
            
            Fida.File newff = new Fida.File();
            next_commit.layout.add(newff);
            
            Document doc = null;
            try {
                // The String ff.path is a relative path to the repo basedir
                File source = new File(fidaRepo.file.getParentFile(), ff.path);
                // Attempt reading the XML document. This may throw.
                doc = XMLFileHelper.deserialize_document(source);
                
            } catch(Exception ex) {
                // Bubble up the message
                throw new RuntimeException(ex.getMessage(), ex);
            } // try-catch
            
            // If this point is reached, the file is a well-formed XML doc.
            // We might as well record it already to the Fida.File object.
            newff.doc = doc;
            newff.path = ff.path;
            // Preprocess (Should be done when the context-depend ids
            // are introduced)
            //Element root = doc.getRootElement();
            //preprocess(root);
        } // for: each file in the current tree
        
        return next_commit;
    } // read_tree()

    //=========================================================================
    // Migrate files
    //=========================================================================
    
    public static void migrate_files(List<String> args) {
        
        // Allocate a commit, and read the head tree into it
        Fida.Commit next_commit = read_tree(g_fida);

        // Create link
        FidaRepository db = new FidaRepository(g_fida);
        
        // Traverse through all files read
        List<Fida.File> files = new LinkedList<Fida.File>();
        for (Fida.File ff : next_commit.layout) {
            Element root = ff.doc.getRootElement();
            Map<Fida.Node, Fida.Node> map = new LinkedHashMap<Fida.Node, Fida.Node>();
            try {
                migrate_element(db, root, map);
            } catch(Exception ex) {
                throw new RuntimeException(String.format(
                    "%s: %s", ff.path, ex.getMessage()), ex);
            } // try-catch
            // if there was no migrations at all, skip the rest
            if (map.size() == 0) {
                continue;
            }
            
            // Otherwise, schedule this file for rewriting
            files.add(ff);
            
            // and display detailed information about migrations.
            System.out.printf("%s: %d migrations\n", ff.path, map.size());
            for (Map.Entry<Fida.Node, Fida.Node> entry : map.entrySet()) {
                Fida.Node source = entry.getKey();
                Fida.Node target = entry.getValue();
                System.out.printf("   %25s -> %s\n", 
                    XidString.serialize(source.payload_xid),
                    XidString.serialize(target.payload_xid)
                );
            } //  for
                
        } // for: each file read
        next_commit.layout = files;
        
        for (Fida.File rewriteff : next_commit.layout) {
            File f = new File(g_fida.file.getParent(), rewriteff.path);
            //File f = new File(rewriteff.path);
            Document doc = rewriteff.doc;
            try {
                // If the file has migrations, rewrite it
                XMLFileHelper.serialize_document_formatted(doc, f);
                //XMLFileHelper.serialize_document_verbatim(doc, f);
            } catch(Exception ex) {
                throw new RuntimeException(ex);
            } // try-catch
        } // fo
        System.out.printf("Files updated. Run \"fida update\"\n");;
        System.out.printf("(The migration is not implemented correctly yet)\n");
        // Write down
        
    } // migrate_files()
    
    // TODO: This cannot account for file's manifestations data.
    // TODO: Determine spanning tree / trace the breadth of changes
    // in dijkstra style. Allow the use of # in references?
    public static void migrate_element(
        AbstractRepository db,
        Element elem,
        Map<Fida.Node, Fida.Node> map
    ) {
        Xid xid = XidIdentification.get_xid(elem);
        boolean recurse = true;
        
        if (xid != null) {
            // has xid
            Fida.Node node = db.get_node(xid);
            if (node == null) {
                throw new RuntimeException(String.format(
                    "unrecognized xid=%s", XidString.serialize(xid)));
            }
            
            if (node.next.size() > 0) {
                // Migrate! seek the first
                Fida.Node target = map.get(node);
                if (target == null) {
                    target = node;
                    while (target.next.size() > 0) {
                        target = target.next.get(0);
                    } // whilw
                    map.put(node, target);
                } // if: no target rewrote yet.
                node = target;
                
                Element mig_elem = denormalize(db, node.payload_element, null, map);
                // Replace mig_elem in-place with elem.
                replace_inplace(elem, mig_elem);
            } // if: can be migrated
        } else {
            // no xid, do nothing
        } // if-else
        
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            // Recurse
            Element c = (Element) obj;
            migrate_element(db, c, map);
        } // for
        
    } // migrate_element()
    
    public static void replace_inplace(Element target, Element source) {
        target.setAttributes(null);
        target.removeContent();
        // TODO: remove additional namespace declarations?
        target.setName(source.getName());
        target.setNamespace(source.getNamespace());

        // Clone attributes
        List attributes = source.getAttributes();
        for (Object obj : attributes) {
            Attribute a_orig = (Attribute) obj;
            Attribute a_copy = (Attribute) a_orig.clone();
            target.setAttribute(a_copy);
        } // for: each attr
        
        // Deparent source
        List list = source.removeContent();
        // Add to target
        target.setContent(list);
        
    } // replace_inplace()
        

    //=========================================================================
    // HELPER METHODS BEGIN HERE
    //=========================================================================
    
    public static void validate_ref_xids(Fida.Commit next_commit) {
        for (Fida.Node node : next_commit.nodes) {
            Element elem = node.payload_element;
            validate_ref_xids(elem);
        }
    }
    
    private static void validate_ref_xids(Element elem) {
        // depth-first
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            validate_ref_xids(c);
        }
        
        String refstring = elem.getAttributeValue("ref_xid");
        
        if (refstring != null) {
            Xid ref_xid = XidString.deserialize(refstring);
            Fida.Node target_node = g_fida.state.externals.get(ref_xid);
            if (target_node == null) {
                throw new RuntimeException(String.format(
                    "Unresolved ref_xid=\"%s\"", XidString.serialize(ref_xid)));
            }
        } // if: has a reference
    } // validate_ref_xids()


    public static Fida.File get_nearest_file(int rev, String path) {
        // This will handle the system-dependent dir-separators.
        path = new File(path).getPath();
        
        Fida.File nearest = null;
        for (Fida.Commit fc : g_fida.commits) {
            for (Fida.File ff : fc.layout) {
                if (path.equals(ff.path) == false) {
                    continue;
                }
                
                if (ff.item_xid.rev > rev) {
                    continue;
                }
                
                // Update nearest, if closer to the requested rev
                if (nearest == null) {
                    nearest = ff;
                } else if (ff.item_xid.rev > nearest.item_xid.rev) {
                    nearest = ff;
                } //if-else
            } // for: each file in a commit
        } // for
        
        return nearest;
    } // get_nearest_file()
    
    public static Fida.Commit get_nearest_commit(int rev) {
        Fida.Commit nearest = null;
        for (Fida.Commit fc : g_fida.commits) {
            
            if (fc.item_xid.rev > rev) {
                continue;
            }
                
            // Update nearest, if closer to the requested rev
            if (nearest == null) {
                nearest = fc;
            } else if (fc.item_xid.rev > nearest.item_xid.rev) {
                nearest = fc;
            } //if-else
        } // for: each commit
        
        return nearest;
    } // get_nearest_commit()
    
    protected static String unexpand2string(Stack<Xid> stack) {
        StringBuilder sb = new StringBuilder();
        for (int i = stack.size()-1; i >= 0; i--) {
            sb.append(String.format("/%s",
                XidString.serialize(stack.elementAt(i))));
        } // for
        return sb.toString();
    }
    
    protected static void dump_unexpand_list(List<Stack<Xid>> list) {
        for (Stack<Xid> stack : list) {
            System.out.printf("%s\n", unexpand2string(stack));
        } // for each
    } // dump_unexpand_list() 
    
    /**
     * Returns the node with the greatest rev and the given id.
     * If no such id is known, returns {@code null}.
     */
    public static Fida.Node get_latest_node(String id) {
        // Go through all external xids.
        Fida.Node latest = null;
        for (Map.Entry<Xid, Fida.Node> entry : 
            g_fida.state.externals.entrySet()) 
        {
            Xid xid = entry.getKey();
            if (xid.id.equals(id) == false) {
                continue;
            }
            
            if (latest == null) {
                latest = entry.getValue();
            } else if (xid.rev > latest.payload_xid.rev) {
                latest = entry.getValue();
            } // if-else
        } // for: all user namespace xids
        return latest;
    } // get_latest_node()

    private static Element resolve_payload_xid(Xid xid) {
        Fida.Node node = g_fida.state.externals.get(xid);
        if (node == null) {
            throw new RuntimeException(String.format(
                "Cannot resolve payload xid=%s", XidString.serialize(xid)));
        }
        return node.payload_element;
    } // resolve_payload_xid()
    
    /**
     * TODO: The methods accessing the ref_xid attribute should be
     * separated and encapsulated into their own class.
     * 
     */
    private static Xid get_ref_xid(Element elem) {
        Xid rval = null;
        String value = elem.getAttributeValue("ref_xid");
        if (value != null) {
            rval = XidString.deserialize(value);
        }
        return rval;
    } // get_ref_xid()

    public static int deserialize_revstring(String revstring) {
        // Return variable
        int rval;
        try {
            rval = Integer.parseInt(revstring);
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "Expected a revision number (an integer), but found: %s", revstring));
        } // try-catch
        
        return rval;
    } // deserialize_revstring()

} // XidClient

