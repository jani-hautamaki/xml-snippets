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

/**
 * An identity data container object for the xml-snippet's 
 * identification system.
 * 
 */
public class Xid
{
    // CONSTANTS
    //===========
    
    /**
     * Value indicating that {@code id} is not valid
     */
    public static final String INVALID_ID               = null;
    
    /**
     * Value indicating that {@code rev} is not valid
     */
    public static final int INVALID_REV                 = -1;
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * The value of @id attribute, or INVALID_ID if not valid.
     */
    public String id;
    
    /**
     * The value of @rev attribute, or INVALID_REV if not valid
     */
    public int rev;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Creation with specified values for {@code id} and {@code rev}.
     * The values are assigned to member variables as such. No validation
     * whatsoever is done.
     *
     * @param id specifies the value of the corresponding member variable
     * @param rev specifies the value of the corresponding member variable
     */
    public Xid(String id, int rev) {
        if (id == null) {
            throw new NullPointerException();
        } // if: null
        
        this.id = id;
        this.rev = rev;
    } // ctor
    
    // OTHER METHODS
    //===============
    
    // JAVA OBJECT OVERRIDES
    //=======================

    /**
     * Converts the identity to a simple string representation).
     */
    public String toString() {
        return String.format("(id=%s, rev=%d)",  id, rev);
    } // toString()
    
    /**
     * Tests the equivalence of {@code Xid} objects.
     * 
     * @return {@code true} if both {@link #id} and {@link #rev}
     * are equal. Otherwise, {@code false} is returned.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            System.out.printf("other was null\n");
            return false;
        }
        // other != null
        if (other instanceof Xid) {
            Xid xid = (Xid) other;
            if (this.id.equals(xid.id) && (this.rev == xid.rev)) {
                return true;
            } // if: equal
            // Otherwise theuy are unequal
        } // if: correct dynamic type
        
        // Either unequal or not an instancof the class at all.
        return false;
    } // equals()
    
    /**
     * Hash code corresponding to the overrided equals() method.
     *
     * @return Value {@code (rev << 24) | (id.hashCode() & 0x00ffffff)}.
     */
    @Override
    public int hashCode() {
        return (rev << 24) | (id.hashCode() & 0x00ffffff);
    } // hashCode()
    
} // class Xid