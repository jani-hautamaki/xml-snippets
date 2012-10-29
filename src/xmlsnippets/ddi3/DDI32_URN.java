
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

package xmlsnippets.ddi3;

public class DDI32_URN {
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Constructor disabled
     */
    private DDI32_URN() {
    } // ctor

    // HELEPR METHODS
    //================
    
    /**
     * Returns a version string in which the dots in the version number
     * have been converted to underscores.
     *
     * @return The input string with dots replaced with underscores.
     */
    protected static String urn_version(String version) {
        return version.replace('.', '_');
    }

    /**
     * Returns a version string in which the underscores are converted
     * back to dots.
     *
     * @return The input string with undescores replaced with dots.
     */
    protected static String deurn_version(String version) {
        return version.replace('_', '.');
    }
    
    
    // SERIALIZATION
    //===============
    
    /**
     * Converts the object into a String representation suitable
     * for use in an URN.
     *
     * @return String representation suitable for an URN.
     *
     */
    public static String serialize(DDI32_ID ddi_id) {
        // Determine whether this is two-part or just one-part identity,
        // and return the string based on that.
        
        String prefix = "urn:ddi:3_2";
        String part1 = null;
        String part2 = ddi_id.maintainable_agency;
        String part3 = null;
        
        if (ddi_id.identifiable_element != null) {
            // ElementName1.ElementName2
            part1 = String.format("%s.%s", 
                ddi_id.maintainable_element, ddi_id.identifiable_element);

            // Id1(Version_1).Id2(Version_2)s
            part3 = String.format("%s(%s).%s(%s)",
                ddi_id.maintainable_id, urn_version(ddi_id.maintainable_version),
                ddi_id.identifiable_id, urn_version(ddi_id.versionable_version)
            );
        } else {
            // ElementName1
            part1 = String.format("%s", ddi_id.maintainable_element);
            
            // Id1(Version_1)
            part3 = String.format("%s(%s)",
                ddi_id.maintainable_id, urn_version(ddi_id.maintainable_version)
            );
            
        } // if-else
        return String.format("%s:%s=%s:%s",
            prefix, part1, part2, part3);
    } // toString()
    
    
    // STATE VARIABLES
    //=================
    
    private static final int S_URN                      = 1;
    private static final int S_URN_DDI                  = 2;
    private static final int S_URN_DDI_VERSION          = 3;
    private static final int S_MAINTAINABLE_ELEMENT     = 4;
    private static final int S_IDENTIFIABLE_ELEMENT     = 5;
    private static final int S_MAINTAINABLE_AGENCY      = 6;
    private static final int S_MAINTAINABLE_ID          = 7;
    private static final int S_MAINTAINABLE_VERSION     = 8;
    private static final int S_EXPECT_IDENTIFIABLE_ID   = 9;
    private static final int S_IDENTIFIABLE_ID          = 10;
    private static final int S_VERSIONABLE_VERSION      = 11;
    private static final int S_ACCEPT                   = 12;
    
    /**
     * Parse an URN into {@link DDI32_ID} object.
     * The parsing utilizes deterministic finite automata (DFA).
     *
     * @param urn the URN to parse
     *
     * @return the parsed {@code DDI32_ID} object corresponding to the URN.
     */
    public static DDI32_ID deserialize(String urn) {
        String m_agency = null;
        String m_element = null;
        String m_id = null;
        String m_version = null;
        String i_element = null;
        String i_id = null;
        String v_version = null;
        
        int len = urn.length();
        int state = S_URN;
        int from = 0;
        // Auxiliary variable for remembering whether the urn
        // has a sub-maintainable object too or not.
        boolean submaintainable = false;
        
        for (int i = 0; i < len; i++) {
            
            boolean eps = false;
            char c = urn.charAt(i);
            
            switch(state) {
                case S_URN:
                    // Skip until "urn:"
                    if (c == ':') {
                        String piece = urn.substring(from, i);
                        if (piece.equals("urn") == false) {
                            throw new RuntimeException(String.format(
                                "Expected \"urn\", but found: \"%s\"", piece));
                        }
                        state = S_URN_DDI;
                        from = i+1;
                    }
                    break;
                    
                case S_URN_DDI:
                    // Skip until "urn:ddi:"
                    if (c == ':') {
                        String piece = urn.substring(from, i);
                        if (piece.equals("ddi") == false) {
                            throw new RuntimeException(String.format(
                                "Expected \"ddi\", but found: \"%s\"", piece));
                        }
                        state = S_URN_DDI_VERSION;
                        from = i+1;
                    }
                    break;
                    
                case S_URN_DDI_VERSION:
                    // Skip until "urn:ddi:3_2:"
                    if (c == ':') {
                        String piece = urn.substring(from, i);
                        if (piece.equals("3_2") == false) {
                            throw new RuntimeException(String.format(
                                "Expected \"3_2\", but found: \"%s\"", piece));
                        }
                        state = S_MAINTAINABLE_ELEMENT;
                        from = i+1;
                    }
                    break;
                    
                case S_MAINTAINABLE_ELEMENT:
                    // Skip until "urn:ddi:3_2:ElementName1."
                    // or until "urn:ddi:3_2:ElementName1="
                    if ((c == '.') || (c == '=')) {
                        m_element = urn.substring(from, i);
                        
                        from = i+1;
                        if (c == '.') {
                            state = S_IDENTIFIABLE_ELEMENT;
                            submaintainable = true;
                        } 
                        else if (c == '=') {
                            state = S_MAINTAINABLE_AGENCY;
                        }
                    }
                    break;
                    
                case S_IDENTIFIABLE_ELEMENT:
                    // Skip until "urn:ddi:3_2:ElementName1.ElementName2="
                    if (c == '=') {
                        i_element = urn.substring(from, i);
                        from = i+1;
                        state = S_MAINTAINABLE_AGENCY;
                    }
                    break;
                    
                case S_MAINTAINABLE_AGENCY:
                    // Skip until "urn:ddi:3_2:xxx=agency:"
                    if (c == ':') {
                        m_agency = urn.substring(from, i);
                        from = i+1;
                        state = S_MAINTAINABLE_ID;
                    }
                    break;
                    
                case S_MAINTAINABLE_ID:
                    // Skip until "urn:ddi:3_2:xxx=agency:Id1("
                    if (c == '(') {
                        m_id = urn.substring(from, i);
                        from = i+1;
                        state = S_MAINTAINABLE_VERSION;
                    }
                    break;
                    
                case S_MAINTAINABLE_VERSION:
                    // Skip until "urn:ddi:3_2:xxx=agency:Id1(Version_1)"
                    if (c == ')') {
                        m_version = urn.substring(from, i);
                        m_version = deurn_version(m_version);
                        from = i+1;
                        state = S_EXPECT_IDENTIFIABLE_ID;
                    }
                    break;
                
                case S_EXPECT_IDENTIFIABLE_ID:
                    if (c == '.') {
                        // Continues. Verify the submaintainable status
                        if (submaintainable == false) {
                            throw new RuntimeException(String.format(
                                "No sub-MaintainableType element present, but id given"));
                        }
                        from = i+1;
                        state = S_IDENTIFIABLE_ID;
                    } else {
                        // Should be done, but isn't. Error
                        throw new RuntimeException(String.format(
                            "Expected end-of-line or \'.\', but found: \'%c\'", c));
                    }
                    break;
                    
                case S_IDENTIFIABLE_ID:
                    // Skip until "urn:ddi:3_2:xxx=agency:Id1(Version_1).Id2("
                    if (c == '(') {
                        i_id = urn.substring(from, i);
                        from = i+1;
                        state = S_VERSIONABLE_VERSION;
                    }
                    break;

                case S_VERSIONABLE_VERSION:
                    // Skip until "urn:ddi:3_2:xxx=agency:Id1(Version_1).Id2(Version_2)"
                    if (c == ')') {
                        v_version = urn.substring(from, i);
                        v_version = deurn_version(v_version);
                        from = i+1;
                        state = S_ACCEPT;
                    }
                    break;
                    
            } // switch
            
            if (eps) {
                i--;
            }
        } // for
        
        if (state == S_EXPECT_IDENTIFIABLE_ID) {
            // Whether a sub-maintainable id was expected
            if (submaintainable == true) {
                // Yes. So this is a syntax error
                throw new RuntimeException(String.format(
                    "The sub-MaintainableType element present, but no id"));
            } else {
                // Accept
                state = S_ACCEPT;
            }
        } // if
        
        if (state != S_ACCEPT) {
            throw new RuntimeException(String.format(
                "Unexpected end-of-urn"));
        }
        
        return new DDI32_ID(
            m_agency,
            m_element, m_id, m_version,
            i_element, i_id, v_version
        );
    } // serialize()
    
} // class DDI32_URN