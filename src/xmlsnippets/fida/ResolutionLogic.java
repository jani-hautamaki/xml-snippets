//*******************************{begin:header}******************************//
//     XML Processing Snippets - https://code.google.com/p/xml-snippets/     //
//***************************************************************************//
//
//      xml-snippets:   XML Processing Snippets 
//                      with Some Theoretical Considerations
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

package xmlsnippets.fida;

// core java
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
// jdom
import org.jdom.Element;
// xmlsnippets
import xmlsnippets.core.Xid;
import xmlsnippets.core.XidString;
import xmlsnippets.core.Xref;
import xmlsnippets.core.XrefString;
import xmlsnippets.core.PidIdentification;


/**
 * A reference to a property or xidentified XML element.
 */
public class ResolutionLogic
{
    
    public static class XrefBinding {
        // What was resolved, either property or a xid
        public String property;
        public Xid xid;
        
        // What it was resolved to
        public Element element;
        public Fida.Node node;
        
        public XrefBinding(String property) {
            this.property = property;
            this.xid = null;
            
            this.element = null;
            this.node = null;
        }
        
        public XrefBinding(String property, Element elem) {
            this.property = property;
            this.xid = null;
            
            this.element = elem;
            this.node = null;
        }

        public XrefBinding(String property, Fida.Node node) {
            this.property = property;
            this.xid = null;
            
            if (node != null) {
                this.element = node.payload_element;
                this.node = node;
            } else {
                this.element = null;
                this.node = null;
            }
        }
        
        public XrefBinding(Xid xid, Fida.Node node) {
            this.property = null;
            this.xid = xid;
            
            if (node != null) {
                this.element = node.payload_element;
                this.node = node;
            } else {
                this.element = null;
                this.node = null;
            }
        }
    }
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Intentionally private
     */
    private ResolutionLogic() {
    } // ctor

    
    // OTHER METHODS
    //===============
    
    public static List<XrefBinding> get_bindings(
        Xref xref, 
        AbstractRepository db
    ) {
        List<XrefBinding> bindings = new LinkedList<XrefBinding>();

        Xid base = xref.base;
        Fida.Node fromNode = db.get_node(base);
        if (fromNode == null) {
            return null;
        }
        
        bindings.add(new XrefBinding(xref.base, fromNode));

        
        ListIterator<String> iter = xref.path.listIterator();
        Element elem = fromNode.payload_element;
        
        Element found = elem;
        while (iter.hasNext()) {
            String pid = iter.next();
            
            found = bfs_search(pid, elem);
            
            if (found == null) {
                bindings.add(new XrefBinding(pid));
                continue;
            }
            
            String s = elem.getAttributeValue("ref_xid");
            
            if (s != null) {
                // An inclusion-by-xid; resolve it
                Xid ref_xid = XidString.deserialize(s);
                Fida.Node node = db.get_node(ref_xid);
                if (node == null) {
                    throw new RuntimeException(String.format(
                        "Database integrity violated, ref_xid not found: %s",
                        s));
                }
                found = node.payload_element;
                
                bindings.add(new XrefBinding(pid, node));
            } else {
                bindings.add(new XrefBinding(pid, found));
            } // if-else
            
            elem = found;
        } // while: has next
        
        return bindings;
    }
    
    public static Element resolve(
        Xref xref, 
        AbstractRepository db
    ) {
        Xid base = xref.base;
        Fida.Node fromNode = db.get_node(base);
        if (fromNode == null) {
            return null;
        }
        
        //ListIterator<String>
        
        return resolve(xref.path.listIterator(), fromNode.payload_element, db);
    } // resolve()
    
    public static Element resolve(
        ListIterator<String> iter, 
        Element elem,
        AbstractRepository db
    ) {
        
        Element found = elem;
        while (iter.hasNext()) {
            String pid = iter.next();
            
            found = bfs_search(pid, elem);
            
            if (found == null) {
                // Stop search here
                break;
            }

            String s = found.getAttributeValue("ref_xid");
            
            if (s != null) {
                // An inclusion-by-xid; resolve it
                Xid ref_xid = XidString.deserialize(s);
                Fida.Node node = db.get_node(ref_xid);
                if (node == null) {
                    throw new RuntimeException(String.format(
                        "Database integrity violated, ref_xid not found: %s",
                        s));
                }
                found = node.payload_element;
            }
            
            elem = found;
        } // while: has next
        
        return found;
    }
    
    public static Element bfs_search(String pid, Element elem) {
        if (elem == null) {
            return null;
        }
        
        Element found = null;
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                // Skip this
                continue;
            }
            
            // Otherwise check it.
            Element child = (Element) obj;
            
            // See if the child has the pid we are looking for
            String child_pid = PidIdentification.get_pid(child);
            
            // See if we are to use default value for the pid
            if ((child_pid != null) && (child_pid.equals(""))) {
                /*
                String s = child.getAttributeValue("ref_xid");
                if (s == null) {
                    throw new RuntimeException(String.format(
                        "Database integrity violated, default property name specified, but no the element has no ref_xid"));
                }
                Xid ref_xid = XidString.deserialize(s);
                child_pid = ref_xid.id;
                */
                
                child_pid = child.getName();
            }
            
            if ((child_pid != null) && (child_pid.equals(pid))) {
                // Yes we found the one we are looking for.
                found = child;
            } else if (child_pid == null) {
                // If it is an inclusion-by-xid, the inclusion is not
                // followed, since the properties are local.
                
                // If the element has a property name, then the element
                // is not followed, since property introduces a new scope.
                
                found = bfs_search(pid, child);
            } // if-else

            if (found != null) {
                // Property found. Stop searching immediately.
                break;
            } // if: found
        }
        
        return found;
    }
    
    
} // class Xid