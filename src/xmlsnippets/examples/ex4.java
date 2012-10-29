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

// Example 4: Testing membership

// imports
import org.jdom.Element;

public class ex4 {

    // Tests whether the element "elem" of the set XML
    // is a member of the subset X of XML.
    public static boolean memberof_x(Element elem) {
        if ((elem.getAttribute("id") == null) 
            || (elem.getAttribute("rev") == null))
        {
            return false;
        } // if
        
        return true;
    } // memberof_x()

    public static void main(String[] args) {
        Element elem = new Element("anon")
            .setAttribute("id", "theid")
            .setAttribute("rev", "1");
        System.out.printf("memberof_x(e): %s\n", memberof_x(elem));
    } // main()
    
} // class ex4
