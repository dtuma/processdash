package net.sourceforge.processdash.util;

import net.sourceforge.processdash.util.glob.GlobEngineTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllUtilTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(
                "Test for net.sourceforge.processdash.util");
        //$JUnit-BEGIN$
        suite.addTestSuite(ThreeWayDiffTest.class);
        suite.addTestSuite(HashTreeTest.class);
        suite.addTestSuite(StringUtilsTest.class);
        suite.addTestSuite(HTMLTableWriterTest.class);
        suite.addTestSuite(TestFormatUtil.class);
        suite.addTestSuite(TimeNumberFormatTest.class);
        suite.addTestSuite(PatternListTest.class);
        suite.addTestSuite(PreferencesUtilTest.class);
        suite.addTestSuite(GlobEngineTest.class);
        suite.addTestSuite(OrderedListMergerTest.class);
        suite.addTestSuite(FallbackObjectFactoryTest.class);
        suite.addTestSuite(RuntimeUtilsTest.class);
        //$JUnit-END$
        return suite;
    }

}
