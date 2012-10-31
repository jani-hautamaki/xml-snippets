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
// xmlsnippets core imports
import xmlsnippets.core.Normalization;


public class XidDebugger 
    extends XPathDebugger
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
    
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Default constructor
     */
    public XidDebugger() {
        super();
    } // ctor
    
    // OTHER METHODS
    //===============
    
    @Override
    protected void on_document(File file, Document doc) {
        super.on_document(file, doc);
    } // on_document();
    
    @Override
    protected void on_init() {
        super.on_init();
        
        // Use a different prompt
        set_prompt("xid> ");
    } // on_init();
    
    @Override
    protected boolean on_escape_command(String cmd, String rest) {
        if (cmd.equals("normalize")) {
            command_normalize(rest);
        } 
        else {
            return false;
        }
        
        return true;
    } // on_escape_command();
    
    
    protected final void command_normalize(String xpe) {
        // Select nodes
        List nodelist = null;
        try {
            nodelist = XPath.selectNodes(get_context(), xpe);
        } catch(Exception ex) {
            System.out.printf("XPath.selectNodes() failed:\n");
            System.out.printf("%s\n", ex.getMessage());
            return;
        } // try-catch
        
        if (nodelist.size() != 1) {
            System.out.printf("Error: %d nodes returned; a single node is required\n", nodelist.size());
            return;
        }
        
        Object obj = nodelist.get(0);
        if ((obj instanceof Element) == false) {
            System.out.printf("Error: the returned node has incorrect dynamic type: %s\n", obj.getClass().getName());
            return;
        }
        
        Element elem = (Element) obj;
        
        Element result = Normalization.normalize(elem);
        
        // Display results
        output_element(result);
        
        
    } // command_normalize()



    
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
            XidDebugger debugger = new XidDebugger();
            
            // May throw
            debugger.parse_arguments(args);
            
            // Execute
            debugger.prompt();
            
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(EXIT_FAILURE);
        } //  try-catch
        
        // Indicate succesful exit
        System.exit(EXIT_SUCCESS);
    } // main()
    
} // class XidDebbuger
