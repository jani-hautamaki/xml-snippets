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
// jdom imports
import org.jdom.Element;
import org.jdom.Document;
// xmlsnippets imports
import xmlsnippets.core.Xid;
import xmlsnippets.core.XidString;
import xmlsnippets.core.XidIdentification;
import xmlsnippets.core.ContentualEq;
import xmlsnippets.core.Normalization;
import xmlsnippets.util.XMLFileHelper;
import xmlsnippets.util.XPathIdentification;
import xmlsnippets.util.XPathDebugger;

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
        public int nextid = 1001;
        
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
        
        public void add_node(
            //Xid xid,
            Element normalized_element, 
            FidaNode previous
        ) {
            
            Xid xid = XidIdentification.get_xid(normalized_element);
            if (get_node(xid) != null) {
                throw new RuntimeException();
            }
            
            FidaNode node = new FidaNode();
            
            node.item_element = null; // No carrier element yet
            
            node.item_xid = new Xid(
                String.format("#node!%d", this.unique()), 
                1
            ); // new Xid(); rev will be the repositorys revision?
            
            // If any
            node.prev = previous;
            node.next = null; // can't be yet
        
            System.out.printf("Added element should not have a parent: %s\n", normalized_element.getParent() == null ? "null" : "has parent :(");
            node.payload_element = normalized_element;
            node.payload_xid = xid;
            node.commit = null; // could be, but not now.
            
            newnodes.put(xid, node);
            System.out.printf("Internal xid=%s and payload xid=%s added\n", 
                XidString.serialize(node.item_xid),
                XidString.serialize(node.payload_xid)
            ); // printf
        } // add_node()
        
        // Poor man's uuid generator
        public int unique() {
            return ++nextid;
        }
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
        public List<Stack<Xid>> unexpand_list = null;
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
            parse_repository(r);
            
            if (command.equals("add")) {
                if (cmd_args.filenames.size() == 0) {
                    throw new RuntimeException("No files to add");
                }
                add_files(r, cmd_args.filenames);
            }
            else if (command.equals("debug")) {
                // Execute debug mode
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
        root.setAttribute("nextid", "1001");
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
        Map<File, Document> added_files = new LinkedHashMap<File, Document>();
        Map<File, List<Stack<Xid>>> unexpands_map 
            = new LinkedHashMap<File, List<Stack<Xid>>>();
        
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
            Map<Element, List<Stack<Xid>>> manifestations_map
                = new LinkedHashMap<Element, List<Stack<Xid>>>();
            
            System.out.printf("Processing file %s\n", file.getPath());
            populate(r, doc.getRootElement(), manifestations_map);
            System.out.printf("\n\n");
            
            dump_mmap(manifestations_map);
            
            // Remove the following comment to see how the file looks
            // after the population procedure. The xid revs should have
            // been filled with proper values.
            //XPathDebugger.debug(doc);
            added_files.put(file, doc);
            List<Stack<Xid>> unexpand_list = manifestations_map.get(null);
            if (unexpand_list != null) {
                unexpands_map.put(file, unexpand_list);
            }
            
        } // for: each file name
        
        // TODO:
        // Check all ref_xid values. They should point to known xids
        // that are known to the system by now.
        
        // If all is in order, this point is reached and the commit
        // can be executed.
        
        // Combine all added nodes into a document
        /*
        Document newdoc = new Document();
        Element newroot = new Element("root");
        newdoc.setRootElement(newroot);
        for (FidaNode n : r.newnodes.values()) {
            newroot.addContent(n.payload_element);
        }
        XPathDebugger.debug(newdoc);
        */
        
        // Create a commit
        FidaCommit commit = new FidaCommit();
        
        commit.date = "1235";
        commit.author = "author";
        
        // Insert files to the current layout
        List<FidaFile> layout = new LinkedList<FidaFile>();
        for (Map.Entry<File, Document> entry : added_files.entrySet()) {
            File file = entry.getKey();
            Document doc = entry.getValue();
            FidaFile fidafile = new FidaFile();
            
            // TODO: normalize the path relative to the xidstore
            fidafile.path = file.getPath();
            
            // TODO: root must have a xid!!
            fidafile.root_xid = XidIdentification.get_xid(
                doc.getRootElement());
            
            fidafile.unexpand_list = unexpands_map.get(file);
            // TODO: calculate checksum
            //fidafile.checksum = null;
            fidafile.commit = commit;
            layout.add(fidafile);
        } // for: each added file
        commit.layout = layout;
        
        // Create the nodes set
        List<FidaNode> added_nodes = new LinkedList<FidaNode>();
        for (FidaNode node : r.newnodes.values()) {
            node.commit = commit;
            added_nodes.add(node);
        }
        commit.nodes = added_nodes;
        
        serialize_commit(r, commit);
    
        Element docroot = r.doc.getRootElement();
        docroot.addContent(commit.item_element);
        serialize_repository(r);
        
        try {
            for (Map.Entry<File, Document> entry : added_files.entrySet()) {
                File file = entry.getKey();
                Document doc = entry.getValue();
                XMLFileHelper.serialize_document_verbatim(doc, file);
            }
        } catch(Exception ex) {
        } // try-catch
        
    } // add_files()
    
    public static void serialize_repository(Repository r) {
        // Get document root
        Element docroot = r.doc.getRootElement();
        
        // Set nextid attribute value
        docroot.setAttribute("nextid", String.format("%d", r.nextid));
        XidIdentification.set_xid(docroot, r.xid);
        
        try {
            System.out.printf("Rewriting %s\n", r.file.getPath());
            XMLFileHelper.serialize_document_formatted(r.doc, r.file);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    } // serialize_repository
    
    public static void serialize_commit(Repository r, FidaCommit commit) {
        Element node = new Element("Commit");
        commit.item_element = node;
        
        commit.item_xid = new Xid(String.format("#commit!%d", r.unique()), 1);
        XidIdentification.set_xid(node, commit.item_xid);
        
        node.addContent(
            new Element("Date").setText("cur date")
        );
        node.addContent(
            new Element("Author").setText("cur author")
        );
        
        node.addContent(
            serialize_layout(r, commit.layout)
        );
        node.addContent(
            serialize_nodes(r, commit.nodes)
        );
    } // serialize_commit()
    
    public static Element serialize_layout(Repository r, List<FidaFile> layout) {
        Element node = new Element("Layout");
        
        for (FidaFile ff : layout) {
            serialize_fidafile(r, ff);
            node.addContent(ff.item_element);
        } // for
        
        return node;
    }
    
    public static void serialize_fidafile(Repository r, FidaFile ff) {
        Element node = new Element("File");
        ff.item_element = node;
        
        ff.item_xid = new Xid(String.format("#file!%d", r.unique()), 1);
        
        node.addContent(
            new Element("Path").setText(ff.path)
        );
        
        node.addContent(
            new Element("Root").setAttribute("node", 
                XidString.serialize(ff.root_xid))
        );
        
        node.addContent(
            new Element("sha1").setText("n/a")
        );
        
        List<Stack<Xid>> list = ff.unexpand_list;
        if (list != null) {
            Element child = new Element("Manifestation");
            node.addContent(child);
            for (Stack<Xid> stack : list) {
                Element e = new Element("Unexpand");
                child.addContent(e);
                e.setText(unexpand2string(stack));
            }
        } // if
        
    } // serialize_fidafile()
    
    public static Element serialize_nodes(Repository r, List<FidaNode> nodes) {
        Element rval = new Element("Nodes");
        for (FidaNode cnode : nodes) {
            
            Element child = new Element("Node");
            rval.addContent(child);
            
            cnode.item_element = child;
            
            XidIdentification.set_xid(child, cnode.item_xid);

            if (cnode.prev != null) {
                
                child.addContent(
                    new Element("Previous").setAttribute("node",
                        XidString.serialize(cnode.prev.item_xid))
                );
            }
            
            child.addContent(
                new Element("Payload")
                .addContent(cnode.payload_element)
            );
        } // for
        
        return rval;
    }

    protected static void error_unexpected(Element elem) {
        throw new RuntimeException(String.format(
            "%s: unexpected elemenet name",
            XPathIdentification.get_xpath(elem)));
    }
    
    public static void parse_repository(Repository r) {
        
        r.oldnodes = new HashMap<Xid, FidaNode>();
        r.newnodes = new LinkedHashMap<Xid, FidaNode>();
        r.commits = new LinkedList<FidaCommit>();
        
        Element root = r.doc.getRootElement();
        
        r.xid = XidIdentification.get_xid(root);
        
        // INCREASE THE REPOSITORY STATE
        r.xid.rev++;
        
        try {
            String s = root.getAttributeValue("nextid");
            r.nextid = Integer.parseInt(s);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
        
        
        for (Object obj : root.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            String name = c.getName();
            if (name.equals("Commit")) {
                parse_commit(r, c);
            }
            else {
                error_unexpected(c);
            }
        } // for
    } // parse_repository()
    
    public static void parse_commit(Repository r, Element elem) {
        FidaCommit commit = new FidaCommit();
        r.commits.add(commit);
        
        commit.item_element = elem;
        commit.item_xid = XidIdentification.get_xid(elem);
        
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            String name = c.getName();

            if (name.equals("Date")) {
                commit.date = c.getText();
            }
            else if (name.equals("Author")) {
                commit.author = c.getText();
            }
            else if (name.equals("Layout")) {
                parse_layout(r, c, commit);
            }
            else if (name.equals("Nodes")) {
                parse_nodes(r, c, commit);
            }
            else {
                error_unexpected(c);
            } // if-else
        } // for: child
        
    } // parse_commit()
    
    public static void parse_layout(
        Repository r, 
        Element elem,
        FidaCommit commit
    ) {
        List<FidaFile> layout = new LinkedList<FidaFile>();
        commit.layout = layout;
        
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            String name = c.getName();
            if (name.equals("File")) {
                parse_fidafile(r, c, layout);
            }
            else {
                error_unexpected(c);
            } // if-else
        } // for
    } // parse_layout()
    
    public static void parse_fidafile(
        Repository r,
        Element elem,
        List<FidaFile> layout
    ) {
        FidaFile ff = new FidaFile();
        layout.add(ff);
        
        ff.item_element = elem;
        ff.item_xid = XidIdentification.get_xid(elem);
        
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            String name = c.getName();
            if (name.equals("Path")) {
                ff.path = c.getText();
            }
            else if (name.equals("Root")) {
                String xidstr = c.getAttributeValue("node");
                ff.root_xid = XidString.deserialize(xidstr);
            }
            else if (name.equals("sha1")) {
                // Skip
            }
            else if (name.equals("Manifestation")) {
                parse_manifestation(r, c, ff);
            }
            else {
                error_unexpected(c);
            } // if-else
        }
    } // parse_fidafile
    
    public static void parse_manifestation(
        Repository r,
        Element elem,
        FidaFile ff
    ) {
        List<Stack<Xid>> list = new LinkedList<Stack<Xid>>();
        ff.unexpand_list = list;
        
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            String name = c.getName();
            if (name.equals("Unexpand")) {
                String s = c.getText();
                list.add(parse_unexpand(s));
            }
            else {
                error_unexpected(c);
            }
        }
    } // parse_manifestation()
    
    public static Stack<Xid> parse_unexpand(String s) {
        Stack<Xid> stack = new Stack<Xid>();
        
        String[] array = s.split("/");
        for (int i = array.length-1; i >= 0; i--) {
            String piece = array[i];
            if (piece.length() > 0) {
                stack.push(XidString.deserialize(piece));
            }
        }
        return stack;
    } // parse_unexpand()

    public static void parse_nodes(
        Repository r, 
        Element elem,
        FidaCommit commit
    ) {
        List<FidaNode> nodes = new LinkedList<FidaNode>();
        commit.nodes = nodes;
        
        
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            String name = c.getName();
            if (name.equals("Node")) {
                parse_fidanode(r, c, nodes);
            }
            else {
                error_unexpected(c);
            }
        }
    } // parse_layout()
    
    public static void parse_fidanode(
        Repository r,
        Element elem,
        List<FidaNode> nodes
    ) {
        FidaNode fidanode = new FidaNode();
        nodes.add(fidanode);
        fidanode.item_element = elem;
        fidanode.item_xid = XidIdentification.get_xid(elem);
        

        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element c = (Element) obj;
            String name = c.getName();
            
            if (name.equals("Previous")) {
                String xidstr = c.getAttributeValue("node");
                Xid prev_xid = XidString.deserialize(xidstr);
                
                fidanode.prev = r.get_node(prev_xid);
            }
            else if (name.equals("Payload")) {
                fidanode.payload_element = (Element) c.getChildren().get(0);
                fidanode.payload_xid = XidIdentification.get_xid(
                    fidanode.payload_element);
            }
            else {
                error_unexpected(c);
            }
        } // for
        
        // Add node to oldnodes
        r.oldnodes.put(fidanode.payload_xid, fidanode);
        
    } // parse_fidanode()
    
    /**
     * @param r [in/out] Repository
     * @param node [in] the node whose contents are to be put into repository
     * @param man [out] the manifestation data for this particular instance
     */
    public static void populate(
        Repository r,
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
            
            populate(r, child, manifestations_map);
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
        // the function. May throw:
        Xid xid = XidIdentification.get_xid(node);
        
        System.out.printf("Node: %s\n", XidString.serialize(xid));
        
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
        
        // This is required for "Element translation"
        Map<Element, Element> map = new HashMap<Element, Element>();
        
        // NORMALIZE THE NODE
        // This creates an unparented copy of the element.
        // Without an additional data structure the connections between 
        // original elements and their ref_xid replacement counterparts 
        // are lost.
        normalnode = Normalization.normalize(node, map);
        
        // Normalize the references too, and build the denormalization 
        // table while at it.
        table = Normalization.normalize_refs(normalnode);
        
        // Assign unique link_xid values to each inclusion-by-xid element
        // in the table. These are not reflected in the actual elements
        // just yet.
        assign_link_xids(r, table);

        // TODO:
        // Connect the linkmap data with the suppressed elements.
        
        // Next thing to do is to determine whether this xid already exists 
        // in the repository. If it exists, see if it is equal to it.
        FidaNode item = null;
        
        item = r.get_node(xid);
        if (item != null) {
            // See if it is equal to it.
            
            // Calculate normalization table
            List<Normalization.RefXidRecord> oldtable = null;
            
            // Normalize inclusion-by-xid elements
            oldtable = Normalization.build_normalization_table(item.payload_element);
            
            if (nodes_equal(normalnode, item.payload_element, oldtable)) {
                // The nodes are contentually equal, which means that
                // the current instance is already stored into
                // the revision control repository or is already included
                // in the commit node set.
                // This element is therefore not a concern anymore.
                
                // TODO: Even though the contentual equivalence is asserted,
                // it is possible that the manifestation has changed.
                // Therefore, the inclusion-by-xid manifestation should
                // still be calculated...
                
                // TODO: Since the nodes are contentually equal, their
                // normalization tables should be identical in terms
                // of size and the order of references. The values of
                // @expands and @link_xids may differ.
                calculate_manifestation(
                    table,
                    oldtable,
                    map,
                    manifestations_map
                );
                
                System.out.printf("Already recorded\n");
                return;
                
            } // if
            
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
                //List<Normalization.RefXidRecord> newtable = null;
                // Normalize inclusion-by-xid elements
                oldtable = Normalization.build_normalization_table(newitem.payload_element);
                
                // There is already a new revision of this element.
                // Unless the already included node is contentually equal
                // to the current instance, the user must solve the conflicting
                // revisions by hand.
                if (nodes_equal(normalnode, newitem.payload_element, oldtable)) {
                    // They are equal; the revision changes are consistent,
                    // and the new revision is already included in 
                    // the repository or in the commit set.
                    // This element is no longer a concern
                    // TODO: The inclusion-by-xid reference manifestations...
                    calculate_manifestation(
                        table,
                        oldtable,
                        map,
                        manifestations_map
                    );
                    
                    System.out.printf("Already recorded\n");
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
            
        } else {
            // if: no previous node with such xid.
            // Verify that it is allowed for the xid not to be found
            if (allow_new == false) {
                dump_repo(r);
                throw new RuntimeException(String.format(
                    "The element %s has an unknown xid=%s",
                    XPathIdentification.get_xpath(node), 
                    XidString.serialize(xid)));
            }
        }
        
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
        calculate_manifestation(
            table,
            null, // no older element in the repo
            map,
            manifestations_map
        );
        
        
        // This is no longer needed.
        //child_manifestations_map.clear();
        System.out.printf("Add\n");
        
        // Apply the denormalization table to the references of 
        // the normalized element.
        Normalization.denormalize_refs(table);
        
        r.add_node(normalnode, item);
        
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
    
    protected static void dump_repo(Repository r) {
        int count = 0;
        System.out.printf("Repository dump ------\n");
        System.out.printf("new nodes\n");
        for (Map.Entry<Xid, FidaNode> entry : r.newnodes.entrySet()) {
            FidaNode node = entry.getValue();
            Xid xid = entry.getKey();
            System.out.printf("Key    xid:          %s\n", XidString.serialize(xid));
            System.out.printf("Value  payload xid:  %s\n", XidString.serialize(node.payload_xid));
            System.out.printf("       lifeline xid: %s\n", XidString.serialize(node.item_xid));
            System.out.printf("\n");
        }
        
        
        FidaNode result = r.newnodes.get(new Xid("jani/left_foot", 1));
        if (result != null) {
            System.out.printf("found\n");
        }
        
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
        Repository r,
        List<Normalization.RefXidRecord> table
    ) {
        // Assign unique link_xid to each inclusion-by-xid element.
        for (Normalization.RefXidRecord record : table) {
            Xid linkxid = new Xid(String.format("#link!%d", r.unique()), 1);
            record.xid = linkxid;
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
    
} // XidClient





