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

public class ex7 {

    // A class to represent an element id in the set ID
    public static class DDI31_ID {
        // value of @id attribute
        public String agency_name;
        public String maintainable_name;
        public String maintainable_version;
        public String identifiable_name;
        public String versionable_version;
        
        // Ctor
        public DDI31_ID(
            String agency,
            String mname, 
            String mversion,
            String iname,
            String vversion
        ) {
            agency_name = agency;
            maintainable_name = mname;
            maintainable_version = mversion;
            identifiable_name = iname;
            versionable_version = vversion;
        } // ctor
        
        public String toString() {
            if (identifiable_name != null) {
                // sub-maintainable
                return String.format("%s:%s.%s:%s.%s",  
                    agency_name,
                    maintainable_name,
                    maintainable_version,
                    identifiable_name,
                    versionable_version
                );
            } else {
                // maintainable
                return String.format("%s:%s.%s",  
                    agency_name,
                    maintainable_name,
                    maintainable_version
                );
            } // if-else: sub-maintainable?
        } // toString()
        
        // equivalence for elements of the set ID
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            
            // other != null
            DDI31_ID id = null;
            if (other instanceof DDI31_ID) {
                // cast
                id = (DDI31_ID) other;
            } else {
                return false;
            } // if-else
            
            // compare first part
            if (this.agency_name.equals(id.agency_name)
                && this.maintainable_name.equals(id.maintainable_name)
                && this.maintainable_version.equals(id.maintainable_version))
            {
                // Inconclusive, but so far equal.
            } else {
                // mismatch in the agency/maintainable part
                return false;
            } // if-else
            
            // compare second part
            if ((this.identifiable_name == null)
                && (id.identifiable_name == null)) 
            {
                // both are maintainables; no versionable/identifiable part
                return true;
            } // if
            
            // either both or only of of them are non-maintainables.
            if (((this.identifiable_name == null)
                && (id.identifiable_name != null))
                ||
                ((this.identifiable_name != null)
                && (id.identifiable_name == null)))
            {
                // one is maintainable and the other is non-maintainable.
                return false; // fails.
            } // if
            
            // both are non-maintainables, 
            // both have non-null identifiable name,
            // and consequently both have non-null versionable version.
            if (this.identifiable_name.equals(id.identifiable_name)
                && this.versionable_version.equals(id.versionable_version))
            {
                // matches!
                return true;
            }
            // Otherwise mismatches in the versionable/identifiable part
            
            return false;
        } // equals()
    } // class DDI31_ID
    
    public static String get_actual_version(String version) {
        // Yeah, remember the value of version is assumed
        // to be 1.0.0 if it is not stated.
        if (version == null) {
            // default
            version = "1.0.0";
        } // if: no version provided
        
        return version;
    } // get_actual_version()
    
    // Identify the XML element x in the subset X of XML.
    // (DDI-Lifecycle 3.1 case-study)
    public static DDI31_ID identify_DDI31(Element x) 
        throws DataConversionException
    {
        String agency_name = null;
        String maintainable_name = null;
        String maintainable_version = null;
        String identifiable_name = null;
        String versionable_version = null;
        
        // Fill the identifiable name if the element is sub-maintainable
        if ((x.getAttribute("isIdentifiable") != null)
            || (x.getAttribute("isVersionable") != null))
        {
            identifiable_name = String.format(
                "%s.%s", 
                x.getName(),
                x.getAttributeValue("id")
            ); // format()
            
            // Find the parent versionable 
            // Note: maintainable is-a versionable.
            while ((x != null) && 
                (x.getAttribute("isVersionable") == null)
                && (x.getAttribute("isMaintainable") == null))
            {
                x = x.getParentElement();
            } // while: not null and non-identifiable
            assert x != null;
            
            versionable_version = get_actual_version(
                x.getAttributeValue("version")
            ); // get_actual_version()
            
                
        } // if: sub-maintainable
        
        // Find the parent maintainable
        while ((x != null) &&
            (x.getAttribute("isMaintainable") == null))
        {
            x = x.getParentElement();
        } // while: not null and non-maintianable
        assert x != null;
        
        maintainable_name = String.format(
            "%s.%s", 
            x.getName(),
            x.getAttributeValue("id")
        ); // format()
        
        maintainable_version = get_actual_version(
            x.getAttributeValue("version")
        ); // get_actual_version()
        
        // Find agency
        while ((x != null) 
            && (x.getAttribute("agency") == null)) 
        {
            x = x.getParentElement();
        }
        assert x != null;
        
        agency_name = x.getAttributeValue("agency");
        
        return new DDI31_ID(
            agency_name,
            maintainable_name, maintainable_version,
            identifiable_name, versionable_version
        ); // return
    } // identify_DDI31()
    
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
        ida = identify_DDI31(a);
        idb = identify_DDI31(b);
        idc = identify_DDI31(c);
        
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