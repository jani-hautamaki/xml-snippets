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

package xmlsnippets.core;

// java imports
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
// jdom imports
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Content;
// xmlsnippets imports
import xmlsnippets.util.XPathIdentification;
import xmlsnippets.core.XidIdentification;

/**
 * Methods to perform normalization of an XML element
 * 
 */
public class Normalization
{
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally disabled.
     */
    private Normalization() {
    } // ctor
    
    // CLASS METHODS
    //===============
    
    /**
     * Either creates an inclusion-by-xid element or normalizes
     * the child element. 
     * @param child [in] the child element which is to be normalized
     * @param map [out] collects information regarding mappings between
     * the created inclusion-by-reference elements and their original
     * counter-parts. If left {@code null} the informatino is not collected.
     * The keys are elements in returned element, and the values are
     * elements of the specified child element.
     * @return Either a normalized child element or an inclusion-by-xid element.
     */
    private static Element normalize_child(
        Element child,
        Map<Element, Element> map
    ) {
        // Return variable
        Element rval = null;
        
        // Attempt identification
        Xid xid = XidIdentification.get_xid(child);
        
        if (xid != null) {
            // It is a xidentified child. Return value will be
            // a referencing copy. 
            // First, create an initial copy
            rval = new Element(child.getName(), child.getNamespace());
            
            // Record the connection between "rval" and "child" into some
            // data structure. That information is needed later.
            // Element.equals(x) is simply referential equivalence,
            // which is the same as this==x. That is exactly what I need.
            if (map != null) {
                map.put(rval, child);
            }
            
            // Then, make it a referencing copy by setting the attribute
            // signaling inclusion-by-xid properly
            rval.setAttribute("ref_xid", XidString.serialize(xid));
            
            // TODO: Mark the inclusion-by-xid to be expanded automatically,
            // since it was automatically pruned.
            rval.setAttribute("expand", "true");
        } else {
            // Otherwise, the child itself is unidentifiable.
            // It can either be an inclusion-by-xid referencing copy,
            // or just plain unxidentifiable element.
            
            // If the element is inclusion-by-xid, the reference
            // should be verified at some point.
            
            // If the element is just a plain unidentifiable element, 
            // nothing prevents its children to be identifiable again. 
            // Consequently, the element's contents must be recursively 
             // normalized
            rval = normalize(child, map);
            
            // Determine if the normalized copy has @ref_xid attribute
            // implying that this is an inclusion-by-xid element.
            // If that is the case, mark the inclusion-by-xid unverified.
            if (rval.getAttribute("ref_xid") != null) {
                // TODO: Mark unverified. Mark the inclusion-by-xid not
                // to be automatically expanded, since it was not pruned
                // by the normalization.
                rval.setAttribute("expand", "false");
                
                // Record the connection between "rval" and "child"
                if (map != null) {
                    map.put(rval, child);
                }
            } // if: the child was an ref-by-xid element
        } // if-else
        return rval;
    } // normalize_child()
    
    /**
     * Returns an unparented, normalized deep-copy of the specified element.
     *
     * @param elem [in] the element to be normalized
     * @param map [out] a map which will be populated by the information
     * regarding which elements were pruned and replaced with what. If
     * {@code null} the information will not be collected.
     * @return The normalized, unparented deep-copy.
     * 
     * TODO: rename into normalize_content() ?
     */
    public static Element normalize(
        Element elem,
        Map<Element, Element> map
    
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
                rval.addContent(normalize_child(child, map));
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
    } // normalize()
    
    //========================================================================
    // TODO: The following code should probably belong to somewhere else
    //========================================================================
    
    public static class RefXidRecord {
        
        // MEMBER VARIABLES
        //==================
        
        /**
         * The inclusion-by-xid element to which the information applies to.
         */
        public Element element;
        
        /**
         * The value of the {@code @expand} attribute
         */
        public boolean expand;
        
        /**
         * The inclusion-by-xid element's own xid information, if any.
         */
        public Xid xid;
        
        // CONSTUCTORS
        //=============
        
        /**
         * Default constructor
         */
        public RefXidRecord(Element elem) {
            element = elem;
            expand = true;
            xid = null;
        } // ctor
    } // class RefXidRecord
    
    /**
     * Rips off the inclusion-by-xid expansion and identification information
     * @return de/normalization table
     */
    public static List<RefXidRecord> normalize_refs(
        Element element
    ) {
        List<RefXidRecord> table = build_normalization_table(element);
        normalize_refs(table);
        return table;
    } // normalize_ref()

    public static List<RefXidRecord> build_normalization_table(
        Element element
    ) {
        List<RefXidRecord> table = new LinkedList<RefXidRecord>();
        return build_normalization_table(table, element);
    } // build_normalization_table()
    
    protected static List<RefXidRecord> build_normalization_table(
        List<RefXidRecord> table,
        Element element
    ) {
        for (Object obj : element.getContent()) {
            if ((obj instanceof Element) == false) {
                // Not a child, skip to next.
                continue;
            }
            Element child = (Element) obj;
            // Depth-first recurse
            table = build_normalization_table(table, child);
        } // for: each child
        
        if (element.getAttribute("ref_xid") != null) {
            // The element itself is a inclusion-by-xid.
            // Create a record for the element
            RefXidRecord record = new RefXidRecord(element);
            
            // If no link_xid, returns null.
            String linkxid = element.getAttributeValue("link_xid");
            if (linkxid != null) {
                record.xid = XidString.deserialize(linkxid);
            } else {
                record.xid = null;
            }
            
            // Drop the xid, if any.
            //XidIdentification.unset_xid(element);
            
            // Pick the expand attribute
            String expand = element.getAttributeValue("expand");
            
            if (expand == null) {
                record.expand = true;
            }
            else if (expand.equals("true")) {
                record.expand = true;
            } else if (expand.equals("false")) {
                record.expand = false;
            } else {
                // Invalid value!
                throw new RuntimeException(String.format(
                    "%s: the attribute @expand must be either \"true\" or \"false\"",
                    XPathIdentification.get_xpath(element)));
            } // if-else
            
            // Drop the expand attribute
            //element.removeAttribute("expand");
            
            // Record is ready to be added
            table.add(record);
            
        } // if: the element is incl-by-xid
        
        return table;
    } // normalize_refs()
    
    public static void normalize_refs(List<RefXidRecord> table) {
        for (RefXidRecord record : table) {
            // Local for convenience; avoids double dot expressions.
            Element element = record.element;
            // Drop the xid, if any.
            element.removeAttribute("link_xid");
            //XidIdentification.unset_xid(element);
            // Drop the expand attribute
            element.removeAttribute("expand");
        } // for: each record
    } // normalize_refs()
    
    public static void denormalize_refs(List<RefXidRecord> table) {
        for (RefXidRecord record : table) {
            Element element = record.element;
            // Set @expand attribute
            /*
            if (record.expand == true) {
                element.setAttribute("expand", "true");
            } else {
                element.setAttribute("expand", "false");
            } // if-else
            */
            
            // Set xid (this will convert (@id,@rev) pairs to @xid
            if (record.xid != null) {
                //XidIdentification.set_xid(record.element, record.xid);
                element.setAttribute("link_xid", 
                    XidString.serialize(record.xid));
                //XidIdentification.set_xid(element, record.xid);
            } // if
        } // for
    } // denormalize_refs()
    
} // class Normalization
