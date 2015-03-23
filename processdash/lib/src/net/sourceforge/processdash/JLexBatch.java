// Copyright (C) 2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;

public class JLexBatch extends MatchingTask {


    public void setDir(File dir) {
        fileset.setDir(dir);
    }

    public void execute() throws BuildException {
        DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
        String[] srcFilenames = ds.getIncludedFiles();
        if (srcFilenames.length == 0)
            throw new BuildException
                ("You must designate at least one input lexer file.");

        for (int j = 0; j < srcFilenames.length; j++) {
            String inputFilename = srcFilenames[j];
            File inputFile = new File(ds.getBasedir(), inputFilename);
            String outputFilename = getOutputFilename(inputFilename);
            File outputFile = new File(ds.getBasedir(), outputFilename);

            if (outputFile.lastModified() > inputFile.lastModified())
                // file is already up-to-date
                continue;

            try {
                JLex.Main.main(new String[] { inputFile.getAbsolutePath(),
                        outputFile.getAbsolutePath()});
            } catch (IOException ioe) {
                throw new BuildException("Cannot create file '"+outputFile+"'");
            }
        }
    }

    private String getOutputFilename(String inputFilename) {
        int suffixPos = inputFilename.lastIndexOf('.');
        if (suffixPos != -1)
            inputFilename = inputFilename.substring(0, suffixPos);
        return inputFilename + ".java";
    }

}
