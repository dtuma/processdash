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

package pspdash.data;

import pspdash.ResourcePool;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import com.oroinc.text.perl.Perl5Util;
import com.oroinc.text.perl.MalformedPerl5PatternException;

                                // We do not explicitly use these classes, but
                                // if we do not import them, they never get
                                // compiled.
import pspdash.data.AdditionFunction;
import pspdash.data.SubtractionFunction;
import pspdash.data.MultiplicationFunction;
import pspdash.data.DivisionFunction;
import pspdash.data.AndFunction;
import pspdash.data.OrFunction;
import pspdash.data.NotFunction;
import pspdash.data.EqualsFunction;
import pspdash.data.LessThanFunction;
import pspdash.data.GreaterThanFunction;
import pspdash.data.LessThanOrEqualsFunction;
import pspdash.data.GreaterThanOrEqualsFunction;
import pspdash.data.NotEqualsFunction;



class ValueFactory {

    public static ResourcePool perlPool;
    static final String doublePattern =
        "m\n^-?\\d+\\.\\d+([eE](\\+|-)\\d+)?|-?NaN|-?Infinity$\n";
    static final String integerPattern = "m\n^[-+]?[0-9]+$\n";
    static final String datePattern = "m\n^@[-+]?[0-9]+$\n";
    static final String functionPattern = "m\n^!\\[\\(\n";

    static final char FB = '\u0000';    // function begin
    static final char FE = '\u0001';    // function end
    static final String simpleFunction = FB + "([^" + FB + FE + "]+)" + FE;

    static Hashtable functionConstructors;

    static final String[][] availableFunctions =
    { { "+",  "AdditionFunction" },
        { "-",  "SubtractionFunction" },
        { "*",  "MultiplicationFunction" },
        { "/",  "DivisionFunction"},
        { "&&", "AndFunction" },
        { "||", "OrFunction" },
        { "!",  "NotFunction" },
        { "=",  "EqualsFunction" },
        { "==", "EqualsFunction" },
        { "<",  "LessThanFunction" },
        { ">",  "GreaterThanFunction" },
        { "<=", "LessThanOrEqualsFunction" },
        { ">=", "GreaterThanOrEqualsFunction" },
        { "!=", "NotEqualsFunction" },
        { "<>", "NotEqualsFunction" },
        { "#=", "NumberAliasFunction" },
        { "\"=","StringAliasFunction" },
    };


    static {

        perlPool = new ResourcePool() {
                protected Object createNewResource() {
                    return new Perl5Util();
                }
            };

        functionConstructors = new Hashtable();

        Class
            string = java.lang.String.class,
            data   = pspdash.data.DataRepository.class;
        Class[] parameterTypes = {string, string, string, data, string};

        String operatorString = null, className = null;
        Class functionClass;
        Constructor constructor;

        for (int i=availableFunctions.length; i-- > 0; ) try {
            operatorString = (String)availableFunctions[i][0];
            className      = "pspdash.data." +(String)availableFunctions[i][1];

            functionClass = Class.forName(className);
            constructor = functionClass.getConstructor(parameterTypes);

            functionConstructors.put(operatorString, constructor);

        } catch (ClassNotFoundException e) {
            System.err.println("Could not find class "+className);
        } catch (NoSuchMethodException e) {
            System.err.println
                ("Could not find constructor for class "+className);
        } catch (Exception e) {
            System.err.println("Could not map operator " + operatorString);
        }
    }



    public static String regexpQuote(String s) {
        final String metachars = ".[]\\()?*+{}|^$";

        StringBuffer result = new StringBuffer();
        int length = s.length();
        char c ;

        for (int i=0;   i < length;   i++) {
            c = s.charAt(i);
            if (metachars.indexOf(c) == -1)
                result.append(c);
            else {
                result.append('\\');
                result.append(c);
            }
        }

        return result.toString();
    }



    public static SaveableData create
        (String name, String value, DataRepository r, String prefix)
        throws MalformedValueException {

        if (value.charAt(0) == '!') {
            try {
                return parseFunction(name, value, r, prefix);
            } catch (MalformedValueException mve) {
                System.err.println("Malformed value: " + name + " = " + value);
                if (mve.getMessage() != null)
                    System.err.println("-----error was: " + mve.getMessage());
                throw mve;
            }
        } else
            return createQuickly(name, value, r, prefix);
    }

    private static char flagChar(String s, int pos) {
        char result = s.charAt(pos);
        if (result == '?') result = s.charAt(pos+1);
        return result;
    }


    public static SaveableData createQuickly
        (String name, String value, DataRepository r, String prefix)
        throws MalformedValueException {

        if (value.charAt(0) == '!')
            return new DeferredData(name, value, r, prefix);
        else if (value.charAt(0) == '#')
            return createFrozen(name, value, r, prefix);
        else
            return createSimple(value);
    }



    public static SimpleData createSimple(String value) throws
        MalformedValueException {

        if ("null".equals(value))
            return null;
        else {
            switch (flagChar(value, 0)) {
            case '@': return new DateData(value);
            case '"': return new StringData(value);
            case 'T': return TagData.getInstance();
            default:  return new DoubleData(value);
            }
        }
    }



    public static SimpleData createFrozen
        (String name, String value, DataRepository r, String prefix)
        throws MalformedValueException {

        switch (flagChar(value, 1)) {
        case '@': return new FrozenDate(name, value, r, prefix);
        case '"': return new FrozenString(name, value, r, prefix);
        default:  return new FrozenDouble(name, value, r, prefix);
        }
    }



    private static SaveableData parseSimpleFunc(String n, String v, String s,
                                                DataRepository r, String p)
        throws MalformedValueException {

        try {
                // get the operator from the beginning of the string
            int initialTabPos = v.indexOf('\t');
            String operatorString = v.substring(0, initialTabPos);

                                  // lookup the constructor for that operator
            Constructor constructor =
                (Constructor)functionConstructors.get(operatorString);

                                    // create the simple function object
            Object[] parameters = {n, v.substring(initialTabPos+1), s, r, p};
            return (SaveableData) (constructor.newInstance(parameters));

        } catch (Exception e) {
            //e.printStackTrace();
            //debug ("parseSimpleFunc("+n+","+v+","+s+","+p+")");
            throw new MalformedValueException(e + " in parseSimpleFunc()");
        }

    }


    private static SaveableData parseFunction
        (String name, String value, DataRepository r, String prefix)
        throws MalformedValueException {

        SaveableData result = null;
        Perl5Util perl = (Perl5Util) perlPool.get();
        try {
            String isSimpleFunction = "m\n^" + simpleFunction + "$\n";
            String containsSimpleFunction = "m\n" + simpleFunction + "\n";

            String expression = value.substring(1);    // remove initial "!"
            expression = perl.substitute("s/\\[\\(/" + FB + "/g", expression);
            expression = perl.substitute("s/\\)\\]/" + FE + "/g", expression);

            String pre, func, post, tempname;

            while (! perl.match(isSimpleFunction, expression)) {
                try {
                    if (!perl.match(containsSimpleFunction, expression))
                        throw new MalformedValueException
                            ("mismatched parentheses");
                } catch (MalformedPerl5PatternException e) {
                    throw new MalformedValueException(e.toString());
                }

                pre = perl.preMatch();
                func = perl.group(1);
                post = perl.postMatch();

                tempname = getAnonymousFunctionName();
                //r.makeUniqueName(r.anonymousPrefix + "_Function");
                try {
                    parseSimpleFunc(tempname, func, null, r, prefix);
                } catch (MalformedValueException e) {
                    r.removeValue(tempname);
                    throw e;//new MalformedValueException();
                }
                expression = pre + tempname + post;

            }

            expression = perl.group(1);
            result = parseSimpleFunc(name, expression, value, r, prefix);
        } finally {
            perlPool.release(perl);
        }
        return result;
    }

    private static int anonymous_function_counter = 0;
    private static final Object anonymous_function_counter_lock = new Object();

    private static String getAnonymousFunctionName() {
        int num;
        synchronized (anonymous_function_counter_lock) {
            num = anonymous_function_counter++;
        }
        return DataRepository.anonymousPrefix + "_Function" + num;
    }


    private static void debug(String s) {
        System.out.println("ValueFactory: "+s);
    }


}
