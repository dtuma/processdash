// Copyright (C) 2000-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.data.repository;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;


/**
 * A <code>PrefixHierarchy</code> object is a node in a multiply-branching
 * tree designed to optimize the dispatch of data added and data removed
 * events.
 * <p>
 * Most of the listeners in the repository, when they receive a data added or
 * data removed event, invoke a (computationally expensive) Perl-style pattern
 * match to see if they are really interested in the event.  We can therefore
 * greatly increase efficiency if we only dispatch events to potentially
 * interested parties.
 * <p>
 * Most of the objects interested in added/removed events are not interested
 * in the entire repository.  Rather, they are interested in elements added
 * or deleted underneath a certain path.  This object organizes those
 * dependencies in a hierarchical manner for efficient dispatch with minimal
 * string comparison.
 * <p>
 * Each node in the tree contains a list of interested listeners, and a
 * set of children.  Each child has a prefix associated with it.
 */


class PrefixHierarchy {


    /**
     * A list of RepositoryListeners that are interested in all events that
     * reache this node of the tree.
     */
    Vector listeners = null;


    /** A list of children of this node in the tree.  The keys in the
     * Hashtable represent possible prefixes, and the values represent
     * children of this node.  It is also important to realize that
     * these prefixes are normalized to this node: prefix characters
     * associated with the parent of this node (if any) have already
     * been removed.  The keys therefore no longer contain any data
     * about the parent's prefix. <p>
     *
     * If a data event's name begins with one of the prefixes in the
     * hashtable, then the associated child is also interested in the
     * event.  The keys in this Hashtable are maintained such that they
     * all start with a different initial character.  Thus, at any given
     * node of the tree, no more than one child will be interested in a
     * particular data event.
     */
    Hashtable children = null;



    /** Create an empty PrefixHierarchy. */
    public PrefixHierarchy() {}



    // create a PrefixHierarchy containing one listener.
    //
    private PrefixHierarchy(RepositoryListener l) {
        addListener(l);
    }



    // create a PrefixHierarchy with two children. Name1 should map to child1,
    // and name2 should map to a new PrefixHierarchy for listener2.
    //
    private PrefixHierarchy(String name1, PrefixHierarchy child1,
                            String name2, RepositoryListener listener2) {

        children = new Hashtable(3);
        children.put(name1, child1);

        if (name2.length() == 0)
            addListener(listener2);
        else
            children.put(name2, new PrefixHierarchy(listener2));
    }



    public void dispatchAdded(String dataName) {
        dispatch(true, dataName, 0);
    }
    public void dispatchRemoved(String dataName) {
        dispatch(false, dataName, 0);
    }



    // Perform the work associated with dispatching data event e .
    //
    private void dispatch(boolean added, String fullName, int prefixLen) {
        dispatchToListeners(added, fullName);
        dispatchToChildren(added, fullName, prefixLen);
    }



    // If we have a list of listeners, notify them of this event. Otherwise,
    // do nothing.
    //
    private void dispatchToListeners(boolean added, String dataName) {

        if (listeners == null) return;

        int i = listeners.size();

        if (added)
            while (i-- != 0 )
                ((RepositoryListener) listeners.elementAt(i)).dataAdded(dataName);

        else
            while (i-- != 0)
                ((RepositoryListener) listeners.elementAt(i)).dataRemoved(dataName);
    }



    // If we have any children, find the one child that is interested in this
    // event, and dispatch it to that child.
    //
    private void dispatchToChildren(boolean added, String fullName,
            int prefixLen) {
        if (children == null) return;

        PrefixHierarchy child = null;
        String prefix = null;
        synchronized (children) {
            for (Iterator i = children.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                prefix = (String) e.getKey();
                if (fullName.startsWith(prefix, prefixLen)) {
                    child = (PrefixHierarchy) e.getValue();
                    break;
                }
            }
        }
        if (child != null)
            child.dispatch(added, fullName, prefixLen + prefix.length());
    }



    // Add this listener to our list.
    //
    private synchronized void addListener(RepositoryListener l) {
        if (listeners == null) listeners = new Vector();
        listeners.addElement(l);
    }



    /** add a listener. This listener is interested in dataAdded and
     *  dataRemoved events for elements whose name begins with @param prefix.
     */
    public void addListener(RepositoryListener l, String prefix) {

                                  // if prefix is the empty string, just add
                                  // this listener to our listener list.
        if (prefix == null || prefix.length() == 0) {
            addListener(l);
            return;
        }

                                    // create the "children" member if it
                                    // doesn't already exist.
        synchronized (this) {
            if (children == null) children = new Hashtable(3);
        }

        synchronized (children) {
            addListenerImpl(l, prefix);
        }
    }

    private void addListenerImpl(RepositoryListener l, String prefix) {
                                  // if "prefix" already appears as the prefix
                                  // for one of our children (a common case),
                                  // simply add the listener to the child's
                                  // list.
        PrefixHierarchy child = (PrefixHierarchy) children.get(prefix);
        if (child != null) {
            child.addListener(l);
            return;
        }

                                    // step through all of our children.
        Enumeration childPrefixes = children.keys();
        String childPrefix;
        int match;
        while (childPrefixes.hasMoreElements()) {
            childPrefix = (String) childPrefixes.nextElement();
            match = initialMatchLength(childPrefix, prefix);
            switch (match) {
                                      // if "prefix" doesn't share any characters
                                      // with this child's prefix, skip this child
            case  0: break;		// and look at the next one.

                                      // if "prefix" begins with this child's
            case -1:			// prefix, add this listener to this child.
                ((PrefixHierarchy) children.get(childPrefix)).addListener
                    (l, prefix.substring(childPrefix.length()));
                return;

                                        // if this child's prefix starts with
                                        // "prefix", or if they share any initial
                                        // characters, create a new node in the tree
                                        // to act as a decision point between this
            default:			// child and this listener.
                children.put
                    (prefix.substring(0, match),
                     new PrefixHierarchy(childPrefix.substring(match),
                                         (PrefixHierarchy) children.remove(childPrefix),
                                         prefix.substring(match), l));
                return;
            }
        }

                                    // getting to this point means that "prefix"
                                    // is not like any of our children.  Just
                                    // add it to our list of children.
        children.put(prefix, new PrefixHierarchy(l));
    }



    /** Remove @param rl from this data structure.
     */
    public void removeListener(RepositoryListener rl) {
        if (listeners != null)
            listeners.removeElement(rl);

        if (children != null) {
            Enumeration c = children.elements();
            while (c.hasMoreElements())
                ((PrefixHierarchy) c.nextElement()).removeListener(rl);
        }
    }



    // If @param s2 startsWith @param s1, return -1.
    // If the first n characters of @param s1 and @param s2 are equal, return n.
    // Otherwise return 0.
    //
    private int initialMatchLength(String s1, String s2) {
        int result = 0;
        int len1 = s1.length(), len2 = s2.length();
        int len = (len1 < len2 ? len1 : len2);

        while ((result < len) &&
               (s1.charAt(result) == s2.charAt(result)))
            result++;

        return (result == len1 ? -1 : result);
    }


    public void debugPrint() {
        System.out.println("PrefixHierarchy dump:");
        debugPrint("    ");
    }
    private void debugPrint(String p) {
        if (listeners != null)
            System.out.println(p + "# listeners: "+listeners.size());
        if (children != null)
            for (Enumeration c = children.keys(); c.hasMoreElements(); ) {
                String n = (String) c.nextElement();
                System.out.println(p + "'" + n + "'");
                ((PrefixHierarchy) children.get(n)).debugPrint(p + "    ");
            }
    }
}

