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
import xmlsnippets.core.XML_ID;
import xmlsnippets.core.XML;
import org.jdom.Element;
import org.jdom.DataConversionException;

public class ex6 {

    public static void main(String[] args) 
        throws Exception
    {
        Element elem = new Element("anon")
            .setAttribute("id", "theid")
            .setAttribute("rev", "1");
        
        System.out.printf("%s\n", XML.identify(elem));
    } // main()

} // class ex5