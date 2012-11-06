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
 * Methods for manipulating Xid identification of an XML element.
 * This class provides the interface to read, write, alter and delete
 * the xid identification of an XML element. The identification attributes
 * shouldn't accessed directly. This class should be used instead.
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
    
    /**
     * Determines the syntactic validity of an XML element from 
     * the Xid point of view. The method retuns true if one and only one 
     * of the following conditions hold:
     * <ul>
     *      <li>No {@code @xid}, {@code @id} nor {@code @rev} attribute present.
     *      <li>Has {@code @xid}, but not {@code @id} nor {@code @rev} attribute.
     *      <li>Has {@code @id} and {@code @rev}, but not {@code @xid} attribute.
     * </ul>
     *
     * That is, either only {@code @xid} or the pair {@code @id} and {@code @rev}
     * must be present, but not both.
     *
     * @param elem the element whose xid validity is tested
     * @return {@code true} if one and only one of the above conditions hold.
     * Otherwise, {@code false} is returned.
     */
    public static boolean is_valid(Element elem) {
        // First, pick all attributes
        Attribute xid = elem.getAttribute("xid");
        Attribute id = elem.getAttribute("id");
        Attribute rev = elem.getAttribute("rev");
        Attribute version = elem.getAttribute("version");
        
        if ((xid == null) && (id == null) && (rev == null)) {
            return true;
        }
        else if ((xid != null) && (id == null) && (rev == null)) {
            return true;
        }
        else if ((xid == null) && (id != null) && (rev != null)) {
            return true;
        }
        
        return false;
    } // is_valid()
    
    /**
     * Attempts to get xid identification for the XML element.
     *
     * @param elem the XML element which is to be identified
     * @return {@code Xid} for the element, or {@code null} if
     * the element does not have any identification information
     *
     * @throws RuntimeException If the XML element is not valid with
     * respect to {@link #is_valid(Element)}. The error message gives
     * more detailed information which constraint is being violated.
     *
     */
    public static Xid get_xid(Element elem) {
        Xid rval = null;
        
        String xid = elem.getAttributeValue("xid");
        String id = elem.getAttributeValue("id");
        String rev = elem.getAttributeValue("rev");
        
        if (xid == null) {
            // No @xid.
            // See if the (id, rev) pair is also nonexistent.
            if ((id == null) && (rev == null)) {
                // Unidentified XML element. Return null.
                return null;
            } // if
            
            // Syntactic validity check:
            // Both @id and @rev must be present
            if ((id == null) || (rev == null)) {
                // Error. Either one or the other is null.
                throw new RuntimeException(String.format(
                    "%s: both @id or @rev must be present, not just either one",
                    XPathIdentification.get_xpath(elem)));
            } // if: invalid
            
            // Convert the (id, rev) 2-tuple into XidString.
            // If the attributes were directly used to create Xid object,
            // their values wouldn't get validated.
            xid = String.format("%s:%s", id, rev);
            // Cannot be done with the .serialize() operatin below, because
            // it would require converting the rev string into an integer.
            //xid = XidString.serialize(id, rev); // Unusable
            
        } else {
            // Has @xid.
            // Check that the (id, rev) pair is nonexistent.
            if ((id != null) || (rev != null)) {
                // Error. There is @xid, but there is also @id or @rev or both.
                throw new RuntimeException(String.format(
                    "%s: either @xid or the pair (@id, @rev) must be present, but not both",
                    XPathIdentification.get_xpath(elem)));
            } // if
        } // if-else: no xid?
        
        // Attempt to parse. May throw because the internal syntax
        // of the xid is incorrect;
        rval = XidString.deserialize(xid);
        
        return rval;
    } // identify()
    
    /**
     * Removes all xid information from an XML element.
     *
     * TODO: Consider the return value to be the dropped xid, if any?
     * This could be utilized in the building of the normalization table.
     *
     * @param elem the element from which to remove the xid information
     * @return The {@code elem} parameter for convenience.
     */
    public static Element unset_xid(Element elem) {
        elem.removeAttribute("id");
        elem.removeAttribute("rev");
        elem.removeAttribute("xid");
        return elem;
    } // unset_xid()
    
    /**
     * Assigns the given xid information to an XML element.
     * The function will exploit the existing xid representation,
     * if there is any. That is, if the element already has the ({@code @id}, 
     * {@code @rev}) pair, then the xid information will be overwritten into 
     * those. If the element has {@code @xid} instead, then the xid information
     * will be overwritten into that. If the element does not have any
     * prior xid information, the {@code @xid} will be used.
     * 
     * @param elem the element to which to set the xid information
     * @param xid the xid information which is to be set to the element.
     * @return The {@code elem} parameter for convenience.
     */
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
