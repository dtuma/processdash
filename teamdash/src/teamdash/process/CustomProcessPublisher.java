
package teamdash.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pspdash.HTMLPreprocessor;
import pspdash.ProgressDialog;
import pspdash.StringUtils;
import pspdash.TemplateAutoData;
import pspdash.TinyWebServer;
import pspdash.XMLUtils;


public class CustomProcessPublisher {

    private static final String SETTINGS_FILENAME = "settings.xml";

    public static CustomProcess open(File openFile) {
        try {
            ZipFile zip = new ZipFile(openFile);
            ZipEntry entry = zip.getEntry(SETTINGS_FILENAME);
            if (entry == null) return null;
            Document doc = XMLUtils.parse(zip.getInputStream(entry));
            return new CustomProcess(doc);
        } catch (Exception e) {
            return null;
        }
    }

    public static void publish(CustomProcess process, File destFile,
                               TinyWebServer webServer)
        throws IOException {

        CustomProcessPublisher pub =
            new CustomProcessPublisher(destFile, webServer);
        pub.publish(process);
        pub.close();
    }


    ZipOutputStream zip;
    Writer out;

    TinyWebServer webServer;
    HTMLPreprocessor processor;
    HashMap customParams, parameters;


    protected CustomProcessPublisher(File destFile, TinyWebServer webServer)
        throws IOException
    {
        FileOutputStream fos = new FileOutputStream(destFile);
        zip = new ZipOutputStream(fos);
        out = new OutputStreamWriter(zip);

        this.webServer = webServer;
        parameters = new HashMap();
        customParams = new HashMap();
        processor = new HTMLPreprocessor(webServer, null, null, "",
                                         customParams, parameters);
    }

    protected synchronized void publish(CustomProcess process)
        throws IOException {
        writeXMLSettings(process);
        initProcess(process);
        runGenerationScript(process.getGeneratorScript());
    }

    protected void close() throws IOException {
        out.flush();
        zip.closeEntry();
        zip.close();
    }

    protected void initProcess(CustomProcess process) {
        String processName = process.getName();
        String versionNum = process.getVersion();
        String fullName, versionString;

        if (versionNum == null || versionNum.length() == 0)
            versionNum = versionString = "";
        else
            versionString = " (v" + versionNum + ")";
        fullName = processName + versionString;

        setParam("Process_ID",     process.getProcessID());
        setParam("Process_Name",   processName);
        setParam("Version_Num",    versionNum);
        setParam("Version_String", versionString);
        setParam("Full_Name",      fullName);

        String[] phaseList = new String[process.getRowCount()];
        Iterator i = process.getPhaseIterator();
        CustomProcess.CustomPhase phase;
        int phaseNum = 0;
        lastPhaseID = null;
        while (i.hasNext()) {
            phaseList[phaseNum] =
                initPhase((CustomProcess.CustomPhase) i.next(), phaseNum);
            phaseNum++;
        }
        parameters.put("Phase_List_ALL", phaseList);

        // parameters.put("USE_TO_DATE_DATA", "t");
    }

    private String lastPhaseID;
    protected String initPhase(CustomProcess.CustomPhase phase, int pos) {
        String id = CustomProcess.makeUltraSafe(phase.name);

        setParam(id, id);
        setParam(id + "_Name", phase.name);
        setParam(id + "_Long_Name", phase.longName);
        setParam(id + "_Type", phase.type);

        if (TemplateAutoData.isAppraisalPhaseType(phase.type)) {
            setParam(id + "_Is_Appraisal", "t");
            setParam(id + "_Is_Quality", "t");
        } else if (TemplateAutoData.isFailurePhaseType(phase.type)) {
            setParam(id + "_Is_Failure", "t");
            setParam(id + "_Is_Quality", "t");
        } else if (TemplateAutoData.isDevelopmentPhaseType(phase.type)) {
            setParam(id + "_Is_Development", "t");
        } else if (TemplateAutoData.isOverheadPhaseType(phase.type)) {
            setParam(id + "_Is_Overhead", "t");
        }

        String phasePos = "" + pos;
        while (phasePos.length() < 3) phasePos = "0" + phasePos;
        setParam(id + "_Pos", phasePos);

        String sizeMetric = (String) PHASE_SIZE_METRIC.get(phase.type);
        if (sizeMetric != null) setParam(id + "_Size_Metric", sizeMetric);

        if (lastPhaseID != null) {
            setParam(lastPhaseID + "_Next_Sibling", id);
            setParam(id + "_Prev_Sibling", lastPhaseID);
        }
        lastPhaseID = id;

        return id;
    }

    protected void setParam(String parameter, String value) {
        parameters.put(parameter, value);
    }

    protected void writeXMLSettings(CustomProcess process) throws IOException {
        startFile(SETTINGS_FILENAME);
        process.writeXMLSettings(out);
    }

    protected void startFile(String filename) throws IOException {
        out.flush();
        if (filename.startsWith("/")) filename = filename.substring(1);
        zip.putNextEntry(new ZipEntry(filename));
    }

    protected String getFile(String filename) throws IOException {
        return processContent(getRawFile(filename));
    }
    protected String getRawFile(String filename) throws IOException {
        byte[] rawContent = getRawFileBytes(filename);
        if (rawContent == null) return null;
        return new String(rawContent);
    }
    protected byte[] getRawFileBytes(String filename) throws IOException {
        return webServer.getRawRequest(filename);
    }
    protected String processContent(String content) throws IOException {
        if (content == null) return null;
        return processor.preprocess(content);
    }

    protected void runGenerationScript(String scriptName) throws IOException {
        Document script = null;
        try {
            script = XMLUtils.parse(getFile(scriptName));
        } catch (SAXException se) {
            System.err.print(se);
            se.printStackTrace();
            throw new IOException("Invalid XML file");
        }
        NodeList files = script.getElementsByTagName("file");
        String defaultInDir =
            script.getDocumentElement().getAttribute("inDirectory");
        String defaultOutDir =
            script.getDocumentElement().getAttribute("outDirectory");
        ProgressDialog progressDialog =
            new ProgressDialog((java.awt.Frame) null, "Saving",
                               "Saving Custom Process...");
        for (int i=0;   i < files.getLength();   i++)
            progressDialog.addTask(new FileGenerator((Element) files.item(i),
                                                     defaultInDir,
                                                     defaultOutDir));
        progressDialog.run();
    }

    private class FileGenerator implements Runnable {
        Element file;
        String defaultInDir, defaultOutDir;
        public FileGenerator(Element f, String inDir, String outDir) {
            file = f; defaultInDir = inDir; defaultOutDir = outDir;
        }
        public void run() {
            try {
                generateFile(file, defaultInDir, defaultOutDir);
            } catch (IOException ioe) { System.err.println(ioe); }
        }
    }

    private String maybeDefaultDir(String file, String dir) {
        if (file == null || file.startsWith("/")) return file;
        return dir + "/" + file;
    }

    protected void generateFile(Element file, String defaultInDir,
                                String defaultOutDir) throws IOException {
        String inputFile = file.getAttribute("in");
        String outputFile = file.getAttribute("out");
        if (!XMLUtils.hasValue(inputFile)) return;
        if (!XMLUtils.hasValue(outputFile)) outputFile = inputFile;
        inputFile = maybeDefaultDir(inputFile, defaultInDir);
        outputFile = maybeDefaultDir(outputFile, defaultOutDir);

        String encoding = file.getAttribute("encoding");

        if ("binary".equals(encoding)) {
            byte[] contents = getRawFileBytes(inputFile);
            if (contents == null) return;
            startFile(outputFile);
            zip.write(contents);
            return;
        }

        processor.setDefaultEchoEncoding(encoding);

        String contents = getRawFile(inputFile);
        if (contents == null) return;

        customParams.clear();
        NodeList params = file.getElementsByTagName("param");
        String name, val;
        if (params != null)
            for (int i=0;   i<params.getLength();   i++) {
                Element param = (Element) params.item(i);
                customParams.put(name = param.getAttribute("name"),
                                 val = param.getAttribute("value"));
                if (XMLUtils.hasValue(param.getAttribute("replace")))
                    contents = StringUtils.findAndReplace(contents, name, val);
            }
        contents = processContent(contents);
        contents = StringUtils.findAndReplace(contents, "[!--#", "<!--#");
        contents = StringUtils.findAndReplace(contents,   "--]",   "-->");

        startFile(outputFile);
        zip.write(contents.getBytes());
    }


    private static final Map PHASE_SIZE_METRIC = new HashMap();
    private static final String INSP = "Inspected ";
    private static final String REQ_PAGES = "Req Pages";
    private static final String TEXT_PAGES = "Text Pages";
    private static final String HLD_PAGES = "HLD Pages";
    private static final String DLD_LINES = "DLD Lines";
    private static final String SIZE_LOC = "New & Changed LOC";
    static {
        PHASE_SIZE_METRIC.put("REQ",      REQ_PAGES);
        PHASE_SIZE_METRIC.put("REQINSP",  INSP + REQ_PAGES);
        PHASE_SIZE_METRIC.put("STP",      TEXT_PAGES);
        PHASE_SIZE_METRIC.put("ITP",      TEXT_PAGES);
        PHASE_SIZE_METRIC.put("TD",       TEXT_PAGES);
        PHASE_SIZE_METRIC.put("DOC",      TEXT_PAGES);
        PHASE_SIZE_METRIC.put("HLD",      HLD_PAGES);
        PHASE_SIZE_METRIC.put("HLDRINSP", INSP + HLD_PAGES);
        PHASE_SIZE_METRIC.put("DLD",      DLD_LINES);
        PHASE_SIZE_METRIC.put("DLDR",     DLD_LINES);
        PHASE_SIZE_METRIC.put("DLDINSP",  INSP + DLD_LINES);
        PHASE_SIZE_METRIC.put("CODEINSP", INSP + SIZE_LOC);
    }

}
