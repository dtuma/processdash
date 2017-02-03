// Copyright (C) 2007-2017 Tuma Solutions, LLC
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.tool.export.mgr;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import net.sourceforge.processdash.tool.export.impl.ExternalResourceAutoLocator;
import net.sourceforge.processdash.tool.export.impl.ExternalResourceManifestXMLv1;
import net.sourceforge.processdash.util.StringUtils;

public class ExternalLocationMapper {

    static final String DEFAULT_MAP_DATA_SOURCE_PROPERTY =
        ExternalLocationMapper.class.getName() + ".defaultMapDataSource";

    private static final Logger logger = Logger
            .getLogger(ExternalLocationMapper.class.getName());

    private static ExternalLocationMapper INSTANCE = new ExternalLocationMapper();

    public static ExternalLocationMapper getInstance() {
        return INSTANCE;
    }

    Map pathRemappings = null;

    Map generalizedRemappings = null;

    String datasetUrl = null;

    public ExternalLocationMapper() {}

    public void loadDefaultMappings() {
        String setting = System.getProperty(DEFAULT_MAP_DATA_SOURCE_PROPERTY);
        if (StringUtils.hasValue(setting)) {
            File mapDataSource = new File(setting);
            ExternalResourceManifestXMLv1 loader = new ExternalResourceManifestXMLv1();
            loadMappings(loader.load(mapDataSource));
            datasetUrl = loader.getDatasetUrl();
        }
    }

    public boolean loadMappings(Map mappings) {
        pathRemappings = normalizeMappings(mappings);
        generalizedRemappings = ExternalResourceAutoLocator
            .getGeneralizedMappings(pathRemappings);

        if (isEmpty(pathRemappings))
            return false;
        else {
            logger.config("Path remappings: " + pathRemappings);
            logger.config("Generalized remappings: " + generalizedRemappings);
            return true;
        }
    }

    private Map normalizeMappings(Map mappings) {
        if (isEmpty(mappings))
            return null;

        Map result = new HashMap();

        for (Iterator i = mappings.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            String origPath = (String) e.getKey();
            String newPath = (String) e.getValue();
            result.put(normalize(origPath), normalize(newPath));
        }
        return result;
    }

    private boolean isEmpty(Map m) {
        return m == null || m.isEmpty();
    }

    private String normalize(String path) {
        return path.replace('\\', '/');
    }

    private String denormalize(String path) {
        return path.replace('/', File.separatorChar);
    }

    /**
     * @return the dataset URL, as read from the external resources manifest
     *         file. If this process is not running from a compressed archive,
     *         this will return null.
     * @since 2.3.2
     */
    public String getDatasetUrl() {
        return datasetUrl;
    }

    /** @since 2.1.10 */
    public boolean isMapping() {
        return !isEmpty(pathRemappings);
    }

    public String remapFilename(String origFile) {
        if (pathRemappings == null || origFile == null)
            return origFile;

        String origNormalizedPath = normalize(origFile);

        for (Iterator i = pathRemappings.entrySet().iterator(); i.hasNext();) {
            String remappedPath = performAbsoluteRemapping(origNormalizedPath,
                    (Map.Entry) i.next());
            if (remappedPath != null)
                return denormalize(remappedPath);
        }

        for (Iterator i = generalizedRemappings.entrySet().iterator(); i
                .hasNext();) {
            String remappedPath = performGeneralizedRemapping(
                    origNormalizedPath, (Map.Entry) i.next());
            if (remappedPath != null)
                return denormalize(remappedPath);
        }

        return origFile;
    }

    static String performAbsoluteRemapping(String path, Map.Entry mapping) {
        return performAbsoluteRemapping(path, (String) mapping.getKey(),
                (String) mapping.getValue());
    }

    static String performAbsoluteRemapping(String origPath, String fromPath,
            String toPath) {
        if (origPath.equalsIgnoreCase(fromPath)
                || origPath.equalsIgnoreCase(toPath))
            return toPath;
        if (origPath.regionMatches(true, 0, fromPath, 0, fromPath.length())
                && origPath.charAt(fromPath.length()) == '/')
            return toPath + origPath.substring(fromPath.length());
        return null;
    }

    static String performGeneralizedRemapping(String path, Map.Entry mapping) {
        return performGeneralizedRemapping(path, (String) mapping.getKey(),
                (String) mapping.getValue());
    }

    static String performGeneralizedRemapping(String origPath,
            String generalizedPath, String toPath) {
        String origLower = origPath.toLowerCase();
        generalizedPath = generalizedPath.toLowerCase();

        if (origLower.endsWith(generalizedPath))
            return toPath;
        int pos = origLower.indexOf(generalizedPath);
        if (pos != -1) {
            int end = pos + generalizedPath.length();
            if (origPath.charAt(end) == '/')
                return toPath + origPath.substring(end);
        }
        return null;
    }

}
