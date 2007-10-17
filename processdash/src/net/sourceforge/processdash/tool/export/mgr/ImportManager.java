// Copyright (C) 2005-2007 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC: processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.tool.export.DataImporter;

import org.w3c.dom.Element;

public class ImportManager extends AbstractManager {

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
        DataImporter.waitForAllInitialized();
    }

    private ImportManager() {
        super();
        initialize();

        if (logger.isLoggable(Level.CONFIG))
            logger.config("ImportManager contents:\n" + getDebugContents());
    }

    protected String getTextSettingName() {
        return "import.directories";
    }

    protected String getXmlSettingName() {
        return "import.instructions";
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

    private String massagePrefix(String p) {
        p = p.replace(File.separatorChar, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        return p;
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
            String origDir = instr.getDirectory();
            String remappedDir = ExternalResourceManager.getInstance()
                    .remapFilename(origDir);
            DataImporter.addImport(data, prefix, remappedDir);
            return null;
        }

    }

    private InstructionAdder instructionAdder = new InstructionAdder();

    private class InstructionRemover implements ImportInstructionDispatcher {

        public Object dispatch(ImportDirectoryInstruction instr) {
            String prefix = instr.getPrefix();
            String origDir = instr.getDirectory();
            String remappedDir = ExternalResourceManager.getInstance()
                    .remapFilename(origDir);
            DataImporter.removeImport(prefix, remappedDir);
            return null;
        }

    }

    private InstructionRemover instructionRemover = new InstructionRemover();
}
