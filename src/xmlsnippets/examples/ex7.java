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

// Example 7: DDI-Lifecycle 3.1 case-study. The identification function
//            and a helper class for storing and comparing identities. 

// imports
import org.jdom.Element;
import org.jdom.DataConversionException;

import xmlsnippets.ddi3.DDI31_ID;
import xmlsnippets.ddi3.DDI31;

public class ex7 {
    public static void main(String[] args) 
        throws Exception
    {
        
        Element a, b, c;
        DDI31_ID ida, idb, idc;

        // First test case is a piece of XML from the specification,
        // part 1, p. 36. It's missing the @agency attribute,
        // but I added it. Actually the same example is in part 2,
        // p. 22 (there are no page numbers in part 2. This number 
        // is from the reader). The part 2 is fixed from one part
        // but another error is introduced. So in practice there is
        // no truly working example of this... But I trust that
        // the examples will be reviewed thoroughly for the next
        // version.

        /*
        <DataCollection isMaintainable="true"
                        agency="us.icpsr"
                        id="DC_5698"
                        version="2.4.0">
    
            <Methodology isVersionable="true"
                         id="Meth_Type_1"
                         version="1.0.0">
                         
                <TimeMethod isIdentifiable="true"
                            id="TM_1">
                            
                </TimeMethod>
            </Methodology>
        </DataCollection>
        */
        
        a = new Element("DataCollection")
            .setAttribute("isMaintainable", "true")
            .setAttribute("agency", "us.icpsr")
            .setAttribute("id", "DC_5698")
            .setAttribute("version", "2.4.0");
        b = new Element("Methodology")
            .setAttribute("isVersionable", "true")
            .setAttribute("id", "Meth_Type_1")
            .setAttribute("version", "1.0.0");
        c = new Element("TimeMethod")
            .setAttribute("isIdentifiable", "true")
            .setAttribute("id", "TM_1");
        // Create the hiearachy
        b.addContent(c);
        a.addContent(b);
        
        // Identify the elements
        ida = DDI31.identify_DDI31(a);
        idb = DDI31.identify_DDI31(b);
        idc = DDI31.identify_DDI31(c);
        
        // Show results. The URN given for the <TimeMethod>
        // is in fact incorrect in the spec. Don't bother to check
        // the results against it.
        System.out.printf("a: %s\nb: %s\nc: %s\n", ida, idb, idc);
        System.out.printf("equals(ida,ida) == %s\n", ida.equals(ida));
        System.out.printf("equals(ida,idb) == %s\n", ida.equals(idb));
        System.out.printf("equals(idb,idb) == %s\n", idb.equals(idb));
        System.out.printf("equals(idb,idc) == %s\n", idb.equals(idc));
        System.out.printf("equals(idc,idc) == %s\n", idc.equals(idc));
        System.out.printf("equals(idc,ida) == %s\n", idc.equals(ida));
    } // main()
} // class ex7