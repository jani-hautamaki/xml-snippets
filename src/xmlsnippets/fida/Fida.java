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

// java core imports
import java.util.List;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
// jdom imports
import org.jdom.Element;
import org.jdom.Document;
// xmlsnippets imports
import xmlsnippets.core.Xid;
import xmlsnippets.util.Digest;

public class Fida {

    // SOME CONSTANTS
    //================

    /**
     * These constants are used with Fida.File.action
     */
    public static final int ACTION_FILE_NOP                = 0;
    public static final int ACTION_FILE_REMOVED            = 1;
    public static final int ACTION_FILE_UPDATED            = 2;
    public static final int ACTION_FILE_ADDED              = 3;

    // CONSTRUCTORS
    //==============

    /**
     * Construction is intentionally disabled
     */
    private Fida() {
    } // ctor

    // NESTED STATIC CLASSES
    //=======================

    public static class Item {
        /**
         * Each Fida.Item has an internal xid
         */
        public Xid item_xid;
    } // class Item

    /**
     * Data structure representing an instance a tracked XML document,
     * and also representing a manifestation of an XML element (the root).
     *
     */
    public static class File
        extends Item
    {

        // MEMBER VARIABLES
        //==================

        /**
         * Links to the previous instance of the file, if any.
         */
        public Fida.File prev;

        /**
         *
         */
        public int action;

        /**
         * The xid of the previous file prior to resolution.
         * Once resolved, this is set to {@code null}.
         */
        public Xid prev_xid;

        /**
         * Links to the succeeding revisions; branching is not allowed,
         * so it isn' neccessary to use a list
         */
        public List<Fida.File> next;

        /**
         * Path to an XML document, relative to directory where the xid
         * repository database is located at.
         */
        public String path;

        /**
         * If the file is read, this member variable can be used to store
         * the XML document corresponding to the file. Otherwise, this is
         * kept {@code null} and is not stored to repository db nor read
         * from it.
         */
         public Document doc;

        /**
         * The Digest for the XML document; used to track whether the document
         * is unmodified.
         */
        public Digest digest;

        /**
         * The root node's xid. The xid has to be in the user namespace.
         */
        public Xid root_xid;

        /**
         * How the root node is manifestated in this particular file
         */
        public List<Stack<Xid>> manifestation;

        /**
         * The commit which introduced this particular File.
         */
        public Commit parent_commit;

        // CONSTRUCTORS
        //==============

        public File() {
            action = ACTION_FILE_NOP;
            prev = null;
            prev_xid = null;
            next = new LinkedList<Fida.File>();
            path = null;
            doc = null;
            digest = null;
            root_xid = null;
            manifestation = null;
            parent_commit = null;
        } // ctor

    } // class File

    /**
     * Data structure representing a stored, normalized payload element in
     * the repository.
     *
     */
    public static class Node
        extends Item
    {

        // MEMBER VARIABLES
        //==================

        /**
         * Link to the Node corresponding to the predecessor revision of
         * the payload element, or {@code null} if the payload element
         * did not have a preceeding revisions.<p>
         *
         * TODO: This should be a list to facilitate merging of branches.
         * However, the system is restricetd not to allow branching, so
         * merging won't happen.
         */
        public List<Fida.Node> prev;

        /**
         * The xid of the previous node prior to resolution.
         * Once resolved, this is set to {@code null}.
         */
        public List<Xid> prev_xid;

        /**
         * Link(s) to Nodes corresponding to the successor revision(s)
         * of the payload element, or an empty list if the payload element
         * does not have succeeding revisions.<p>
         *
         * <b>Note:</b> the system is restrictied not to allow branching.
         * Therefore, each node can have at most only one successor node.<p>
         *
         * <b>Note:</b> these links are not recorded into the revision
         * database. Instead, they are discovered during parsing.
         */
        public List<Fida.Node> next;

        /**
         * The revisioned payload XML element
         */
        public Element payload_element;

        /**
         * The xid of the payload XML element
         */
        public Xid payload_xid;

        // TODO: A digest value could be used for testing quickly
        // the contentual eqivalence.
        // public Digest payload_digest;

        /**
         * The commit which introduced this particular File.
         */
        public Commit parent_commit;


        // CONSTRUCTORS
        //==============

        public Node() {
            prev = new LinkedList<Fida.Node>();
            prev_xid = new LinkedList<Xid>();
            next = new LinkedList<Fida.Node>();
            payload_element = null;
            payload_xid = null;
            parent_commit = null;
        } // ctor

        // OTHER METHODS
        //===============

        public boolean containsNext(Fida.Node item) {
            Xid xid = item.payload_xid;
            for (Fida.Node cur : next) {
                if (cur.payload_xid.equals(xid)) {
                    return true;
                }
            }
            return false;
        }

        public boolean containsPrev(Fida.Node item) {
            Xid xid = item.payload_xid;
            for (Fida.Node cur : prev) {
                if (cur.payload_xid.equals(xid)) {
                    return true;
                }
            }
            return false;
        }

    } // class Node

    /**
     * Data structure representing a single commit set,
     */
    public static class Commit
        extends Item
    {
        // MEMBER VARIABLES
        //==================

        // TODO:
        // Add a link to the previosu commit

        /**
         * Commit date
         */
        public Date date;

        /**
         * Commit author
         */
        public String author;

        /**
         * Snapshot of the repository directory tree/layout.
         * Includes the removed files also! TODO: The Fida.File struct has
         * more fields than is neccessary for deleted files.
         */
        public List<Fida.File> layout;

        /**
         * List of all new nodes introduced in this commit
         */
        public List<Fida.Node> nodes;

        // CONSTRUCTORS
        //==============

        public Commit() {
            // Use the time of the creation of the commit object
            date = new Date();
            author = null;
            layout = new LinkedList<Fida.File>();
            nodes = new LinkedList<Fida.Node>();
        } // ctor
    } // class Commit

    /**
     * TODO:
     * Move externals and internals from State class to Repository class?
     */
    public static class State {

        // MEMBER VARIABLES
        //==================

        /**
         * Serialization request flag. This indicates the repository
         * has been modified and should be serialized to disk again.
         */
        public boolean modified;

        /**
         * The random number generator which is used to generate
         * "fida uid" numbers; the seed value is stored and retrieved
         * from the repository db.
         */
        public Random rng;

        /**
         * The latest commit, or {@code null} if none.
         */
        public Commit head;

        /**
         * The internal xid of the latest commit; used for resolving.
         */
        public Xid head_xid;

        /**
         * Map of all used fida_uid's. TODO: a sorted array should be used
         * instead, because bisectioning is would be most desirable
         * search method, and directly accessing the primitive data type
         * instead of an Object.
         */
        public Map<Integer, Fida.Item> internals;

        /**
         * Mapping from all stored XML elements with a user namespace xid
         * to their corresponding internal Fida.Node objects.
         */
        public Map<Xid, Fida.Node> externals;

        /**
         * Total layout at the latest commit
         */
        public List<Fida.File> tree;

        /**
         * Mapping from all user namespace xids in the current commit
         * to their corresponding Fida.Node objects. This is a mirroring
         * copy of {@code Commit} objects {@link Fida.Commit#nodes} list,
         * but the purpose is different; this one is solely for resolution
         * purposes.
         */
        public Map<Xid, Fida.Node> commit_externals;

        /**
         * Flag signaling that elements with revision numbers specified,
         * but which are unknown to the repository, can be added.
         * This may cause the repository's state revision number to
         * jump arbitrarily upwards.
         */
        public boolean allow_unknowns;

        /**
         * Flag signaling that when an unknown element is encountered,
         * it should be unrevisioned and retry ingesting.
         *
         */
        public boolean unrev_unknowns;


        /**
         * Flag signaling that referencing attributes without
         * revision numbers are automatically assigned the latest
         * revision of the specified lifeline designator or
         * if no such lifeline designator is found, the newest
         * revision of the repository.
         */
        public boolean autoref;

        // CONSTRUCTORS
        //==============

        public State() {
            modified = false;
            rng = new Random(); // with a random seed
            head = null;
            head_xid = null;

            internals = new HashMap<Integer, Fida.Item>();
            externals = new HashMap<Xid, Fida.Node>();
            commit_externals = new HashMap<Xid, Fida.Node>();
            tree = null;
            allow_unknowns = false;
            unrev_unknowns = false;
            autoref = false;
        } // ctor

        public int new_uid() {
            int uid;
            boolean match;

            // WARNING. The duration of this loop is unpredictable!
            do {
                uid = rng.nextInt();
                match = internals.containsKey(uid);
            } while (match == true);

            // Record the uid with null value. This simply reserves
            // the created uid.
            internals.put(uid, null);

            return uid;
        } // new_uid()

    } // class State

    public static class Repository
        extends Item
    {

        // MEMBER VARIABLES
        //==================

        /**
         * File corresponding to the repository
         */
        public java.io.File file;

        /**
         * The current state of the repository
         */
        public State state;

        /**
         * List of all commits
         */
        public List<Fida.Commit> commits;

        /**
         * The next commit object which is being built, if any.
         */
        public Commit next_commit;

        // CONSTRUCTORS
        //==============

        public Repository() {
            state = new State();
            commits = new LinkedList<Fida.Commit>();
            next_commit = null;
        } // ctor

        // OTHER METHODS
        ///==============

    } // class repository

} // class
