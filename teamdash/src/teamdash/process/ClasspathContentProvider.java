package teamdash.process;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.sourceforge.processdash.net.http.ContentSource;
import net.sourceforge.processdash.util.FileUtils;

public class ClasspathContentProvider implements ContentSource {

    private String contentPrefix;

    public ClasspathContentProvider() {
        this("/Templates");
    }

    public ClasspathContentProvider(String prefix) {
        this.contentPrefix = prefix;
    }

    public byte[] getContent(String context, String uri, boolean raw)
            throws IOException {
        if (!uri.startsWith("/")) {
            URL contextURL = new URL("http://unimportant" + context);
            URL uriURL = new URL(contextURL, uri);
            uri = uriURL.getFile();
        }
        String resName = contentPrefix + uri;
        InputStream in = GenerateProcess.class.getResourceAsStream(resName);
        if (in == null)
            throw new IOException("No such file");
        else
            return FileUtils.slurpContents(in, true);
    }

}
