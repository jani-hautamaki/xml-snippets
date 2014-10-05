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

package xmlsnippets.fida;

// jdom imports
import org.jdom.Element;

// xmlsnippets imports
import xmlsnippets.core.Xid;
import xmlsnippets.fida.Fida;

/**
 * An abstraction of the query and update methods that a repository must have.
 * TODO: Replace {@code Fida.Node} return values with an abstraction,
 * eg. {@code AbstractNode}.
 */
public interface AbstractRepository {
    
    /**
     * Retrieves the repository entry for a specified user namespace
     * (payload content's) xid.
     *
     * @param xid the user namespace (payload content's) xid.
     * @return The administrative entry in the repository
     * for the specified xid. If no such xid is found, {@code null}
     * is returned.
     */
    public Fida.Node get_node(Xid xid);
    
    /**
     * Assign new/latest version-revision information to the xid.
     * @param xid the Xid whose version-revision information is revised.
     *
     */
    public void set_new_revision(Xid xid);
    
    /**
     * Generates an unused xid for an internal use.
     *
     * @param type A name describing the type of the object for which
     * the xid is generated
     * @return The generated, unused xid.
     */
    public Xid generate_xid(String typename);
    
    /**
     * Creates and adds/ingests a new administrative node for a new payload 
     * content to the repository. The payload content must have xid 
     * identification data. The xid of the payload element must be unused. 
     * The new node can be associated to the administrative node of data 
     * object's previous instance/revision in the lifeline. The created 
     * administrative node is returned.<p>
     *
     * TODO: Should the method simply create an administrative entry and
     * leave the assocation of the payload ({@code Element}) to the callee?
     * Probably not...<p>
     * 
     * @param payload the content payload XML element with an unused xid.
     * @param prev the administrative node of the predecessor instance,
     * or {@code null} if the data object does not have a previous revision.
     *
     * @return The administrative node created for the ingested payload.
     */
    public Fida.Node add_node(Element payload, Fida.Node prev);
    
    /**
     * Retrieves the latest or the current leaser of the specified
     * lifeline designator present in the current tree.<p>
     *
     * The concepts lifeline, lifeline designator, leaser
     * of the lifeline designator, and tree are central here. They are
     * explained in detail below.<p>
     *
     * <b>Lifeline</b> means a series of data objects which is considered 
     * to be instances of the same data object, but at different points
     * in time.<p>
     *
     * <b>Lifeline designator</b> is an identifier which at each particular
     * point in time identifies a single lifeline. In other words, at any
     * given instant of time, each leased lifeline designator must 
     * unambiguously correspond to a single lifeline. <p>
     *
     * <b>Leaser of the lifeline designator</b> is the lifeline which
     * is at the specified instant of time corresponding to the lifeline
     * designator. As the choice of the word "lease" suggests, a lifeline
     * designator is leased to a lifeline, which means that <b><i>at different
     * times the same lifeline designator may correspond to different
     * lifelines.</i></b> The lifeline does not own its designator, but instead
     * it just leases it.<p>
     *
     * <b>tree</b> is the tree graph whose nodes are the identified 
     * XML elements that are reachable from the root nodes of the tracked
     * files' current revisions.
     *
     * @param id the lifeline designator
     *
     * @return The administrative entry for the data object who is the latest 
     * leaser of the specified lifeline designator present the current tree. 
     * If the lifeline designator has not yet been leased to anyone,
     * or if there is no previous leaser present in the current tree,
     * {@code null} is returned.
     *
     */
    public Fida.Node get_latest_leaser(String id);
    
} // interface AbstractRepository
