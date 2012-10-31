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

// jdom imports
import org.jdom.Element;
import org.jdom.Attribute;
// xmlsnippets imports
import xmlsnippets.util.XPathIdentification;
import xmlsnippets.core.XidString;

/**
 * Methods for Xid identification of an XML element.
 * 
 */
public class XidIdentification
{
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally disabled.
     */
    private XidIdentification() {
    } // ctor
    
    // CLASS METHODS
    //===============
    
    // TODO: has_valid_xid?
    public static boolean has_xid(Element elem) {
        if (elem.getAttribute("xid") != null) {
            return true;
        }
        
        if ((elem.getAttribute("id") != null) 
            && (elem.getAttribute("rev") != null)) 
        {
            return true;
        } 
        
        return false;
    } // is_identifiable()
        
    public static Xid get_xid(Element elem) {
        Xid rval = null;
        
        String xid = elem.getAttributeValue("xid");
        String id = elem.getAttributeValue("id");
        String rev = elem.getAttributeValue("rev");
        
        if (xid == null) {
            // No @xid. See if there's any identification information at all.
            if ((id == null) && (rev == null)) {
                // no xid at all
                return null;
            } // if
            
            // There is either @id or @rev or both.
            // Make sure that there are both
            if ((id == null) || (rev == null)) {
                throw new RuntimeException(String.format(
                    "%s: both @id or @rev must be present, not just either one",
                    XPathIdentification.identify(elem)));
            }
            // Convert the (id, rev) 2-tuple into XidString.
            // If the attributes were directly used to create Xid object,
            // their values wouldn't get validated.
            xid = String.format("%s:%s", id, rev);
        } else {
            // Make sure that no simultaneous @id or @rev exists
            if ((id != null) || (rev != null)) {
                throw new RuntimeException(String.format(
                    "%s: either @xid or the pair (@id, @rev) must be present, but not both",
                    XPathIdentification.identify(elem)));
            } // if
        } // if-else: no xid?
        
        // Attempt to parse. May throw
        rval = XidString.deserialize(xid);
        
        return rval;
    } // identify()
    
    public static Element unset_xid(Element elem) {
        elem.removeAttribute("id");
        elem.removeAttribute("rev");
        elem.removeAttribute("xid");
        return elem;
    } // unset_xid()
    
    public static Element set_xid(Element elem, Xid xid) {
        // Determine whether the element has a previous identification.
        Attribute a_id = elem.getAttribute("id");
        Attribute a_rev = elem.getAttribute("rev");
        
        if ((a_id != null) && (a_rev != null)) {
            a_id.setValue(xid.id);
            a_rev.setValue(String.format("%d", xid.rev));
        }
        else {
            // There might be a xid which will be updated or not.
            elem.setAttribute("xid", XidString.serialize(xid));
        } // if-else
        
        return elem;
    } // set_xid()

} // XidIdentification
