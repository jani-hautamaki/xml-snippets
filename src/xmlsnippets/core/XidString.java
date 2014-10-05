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
    public static final String REV_UNASSIGNED_STRING = "#";

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
     * Construction is intentionally unallowed.
     */
    private XidString() {
    } // ctor

    // CLASS METHODS
    //===============

    /**
     * Serializes {@code Xid} into a {@code String} representation;
     * The input data is not validated in any way. It can be arbitrary,
     * and most importantly, invalid. <p>
     *
     * The format will be either {@code
     *    <xidstring> ::= <id> ':' <revspec>
     * }
     * where {@code
     *    <revspec> ::= <rev>
     *                | <v_major> '.' <v_minor> '.' <rev>
     * }<p>
     *
     * If {@code xid.rev} has the value of {@link Xid#REV_UNASSIGNED},
     * then the resulting {@code <rev>} will have the value of
     * {@link #REV_UNASSIGNED_STRING}.
     *
     * @param xid the {@code Xid} to be serialized
     * @return {@code XidString} representation of the {@code Xid} object.
     */
    public static String serialize(Xid xid) {
        String rval = null;
        if (xid.rev == Xid.REV_MISSING) {
            // Do not allow. This is an indication of a programming error.
            throw new RuntimeException(String.format(
                "Attempting to set xid with id=%s and rev=missing; this is a programming error", xid.id));
            //rval = String.format("%s", id);
        }

        String revspec = serialize_revspec(xid);
        rval = String.format("%s:%s", xid.id, revspec);

        return rval;
    } // serialize()

    /**
     * This is a special method which is required in
     * {@link XidIdentification#set_xid(Element, Xid)}. That method may need
     * to set the revspec separately into attribute {@code @rev} or
     * into attribute {@code @version}.
     * @param xid the xid for which the revspec is serialized
     * @return The revspec part of the serialization
     */
    protected static String serialize_revspec(Xid xid) {
        String rval = null;

        // Serialize revstring part of the revspec.
        String revstring = serialize_revstring(xid);

        if (xid.has_version()) {
            // Has noise payload
            rval = String.format("%d.%d.%s",
                xid.v_major, xid.v_minor, revstring);
        } else {
            // True xid
            rval = revstring;
        } // if-else

        return rval;
    } // serialize_revspec()

    /**
     * Special method that is only available to
     * {@link XidIdentification#set_xid(Element, Xid)}.
     */
    protected static String serialize_revstring(Xid xid) {
        String rval = null;

        if (xid.rev == Xid.REV_MISSING) {
            throw new RuntimeException(String.format(
                "Attempting to serialize a missing revision; this is an indication of a programming error"));
        }
        else if (xid.rev == Xid.REV_UNASSIGNED) {
            rval = REV_UNASSIGNED_STRING;
        }
        else {
            rval = String.format("%d", xid.rev);
        } // if-else
        return rval;
    } // serialize_revstring()

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
        return deserialize(text, false);
    }

    public static Xid deserialize(
        String text,
        boolean allow_missing_rev
    ) {
        // Return variable
        Xid rval = null;
        // The picked id
        String id = null;
        // Multiple possible rev parts
        String[] revpart = new String[3];
        // Augment the DFA states with a variable; this corresponds
        // to multiplicating number of states related to this variable..
        int partnum = 0;

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
                    // This is for being able to check the high-byte
                    state = S_ID;

                case S_ID:
                    if (c == ':') {
                        id = text.substring(from, i);
                        from = i+1; // skips the colon
                        state = S_REVPART_EMPTY;
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

                case S_REVPART_EMPTY:
                    state = S_REVPART_AFTER_DOT;
                    // ** FALL THROUGH **

                case S_REVPART_AFTER_DOT:
                    // When a new revpart is beginning,
                    // do not accept revpart separator is not accepted.
                    //
                    if (c == '.') {
                        throw new RuntimeException(String.format(
                            "Unexpected dot \'%c\' at offset=%d in \"%s\"", c, i, text));
                    } else {
                        // Switch to an acceptable state
                        state = S_REVPART;
                    } // if-else
                    break;

                case S_REVPART:
                    if (c == '.') {
                        // A new revpart begins; record the previous
                        revpart[partnum] = text.substring(from, i);
                        // Reset the starting position
                        from = i+1;
                        // Increase the number of revparts
                        partnum++;
                        // Accept at most 3 parts.
                        if (partnum == 3) {
                            throw new RuntimeException(String.format(
                                "Too deep; unexpected third dot \'%c\' at offset=%d in \"%s\"", c, i , text));
                        }
                        // Switch state back to an empty revpart
                        state = S_REVPART_AFTER_DOT;
                    } else {
                        // It should be an integer unless the revpart
                        // is the REV_UNASSIGNED_STRING. Can't really check
                        // yet, so accept for now.
                    } // if-else
                    break;
                default:
                    throw new RuntimeException(String.format(
                        "Internal error; unrecognized state=%d", state));
            } // switch
        } // for: each char

        // If the exit state is S_ID, it indicates that there is no colon;
        // only the id part is present. In that case, the state is manually
        // transit to the ending state which it should be...
        if (state == S_ID) {
            id = text;
            state = S_REVPART_EMPTY;
        } // if: no colon

        if (state == S_REVPART) {
            // The last revpart must always be stored manually..
            revpart[partnum] = text.substring(from);
            partnum++;
            state = S_COMPLETE;
        } // if: revpart

        // Assert the ending state is acceptable
        if ((state == S_COMPLETE)
            || ((state == S_REVPART_EMPTY) && (allow_missing_rev == true)))
        {
            // Accept
        } else {
            // Reject
            if (state == S_ID_EMPTY) {
                throw new RuntimeException(String.format(
                    "Expected a xid, but found an empty string"));
            }
            else if (state == S_ID) {
                throw new RuntimeException(String.format(
                    "Unexpected end of string: no colon nor revision present in \"%s\"", text));
            }
            else if (state == S_REVPART_EMPTY) {
                throw new RuntimeException(String.format(
                    "Unexpected end of string: no revision present in \"%s\"", text));
            }
            else if (state == S_REVPART_AFTER_DOT) {
                throw new RuntimeException(String.format(
                    "Unexpected end of string: the revstring cannot end to a dot in \"%s\"", text));
            }
            else {
                // a fall-back safety guard
                throw new RuntimeException(String.format(
                    "Unexpected end of the string: \"%s\", state=%d", text, state));
            } // if-else
        } // if-else: acceptable ending state

        String revstring = null;
        String majorstring = null;
        String minorstring = null;

        int rev;
        int v_major = Xid.VERSION_INVALID;
        int v_minor = Xid.VERSION_INVALID;

        // then determine what kind of revision there is..
        if (partnum == 0) {
            // No revision at all
            revstring = null;
        }
        else if (partnum == 1) {
            // A single revision number; it is interpreted as revstring.
            revstring = revpart[0];
        }
        else if (partnum == 2) {
            // two revparts; either <v_major> '.' <rev>
            //                   or <v_major> '.' <v_minor>
            //
            // Disambiguation is impossible in the general situation,
            // so this will be interpreted from now on always as
            //                   or <v_major> '.' <v_minor>
            majorstring = revpart[0];
            minorstring = revpart[1];
            revstring = null;
        }
        else if (partnum == 3) {
            // This is parsed as <v_major> '.' <v_minor> '.' <rev>
            majorstring = revpart[0];
            minorstring = revpart[1];
            revstring   = revpart[2];
        }
        else {
            // Should not happen
            throw new RuntimeException(String.format(
                "The DFA somehow accepted more than three revparts; this shouldnt happen! The input text=\"%\"", text));
        }

        // First, attempt to deserialize revstring

        // The revision is all integers, so no conversion error should occur.
        if (revstring == null) {
            if (allow_missing_rev == true) {
                rev = Xid.REV_MISSING;
            } else {
                // No rev in the revstring even though it is required.
                throw new RuntimeException(String.format(
                    "Unexpected end of string: no revision present in \"%s\"", text));
            } // if-else
        }
        else {
            // revstring not null
            if (revstring.equals(REV_UNASSIGNED_STRING)) {
                rev = Xid.REV_UNASSIGNED;
            }
            else {
                rev = parse_revspec_integer(revstring, text);
            }
        } // if-else

        // Second, attempt to deserialize v major and minor, if any

        if (majorstring != null) {
            v_major = parse_revspec_integer(majorstring, text);
        }
        if (minorstring != null) {
            v_minor = parse_revspec_integer(minorstring, text);
        }

        return new Xid(id, rev, v_major, v_minor);
    } // deserialize()



    private static int parse_revspec_integer(
        String input,
        String context
    ) {
        int rval;
        try {
            rval = Integer.parseInt(input);
        } catch(NumberFormatException ex) {
            throw new RuntimeException(String.format(
                "Unable to parse \"%s\" into an integer in revstring \"%s\"", input, context));
        } // try-catch
        if (rval < 0) {
            throw new RuntimeException(String.format(
                "The number \"%s\" should be a non-negative integer in revstring \"%s\"", input, context));
        } // if: negative-valued rev
        return rval;
    } // parse_revstring_integer()

    public static boolean is_valid(String text) {
        boolean rval = true;
        try {
            deserialize(text);
        } catch(Exception e) {
            rval = false;
        } // try-catch
        return rval;
    } // is_valid()


    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        try {
            for (int i = 0; i < args.length; i++) {
                //Xid xid = XidString.deserialize(args[i], true);
                Xid xid = XidString.deserialize(args[i], false);
                System.out.printf("           id: \"%s\"\n", xid.id);
                System.out.printf("          rev: %d\n", xid.rev);
                System.out.printf("      v_major: %d\n", xid.v_major);
                System.out.printf("      v_minor: %d\n", xid.v_minor);
                System.out.printf("serialization: %s\n",
                    XidString.serialize(xid));
                System.out.printf("\n");
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            System.out.printf("%s\n", ex.getMessage());
        }
    } // main()

} // class XidString



