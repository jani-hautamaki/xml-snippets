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
 * A representation of an XML elements id in the set ID.
 */
public class XML_ID {
    
    // MEMBER VARIABLES
    //==================
    
    /**
     * Value of the {@code @id} attribute.
     */
    public String id_attr;
    
    /**
     * Value of the {@code @rev} attribute, must be non-negative.
     */
    
    public int rev_attr;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Basic constructor. Constructs an XML_ID object with defined
     * id and revision number. The revision number must be non-negative.
      *
     * @param id the value of the XML element's {@code id} attribute.
     * @param rev the numeric value of the XML elements {@code rev} attribute.
     */
    public XML_ID(String id, int rev) {
        id_attr = id;
        rev_attr = rev;
    } // ctor
    
    /**
     * Converts the identity to a simple string representation).
     */
    public String toString() {
        return String.format("(id=%s, rev=%d)",  id_attr, rev_attr);
    } // toString()
    
    /**
     * Tests for the equivalence of two members in the set ID.
     * 
     * @return {@code true} if both {@link #id_attr} and {@link #rev_attr}
     * are equal. Otherwise, {@code false} is returned.
     */
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        // other != null
        if (other instanceof XML_ID) {
            XML_ID id = (XML_ID) other;
            if (id.id_attr.equals(this.id_attr)
                && (id.rev_attr == this.rev_attr))
            {
                return true;
            }
            // Unqual
        } // if
        
        // Either unequal or not an instancof the class
        // at all.
        return false;
    } // equals()
} // class XML_ID

