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
// jdom imports
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Content;

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
    
    private static Element normalize_child(Element child) {
        // Return variable
        Element rval = null;
        
        // Attempt identification
        Xid xid = XidIdentification.get_xid(child);
        
        if (xid != null) {
            // It is a xidentified child. Return value will be
            // a referencing copy. 
            // First, create an initial copy
            rval = new Element(child.getName(), child.getNamespace());
            
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
            rval = normalize(child);
            
            // Determine if the normalized copy has @ref_xid attribute
            // implying that this is an inclusion-by-xid element.
            // If that is the case, mark the inclusion-by-xid unverified.
            if (rval.getAttribute("ref_xid") != null) {
                // TODO: Mark unverified. Mark the inclusion-by-xid not
                // to be automatically expanded, since it was not pruned
                // by the normalization.
                rval.setAttribute("expand", "false");
            }
        } // if-else
        return rval;
    } // normalize_child()
    
    // TODO: normalize_content() ?
    public static Element normalize(Element elem) {
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
                Element child = (Element) obj;
                // Add the copy of child, no matter whether it is
                // a referencing copy or a normalized copy.
                rval.addContent(normalize_child(child));
            } 
            else {
                // Just make an identical copy of it
                Content content_orig = (Content) obj;
                Content content_copy = (Content) content_orig.clone();
                rval.addContent(content_copy);
            } // if-else: instance of Element
        } // for: each content object
        
        return rval;
    } // normalize()
    
} // class Normalization
