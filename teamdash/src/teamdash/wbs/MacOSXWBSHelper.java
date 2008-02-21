package teamdash.wbs;

import net.sourceforge.processdash.ui.macosx.MacGUIUtils;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

public class MacOSXWBSHelper implements ApplicationListener {

    WBSEditor wbsEditor;

    public MacOSXWBSHelper(WBSEditor wbsEditor) {
        if (!MacGUIUtils.isMacOSX())
            throw new IllegalArgumentException("Not Mac OS X");

        this.wbsEditor = wbsEditor;
        Application.getApplication().addApplicationListener(this);
    }

    public void handleQuit(ApplicationEvent e) {
        wbsEditor.maybeClose();
    }

    public void handleAbout(ApplicationEvent e) {}

    public void handleOpenApplication(ApplicationEvent e) {}

    public void handleOpenFile(ApplicationEvent e) {}

    public void handlePreferences(ApplicationEvent e) {}

    public void handlePrintFile(ApplicationEvent e) {}

    public void handleReOpenApplication(ApplicationEvent e) {}

}
