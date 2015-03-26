// Copyright (C) 2003-2006 Tuma Solutions, LLC
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import net.sourceforge.processdash.util.*;


/** Utility class for verifying that a Jar file was signed by the
 * Process Dashboard development team.
 * 
 * <b>NOTE:</b>  This class is not currently used by the Process Dashboard.
 * Instead, the dashboard uses the Java security framework to verify JAR file
 * signatures, and to restrict code in untrusted add-ons to a sandbox.
 * 
 * @deprecated
 */
public class JarVerifier {

    /** Returns true if the given file is a jar file that has been
     * digitally signed by the Process Dashboard development team.
     */
    public static boolean verify(File file) throws IOException {
        JarInputStream jarIn = null;
        try {
            jarIn = new JarInputStream(new FileInputStream(file));
            JarEntry entry;
            byte[] buf = new byte[1024];

            while ((entry = jarIn.getNextJarEntry()) != null) {
                // the items in the "meta-inf" directory are not signed.
                if (entry.getName().toUpperCase().startsWith("META-INF/"))
                    continue;

                // it is necessary to read the entire contents of the
                // jar file entry, to allow the java platform to verify
                // that the contents are intact
                while (jarIn.read(buf, 0, buf.length) != -1)
                    ; // discard the data

                // if the entry was not signed with a valid certificate,
                // return false
                if (!verify(entry.getCertificates()))
                    return false;
            }

            // all the files seem okay - return true.
            return true;
        } finally {
            if (jarIn != null)
                try { jarIn.close(); } catch (Exception e) {}
        }
    }


    /** Returns true if at <code>certs</code> contains at least one
     * certificate which can be verified using the one of the trusted
     * public keys.
     */
    public static boolean verify(Certificate[] certs) {
        if (certs == null || certs.length == 0)
            return false;

        for (int c = 0;   c < certs.length;   c++) {
            for (int s = 0;   s < SIGNERS.length;   s++)
                if (SIGNERS[s] != null)
                    try {
                        certs[c].verify(SIGNERS[s]);
                        return true;
                    } catch (Throwable t) { }

            if (debugOutputPubKey) debugOutputPubKey(certs[c]);
        }

        return false;
    }



    private static final String[] PUBLIC_KEY_DATA = {
        // public key of certificate used to sign process dashboard
        // add-on process sets (owned by <tuma@users.sourceforge.net>)
        "rO0ABXNyACJzdW4uc2VjdXJpdHkucHJvdmlkZXIuRFNBUHVibGljS2V51nJ9DQQZ6" +
        "3sCAAFMAAF5dAAWTGphdmEvbWF0aC9CaWdJbnRlZ2VyO3hyABlzdW4uc2VjdXJpdH" +
        "kueDUwOS5YNTA5S2V5taAdvmSacqYDAANMAAVhbGdpZHQAH0xzdW4vc2VjdXJpdHk" +
        "veDUwOS9BbGdvcml0aG1JZDtbAAplbmNvZGVkS2V5dAACW0JbAANrZXlxAH4ABHhw" +
        "egAAAbswggG3MIIBLAYHKoZIzjgEATCCAR8CgYEA/X9TgR11EilS30qcLuzk5/YRt" +
        "1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUK" +
        "Wkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2H" +
        "XKu/yIgMZndFIAccCFQCXYFCPFSMLzLKSuYKi64QL8Fgc9QKBgQD34aCF1ps93su8" +
        "q1w2uFe5eZSvu/o66oL5V0wLPQeCZ1FZV4661FlP5nEHEIGAtEkWcSPoTCgWE7fPC" +
        "TKMyKbhPBZ6i1R8jSjgo64eK7OmdZFuo38L+iE1YvH7YnoBJDvMpPG+qFGQiaiD3+" +
        "Fa5Z8GkotmXoB7VSVkAUw7/s9JKgOBhAACgYBRlkxZ2JXzCZINkrib9Wrjka/o7aR" +
        "A6D3FSI1D9c2dOozQgJecHenMxqtp4lGR9RK077eAeLLZN0YVxIqcCop89Rk6ut6N" +
        "+aORiRvYr697pMH4uKTgztfWHlK/Kw73tmXh3HWWVdzuiMNU70WRqBIns8r8S/BUb" +
        "XCFm8H3MmZkYHhzcgAUamF2YS5tYXRoLkJpZ0ludGVnZXKM/J8fqTv7HQIABkkACG" +
        "JpdENvdW50SQAJYml0TGVuZ3RoSQATZmlyc3ROb256ZXJvQnl0ZU51bUkADGxvd2V" +
        "zdFNldEJpdEkABnNpZ251bVsACW1hZ25pdHVkZXEAfgAEeHIAEGphdmEubGFuZy5O" +
        "dW1iZXKGrJUdC5TgiwIAAHhw///////////////+/////gAAAAF1cgACW0Ks8xf4B" +
        "ghU4AIAAHhwAAAAgFGWTFnYlfMJkg2SuJv1auORr+jtpEDoPcVIjUP1zZ06jNCAl5" +
        "wd6czGq2niUZH1ErTvt4B4stk3RhXEipwKinz1GTq63o35o5GJG9ivr3ukwfi4pOD" +
        "O19YeUr8rDve2ZeHcdZZV3O6Iw1TvRZGoEiezyvxL8FRtcIWbwfcyZmRg",

        // add additional allowed public keys here (for example, if
        // the original dashboard developers and their original signing
        // certificate are no longer available.)  You can generate the
        // String needed for this array by invoking the main method below.
        // on a jar file which you have previously signed.
    };

    private static final PublicKey[] SIGNERS;


    /* Initialize the array of trusted signatures */
    static {
        SIGNERS = new PublicKey[PUBLIC_KEY_DATA.length];
        for (int i = 0;   i < PUBLIC_KEY_DATA.length;   i++) {
            SIGNERS[i] = null;

            if (PUBLIC_KEY_DATA[i] != null) try {
                byte[] pubKeyBytes = PUBLIC_KEY_DATA[i].getBytes("US-ASCII");
                pubKeyBytes = Base64.decode(pubKeyBytes);
                ObjectInputStream in = new ObjectInputStream
                    (new ByteArrayInputStream(pubKeyBytes));
                SIGNERS[i] = (PublicKey) in.readObject();
                in.close();
            } catch (Throwable t) {}
        }
    }



    /// The following methods and fields are not used during normal
    //operation of this class - they are only used when this class's
    //main() method is called from the command line.

    private static boolean debugOutputPubKey = false;

    private static void debugOutputPubKey(Certificate cert) {
        if (cert == null) return;

        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(bytesOut);
            objOut.writeObject(cert.getPublicKey());
            objOut.close();

            System.out.println
                ("        // public key string to enable validation:");

            String s = Base64.encodeBytes(bytesOut.toByteArray(),
                    Base64.DONT_BREAK_LINES);
            while (s.length() > 65) {
                System.out.print("        \"");
                System.out.print(s.substring(0, 65));
                System.out.println("\" +");
                s = s.substring(65);
            }
            System.out.print("        \"");
            System.out.print(s);
            System.out.println("\",");
        } catch (IOException ioe) {}

        debugOutputPubKey = false;
    }

    public static void main(String argv[]) {
        if (argv == null || argv.length == 0) {
            System.out.println("Usage: java net.sourceforge.processdash.templates.JarVerifier filename.jar");
        } else {
            for (int i = 0;   i < argv.length;   i++) try {
                String filename = argv[i];
                System.out.println("Checking " + filename);
                debugOutputPubKey = true;
                if (verify(new File(filename)))
                    System.out.println("    file is valid.");
                else if (debugOutputPubKey)
                    System.out.println("    file is not signed.");
                else
                    System.out.println("    file is invalid, pending "+
                                       "addition of the above public key.");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

}
