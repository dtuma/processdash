// Copyright (C) 2009-2021 Tuma Solutions, LLC
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import net.sourceforge.processdash.InternalSettings;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.i18n.Resources;
import net.sourceforge.processdash.util.StringUtils;

public class DataVersionChecker {

    public static void ensureVersionsOrExit() {
        List<Incompatibility> unsatisfied = checkPackageVersions();
        if (unsatisfied.isEmpty())
            return;

        Resources res = ResourcesHolder.resources;
        StringBuilder upgradeList = new StringBuilder();
        StringBuilder missingList = new StringBuilder();
        for (Incompatibility item: unsatisfied) {
            (item.pkg == null ? missingList : upgradeList).append("\n")
                    .append(item.getBullet());
        }
        String title = res.getString("Title");
        Object[] message = new Object[3];
        message[0] = res.getStrings("Header");
        if (missingList.length() > 0) {
            message[1] = missingList.substring(1).split("\n");
            message[2] = res.getStrings("Missing_Message");
        } else {
            message[1] = upgradeList.substring(1).split("\n");
            message[2] = res.getStrings("Upgrade_Message");
        }
        JOptionPane.showMessageDialog(null, message, title,
            JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static List<Incompatibility> checkPackageVersions() {
        return checkPackageVersions(loadRequirements());
    }

    public static List<Incompatibility> checkPackageVersions(
            Map<String, String> requirements) {
        List<Incompatibility> result = new ArrayList();
        for (Iterator i = requirements.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, String> req = (Map.Entry<String, String>) i
                    .next();
            String packageId = req.getKey();
            String minVersion = req.getValue();
            if (!TemplateLoader.meetsPackageRequirement(packageId, minVersion))
                result.add(new Incompatibility(packageId, minVersion));
        }
        return result;
    }

    public static void registerDataRequirements(String reqts) {
        registerDataRequirements(parseRequirementsString(reqts));
    }

    public static void registerDataRequirements(Map<String, String> reqts) {
        for (Entry<String, String> e : reqts.entrySet())
            registerDataRequirement(e.getKey(), e.getValue());
    }

    public synchronized static void registerDataRequirement(String packageId,
            String version) {
        Map<String, String> requirements = loadRequirements();
        String currentMinVersion = requirements.get(packageId);
        if (currentMinVersion == null ||
                DashPackage.compareVersions(currentMinVersion, version) < 0) {
            requirements.put(packageId, version);
            saveRequirements(requirements);
        }
    }

    private synchronized static Map<String, String> loadRequirements() {
        String reqSetting = Settings.getVal(SETTING_NAME);
        return parseRequirementsString(reqSetting);
    }

    /** @since 2.6.6 */
    public static Map<String, String> parseRequirementsString(
            String reqSetting) {
        Map<String, String> result = new TreeMap<String, String>();
        if (StringUtils.hasValue(reqSetting)) {
            String[] reqSpecs = reqSetting.split(";");
            for (String req : reqSpecs) {
                int sepPos = req.indexOf(REQ_SEP);
                if (sepPos > 0) {
                    String packageId = req.substring(0, sepPos).trim();
                    String version = req.substring(sepPos + REQ_SEP.length())
                            .trim();
                    if (StringUtils.hasValue(packageId)
                            && StringUtils.hasValue(version))
                        result.put(packageId, version);
                }
            }
        }
        return result;
    }

    private synchronized static void saveRequirements(
            Map<String, String> requirements) {
        InternalSettings.set(SETTING_NAME,
            formatRequirementsString(requirements));
    }

    /** @since 2.6.6 */
    public static String formatRequirementsString(
            Map<String, String> requirements) {
        if (requirements == null || requirements.isEmpty())
            return null;

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> req : requirements.entrySet()) {
            String packageId = req.getKey();
            String minVersion = req.getValue();
            result.append(";").append(packageId).append(" ").append(REQ_SEP)
                    .append(" ").append(minVersion);
        }
        return result.substring(1);
    }

    private static class ResourcesHolder {
        private static Resources resources = Resources.getDashBundle(
            "ProcessDashboard.Errors.Version_Incompatibility");
    }


    public static final String SETTING_NAME = "data.versionRequirements";
    private static final String REQ_SEP = "version";
    private static final String BULLET = "    \u2022 ";

    public static class Incompatibility {
        String packageId;
        String minVersion;
        DashPackage pkg;

        private Incompatibility(String packageId, String minVersion) {
            this.packageId = packageId;
            this.minVersion = minVersion;
            this.pkg = TemplateLoader.getPackage(packageId);
        }

        public String getPackageDisplayName() {
            return (pkg != null ? pkg.name : packageId);
        }

        public String getBullet() {
            return BULLET + toString();
        }

        public String toString() {
            return ResourcesHolder.resources.format("Item_FMT",
                getPackageDisplayName(), minVersion);
        }
    }
}
