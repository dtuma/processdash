// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003 Software Process Dashboard Initiative
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  tuma@users.sourceforge.net

package net.sourceforge.processdash.data.repository;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

// 77305 MB is the mem usage to beat.

/** Implements a specialized hashtable.
 *
 * Keys must be strings, but values can be of any type. Neither keys
 * nor values may be null.
 *
 * Key strings are assumed to contain frequent instances of the '/'
 * character. Keys are split apart on these '/' characters, and a
 * hierarchically nested tree of hashtables is used to store the
 * results.
 */
public class HashTree {

    /** The character used to split apart key strings */
    private static final char SEPARATOR = '/';

    /** The default capacity of a HashTree node */
    private static final int DEFAULT_CAPACITY = 4;

    /** The default capacity of a HashTree nodes attributes table */
    private static final int DEFAULT_ATTR_CAPACITY = 4;

    /** The contents of this node of the HashTree */
    private HashMap contents;

    /** Attribute values associated with this node */
    private HashMap attributes;

    /** The root of this collection of HashTree nodes. */
    private HashTree root;

    /** The canonical parent of this node of the HashTree. (The root
     * node of the HashTree will have a null parent) */
    private HashTree parent;



    /** Create a new, empty HashTree with a default capacity */
    public HashTree() {
        this(DEFAULT_CAPACITY);
    }



    /** Create a new, empty HashTree with the given capacity */
    public HashTree(int capacity) {
        contents = new HashMap(capacity);
        attributes = null;
        root = this;
        parent = null;
    }



    /** Create a HashTree node with the given node as its parent */
    protected HashTree(HashTree parent, int capacity) {
        this.contents = new HashMap(capacity);
        this.attributes = null;
        this.root = (parent == null ? this : parent.root);
        this.parent = parent;
    }


    public HashTree getRoot() {
        return root;
    }
    public HashTree getParent() {
        return parent;
    }


    /** Returns the value to which the specified key is mapped in this
     * hashtree.
     *
     * If key starts with a slash, it is an absolute path and will be
     * looked up relative to the root of the HashTree hierarchy.  Otherwise,
     * it is looked up relative to this node of the HashTree.
     */
    public Object get(String key) {
        // null keys are not allowed.
        if (key == null)
            return null;

        synchronized (root) {
            // if the key starts with a slash, it is absolute, relative to the
            // root of the HashTree
            if (key.startsWith("/"))
                return root.getImpl(key.substring(1));

            // if the key is not absolute, call getImpl to lookup the relative
            // reference.
            else
                return getImpl(key);
        }
    }



    /** Returns the value to which the specified key is mapped in this
     * hashtree, when key is known to be a non-null, relative reference.
     */
    protected Object getImpl(String key) {

        int slashPos = key.indexOf(SEPARATOR);
        if (slashPos == -1)
            return contents.get(key);

        slashPos++;
        String subKey = key.substring(0, slashPos);
        HashTree subHash;
        if ("../".equals(subKey) && parent != null)
            subHash = parent;
        else
            subHash = (HashTree) contents.get(subKey);

        if (slashPos == key.length())
            return subHash;
        else if (subHash != null)
            return subHash.getImpl(key.substring(slashPos));
        else
            return null;
    }



    /** Removes the key (and its corresponding value) from this hashtree.
     * This method does nothing if the key is not in the hashtree.
     * @return the item that was removed.
     */
    public Object remove(String key) {
        if (key == null) return null;

        synchronized (root) {
            // if the key starts with a slash, it is absolute, relative to the
            // root of the HashTree
            if (key.startsWith("/"))
                return root.removeImpl(key.substring(1));

            // if the key is not absolute, call removeImpl to look up the
            // relative reference.
            else
                return removeImpl(key);
        }
    }



    /** Removes the key (and its corresponding value) from this hashtree,
     * when key is known to be a non-null, relative reference.
     */
    protected Object removeImpl(String key) {

        int slashPos = key.indexOf(SEPARATOR);
        if (slashPos == -1)
            return contents.remove(key);

        slashPos++;
        if (slashPos == key.length()) {
            HashTree result = (HashTree) contents.remove(key);
            if (result != null && result.parent == this)
                result.parent = null;
            return result;
        }

        String subKey = key.substring(0, slashPos);
        HashTree subHash;
        if ("../".equals(subKey) && parent != null)
            subHash = parent;
        else
            subHash = (HashTree) contents.get(subKey);

        if (subHash != null)
            return subHash.removeImpl(key.substring(slashPos));
        else
            return null;
    }



    /** Maps the specified key to the specified value in this
     * hashtree.  Neither the key nor the value can be null.  The
     * value can be retrieved by calling the get method with a key
     * that is equal to the original key.
     */
    public Object put(String key, Object value) {
        // check for error conditions.
        if (key == null)
            throw new NullPointerException
                ("null keys are not allowed in HashTree objects");
        else if (value == null)
            throw new NullPointerException
                ("null values not allowed in HashTree objects");
        else if (key.endsWith("/") && !(value instanceof HashTree))
            throw new IllegalArgumentException
                ("key names a context, but value is not a HashTree");

        synchronized (root) {
            // if the key starts with a slash, it is absolute, relative to the
            // root of the HashTree
            if (key.startsWith("/"))
                return root.putImpl(key.substring(1), value);

            // if the key is not absolute, call putImpl to work with the
            // relative reference.
            else
                return putImpl(key, value);
        }
    }

    /** Maps the specified key to the specified value in this
     * hashtree, where key is known to be a non-null, relative
     * reference.
     */
    protected Object putImpl(String key, Object value) {

        int slashPos = key.indexOf(SEPARATOR);
        if (slashPos == -1)
            return contents.put(key, value);

        slashPos++;
        if (slashPos == key.length()) {
            HashTree child = (HashTree) value;
            child.root = this.root;
            // note - the next line assumes only one parent per node.
            child.parent = this;
            return contents.put(key, value);
        }

        String subKey = key.substring(0, slashPos);
        HashTree subHash = (HashTree) contents.get(subKey);
        if (subHash == null) {
            subHash = new HashTree(this, DEFAULT_CAPACITY);
            contents.put(subKey.intern(), subHash);
        }

        return subHash.putImpl(key.substring(slashPos), value);
    }

    /*
    public void putAll(Map m) {
        synchronized (root) {
            Iterator i = m.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                if (e.getKey() instanceof String)
                    put((String) e.getKey(), e.getValue());
            }
        }
    }
    */

    public Object getAttribute(String name, boolean inherit) {
        Object result = null;
        synchronized (this) {
            if (attributes != null) result = attributes.get(name);
        }
        if (result == null && inherit && parent != null)
            return parent.getAttribute(name, true);
        else
            return result;
    }

    public synchronized void putAttribute(String name, Object value) {
        if (attributes == null)
            attributes = new HashMap(DEFAULT_ATTR_CAPACITY);
        attributes.put(name, value);
    }



    /** Return an iterator containing all the keys in this hashtree.
     */
    public Iterator getAllKeys() {
        return getKeysEndingWith(null);
    }


    public Iterator getKeysEndingWith(String terminalName) {
        synchronized (root) {
            return new HashTreeIterator
                (this.keyClone(null, terminalName),
                 (this == this.root ? "/" : ""));
        }
    }



    /** Return a copy of this hashtree, with all keys mapped to a bogus
     * value. If the resulting hashtree is empty, returns null.
     */
    private HashTree keyClone(HashTree parent, String terminalName) {
        int s = contents.size();
        if (s == 0) return null;

        HashTree result = new HashTree(parent, s*2);
        Iterator i = contents.entrySet().iterator();
        Map.Entry e;
        Object val;
        while (i.hasNext()) {
            e = (Map.Entry) i.next();
            if (e.getValue() instanceof HashTree)
                val = ((HashTree) e.getValue()).keyClone(result, terminalName);
            else if (terminalName == null || terminalName.equals(e.getKey()))
                val = Boolean.TRUE;
            else
                val = null;

            if (val != null)
                result.contents.put(e.getKey(), val);
        }

        if (result.contents.size() == 0)
            return null;
        else
            return result;
    }

    private class HashTreeIterator implements Iterator {
        /** this iterates over the entries in our "contents" field. */
        Iterator top;

        /** if the current item returned by the "top" iterator was a HashTree,
         * this iterates over that hashtree. */
        Iterator sub;

        /** Append this prefix to all the values we return. */
        String prefix;



        private HashTreeIterator(HashTree t, String prefix) {
            if (t == null)
                top = null;
            else
                top = t.contents.entrySet().iterator();
            sub = null;
            this.prefix = prefix;
        }


        /** Returns true if the iteration has more elements. (In other
         * words, returns true if next would return an element rather
         * than throwing an exception.)  */
        public boolean hasNext() {
            return ((sub != null && sub.hasNext()) ||
                    (top != null && top.hasNext()));
        }


        /** Returns the next element in the iteration */
        public Object next() {
            if (sub != null && sub.hasNext())
                return sub.next();

            if (top == null)
                throw new NoSuchElementException();

            // the next line could (correctly) throw NoSuchElementException
            Map.Entry e = (Map.Entry) top.next();
            String subKey = prefix + (String) e.getKey();

            if (e.getValue() instanceof HashTree) {
                sub = new HashTreeIterator((HashTree) e.getValue(), subKey);
                return sub.next();

            } else {
                sub = null;
                return subKey;
            }
        }

        /** This operation is not supported by this iterator. */
        public void remove() {
            throw new UnsupportedOperationException
                ("HashTreeIterator does not support remove()");
        }
    }

}
