
package pspdash;


/**
 * <P>  The <B>ProfTimer</B> class is used for crude profiling.
 * <P>
 */
public class ProfTimer {

    String name;
    long lastTime;
    boolean print = true;

    private void init(String name, boolean print) {
        int idnum = (int) (Math.random() * 10000.0);
        this.name = name + "(" + idnum + "): ";
        lastTime = System.currentTimeMillis();
        this.print = print;
        if (print)
            System.out.println(this.name + "starting.");
    }
    public ProfTimer(String name) {
        init(name, true);
    }
    public ProfTimer(String name, boolean print) {
        init(name, print);
    }

    public void click( String msg )
    {
        long currTime = System.currentTimeMillis();
        long diff = currTime - lastTime;
        if (print)
            System.out.println(name + msg + ", took " + diff + "ms.");
        lastTime = currTime;
    }
}
