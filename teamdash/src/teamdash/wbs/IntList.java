
package teamdash.wbs;

/** Certain calculations in the WBSModel require frequent creation and
 * manipulations of lists of integers.  Rather than using ArrayList and
 * allocating Integer objects on the heap, this class provides a simple
 * implementation of a list of primitive int values.
 *
 * This class does not attempt to provide all of the methods defined
 * in the java.util.List interface - it only provides what is needed
 * by the WBSModel logic.
 */
public class IntList {

    private int[] contents;
    private int length;


    /** Create a new, empty <code>int</code> list with a default capacity. */
    public IntList() { this(10); }


    /** Create a new, empty <code>int</code> list with the specified
     * capacity. */
    public IntList(int maxLen) {
        contents = new int[maxLen];
        length = 0;
    }


    /** Return the number of <code>int</code> values in this list */
    public int size() { return length; }


    /** Add a single <code>int</code> value to the end of this list */
    public synchronized void add(int i) {
        ensureCapacity(length+1);
        contents[length++] = i;
    }


    /** Add all of the <code>int</code> values from the other list to
     * the end of this list */
    public synchronized void addAll(IntList other) {
        ensureCapacity(length + other.size());
        for (int i = 0;   i < other.size();   i++)
            add(other.get(i));
    }


    /** ensure that this list has the capacity to hold a certain
     * number of <code>int</code> values, expanding the internal array
     * structure if necessary.
     */
    public synchronized void ensureCapacity(int totalCapacity) {
        if (totalCapacity <= contents.length) return;

        // grow the size in factors of two.
        int doubleSize = contents.length * 2;
        if (totalCapacity < doubleSize) totalCapacity = doubleSize;

        int[] newContents = new int[totalCapacity];
        System.arraycopy(contents, 0, newContents, 0, length);
        contents = newContents;
    }


    /** Get a single <code>int</code> value from this list.
     * @return the <code>int</code> in position <code>pos</code>,
     * or -1 if that position is invalid.
     */
    public int get(int pos) {
        if (pos >= 0  && pos < length) return contents[pos];
        return -1;
    }


    /** Return true if this list contains the given <code>int</code> value. */
    public boolean contains(int num) {
        for (int i=size();   i-- > 0; )
            if (contents[i] == num)
                return true;
        return false;
    }


    /** Return an array containing the <code>int</code> values in this
     * list.  If this list has size 0, so will the resulting array. */
    public int[] getAsArray() {
        int[] result = new int[length];
        System.arraycopy(contents, 0, result, 0, length);
        return result;
    }


    /** Return a string respresentation of this list, useful for
     * debugging purposes only.
     */
    public String toString() {
        if (size() == 0) return "";
        StringBuffer b = new StringBuffer();
        for (int i=0;   i < size();   i++)
            b.append(get(i)).append(",");
        String result = b.toString();
        return result.substring(0, result.length()-1);
    }

}
