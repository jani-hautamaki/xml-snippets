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

// Example 6: The identification function 'f' for the measurable space S
//            which is in this case XML elements with both @id and @rev
//            attributes.

// imports
import org.jdom.Element;
import org.jdom.DataConversionException;

public class ex6 {

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
    
    // Identify the XML element x in the subset X of XML.
    public static ID identify(Element x) 
        throws DataConversionException
    {
        return new ID(
            x.getAttributeValue("id"), 
            x.getAttribute("rev").getIntValue()
        ); // return new ID()
    } // identify()
    
    public static void main(String[] args) 
        throws Exception
    {
        Element elem = new Element("anon")
            .setAttribute("id", "theid")
            .setAttribute("rev", "1");
        
        System.out.printf("%s\n", identify(elem));
    } // main()

} // class ex5