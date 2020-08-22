/*
 * CommandHub.java
 *second mod after cvs checkout.
 * Created on August 7, 2003, 10:30 AM
 */

package Wattos.CloneWars;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.BitSet;
import java.util.Date;
import java.util.Stack;

import Wattos.Database.DBMS;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Episode_II.Globals;
import Wattos.Soup.Atom;
import Wattos.Soup.AtomMap;
import Wattos.Soup.Entry;
import Wattos.Soup.Gumbo;
import Wattos.Soup.Model;
import Wattos.Soup.Molecule;
import Wattos.Soup.Residue;
import Wattos.Soup.Constraint.AssignStereo;
import Wattos.Soup.Constraint.Completeness;
import Wattos.Soup.Constraint.Surplus;
import Wattos.Star.STARFilter;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.Strings;

/**
 *The UI's center for the commands it can execute.
 * @author Jurgen F. Doreleijers
 */
public class CommandHub implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    /** The user interface has access to all data */
    public UserInterface ui;

    public String currentCommandName;

    /** BEGIN BLOCK */
    public DBMS         dbms;
    public Gumbo        gumbo;
    public Atom         atom;
    public Residue      res;
    public Molecule     mol;
    public Model        model;
    public Entry        entry;
    public Relation     atomMain;
    public Relation     resMain;
    public Relation     molMain;
    public Relation     modelMain;
    public Relation     entryMain;
    /** END BLOCK */

    /** Will contain a list of input streams to handle before returning from
     *ExecuteScript
     */
    private Stack inputs = new Stack();

    private boolean originalInteractive;

    /** Creates a new instance of CommandHub */
    public CommandHub(UserInterface ui, BufferedReader in) {
        this.ui = ui;
        dbms=ui.dbms;
        initConvenienceVariables();
    }

    /** BEGIN BLOCK FOR SETTING LOCAL CONVENIENCE VARIABLES COPY FROM Wattos.Soup.PdbFile */
    public boolean initConvenienceVariables() {

        atomMain = dbms.getRelation( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_MAIN_RELATION_NAME] );
        if ( atomMain == null ) {
            General.showError("failed to find the atom main relation");
            return false;
        }
        atom = (Atom) atomMain.getRelationSetParent();
        if ( atom == null ) {
            General.showError("failed to find atom RelationSet");
            return false;
        }
        gumbo = (Gumbo) atom.getRelationSoSParent();
        if ( gumbo == null ) {
            General.showError("failed to find the gumbo RelationSoS");
            return false;
        }
        atom    = gumbo.atom;
        res     = gumbo.res;
        mol     = gumbo.mol;
        model   = gumbo.model;
        entry   = gumbo.entry;
        return true;
    }
    /** END BLOCK */

    /**
     * This will be a list of commands to do
     */
    public boolean invoke(int idxCommand) {
        // Get the correct cased name
        currentCommandName = (String) ui.mainMenu.mainMenuCommandNames.get(idxCommand);
        try {
            if ( ui.interactiveSession ) {
                General.showDebug( "Executing: " + currentCommandName );
            }

            Class thisClass = this.getClass();
            Method commandMethod = thisClass.getMethod( currentCommandName, new Class[]{});
            Object result = commandMethod.invoke( this, new Object[] {});
            return ((Boolean) result).booleanValue();

        } catch ( NoSuchMethodException nsme ) {
            if ( ui.mainMenu.containsSubMenuInAny(currentCommandName) ) {
                General.showError("can't execute sub menu with name: " + currentCommandName );
                return false;
            }
            General.showError("No method with command name: " + currentCommandName);
            General.showThrowable(nsme);
        // Catch any exception
        } catch ( Exception e ) {
            General.showError("Error invoking reflected command: " + currentCommandName);
            General.showThrowable(e);
        }
        return false;
    }

    /**
     *<UL>
     *<LI>All methods below have to be spelled exactly the same
     *as they occur in the xls definition file. That file also contains
     *documentation that might be absent here. No need to duplicate
     *the effort over and over.
     *
     *<LI>The order of the commands is kept like the definition file
     *but is only important for human readibility.
     *</UL>
     */

    public boolean End() {
        General.showCodeBug("Command: "+currentCommandName+" should have been caught and executed in main ui.");
        return false;
    }


    /** Nulls all objects in the ui */
    public boolean InitAll() {
        General.showOutput( "Doing InitAll");
        return ui.initResources();
    }

    /** Show the memory used. */
    public boolean ShowMemory() {
        General.showOutput( "Doing ShowMemory");
        General.showMemoryUsed();
        return true;
    }

    /** Show the current time in ms and regular. */
    public boolean ShowTime() {
        Date now = new java.util.Date();
        if ( ui.lastTimeShown == null ) {
            ui.lastTimeShown = ui.startTime;
        }
        General.showOutput("Wattos last time        : " + Wattos.Utils.Dates.getDate( ui.lastTimeShown ));
        General.showOutput("Now at                  : " + Wattos.Utils.Dates.getDate( now ));
        General.showOutput("Since last time (#ms)   : " + (now.getTime()-ui.lastTimeShown.getTime()));
        ui.lastTimeShown = now;
        return true;
    }


    /** Sleeps current thread.
     */
    public boolean Sleep(Object[] methodArgs) {
        int printTime = 1000;
        beforeEachCommand(new Exception().getStackTrace()[0].getMethodName(),methodArgs);
        Integer sleepTime = (Integer) methodArgs[0];
        int sleepTimeInt = sleepTime.intValue();
        while ( sleepTimeInt > 0 ) {
            General.showOutput("Sleeping for: " + sleepTimeInt);
            General.sleep(printTime);
            sleepTimeInt -= printTime;
        }
        General.showOutput("Done sleeping.");
//        boolean status = ShowTime();
//        if ( ! status ) {
//            General.showError("Failed to ShowTime");
//            return false;
//        }
        return true;
    }

    /** Show the current time in ms and regular. */
    public boolean Sleep() {
        Integer   sleepTime      = new Integer(   Strings.getInputInt(      UserInterface.in, "Number of milliseconds to sleep (>=0 suggested)", null));
        Object[] methodArgs = { sleepTime };
        General.showOutput( "Doing Sleep with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return Sleep(methodArgs);
    }

    /** Nulls all objects in the ui */
    public boolean ShowSoupSTAR() {
        // TODO: debug
        General.showOutput("There will be several WARNINGS issued for setValue: given value is not of String like column but is of type: java.lang.Boolean TODO:");
        String result = dbms.toSTAR();
        if ( result == null ) {
            General.showError("Failed to render the dbms in STAR.");
            return false;
        }
        General.showOutput( result );
//        General.showOutput( dbms.toString(true) );
        return true;
    }

    /** Nulls all objects in the ui */
    public boolean ShowSoup() {
//        General.showOutput( "Doing ShowSoup" );
        General.showOutput( dbms.toString(true) );
        return true;
    }


    /** Reads a PDB formatted PDB entry with possibly multiple models. A molecular system
     *will be deducted from the coordinates.
     *Improvements:
     *<UL>
     *<LI>Deduct the molecular system description from the SEQRES records.
     *<LI>Detect breaks in a chain with END records and missing bonds etc.
     *<LI>Do fancy shuffeling of components i.s.o. trusting the chain id.
     *<LI>
     *</UL>
     */
    public boolean ReadEntryPDB() {
        URL url = InOut.getUrlFile(UserInterface.in, "Enter url to (gzipped) PDB formatted coordinate file: ");
        String atomNomenclatureFlavor = AtomMap.NOMENCLATURE_ID_PDB;
        Object[] methodArgs = { url, atomNomenclatureFlavor };
        General.showOutput( "Doing ReadEntryPDB with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! entry.readPdbFormattedFile(url, atomNomenclatureFlavor) ) {
            General.showError("Failed to entry.readPdbFormattedFile");
            return false;
        }
        if ( ui.hasGuiAttached() ) {
            if ( ! ui.gui.show3D(url)) {
                General.showError("Failed to ui.show3D");
                return false;
            }
        }
        return true;
    }

    /** Writes PDB formatted file for selected entries by going through
     *an STAR intermediate.
     */
    public boolean WriteEntryPDB() {
        String location = null;
//        String StarVersion = null;
        int maxTries = 0;
        String prompt = "Enter filename (-base) of PDB formatted coordinate file(s): ";
        while ( location == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
//        Integer PdbVersion = Integer.valueOf( Strings.getInputInt(UserInterface.in, "PDB version: (0 for 2.1; 1 for 3.1)"));
        Boolean generateStarFileToo = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Create a STAR file too, at no extra cost;-)"));
        Object[] methodArgs = { location, generateStarFileToo };
        General.showOutput( "Doing WriteEntryPDB with arguments: " + PrimitiveArray.toString( methodArgs ) );
        entry.writePdbFormattedFileSet(location, generateStarFileToo, ui);
        return true;
    }

    /** Using the nomenclature in a what if PDB file to update the atom names
     *and move the renamed atom names to the author atom name list.
     */
    public boolean ReadEntryNomenclatureWHATIFPDB() {
        URL url = null;
        int maxTries = 0;
        String atomNomenclatureFlavor = AtomMap.NOMENCLATURE_ID_IUPAC;
        String prompt = "Enter url to (gzipped) WHAT IF PDB formatted file: ";
        while ( url == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            url = InOut.getUrlFile(UserInterface.in, prompt);
            maxTries++;
        }
        Object[] methodArgs = { url };
        General.showOutput( "Doing ReadEntryNomenclatureWHATIFPDB with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! entry.readNomenclatureWHATIFPDB(url, atomNomenclatureFlavor) ) {
            General.showError("Failed to entry.readNomenclatureWHATIFPDB");
            return false;
        }
        return true;
    }

    /** Determine hydrogen bonds.
     */
    public boolean CalcHydrogenBond() {
        Float   hbHADistance         = new Float(   Strings.getInputFloat(  UserInterface.in, "Hydrogen bond distance between proton and acceptor cutoff (2.7 Angstroms suggested)", null));
        Float   hbDADistance         = new Float(   Strings.getInputFloat(  UserInterface.in, "Hydrogen bond distance between donor and acceptor cutoff (3.35 Angstroms suggested)", null));
        Float   hbDHAAngle           = new Float(   Strings.getInputFloat(  UserInterface.in, "Hydrogen bond angle (D-H-A) cutoff (90 degrees suggested)", null));
        int maxTries = 0;
        String summaryFileName = null;
        String prompt = "Enter star file name (with path) for output: (e.g. C:\\1brv_hb.txt)";
        while ( summaryFileName == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            summaryFileName = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        Object[] methodArgs = { hbHADistance, hbDADistance, hbDHAAngle, summaryFileName };
        General.showOutput( "Doing CalcHydrogenBond with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! entry.calcHydrogenBond(
                hbHADistance.floatValue(),
                hbDADistance.floatValue(),
                hbDHAAngle.floatValue(),
                summaryFileName)) {
            General.showError("Failed to entry.CalcHydrogenBond");
            return false;
        }
        return true;
    }

    /** Will list a list of coplanar bases.
     */
    public boolean CalcCoPlanarBasesSet() {
        //Float   distance         = new Float(   Strings.getInputFloat(  UserInterface.in, "Distance cutoff (2.0 Angstroms suggested)"));
        Float   angle   = new Float(   Strings.getInputFloat(  UserInterface.in, "Angle cutoff (40.0 degrees suggested)", null));
        Boolean onlyWC  = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Use only Watson Crick basepairing (false suggested)"));
        int maxTries = 0;
        String location = null;
        String prompt = "Enter cvs file name (with path) for output of CalcCoPlanarBasesSet.: ";
        while ( location == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        if ( location == null ) {
            General.showError("Failed to get value to parameter location_base in CalcCoPlanarBasesSet" );
            return false;
        }
        Object[] methodArgs = { angle, onlyWC, location };
        General.showOutput( "Doing CalcCoPlanarBasesSet with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! entry.calcCoPlanarBasesSet(angle.floatValue(),onlyWC.booleanValue(),location)) {
            General.showError("Failed to entry.CalcCoPlanarBasesSet");
            return false;
        }
        return true;
    }

    /** Determine bonds.
     */
    public boolean CalcBond() {
        Float   bCutOff         = new Float(   Strings.getInputFloat(  UserInterface.in, "Tolerance (0.1 Angstrom suggested)", null));
        Object[] methodArgs = { bCutOff };
        General.showOutput( "Doing CalcBond with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! entry.calcBond(bCutOff.floatValue()) ) {
            General.showError("Failed to entry.calcBond");
            return false;
        }
        return true;
    }

    /** Determine pair of atoms that are close together.
     */
    public boolean CalcDist() {
        Float     cutoff          = new Float(     Strings.getInputFloat(    UserInterface.in, "Cutoff (5.0 Angstrom suggested out of the blue)", null));
        Integer   minResDiff      = new Integer(   Strings.getInputInt(      UserInterface.in, "Minimum number of residues apart (>=1 suggested)", null));
        Integer   minModels       = new Integer(   Strings.getInputInt(      UserInterface.in, "Minimum number of models (1 suggested)", null));
        Boolean   intraMolecular  = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Within one molecule (true suggested)"));
        Boolean   interMolecular  = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Between one molecule (true suggested)"));
        Object[] methodArgs = { cutoff, minResDiff, minModels, intraMolecular, interMolecular};
        General.showOutput( "Doing CalcDist with arguments: " + PrimitiveArray.toString( methodArgs ) );

        if ( ! atom.calcDistance(  cutoff.floatValue(),
                                minResDiff.intValue(),
                                minModels.intValue(),
                                intraMolecular.booleanValue(),
                                interMolecular.booleanValue())) {
            General.showError("Failed to entry.calcDist");
            return false;
        }
        return true;
    }

    /** Determine dihedral angles for all selected atoms */
    public boolean CalcDihe() {
        int maxTries = 0;
        String summaryFileName = null;
        String prompt = "Enter star file name (with path) for output: (e.g. C:\\1brv_dih.str)";
        while ( summaryFileName == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            summaryFileName = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        Object[] methodArgs = { summaryFileName };
        General.showOutput( "Doing CalcDihe with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! atom.calcDihe(summaryFileName)) {
            General.showError("Failed to entry.CalcDihe");
            return false;
        }
        return true;
    }



    /** Determine pair of atoms that are close together.
     */
    public boolean FilterHighDistanceViol() {
        Float     cutoff          = new Float(     Strings.getInputFloat(    UserInterface.in, "Distance tolerance above which to delete (2.0 suggested)", null));
        Integer   maxRemove       = new Integer(   Strings.getInputInt(      UserInterface.in, "Maximum number of violations to remove. Largest violations will be removed (3 suggested)", null));

        int maxTries = 0;
        String fileNameBase = null;
        String prompt = "Enter file name (with path) for output of removed constraints: (high_viol.str suggested)";
        while ( fileNameBase == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            fileNameBase = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        Object[] methodArgs = { cutoff, maxRemove, fileNameBase };
        General.showOutput( "Doing FilterHighDistanceViol with arguments: " + PrimitiveArray.toString( methodArgs ) );

        if ( ! ui.constr.dc.filterHighDistanceViol(cutoff.floatValue(), maxRemove.intValue(),
                fileNameBase)) {
            General.showError("Failed FilterHighDistanceViol");
            return false;
        }
        return true;
    }


    public boolean CalcDistConstraintViolation() {
        Float     cutoff          = new Float(     Strings.getInputFloat(    UserInterface.in, "Cutoff (0.5 Angstrom suggested)", null));
        int maxTries = 0;
        String summaryFileName = null;
        String prompt = "Enter file name base (with path) for summary output of check: ";
        while ( summaryFileName == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            summaryFileName = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        Object[] methodArgs = { cutoff, summaryFileName };
        General.showOutput( "Doing CalcDistConstraintViolation with arguments: " + PrimitiveArray.toString( methodArgs ) );

        if ( ! ui.constr.dc.calcDistConstraintViolation(cutoff.floatValue(),
                summaryFileName)) {
            General.showError("Failed CalcDistConstraintViolation");
            return false;
        }
        return true;
    }

    public boolean CalcDihConstraintViolation() {
        Float     cutoff          = new Float(     Strings.getInputFloat(    UserInterface.in, "Cutoff (5.0 degree suggested)", null));
        int maxTries = 0;
        String summaryFileName = null;
        String prompt = "Enter file name base (with path) for summary output of check: ";
        while ( summaryFileName == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            summaryFileName = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        Object[] methodArgs = { cutoff, summaryFileName };
        General.showOutput( "Doing CalcDihConstraintViolation with arguments: " + PrimitiveArray.toString( methodArgs ) );

        if ( ! ui.constr.cdih.calcSimpleConstraintViolation(cutoff.floatValue(),
                summaryFileName)) {
            General.showError("Failed CalcDihConstraintViolation");
            return false;
        }
        return true;
    }

    public boolean CalcRDCConstraintViolation() {
        Float     cutoff          = new Float(     Strings.getInputFloat(    UserInterface.in, "Cutoff (5.0 degree suggested)", null));
        int maxTries = 0;
        String summaryFileName = null;
        String prompt = "Enter file name base (with path) for summary output of check: ";
        while ( summaryFileName == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            summaryFileName = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        Object[] methodArgs = { cutoff, summaryFileName };
        General.showOutput( "Doing CalcRDCConstraintViolation with arguments: " + PrimitiveArray.toString( methodArgs ) );

        if ( ! ui.constr.rdc.calcSimpleConstraintViolation(cutoff.floatValue(),
                summaryFileName)) {
            General.showError("Failed CalcRDCConstraintViolation");
            return false;
        }
        return true;
    }

//    /** Read a separate restraint files and match onto the selected entry.
//     *Automatically determines best nomenclature scheme basis.
//     *Allows for additional mappings to be added from a separate STAR file.
//     */
//    public boolean ReadConstrNMRSTAR() {
//        if ( true ) return false;
//        URL url = null;
//        int maxTries = 0;
//        String prompt = "Enter url to (gzipped) NMR-STAR formatted restraint file: ";
//        while ( url == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
//            url = InOut.getUrlFile(UserInterface.in, prompt);
//            maxTries++;
//        }
//        URL urlMap = null;
//        maxTries = 0;
//        prompt = "Enter url to (gzipped) NMR-STAR formatted mapping file (. suggested):";
//        while ( urlMap == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
//            urlMap = InOut.getUrlFile(UserInterface.in, prompt);
//            maxTries++;
//        }
//        Object[] methodArgs = { url, urlMap };
//        General.showOutput( "Doing ReadConstrNMRSTAR with arguments: " + PrimitiveArray.toString( methodArgs ) );
//        if ( ! entry.ReadConstrNMRSTAR(url, urlMap) ) {
//            General.showError("Failed to entry.ReadConstrNMRSTAR");
//            return false;
//        }
//        return true;
//    }

    /**
     *@see Wattos.Soup.Entry#readEntryExtraCoordinatesWHATIFPDB
     */
    public boolean ReadEntryExtraCoordinatesWHATIFPDB() {
        URL url = null;
        int maxTries = 0;
        String atomNomenclatureFlavor = AtomMap.NOMENCLATURE_ID_IUPAC;
        String prompt = "Enter url to (gzipped) WHAT IF PDB formatted file: ";
        while ( url == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            url = InOut.getUrlFile(UserInterface.in, prompt);
            maxTries++;
        }
        Object[] methodArgs = { url };
        General.showOutput( "Doing ReadEntryExtraCoordinatesWHATIFPDB with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! entry.readEntryExtraCoordinatesWHATIFPDB(url, atomNomenclatureFlavor) ) {
            General.showError("Failed to entry.readEntryExtraCoordinatesWHATIFPDB");
            return false;
        }
        return true;
    }

    /**
     * Reads a bunch of entries in NMR-STAR or PDB format.
     */
    public boolean ReadEntrySetNMRSTAR() {
        URL url = null;
        String StarVersion = null;
        int maxTries = 0;
        String prompt = "Enter url to file with file names of (gzipped) NMR-STAR 3.0 formatted file: ";
        while ( url == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            url = InOut.getUrlFile(UserInterface.in, prompt);
            maxTries++;
        }
        Boolean removeUnlinkedRestraints  = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Remove unlinked restraints (true suggested)"));

        Object[] methodArgs = { url, StarVersion,removeUnlinkedRestraints };
        General.showOutput( "Doing ReadEntrySetNMRSTAR with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return entry.readNmrStarFormattedFileSet(url,null,ui, removeUnlinkedRestraints.booleanValue());
    }


    /** Reads an NMR-STAR 3.0 formatted entry with many types of data. A molecular system
     *will be deducted from the saveframes describing it.
     */
    public boolean ReadEntryNMRSTAR() {
        URL url = null;
        String StarVersion = null;
        int maxTries = 0;
        String prompt = "Enter url to (gzipped) NMR-STAR 3.0 formatted file: ";
        while ( url == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            url = InOut.getUrlFile(UserInterface.in, prompt);
            maxTries++;
        }

        Boolean doEntry                     = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Read molecular system and coordinates (true suggested)"));
        Boolean doRestraints                = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Read restraints (true suggested)"));
        Boolean matchRestraints2Soup        = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Match restraints to soup by regular NMR-STAR scheme. (true suggested)"));
        Boolean matchRestraints2SoupByAuthorDetails = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Match restraints to soup by author atom and residue names etc. (false suggested)"));
        Boolean removeUnlinkedRestraints    = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Remove unlinked restraints (true suggested)"));
        Boolean syncModels                  = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Sync over models; removing inconsistent atoms (true suggested)"));

        Object[] methodArgs = { url, StarVersion, doEntry, doRestraints,
            matchRestraints2Soup, removeUnlinkedRestraints, syncModels };
        General.showOutput( "Doing ReadEntryNMRSTAR with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return entry.readNmrStarFormattedFile(url,null,ui,
                doEntry.booleanValue(),
                doRestraints.booleanValue(),
                matchRestraints2Soup.booleanValue(),
                matchRestraints2SoupByAuthorDetails.booleanValue(),
                removeUnlinkedRestraints.booleanValue(),
                syncModels.booleanValue()
                );
    }


    /** Reads an mmCIF formatted entry with coordinte data. A molecular system
     *will be deducted.
     */
    public boolean ReadEntryMMCIF() {
        URL url = null;
        int maxTries = 0;
        String prompt = "Enter url to (gzipped) mmCIF formatted file: ";
        while ( url == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            url = InOut.getUrlFile(UserInterface.in, prompt);
            maxTries++;
        }
        Boolean syncModels                  = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Sync over models; removing inconsistent atoms (true suggested)"));
        Object[] methodArgs = { url, syncModels };
        General.showOutput( "Doing ReadEntryMMCIF with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return entry.readmmCIFFormattedFile(url,ui,syncModels.booleanValue());
    }

    /** Reads, optionally filters and writes STAR formatted file.
     */
    public boolean FilterSTAR() {
        String inputFile = null;
        String outputFile = null;
        String filterFile = null;

        int maxTries = 0;
        String prompt = "Enter url to (gzipped) STAR formatted input file: ";
        while ( inputFile == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            inputFile = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        maxTries = 0;
        prompt = "Enter name for output STAR file: ";
        while ( outputFile == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            outputFile = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        maxTries = 0;
        prompt = "Enter name for filter STAR file (. suggested as it is not implemented yet): ";
        while ( filterFile == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            filterFile = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        Object[] methodArgs = { inputFile, outputFile, filterFile };
        String[] methodArgsStr = Strings.toStringArray(methodArgs);
        General.showOutput( "Doing FilterSTAR with arguments: " + PrimitiveArray.toString( methodArgs ) );
        STARFilter sFilter = new STARFilter();
        return sFilter.filter(methodArgsStr);
    }


    /** Writes an NMR-STAR 3.0 formatted entry with many types of data.
     */
    public boolean WriteEntryNMRSTAR() {
        String location = null;
        String StarVersion = null;
        int maxTries = 0;
        String prompt = "Enter file names (with path) for output to NMR-STAR 3.0 formatted file(s): ";
        while ( location == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        //Boolean   prettyPrint  = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Align columns in the output tables nicely (false suggested)"));

        Object[] methodArgs = { location, StarVersion };
        General.showOutput( "Doing WriteEntryNMRSTAR with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return entry.writeNmrStarFormattedFileSet(location,
                StarVersion,
                ui
                );
    }

    /** Writes the restraint lists to xplor format.
     */
    public boolean WriteEntryXplor() {
        String location = null;
        int maxTries = 0;
        String prompt = "Enter file name base (with path) for output to XPLOR formatted file(s): ";
        while ( location == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        String atomNomenclature = null;
        prompt = "Enter atom nomenclature (XPLOR suggested): ";
        while ( atomNomenclature == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            atomNomenclature = Strings.getInputString(UserInterface.in, prompt,ui.wattosLib.atomMap.NOMENCLATURE_NAMES, null);
            maxTries++;
        }
        Boolean sortRestraints = new Boolean(   Strings.getInputBoolean(  UserInterface.in, "Sort restraints (false suggested)"));

        Object[] methodArgs = { location, atomNomenclature, sortRestraints };
        General.showOutput( "Doing WriteEntryXplor with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return entry.writeXplorOrSoFormattedFileSet(location, ui, atomNomenclature,
                sortRestraints.booleanValue(), null);
    }

    /** Sets an internal wattos property.     */
    public boolean SetProp() {
        String promptName = "Enter property name: ";
        String propertyName = Strings.getInputString(UserInterface.in, promptName, null);
        String promptValue = "Enter property value (watch value type) for " +
            propertyName + ": ";
        String propertyValue = Strings.getInputString(UserInterface.in, promptValue, null);
        Object[] methodArgs = { propertyName, propertyValue };
        General.showOutput( "Doing SetProp with arguments: " + PrimitiveArray.toString( methodArgs ) );
//        Object prevProp = ui.wattosProp.setProperty( propertyName, propertyValue );
        ui.wattosProp.setProperty( propertyName, propertyValue );
//        if ( prevProp != null ) {
//            General.showDebug( "Replaced property's original value: " + prevProp);
//        }
        if ( ! ui.wattosProp.setProperties(ui)) {
            General.showError("Failed to set properties.");
            return false;
        }
        return true;
    }

    public boolean beforeEachCommand(String methodName, Object[] methodArgs) {
//        General.showOutput( "Doing "+methodName+" with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return true;
    }

    /** Executes a (remote) script. May be called recursively.
     Script needs to be complete and materialized before calling
     this method as it will read the whole script before executing
     anything in it.
     */
    public boolean ExecuteMacroUser(Object[] methodArgs) {
        beforeEachCommand(new Exception().getStackTrace()[0].getMethodName(),methodArgs);
        URL url = (URL) methodArgs[0];
        String scriptContent = InOut.readTextFromUrl(url);
        if ( scriptContent == null ) {
            General.showError("Failed to ExecuteScript because url was not read.");
            return false;
        }

        // save the original interactiveness.
        if ( inputs.empty() ) {
            originalInteractive = ui.interactiveSession;

        }
        inputs.push( UserInterface.in ); // safe the reference in order to pick up
        // the stream afterwards. As there might be another script surrounding this
        // call it's important to keep the stream open and alive.

        ui.interactiveSession = false; // Scripted means non-interactive always.
        UserInterface.in = new BufferedReader(new StringReader(scriptContent));
        // execute the read commands
        if ( ! ui.doNonGuiMain()) {
            return false;
        }

        UserInterface.in = (BufferedReader) inputs.pop(); // it's a code bug if the stack is empty.
        if ( inputs.empty() ) {
            ui.interactiveSession = originalInteractive;
            if ( ui.hasGuiAttached() ) {
                ui.inGuiMode = true;
            }
        }
        return true;
    }

    /** Convenience method */
    public boolean ExecuteMacroUser() {
        URL url = InOut.getUrlFile(UserInterface.in, "Enter url to (gzipped) Wattos script file: ("+Globals.wattos_home_page+"/macros/QuitBeforeBegun.wcf)");
        Object[] methodArgs = { url };
        General.showOutput( "Doing ExecuteMacroUser with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return ExecuteMacroUser(methodArgs);
    }

    /** Sets an internal wattos property.     */
    public boolean ListProp() {
        General.showOutput( "Doing ListProp" );
        General.showOutput( Strings.toString( ui.wattosProp ));
        return true;
    }

    /** Checks for bad contacts between a atoms in one model. This should be familiar to the MOLMOL
     *users.
     *
    public boolean CheckContact() {
        General.showOutput( "Doing CheckContact without arguments");
        AtomCheck atomCheck = new AtomCheck(gumbo);
        /**
        BitSet bs = new BitSet();
        bs.cardinality()
         *
        return atomCheck.CheckContact();
    }
     */

    /**
     * @see Wattos.Database.DBMS#dumpSQL
     */
    public boolean WriteSQLDump() {
        Boolean containsHeaderRow = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should a header row be written (y suggested)"));
        String location_file = null;
        int maxTries = 0;
        String prompt = "Enter file name (without path) for SQL commands (wattos_dump.sql suggested): ";
        while ( location_file == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location_file = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        String location_dir = null;
        maxTries = 0;
        prompt = "Enter directory (the path) for the SQL file and the CSV files to be dumped (. suggested): ";
        while ( location_dir == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location_dir = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        Object[] methodArgs = {
            location_file,
            location_dir,
            containsHeaderRow
        };
        General.showOutput( "Doing WriteSQLDump with arguments: " + PrimitiveArray.toString( methodArgs ) );
        boolean result = dbms.dumpSQL(
            location_file,
            location_dir,
            containsHeaderRow.booleanValue()
            );
        if ( ! result  ) {
            General.showError( "Failed WriteSQLDump.");
            return false;
        }
        return true;
    }


    /** Calculates the distance restraint violation energies for the regular and
     * swapped states and modifies stereospecific assignments.
     */
    public boolean CheckAssignment() {

        Float energy_abs_criterium           = new Float(   Strings.getInputFloat(  UserInterface.in, "Criterium on absolute energy difference (0.1 A^2 per model per triplet suggested)", null));
        Float energy_rel_criterium           = new Float(   Strings.getInputFloat(  UserInterface.in, "Criterium on relative energy difference (0 % suggested)", null));
        Float model_criterium                = new Float(   Strings.getInputFloat(  UserInterface.in, "Criterium on perc. models in favored state (>=49 % suggested)", null));
        Float single_model_violation_deassign_criterium         = new Float(   Strings.getInputFloat(  UserInterface.in, "Deassignment criterium for single model violation (1.0 Angstrom suggested)", null));
        Float multi_model_violation_deassign_criterium          = new Float(   Strings.getInputFloat(  UserInterface.in, "Deassignment criterium for multiple model violation (0.5 Angstrom suggested)", null));
        Float multi_model_rel_violation_deassign_criterium      = new Float(   Strings.getInputFloat(  UserInterface.in, "Deassignment criterium for multiple model violation percentage (50 % suggested)", null));
        String location_file = null;
        int maxTries = 0;
        String prompt = "Enter name for the star formatted result file (stereo_assign.str suggested): ";
        while ( location_file == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location_file = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        Object[] methodArgs = {
            energy_abs_criterium,
            energy_rel_criterium,
            model_criterium,
            single_model_violation_deassign_criterium,
            multi_model_violation_deassign_criterium,
            multi_model_rel_violation_deassign_criterium,
            location_file
        };
        General.showOutput( "Doing CheckAssignment with arguments: " + PrimitiveArray.toString( methodArgs ) );

        AssignStereo assignStereo = new AssignStereo(ui);
        return assignStereo.doAssignStereo(
            energy_abs_criterium.floatValue(),
            energy_rel_criterium.floatValue(),
            model_criterium.floatValue(),
            single_model_violation_deassign_criterium.floatValue(),
            multi_model_violation_deassign_criterium.floatValue(),
            multi_model_rel_violation_deassign_criterium.floatValue(),
            location_file
                );
    }

    /** Convenience method */
    public boolean ShowPlotCompletenessResidue() {
        URL url = InOut.getUrlFile(UserInterface.in, "Enter url to STAR file with completeness output: (1hue_DOCR_compl_sum.str)");
        Boolean write = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should a jpg file be written (y suggested)"));
        String location_base = null;
        int maxTries = 0;
        String prompt = "Enter file name base (with path) for output of image file(s): ";
        while ( location_base == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location_base = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        if ( location_base == null ) {
            General.showError("Failed to get value to parameter location_base in ShowPlotCompletenessResidue" );
            return false;
        }
        Object[] methodArgs = { url,write,location_base };
        General.showOutput( "Doing ShowPlotCompletenessResidue with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return ShowPlotCompletenessResidue(methodArgs);
    }

    /** Convenience method */
    public boolean ShowPlotViolationResidue() {
        URL url = InOut.getUrlFile(UserInterface.in, "Enter url to STAR file with violation output: (1rjj_viol.str)");
        Boolean write = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should a jpg file be written (y suggested)"));
        String location_base = null;
        int maxTries = 0;
        String prompt = "Enter file name base (with path) for output of image file(s): ";
        while ( location_base == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location_base = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        if ( location_base == null ) {
            General.showError("Failed to get value to parameter location_base in ShowPlotCompletenessResidue" );
            return false;
        }
        Object[] methodArgs = { url,write,location_base };
        General.showOutput( "Doing ShowPlotViolationResidue with arguments: " + PrimitiveArray.toString( methodArgs ) );
        return ShowPlotViolationResidue(methodArgs);
    }

    /** Shows the violation per residue plot in Gui if available.
     */
    public boolean ShowPlotViolationResidue(Object[] methodArgs) {
        beforeEachCommand(new Exception().getStackTrace()[0].getMethodName(),methodArgs);
        URL     url =                (URL) methodArgs[0];
        boolean saveImage =     ((Boolean) methodArgs[1]).booleanValue();
        String  location_file =   (String) methodArgs[2];

        General.showOutput( "Doing ShowPlotViolationResidue with arguments: " + PrimitiveArray.toString( methodArgs ) );

        boolean status = ui.constr.dc.showPlotPerResidue(url, saveImage, location_file);
        if ( ! status ) {
            General.showError("Failed ShowPlotViolationResidue");
            return false;
        }
        return true;
    }

    /** Shows the completeness per residue plot in Gui if available.
     */
    public boolean ShowPlotCompletenessResidue(Object[] methodArgs) {
        beforeEachCommand(new Exception().getStackTrace()[0].getMethodName(),methodArgs);
        URL     url =                (URL) methodArgs[0];
        boolean saveImage =     ((Boolean) methodArgs[1]).booleanValue();
        String  location_file =   (String) methodArgs[2];
        Completeness completeness = new Completeness(ui);
        General.showOutput( "Doing ShowPlotCompletenessResidue with arguments: " + PrimitiveArray.toString( methodArgs ) );
        boolean status = completeness.showPlotPerResidue(url, saveImage, location_file);
        if ( ! status ) {
            General.showError("Failed ShowPlotCompletenessResidue");
            return false;
        }
        return true;
    }


    /** Checks for surplus in the distance constraints.
     */
    public boolean CheckSurplus() {

        Float thresholdRedundancy           = new Float(   Strings.getInputFloat(  UserInterface.in, "Redundancy tolerance (5% suggested)", null));
        Boolean updateOriginalConstraintsObj= Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should impossible target distance be reset to null (y suggested)"));
        Boolean onlyFilterFixed             = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should only fixed distances be considered surplus (n suggested)"));
        Integer avg_method                  = new Integer( Strings.getInputInt(    UserInterface.in, "Averaging method id. Center,Sum,R6 are 0,1, and 2 respectively and -1 to let it be determined per list but that's not completely implemented yet: (1 suggested)", null));
        Integer monomers                    = new Integer( Strings.getInputInt(    UserInterface.in, "Number of monomers but only relevant when Sum averaging is selected: (1 suggested)", null));
        String location_base = null;
        int maxTries = 0;
        String prompt = "Enter file name base (with path) for output of surplus check summary and constraint lists.: ";
        while ( location_base == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            location_base = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }
        Boolean writeNonRedundant           = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should non-redundant constraints be written (y suggested)"));
        Boolean writeRedundant              = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should redundant constraints be written (n suggested)"));
        Boolean removeSurplus               = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should redundant constraints be removed (y suggested)"));

        if ( location_base == null ) {
            General.showError("Failed to get value to parameter location_base in CheckSurplus" );
            return false;
        }

        Object[] methodArgs = {
            thresholdRedundancy,
            updateOriginalConstraintsObj,
            onlyFilterFixed,
            avg_method,
            monomers,
            location_base,
            writeNonRedundant,
            writeRedundant,
            removeSurplus
        };
        General.showOutput( "Doing CheckSurplus with arguments: " + PrimitiveArray.toString( methodArgs ) );

        Surplus surplus = new Surplus(ui);
//        boolean updateOriginal = true;
        BitSet result = surplus.getSelectionSurplus(
            ui.constr.dc.selected,
            thresholdRedundancy.floatValue(),
            updateOriginalConstraintsObj.booleanValue(),
            onlyFilterFixed.booleanValue(),
            avg_method.intValue(),
            monomers.intValue(),
            location_base,
            false, // append to summary file (or overwrite)
            writeNonRedundant.booleanValue(),
            writeRedundant.booleanValue(),
            removeSurplus.booleanValue()
            );

        //constr.dc.order( constr.dc.selected );
        if ( result == null ) {
            General.showError( "Failed surplus check.");
            return false;
        }
        return true;
    }

    /** Checks for surplus in the distance constraints.
     */
    public boolean ShowDCClass() {
        Object[] methodArgs = { };
        General.showOutput( "Doing ShowDCClass with arguments: " + PrimitiveArray.toString( methodArgs ) );
        boolean status = ui.constr.dc.getClassification( ui.constr.dc.selected );
        if ( ! status ) {
            General.showError("Failed to show the dc's classifications");
            return false;
        }
        return true;
    }

    /** Checks for surplus in the distance constraints.
     */
    public boolean CheckCompleteness() {

        Float   max_dist_expectedOverall  = new Float(   Strings.getInputFloat(  UserInterface.in, "Maximum distance expected (4.0 suggested)", null));

        Float   min_dist_observed         = new Float(   Strings.getInputFloat(  UserInterface.in, "Minimum distance observed for per shell listing (2.0 suggested)", null));
        Float   max_dist_observed         = new Float(   Strings.getInputFloat(  UserInterface.in, "Maximum distance observed for per shell listing (4.0 suggested)", null));
        Integer numb_shells_observed      = new Integer( Strings.getInputInt(    UserInterface.in, "Number of shells observed for per shell listing (2 suggested; max is 9)", null));

        Float   min_dist_expected         = new Float(   Strings.getInputFloat(  UserInterface.in, "Minimum distance expected for per shell listing (2.0 suggested)", null));
        Float   max_dist_expected         = new Float(   Strings.getInputFloat(  UserInterface.in, "Maximum distance expected for per shell listing (10.0 suggested)", null));
        Integer numb_shells_expected      = new Integer( Strings.getInputInt(    UserInterface.in, "Number of shells expected for per shell listing (16 suggested; no max)", null));

        Float   avg_power_models          = new Float(   Strings.getInputFloat(  UserInterface.in, "Averaging power over models (1.0 suggested)", null));
        Integer avg_method                = new Integer( Strings.getInputInt(    UserInterface.in, "Averaging method id. Center,Sum,R6 are 0,1, and 2 respectively : (1 suggested)", null));
        Integer monomers                  = new Integer( Strings.getInputInt(    UserInterface.in, "Number of monomers but only relevant when Sum averaging is selected: (1 suggested)", null));

        //Boolean double_count              = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Double counting [doesn't matter yet] (n suggested)"));
        Boolean use_intra                 = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should intraresiduals be considered (n suggested)"));

        int maxTries = 0;
        String ob_file_name = null;
        String prompt = "Enter file name of a standard set of observable atoms (ob_standard.str suggested): ";
        while ( ob_file_name == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            ob_file_name = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }


        maxTries = 0;
        String summaryFileNameCompleteness = null;
        prompt = "Enter file name base (with path) for output of completeness check summary: ";
        while ( summaryFileNameCompleteness == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            summaryFileNameCompleteness = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        Boolean write_dc_lists            = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should distance constraints be written (y suggested)"));

        String file_name_base_dc = null;
        maxTries = 0;
        prompt = "Enter file name base (with path) for surplus analysis and distance constraints (if selected) to be written: ";
        while ( file_name_base_dc == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            file_name_base_dc = InOut.getPossibleFileName(UserInterface.in, prompt);
            maxTries++;
        }

        if ( ob_file_name == null ) {
            General.showError("Failed to get value to parameter ob_file_name in CheckCompleteness" );
            return false;
        }
        if ( file_name_base_dc == null ) {
            General.showError("Failed to get value to parameter file_name_base_dc in CheckCompleteness" );
            return false;
        }

        Object[] methodArgs = {
            max_dist_expectedOverall,
            min_dist_observed,
            max_dist_observed,
            numb_shells_observed,
                min_dist_expected,
                max_dist_expected,
                numb_shells_expected,
                avg_power_models,
                avg_method,
                monomers,
                use_intra,
                ob_file_name,
                summaryFileNameCompleteness,
                write_dc_lists,
                file_name_base_dc
        };

        General.showOutput( "Doing CheckCompleteness with arguments: " + PrimitiveArray.toString( methodArgs ) );


        Completeness completeness = new Completeness(ui);
        boolean status = completeness.doCompletenessCheck(
            max_dist_expectedOverall.floatValue(),
            min_dist_observed.floatValue(),
            max_dist_observed.floatValue(),
            numb_shells_observed.intValue(),
            min_dist_expected.floatValue(),
            max_dist_expected.floatValue(),
            numb_shells_expected.intValue(),
            avg_power_models.floatValue(),
            avg_method.intValue(),
            monomers.intValue(),
            use_intra.booleanValue(),
            ob_file_name,
            summaryFileNameCompleteness,
            write_dc_lists.booleanValue(),
            file_name_base_dc
            );
        if ( ! status ) {
            General.showError("Failed the completeness check");
            return false;
        }
        return true;
    }

    /** Does a variety of check on the healthiness of the soup. E.g. multiple instances of the
     *atoms, residues, etc. Tries to correct the situation if asked to do so.
     */
    public boolean CheckSoup() {
        Boolean makeCorrections = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should corrections be made?"));
        Object[] methodArgs = { makeCorrections };
        General.showOutput( "Doing CheckSoup with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! gumbo.check(makeCorrections.booleanValue())) {
            General.showError("Failed the soup check.");
        }
        return true;
    }

    /** @see Residue#addMissingAtoms
     */
    public boolean AddMissingAtoms() {
        Object[] methodArgs = {
        };
        General.showOutput( "Doing AddMissingAtoms with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! entry.addMissingAtoms()) {
            General.showError("Failed to entry.addMissingAtoms");
            return false;
        }
        return true;
    }

    /** @see Atom#removeAtoms
     */
    public boolean RemoveAtoms() {
        Object[] methodArgs = {
        };
        General.showOutput( "Doing RemoveAtoms with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! atom.removeAtoms()) {
            General.showError("Failed to atom.removeAtoms");
            return false;
        }
        return true;
    }
    /** @see Atom#selectAtomsByNameRegExp
     */
    public boolean SelectAtomsByNameRegExp() {
        int maxTries = 0;
        String regExp = null;
        String prompt = "Enter atom name regular expression pattern. (H.* for example): ";
        while ( regExp == null && maxTries < UserInterface.DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT ) {
            regExp = Strings.getInputString(UserInterface.in, prompt, null);
            maxTries++;
        }

        Object[] methodArgs = {     regExp
        };
        General.showOutput( "Doing SelectAtomsByNameRegExp with arguments: " + PrimitiveArray.toString( methodArgs ) );
        if ( ! atom.selectAtomsByNameRegExp(regExp)) {
            General.showError("Failed to atom.selectAtomsByNameRegExp");
            return false;
        }
        return true;
    }

    /** Swaps atom nomenclature if asked.
     */
    public boolean CheckAtomNomenclature() {
        Boolean doCorrect = Boolean.valueOf( Strings.getInputBoolean(UserInterface.in, "Should corrections be made (y suggested)"));

        Object[] methodArgs = {
                doCorrect
        };
        General.showOutput( "Doing CheckAtomNomenclature with arguments: " + PrimitiveArray.toString( methodArgs ) );

        if ( ! entry.checkAtomNomenclature(doCorrect.booleanValue())) {
            General.showError("Failed to entry.checkAtomNomenclature");
            return false;
        }
        return true;
    }

    public boolean TruncateEnsembleToMaxResidues() {
        Integer maxResCount = Integer.valueOf( Strings.getInputInt(UserInterface.in, "Number of maximum total number of residues (7500 suggested for FC)", null));

        Object[] methodArgs = {
                maxResCount
        };
        General.showOutput( "Doing TruncateEnsembleToMaxResidues with arguments: " + PrimitiveArray.toString( methodArgs ) );

        if ( ! entry.truncateEnsembleToMaxResidues(maxResCount.intValue())) {
            General.showError("Failed to entry.truncateEnsembleToMaxResidues");
            return false;
        }
        return true;
    }
    public boolean TruncateEnsembleToMaxModels() {
        Integer maxModelCount = Integer.valueOf( Strings.getInputInt(UserInterface.in, "Number of maximum number of models (1 suggested for interactive work with FC)", null));

        Object[] methodArgs = { maxModelCount };
        General.showOutput( "Doing TruncateEnsembleToMaxModels with arguments: " + PrimitiveArray.toString( methodArgs ) );

        if ( ! entry.truncateEnsembleToMaxModels(maxModelCount.intValue())) {
            General.showError("Failed to entry.truncateEnsembleToMaxModels");
            return false;
        }
        return true;
    }
}
