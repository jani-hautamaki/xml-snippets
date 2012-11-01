//*******************************{begin:header}******************************//
//     XML Processing Snippets - https://code.google.com/p/xml-snippets/     //
//***************************************************************************//
//
//      xml-snippets:   XML Processing Snippets 
//                      with Some Theoretical Considerations
//
//      Copyright (C) 2012 Jani Hautam�ki <jani.hautamaki@hotmail.com>
//
//      Licensed under the terms of GNU General Public License v3.
//
//      You should have received a copy of the GNU General Public License v3
//      along with this program as the file LICENSE.txt; if not, please see
//      http://www.gnu.org/licenses/gpl-3.0.html
//
//********************************{end:header}*******************************//

package xmlsnippets.util;

// java core imports
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.LinkedList;
import java.io.OutputStreamWriter;
// java core for dates, deprecated
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
// jdom imports
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.DataConversionException;
// jdom imports for debug output
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
// xmlsnippets imports
import xmlsnippets.util.XMLFileHelper;
import xmlsnippets.core.Xid;
import xmlsnippets.core.XidIdentification;
import xmlsnippets.core.ContentualEq;
import xmlsnippets.core.Normalization;

public class Fida
{
    // CONSTANTS
    //===========
    
    /**
     * System exit value for succesful run is zero (0).
     */
    public static final int EXIT_SUCCESS = 0;
    
    /**
     * System exit value for unsuccesful run is on (1).
     */
    public static final int EXIT_FAILURE = 1;
    
    /**
     * ...
     */
    private static int g_revision = 0;
    
    private static XMLOutputter g_xmloutputter = null;
    
    // MEMBER VARIABLES
    //==================
    
    // CONSTRUCTORS
    //==============
    
    public Fida() {
    } // ctor
    
    // OTHER METHODS
    //===============
    
    protected static void create_repository(File file) 
        throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        Element root = new Element("FidaRepository");
        root.setAttribute("id", "anon");
        root.setAttribute("rev", "0");
        root.setAttribute("last", "");
        Document doc = new Document();
        doc.setRootElement(root);
        
        // This may fail
        XMLFileHelper.serialize_document_formatted(doc, file);
        
    } // create_repository()
    
    protected static Element get_payload(Element item) {
        for (Object obj : item.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            // Cast
            Element elem = (Element) obj;
            // Check if this is the Payload
            if (elem.getName().equals("Payload")) {
                // get the only child element.
                return (Element) elem.getChildren().get(0);
            } // if
        } // for
        throw new RuntimeException("Payload missing");
    } // get_payload_element()
    
    protected static Element add_item(
        List<Element> itemlist,
        Element payload,
        Element prev_item
    ) {
        // Create a clone of the payload XML element
        Element item = new Element("FidaItem");
        item.setAttribute("id", payload.getAttributeValue("id"));
        item.setAttribute("rev", payload.getAttributeValue("rev"));
        
        // Link to previous if such exists
        if (prev_item != null) {
            Element property_prev = new Element("PrevItem");
            property_prev.setAttribute("id", prev_item.getAttributeValue("id"));
            property_prev.setAttribute("rev", prev_item.getAttributeValue("rev"));
            item.addContent(property_prev);
        }
        
        Element property_cdate = new Element("Date");
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");        
        Date date = new Date();
        property_cdate.setText(dateFormat.format(date));
        item.addContent(property_cdate);
        
        Element property_payload = new Element("Payload");
        // Creates an unparented deep copy
        property_payload.addContent((Element) payload.clone());
        item.addContent(property_payload);
        
        itemlist.add(item);
        
        return item;
    } // add_payload()
    
    protected static Element resolve(List<Element> items, String id, int rev) {
        Element rval = null;
        try {
            for (Element elem : items) {
                String cur_id = elem.getAttributeValue("id");
                String s = elem.getAttributeValue("rev");
                int cur_rev = Integer.parseInt(s);
                if (cur_id.equals(id) && (cur_rev==rev)) {
                    return elem;
                }
            } // for each
        } catch(Exception ex) {
            throw new RuntimeException(
                "An unexpected error during resolve()", ex);
        } // try-catch
        
        return null;
    } // resolve()

    protected static Element resolve(List<Element> items, Xid xid) {
        return resolve(items, xid.id, xid.rev);
    } // resolve()
    
    protected static Xid identify(Element elem) {
        Xid rval = null;
        try {
            rval = XidIdentification.get_xid(elem);
        } catch(Exception ex) {
            throw new RuntimeException(String.format(
                "The XML Element %s has an invalid identification data: %s",
                XPathIdentification.get_xpath(elem), ex.getMessage()));
        } // try-catch
        return rval;
    } // identify()
    
    protected static void update_repository(
        Element root,
        List<Element> rset
    ) {
        for (Element elem : rset) {
            if (elem.getParent() != null) {
                continue;
            }
            // otherwise, add to the repository
            root.addContent(elem);
        } // for
    } // update_repository()
    
    protected static void inspect_repository(
        List<Element> rset
    ) {
        BufferedReader linereader = null;
        
        linereader = new BufferedReader(
            new InputStreamReader(System.in));
        
        // Quit flag; the main loop is halted when this is set to true.
        boolean quit = false;
        boolean expand = false;
        
        do {
            String line = null;
            System.out.printf("xid> ");
            // Read a command; this can also throw an exception
            try {
                line = linereader.readLine();
            } catch(IOException ex) {
                throw new RuntimeException(String.format(
                    "BufferedReader.readLine() failed"), ex);
            } // try-catch
            
            // TODO: parse_command()
            
            if (line.equals("stop") || line.equals("quit")) {
                quit = true;
            }
            else if (line.equals("expand")) {
                expand = true;
            }
            else if (line.equals("noexpand")) {
                expand = false;
            }
            else {
                // Otherwise, assume it is an XID
                int pos = line.indexOf(':');
                if (pos == -1) {
                    System.out.printf("Error: Syntax error\n");
                    continue;
                }
                String id = line.substring(0, pos).trim();
                String s = line.substring(pos+1).trim();
                int rev = -1;
                try {
                    rev = Integer.parseInt(s);
                } catch(Exception e) {
                    System.out.printf("Error: revision is not a number: %s\n", s);
                    continue;
                } // try-catch
                
                resolve_xid(rset, id, rev, expand);
                
            } // if-else
            
        } while (!quit);
        
    } // inspect_repository
    
    public static Element expand(List<Element> rset, Element payload) {
        Element rval = (Element) payload.clone();
        for (Object obj : rval.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            Element child = (Element) obj;
            String refid = child.getAttributeValue("ref_id");
            if (refid == null) {
                continue;
            }
            // Otherwise, split
            
        } // for
        return null;
    } // expand()
    
    public static void resolve_xid(
        List<Element> rset,
        String id,
        int rev,
        boolean expand
    ) {
        Element item = resolve(rset, id, rev);
        if (item == null) {
            System.out.printf("No such XID\n");
            return;
        }
        
        String cdate = null;
        String predecessor = null;
        Element elem = null;
        
        elem = item.getChild("Date");
        if (elem != null) {
            cdate = elem.getText();
        }
        elem = item.getChild("PrevItem");
        if (elem != null) {
            predecessor = String.format("%s:%d",
                elem.getAttributeValue("id"), elem.getAttributeValue("rev"));
        } // if
        
        elem = item.getChild("Payload");
        if (elem == null) {
            System.out.printf("Payload missing! Serious error\n");
            return;
        }
        
        Element payload = (Element) elem.getChildren().get(0);
        if (expand == true) {
            //payload = expand(rset, payload);
        }
        
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(System.out, "Cp1252");
            g_xmloutputter.output(payload, writer);
            System.out.printf("\n");
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        } // try-catch
    } // resolve_xid()
    
    
    // MAIN
    //======
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.printf("No arguments\n");
            System.exit(EXIT_SUCCESS);
        }
        
        Format fmt = Format.getCompactFormat();
        fmt.setIndent("  ");
        fmt.setEncoding("Cp1252");
        g_xmloutputter = new XMLOutputter(fmt);
        
        // Otherwise the first file is considered to be the repository
        // database XML file. If it doesn't exist, create it.
        File repofile = new File(args[0]);
        
        if (repofile.isFile() == false) {
            System.out.printf("Creating repository: %s\n", repofile.getPath());
            try {
                create_repository(repofile);
            } catch(Exception ex) {
                System.out.printf("Creation failed:\n");
                System.out.printf("%s\n", ex.getMessage());
                System.exit(EXIT_FAILURE);
            } // try-catch
        } // if
        
        // Attempt loading the repository
        Document doc = null;
        
        try {
            System.out.printf("Loading repository %s\n", repofile.getPath());
            doc = XMLFileHelper.deserialize_document(repofile);
        } catch(Exception ex) {
            System.err.printf("Load failed: %s\n", ex.getMessage());
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        // Attempt to parse the repository
        List<Element> rset; // The resolvable set
        rset = new LinkedList<Element>();
        Element root = doc.getRootElement();
        
        if (root.getName().equals("FidaRepository") == false) {
            System.err.printf("Error: not a repository\n");
            System.exit(EXIT_FAILURE);
        }
        
        if ((root.getAttribute("id") == null) 
            || (root.getAttribute("rev") == null)) 
        {
            System.err.printf("Error: Repository identification missing\n");
            System.exit(EXIT_FAILURE);
        }
        
        Xid repoxid = null;
        
        try {
            repoxid = XidIdentification.get_xid(root);
        } catch(Exception ex) {
            System.err.printf("Cannot identify the repository\n");
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        
        // Increase revision number. This may not always be neccessary,
        // but making the increasement here keeps the demo simple enough.
        // TODO: In a more advanced version this is more precisely controlled.
        repoxid.rev++;
        g_revision = repoxid.rev;
        root.setAttribute("rev", String.format("%d", g_revision));
        
        for (Object obj : root.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            } 
            Element child = (Element) obj;
            if (child.getName() != "FidaItem") {
                continue;
            }
            // Otherwise append to the list
            Xid xid = null;
            try {
                xid = XidIdentification.get_xid(child);
            } catch(Exception ex) {
                System.err.printf("Identification failed for XML element:\n");
                System.err.printf("%s\n", XPathIdentification.get_xpath(child));
                System.exit(EXIT_FAILURE);
            } // try-catch
            
            // Assert no such element is yet inserted
            if (resolve(rset, xid) != null) {
                System.err.printf("The repository is invalid; it contains an XML element\n");
                System.err.printf("with identity %s at least twice\n", xid.toString());
                System.exit(EXIT_FAILURE);
            } // if: already exists
            
            rset.add(child);
        } // for
        
        System.out.printf("Total %d repository items loaded\n", rset.size());
        
        if (args.length > 1) {
            
            for (int i = 1; i < args.length; i++) {
                File datafile = new File(args[i]);
                System.out.printf("Ingesting %s\n", datafile.getPath());
                
                // Attempt parsing the file
                Document datadoc = null;
                try {
                    datadoc = XMLFileHelper.deserialize_document(datafile);
                } catch(Exception ex) {
                    System.out.printf("Load failed\n");
                    System.out.printf("%s\n", ex.getMessage());
                    System.exit(EXIT_FAILURE);
                } // try-catch
                
                try {
                    populate_dfs(rset, null, datadoc.getRootElement());
                } catch(Exception ex) {
                    ex.printStackTrace();
                    System.exit(EXIT_FAILURE);
                } // try-catch
                
                // Re-serialize the repository and the ingested file
                // to reflect updates on the revision numbers
                try {
                    System.out.printf("Reserializing data file\n");
                    XMLFileHelper.serialize_document_verbatim(datadoc, datafile);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    System.exit(EXIT_FAILURE);
                } // try-catch
                
                update_repository(root, rset);
                
                try {
                    System.out.printf("Reserializing repository\n");
                    XMLFileHelper.serialize_document_formatted(doc, repofile);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    System.exit(EXIT_FAILURE);
                } // try-catch
            } // for: each file
        } else if (rset.size() > 0) {
            inspect_repository(rset);
        } // if-else
    } // main()
    
    protected static void populate_dfs(
        List<Element> rset,
        Element parent, 
        Element cnode
    ) 
        throws IOException
    {
        for (Object obj : cnode.getContent()) {
            // Depth-first
            if (obj instanceof Element) {
                Element child = (Element) obj;
                populate_dfs(rset, cnode, child);
            } // if
        } // for

        // And after all children, inspect 
        // the current node
        if (cnode.getAttribute("id") == null) {
            // No evolution line designator.
            // This is not our concern.
            return;
        }
        
        String id = cnode.getAttributeValue("id");
        boolean allowNew = false;
        
        String revstring = cnode.getAttributeValue("rev");
        if ((revstring == null) || revstring.equals("#")) {
            // A new element!
            // TODO: Determine if the evolution line designator
            // is already in use.
            cnode.setAttribute("rev", String.format("%d", g_revision));
            allowNew = true;
        } 
        else {
            // An old element! 
            // Has it been modified? Is the modification allowed?
        } // if-else
        
        Xid xid = identify(cnode);
        System.out.printf("Evaluating item %s\n", xid.toString());
        
        // normalize cnode
        Element cnode_normal = Normalization.normalize(cnode, null);
        
        // Seek the previous element
        Element prev_item = resolve(rset, xid);
        Element prev_payload = null;
        
        if (prev_item == null) {
            // a new element!
            // Is the evolution line designator available?
            if (allowNew == false) {
                throw new RuntimeException(String.format(
                    "The XML Element %s has invalid identification data\n",
                    XPathIdentification.get_xpath(cnode)));
            }
        } else {
            // There is already an element with the same xid.
            // Get it.
            prev_payload = get_payload(prev_item);
            // Determine if the stored and the current elements are 
            // contentually equal. If they are, then the current element
            // does not require any further actions, and it can be simply
            // ignored. On the other hand, if the current instance is not
            // equal to the stored instance, then it must be considered
            // a new revision of it.
            
            // The ref-by-xid elements must be normalized prior to 
            // the conteutal equivalence.
            
            List<Normalization.RefXidRecord> table_old;
            List<Normalization.RefXidRecord> table_new;
            table_old = Normalization.normalize_refs(prev_payload);
            table_new = Normalization.normalize_refs(cnode_normal);
            
            // Record the result of the contentual equivalence test
            boolean contentually_equal = 
                ContentualEq.equal(cnode_normal, prev_payload);
            
            // Denormalize the references back to as they were
            Normalization.denormalize_refs(table_old);
            Normalization.denormalize_refs(table_new);
            
            // If the elements were contentually equivalent after
            // the references were normalized, then the current instance
            // be just dropped and ignored
            if (contentually_equal) {
                // action: nop
                return;
            } // if
            
            // Otherwise, the current instance must be considered a new 
            // revision of the old one. What needs to be checked next
            // is the legality of the modification. The modification is 
            // considerd legal or allowed if and only if
            //      the lifeline designator (@id) of the old element 
            //      is a) active, b) still reserved for the same lifeline,
            //      and c) the old instance is the latest instance.
            //      (this means no branching from older revisions)
            
            // TODO ^^^
            
            // Currently, no such check is made, and the element is simply
            // considered as a new reivsion.
            
            // A new revision, because this is a modification
            // Assign the current revision to the modified element
            xid.rev = g_revision;
            
            // Propagate the revision number to the actual Element instance
            // also. This action gives the element a NEW identity which means
            // that a check for the existence and possibly the contentual
            // equivalence must be repeated for the new identity.
            System.out.printf("Updating the revision number to %d\n", g_revision);
            cnode.setAttribute("rev", String.format("%d", g_revision));
            cnode_normal.setAttribute("rev", String.format("%d", g_revision));
            
            // The same check as above have to be repeated with
            // the new identity.
            
            // Repeat resolution with the updated xid
            Element prev_item2 = resolve(rset, xid);
            
            // If there is already a stored instance of the newer revision,
            // then the possibilities are that 1) the current and 
            // the stored newer instance are equal in which case the current
            // instance is dropped, or 2) the current and the stored newer
            // instance are unequal in which case it is a conflict
            // that needs to be taken care of by the user.
            if (prev_item2 != null) {
                Element prev_payload2 = get_payload(prev_item2);

                
                table_old = Normalization.build_normalization_table(prev_payload2);
                
                Normalization.normalize_refs(table_old);
                Normalization.normalize_refs(table_new);
            
                // Record the result of the contentual equivalence test
                contentually_equal = 
                    ContentualEq.equal(cnode_normal, prev_payload2);
            
                // Denormalize the references back to as they were
                Normalization.denormalize_refs(table_old);
                Normalization.denormalize_refs(table_new);
            
                // If the elements were contentually equivalent after
                // the references were normalized, then the current instance
                // be just dropped and ignored
                if (contentually_equal) {
                    // action: nop
                    return;
                } // if
                
                // If the elements were contentually inequivalent,
                // it means that there is inconsitent data:
                // two differing newer revisions of the same old data.
                // The user must resolve the conflict between newer revisions.
                
                throw new RuntimeException(String.format(
                    "The updated element %s has already an inconsistent revision in the repository",
                    XPathIdentification.get_xpath(cnode)));
            } else {
                // No previous instance of the newer revision.
                // Good to go. The action will be that the element is
                // added to the repository
                // action: add
            } // if
            
            // TODO:
            // Manage a link between the new revision and the old revision
            // existing already in the repository. 
            // TODO: ^^ that is being already done in add_item()
        } // if-else
        
        // If this point is reached then the XML element is going
        // to be added to the repository.
        System.out.printf("Adding an item %s\n", xid.toString());
        
        // This is the point where each ref-by-xid element should be
        // assigned an identity.
        
        
        add_item(rset, cnode_normal, prev_item);
        
        
    } // populate_dfs()
    
} // class Fida
    
    