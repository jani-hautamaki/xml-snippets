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

// Example 3: Testing for the contentual equivalence

// imports
import xmlsnippets.core.XML;
import java.io.IOException;
import org.jdom.Element;

public class ex3 {
    
    // The test code
    public static void main(String[] args) 
        throws IOException
    {
        Element a = new Element("hello").setText("   hello   world   \n    hello    world   \n");
        Element b = new Element("hello").setText("hello world hello world");
        Element c = new Element("hello").setText("hello world");
        
        System.out.printf("eq(a, b): %s\n", XML.eq(a,b));
        System.out.printf("eq(b, c): %s\n", XML.eq(b,c));
        
    } // main()
    
} // ex3