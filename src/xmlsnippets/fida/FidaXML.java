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
import java.util.List;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
// java date formatting 
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
// jdom imports
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Attribute;
import org.jdom.Text;
// xmlsnippets imports
import xmlsnippets.core.Xid;
import xmlsnippets.core.XidString;
import xmlsnippets.core.XidIdentification;
import xmlsnippets.fida.Fida;
import xmlsnippets.util.Digest;
import xmlsnippets.util.XMLFileHelper;
import xmlsnippets.util.XPathIdentification;


/**
 * XML Serialization / Deserialization of {@link Fida} data objects.
 */
 
public class FidaXML {

    // CONSTANTS
    //===========
    
    public static final String
        ELEM_FIDA_REPOSITORY                    = "FidaRepository";
    
    // Repository/State
    
    public static final String
        ELEM_FIDA_REPOSITORY_STATE              = "FidaState";
    
    public static final String
        ELEM_FIDA_REPOSITORY_SEED               = "Seed";
    
    public static final String
        ELEM_FIDA_REPOSITORY_HEAD               = "LatestCommit";
    
    public static final String
        ATTR_FIDA_REPOSITORY_HEAD_XID_LINK      = "link";
    
    // Repository/Commit
    
    public static final String
        ELEM_FIDA_COMMIT                        = "FidaCommit";
    
    public static final String
        ELEM_FIDA_COMMIT_AUTHOR                 = "Author";
    
    public static final String
        ELEM_FIDA_COMMIT_DATE                   = "Date";
    
    // Repository/Commit/Layout
    
    public static final String
        ELEM_FIDA_COMMIT_LAYOUT                 = "Layout";
        
    public static final String
        ELEM_FIDA_FILE                          = "FidaFile";
        
    public static final String
        ELEM_FIDA_FILE_PREVIOUS                 = "PreviousFile";
        
    public static final String
        ATTR_FIDA_FILE_PREVIOUS_XID_LINK        = "link";
        
    public static final String
        ELEM_FIDA_FILE_PATH                     = "Path";
        
    public static final String
        ELEM_FIDA_FILE_DIGEST                   = "Digest";
        
    public static final String
        ATTR_FIDA_FILE_DIGEST_ALGO              = "algo";
        
    public static final String
        ELEM_FIDA_FILE_ROOT                     = "RootElement";
        
    public static final String
        ATTR_FIDA_FILE_ROOT_XID_LINK            = "link";
        
    public static final String
        ELEM_FIDA_FILE_MANIFESTATION            = "Manifestation";
        
    public static final String
        ELEM_FIDA_UNEXPAND_ENTRY                = "Unexpand";
    
    // Repository/Commit/Nodes
    
    public static final String
        ELEM_FIDA_COMMIT_NODES                  = "Nodes";


    public static final String
        ELEM_FIDA_NODE                          = "FidaNode";
        
    public static final String
        ELEM_FIDA_NODE_PREVIOUS                 = "PreviousNode";
    
    public static final String
        ATTR_FIDA_NODE_PREVIOUS_XID_LINK        = "link";
        
    public static final String
        ELEM_FIDA_NODE_PAYLOAD_CONTAINER        = "Payload";
    
    // General purpose xid link
    
    // CLASS VARIABLES
    //=================
    
    /**
     * Singleton object used to create and parse date strings.
     */
    private static DateFormat date_fmt = null;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally disabled.
     */
    private FidaXML() {
    } // ctor
    
    
    // PRIMARY METHODS
    //=================
    
    public static void serialize(Fida.Repository r) {
        try {
            Document doc = new Document();
            Element root = serialize_repository(r);
            doc.setRootElement(root);
            XMLFileHelper.serialize_document_formatted(doc, r.file);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    } // serialize()
    
    public static Fida.Repository deserialize(File file) {
        
        Document doc = null;
        try {
            doc = XMLFileHelper.deserialize_document(file);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        } // try-catch
        
        
        Fida.Repository rval = null;
        rval = deserialize_repository(doc.getRootElement());

        // Set the repository file
        rval.file = file;
        
        // Secondary deserialization
        build(rval);
        
        return rval;
    } // deserialize()
    
    // SECONDARY DESERIALIZATION
    //===========================
    
    protected static void build(Fida.Repository r) {
        // Semantically inclined deserialization. Here all internal
        // stuff are being populated
        Fida.State state = r.state;
        
        state.internals = build_internals(r);
        
        build_links(r);
        
        build_externals(r);
        
    } // build()
    
    public static Map<Integer, Fida.Item> build_internals(
        Fida.Repository r
    ) {
        Map<Integer, Fida.Item> map = new HashMap<Integer, Fida.Item>();
        
        // Traverse through the repository. This would be nicer if
        // all Fida.Item objects would have a list of their children
        //put_uid(map, r); // the uid part of the repository is the name
        // of the repository
        
        for (Fida.Commit fc : r.commits) {
            put_uid(map, fc);
            
            for (Fida.File ff : fc.layout) {
                put_uid(map, ff);
            } // for: each file
            
            for (Fida.Node fn : fc.nodes) {
                put_uid(map, fn);
                // Discover all used uid's in link_xid attributes in
                // the normalized payload content
                build_link_xids(map, fn.payload_element);
                
            } // for: each node
        } // for
        
        return map;
    } // build_internals()
    
    public static void build_link_xids(
        Map<Integer, Fida.Item> map,
        Element elem
    ) {
        // Depth first
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element child = (Element) obj;
            build_link_xids(map, child);
        } // for
        
        String link_xid = elem.getAttributeValue("link_xid");
        if (link_xid != null) {
            int uid = get_uid(link_xid);
            // Create an empty entry
            put_uid(map, uid, null);
        } // if
    } // build_link_xids()
    
    
    public static void build_links(
        Fida.Repository r
    ) {
        // Pick the internals map to a local variable for convenience.
        Map<Integer, Fida.Item> map = r.state.internals;

        if (r.state.head_xid != null) {
            r.state.head = (Fida.Commit) resolve_xid(map, r.state.head_xid);
            r.state.head_xid = null;
        }
        
        // Traverse through the repository
        for (Fida.Commit fc : r.commits) {
        
            for (Fida.File ff : fc.layout) {
                if (ff.prev_xid == null) {
                    continue;
                }
                // otherwise attempt resolving
                ff.prev = (Fida.File) resolve_xid(map, ff.prev_xid);
                ff.prev_xid = null;
            } // for: each file
            
            for (Fida.Node fn : fc.nodes) {
                if (fn.prev_xid == null) {
                    continue;
                }
                // otherwise attempt resolving
                fn.prev = (Fida.Node) resolve_xid(map, fn.prev_xid);
                fn.prev_xid = null;
                
                // Add forward link to the prev node
                fn.prev.next.add(fn);
                
            } // for: each node
        } // for
        
    } // build_backward_links()
    
    
    
    public static void build_externals(
        Fida.Repository r
    ) {
        // Maps user namespace xid to the corresponding Fida.Node
        Map<Xid, Fida.Node> map = new HashMap<Xid, Fida.Node>();
        // traverse whole repository
        for (Fida.Commit fc : r.commits) {
            for (Fida.Node fn : fc.nodes) {
                // The payload xid has been already populated
                // while deserializing. Use it
                if (map.get(fn.payload_xid) != null) {
                    throw new RuntimeException(String.format(
                        "User namespace xid=\"%s\" is a duplicate", 
                        XidString.serialize(fn.payload_xid)));
                } // if
                map.put(fn.payload_xid, fn);
            } // for: each node
        } // for: each commit
        
        // Record
        r.state.externals = map;

    } // build_externals()
        
    
    
    
    
    private static Fida.Item resolve_xid(
        Map<Integer, Fida.Item> map,
        Xid xid
    ) {
        
        // This may throw
        int uid = get_uid(xid.id);
        
        // Get the item
        Fida.Item item = map.get(uid);
        
        // It must exist
        if (item == null) {
            // NOTE: attempting to resolve a link_xid's uid will throw!
            throw new RuntimeException(String.format(
                "Cannot resolve uid \"%08x\"", uid));
        }
        
        return item;
    } // get_uid()
    
    /**
     * Creates a mapping between the Fida.Item's internal xid's uid
     * and the item itself.
     */
    private static void put_uid(
        Map<Integer, Fida.Item> map,
        Fida.Item item
    ) {
        // Check that the item has a xid.
        if (item.item_xid == null) {
            throw new RuntimeException("xid missing! Cant tell exactly where :(");
        }
        
        // get uid from xid
        int uid = get_uid(item.item_xid.id);
        
        // associate uid with the item
        put_uid(map, uid, item);
    } // put_uid();
    
    /**
     * Associates an uid to an Fida.Item object, and verifies that the uid
     * is not taken.
     */
    private static void put_uid(
        Map<Integer, Fida.Item> map,
        int uid,
        Fida.Item item
    ) {
        if (map.get(uid) != null) {
            throw new RuntimeException(String.format(
                "uid=\"%08x\" is duplicate!", uid));
        }
        map.put(uid, item);
    } // put_uid()
    
    /**
     * Creates an empty mapping; just reserves the uid in the internal xid
     * Used for link_xids.
     */
    private static void put_uid(
        Map<Integer, Fida.Item> map,
        Xid xid
    ) {
        int uid = get_uid(xid);
        
        // create a null link (DANGEROUS!)
        put_uid(map, uid, null);
    } // put_uid();
    
    
    /**
     * For convenience.
     */
    private static int get_uid(Xid xid) {
        return get_uid(xid.id);
    }
    
    /**
     * Deserializes the uid from the internal xid
     */
    private static int get_uid(String id) {
        int from = id.indexOf('!') + 1;
        if (from == -1)  {
            throw new RuntimeException(String.format(
                "Invalid internal xid.id; no exclamation mark: \"%s\"", id));
        }
        
        String uidstring = id.substring(from);
        if (uidstring.length() > 8) {
            throw new RuntimeException(String.format(
                "Invalid internal xid uid; too long, max of 8 chars expected: \"%s\"", id));
        } 
        else if (uidstring.length() == 0) {
            throw new RuntimeException(String.format(
                "Invalid internal xid uid; it is empty: \"%s\"", id));
        }
        
        int rval;
        try {
            //rval = Integer.parseInt(uidstring, 16); // radix=16 (hex)
            long tmp = Long.parseLong(uidstring, 16); // radix=16 (hex)
            rval = (int) (tmp & 0x00000000ffffffff);
            
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "Invalid internal xid.id; either too long or not a hexadecimal at all: %s", id));
        } // try-catch
        
        return rval;
    }
    
    // OTHER METHODS
    //===============
    
    
    protected static DateFormat get_date_fmt() {
        if (date_fmt == null) {
            date_fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        }
        return date_fmt;
    } // get_date_fmt()
    
    protected static String serialize_date(Date date) {
        DateFormat fmt = get_date_fmt();
        
        return fmt.format(date);
    } // serialize_date()
    
    protected static Date deserialize_date(String s) 
        throws ParseException
    {
        DateFormat fmt = get_date_fmt();
        return fmt.parse(s);
    }
    
    
    // SERIALIZATION (easier)
    //========================
    
    
    
    public static void set_item_xid(Element elem, Fida.Item item) {
        XidIdentification.set_xid(elem, item.item_xid);
    } // set_item_xid()
    
    public static Element serialize_repository(Fida.Repository r) {
        Element rval = new Element(ELEM_FIDA_REPOSITORY);
        
        // Set internal xid
        set_item_xid(rval, r);
        
        // Serialize state
        rval.addContent(serialize_state(r.state));
        
        // Serialize all commits
        for (Fida.Commit commit : r.commits) {
            rval.addContent(serialize_commit(commit));
        } // for: each commit
        
        return rval;
    } // serialize_repository()
    
    public static Element serialize_state(Fida.State state) {
        Element rval = new Element(ELEM_FIDA_REPOSITORY_STATE);
        
        rval.addContent(serialize_rng_seed(state.rng));
        
        // The empty repository might not have a head
        if (state.head != null) {
            rval.addContent(serialize_commit_head_link(state.head.item_xid));
        }
        
        return rval;
    } // serialize_state()
    
    public static Element serialize_rng_seed(
        Random rng
    ) {
        Element rval = new Element(ELEM_FIDA_REPOSITORY_SEED);
        
        // TODO!
        // The seed cannot be retrieved from Random.
        // A bit of a problem. A single failing commit canno be
        // re-created exactly.
        
        // TODO:
        // There a problem also with the format(). Unless restrictied
        // with anding, it serializes bigger longs than it is capable
        // of parsing later with long.parseLong()! Fuck.
        
        // For now, just pretend that a produced random number is the seed
        rval.setText(String.format("%x", 
            ((int) rng.nextLong()) & 0xffffffff));
        
        return rval;
    } // serialize_rng_seed()
    
    public static Element serialize_commit_head_link(
        Xid xid
    ) {
        Element rval = new Element(ELEM_FIDA_REPOSITORY_HEAD);

        rval.setAttribute(  
            ATTR_FIDA_REPOSITORY_HEAD_XID_LINK,
            XidString.serialize(xid)
        );
        
        return rval;
    } // serialize_commit_head_link()
    
    // Repository/Commit
    
    public static Element serialize_commit(Fida.Commit commit) {
        Element rval = new Element(ELEM_FIDA_COMMIT);

        // Set internal xid
        set_item_xid(rval, commit);
        
        rval.addContent(serialize_commit_author(commit));
        rval.addContent(serialize_commit_date(commit));
        rval.addContent(serialize_commit_layout(commit.layout));
        rval.addContent(serialize_commit_nodes(commit.nodes));
        
        return rval;
    } // serialize_commit()
    
    public static Element serialize_commit_author(Fida.Commit commit) {
        Element rval = new Element(ELEM_FIDA_COMMIT_AUTHOR);
        
        rval.setText(commit.author);
        
        return rval;
    } // serialize_commit_author()

    public static Element serialize_commit_date(Fida.Commit commit) {
        Element rval = new Element(ELEM_FIDA_COMMIT_DATE);
        
        rval.setText(serialize_date(commit.date));
        
        return rval;
    } // serialize_commit_date()

    
    // FidaRepository/Commit/Layout
    
    
    public static Element serialize_commit_layout(
        List<Fida.File> layout
    ) {
        Element rval = new Element(ELEM_FIDA_COMMIT_LAYOUT);
        
        for (Fida.File ff : layout) {
            rval.addContent(serialize_file(ff));
        } // for: each file
        
        return rval;
    } // serialize_commit_layout()

    public static Element serialize_file(
        Fida.File ff
    ) {
        Element rval = new Element(ELEM_FIDA_FILE);

        // Set internal xid
        set_item_xid(rval, ff);
        
        // Optionally serialize the link to the previous, if any
        if (ff.prev != null) {
            rval.addContent(serialize_file_previous(ff.prev.item_xid));
        }
        
        rval.addContent(serialize_file_path(ff));
        rval.addContent(serialize_file_digest(ff.digest));
        rval.addContent(serialize_file_root(ff));
        
        // Optionally serialize the manifestation, if there is one.
        if (ff.manifestation != null) {
            rval.addContent(serialize_file_manifestation(ff.manifestation));
        }
        
        return rval;
    } // serialize_file
    
    public static Element serialize_file_previous(
        Xid xid
    ) {
        Element rval = new Element(ELEM_FIDA_FILE_PREVIOUS);
        
        rval.setAttribute(
            ATTR_FIDA_FILE_PREVIOUS_XID_LINK,
            XidString.serialize(xid)
        );
        return rval;
    } // serialize_file_previous()

    public static Element serialize_file_path(
        Fida.File ff
    ) {
        Element rval = new Element(ELEM_FIDA_FILE_PATH);
        
        rval.setText(ff.path);
        return rval;
    } // serialize_file_path

    public static Element serialize_file_digest(
        Digest digest
    ) {
        Element rval = new Element(ELEM_FIDA_FILE_DIGEST);
        
        rval.setAttribute(
            ATTR_FIDA_FILE_DIGEST_ALGO, digest.get_digest_algo());
        
        rval.setText(digest.to_hexstring());
        
        return rval;
    }

    public static Element serialize_file_root(
        Fida.File ff
    ) {
        Element rval = new Element(ELEM_FIDA_FILE_ROOT);
        
        rval.setAttribute(  
            ATTR_FIDA_FILE_ROOT_XID_LINK,
            XidString.serialize(ff.root_xid)
        );
        
        return rval;
    }
    
    public static Element serialize_file_manifestation(
        List<Stack<Xid>> manifestation
    ) {
        Element rval = new Element(ELEM_FIDA_FILE_MANIFESTATION);
        
        for (Stack<Xid> unexpand : manifestation) {
            rval.addContent(serialize_unexpand_entry(unexpand));
        } // for
        
        return rval;
    }
    
    public static Element serialize_unexpand_entry(
        Stack<Xid> unexpand
    ) {
        Element rval = new Element(ELEM_FIDA_UNEXPAND_ENTRY);
        rval.setText(serialize_unexpand(unexpand));
        
        return rval;
    }
    
    
    // FidaRepository/Commit/Nodes
    

    public static Element serialize_commit_nodes(
        List<Fida.Node> nodes
    ) {
        Element rval = new Element(ELEM_FIDA_COMMIT_NODES);
        for (Fida.Node node : nodes) {
            rval.addContent(serialize_node(node));
        }
        return rval;
    } // serialize_commit_nodes()

    public static Element serialize_node(
        Fida.Node node
    ) {
        Element rval = new Element(ELEM_FIDA_NODE);

        // Set internal xid
        set_item_xid(rval, node);
        
        // Optionally serialize the previous node's xid link,
        // if there is one
        if (node.prev != null) {
            rval.addContent(
                serialize_node_previous_link(node.prev.item_xid)
            ); // addContent()
        } // if
        
        rval.addContent(serialize_node_payload(node));
        
        return rval;
    } // serialize_node()

    public static Element serialize_node_previous_link(
        Xid xid
    ) {
        Element rval = new Element(ELEM_FIDA_NODE_PREVIOUS);
        
        rval.setAttribute(
            ATTR_FIDA_NODE_PREVIOUS_XID_LINK,
            XidString.serialize(xid)
        ); // setAttribute
        
        return rval;
    } // serialize_node_previous_link()
    

    public static Element serialize_node_payload(
        Fida.Node node
    ) {
        Element rval = new Element(ELEM_FIDA_NODE_PAYLOAD_CONTAINER);
        
        // Just embed a clone of the payload
        Element unparented = (Element) node.payload_element.clone();
        
        rval.addContent(unparented);
        
        return rval;
    } // serialize_node_payload()
    
    // DESERIALIZATION (more difficult)
    //========================================================================

    // Repository
    //========================================================================
    
    
    public static Fida.Repository deserialize_repository(Element elem) {
        Fida.Repository rval = new Fida.Repository();
        
        expect_name(elem, ELEM_FIDA_REPOSITORY);
        
        // Replacement state
        Fida.State state = null;
        
        for (Object obj : elem.getContent()) {
            if (skip(obj)) continue;
            Element c = (Element) obj;
            String name = c.getName();
            
            if (name.equals(ELEM_FIDA_REPOSITORY_STATE)) {
                expect_unset(c, state);
                state = deserialize_state(c);
            }
            else if (name.equals(ELEM_FIDA_COMMIT)) {
                Fida.Commit commit = deserialize_commit(c);
                rval.commits.add(commit);
            }
            else {
                unexpected_child(c);
            }

        } // for: each content
        
        expect_set(elem, ELEM_FIDA_REPOSITORY_STATE, state);
        
        rval.state = state;
        rval.item_xid = get_xid(elem);
        
        
        return rval;
    } // deserialize_repository
    
    // Repository/State
    //========================================================================
    
    public static Fida.State deserialize_state(Element elem) {
        Fida.State rval = new Fida.State();
        
        expect_name(elem, ELEM_FIDA_REPOSITORY_STATE);
        
        Long seed = null;
        Xid head_xid = null;
        
        for (Object obj : elem.getContent()) {
            if (skip(obj)) continue;
            Element c = (Element) obj;
            String name = c.getName();
            if (name.equals(ELEM_FIDA_REPOSITORY_SEED)) {
                expect_unset(c, seed);
                seed = deserialize_seed(c);
            }
            else if (name.equals(ELEM_FIDA_REPOSITORY_HEAD)) {
                expect_unset(c, head_xid);
                head_xid = deserialize_xid_link(
                    c, ATTR_FIDA_REPOSITORY_HEAD_XID_LINK);
            }
            else {
                unexpected_child(c);
            }
        } // for
        
        expect_set(elem, ELEM_FIDA_REPOSITORY_SEED, seed);
        //expect_set(elem, ELEM_FIDA_REPOSITORY_HEAD, head_xid);
        // Empty repository doesnt have a head!
        
        rval.rng = new Random(seed);
        rval.head_xid = head_xid;
        
        return rval;
    } // deserialize_state()
    
    public static long deserialize_seed(Element elem) {
        expect_name(elem, ELEM_FIDA_REPOSITORY_SEED);
        expect_nochildren(elem);
        
        String s = elem.getText();
        long rval;
        
        try {
            rval = Long.parseLong(s, 16); // radix=16 (hex)
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(String.format(
                "%s: expected an integer, but found \"%s\"",
                get_addr(elem), s));
        } // try-catch
        return rval;
    } // deserialize_seed()

    // Repository/Commit
    //========================================================================

    
    public static Fida.Commit deserialize_commit(Element elem) {
        Fida.Commit rval = new Fida.Commit();
        
        expect_name(elem, ELEM_FIDA_COMMIT);
        
        String author = null;
        Date date = null;
        List<Fida.File> layout = null;
        List<Fida.Node> nodes = null;
        
        for (Object obj : elem.getContent()) {
            if (skip(obj)) continue;
            Element c = (Element) obj;
            String name = c.getName();
            
            if (name.equals(ELEM_FIDA_COMMIT_AUTHOR)) {
                expect_unset(c, author);
                author = deserialize_commit_author(c);
            }
            else if (name.equals(ELEM_FIDA_COMMIT_DATE)) {
                expect_unset(c, date);
                date = deserialize_commit_date(c);
            }
            else if (name.equals(ELEM_FIDA_COMMIT_LAYOUT)) {
                expect_unset(c, layout);
                layout = deserialize_commit_layout(c);
            }
            else if (name.equals(ELEM_FIDA_COMMIT_NODES)) {
                expect_unset(c, nodes);
                nodes = deserialize_commit_nodes(c);
            }
            else {
                unexpected_child(c);
            }
        } // for
        
        expect_set(elem, ELEM_FIDA_COMMIT_AUTHOR,  author);
        expect_set(elem, ELEM_FIDA_COMMIT_DATE,    date);
        expect_set(elem, ELEM_FIDA_COMMIT_LAYOUT,  layout);
        expect_set(elem, ELEM_FIDA_COMMIT_NODES,   nodes);
        
        rval.author = author;
        rval.date = date;
        rval.layout = layout;
        rval.nodes = nodes;
        rval.item_xid = get_xid(elem);
        
        return rval;
    } // deserialize_commit()
    
    public static String deserialize_commit_author(Element elem) {
        expect_name(elem, ELEM_FIDA_COMMIT_AUTHOR);
        expect_nochildren(elem);
        String rval = elem.getText();
        return rval;
    } // deserialize_commit_author()
    
    public static Date deserialize_commit_date(Element elem) {
        expect_name(elem, ELEM_FIDA_COMMIT_DATE);
        expect_nochildren(elem);
        String datestring = elem.getText();
        Date rval = null;
        try {
            rval = deserialize_date(datestring);
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "%s: invalid date \"%s\"", 
                get_addr(elem), datestring), ex);
        } // try-catch
        
        return rval;
    } // deserialize_commit_date()

    // Repository/Commit/Layout
    //========================================================================
    
    public static List<Fida.File> deserialize_commit_layout(Element elem) {
        List<Fida.File> rval = new LinkedList<Fida.File>();
        
        expect_name(elem, ELEM_FIDA_COMMIT_LAYOUT);
        
        for (Object obj : elem.getContent()) {
            if (skip(obj)) continue;
            Element c = (Element) obj;
            String name = c.getName();
            
            if (name.equals(ELEM_FIDA_FILE)) {
                Fida.File file = deserialize_file(c);
                rval.add(file);
            }
            else {
                unexpected_child(c);
            }
        } // for
        
        return rval;
    } // deserialize_commit_layout()
    
    public static Fida.File deserialize_file(Element elem) {
        Fida.File rval = new Fida.File();
        
        expect_name(elem, ELEM_FIDA_FILE);
        
        String path = null;
        Digest digest = null;
        Xid root_xid = null;
        Xid prev_xid = null;
        List<Stack<Xid>> manifestation = null;
        
        for (Object obj : elem.getContent()) {
            if (skip(obj)) continue;
            Element c = (Element) obj;
            String name = c.getName();
            
            if (name.equals(ELEM_FIDA_FILE_PATH)) {
                expect_unset(c, path);
                path = deserialize_file_path(c);
            } 
            else if (name.equals(ELEM_FIDA_FILE_DIGEST)) {
                expect_unset(c, digest);
                digest = deserialize_file_digest(c);
            }
            else if (name.equals(ELEM_FIDA_FILE_ROOT)) {
                expect_unset(c, root_xid);
                root_xid = deserialize_xid_link(c, ATTR_FIDA_FILE_ROOT_XID_LINK);
            }
            else if (name.equals(ELEM_FIDA_FILE_MANIFESTATION)) {
                expect_unset(c, manifestation);
                manifestation = deserialize_file_manifestation(c);
            }
            else if (name.equals(ELEM_FIDA_FILE_PREVIOUS)) {
                expect_unset(c, prev_xid);
                prev_xid = deserialize_xid_link(c, ATTR_FIDA_FILE_PREVIOUS_XID_LINK);
            }
            else {
                unexpected_child(c);
            } // if-else
            
        } // for
        
        expect_set(elem, ELEM_FIDA_FILE_PATH,   path);
        expect_set(elem, ELEM_FIDA_FILE_DIGEST, digest);
        expect_set(elem, ELEM_FIDA_FILE_ROOT,   root_xid);
        
        rval.prev_xid = prev_xid;
        rval.path = path;
        rval.digest = digest;
        rval.root_xid = root_xid;
        rval.manifestation = manifestation;
        rval.item_xid = get_xid(elem);
        
        return rval;
    } // deserialize_file()
    
    public static String deserialize_file_path(Element elem) {
        String rval = null;
        expect_name(elem, ELEM_FIDA_FILE_PATH);
        expect_nochildren(elem);
        rval = elem.getText();
        return rval;
    } // deserialize_file_path()
    
    public static Digest deserialize_file_digest(Element elem) {
        Digest rval = null;
        expect_name(elem, ELEM_FIDA_FILE_DIGEST);
        expect_nochildren(elem);
        
        String algo = get_attr(elem, ATTR_FIDA_FILE_DIGEST_ALGO);
        String hexstring = elem.getText().trim();
        
        rval = new Digest();
        try {
            rval.set_hex(algo, hexstring);
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "%s: invalid digest; %s", 
                get_addr(elem), ex.getMessage()),
                ex
            ); // new ..
        } // try-catch
        
        return rval;
    } // deserialize_file_digest()
    
    public static List<Stack<Xid>> deserialize_file_manifestation(
        Element elem
    ) {
        List<Stack<Xid>> rval = new LinkedList<Stack<Xid>>();
        
        expect_name(elem, ELEM_FIDA_FILE_MANIFESTATION);
        
        for (Object obj : elem.getContent()) {
            if (skip(obj)) continue;
            Element c = (Element) obj;
            String name = c.getName();
            
            if (name.equals(ELEM_FIDA_UNEXPAND_ENTRY)) {
                Stack<Xid> unexpand = null;
                unexpand = deserialize_unexpand_entry(c);
                rval.add(unexpand);
            }
            else {
                unexpected_child(c);
            }
        } // for
        
        return rval;
    } // deserialize_file_manifestation()
    
    public static Stack<Xid> deserialize_unexpand_entry(Element elem) {
        expect_name(elem, ELEM_FIDA_UNEXPAND_ENTRY);
        expect_nochildren(elem);
        
        String text = elem.getText();
        Stack<Xid> rval = deserialize_unexpand(text);
        
        return rval;
    } // deserialize_unexpand()

    
    // Repository/Commit/Nodes
    //========================================================================
    
    
    public static List<Fida.Node> deserialize_commit_nodes(Element elem) {
        List<Fida.Node> rval = new LinkedList<Fida.Node>();
        
        expect_name(elem, ELEM_FIDA_COMMIT_NODES);

        for (Object obj : elem.getContent()) {
            if (skip(obj)) continue;
            Element c = (Element) obj;
            String name = c.getName();
            
            if (name.equals(ELEM_FIDA_NODE)) {
                Fida.Node node = deserialize_node(c);
                rval.add(node);
            }
            else {
                unexpected_child(c);
            } // if-else
        } // for
        
        
        return rval;
    } // deserialize_commit_nodes(9
    
    public static Fida.Node deserialize_node(Element elem) {
        Fida.Node rval = new Fida.Node();
        
        expect_name(elem, ELEM_FIDA_NODE);
        
        Xid prev_xid = null;
        Xid payload_xid = null;
        Element payload_element = null;
        
        for (Object obj : elem.getContent()) {
            if (skip(obj)) continue;
            Element c = (Element) obj;
            String name = c.getName();
            
            if (name.equals(ELEM_FIDA_NODE_PREVIOUS)) {
                expect_unset(c, prev_xid);
                prev_xid = deserialize_xid_link(
                    c, ATTR_FIDA_NODE_PREVIOUS_XID_LINK);
            }
            if (name.equals(ELEM_FIDA_NODE_PAYLOAD_CONTAINER)) {
                expect_unset(c, payload_element);
                payload_element = deserialize_payload_container(c);
            }
            else {
                unexpected_child(c);
            } // if-else
        } /// for
        
        expect_set(elem, ELEM_FIDA_NODE_PAYLOAD_CONTAINER, payload_element);
        payload_xid = get_xid(payload_element);
        
        rval.prev_xid = prev_xid;
        rval.payload_element = payload_element;
        rval.payload_xid = payload_xid;
        rval.item_xid = get_xid(elem);
        
        return rval;
    } // deserialize_node()
    
    public static Element deserialize_payload_container(Element elem) {
        expect_name(elem, ELEM_FIDA_NODE_PAYLOAD_CONTAINER);
        List list = elem.getChildren();
        if (list.size() != 1) {
            throw new RuntimeException(String.format(
                "%s: the payload container must have a single child, but %d was found",
                get_addr(elem),
                list.size())
            ); // new ..
        } // if
        
        Element rval = (Element) list.get(0);
        return rval;
    } // deserialize_payload_container()

    
    // Miscellaneous
    //========================================================================
    
    
    public static Xid deserialize_xid_link(Element elem, String aname) {
        String aval = get_attr(elem, aname);
        Xid rval = XidString.deserialize(aval);
        return rval;
    }
    
    
    
    
    
    
    
    
    public static Stack<Xid> deserialize_unexpand(String s) {
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
    
   public static String serialize_unexpand(Stack<Xid> stack) {
        StringBuilder sb = new StringBuilder();
        for (int i = stack.size()-1; i >= 0; i--) {
            sb.append(String.format("/%s",
                XidString.serialize(stack.elementAt(i))));
        } // for
        return sb.toString();
    }
     
    
    
    // DESERIALIZATION HELPER METHODS
    //================================
    
    /**
     * Provides some kind of address of the element for the user
     */
    public static String get_addr(Element element) {
        return XPathIdentification.get_xpath(element);
    } // get_addr()

    public static String get_addr(Attribute attr) {
        return XPathIdentification.get_xpath(attr);
    } // get_addr()

    public static String get_addr(Text text) {
        return XPathIdentification.get_xpath(text);
    } // get_addr()
    
    
    public static void expect_name(Element elem, String name) {
        String elem_name = elem.getName();
        if (name.equals(elem_name) == false) {
            throw new RuntimeException(String.format(
                "%s: unexpected element; expected <%s>, but got <%s> instead",
                get_addr(elem), name, elem_name));
        } // if
    } // expect_name
    
    
    public static String get_attr(Element elem, String aname) {
        String rval = elem.getAttributeValue(aname);
        if (rval == null) {
            throw new RuntimeException(String.format(
                "%s: an attribute @%s is expected, but was not found",
                get_addr(elem), aname));
        }
        return rval;
    }
    
    public static Element get_child(
        Element elem, String name
    ) {
        List list = elem.getChildren(name);
        if (list.size() == 0) {
            throw new RuntimeException(String.format(
                "%s: a child element <%s> is expected, but was not found",
                get_addr(elem), name));
        }
        else if (list.size() > 1) {
            throw new RuntimeException(String.format(
                "%s: a single child element <%s> is expected, but %d was found",
                get_addr(elem), name, list.size()));
        }
        
        return (Element) list.get(0);
    } // get_child()
    
    public static boolean skip(Object obj) {
        if (obj instanceof Text) {
            Text text = (Text) obj;
            String s = text.getText();
            if (s.trim().length() > 0) {
                throw new RuntimeException(String.format(
                    "%s: text is not allowed here", 
                    get_addr( (Element) ((Text) obj).getParent() )
                )); // new ...
            }
            return true;
        } // if
        
        if (obj instanceof Element) {
            return false;
        }
        
        // Everything else can be skipped
        return true;
    } // skip()
    
    
    public static void expect_unset(
        Element elem,
        Object obj
    ) {
        if (obj != null) {
            throw new RuntimeException(String.format(
                "%s: is already specified earlier",
                get_addr(elem)));
        } // if
    } // expect_unset()
    
    public static void expect_set(
        Element elem,
        String name,
        Object obj
    ) {
        if (obj == null) {
            throw new RuntimeException(String.format(
                "%s: a child element <%s> is expected, but was not found",
                get_addr(elem), name));
        } // if
    } // expect_unset()

    public static void expect_attr_set(
        Element elem,
        String aname,
        Object obj
    ) {
        if (obj == null) {
            throw new RuntimeException(String.format(
                "%s: an attribute @%s is expected, but was not found",
                get_addr(elem), aname));
        } // if
    } // expect_unset()

    public static void unexpected_child(Element child) {
        throw new RuntimeException(String.format(
            "%s: an unexpcted child element",
            get_addr(child)));
    } // unexpected_child()
    
    public static void expect_nochildren(Element elem) {
        if (elem.getChildren().size() > 0) {
        throw new RuntimeException(String.format(
            "%s: no children is expected",
            get_addr(elem)));
        }
    } // expect_nochildren()

    public static Xid get_xid(Element elem) {
        Xid rval = null;
        try {
            rval = XidIdentification.get_xid(elem);
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "%s: has an invalid xid; %s", 
                get_addr(elem), ex.getMessage()), ex);
        } // try-catch
        
        if (rval == null) {
            throw new RuntimeException(String.format(
                "%s: a xid is expected, but was not found", 
                get_addr(elem)));
        } // if
        
        return rval;
    } // get_xid()
    
    
    
} // class FidaXML