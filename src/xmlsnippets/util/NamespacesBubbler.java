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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
// jdom imports
import org.jdom.Element;
import org.jdom.Namespace;

// imports for main(); these could be removed by removing main().
import java.io.File;
import xmlsnippets.util.XMLFileHelper;
import org.jdom.Document;


/**
 * Bubbles XML namespaces upstream in the tree hierarchy in order
 * to reduce redudant namespace declarations.
 *
 * The important details regarding XML namespaces, see
 * <a href="http://www.w3.org/TR/REC-xml-names/#scoping-defaulting">
 * Namespaces in XML 1.0 (Third Edition)
 * </a><p>
 *
 * The most important detail of the forementioned specification 
 * is probably this:<br>
 * "The scope of a namespace declaration declaring a prefix extends from 
 * the beginning of the start-tag in which it appears to the end of 
 * the corresponding end-tag, <b>excluding the scope of any inner declarations 
 * with the same NSAttName part</b>. In the case of an empty tag, the scope is 
 * the tag itself."<p>
 *
 * Also, regarding the jdom library, it is important to be aware of how
 * jdom manages the equivalence for {@code Namespace} objects. See
 * <a href="http://www.jdom.org/docs/apidocs.1.1/org/jdom/Namespace.html">
 * here
 * </a>.
 * The thing is that jdom does not include the prefix in any way in
 * the equivalence. Two {@code Namespace} objects are equal iff they have
 * the same URI byte-wise.<p>
 *
 * Also, what is not explicitly told in the jdom v1.1.3 documentation
 * is that in the case of default namespace {@code getPrefix()} returns
 * an empty string.<p>

 * 
 */
public class NamespacesBubbler {

    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally disabled.
     */
    private NamespacesBubbler() {
    } // ctor
    
    // CLASS METHODS
    //===============
    
    /**
     * Bubbles all namespaces present in the element and in its children
     * as close to the element as possible. The process attempts to bubble
     * all namespaces as up as possible. If at some level the direct children
     * define conflicting namespace prefixes or multiple prefixes for a single
     * namespace URI, those namespaces are left there.<p>
     *
     * @param element bubbles namespaces in the children recursively upwards.
     * @return The namespaces which can be bubbled from the specified element
     * upstream and which namespaces caused conflicts in the prefixes.
     *
     */
    public static List<Namespace> bubble_namespaces_greedy(Element element) {
        // Depth first
        Map<Element, List<Namespace>> map =
            new LinkedHashMap<Element, List<Namespace>>();
        
        for (Object obj : element.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            
            // Depth-first recursion
            Element c = (Element) obj;
            List<Namespace> rval = null;
            rval = bubble_namespaces_greedy(c);
            map.put(c, rval);
        } // for
        
        // Create a set containing all different namespaces in the children
        List<Namespace> set = new LinkedList<Namespace>();
        
        for (Map.Entry<Element, List<Namespace>> entry 
            : map.entrySet())
        {
            for (Namespace a : entry.getValue()) {
                // Pick these into local variables for convenience
                // and to avoid frequently calling the member methods..
                String uri = a.getURI();
                String prefix = a.getPrefix();
                
                boolean already = false;
                
                for (Namespace b : set) {
                    boolean uri_equal = uri.equals(b.getURI());
                    boolean prefix_equal = prefix.equals(b.getPrefix());
                    
                    if (uri_equal & prefix_equal) {
                        // already included
                        already = true;
                        break;
                    }
                } // for: all total
                
                if (already == false) {
                    set.add(a);
                } // if: not already
            } // for each ns
        } // for
        
        // The list "set" contains now all namespaces present in all
        // children. Next the conflicting ones need to be singled out.
        
        // Return namespaces for this element
        List<Namespace> pset = new LinkedList<Namespace>();
        Namespace pns = element.getNamespace();
        if (pns != null) {
            pset.add(pns);
        }
        
        for (Object obj : element.getAdditionalNamespaces()) {
            pset.add((Namespace) obj);
        } // for
        
        // The list "pset" contains now all namespaces present
        // in the parent element itself.
        
        List<Namespace> set2 = new LinkedList<Namespace>();
        
        for (Namespace ns1 : set) {
            String uri = ns1.getURI();
            String prefix = ns1.getPrefix();
            
            boolean conflicting = false;
            for (Namespace ns2 : set) {
                if (ns1 == ns2) {
                    continue;
                }
                boolean uri_eq = uri.equals(ns2.getURI());
                boolean p_eq = prefix.equals(ns2.getPrefix());
                
                if (p_eq != uri_eq) {
                    // Conflict. Drop both ns1 and ns2.
                    set2.add(ns1);
                    conflicting = true;
                    // The ns2 will come..
                    break;
                } // if
            } // for
            
            if (conflicting) {
                continue;
            }
            
            // Make sure that ns1 does not conflict with the parent either
            for (Namespace ns2 : pset) {
                if (ns1 == ns2) {
                    continue;
                }
                boolean uri_eq = uri.equals(ns2.getURI());
                boolean p_eq = prefix.equals(ns2.getPrefix());
                
                if (p_eq != uri_eq) {
                    // Conflict. Drop both ns1 only; it cannot be
                    // propagated more upwards.
                    set2.add(ns1);
                    conflicting = true;
                    break;
                } // if
            }
        } // for
        
        // the list "set2" is now a list of all conflicting nodes
        // in the children
        set.removeAll(set2);
        // At this point:
        //      set:  a list of all namespaces which can be propagated
        //            upwards without conflicts.
        //      set2: a list of all namespaces which are conflicting
        
        for (Map.Entry<Element, List<Namespace>> entry 
            : map.entrySet())
        {
            // see which namespaces can be propagated to this..
            List<Namespace> list = entry.getValue();
            Element child = entry.getKey();
            
            for (Namespace ns : list) {
                // ------- Find if ns belongs in set
                boolean contains = false;
                String uri = ns.getURI();
                String p = ns.getPrefix();
                for (Namespace x : set) {
                    boolean uri_eq = uri.equals(x.getURI());
                    boolean p_eq = p.equals(x.getPrefix());
                    if (p_eq && uri_eq) {
                        contains = true;
                        break;
                    }
                } // for
                // if "ns" is contained in "set",
                // it can be removed from the child
                
                if (contains) {
                    // Namespace "ns" can be propagated
                    child.removeNamespaceDeclaration(ns);
                } // if: contains
            } // for
        } // for
        
        // Add all not in pset to the parent
        List<Namespace> rval = new LinkedList<Namespace>();
        
        for (Namespace ns : set) {
            String uri = ns.getURI();
            String p = ns.getPrefix();
            boolean contains = false;
            for (Namespace x : pset) {
                boolean uri_eq = uri.equals(x.getURI());
                boolean p_eq = p.equals(x.getPrefix());
                if (p_eq && uri_eq) {
                    contains = true;
                    break;
                }
            } // for
            
            if (!contains) {
                element.addNamespaceDeclaration(ns);
                rval.add(ns);
            }
        } // for
        
        rval.addAll(pset);
        
        return rval;
    } // bubble_namespaces_greedy()

    
    /**
     * The results set of the bubbler. The primary reason for the existence
     * of this nested class is to allow the bubbling function have a return
     * value which consists of more than just a single {@code List} object;
     * In Java it is not possible to easily to define a tuple return value.
     * In C++ one would have probably used a template similar to "tuple<T, U>"
     * or something.<p>
     *
     * Because of how the equivalence is implemented in the {@code Namespace}
     * lists have to be used instead of sets.
     * 
     */
    public static class Results {
        
        // MEMBER VARIABLES
        //==================
        
        /**
         * Conflicting namspaces, these can't be bubbled.
         */
        public List<Namespace> conflict;
        
        /**
         * Unconflicting namespaces, these are bubbling.
         */
        public List<Namespace> bubble;
        
        // CONSTRUCTORS
        //==============
        
        public Results() {
            conflict = new LinkedList<Namespace>();
            bubble = new LinkedList<Namespace>();
        } // ctor
    } // class Results
    
    
    /**
     * Provided for convenience.
     */
    public static List<Namespace> collect_namespaces(
        Element element
    ) {
        return collect_namespaces(element, null);
    } // collect_namespaces()
    
    /**
     * Collects all namespaces found from the current element and from its all 
     * children. The initial call should pass either an empty {@code List} or 
     * a [@code null} value.
     *
     * @param element the element from which the namespaces are recursively
     * collected.
     * @param list the current list of collected namespaces, or {@code null},
     * if none yet.
     * @return List of all namespaces which differ either in uri or in prefix.
     * The returned list contains each unique pair only once.
     */
    public static List<Namespace> collect_namespaces(
        Element element,
        List<Namespace> list
    ) {
        
        // Breadth-first
        
        List<Namespace> pset = new LinkedList<Namespace>();
        
        // First: collect all namespaces present in the current element
        
        // The namespace of the XML element itself, or null if none.
        Namespace pns = element.getNamespace();
        if (pns != null) {
            pset.add(pns);
        }
        
        // The additional namespace declarations present in this XML element.
        for (Object obj : element.getAdditionalNamespaces()) {
            pset.add((Namespace) obj);
        } // for

        // Second: insert all those namespaces into the master list
        // which aren't already there.
        
        if (list == null) {
            list = new LinkedList<Namespace>();
        }
        
        for (Namespace ns : pset) {
            // Ignore default namespaces. 
            if (ns.getPrefix().equals("")) {
                continue;
            } // if: default ns
            
            // If the exactly same ns is already in the return list,
            // do nothing.
            if (contains_same_ns(list, ns)) {
                continue;
            } // if: already there
            
            // Otherwise, good to add
            list.add(ns);
        } // for: each ns defined in the current elemenet
        
        // Then pass the list to all children to populate
        for (Object obj : element.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            } // if
            list = collect_namespaces((Element) obj, list);
        } // for: each child
        
        return list;
    } // collect_namespaces()
    
    /**
     * Bubbles the namespaces in element and in all its children prudently
     * upstream. More specifically, all conflicting elements are determined
     * prior to bubbling, and only unconflicting elements are bubbled
     * immediately to the specified element.<p<
     *
     * Default namespaces are left where they are found.
     *
     */
    public static NamespacesBubbler.Results bubble_namespaces_prudent(
        Element element
    ) {
        // The return variable
        NamespacesBubbler.Results rval = new NamespacesBubbler.Results();
        
        // Collect all namespaces
        List<Namespace> all = collect_namespaces(element, null);
        
        // Partition the namespaces
        for (Namespace ns : all) {
            // Not equal but similar ones are put into conflict category,
            // and others into bubble.
            if (contains_similar_ns(all, ns)) {
                rval.conflict.add(ns);
            } else {
                rval.bubble.add(ns);
            } // if-else
        } // for: each namespace
        
        // Now bubble those that can be bubbled.
        remove_namespaces(element, rval.bubble);
        
        // And populate the current element..
        for (Namespace ns : rval.bubble) {
            element.addNamespaceDeclaration(ns);
        } // for
        
        
        return rval;
    } // bubble_namespaces_prudent

    /**
     * Removes the specified namespaces recursively from all children.
     *
     * @param element the element from which the specified namespaces should
     * be removed recursively.
     * @param list the namespaces to remoev
     */
    public static void remove_namespaces(
        Element element,
        List<Namespace> list
    ) {
        // Depth first
        for (Object obj : element.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            remove_namespaces((Element) obj, list);
        } // for

        // Remove all specified namespaces. Done in a manner which is
        // independent of the referential equivalence. Also, this is independent
        // of the Namespace objects equivalence, which accounts only for URI.
        
        Namespace pns = element.getNamespace();
        if (pns != null) {
            if (contains_same_ns(list, pns)) {
                element.removeNamespaceDeclaration(pns);
            }
        } // if
        
        List<Namespace> del = new LinkedList<Namespace>();
        for (Object obj : element.getAdditionalNamespaces()) {
            Namespace ns = (Namespace) obj;
            if (contains_same_ns(list, ns)) {
                // Schedule for deletion; cannot delete immedately,
                // because it would be concurrent modification
                del.add(ns);
            }
        } // for
        
        // Execute deletion
        for (Namespace ns : del) {
            // from jdom's javadoc: "If the declaration is not present, 
            // this method does nothing."
            element.removeNamespaceDeclaration(ns);
        } // for
        
    } // remove_namespaces()
    
    /**
     * Returns true if the set contains exactly the same {@code Namespace}.
     *
     * @param ns the namespace to look for
     * @param set the set of namespaces which are looked through.
     * @return {@code true} if there is the same namespace in the set.
     * Otherwise {@code false} is returned.
     *
     */
    private static boolean contains_same_ns(
        List<Namespace> set,
        Namespace ns
    ) {
        // Pick both uri and prefix to a local variable for convenience.
        String uri = ns.getURI();
        String p = ns.getPrefix();

        // Traverse all namespaces in the set
        for (Namespace x : set) {
            
            // Make comparisons
            boolean uri_eq = uri.equals(x.getURI());
            boolean p_eq   = p.equals(x.getPrefix());
            
            // If either uri or prefix equals
            if (uri_eq && p_eq) {
                return true;
            } // if
            
            // If 
        } // for
        
        return false;
    } // contains_same_ns()

    /**
     * Returns true if the set contains a similar namespace.
     * A similar namespace is one which has either equal prefix or 
     * equal uri, but not both equal.
     *
     * @param ns the namespace to look for
     * @param set the set of namespaces which are looked through.
     * @return {@code true} if there is a similar namespace in the set.
     * Otherwise {@code false} is returned.
     *
     */
    private static boolean contains_similar_ns(
        List<Namespace> set,
        Namespace ns
    ) {
        // Pick both uri and prefix to a local variable for convenience.
        String uri = ns.getURI();
        String p = ns.getPrefix();

        // Traverse all namespaces in the set
        for (Namespace x : set) {
            
            // Make comparisons
            boolean uri_eq = uri.equals(x.getURI());
            boolean p_eq   = p.equals(x.getPrefix());
            
            if (uri_eq && p_eq) {
                // The same, okay.
                continue;
            }
            
            // If only either uri or prefix equals
            if (uri_eq || p_eq) {
                return true;
            } // if
            
            // If 
        } // for
        
        return false;
    } // contains_similar_ns
    
    
    
    
    /** Exit code for succesful run. */
    public static final int EXIT_SUCCESS = 0;
    
    /** Exit code for unsuccesful run. */
    public static final int EXIT_FAILURE = 1;
    
    /*
    public static void display_namespaces(String indent, Element elem) {
        System.out.printf("%sElement: %s\n", indent, elem.getQualifiedName());
        Namespace ns = elem.getNamespace();
        if (ns != null) {
        System.out.printf("%sNamespace: p=\"%s\", uri=\"%s\"\n", 
                indent, ns.getPrefix(), ns.getURI());
        } else {
        System.out.printf("%sNamespace: null", indent);
        } // if: has ns

        List list = elem.getAdditionalNamespaces();
        if (list.size() > 0) {
            for (Object obj : list) {
                Namespace x = (Namespace) obj;
                System.out.printf("%sAdditional: p=\"%s\", uri=\"%s\"\n", 
                        indent, x.getPrefix(), x.getURI());
            
            } // for
        } else {
            System.out.printf("%sAdditional: none\n", indent);
        } // if-else
        
        // Recurse
        indent = indent + "  ";
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            display_namespaces(indent, (Element) obj);
        } // for: each child
    } // display_namespaces
    */
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.printf("Not enough arguments!\n");
            System.out.printf("args: <source_file> <target_file> [greedy|prudent]\n");
            System.exit(EXIT_FAILURE);
        } // if
        
        try {
            Document doc = null;
            doc = XMLFileHelper.deserialize_document(new File(args[0]));
            /*
            display_namespaces("", doc.getRootElement());
            
            List<Namespace> list = collect_namespaces(doc.getRootElement());
            System.out.printf("All namespaces\n");
            for (Namespace ns : list) {
                System.out.printf("  Namespace: p=\"%s\", uri=\"%s\"\n", 
                    ns.getPrefix(), ns.getURI());
            } // for
            // TODO:
            // Bubble, and serialize the output
            */
            Element root = doc.getRootElement();
            if (args.length > 2) {
                NamespacesBubbler.Results results = null;
                if (args[2].equals("greedy")) {
                    bubble_namespaces_greedy(root);
                } else if (args[2].equals("prudent")) {
                    results = bubble_namespaces_prudent(root);
                } else {
                    System.out.printf("The third argument should be either \"greedy\" or \"prudent\"\n");
                    System.out.printf("Assuming prudent\n");
                    results = bubble_namespaces_prudent(root);
                }
                
                if (results != null) {
                    System.out.printf("Conflicting (unbubbled) namespaces\n");
                    for (Namespace ns : results.conflict) {
                        System.out.printf("  Namespace: p=\"%s\", uri=\"%s\"\n", 
                            ns.getPrefix(), ns.getURI());
                    } // for: each ns
                    System.out.printf("Total %d namespaces\n", results.conflict.size());
                } // if: has results
                
            } else {
                bubble_namespaces_greedy(root);
            }
            
            XMLFileHelper.serialize_document_verbatim(doc, new File(args[1]));
            
            
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(EXIT_FAILURE);
        } // try-catch
        
        System.exit(EXIT_SUCCESS);
    } // main()

} // class NamespacesBubbler

