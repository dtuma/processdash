// Copyright (C) 2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
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
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

public class CompletionStatus {

    /** Interface that is implemented by objects which are capable of
     * reporting their completion status.
     */
    public interface Capable {
        /** Return the completion status of an operation (never null). */
        public CompletionStatus getCompletionStatus();
    }

    public static final String NOT_RUN = "Not_Run";

    public static final String NO_WORK_NEEDED = "Nothing_Needed";

    public static final String SUCCESS = "Success";

    public static final String ERROR = "Error";


    private String status;
    private Object target;
    private Throwable exception;


    public CompletionStatus(String status, Object target, Throwable exception) {
        this.status = status;
        this.target = target;
        this.exception = exception;
    }

    /** Return a key indicating the status of the operation.
     * 
     * {@see #NOT_RUN} {@see #NO_WORK_NEEDED} {@see #SUCCESS} {@see #ERROR}
     */
    public String getStatus() {
        return status;
    }

    /** Return the target of this operation.
     * 
     * For import / export operations that read/write to a file, this will be
     * a File object describing the file in question.
     */
    public Object getTarget() {
        return target;
    }

    /** If an error occurred, this returns the exception.  Otherwise, returns
     * null.
     */
    public Throwable getException() {
        return exception;
    }

    public static final CompletionStatus NOT_RUN_STATUS = new CompletionStatus(
            NOT_RUN, null, null);

}
