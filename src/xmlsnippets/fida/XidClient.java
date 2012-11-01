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
import java.util.LinkedList;
import java.util.Map;
import java.util.LinkedHashMap;
// jdom imports
import org.jdom.Element;
import org.jdom.Document;
// xmlsnippets imports
import xmlsnippets.core.Xid;
import xmlsnippets.core.XidIdentification;
import xmlsnippets.core.ContentualEq;
import xmlsnippets.core.Normalization;
import xmlsnippets.util.XMLFileHelper;
import xmlsnippets.util.XPathIdentification;

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
     * Default repository file name.
     */
    public static final String DEFAULT_REPO_FILENAME = "xidstore.xml";
    
    // MEMBER VARIABLES
    //==================
    
    // CLASS METHODS
    //===============
    
    // HELPER CLASS TO CAPTURE COMMAND-LINE
    public static class CmdArgs {
        public boolean debug_flag = false;
        public String repo_filename = DEFAULT_REPO_FILENAME;
        public List<String> filenames = new LinkedList<String>();
        public String command_arg = null;
    } // class CmdArgs
    
    public static class Repository {
        
        public File file = null;
        public Document doc = null;
        public Xid xid = null;
        
        //public Map<String, Element> elements;
        // List of all known nodes
        public Map<Xid, FidaNode> oldnodes = null;
        public Map<Xid, FidaNode> newnodes = null;
        public List<FidaCommit> commits = null;
        
        /**
         * @param xid the payload's xid
         */
        public FidaNode get_node(Xid payload_xid) {
            FidaNode node = oldnodes.get(payload_xid);
            if (node == null) {
                node = newnodes.get(payload_xid);
            }
            return node;
        } // get_node()
        
        public void add_node(FidaNode node) {
            Xid xid = node.payload_xid;
            if (get_node(xid) != null) {
                throw new RuntimeException();
            }
            newnodes.put(xid, node);
        } // add_node()
    } // class Repository
    
    /**
     * Reprents a tracked item; an XML element with its administrative data.
     */
    public static class FidaNode {
        
        // The acutal element for the item itself
        public Element item_element = null;
        
        // Internal xid for the item; the lifelene id
        public Xid item_xid = null;
        
        // Previous item, or null if no previous.
        public FidaNode prev = null;
        
        // Next item, or null if the newest
        public FidaNode next = null;
        
        // the actual element for the payload
        public Element payload_element = null;
        
        // Xid of the actual element
        public Xid payload_xid = null;
        
        // In which commit the element was introduced
        public FidaCommit commit = null;
    } // class FidaItem
    
    public static class FidaFile {
        // The actual element for the item itself
        public Element item_element = null;
        
        // The lifelien id of this file object
        public Xid item_xid = null;
        
        //public Object checksum;
        
        public String path = null;
        
        // Root node's xid
        public Xid root_xid = null;
        
        // Specifications for the individual manifestation
        // of the root node.
        // The noexpand must be a path specification!!
        // this must be so because otherwise the file could
        // contain the same inclusion-by-xid element with
        // one instance being expanded and the other being unexpanded.
        // public List<Xid> noexpands;
        // The same remark applies to the content instance variant 
        // specifications.
        
        // In which commit the file spec was introduced
        public FidaCommit commit = null;
        
    } // FidaFile
    
    public static class FidaCommit {
        // The actual element information
        public Element item_element = null;
        public Xid item_xid = null;
        
        public String date = null;
        
        public String author = null;
        
        public List<FidaFile> layout = null;
        
        public List<FidaNode> nodes = null;
    } // FidaCommit
    
    public static class FidaManifestation {
        public Xid source_link = null;
        
        // Denormalization table for a particular manifestation
        // of the source_link inclusion-by-reference node
        public List<Normalization.RefXidRecord> table = null;
        
    } // FidaManifestation
    
    // MAIN
    //======
    
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
                create_repository(cmd_args.repo_filename);
                System.out.printf("Created: %s\n", cmd_args.repo_filename);
                System.exit(EXIT_SUCCESS);
            }
            
            // At this stage, read the repository file
            Repository r = null;
            r = read_repository(cmd_args.repo_filename);
            
            // Build catalouges and indices and link maps.
            // TODO
            parse_repository(r);
            
            if (command.equals("add")) {
                if (cmd_args.filenames.size() == 0) {
                    throw new RuntimeException("No files to add");
                }
                add_files(r, cmd_args.filenames);
            }
            else {
                throw new RuntimeException(String.format(
                    "Error: unknown command \"%s\"", command));
            }
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
    
    
    public static void create_repository(String filename) {
        File file = new File(filename);
        if (file.isFile() && file.exists()) {
            throw new RuntimeException(String.format(
                "Cannot create repository. File already exists: %s", file.getPath()));
        } // if
        
        // Otherwise good to go
        Document doc = new Document();
        Element root = new Element("XidStore");
        root.setAttribute("id", "#repo!unnamed");
        root.setAttribute("rev", "0");
        doc.setRootElement(root);
        
        // This may fail
        try {
            XMLFileHelper.serialize_document_formatted(doc, file);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        } // try-catch
        
    } // create_repository()
    
    public static Repository read_repository(String filename) {
        Repository rval = new Repository();
        File file = new File(filename);
        
        if ((file.isFile() == false) || (file.exists() == false)) {
            throw new RuntimeException(String.format(
                "Not a file or does not exist: %s", file.getPath()));
        } // if
        
        // Otherwise attempt to read
        try {
            rval.doc = XMLFileHelper.deserialize_document(file);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        } // try-catch
        
        rval.file = file;
        
        return rval;
    } // read_repository()

    public static void add_files(Repository r, List<String> filenames) {
        
        for (String fname : filenames) {
            // TODO:
            // Verify that the file is not already being tracked.
            
            File file = new File(fname);
            if ((file.isFile() == false) || (file.exists() == false)) {
                // Abort
                throw new RuntimeException(String.format(
                    "Not a file or does not exist: %s", file.getPath()));
            } // if
            
            // Attempt to read the XML document
            Document doc = null;
            try {
                doc = XMLFileHelper.deserialize_document(file);
            } catch(Exception ex) {
                // Bubble up the message
                throw new RuntimeException(ex.getMessage(), ex);
            } // try-catch
            
            // Read succesful
            // TODO: Preprocess the XML document. That is, expand
            // relative @ids, missing rev data from new xids, and so on.
            
            // Then:
            // See if the file is valid as an individual file
            // See if the file is valid with respect to the repository
            // Manifestation of the file
            FidaManifestation manifestation = new FidaManifestation();
            
            populate(r, doc.getRootElement(), manifestation);
            
        } // for: each file name
        
        // If all is in order, this point is reached and the commit
        // can be executed.
        
    } // add_files()

    public static void parse_repository(Repository r) {
        // TODO
    } // parse_repository()
    
    
    public static void populate(
        Repository r,
        Element node,
        FidaManifestation man
    ) {
        
        // Depth first
        for (Object obj : node.getContent()) {
            if ((obj instanceof Element) == false) {
                // Not an Element object. Skip
                continue;
            } // if: not an element
            
            // Depth-first recursion
            Element child = (Element) obj;
            populate(r, child, man);
        } // for: each child object
        
        // After all children are processed, process the node itself.
        
        // If the element does not have @id or @xid, then this element
        // is not subject to be populated as an identified individual
        // to the repository and can be ignored.
        // TODO: These operations should belong to the class XidIdentification.
        String idstring = node.getAttributeValue("id");
        String xidstring = node.getAttributeValue("xid");
        
        if ((idstring == null) && (xidstring == null)) {
            // No xid, not even a pre-xid.
            // This element is not a concern.
            return;
        } // if: no xid
        
        // Determine whether it is a xid or a pre-xid
        if (idstring != null) {
            String revstring = node.getAttributeValue("rev");
            if (revstring == null) {
                // It is a new element and this is a pre-xid
                revstring = "#";
                node.setAttribute("rev", revstring);
            } else {
                // Element with a true xid. This is either an existing
                // element or a non-existing bogus.
                // If it is an existing element, then it can be either
                // a) unmodified; or b) modified, in which case it is
                // further inspection is needed to determine whether 
                // the modification was legal or not. If it was legal,
                // then the element is considered a new revision.
            }
        } else {
            // id is null, so xid must be non-null
        } // if-else
        
        // Pre-processing done. Lets get down to the real thing.
        
        boolean allow_new = false;
        // At this stage the element should have a valid values in
        // xid attributes, but nothing guarantees that there is a correct
        // set of xid attributes. It will be figured out shortly, by calling
        // the function
        Xid xid = XidIdentification.get_xid(node);
        
        // See if the rev value indicates a new element
        if (xid.rev == Xid.INVALID_REV) {
            // Update the rev to the current revision
            xid.rev = r.xid.rev;
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
        // Denormalization table for the inclusion-by-xid elements.
        List<Normalization.RefXidRecord> table = null;
        
        // Normalize the node; this creates an unparented copy of the element
        normalnode = Normalization.normalize(node);
        
        // Normalize the references too, and build the denormalization 
        // table while at it.
        table = Normalization.normalize_refs(normalnode);
            
        
        // Next thing to do is to determine whether this xid already exists 
        // in the repository. If it exists, see if it is equal to it.
        FidaNode item = null;
        
        item = r.get_node(xid);
        if (item != null) {
            // See if it is equal to it.
            if (nodes_equal(normalnode, item.payload_element)) {
                // The nodes are contentually equal, which means that
                // the current instance is already stored into
                // the revision control repository or is already included
                // in the commit node set.
                // This element is therefore not a concern anymore.
                return;
            }
            
            // If this point is reached, the elmeent is a modification
            // of an existing element. Need to determine is the modification
            // allowed.
            
            // Make the current instance into a newest revision
            // if it it isn't already.
            xid.rev = r.xid.rev;
            // Now the revision change must be remebered to propagated not only
            // to the original element but ALSO to the normalized element
            XidIdentification.set_xid(node, xid);
            XidIdentification.set_xid(normalnode, xid);
            
            // Becase the revision was changed, the previous check has
            // to be repeated.
            
            FidaNode newitem = null;
            newitem = r.get_node(xid);
            
            if (newitem != null) {
                // There is already a new revision of this element.
                // Unless the already included node is contentually equal
                // to the current instance, the user must solve the conflicting
                // revisions by hand.
                if (nodes_equal(normalnode, newitem.payload_element)) {
                    // They are equal; the revision changes are consistent,
                    // and the new revision is already included in 
                    // the repository or in the commit set.
                    // This element is no longer a concern
                    return;
                }
                // Otherwise, the revision changes are inconsistent.
                // The user must resolve conflicts by hand.
                throw new RuntimeException(String.format(
                    "The updated element %s has already an inconsistent revision in the repository",
                    XPathIdentification.get_xpath(node)));
            } else {
                // the current instance consitutes a new revision of an older
                // node.
            } // if-else
            
            // Next thing to do is to check whether a new revision is allowed
            // of the older node. It is not allowed, unless the new revision
            // is based on the latest revision. That is, otherwise the old
            // node would already have a successor, and adding this element
            // would cause a branch to be created.
            
            if (item.next != null) {
                throw new RuntimeException(String.format(
                    "The modified element %s constitutes a branch; branches are unallowed",
                    XPathIdentification.get_xpath(node)));
            } // if
            
        } // if: had an existing node
        
        // the other thing to check is that the insertion of this
        // new (id, rev) won't cut some one elses lifeline. That is,
        // the id must be inactive in the latest snapshot.
        
        // TODO.
        // It could be solved by an answer to the question:
        // Is the element with the same id and greatest rev number
        // traceable back to the latest snapshot?
        // Another way to solve it would be to just simply take
        // the latest snapshot's layout/dirtree and calculate
        // a spanning tree of the nodes.
        // The spanning tree is probably nicer way to do it.
        
        // For now, it is simply assumed that there is no killing of 
        // anyones lifelines and the adding proceed.
        
        /// either the node was completetly new
        //r.add_node();
        
        
        
        
        
    } // populate()
    
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
    
} // XidClient





