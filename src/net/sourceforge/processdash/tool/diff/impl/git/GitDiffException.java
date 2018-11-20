// Copyright (C) 2018 Tuma Solutions, LLC
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

package net.sourceforge.processdash.tool.diff.impl.git;

import java.util.List;

public class GitDiffException extends RuntimeException {


    public GitDiffException() {}

    public GitDiffException(String message) {
        super(message);
    }

    public GitDiffException(Throwable cause) {
        super(cause);
    }

    public GitDiffException(String message, Throwable cause) {
        super(message, cause);
    }


    public static class NotGitWorkingDir extends GitDiffException {
    }

    public static class RefNotFoundException extends GitDiffException {
        private String ref;

        public RefNotFoundException(String ref) {
            this.ref = ref;
        }

        public RefNotFoundException(String ref, Throwable cause) {
            this.ref = ref;
            initCause(cause);
        }

        public String getMissingRef() {
            return ref;
        }
    }

    public static class TooManyIdArgsException extends GitDiffException {
        private List<String> args;

        public TooManyIdArgsException(List<String> args) {
            this.args = args;
        }

        public List<String> getArgs() {
            return args;
        }
    }

}
