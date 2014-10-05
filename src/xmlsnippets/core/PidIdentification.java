//*******************************{begin:header}******************************//
//               fida - https://code.google.com/p/xml-snippets/
//***************************************************************************//
//
//      fida: an XML Revision Tracking and Version Control Software.
//
//      Copyright (C) 2012-2014 Jani Hautamaki <jani.hautamaki@hotmail.com>
//
//      Licensed under the terms of GNU General Public License v3.
//
//      You should have received a copy of the GNU General Public License v3
//      along with this program as the file LICENSE.txt; if not, please see
//      http://www.gnu.org/licenses/gpl-3.0.html
//
//********************************{end:header}*******************************//

package xmlsnippets.core;

// core java
import java.util.List;

// jdom imports
import org.jdom.Element;
import org.jdom.Attribute;
// xmlsnippets imports
import xmlsnippets.util.XPathIdentification;

/**
 * Methods for manipulating Xid identification of an XML element.
 * This class provides the interface to read, write, alter and delete
 * the xid identification of an XML element. The identification attributes
 * shouldn't accessed directly. This class should be used instead.
 * 
 */
public class PidIdentification
{
    
    // CONSTANTS
    //===========
    
    /**
     * Name of the attribute holding property id
     */
    public static final String
        ATTR_PID                                = "a";
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally disabled.
     */
    private PidIdentification() {
    } // ctor
    
    // CLASS METHODS
    //===============
    
    /**
     * Attempts to get property id for the XML element.
     *
     * @param elem the XML element which is to be identified
     * @return Property id for the element, or {@code null} if
     * the element is not identified as a property.
     *
     */
    public static String get_pid(Element elem) {
        String pid = elem.getAttributeValue(ATTR_PID);
        
        return pid;
    } // get_xid()
    
    public static void unset_pid(Element elem) {
        elem.removeAttribute(ATTR_PID);
    } // unset_xid()
    
    /**
     * Assigns the given property id information to an XML element.
     * 
     * @param elem the element to which to set the pid information
     * @param pid the pid information which is to be set to the element.
     * @return The {@code elem} parameter for convenience.
     */
    @SuppressWarnings("unchecked")
    public static Element set_pid(Element elem, String pid) {
        Attribute a_pid = elem.getAttribute(ATTR_PID);
        
        if (a_pid != null) {
            // Use the pid attribute immediately
            a_pid.setValue(pid);
        }
        else {
            a_pid = new Attribute(ATTR_PID, pid);
            
            // NOTE NOTE NOTE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // This PREPENDS the attribute with jdom v1.1.3, but 
            // the v1.1.3 documentation clearly states that the 'live' list 
            // contains the attributes in *NO PARTICULAR ORDER*. 
            // So, in theory adding an attribute to list could simply be 
            // translated into a call to Element.setAttribute().
            
            // Get a live list. 
            List attrs = elem.getAttributes();
            // Prepend the pid
            attrs.add(0, a_pid); // UNCHECKED CAST
            
            //elem.setAttribute(ATTR_PID, pid);
        } // if-else
        
        return elem;
    } // set_xid()

} // XidIdentification
