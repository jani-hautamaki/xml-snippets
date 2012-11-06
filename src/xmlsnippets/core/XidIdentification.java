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
    
    // CONSTANTS
    //===========
    
    public static final String
        ATTR_XID                                = "xid";
    
    public static final String
        ATTR_ID                                 = "id";

    public static final String
        ATTR_REVSTRING                          = "rev";

    public static final String
        ATTR_REVSPEC                            = "version";
    
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
        Attribute xid = elem.getAttribute(ATTR_XID);
        Attribute id = elem.getAttribute(ATTR_ID);
        Attribute rev = elem.getAttribute(ATTR_REVSTRING);
        Attribute spec = elem.getAttribute(ATTR_REVSPEC);
        
        // TODO
        // Allow possibility to ignore revspecs completetly.
        
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
        return get_xid(elem, false);
        /*
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
        */
    } // get_xid()

    public static Xid get_xid(
        Element elem, 
        boolean allow_missing_rev
    ) {
        Xid rval = null;
        
        String xidstring = elem.getAttributeValue(ATTR_XID);
        String id = elem.getAttributeValue(ATTR_ID);
        String revstring = elem.getAttributeValue(ATTR_REVSTRING);
        String revspec = elem.getAttributeValue(ATTR_REVSPEC);
        
        if ((xidstring == null)
            & (id == null) && (revstring == null) && (revspec == null))
        {
            // Unidentified XML element. Return null.
            // No identification at all
            return null;
        }
        
        // Whether or not noise version is allowed
        boolean force_revstring = false;
        
        // For these xid attributes to be valid, either @xid or @id
        // must be non null
        if (xidstring == null) {
            if (id == null) {
                throw new RuntimeException(String.format(
                    "%s: either @%s or %s must be specified when attribute @%s or @%s is present",
                    XPathIdentification.get_xpath(elem),
                    ATTR_XID, ATTR_ID, ATTR_REVSTRING, ATTR_REVSPEC));
            } // if: neither xidstring nor id
            
            // Assert that only either @rev or @version is present
            if ((revstring != null) && (revspec != null)) {
                throw new RuntimeException(String.format(
                    "%s: only either @%s or @%s must be specified, not both!",
                    XPathIdentification.get_xpath(elem),
                    ATTR_REVSTRING, ATTR_REVSPEC));
            } // if: both @rev and @version present
            
            if (revstring != null) {
                // revstring != null, revspec == null
                xidstring = String.format("%s:%s", id, revstring);
                force_revstring = true;
            } 
            else if (revspec != null) {
                // revstring == null, revspec != null
                xidstring = String.format("%s:%s", id, revspec);
            } 
            else {
                // both are null; only id present
                xidstring = String.format("%s", id);
            } // if-else
        }
        else {
            // xidstring != null
        } // if-else
        
        // Attempt to parse. May throw because the internal syntax
        // of the xid is incorrect;
        rval = XidString.deserialize(xidstring, allow_missing_rev);
        
        // Verify that @rev attribute didn't contain revspec
        if (force_revstring == true) {
            if ((rval.v_major != Xid.VERSION_INVALID)
                || (rval.v_minor != Xid.VERSION_INVALID))
            {
                throw new RuntimeException(String.format(
                    "%s: attribute @%s is not allowed to contain revspec: %s",
                    XPathIdentification.get_xpath(elem),
                    ATTR_REVSTRING, xidstring));
            } // if: has revspec
        } // if: revstring forced
        
        return rval;
    } // get_xid()
    
    public static Xid get_xid2(
        Element elem, 
        boolean allow_missing_rev
    ) {
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
            // At this line: either id or rev or both not-null.
            
            // Syntactic validity check:
            // Both @id and @rev must be present,
            // unless allow_missing_rev is enabled
            if ((id == null) ||
                ((allow_missing_rev == false) && (rev == null)))
            {
                // Error. Either one or the other is null.
                throw new RuntimeException(String.format(
                    "%s: both @id or @rev must be present, not just either one",
                    XPathIdentification.get_xpath(elem)));
            } // if: invalid
            
            // Convert the (id, rev) 2-tuple into XidString.
            // If the attributes were directly used to create Xid object,
            // their values wouldn't get validated.
            if ((allow_missing_rev == true) && (rev == null)) {
                xid = String.format("%s", id);
            } else {
                xid = String.format("%s:%s", id, rev);
            }
            
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
        rval = XidString.deserialize(xid, allow_missing_rev);
        
        return rval;
    } // get_xid()
    
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
        // It has to be taken into account that the element may not 
        // neccessarily have a previous @rev attribute value (nor @version).
        Attribute a_id = elem.getAttribute(ATTR_ID);
        Attribute a_revstring = elem.getAttribute(ATTR_REVSTRING);
        Attribute a_revspec = elem.getAttribute(ATTR_REVSPEC);
        
        // For convenience.
        boolean has_revspec;
        if ((xid.v_major != Xid.VERSION_INVALID)
            || (xid.v_minor != Xid.VERSION_INVALID))
        {
            has_revspec = true;
        } else {
            has_revspec = false;
        }
        
        if (a_id != null) {
            // Use the @id attribute immediately
            a_id.setValue(xid.id);
            
            // The revspec is also needed, but it is not yet clear
            // where it will be put. Anyway, calculate it already.
            String revspec = XidString.serialize_revspec(xid);
            
            // revision spec data requires more studying..
            if (a_revspec != null) {
                // This attribute can take both: revstring and revspec.
                // So it is good to go.
                a_revspec.setValue(revspec);
            }
            else if (a_revstring != null) {
                // This attribute takes only revstring, so it must
                // be made sure that the xid contains only revstring
                if (has_revspec == false) {
                    // Has only revstring. Good to go.
                    a_revstring.setValue(revspec);
                } else {
                    // Has a revspec (probably added), the attribute must 
                    // be switched! Remove the old attribute
                    elem.removeAttribute(a_revstring);
                    // And add revspec attribute instead
                    elem.setAttribute(ATTR_REVSPEC, revspec);
                } // if-else
            }
            else {
                // the element does not have neither attribute yet.
                if (has_revspec == false) {
                    // Has only revstring, so use @rev then
                    elem.setAttribute(ATTR_REVSTRING, revspec);
                } else {
                    // Has revspec, so use @version then.
                    elem.setAttribute(ATTR_REVSPEC, revspec);
                }
            } // if-else
        }
        else {
            // There might be a xid which will be updated or not.
            elem.setAttribute("xid", XidString.serialize(xid));
        } // if-else
        
        return elem;
    } // set_xid()

} // XidIdentification
