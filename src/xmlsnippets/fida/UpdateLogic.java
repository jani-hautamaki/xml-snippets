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

// java core imports
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Stack;
// jdom imports
import org.jdom.Element;
// xmlsnippets imports
import xmlsnippets.core.Xid;
import xmlsnippets.core.XidIdentification;
import xmlsnippets.core.XidString;
import xmlsnippets.core.ContentualEq;
import xmlsnippets.core.Normalization;
import xmlsnippets.core.PidIdentification;
import xmlsnippets.fida.AbstractRepository;
import xmlsnippets.util.XPathIdentification;

/**
 * Encapsulates the business logic of the update commit creation
 *
 * 
 */
public class UpdateLogic {

    // CONFIGURATION VARIABLES
    //=========================
    
    /**
     * Flag indicating that when an element with an unknown xid is encountered,
     * the element should be unrevisioned and then attempt ingesting again.
     */
    public static boolean g_opt_unrev_unknowns = false;
    
    /**
     * If the flag is set, then the elements with unknown xids are ingested.
     * Otherwise, when an element with an unknown xid is encountered, and
     * {@link #g_opt_unrev_unknowns} is not set, the element is rejected.
     */
    public static boolean g_opt_ingest_unknowns = false;
    
    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally disabled
     */
    
    private UpdateLogic() {
    } // ctor
    
    
    /**
     * Transform an XML element into a series of additions of normalized
     * XML elements into a repository. The specified XML element is
     * either fully ingested or rejected at some point. The function is
     * applied recursively to the children of the specified XML element.
     *
     */
    public static void ingest(
        AbstractRepository db,
        Element elem,
        Map<Element, List<Stack<Xid>>> manifestations_map,
        Map<String, Element> properties
    ) {
        
        // If there's "p" attribute, the property should go
        // to the parent's scope, if any
        String pid = PidIdentification.get_pid(elem);
        
        if (pid != null) {
            // Add to parent's scope, if any
            if (properties == null) {
                throw new RuntimeException(String.format(
                    "No xidentified ancestor found, cannot assing property id \'%s\' to element: %s",
                    pid,
                    XPathIdentification.get_xpath(elem)));
            }
            // Make sure that the property id is unique
            Element earlier = properties.get(pid);
            if (earlier != null) {
                throw new RuntimeException(String.format(
                    "Property id already exists in the local scope, cannot assing property id \'%s\' to element: %s\nThe earlier property id was introduced here: %s",
                    pid,
                    XPathIdentification.get_xpath(elem),
                    XPathIdentification.get_xpath(earlier)));
            }
            
            // For debugging:
            System.out.printf("Assigning pid=\'%s\' to element: %s\n",
                pid,
                XPathIdentification.get_xpath(elem));
            
            // Okay, pid can be assigned
            properties.put(pid, elem);
            
        } // if: has pid
        
        
        // If the current XML Element is a xidentified element,
        // create a new scope for properties
        Map<String, Element> local_properties = null;
        
        // TODO: If the XML element has a property id set,
        // then it should probably create a new local scope too?
        
        Xid xid = XidIdentification.get_xid(elem);
        if ((xid != null) || (properties == null)) {
            local_properties = new HashMap<String, Element>();
        } else {
            // xid == null AND properties != null.
            // Use the inherited scope.
            local_properties = properties;
        } // if-else
        
        
        // Depth-first recursion to the child elements
        for (Object obj : elem.getContent()) {
            if ((obj instanceof Element) == false) {
                // Not an Element object. Skip
                continue;
            } // if: not an element
            
            // Depth-first recursion
            Element child = (Element) obj;
            
            ingest(db, child, manifestations_map, local_properties);
            // Debug the manifestations_map

            // Reassign the key of the manifestations mapped to the null key
            List<Stack<Xid>> nulstack = manifestations_map.remove(null);
            if (nulstack != null) {
                manifestations_map.put(child, nulstack);
            } // if
        } // for: each child object
        
        // After all children are processed, process the node itself.
        ingest_element(db, elem, manifestations_map);
    }
    
    private static void ingest_element(
        AbstractRepository db,
        Element elem,
        Map<Element, List<Stack<Xid>>> manifestations_map
    ) {
        // The element should now have a valid xid, either as (@id, @rev) pair
        // or as @xid. If this throws an exception, it is an indication of
        // a programming error in preprocess().
        Xid xid = XidIdentification.get_xid(elem);

        // If the element does not have any identification at all,
        // it can be skipped.
        if (xid == null) {
            return;
        }
        
        // This flag is used to determine later whether the element
        // is allowed to have a xid which is not known to the system earlier.
        boolean allow_new = false;
        
        //System.out.printf("Process node: %s\n", XidString.serialize(xid));
        
        // Detect if the xid is an unrevisioned pre-xid. If that's the case,
        // then assign the current revision number to both the xid
        // and the XML element (which will be written back to disk later).
        if (xid.rev == Xid.REV_UNASSIGNED) {
            
            // Update the rev to the current revision
            db.set_new_revision(xid);
            
            // Propagate the update to the element itself too.
            XidIdentification.set_xid(elem, xid);
            
            // Because this is meant to be a new element, set the flag
            // allowing the element to unexist in the current repository
            allow_new = true;
        } else if (xid.rev == Xid.REV_MISSING) {
            // This shouldn't really happen. If this line is reached,
            // then there's a programming error with preprocess().
            throw new RuntimeException(String.format(
                "The element %s does not have rev even though it has an id. It really should have a valid rev by now. This is a programming error.",
                XPathIdentification.get_xpath(elem)));
        } // if-else

        // Normalize the content element. Regardless whether the element
        // is going to be added or not to the repository, it needs to be
        // in the normalized form in any case.
        Element normal = null;
        
        // Normalization table for the inclusion-by-xid elements.
        List<Normalization.RefXidRecord> table = null;
        
        // This is required for translating cloned copy references
        // to original object references.
        Map<Element, Element> map = new HashMap<Element, Element>();
        
        // Normalize the current XML element. This creates an unparented
        // copy of the XML element. Without the map it would be impossible
        // to make out the connections between original XML elements
        // and their shortened ref_xid replacements.
        normal = Normalization.normalize(elem, map);
        
        // Remove local information from the normalized copy,
        // so that it won't be embedded in the globally used instance.
        PidIdentification.unset_pid(normal);
        
        // Normalize the inclusion-by-xid elements too. During the operation
        // a so called normalization table is created. It can be used
        // to normalize and, more importantly, to denormalize 
        // the inclusion-by-xid elements back to their original form. 
        // Also, the normalization table will contain information about
        // whether the file's manifestation contained some unexpanded
        // inclusion-by-xid elements.
        table = Normalization.normalize_refs(normal);
        
        // Assign unique identities for each inclusion-by-xid element.
        // They will be identified with link_xid attribute values. 
        // The actual elements wont receive the link_xid attribute just yet.
        // At the moment their unique identities are stored only into
        // the normalization table.
        assign_link_xids(db, table);
        
        // The processing is now quite done.
        
        // Next, it is studied whether the xid this element has is already
        // known to the system or not. The previous instance may either be
        // in the repository or in the current commit set.
        Fida.Node item = db.get_node(xid);
        
        // If the xid exists, then the element should be contentually 
        // equivalent to it. If the element is not the same, then it must be 
        // considered as a new revision of it.

        //------- below: unrev_unknowns option (optional) ------- //

        // However, before that the program needs to check if the xid
        // is unknown (= not new and not old) and they are to be re-revisioned.
        if ((item == null) 
            && (allow_new == false) 
            && (g_opt_unrev_unknowns))
        {
            // Get a new revision, since unknowns are unrev'd
            db.set_new_revision(xid);
            
            // Propagate the new xid values back to both the original doc
            // and teh unparented normalized copy.
            XidIdentification.set_xid(elem, xid);
            XidIdentification.set_xid(normal, xid);

            // Allow this to be a new item
            allow_new = true;
        } // if: unrev_unknowns
        
        //------- above: unrev_unknowns option (optional) ------- //
        
        
        if (item != null) {
            // Yes, there is an XML element already in the system with 
            // the same xid. Contentual equivalence of the current instance
            // and the known instance must be calculated. To do that,
            // the inclusion-by-xid elements in both instances must be
            // normalized ("anonymized") by removing their unique link_xids.
            
            // Calcualte the normalization table for the older instance
            List<Normalization.RefXidRecord> oldtable = null;
            oldtable = Normalization.build_normalization_table(item.payload_element);
            
            // See if the current and the olde instance are contentually
            // equivalent.
            
            if (nodes_equal(normal, item.payload_element, oldtable)) {
                // The nodes ARE contentually equal. The current instance
                // has been already stored either into the repository
                // or into the current commit set. Because it is already
                // stored, we won't store it again.
                
                // Because it won't be stored, the manifestation information
                // calculated from the current instance must be transformed
                // into terms of the already recorded instance. Specifically,
                // it means translating the unique link_xid values just
                // assigned to the ones in the already recorded instance.
                
                // Since the instances are contentually equivalent,
                // their normalization tables SHOULD be 1) in identical
                // order and 2) the ref_xid values should be identical
                // in the identical order.
                
                calculate_manifestation(
                    table,
                    oldtable,
                    map,
                    manifestations_map
                );
                
                // End processing here
                return;
            } // if: contentually equal
            
            // If this line was reached, the instances were not contentually
            // equal. The newer instance must be considered as a new revision
            // of the older instance. 
            
            // The instance is given a new revision number.
            //=========================================================
            db.set_new_revision(xid);
            
            // It may be, that the element already had the revision value
            // which the new_xid_revision() function assigned. In that case,
            // the xid was unchanged.
            
            // In any case, the possibly new xid values must be propagated
            // also to the original XML element and to the normalized copy,
            // becuase those XML elements are written back to the disk later.
            XidIdentification.set_xid(elem, xid);
            XidIdentification.set_xid(normal, xid);
            
            // Because the revision changed and the XML element possibly
            // got a new identity the check for already known instance
            // must be repeated.
            
            //=========================================================
            // The revision was changed -> checking has to be repeated.
            //=========================================================
            
            Fida.Node newitem = null;
            newitem = db.get_node(xid);
            
            if (newitem != null) {
                // The xid is known to the system: either in the repository
                // records or in the current commit set.
                
                // Build normalization table for the inclusion-by-xid elements.
                oldtable = Normalization.build_normalization_table(newitem.payload_element);
                
                // Determine the contentual equivalence of the current
                // and oler instance of this xid.
                if (nodes_equal(normal, newitem.payload_element, oldtable)) {
                    // The current and the older instance of this xid
                    // are contentually equal. The current instance is already
                    // then recorded, and does not need to be recorded twice.
                    // Just like earlier, the manifestation information
                    // created for the normalized copy of the current instance
                    // must be translated into terms of the older instance.
                    calculate_manifestation(
                        table,
                        oldtable,
                        map,
                        manifestations_map
                    );
                    
                    // End processing here
                    return;
                } // if: contentually equal
                
                // The instances were not contentually equal.
                // They have the same id and the same new revision,
                // but their contents differ. Their modifications have
                // been inconsistent, and the user must resolve the issue.
                if (allow_new == true) {
                    throw new RuntimeException(String.format(
                        "The new element %s is inconsistent with another new element having the same xid=%s",
                        XPathIdentification.get_xpath(elem),
                        XidString.serialize(xid)
                    ));
                } else {
                    throw new RuntimeException(String.format(
                        "The updated element %s is inconsistent with an already recorded element having the same xid=%s",
                        XPathIdentification.get_xpath(elem),
                        XidString.serialize(xid)
                    ));
                }
            } else {
                // There is no earlier record in the repository or in
                // the current commit set of the updated xid. This suggests 
                // the modification created a valid new revision.
            } // if-else
            
            // Next it is determined whether the older instance of
            // the XML element was the latest revision up-to-date.
            // If the modified XML element was an instance of some
            // "already archived" XML element which already had a newer
            // revision, the modification is rejected. The new revision
            // must be a successor of the currently latest revision.
            if (item.next.size() != 0) {
                // TODO:
                // If branching is allowed, then this code needs to be
                // modified; probably the rev shuold be renamed into some
                // branch number which don't reveal any ordering.
                throw new RuntimeException(String.format(
                    "The modified element %s constitutes a branch of xid=%s",
                    XPathIdentification.get_xpath(elem),
                    XidString.serialize(item.payload_xid)
                )); // throw new ..
            } // if
            
            // Make sure that that there isn't an XML element in
            // the current tree such that it has the same id, but 
            // does not belong to the same lifeline as the latest instance
            // of the current XML element.
            
            Fida.Node active_node = db.get_latest_leaser(xid.id);
            if ((active_node != null) && (active_node != item)) {
                // There is an active lifeline in the tree, and
                // the lifeline is different from this elements lifeline.
                // It means that @id taken as a lifeline designator,
                // is in use currently, and cannot be taken for a different
                // lifeline now, but maybe somewhere in the future.
                
                // The situation from lifelines point of view (both have
                // used the same xid.id):
                //
                //                                             now
                //  revision            1   2   3   4   5   6   7   8
                //  another_lifeline                    x---x---x     
                //  this_lifeline       x---x---x                   X?
                //
                // The instances present in the tree at each revision:
                //
                //                                 tree rev
                //  data object         1   2   3   4   5   6   7   8
                //     id:r1            x                            
                //     id:r2                x               x   x    
                //     id:r3                    x               x    
                //     id:r4                                         
                //     id:r5                            x            
                //     id:r6                                x        
                //     id:r7                                    x    
                //     id:r8
                //
                // The above diagram is possible, because adding old (ingested)
                // instances of the XML elements into a tracked file does not
                // create a new revision of the inserted XML element, but only
                // a new revision of the parent element. So that is the method
                // to introduce the latest instances of two different lifelines
                // into a same tree.
                //
                // So at the r6 the tree contained an identical copy of data
                // object in this_lifeline:r2 (which is equal to id:r2). This
                // data object would not have been the latest instance in that
                // lifeline. At that point r6 it would have been unproblematic 
                // to create a new revision r7 of the data object 
                // another_lifeine:r6. However, at the time instant r7 the tree
                // contained an identical copies of this_lifeline:r2 and
                // this_lifeline:r3 which correspond to id:r2 and id:r3
                // respectively. The id:r3 was the latest leaser in that 
                // lifeline. But it cannot be revisioned to create a next entry
                // to its lifeline, because it would need to end the lifeline 
                // of the current leaser of this lifeline designator.
                
                throw new RuntimeException(String.format(
                    "The element %s with xid=%s cannot be revisioned, because it would hide a newer and active xid=%s (%s)",
                    XPathIdentification.get_xpath(elem), 
                    XidString.serialize(item.payload_xid),
                    XidString.serialize(active_node.payload_xid),
                    XidString.serialize(active_node.item_xid)
                )); // throw
            }  // if: there is an active lifeline in the tree different from this
            
        } else {
            // There was no previous XML element with the same xid.
            // Either the XML element contained an unknown xid or it was
            // intentionally assigned an unexisting xid. See which one is it.
            if (allow_new == false) {
                // Unexisting xid wasn't allowed. See if there is
                // an overriding policy in effect..
                if (g_opt_ingest_unknowns == true) {
                    // It can be added. 
                    // This may abruptly jump the repository's revision 
                    // number.
                } else {
                    // No, the overriding policy is not in effect.
                    // This is an error then.
                    throw new RuntimeException(String.format(
                        "The element %s has an unknown xid=%s",
                        XPathIdentification.get_xpath(elem), 
                        XidString.serialize(xid)));
                } // if-else: unknowns allowed?
            } // if: new allowed?
            
            // If the unexisting xid can be added. It still needs to
            // be check that the lifeline designator @id is not currently
            // in active use for someone elses lifeline.
            Fida.Node active_node = db.get_latest_leaser(xid.id);
            
            if (active_node != null) {
                
                if (active_node.payload_xid.rev > xid.rev) {
                    // Newer ones are not hidden. This revision will
                    // immediately constitute "an old" and "sealed" revision.
                    // TODO:
                    // It could be added. Make it into an option
                    // whether to allow it.
                    throw new RuntimeException(String.format(
                        "The element %s with xid=%s cannot be added, because it would be misunderstood as an older revision of the newer, and still active xid=%s (%s)",
                        XPathIdentification.get_xpath(elem), 
                        XidString.serialize(xid), 
                        XidString.serialize(active_node.payload_xid),
                        XidString.serialize(active_node.item_xid)
                    )); // throw
                } else {
                    throw new RuntimeException(String.format(
                        "The element %s with xid=%s cannot be added, because it would hide an older, but still active xid=%s (%s)",
                        XPathIdentification.get_xpath(elem), 
                        XidString.serialize(xid), 
                        XidString.serialize(active_node.payload_xid),
                        XidString.serialize(active_node.item_xid)
                    )); // throw
                } // if-else
                
            } else {
                // The lifeline designator @id is currently not reserved
                // for any lifeline.
            } // if-else: there is an active node, which could be hidden
        } // if-else: the xid was already known to the system?
        
        // If this line is reached, then the current XML element is going
        // to be added to the current commit set as a new XML element.
        
        // Update the manifestation for this
        calculate_manifestation(
            table,
            null, // no older element in the repo
            map,
            manifestations_map
        );
        
        // Apply the denormalization table to the references of 
        // the normalized element.
        Normalization.denormalize_refs(table);
        
        // This should go to the current commit set!
        db.add_node(normal, item);
    } // ingest()
    
    // HELPER METHODS
    //================
    
    protected static void assign_link_xids(
        AbstractRepository db,
        List<Normalization.RefXidRecord> table
    ) {
        // Assign unique link_xid to each inclusion-by-xid element.
        for (Normalization.RefXidRecord record : table) {
            Xid link_xid = null;
            
            link_xid = db.generate_xid("link");
            
            record.xid = link_xid;
            // TODO: This operation should be in RefXidRecord class.
            //record.element.setAttribute("link_xid", XidString.serialize(linkxid));
            
        } // for
    } // assign_link_xids()
    

    protected static boolean nodes_equal(
        Element newelem,
        Element oldelem,
        List<Normalization.RefXidRecord> oldtable
    ) {
        // Return value
        boolean rval;
        
        // Normalize inclusion-by-xid elements
        Normalization.normalize_refs(oldtable);
        
        // Test contentual equivalence
        try {
            // May throw an IOException
            rval = ContentualEq.equal(newelem, oldelem);
        } catch(Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        
        // Denormalize inclusion-by-xid elements
        Normalization.denormalize_refs(oldtable);
        
        return rval;
    } // nodes_equal()

    protected static void calculate_manifestation(
        List<Normalization.RefXidRecord> newtable,
        List<Normalization.RefXidRecord> oldtable,
        Map<Element, Element> map,
        Map<Element, List<Stack<Xid>>> manifestations_map
    ) {
        
        // Calculate the xid mapping
        Map<Xid, Xid> xidmap = calculate_xid_map(newtable, oldtable);
        
        // Append noexpands from the current denormalization table to it also
        
        List<Stack<Xid>> nullist = manifestations_map.get(null);
        if (nullist == null) {
            nullist = new LinkedList<Stack<Xid>>();
        } else {
            System.out.printf("Null stack... Don\'t know why??\n");
        }
        
        // Populate the nullist with noexpand entried from the current
        // denormalization table
        for (Normalization.RefXidRecord record : newtable) {
            
            // This is the link_xid of the inclusion-by-xid element itself,
            // not the target.
            Xid targetxid = record.xid;
            
            // If the cloned copy is not going to be added to the repository,
            // then the link_xid of the cloned copy must be translated
            // into terms of the stored item.
            if (xidmap != null) {
                // Translate newtable's xid to oldtable's xid
                targetxid = xidmap.get(targetxid);
            }
            
            if (record.expand == true) {
                // Propagate all manifestations_map entries with
                // key matching to the original child of this link
                // to correspond null
                
                // Pick the inclusion-by-xid Element object 
                // in the normalized copy
                Element replacement_elem = record.element;
            
                // Translate the inclusion-by-xid Element reference
                // to Element in the original content element.
                Element orig_elem = map.get(replacement_elem);
                
                List<Stack<Xid>> stacklist = manifestations_map.remove(orig_elem);
                if (stacklist != null) {
                    for (Stack<Xid> stack : stacklist) {
                        stack.push(targetxid);
                        nullist.add(stack);
                    } // for
                }
                
            } else {
                Stack<Xid> newstack = new Stack<Xid>();
                newstack.push(targetxid);
                nullist.add(newstack);
            } // if-else: expand?
        } // for
        
        if (nullist.size() > 0) {
            manifestations_map.put(null, nullist);
        }
        
    } // calculate_manifestation()
    
    protected static Map<Xid, Xid> calculate_xid_map(
        List<Normalization.RefXidRecord> newtable,
        List<Normalization.RefXidRecord> oldtable
    ) {
        
        if (oldtable == null) {
            return null;
        }
        
        // Create translation map from new table to old table.
        
        // Return variable
        Map<Xid, Xid> xidmap = new HashMap<Xid, Xid>();
        
        // Iterators over both lists
        ListIterator<Normalization.RefXidRecord> iter_new;
        ListIterator<Normalization.RefXidRecord> iter_old;
        
        // Initialize the iterators
        iter_new = newtable.listIterator();
        iter_old = oldtable.listIterator();
        
        // Repeat while both have next
        while (iter_new.hasNext() && iter_old.hasNext()) {
            // Pick the next item from both lists
            Normalization.RefXidRecord new_rec = iter_new.next();
            Normalization.RefXidRecord old_rec = iter_old.next();
            
            // Verify that both ref_xids reference to the same target
            String new_refxid = new_rec.element.getAttributeValue("ref_xid");
            String old_refxid = old_rec.element.getAttributeValue("ref_xid");
            
            if (old_refxid.equals(new_refxid) == false) {
                // The ref_xid targets differ. This is an error
                throw new RuntimeException(String.format(
                    "ref_xid values differ: \"%s\" vs \"%s\"",
                    new_refxid, old_refxid));
            } // if
            
            // Insert the assocation to the map
            xidmap.put(new_rec.xid, old_rec.xid);
        } // while
        
        // Verify that both lists were completetly consumed
        if (iter_new.hasNext() || iter_old.hasNext()) {
            throw new RuntimeException(String.format(
                "RefXidRecord tables have mismatching number of entries"));
        } // if: mismatch
        
        // Return the map
        return xidmap;
    } // calculate_xid_map()
    
    
    
    
    
} // class UpdateLogic







