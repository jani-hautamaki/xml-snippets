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
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.io.StringWriter;
import java.io.IOException;

public class ex3 {

    // A helper function to create a properly configured
    // instance of XMLOutputter class.
    public static XMLOutputter create_outputter() {
        // Returns a new Format object that performs no whitespace changes, 
        // uses the UTF-8 encoding, doesn't expand empty elements, includes 
        // the declaration and encoding, and uses the default entity escape strategy.
        Format fmt = Format.getRawFormat();
        // Make some adjustments which relax the equivalence relation.
        // Remove any indentation
        fmt.setIndent("");
        // left and right trim plus internal whitespace is normalized to 
        // a single space
        fmt.setTextMode(Format.TextMode.NORMALIZE);
        
        // Instantiate with fmt
        return new XMLOutputter(fmt);
    } // createXmlOutputter()
    
    // Test for the contentual equivalence of x and y in XML
    public static boolean eq(Element x, Element y) 
        throws IOException
    {
        // Auxiliary variables
        StringWriter w = null;
        String sx = null;
        String sy = null;

        // XML serializer with specific settings
        XMLOutputter xmlserializer = create_outputter();
        
        // Serialize x into a string
        w = new StringWriter();
        xmlserializer.output(x, w);
        sx = w.toString();
        
        // Serialize y into a string
        w = new StringWriter();
        xmlserializer.output(y, w);
        sy = w.toString();
       
        // Compare the strings
        return sx.equals(sy);
    } // eq()
    
    
    // The test code
    public static void main(String[] args) 
        throws IOException
    {
        Element a = new Element("hello").setText("   hello   world   \n    hello    world   \n");
        Element b = new Element("hello").setText("hello world hello world");
        Element c = new Element("hello").setText("hello world");
        
        System.out.printf("eq(a, b): %s\n", eq(a,b));
        System.out.printf("eq(b, c): %s\n", eq(b,c));
        
    } // main()
    
} // ex3