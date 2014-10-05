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

package xmlsnippets.core;

// core java
import java.util.List;
import java.util.LinkedList;

/**
 * A String serialization and deserialization for {@code Xid} objects.
 * 
 */
public class XrefString
{
    // CONSTANTS
    //===========
    
    /**
     * Delimitter marker
     */
    public static final char PATH_DELIMITTER            = '/';
    
    // INTERNAL STATES FOR THE DFA
    //=============================
    
    
    private static final int S_ID_EMPTY                 = 1;
    private static final int S_ID                       = 2;
    private static final int S_REVPART_AFTER_DOT        = 3;
    private static final int S_REVPART_EMPTY            = 4;
    private static final int S_REVPART                  = 5;
    private static final int S_COMPLETE                 = 6;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally disabled.
     */
    private XrefString() {
    } // ctor
    
    // CLASS METHODS
    //===============
    
    /**
     *
     */
    public static String serialize(Xref xref) {
        if (xref == null) {
            throw new IllegalArgumentException();
        }
        
        if (xref.base == null) {
            throw new RuntimeException(String.format(
                "Cannot serialize an xref; has no base xid"));
        }
        
        String xidstring = XidString.serialize(xref.base);
        
        int len = 0;
        
        len += xidstring.length();
        for (String s : xref.path) {
            // The preceding path delimitter
            len += 1;
            // The path itself
            len += s.length();
        }
        
        StringBuilder sb = new StringBuilder(len);
        sb.append(xidstring);
        for (String s : xref.path) {
            sb.append(PATH_DELIMITTER);
            sb.append(s);
        }
        
        return sb.toString();
    } // serialize()
    
    public static Xref deserialize(
        String text,
        boolean allow_missing_rev
    ) {
        // Split into parts
        List<String> parts = split_string(text, PATH_DELIMITTER);
        
        if (parts.size() == 0) {
            throw new RuntimeException(String.format(
                "Empty reference"));
        }
        
        String xidstring = parts.remove(0);
        
        Xid xid = XidString.deserialize(xidstring, allow_missing_rev);
        
        return new Xref(xid, parts);
    } // deserialize()

    
    public static List<String> split_string(String s, char delim) {
        int from = 0;
        int to = -1;
        int len = s.length();
        
        List<String> rval = new LinkedList<String>();
        
        // This is naive, doesn't account for surrogate pairs.
        // TODO: Take surrogate pairs into account
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (Character.isHighSurrogate(c)) {
                // Read next
                i++;
                char d = s.charAt(i);
                if (Character.isLowSurrogate(d)) {
                    // We are good. Ignore it.
                    continue;
                } else {
                    // Treat the high surrogate as the char
                } // if-else: low surrogate
            } // if-else: high surrogate
            
            if (c == delim) {
                if (from == i) {
                    throw new RuntimeException(String.format(
                        "Reference contains an empty property id: %s", s));
                }
                // split here
                String part = s.substring(from, i);
                
                rval.add(part);
                from = i+1;
            }
        } // for
        
        if (from >= len) {
            throw new RuntimeException(String.format(
                "Reference cannot end with the path delimitter character: %s",
                s));
        }
        
        // Otherwise, add the last part
        String part = s.substring(from, len);
        rval.add(part);
        
        return rval;
    }
    
    
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        try {
            for (int i = 0; i < args.length; i++) {
                //Xid xid = XidString.deserialize(args[i], true);
                Xref xref = XrefString.deserialize(args[i], false);
                System.out.printf("xref: <%s>\n", XrefString.serialize(xref));
                System.out.printf("\n");
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            System.out.printf("%s\n", ex.getMessage());
        }
    } // main()
    
} // class XidString



