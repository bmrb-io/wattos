/*
 * WattosLib.java
 *
 * Created on August 28, 2003, 8:21 AM
 */

package Wattos.CloneWars;

import java.io.*; 
import java.util.*; 
import Wattos.Utils.*;
import Wattos.Star.NMRStar.*;
import Wattos.Soup.*;
import Wattos.Soup.Constraint.*;

/**
 * Collection of all libraries known by Wattos.
 * @author Jurgen F. Doreleijers
 */
public class WattosLib implements Serializable {
        
    private static final long serialVersionUID = -1207795172754062330L;    
    
    public static final String DEFAULT_DICTIONARY_STAR          = "STAR dictionary";
    public static final String DEFAULT_DICTIONARY_VALIDATOR     = "Validator STAR dictionary";
    public static final String DEFAULT_LIBRARY_PSEUDO_LIB       = "pseudo atom library";
    public static final String DEFAULT_LIBRARY_ATOM_NOMEN_LIB   = "atom nomen library";
    public static final String DEFAULT_LIBRARY_REDUNDANCY       = "redundancy library";
    public static final String DEFAULT_LIBRARY_COMPLETENESS     = "completeness library";
    public static final String DEFAULT_LIBRARY_ATOM_AMBER       = "atom amber library";
    public static final String DEFAULT_LIBRARY_TOPOLOGY         = "topology library";
    public static final String DEFAULT_LIBRARY_ATOM_MAP         = "atommap library";
    public static final String DEFAULT_LIBRARY_ATOM             = "atom library";
    
    public HashMap lib;
    public UserInterface ui;

    /** Convenience variables: */
    public StarDictionary starDictionary    = null;
    public ValidatorDictionary validDictionary = null;
    public PseudoLib pseudoLib              = null;                
    public AtomNomenLib atomNomenLib        = null;                
    public RedundantLib redundantLib        = null;                
    public CompletenessLib completenessLib  = null;                
    public AtomLibAmber atomLibAmber        = null;    
    public TopologyLib topologyLib          = null;    // depends on AtomLibAmber 
    public AtomMap atomMap                  = null;    
    public AtomLib atomLib                  = null;    
    
    /** Creates a new instance of WattosLib */
    public WattosLib(UserInterface ui) {
        this.ui = ui;
        lib = new HashMap();
    }

    public boolean initResources() {
        // STAR dictionary
        starDictionary = new StarDictionary();
        boolean status = starDictionary.readCsvFile(null); // Read default resource file
        if ( ! status ) {
            General.showError("Failed to read the star dictionary");
        }
        lib.put( DEFAULT_DICTIONARY_STAR, starDictionary );
        
        // VALIDATOR dictionary
        validDictionary = new ValidatorDictionary();
        status = validDictionary.readFile(null); // Read default resource file
        if ( ! status ) {
            General.showError("Failed to read the validator dictionary");
        }
        lib.put( DEFAULT_DICTIONARY_VALIDATOR, validDictionary );
        
        //pseudo atom library
        atomNomenLib = new AtomNomenLib();                
        status = atomNomenLib.readStarFile( null ); // read from standard resource
        if (! status) {
            General.showError(" in AtomNomenLib.main found:");
            General.showError(" reading AtomNomenLib star file.");
        }
        lib.put( DEFAULT_LIBRARY_ATOM_NOMEN_LIB, atomNomenLib );        
        
        //pseudo atom library
        pseudoLib = new PseudoLib();                
        status = pseudoLib.readStarFile( null ); // read from standard resource
        if (! status) {
            General.showError(" in PseudoLib.main found:");
            General.showError(" reading PseudoLib star file.");
        }
        lib.put( DEFAULT_LIBRARY_PSEUDO_LIB, pseudoLib );        
        
        //redundancy library
        redundantLib = new RedundantLib();                
        status = redundantLib.readStarFile( null ); // read from standard resource
        if (! status) {
            General.showError(" in RedundantLib.main found:");
            General.showError(" reading RedundantLib star file.");
        }
        lib.put( DEFAULT_LIBRARY_REDUNDANCY, redundantLib );        

        //completeness library
        completenessLib = new CompletenessLib();                
        status = completenessLib.readStarFile( null ); // read from standard resource
        if (! status) {
            General.showError(" in CompletenessLib.main found:");
            General.showError(" reading CompletenessLib star file.");
        }
        lib.put( DEFAULT_LIBRARY_COMPLETENESS, completenessLib );        

        //AtomLibAmber
        atomLibAmber = new AtomLibAmber();                
        status = atomLibAmber.readStarFile( null ); // read from standard resource
        if (! status) {
            General.showError(" in AtomLibAmber.main found:");
            General.showError(" reading AtomLibAmber file.");
        }
        lib.put( DEFAULT_LIBRARY_TOPOLOGY, topologyLib );        

        //TopologyLib library
        topologyLib = new TopologyLib();                
        status = topologyLib.readWIFFile( null, atomLibAmber ); // read from standard resource
        if (! status) {
            General.showError(" in TopologyLib.main found:");
            General.showError(" reading TopologyLib WHAT IF file.");
        }
        lib.put( DEFAULT_LIBRARY_TOPOLOGY, topologyLib );        

        //Atommap library
        atomMap = new AtomMap();                
        status = atomMap.readStarFile( null ); // read from standard resource
        if (! status) {
            General.showError("from AtomMap.readStarFile");
        }
        lib.put( DEFAULT_LIBRARY_ATOM_MAP, atomMap );        

        //Atom library
        atomLib = new AtomLib();                
        status = atomLib.readStarFile( null ); // read from standard resource
        if (! status) {
            General.showError("from atomLib.readStarFile");
        }
        lib.put( DEFAULT_LIBRARY_ATOM, atomLib );        
        return true;
    }
    
    
    /**
     *Main is for testing only.
     * @param args the command line arguments (unused)
     */
    public static void main(String[] args) {
        
        UserInterface ui = new UserInterface();
        String location_bin = "C:\\jurgen\\temp\\WattosLib.bin"; // just for testing.

        WattosLib wattosLib = new WattosLib(ui);
        General.verbosity = General.verbosityDebug;
        
        boolean status = wattosLib.initResources();
        if (! status) {
            General.showError(" in WattosLib.main found:");
            General.showError(" reading WattosLib resources.");
            System.exit(1);
        } else {
            General.showDebug(" done with init of WattosLib resources (the libs).");
        }        
        status = InOut.writeObject( wattosLib, location_bin ); 
        if (! status) {
            General.showError(" in WattosLib.main found:");
            General.showError(" writing WattosLib BIN file.");
            System.exit(1);
        } else {
            General.showDebug(" written WattosLib BIN file.");
        }
        
        wattosLib = (WattosLib) InOut.readObjectOrEOF( location_bin ); 
        if ( wattosLib == null ) {
            General.showError(" in WattosLib.main found:");
            General.showError(" reading WattosLib BIN file.");
            System.exit(1);
        } else {
            General.showDebug(" read WattosLib BIN file.");
        }
        
        if ( true ) {
            General.showOutput("WattosLib:\n" + wattosLib);
        }
        General.showOutput("Done with WattosLib parsing and serializing.");
    }        
}
