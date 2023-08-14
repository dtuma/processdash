// Copyright (C) 2006-2023 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.export.mgr;

import java.util.EventObject;

public class CompletionStatus {

    /** Interface that is implemented by objects which are capable of
     * reporting their completion status.
     */
    public interface Capable {
        /** Return the completion status of an operation (never null). */
        public CompletionStatus getCompletionStatus();
    }

    /** Interface that is implemented by objects which are interested in
     * hearing the results of a CompletionStatus.Capable event. */
    public interface Listener {
        public void completionStatusReady(Event event);
    }

    /** Event object used by the Listener interface above */
    public static class Event extends EventObject {

        public Event(Capable source) {
            super(source);
        }

        @Override
        public Capable getSource() {
            return (Capable) super.getSource();
        }
    }

    public static final String NOT_RUN = "Not_Run";

    public static final String NO_WORK_NEEDED = "Nothing_Needed";

    public static final String SUCCESS = "Success";

    public static final String ERROR = "Error";


    private String status;
    private Object target;
    private boolean cloudStorage;
    private Throwable exception;


    public CompletionStatus(String status, Object target, Throwable exception) {
        this(status, target, false, exception);
    }

    public CompletionStatus(String status, Object target, boolean cloudStorage,
            Throwable exception) {
        this.status = status;
        this.target = target;
        this.cloudStorage = cloudStorage;
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

    /** Return true if the target is on cloud storage */
    public boolean isCloudStorage() {
        return cloudStorage;
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
