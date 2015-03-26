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

package net.sourceforge.processdash.tool.diff.impl.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.processdash.tool.diff.ui.AbstractLOCDiffReport;

public class Main extends AbstractLOCDiffReport {

    private FileSystemFileSet fileSet;

    public Main(String... args) throws IOException {
        this(new ArrayList<String>(Arrays.asList(args)));
    }

    public Main(List<String> args) throws IOException {
        super.processArgs(args);
        createFileSet(args);
    }

    private void createFileSet(List<String> args) {
        List<String> fileArgs = extractFileArgs(args);
        if (fileArgs.size() == 1)
            engine.setSkipIdenticalFiles(false);

        // now that we've extracted the file args, the remaining args should
        // be considered arguments for the diff engine.
        super.setEngineOptions(args);

        fileSet = new FileSystemFileSet();
        for (String path : fileArgs)
            fileSet.addFile(new File(path));

        fileSet.validate();
    }

    private List<String> extractFileArgs(List<String> args) {
        int fileArgPos = getFirstFileArgPos(args);
        if (fileArgPos > args.size() - 1)
            throw new FileSystemDiffException.NoFilesListed();

        List<String> fileArgs = args.subList(fileArgPos, args.size());
        List<String> files = new ArrayList<String>(fileArgs);
        fileArgs.clear();
        return files;
    }

    private int getFirstFileArgPos(List<String> args) {
        for (int i = args.size(); i-- > 0;) {
            String arg = args.get(i);
            if ("-files".equals(arg) || "-dirs".equals(arg)) {
                args.remove(i);
                return i;
            }
            else if (arg.startsWith("-") || arg.startsWith("+")) {
                return i + 1;
            }
        }
        return 0;
    }

    private void run() throws IOException {
        engine.addFilesToAnalyze(fileSet);
        engine.run();
    }

    public static void main(String[] args) {
        try {
            new Main(args).run();
        } catch (FileSystemDiffException.NoFilesListed e) {
            System.err.println("No files were listed on the command line.");
        } catch (FileSystemDiffException.FileNotFound e) {
            System.err.println("Could not locate the file '"
                    + e.getMissingFile().getPath() + "'");
        } catch (FileSystemDiffException.TypeMismatch e) {
            System.err.println("Some of the arguments named files, "
                    + "while others named directories");
        } catch (IOException ioe) {
            System.err.println("Unexpected problem encountered:");
            ioe.printStackTrace();
        }
    }

}
