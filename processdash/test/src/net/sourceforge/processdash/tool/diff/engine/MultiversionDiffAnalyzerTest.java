// Copyright (C) 2011 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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

package net.sourceforge.processdash.tool.diff.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.processdash.tool.diff.CFilter;
import net.sourceforge.processdash.tool.diff.LanguageFilter;
import net.sourceforge.processdash.util.StringUtils;

import junit.framework.TestCase;

public class MultiversionDiffAnalyzerTest extends TestCase {

    public void testDeletedLine() throws Exception {
        MockFile del = new MockFile("deletion", "a|b|c", "a|c");
        DiffResult r = analyzeSingleFile(del);
        assertLocCounts(r.getLocCounts(), 3, 1, 0, 0, 2);
        assertEquals("a|-b|c|", getRedlinesAsText(r.getRedlines()));

        del = new MockFile("deletion", "a|b|c", "b|c");
        r = analyzeSingleFile(del);
        assertLocCounts(r.getLocCounts(), 3, 1, 0, 0, 2);
        assertEquals("-a|b|c|", getRedlinesAsText(r.getRedlines()));

        del = new MockFile("deletion", "a|b|c", "a|b");
        r = analyzeSingleFile(del);
        assertLocCounts(r.getLocCounts(), 3, 1, 0, 0, 2);
        assertEquals("a|b|-c|", getRedlinesAsText(r.getRedlines()));

        del = new MockFile("deletion", "a|b|//c", "a|b");
        r = analyzeSingleFile(del);
        assertLocCounts(r.getLocCounts(), 2, 0, 0, 0, 2);
        assertEquals("a|b|-{//c}|", getRedlinesAsText(r.getRedlines()));
    }

    public void testAddedLine() throws Exception {
        MockFile add = new MockFile("addition", "a|c", "a|b|c");
        DiffResult r = analyzeSingleFile(add);
        assertLocCounts(r.getLocCounts(), 2, 0, 0, 1, 3);
        assertEquals("a|+b|c|", getRedlinesAsText(r.getRedlines()));

        add = new MockFile("addition", "b|c", "a|b|c");
        r = analyzeSingleFile(add);
        assertLocCounts(r.getLocCounts(), 2, 0, 0, 1, 3);
        assertEquals("+a|b|c|", getRedlinesAsText(r.getRedlines()));

        add = new MockFile("addition", "a|b", "a|b|c");
        r = analyzeSingleFile(add);
        assertLocCounts(r.getLocCounts(), 2, 0, 0, 1, 3);
        assertEquals("a|b|+c|", getRedlinesAsText(r.getRedlines()));

        add = new MockFile("addition", "a|b", "a|b|//c");
        r = analyzeSingleFile(add);
        assertLocCounts(r.getLocCounts(), 2, 0, 0, 0, 2);
        assertEquals("a|b|+{//c}|", getRedlinesAsText(r.getRedlines()));
    }

    public void testModifiedLine() throws Exception {
        MockFile mod = new MockFile("mod", "a|b|c", "a|d|c");
        DiffResult r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 3, 0, 1, 0, 3);
        assertEquals("a|-b|+d|c|", getRedlinesAsText(r.getRedlines()));

        mod = new MockFile("mod", "a|b|c", "d|b|c");
        r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 3, 0, 1, 0, 3);
        assertEquals("-a|+d|b|c|", getRedlinesAsText(r.getRedlines()));

        mod = new MockFile("mod", "a|b|c", "a|b|d");
        r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 3, 0, 1, 0, 3);
        assertEquals("a|b|-c|+d|", getRedlinesAsText(r.getRedlines()));

        mod = new MockFile("mod", "a|b|//c", "a|b|//d");
        r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 2, 0, 0, 0, 2);
        assertEquals("a|b|-{//c}|+{//d}|", getRedlinesAsText(r.getRedlines()));

        mod = new MockFile("mod", "a|b|e//c", "a|b|e//d");
        r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 3, 0, 0, 0, 3);
        assertEquals("a|b|-e{//c}|+e{//d}|", getRedlinesAsText(r.getRedlines()));
    }

    public void testWhitespaceOnlyChange() throws Exception {
        MockFile mod = new MockFile("mod", "a|b|c", "a| b|c");
        DiffResult r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 3, 0, 0, 0, 3);
        assertEquals("a| b|c|", getRedlinesAsText(r.getRedlines()));
    }

    public void testCommentOnlyChange() throws Exception {
        MockFile mod = new MockFile("mod", "a|b|c//d|e", "a|c//f|g|e");
        DiffResult r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 4, 1, 0, 1, 4);
        assertEquals("a|-b|-c{//d}|+c{//f}|+g|e|", getRedlinesAsText(r
                .getRedlines()));

        mod = new MockFile("mod", "a|b|c//d|e", "a|h|c//f|g|e");
        r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 4, 0, 1, 1, 5);
        assertEquals("a|-b|-c{//d}|+h|+c{//f}|+g|e|", getRedlinesAsText(r
                .getRedlines()));
    }

    public void testInterleavingChanges() throws Exception {
        MockFile mod = new MockFile("mod", "a|b|c", "a|b|c|d", "a|c|d", "e|c|d");
        DiffResult r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 2, 0, 1, 1, 3);
        assertEquals("-a|+e|c|+d|", getRedlinesAsText(r.getRedlines()));

        mod = new MockFile("mod", "a|b|c", "a|d|c", "a|e|c");
        r = analyzeSingleFile(mod);
        assertLocCounts(r.getLocCounts(), 4, 1, 0, 0, 3);
        assertEquals("a|-b|e|c|", getRedlinesAsText(r.getRedlines()));
    }


    private void assertLocCounts(int[] counts, int... expected) {
        for (AccountingType t : AccountingType.values())
            assertEquals(expected[t.ordinal()], counts[t.ordinal()]);
    }

    private DiffResult analyzeSingleFile(FileToAnalyze f) throws IOException {
        FileTraits traits = FileTraits.examineFile(f, CHARSET);
        DiffAnalysisRequest r = new DiffAnalysisRequest(f, FILTER, "", CHARSET,
                traits);
        return new MultiversionDiffAnalyzer().analyze(r);
    }

    private String getRedlinesAsText(List<DiffFragment> fragments) {
        StringBuilder result = new StringBuilder();
        for (DiffFragment f : fragments) {
            String flag = "";
            if (f.type == AccountingType.Deleted)
                flag = "-";
            else if (f.type == AccountingType.Added)
                flag = "+";
            String block = f.text;
            block = StringUtils.findAndReplace(block, "\n", "|" + flag);
            if (flag.length() == 1)
                block = flag + block.substring(0, block.length() - 1);
            block = block.replace(LanguageFilter.COMMENT_START, '{');
            block = block.replace(LanguageFilter.COMMENT_END, '}');
            result.append(block);
        }
        return result.toString();
    }

    /*
     * private class MockFileSet implements FileAnalysisSet {
     * 
     * private List<FileToAnalyze> files;
     * 
     * public MockFileSet(FileToAnalyze... files) { this.files =
     * Arrays.asList(files); } public List<? extends FileToAnalyze>
     * getFilesToAnalyze() { return files; } }
     */

    private class MockFile implements FileToAnalyze {
        String filename;

        Object[] versions;

        public MockFile(String filename, String... versions) {
            this.filename = filename;

            for (int i = 0; i < versions.length; i++)
                versions[i] = versions[i].replace('|', '\n');
            this.versions = versions;
        }

        public String getFilename() {
            return filename;
        }

        public List<Object> getVersions() {
            return Arrays.asList(versions);
        }

        public InputStream getContents(Object version) throws IOException {
            if (version instanceof String) {
                return new ByteArrayInputStream(((String) version)
                        .getBytes(CHARSET));
            } else {
                return null;
            }
        }

    }

    private static final CFilter FILTER = new CFilter();

    private static final Charset CHARSET = Charset.defaultCharset();

}
