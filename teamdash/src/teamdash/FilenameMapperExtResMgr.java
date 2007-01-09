package teamdash;

import net.sourceforge.processdash.tool.export.mgr.ExternalResourceManager;
import net.sourceforge.processdash.util.StringMapper;

/** Consult the ExternalResourceManager to remap filenames, if appropriate.
 */
public class FilenameMapperExtResMgr implements StringMapper {

    private ExternalResourceManager mgr;

    public FilenameMapperExtResMgr() {
        mgr = ExternalResourceManager.getInstance();
    }

    public String getString(String filename) {
        return mgr.remapFilename(filename);
    }

}
