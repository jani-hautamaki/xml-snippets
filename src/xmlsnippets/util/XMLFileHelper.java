//*******************************{begin:header}******************************//
//               fida - https://code.google.com/p/xml-snippets/
//***************************************************************************//
//
//      fida: an XML Revision Tracking and Version Control Software.
//
//      Copyright (C) 2012-2014 Jani Hautamaki <jani.hautamaki@hotmail.com>
//
//      Licensed under the terms of GNU General Public License v3.
//
//      You should have received a copy of the GNU General Public License v3
//      along with this program as the file LICENSE.txt; if not, please see
//      http://www.gnu.org/licenses/gpl-3.0.html
//
//********************************{end:header}*******************************//

package xmlsnippets.util;

// jdom imports
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
// java core imports
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class XMLFileHelper
{

    // CLASS VARIABLES
    //=================

    /**
     * Singleton SAXBuilder object for verbatim parse
     */
    private static SAXBuilder g_saxbuilder = null;

    /**
     * Singleton XMLOutputter object for the normalized and indented
     * output
     */
    private static XMLOutputter g_formatting_xmloutputter = null;

    /**
     * Singleton XMLOutputter object for the verbatim output
     */
    private static XMLOutputter g_verbatim_xmloutputter = null;

    // CONSTRUCTORS
    //==============

    /**
     * Construction of this object is not allowed.
     */
    private XMLFileHelper() {
    } // ctor

    // PRIVATE HELPER METHODS
    //========================

    /**
     * Instantiates and configures a {@code SAXBuilder} object.
     *
     * @return the configured {@code SAXBuilder} object.
     */
    private static SAXBuilder new_saxbuilder() {
        SAXBuilder saxbuilder = new SAXBuilder();

        // No validation; gives a speedup.
        saxbuilder.setFeature("http://xml.org/sax/features/validation", false);
        saxbuilder.setFeature("http://apache.org/xml/features/validation/schema", false);

        // Do not load any external data; gives also a nice speedup.
        saxbuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        saxbuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        // Verbatim read; do not modify whitespaces between XML elements.
        saxbuilder.setIgnoringBoundaryWhitespace(false);
        saxbuilder.setExpandEntities(false);

        return saxbuilder;
    } // new_saxbuilder()

    /**
     * Returns the common {@code SAXBuilder} used to parse all input
     * documents. The instance is created and initialized, if neccessary.
     *
     * @return the singleton {@code SAXBuilder}
     */
    private static SAXBuilder get_saxbuilder() {
        if (g_saxbuilder == null) {
            // Instantiate
            g_saxbuilder = new_saxbuilder();
        } // if
        return g_saxbuilder;
    } // get_saxbuilder()

    /**
     * Instantiates and configures a {@code XMLOutputter} object
     * which indents and normalizes the input XML data.
     *
     * @return the configured {@code XMLOutputter} object.
     */
    private static XMLOutputter new_formatting_xmloutputter() {
        // Returns a new Format object that performs no whitespace changes,
        // uses the UTF-8 encoding, doesn't expand empty elements, includes
        // the declaration and encoding, and uses the default entity escape
        // strategy.
        Format fmt = Format.getRawFormat();
        // Remove any indentation
        fmt.setIndent("  ");
        // Left and right trim whitespaces plus normalize
        // any consequetive internal whitepaces to a single whitespace.
        fmt.setTextMode(Format.TextMode.NORMALIZE);

        // Instantiate with the configured Format object fmt
        return new XMLOutputter(fmt);
    } // new_formatting_xmloutputter();

    /**
     * Instantiates and configures a {@code XMLOutputter} object
     * which outputs the XML data as is without modifying its contents.
     *
     * @return the configured {@code XMLOutputter} object.
     */
    private static XMLOutputter new_verbatim_xmloutputter() {
        // Returns a new Format object that performs no whitespace changes,
        // uses the UTF-8 encoding, doesn't expand empty elements, includes
        // the declaration and encoding, and uses the default entity escape
        // strategy.
        Format fmt = Format.getRawFormat();

        // Instantiate with the configured Format object fmt
        return new XMLOutputter(fmt);
    } // new_verbatim_xmloutputter();

    /**
     * Returns the singleton verbatim {@code XMLOutputter} object.
     *
     * @return the verbatim {@code XMLOutputter}.
     */
    private static XMLOutputter get_verbatim_xmloutputter() {
        if (g_verbatim_xmloutputter == null) {
            // Instantiate
            g_verbatim_xmloutputter = new_verbatim_xmloutputter();
        } // if
        return g_verbatim_xmloutputter;
    } // get_verbatim_xmloutputter()

    /**
     * Returns the singleton indenting and normalizing {@code XMLOutputter}
     * object.
     *
     * @return the indenting and normalizing {@code XMLOutputter}.
     */
    private static XMLOutputter get_formatting_xmloutputter() {
        if (g_formatting_xmloutputter == null) {
            g_formatting_xmloutputter = new_formatting_xmloutputter();
        } // if
        return g_formatting_xmloutputter;
    } // get_formatting_xmloutputter()

    /**
     * Returns an {@code OutputStreamWriter} for {@code FileOutputStream}
     * with encoding configured to match {@code XMLOutputter}.
     * @param xmloutputter the {@code XMLOutputter} from which the encoding
     * is taken.
     * @param file the output file
     * @return The writer for the output file with correct encoding.
     */
    private static OutputStreamWriter get_writer_for(
        XMLOutputter xmloutputter,
        File file
    )
        throws FileNotFoundException, UnsupportedEncodingException
    {
        return new OutputStreamWriter(
            new FileOutputStream(file),
            xmloutputter.getFormat().getEncoding()
        ); // new OutputStreamWriter()
    } // get_writer_for()

    // CLASS METHODS
    //===============

    /**
     * Parses a computer file into JDOM's {@code Document} object.
     *
     * @param file the input file
     * @return the parsed JDOM {@code Document} object.
     */
    public static Document deserialize_document(
        File file
    )
        throws JDOMException, IOException
    {
        Document doc = null;

        // Retrieve, and instantiate if neccessary,
        // the singleton SAXBuilder object.
        SAXBuilder saxbuilder = get_saxbuilder();

        // Try parsing the file pointed by the File object into a Document
        // object. The build() call may throw the following exceptions:
        // JDOMException, IOException, FileNotFoundException
        doc = saxbuilder.build(file);

        return doc;
    } // deserialize_document()

    /**
     * Serializes the given XML document into a file without modifying the XML
     * data contents.
     *
     * @param doc the document to be serialized
     * @param file the output file
     */
    public static void serialize_document_verbatim(
        Document doc,
        File file
    )
        throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        XMLOutputter xmloutputter = get_verbatim_xmloutputter();

        OutputStreamWriter writer = get_writer_for(xmloutputter, file);
        xmloutputter.output(doc, writer);
        writer.close();
    } // serialize_document_verbatim()

    /**
     * Serializes the given XML document into a file with a proper indentation,
     * whitespace trimming and normalization.
     *
     * @param doc the document to be serialized
     * @param file the output file
     */
    public static void serialize_document_formatted(
        Document doc,
        File file
    )
        throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        XMLOutputter xmloutputter = get_formatting_xmloutputter();

        OutputStreamWriter writer = get_writer_for(xmloutputter, file);
        xmloutputter.output(doc, writer);
        writer.close();
    } // serialize_document_formatted()

} // class XMLHelper
