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

// java core imports
import java.io.File;
// jdom imports
import org.jdom.Document;
// xmlsnippets imports
import xmlsnippets.util.XMLFileHelper;

/**
 * A round-trip for an XML file
 *
 */
public class RoundTrip {

    public static final int EXIT_SUCCESS = 0;

    public static final int EXIT_FAILURE = 1;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.printf("Not enough arguments!\n");
            System.err.printf("Arguments: <source_file> <target_file>\n");
            System.exit(EXIT_FAILURE);
        } // if: not enough args

        try {
            File source = new File(args[0]);
            Document doc = XMLFileHelper.deserialize_document(source);

            // TODO: 
            // Determine the source encoding

            File target = new File(args[1]);
            XMLFileHelper.serialize_document_verbatim(doc, target);
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(EXIT_FAILURE);
        }

        System.exit(EXIT_SUCCESS);
    } // main()

} // class RoundTrip
