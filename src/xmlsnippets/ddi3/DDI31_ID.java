
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

/**
 * A representation of an XML element {@code x}'s identity {@code id=f(x)} in 
 * the set ID, in the case of DDI-Lifecycle 3.1. One possible implementation.
 * <p>
 * The specification for DDI-Lifecycle 3.1 is available at 
 * <a href="http://www.ddialliance.org/Specification/DDI-Lifecycle/3.1/">here</a>
 * <p>
 * To construct a resolution function, the internal structure of
 * {@link #maintainable_name} and {@link #identifiable_name} would have to be
 * parsed in order to separate the element's name from its id. This could be
 * done prior to creating the element's identification. It is a possible 
 * future optimization for this class.
 */
public class DDI31_ID {
    
    // MEMBER VARIABLES
    //==================
    
    /** 
     * Attribute {@code @agency} of the closest MaintainableType 
     * parent having the attribute. Example: {@code "fi.dsf"}. 
     */
    public String agency_name;
    
    /**
     * Attribute {@code @id} of the closest MaintainableType parent.
     * Includes the element's name. Example: {@code "CategoryScheme.CS1"}. 
     */
    public String maintainable_name;
    
    /** 
     * Attribute {@code @version} of the closest MaintainableType parent. 
     * Example: {@code "1.0.0"}. 
     */
    public String maintainable_version;
    
    /**
     * Attribute {@code @id} of the element itself.
     * Includes the element's name. Example: {@code "TimeMethod.TM1"}. 
     */
    public String identifiable_name;
    
    /**
     * Attribute {@code @version} of the closest VersionableType parent. 
     * Example: {@code "2.3.4"}.
     */
    public String versionable_version;
    
    // CONSTRUCTORS
    //==============
    
    /** 
     * Basic Constructor. Assumes the inputs to have sensible values.
     *
     * @param agency the agency name. Example: {@code "fi.dsf"}.
     *
     * @param mname the MaintainableType element name, including the XML 
     * Schema type name.
     *
     * @param mversion the version number of the MaintainableType element.
     *
     * @param iname the IdentifiableType element name, including the XML
     * Schema type name, within the MaintainableType element. If the element 
     * is MaintainableType, leave {@code null}. 
     *
     * @param vversion the version number from the next VersionableType
     * parent element. If the element is MaintainableType, leave {@code null}.
     */
    public DDI31_ID(
        String agency,
        String mname,
        String mversion,
        String iname,
        String vversion
    ) {
        agency_name = agency;
        maintainable_name = mname;
        maintainable_version = mversion;
        identifiable_name = iname;
        versionable_version = vversion;
    } // ctor
    
    
    // OTHER MEMBER VARIABLES
    //========================
    
    /**
     * Converts the identity into its standard String representation.
     * <p>
     * Example: for an IdentifiableType or VersionableType element,<br/>
     * {@code "fi.dsf:CategoryScheme.ISCO_2012.1.2.3:TimeMethod.TM_1.1.0.0"}.
     * <p>
     * Example: for an MaintainableType element,<br/>
     * {@code "fi.dsf:CategoryScheme.ISCO_2012.1.2.3"}.
     *
     * @return the standard representation of the identity
     */
    public String toString() {
        if (identifiable_name != null) {
            // sub-maintainable
            return String.format("%s:%s.%s:%s.%s",  
                agency_name,
                maintainable_name,
                maintainable_version,
                identifiable_name,
                versionable_version
            );
        } else {
            // maintainable
            return String.format("%s:%s.%s",  
                agency_name,
                maintainable_name,
                maintainable_version
            );
        } // if-else: sub-maintainable?
    } // toString()
    
    /** 
     * Test equivalence of the identity objects.
     *
     * @return {@code true} if all parts match. Otherwise, {@code false}.
     */
    public boolean equals(Object other) {
        // Null object. Return false immediately, and
        // don't bother to cast. 
        if (other == null) {
            return false;
        }
        
        // Here: other != null
        
        // Non-null object. Determine if it is an instance
        // of DDI31_ID class. If so, do dynamic cast
        DDI31_ID id = null;
        if (other instanceof DDI31_ID) {
            // An instance of this class; dynamic cast
            id = (DDI31_ID) other;
        } else {
            // Not an instance of this class. Return false.
            return false;
        } // if-else
        
        // Compare the first part: agency + maint_name + maint_version
        // compare first part
        if (this.agency_name.equals(id.agency_name)
            && this.maintainable_name.equals(id.maintainable_name)
            && this.maintainable_version.equals(id.maintainable_version))
        {
            // All equal.
            // This is still inconclusive, so more tests are needed.
        } else {
            // Mismatch in the first part. Return false.
            return false;
        } // if-else
        
        // Determine whether both objects have null second parts
        
        if ((this.identifiable_name == null)
            && (id.identifiable_name == null)) 
        {
            // Both are maintainables; no versionable/identifiable part.
            // Since the first part was equal and there is no second part,
            // the objects are equal.
            return true;
        } // if
        
        // Check if one is sub-maintainable and the other is maintainable.
        if (((this.identifiable_name == null)
            && (id.identifiable_name != null))
            ||
            ((this.identifiable_name != null)
            && (id.identifiable_name == null)))
        {
            // One is MaintainableType and the other is sub-Maintainabletype.
            // The objects cannot match, so they are unequal.
            return false;
        } // if
        
        // Both objects are identities of a sub-MaintainableType,
        // and therefore both have non-null second part.
        // Compare the second part: identifiable_name + versionable_version
        if (this.identifiable_name.equals(id.identifiable_name)
            && this.versionable_version.equals(id.versionable_version))
        {
            // Second part matches too, so the objects are equal!
            return true;
        }
        
        // Otherwise mismatches in the versionable/identifiable part,
        // and the objects are unequal.
        
        return false;
    } // equals()
} // class DDI31_ID

