// Copyright (C) 2003-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.util;

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
    protected static final char SEPARATOR_CHAR = '/';
    protected static final String SEPARATOR = "/";

    /** The name used to refer to the parent of a context */
    protected static final String PARENT_NAME = "..";
    protected static final String PARENT_PREFIX = PARENT_NAME + SEPARATOR;
    private static final String PARENT_COMPONENT =
        SEPARATOR + PARENT_NAME + SEPARATOR;

    /** The default capacity of a HashTree node */
    protected static final int DEFAULT_CAPACITY = 4;

    /** The default capacity of a HashTree nodes attributes table */
    private static final int DEFAULT_ATTR_CAPACITY = 4;

    /** The contents of this node of the HashTree */
    protected Map contents;

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



    /** Create a new, empty HashTree that uses the given Map class for storage.
     */
    public HashTree(Class mapClass) throws IllegalArgumentException {
        try {
            contents = (Map) mapClass.newInstance();
        } catch (Exception e) {
            IllegalArgumentException iae = new IllegalArgumentException();
            iae.initCause(e);
            throw iae;
        }

        attributes = null;
        root = this;
        parent = null;
    }



    /** Create a HashTree node with the given node as its parent */
    protected HashTree(HashTree parent, int capacity) {
        if (parent != null && !(parent.contents instanceof HashMap))
            try {
                this.contents = (Map) parent.contents.getClass().newInstance();
            } catch (Throwable t) {
                this.contents = new HashMap(capacity);
            }
        else
            this.contents = new HashMap(capacity);

        this.attributes = null;
        this.root = (parent == null ? this : parent.root);
        this.parent = parent;
    }



    /** Get the root of the entire HashTree */
    public HashTree getRoot() {
        return root;
    }



    /** Get the immediate parent of this node in the HashTree */
    public HashTree getParent() {
        return parent;
    }


    /** Returns an iteration of the keys which name children of this HashTree node.
     */
    public EnumerIterator getChildren() {
        return new ChildFilter();
    }


    /** Returns an iteration of the keys which name the contents of this HashTree node.
     */
    public EnumerIterator getContents() {
        return new ContentsFilter();
    }


    /** Returns the value to which the specified key is mapped in this
     * hashtree.
     *
     * If key starts with a slash, it is an absolute path and will be
     * looked up relative to the root of the HashTree hierarchy.  Otherwise,
     * it is looked up relative to this node of the HashTree.
     */
    public Object get(String key) {
        return getImpl0(key, false);
    }

    /** Returns the deepest HashTree object that is currently nested under
     * this object, that would be an ancestor of the given key.
     * 
     * Note that if the key exactly names a tree, but does not end in a slash,
     * this will return the parent of that tree.
     */
    public HashTree getDeepestExistingSubtree(String key) {
        return (HashTree) getImpl0(key, true);
    }

    protected Object getImpl0(String key, boolean deepestChild) {
        // null keys are not allowed.
        if (key == null)
            return null;

        // if the key starts with a slash, it is absolute, relative to the
        // root of the HashTree
        if (key.startsWith(SEPARATOR))
            return root.getImpl1(canonicalizeKey(key.substring(1)), deepestChild);

        // remove ".." sequences from the key
        key = canonicalizeKey(key);

        // if the key starts with "../" it is relative to the parent context
        if (key.startsWith(PARENT_PREFIX))
            return (parent == null ? null
                    : parent.get(key.substring(PARENT_PREFIX.length())));

        // if the key is not absolute, call getImpl to lookup the relative
        // reference.
        else
            return getImpl1(key, deepestChild);
    }



    /** Returns the value to which the specified key is mapped in this
     * hashtree, when key is known to be a non-null, relative reference.
     */
    protected synchronized Object getImpl1(String key, boolean deepestChild) {
        if (key.length() == 0)
            return this;

        int slashPos = key.indexOf(SEPARATOR_CHAR);
        if (slashPos == -1)
            if (deepestChild)
                return this;
            else
                return contents.get(key);

        slashPos++;
        String subKey = key.substring(0, slashPos);
        HashTree subHash = (HashTree) contents.get(subKey);

        if (subHash == null)
            return deepestChild ? this : null;
        else
            return subHash.getImpl1(key.substring(slashPos), deepestChild);
    }



    /** Removes the key (and its corresponding value) from this hashtree.
     * This method does nothing if the key is not in the hashtree.
     * @return the item that was removed.
     */
    public Object remove(String key) {
        if (key == null) return null;

        // if the key starts with a slash, it is absolute, relative to the
        // root of the HashTree
        if (key.startsWith(SEPARATOR))
            return root.removeImpl(canonicalizeKey(key.substring(1)));

        key = canonicalizeKey(key);

        // if the key starts with "../" it is relative to the parent context
        if (key.startsWith(PARENT_PREFIX))
            return (parent == null ? null
                    : parent.remove(key.substring(PARENT_PREFIX.length())));

        // if the key is not absolute, call removeImpl to remove the
        // relative reference.
        else
            return removeImpl(key);
    }


    /** Removes the key (and its corresponding value) from this hashtree,
     * when key is known to be a non-null, relative reference.
     */
    protected synchronized Object removeImpl(String key) {

        int slashPos = key.indexOf(SEPARATOR_CHAR);
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
        HashTree subHash = (HashTree) contents.get(subKey);

        if (subHash == null)
            return null;
        else
            return subHash.removeImpl(key.substring(slashPos));
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
                ("Null keys are not allowed in HashTree objects");
        if (value == null)
            throw new NullPointerException
                ("Null values not allowed in HashTree objects");

        // if the key starts with a slash, it is absolute, relative to the
        // root of the HashTree
        if (key.startsWith(SEPARATOR))
            return root.putImpl(canonicalizeKey(key.substring(1)), value);

        key = canonicalizeKey(key);

        // if the key starts with "../" it is relative to the parent context
        if (key.startsWith(PARENT_PREFIX)) {
            if (parent == null)
                throw new IllegalArgumentException
                    ("No such parent exists for put operation");
            else
                return parent.put
                    (key.substring(PARENT_PREFIX.length()), value);
        }

        // if the key is not absolute, call putImpl to save the value to the
        // relative reference.
        else
            return putImpl(key, value);
    }


    /** Check put parameters for validity */
    protected Object putImpl(String key, Object value) {
        if (key.length() == 0)
            throw new IllegalArgumentException
                ("Cannot replace self using put operation");

        if (key.endsWith(SEPARATOR) && !isValidHashTreeNode(value))
            throw new IllegalArgumentException
                ("Key names a context, but value is not a valid HashTree");

        return putImpl2(key, value);
    }


    /** Maps the specified key to the specified value in this
     * hashtree, where key is known to be a non-null, relative
     * reference.
     */
    protected Object putImpl2(String key, Object value) {

        int slashPos = key.indexOf(SEPARATOR_CHAR);
        if (slashPos == -1)
            return contents.put(StringUtils.intern(key, true), value);

        slashPos++;
        if (slashPos == key.length()) {
            HashTree child = (HashTree) value;
            child.root = this.root;
            // note - the next line assumes only one parent per node.
            child.parent = this;
            return contents.put(StringUtils.intern(key, true), value);
        }

        String subKey = key.substring(0, slashPos);
        HashTree subHash = (HashTree) contents.get(subKey);
        if (subHash == null) {
            subHash = newHashTreeNode();
            contents.put(StringUtils.intern(subKey, true), subHash);
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



    protected HashTree newHashTreeNode() {
        return new HashTree(this, DEFAULT_CAPACITY);
    }
    protected boolean isValidHashTreeNode(Object obj) {
        return (obj instanceof HashTree);
    }



    /** Associate an attribute value with this node in the HashTree
     */
    public synchronized void putAttribute(String name, Object value) {
        if (attributes == null)
            attributes = new HashMap(DEFAULT_ATTR_CAPACITY);
        attributes.put(name, value);
    }



    /** Retrieve an attributes value previously stored on this node
     * with the putAttribute method.
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


    /** Remove "../" sequences from the given key. */
    protected static String canonicalizeKey(String key) {
        if (key.equals(PARENT_NAME))
            return PARENT_PREFIX;
        if (key.endsWith(SEPARATOR+PARENT_NAME))
            key = key + SEPARATOR;

        int end = 0;
        int pos, beg;
        while (true) {
            pos = key.indexOf(PARENT_COMPONENT, end);
            if (pos == -1)
                return key;

            beg = key.lastIndexOf(SEPARATOR_CHAR, pos-1) + 1;
            if (!key.regionMatches(beg, PARENT_NAME, 0, pos-beg)) {
                key = (key.substring(0, beg) +
                       key.substring(pos+PARENT_COMPONENT.length()));
                end = Math.min(0,beg-1);
            } else {
                end = pos + PARENT_COMPONENT.length();
            }
        }
    }



    /** Return an iterator containing all the keys in this hashtree.
     */
    public Iterator getAllKeys() {
        return getKeysEndingWith(null);
    }



    /** Return an iterator containing all the keys in this hashtree
     * that end with the given string.
     */
    public Iterator getKeysEndingWith(String terminalName) {
        if (terminalName == null || terminalName.indexOf(SEPARATOR_CHAR) == -1)
            return getKeysEndingWithSimpleTerminal(terminalName);
        else
            return getKeysEndingWithComplexTerminal(terminalName);
    }


    private HashTreeIterator getKeysEndingWithSimpleTerminal(String name) {
        return new HashTreeIterator
            (this.keyClone(null, name),
             (this == this.root ? SEPARATOR : ""));
    }


    private Iterator getKeysEndingWithComplexTerminal(String name) {
        int lastSlashPos = name.lastIndexOf(SEPARATOR_CHAR);
        String finalTerminal = name.substring(lastSlashPos+1);
        HashTreeIterator baseIterator =
            getKeysEndingWithSimpleTerminal(finalTerminal);
        return new HashTreeIteratorFilter(baseIterator, name);
    }


    /** Return a copy of this hashtree, with all keys mapped to a bogus
     * value. if terminalName is not null, only keys whose final name
     * component is equal to terminalName will be included.  If the resulting
     * hashtree is empty, returns null.
     */
    private synchronized HashTree keyClone(HashTree parent, String terminalName) {
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


    /** An iterator that returns all the keys in a given HashTree.
     *
     * This implementation assumes that there are no empty nodes in the
     * given HashTree - that is, that if a node was empty, it would have
     * been 'pruned' away before being given to the constructor of this
     * class.
     */
    private class HashTreeIterator implements Iterator {

        /** this iterates over the entries in the "contents" field of the
         * immediate HashTree node. */
        Iterator top;

        /** if the current item returned by the "top" iterator was a HashTree,
         * this iterates over that HashTree. */
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
            // note: this works because no brancehs of the tree are empty
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
                // this will return a value because no branches of the tree
                // are empty
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


    /** keyClone and HashTreeIterator together can return all keys ending
     * with a particular, single terminal name component (e.g. "Foo").  But
     * they cannot handle a more complex terminal name that is composed of
     * multiple name components (e.g. "Foo/Bar/Baz"). To make up for this
     * limitation, this class can sift through the results of a plain
     * HashTreeIterator and return only the items which end with the
     * complex terminal name.
     */
    private class HashTreeIteratorFilter extends IteratorFilter {

        private String terminal, slashTerminal;

        public HashTreeIteratorFilter(HashTreeIterator parent,
                                      String terminal)
        {
            super(parent);
            this.terminal = terminal;
            this.slashTerminal = SEPARATOR + terminal;
            init();
        }

        protected boolean includeInResults(Object o) {
            String s = (String) o;
            return s.equals(terminal) || s.endsWith(slashTerminal);
        }

    }

    private abstract class EntriesFilter extends IteratorFilter {

        protected EntriesFilter() {
            super(contents.entrySet().iterator());
            init();
        }

        protected boolean includeInResults(Object o) {
            Map.Entry e = (Map.Entry) o;
            String name = (String) e.getKey();
            return nameMatches(name);
        }

        abstract boolean nameMatches(String name);
    }

    private class ChildFilter extends EntriesFilter {
        boolean nameMatches(String name) {
            return name.endsWith(SEPARATOR);
        }
    }

    private class ContentsFilter extends EntriesFilter {
        boolean nameMatches(String name) {
            return !name.endsWith(SEPARATOR);
        }
    }

}
