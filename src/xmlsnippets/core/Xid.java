//*******************************{begin:header}******************************//
//     XML Processing Snippets - https://code.google.com/p/xml-snippets/     //
//***************************************************************************//
//
//      xml-snippets:   XML Processing Snippets 
//                      with Some Theoretical Considerations
//
//      Copyright (C) 2012 Jani Hautamaki <jani.hautamaki@hotmail.com>
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
 * identification system. Each xid contains an identifier {@link #id}
 * and a revision number {@link #rev}. There are also containers for
 * major and minover version numbers {@link #v_major} and {@link v_minor}.
 * The equivalence of {@code Xid} objects is based on ({@code id}, {@code rev})
 * tuples. If both the id and the rev matches, then xids are considered equal.
 * The version numbers are just payload noise, and they do not affect 
 * the equivalence in any way. They can be utilized or just ignored.
 * 
 */
public class Xid
{
    // CONSTANTS
    //===========
    
    /**
     * Value indicating an invalid {@code id}
     */
    public static final String ID_INVALID              = null;
    
    /**
     * Value in {@code rev} indicating that this xid is waiting for 
     * a revision number to be assigned; this is a fresh xid instance.
     */
    public static final int REV_UNASSIGNED              = -2;
    
    /** 
     * Value in {@code rev} indicating that there is no revision number
     * whatsoever in this xid; this is an invalid/incomplete xid.
     */
    public static final int REV_MISSING                 = -3;
    
    
    /**
     * Value indicating an invalid {@code v_major} or {@code v_minor}
     */
    public static final int VERSION_INVALID             = -1;
    
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

    /**
     * Minor version number, or INVALID_VERSION if not valid
     */
    public int v_minor;

    /**
     * Major version number, or INVALID_VERSION if not valid
     */
    public int v_major;

    // CONSTRUCTORS
    //==============
    
    /**
     * Creation with specified values for {@code id} and {@code rev}.
     * The values are assigned to member variables as such. No validation
     * whatsoever is done, expect that {@code id} cannot be {@code null}
     *
     * @param id  specifies the value of the corresponding member variable
     * @param rev specifies the value of the corresponding member variable
     */
    public Xid(String id, int rev) {
        if (id == null) {
            throw new NullPointerException();
        } // if: null
        
        this.id = id;
        this.rev = rev;
        this.v_major = VERSION_INVALID;
        this.v_minor = VERSION_INVALID;
    } // ctor
    
    /**
     * Creates a xid with the specified values. No validation whatsoever
     * is done, expect that {@code id} cannot be {@code null}.
     *s
     * @param id      specifies the value of the corresponding member variable
     * @param rev     specifies the value of the corresponding member variable
     * @param v_major specifies the value of the corresponding member variable
     * @param v_minor specifies the value of the corresponding member variable
     */
    public Xid(String id, int rev, int v_major, int v_minor) {
        if (id == null) {
            throw new NullPointerException();
        } // if: null id
        
        // Assign vars
        this.id      = id;
        this.rev     = rev;
        this.v_major = v_major;
        this.v_minor = v_minor;
    } // ctor
    
    // OTHER METHODS
    //===============
    
    /**
     * Returns true the Xid has second-order versioning data.
     * @return {@code true} if both {@code v_major} and {@code v_minor}
     * have values different from {@code VERSION_INVALID}. Othherwise,
     * {@code false} is returned.
     */
    public boolean has_version() {
        return ((v_major != VERSION_INVALID) 
            && (v_minor != VERSION_INVALID));
    } // has_version()
    
    // JAVA OBJECT OVERRIDES
    //=======================
    
    /**
     * Creates an equivalent clone. The minor and major version numbers
     * are also cloned, even though they are not neccessary for
     * the equivalence.
    
     * @return a clone for which {@code equals()} is {@code true}.
     */
    @Override
    public Object clone() {
        return new Xid(id, rev, v_major, v_minor);
    } // clone()

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
            //System.out.printf("other was null\n");
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