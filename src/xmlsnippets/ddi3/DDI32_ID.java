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
 * Represents an XML element's identity in the current DDI-Lifecycle 3.2 proposal.
 * For further information, see the Assembla repository 
 * <a href="https://www.assembla.com/code/ddi-tic-temp/subversion/nodes">here</a>.
 * The documentation used here is located in the repository's directory 
 * {@code /branches/proposed3.2/doc/}.
 *
 */
public class DDI32_ID 
{

    // MEMBER VARIABLES
    //==================
    
    /** The {@code @agency} of the closest MaintainableType parent element
     * having the attribute. */
    String maintainable_agency;
    
    /** The name of the closest MaintainableType parent element. */
    String maintainable_element;
    /** The {@code @id} of the closest MaintainableType parent element. */
    String maintainable_id;
    /** The {@code @version} of the closest MaintainableType parent element. */
    String maintainable_version;
    
    /** The name of the referenced IdentifiableType element. */
    String identifiable_element;
    /** The {@code @id} of the referenced IdentifiableType element. */
    String identifiable_id;
    /** The {@code @version} of the closest parent VersinoableType element. */
    String versionable_version;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Basic constructor.
     *
     * @param m_agency the agency of the XML element
     * @param m_element the XML element name of the closest MaintainableType
     * parent element
     * @param m_id the {@code @id} of the closest MaintainableType parent 
     * element
     * @param m_version the {@code @version} of the closest MaintainableType
     * parent element
     * @param i_element the XML element name of the XML element, 
     * if it is sub-MaintaianbleType
     * @param i_id the {@code @id} of the XML element,
     * if it is sub-MaintainableType
     * @param v_version the {@code @version} of the closest VersionableType
     * parent element, if the XML element itself is sub-MaintainableType.
     */
    public DDI32_ID(
        String m_agency,
        String m_element,
        String m_id,
        String m_version,
        String i_element,
        String i_id,
        String v_version
    ) {
        // Verify that the closest MaintainableType parent element's
        // information is properly set
        if ((m_agency == null) 
            || (m_element == null) || (m_id == null) || (m_version == null))
        {
            throw new IllegalArgumentException();
        } // if
        
        // Also verify that either the whole set of sub-maintainable information
        // is given or none
        if (((i_element == null) && (i_id == null) && (v_version == null))
            || ((i_element != null) && (i_id != null) && (v_version != null)))
        {
            // Accept
        } else {
            // Invalid set of data provided for the sub-maintainable element.
            throw new IllegalArgumentException();
        } // if-else
        
        // Copy the information: parent Maintainabletype
        maintainable_agency = m_agency;
        maintainable_element = m_element;
        maintainable_id = m_id;
        maintainable_version = m_version;
        
        // Copy the information: sub-maintainable, if any
        identifiable_element = i_element;
        identifiable_id = i_id;
        versionable_version = v_version;
    } // ctor
    
    // OTHER METHODS
    //===============
    
    /**
     * Compares for equivalence.
     *
     * @return {@code true} if both objects are equal on all parts.
     * Otherwise, {@code false} is returned.
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } // if: null
        
        DDI32_ID x = null;
        if (obj instanceof DDI32_ID) {
            x = (DDI32_ID) obj;
        } else {
            // Incorrect dynamic type
            return false;
        }
        
        if (maintainable_agency.equals(x.maintainable_agency)
            && maintainable_element.equals(x.maintainable_element)
            && maintainable_id.equals(x.maintainable_id)
            && maintainable_version.equals(x.maintainable_version))
        {
            // First part matches OK.
        } else {
            // Mismatch in the first part.
            return false;
        } // if-else
        
        // Determine whether they have second parts.
        if ((identifiable_element == null)
            && (x.identifiable_element == null)) 
        {
            // Both are MaintainableType, and don't have sub-maintainable
            // second parts. Comparison OK.
            return true;
        }
        
        if ((identifiable_element != null) 
            && (x.identifiable_element != null)) 
        {
            // Both have a sub-MaintainableType part. 
            // Compare that next.
        } else {
            // One of the DDI32_ID objects has a sub-MaintaianbleType
            // second part, and the other has not.
            return false;
        } // if-else
        
        if (identifiable_element.equals(x.identifiable_element)
            && identifiable_id.equals(x.identifiable_id)
            && versionable_version.equals(x.versionable_version))
        {
            // Second part matches OK
            return true;
        } // if-else
        
        // Otherwise, the second part had a mismatch.
        
        return false;
    } // equals()
    
    public String toString() {
        return DDI32_URN.serialize(this);
    }

    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.printf("No input urn\n");
            return;
        }
        
        System.out.printf("Input urn: <%s>\n", args[0]);
        
        try {
            DDI32_ID id = DDI32_URN.deserialize(args[0]);
            System.out.printf("Parse ok.\nURN: %s\n", id.toString());
        } catch(Exception e) {
            System.out.printf("Parse error: %s\n", e.getMessage());
        } // try-catch
    } // main()
    
} // class DDI32_ID