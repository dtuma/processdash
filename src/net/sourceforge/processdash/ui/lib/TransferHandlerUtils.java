// Copyright (C) 2012-2019 Tuma Solutions, LLC
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

package net.sourceforge.processdash.ui.lib;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class TransferHandlerUtils {

    /**
     * Check to see whether a file list can potentially be extracted from
     * any of the given data flavors.
     */
    public static boolean hasFileListFlavor(DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (DataFlavor.javaFileListFlavor.equals(flavors[i]))
                return true;
            if (uriListFlavor != null && uriListFlavor.equals(flavors[i]))
                return true;
        }
        return false;
    }

    /**
     * Extract a list of File objects that were transferred as part of a
     * copy/paste or drag/drop operation.
     * 
     * @return a list of java files, or null if no list could be extracted.
     */
    public static List<File> getTransferredFileList(Transferable t) {
        try {
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                return (List) t.getTransferData(DataFlavor.javaFileListFlavor);

            if (uriListFlavor != null && t.isDataFlavorSupported(uriListFlavor)) {
                String data = (String) t.getTransferData(uriListFlavor);
                return textURIListToFileList(data);
            }

        } catch (Exception e) {
        }
        return null;
    }

    private static List<File> textURIListToFileList(String data) {
        List<File> result = new ArrayList(1);
        for (URI oneUri : parseURIList(data)) {
            try {
                File oneFile = new File(oneUri);
                result.add(oneFile);
            } catch (IllegalArgumentException e) {
                // the URI is not a valid 'file:' URI
            }
        }
        return result;
    }


    /**
     * Check to see whether a URI list can potentially be extracted from
     * any of the given data flavors.
     */
    public static boolean hasURIListFlavor(DataFlavor[] flavors) {
        for (int i = 0; i < flavors.length; i++) {
            if (uriListFlavor != null && uriListFlavor.equals(flavors[i]))
                return true;
        }
        return false;
    }

    /**
     * Extract a list of URI objects that were transferred as part of a
     * copy/paste or drag/drop operation.
     * 
     * @return a list of URIs, or null if no list could be extracted.
     */
    public static List<URI> getTransferredURIList(Transferable t) {
        try {
            if (uriListFlavor != null && t.isDataFlavorSupported(uriListFlavor)) {
                String data = (String) t.getTransferData(uriListFlavor);
                return parseURIList(data);
            }

        } catch (Exception e) {
        }
        return null;
    }


    private static List<URI> parseURIList(String data) {
        List<URI> result = new ArrayList(1);
        StringTokenizer lines = new StringTokenizer(data, "\r\n");
        while (lines.hasMoreTokens()) {
            String oneLine = lines.nextToken();
            if (oneLine.startsWith("#")) {
                // the line is a comment (as per the RFC 2483)
                continue;
            }
            try {
                URI oneUri = new URI(oneLine);
                result.add(oneUri);
            } catch (URISyntaxException e) {
                // malformed URI
            }
        }
        return result;
    }

    private static final DataFlavor uriListFlavor = createFlavor(
            "text/uri-list;class=java.lang.String");

    private static DataFlavor createFlavor(String mimeType) {
        try {
            return new DataFlavor(mimeType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
