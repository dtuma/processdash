package teamdash.process;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import net.sourceforge.processdash.DashboardContext;
import net.sourceforge.processdash.templates.TemplateLoader;

public class CustomProcessEditorAction extends AbstractAction {

    private DashboardContext context;

    public CustomProcessEditorAction() {
        super("Team Metrics Framework Editor");
    }

    public void setDashboardContext(DashboardContext context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent e) {
        CustomProcessEditor editor = new CustomProcessEditor(null, context
                .getWebServer());

        File defaultTemplatesDir = TemplateLoader.getDefaultTemplatesDir();
        if (defaultTemplatesDir != null)
            defaultTemplatesDir.mkdir();
        editor.setDefaultDirectory(defaultTemplatesDir);
    }

}
