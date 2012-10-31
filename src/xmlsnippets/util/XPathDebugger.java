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

package xmlsnippets.util;

// java core imports
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.List;
// jdom imports
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Text;
import org.jdom.xpath.XPath;
import org.jdom.Document;
// xmlsnippets imports
import xmlsnippets.util.XMLFileHelper;
import xmlsnippets.util.XPathIdentification;

public class XPathDebugger {
    
    // CONSTANTS
    //===========
    
    /**
     * System exit value for succesful execution is zero (0).
     */
    public static final int EXIT_SUCCESS = 0;
    
    /**
     * System exit value for unsuccesful execution is one (1).
     */
    public static final int EXIT_FAILURE = 1;
    
    // MEMBER VARIABELS
    //==================
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally unallowed
     */
    private XPathDebugger() {
    } // ctor
    
    // OTHER METHODS
    //===============

    /**
     * Interactive inspection of an XML context. This function can be
     * called anytime. Also, a stand-alone utility program is provided
     * to inspect XML documents.
     *
     * @param context the XML context
     */
    public static void debug(Object context) {
        BufferedReader linereader = null;
        
        linereader = new BufferedReader(
            new InputStreamReader(System.in));
        
        // Quit flag; the main loop is halted when this is set to true.
        boolean quit = false;
        
        do {
            String line = null;
            System.out.printf("xpath> ");
            // Read a command; this can also throw an exception
            try {
                line = linereader.readLine();
            } catch(IOException ex) {
                throw new RuntimeException(String.format(
                    "BufferedReader.readLine() failed"), ex);
            } // try-catch
            
            // TODO: parse_command()
            
            if (line.equals("stop") || line.equals("quit")) {
                quit = true;
            }
            else {
                // Otherwise, assume it is an XPath
                command_resolve_xpath(context, line);
            } // if-else
            
        } while (!quit);
    } // debug()
    
    /**
     * Displays information about all nodes in a result set.
     *
     * @param nodelist the result list of an XPath expression evaluation
     */
    private static void display_node_list(List nodelist) {
        int size = nodelist.size();
        
        if (size == 0) {
            System.out.printf("Empty result set.\n");
            return;
        }
        
        // In which case all are displayed in a compact listing.
        System.out.printf("Result set size: %d objects\n", size);
        
        for (Object obj : nodelist) {
            String value = null;
            
            if (obj instanceof Element) {
                Element elem = (Element) obj;
                String details = String.format("%d children, %d attrs", 
                    elem.getChildren().size(), elem.getAttributes().size());
                value = String.format("%s (%s)",
                    elem.getQualifiedName(), details);
            }
            else if (obj instanceof Attribute) {
                Attribute attr = (Attribute) obj;
                value = String.format("@%s=\'%s\'", 
                    attr.getQualifiedName(), attr.getValue());
            }
            else if (obj instanceof Text) {
                Text text = (Text) obj;
                value = String.format("\"%s\"", text.getText());
            }
            else if (obj instanceof String) {
                String s = (String) obj;
                value = String.format("\"%s\"", s);
            }
            else if (obj instanceof Boolean) {
                Boolean b = (Boolean) obj;
                value = String.format("%s", b.toString());
            }
            else if (obj instanceof Double) {
                Double d = (Double) obj;
                value = String.format("%s", d.toString());
            }
            else {
                value = "Unsupported dynamic type";
            } // if-else
            
            System.out.printf("   (%s) %s\n", obj.getClass().getName(), value);
        } // for
    } // display_node_list()
    
    private static void command_resolve_xpath(
        Object context, 
        String xpe
    ) {
        // Select nodes
        List nodelist = null;
        try {
            nodelist = XPath.selectNodes(context, xpe);
        } catch(Exception ex) {
            System.out.printf("XPath.selectNodes() failed:\n");
            System.out.printf("%s\n", ex.getMessage());
            return;
        } // try-catch

        display_node_list(nodelist);
        
        // If the XPath expression results in a single node,
        // attempt to do a reverse XPath resolution.
        if (nodelist.size() == 1) {
            // Single individual
            String xpath_reverse = null;
            try {
                // XPathIdentification.is_doable()
                // XPathIdentification.do_for()
                xpath_reverse = XPathIdentification.identify(nodelist.get(0));
                System.out.printf("Reverse XPath resolution: %s\n", xpath_reverse);
            } catch(Exception ex) {
                System.out.printf("Reverse XPath resolution: failed!\n");
                System.out.printf("Error: %s\n", ex.getMessage());
            } // try-catch
        } // if
    } // command_resolve_xpath()
    
    /**
     * Executes the XPath debugger as a stand-alone program.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.printf("No arguments\n");
            System.exit(EXIT_SUCCESS);
        } // if
        
        File file = new File(args[0]);
        Document doc = null;
        // Otherwise, attempt to load the file
        try {
            System.out.printf("Loading %s\n", file.getPath());
            doc = XMLFileHelper.deserialize_document(file);
        } catch(Exception ex) {
            System.out.printf("Error: %s\n", ex.getMessage());
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        // Execute the debugger
        System.out.printf("Type \'quit\' or \'stop\' to exit.\n");
        XPathDebugger.debug(doc);
        
        // Indicate succesful exit
        System.exit(EXIT_SUCCESS);
    } // main()
    
} // class XPathDebbuger
