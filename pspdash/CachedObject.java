// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
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
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  ken.raisor@hill.af.mil

package pspdash;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public abstract class CachedObject implements Serializable {

    private transient ObjectCache objectCache = null;
    private int id;
    private String type;
    protected int challenge;
    protected Date refreshDate = null;
    protected HashMap localAttrs;
    protected byte[] data = null;

    protected transient String errorMessage = null;

    // for use only by the serialization mechanism!!!
    protected CachedObject() {}

    public CachedObject(ObjectCache c, String type) {
        this(c, type, c.getNextID());
    }

    public CachedObject(ObjectCache c, String type, int id) {
        if (c == null) throw new NullPointerException("cache cannot be null");

        this.objectCache = c;
        this.id = id;
        this.type = type;
        this.challenge = (new Random()).nextInt();
    }

    public ObjectCache getCache()   { return objectCache;      }
    public int getID()              { return id;               }
    public String getType()         { return type;             }
    public Date getDate()           { return refreshDate;      }
    public String getErrorMessage() { return errorMessage;     }
    public byte[] getBytes()        { return data;             }
    public String getString()       { return getString(null);  }
    public String getString(String encoding) {
        try {
            if (data == null) return null;
            if (encoding == null) return new String(data);
            return new String(data, encoding);
        } catch (UnsupportedEncodingException uee) { return null; }
    }
    public Object getLocalAttr(String name) {
        if (localAttrs == null) return null;
        return localAttrs.get(name);
    }
    public synchronized void setLocalAttr(String name, Object val) {
        if (localAttrs == null) localAttrs = new HashMap();
        localAttrs.put(name, val);
        store();
    }


    public abstract boolean refresh();

    /** refresh the object if it is older than maxAge days.
     * @return true if the object was already recent enough, or if the
     *    refresh was successful.
     */
    public boolean refresh(double maxAge) {
        return !olderThanAge(maxAge) || refresh();
    }

    public void store(byte[] data) {
        this.data = data;
        store();
    }

    public void store() {
        objectCache.storeCachedObject(this);
    }

    void setCache(ObjectCache o) throws IllegalStateException {
        if (objectCache == null)
            objectCache = o;
        else
            throw new IllegalStateException("cache has already been set");
    }

    public boolean olderThanAge(double maxAge) {
        return olderThanAge(refreshDate, maxAge);
    }

    public static boolean olderThanAge(Date date, double maxAge) {
        if (maxAge < 0) return false;
        if (maxAge == 0 || date == null) return true;
        long ageMillis = System.currentTimeMillis() - date.getTime();
        double ageDays = ageMillis / (double) DAY_MILLIS;
        return ageDays > maxAge;
    }
    private static final long DAY_MILLIS =
        24L /*hours*/ * 60 /*minutes*/ * 60 /*seconds*/ * 1000 /*millis*/;
}
