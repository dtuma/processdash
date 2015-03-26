//Copyright (C) 2003 Tuma Solutions, LLC
//Process Dashboard - Data Automation Tool for high-maturity processes
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 3
//of the License, or (at your option) any later version.
//
//Additional permissions also apply; see the README-license.txt
//file in the project root directory for more information.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, see <http://www.gnu.org/licenses/>.
//
//The author(s) may be contacted at:
//    processdash@tuma-solutions.com
//    processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;


public class GenerateTranslator extends MatchingTask {


    private static final String TEMPLATE_LEX =
        "GenerateTranslatorTemplate.lex";
    private static final String SCRIPT_CHARSET = "US-ASCII";
    private static final String PACKAGE_PATTERN = "PACKAGE";
    private static final String CLASSNAME_PATTERN = "CLASSNAME";
    private static final String REGEXP_PATTERN = "\"REGEXP\"";
    private static final String KEY_PATTERN = "KEY";
    private static final String LINE_END = System.getProperty("line.separator");
    private static final String SPACE_PATTERN = "{SP}+";



    private String packageName;
    private String className;
    private boolean keepLexerFile = false;
    private File outputDir = null;
    private Properties dictionary;


    public GenerateTranslator() {}


    public void setClassname(String fullName) {
        int dotPos = fullName.lastIndexOf('.');
        if (dotPos == -1) {
            packageName = null;
            className = fullName;
        } else {
            packageName = fullName.substring(0, dotPos);
            className = fullName.substring(dotPos + 1);
        }
    }

    public void setKeeplexerfile(boolean keep) {
        keepLexerFile = keep;
    }

    public void setDir(File dir) {
        fileset.setDir(dir);
    }

    public void setDestdir(File destDir) {
        outputDir = destDir;
    }


    public void execute() throws BuildException {
        validate();

        DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
        String[] srcFilenames = ds.getIncludedFiles();
        if (srcFilenames.length == 0)
            throw new BuildException
                ("You must designate at least one input properties file.");

        File[] srcFiles = new File[srcFilenames.length];
        for (int j = 0; j < srcFiles.length; j++)
            srcFiles[j] = new File(ds.getBasedir(), srcFilenames[j]);


        try {
            openProperties(srcFiles);
        } catch (IOException e) {
            throw new BuildException("Could not read properties files.");
        }


        File lexerFile = getLexFileName();
        try {
            writeOutput(lexerFile);
        } catch (IOException ioe) {
            throw new BuildException("Cannot create file '"+lexerFile+"'.");
        }

        try {
            JLex.Main.main(new String[] { lexerFile.getAbsolutePath() });
        } catch (IOException ioe) {
            throw new BuildException
                ("Cannot create file '"+lexerFile+".java'");
        }

        if (!keepLexerFile)
            lexerFile.delete();
    }


    private void validate() throws BuildException {
        if (className == null)
            throw new BuildException("Class name must be specified.");

        if (outputDir == null)
            throw new BuildException("Uutput directory must be specified.");
    }


    private void openProperties(File[] propFiles) throws IOException {
        dictionary = new Properties();
        Properties p = new Properties();
        for (int i = propFiles.length;   i-- > 0;   ) {
            if (propFiles[i] != null) {
                p.load(new FileInputStream(propFiles[i]));
                dictionary.putAll(p);
                p.clear();
            }
        }
        resolveProperties();
    }

    private void resolveProperties() {
        LinkedList keys = new LinkedList(dictionary.keySet());
        Iterator i = keys.iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            if (key.indexOf("__") != -1 || key.endsWith("_"))
                dictionary.remove(key);
            else
                resolveProperty(key);
        }
    }

    private static final String VAR_START_PAT = "${";
    private static final String VAR_END_PAT = "}";
    public String resolveProperty(String key) {
        String s = (String) dictionary.get(key);
        while (true) {
            int beg = s.indexOf(VAR_START_PAT);
            if (beg == -1) return s;

            int end = s.indexOf(VAR_END_PAT, beg);
            if (end == -1) return s;

            String var = s.substring(beg+VAR_START_PAT.length(), end);
            String replacement = resolveProperty(var);
            s = s.substring(0, beg) + replacement +
                s.substring(end+VAR_END_PAT.length());
        }
    }

    private File getLexFileName() {
        File dir = outputDir;
        if (packageName != null) {
            String packagePath = packageName.replace('.', File.separatorChar);
            dir = new File(outputDir, packagePath);
        }
        return new File(dir, className);
    }


    private void writeOutput(File outFile) throws IOException {
        File packageDir = outFile.getParentFile();
        packageDir.mkdirs();
        if (!packageDir.isDirectory())
            throw new BuildException
                ("Cannot create directory '"+packageDir+"'.");

        Writer out = new OutputStreamWriter
            (new FileOutputStream(outFile), SCRIPT_CHARSET);


        BufferedReader in =
            new BufferedReader(
                new InputStreamReader(
                    GenerateTranslator.class.getResourceAsStream(TEMPLATE_LEX),
                    SCRIPT_CHARSET));

        String line;
        while ((line = in.readLine()) != null) {
            if (line.indexOf(REGEXP_PATTERN) != -1
                && line.indexOf(KEY_PATTERN) != -1)
                writeRules(out, line);
            else {
                line = replacePatterns(line);
                out.write(line);
                out.write(LINE_END);
            }
        }
        out.close();
    }

    private String replacePatterns(String line) {
        if (line.indexOf(PACKAGE_PATTERN) != -1) {
            if (packageName == null)
                return "";
            else
                return findAndReplace(line, PACKAGE_PATTERN, packageName);
        }
        line = findAndReplace(line, CLASSNAME_PATTERN, className);
        return line;
    }


    private void writeRules(Writer out, String template)
        throws IOException {
        int pos = template.indexOf(REGEXP_PATTERN);
        String partA = template.substring(0, pos);
        template = template.substring(pos + REGEXP_PATTERN.length());
        pos = template.indexOf(KEY_PATTERN);
        String partB = template.substring(0, pos);
        String partC = template.substring(pos + KEY_PATTERN.length());

        Iterator i = dictionary.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            String key = (String) e.getKey();
            if (key.endsWith("_FMT") || key.indexOf("__") != -1)
                continue;
            String pattern = makePattern(key);

            out.write(partA);
            out.write(pattern);
            out.write(partB);
            out.write(key);
            out.write(partC);
            out.write(LINE_END);
        }
    }

    private String makePattern(String dictTerm) {
        StringBuffer result = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(dictTerm, "_ ");
        result.append('"').append(tok.nextToken()).append('"');
        while (tok.hasMoreTokens())
            result.append(SPACE_PATTERN)
                .append('"')
                .append(tok.nextToken())
                .append('"');
        return result.toString();
    }

    private static final String findAndReplace(
        String text,
        String find,
        String replace)
    {
        // handle degenerate case: if no replacements need to be made,
        // return the original text unchanged.
        int replaceStart = text.indexOf(find);
        if (replaceStart == -1)
            return text;

        int findLength = find.length();
        StringBuffer result = new StringBuffer();

        while (replaceStart != -1) {
            result.append(text.substring(0, replaceStart)).append(replace);
            text = text.substring(replaceStart + findLength);
            replaceStart = text.indexOf(find);
        }

        result.append(text);
        return result.toString();
    }
}
