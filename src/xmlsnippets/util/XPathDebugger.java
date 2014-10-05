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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.util.List;
// jdom imports
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Text;
import org.jdom.xpath.XPath;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
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
    
    /**
     * Default encoding; this is for Windows.
     */
    public static String DEFAULT_ENCODING = "Cp1252";
    
    // MEMBER VARIABELS
    //==================
    
    /**
     * The context object.
     */
    private Object context_object;
    
    /**
     * The current context
     */
    private Object current_context;
    
    /**
     * The {@code Format} object used in the {@code XMLOutputter}.
     */
    private Format format;
    
    /**
     * Used to output the XML data.
     */
    private XMLOutputter xmloutputter;
    
    /**
     * The writer to be used with System.out
     */
    private OutputStreamWriter writer;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally unallowed
     */
    public XPathDebugger() {
        // Default to pretty format
        format = Format.getPrettyFormat();
        // Create these with a guaranteed encoding
        xmloutputter = new XMLOutputter(format);
        writer = new OutputStreamWriter(System.out);
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
     * Returns the current context
     */
    protected Object get_context() {
        return current_context;
    } // get_context()
    
    /**
     * Sets the given encoding
     */
    public void set_encoding(String encoding) {
        try {
            writer = new OutputStreamWriter(System.out, encoding);
            format.setEncoding(encoding);
            // update outputter
            xmloutputter.setFormat(format);
            
        } catch(Exception ex) {
            System.out.printf("Error: cannot set the encoding \"%s\".\n", encoding);
        } // try-catch
    } // set_encoding()
    
    
    public void output_element(Element element) {
        try {
            xmloutputter.output(element, writer);
            System.out.printf("\n");
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if (msg == null) {
                ex.printStackTrace();
            } else {
                System.out.printf("XMLOutputter.output(): %s\n", ex.getMessage());
            } // if-else
        } // try-catch
    } // output_element()
    
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
        
        // Set default encoding
        set_encoding(DEFAULT_ENCODING);
        
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
            boolean rval = on_escape_command(cmd, rest);
            if (rval == false) {
                System.out.printf("Error: Unknown special command \":%s\"\n", cmd);
            }
        } // if-else
        
        return true;
    } // on_command()
    
    
    protected boolean on_escape_command(String cmd, String rest) {
        // Otherwise, see what is the command
        if (cmd.equals("unset")) {
            // Unset local context
            current_context = context_object;
            System.out.printf("Original context resumed\n");
        }
        else if (cmd.equals("set")) {
            // Set context
            command_set_context(rest);
        }
        else if (cmd.equals("output")) {
            command_output(rest);
        }
        else if (cmd.equals("set_indent")) {
            command_set_indent(rest);
        }
        else if (cmd.equals("set_encoding")) {
            set_encoding(rest);
        }
        else if (cmd.equals("encoding")) {
            System.out.printf("Encoding is \"%s\"\n", format.getEncoding());
        }
        else if (cmd.equals("indent")) {
            System.out.printf("Indent is \"%s\"\n", format.getIndent());
        }
        else if (cmd.equals("set_pretty")) {
            System.out.printf("Setting pretty formatting\n");
            command_set_format(Format.getPrettyFormat());
        }
        else if (cmd.equals("set_raw")) {
            System.out.printf("Setting raw formatting\n");
            command_set_format(Format.getRawFormat());
        }
        else if (cmd.equals("set_compact")) {
            System.out.printf("Setting compact formatting\n");
            command_set_format(Format.getCompactFormat());
        }
        else if (cmd.equals("set_textmode")) {
            command_set_textmode(rest);
        }
        else {
            return false;
        } // if-else: recognized cmd?
        
        return true;
    } // on_escape_command()
    
    protected final void command_set_context(String rest) {
        String xpe = rest;
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
        System.out.printf("Context set, %d nodes.\n", nodelist.size());
    } // command_set_context()
    
    protected final void command_output(String rest) {
        String xpe = rest;
        if (xpe.length() == 0) {
            System.out.printf("Error: expected an XPath expression");
            return;
        }
        // Select nodes
        List nodelist = null;
        try {
            nodelist = XPath.selectNodes(current_context, xpe);
        } catch(Exception ex) {
            System.out.printf("XPath.selectNodes() failed:\n");
            System.out.printf("%s\n", ex.getMessage());
            return;
        } // try-catch
        
        System.out.printf("Outputting %d nodes\n", nodelist.size());
        
        if (nodelist.size() == 0) {
            return;
        }

        if (nodelist.size() != 1) {
            // TODO: error?
        }
        
        try {
            xmloutputter.output(nodelist, writer);
            System.out.printf("\n");
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if (msg == null) {
                ex.printStackTrace();
            } else {
                System.out.printf("XMLOutputter.output(): %s\n", ex.getMessage());
            }
        } // try-catch
    } // command_output()

    protected final void command_set_indent(String indent) {
        if (indent.equals("null")) {
            format.setIndent(null);
            System.out.printf("Indentation unset", indent);
        } 
        else {
            format.setIndent(indent);
            System.out.printf("Indentation set to \"%s\"\n", indent);
        }
        
        // update outputter
        xmloutputter.setFormat(format);
    } // command_set_indent()

    protected final void command_set_format(Format fmt) {
        // get the current encoding
        String enc = format.getEncoding();
        // Update the format
        format = (Format) fmt.clone();
        // put the current encoding back
        format.setEncoding(enc);
        
        // update outputter
        xmloutputter.setFormat(format);
        System.out.printf("Format set\n");
    } // command_set_format()

    protected final void command_set_textmode(String rest) {
        if (rest.equals("normalize")) {
            format.setTextMode(Format.TextMode.NORMALIZE);
        }
        else if (rest.equals("preserve")) {
            format.setTextMode(Format.TextMode.PRESERVE);
        }
        else if (rest.equals("trim")) {
            format.setTextMode(Format.TextMode.TRIM);
        }
        else if (rest.equals("trim_full_white")) {
            format.setTextMode(Format.TextMode.TRIM_FULL_WHITE);
        }
        else {
            System.out.printf("Error: unknown text mode \"%s\"", rest);
            return;
        }
        
        // update outputter
        xmloutputter.setFormat(format);
        
        System.out.printf("Text mode set: %s\n", rest);
    } // command_set_textmode()
    
    /**
     * Displays information about all nodes in a result set.
     *
     * @param nodelist the result list of an XPath expression evaluation
     */
    public static void display_node_list(List nodelist) {
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
        
        System.out.printf("Result: %d nodes\n", size);
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
                xpath_reverse = XPathIdentification.get_xpath(nodelist.get(0));
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
