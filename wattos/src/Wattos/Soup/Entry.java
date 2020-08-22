/*
 * Created on November 8, 2002, 4:41 PM
 */

package Wattos.Soup;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

import Wattos.CloneWars.UserInterface;
import Wattos.Common.OrfIdList;
import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Soup.Constraint.SimpleConstrList;
import Wattos.Star.MmCif.CIFCoord;
import Wattos.Star.NMRStar.File31;
import Wattos.Utils.General;
import Wattos.Utils.HashOfHashes;
import Wattos.Utils.HashOfHashesOfHashes;
import Wattos.Utils.InOut;
import Wattos.Utils.Objects;

/**
 *Entry contains one or more models.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Entry extends GumboItem implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    public static final String DEFAULT_ATTRIBUTE_SET_NAME  = "entry";
    public static final int ERROR_MESSAGES_2_PRINT = 100;


    /** Convenience variables */
    public Object[]    orfIdList;      // non fkcs
    public Object[]    atomsHash;
    public String[]    assemblyNameList;
    public String[]    pdbCoordinateFileVersionNameList;
    public BitSet      modelsSynced;
    public Gumbo       gumbo;          // so cast doesn't need to be done.

    public Entry(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        gumbo = (Gumbo) relationSoSParent;
    }

    public Entry(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
    }

    public boolean init(DBMS dbms) {
        super.init(dbms);

        name = DEFAULT_ATTRIBUTE_SET_NAME;

        /** MAIN RELATION in addition to the ones in gumbo item. */
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ORF_ID_LIST,   new Integer(DATA_TYPE_OBJECT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ASSEMBLY_NAME, new Integer(DATA_TYPE_STRING));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_MODELS_SYNCED, new Integer(DATA_TYPE_BIT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ATOMS_HASH,    new Integer(DATA_TYPE_OBJECT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_PDB_COORDINATE_FILE_VERSION,    new Integer(DATA_TYPE_STRING));

        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ORF_ID_LIST);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ASSEMBLY_NAME);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_MODELS_SYNCED);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ATOMS_HASH);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_PDB_COORDINATE_FILE_VERSION);

        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME];
        try {
            relation = new Relation(relationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }
        relation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, DEFAULT_ATTRIBUTE_VALUES, null);
        addRelation( relation );
        mainRelation = relation;
        return true;
    }

    /** @see Residue#calcCoPlanarBasesSet
     */
    public boolean calcCoPlanarBasesSet( float calcCoPlanarBasesSet, boolean onlyWC, String location ) {
        BitSet resInMaster = getResInMasterModel();
        if ( resInMaster == null ) {
            General.showError("Failed to get the master residues");
            return false;
        }
        if ( ! gumbo.res.calcCoPlanarBasesSet(resInMaster, calcCoPlanarBasesSet, onlyWC, location )) {
            General.showError("Failed to gumbo.entry.calcCoPlanarBasesSet");
            return false;
        }
        return true;
    }

    /** @see Atom#calcHydrogenBond
     */
    public boolean calcHydrogenBond(float hbHADistance,float hbDADistance,float hbDHAAngle,
            String summaryFileName) {
        BitSet atomsInMaster = getAtomsInMasterModel();
        if ( atomsInMaster == null ) {
            General.showError("Failed to get the master atoms");
            return false;
        }
        if ( ! gumbo.atom.calcHydrogenBond(atomsInMaster, hbHADistance,hbDADistance,hbDHAAngle,summaryFileName )) {
            General.showError("Failed to gumbo.atom.calcHydrogenBond");
            return false;
        }
        return true;
    }

    /** @see Residue#addMissingAtoms
     */
    public boolean addMissingAtoms() {
        BitSet resInMaster = getResInMasterModel();
        if ( resInMaster == null ) {
            General.showError("Failed to get the master res");
            return false;
        }
        if ( ! gumbo.res.addMissingAtoms(resInMaster)) {
            General.showError("Failed to gumbo.atom.addMissingAtoms");
            return false;
        }
        return true;
    }

    /** @see Residue#calcHydrogenBond
     */
    public boolean checkAtomNomenclature(boolean  doCorrect) {
        BitSet resInMaster = getResInMasterModel();
        if ( resInMaster == null ) {
            General.showError("Failed to get the master res");
            return false;
        }
        if ( ! gumbo.res.checkAtomNomenclature(doCorrect, resInMaster)) {
            General.showError("Failed to gumbo.atom.checkAtomNomenclature");
            return false;
        }
        return true;
    }

    /** See Atom.calcBond */
    public boolean calcBond( float tolerance) {
        BitSet resInMaster = getResInMasterModel();
        if ( resInMaster == null ) {
            General.showError("Failed to get the master atoms");
            return false;
        }
        if ( ! gumbo.atom.calcBond(resInMaster, tolerance)) {
            General.showError("Failed to gumbo.atom.calcBond");
            return false;
        }
        return true;
    }

    /** Returns the selected entry id or negative value if none or more than one
     *entry is selected
     */
    public int getEntryId() {
        int selectedCount = selected.cardinality();
        if ( selectedCount != 1 ) {
            General.showError("In getEntryId; Expected 1 and only 1 selected entry but found: " + selectedCount);
            return -1;
        }
        int entryRID = selected.nextSetBit(0);
        return entryRID;
    }

    /** Returns the first model rid for an entry rid or negative value if none or more than one
      is found
     */
    public int getMasterModelId(int entryRID) {
        BitSet modelsInEntry = getModelsInEntry(entryRID);
        if ( modelsInEntry == null ) {
            General.showError("Failed to do getAtomsInMasterModel because failed to get the models in this entry for rid: " + entryRID);
            return -1;
        }
//        int modelCount = modelsInEntry.cardinality();
//        General.showDebug( "Found number of models in entry: " + modelCount);
        int modelOneRid = gumbo.model.getModelRidWithNumber( modelsInEntry, 1 );
        return modelOneRid;
    }

    /** Returns the atoms in the first model of the selected entry or null in case of an error.
     */
    public BitSet getAtomsInMasterModel() {
        int entryRID = getEntryId();
        if ( entryRID < 0 ) {
            General.showError("In getAtomsInMasterModel; Failed to find just one selected entry.");
            return null;
        }
        int modelOneRid = getMasterModelId( entryRID );
        if ( modelOneRid < 0 ) {
            General.showError("Failed to do getAtomsInMasterModel because failed to get the first model's rid");
            return null;
        }
        BitSet atomsInMasterModel = SQLSelect.selectBitSet(dbms, gumbo.atom.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
            SQLSelect.OPERATION_TYPE_EQUALS, new Integer( modelOneRid ), false);
        if ( atomsInMasterModel == null ) {
            General.showError("Failed to do getAtomsInMasterModel because failed to get the atoms in first model for this entry for rid: " + entryRID);
            return null;
        }
//        int atomCountMasterModel = atomsInMasterModel.cardinality();
//        General.showDebug("Found number of atoms in master model: " + atomCountMasterModel);
        return atomsInMasterModel;
    }

    /** Returns the residues in the first model of the selected entry or null in case of an error.
     *Those residues should also be 'selected'.
     */
    public BitSet getResInMasterModel() {
        int entryRID = getEntryId();
        if ( entryRID < 0 ) {
            General.showError("In getResInMasterModel; Failed to find just one selected entry.");
            return null;
        }
        int modelOneRid = getMasterModelId( entryRID );
        if ( modelOneRid < 0 ) {
            General.showError("Failed to do getResInMasterModel because failed to get the first model's rid");
            return null;
        }
        BitSet resInMasterModel = SQLSelect.selectBitSet(dbms, gumbo.res.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
            SQLSelect.OPERATION_TYPE_EQUALS, new Integer( modelOneRid ), false);
        if ( resInMasterModel == null ) {
            General.showError("Failed to do getResInMasterModel because failed to get the atoms in first model for this entry for rid: " + entryRID);
            return null;
        }
        resInMasterModel.and( gumbo.res.selected );
//        int resCountMasterModel = resInMasterModel.cardinality();
//        General.showDebug("Found number of residues in master model: " + resCountMasterModel);
        return resInMasterModel;
    }


    /** The name says it all. It will be read and appended to the ensemble list.
     */
    public boolean readPdbFormattedFile( URL url, String atomNomenclatureFlavor ) {
        PdbFile pdbFile = new PdbFile( dbms );
        boolean status = pdbFile.readFile(url, atomNomenclatureFlavor);
        if ( ! status ) {
            General.showWarning("entry.readPdbFormattedFile was unsuccessful. Failed to read pdb formatted file");
            return false;
        } else {
            General.showOutput("Read total number of atoms from PDB formatted coordinate list: "+gumbo.atom.used.cardinality());
        }
        int currentEntryId = getEntryId();
        // Sync the atoms over the models in the entry.
        if ( ! modelsSynced.get( currentEntryId  ) && ! syncModels( currentEntryId )) {
            General.showError("Failed to sync models after reading in the coordinates. Deleting the whole entry again.");
            if ( ! mainRelation.removeRowCascading( currentEntryId, true ) ) {
                General.showError("Failed to deleting the whole entry.");
            }
            return false;
        }
        if ( ! postProcessAfterReading(true) ) {
            return false;
        }
        return status;
    }


    /** Does action after each read of a molecular system.
     * Note that the atom type is only set for the master atoms.
     * @param syncModels TODO
     */
    private boolean postProcessAfterReading(boolean syncModels) {
        BitSet atomsInMasterModel = getAtomsInMasterModel();
        if ( atomsInMasterModel == null ) {
            General.showError("Failed to getAtomsInMasterModel");
            return false;
        }
        AtomLibAmber atomLibAmber = null;

        try {
            atomLibAmber = dbms.ui.wattosLib.atomLibAmber;
        } catch (RuntimeException e) {
        }

        if ( atomLibAmber != null ) {
            // Set atom types from Amber lib.
            for (int atomRid=atomsInMasterModel.nextSetBit(0);atomRid>=0;atomRid=atomsInMasterModel.nextSetBit(atomRid+1)) {
                int resRid = gumbo.atom.resId[atomRid];
                int typeId = atomLibAmber.getAtomTypeId(
                        gumbo.res.nameList[resRid],
                        gumbo.atom.nameList[atomRid]);
//                int modelCount = gumbo.atom.modelSiblingIds[atomRid].length;
                gumbo.atom.type[ atomRid ] = typeId;
//                for (int m=0;m<modelCount;m++) {
//                    gumbo.atom.type[ gumbo.atom.modelSiblingIds[atomRid][m] ] = typeId;
//                }
            }
        } else {
            General.showDebug("Skipping to assign amber atom types in postProcessAfterReading");
        }

        // Set atom types from Amber lib before
        if ( ! syncModels ) {
            return true;
        }
        return gumbo.atom.calcBond();
    }

    /**
     */
    public boolean readNomenclatureWHATIFPDB( URL url, String atomNomenclatureFlavor ) {
        // Check if there is only 1 entry selected now.
        BitSet selectedOrg = (BitSet) selected.clone();
        int selectedOrgCount = selectedOrg.cardinality();
        if ( selectedOrgCount != 1 ) {
            General.showError("In readNomenclatureWHATIFPDB; Expected 1 and only 1 selected entry but found: " + selectedOrgCount);
            return false;
        }
        int selectedEntryId = selectedOrg.nextSetBit(0);
        if ( selectedEntryId < 0 ) {
            General.showError("Failed to find a selected entry; weird, that's a code bug actually.");
            return false;
        }

        PdbFile pdbFile = new PdbFile( dbms );
        boolean status = pdbFile.readFile(url, atomNomenclatureFlavor);
        if ( ! status ) {
            General.showError("entry.readPdbFormattedFile was unsuccessful. Failed to read pdb formatted file");
            return false;
        }
        General.showOutput("Read a new entry from a PDB formatted coordinate list");

        //General.showDebug("DBMS: " + dbms.toString(true));

        BitSet selectedNew = (BitSet) selected.clone();
        int selectedNewCount = selectedNew.cardinality();
        if ( selectedNewCount != 2 ) {
            General.showError("Expected 2 and only 2 selected entries but found: " + selectedNewCount);
            return false;
        }
        int newEntryId = selectedNew.nextSetBit(0);
        if ( newEntryId == selectedEntryId ) {
            newEntryId = selectedNew.nextSetBit(newEntryId+1);
        }

        status = gumbo.atom.renameByEntry( selectedEntryId, newEntryId);
        if ( ! status ) {
            General.showError("atom.renameByEntry was unsuccessful.");
        }
        // Always try to remove the new entry.
        if ( ! mainRelation.removeRowCascading(newEntryId, true)) {
            General.showError("atom.renameByEntry was unsuccessful.");
            return false;
        }
        if ( ! postProcessAfterReading(true) ) {
            return false;
        }
        if ( status ) {
            General.showOutput("Done with nomenclature update from a PDB formatted coordinate list");
        }
        return status;
    }


    /** @see Atom#addAtomsByEntry
     */
    public boolean readEntryExtraCoordinatesWHATIFPDB( URL url, String atomNomenclatureFlavor ) {
        // Check if there is only 1 entry selected now.
        BitSet selectedOrg = (BitSet) selected.clone();
        int selectedOrgCount = selectedOrg.cardinality();
        if ( selectedOrgCount != 1 ) {
            General.showError("In readEntryExtraCoordinatesWHATIFPDB; Expected 1 and only 1 selected entry but found: " + selectedOrgCount);
            return false;
        }
        int selectedEntryId = selectedOrg.nextSetBit(0);
        if ( selectedEntryId < 0 ) {
            General.showError("Failed to find a selected entry; weird, that's a code bug actually.");
            return false;
        }

        PdbFile pdbFile = new PdbFile( dbms );
        boolean status = pdbFile.readFile(url, atomNomenclatureFlavor);
        if ( ! status ) {
            General.showError("entry.readPdbFormattedFile was unsuccessful. Failed to read pdb formatted file");
            return false;
        }
        General.showOutput("Read a new entry from a PDB formatted coordinate list");

        BitSet selectedNew = (BitSet) selected.clone();
        int selectedNewCount = selectedNew.cardinality();
        if ( selectedNewCount != 2 ) {
            General.showError("Expected 2 and only 2 selected entries but found: " + selectedNewCount);
            return false;
        }
        int newEntryId = selectedNew.nextSetBit(0);
        if ( newEntryId == selectedEntryId ) {
            newEntryId = selectedNew.nextSetBit(newEntryId+1);
        }

        status = false;
        try {
            status = gumbo.atom.addAtomsByEntry( selectedEntryId, newEntryId);
        } catch ( Throwable t ) {
            General.showThrowable(t);
        }
        if ( ! status ) {
            General.showError("atom.addAtomsByEntry was unsuccessful.");
        }
        // Always try to remove the new entry.
        if ( ! mainRelation.removeRowCascading(newEntryId, true)) {
            General.showError("atom.renameByEntry was unsuccessful.");
            status = false;
        }
        if ( ! syncModels( selectedEntryId ) ) {
            General.showError("syncModels on selected entry was unsuccessful.");
            status = false;
        }
        if ( ! postProcessAfterReading(true) ) {
            return false;
        }

        return status;
    }


    /** Possibly writing multiple files for the selected entries.
     */
    public boolean writePdbFormattedFileSet( String fn, Boolean generateStarFileToo, UserInterface ui ) {

        boolean usePostFixedOrdinalsAtomName = true;
//        if ( PdbVersion != null && PdbVersion.intValue() < 1 ) {
//            usePostFixedOrdinalsAtomName = false;
//        }
        String fileNameSTAR = InOut.changeFileNameExtension( fn, "str" );
        if ( ! writeNmrStarFormattedFileSet( fileNameSTAR, null, ui, usePostFixedOrdinalsAtomName ) ) {
            General.showError("Failed to first writeNmrStarFormattedFileSet");
            return false;
        }

        // Find the selected entries making sure that unused selected entries aren't included.
        selected.and( used ); // paranoia, all selected should be in use but you never know...
        BitSet selEntries = (BitSet) selected.clone();
        int entryCount = selEntries.cardinality();
        if ( entryCount == 0 ) {
            General.showWarning("No entry selected to be rewritten to PDB");
            return true;
        }
        if ( entryCount > 1 ) {
            General.showOutput("Using automatic numbering scheme for file names for entries selected based on the name: " + fn);
        }

        int fileCount = 0;
        for (int entryRID = selEntries.nextSetBit(0); entryRID >= 0; entryRID=selEntries.nextSetBit(entryRID+1)) {
            fileCount++;
            String inputFileName = fileNameSTAR;
            if ( selEntries.cardinality() != 1 ) {
                inputFileName = InOut.addFileNumberBeforeExtension( fileNameSTAR, fileCount, true, 3 );
            }
            String outputFileName = fn;
            if ( selEntries.cardinality() != 1 ) {
                outputFileName = InOut.addFileNumberBeforeExtension(          fn, fileCount, true, 3 );
            }
            boolean status = PdbWriter.processFile( inputFileName, outputFileName );
            if ( ! status ) {
                General.showError("Failed PdbWriter.processFile");
                return false;
            }
            if ( ! generateStarFileToo.booleanValue() ) {
                File f = new File(fileNameSTAR);
                if ( ! f.delete() ) {
                    General.showWarning("Failed to remove intermediate STAR file: " + fileNameSTAR);
                }
            }
        }
        General.showOutput("Done writing PDB formatted coordinate list entry/entries.");
        return true;


        /** Old method for writing
        // Find the selected entries making sure that unused selected entries aren't included.
        selected.and( used ); // paranoia, all selected should be in use but you never know...
        BitSet selEntries = (BitSet) selected.clone();
        //General.showDebug("Found number of selected entries: " + selEntries.cardinality());

        // If more than 1 entry needs to be written start automatic numbering scheme.
        if ( selEntries.cardinality() > 1 ) {
            General.showDebug("Will start automatic numbering scheme for entries.");
        }

        int fileCount = 0;
        int entryRID = selEntries.nextSetBit(0);
        for (; entryRID >= 0; entryRID=selEntries.nextSetBit(entryRID+1)) {
            fileCount++;
            String outputFileName = filename;
            if ( selEntries.cardinality() != 1 ) {
                outputFileName = InOut.addFileNumberBeforeExtension( filename, fileCount, true, 3 );
            }
            // Use a temporary dbms so the data doesn't clobber the regular one.
            DBMS dbmsTemp = getDBMSForEntry( entryRID );
            if ( dbmsTemp == null ) {
                General.showError("Failed to do entry.getDBMSForEntry( entryRID ). Not writing any more entries.");
                return false;
            }
            PdbFile pdbFile = new PdbFile( dbmsTemp );
            boolean status = pdbFile.writeFile( outputFileName );
            if ( ! status ) {
                General.showError("Failed to write pdb formatted file.");
                General.showError("Not writing any more entries.");
                return false;
            }
        }
         */
    }


    /** It will be read and appended to the list of entries.
     *The file may contain empty lines and comments by # starting a line.
     */
    public boolean readNmrStarFormattedFileSet( URL urlSet, String starVersion,
            UserInterface ui , boolean removeUnlinkedRestraints) {
        boolean status = true;
        URL url = null;
        try {
            BufferedInputStream bis = InOut.getBufferedInputStream(urlSet);
            BufferedReader br = new BufferedReader( new InputStreamReader(bis));

            String line = br.readLine();
            while ( line != null ) {
                line = line.trim();
                if ((line.length() == 0 ) || line.startsWith("#")) {
                    General.showDebug("Skipping line in entry list: [" + line + "]");
                    line = br.readLine();
                    continue;
                }
                url = InOut.getUrlFileFromName( line );
                if ( url == null ) {
                    General.showError("Failed to get URL from: " + line);
                }
                File31 file = new File31( dbms, ui );
                General.showOutput("Reading NMR-STAR file from URL: " + url);
                status = file.toWattos(url, true, true, true, false, removeUnlinkedRestraints,true);
                if ( ! status ) {
                    General.showError("Failed to convert File31 to Wattos from URL: " + url);
                    return false;
                }
                line = br.readLine();
            }
        } catch (Exception e) {
            General.showCodeBug( e.getMessage() );
            General.showError("Failed to readNmrStarFormattedFileSet from URL: " + url);
            return false;
        }
        if ( ! postProcessAfterReading(true) ) {
            return false;
        }
        General.showOutput("Done reading STAR formatted entry.");
        return status;
    }

    /** The name says it all. It will be read and appended to the list of entries.
     */
    public boolean readNmrStarFormattedFile( URL url, String starVersion,
            UserInterface ui, boolean doEntry, boolean doRestraints,
            boolean matchRestraints2Soup, boolean matchRestraints2SoupByAuthorDetails,
            boolean removeUnlinkedRestraints,
            boolean syncModels ) {
        boolean status = true;
        File31 file = null;
        try {
            file = new File31( dbms, ui );
        } catch (Exception e) {
            General.showCodeBug( e.getMessage() );
            return false;
        }
        status = file.toWattos(
                url,
                doEntry,
                doRestraints,
                matchRestraints2Soup,
                matchRestraints2SoupByAuthorDetails,
                removeUnlinkedRestraints,
                syncModels);
        if ( ! status ) {
            General.showError("entry.readNmrStarFormattedFile was unsuccessful. Failed to read nmrstar formatted file");
        } else {
            General.showOutput("Done reading STAR formatted entry.");
        }
        if ( ! postProcessAfterReading(syncModels) ) {
            return false;
        }
        return status;
    }

    /** The name says it all. It will be read and appended to the list of entries.
     */
    public boolean readmmCIFFormattedFile( URL url, UserInterface ui, boolean syncModels ) {
        boolean status = true;
        CIFCoord file = null;
        try {
            file = new CIFCoord( dbms, ui );
        } catch (Exception e) {
            General.showCodeBug( e.getMessage() );
            return false;
        }
        status = file.toWattos(url,syncModels);
        if ( ! status ) {
            General.showError("entry.readmmCIFFormattedFile was unsuccessful. Failed to read nmrstar formatted file");
        }
        if ( ! postProcessAfterReading(syncModels) ) {
            return false;
        }
        General.showOutput("Done readmmCIFFormattedFile.");
        return status;
    }

    /** Convenience method true for usePostFixedOrdinalsAtomName
     */
    public boolean writeNmrStarFormattedFileSet( String fn,
            String starVersion, UserInterface ui ) {
        return writeNmrStarFormattedFileSet(  fn,
                 starVersion,  ui, true);
    }


    /** The name says it all. It will be written to one or more files depending on selection.
     */
    public boolean writeNmrStarFormattedFileSet( String fn,
            String starVersion, UserInterface ui, boolean usePostFixedOrdinalsAtomName ) {

        // Find the selected entries making sure that unused selected entries aren't included.
        selected.and( used ); // paranoia, all selected should be in use but you never know...
        BitSet selEntries = (BitSet) selected.clone();
        int entryCount = selEntries.cardinality();
        if ( entryCount == 0 ) {
            General.showWarning("No entry selected to be written");
            return true;
        }
        if ( entryCount > 1 ) {
            General.showOutput("Will start automatic numbering scheme for file names for entries selected based on the name: " + fn);
        }

        int fileCount = 0;
        for (int entryRID = selEntries.nextSetBit(0); entryRID >= 0; entryRID=selEntries.nextSetBit(entryRID+1)) {
            fileCount++;
            String outputFileName = fn;
            if ( selEntries.cardinality() != 1 ) {
                outputFileName = InOut.addFileNumberBeforeExtension( fn, fileCount, true, 3 );
            }
            // Use a temporary dbms so the data doesn't clobber the regular one.
            DBMS dbmsTemp = getDBMSForEntry( entryRID );
            if ( dbmsTemp == null ) {
                General.showError("Failed to do entry.getDBMSForEntry( entryRID ). Not writing any more entries");
                return false;
            }
            File31 file = null;
            try {
                file = new File31( dbmsTemp, ui );
            } catch (Exception e) {
                String msg = e.getMessage();
                if ( msg == null ) {
                    msg = "In Entry, failed to create a new File31 object(dbmsTemp, ui )";
                }
                General.showCodeBug( msg );
                return false;
            }
            boolean status = file.toSTAR(outputFileName, usePostFixedOrdinalsAtomName );
            if ( ! status ) {
                General.showError("entry.writeNmrStarFormattedFileSet was unsuccessful. Failed to write nmrstar formatted file");
                General.showError("Not writing any more entries");
                return false;
            }
        }
        General.showOutput("Done writing STAR formatted entry/entries.");
        return true;
    }

    /** Write one or more files depending on selection and current possibilities.
     */
    public boolean writeXplorFormattedConstraints( String fn, int entryRID,
            UserInterface ui, String atomNomenclature,boolean sortRestraints ) {
        BitSet ridsDCListInEntry = SQLSelect.selectBitSet(dbms, ui.constr.dcList.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
            SQLSelect.OPERATION_TYPE_EQUALS, new Integer( entryRID ), false);
        if ( ! ui.constr.dcList.toXplorOrSo(ridsDCListInEntry, fn, atomNomenclature,sortRestraints, AtomMap.NOMENCLATURE_ID_XPLOR)) {
            General.showError("Failed to write distance restraints; skipping other restraints.");
            return false;
        }
        SimpleConstrList[] simpleConstraintLoL = new SimpleConstrList[] {
                ui.constr.cdihList,
                ui.constr.rdcList
        };
        for ( int i=0;i<simpleConstraintLoL.length;i++) {
            if ( ! simpleConstraintLoL[i].toXplorOrSo(entryRID, fn, atomNomenclature,sortRestraints, null)) {
                General.showError("Failed to write distance restraints; skipping other restraints.");
                return false;
            }
        }
        return true;
    }


    public int getFirstModelInEntry(int entryRID) {
        BitSet ridsModelInEntry = getModelsInEntry(entryRID);
        if ( ridsModelInEntry == null ) {
            General.showError("Failed to getModelsInEntry");
            return -1;
        }
        int firstModelRid = -1;
        for (int r=ridsModelInEntry.nextSetBit(0);r>=0;r=ridsModelInEntry.nextSetBit(r+1)) {
            if ( gumbo.model.number[r] == 1 ) {
                firstModelRid = r;
                break;
            }
        }
        if ( firstModelRid < 0 ) {
            General.showError("Failed to find first model in entry");
            return -1;
        }
        return firstModelRid;
    }

    /** Write one or more files depending on selection and current possibilities.
     * @param format TODO
     */
    public boolean writeXplorOrSoFormattedSequenceList( String fn, int entryRID,
            UserInterface ui, String format  ) {
        int firstModelRid = getFirstModelInEntry(entryRID);
        if ( firstModelRid < 0 ) {
            General.showError("Failed to find first model in entry");
            return false;
        }
        BitSet ridsMolInModel = SQLSelect.selectBitSet(dbms,
                gumbo.mol.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer( firstModelRid ),
                false);
        return gumbo.mol.toXplorOrSor(ridsMolInModel, fn, format);
    }


    /** The name says it all. It will be written to one or more files depending on selection.
     * @param format TODO
     */
    public boolean writeXplorOrSoFormattedFileSet( String fn,
            UserInterface ui, String atomNomenclature, boolean sortRestraints, String format ) {

        // Find the selected entries making sure that unused selected entries aren't included.
        selected.and( used ); // paranoia, all selected should be in use but you never know...
        BitSet selEntries = (BitSet) selected.clone();
        int entryCount = selEntries.cardinality();
        if ( entryCount == 0 ) {
            General.showWarning("No entry selected to be written");
            return true;
        }
        if ( entryCount > 1 ) {
            General.showOutput("Will start automatic numbering scheme for file names for entries selected based on the name: " + fn);
        }

        int fileCount = 0;
        for (int entryRID = selEntries.nextSetBit(0); entryRID >= 0; entryRID=selEntries.nextSetBit(entryRID+1)) {
            fileCount++;
            String outputFileName = fn;
            if ( selEntries.cardinality() != 1 ) {
                outputFileName = InOut.addFileNumberBeforeExtension( fn, fileCount, true, 3 );
//                General.showDebug("outputFileName in writeXplorFormattedFileSet: " + outputFileName);
            }
            boolean status = writeXplorOrSoFormattedSequenceList( outputFileName,entryRID, ui, format );
            if ( ! status ) {
                General.showError("Failed entry.writeXplorFormattedSequenceList.");
                General.showError("Not writing any more entries");
                return false;
            }
            status = writeXplorFormattedConstraints( outputFileName,entryRID,ui,atomNomenclature,sortRestraints);
            if ( ! status ) {
                General.showError("Failed entry.writeXplorFormattedDC.");
                General.showError("Not writing any more entries");
                return false;
            }
        }
        General.showOutput("Done writing xplor formatted entry/entries.");
        return true;
    }

    /** Returns a copy of the dbms with all but given entry removed. This is
     *very expensive in memory but quite speedy. The routine also removes
     *any unselected components from the dbms afterwards and then
     *renumbers the components in the gumbo.
     *Returns null on error.
     */
    public DBMS getDBMSForEntry( int entryRID ) {

        boolean overall_status = true;
        DBMS resultDbms = null;
        try {
            resultDbms = (DBMS) Objects.deepCopy( dbms );
            if ( resultDbms == null ) {
                General.showMemoryUsed();
                General.showError("Failed to get a deep copy of the whole DBMS. Did we run out of memory?");
                return null;
            }
            /** Remove all but entry to be written */
            BitSet rowSet = (BitSet) mainRelation.used.clone();
            rowSet.clear(entryRID);
            Relation resultEntryMain = resultDbms.getRelation( mainRelation.name );
            RelationSet resultRelationSet = resultEntryMain.relationSetParent;
            Gumbo resultGumbo = (Gumbo) resultRelationSet.relationSoSParent;
            resultEntryMain.removeRowsCascading(rowSet, true);

            /**
            General.showDebug("Number of atoms in gumbo before removeUnselectedRowsInAllRelationsWithSelectionCapability: " +
                                                          gumbo.atom.mainRelation.used.cardinality());
            General.showDebug( "Produced entries: "     + gumbo.entry.mainRelation.toString() );
            General.showDebug( "Produced models: "      + gumbo.model.mainRelation.toString() );
            General.showDebug( "Produced mols: "        + gumbo.mol.mainRelation.toString() );
            //General.showDebug( "Produced residues: "    + gumbo.res.mainRelation.toString() );
            //General.showDebug( "Produced atoms: "       + gumbo.atom.mainRelation.toString() );
             */

            // Remove all that isn't selected
            if ( ! resultDbms.removeUnselectedRowsInAllRelationsWithSelectionCapability()) {
                throw new Exception("Failed to remove the non-selected rows in all relations that have a selection column.");
            }
            // Renumber all in new gumbo
            if ( ! resultGumbo.renumberRows(true)) {
                throw new Exception("Failed to reset numbers of all components (including the atoms) in the gumbo.");
            }
        } catch ( Exception e ) {
            General.showThrowable(e);
            General.showError("Failed to reduce the dbms to just the data for 1 entry for just the selected components.");
            overall_status = false;
        }
        if ( overall_status && resultDbms != null ) {
            return resultDbms;
        }
        General.showError("Failed to reduce the dbms to just the data for 1 entry for just the selected components.");
        return null;
    }


    public BitSet getModelsInEntry(int entryRID) {
        BitSet modelsInEntry = SQLSelect.selectBitSet(dbms, gumbo.model.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
            SQLSelect.OPERATION_TYPE_EQUALS, new Integer( entryRID ), false);
        return modelsInEntry;
    }

    /** Generates a list for each atom in the first model with itself and siblings in parallel models.
     *Then it sets the sync-ed attribute for this entry. The attribute is described in the Gumbo class.
     *It will print an error message for the first couple of (10) atoms missing AND it will delete atoms for
     *which atoms are not represented in all models.
     *Empty models, mols, residues (those without atoms) will NOT be removed at this stage.
     *Also fills the ridAtomInMaster array.
     */
    public boolean syncModels( int entryRID ) {
        int showedMessageCountMax = 10;
        int atomRIDFoundInt;

//        General.showDebug("Starting syncModels");
        if ( ! mainRelation.used.get( entryRID ) ) {
            General.showError("Failed to do syncModels. Entry rid isn't in use for rid: " + entryRID);
            return false;
        }

        BitSet atomsInEntry = SQLSelect.selectBitSet(dbms, gumbo.atom.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
            SQLSelect.OPERATION_TYPE_EQUALS, new Integer( entryRID ), false);
        if ( atomsInEntry == null ) {
            General.showError("Failed to do syncModels because failed to get the atoms in this entry for rid: " + entryRID);
            return false;
        }
        BitSet atomsToDelete = new BitSet();
        BitSet modelsInEntry = getModelsInEntry(entryRID);
        if ( modelsInEntry == null ) {
            General.showError("Failed to do syncModels because failed to get the models in this entry for rid: " + entryRID);
            return false;
        }
        int modelCount = modelsInEntry.cardinality();
//        General.showDebug( "Found number of models in entry: " + modelCount);

        BitSet atomsInMasterModel = getAtomsInMasterModel(); // repeats some of the previous actions.
        if ( atomsInMasterModel == null ) {
            General.showError("Failed to do syncModels because failed to get the atoms in first model for this entry for rid: " + entryRID);
            return false;
        }
//        int atomCountMasterModel = atomsInMasterModel.cardinality();
//        General.showDebug("Found number of atoms in master model: " + atomCountMasterModel);
        // Reserve the space. for 10 million atoms this will be 10^7 * 4 bytes per int = 40 Mb without
        // even using the overhead that int[] have. Only 10^7/50 is 2*10^5 objects though.
        Relation.createIntArrays(gumbo.atom.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_MODEL_SIBLINGS, atomsInMasterModel, modelCount);


        /** Generate a cache of the atom rid in the first model so there's a quick pointer possbile.
        HashOfHashesOfHashes has keys: mol number, res number, atom name
        order of magnitude:   10      *   100     *  20 = 20,000 keys (1,000 hash maps).
        To stick with the numbers in the package doc you need to multiply this with 10 for the estimated 10 entries
        Wattos should be able to handle at the same time.
         */
        HashOfHashesOfHashes atomFirstRID = new HashOfHashesOfHashes();
        Integer molNumber = null;
        Integer resNumber = null;
        HashMap prevHashMapRes = null;
        int prevResRID = -1;
        for (int i=atomsInMasterModel.nextSetBit(0); i>=0; i=atomsInMasterModel.nextSetBit(i+1))  {
            if ( gumbo.atom.resId[i] != prevResRID ) { // gets executed only once per residue. speed gain of factor ~20.
                molNumber   = new Integer( gumbo.mol.number[ gumbo.atom.molId[i] ]);
                prevResRID  = gumbo.atom.resId[i];
                resNumber   = new Integer( gumbo.res.number[ prevResRID ]);
                prevHashMapRes = atomFirstRID.get( molNumber, resNumber );
                if ( prevHashMapRes == null ) {
                    HashOfHashes tmpMap = atomFirstRID.getHashOfHashes( molNumber );
                    if ( tmpMap == null ) { // level 2 is even absent
                        tmpMap = new HashOfHashes();
                        atomFirstRID.put( molNumber, tmpMap );
                    }
                    prevHashMapRes = new HashMap();
                    tmpMap.put( resNumber, prevHashMapRes );
                }
            }
            prevHashMapRes.put( gumbo.atom.nameList[i], new Integer(i));
            // fill array with the null value indicating no match has been made
            Arrays.fill( gumbo.atom.modelSiblingIds[i], Defs.NULL_INT);
            gumbo.atom.modelSiblingIds[i][0] = i; // kind of redundant of course.
            gumbo.atom.masterAtomId[i]=i; // and the reverse index, still kind of redundant.
        }


//        General.showDebug("Set the siblings ids for atoms in models 2 and on.");
        for (int m=2;m<=modelCount;m++) {
            //General.showDebug("Working on syncing model with number: " + m);
            int modelRid = gumbo.model.getModelRidWithNumber( modelsInEntry, m );
            if ( modelRid < 0 ) {
                General.showError("Failed to do syncModels because failed to get the model's rid for model with number: " + m);
                return false;
            }
            BitSet atomsInModel = SQLSelect.selectBitSet(dbms, gumbo.atom.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer( modelRid ), false);
            if ( atomsInModel == null ) {
                General.showError("Failed to do syncModels because failed to get the atoms in model with number: " + m);
                return false;
            }
            molNumber = null;
            resNumber = null;
            prevHashMapRes = null;
            prevResRID = -1;
            for (int i=atomsInModel.nextSetBit(0); i>=0; i=atomsInModel.nextSetBit(i+1))  {
                //General.showDebug("Working on syncing model with atom with name: " + gumbo.atom.nameList[i] + " and rid: " + i);
                if ( gumbo.atom.resId[i] != prevResRID ) {
                    prevResRID = gumbo.atom.resId[i];
                    resNumber = new Integer( gumbo.res.number[ prevResRID ]);
                    molNumber   = new Integer( gumbo.mol.number[ gumbo.atom.molId[i] ]);
                    prevHashMapRes = atomFirstRID.get( molNumber, resNumber );
                    if ( prevHashMapRes == null ) {
                        //General.showDebug("Will remove unsynced atom: " + gumbo.atom.toString(i));
                        atomsToDelete.set( i );
                        prevResRID = -1; // take no chances as whole residues might not exist as residues 124 in entry 1krw
                        continue;
                    }
                }
                Integer atomRIDFound = (Integer) prevHashMapRes.get( gumbo.atom.nameList[i] );
                if ( atomRIDFound == null ) {
                    //General.showDebug("Will remove unsynced atom: " + gumbo.atom.toString(i));
                    atomsToDelete.set( i );
                    continue;
                }
                //General.showDebug("Adding to atom in first model with rid: " + atomRIDFound);
                atomRIDFoundInt = atomRIDFound.intValue();
                gumbo.atom.modelSiblingIds[atomRIDFoundInt][m-1] = i; // m-1 because models start numbering at 1.
                gumbo.atom.masterAtomId[i] = atomRIDFoundInt; // and the reverse index.
            } // end loop over atoms in model.
        } // end loop over models 2 and on.

        int showedMessageCount = 0;
        boolean showedBeginMessage = false;
        boolean showedFinalMessage = false;
        // Add atoms to delete from first/master model if sync list is incomplete.
        for (int i=atomsInMasterModel.nextSetBit(0); i>=0; i=atomsInMasterModel.nextSetBit(i+1))  {
            for (int j=0;j<modelCount;j++) {
                if ( Defs.isNull( gumbo.atom.modelSiblingIds[i][j]) ) {
                    if ( showedMessageCount < showedMessageCountMax ) {
                        if ( ! showedBeginMessage ) {
                            General.showDebug("Deleting following atoms in first/master model and related ones:");
                            showedBeginMessage = true;
                        }
//                        General.showDebug("Sync list is incomplete for atom in master atom: " + i + " and model id: " + j);
                        General.showDebug(gumbo.atom.toString(i));
//                        General.showDebug(PrimitiveArray.toString(gumbo.atom.modelSiblingIds[i]));
                        showedMessageCount++;
                    } else {
                        if ( ! showedFinalMessage ) {
                            General.showDebug(" and possibly more." );
                            showedFinalMessage = true;
                        }
                    }
                    for (int jj=0;jj<modelCount;jj++) {
                        if ( ! Defs.isNull( gumbo.atom.modelSiblingIds[i][jj]) ) {
                            atomsToDelete.set(gumbo.atom.modelSiblingIds[i][jj]);
                        }
                    }
                    break;
                }
            }
        }

        if ( atomsToDelete.cardinality() != 0 ) {
            General.showDebug("Will remove unsynced atoms: " + atomsToDelete.cardinality() + General.eol +
                    gumbo.atom.toString(atomsToDelete));
            if ( ! gumbo.atom.mainRelation.removeRowsCascading( atomsToDelete, true)) {
                General.showError("Failed to remove atoms that aren't represented in all models.");
                return false;
            }
            // Do a recursive call in order to remove the invalid entries in atomFirstRID.
            if ( ! syncModels(entryRID)) {
                General.showError("Failed iterative call to syncmodels");
                return false;
            }
        } else { // only the iteration that has no atoms to delete will set the sync-ed records.
            if ( ! setModelsSynced( entryRID, true, atomFirstRID ) ) {
                General.showError("Failed to set the entry to have synced models");
                return false;
            }
        }
        /** Top down is the fastest but probably not important if few atoms are off.
        gumbo.model.removeWithoutAtom();
        gumbo.mol.removeWithoutAtom();
        gumbo.res.removeWithoutAtom();

        modelsInEntry = SQLSelect.selectBitSet(dbms, gumbo.model.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
            SQLSelect.OPERATION_TYPE_EQUALS, new Integer( entryRID ), false);
        if ( modelsInEntry == null ) {
            General.showError("Failed to do syncModels AT THE END of routine because failed to get the models in this entry for rid: " + entryRID);
            return false;
        }
        if ( modelCount != modelsInEntry.cardinality()) {
            General.showDebug( "Resyncing because number of models changed from: " + modelCount);
            General.showDebug( "to: " + modelsInEntry.cardinality());
            return syncModels( entryRID ); // let's hope it doesn't cycle too often;-)
        }
         */

        return true;
    }


    /** Convenience wrapper also nice to indicate/document it's importance.
     *Return true for success.
     */
    public boolean setModelsSynced( int entryRID, boolean value, HashOfHashesOfHashes atomFirstRID ) {
        if ( ! used.get( entryRID ) ) {
            return false;
        }
        modelsSynced.set( entryRID, value );
        atomsHash[entryRID] = atomFirstRID;
        return true;
    }

    /** Convenience wrapper also nice to indicate/document it's importance.
     *Return whether or not the entry's models are synced.
     */
    public boolean getModelsSynced( int entryRID ) {
        if ( ! used.get( entryRID ) ) {
            return false;
        }
        return modelsSynced.get( entryRID );
    }

    /**     */
    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();
        orfIdList        = (Object[])  mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_ORF_ID_LIST);       // non fkcs
        atomsHash        = (Object[])  mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_ATOMS_HASH);
        assemblyNameList = (String[])  mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_ASSEMBLY_NAME);
        pdbCoordinateFileVersionNameList = (String[])  mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_PDB_COORDINATE_FILE_VERSION);
        modelsSynced     = (BitSet)    mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_MODELS_SYNCED);
        return true;
    }

   /** Adds a new entry in the array. The orf id list can be null;
     *Returns -1 for failure.
     */
    public int add(String entryName, OrfIdList entryOrfIdList, String assemblyName) {
        int result = super.add( entryName );
        if ( result < 0 ) {
            General.showCodeBug( "Failed to get a new row id for an entry with name: " + entryName);
            return -1;
        }
        orfIdList[          result ] = entryOrfIdList;
        assemblyNameList[   result ] = assemblyName;
        number[             result ] = mainRelation.sizeRows;
        return result;
    }

    public boolean truncateEnsembleToMaxResidues(int maxResidueCountTotal) {
        int entryRID = getEntryId();
        BitSet modelSet = getModelsInEntry( entryRID );
        if ( modelSet == null ) {
            General.showError("Failed to get even an empty set of models in truncateEnsembleToMaxResidues");
            return false;
        }
        int modelCount = modelSet.cardinality();
        BitSet resMasterModelSet = getResInMasterModel();
        int resCountSingleModel = resMasterModelSet.cardinality();
        int resCountTotal = resCountSingleModel * modelCount;
        int modelCountAllowed = maxResidueCountTotal / resCountSingleModel;
        int modelCountToRemove = Math.max( modelCount - modelCountAllowed, 0 );

        General.showOutput("Model count:                    " + modelCount);
        General.showOutput("Residue count (single model):   " + resCountSingleModel);
        General.showOutput("Residue count (all):            " + resCountTotal);
        General.showOutput("Residue count allowed (all):    " + maxResidueCountTotal);
        General.showOutput("Model count allowed:            " + modelCountAllowed);
        General.showOutput("Model count to remove:          " + modelCountToRemove);

        if (modelCountToRemove > 0) {
            BitSet modelSetToRemove = (BitSet) modelSet.clone();
            for (int i = modelSetToRemove.nextSetBit(0); i >= 0; i=modelSetToRemove.nextSetBit(i+1)) {
                int modelNumber = gumbo.model.number[i];
                if ( modelNumber > modelCountAllowed ) {
//                    General.showDebug("Removing model number: " + modelNumber);
                    gumbo.model.mainRelation.removeRowCascading(i, true); // Cascades and removes indices automatically.
                }
            }
            BitSet modelSetLeft = getModelsInEntry( entryRID );
            if ( modelSetLeft == null ) {
                General.showError("Failed to get even an empty set of models LEFT in truncateEnsembleToMaxResidues");
                return false;
            }
//            General.showOutput("Model count left:               " + modelSetLeft.cardinality());
        }
        return true;
    }

    public boolean truncateEnsembleToMaxModels(int maxModelCountTotal) {
        int entryRID = getEntryId();
        BitSet modelSet = getModelsInEntry( entryRID );
        if ( modelSet == null ) {
            General.showError("Failed to get even an empty set of models in truncateEnsembleToMaxModels");
            return false;
        }
        int modelCount = modelSet.cardinality();
        int modelCountToRemove = Math.max( modelCount - maxModelCountTotal, 0 );

        General.showOutput("Model count " + modelCount + " with allowed: " + maxModelCountTotal+
                " and to remove: " + modelCountToRemove);
        if (modelCountToRemove > 0) {
            BitSet modelSetToRemove = (BitSet) modelSet.clone();
            for (int i = modelSetToRemove.nextSetBit(0); i >= 0; i=modelSetToRemove.nextSetBit(i+1)) {
                int modelNumber = gumbo.model.number[i];
                if ( modelNumber > maxModelCountTotal ) {
//                    General.showDebug("Removing model number: " + modelNumber);
                    gumbo.model.mainRelation.removeRowCascading(i, true); // Cascades and removes indices automatically.
                }
            }
            BitSet modelSetLeft = getModelsInEntry( entryRID );
            if ( modelSetLeft == null ) {
                General.showError("Failed to get even an empty set of models LEFT in truncateEnsembleToMaxModels");
                return false;
            }
//            General.showOutput("Model count left:               " + modelSetLeft.cardinality());
        }
        return true;
    }


    /** OLD
    public boolean calculateHydrogenBonds( String dcListName, float percentagePresent,
        float minimumEnergy ) {
        // Find the selected entries making sure that unused selected entries aren't included.
        selected.and( used ); // paranoia, all selected should be in use but you never know...
        BitSet selEntries = (BitSet) selected.clone();
        int entryCount = selEntries.cardinality();
        if ( entryCount == 0 ) {
            General.showWarning("No entry selected");
            return true;
        }
        if ( entryCount > 1 ) {
            General.showError("Expected no more than 1 entry to be selected, but found: " + entryCount);
            return false;
        }

        // Find the possible hydrogen bond donors and acceptors in the first model
        // loop over all models.
        int ridN = -1;
        int ridH = -1;
        int ridO = -1;
        int ridC = -1;
        gumbo.atom.mainRelation.insertColumn("isHBDonor",Relation.DATA_TYPE_BIT,null);
        gumbo.atom.mainRelation.insertColumn("isHBAccep",Relation.DATA_TYPE_BIT,null);
        float HBenergy = Calculation.calculateHydrogenBondEnergyKS( gumbo.atom,
            ridN, ridH, ridO, ridC);


    return true;
    }
     */

}

