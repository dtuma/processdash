// Copyright (C) 2005-2008 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.tool.bridge.client.BridgedImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectory;
import net.sourceforge.processdash.tool.bridge.client.ImportDirectoryFactory;
import net.sourceforge.processdash.tool.export.DataImporter;
import net.sourceforge.processdash.util.StringUtils;

import org.w3c.dom.Element;

public class ImportManager extends AbstractManager {

    public static final String SETTING_NAME = "import.instructions";

    private static final Logger logger = Logger.getLogger(ImportManager.class
            .getName());

    private static ImportManager INSTANCE = null;

    public synchronized static ImportManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ImportManager();
        return INSTANCE;
    }

    public static void init(DataRepository dataRepository) {
        getInstance().setData(dataRepository, true);
        if (getInstance().urlValueChanged)
            getInstance().saveSetting();
        DataImporter.waitForAllInitialized();
    }

    private boolean urlValueChanged = false;

    private ImportManager() {
        super();
        initialize();
        if (urlValueChanged)
            saveSetting();

        if (logger.isLoggable(Level.CONFIG))
            logger.config("ImportManager contents:\n" + getDebugContents());
    }

    protected String getTextSettingName() {
        return "import.directories";
    }

    protected String getXmlSettingName() {
        return SETTING_NAME;
    }

    protected void parseXmlInstruction(Element element) {
        if (ImportDirectoryInstruction.matches(element))
            doAddInstruction(new ImportDirectoryInstruction(element));
    }

    protected void parseTextInstruction(String left, String right) {
        if ("Imported".equals(left) && "./import".equals(right))
            return;

        String prefix = massagePrefix(left);
        String dir = Settings.translateFile(right);
        doAddInstruction(new ImportDirectoryInstruction(dir, prefix));
    }

    @Override
    protected void saveSetting() {
        super.saveSetting();
        urlValueChanged = false;
    }

    private String massagePrefix(String p) {
        p = p.replace(File.separatorChar, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        return p;
    }

    private String getDirInfo(ImportDirectoryInstruction instr) {
        if (StringUtils.hasValue(instr.getDirectory()))
            return instr.getDirectory();
        else
            return instr.getURL();
    }

    public void handleAddedInstruction(AbstractInstruction instr) {
        if (instr.isEnabled()) {
            // use the visitor pattern to invoke the correct handler method
            instr.dispatch(instructionAdder);
        }
    }

    protected void handleRemovedInstruction(AbstractInstruction instr) {
        if (instr.isEnabled()) {
            // use the visitor pattern to invoke the correct handler method
            instr.dispatch(instructionRemover);
        }
    }

    private class InstructionAdder implements ImportInstructionDispatcher {

        public Object dispatch(ImportDirectoryInstruction instr) {
            String prefix = instr.getPrefix();
            ImportDirectory importDir = ImportDirectoryFactory.getInstance()
                    .get(instr.getURL(), instr.getDirectory());

            if (importDir instanceof BridgedImportDirectory) {
                BridgedImportDirectory bid = (BridgedImportDirectory) importDir;
                String oldURL = instr.getURL();
                String newURL = bid.getRemoteURL();
                if (newURL != null && !newURL.equals(oldURL)) {
                    instr.setURL(newURL);
                    urlValueChanged = true;
                }
            }

            DataImporter.addImport(data, prefix, getDirInfo(instr), importDir);
            return null;
        }

    }

    private InstructionAdder instructionAdder = new InstructionAdder();

    private class InstructionRemover implements ImportInstructionDispatcher {

        public Object dispatch(ImportDirectoryInstruction instr) {
            String prefix = instr.getPrefix();
            DataImporter.removeImport(prefix, getDirInfo(instr));
            return null;
        }

    }

    private InstructionRemover instructionRemover = new InstructionRemover();
}
