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

    static Perl5Util perl;
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
    };


    static {

        perl = new Perl5Util();

        functionConstructors = new Hashtable();

        try {
            Class
                string = Class.forName("java.lang.String"),
                data   = Class.forName("pspdash.data.DataRepository");
            Class[] parameterTypes = {string, string, string, data, string};

            String operatorString = null, className = null;
            Class functionClass;
            Constructor constructor;

            for (int i=availableFunctions.length; i-- > 0; ) try {
                operatorString = (String)availableFunctions[i][0];
                className      = "pspdash.data." + (String)availableFunctions[i][1];

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
        } catch (ClassNotFoundException e) {
            System.err.println("Unable to find class: " + e);
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

        if ("null".equals(value))
            return null;
        else try {
            switch (value.charAt(0)) {
            case '@': return new DateData(value);
            case '"': return new StringData(value);
            case '!': return parseFunction(name, value, r, prefix);
            default:  return new DoubleData(value);
        } } catch (MalformedValueException mve) {
            System.err.println("Malformed value: " + prefix + name +
                               " = " + value);
            throw mve;
        }

    }



    public static SaveableData createQuickly
        (String name, String value, DataRepository r, String prefix)
        throws MalformedValueException {

        if (value.charAt(0) == '!')
            return new DeferredData(name, value, r, prefix);
        else
            return createSimple(value);
    }



    public static SimpleData createSimple(String value) throws
        MalformedValueException {

        if ("null".equals(value))
            return null;
        else
            switch (value.charAt(0)) {
            case '@': return new DateData(value);
            case '"': return new StringData(value);
            case '?':
                if (value.length() > 1) switch (value.charAt(1)) {
                case '#': return UndefinedDouble.NaN;
                case '0': return UndefinedDouble.ZERO;
                default:  return new UndefinedDouble(value);
                }
                throw new MalformedValueException();

            default:  return new DoubleData(value);
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
            throw new MalformedValueException();
        }

    }


    private static synchronized SaveableData parseFunction
        (String name, String value, DataRepository r, String prefix)
        throws MalformedValueException {

        String isSimpleFunction = "m\n^" + simpleFunction + "$\n";
        String containsSimpleFunction = "m\n" + simpleFunction + "\n";

        String expression = value.substring(1);    // remove initial "!"
        expression = perl.substitute("s/\\[\\(/" + FB + "/g", expression);
        expression = perl.substitute("s/\\)\\]/" + FE + "/g", expression);

        String pre, func, post, tempname;

        while (! perl.match(isSimpleFunction, expression)) {
            try {
                if (!perl.match(containsSimpleFunction, expression))
                    throw new MalformedValueException();
            } catch (MalformedPerl5PatternException e) {
                throw new MalformedValueException();
            }

            pre = perl.preMatch();
            func = perl.group(1);
            post = perl.postMatch();

            tempname = r.makeUniqueName(r.anonymousPrefix + "_Function");
            try {
                parseSimpleFunc(tempname, func, null, r, prefix);
            } catch (MalformedValueException e) {
                r.removeValue(tempname);
                throw new MalformedValueException();
            }
            expression = pre + tempname + post;

        }

        expression = perl.group(1);
        return parseSimpleFunc(name, expression, value, r, prefix);
    }


    private static void debug(String s) {
        System.out.println("ValueFactory: "+s);
    }


}
