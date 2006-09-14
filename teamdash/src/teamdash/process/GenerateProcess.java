package teamdash.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Document;

import teamdash.XMLUtils;

import net.sourceforge.processdash.net.http.DashboardURLStreamHandlerFactory;
import net.sourceforge.processdash.net.http.WebServer;

public class GenerateProcess {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.err.println("You must supply the name of a "
                        + "settings.xml file on the command line.");
                System.exit(1);
            }
            File settingsFile = new File(args[0]);
            if (!settingsFile.exists()) {
                System.err.println("Could not open the file '" + args[0] + "'");
                System.exit(1);
            }
            Document settings = null;
            try {
                settings = XMLUtils.parse(new FileInputStream(settingsFile));
            } catch (Exception e) {
                System.err.println("Invalid settings file '" + args[0] + "'");
                System.exit(1);
            }

            String destDirname = ".";
            if (args.length > 1)
                destDirname = args[1];
            File destDir = new File(destDirname);
            if (!destDir.isDirectory()) {
                System.err.println("No such directory '" + args[1] + "'");
                System.exit(1);
            }

            GenerateProcess instance = new GenerateProcess(destDir, settings,
                    settingsFile.toURL());
            instance.run();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File destDir;

    private Document settingsFile;

    private URL extBase;

    public GenerateProcess(File destDir, Document settingsFile, URL extBase) {
        this.destDir = destDir;
        this.settingsFile = settingsFile;
        this.extBase = extBase;
    }

    private void run() throws Exception {
        CustomProcess process = new CustomProcess(settingsFile);

        File outputFile = new File(destDir, process.getJarName());

        WebServer webServer = getTinyWebServer();
        CustomProcessPublisher.publish(process, outputFile, webServer, extBase);
    }

    static WebServer getTinyWebServer() throws IOException {
        URL[] roots = getRoots();

        // do not reinitialize factory
        DashboardURLStreamHandlerFactory.disable();
        WebServer result = new WebServer();
        result.setRoots(roots);
        return result;
    }

    static URL[] getRoots() throws IOException {
        URL[] result = new URL[2];
        result[0] = fixURL(getUrlForClass(GenerateProcess.class));
        result[1] = fixURL(getUrlForClass(WebServer.class));

        return result;
    }

    static URL getUrlForClass(Class class1) {
        String resourceName = "/" + class1.getName().replace('.', '/')
                + ".class";
        return GenerateProcess.class.getResource(resourceName);
    }

    static URL fixURL(URL u) throws MalformedURLException {
        String url = u.toString();
        if (url.startsWith("jar:")) {
            int exclPos = url.indexOf("!/");
            url = url.substring(0, exclPos) + "!/Templates/";
        } else if (url.indexOf("/bin") != -1) {
            int binPos = url.indexOf("/bin");
            url = url.substring(0, binPos) + "/Templates/";
        }
        // System.out.println("Using url: " + url);
        return new URL(url);
    }

}
