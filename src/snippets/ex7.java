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
        
        /** Constructor.
         * @param agency the agency name, eg. "fi.dsf"
         * @param mname the MaintainableType element name, including the XML 
         * Schema type name. Example: "CategoryScheme.ISCO_2012".
         * @param mversion the version number of the MaintainableType element.
         * @param iname the IdentifiableType element name, including the XML
         * Schema type name, within the MaintainableType element. If the element is MaintainableType,
         * leave null. Example: "TimeMethod.TM".
         * @param vversion the version number from the next VersionableType
         * parent element. If the element is MaintainableType, leave null.
         */
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
        
        /**
         * Converts the identity into its standard String representation.
         * Example: for an IdentifiableType or VersionableType element,
         * "fi.dsf:CategoryScheme.ISCO_2012.1.2.3:TimeMethod.TM_1.1.0.0".
         * example: for an MaintainableType element,
         * "fi.dsf:CategoryScheme.ISCO_2012.1.2.3"
         * @return the standard representation of the identity
         */
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
        
        /** Test equivalence of the identity objects.
         * @return true all parts match. Otherwise, false.
         */
        public boolean equals(Object other) {
            // Null object. Return false immediately, and
            // don't bother to cast. 
            if (other == null) {
                return false;
            }
            
            // Here: other != null
            
            // Non-null object. Determine if it is an instance
            // of DDI31_ID class. If so, do dynamic cast
            DDI31_ID id = null;
            if (other instanceof DDI31_ID) {
                // An instance of this class; dynamic cast
                id = (DDI31_ID) other;
            } else {
                // Not an instance of this class. Return false.
                return false;
            } // if-else
            
            // Compare the first part: agency + maint_name + maint_version
            // compare first part
            if (this.agency_name.equals(id.agency_name)
                && this.maintainable_name.equals(id.maintainable_name)
                && this.maintainable_version.equals(id.maintainable_version))
            {
                // All equal.
                // This is still inconclusive, so more tests are needed.
            } else {
                // Mismatch in the first part. Return false.
                return false;
            } // if-else
            
            // Determine whether both objects have null second parts
            
            if ((this.identifiable_name == null)
                && (id.identifiable_name == null)) 
            {
                // Both are maintainables; no versionable/identifiable part.
                // Since the first part was equal and there is no second part,
                // the objects are equal.
                return true;
            } // if
            
            // Check if one is sub-maintainable and the other is maintainable.
            if (((this.identifiable_name == null)
                && (id.identifiable_name != null))
                ||
                ((this.identifiable_name != null)
                && (id.identifiable_name == null)))
            {
                // One is MaintainableType and the other is sub-Maintainabletype.
                // The objects cannot match, so they are unequal.
                return false;
            } // if
            
            // Both objects are identities of a sub-MaintainableType,
            // and therefore both have non-null second part.
            // Compare the second part: identifiable_name + versionable_version
            if (this.identifiable_name.equals(id.identifiable_name)
                && this.versionable_version.equals(id.versionable_version))
            {
                // Second part matches too, so the objects are equal!
                return true;
            }
            
            // Otherwise mismatches in the versionable/identifiable part,
            // and the objects are unequal.
            
            return false;
        } // equals()
    } // class DDI31_ID
    
    /**
     * Returns the version number corresponding to @version attribute
     * value; this takes into accound the default version number "1.0.0".
     * @param version the value of the @version attribute, or null
     * if no such attribute.
     * @return the version number
     */
    public static String get_actual_version(String version) {
        // Yeah, remember the value of version is assumed
        // to be 1.0.0 if it is not stated.
        if (version == null) {
            // default
            version = "1.0.0";
        } // if: no version provided
        
        return version;
    } // get_actual_version()
    
    /** 
     * Creates a DDI31_ID object representing an XML elements identity
     * in terms of DDi-Lifecycle 3.1.
     * @param x the element for which the identity object is built
     * @return the object representing the identity.
     */
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