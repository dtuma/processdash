package teamdash.process;

import java.io.File;
import java.io.IOException;

import net.sourceforge.processdash.net.http.ContentSource;

public class CustomProcessEditor extends AbstractCustomProcessEditor {

    public static void main(String[] args) {
        new CustomProcessEditor(null, new ClasspathContentProvider());
    }

    ContentSource contentSource;

    public CustomProcessEditor(String prefix, ContentSource contentSource) {
        super(prefix);
        this.contentSource = contentSource;
        frame.setVisible(true);
    }

    protected void publishProcess(CustomProcess process, File destFile)
            throws IOException {
        CustomProcessPublisher.publish(process, destFile, contentSource);
    }
}
