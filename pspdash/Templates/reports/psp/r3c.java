// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil


import pspdash.*;
import pspdash.data.DataRepository;
import java.io.IOException;
import java.util.Enumeration;
import java.text.NumberFormat;

public class r3c extends TinyCGIBase implements DefectAnalyzer.Task {

    /** Generate CGI script output. */
    protected void writeContents() {
        initValues();
        DefectAnalyzer.run(getPSPProperties(), getDataRepository(),
                           getPrefix(), parameters, this);

        out.println("<TABLE NAME=D22 BORDER><TR>");
        out.println("<TD colspan=5 class=header>Defect Fix Times</TD></TR>");
        out.println("<TR><TD colspan=2></TD>");
        out.println("<TD VALIGN=bottom>Defects found in compiling</TD>");
        out.println("<TD VALIGN=bottom>Defects found in testing</TD>");
        out.println("<TD VALIGN=bottom>Total defects found</TD></TR>");

        out.println("<TR><TD rowspan=3>Defects injected in designing</TD>");
        out.println("<TD ALIGN=right NOWRAP>Tot. fix time</TD>");
        out.println("<TD>" + time(INJ_DESIGN, REM_COMPILE) + "</TD>");
        out.println("<TD>" + time(INJ_DESIGN, REM_TEST) + "</TD>");
        out.println("<TD>" + time(INJ_DESIGN, REM_TOTAL) + "</TD></TR>");
        out.println("<TR><TD ALIGN=right NOWRAP>Tot. defects</TD>");
        out.println("<TD>" + count(INJ_DESIGN, REM_COMPILE) + "</TD>");
        out.println("<TD>" + count(INJ_DESIGN, REM_TEST) + "</TD>");
        out.println("<TD>" + count(INJ_DESIGN, REM_TOTAL) + "</TD></TR>");
        out.println("<TR><TD ALIGN=right NOWRAP>Avg. fix time</TD>");
        out.println("<TD>" + avgTime(INJ_DESIGN, REM_COMPILE) + "</TD>");
        out.println("<TD>" + avgTime(INJ_DESIGN, REM_TEST) + "</TD>");
        out.println("<TD>" + avgTime(INJ_DESIGN, REM_TOTAL) + "</TD></TR>");

        out.println("<TR><TD rowspan=3>Defects injected in coding</TD>");
        out.println("<TD ALIGN=right NOWRAP>Tot. fix time</TD>");
        out.println("<TD>" + time(INJ_CODE, REM_COMPILE) + "</TD>");
        out.println("<TD>" + time(INJ_CODE, REM_TEST) + "</TD>");
        out.println("<TD>" + time(INJ_CODE, REM_TOTAL) + "</TD></TR>");
        out.println("<TR><TD ALIGN=right NOWRAP>Tot. defects</TD>");
        out.println("<TD>" + count(INJ_CODE, REM_COMPILE) + "</TD>");
        out.println("<TD>" + count(INJ_CODE, REM_TEST) + "</TD>");
        out.println("<TD>" + count(INJ_CODE, REM_TOTAL) + "</TD></TR>");
        out.println("<TR><TD ALIGN=right NOWRAP>Avg. fix time</TD>");
        out.println("<TD>" + avgTime(INJ_CODE, REM_COMPILE) + "</TD>");
        out.println("<TD>" + avgTime(INJ_CODE, REM_TEST) + "</TD>");
        out.println("<TD>" + avgTime(INJ_CODE, REM_TOTAL) + "</TD></TR>");

        out.println("<TR><TD rowspan=3>Total defects injected</TD>");
        out.println("<TD ALIGN=right NOWRAP>Tot. fix time</TD>");
        out.println("<TD>" + time(INJ_TOTAL, REM_COMPILE) + "</TD>");
        out.println("<TD>" + time(INJ_TOTAL, REM_TEST) + "</TD>");
        out.println("<TD>" + time(INJ_TOTAL, REM_TOTAL) + "</TD></TR>");
        out.println("<TR><TD ALIGN=right NOWRAP>Tot. defects</TD>");
        out.println("<TD>" + count(INJ_TOTAL, REM_COMPILE) + "</TD>");
        out.println("<TD>" + count(INJ_TOTAL, REM_TEST) + "</TD>");
        out.println("<TD>" + count(INJ_TOTAL, REM_TOTAL) + "</TD></TR>");
        out.println("<TR><TD ALIGN=right NOWRAP>Avg. fix time</TD>");
        out.println("<TD>" + avgTime(INJ_TOTAL, REM_COMPILE) + "</TD>");
        out.println("<TD>" + avgTime(INJ_TOTAL, REM_TEST) + "</TD>");
        out.println("<TD>" + avgTime(INJ_TOTAL, REM_TOTAL) + "</TD></TR>");

        out.println("</TABLE></BODY></HTML>");
    }
    private void initValues() {
        for (int inj=0;  inj<4;  inj++)
            for (int rem=0;  rem<4;  rem++)
                time[inj][rem] = count[inj][rem] = 0;
    }
    private static final String NA = "-";
    protected String count(int i, int r) {
        return (count[i][r] == 0 ? NA : Integer.toString(count[i][r]));
    }
    protected String time(int i, int r) {
        return (time[i][r] == 0 ? NA : nf.format(time[i][r]));
    }
    protected String avgTime(int i, int r) {
        return (count[i][r] == 0 ? NA : nf.format(time[i][r] / count[i][r]));
    }
    private static NumberFormat nf = NumberFormat.getInstance();
    static { nf.setMaximumFractionDigits(2); }

    public static final int MISC_UNUSED = 0;
    public static final int INJ_DESIGN  = 1;
    public static final int INJ_CODE    = 2;
    public static final int INJ_TOTAL   = 3;
    public static final int REM_COMPILE = 1;
    public static final int REM_TEST    = 2;
    public static final int REM_TOTAL   = 3;
    protected int [][] count  = new int[4][4];
    protected float [][] time = new float[4][4];

    public void analyze(String path, Defect d) {
        int inj = MISC_UNUSED, rem = MISC_UNUSED;
        if (d.phase_injected.endsWith("Design")) inj = INJ_DESIGN;
        else if (d.phase_injected.endsWith("Code")) inj = INJ_CODE;
        if (d.phase_removed.endsWith("Compile")) rem = REM_COMPILE;
        else if (d.phase_removed.endsWith("Test")) rem = REM_TEST;

        float fixtime = Float.parseFloat(d.fix_time);

        count[inj][rem] += 1;
        time [inj][rem] += fixtime;

        count[inj][REM_TOTAL] += 1;
        time [inj][REM_TOTAL] += fixtime;

        count[INJ_TOTAL][rem] += 1;
        time [INJ_TOTAL][rem] += fixtime;

        count[INJ_TOTAL][REM_TOTAL] += 1;
        time [INJ_TOTAL][REM_TOTAL] += fixtime;
    }
}
