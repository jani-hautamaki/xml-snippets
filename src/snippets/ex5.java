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

// Example 5: A helper class 'ID' for storing and comparing 
//            the (id, rev) 2-tuples

// no imports needed.

public class ex5 {

    // A class to represent an element id in the set ID
    public static class ID {
        // value of @id attribute
        public String id_attr;
        
        // value of @rev attribute
        public int rev_attr;
        
        // Ctor
        public ID(String a, int b) {
            id_attr = a;
            rev_attr = b;
        } // ctor
        
        public String toString() {
            return String.format("(id=%s, rev=%d)",  id_attr, rev_attr);
        } // toString()
        
        // equivalence for elements of the set ID
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            // other != null
            if (other instanceof ID) {
                ID id = (ID) other;
                if (id.id_attr.equals(this.id_attr)
                    && (id.rev_attr == this.rev_attr))
                {
                    return true;
                }
            } // if
            return false;
        } // equals()
    } // class ID
    
    public static void main(String[] args) {
        System.out.printf("%s\n", new ID("theid", 2));
    } // main()

} // class ex5