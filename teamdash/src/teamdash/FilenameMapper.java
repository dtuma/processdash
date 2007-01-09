package teamdash;

import net.sourceforge.processdash.util.StringMapper;

/** Consult the ExternalResourceManager to remap filenames, if that
 * class exists in the hosting dashboard.
 * 
 * This class acts as an insulator for backward binary compatibility.
 * If the current team process is running in an
 * older version of the dashboard, which does not have the
 * ExternalResourceManager, this class will catch the ClassNotFoundException
 * and gracefully degrade to no-op functionality.
 */
public class FilenameMapper {

    private static StringMapper delegate;

    static {
        try {
            String className = FilenameMapper.class.getName() + "ExtResMgr";
            Class clz = Class.forName(className);
            delegate = (StringMapper) clz.newInstance();
        } catch (Throwable t) {
            delegate = null;
        }
    }


    public static String remap(String filename) {
        if (delegate == null)
            return filename;
        else
            return delegate.getString(filename);
    }
}
