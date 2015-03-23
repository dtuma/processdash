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

package net.sourceforge.processdash.tool.diff.ui;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import net.sourceforge.processdash.tool.diff.HardcodedFilterLocator;
import net.sourceforge.processdash.tool.diff.engine.DefaultCharsetSelector;
import net.sourceforge.processdash.tool.diff.engine.DiffEngine;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.XMLUtils;

public class AbstractLOCDiffReport {

    protected DiffEngine engine;

    protected AbstractLOCDiffReport() {
        engine = new DiffEngine();
        engine.setLanguageFilters(HardcodedFilterLocator.getFilters());
    }

    protected void processArgs(List<String> args) throws IOException {
        String destFilename = getArg(args, "-dest");
        boolean printProgress;

        if (getFlag(args, "-xml")) {
            engine.addDiffListener(new XmlDiffReportWriter(destFilename));
            printProgress = XMLUtils.hasValue(destFilename);
        } else {
            HtmlDiffReportWriter html = new HtmlDiffReportWriter(destFilename);
            Integer tabWidth = getInt(args, "-tabWidth");
            if (tabWidth != null)
                html.setTabWidth(tabWidth);
            if (getFlag(args, "-noRedlines"))
                html.setNoRedlines(true);
            engine.addDiffListener(html);
            printProgress = true;
        }

        if (getFlag(args, "-quiet"))
            printProgress = false;

        if (printProgress)
            engine.addDiffListener(StdoutProgressMonitor.INSTANCE);

        String charset = getArg(args, "-charset");
        if (charset != null) {
            try {
                engine.setCharsetSelector(new DefaultCharsetSelector(Charset
                        .forName(charset)));
            } catch (Exception e) {
                System.err.println("Unrecognized character set '" + charset
                        + "'");
            }
        }

        if (getFlag(args, "-countUnchanged"))
            engine.setSkipIdenticalFiles(false);

        // by default, pass all remaining unprocessed options to the DiffEngine
        setEngineOptions(args);
    }

    protected void setEngineOptions(List<String> args) {
        String engineOptions = StringUtils.join(args, " ");
        engine.setFileOptions(engineOptions);
    }

    protected String getArg(List<String> args, String flag) {
        int pos = args.indexOf(flag);
        if (pos != -1 && pos + 1 < args.size()) {
            String result = args.remove(pos + 1);
            args.remove(pos);
            return result;
        } else {
            String flagPrefix = flag + "=";
            for (int i = args.size() - 1; i-- > 0;) {
                if (args.get(i).startsWith(flagPrefix)) {
                    String result = args.remove(i);
                    return result.substring(flagPrefix.length());
                }
            }
        }
        return null;
    }

    protected boolean getFlag(List<String> args, String flag) {
        return args.remove(flag);
    }

    protected Integer getInt(List<String> args, String flag) {
        String value = getArg(args, flag);
        if (value != null) {
            try {
                return new Integer(value);
            } catch (Exception e) {}
        }
        return null;
    }

}
