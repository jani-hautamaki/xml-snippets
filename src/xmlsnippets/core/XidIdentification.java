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
    
    // CLASS VARIABLES
    //=================
    
    /**
     * Flag indicating that the version attribute should be ignored
     * completetly while get_xid() and set_xid()
     */
    public static boolean g_ignore_version   = true;
    
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
            if ((g_ignore_version == true) || (revspec == null)) {
                // Unidentified XML element. Return null.
                // No identification at all.
                return null;
            } 
        } // if: no xid data
        
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
            if ((g_ignore_version == false)
                && (revstring != null) && (revspec != null)) 
            {
                // TODO:
                // Some option to drop either one?
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
            else if ((revspec != null) && (g_ignore_version == false)) {
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
            // Assert the element does nto have any additional attrs
            if ((id != null) || (revstring != null)) {
                throw new RuntimeException(String.format(
                    "%s: When @%s specified, neither @%s nor @%s is allowed!",
                    XPathIdentification.get_xpath(elem),
                    ATTR_XID, ATTR_ID, ATTR_REVSTRING));
            }
            if ((g_ignore_version == false) && (revspec != null)) {
                throw new RuntimeException(String.format(
                    "%s: When @%s specified, neither @%s, @%s nor @%s is allowed!",
                    XPathIdentification.get_xpath(elem),
                    ATTR_XID, ATTR_ID, ATTR_REVSTRING, ATTR_REVSPEC));
            } // if-else
            
            if (g_ignore_version == true) {
                force_revstring = true;
            }
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
        elem.removeAttribute(ATTR_ID);
        elem.removeAttribute(ATTR_REVSTRING);
        elem.removeAttribute(ATTR_XID);
        if (g_ignore_version == false) {
            elem.removeAttribute(ATTR_REVSPEC);
        }
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
            
            if (has_revspec == true) {
                // Get rid of @rev if it exists
                if (a_revstring != null) {
                    // remove old @rev attribute
                    elem.removeAttribute(a_revstring);
                }
                
                if (a_revspec != null) {
                    // Use existing attr
                    a_revspec.setValue(revspec);
                } else {
                    // Create new attr
                    elem.setAttribute(ATTR_REVSPEC, revspec);
                } // if-else
                
            } else {
                // Does not have revspec. It could be still put into
                // revspec attribute if it originally were there.
                // But if @version attribute is to be ignored, then
                // dont use it in any way
                if ((g_ignore_version == false) && (a_revspec != null)) {
                    // Yes put it there
                    a_revspec.setValue(revspec);
                } else {
                    // No, see if old @rev existed
                    if (a_revstring != null) {
                        a_revstring.setValue(revspec);
                    } else {
                        // NO previous @rev. Create.
                        elem.setAttribute(ATTR_REVSTRING, revspec);
                    } // if: has previous @rev
                } // if-else: had @version attribute already?
            } // if-else
        }
        else {
            // There might be a xid which will be updated or not.
            elem.setAttribute("xid", XidString.serialize(xid));
        } // if-else
        
        return elem;
    } // set_xid()

} // XidIdentification
