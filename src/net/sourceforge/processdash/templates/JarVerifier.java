// Copyright (C) 2003-2023 Tuma Solutions, LLC
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


package net.sourceforge.processdash.templates;


import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;


/** Utility class for verifying that a Jar file was signed by the
 * Process Dashboard development team.
 * 
 * This class has been moved to the security package. This stub forwards
 * requests to that new class for binary backward compatibility.
 * 
 * @deprecated
 */
public class JarVerifier {

    /** Returns true if the given file is a jar file that has been
     * digitally signed by the Process Dashboard development team.
     */
    public static boolean verify(File file) throws IOException {
        return net.sourceforge.processdash.security.JarVerifier.verify(file);
    }


    /** Returns true if at <code>certs</code> contains at least one
     * certificate which can be verified using the one of the trusted
     * public keys.
     */
    public static boolean verify(Certificate[] certs) {
        return net.sourceforge.processdash.security.JarVerifier.verify(certs);
    }

}
