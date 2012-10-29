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

package xmlsnippets.ddi3;

// imports
import java.util.List;
import org.jdom.Element;
import org.jdom.DataConversionException;

import xmlsnippets.ddi3.DDI31_ID;

/**
 * Contains the methods for identification and resolution of DDI-Lifecycle 3.1
 * XML elements. The methods 
 * {@link #identify_DDI31(Element)} and 
 * {@link #resolve_DDI31(List, DDI31_ID)} (TODO)
 * correspond to the theoretical identification and resolution functions 
 * respectively.<p>
 * 
 * The specification for DDI-Lifecycle 3.1 is available at 
 * <a href="http://www.ddialliance.org/Specification/DDI-Lifecycle/3.1/">here</a>
 * <p>
 *
 */
public class DDI31
{
    // CONSTRUCTORS
    //==============
    
    /** Constructor disabled, instantiation is unallowed. */
    private DDI31() {
        // unallowed
    } // ctor
    
    // HELPER METHODS
    //================
    
    /**
     * Takes into account the default version number {@code "1.0.0"}
     * if the version attribute does not exist.
     * 
     * @param version the value of the {@code @version} attribute, or 
     * {@code null} if the element does not have such an attribute.
     *
     * @return the version number
     */
    protected static String get_actual_version(String version) {
        // Yeah, remember the value of version is assumed
        // to be 1.0.0 if it is not stated.
        if (version == null) {
            // default
            version = "1.0.0";
        } // if: no version provided
        
        return version;
    } // get_actual_version()
    
    // OTHER METHODS
    //===============
    
    /** 
     * Creates {@link DDI31_ID} object representing the XML element's 
     * identity in terms of DDI-Lifecycle 3.1.
     * 
     * @param x the element for which the identity object is built
     * 
     * @return the {@link DDI31_ID} object representing the identity.
     */
    public static DDI31_ID identify_DDI31(Element x) 
        throws DataConversionException
    {
        String agency_name = null;
        String maintainable_name = null;
        String maintainable_version = null;
        String identifiable_name = null;
        String versionable_version = null;
        
        // Fill the identifiable name if the element is sub-maintainable
        if ((x.getAttribute("isIdentifiable") != null)
            || (x.getAttribute("isVersionable") != null))
        {
            identifiable_name = String.format(
                "%s.%s", 
                x.getName(),
                x.getAttributeValue("id")
            ); // format()
            
            // Find the parent versionable 
            // Note: maintainable is-a versionable.
            while ((x != null) && 
                (x.getAttribute("isVersionable") == null)
                && (x.getAttribute("isMaintainable") == null))
            {
                x = x.getParentElement();
            } // while: not null and non-identifiable
            assert x != null;
            
            versionable_version = get_actual_version(
                x.getAttributeValue("version")
            ); // get_actual_version()
            
                
        } // if: sub-maintainable
        
        // Find the parent maintainable
        while ((x != null) &&
            (x.getAttribute("isMaintainable") == null))
        {
            x = x.getParentElement();
        } // while: not null and non-maintianable
        assert x != null;
        
        maintainable_name = String.format(
            "%s.%s", 
            x.getName(),
            x.getAttributeValue("id")
        ); // format()
        
        maintainable_version = get_actual_version(
            x.getAttributeValue("version")
        ); // get_actual_version()
        
        // Find agency
        while ((x != null) 
            && (x.getAttribute("agency") == null)) 
        {
            x = x.getParentElement();
        }
        assert x != null;
        
        agency_name = x.getAttributeValue("agency");
        
        return new DDI31_ID(
            agency_name,
            maintainable_name, maintainable_version,
            identifiable_name, versionable_version
        ); // return
    } // identify_DDI31()
    
    
    /**
     * (TODO) Resolves an identity {@code id} to the corresponding XML 
     * element {@code x} in a given resolvable set {@code s}. 
     * Not implemented yet.
     * 
     * @param s the resolvable set of XML elements.
     * @param id the identity of the element which is to be resolved
     *
     * @return a list of all matching XML elements (there should be 
     * exactly one).
     */
    public static List<Element> resolve_DDI31(
        List<Element> s, 
        DDI31_ID id
    ) {
        // TODO
        return null;
    } // resolve_DDI31()
    
} // class DDI31
