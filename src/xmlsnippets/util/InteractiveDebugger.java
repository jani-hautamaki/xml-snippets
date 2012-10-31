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
// jdom imports
import org.jdom.Document;
// xmlsnippets imports
import xmlsnippets.util.XMLFileHelper;

public class InteractiveDebugger {
    
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
     * Default prompt
     */
    public static final String DEFAULT_PROMPT = "cmd> ";
    
    // MEMBER VARIABELS
    //==================
    
    /**
     * When true, the command-loop is halted
     */
    private boolean quit_flag;
    
    /**
     * The command prompt
     */
    private String prompt;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Basic constructor
     */
    public InteractiveDebugger() {
        // Set default prompt
        prompt = DEFAULT_PROMPT;
        // Set quit flag, even though not neccessary
        quit_flag = true;
    } // ctor
    
    
    // OTHER METHODS
    //===============

    /**
     * Sets the quit flag to the specified value.
     */
    protected final void set_quit(boolean value) {
        quit_flag = value;
    } // set_quit()
    
    /**
     * Sets the command prompt to the specified value
     */
    protected final void set_prompt(String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }
        prompt = value;
    } // set_prompt()
    
    
    /**
     * Runs the interactive command prompt
     *
     */
    public final void prompt() {
        // This will the reader object
        BufferedReader linereader = null;
        
        // Instantiate for System.in
        linereader = new BufferedReader(
            new InputStreamReader(System.in));

        // Reset quit flag
        set_quit(false);

        // Call the sub-class initialization/preparation function
        on_init();
        
        // Enter the command-line loop
        do {
            // input line
            String line = null;
            
            // Display prompt
            System.out.printf("%s", prompt);
            
            // Read a line; this can also throw an exception.
            try {
                line = linereader.readLine();
            } catch(IOException ex) {
                String msg = ex.getMessage();
                if (msg == null) {
                    throw new RuntimeException(ex);
                }  // if: no msg
                
                throw new RuntimeException(String.format(
                    "BufferedReader.readLine(): %s", msg), ex);
            } // try-catch
            
            // If this point is reached, the line was succesfully read.
            // Pass the line to the sub-class
            on_command(line);
            
            // Loop until quit flag is set
        } while (!quit_flag);
        
    } // command_prompt()
    
    /**
     * This will be called when the main command prompt loop is
     * about to be entered. Here the internal state of the debugger
     * should be reset.
     * 
     */
    protected void on_init() {
        
    } // on_init()
    
    /**
     * This will receive each command; override in a sub-class to provide
     * more functionality. The basic implementation takes care just of
     * quit commands.
     *
     * @param line the command-line from stdin
     */
    protected boolean on_command(String line) {
        boolean rval = false;
        
        if (line.equals("stop") 
            || line.equals("quit")
            || line.equals("exit"))
        {
            set_quit(true);
            rval = true;
        } // if
        
        return rval;
    } // command()
    
    
    /**
     * Framework invokes this to order a load, the loaded document is
     * passed forward to {@link #on_document(File, Document)}.
     *
     * @param file the file to load
     */
    public final void load_file(File file) {
        Document doc = null;
        
        if (file.isFile() == false) {
            throw new RuntimeException(String.format(
                "load_file(): Not a file: \"%s\"", file.getPath()));
        } // if: not a file
        
        // Attempt loading the file. This may throw an exception.
        try {
            doc = XMLFileHelper.deserialize_document(file);
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if (msg == null) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(String.format(
                "XMLFileHelper.deserialize_document() failed: %s", ex.getMessage()), ex);
        } // try-catch
        
        // If this point is reached, no exception was raised.
        // Pass the document to the sub-class
        on_document(file, doc);
    } // load_document()
    
    /**
     * A document resulting from {@link #load_file(File)} is passed to this 
     * function. The sub-class can decide what it does with the document
     *
     * @param file the file from which the document was read
     * @param document the loaded document
     */
    protected void on_document(File file, Document document) {
        // Override
    } // on_document()
    
    /**
     * Default implementation treats all arguments as files, and loads them.
     *
     * @param args the command-line arguments
     *
     */
    public void parse_arguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            File file = new File(args[i]);
            System.out.printf("Loading file %s\n", file.getPath());
            load_file(file);
        } // for: each arg
    } // parse_arguments()
    
    /**
     * Test execution of the InteractiveDebugger class.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.printf("No arguments\n");
            System.exit(EXIT_SUCCESS);
        } // if
        
        InteractiveDebugger idebugger = new InteractiveDebugger();
        try {
            // May fail
            idebugger.parse_arguments(args);
            
            // If not, call the debugger
            System.out.printf("Type \'quit\' or \'stop\' to exit.\n");
            idebugger.prompt();
            
        } catch(Exception ex) {
            String msg = ex.getMessage();
            if (msg != null) {
                System.out.printf("%s\n", msg);
            } else {
                ex.printStackTrace();
            } // if-else
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        // Indicate succesful exit
        System.exit(EXIT_SUCCESS);
    } // main()
    
} // class XPathDebbuger
