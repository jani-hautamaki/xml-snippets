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

// Example 2: Testing for the literal equivalence

// imports
import org.jdom.Element;

public class ex2 {

    // Test for the literal equivalence of x and y in XML
    public static boolean literal_eq(Element x, Element y) {
        return x==y;
    } // literal_eq()
    
    public static void main(String[] args) {
    } // main()
} // ex2