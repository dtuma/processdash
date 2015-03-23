// Copyright (C) 2009 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.web.psp;

import java.io.IOException;

import net.sourceforge.processdash.log.defects.Defect;

public class FixTimeConsistencyCheck extends PspForEngDefectBase {

    private double compileFixTime;

    private double testFixTime;

    @Override
    protected void writeContents() throws IOException {
        compileFixTime = testFixTime = 0;

        runDefectAnalysis();

        out.println(HEADER);
        printComparison("Compile", compileFixTime);
        printComparison("Test", testFixTime);
        out.print(FOOTER);
    }

    public void analyze(String path, Defect d) {
        if ("Compile".equalsIgnoreCase(d.phase_removed))
            compileFixTime += fixTime(d);
        else if ("Test".equalsIgnoreCase(d.phase_removed))
            testFixTime += fixTime(d);
    }

    private void printComparison(String phase, double fixTime)
            throws IOException {
        boolean foundDefects = getDoubleData(phase + "/Defects Removed") > 0;
        double phaseTime = getDoubleData(phase + "/Time");
        double unaccountedForTime = phaseTime - fixTime;
        Object[] args = new Object[] { phase.toLowerCase(), fixTime, phaseTime,
                unaccountedForTime };

        if (fixTime > phaseTime) {
            printStudentDataError(res().format("Fix_Time_Too_High_FMT", args));
            return;
        }

        double fudgeMinutes = getDoubleParam(phase + "FudgeMinutes", 5);
        if (unaccountedForTime > fudgeMinutes) {
            if (foundDefects == false) {
                printStudentDataError(res().format("Zero_Defects_FMT", args));
                return;
            }

            double minPercent = getDoubleParam(phase + "FixPercent", 80) / 100;
            double actualPercent = fixTime / phaseTime;
            if (actualPercent < minPercent) {
                printStudentDataError(res().format("Fix_Time_Too_Low_FMT", args));
                return;
            }
        }

        printStudentDataOK(res().format("Fix_Time_Reasonable_FMT", args));
    }

}
