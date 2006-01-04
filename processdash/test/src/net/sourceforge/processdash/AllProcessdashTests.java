package net.sourceforge.processdash;

import net.sourceforge.processdash.log.time.AllTimeLogTests;
import net.sourceforge.processdash.util.AllUtilTests;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllProcessdashTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for Process Dashboard");
		//$JUnit-BEGIN$
		suite.addTest(AllUtilTests.suite());
		suite.addTest(AllTimeLogTests.suite());
		//$JUnit-END$
		return suite;
	}

}
