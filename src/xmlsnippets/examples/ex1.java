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

// Example 1: testing membership of the set XML

// imports
import org.jdom.Element;

public class ex1 {
    
    // Tests whether "obj" is a member of the set XML
    public static boolean memberof_xml(Object obj) {
        if (obj instanceof Element) {
            return true;
        }
        return false;
    } // memberof_xml()
    
    public static void main(String[] args) {
        Element a = new Element("html");
        System.out.printf("memberof_xml(Element): %s\n", memberof_xml(a));
        System.out.printf("memberof_xml(String):  %s\n", memberof_xml(new String()));
    } // main()
} // ex1