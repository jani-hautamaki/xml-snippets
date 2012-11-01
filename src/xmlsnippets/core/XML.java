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

// imports
import java.util.List;
import org.jdom.Element;
import org.jdom.DataConversionException;

// for contentual equivalence
import java.io.StringWriter;
import java.io.IOException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

/**
 * Contains the identification function {@code f} and the resolution
 * function {@code g} for the proposed system. Also, the contentual
 * equivalence relationship {@code eq()} is contained in the cass.
 */
public class XML {
    
    // CLASS VARIABLES
    //==================
    
    /**
     * An auxiliary variable to avoid constructing the {@code XMLOutputter}
     * object more than once.
     */
    private static XMLOutputter g_xmlserializer = null;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction unallowed.
     */
    private XML() {
    } // ctor
    
    // OTHER METHODS
    //===============
    
    /**
     * Identifies the given XML element {@code x}.
     * The XML element {@code x} is assumed to belong to the measurable 
     * space {@code X} of all XML elements.
     * 
     * @param x the XML element to identify
     * 
     * @return the {@link XML_ID} object corresponding to the measured
     * identity.
     */
    public static Xid identify(Element x) 
        throws DataConversionException
    {
        return new Xid(
            x.getAttributeValue("id"), 
            x.getAttribute("rev").getIntValue()
        ); // return new ID()
    } // identify()
    
    /**
     * Resolved the given identity to the corresponding XML element
     * in a resolvable subset {@code S} of {code X}.
     *
     * @param s the resolvable set of XML elements
     * @param xid the identity of the XML element that is being resolved.
     *
     * @return the corresponding XML element, or null if no such XML
     * element found. 
     *
     * @throws RuntimeException if more than one XML element with
     * matching identity is found from the list.
     */
    public static Element resolve(
        List<Element> s,
        Xid xid
    ) {
        return null;
    } // resolve()


    /**
     * A helper function to create a properly configured
     * instance of {@code XMLOutputter} class.
     * 
     * @return the object used to serialize the XML elements.
     */
    private static XMLOutputter new_xmloutputter() {
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
    
    /**
     * Singleton and create once for the {@code XMLOutputter} object.
     * If the class variable {@link #g_xmlserializer} has not been
     * initialized yet, the variable is then initialized prior to returning
     * it to the caller.
     *
     * @return the initialized {@link #g_xmlserializer} variable.
     */
    private static XMLOutputter get_xmlserializer() {
        if (g_xmlserializer == null) {
            // First call. Initialize
            g_xmlserializer = new_xmloutputter();
        }
        return g_xmlserializer;
    } // get_xmlserializer()
    
    /**
     * Tests for the contentual equivalence of XML elements {@code x} and 
     * {@code y} in the set {@code XML}.
     *
     * @param x the XML element on the left-hand side of the relation
     * @param y the XML element on the right-hand side of the relation
     *
     * @return {@code true} of the elements are contentually equivalent.
     * Otherwise, {@code false} is returned.
     */
    public static boolean eq(Element x, Element y) 
        throws IOException
    {
        // Auxiliary variables
        StringWriter w = null;
        String sx = null;
        String sy = null;

        // XML serializer with specific settings
        XMLOutputter xmlserializer = get_xmlserializer();
        
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
    
    
} // class XML