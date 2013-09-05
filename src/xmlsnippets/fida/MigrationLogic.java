//*******************************{begin:header}******************************//
//     XML Processing Snippets - https://code.google.com/p/xml-snippets/     //
//***************************************************************************//
//
//      xml-snippets:   XML Processing Snippets 
//                      with Some Theoretical Considerations
//
//      Copyright (C) 2012 Jani Hautamki <jani.hautamaki@hotmail.com>
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
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.io.File;

// jdom
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Document;

// fida
import xmlsnippets.util.XMLFileHelper;
import xmlsnippets.util.XPathIdentification;
import xmlsnippets.fida.XidClient.FidaRepository;
import xmlsnippets.fida.XidClient;
import xmlsnippets.core.Xid;
import xmlsnippets.core.XidString;
import xmlsnippets.core.XidIdentification;
import xmlsnippets.fida.Fida;

/**
 * Encapsulates the business logic of the reference migration
 * 
 */
public class MigrationLogic {

    // AUXILIARY CLASSES
    //===================
    
    /**
     * A Node in the graph
     */
    public static class GraphNode {

        // MEMBER VARIABLES
        //==================
        
        /**
         * The Fida.Node instance to which this graph node corresponds to
         */
        public Fida.Node fidaNode;
        
        /**
         * Whether the logical node has been modified
         */
        public boolean isModified;
        
        /**
         * A clone of, or a reference to, the payload xid of the node
         */
        public Xid xid;
        
        /**
         * Manifestations of the xid present in the tree.
         */
        public Vector<Element> manifestations;
        
        /**
         * List of edges this node has. The list contains BOTH
         * the inclusions and the references.
         */
        public Vector<GraphEdge> children;
        
        /**
         * Edges having this node as the destination.
         * The list contains both kinds of edges, that is, 
         * inclusions and references pointing to this node.
         */
        public Vector<GraphEdge> parents;

        // CONSTRUCTORS
        //==============

        public GraphNode() {
            this(null);
        }
        
        public GraphNode(Fida.Node node) {
            fidaNode = node;
            if (node != null) {
                xid = node.payload_xid;
            }
            isModified = false;
            children = new Vector<GraphEdge>();
            parents = new Vector<GraphEdge>();
            manifestations = new Vector<Element>();
        }
        
    }; // class GraphNode
    
    /**
     * Invalid edge type
     */
    public static final int EDGE_INVALID = 0;
    
    /**
     * Referencing edge type
     */
    public static final int EDGE_REFERENCE = 1;
    
    /**
     * Inclusion edge type
     */
    public static final int EDGE_INCLUSION = 2;
    
    /**
     * An edge in the graph
     */
    public static class GraphEdge {
        
        /** 
         * Edge type; either reference or inclusion.<p>
         *
         * Reference is an element with attribute(s) named as ref*.
         * Inclusion is a parent-child relationship between 
         * two xidentified elements.
         */
        public int type;
        
        /**
         * Source node of the edge
         */
        public GraphNode source;
        
        /**
         * Destination node of the edge
         */
        public GraphNode dest;
        
        /**
         * Manifestations of the reference attribute
         */
        public Vector<Attribute> manifestations;
        
        
        public GraphEdge() {
            type = EDGE_INVALID;
            source = null;
            dest = null;
            manifestations = new Vector<Attribute>();
        }
        
        public GraphEdge(int type, GraphNode source, GraphNode dest) {
            this.type = type;
            this.source = source;
            this.dest = dest;
            manifestations = new Vector<Attribute>();
        }
    };

    // MEMBER VARIABLES
    //==================
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Constructor intentionally disabled.
     */
    private MigrationLogic() {
    } // ctor

    
    // METHODS FOR GRAPH BUILDING
    //============================
    
    /**
     * Build a graph from the specified files
     * 
     * @param db [in] The repository to use for resolving xids to elements.
     * @param commit [in] List of files to graph.
     * @param graph [out] Map of nodes
     * @param roots [out] Map fo root nodes corresponding to the files
     */
    public static void build_graph(
        FidaRepository db,
        Fida.Commit commit,
        Map<Xid, GraphNode> graph,
        Map<Fida.File, GraphNode> roots
    ) {
        // Build a graph of the current tree/layout. 
        // The graph is built by traversing through each file.
        for (Fida.File ff : commit.layout) {
            // Get the root element of the file
            Element root = ff.doc.getRootElement();
            
            // Traverse the root element and its children.
            try {
                GraphNode rootNode = null;
                rootNode = build_graph_node(db, graph, null, root);
                roots.put(ff, rootNode);
            } catch(Exception ex) {
                // In the case of an error, halt execution immediately
                // with stack trace.
                ex.printStackTrace();
                throw new RuntimeException(String.format(
                    "%s: %s", ff.path, ex.getMessage()), ex);
            } // try-catch
            
        } // for: each file in the commit layout
    } // build_graph()
    
    /**
     * Build a {@code GraphNode} from the specified {@code Element}.
     *
     * @param db [in] The repository used for resolving xids to elements
     * @param graph [in/out] The current graph which is modified
     * @param nearestParentNode [in] The nearest parent
     * @param element [in] The element used to build {@code GraphNode}
     */
    public static GraphNode build_graph_node(
        FidaRepository db,
        Map<Xid, GraphNode> graph,
        GraphNode nearestParentNode,
        Element element
    ) {
        // See if the element itself has a xid.
        Xid xid = XidIdentification.get_xid(element);
        
        if (xid != null) {
            // The node has a xid. Create a new or retrieve an existing
            // Node representation of the Element.
            GraphNode dest = get_or_create_node(db, graph, xid, null);
            
            // Add the input element to the GraphNode 
            // as a manifestation of it.
            dest.manifestations.add(element);

            // Only root nodes don't have nearest xidentified parent
            if (nearestParentNode != null)  {
                // Connect the parent to its child with an edge.
                GraphEdge graphEdge = get_or_create_edge(
                    nearestParentNode, dest, EDGE_INCLUSION);
            }
            
            // Update the nearestParentNode to the current node
            nearestParentNode = dest;
        } else {
            // no xid, do nothing
        } // if-else: has xid

        // Loop through all attributes to find out references.
        for (Object obj : element.getAttributes()) {
            Attribute a = (Attribute) obj;
            
            if (is_ref(a) == false) {
                continue; // ignore
            }
            
            // Get the reference, and de-serialize.
            // Rev is not allowed to be missing.
            String s = a.getValue();
            Xid ref = XidString.deserialize(s, true);
            
            if (ref == null) {
                // Reference has xid syntax error
                System.out.printf("%s: xid syntax error; ignored.\n",
                    XPathIdentification.get_xpath(a));
                continue;
            }
            
            if (ref.rev == Xid.REV_MISSING) {
                // TODO: Error: reference has an id but no revision.
                // Ignore silently?
                System.out.printf("%s: has an id but no revision; ignored.\n",
                    XPathIdentification.get_xpath(a));
                continue;
            }
            
            
            // Resolve the reference destionation graph node.
            GraphNode ref_dest = get_or_create_node(db, graph, ref, null);
            
            // TODO: nearestParentNode should never be null here.
            if (nearestParentNode == null)  {
                // Should never happen
                continue;
            }
            
            // Get or create a GraphEdge representing the referencing 
            // connection between the current element and 
            // the referenced element.
            GraphEdge graphEdge = get_or_create_edge(
                nearestParentNode, ref_dest, EDGE_REFERENCE);
            
            // Add the current attribute to the edge as a manifestation of it.
            graphEdge.manifestations.add(a);
            
        } // for each attr
        
        // Finally, recursion
        for (Object obj : element.getContent()) {
            if ((obj instanceof Element) == false) {
                continue;
            }
            // Recurse
            Element c = (Element) obj;
            build_graph_node(db, graph, nearestParentNode, c);
        } // for
        
        return nearestParentNode;
    } // build_graph()
    
    
    public static GraphEdge get_or_create_edge(
        GraphNode source,
        GraphNode dest,
        int type
    ) {
        GraphEdge rval = null;
        
        // See if the source node already has an edge to the destination node.
        boolean hasEdge = false;
        for (GraphEdge edge : source.children) {
            if (edge.type != type) {
                // Skip other types of edges
                continue;
            }
            if (edge.dest == dest) {
                hasEdge = true;
                rval = edge;
                break;
            }
        } // for: each edge
        
        if (hasEdge == false) {
            // No such edge. Create the edge and connect the nodes.
            GraphEdge edge = new GraphEdge(type, source, dest);
            source.children.add(edge);
            dest.parents.add(edge);
            
            rval = edge;
        } // if: no edge found
        
        return rval;
    }

    public static GraphNode get_or_create_node(
        FidaRepository db,
        Map<Xid, GraphNode> graph,
        Xid xid,
        Element element
    ) {
        // See if the graph already contains a GraphNode for the xid
        GraphNode graphNode = graph.get(xid);
        
        if (graphNode == null) {
            // No such node; create a GraphNode for the xid
            graphNode = new GraphNode();
            graphNode.xid = (Xid) xid.clone();
            graph.put(xid, graphNode);
            
            // See if the database contains 
            // Fida.Node object corresponding to the xid.
            Fida.Node node = db.get_node(xid);
            
            if (node == null) {
                // References can point to non-local items in which case
                // there is no such Fida.Node in the database.
            }
            
            // Record the Fida.Node to the GraphNode
            graphNode.fidaNode = node;
        }
        
        return graphNode;
    } // get_or_create_node()



    public static void print_graph_node(GraphNode n, Xid xid) {
        System.out.printf("Node for xid=\"%s\"\n",
            XidString.serialize(xid));
        if (n.fidaNode == null) {
            System.out.printf("   Payload: null\n");
        } else {
            System.out.printf("   Payload: present\n");
        }
        System.out.printf("   Manifestations: %d\n",
            n.manifestations.size());

        System.out.printf("   Parents (%d):\n",
            n.parents.size());
        
        for (GraphEdge edge : n.parents) {
            Fida.Node sourceNode = edge.source.fidaNode;
            if (sourceNode == null) {
                System.out.printf("       %d: ? unknown xid\n", edge.type);
            } else {
                System.out.printf("       %d: %s\n",
                    edge.type,
                    XidString.serialize(sourceNode.payload_xid));
            }
        } // for: each parent

        System.out.printf("   Children (%d):\n",
            n.children.size());
        
        for (GraphEdge edge : n.children) {
            Fida.Node destNode = edge.dest.fidaNode;
            if (destNode == null) {
                System.out.printf("       %d: ? unknown xid\n",
                    edge.type);
            } else {
                System.out.printf("       %d: %s\n",
                    edge.type,
                    XidString.serialize(destNode.payload_xid));
            }
        } // for: each parent
        
    }

    // METHODS FOR MIGRATING
    //=======================
    
    /*
     * Note: the algorithm doesn't recall this for any node
     */
    public static void migrate_node(
        Map<Xid, GraphNode> graph,
        FidaRepository db,
        Map<Attribute, Xid> map,
        GraphNode node
    ) {
        // Migrate the whole tree of a given node 
        // recursively and depth-first.
        
        for (GraphEdge edge : node.children) {
            if (edge.type == EDGE_INCLUSION) {
                // Recurse; depth-first
                migrate_node(graph, db, map, edge.dest);
            } else {
                // Otherwise see if the edge needs to be migrated.
                migrate_edge(graph, db, map, edge);
            }
        } // for
    } // migrate_node()
    
    /**
     * Migrate an edge; that is, see if there's a newer version of
     * the destination. If there's a newer version of the destination,
     * update the edge AND propagate the triggered updates in edges 
     * referencing the source node or any of its parents.
     * 
     * @param graph [in/out] The graph
     * @param db [in] The repository
     * @param edge [in/out] The edge to migrate
     */
    public static void migrate_edge(
        Map<Xid, GraphNode> graph,
        FidaRepository db,
        Map<Attribute, Xid> map,
        GraphEdge edge
    ) {
        
        // Asserted: edge.type == EDGE_REFERENCE
        
        //GraphNode sourceGraphNode = edge.source;
        //GraphNode destGraphNode = edge.dest;
        Fida.Node destNode = edge.dest.fidaNode;
        
        if (destNode == null) {
            // Either a non-local element is referenced, 
            // or the reference is in error.
            System.out.printf("Node %s references %s which does not exist; ref is UNMIGRABLE\n",
                XidString.serialize(edge.source.fidaNode.payload_xid),
                XidString.serialize(edge.dest.xid)
            );
            return;
        }

        // Find the graph node corresponding to newestNode
        Fida.Node newestNode = get_newest_revision(destNode);
        
        // See if the newest revision of the element has 
        // any instances in the tree.
        GraphNode newestGraphNode = graph.get(newestNode.payload_xid);
        if (newestGraphNode == null) {
            // This may occur, when the newest node is only referenced in 
            // the tree, but not present. The situation could be fixed
            // by rebuilding the node to memory at this point.
            
            // TODO: Create corresponding GraphNode,
            // and rebuild jdom Element corresponding to the xid.
            
            System.out.printf("Node %s references %s whose newest rev %s is not present in tree; ref is UNMIGRABLE\n",
                XidString.serialize(edge.source.fidaNode.payload_xid),
                XidString.serialize(edge.dest.xid),
                XidString.serialize(newestNode.payload_xid)
            );
            return;
        }
        
        // Migration will take place if the destination is different
        // than the newest revision OR if the destination node has been
        // modified by the migration algorithm. If the migration algorithm
        // has modified the destination node, it is possible that 
        // destNode==newestNode, but the edge contains, for instance,
        // a literal revision such as "node:123" instead of "node:#" 
        // what it should be. Therefore the reference has to be migrated.
        
        if ((destNode != newestNode) || (newestGraphNode.isModified)) {
            // Migrate the edge's destination
            /*
            if (newestGraphNode.isModified == false) {
                System.out.printf("Edge %s -> %s has a NEWER revision: %s\n",
                    XidString.serialize(edge.source.fidaNode.payload_xid),
                    XidString.serialize(destNode.payload_xid),
                    XidString.serialize(newestNode.payload_xid)
                ); // notify
            } else {
                System.out.printf("Edge %s -> %s has a NEWER revision: %s:#\n",
                    XidString.serialize(edge.source.fidaNode.payload_xid),
                    XidString.serialize(destNode.payload_xid),
                    newestNode.payload_xid.id
                ); // notify
            }
            */
        } else {
            /*
            System.out.printf("Edge %s -> %s is already newest. Not migrating.\n",
                XidString.serialize(edge.source.fidaNode.payload_xid),
                XidString.serialize(destNode.payload_xid)
            );
            */
            return;
        } // if-else

        // Serialize the new reference value
        Xid xid = (Xid) newestNode.payload_xid.clone();
        if (newestGraphNode.isModified == true) {
            // If modified, use hash mark
            xid.rev = Xid.REV_UNASSIGNED;
        }
        String newest_xid = XidString.serialize(xid);
        
        // Traverse all manifestations of the current edge,
        // and update the attribute values
        for (Attribute a : edge.manifestations) {
            a.setValue(newest_xid);
            map.put(a, xid);
        }
        
        // The next code segment is for maintaing the graph.
        // Maintaining the edges of the graph is crucial!
        
        // Remove the edge from the old dest's parents
        edge.dest.parents.remove(edge);
        // Assign a new dest to the edge, 
        // and add the edge to its parents list.
        edge.dest = newestGraphNode;
        edge.dest.parents.add(edge);
        
        
        // The next bit of code is to determine whether 
        // the modification/update of the source node causes further
        // modifications in other nodes which reference the source node
        // of this edge.
        // 
        // In other words, the purpose is to see whether the modifications
        // of the source node has to be PROPAGATED BACKWARDS. 
        // 
        // This is the most complicated piece of the algorithm, 
        // since it will create a sense of uncontrolled avalanche.
        //
        // If the source node has isModified flag set, then the backward
        // propagation for it has already been taken place or is currently
        // in process, and therefore it does not need any further actions.

        if (edge.source.isModified == false) {
            // Mark the source node as "modified" (equals being revisioned).
            // This flag also takes care that recursion doesn't redo 
            // backpropagation for this particular source node.
            
            // Propagate the updates caused by modifying "source".
            // That is, find all nodes which have this edge's source
            // as their dest, and migrate them. Repeat.
            edge.source.isModified = true;

            // Propagate updates to references 
            // triggered by revising edge.source
            backpropagate_node_modification(graph, db, map, edge.source);

            // Propagate modification to the parents, grand-parents,
            // and so on, of "edge.source".
            
            // Copy the parents to avoid comodification.
            // The parents make up the initial breadth.
            Vector<GraphEdge> breadth 
                = new Vector<GraphEdge>(edge.source.parents);
            
            while (breadth.size() > 0) {
                breadth = backpropagate_breadth(graph, db, map, breadth);
            }
        } // if: not yet modified
    } // migrate_edge()
    
    // @SuppressWarnings("unchecked")
    
    /**
     * 
     */
    public static void backpropagate_node_modification(
        Map<Xid, GraphNode> graph,
        FidaRepository db,
        Map<Attribute, Xid> map,
        GraphNode dest
    ) {
        // Copy the parents to avoid comodification.
        Vector<GraphEdge> parents = new Vector<GraphEdge>(dest.parents);
        
        for (GraphEdge edge : parents) {
            // Filter out other types except references
            if (edge.type != EDGE_REFERENCE) {
                continue;
            }

            // Asserted: edge.source references edge.dest
            
            // The following call causes the program to reinspect
            // the edge from edge.source to dest for further migration.
            migrate_edge(graph, db, map, edge);
        } // for: each parent edge
    }
    
    /**
     * Breadth-first backpropagation step.
     *
     * @return The breadth for the next step
     */
    public static Vector<GraphEdge> backpropagate_breadth(
        Map<Xid, GraphNode> graph,
        FidaRepository db,
        Map<Attribute, Xid> map,
        Vector<GraphEdge> breadth
    ) {
        // breadth for the next step
        Vector<GraphEdge> next = new Vector<GraphEdge>();
        for (GraphEdge edge : breadth) {
            // Follow only inclusions, because modifications 
            // propagate _upwards_ (to the parent) in the hierarchy.
            if (edge.type != EDGE_INCLUSION) {
                continue;
            }
            
            // Asserted: edge.source is a parent of edge.dest
            
            // For convenience
            GraphNode edge_source = edge.source;
            
            if (edge_source.isModified) {
                // Already handled or in-process.
                continue;
            } // if
            
            backpropagate_node_modification(graph, db, map, edge_source);
            
            // Add all parents to the next step
            next.addAll(edge_source.parents);
        } // for
        
        return next;
    }
    
    /**
     * Returns the newest revision of the specified node by traversing
     * the "next" objects as far as possible until no more next.
     */
    public static Fida.Node get_newest_revision(
        Fida.Node node
    ) {
        while (node.next.size() > 0) {
            node = node.next.get(0);
        }
        return node;
    }

    public static boolean is_ref(Attribute a) {
        return is_ref(a.getName());
    }
    
    /*
     * TBC: endsWith() better? eg. xref, aref, bref, myref, refData?
     * What about camelCase? myRef? dataRef=? 
     */
    public static boolean is_ref(String attrName) {
        return attrName.startsWith("ref");
    }

} // class MigrationLogic
