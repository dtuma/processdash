package net.sourceforge.processdash.team.ui;

import java.io.IOException;
import java.net.URL;

import net.sourceforge.processdash.team.mcf.MCFManager;
import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;

public class GetProcessMetadataURL extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        // extract the ID of the MCF from the URI used to call this script
        String selfUri = (String) env.get("SCRIPT_NAME");
        int slashPos = selfUri.indexOf('/', 1);
        String mcfID = selfUri.substring(1, slashPos);

        // look up the URL for the process XML file in that MCF, and write it
        URL result = MCFManager.getInstance().getMcfSourceFileUrl(mcfID,
            TemplateLoader.MCF_PROCESS_XML);
        if (result != null)
            out.write(result.toString());
    }

}
