/*
 * UserInterfaceTest.java
 * JUnit based test
 *
 * Created on November 30, 2005, 1:58 PM
 */

package Wattos.CloneWars;

import junit.framework.*;

/**
 *
 * @author jurgen
 */
public class UserInterfaceTest extends TestCase {
    
    public UserInterfaceTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(UserInterfaceTest.class);
        
        return suite;
    }

    /**
    public void testEnsurePresenceUserDefs() {
        boolean expResult = true;
        boolean result = UserInterface.ensurePresenceUserDefs();
        assertEquals(expResult, result);
    }

    public void testReadProperties() {
        UserInterface instance = new UserInterface();
        
        boolean expResult = true;
        boolean result = instance.readProperties();
        assertEquals(expResult, result);
    }

    public void testInit() {
        boolean testing = true;
        
        UserInterface expResult = null;
        UserInterface result = UserInterface.init(testing);
        assertEquals(expResult, result);
    }

     */
    public void testInitResources() {
        //General.verbosity = General.verbosityNothing;
        UserInterface instance = UserInterface.init(true);
        if ( instance == null ) {
            fail( "Failed to instantiate UI");
        }
    }

    /*
    public void testShowUsage() {
        String str = "";
        
        UserInterface.showUsage(str);
    }

    public void testDoNonGuiMain() {
        UserInterface instance = new UserInterface();
        
        boolean expResult = true;
        boolean result = instance.doNonGuiMain();
        assertEquals(expResult, result);
    }

    public void testDoGuiMain() {
        UserInterface instance = new UserInterface();
        
        boolean expResult = true;
        boolean result = instance.doGuiMain();
        assertEquals(expResult, result);
    }

    public void testMain() {
        String[] args = null;
        
        UserInterface.main(args);
    }
    
     */
}
