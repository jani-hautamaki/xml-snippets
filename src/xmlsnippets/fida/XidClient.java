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
            List<Stack<Xid>> manifestation = new LinkedList<Stack<Xid>>();
            Map<Element, List<Stack<Xid>>> manifestations_map
            = new LinkedHashMap<Element, List<Stack<Xid>>>();
            
            System.out.printf("Processing file %s\n", file.getPath());
            populate(r, doc.getRootElement(), manifestation, manifestations_map);
            System.out.printf("\n\n");
            
            // Remove the following comment to see how the file looks
            // after the population procedure. The xid revs should have
            // been filled with proper values.
            //XPathDebugger.debug(doc);
            
            dump_manifestation(manifestation);
            dump_mmap(manifestations_map);
            
            
        } // for: each file name
        
        // TODO:
        // Check all ref_xid values. They should point to known xids
        // that are known to the system by now.
        
        // If all is in order, this point is reached and the commit
        // can be executed.
        
        // Combine all added nones into a document
        Document newdoc = new Document();
        Element newroot = new Element("root");
        newdoc.setRootElement(newroot);
        for (FidaNode n : r.newnodes.values()) {
            newroot.addContent(n.payload_element);
        }
        XPathDebugger.debug(newdoc);
        
        
    } // add_files()

    public static void parse_repository(Repository r) {
        // TODO
        
        r.xid = new Xid("anon", 1);
        
        r.oldnodes = new HashMap<Xid, FidaNode>();
        r.newnodes = new LinkedHashMap<Xid, FidaNode>();
        r.commits = new LinkedList<FidaCommit>();
        
        
    } // parse_repository()
    
    /**
     * @param r [in/out] Repository
     * @param node [in] the node whose contents are to be put into repository
     * @param man [out] the manifestation data for this particular instance
     */
    public static void populate(
        Repository r,
        Element node,
        List<Stack<Xid>> manifestation,
        Map<Element, List<Stack<Xid>>> manifestations_map
    ) {
        
        // Map for the manifestations
        Map<Element, List<Stack<Xid>>> child_manifestations_map
            = new LinkedHashMap<Element, List<Stack<Xid>>>();
        
        // Depth first
        for (Object obj : node.getContent()) {
            if ((obj instanceof Element) == false) {
                // Not an Element object. Skip
                continue;
            } // if: not an element
            
            // Depth-first recursion
            Element child = (Element) obj;
            
            List<Stack<Xid>> child_manifestation = 
                new LinkedList<Stack<Xid>>();
            
            populate(r, child, child_manifestation, manifestations_map);
            dump_mmap(manifestations_map);
            
            // If the child manifestation contains manually-defined
            // unexpand nodes, include the manifestation in the map
            if (child_manifestation.size() > 0) {
                //System.out.printf("Child %s had non-empty manifestation\n", XPathIdentification.get_xpath(child));
                child_manifestations_map.put(child, child_manifestation);
            }
            // Reassign the key
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
            /*
            // But remember to propagate all manifestations.
            calculate_manifestation(
                table,
                null, // no corresponding table for this unidentified element
                map,
                child_manifestations_map,
                manifestation
            );
            */
            
            
            
            // Simply bubble up the manifestation info
            for (List<Stack<Xid>> list : child_manifestations_map.values()) {
                manifestation.addAll(list);
            }
            System.out.printf("Bubbling up:\n");
            dump_manifestation(manifestation);
            System.out.printf("---<eom>\n");
            
            
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
                    child_manifestations_map,
                    manifestation,
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
                        child_manifestations_map,
                        manifestation,
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
            child_manifestations_map,
            manifestation,
            manifestations_map
        );
        
        
        // This is no longer needed.
        //child_manifestations_map.clear();
        System.out.printf("Add\n");
        
        // Apply the denormalization table to the references of 
        // the normalized element.
        Normalization.denormalize_refs(table);
        
        r.add_node(normalnode, item);
        System.out.printf("newnodes size: %d\n", r.newnodes.size());
        
    } // populate()
    
    protected static void calculate_manifestation(
        List<Normalization.RefXidRecord> table,
        List<Normalization.RefXidRecord> oldtable,
        Map<Element, Element> map,
        Map<Element, List<Stack<Xid>>> child_manifestations_map,
        List<Stack<Xid>> manifestation,
        Map<Element, List<Stack<Xid>>> manifestations_map
    ) {
        
        // Create translation map from new table to old table.
        Map<Xid, Xid> xidmap = null;
        
        if (oldtable != null) {
            System.out.printf("Creating link_xid map\n");
            xidmap = new HashMap<Xid, Xid>();
            // Assert that the tables match
            ListIterator<Normalization.RefXidRecord> iter_new;
            ListIterator<Normalization.RefXidRecord> iter_old;
            
            iter_new = table.listIterator();
            iter_old = oldtable.listIterator();
            
            while (iter_new.hasNext() && iter_old.hasNext()) {
                Normalization.RefXidRecord new_rec = iter_new.next();
                Normalization.RefXidRecord old_rec = iter_old.next();
                
                // Verify that both ref_xids reference to the same target
                String new_refxid = new_rec.element.getAttributeValue("ref_xid");
                String old_refxid = old_rec.element.getAttributeValue("ref_xid");
                if (old_refxid.equals(new_refxid) == false) {
                    throw new RuntimeException(String.format(
                        "ref_xid values differ: \"%s\" vs \"%s\"",
                        new_refxid, old_refxid));
                } // if
                
                System.out.printf("new=%s ---> old=%s\n",
                    XidString.serialize(new_rec.xid),
                    XidString.serialize(old_rec.xid)
                );
                xidmap.put(new_rec.xid, old_rec.xid);
            } // while
            
            if (iter_new.hasNext() || iter_old.hasNext()) {
                throw new RuntimeException(String.format(
                    "RefXidRecord tables have mismatching number of entries"));
            } // if: mismatch
        } // if: there is an oldtable.
        
        
        Map<Element, List<Stack<Xid>>> newmap = 
            new LinkedHashMap<Element, List<Stack<Xid>>>();
        /*
        for (Map.Entry<Element, List<Stack<Xid>>> entry :
            manifestations_map.entrySet())
        {
            Element orig_child = entry.getKey();
            List<Stack<Xid>> list = entry.getValue();
            
            if (orig_child == null) {
            } else {
                // See if the child was pruned or cloned and replaced
                // with an ref_xid child having link_xid identity
                Element replacement_child = rget(map, orig_child);
                
                if (replacement_child != null) {
                    // Yes. determine what was the link_xid value
                    Xid link_xid = null;
                    for (Normalization.RefXidRecord record : table) {
                        if (record.element == replacement_child) {
                            link_xid = record.xid;
                            break;
                        }
                    }  // for
                    
                    if (link_xid  == null) {
                        throw new RuntimeException("Shouldnt happen?");
                    }
                    
                    // Translate the newtable's link_xid to an oldtable's
                    // link_xid
                    if (xidmap != null) {
                        // Translate newtable's xid to oldtable's xid
                        link_xid = xidmap.get(link_xid);
                    }
                    // Append all stacks in the list with this xid
                    for (Stack<Xid> stack : list) {
                        stack.push(link_xid);
                    }
                } else {
                    //System.out.printf("Original child in the manifestation mapping not found from normalization map\n");
                    //System.out.printf("Xpath: %s\n", XPathIdentification.get_xpath(orig_child));
                } // if-else
            } // if-else: child not null
            
        } // for: each manifestations_map entry
        */
        
        // Append noexpands from the current denormalization table
        // to it also
        List<Stack<Xid>> nullist = manifestations_map.get(null);
        if (nullist == null) {
            nullist = new LinkedList<Stack<Xid>>();
        } else {
            System.out.printf("Null stack existing... Don\'t know why??\n");
        }
        
        // Populate the nullist with noexpand entried from the current
        // denormalization table
        for (Normalization.RefXidRecord record : table) {
            
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
                
                
                // These are discarded; no need to remember expansions.
                continue;
            } else {
                Stack<Xid> newstack = new Stack<Xid>();
                newstack.push(targetxid);
                nullist.add(newstack);
            }
        } // for
        
        if (nullist.size() > 0) {
            manifestations_map.put(null, nullist);
        }
        
        
        
        // Map inclusion-by-xid link_xids
        
        for (Normalization.RefXidRecord record : table) {
            
            // Pick the inclusion-by-xid Element object 
            // in the normalized copy
            Element replacement_elem = record.element;
            
            // Translate the inclusion-by-xid Element reference
            // to Element in the original content element.
            Element orig_elem = map.get(replacement_elem);
            
            // Use the reference to the original Element
            // to dig up its manifestation's unexpansion table
            // Translate the original element to its unexpand table.
            List<Stack<Xid>> child_manifestation
                = child_manifestations_map.remove(orig_elem);
            
            if (child_manifestation == null) {
                System.out.printf("No child manifestation for %s\n",
                    XPathIdentification.get_xpath(orig_elem));
            } else {
                System.out.printf("Child has manifestation info: %s\n",
                    XPathIdentification.get_xpath(orig_elem));
                System.out.printf("Size: %d\n", child_manifestation.size());
            }
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
            
            // Append each noexpand entry in the child's manifestation 
            // with the link_xid of the inclusion-by-xid element.
            // After appending, insert the stack to the actual
            // return variable noexpand manifestation list.
            if (child_manifestation != null) {
                for (Stack<Xid> stack : child_manifestation) {
                    stack.push(targetxid);
                    // Push the stack to the return value manifestation
                    manifestation.add(stack);
                } // for: each child_manifestation entry
            } // if: child had a noexpand manifestation list
            
            // If the current record is an unexpand record,
            // Include that too in the result set.
            
            if (record.expand == false) {
                System.out.printf("------ unexpand\n");
                System.out.printf("Unexpand: %s     link_xid=%s (orig) link_xid=%s (target)\n", 
                    record.element.getAttributeValue("ref_xid"),
                    XidString.serialize(record.xid),
                    XidString.serialize(targetxid)
                );
                // Create a new stack, since this is the leaf node
                // of this noexpand entry.
                Stack<Xid> cstack = new Stack<Xid>();
                // Add the link_xid of the corresponding stored normalized
                // element to the newly created stack
                cstack.push(targetxid);
                // Include the newly created stack to the return variable
                manifestation.add(cstack);
            } // if: the current record itself is an unexpand record
            
            // NOTE: The two of these should be mutually exclusive.
            // If the currently studied record is an expand=false record,
            // then it shouldn't be possible for it to have any entry on
            // the child_manifestation_map.
            
        } // for: each inclusion-by-xid element in the cloned copy
        
        System.out.printf("child_manifestations.map.size() left: %d\n",
            child_manifestations_map.size());
        // Process all child manifestations that were left, ie.
        // are not behind any identified element
        for (Map.Entry<Element, List<Stack<Xid>>> entry : 
            child_manifestations_map.entrySet())
        {
            List<Stack<Xid>> list = entry.getValue();
            
            System.out.printf("Bubbling up leftovers:\n");
            dump_manifestation(list);
            System.out.printf("---<eom>\n");
            
            // Add all
            manifestation.addAll(list);
        } // for
        
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
                StringBuilder sb = new StringBuilder();
                for (int i = stack.size()-1; i >= 0; i--) {
                    sb.append(String.format("/%s",
                        XidString.serialize(stack.elementAt(i))));
                } // for
                System.out.printf("   %s\n", sb.toString());
            }
            System.out.printf("---\n");
        } // for
        System.out.printf("Manifestations map <end>\n");
    } // dump_mmap()
    
    protected static void dump_manifestation(List<Stack<Xid>> m) {
        for (Stack<Xid> stack : m) {
            StringBuilder sb = new StringBuilder();
            for (int i = stack.size()-1; i >= 0; i--) {
                sb.append(String.format("/%s",
                    XidString.serialize(stack.elementAt(i))));
            } // for
            System.out.printf("%s\n", sb.toString());
        } // for each
    } // dump_manifestation() 
    
    
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





