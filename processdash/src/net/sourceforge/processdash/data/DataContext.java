package net.sourceforge.processdash.data;

public interface DataContext {

    public SaveableData getValue(String name);

    public SimpleData getSimpleValue(String name);

    public void putValue(String name, SaveableData value);

}
