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
import java.util.Stack;
// jdom imports
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Text;
import org.jdom.Document;
import org.jdom.Parent;

/**
 * Constructs XPath expressions that identify XML content nodes uniquely.
 * See the class method {@link #get_xpath(Object)}.
 *
 * Name suggestions:<br/>
 * Two different paths can be taken. The first path is to give the class 
 * a noun name of the product resulting from the process. The second path
 * is to give the class a noun name of the process. There is also a third
 * path in which a noun name of "an actor" is given to the class.<p>
 *
 * 1) The class has the name of the product. An example:
 * {@code XPathIdentity}. In this case it is possible to use method names
*  {@code obtain()} and {@code is_obtainable()}. If the method names were to 
 * be completed with prepositions, the correct one would probably be "from", 
 * eg. {@code obtain_from()} and  {@code is_obtainable_from}. To further 
 * specicy the parameter, one could name the method 
 * {@code obtain_from_attribute()}. <p>
 *
 * 2) The class has the name of the process. An example: 
 * {@code XPathIdentification}. In this case it is possible use method names 
 * {@code is_doable()} and {@code do()}, but here is a caveat, because 
 * {@code do} is a keyword in Java, and in C too. The method names could be 
 * completed with a preposition. The correct preposition in this case would 
 * probably be "for", eg. {@code is_doable_for()} and {@code do_for()}. 
 * The parameter could be specified like in the previous case, 
 * eg. {@code do_for_attribute()}. <p>
 *
 * 3) The class has a name of "an actor". An example: 
 * {@code XPathBuilder} or {@code XPathHelper} or something similar like that.
 * In this case the method names would probably have to include some hint
 * of what is being done, because a builder or a helper can do many more
 * things than just identify or test identifiabiliy. <p>
 *
 * I have chosen a kind of compromise, in which the verb "do" has been replaced
 * with a verb corresponding to the process. The argument I have for doing
 * it like this is that a method name "do_for()" is not very meaningful without
 * the class name in front of it. When looking at source code, the class is
 * well forgotten after the beginning of the file, and all what is seen is
 * just the method name. <p>
 *
 * Here are some possible name suggestions that occurred to me. 
 * The first case 1) the class has the product's name:
 * <ul>
 *      <li>{@code XPathIdentity.is_feasible()} - no; feasibility of an identity is a different thing</li>
 *      <li>{@code XPathIdentity.is_identifiable()} - yes; untied from the class' name</li>
 *      <li>{@code XPathIdentity.is_possible()} - no; the possibility of what is unspecified, and "identity" can't be "possible"</li>
 *      <li>{@code XPathIdentity.is_doable()} - no; what is being doable is unspecfied, and "identity" can't be "doable"</li>
 *      <li>{@code XPathIdentity.is_performable()} - no; what is being performable is unspecified, and "identity" can't be "performable"</li>
 *      <li>{@code XPathIdentity.is_applicable()} - no; applicability of identity is different thing, and otherwise what is being applicable to what is unspecified</li>
 *      <li>{@code XPathIdentity.identify()} - yes; untied from the class' name</li>
 *      <li>{@code XPathIdentity.execute()} - no; what will be executed is unspecified</li>
 *      <li>{@code XPathIdentity.perform()} - no; what will be performed is unspecified</li>
 *      <li>{@code XPathIdentity.obtain()} - yes; what is being obtained is implied by the class' name</li>
 *      <li>{@code XPathIdentity.obtain_from()} - yes; same as above, but defines the role of the parameter</li>
 *      <li>{@code XPathIdentity.do_for()} - no; what will be done is unspecified</li>
 *      <li>{@code XPathIdentity.create_for()} - yes; what is being created is implied, but no; identity is not created, it is just picked out</li>
 *      <li>{@code XPathIdentity.get()} - yes; what is being get is implied by the class' name</li>
 * </ul>
 * <p>
 *
 * The second case 2) the class has the process' name:
 * <ul>
 *      <li>{@code XPathIdentification.is_feasible()} - yes; whose feasibility is tested is implied by the class' name</li>
 *      <li>{@code XPathIdentification.is_identifiable()} - yes; untied from the class' name</li>
 *      <li>{@code XPathIdentification.is_possible()} - yes; whose possibility is tested is implied by the class' name</li>
 *      <li>{@code XPathIdentification.is_doable()} - yes; whose doability is tested is implied by the class' name</li>
 *      <li>{@code XPathIdentification.is_performable()} - yes; whose performability is tested is implied by the class' name</li>
 *      <li>{@code XPathIdentification.is_applicable()} - yes; whos applicability is tested is implied by the class' name</li>
 *      <li>{@code XPathIdentification.identify()} - yes; untied from the class' name</li>
 *      <li>{@code XPathIdentification.execute()} - yes; what is being executed is implied by the class' name</li>
 *      <li>{@code XPathIdentification.perform()} - yes; what is being performed is implied by the class' name</li>
 *      <li>{@code XPathIdentification.obtain()} - no; what is being obtained is unspecified, and also yes; identification is thought as what is obtain()</li>
 *      <li>{@code XPathIdentification.obtain_from()} - no; what is being obtained is unspecified</li>
 *      <li>{@code XPathIdentification.do_for()} - yes; what is being done is implied by the class' name</li>
 *      <li>{@code XPathIdentification.get()} - yes; identification is thought as the object of get()</li>
 * </ul>
 * <p>
 *
 * The third case 3) the class has an actor name:
 * <ul>
 *      <li>{@code XPathBuilder.is_feasible()} - no; whose feasibility is tested is unspecified</li>
 *      <li>{@code XPathBuilder.is_identifiable()} - yes; untied from the class' name</li>
 *      <li>{@code XPathBuilder.is_possible()} - no; whose possibility is tested is unspecified</li>
 *      <li>{@code XPathBuilder.is_doable()} - no; whose doability is tested is unspecified</li>
 *      <li>{@code XPathBuilder.is_performable()} - no; whose performability is tested is unspecified</li>
 *      <li>{@code XPathBuilder.is_applicable()} - no; whose applicability is tested is unspecified</li>
 *      <li>{@code XPathBuilder.identify()} - yes; untied from the class' name, but also no; what is being identified is unspecified</li>
 *      <li>{@code XPathBuilder.execute()} - no; what is begin executed is unspecified</li>
 *      <li>{@code XPathBuilder.perform()} - no; what is begin performed is unspecified</li>
 *      <li>{@code XPathBuilder.obtain()} - no; what is being obtained is unspecified, but also yes; XPath is implied by the class' name</li>
 *      <li>{@code XPathBuilder.obtain_from()} - no; same as above</li>
 *      <li>{@code XPathBuilder.do_for()} - no; what is being done is unspecified</li>
 *      <li>{@code XPathBuilder.get()} - no; what is being get is unspecified</li>
 * </ul>
 * Some additional suggestions for the actor name:
 * <ul>
 *      <li>{@code XPathBuilder.obtain_xpath()} - yes; untied from the class' name, and the class' name is not in contradiction</li>
 *      <li>{@code XPathBuilder.build()} - yes; what is being build is implied</li>
 *      <li>{@code XPathBuilder.build_from()} - yes; what is being build is implied</li>
 *      <li>{@code XPathBuilder.is_buildable()} - yes; what is being build is implied</li>
 * </ul>
 * More suggestions for the actor name:
 * <ul>
 *      <li>{@code XPathHelper.build_for()} - no, what is being build is unspecified</li>
 *      <li>{@code XPathHelper.build_xpath_for()} - yes, what is being build is explicitly told</li>
 *      <li>{@code XPathHelper.obtain_xpath()} - yes; what is begin obtained is epxlicitly told, and the method name is not in contradiction with the class' name</li>
 * </ul>
 * <p>
 * 
 * Well, the current one is good one, but probably not the best.
 * It is at sub-optimal, because the verb "identify" is used even though 
 * it could be implicitly understood from the class' name. However, 
 * the verb is provided explicitly for the readibility of the individual 
 * method names. The class name is visible only at the beginning of a file,
 * and well forgotten after a few method calls. In such situations a method
 * name like "do_for" or "execute()" are probably not very good, since
 * it is not so clear what should be done or executed. <p>
 *
 * In any case, I definitely wanted the identification functions to be
 * separated from the data container objects. When considering file names,
 * the file name should reflect and also emphasize the content being directly
 * related to the identification. Also, I wanted to separate technically
 * the depedency to jdom from the data container objects.<p>
 *
 */
public class XPathIdentification
{
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally unallowed.
     */
    private XPathIdentification() {
    } // ctor
    
    // PRIVATE HELPER CLASS METHODS
    //==============================
    
    /**
     * Identifies the XPath for an element with respect to a parent.
     *
     * @param parent the parent, this must equal to {@code Element.getParent()}
     * @param elem the XML element whose XPath is identified
     * @return An XPath expression identifying the element with respect
     * to the parent
     */
    public static String get_element_xpath(
        Parent parent, 
        Element elem
    ) {
        // Get the fully qualified name of the XML element
        String qname = elem.getQualifiedName();

        // The return variable
        // TBD: Pre-format with assumption that disambiguation isn't needed?
        String nodespec = null;
        
        // If the parent is non-null, determine whether there are multiple 
        // child XML elements with the same qualified name. If so, include
        // more detailed specification of the node.
        
        if (parent != null) {
            
            // Number of child Element objects with the same qualified name
            int qname_count = 0; 
            // The qname_count at "element".
            int match_number = 0;
            
            for (Object obj : parent.getContent()) {
                if (obj instanceof Element) {
                    Element child = (Element) obj;
                    // If the child element's qualified name matches,
                    // increase the match count.
                    if (child.getQualifiedName().equals(qname)) {
                        qname_count++;
                    }
                    // If the child is the specified "elem", record
                    // the number of qname_count.
                    if (child == elem) {
                        match_number = qname_count;
                        // The loop cannot be breaked just yet!
                        // Otherwise, it won't be possible to know reliably 
                        // is there more than just one XML element with this
                        // particular qualified name.
                    } // if
                } // if: an Element
            } // for: each child content
            
            // Is more detailed node specification needed?
            if (qname_count > 1) {
                // Disambiguation is required. There are more than one
                // child XML elements with the same qualified name.
                // The match number is needed.
                nodespec = String.format("%s[%d]", qname, match_number);
            } else {
                // No disambiguation required. The match number is not 
                // neccessarily needed.
                nodespec = String.format("%s", qname);
            } // if-else: not a unique qname?
            
        } else {
            // No parent; no disambiguation needed.
            nodespec = String.format("%s", qname);
        } // if-else: has a parent 
        
        return nodespec;
    } // get_element_xpath()
    
    /**
     * Identifies the XPath for an attribute with respect to a parent.
     *
     * @param parent the parent, this must equal to 
     * {@code Attribute.getParent()}; however, this is currently unused
     * @param attr the XML attribute whose XPath is identified
     * @return An XPath expression identifying the attribute with respect
     * to the parent
     */
    public static String get_attribute_xpath(
        Parent parent, 
        Attribute attr
    ) {
        // Get the fully qualified name of the attribute
        String qname = attr.getQualifiedName();
        
        // Return variable
        String nodespec = String.format("@%s", qname);
        
        return nodespec;
    } // get_attribute_xpath()
    
    /**
     * Identifies the XPath for a text node with respect to a parent.
     *
     * @param parent the parent, this must equal to 
     * {@code Text.getParent()}; however, this is currently unused
     * @param text the XML text node whose XPath is identified
     * @return An XPath expression identifying the text node with respect
     * to the parent
     */
    private static String get_text_xpath(
        Parent parent,
        Text text
    ) {
        // Return variable
        String nodespec = String.format("text()");
        
        return nodespec;
    } // get_text_xpath()
    
    /**
     * Identifies the XPath for an XML document with respect to a parent.
     *
     * @param parent the parent, this should be {@code null}; however, 
     * the parameter is currently unused
     * @param doc the XML document whose XPath is identified
     * @return An XPath expression identifying the document node with respect
     * to the parent. The return value is an empty string.
     */
    private static String get_document_xpath(
        Parent parent,
        Document doc
    ) {
        // TODO: The correct XPath would be document('filename.xml')
        // but that is undesired in most use cases, because the document
        // is implied.
        
        // Return variable.
        String nodespec = "";
        
        return nodespec;
    } // get_document_xpath()
    
    // CLASS METHODS
    //===============
    
    /**
     * Identifies the XPath for an XML content node within its document.
     *
     * @param obj the XML content node whose XPath is identified
     * @return An XPath expression identifying the XML content node
     * within its document.
     * @throws RuntimeException if an object of unexpected dynamic type
     * is encountered
     */
    public static String get_xpath(Object obj) {
        // The XPath string is built one node at a time. The build direction 
        // is from the end (the most deepest node) to the beginning (the root
        // node). Stack is utilized to record each node's XPath specification.
        Stack<String> stack = new Stack<String>();
        
        // Sum the string lengths of the nodespecs while building them.
        int len = 0;
        
        // Repeat while the object is not null (ie. the parent did exist)
        while (obj != null) {
            
            // The current node's XPath specification with respect to
            // its parent. The actual contents are determined based on
            // the dynamic type of the current node.
            String nodespec = null;
            
            // The value for the Object "obj" in the next iteration.
            Parent parent = null;
            
            // Determine the dynamic of the object
            if (obj instanceof Element) {
                // Cast
                Element elem = (Element) obj;
                // Get the parent; 
                // this is an instance of Element, Document, or null.
                parent = elem.getParent();
                // Determine the node spec
                nodespec = get_element_xpath(parent, elem);
            } 
            else if (obj instanceof Attribute) {
                // Cast
                Attribute attr = (Attribute) obj;
                // Get the parent; 
                // this is an instance of Element or null.
                parent = attr.getParent(); 
                // Determine the node spec
                nodespec = get_attribute_xpath(parent, attr);
            }
            else if (obj instanceof Text) {
                // Cast
                Text text = (Text) obj;
                // Get the parent;
                // This is an instance of Element or null.
                parent = text.getParent();
                // Determine the node spec
                nodespec = get_text_xpath(parent, text);
            }
            else if (obj instanceof Document) {
                // Cast
                Document doc = (Document) obj;
                // No parent
                parent = null;
                // Determine the node spec
                nodespec = get_document_xpath(parent, doc);
            }
            else {
                // Unexpected object type. Throw an error
                throw new RuntimeException(String.format(
                    "Argument has an unexpected dynamic type: %s",
                    obj.getClass().getName()));
            } // if-else
            
            // Assert: the nodespec should be non-null by now.
            
            // Add the nodespec's length to the total length
            len += nodespec.length();
            
            // Push the current nodespec to the stack
            stack.push(nodespec);
            
            // Update the loop variable for the next iteration
            obj = parent;
        } // while
        
        // After each node's XPath has been identified, the nodespecs
        // are combined to give the final XPath identifier.
        
        // Adjust the total length of the node specs with by taking 
        // the required separators into account
        len = len + stack.size() * 1;
        
        // Use StringBuilder to create the final XPath identifier.
        StringBuilder sb = new StringBuilder(len);
        
        // Auxiliary variable to test if the current iteration is the first.
        boolean first = true;
        
        // Traverse the stack from top to bottom.
        for (int i = stack.size()-1; i >= 0; i--) {
            if (!first) {
                sb.append("/");
            }
            sb.append(stack.elementAt(i));
            first = false;
        } // for
        
        return sb.toString();
    } // get_xpath()

} // class XPathIdentification

