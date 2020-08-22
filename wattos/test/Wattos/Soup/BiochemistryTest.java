package Wattos.Soup;

import Wattos.Utils.General;
import junit.framework.TestCase;

public class BiochemistryTest extends TestCase {

    public void testIsWCBond() {
        General.setVerbosityToDebug();
        assertTrue( Biochemistry.isWCBond("N4","C", "O6","G"));
        assertFalse(Biochemistry.isWCBond("C","N4", "G","O6")); // bad order
        assertFalse(Biochemistry.isWCBond("N5","C", "O6","G")); // N5 is bad atom name
        assertTrue( Biochemistry.isWCBond("O6","G", "N4","C")); // switched donor/acceptor
        assertTrue( Biochemistry.isWCBond("N3","T", "N1","A"));
        assertTrue( Biochemistry.isWCBond("N3","U", "N1","A"));
        assertTrue( Biochemistry.isWCBond("N1","G", "N3","C"));
    }
}
