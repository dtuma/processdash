package net.sourceforge.processdash.util;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllUtilTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(
                "Test for net.sourceforge.processdash.util");
        //$JUnit-BEGIN$
        suite.addTestSuite(StringUtilsTest.class);
        suite.addTestSuite(HTMLTableWriterTest.class);
        suite.addTestSuite(TestFormatUtil.class);
        suite.addTestSuite(TimeNumberFormatTest.class);
        suite.addTestSuite(PatternListTest.class);
        //$JUnit-END$
        return suite;
    }

}
