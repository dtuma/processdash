// Copyright (C) 2000-2006 Tuma Solutions, LLC
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

package net.sourceforge.processdash.data.util;



import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


import net.sourceforge.processdash.data.*;
import net.sourceforge.processdash.data.repository.DataEvent;
import net.sourceforge.processdash.data.repository.DataListener;
import net.sourceforge.processdash.data.repository.DataRepository;
import net.sourceforge.processdash.data.repository.RepositoryListener;
import net.sourceforge.processdash.util.Perl5Util;
import net.sourceforge.processdash.util.PerlPool;


abstract class DataList {

    private static Perl5Util perl = PerlPool.get();
    String dataName = null;
    String prefix = null;
    String customerName = null;
    String re = null;
    DataRepository data;
    Hashtable dataList;
    Handler handler = null;


    /** the Hashtable class does not allow null to be used as either a
     * key or a value.  Since some of our data values may be null, we
     * must therefore wrap them in a non-null object to keep Hashtable
     * happy.
     */
    class DataListValue {
        SimpleData value;
        public DataListValue(SimpleData v) { value = v; }
    }


    /** an object which can do our work for us, and that we can dispose of
     * at will.
     */
    private interface Handler { public void dispose(); }


    DataList(DataRepository rep, String dataName, String prefix, String forWho) {
        /** create a data list that watches for changes in the repository.
            if dataName begins with a ~, it is a pattern to scan rep for.
            all matching names will be found and added to the dataList.
            otherwise, dataName is the name of a single variable to watch.
            in either case, if the pattern or name does not start with a
            forward slash, the prefix will be prepended. */

        // debug("constructor(dataName="+dataName+", prefix="+prefix+", forWho="+forWho+")");

        data = rep;
        dataList = new Hashtable(3);
        this.dataName = dataName;
        this.prefix = prefix;
        customerName = forWho;

        if (dataName.charAt(0) ==  '~')
            if (dataName.indexOf('{') != -1)
                handler = new ConditionWatcher();
            else
                handler = new PatternWatcher();
        else
            handler = new SimpleDataWatcher();

        recalc();
    }


    public abstract void recalc();


    private void debug(String msg) {
        // System.out.println("DataList." + msg);
        // System.err.println("DataList." + msg);
    }

    private String makeNiceName(String name) {
        return name.replace('(', '_').replace(')', '_')
            .replace('[', '_').replace(']', '_');
    }


    public void dispose() {
        // debug("dispose");
        handler.dispose();
        dataList = null;
        handler = null;
        data = null;
        dataName = prefix = customerName = null;
    }




    /** Do the work involved with watching a single, simple data element.
     *  This simply involves registering as a datalistener for the element.
     */
    private class SimpleDataWatcher implements Handler, DataListener {

        SimpleDataWatcher() {
            dataName = DataRepository.createDataName(prefix, dataName);
            dataList.put(dataName, new DataListValue(null)); // needed?
            data.addActiveDataListener(dataName, this, customerName);
        }

        private void handleDataEvent(DataEvent e) {
            if (e.getName().equals(dataName)) // needed?
                dataList.put(dataName, new DataListValue(e.getValue()));
            else
                System.err.println
                    ("SimpleDataWatcher.dataValueChanged called unnecesarily!");
        }

        public void dataValuesChanged(Vector v) {
            if (v == null || v.size() == 0) return;
            for (int i = v.size();  i > 0; )
                handleDataEvent((DataEvent) v.elementAt(--i));
            recalc();
        }

        public void dataValueChanged(DataEvent e) { handleDataEvent(e); recalc(); }
        public void dispose() { data.removeDataListener(dataName, this); }

        private void debug(String msg)
            { DataList.this.debug("SimpleDataWatcher."+msg); }
    }



    /** Do the work involved with watching the repository for data that
     *  matches a simple pattern.  This involves registering as a repository
     *  listener and registering for appropriate elements that come along.
     */
    private class PatternWatcher implements Handler, DataListener,
        RepositoryListener {

        PatternWatcher() {
            dataName = dataName.substring(1);         // remove "~" from beginning
            String prefixUsed = null;

            // if the dataName starts with "/", prepend "^" for completeness.
            if (dataName.charAt(0) == '/')
                re = "^" + dataName;

            // if the dataName already starts with "^", nothing needs to be added.
            else if (dataName.charAt(0) == '^')
                re = dataName;

            // otherwise, we need to add the prefix to the beginning.
            else
                re = "^"+ ValueFactory.regexpQuote(prefixUsed=prefix) +"/"+ dataName;

            // if the regexp doesn't end with a $, add one.
            if (!re.endsWith("$"))
                re = re + "$";

            // convert the regexp into a perl pattern match.
            re = "m\n" + re + "\ns";

            data.addRepositoryListener(this, prefixUsed);
        }

        private void handleDataEvent(DataEvent e) {
            if (dataList.containsKey(e.getName()))
                dataList.put(e.getName(), new DataListValue(e.getValue()));
            else
                System.err.println
                    ("PatternWatcher.dataValueChanged called unnecesarily!!!\n");
        }

        public void dataValueChanged(DataEvent e) {
            handleDataEvent(e);
            recalc();
        }

        public void dataValuesChanged(Vector v) {
            if (v == null || v.size() == 0) return;
            for (int i = v.size();  i > 0; )
                handleDataEvent((DataEvent) v.elementAt(--i));
            recalc();
        }


        public void dataAdded(String name) {
            try {
                if (perl.match(re, name)) {
                    dataList.put(name, new DataListValue(null)); // needed?
                    data.addActiveDataListener(name, this, customerName);
                }
            } catch (Perl5Util.RegexpException m) {
                System.err.println("RegexpException: " + re);
                re = null;
            }
        }

        public void dataRemoved(String name) {
            if (dataList.remove(name) != null) {
                data.removeDataListener(name, this);
                recalc();
            }
        }

        public void dispose() {
            data.removeRepositoryListener(this);
            data.deleteDataListener(this);
            re = null;
        }

        private void debug(String msg)
            { DataList.this.debug("PatternWatcher."+msg); }
    }


    /** Do the work involved with watching the repository for data that
     *  matches a pattern containing conditional expressions.  This
     *  involves registering as a repository listener and creating data
     *  elements to track values of conditional expressions.
     */
    private class ConditionWatcher implements Handler, DataListener,
        RepositoryListener {

        String condition = null;    // the conditional expression
        Hashtable condDataList = null;
        Hashtable dataWatchers = null;

        ConditionWatcher() {
            condDataList = new Hashtable();
            dataWatchers = new Hashtable();

            StringBuffer condExp = new StringBuffer("![(&&");
            String prefixUsed = null;

            dataName = dataName.substring(1);         // remove "~" from beginning

            // if the dataName starts with "^", remove it for now.
            if (dataName.charAt(0) == '^')
                re = dataName.substring(1);

            // if the dataName starts with "/", nothing needs to be added.
            else if (dataName.charAt(0) == '/')
                re = dataName;

            // otherwise, we need to add the prefix to the beginning.
            else
                re = ValueFactory.regexpQuote(prefixUsed = prefix) + "/" + dataName;

            int num_conditions = ArgumentList.countChars(re.toCharArray(), '{');

            int openBracePos, closeBracePos;
            String condPart;
            while ((openBracePos = re.indexOf('{')) != -1) {
                closeBracePos = re.indexOf('}', openBracePos);

                                         // retrieve the {conditional} part.
                condPart = re.substring(openBracePos+1, closeBracePos);

                                         // remove the {conditional} part and put
                                         // grouping parentheses around the preceeding
                                         // part.
                re = ("(" + re.substring(0,openBracePos) + ")" +
                      re.substring(closeBracePos+1));

                if (condPart.indexOf('\t') == -1)
                    condExp.append(addPrefixToVariable(condPart, num_conditions));
                else {
                    ArgumentList args = new ArgumentList("[(" + condPart + ")]");
                    while (args.hasMoreElements())
                        condExp.append(addPrefixToVariable(args.nextElement(),
                                                           num_conditions));
                }
                num_conditions--;
            }
            condExp.append(")]");

            // if the regexp doesn't end with a $, add one.
            if (!re.endsWith("$"))
                re = re + "$";

            condition = "s\n^" + re +  "\n" + condExp.toString() + "\ns";

            re = "m\n^" + re + "\ns";

            data.addRepositoryListener(this, prefixUsed);
        }

        private String addPrefixToVariable(String arg, int number) {
            if (arg.startsWith("[(") ||                       // function operator
                arg.startsWith("\"") ||                       // string literal
                arg.startsWith("@")  ||                       // date literal
                arg.startsWith("/")  ||                       // absolute path var
                arg.startsWith("~/") ||                       // abs. path pattern
                arg.startsWith("~^") ||                       // abs. path pattern
                "0123456789-+.".indexOf(arg.charAt(0)) != -1) // number literal
                return "\t" + arg;
            else                              // this is a relative variable name.
                return "\t$" + number + arg;    // prepend $number
        }

        private boolean isActiveDataElement(String name) {
            return (dataList.containsKey(name));
        }

        private boolean isCondition(String name) {
            return (dataWatchers.containsKey(name));
        }

        private boolean handleDataEvent(DataEvent e) {

            String name = e.getName();
            DataListValue val = new DataListValue(e.getValue());

                                              // store this value in our internal
            condDataList.put(name, val);      // data list (condDataList)

                                              // if this data element is already
            if (isActiveDataElement(name)) {  // in the master data list,
                dataList.put(name, val);        // update its value there, and
                return true;                    // signal the need to recalculate.

            } else if (isCondition(name)) {   // if this data element is a condition,

                                      // interpret it as a boolean value.
                DoubleData conditionValue = (DoubleData) e.getValue();
                boolean conditionIsTrue = (conditionValue != null &&
                                           conditionValue.test());

                                        // find the data Element it turns on or off.
                String dataName = (String) dataWatchers.get(name);
                boolean conditionWasTrue = isActiveDataElement(dataName);

                if (conditionIsTrue) {
                    if (!conditionWasTrue)
                        data.addActiveDataListener(dataName, this, customerName);
                    dataList.put(dataName, condDataList.get(dataName));
                } else {
                    dataList.remove(dataName);
                    if (conditionWasTrue)
                        data.removeDataListener(dataName, this);
                }

                                        // the set of active data has now changed, so
                return true;            // we need to recalculate.
            }
                                      // if we made it to here, the data element
                                      // was a regular, non-active element, and
            return false;             // thus we do not need to recalculate.
        }


        public void dataValueChanged(DataEvent e) {
            boolean needToRecalc = handleDataEvent(e);

            if (needToRecalc) recalc();
        }

        public void dataValuesChanged(Vector v) {
            boolean needToRecalc = false;
            if (v == null || v.size() == 0) return;
            for (int i = v.size();  i-- > 0; )
                if (handleDataEvent((DataEvent) v.elementAt(i)))
                    needToRecalc = true;

            if (needToRecalc) recalc();
        }


        public void dataAdded(String dataName) {
            String condExpr = null;
            try {
                synchronized (perl) {
                    if (!perl.match(re, dataName))
                        return;

                    // use pattern substitution to compute expression for conditional
                    condExpr = perl.substitute(condition, dataName);
                }

                // System.out.println("dataAdded():");
                // System.out.println("   customer = '" + customerName + "'");
                // System.out.println("   dataName = '" + dataName + "'");
                // System.out.println("   condition = '" + condExpr +"'");
                // System.out.println("");

                // get a new data name for the conditional expression
                String dataCondName = DataRepository.anonymousPrefix + "_Conditional/" +
                    makeNiceName(condExpr);

                // add the dataCondName -> dataName pair to watched data
                dataWatchers.put(dataCondName, dataName);

                // add dataName to the condDataList with null value
                condDataList.put(dataName, new DataListValue(null));

                // add dataCondName to the condDataList with DoubleData 0
                condDataList.put(dataCondName, new DataListValue(new DoubleData(0)));

                // create conditional function in repository
                data.maybeCreateValue(dataCondName, condExpr, "");

                // addActiveDataListener for dataCondName
                data.addActiveDataListener(dataCondName, this, customerName);
            } catch (Perl5Util.RegexpException m) {
                System.err.println("RegexpException: " + re);
                re = null;
            }
        }


        /** Find the name of the conditional data element that acts as gatekeeper
         *  for the given dataName.
         */
        private String lookupConditionName(String dataName) {

            // perform a reverse lookup in the dataWatchers hashtable.  That is,
            // find the key whose value is dataName.

            Enumeration conditionNames = dataWatchers.keys();
            String dataCondName;
            while (conditionNames.hasMoreElements()) {
                dataCondName = (String) conditionNames.nextElement();
                if (((String) dataWatchers.get(dataCondName)).equals(dataName))
                    return dataCondName;
            }
            return null;
        }

        public void dataRemoved(String dataName) {
            String dataCondName;
            if (condDataList.remove(dataName) != null) {
                data.removeDataListener(dataName, this);
                dataCondName = lookupConditionName(dataName);
                if (dataCondName != null) {
                    condDataList.remove(dataCondName);
                    data.removeDataListener(dataCondName, this);
                }
                recalc();
            }
        }

        public void dispose() {
            data.removeRepositoryListener(this);
            data.deleteDataListener(this);
            condDataList = null;
            dataWatchers = null;
            re = condition = null;
        }

        private void debug(String msg)
            { DataList.this.debug("ConditionWatcher."+msg); }
    }
}
