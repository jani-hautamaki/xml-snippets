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

/**
 * A String serialization and deserialization for {@code Xid} objects.
 * 
 */
public class XidString
{
    // CONSTANTS
    //===========
    
    /**
     * Marker used to indicate that the newest revision should used
     * to fill the revision value.
     */
    public static final String INVALID_REV_STRING = "#";
    
    // INTERNAL STATES FOR THE DFA
    //=============================
    
    private static final int S_ID_EMPTY                 = 1;
    private static final int S_ID                       = 2;
    private static final int S_REV_EMPTY                = 3;
    private static final int S_REV                      = 4;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally unallowed.
     */
    private XidString() {
    } // ctor
    
    // CLASS METHODS
    //===============
    
    /**
     * Serializes {@code Xid} into a {@code String} representation.
     * The format will be {@code id}':'{@code rev}.
     * If the {@code rev} has the value {link Xid#INVALID_REV},
     * then the value {link #INVALID_REV_STRING} is used in place of
     * the actual {@code rev} number.
     *
     * @param xid the {@code Xid} to be serialized
     * @return A text serialization of the {@code Xid}
     */
    public static String serialize(Xid xid) {
        String rval = null;
        if (xid.rev == Xid.INVALID_REV) {
            rval = String.format("%s:%s", xid.id, INVALID_REV_STRING);
        } else {
            rval = String.format("%s:%d", xid.id, xid.rev);
        } // if-else
        
        return rval;
    } // serialize()
    
    /**
     * Parses a {@code String} representation into a {@code Xid} object.
     * The string is expected to have a format {@code <id> ':' <rev>},
     * where {@code <id> ::= [^#:!]+} and {@code <rev> ::= <non-negative-integer> 
     * | INVALID_REV_STRING}. The parsing is done with a DFA.
     *
     * @param text the text representation to parse.
     * @return The {@code Xid} object
     */
    public static Xid deserialize(String text) {
        Xid rval = null;
        String revstring = null;
        String id = null;
        int rev;
        int from = 0;
        
        int state = S_ID_EMPTY;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            switch(state) {
                case S_ID_EMPTY:
                    if (c == ':') {
                        throw new RuntimeException(String.format(
                            "Unexpected colon \'%c\', at offset=%d in \"%s\"", c, i, text));
                    } // if
                    
                    // ** fall-through **
                    state = S_ID;
                    
                case S_ID:
                    if (c == ':') {
                        id = text.substring(from, i);
                        from = i+1; // skips the colon
                        state = S_REV_EMPTY;
                    } 
                    else if ((((int) c) & 0xff) >= 0x80) {
                        // Do not accept
                        throw new RuntimeException(String.format(
                            "High-byte character \'%c\' at offset=%d in \"%s\"", c, i, text));
                    } 
                    else {
                        // Character is accepted
                    } // if-else
                    break;
                    
                case S_REV_EMPTY:
                    // The state is transited immediately
                    state = S_REV;
                    // The revision is picked here
                    revstring = text.substring(i);
                    // Check immediately if the revision matches to the
                    // invalid revision string. If that is the case, do not
                    // bother to validate the input
                    if (revstring.equals(INVALID_REV_STRING)) {
                        // Intern the string
                        revstring = INVALID_REV_STRING;
                        // Exit immediately. 
                        i = len;
                        break;
                    } // if
                    // Otherwise, the characters need to be validated.
                    
                    // ** fall-trhough **
                    
                case S_REV:
                    if (Character.isDigit(c) == false) {
                        throw new RuntimeException(String.format(
                            "Expected a digit, but found \'%c\' at offset=%d in \"%s\"", c, i, text));
                    } // if: not a digit
                    break;
                    
                default:
                    throw new RuntimeException(String.format(
                        "Internal error; unrecognized state=%d", state));
            } // switch
        } // for: each char
        
        // Assert the ending state is acceptable
        if (state != S_REV) {
            if (state == S_ID_EMPTY) {
                throw new RuntimeException(String.format(
                    "Expected a xid, but found an empty string"));
            }
            else if (state == S_ID) {
                throw new RuntimeException(String.format(
                    "Unexpected end of string: no colon nor revision present in \"%s\"", text));
            } 
            else if (state == S_REV_EMPTY) {
                throw new RuntimeException(String.format(
                    "Unexpected end of string: no revision present in \"%s\"", text));
            }
            else {
                // a fall-back safety guard
                throw new RuntimeException(String.format(
                    "Unexpected end of the string: \"%s\"", text));
            } // if-else
        } // if: unacceptable ending state
        
        // The revision is all integers, so no conversion error should occur.
        if (revstring != INVALID_REV_STRING) {
            try {
                rev = Integer.parseInt(revstring);
            } catch(NumberFormatException ex) {
                throw new RuntimeException(String.format(
                    "Cannot convert the revision \"%s\" into an integer. This shouldn\'t happen.", revstring));
            } // try-catch
        } else {
            rev = Xid.INVALID_REV;
        } // if-else
        
        return new Xid(id,  rev);
    } // deserialize()
    
    public static boolean is_valid(String text) {
        boolean rval = true;
        try {
            deserialize(text);
        } catch(Exception e) {
            rval = false;
        } // try-catch
        return rval;
    } // is_valid()
    
} // class XidString



