
package teamdash.wbs;


public class IntList {

    private int[] contents;
    private int length;

    public IntList() { this(10); }

    public IntList(int maxLen) {
        contents = new int[maxLen];
        length = 0;
    }

    public int size() { return length; }

    public synchronized void add(int i) {
        ensureCapacity(length+1);
        contents[length++] = i;
    }

    public synchronized void addAll(IntList other) {
        for (int i = 0;   i < other.size();   i++)
            add(other.get(i));
    }

    public synchronized void ensureCapacity(int totalCapacity) {
        if (totalCapacity <= contents.length) return;

        int doubleSize = contents.length * 2;
        if (totalCapacity < doubleSize) totalCapacity = doubleSize;

        int[] newContents = new int[totalCapacity];
        System.arraycopy(contents, 0, newContents, 0, length);
        contents = newContents;
    }

    public int get(int pos) {
        if (pos >= 0  && pos < length) return contents[pos];
        return -1;
    }

    public int[] getAsArray() {
        int[] result = new int[length];
        System.arraycopy(contents, 0, result, 0, length);
        return result;
    }

    public String toString() {
        if (size() == 0) return "";
        StringBuffer b = new StringBuffer();
        for (int i=0;   i < size();   i++)
            b.append(get(i)).append(",");
        String result = b.toString();
        return result.substring(0, result.length()-1);
    }

}
