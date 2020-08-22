/*
 * CIFCoord.java
 *
 * Created on June 19, 2006, 10:58 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.Star.MmCif;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

import Wattos.CloneWars.UserInterface;
import Wattos.Common.OrfIdList;
import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.SQLSelect;
import Wattos.Soup.Atom;
import Wattos.Soup.Chemistry;
import Wattos.Soup.Entry;
import Wattos.Soup.Gumbo;
import Wattos.Soup.Model;
import Wattos.Soup.Molecule;
import Wattos.Soup.Residue;
import Wattos.Star.DataBlock;
import Wattos.Star.StarFileReader;
import Wattos.Star.StarNode;
import Wattos.Star.TagTable;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.ObjectIntMap;
import Wattos.Utils.Strings;

/**
 * Fri Jun 15 15:01:43 CDT 2007
 * @author jurgen
 */
public class CIFCoord {
    StarNode topNode;
    UserInterface ui;

    public static ArrayList dataTypeTranslationExceptions = new ArrayList();

    /** BEGIN BLOCK COPY FROM Wattos.Soup.PdbFile */
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

    int previousAtomId  = Defs.NULL_INT;
    int currentAtomId   = Defs.NULL_INT;
    int currentResId    = Defs.NULL_INT;
    int currentMolId    = Defs.NULL_INT;
    int currentModelId  = Defs.NULL_INT;
    int currentEntryId  = Defs.NULL_INT;
//    public String tagNameEntryId;
//    public String tagNameEntryName;
//    public String tagNameMolEntryId;
    public String tagNameMolId;
    public String tagNameMolAssEntityId;
//    public String tagNameMolAssEntityLabel;
//    public String tagNameMolSFCategory;
    public String tagNameMolEntityId;
    public String tagNameMolType;
    public String tagNameMolName; // same values as another?
    public String tagNameMolPolType;
    public String tagNameMolPolMolId;

//    public String tagNameMolSeqLength;
//    public String tagNameMolSeq;
//    public String tagNameResEntityId;
    public String tagNameResNum;
    public String tagNameResCompId;
    public String tagNameResMolId;
//    public String tagNameResResId;
//    public String tagNameResNum_2;
    public String tagNameResName;
    public String tagNameResNonPolMolId;
    public String tagNameResNonPolCompId;
    public String tagNameResNonPolNum;

//    public String tagNameChem_compSFCategory;
//    public String tagNameChem_compId;
//    public String tagNameChem_compType;

//    public String tagNameAtomSFCategory;
//    public String tagNameAtomSFId_1;
//    public String tagNameAtomSFId_2;
    public String tagNameAtomModelId;
    public String tagNameAtomId;
    public String tagNameAtomAsym_ID;
    public String tagNameAtomEntityId;
    public String tagNameAtomResId;
    public String tagNameAtomResName;
    public String tagNameAtomName;
    public String tagNameAtomAuthMolId;
    public String tagNameAtomAuthResId;
    public String tagNameAtomAuthResName;
    public String tagNameAtomAuthName;
    public String tagNameAtomPdbInsertionCode;
    public String tagNameAtomElementId;
    public String tagNameAtomCoorX;
    public String tagNameAtomCoorY;
    public String tagNameAtomCoorZ;
    public String tagNameAtomBFactor;
    public String tagNameAtomOccupancy;

    /** END BLOCK */

    static {
        dataTypeTranslationExceptions.add("_struct_asym.id");
        dataTypeTranslationExceptions.add("_atom_site.label_asym_id");

    }
    /** Creates a new instance of File31 */
    public CIFCoord(DBMS dbms, UserInterface ui) throws Exception {
//        this.topNode = topNode;
        this.dbms    = dbms;
        this.ui      = ui;
        if ( ! initConvenienceVariables() ) {
            throw new Exception( "Failed to initConvenienceVariables" );
        }
        if ( ! initConvenienceVariablesStar() ) {
            throw new Exception( "Failed to initConvenienceVariablesStar" );
        }
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
        atomMain   = atom.mainRelation;
        resMain    = res.mainRelation;
        molMain    = mol.mainRelation;
        modelMain  = model.mainRelation;
        entryMain  = entry.mainRelation;
        return true;
    }
    /** END BLOCK */

    /**
     * BEGIN BLOCK FOR SETTING NMR-STAR CONVENIENCE VARIABLES COPY FROM Wattos.Star.MmCif.CIFCoord
     */
    public boolean initConvenienceVariablesStar() {
        StarDictionary starDict = ui.wattosLib.starDictionary;
        // Please note that the following names are not hard-coded as star names necessarily (like in mol_id)
        try {
            tagNameMolId                      = (String) ((ArrayList)starDict.toCIF2D.get( "mol_main",       "mol_id"                                       )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameMolAssEntityId             = (String) ((ArrayList)starDict.toCIF2D.get( "mol_main",       "_Entity_assembly.Entity_ID"                   )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameMolEntityId                = (String) ((ArrayList)starDict.toCIF2D.get( "mol_main",       "_Entity.ID"                                   )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameMolType                    = (String) ((ArrayList)starDict.toCIF2D.get( "mol_main",       "type"         )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameMolName                    = (String) ((ArrayList)starDict.toCIF2D.get( "mol_main",       "name"         )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameMolPolType                 = (String) ((ArrayList)starDict.toCIF2D.get( "mol_main",       "pol_type"         )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameMolPolMolId                = (String) ((ArrayList)starDict.toCIF2D.get( "mol_main",       "_entity_poly.entity_id" )).get(StarDictionary.POSITION_CIF_TAG_NAME);



            tagNameResNum                     = (String) ((ArrayList)starDict.toCIF2D.get( "res_main",       "res_id"                       )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameResCompId                  = (String) ((ArrayList)starDict.toCIF2D.get( "res_main",       "name"                   )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameResMolId                   = (String) ((ArrayList)starDict.toCIF2D.get( "res_main",       "mol_id"                                       )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameResName                    = (String) ((ArrayList)starDict.toCIF2D.get( "res_main",       Relation.DEFAULT_ATTRIBUTE_NAME                                         )).get(StarDictionary.POSITION_CIF_TAG_NAME);

            tagNameResNonPolMolId             = (String) ((ArrayList)starDict.toCIF2D.get( "res_main",     "_pdbx_nonpoly_scheme.asym_id"         )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameResNonPolCompId            = (String) ((ArrayList)starDict.toCIF2D.get( "res_main",     "_pdbx_nonpoly_scheme.mon_id"         )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameResNonPolNum               = (String) ((ArrayList)starDict.toCIF2D.get( "res_main",     "_pdbx_nonpoly_scheme.ndb_seq_num"         )).get(StarDictionary.POSITION_CIF_TAG_NAME);

            tagNameAtomModelId                = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       "_Atom_site.Model_ID"                         )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomId                     = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       "_Atom_site.ID"                               )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomAsym_ID                = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       "_Atom_site.Label_asym_ID"         )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomEntityId               = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       "_Atom_site.Label_entity_ID"                  )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomResId                  = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       "_Atom_site.Label_comp_index_ID"              )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomResName                = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       "_Atom_site.Label_comp_ID"                    )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomName                   = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Relation.DEFAULT_ATTRIBUTE_NAME                                        )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomAuthMolId              = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME                               )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomAuthResId              = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID                                 )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomAuthResName            = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME                               )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomAuthName               = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME                              )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomPdbInsertionCode       = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE                              )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomElementId              = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID                                  )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomCoorX                  = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_COOR_X                                      )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomCoorY                  = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_COOR_Y                                      )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomCoorZ                  = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_COOR_Z                                      )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomBFactor                = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_BFACTOR                                    )).get(StarDictionary.POSITION_CIF_TAG_NAME);
            tagNameAtomOccupancy              = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main",       Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY                                    )).get(StarDictionary.POSITION_CIF_TAG_NAME);
        } catch ( Exception e ) {
            General.showThrowable(e);
            General.showError("Failed to get all the tag names from dictionary compare code with dictionary");
            return false;
        }
        return true;
    }
    /** END BLOCK */



    /**
     * Creates a new instance of CIFCoord
     */
    public CIFCoord() {
    }

    /** Convert the data in the file to components in the gumbo etc.
       Will list max of one conversion error per column in db.
     *Deselects all other entries in DBMS until now.
     *If atoms in restraint data can not be linked to atoms in coordinate list they will be marked in:
     *dc.hasUnLinkedAtom.
     */
    public boolean toWattos(URL url, boolean syncModels) {

        BitSet atomSetBad = new BitSet();
        atomSetBad.clear(); // Unnecessary but clear.

        // Use a temporary dbms so the data doesn't clobber the regular one.
        DBMS dbmsTemp = new DBMS();
        StarFileReader sfr = new StarFileReader(dbmsTemp);
        StarNode sn = sfr.parse( url );
        if ( sn == null ) {
            General.showError("toWattos was unsuccessful. Failed to read mmcif formatted file");
            return false;
        }
        Object o_tmp_1 = sn.get(0);
        if ( !(o_tmp_1 instanceof DataBlock)) {
            General.showError("Expected top level object of type DataBlock but got: " + o_tmp_1.getClass().getName());
            return false;
        }
        DataBlock db = (DataBlock) o_tmp_1;

//        General.showDebug( "Read star file was: "+db.toSTAR());

        StarDictionary starDict = ui.wattosLib.starDictionary;
        if ( starDict == null ) {
            General.showCodeBug("Couldn find the star dictionary in ui.wattosLib");
            return false;
        }
        boolean isMMCIF = true;
        boolean status = db.translateToNativeTypesByDict( starDict, isMMCIF);
        if ( ! status ) {
            General.showError("Failed to automatically convert to native types as defined in the star dictionary. Check input and try again please.");
            return false;
        }
//        General.showDebug("Automatically converted to native types as defined in the star dictionary.");

        /**
_pdbx_version.entry_id        1AI0
_pdbx_version.revision_date   2007-03-16
_pdbx_version.major_version   3
_pdbx_version.minor_version   100
_pdbx_version.details         ?
        */
        TagTable tTPdbxVersion = db.getTagTable("_pdbx_version.major_version",true);

        /**
            _struct_asym.id
            _struct_asym.pdbx_blank_PDB_chainid_flag
            _struct_asym.pdbx_modified
            _struct_asym.entity_id
            _struct_asym.details
            A N N 1 ?
            B N N 2 ?
            */
        TagTable tTStructAsym = db.getTagTable(tagNameMolId,true);
        /**
            _entity.id
            _entity.type
            _entity.pdbx_description

            ...
            1 polymer     syn 55-MER                      17626.533 1 ? ? ?                                     ?
            2 polymer     man 'Transcription factor IIIA' 10299.908 1 ? ? 'zinc fingers 4-6 (residues 127-212)' ?
            3 non-polymer syn 'ZINC ION'                  65.380    3 ? ? ?
            */
        TagTable tTEntity     = db.getTagTable(tagNameMolEntityId,true);
        /** _entity_poly.entity_id                      1
            _entity_poly.type                           polypeptide(L) */
        TagTable tTEntityPoly = db.getTagTable(tagNameMolPolType,false);  // if present

        /** _entity_poly_seq.entity_id
            _entity_poly_seq.num
            _entity_poly_seq.mon_id
            1  1 MET
            1  2 SER etc... Optional for when there are no polymers.*/
        TagTable tTEntityPolySeq = db.getTagTable(tagNameResMolId,false); // if present

        /** _pdbx_nonpoly_scheme.asym_id
            _pdbx_nonpoly_scheme.entity_id
            _pdbx_nonpoly_scheme.mon_id
            _pdbx_nonpoly_scheme.ndb_seq_num # important tag for numbering within asym
            B 2 MG  1 201 201 MG  MG  ? .
            C 3 GDP 1 200 200 GDP GDP ? .
            D 4 HOH 1 1   1   HOH HOH ? .
            D 4 HOH 2 2   2   HOH HOH ? .             */
        TagTable tTEntityNonPoly = db.getTagTable(tagNameResNonPolMolId,true);

        /**
_atom_site.group_PDB
_atom_site.id
_atom_site.type_symbol
_atom_site.label_atom_id
_atom_site.label_alt_id
_atom_site.label_comp_id
_atom_site.label_asym_id
_atom_site.label_entity_id
_atom_site.label_seq_id
_atom_site.pdbx_PDB_ins_code
_atom_site.Cartn_x
_atom_site.Cartn_y
_atom_site.Cartn_z
_atom_site.occupancy
_atom_site.B_iso_or_equiv
_atom_site.Cartn_x_esd
_atom_site.Cartn_y_esd
_atom_site.Cartn_z_esd
_atom_site.occupancy_esd
_atom_site.B_iso_or_equiv_esd
_atom_site.auth_seq_id
_atom_site.auth_comp_id
_atom_site.auth_asym_id
_atom_site.auth_atom_id
_atom_site.pdbx_PDB_model_num

2hgh / 1ai0 waters without residue number in same entity:

ATOM   1     P  P      . G   A 1 1  ? -6.937  29.285  -10.973 1.00 0.00 ? ? ? ? ? 1   G   B P      1
ATOM   6329  H  HB3    . ASP B 2 87 ? -13.031 33.553  -1.159  1.00 0.00 ? ? ? ? ? 190 ASP A HB3    2
HETATM 6330  ZN ZN     . ZN  C 3 .  ? 15.007  -10.087 -10.730 1.00 0.00 ? ? ? ? ? 191 ZN  A ZN     2
HETATM 6331  ZN ZN     . ZN  D 3 .  ? -7.655  6.077   -12.407 1.00 0.00 ? ? ? ? ? 192 ZN  A ZN     2
HETATM 6332  ZN ZN     . ZN  E 3 .  ? -17.047 23.691  1.024   1.00 0.00 ? ? ? ? ? 193 ZN  A ZN     2
HETATM 4797  O  O      . HOH U 5 .  ? 0.104   -1.907  -19.678 1.00 0.00 ? ? ? ? ? 9  HOH ? O    1
HETATM 4798  H  H1     . HOH U 5 .  ? 0.336   -2.657  -19.128 1.00 0.00 ? ? ? ? ? 9  HOH ? H1   1
HETATM 4799  H  H2     . HOH U 5 .  ? 0.882   -1.736  -20.204 1.00 0.00 ? ? ? ? ? 9  HOH ? H2   1
HETATM 4800  O  O      . HOH U 5 .  ? 0.205   -2.311  -3.453  1.00 0.00 ? ? ? ? ? 10 HOH ? O    1   */
        TagTable tTCoor       = db.getTagTable(tagNameAtomAsym_ID,true);

        if ( tTStructAsym == null ) {
            General.showError("Failed to find assembly defs.");
            return false;
        }
        if ( tTEntity == null ) {
            General.showWarning("Failed to find entity defs.");
        }
        if ( tTEntityPolySeq == null ) {
            General.showWarning("No entity poly defs.");
        }
        if ( tTCoor == null ) {
            General.showDebug("Failed to find any coordinates.");
        }

        // Translate As to 1s.
        /** _struct_asym.id
            _struct_asym.entity_id
            A 1
            B 1
         *From 1b4c. Need to translate ABC etc to 123 for char to int.
         *Per asym_id only 1 entity id.
         */
        ObjectIntMap asymId2IntMap = getMapAsymId2Int( tTStructAsym, tagNameMolId );
//        General.showDebug("asymId2IntMap is: " + Strings.toString(asymId2IntMap));
        // Save the column too
        String savedColumnName = tagNameMolId+"saved";
        tTStructAsym.insertColumn(savedColumnName,Relation.DATA_TYPE_STRINGNR,null);
        tTStructAsym.copyColumnBlock(tTStructAsym, tagNameMolId, 0, savedColumnName, 0, tTStructAsym.sizeMax);
        if ( ! convertChainId2EntityId(tTStructAsym, tagNameMolId,  asymId2IntMap)) {
            General.showError("Failed to convertChainId2EntityId for: "+tagNameMolId);
            return false;
        }
//        General.showDebug("Column: "+ tagNameMolName+" is (2): " + Strings.toString(
//                tTEntity.getColumnString(tagNameMolName)));
        Strings.deriveUniqueNames( tTEntity.getColumnString(tagNameMolName) );
//        General.showDebug("Column: "+ tagNameMolName+" is (2): " + Strings.toString(
//                tTEntity.getColumnString(tagNameMolName)));
        if ( ! convertChainId2EntityId(tTEntityNonPoly,tagNameResNonPolMolId, asymId2IntMap)) {
            General.showError("Failed to convertChainId2EntityId for: "+tagNameResNonPolMolId);
            return false;
        }
        if ( ! convertChainId2EntityId(tTCoor,       tagNameAtomAsym_ID, asymId2IntMap)) {
            General.showError("Failed to convertChainId2EntityId for: "+tagNameAtomAsym_ID);
            return false;
        }

        // ENTRY
        // MODEL
        // Maximum number of models found
        int modelCountMax = 1; // check if there are gaps in the model numbering.
//        int modelCountOffset = 0;
        if ( tTCoor != null ) { // Make just one model if there's no coordinates.
            int modelCountMin = 1; // check if there are gaps in the model numbering.
            // This wouldn't work with gaps in relation but since the data is read consecutively from
            // the star file it always works.
            // The models need to be numbered consecutive but not necessarily starting at 1.
            modelCountMin = tTCoor.getValueInt(0, tagNameAtomModelId);
            modelCountMax = tTCoor.getValueInt(tTCoor.sizeRows-1, tagNameAtomModelId);

            // All actions below don't need to be fast because it doesn't happen often.
            if ( modelCountMin > modelCountMax ) {
                General.showError("First model number " + modelCountMin+ " is less than last model number " + modelCountMin);
                return false;
            }
            int violatingRowIdx = tTCoor.isConsecutiveInColumn(tagNameAtomModelId);
            if ( violatingRowIdx == Relation.DEFAULT_VALUE_INDICATION_COLUMN_ORDER_CHECK_ERROR) {
                General.showError("Model numbers failed to be checked for consecutiveness");
                return false;
            }
            if ( violatingRowIdx != Relation.DEFAULT_VALUE_INDICATION_COLUMN_CONSECUTIVE) {
                General.showError("Model numbers not consecutive for the first time for atom coordinates:\n" + tTCoor.toStringRow(violatingRowIdx));
                return false;
            }
            int modelCountOffset = 1 - modelCountMin; // Number to add to go from CIF model number to Wattos.
            if ( modelCountOffset != 0 ) {
                if ( ! tTCoor.renumberRowsByOffset( tagNameAtomModelId, modelCountOffset ) ) {
                    General.showError("Model numbers could not be shifted by modelCountOffset" + modelCountOffset);
                    return false;
                }
                modelCountMax += modelCountOffset;
            }
        }

        // MOLECULES
        if ( ! tTStructAsym.isSortedFromOneInColumn( tagNameMolId ) ) {
            General.showError("The assembly tT needs to be ordered on the entity id.");
            return false;
        }
        // Assume no rows are deleted (Safe after test above)
        int molCountMax = tTStructAsym.sizeRows;



        int[] entityIdList = (int[]) tTStructAsym.getColumn(tagNameMolAssEntityId);
        // Cache the rids so we don't need fancy lookup below.
        // Costs are reasonable: 40*2*200*4 bytes=64kb
        int[]       rid_model = new int[modelCountMax+1];
        int[][]     rid_mol   = new int[modelCountMax+1][molCountMax+1];
        int[][][]   rid_res   = new int[modelCountMax+1][molCountMax+1][];


// ENTRY
        String entryName    = InOut.getFilenameBase( new File(url.getFile()));
        if ( entryName.length() > 4 ) {
            entryName    = entryName.substring(0, 4);
        }
        String assemblyName = entryName;

        OrfIdList orfIdList = null;
        currentEntryId = entry.add(entryName, orfIdList, assemblyName);
        if ( currentEntryId < 0 ) {
            General.showCodeBug("Failed to add an entry into dbms.");
            return false;
        }
//        General.showDebug("Unselecting all entries, models, mols, etc. in DBMS" );
        if ( ! dbms.setValuesInAllTablesInColumn(Relation.DEFAULT_ATTRIBUTE_SELECTED,new Boolean(false))) {
            General.showError( "Failed dbms.setValuesInAllTablesInColumn");
            return false;
        }
//        General.showDebug("Setting selected for entry with rid: " + currentEntryId);
        entry.selected.set( currentEntryId );
        if ( tTPdbxVersion == null ) {
            General.showWarning("Failed to find table with tag _pdbx_version.major_version defs. Continueing without setting version.");
        } else {
//            General.showDebug("Found table with tag _pdbx_version.major_version defs.");
            String majorVersion = tTPdbxVersion.getValueString(0, "_pdbx_version.major_version");
            String minorVersion = tTPdbxVersion.getValueString(0, "_pdbx_version.minor_version");
            String pdbVersion = null;
            if (majorVersion == null || minorVersion == null ) {
                General.showWarning("Failed to find tag _pdbx_version.min/major_version defs. Continueing without setting version.");
            } else{
                pdbVersion = majorVersion + "." + minorVersion;
//                General.showDebug("Setting pdbCoordinateFileVersionNameList to: " + pdbVersion);
                entry.pdbCoordinateFileVersionNameList[currentEntryId] = pdbVersion;
            }
        }

// MODEL
//        boolean showWarningtTResHeader = true;
        for (int modelCount=1;modelCount<=modelCountMax;modelCount++) {
            currentModelId = model.add(modelCount, currentEntryId);
            if ( currentModelId < 0 ) {
                General.showCodeBug("Failed to add model number:" + modelCount + " into dbms.");
                return false;
            }
            model.selected.set( currentModelId );
            // cache rid model
            rid_model[ modelCount ] = currentModelId;

            int tTAssemblyRID = 0;
// MOLECULE
            for (int molCount=1;molCount<=molCountMax;molCount++) {
//                General.showDebug("Working on molecule: " + Integer.toString(molCount));
                int entityId = entityIdList[ tTAssemblyRID ];
                String asymId = tTStructAsym.getValueString(tTAssemblyRID, savedColumnName);
//                General.showDebug("Found asymId from savedColumnName: ["+asymId+"]");
//                Parameters p = new Parameters(); // Printf parameters
//                p.add( gumbo.entry.nameList[ gumbo.model.entryId[ currentModelId ]] );
//                p.add( entityId );
//                String molName = Format.sprintf("%s%03i", p);
                String molName = tTEntity.getValueString(entityId-1,tagNameMolName);
                if ( molName == null ) {
                    General.showError("Failed to get molName for molecule entity: " + entityId);
                    return false;
                }
                currentMolId = mol.add(molName,Defs.NULL_CHAR,currentModelId,asymId);
                if ( currentMolId < 0 ) {
                    General.showCodeBug("Failed to add mol number:" + currentMolId + " into dbms.");
                    return false;
                }
                mol.selected.set( currentMolId );

                // cache rid molecule
                rid_mol[modelCount][molCount] = currentMolId;
                /** E.g. polymer, non-polymer, water, other, or unknown */
                String molType      = Molecule.typeEnum[     Molecule.UNKNOWN_TYPE];
                /** E.g. polypeptide(L) */
                String molPolType   = Molecule.polTypeEnum[  Molecule.UNKNOWN_POL_TYPE];

                if ( tTEntity != null ) {
//                    General.showDebug("tTEntity reads: " + tTEntity.toSTAR());
                    BitSet entitySet = SQLSelect.selectBitSet(dbmsTemp,tTEntity,tagNameMolEntityId,
                            SQLSelect.OPERATION_TYPE_EQUALS,new Integer(entityId),false);
                    if ( entitySet == null ) {
                        General.showError("Missed entity: "+entityId+" in tTEntity: " + tTEntity);
                        return false;
                    }
                    if ( entitySet.cardinality() != 1 ) {
                        General.showError("Expected exactly 1 hit for entity: "+entityId+" in tTEntity: " + tTEntity);
                        return false;
                    }
                    int rid = entitySet.nextSetBit(0);
                    molType = tTEntity.getValueString(rid,tagNameMolType);
                } else {
                    General.showError("Each entity should have been defined; none found for molecule: " + mol.nameList[currentMolId]);
                    return false;
                }
                if ( tTEntityPoly != null ) {
                    BitSet entitySet = SQLSelect.selectBitSet(dbmsTemp,tTEntityPoly,tagNameMolPolMolId,
                            SQLSelect.OPERATION_TYPE_EQUALS,new Integer(entityId),false);
                    if ( entitySet == null ) {
                        General.showError("Missed entity: "+entityId+" in tTEntityPoly: " + tTEntityPoly);
                        return false;
                    }
                    if ( entitySet.cardinality() != 1 ) {
//                        General.showDebug("Expected exactly 1 hit for entity: "+entityId+" in tTEntityPoly: " + tTEntityPoly.name);
//                        General.showDebug("Assuming it's a non-polymer");
                        molPolType   = Molecule.polTypeEnum[  Molecule.NON_POLYMER_POL_TYPE];
                    } else {
                        int rid = entitySet.nextSetBit(0);
                        molPolType = tTEntityPoly.getValueString(rid,tagNameMolPolType);
                    }
                } else {
                    molPolType   = Molecule.polTypeEnum[  Molecule.NON_POLYMER_POL_TYPE];
                }
//                General.showDebug("Found molType    : "+ molType);
//                General.showDebug("Found molPolType : "+ molPolType);
//                mol.setName(   currentMolId, sFRes.title );
                mol.setType(   currentMolId, molType);
                mol.setPolType(currentMolId, molPolType);
                mol.nameList[currentMolId] = molName;
// RESIDUES
                // Can the sequence be retrieved from the poly section or does
                // it come from the coordinate list
                boolean doneWithEntityPolyOrNonPoly = false;
                if ( tTEntityPolySeq != null && (mol.type[currentMolId]==Molecule.POLYMER_TYPE)) {
//                    General.showDebug("Looking at table: " + tTEntityPolySeq);
                    BitSet resSet = SQLSelect.selectBitSet(dbmsTemp,tTEntityPolySeq,tagNameResMolId,
                            SQLSelect.OPERATION_TYPE_EQUALS,new Integer(entityId),false); // TODO check: search by entityId?
                    if ( resSet == null ) {
                        General.showError("Missed residues for entity: " + entityId);
                        return false;
                    }
                    // Check to see the first is 1
                    int firstResRid = resSet.nextSetBit(0);
                    if ( firstResRid < 0 ) { // No residues will be added from tTEntityPolySeq
                        General.showWarning("Please check; no residue found yet for entity: " + entityId);
                    } else {
                        if ( tTEntityPolySeq.getValueInt(firstResRid, tagNameResNum) != 1 ) {
                            General.showError("The residue's ids need to be numbered starting at 1 but at start found:" + tTEntityPolySeq.getValueInt(firstResRid, tagNameResNum));
                            General.showError("Looking at entityId: " + entityId + " and firstResRid: " +
                                    firstResRid + " Overall table is: " + tTEntityPolySeq);
                            return false;
                        }
                        // Last residue's id
                        int resCountMax = resSet.cardinality();
//                        General.showDebug("Found number of residues: " + resCountMax);
                        if ( resCountMax < 1 ) {
                            General.showError("Expected a positive resCountMax (not null either) but found:" + resCountMax);
                            return false;
                        }
                        rid_res[modelCount][molCount] = new int[resCountMax+1]; // assign last dimension within.
                        String[] resNameList    = tTEntityPolySeq.getColumnString( tagNameResName );
                        int[] resSeqId          = tTEntityPolySeq.getColumnInt( tagNameResNum );
                        int resCount=1;
                        for (int tTResRID=resSet.nextSetBit(0);tTResRID>=0;tTResRID=resSet.nextSetBit(tTResRID+1)) {
                            if ( resSeqId[tTResRID] != resCount ) {
                                General.showError("Expected resSeqId[tTResRID] == resCount but found: " +
                                        resSeqId[tTResRID] + " and: " + resCount + " at tTResRID: " + tTResRID);
                                return false;
                            }
                            currentResId = res.add( resNameList[tTResRID], resSeqId[tTResRID],
                                    Defs.NULL_STRING_NULL, Defs.NULL_STRING_NULL, currentMolId );
                            if ( currentResId < 0 ) {
                                General.showCodeBug("Failed to add res id:" + resCount + " into dbms.");
                                return false;
                            }
                            res.selected.set( currentResId );
                            // cache rid residue
                            rid_res[modelCount][molCount][resCount] = currentResId;
                            resCount++;
                        }
                        if ( resCount > 1 ) { // at least 1 was added to the soup.
                            doneWithEntityPolyOrNonPoly = true;
                        }
                    } // end of conditional residues being present block; can't break out of an if block?
                } // end of work on polyseq

                // Try to add from Coor
                if ( ! doneWithEntityPolyOrNonPoly) { // try to get residue name(s) from the coordinates
//                    General.showDebug("Not done with entity poly; must be a nonpoly/water.");
                    BitSet atomSet = SQLSelect.selectBitSet(dbmsTemp,tTCoor,tagNameAtomEntityId,
                            SQLSelect.OPERATION_TYPE_EQUALS,new Integer(entityId),false);
                    BitSet atomSetModel = SQLSelect.selectBitSet(dbmsTemp,tTCoor,tagNameAtomModelId,
                            SQLSelect.OPERATION_TYPE_EQUALS,new Integer(modelCount),false);
                    if ( atomSet.cardinality() < 0 ) {
                        General.showError("Expected at least one unique residue name in coor list for entity: " + entityId );
                        return false;
                    }
                    if ( atomSetModel.cardinality() < 0 ) {
                        General.showError("Expected at least one unique residue name in coor list for entity in current model: " + entityId );
                        return false;
                    }
                    atomSet.and(atomSetModel);
                    int firstAtomRid = atomSet.nextSetBit(0);
//                    General.showDebug("Looking at coor table for record: " + tTCoor.toStringRow(firstAtomRid, false));
                    if ( firstAtomRid < 0 ) {
                        General.showWarning("Failed to find any atom in model "+modelCount+" for entity: " + entityId);
                        General.showWarning("This was found in for instance entry 1j6t for the PO3 group and was reported in NRG issue 157");
//                        atomSetNotInFirstModel.set(firstAtomRid);
//                        General.showWarning("Increased number of these atoms to: " + atomSetNotInFirstModel.cardinality());
                    } else {
                        int resCount=1;
                        String resName = tTCoor.getValueString(firstAtomRid, tagNameAtomResName);
                        // Water is so special for live on earth.
                        if ( mol.type[currentMolId]!=Molecule.WATER_TYPE ) {
                            rid_res[modelCount][molCount] = new int[resCount+1]; // assign last dimension within.
                            currentResId = res.add( resName, resCount, Defs.NULL_STRING_NULL, Defs.NULL_STRING_NULL, currentMolId );
                            if ( currentResId < 0 ) {
                                General.showCodeBug("Failed to add res count:" + resCount + " into dbms.");
                                return false;
                            }
                            res.selected.set( currentResId );
                            // cache rid residue
                            rid_res[modelCount][molCount][resCount] = currentResId;
                        } else {
                            if ( ! resName.equals("HOH")) {
                                General.showError("Found a water posing residue: " + resName);
                                return false;
                            }
                            BitSet atomOSet = SQLSelect.selectBitSet(dbmsTemp,tTCoor,tagNameAtomName,
                                    SQLSelect.OPERATION_TYPE_EQUALS,"O",false);
                            atomOSet.and(atomSet); // only within this specific entity
                            int water_count = atomOSet.cardinality();
//                            General.showDebug("Found number of oxygen atoms indicating a water molecule: "+water_count);
                            rid_res[modelCount][molCount] = new int[water_count+1]; // assign last dimension within.
                            for ( int atomRid=atomOSet.nextSetBit(0);atomRid>=0;atomRid = atomOSet.nextSetBit(atomRid+1)) {
                                tTCoor.setValue(atomRid, tagNameAtomResId, new Integer(resCount));
                                currentResId = res.add( resName, resCount, Defs.NULL_STRING_NULL, Defs.NULL_STRING_NULL, currentMolId );
                                if ( currentResId < 0 ) {
                                    General.showCodeBug("Failed to add WATER:" + resCount + " into dbms.");
                                    return false;
                                }
                                res.selected.set( currentResId );
                                // cache rid residue
                                rid_res[modelCount][molCount][resCount] = currentResId;
                                // are there follow up Hydrogens?
                                for (int offset=1;offset<3;offset++) {
                                    int atomRid2 = atomRid+offset;
                                    if ( atomSet.get(atomRid2) && tTCoor.getValueString(atomRid2, tagNameAtomName).startsWith("H")) {
                                        tTCoor.setValue(atomRid2, tagNameAtomResId, new Integer(resCount));
    //                                    General.showDebug("Renumbered water residue for coor table at: " + tTCoor.toStringRow(atomRid2, false));
                                    }
                                }
                                resCount++;
                            } // End for over atomOSet
                        } // end else
                    } // end for atomSetNotInFirstModel
                } // end for !doneWithEntityPolyOrNonPoly
                tTAssemblyRID++;
            } // end of loop over molecules
        } // end of loop over models
//        int atomNotInFirstModelCount = atomSetBad.cardinality();
//        General.showWarning("There are " + atomNotInFirstModelCount + " atoms that were skipped because no accomodating residue was present in the first model. Fixes issue 157.");

// ATOMS
        // Already checked to see the first is 1
        int atomCountMax = tTCoor.sizeRows;

        /** Strategy will be to add columns to the original table in STAR and
         *then copy the complete set to the dbms main table for atoms. The copy
         *can be batched and will be faster than copy one by one row.
         */
        String[] newbies = {
            Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RelationSet.RELATION_ID_COLUMN_NAME ],
            Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[ RelationSet.RELATION_ID_COLUMN_NAME ],
            Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ],
            Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME ],
            Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR,
            Relation.DEFAULT_ATTRIBUTE_SELECTED,
            Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID
        };
        if ( !(
            tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RelationSet.RELATION_ID_COLUMN_NAME ],        Relation.DATA_TYPE_INT,null) &&
            tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[ RelationSet.RELATION_ID_COLUMN_NAME ],        Relation.DATA_TYPE_INT,null) &&
            tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ],      Relation.DATA_TYPE_INT,null) &&
            tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME ],      Relation.DATA_TYPE_INT,null) &&
            tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR,           Relation.DATA_TYPE_BIT,null) &&
            tTCoor.insertColumn(0, Relation.DEFAULT_ATTRIBUTE_SELECTED,        Relation.DATA_TYPE_BIT,null) &&
            tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID,         Relation.DATA_TYPE_INT,null)
        )) {
            General.showError("Failed to insert all required columns");
            return false;
        }

        // Rename some.
        String[] equivalents = {
                Relation.DEFAULT_ATTRIBUTE_NAME,
                Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME,
                Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID,
                Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME,
                Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME,
                Gumbo.DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE,
                Gumbo.DEFAULT_ATTRIBUTE_COOR_X,
                Gumbo.DEFAULT_ATTRIBUTE_COOR_Y,
                Gumbo.DEFAULT_ATTRIBUTE_COOR_Z,
                Gumbo.DEFAULT_ATTRIBUTE_BFACTOR,
                Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY
        };
        for ( int n=0;n<equivalents.length;n++) {
            String columnName = (String) ((ArrayList)starDict.toCIF2D.get( "atom_main", equivalents[n])).get(StarDictionary.POSITION_CIF_TAG_NAME);
            if ( columnName == null ) {
                General.showCodeBug("Failed to find definition for Wattos column: " + equivalents[n] + "in dictionary.");
                return false;
            }
            if ( ! tTCoor.renameColumn(columnName, equivalents[n]) ) {
                General.showError("Failed to rename column: " + columnName + " to: " + equivalents[n] + ". Is it present?");
                return false;
            }
        }
        /** Translate the string elements to int elements using an slightly optimized routine.
         */
        int[] elementIds = (int[]) tTCoor.getColumn( Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID );
        String[] elementIdsTmp = (String[]) tTCoor.getColumn(tagNameAtomElementId);
        if ( elementIdsTmp == null ) {
            General.showWarning("No element ids tag  present in input at with name: " + tagNameAtomElementId + " ; setting all to null.");
            tTCoor.setValueByColumn( Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID, new Integer(Defs.NULL_INT));
        } else {
            // Only need to do for sizeRows; not maxRows because we are guaranteed they're not all used.
            if ( ! Chemistry.translateElementNameToIdInArrays( elementIdsTmp, elementIds, false, 0, tTCoor.sizeRows ) ) {
                General.showError("Failed to translate all element names to ids in arrays; setting all to null. Shouldn't be fatal but it might.");
                tTCoor.setValueByColumn( Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID, new Integer(Defs.NULL_INT));
                return false;
            }
        }
        tTCoor.setValueByColumn(Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR,  Boolean.valueOf(true));

        /** Set fkcs */
        tTCoor.setValueByColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME ], new Integer(currentEntryId));
        int[]   atomModelIdList         = (int[])       tTCoor.getColumn( tagNameAtomModelId );
        int[]   atomMolIdList           = (int[])       tTCoor.getColumn( tagNameAtomAsym_ID );
        int[]   atomResIdList           = (int[])       tTCoor.getColumn( tagNameAtomResId );
        int[]   model_main_id           = (int[])       tTCoor.getColumn( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ]);
        int[]   mol_main_id             = (int[])       tTCoor.getColumn( Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[ RelationSet.RELATION_ID_COLUMN_NAME ] );
        int[]   res_main_id             = (int[])       tTCoor.getColumn( Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RelationSet.RELATION_ID_COLUMN_NAME ] );
        if ( ( atomModelIdList == null ) ||
             ( atomMolIdList == null ) ||
             ( atomResIdList == null ) ||
             ( model_main_id == null ) ||
             ( mol_main_id == null ) ||
             ( res_main_id == null ) ) {
             General.showError("Failed to get all the required columns; skipping the read of all atoms.");
             return false;
        }
        int modelNum;
        int molNum;
        int resNum;
        int atomIdx=0;
//        try {
        for (;atomIdx<atomCountMax;atomIdx++) {        // This loop might want to be optimized
            resNum = Defs.NULL_INT;
            try {
                modelNum    = atomModelIdList[  atomIdx];
                molNum      = atomMolIdList[    atomIdx];
                resNum      = atomResIdList[    atomIdx];
                if ( Defs.isNull(resNum)) {
                    resNum = 1; // unnumbered residues such as ions and waters
                    // waters will be renumbered again later.
                }
                model_main_id[  atomIdx] = rid_model[ modelNum];
                mol_main_id[    atomIdx] = rid_mol[   modelNum][molNum];
                res_main_id[    atomIdx] = rid_res[   modelNum][molNum][resNum];
            } catch ( Throwable t ) {
                atomSetBad.set(atomIdx);
                General.showDebug("atomSetNotInFirstModel grew to size: " + atomSetBad.cardinality());
            }
        }
//        } catch ( Throwable t ) {
//            General.showThrowable(t);
//            General.showError("For atom: " + atomCount + " in the file, failed to set the RIDs" );
//            General.showError("Model number: " + atomModelIdList[  atomCount] + ", molecule number: " + atomMolIdList[    atomCount] + ", residue number: " + atomResIdList[    atomCount]);
//            General.showError("Is the atom in known topology. E.g. does the residue occur in the system description.");
//            General.showError("This also happens when the model numbering is significantly off as per issue 169 for eg entries 1j6y and 2fce");
//            return false;
//        }

        // Select and set 'hasCoor' for all atoms in bulk
        BitSet atomSetOk = new BitSet();
        atomSetOk.or(tTCoor.used);

        if ( atomSetBad.cardinality() > 0 ) {
            General.showDebug("atomSetBad size: " + atomSetBad.cardinality());
            General.showDebug("atomsSetOk size: " + atomSetOk.cardinality());
            atomSetOk.andNot( atomSetBad );
            General.showDebug("atomsSetOk size (after non-Oks zapped): " + atomSetOk.cardinality());
        }
        BitSet selected = tTCoor.getColumnBit( Relation.DEFAULT_ATTRIBUTE_SELECTED ); // all false at this point
        BitSet hasCoor = tTCoor.getColumnBit( Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR );
        selected.or( atomSetOk );
        hasCoor.or( atomSetOk );



        ArrayList columnsToCopy = new ArrayList();
        columnsToCopy.addAll( Arrays.asList( equivalents ) );
        columnsToCopy.addAll( Arrays.asList( newbies ) );
        if ( ! atomMain.append( tTCoor, 0, atomCountMax, columnsToCopy, false) ) {
            General.showCodeBug("Failed to append from modifed tag table in STAR file to Wattos atom main relation");
            return false;
        }

        // Sync the atoms over the models in the entry. Note that this will remove unsynced atoms.
        if ( syncModels ) {
            if ( (! entry.modelsSynced.get( currentEntryId  )) &&
                 (! entry.syncModels( currentEntryId ))) {
                General.showError("Failed to sync models after reading in the coordinates. Deleting the whole entry again.");
                if ( ! entry.mainRelation.removeRowsCascading( currentEntryId, true ) ) {
                    General.showError("Failed to deleting the whole entry.");
                }
                return false;
            }
        } else {
            General.showDebug("Disabled syncing over models.");
//            General.showDebug("This might lead to inconsistencies in Wattos internal data model.");
//            General.showDebug("Needs testing for sure.");
//            General.showDebug("The code will definitely not work when reading in restraints on top of partly missing atoms.");
        }


        // Select all newly added atoms; code added later.
        BitSet orgAtoms = SQLSelect.selectBitSet(dbms, atom.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(currentEntryId), false);
        atom.selected.or( orgAtoms );
        return true;
    }


    /** @see Wattos.Utils.PrimitiveArray#convertString2Int
     */
   public static boolean convertChainId2EntityId(TagTable t, String label, ObjectIntMap asymId2IntMap) {
       if ( t == null ) {
//           General.showDebug("got empty tagtable for convertChainId2EntityId");
           return true;
       }
        String[] column = t.getColumnString(label);
        if ( column == null ) {
            General.showError("Failed to get String[] for column labeled: " + label);
            return false;
        }
//        General.showDebug("Column in convertChainId2EntityId is: " + PrimitiveArray.toString(column));
        /** Modified from PrimitiveArray
         */
        int previousValue;
        String previousString;
        int currentValue;
        String currentString;
        int sizeMax = column.length;
        int[] result = new int[column.length];
        if ( column.length == 0 ) {
            return false;
        }
        int r=sizeMax-1;
        try {
            // Do the last one first.
            currentString =  column[sizeMax-1];
            currentValue = asymId2IntMap.getValueInt(currentString);

            result[sizeMax-1] = currentValue;
            previousValue = currentValue;
            previousString = currentString;

            // Do the rest.
            r=sizeMax-2;
            for (;r>-1;r--) {
                // Only do parse if previous string was different.
                currentString = column[r];
                if ( (currentString != null) && currentString.equals( previousString ) ) {
                    result[r] = previousValue;
                } else {
                    currentValue = asymId2IntMap.getValueInt(currentString);
                    result[r] = currentValue;
                    previousValue = currentValue;
                    previousString = currentString;
                }
            }
        } catch ( Throwable th ) {
            General.showThrowable(th);
            General.showError("For value on row id: " + r + " Rows are processed last first.");
            return false;
        }
        t.replaceColumn(label,Relation.DATA_TYPE_INT,result);
//        General.showDebug("Column converted to: " + PrimitiveArray.toString(result));
        return true;
    }

         // Translate As to 1s.
        /** _struct_asym.id
            _struct_asym.entity_id
            A 1
            B 1
         *From 1b4c. Need to translate ABC etc to 123 for char to int.
         *Per asym_id only 1 entity id.
         */
   public static ObjectIntMap getMapAsymId2Int( TagTable tT, String tagName ) {
        ObjectIntMap result = new ObjectIntMap();
        int i = 1;
        for (int r=tT.used.nextSetBit(0);r>=0;r=tT.used.nextSetBit(r+1)) {
            String key = tT.getValueString(r,tagName);
            if ( result.containsKey(key)) { // double lookup is efficiency issue
                General.showError("Found same (key) asymId in cif which is not allowed");
                return null;
            }
            result.put(key,i);
            i++;
        }
        return result;
    }
}
