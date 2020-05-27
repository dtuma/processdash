// Copyright (C) 2002-2020 Tuma Solutions, LLC
// Team Functionality Add-ons for the Process Dashboard
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.team.mcf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sourceforge.processdash.net.http.ContentSource;
import net.sourceforge.processdash.net.http.HTMLPreprocessor;
import net.sourceforge.processdash.process.PhaseUtil;
import net.sourceforge.processdash.templates.DashPackage;
import net.sourceforge.processdash.ui.lib.ProgressDialog;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.VersionUtils;
import net.sourceforge.processdash.util.XMLUtils;


public class CustomProcessPublisher {

    private static final String EXT_FILE_PREFIX = "extfile:";

    public static void publish(CustomProcess process, File destFile,
            ContentSource contentSource) throws IOException {

        publish(process, destFile, contentSource, null, false);
    }

    public static void publish(CustomProcess process, OutputStream output,
            ContentSource contentSource) throws IOException {

        publish(process, output, contentSource, null, false);
    }

    public static void publish(CustomProcess process, File destFile,
            ContentSource contentSource, URL extBase, boolean useLightGenerator)
            throws IOException {

        publish(process, new FileOutputStream(destFile), contentSource,
            extBase, useLightGenerator);
    }

    public static void publish(CustomProcess process, OutputStream output,
            ContentSource contentSource, URL extBase, boolean useLightGenerator)
            throws IOException {

        CustomProcessPublisher pub = new CustomProcessPublisher(contentSource,
                extBase);
        pub.setHeadless(true);
        pub.setUseLightGenerator(useLightGenerator);
        pub.publish(process, output);
        pub.close();
    }

    JarOutputStream zip;

    Writer out;

    ContentSource contentSource;

    Map parameters;

    Map<String, FileGenerator> fileGenerators;

    Date timestamp;

    URL extBase;

    boolean extFileAllowed;

    boolean headless;

    boolean useLightGenerator;

    protected CustomProcessPublisher(ContentSource contentSource, URL extBase)
            throws IOException {
        this.contentSource = contentSource;
        this.extBase = extBase;
        this.extFileAllowed = (extBase != null);
        this.parameters = new HashMap();
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public boolean isUseLightGenerator() {
        return useLightGenerator;
    }

    public void setUseLightGenerator(boolean useLightGenerator) {
        this.useLightGenerator = useLightGenerator;
    }

    public boolean isExtFileAllowed() {
        return extFileAllowed;
    }

    public void setExtFileAllowed(boolean extFileAllowed) {
        this.extFileAllowed = extFileAllowed;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    private boolean isInMemoryMode() {
        return zip == null;
    }

    public byte[] getGeneratedFileContents(String name) throws IOException {
        FileGenerator gen = fileGenerators.get(name);
        if (gen == null)
            return null;
        else
            return generateFile(gen, "utf-8");
    }

    protected synchronized void publish(CustomProcess process,
            OutputStream output) throws IOException {
        initProcess(process);

        Document script = loadScript(process.getGeneratorScript());

        if (output == null) {
            fileGenerators = new HashMap();
        } else {
            openStreams(process, script, output);
            writeXMLSettings(process);
        }

        runGenerationScript(script);
    }

    protected Document loadScript(String scriptName) throws IOException {
        try {
            if (useLightGenerator)
                scriptName = StringUtils.findAndReplace(scriptName, ".xml",
                    "-light.xml");
            String script = processContent(getRawFile(scriptName), null, "xml");
            return XMLUtils.parse(script);
        } catch (SAXException se) {
            System.err.print(se);
            se.printStackTrace();
            throw new IOException("Invalid XML file");
        }
    }

    public void loadInfoFromManifest(Manifest manifest) {
        String version = manifest.getMainAttributes().getValue(
            DashPackage.VERSION_ATTRIBUTE);
        loadTimestampFromVersion(version);
    }

    public void loadTimestampFromVersion(String version) {
        if (version != null) {
            try {
                int dotPos = version.lastIndexOf('.');
                String timeStr = version.substring(dotPos + 1);
                timestamp = TIMESTAMP_FORMAT.parse(timeStr);
            } catch (Exception e) {
            }
        }
    }

    protected void openStreams(CustomProcess process, Document script,
            OutputStream output) throws IOException {

        String scriptVers = script.getDocumentElement().getAttribute("version");
        String scriptReqt = script.getDocumentElement().getAttribute(
                "requiresDashboard");
        String scriptStartingJar = script.getDocumentElement().getAttribute(
                "startingJar");

        if (process.isPspCompatible() == false)
            scriptReqt = maxVersion(scriptReqt, MIN_NON_PSP_VERSION);

        Manifest mf = new Manifest();
        JarInputStream startingJarIn = null;
        if (scriptStartingJar != null) {
            startingJarIn = openStartingJar(scriptStartingJar);
            if (startingJarIn != null)
                mf = startingJarIn.getManifest();
        }

        Attributes attrs = mf.getMainAttributes();
        attrs.putValue("Manifest-Version", "1.0");
        String packageName = (String) parameters.get("Dash_Package_Name");
        if (packageName == null)
            packageName = (String) parameters.get("Full_Name");
        attrs.putValue(DashPackage.NAME_ATTRIBUTE, packageName);
        attrs.putValue(DashPackage.ID_ATTRIBUTE, process.getProcessID());
        if (timestamp == null)
            timestamp = new Date();
        attrs.putValue(DashPackage.VERSION_ATTRIBUTE, scriptVers + "."
                + TIMESTAMP_FORMAT.format(timestamp));
        if (scriptReqt != null)
            attrs.putValue(DashPackage.REQUIRE_ATTRIBUTE, scriptReqt);

        zip = new JarOutputStream(output, mf);
        out = new OutputStreamWriter(zip);

        if (startingJarIn != null)
            copyFilesFromStartingJar(startingJarIn);
    }
    private String maxVersion(String versionA, String versionB) {
        if (versionA == null) return versionB;
        if (versionB == null) return versionA;
        return (VersionUtils.compareVersions(versionA, versionB) > 0 //
                ? versionA : versionB);
    }
    private static final String MIN_NON_PSP_VERSION = "2.5.4.1b";

    protected void close() throws IOException {
        out.flush();
        zip.closeEntry();
        zip.close();
    }

    private JarInputStream openStartingJar(String scriptStartingJar)
            throws IOException {
        if (scriptStartingJar == null || scriptStartingJar.length() == 0)
            return null;
        byte[] contents = getRawFileBytes(scriptStartingJar);
        if (contents == null)
            return null;

        ByteArrayInputStream bytesIn = new ByteArrayInputStream(contents);
        return new JarInputStream(bytesIn);
    }

    private void copyFilesFromStartingJar(JarInputStream startingJarIn)
            throws IOException {
        ZipEntry entry;
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((entry = startingJarIn.getNextEntry()) != null) {
            ZipEntry outEntry = cloneZipEntry(entry);
            zip.putNextEntry(outEntry);
            while ((bytesRead = startingJarIn.read(buffer)) != -1)
                zip.write(buffer, 0, bytesRead);
            zip.closeEntry();
        }
    }

    private ZipEntry cloneZipEntry(ZipEntry entry) {
        ZipEntry result = new ZipEntry(entry.getName());

        if (entry.getComment() != null)
            result.setComment(entry.getComment());
        if (entry.getExtra() != null)
            result.setExtra(entry.getExtra());
        if (entry.getTime() != -1)
            result.setTime(entry.getTime());

        return result;
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
        timestamp = process.processTimestamp;

        setParam("Process_ID", process.getProcessID());
        setParam("Process_Name", processName);
        setParam("Version_Num", versionNum);
        setParam("Version_String", versionString);
        setParam("Full_Name", fullName);

        if (process.isPspCompatible())
            setParam("PSP_Compatible", "t");

        for (Iterator iter = process.getItemTypes().iterator(); iter.hasNext();) {
            String type = (String) iter.next();
            handleItemList(process, type);
        }

        if (!parameters.containsKey(PROBE_PARAM))
            setParam(PROBE_PARAM, probePhase);

        setParam("Is_Extfile_Allowed", extFileAllowed ? "t" : "");
    }

    private void handleItemList(CustomProcess process, String itemType) {
        List processItems = process.getItemList(itemType);
        String[] itemList = new String[processItems.size()];
        String itemPrefix = CustomProcess.bouncyCapsToUnderlines(itemType);

        Iterator i = processItems.iterator();
        int itemNum = 0;
        lastItemID = null;
        while (i.hasNext()) {
            CustomProcess.Item item = (CustomProcess.Item) i.next();
            // String itemID = itemType + itemNum;
            String itemID = getItemID(itemPrefix, itemNum);
            itemList[itemNum] = itemID;

            if (CustomProcess.PHASE_ITEM.equals(itemType))
                initPhase(item, itemID);
            else if (CustomProcess.PARAM_ITEM.equals(itemType))
                initParam(item);

            setupItem(item, itemID, itemNum);

            itemNum++;
            lastItemID = itemID;
        }
        parameters.put(itemPrefix + "_List_ALL", itemList);
    }

    private String lastItemID;

    private String getItemID(String itemPrefix, int pos) {
        String phasePos = "" + pos;
        while (phasePos.length() < 3)
            phasePos = "0" + phasePos;
        String id = itemPrefix + phasePos;
        setParam(id + "_Pos", phasePos);
        return id;
    }

    private void setupItem(CustomProcess.Item phase, String id, int pos) {
        Iterator iter = phase.getAttributes().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry) iter.next();
            String attrName = CustomProcess.bouncyCapsToUnderlines((String) e
                    .getKey());
            String attrValue = (String) e.getValue();

            if (attrName.startsWith("Is_")) {
                // normalize boolean attributes
                if (attrValue != null && attrValue.length() > 0
                        && "tTyY".indexOf(attrValue.charAt(0)) != -1)
                    attrValue = "t";
                else
                    attrValue = "";
            }

            setParam(id + "_" + attrName, attrValue);

            if (attrName.endsWith("Filename") || attrName.endsWith("File_Name")) {
                enhanceFilenameAttribute(id + "_" + attrName, attrValue);
            }
        }

        if (lastItemID != null) {
            setParam(lastItemID + "_Next_Sibling", id);
            setParam(id + "_Prev_Sibling", lastItemID);
        }
    }

    private void enhanceFilenameAttribute(String attrName, String filename) {
        if (!extFileAllowed) {
            // we have a convention that when a custom process has "filename"
            // parameters, those refer to external files outside the custom
            // process XML file.  If we have no extBase, it means we cannot
            // load external files (most likely because we are running inside a
            // servlet process).  To avoid problems, we choose to silently
            // ignore those custom process parameters.  Remove them from the
            // parameter map to make it look like they have not been set.
            parameters.remove(attrName);
            return;
        }

        String directory = "";
        String baseName = filename;

        Matcher m = FILENAME_PATTERN.matcher(filename);
        if (m.matches()) {
            directory = m.group(1);
            baseName = m.group(2);
        }

        setParam(attrName + "_Directory", directory);
        setParam(attrName + "_Basename", baseName);
    }

    private static Pattern FILENAME_PATTERN = Pattern
            .compile("(.*[/\\\\]|)([^/\\\\]+)");

    protected void initPhase(CustomProcess.Item phase, String id) {
        String phaseName = phase.getAttr(CustomProcess.NAME);
        String phaseID = CustomProcess.makeUltraSafe(phaseName);
        setParam(id + "_ID", phaseID);

        if (phaseName.equalsIgnoreCase("PROBE"))
            registerPossibleProbePhase(phaseName, ProbePriority.Probe);
        else if (phaseName.regionMatches(true, 0, "PLAN", 0, 4))
            registerPossibleProbePhase(phaseName, ProbePriority.Plan);
        else
            registerPossibleProbePhase(phaseName, ProbePriority.FirstPhase);

        String phaseType = phase.getAttr(CustomProcess.TYPE);
        if (phaseType != null && phaseType.trim().length() != 0)
            phaseType = phaseType.trim().toUpperCase();
        else
            phaseType = "DEVELOP";

        if (PhaseUtil.isAppraisalPhaseType(phaseType)) {
            setParam(id + "_Is_Appraisal", "t");
            setParam(id + "_Is_Quality", "t");
            if (phaseType.endsWith("INSP") && !"Reqts Review".equals(phaseName)
                    && !"HLD Review".equals(phaseName))
                setParam(id + "_Is_Inspection", "t");
            else if (!phaseType.equals("APPRAISAL"))
                setParam(id + "_Is_Review", "t");
        } else if (PhaseUtil.isFailurePhaseType(phaseType)) {
            setParam(id + "_Is_Failure", "t");
            setParam(id + "_Is_Quality", "t");
        } else if (PhaseUtil.isDevelopmentPhaseType(phaseType)) {
            setParam(id + "_Is_Development", "t");
        } else if (PhaseUtil.isOverheadPhaseType(phaseType)) {
            registerPossibleProbePhase(phaseName, ProbePriority.Overhead);
            setParam(id + "_Is_Overhead", "t");
        }
        if ("plan".equalsIgnoreCase(phaseType)
                || !PhaseUtil.isOverheadPhaseType(phaseType)) {
            setParam(id + "_Is_Defect_Injection", "t");
            setParam(id + "_Is_Defect_Removal", "t");
        }
        if ("at".equalsIgnoreCase(phaseType)
                || "pl".equalsIgnoreCase(phaseType))
            setParam(id + "_Is_After_Development", "t");

        if (PSP_PHASE_NAMES.contains(phaseName.toLowerCase()))
            setParam(id + "_Is_PSP", "t");
    }
    private static Set PSP_PHASE_NAMES = Collections.unmodifiableSet(
            new HashSet(Arrays.asList(new String[] { "planning", "design",
                    "design review", "code", "code review", "compile", "test",
                    "postmortem" })));

    private static final String PROBE_PARAM = "Probe_Maps_To_Phase";
    private enum ProbePriority { Probe, Plan, Overhead, FirstPhase, Default }
    private String probePhase = "Planning";
    private ProbePriority probePriority = ProbePriority.Default;
    private void registerPossibleProbePhase(String phaseName, ProbePriority p) {
        if (p.compareTo(probePriority) < 0) {
            probePhase = phaseName;
            probePriority = p;
        }
    }

    private void initParam(CustomProcess.Item item) {
        String name = item.getAttr(CustomProcess.NAME);
        name = CustomProcess.bouncyCapsToUnderlines(name);

        String value = item.getAttr(CustomProcess.VALUE);
        if (value == null)
            value = "t";

        setParam(name, value);

        if (name.endsWith("Filename") || name.endsWith("File_Name")) {
            enhanceFilenameAttribute(name, value);
        }
    }

    protected void setParam(String parameter, String value) {
        parameters.put(parameter, value);
    }

    protected void writeXMLSettings(CustomProcess process) throws IOException {
        startFile(CustomProcess.SETTINGS_FILENAME);
        process.writeXMLSettings(out);
    }

    protected void startFile(String filename) throws IOException {
        out.flush();
        if (filename.startsWith("/"))
            filename = filename.substring(1);
        zip.putNextEntry(new ZipEntry(filename));
    }

    protected String getRawFile(String filename) throws IOException {
        byte[] rawContent = getRawFileBytes(filename);
        if (rawContent == null)
            return null;
        return new String(rawContent, "utf-8");
    }

    protected byte[] getRawFileBytes(String filename) throws IOException {
        if (filename != null && filename.startsWith(EXT_FILE_PREFIX))
            return getRawBytesFromExternalFile(filename
                    .substring(EXT_FILE_PREFIX.length()));
        else
            return contentSource.getContent("/", filename, true);
    }

    private byte[] getRawBytesFromExternalFile(String filename)
            throws IOException {
        if (extBase == null)
            return null;
        URL extFile = new URL(extBase, filename);
        URLConnection conn = extFile.openConnection();
        return FileUtils.slurpContents(conn.getInputStream(), true);
    }

    private String processContent(String content, Map fileSpecificParams,
            String echoEncoding) throws IOException {
        if (content == null)
            return null;

        if (fileSpecificParams == null)
            fileSpecificParams = Collections.EMPTY_MAP;
        Map oneRunParams = new HashMap(parameters);
        HTMLPreprocessor processor = new HTMLPreprocessor(contentSource, null,
                null, "", fileSpecificParams, oneRunParams);
        processor.setDefaultEchoEncoding(echoEncoding);

        return processor.preprocess(content);
    }

    protected void runGenerationScript(Document script) throws IOException {
        NodeList files = script.getElementsByTagName("file");
        String defaultInDir = script.getDocumentElement().getAttribute(
                "inDirectory");
        String defaultOutDir = script.getDocumentElement().getAttribute(
                "outDirectory");
        ProgressDialog progressDialog = null;
        if (!headless)
            progressDialog = new ProgressDialog((java.awt.Frame) null,
                    "Saving", "Saving Custom Process...");
        for (int i = 0; i < files.getLength(); i++) {
            FileGenerator task = new FileGenerator((Element) files.item(i),
                    defaultInDir, defaultOutDir);
            if (headless)
                task.run();
            else
                progressDialog.addTask(task);
        }
        if (!headless)
            progressDialog.run();
    }

    private class FileGenerator implements Runnable {
        Element file;

        String inputFile, outputFile, encoding;

        public FileGenerator(Element f, String inDir, String outDir) {
            file = f;

            inputFile = file.getAttribute("in");
            outputFile = file.getAttribute("out");
            if (!XMLUtils.hasValue(inputFile))
                return;
            if (!XMLUtils.hasValue(outputFile))
                outputFile = inputFile;
            inputFile = maybeDefaultDir(inputFile, inDir);
            outputFile = maybeDefaultDir(outputFile, outDir);

            if (isInMemoryMode() && inputFile.startsWith(EXT_FILE_PREFIX)) {
                String nameWithinJar = outputFile;
                if (nameWithinJar.startsWith("/"))
                    nameWithinJar = nameWithinJar.substring(1);
                inputFile = EXT_FILE_PREFIX + nameWithinJar;
            }

            encoding = file.getAttribute("encoding");
        }

        public void run() {
            try {
                boolean isProps = outputFile.endsWith("#properties");
                if (isInMemoryMode() && !isProps) {
                    fileGenerators.put(outputFile, this);
                    return;
                }

                String charset = (isProps ? "8859_1" : "utf-8");
                byte[] contents = generateFile(this, charset);

                if (isProps) {
                    Properties p = new Properties();
                    p.load(new ByteArrayInputStream(contents));
                    parameters.putAll(p);

                } else if (!isInMemoryMode()) {
                    startFile(outputFile);
                    zip.write(contents);
                }

            } catch (FileNotFoundException fnfe) {
                System.err.println("Warning: could not find file "
                        + fnfe.getMessage() + " - skipping");
            } catch (IOException ioe) {
                System.err
                        .println("While processing " + file.getAttribute("in")
                                + ", caught exception " + ioe);
            }
        }
    }

    private String maybeDefaultDir(String file, String dir) {
        if (file == null || file.startsWith("/")
                || file.startsWith(EXT_FILE_PREFIX))
            return file;
        return dir + "/" + file;
    }

    protected byte[] generateFile(FileGenerator fileGen, String charset)
            throws IOException {
        String inputFile = fileGen.inputFile;
        String encoding = fileGen.encoding;

        if ("binary".equals(encoding)) {
            byte[] contents = getRawFileBytes(inputFile);
            if (contents == null)
                throw new FileNotFoundException(inputFile);
            else
                return contents;
        }

        String contents = getRawFile(inputFile);
        if (contents == null)
            throw new FileNotFoundException(inputFile);

        Map customParams = new HashMap();
        NodeList params = fileGen.file.getElementsByTagName("param");
        String name, val;
        if (params != null) {
            for (int i = 0; i < params.getLength(); i++) {
                Element param = (Element) params.item(i);
                customParams.put(name = param.getAttribute("name"), val = param
                        .getAttribute("value"));
                if (XMLUtils.hasValue(param.getAttribute("replace")))
                    contents = StringUtils.findAndReplace(contents, name, val);
            }
        }

        contents = processContent(contents, customParams, encoding);
        contents = StringUtils.findAndReplace(contents, "[!--#", "<!--#");
        contents = StringUtils.findAndReplace(contents, "--]", "-->");

        return contents.getBytes(charset);
    }

    private static final SimpleDateFormat TIMESTAMP_FORMAT =
        new SimpleDateFormat("yyyyMMddHHmmss");
}
