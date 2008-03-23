package teamdash.templates.setup;

import java.io.IOException;
import java.net.URL;

import net.sourceforge.processdash.templates.TemplateLoader;
import net.sourceforge.processdash.ui.web.TinyCGIBase;
import net.sourceforge.processdash.util.StringUtils;

public class GetAddOnInternalURL extends TinyCGIBase {

    @Override
    protected void writeContents() throws IOException {
        try {
            String baseResource = (String) env.get("SCRIPT_NAME");
            URL url = TemplateLoader.resolveURL(baseResource);
            if (url == null) {
                baseResource = baseResource + ".link";
                url = TemplateLoader.resolveURL(baseResource);
            }

            String relativeUri = getParameter("relativeUri");
            if (StringUtils.hasValue(relativeUri)) {
                url = new URL(url, relativeUri);
            }

            String absoluteUri = getParameter("absoluteUri");
            if (StringUtils.hasValue(absoluteUri)) {
                String base = url.toString();
                int exclPos = base.indexOf("!/");
                base = base.substring(0, exclPos + 2);
                if (absoluteUri.startsWith("/"))
                    absoluteUri = absoluteUri.substring(1);
                url = new URL(base + absoluteUri);
            }

            url.openStream().close();
            out.print(url.toString());
        } catch (Exception e) {
        }
    }

}
