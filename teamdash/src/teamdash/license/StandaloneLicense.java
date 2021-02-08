// Copyright (C) 2021 Tuma Solutions, LLC
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

package teamdash.license;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sourceforge.processdash.util.XMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class StandaloneLicense {

    private String xmlText;

    private Element xml;

    private boolean valid;

    StandaloneLicense(String xmlText) throws StandaloneLicenseException {
        this.xmlText = xmlText;
        this.xml = parseXmlText();
        this.valid = StandaloneLicenseValidator.isValid(this);
    }

    public String getXmlText() {
        return xmlText;
    }

    public boolean isValid() {
        return this.valid;
    }

    /**
     * @return the textual representation of the digital signature
     */
    public String getSignature() {
        return getValue(SIGNATURE_TAG);
    }

    public boolean isTrial() {
        return getValue("trial") != null;
    }

    public Date getExpirationDate() {
        return getDate("expires");
    }


    /** parse the XML fragment that makes up this license */
    private Element parseXmlText() throws StandaloneLicenseException {
        Element result;
        try {
            result = XMLUtils.parse(xmlText).getDocumentElement();
        } catch (Exception e) {
            StandaloneLicenseException sle = new StandaloneLicenseException(
                    xmlText);
            sle.initCause(e);
            throw sle;
        }

        if (!"wbsLicense".equals(result.getTagName()))
            throw new StandaloneLicenseException(xmlText);

        return result;
    }

    /**
     * Find a named tag within the license document and return it.
     * 
     * @param tagNames
     *            the names of the tags. These should uniquely identify a set of
     *            nested tags within the document.
     * @return the document element whose path corresponds to those tag names.
     */
    private Element getElement(String... tagNames) {
        Element result = xml;
        for (String tag : tagNames) {
            NodeList matches = result.getElementsByTagName(tag);
            if (matches == null || matches.getLength() != 1)
                return null;
            result = (Element) matches.item(0);
        }
        return result;
    }

    public String getValue(String... tagNames) {
        Element e = getElement(tagNames);
        return getTextContents(e);
    }

    public Map<String, String> getValues(String... tagNames) {
        Element e = getElement(tagNames);
        if (e == null)
            return null;

        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Element item : XMLUtils.getChildElements(e)) {
            result.put(item.getTagName(), getTextContents(item));
        }
        return result;
    }

    private String getTextContents(Element e) {
        if (e == null) {
            return null;
        } else {
            String text = XMLUtils.getTextContents(e);
            return (text == null ? "" : text);
        }
    }

    private Map<String[], Date> dateCache = new HashMap<String[], Date>();

    public Date getDate(String... tagNames) {
        if (dateCache.containsKey(tagNames))
            return dateCache.get(tagNames);

        Date result = getDateImpl(tagNames);
        dateCache.put(tagNames, result);
        return result;
    }

    private Date getDateImpl(String... tagNames) {
        String dateStr = getValue(tagNames);
        if (dateStr == null || dateStr.trim().length() == 0)
            return null;

        try {
            synchronized (DATE_FMT) {
                return DATE_FMT.parse(dateStr);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private DateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof StandaloneLicense) {
            StandaloneLicense that = (StandaloneLicense) obj;
            return this.xmlText.equals(that.xmlText);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return xmlText.hashCode();
    }

    static final String SIGNATURE_TAG = "esig";

}
