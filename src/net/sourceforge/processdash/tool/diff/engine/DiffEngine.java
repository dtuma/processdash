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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;

import net.sourceforge.processdash.tool.diff.BinaryFilter;
import net.sourceforge.processdash.tool.diff.LanguageFilter;
import net.sourceforge.processdash.util.Disposable;

public class DiffEngine {

    private FileOptionsProvider fileOptionsProvider;

    private LanguageFilterSelector languageFilterSelector;

    private CharsetSelector charsetSelector;

    private DiffAnalyzer diffAnalyzer;

    protected List<FileToAnalyze> filesToAnalyze;

    private boolean skipIdenticalFiles;

    private volatile boolean aborted;

    protected EventListenerList listeners;

    protected Logger logger = Logger.getLogger(DiffEngine.class.getName());


    public DiffEngine() {
        this.fileOptionsProvider = DefaultFileOptionsProvider.DEFAULT;
        this.charsetSelector = DefaultCharsetSelector.INSTANCE;
        this.diffAnalyzer = MultiversionDiffAnalyzer.INSTANCE;
        this.filesToAnalyze = new ArrayList<FileToAnalyze>();
        this.skipIdenticalFiles = true;
        this.aborted = false;
        this.listeners = new EventListenerList();
    }

    public FileOptionsProvider getFileOptionsProvider() {
        return fileOptionsProvider;
    }

    public void setFileOptionsProvider(FileOptionsProvider fop) {
        this.fileOptionsProvider = fop;
    }

    public void setFileOptions(String options) {
        setFileOptionsProvider(new DefaultFileOptionsProvider(options));
    }

    public LanguageFilterSelector getLanguageFilterSelector() {
        return languageFilterSelector;
    }

    public void setLanguageFilterSelector(LanguageFilterSelector lfs) {
        this.languageFilterSelector = lfs;
    }

    public void setLanguageFilters(List lf) {
        this.languageFilterSelector = new DefaultLanguageFilterSelector(lf);
    }

    public CharsetSelector getCharsetSelector() {
        return charsetSelector;
    }

    public void setCharsetSelector(CharsetSelector charsetSelector) {
        this.charsetSelector = charsetSelector;
    }

    public DiffAnalyzer getDiffAnalyzer() {
        return diffAnalyzer;
    }

    public void setDiffAnalyzer(DiffAnalyzer da) {
        this.diffAnalyzer = da;
    }

    public void addFilesToAnalyze(List<? extends FileToAnalyze> files) {
        this.filesToAnalyze.addAll(files);
    }

    public void addFilesToAnalyze(FileAnalysisSet files) throws IOException {
        this.filesToAnalyze.addAll(files.getFilesToAnalyze());
    }

    public List<FileToAnalyze> getFilesToAnalyze() {
        return filesToAnalyze;
    }

    public boolean isSkipIdenticalFiles() {
        return skipIdenticalFiles;
    }

    public void setSkipIdenticalFiles(boolean skipIdenticalFiles) {
        this.skipIdenticalFiles = skipIdenticalFiles;
    }

    public void abort() {
        this.aborted = true;
    }

    public boolean isAborted() {
        return aborted;
    }

    public void addDiffListener(DiffListener l) {
        listeners.add(DiffListener.class, l);
    }

    public void removeDiffListener(DiffListener l) {
        listeners.remove(DiffListener.class, l);
    }


    public void run() {
        checkPreconditions();
        if (!aborted)
            fireAnalysisStarting();
        for (FileToAnalyze file : filesToAnalyze) {
            if (!aborted)
                analyzeFileAndFireEvents(file);
        }
        if (!aborted)
            fireAnalysisFinished();
    }

    public void checkPreconditions() {
        ensureNotNull(fileOptionsProvider, "FileOptionsProvider");
        ensureNotNull(languageFilterSelector, "LanguageFilterSelector");
        ensureNotNull(charsetSelector, "CharsetSelector");
        ensureNotNull(diffAnalyzer, "DiffAnalyzer");
    }

    protected void ensureNotNull(Object field, String fieldName) {
        if (field == null)
            throw new NullPointerException(fieldName + " must be set");
    }

    protected void analyzeFileAndFireEvents(FileToAnalyze file) {
        fireFileAnalysisStarting(file);
        DiffResult diffResult = null;
        Exception e = null;
        try {
            diffResult = analyzeFile(file);
        } catch (IOException ioe) {
            e = ioe;
        }
        fireFileAnalysisFinished(file, diffResult, e);
        maybeDispose(file);
        maybeDispose(diffResult);
    }

    protected void fireAnalysisStarting() {
        DiffEvent e = new DiffEvent(this, null, null, null);
        for (DiffListener l : listeners.getListeners(DiffListener.class)) {
            try {
                l.analysisStarting(e);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "DiffListener threw exception", ioe);
            }
        }
    }

    protected void fireFileAnalysisStarting(FileToAnalyze file) {
        DiffEvent e = new DiffEvent(this, file, null, null);
        for (DiffListener l : listeners.getListeners(DiffListener.class)) {
            try {
                l.fileAnalysisStarting(e);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "DiffListener threw exception", ioe);
            }
        }
    }

    protected void fireFileAnalysisFinished(FileToAnalyze file,
            DiffResult result, Exception ex) {
        DiffEvent e = new DiffEvent(this, file, result, ex);
        for (DiffListener l : listeners.getListeners(DiffListener.class)) {
            try {
                l.fileAnalysisFinished(e);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "DiffListener threw exception", ioe);
            }
        }
    }

    protected void fireAnalysisFinished() {
        DiffEvent e = new DiffEvent(this, null, null, null);
        for (DiffListener l : listeners.getListeners(DiffListener.class)) {
            try{
                l.analysisFinished(e);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "DiffListener threw exception", ioe);
            }
        }
    }

    protected void maybeDispose(FileToAnalyze file) {
        List versions = file.getVersions();
        if (versions != null)
            for (Object oneVersion : versions)
                maybeDispose(oneVersion);
        maybeDispose((Object) file);
    }

    protected void maybeDispose(Object d) {
        if (d instanceof Disposable) {
            ((Disposable) d).dispose();
        }
    }


    protected DiffResult analyzeFile(FileToAnalyze file) throws IOException {
        String options = fileOptionsProvider.getOptions(file);
        if (FileOptionsProvider.SKIP_FILE.equalsIgnoreCase(options))
            return null;

        Charset charset = null;
        if (file instanceof CharsetAwareFile)
            charset = ((CharsetAwareFile) file).getCharset();
        if (charset == null)
            charset = charsetSelector.selectCharset(file, options);

        FileTraits fileTraits = examineFile(file, charset);

        if (skipIdenticalFiles && fileTraits.identical)
            return null;

        if (fileTraits.binary)
            return new DiffResult(file, BinaryFilter.INSTANCE, options,
                    fileTraits.changeType, null, null);

        LanguageFilter filter = languageFilterSelector.selectLanguageFilter(
            file, charset, fileTraits.initialContents, options);
        if (filter == null)
            return null;

        DiffAnalysisRequest r = new DiffAnalysisRequest(file, filter, options,
                charset, fileTraits);
        DiffResult result = diffAnalyzer.analyze(r);
        return result;
    }

    protected FileTraits examineFile(FileToAnalyze file, Charset charset)
            throws IOException {
        return FileTraits.examineFile(file, charset);
    }

}
