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

package xmlsnippets.core;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Collection;

/**
 * A reference to a property or xidentified XML element.
 */
public class Xref
{

    // MEMBER VARIABLES
    //==================

    /**
     * The base XML element xid
     *
     */
    public Xid base;

    /**
     * The property path expression with respect to the base element.
     */
    public List<String> path;

    // CONSTRUCTORS
    //==============

    /**
     * Creates an empty XReference
     */
    public Xref() {
        base = null;
        path = new LinkedList<String>();
    } // ctor

    public Xref(Xid base) {
        this(base, null);
    }

    public Xref(Xid base, Collection<String> path) {
        if (base != null) {
            this.base = (Xid) base.clone();
        } else {
            this.base = null;
        }

        if (path != null) {
            this.path = new LinkedList<String>(path);
        } else {
            this.path = null;
        }
    }

    // JAVA OBJECT OVERRIDES
    //=======================

    /**
     * Creates an equivalent clone. The minor and major version numbers
     * are also cloned, even though they are not neccessary for
     * the equivalence.

     * @return a clone for which {@code equals()} is {@code true}.
     */
    @Override
    public Object clone() {
        return new Xref(base, path);
    } // clone()

    /**
     * Converts the identity to a simple string representation).
     */
    public String toString() {
        // TODO: Should use XrefString.serialize()
        return super.toString();
    } // toString()

    /**
     * Tests the equivalence of {@code Xid} objects.
     *
     * @return {@code true} if both {@link #id} and {@link #rev}
     * are equal. Otherwise, {@code false} is returned.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            //System.out.printf("other was null\n");
            return false;
        }
        // other != null
        if (other instanceof Xref) {
            Xref xref = (Xref) other;

            if ((this.base == null) && (xref.base == null)) {
                // match
            }
            else if ((this.base != null) && (xref.base != null)) {
                // compare
                if (this.base.equals(xref.base) == false) {
                    return false;
                } else {
                    // match
                }
            }
            else {
                return false;
            } // if-else


            if ((this.path == null) && (xref.path == null)) {
                // match
            }
            else if ((this.path != null) && (xref.path != null)) {
                // compare lists. TODO
                ListIterator<String> iter1 = this.path.listIterator();
                ListIterator<String> iter2 = xref.path.listIterator();

                while (iter1.hasNext() && iter2.hasNext()) {
                    String s1 = iter1.next();
                    String s2 = iter2.next();
                    if (s1.equals(s2) == false) {
                        return false;
                    }
                } // while

                if (iter1.hasNext() != iter2.hasNext()) {
                    return false;
                }
            }
            else {
                return false;
            }

            return true;
        } // if: correct dynamic type

        // Either unequal or not an instancof the class at all.
        return false;
    } // equals()

    /**
     * Hash code corresponding to the overrided equals() method.
     *
     */
    @Override
    public int hashCode() {
        int rval = 0;
        if (base != null) {
            rval = rval | (base.hashCode() & 0xffff000);
        }

        if (path != null) {
            rval = rval | (path.hashCode() & 0x0000fff);
        }

        return rval;
    } // hashCode()

} // class Xid