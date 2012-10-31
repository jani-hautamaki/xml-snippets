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
import xmlsnippets.util.InteractiveDebugger;
import xmlsnippets.util.XPathIdentification;

public class XPathDebugger 
    extends InteractiveDebugger
{
    
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
    
    /**
     * The context object.
     *
     */
    private Object context_object;
    
    /**
     * The current context
     */
    private Object current_context;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally unallowed
     */
    public XPathDebugger() {
    } // ctor
    
    // OTHER METHODS
    //===============

    /*
     * Sets the content object
     */
    protected void set_context(Object value) {
        context_object = value;
        current_context = context_object;
    }
    
    /**
     * Interactive inspection of an XML context. This function can be
     * called anytime. Also, a stand-alone utility program is provided
     * to inspect XML documents.
     *
     * @param context the XML context
     */
    public static void debug(Object context) {
        // Instanties a new XML debugger and executes it immediately
        // for the given file.
        
        XPathDebugger debugger = new XPathDebugger();
        debugger.set_context(context);
        
        debugger.prompt();
    } // debug()
    
    @Override
    protected void on_document(File file, Document doc) {
        if (context_object != null) {
            throw new RuntimeException(String.format(
                "%s: context already set, this is extra", file.getPath()));
        }
        
        // Otherwise, set the context
        set_context(doc);
    } // on_document();
    
    @Override
    protected void on_init() {
        set_prompt("xpath> ");
        if (context_object == null) {
            throw new RuntimeException("No context object set");
        } // if
        
        // Reset the current context
        current_context = context_object;
    } // on_init();
    
    @Override
    protected boolean on_command(String line) {
        if ((line.length() == 0) 
            || super.on_command(line)) 
        {
            return true;
        } // if
        
        // Attempt to split
        String cmd = null;
        String rest = null;
        
        int n = line.indexOf(' ');
        
        if (n != -1) {
            cmd = line.substring(0, n);
            rest = line.substring(n+1);
        } else {
            cmd = line;
            rest = "";
        } // if: has a whitespace
        
        if (cmd.charAt(0) == ':') {
            cmd = cmd.substring(1);
        } else {
            cmd = null;
            rest = null;
        } // if-else

        String[] args = new String[] { cmd, rest };
        
        if (cmd == null) {
            // If no special command, then just evaluate the xpath
            evaluate_xpath(current_context, line);
        } else {
            // Otherwise, see what is the command
            if (cmd.equals("unset")) {
                // Unset local context
                current_context = context_object;
                System.out.printf("Original context resumed\n");
            }
            else if (cmd.equals("set")) {
                // Set context
                command_set_context(args);
            }
            else {
                System.out.printf("Error: Unknown special command \":%s", cmd);
            } // if-else: recognized cmd?
        } // if-else: a command
        
        return true;
    } // on_command();
    
    protected final void command_set_context(String[] args) {
        String xpe = args[1];
        if (xpe.length() == 0) {
            System.out.printf("Error: expected an XPath expression");
            return;
        }
        System.out.printf("Evaluating \"%s\" against original context\n", xpe);
        
        // Select nodes
        List nodelist = null;
        try {
            nodelist = XPath.selectNodes(context_object, xpe);
        } catch(Exception ex) {
            System.out.printf("XPath.selectNodes() failed:\n");
            System.out.printf("%s\n", ex.getMessage());
            return;
        } // try-catch
        
        if (nodelist.size() == 0) {
            System.out.printf("Error: empty result set, cannot set context\n");
            return;
        }
        
        if (nodelist.size() == 1) {
            current_context = nodelist.get(0);
        } else {
            current_context = nodelist;
        }
        System.out.printf("Context set, %d objects.\n", nodelist.size());
        
    } // command_set_context()
    
    /**
     * Displays information about all nodes in a result set.
     *
     * @param nodelist the result list of an XPath expression evaluation
     */
    private static void display_node_list(List nodelist) {
        int size = nodelist.size();

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
        
        System.out.printf("Results: %d\n", size);
    } // display_node_list()
    
    private static void evaluate_xpath(
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

        try {
            XPathDebugger debugger = new XPathDebugger();
            
            // May throw
            debugger.parse_arguments(args);
            
            // Execute
            debugger.prompt();
            
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(EXIT_FAILURE);
        } //  try-catch
        
        /*
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
        */
        
        // Indicate succesful exit
        System.exit(EXIT_SUCCESS);
    } // main()
    
} // class XPathDebbuger
