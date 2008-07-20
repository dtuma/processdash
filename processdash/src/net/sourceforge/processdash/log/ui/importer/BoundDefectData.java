// Copyright (C) 2007 Tuma Solutions, LLC
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

package net.sourceforge.processdash.log.ui.importer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

import net.sourceforge.processdash.log.defects.DefectDataBag;
import net.sourceforge.processdash.ui.lib.binding.BoundMap;
import net.sourceforge.processdash.ui.lib.binding.ErrorData;
import net.sourceforge.processdash.util.StringMapper;

public class BoundDefectData extends DefectDataBag {

    private static final String ID = BoundDefectData.class.getName();

    public static BoundDefectData getDefectData(BoundMap map) {
        BoundDefectData result = (BoundDefectData) map.get(ID);
        if (result == null) {
            result = new BoundDefectData(map);
            map.put(ID, result);
        }
        return result;
    }


    BoundMap form;

    private ErrorData errorData;

    String[] mapperIDs;

    public BoundDefectData(BoundMap form) {
        this.form = form;

        bindMappers();
        bindDefects();
    }

    public ErrorData getErrorData() {
        return errorData;
    }





    private void bindDefects() {
        form.addPropertyChangeListener("defects", this, "updateDefects");
        updateDefects();
    }

    public void updateDefects() {
        ErrorData error = form.getErrorDataForAttr("defects");
        if (error != null) {
            this.errorData = error;
            setDefectData(Collections.EMPTY_LIST);
        } else {
            Object newValue = form.get("defects");
            this.errorData = null;
            if (newValue instanceof List)
                setDefectData((List) newValue);
            else
                setDefectData(Collections.EMPTY_LIST);
        }
    }



    private void bindMappers() {
        PropertyChangeListener l = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateMapper(evt);
            }
        };

        mapperIDs = new String[ATTRS.length];
        for (int i = 0; i < mapperIDs.length; i++) {
            String mapperId = getMapperId(i);
            mapperIDs[i] = mapperId;
            form.addPropertyChangeListener(mapperId, l);
            setStringMapper(i, (StringMapper) form.get(mapperId));
        }
    }

    private static final String MAPPER_ID_PREFIX = "DefectMapper.";

    public static String getMapperId(int forColumn) {
        return MAPPER_ID_PREFIX + ATTRS[forColumn];
    }

    public void updateMapper(PropertyChangeEvent event) {
        String propName = event.getPropertyName();
        for (int i = 0; i < mapperIDs.length; i++) {
            if (mapperIDs[i].equals(propName)) {
                setStringMapper(i, (StringMapper) event.getNewValue());
                return;
            }
        }
    }
}
