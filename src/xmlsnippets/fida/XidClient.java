//*******************************{begin:header}******************************//
//     XML Processing Snippets - https://code.google.com/p/xml-snippets/     //
//***************************************************************************//
//
//      xml-snippets:   XML Processing Snippets 
//                      with Some Theoretical Considerations
//
//      Copyright (C) 2012 Jani Hautamäki <jani.hautamaki@hotmail.com>
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
    
    // HELPER CLASS TO CAPTURE COMMAND-LINE
    public static class CmdArgs {
        public boolean debug_flag = false;
        public boolean addall_flag = false;
        public String repo_filename = DEFAULT_FIDA_REPOSITORY;
        public List<String> filenames = new LinkedList<String>();
        public String command_arg = null;
    } // class CmdArgs
    
    
    
    // BRIDGE TO THE BACKEND REPOSITORY DATA STRUCTURE
    //================================================
    
    /**
     * The background data structure
     */
    private static Fida.Repository g_fida = null;
    
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
     */
    public static void read_fida_repository(String filename) {
        File file = new File(filename);
        
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
     * Generates an internal xid with an uid, and records that uid
     * as used.
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
        List<Fida.File> tree = g_fida.state.tree;
        
        try {
            file = file.getCanonicalFile();
            
            for (Fida.File ff : tree) {
                File tracked = new File(ff.path);
                // Get canonial file
                tracked = tracked.getCanonicalFile();
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
     * Retrieves the Fida.Node record of the xid corresponding 
     * to the payload. Returns null if no such xid
     *
     */
    public static Fida.Node get_fida_node(Xid payload_xid) {
        return g_fida.state.externals.get(payload_xid);
    } // get_fida_node();
    
    /**
     * Revised the given xid.
     */
    public static void new_xid_revision(Xid xid) {
        xid.rev = g_fida.item_xid.rev;
    } // new_xid_revision()
    
    /**
     * Indicates whether elements with revisions specified but which
     * are not found from the repository can be recorded.
     *
     * @return The member variable {@code g_fida.state.allow_unknowns}.
     */
    public static boolean is_unknowns_allowed() {
        return g_fida.state.allow_unknowns;
    } // is_unknowns_allowed()
    
    /**
     * This functions can cause the repositorys state / current revision 
     * number to jump abruply and in discontinous manner.
     * What is being asserted is that the newest revision number is
     * always the greatest, so that the rev+1 is always non-existent
     * in the repository.
     * 
     * @param rev Revision number which must be guaranteed to be past number.
     */
    public static void update_repository_revision(int rev) {
        if (g_fida.item_xid.rev < rev) {
            g_fida.item_xid.rev = rev;
        }
    } // update_repository_version()
    
    /**
     * Adds a new Fida.Node to the CURRENT commit (it might get discarded)
     * The payload element must be unparented copy! It will be owned
     * by the system after this.
     * 
     * @param payload_element the payload content of the new node
     * The payload element is expected to be unparented!
     * @param prev the previous node, or {@code null} if none.
     */
    public static void add_fida_node(
        Element payload_element, 
        Fida.Node prev
    ) {
        // Create a new Fida.Node
        Fida.Node node = new Fida.Node();
        
        // Assign a xid and uid
        node.item_xid = generate_xid("node");
        
        // Set link to the previous (if any)
        node.prev = prev;
        
        // Set payload content
        node.payload_element = payload_element;
        
        // Pick the xid to a local variable for convenience
        Xid payload_xid = XidIdentification.get_xid(node.payload_element);
        
        // Record the payload xid to the node object
        node.payload_xid = payload_xid;
        
        // Local variable for convenience.
        Map<Xid, Fida.Node> externals = g_fida.state.externals;
        
        // Verify the xid is free
        if (externals.get(payload_xid) != null) {
            throw new RuntimeException(String.format(
                "Error: xid=%s already added to the repository",
                XidString.serialize(payload_xid)));
        } // if
        
        // Assign to the proper commit.
        // The commit information must come from some other route.
        // Currently it is easy, because a global variable is used.
        node.parent_commit = g_fida.next_commit;
        
        // The node is added to the node set of the next commit
        g_fida.next_commit.nodes.add(node);
        
        // Also remember to put it into externals to make
        // it discoverable!
        externals.put(payload_xid, node);
        
        
        // For debugging. Tell what was inserted
        /*
        System.out.printf("Inserted node=%s for payload xid=%s\n",
            XidString.serialize(node.item_xid),
            XidString.serialize(node.payload_xid)
        ); // printf
        */
        
    } // add_fida_node()
    
    
    
    
    
    
    //========================================================================
    // MAIN
    //========================================================================
    
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
            else if (command.equals("init")) {
                
                create_fida_repository(cmd_args.repo_filename, "unnamed");
                write_fida_repository();
                System.out.printf("Created: %s\n", g_fida.file.getPath());
                
                System.exit(EXIT_SUCCESS);
            } // if-else
            
            // At this stage, read the FIDA repository file
            read_fida_repository(cmd_args.repo_filename);
            System.out.printf("Parsed: %s\n", g_fida.file.getPath());
            
            // Increase the revision number
            g_fida.item_xid.rev++;

            if (cmd_args.addall_flag == true) {
                System.out.printf("Warning: allow_unknowns=true.\n");
                g_fida.state.allow_unknowns = true;
            }
            
            if (command.equals("add")) {
                if (cmd_args.filenames.size() == 0) {
                    throw new RuntimeException("No files to add");
                }
                add_files(cmd_args.filenames);
            }
            else if (command.equals("remove")) {
                remove_files(cmd_args.filenames);
            }
            else if (command.equals("update")) {
                update_files();
            }
            else if (command.equals("status")) {
                display_status();
            }
            else if (command.equals("rebuild")) {
                rebuild_file(cmd_args.filenames);
            }
            else if (command.equals("fileinfo")) {
                get_fileinfo(cmd_args.filenames);
            }
            else if (command.equals("debug")) {
                // Execute debug mode
            }
            else if (command.equals("check")) {
                System.out.printf("Check.\n");
            }
            else if (command.equals("tree")) {
                display_tree();
            }
            else if (command.equals("lifelines")) {
                display_lifelines();
            }
            else if (command.equals("help")) {
                display_help();
            }
            else {
                throw new RuntimeException(String.format(
                    "Error: unknown command \"%s\"", command));
            } // if-else known command?
            
            // Re-serialize the ingested files and the repository
            //====================================================
            
            if (g_fida.state.modified == true) {
                // Rewrite the updated files, if any
                // This also updates the file digest values which must be
                // done PRIOR to the serialization of the repository itself.
                for (Fida.File rewriteff : g_fida.next_commit.layout) {
                    if (rewriteff.action == Fida.ACTION_FILE_REMOVED) {
                        // Delete the file?
                        continue;
                    } // if
                    
                    File f = new File(rewriteff.path);
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
                            "%s: cannot calculate digest; %s\n", f.getPath(), ex.getMessage()), ex);
                    } // try-catch
                } // for: each file the commit set

                // Re-serialize the repository
                write_fida_repository();
                // Mark the state unmodified
                g_fida.state.modified = false;
                // Remove the next_commit
                g_fida.next_commit = null;
                
                // Done
                System.out.printf("Current revision %d\n", g_fida.item_xid.rev);
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
        System.out.printf("Commands:\n");
        System.out.printf("  Managing files:\n");
        System.out.printf("    add <file1> [file2] ...          start tracking files\n");
        System.out.printf("    remove <file1> [file2] ...       drop files from tracking\n");
        System.out.printf("    update <file1> [file2] ...       update repository\n");
        System.out.printf("\n");
        System.out.printf("  Miscellaneous:\n");
        System.out.printf("    status                           displaystatus of tracked files\n");
        System.out.printf("    fileinfo <rev> <path>            display file record details\n");
        System.out.printf("    tree                             display currently tracked files\n");
        System.out.printf("    lifelines                        display lifelines of the XML elements\n");
    } // display_help()

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
                    rval.filenames.add(carg);
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
        
        // The commit set is proper. It can be added to the repository
        // for serialization
        g_fida.commits.add(next_commit);
        // Make the newest commit the head commit
        g_fida.state.head = next_commit;
        // Mark the repository modified
        g_fida.state.modified = true;
    } // remove_files()
    
    
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
            File file = new File(ff.path);
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
                        "%s: cannot calculate digest; %s\n", ff.path, ex.getMessage()), ex);
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
    // Update tracked files
    //=========================================================================
    
    public static void update_files() {
        // Create commit object
        Fida.Commit next_commit = allocate_commit();
        
        List<Fida.File> tree = g_fida.state.tree;
        for (Fida.File ff : tree) {
            
            File file = new File(ff.path);
            
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
                    "%s: cannot calculate digest; %s\n", ff.path, ex.getMessage()), ex);
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
            
            
            // Process the file!
            //===================
            
            System.out.printf("Processing file %s\n", file.getPath());
            // Attempt to read the XML document
            Document doc = null;
            try {
                doc = XMLFileHelper.deserialize_document(file);
            } catch(Exception ex) {
                // Bubble up the message
                throw new RuntimeException(ex.getMessage(), ex);
            } // try-catch
            
            newff.doc = doc;
            Element root = doc.getRootElement();
            
            // Data structure for the manifestation details
            Map<Element, List<Stack<Xid>>> manifestations_map
                = new LinkedHashMap<Element, List<Stack<Xid>>>();
            
            // Process the XML document; this method call will do the horse
            // work for revision control
            populate(root, manifestations_map);
            
            newff.root_xid = XidIdentification.get_xid(root);
            if (newff.root_xid == null) {
                throw new RuntimeException(String.format(
                    "%s: the root element must have a xid!", ff.path));
            } // if: no root xid

            newff.manifestation = manifestations_map.get(null);
            
        } // for: each file
        
        // See if anything was actually updated at all.
        if ((next_commit.nodes.size() == 0) 
            && (next_commit.layout.size() == 0))
        {
            // Nothing was updated. Do not continue.
            return;
        } // if: no modifications

        
        // Check all references exist!
        validate_ref_xids(next_commit);
        
        // The commit set is good to go. 
        // It can be added to the repository for later serialization
        g_fida.commits.add(next_commit);
        // Make the newest commit the head commit
        g_fida.state.head = next_commit;
        // Mark the repository modified
        g_fida.state.modified = true;

        // re-serialize the ingested files.
        // It is done later, but I don't know it would be more clear
        // to do it here with a function call.
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
            
            // Convert the file name into relative path
            // TODO^^^^^^^^^^^^^^
            if (is_already_tracked(file)) {
                throw new RuntimeException(String.format(
                    "%s: already tracked!", file.getPath()));
            }
            
            // Create new Fida.File object to record the details of
            // the newly added file.
            Fida.File ff = new Fida.File();
            
            // Set xid
            ff.item_xid = generate_xid("file");
            
            // Add the newly created Fida.File to the Fida.Commit object
            // it belongs to.
            next_commit.layout.add(ff);
            ff.parent_commit = next_commit;
            
            // Record the path
            ff.path = file.getPath();
            
            // Mark proper action
            ff.action = Fida.ACTION_FILE_ADDED;
            
            // The digest will be calculated after all the files have been
            // updated and rewritten correctly.
            // ff.digest = ...
            
            System.out.printf("Processing file %s\n", file.getPath());
            
            // Attempt to read the XML document
            Document doc = null;
            try {
                doc = XMLFileHelper.deserialize_document(file);
            } catch(Exception ex) {
                // Bubble up the message
                throw new RuntimeException(ex.getMessage(), ex);
            } // try-catch
            
            // If this point is reached, the file is a well-formed XML doc.
            // We might as well record it already to the Fida.File object.
            ff.doc = doc;
            
            // TODO: Preprocess the XML document. That is, expand
            // relative @ids, missing rev data from new xids, and so on.
            
            // Then:
            // See if the file is valid as an individual file
            // See if the file is valid with respect to the repository

            // Pick the root element to a local variable for convenience.
            Element root = doc.getRootElement();
            
            // Data structure for the manifestation details
            Map<Element, List<Stack<Xid>>> manifestations_map
                = new LinkedHashMap<Element, List<Stack<Xid>>>();
            
            // Process the XML document; this method call will do the horse
            // work for revision control
            populate(root, manifestations_map);
            
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
            
        } // for: each file name
        
        // TODO: Check all ref_xid values. All of them should be known by now.
        //====================================================================
        validate_ref_xids(next_commit);

        
        // Create a commit set
        //====================================================================
        
        // The commit set is proper. It can be added to the repository
        // for serialization
        g_fida.commits.add(next_commit);
        // Make the newest commit the head commit
        g_fida.state.head = next_commit;
        // Mark the repository modified
        g_fida.state.modified = true;

        // Rewrite the updated files
    } // add_files()

    //=========================================================================
    // Get file information
    //=========================================================================
    
    public static void get_fileinfo(List<String> args) {
        if (args.size() != 2) {
            throw new RuntimeException(String.format(
                "Incorrect number of arguments. Expected: <rev> <path>"));
        }
        
        String revstring = args.get(0);
        String path = args.get(1);
        // TODO:
        // Seek out the file's manifestation at <rev>.
        Fida.File nearest = get_nearest_file(revstring, path);

        if (nearest == null) {
            System.out.printf("%s: file not known\n", path);
        }
        else if (nearest.action == Fida.ACTION_FILE_REMOVED) {
            System.out.printf("File was removed at revision %d\n", nearest.item_xid.rev);
        } else {
            System.out.printf("Nearest match is xid=%s\n", XidString.serialize(nearest.item_xid));
            display_fileinfo(nearest);
        } // if-else
    } // get_fileinfo();
    

    //=========================================================================
    // Rebuild a file
    //=========================================================================
    
    public static void rebuild_file(List<String> args) {
        if (args.size() < 3) {
            throw new RuntimeException(String.format(
                "Incorrect number of arguments. Expected: <rev> <path> <new_name>"));
        }
        
        String revstring = args.get(0);
        String path = args.get(1);
        String newname = args.get(2);
        // TODO:
        // Seek out the file's manifestation at <rev>.
        Fida.File nearest = get_nearest_file(revstring, path);
        
        if (nearest == null) {
            System.out.printf("%s: file not known\n", path);
        }
        else if (nearest.action == Fida.ACTION_FILE_REMOVED) {
            System.out.printf("File was removed at revision %d\n", nearest.item_xid.rev);
        } else {
            // Continue to rebuild
            System.out.printf("Nearest match is xid=%s\n", XidString.serialize(nearest.item_xid));
            System.out.printf("Rebuilding\n");
            
            build_manifestation(nearest, newname);
            
        } // if-else
    } // rebuild_file();
    
    private static Element resolve_payload_xid(Xid xid) {
        Fida.Node node = g_fida.state.externals.get(xid);
        if (node == null) {
            throw new RuntimeException(String.format(
                "Cannot resolve payload xid=%s", XidString.serialize(xid)));
        }
        return node.payload_element;
    } // resolve_payload_xid()
    
    public static void build_manifestation(Fida.File ff, String filename) {
        List<Stack<Xid>> manifestation = ff.manifestation;
        Element root = null;
        
        root = resolve_payload_xid(ff.root_xid);
        
        // Build
        Element newroot = denormalize(root, manifestation);
        Document doc = new Document(newroot);
        
        try {
            File file = new File(filename);
            XMLFileHelper.serialize_document_formatted(doc, file);
            System.out.printf("Created: %s\n", file.getPath());
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    } // build_manifestation()
    
    public static Element denormalize(
        Element elem,
        List<Stack<Xid>> manifestation
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
                rval.addContent(denormalize_child(child, manifestation));
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
    
    private static Xid get_ref_xid(Element elem) {
        Xid rval = null;
        String value = elem.getAttributeValue("ref_xid");
        if (value != null) {
            rval = XidString.deserialize(value);
        }
        return rval;
    } // get_ref_xid()

    @SuppressWarnings("unchecked")
    private static Element denormalize_child(
        Element child,
        List<Stack<Xid>> manifestation
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
                // Jump to a different child
                child = resolve_payload_xid(ref_xid);
            }
            
        } // if: inclusion-by-xid
        
        rval = denormalize(child, next_manifestation);
        
        if (expand == false) {
            // Strip out the link_xid information from the created copy
            rval.removeAttribute("link_xid");
        }
        
        return rval;
    } // denormalize_child()

    
    
    
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
            if (get_fida_node(ref_xid) == null) {
                throw new RuntimeException(String.format(
                    "Unresolved ref_xid=\"%s\"", XidString.serialize(ref_xid)));
            }
        } // if: has a reference
    } // validate_ref_xids()


    //=========================================================================
    // HELPER METHODS BEGIN HERE
    //=========================================================================
    
    public static Fida.File get_nearest_file(String revstring, String path) {
        int rev;
        
        try {
            rev = Integer.parseInt(revstring);
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "Expected an integer, but found: %s\n", revstring));
        } // try-catch
        
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
    
    public static void display_fileinfo(Fida.File ff) {
        
        Fida.File earliest = ff;
        while (earliest.prev != null) {
            earliest = earliest.prev;
        }
        Fida.File latest = ff;
        while (latest.next.size() > 0) {
            latest = latest.next.get(0);
        }  // while
        
        System.out.printf("File details\n");
        System.out.printf("   File xid:        %s\n", XidString.serialize(ff.item_xid));
        System.out.printf("   Path:            %s\n", ff.path);
        System.out.printf("   Digest:          %s\n", ff.digest.toString());
        System.out.printf("   Unexpands:       %d\n", 
            ff.manifestation != null ? ff.manifestation.size() : 0);
        System.out.printf("   Previous xid:    %s\n",
            ff.prev != null ? XidString.serialize(ff.prev.item_xid) : "<no previous>");
        System.out.printf("   Next xid:        %s\n",
            ff.next.size() != 0 ? XidString.serialize(ff.next.get(0).item_xid) : "<no next>");
        System.out.printf("   Commit xid:      %s\n",
            XidString.serialize(ff.parent_commit.item_xid));
        System.out.printf("   Commit date:     %s\n", ff.parent_commit.date);
        System.out.printf("   Commit author:   %s\n", ff.parent_commit.author);
        System.out.printf("   Earliest rev:    %s\n",
            earliest == ff ? "<this>" : XidString.serialize(earliest.item_xid));
        System.out.printf("   Latest rev:      %s\n",
            latest == ff ? "<this>" : XidString.serialize(latest.item_xid));
        
    } // display_file_info();
    
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
            do {
                // Remove the node
                nodes.remove(node);
                if (len > 0) {
                    sb.append(" > ");
                }
                
                if (node.prev != null) {
                    String prev_id = node.prev.payload_xid.id;
                    String cur_id = node.payload_xid.id;
                    if (prev_id.equals(cur_id)) {
                        sb.append(String.format("r%d", node.payload_xid.rev));
                    } else {
                        sb.append(String.format("%s", 
                            XidString.serialize(node.payload_xid)));
                    }
                } else {
                    sb.append(String.format("%s", 
                        XidString.serialize(node.payload_xid)));
                }
                // Length of the lifeline increases
                len++;
                
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
    // THIS IS THE MAIN LOGIC FOR UPDATING REPOSITORY
    //=========================================================================
    
    /**
     * @param r [in/out] Repository
     * @param node [in] the node whose contents are to be put into repository
     * @param man [out] the manifestation data for this particular instance
     */
    public static void populate(
        Element node,
        Map<Element, List<Stack<Xid>>> manifestations_map
    ) {
        
        // Depth first
        for (Object obj : node.getContent()) {
            if ((obj instanceof Element) == false) {
                // Not an Element object. Skip
                continue;
            } // if: not an element
            
            // Depth-first recursion
            Element child = (Element) obj;
            
            populate(child, manifestations_map);
            // Debug the manifestations_map

            // Reassign the key of the manifestations mapped to the null key
            List<Stack<Xid>> nulstack = manifestations_map.remove(null);
            if (nulstack != null) {
                manifestations_map.put(child, nulstack);
            }
            
        } // for: each child object
        
        // After all children are processed, process the node itself.
        
        // If the element does not have @id or @xid, then this element
        // is not subject to be populated as an identified individual
        // to the repository and can be ignored.
        // TODO: These operations should belong to the class XidIdentification.
        
        String idstring = node.getAttributeValue("id");
        String xidstring = node.getAttributeValue("xid");
        
        if ((idstring == null) && (xidstring == null)) {
            // No xid, not even an unrevisioned pre-xid.
            // The element is not studied further.
            return;
        } // if: no xid

        
        // Determine whether it is a xid or an unrevisioned pre-xid
        if (idstring != null) {
            String revstring = node.getAttributeValue("rev");
            if (revstring == null) {
                // It is a new element and this is a pre-xid
                revstring = "#";
                node.setAttribute("rev", revstring);
            } else {
                // The element's xid contains both id and rev.
            } // if-else
        } else {
            // The attribute @id is null -> @xid must be non-null then.
        } // if-else
        
        // This finished the pre-processing part of the XML element.
        // The-preprocessing could be done PRIOR to populating.
        
        // The element should now have a valid xid, either as (@id, @rev) pair
        // or as @xid. However, it is still possible, that there exists
        // all three. Also, it is possible for the xid's id or rev part
        // to contain invalid values. The following call will sort it out.
        Xid xid = XidIdentification.get_xid(node);

        // This flag is used to determine later whether the element
        // is allowed to have a xid which is not known to the system earlier.
        boolean allow_new = false;
        
        //System.out.printf("Process node: %s\n", XidString.serialize(xid));
        
        // Detect if the xid is an unrevisioned pre-xid. If that's the case,
        // then assign the current revision number to both the xid
        // and the XML element (which will be written back to disk later).
        if (xid.rev == Xid.INVALID_REV) {
            
            // Update the rev to the current revision
            new_xid_revision(xid);
            
            // Propagate the update to the element itself too.
            XidIdentification.set_xid(node, xid);
            
            // Because this is meant to be a new element, set the flag
            // allowing the element to unexist in the current repository
            allow_new = true;
        } // if

        // Normalize the content element. Regardless whether the element
        // is going to be added or not to the repository, it needs to be
        // in the normalized form in any case.
        Element normalnode = null;
        
        // Normalization table for the inclusion-by-xid elements.
        List<Normalization.RefXidRecord> table = null;
        
        // This is required for translating cloned copy references
        // to original object references.
        Map<Element, Element> map = new HashMap<Element, Element>();
        
        // Normalize the current XML element. This creates an unparented
        // copy of the XML element. Without the map it would be impossible
        // to make out the connections between original XML elements
        // and their shortened ref_xid replacements.
        normalnode = Normalization.normalize(node, map);
        
        // Normalize the inclusion-by-xid elements too. During the operation
        // a so called normalization table is created. It can be used
        // to normalize and, more importantly, to denormalize 
        // the inclusion-by-xid elements back to their original form. 
        // Also, the normalization table will contain information about
        // whether the file's manifestation contained some unexpanded
        // inclusion-by-xid elements.
        table = Normalization.normalize_refs(normalnode);
        
        // Assign unique identities for each inclusion-by-xid element.
        // They will be identified with link_xid attribute values. 
        // The actual elements wont receive the link_xid attribute just yet.
        // At the moment their unique identities are stored only into
        // the normalization table.
        assign_link_xids(table);
        
        // The processing is now quite done.
        
        // Next, it is studied whether the xid this element has is already
        // known to the system or not. The previous instance may either be
        // in the repository or in the current commit set.
        Fida.Node item = get_fida_node(xid);
        
        // If the xid exists, then the element should be contentually 
        // equivalent to it. If the element is not the same, then it must be 
        // considered as a new revision of it.
        
        if (item != null) {
            // Yes, there is an XML element already in the system with 
            // the same xid. Contentual equivalence of the current instance
            // and the known instance must be calculated. To do that,
            // the inclusion-by-xid elements in both instances must be
            // normalized ("anonymized") by removing their unique link_xids.
            
            // Calcualte the normalization table for the older instance
            List<Normalization.RefXidRecord> oldtable = null;
            oldtable = Normalization.build_normalization_table(item.payload_element);
            
            // See if the current and the olde instance are contentually
            // equivalent.
            
            if (nodes_equal(normalnode, item.payload_element, oldtable)) {
                // The nodes ARE contentually equal. The current instance
                // has been already stored either into the repository
                // or into the current commit set. Because it is already
                // stored, we won't store it again.
                
                // Because it won't be stored, the manifestation information
                // calculated from the current instance must be transformed
                // into terms of the already recorded instance. Specifically,
                // it means translating the unique link_xid values just
                // assigned to the ones in the already recorded instance.
                
                // Since the instances are contentually equivalent,
                // their normalization tables SHOULD be in 1) identical
                // order and 2) the ref_xid values should be identical
                // in the identical order.
                
                calculate_manifestation(
                    table,
                    oldtable,
                    map,
                    manifestations_map
                );
                
                // End processing here
                return;
            } // if: contentually equal
            
            // If this line was reached, the instances were not contentually
            // equal. The newer instance must be considered as a new revision
            // of the older instance. 
            
            // The instance is given a new revision number.
            //=========================================================
            new_xid_revision(xid); 
            
            // It may be, that the element already had the revision value
            // which the new_xid_revision() function assigned. In that case,
            // the xid was unchanged.
            
            // In any case, the possibly new xid values must be propagated
            // also to the original XML element and to the normalized copy,
            // becuase those XML elements are written back to the disk later.
            XidIdentification.set_xid(node, xid);
            XidIdentification.set_xid(normalnode, xid);
            
            // Because the revision changed and the XML element possibly
            // got a new identity the check for already known instance
            // must be repeated.
            
            //=========================================================
            // The revision was changed -> checking has to be repeated.
            //=========================================================
            
            Fida.Node newitem = null;
            newitem = get_fida_node(xid);
            
            if (newitem != null) {
                // The xid is known to the system: either in the repository
                // records or in the current commti set.
                
                // Build normalization table for the inclusion-by-xid elements.
                oldtable = Normalization.build_normalization_table(newitem.payload_element);
                
                // Determine the contentual equivalence of the current
                // and oler instance of this xid.
                if (nodes_equal(normalnode, newitem.payload_element, oldtable)) {
                    // The current and the older instance of this xid
                    // are contentually equal. The current instance is already
                    // then recorded, and does not need to be recorded twice.
                    // Just like earlier, the manifestation information
                    // created for the normalized copy of the current instance
                    // must be translated into terms of the older instance.
                    calculate_manifestation(
                        table,
                        oldtable,
                        map,
                        manifestations_map
                    );
                    
                    // End processing here
                    return;
                } // if: contentually equal
                
                // The instances were not contentually equal.
                // They have the same id and the same new revision,
                // but their contents differ. Their modifications have
                // been inconsistent, and the user must resolve the issue.
                throw new RuntimeException(String.format(
                    "The updated element %s is inconsistent with an already recorded element having the same xid=%s",
                    XPathIdentification.get_xpath(node),
                    XidString.serialize(xid)
                ));
            } else {
                // There is no earlier record in the repository or in
                // the current commit set of the updated xid. This suggests 
                // the modification created a valid new revision.
            } // if-else
            
            // Next it is determined whether the older instance of
            // the XML element was the latest revision up-to-date.
            // If the modified XML element was an instance of some
            // "already archived" XML element which already had a newer
            // revision, the modification is rejected. The new revision
            // must be a successor of the currently latest revision.
            if (item.next.size() != 0) {
                // TODO:
                // If branching is allowed, then this code needs to be
                // modified; probably the rev shuold be renamed into some
                // branch number which don't reveal any ordering.
                throw new RuntimeException(String.format(
                    "The modified element %s constitutes a branch of xid=%s",
                    XPathIdentification.get_xpath(node),
                    XidString.serialize(item.payload_xid)
                )); // throw new ..
            } // if
            
            // Make sure that that there isn't an XML element in
            // the current tree such that it has the same id, but 
            // does not belong to the same lifeline as the latest instance
            // of the current XML element.
            
            Fida.Node active_node = get_active_node(xid.id);
            if ((active_node != null) && (active_node != item)) {
                // There is an active lifeline in the tree, and
                // the lifeline is different from this elements lifeline.
                // It means that @id taken as a lifeline designator,
                // is in use currently, and cannot be taken for a different
                // lifeline now, but maybe somewhere in the future.
                
                /*
                System.out.printf("This previous node: %s = %s\n",
                    XidString.serialize(item.item_xid),
                    XidString.serialize(item.payload_xid));
                System.out.printf("Active node: %s = %s\n", 
                    XidString.serialize(active_node.item_xid),
                    XidString.serialize(active_node.payload_xid));
                */
                throw new RuntimeException(String.format(
                    "The element %s with xid=%s cannot be revisioned, because it would hide a newer and active xid=%s (%s)",
                    XPathIdentification.get_xpath(node), 
                    XidString.serialize(item.payload_xid),
                    XidString.serialize(active_node.payload_xid),
                    XidString.serialize(active_node.item_xid)
                )); // throw
            }  // if: there is an active lifeline in the tree different from this
            
        } else {
            // There was no previous XML element with the same xid.
            // Either the XML element contained an unknown xid or it was
            // intentionally assigned an unexisting xid. See which one is it.
            if (allow_new == false) {
                // Unexisting xid wasn't allowed. See if there is
                // an overriding policy in effect..
                if (is_unknowns_allowed()) {
                    // It can be added. This may abruptly jump the repository's
                    // revision number.
                    update_repository_revision(xid.rev);
                } else {
                    // No, the overriding policy is not in effect.
                    // This is an error then.
                    throw new RuntimeException(String.format(
                        "The element %s has an unknown xid=%s",
                        XPathIdentification.get_xpath(node), 
                        XidString.serialize(xid)));
                } // if-else: unknowns allowed?
            } // if: new allowed?
            
            // If the unexisting xid can be added. It still needs to
            // be check that the lifeline designator @id is not currently
            // in active use for someone elses lifeline.
            Fida.Node active_node = get_active_node(xid.id);
            
            if (active_node != null) {
                
                if (active_node.payload_xid.rev > xid.rev) {
                    // Newer ones are not hidden. This revision will
                    // immediately constitute "an old" and "sealed" revision.
                    // TODO:
                    // It could be added. Make it into an option
                    // whether to allow it.
                    throw new RuntimeException(String.format(
                        "The element %s with xid=%s cannot be added, because it would be misunderstood as an older revision of the newer, and still active xid=%s (%s)",
                        XPathIdentification.get_xpath(node), 
                        XidString.serialize(xid), 
                        XidString.serialize(active_node.payload_xid),
                        XidString.serialize(active_node.item_xid)
                    )); // throw
                } else {
                    throw new RuntimeException(String.format(
                        "The element %s with xid=%s cannot be added, because it would hide an older, but still active xid=%s (%s)",
                        XPathIdentification.get_xpath(node), 
                        XidString.serialize(xid), 
                        XidString.serialize(active_node.payload_xid),
                        XidString.serialize(active_node.item_xid)
                    )); // throw
                } // if-else
                
            } else {
                // The lifeline designator @id is currently not reserved
                // for any lifeline.
            } // if-else: there is an active node, which could be hidden
        } // if-else: the xid was already known to the system?
        
        // If this line is reached, then the current XML element is going
        // to be added to the current commit set as a new XML element.
        
        // Update the manifestation for this
        calculate_manifestation(
            table,
            null, // no older element in the repo
            map,
            manifestations_map
        );
        
        // Apply the denormalization table to the references of 
        // the normalized element.
        Normalization.denormalize_refs(table);
        
        // This should go to the current commit set!
        add_fida_node(normalnode, item);
        
    } // populate()
    
    public static Map<Xid, Xid> calculate_xid_map(
        List<Normalization.RefXidRecord> newtable,
        List<Normalization.RefXidRecord> oldtable
    ) {
        
        if (oldtable == null) {
            return null;
        }
        
        // Create translation map from new table to old table.
        
        // Return variable
        Map<Xid, Xid> xidmap = new HashMap<Xid, Xid>();
        
        // Iterators over both lists
        ListIterator<Normalization.RefXidRecord> iter_new;
        ListIterator<Normalization.RefXidRecord> iter_old;
        
        // Initialize the iterators
        iter_new = newtable.listIterator();
        iter_old = oldtable.listIterator();
        
        // Repeat while both have next
        while (iter_new.hasNext() && iter_old.hasNext()) {
            // Pick the next item from both lists
            Normalization.RefXidRecord new_rec = iter_new.next();
            Normalization.RefXidRecord old_rec = iter_old.next();
            
            // Verify that both ref_xids reference to the same target
            String new_refxid = new_rec.element.getAttributeValue("ref_xid");
            String old_refxid = old_rec.element.getAttributeValue("ref_xid");
            
            if (old_refxid.equals(new_refxid) == false) {
                // The ref_xid targets differ. This is an error
                throw new RuntimeException(String.format(
                    "ref_xid values differ: \"%s\" vs \"%s\"",
                    new_refxid, old_refxid));
            } // if
            
            // Insert the assocation to the map
            xidmap.put(new_rec.xid, old_rec.xid);
        } // while
        
        // Verify that both lists were completetly consumed
        if (iter_new.hasNext() || iter_old.hasNext()) {
            throw new RuntimeException(String.format(
                "RefXidRecord tables have mismatching number of entries"));
        } // if: mismatch
        
        // Return the map
        return xidmap;
    } // calculate_xid_map()
    
    
    protected static void calculate_manifestation(
        List<Normalization.RefXidRecord> newtable,
        List<Normalization.RefXidRecord> oldtable,
        Map<Element, Element> map,
        Map<Element, List<Stack<Xid>>> manifestations_map
    ) {
        
        // Calculate the xid mapping
        Map<Xid, Xid> xidmap = calculate_xid_map(newtable, oldtable);
        
        
        // Append noexpands from the current denormalization table to it also
        
        List<Stack<Xid>> nullist = manifestations_map.get(null);
        if (nullist == null) {
            nullist = new LinkedList<Stack<Xid>>();
        } else {
            System.out.printf("Null stack existing... Don\'t know why??\n");
        }
        
        // Populate the nullist with noexpand entried from the current
        // denormalization table
        for (Normalization.RefXidRecord record : newtable) {
            
            // This is the link_xid of the inclusion-by-xid element itself,
            // not the target.
            Xid targetxid = record.xid;
            
            // If the cloned copy is not going to be added to the repository,
            // then the link_xid of the cloned copy must be translated
            // into terms of the stored item.
            if (xidmap != null) {
                // Translate newtable's xid to oldtable's xid
                targetxid = xidmap.get(targetxid);
            }
            
            if (record.expand == true) {
                // Propagate all manifestations_map entries with
                // key matching to the original child of this link
                // to correspond null
                
                // Pick the inclusion-by-xid Element object 
                // in the normalized copy
                Element replacement_elem = record.element;
            
                // Translate the inclusion-by-xid Element reference
                // to Element in the original content element.
                Element orig_elem = map.get(replacement_elem);
                
                List<Stack<Xid>> stacklist = manifestations_map.remove(orig_elem);
                if (stacklist != null) {
                    for (Stack<Xid> stack : stacklist) {
                        stack.push(targetxid);
                        nullist.add(stack);
                    } // for
                }
                
            } else {
                Stack<Xid> newstack = new Stack<Xid>();
                newstack.push(targetxid);
                nullist.add(newstack);
            } // if-else: expand?
        } // for
        
        if (nullist.size() > 0) {
            manifestations_map.put(null, nullist);
        }
        
    } // calculate_manifestation
    
    protected static Element rget(Map<Element, Element> map, Element value) {
        for (Map.Entry<Element, Element> entry : map.entrySet()) {
            if (entry.getValue() == value) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    
    protected static void dump_mmap(Map<Element, List<Stack<Xid>>> mmap) {
        System.out.printf("Manifestations map <begin>\n");
        for (Map.Entry<Element, List<Stack<Xid>>> entry :
            mmap.entrySet())
        {
            Element key = entry.getKey();
            if (key != null) {
                System.out.printf("Element : %s\n",
                    XPathIdentification.get_xpath(key));
            } else {
                System.out.printf("Element : null\n");
            }
            
            for (Stack<Xid> stack : entry.getValue()) {
                System.out.printf("     %s\n", unexpand2string(stack));
            }
        } // for
        System.out.printf("Manifestations map <end>\n");
    } // dump_mmap()
    
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
     * r is required for the unique value generation
     */
    protected static void assign_link_xids(
        List<Normalization.RefXidRecord> table
    ) {
        // Assign unique link_xid to each inclusion-by-xid element.
        for (Normalization.RefXidRecord record : table) {
            Xid link_xid = null;
            
            link_xid = generate_xid("link");
            
            record.xid = link_xid;
            // TODO: This operation should be in RefXidRecord class.
            //record.element.setAttribute("link_xid", XidString.serialize(linkxid));
            
        } // for
    } // assign_link_xids()
    
    
    public static boolean nodes_equal(
        Element newnode,
        Element oldnode,
        List<Normalization.RefXidRecord> oldtable
    ) {
        // Return value
        boolean rval;
        
        // Normalize inclusion-by-xid elements
        Normalization.normalize_refs(oldtable);
        
        // Test contentual equivalence
        try {
            // May throw an IOException
            rval = ContentualEq.equal(newnode, oldnode);
        } catch(Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        
        // Denormalize inclusion-by-xid elements
        Normalization.denormalize_refs(oldtable);
        
        return rval;
    } // nodes_equal()
    
    
    
    public static boolean nodes_equal(
        Element newnode, 
        Element oldnode
    ) {
        // Return value
        boolean rval;
        
        // The new node is assumed to be already normalized
        List<Normalization.RefXidRecord> oldtable = null;
        
        // Normalize inclusion-by-xid elements
        oldtable = Normalization.normalize_refs(oldnode);
        
        // Test contentual equivalence
        try {
            // May throw an IOException
            rval = ContentualEq.equal(newnode, oldnode);
        } catch(Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        
        // Denormalize inclusion-by-xid elements
        Normalization.denormalize_refs(oldtable);
        
        return rval;
    } // nodes_equal
    
    
    /**
     * Returns the node with the greatest rev and the given id.
     * If no such id is known, returns {@null}.
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
    
    /**
     * Returns {@code Fida.Node} which is reachable from the current tree
     * and which has a payload element whose xid has a matching id.
     * 
     * @param id the {@code xid.id} to look for
     * @return the matching {@code Fida.Node} object, or {@code null}
     * if no such node was reachable.
     */
    public static Fida.Node get_active_node(String id) {
        Fida.Node rval = null;
        
        for (Fida.File ff: g_fida.state.tree) {
            Element e = resolve_payload_xid(ff.root_xid);
            rval = get_active_node(e, id);
            if (rval != null) {
                break;
            } // if
        } // for: each file
        
        return rval;
    } // is_active_id()
    
    /**
     * Returns first active node having the payload_xid's id matching
     * to the specified id.
     *
     * @param elem the payload element from which search is recursively made
     * @param search_id the id to search for
     * @return The {@code Fida.Node} object of the active node,
     * or {@code null} if no active node with payload having the given
     * id was found.
     *
     */
    private static Fida.Node get_active_node(
        Element elem,
        String search_id
    ) {
        Fida.Node rval = null;
        
        
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            
            Element child = null;
            
            Xid ref_xid = get_ref_xid(c);
            
            if (ref_xid != null) {
                child = resolve_payload_xid(ref_xid);
            } else {
                child = c;
            } // if-else
            
            // Recurse
            Fida.Node match = null;
            match = get_active_node(child, search_id);
            
            // If a match was found, exit immediately
            if (match != null) {
                if (rval == null) {
                    rval = match;
                } else if (rval.payload_xid.rev < match.payload_xid.rev) {
                    // Update
                    rval = match;
                } // if
            } // if
        } // for
        
        // See if the current payload element has a matching id.
        Xid xid = XidIdentification.get_xid(elem);
        if ((xid != null) && xid.id.equals(search_id)) {
            // Matching ids. Check further...
            Fida.Node match = get_fida_node(xid);
            if (match.next.size() == 0) {
                // In the current tree there exists an XML element
                // with the same id and which does not have a successor.
                // Adding the searched id would cause the tree to have
                // two different xids with the same id and both constituting
                // an active "lifeline".
                // See if this is "more active" (= later) than the previous
                // match
                if ((rval == null) 
                    || (rval.payload_xid.rev < match.payload_xid.rev))
                {
                    rval = match;
                } // if update
            } // if: does not have a next.
        } // if
        
        return rval;
    } // get_active_node()
    
} // XidClient





