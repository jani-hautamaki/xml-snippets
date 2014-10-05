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

package xmlsnippets.fida;

// jdom
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Content;

// xmlsnippets 
import xmlsnippets.util.XPathIdentification;
import xmlsnippets.fida.MigrationLogic.GraphEdge;

public class XMLError {

    // STATIC CONFIGURATION VARIABLES
    //================================

    public static boolean g_quiet = false;


    // MEMBER VARIABLES
    //==================

    // CONSTRUCTORS
    //==============

    /**
     * Default constructor disabled intentionally.
     */
    private XMLError() {
    }

    // OTHER METHODS
    //===============

    public static void raise(
        Object obj,
        String fmt, Object... args
    ) {
        throw new RuntimeException(format("ERROR", obj, fmt, args));
    }

    public static void printf(
        Object obj,
        String fmt, Object... args
    ) {
        if (g_quiet == true) {
            // Suppress output
            return;
        }

        System.out.printf(format("ERROR", obj, fmt, args));
        System.out.printf("\n");
    }

    public static void info(
        Object obj,
        String fmt, Object... args
    ) {
        if (g_quiet == true) {
            // Suppress output
            return;
        }

        System.out.printf(format("INFO", obj, fmt, args));
        System.out.printf("\n");
    }

    private static String format(
        String level,
        Object obj,
        String fmt, Object... args
    ) {
        // Allocate 1 KB; should be enough for most...
        StringBuilder sb = new StringBuilder(1024);

        String msg = String.format(fmt, args);
        //System.out.printf("%s:\n", filename);

        if (obj instanceof GraphEdge) {
            GraphEdge edge = (GraphEdge) obj;
            for (Attribute a : edge.manifestations) {
                String filename = get_filename(a);
                sb.append(String.format(
                    "URI:   %s\n", filename));
                sb.append(String.format(
                    "ITEM:  %s=\"%s\"\n", 
                    XPathIdentification.get_xpath(a), a.getValue()));
            }
        }
        else if (obj instanceof Attribute) {
            Attribute a = (Attribute) obj;

            String filename = get_filename(obj);
            sb.append(String.format(
                "URI:   %s\n", filename));
            sb.append(String.format(
                "ITEM:  %s=\"%s\"\n", 
                XPathIdentification.get_xpath(a), a.getValue()));
        } else {
            String filename = get_filename(obj);
            sb.append(String.format(
                "URI:   %s\n", filename));
            sb.append(String.format(
                "ITEM:  %s\n", 
                XPathIdentification.get_xpath(obj)));
        }

        sb.append(String.format("%-6s %s\n", level+":", msg));
        return sb.toString();
    }


    private static String get_filename(Object obj) {
        Document doc = null;

        if (obj instanceof Attribute) {
            Attribute a = (Attribute) obj;
            doc = a.getDocument();
        }
        else if (obj instanceof Content) {
            Content c = (Content) obj;
            doc = c.getDocument();
        }
        else if (obj instanceof Document) {
            doc = (Document) obj;
        }
        else {
            // error
            throw new RuntimeException(String.format(
                "Unexpected dynamic type: %s", obj.getClass().getName()));
        }

        return doc.getBaseURI();
    } // get_filename()

} // class XMLError
