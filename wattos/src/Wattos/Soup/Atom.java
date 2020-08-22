/*
 * Atom.java
 *
 * Created on November 8, 2002, 4:41 PM
 */

package Wattos.Soup;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexHashedIntToMany;
import Wattos.Soup.Comparator.ComparatorAtom;
import Wattos.Soup.Comparator.ComparatorAtomPerModelMolResAtom;
import Wattos.Soup.Comparator.ComparatorAuthorAtomWithoutEntry;
import Wattos.Star.DataBlock;
import Wattos.Star.SaveFrame;
import Wattos.Star.StarNode;
import Wattos.Utils.General;
import Wattos.Utils.HashOfHashes;
import Wattos.Utils.InOut;
import Wattos.Utils.ObjectIntPair;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringSet;
import Wattos.Utils.Strings;
import Wattos.Utils.Wiskunde.Geometry;
import cern.colt.list.IntArrayList;
import cern.colt.list.ObjectArrayList;

import com.braju.format.Format;
import com.braju.format.Parameters;
/**
 * Property of an atom like: where is it in the {@link Wattos.Soup} and in 3D.
 * This class serves as the template for the relationsets in the Soup.
 *
 * The atom name is the IUPAC recommended standard. The
 * {@link <a href="http://www.bmrb.wisc.edu">BMRB</a>}
 * preferred HN over
 * {@link <a href="http://www.rcsb.org/pdb">PDB</a>}
 * PDB preferred H will be used. HB2/HB3 i.s.o. HB1/HB2
 * <I>etc</I>.
 * <BR>
 *
 * <P>Some random documentations:<BR>
 * <UL>
 * <LI> Each atom in the first model has information on it's siblings in related models.
 * <LI> Updates and deletions are tricky. Be careful with deleting atoms specific
 * to 1 model. For an example look at
 * {@link Wattos.CloneWars.CommandHub#ReadEntryExtraCoordinatesWHATIFPDB}
 * <LI>When deleting a model in the soup then also nill the atom siblings list.
 * That is: each affected atom sibling list to be set to null.
 * There is an Entry property that shows if models are sync-ed. That is false if this
 * still needs to be done. If true, it may be assumed that the atoms are in sync. This adds complexity
 * because for each update/deletion the entry attribute has to be set to false.
 * </ul>
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Atom extends GumboItem implements Serializable {

    private static final long serialVersionUID = -1654597752202161738L;
    /** Classification of an atom based on it's second char in the atom name */
    public static String[] DEFAULT_CLASS_NAMES      = { "amide", "alpha", "beta", "gamma", "delta", "epsilon", "dzeta  ", "other"};
    public static char[] ATOM_CHAR                  = { '_', 'A', 'B', 'G', 'D', 'E', 'Z', ' '};

    public static final String CAN_HYDROGEN_BOND            = "can_hydrogen_bond";
    /** When CAN_HYDROGEN_BOND is true this parameter identifies a donor.
     *Note that an atom can be a donor and acceptor at the same time in the
     *topology lib only, depending on protonation.     */
    public static final String IS_HB_DONOR                  = "is_HB_donor";

    /** See IS_HB_DONOR     */
    public static final String IS_HB_ACCEP                  = "is_HB_accep";

    private final static boolean DEBUG = true;
    static final float DEFAULT_TOLERANCE_CALCULATE_BONDS = 0.1f;
    static final float DEFAULT_TYPE_UNKNOWN     = 0;
    static final float DEFAULT_TYPE_NONLINEAR   = 1;
    static final float DEFAULT_TYPE_TETRAHEDRAL = 2;
    static final float DEFAULT_TYPE_PLANAR      = 3;

    /** Convenience variables */
    public int[]       resId;          // starting with fkcs
    public int[]       molId;
    public int[]       modelId;
    public int[]       entryId;
    /** the index of the first model is zero*/
    public int[][]     modelSiblingIds;
    /** For the master atom itself this should be set to it's own rid */
    public int[]       masterAtomId;
    public int[]       elementId;      // non fkcs
    public float[]     occupancy;
    public float[]     bfactor;
    public String[]    authMolNameList;
    public StringSet   authMolNameListNR;
    public String[]    authResNameList;
    public StringSet   authResNameListNR;
    public String[]    authResIdList;
    public StringSet   authResIdListNR;
    public String[]    authAtomNameList;
    public StringSet   authAtomNameListNR;
    public String[]    pdbInsertionCodeList;
    public StringSet   pdbInsertionCodeListNR;
    public BitSet      canHydrogenBond;
    public BitSet      isHBDonor;
    public BitSet      isHBAccep;

    public Atom(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public Atom(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug("back in Atom constructor");
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
        //General.showDebug("now in Atom.init()");
        super.init(dbms);
        //General.showDebug("back in Atom.init()");

        name = Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RELATION_ID_SET_NAME];

        // MAIN RELATION in addition to the ones in gumbo item.
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_RES[  RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[  RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID,                         new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY,                          new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_BFACTOR,                            new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME,                      new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME,                      new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID,                        new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME,                     new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE,                     new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_MODEL_SIBLINGS,                     new Integer(DATA_TYPE_ARRAY_OF_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_MASTER_RID,                         new Integer(DATA_TYPE_INT));

        DEFAULT_ATTRIBUTES_TYPES.put( CAN_HYDROGEN_BOND,                                          new Integer(DATA_TYPE_BIT));
        DEFAULT_ATTRIBUTES_TYPES.put( IS_HB_DONOR,                                                new Integer(DATA_TYPE_BIT));
        DEFAULT_ATTRIBUTES_TYPES.put( IS_HB_ACCEP,                                                new Integer(DATA_TYPE_BIT));

        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_RES[   RELATION_ID_COLUMN_NAME ] );
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[   RELATION_ID_COLUMN_NAME ]);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RELATION_ID_COLUMN_NAME ]);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RELATION_ID_COLUMN_NAME ]);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_BFACTOR);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_MODEL_SIBLINGS);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_MASTER_RID);
        DEFAULT_ATTRIBUTES_ORDER.add( CAN_HYDROGEN_BOND);
        DEFAULT_ATTRIBUTES_ORDER.add( IS_HB_DONOR);
        DEFAULT_ATTRIBUTES_ORDER.add( IS_HB_ACCEP);

        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RELATION_ID_COLUMN_NAME],    Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_COLUMN_NAME],    Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME],  Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME],  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME]});

        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RELATION_ID_MAIN_RELATION_NAME];
        try {
            relation = new Relation(relationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);
        relation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER,
            DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS);
        addRelation( relation );
        mainRelation = relation;

        // OTHER RELATIONS HERE
        //..

        return true;
    }

   /** Adds a new entry in the array. Since there are so many parameters it might be faster
    *to in-line this method. Which the JIT might do anyway.
    *Note that the atom coordinates will automatically be rounded to 3 digits after
    *the decimal dot.
 * @param pdbInsertionCode TODO
     */
    public int add(String name,                                                 // WattosItem attributes
        boolean has_coor, float[] coor, float charge,                           // GumboItem attributes
        int elementId, float occupancy, float bfactor,                          // Atom attributes.
        String authMolName, String authResName, String authResId, String authAtomName,
        int parentId, String pdbInsertionCode) {

        int maxSize = mainRelation.sizeMax;
        int result = super.add( name, has_coor, GumboItem.roundToPdbPrecision(coor), charge);
        if ( result < 0 ) {
            General.showCodeBug( "Failed to get a new row id for an atom with name: " + name);
            return -1;
        }
        if ( maxSize != mainRelation.sizeMax) {
            resetConvenienceVariables();
        }

        resId[              result ] = parentId;                                   // Atom attributes. (fkcs)
        molId[              result ] = gumbo.res.molId[     parentId ];
        modelId[            result ] = gumbo.res.modelId[   parentId ];
        entryId[            result ] = gumbo.res.entryId[   parentId ];

        this.elementId[     result ] = elementId;                                   // Atom attributes. (non-fkcs)
        this.occupancy[     result ] = occupancy;
        this.bfactor[       result ] = bfactor;

        authMolNameList[    result ] = authMolNameListNR.intern( authMolName );
        authResNameList[    result ] = authResNameListNR.intern( authResName );
        authResIdList[      result ] = authResNameListNR.intern( authResId );
        authAtomNameList[   result ] = authAtomNameListNR.intern( authAtomName );
        pdbInsertionCodeList[   result ] = pdbInsertionCodeListNR.intern( authAtomName );


        type[ result ] = dbms.ui.wattosLib.atomLibAmber.getAtomTypeId( gumbo.res.nameList[parentId], name);
        return result;
    }

    /** Renumbers selected atoms (to 1 .. N selected atoms in residue) in all selected entries, models, mols, res.
     *Uses a very expensive 5 level loop! See if it's doable.
     *Overrides the default implementation in WattosItem.
    public boolean resetNumbers() {

        /** START OF BLOCK copy from Atom.resetNumbers
        // Some short hand notations
        BitSet usedEntry = gumbo.entry.mainRelation.used;
        BitSet usedModel = gumbo.model.mainRelation.used;
        BitSet usedMol   = gumbo.mol.mainRelation.used;
        BitSet usedRes   = gumbo.res.mainRelation.used;
        BitSet usedAtom  = mainRelation.used;
        BitSet selEntry  = gumbo.entry.mainRelation.getColumnBit( DEFAULT_ATTRIBUTE_SELECTED );
        BitSet selModel  = gumbo.model.mainRelation.getColumnBit( DEFAULT_ATTRIBUTE_SELECTED );
        BitSet selMol    = gumbo.mol.mainRelation.getColumnBit(   DEFAULT_ATTRIBUTE_SELECTED );
        BitSet selRes    = gumbo.res.mainRelation.getColumnBit(   DEFAULT_ATTRIBUTE_SELECTED );
        BitSet selAtom   = mainRelation.getColumnBit(  DEFAULT_ATTRIBUTE_SELECTED );
        /** Just making sure no work is done on selected but not used elements:
        selEntry.and(   usedEntry );
        selModel.and(   usedModel );
        selMol.and(     usedMol );
        selRes.and(     usedRes );
        selAtom.and(    usedAtom );
        String columnLabelEntryId = Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME];
        String columnLabelModelId = Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME];
        String columnLabelMolId   = Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[   RelationSet.RELATION_ID_COLUMN_NAME];
        String columnLabelResId   = Gumbo.DEFAULT_ATTRIBUTE_SET_RES[   RelationSet.RELATION_ID_COLUMN_NAME];
        String columnLabelAtomId  = Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[  RelationSet.RELATION_ID_COLUMN_NAME];
        /** END OF BLOCK

        for (int ent=selEntry.nextSetBit(0);            ent>=0; ent=selEntry.nextSetBit(ent+1))  {
            BitSet selModelSub = SQLSelect.selectBitSet(dbms, gumbo.model.mainRelation,
                columnLabelEntryId, SQLSelect.OPERATION_TYPE_EQUALS, new Integer(ent), false);
            selModelSub.and( selModel );
            General.showDebug("For selected entry: " + gumbo.entry.mainRelation.getValueString(ent, DEFAULT_ATTRIBUTE_NAME ) +
                " found number of selected models: " + selModelSub.cardinality());
            for (int mod=selModelSub.nextSetBit(0);     mod>=0; mod=selModelSub.nextSetBit(mod+1))  {
                BitSet selMolSub = SQLSelect.selectBitSet(dbms, gumbo.mol.mainRelation,
                    columnLabelModelId, SQLSelect.OPERATION_TYPE_EQUALS, new Integer(mod), false);
                selMolSub.and( selMol );
                General.showDebug("Found selected mols: " + selMolSub.cardinality());
                for (int mol=selMolSub.nextSetBit(0);   mol>=0; mol=selMolSub.nextSetBit(mol+1))    {
                    BitSet selResSub = SQLSelect.selectBitSet(dbms, gumbo.res.mainRelation,
                        columnLabelMolId, SQLSelect.OPERATION_TYPE_EQUALS, new Integer(mol), false);
                    selResSub.and( selRes );
                    General.showDebug("Found selected res: " + selResSub.cardinality());
                    for (int res=selResSub.nextSetBit(0);   res>=0; res=selResSub.nextSetBit(res+1))    {
                        BitSet selAtomSub = SQLSelect.selectBitSet(dbms, mainRelation,
                            columnLabelResId, SQLSelect.OPERATION_TYPE_EQUALS, new Integer(res), false);
                        selAtomSub.and( selAtom );
                        General.showDebug("Found selected atoms: " + selAtomSub.cardinality());
                        int n = 1;
                        for (int ato=selAtomSub.nextSetBit(0);   ato>=0; ato=selAtomSub.nextSetBit(ato+1))    {
                            number[ ato ] = n++;
                        }
                    }
                }
            }
        }
        General.showWarning("Renumbering atoms according to physical order at this point");
        return true;
    }
     */

    /** Checks the atoms in the todo set for if they're bonded to one or more instances with the element type given.
     */
    public BitSet isBondedToElement( BitSet todo, int bondedToElementId ) {
        // Instead of using indices, a straight scan of the two sides of the
        // bond list is more efficient for large sets O(n).
        BitSet result = new BitSet(todo.size());
        int[] atom_Id = null; // Atom checked for in todo

        int thisAtomId = -1; // Checked atom.
        int otherAtomId = -1; // Bonded atom.
        // Look at atom A and B.
        for (int j=0;j<2;j++) {
            atom_Id = gumbo.bond.atom_A_Id;
            if ( j == 1 ) {
                atom_Id = gumbo.bond.atom_B_Id;
            }
            for (int i=gumbo.bond.used.nextSetBit(0);i>=0;i=gumbo.bond.used.nextSetBit(i+1)) {
                thisAtomId = atom_Id[ i ];
                if ( ! todo.get(thisAtomId) ) {
                    // The atom doesn't need to be checked.
                    continue;
                }
                otherAtomId = gumbo.bond.atom_B_Id[i];
                if ( j == 1 ) {
                    otherAtomId = gumbo.bond.atom_A_Id[i];
                }
                if ( elementId[ otherAtomId ] == bondedToElementId ) {
                    result.set( thisAtomId );
                }
            }

        }
        return result;
    }


    /** Returns a set of donors and acceptors of those requested
     *NB
     *<OL>
     *<LI>This routine will fail if the hydrogens aren't present.
     *<LI>This routine assumes the Met SD to be an acceptor which it usually isn't. It's only in 5%
     of the cases studied by: F.H. Allen, C.M. Bird, R.S. Rowland and P.R. Raithby, Acta Crystallogr. B53, 696-701 (1997).
     *<LI>A Proline backbone N is incorrectly considered as an acceptor here.
     *</OL>
     *
     *todo fix the above noted mistakes.
     */
    public BitSet[] getHydrogenBondAcceptorsAndDonors(BitSet atomsInMaster) {
        // find possible donors.
        BitSet atomInFirstModelN = PrimitiveArray.getRidsByValue( elementId, Chemistry.ELEMENT_ID_NITROGEN );
        BitSet atomInFirstModelO = PrimitiveArray.getRidsByValue( elementId, Chemistry.ELEMENT_ID_OXYGEN );
        BitSet atomInFirstModelS = PrimitiveArray.getRidsByValue( elementId, Chemistry.ELEMENT_ID_SULFUR );
        atomInFirstModelN.and( atomsInMaster );
        atomInFirstModelO.and( atomsInMaster );
        atomInFirstModelS.and( atomsInMaster );

        BitSet atomInFirstModelDonor = new BitSet(); // initialized to false first.
        BitSet atomInFirstModelAccep = new BitSet();
        atomInFirstModelDonor.or( atomInFirstModelN );
        atomInFirstModelDonor.or( atomInFirstModelO );
        atomInFirstModelDonor.or( atomInFirstModelS );
        atomInFirstModelAccep.or( atomInFirstModelDonor );
        // differentiate by attached hydrogen.
        BitSet atomInFirstModelBondedHydrogen = isBondedToElement( atomInFirstModelDonor, Chemistry.ELEMENT_ID_HYDROGEN );
        if ( atomInFirstModelBondedHydrogen == null ) {
            General.showError("Failed to do calcHydrogenBond because failed to get the atoms that are bonded to at least one hydrogen atom.");
            return null;
        }
        atomInFirstModelDonor.and(      atomInFirstModelBondedHydrogen );
        //atomInFirstModelAccep.andNot(   atomInFirstModelBondedHydrogen );
        // Note that nitrogens/oxygens with one or more bound hydrogens are still a possible acceptor.
        //atomInFirstModelAccep.or(   atomInFirstModelO );
        //atomInFirstModelAccep.or(   atomInFirstModelN );

        //General.showDebug("Donors:\n" + toString(atomInFirstModelDonor));
        //General.showDebug("Acceps:\n" + toString(atomInFirstModelAccep));
        BitSet[] result = new BitSet[] { atomInFirstModelDonor, atomInFirstModelAccep };
        return result;
    }


    /**
     * A hydrogen bond is defined as X-H ... Y. Where X (donor) and Y (acceptor) are both atoms of
     * element types N, O, or S and X has an attached H.
     * This method requires the presence of bonds and hydrogen atoms before this call. <BR>
     * <OL>
     * <LI>Only bonds to the first model will be added.
     * <LI>Intra residual hydrogen bonds are ignored.
     * <LI>Just like for bonds the criterium has to be met for every
     * model otherwise it will be cancelled.
     * <LI>The new hydrogen bonds are those bonds selected after the routine.
     * <LI>Routine will print if more than two hydrogens bond to an acceptor.
     * </OL>
     * @see #calcBond
     * @see #getHydrogenBondAcceptorsAndDonors
     * @param atomsInMaster
     * @return true for success
     */
    public boolean calcHydrogenBond(BitSet atomsInMaster,
        float hbHADistance, float hbDADistance, float hbDHAAngle,
        String summaryFileName) {

//        boolean status = true;
    if ( hbHADistance < 0 ) {
        hbHADistance = Calculation.hbHADistance;
        }
    if ( hbDADistance < 0 ) {
        hbDADistance = Calculation.hbDADistance;
        }
    if ( hbDHAAngle < 0 ) {
        hbDHAAngle = Calculation.hbDHAAngle;
        }

        BitSet[] result = getHydrogenBondAcceptorsAndDonors(atomsInMaster);
        if ( result == null ) {
            General.showError("Failed to get lists of donor and acceptors for hydrogen bonds");
            return false;
        }
        BitSet atomInFirstModelDonor = result[0];
        BitSet atomInFirstModelAccep = result[1];



        int entryRID = gumbo.entry.getEntryId();
        if ( entryRID < 0 ) {
            General.showError("Failed to get single entry id");
            return false;
        }
        BitSet modelsInEntry = gumbo.entry.getModelsInEntry(entryRID);
        if ( modelsInEntry == null ) {
            General.showError("Failed calcHydrogenBond because failed to get the models in this entry for rid: " + entryRID);
            return false;
        }
        int modelCount = modelsInEntry.cardinality();

        try {
            IntArrayList boundHList = new IntArrayList();
            IntArrayList boundListBond = null;
            IntArrayList boundListABond = null;
            IntArrayList boundListBBond = null;
            int atomRid, bondRid;
            int k,l,s,x;
            /** Atoms involved; NB here the acceptor is a N but it could be any of: N, O, and S. */
            int ridH = -1;
            Integer iInt = null;
            boolean foundHBBond = false;
//            float d;

            gumbo.bond.selected.clear();
            Index bondAtomAIndex = gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, Index.INDEX_TYPE_SORTED);
            Index bondAtomBIndex = gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, Index.INDEX_TYPE_SORTED);
            // Index is still valid even if the arrays grew.
            if ( (bondAtomAIndex==null)||(bondAtomBIndex==null)) {
                General.showError("Failed calcHydrogenBond because failed to get the indexes on bonds ");
                return false;
            }

            for (int i=atomInFirstModelDonor.nextSetBit(0);i>=0;i=atomInFirstModelDonor.nextSetBit(i+1)) {
                //General.showDebug("Checking for donor:\n" + toString(i));
                iInt = new Integer(i);
                for (int j=atomInFirstModelAccep.nextSetBit(0);j>=0;j=atomInFirstModelAccep.nextSetBit(j+1)) {
                    //General.showDebug("with accep:\n" + toString(j));
                    if ( i == j ) { // unusual
                        continue;
                    }
                    /** Easiest way to exclude bonded atoms is by doing it on a residue basis
                     */
                    if ( resId[i]==resId[j]) {
                        /**
                        General.showDebug("Ignoring potential hydrogen bond between atoms in same residue for:\n" +
                            toString(i) + General.eol +
                            toString(j));
                         */
                        continue;
                    }
                    // Prevent more complicated tests.
                    if ((  Math.abs( xList[i] - xList[j]) > hbDADistance ) ||
                        (  Math.abs( yList[i] - yList[j]) > hbDADistance ) ||
                        (  Math.abs( zList[i] - zList[j]) > hbDADistance )) {
                        continue;
                    }
                    boundListABond = (IntArrayList) bondAtomAIndex.getRidList( iInt,Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    boundListBBond = (IntArrayList) bondAtomBIndex.getRidList( iInt,Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    // Find the hydrogen(s) from the donor
                    boundHList.setSize(0);
                    for (s=0;s<2;s++) {
                        if ( s==0) {
                            boundListBond = boundListABond;
                        } else {
                            boundListBond = boundListBBond;
                        }
                        for ( x=0;x<boundListBond.size();x++) {
                            bondRid = boundListBond.get(x);
                            if ( s==0) {
                                atomRid = gumbo.bond.atom_B_Id[bondRid];
                            } else {
                                atomRid = gumbo.bond.atom_A_Id[bondRid];
                            }
                            if ( elementId[atomRid] != Chemistry.ELEMENT_ID_HYDROGEN ) {
                                continue;
                            }
                            boundHList.add( atomRid );
                        }
                    }
                    //General.showDebug("Found number of hydrogens attached to donor: " + boundHList.size());
                    foundHBBond = false;
                    for ( k=0;k<boundHList.size();k++) {
                        ridH = boundHList.get(k);
                        if ( Calculation.isHydrogenBond(this, i, ridH, j,
                                    hbHADistance, hbDADistance, hbDHAAngle)) {
                            foundHBBond = true;
                            break;
                        }
                    }
                    if ( ! foundHBBond ) {
                        continue;
                    }
                    // Continue as bifurcated hb's exist too.

                    // Now check the other models using the same criteria, if any of the models
                    // doesn't satisfy the criterium it's considered not a good bond.
                    boolean isValid = true;
                    for (int m=2;m<=modelCount;m++) {
                        k = modelSiblingIds[i][m-1]; // m-1 because models start numbering at 1.
                        l = modelSiblingIds[j][m-1];
                        int n = modelSiblingIds[ridH][m-1];
                        if ( Defs.isNull( k) ) {
                            General.showError("Found a bad sibling for atom in first model for model: " + m + " " + toString(k));
                            return false;
                        }
                        if ( Defs.isNull( l) ) {
                            General.showError("Found a bad sibling for atom in first model for model: " + m + " " + toString(l));
                            return false;
                        }
                        if ( ! Calculation.isHydrogenBond(this, k, n, l,
                                    hbHADistance, hbDADistance, hbDHAAngle)) {
                            /**
                            General.showDebug("Ignoring hydrogen bond between atoms:\n" +
                                toString(k) + General.eol +
                                toString(n) + General.eol +
                                toString(l) + General.eol +
                                "because in model " + m + " the atoms aren't bonded ");
                             */
                            isValid = false;
                            break;
                        }
                    }
                    if ( ! isValid ) {
                        continue;
                    }
                    // Add the hydrogen bond. This is the only memory intensive part.
                    // Method could be more speedier when the new rows are prearranged. Let's see if it's needed first.
                    // This renders the bond selected too.
                    int bond_rid_2 = gumbo.bond.add( i, j, Defs.NULL_INT,  Defs.NULL_INT, Bond.BOND_TYPE_HYDROGEN );
                    if ( bond_rid_2 < -1 ) {
                        String msg = "Failed to add a hydrogen bond between atoms\n" +
                                        toString(i) + General.eol +
                                        toString(j) + General.eol;
                        throw new Exception(msg);
                    }
                    gumbo.bond.value_2[bond_rid_2] = (float) (calcAngle( i, ridH, j )*Geometry.CF);
                }
            }
            General.showOutput("Found number of new hydrogen bond candidates: " + gumbo.bond.selected.cardinality());
            //General.showOutput( gumbo.bond.toString(gumbo.bond.selected) );
            IndexHashedIntToMany onAtomA = (IndexHashedIntToMany) gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, Index.INDEX_TYPE_HASHED);
//            IndexHashedIntToMany onAtomB = (IndexHashedIntToMany) gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, Index.INDEX_TYPE_HASHED);

            // Check if the donor is in zero or two hydrogen bonds. Note that only water can be a twofold donor.
            for (int i=atomInFirstModelDonor.nextSetBit(0);i>=0;i=atomInFirstModelDonor.nextSetBit(i+1)) {
                // The donor is always listed first for hydrogen bonds
                IntArrayList bondList = onAtomA.getRidList( i );
                // the method above returns null when the result is empty.
                // That's very fast and memory cheap but takes some more checking.
                if ( bondList == null ) {
                    bondList = new IntArrayList();
                }
                /**
                General.showDebug("Found a potential hydrogen bond list:\n" +
                    gumbo.bond.toString(PrimitiveArray.toBitSet( bondList,-1)));
                 */
                for (x=bondList.size()-1;x>=0;x--) {
                    bondRid = bondList.get(x);
                    if ( gumbo.bond.type[ bondRid ] != Bond.BOND_TYPE_HYDROGEN ) {
                        bondList.remove(x);
                    }
                }
                if ( bondList.size() > 2 ) {
                    BitSet bondSet = PrimitiveArray.toBitSet(bondList, -1);
                    String bondString = gumbo.bond.toString( bondSet );
                    General.showWarning("Got more than two hydrogen bond for this donor ("+
                        nameList[i] + "). Hydrogen bonds ("+bondList.size()+") read: ");
                    General.showWarning(General.eol + bondString);
                    //return false;
                }
            }
            // Check if the acceptor is in zero to two hydrogen bonds.
            //todo
        } catch ( Throwable t ) {
            General.showThrowable(t);
            General.showError("Trying to restore state after exception");
//            status = false;
        }


        General.showOutput("Found number of new hydrogen bonds: " + gumbo.bond.selected.cardinality());
        gumbo.bond.calculateValues( gumbo.bond.selected );
        return InOut.writeTextToFile(new File(summaryFileName),
                gumbo.bond.toString( gumbo.bond.selected ),
                true,false);
    }


    /**
     * This method adds bonds to the soup based on element id.
     * Only a certain maximum number of bonds will be maintained per atom. E.g. 4 for carbons.
     * <P>
     * The selected attribute of the Bond class will hold the newly added bonds.
     * Atoms without a set element id will be assumed to be non-bonding for this
     * routine.
     * <P>The algorithm uses a method suggested by Gert Vriend. It uses the division of atoms into
     * residues and only does the square interactions between those residues close enough for it's
     * atoms to be able to bound.
     * The argument tolerance is the added distance to half the sum of radii involved that is still to be
     * considered a bond. 0.1 is recommended with atom radii defined in the class Chemistry.
     * NB ONLY bonds to the first model will be added. Use the information on model-related atoms
     * in the Atom class to find the others.
     * @param resInMaster
     * @param tolerance 0.1 recommended. Maybe a null value specified so default will be taken.
     * @return true if successful. Can be successful without selected atoms and residues and
     *without calculating bonds.
     *
     */
    public boolean calcBond(BitSet resInMaster, float tolerance) {
//        General.showOutput("Starting calcBond");
//        boolean printProgress = true;
        boolean status = true;
        boolean isValid;
        if ( resInMaster.cardinality() == 0 ) {
            General.showWarning("No residues in master selected so not able to do calcBond.");
            return true;
        }

        BitSet bondRidSet = gumbo.bond.getBondListForResRidSet(resInMaster);
        if ( bondRidSet.cardinality()==0) {
//            General.showDebug("No bonds found for given residues before doing calcBond");
        } else {
//            General.showDebug("Removing all bonds for given residues before doing calcBond");
            gumbo.bond.mainRelation.removeRows(bondRidSet, false, false);
            gumbo.bond.resetConvenienceVariables();
        }
        BitSet atomsInMaster = gumbo.entry.getAtomsInMasterModel();
        if ( atomsInMaster == null ) {
            General.showError("Failed to get atoms in master model.");
            return false;
        }
        if ( atomsInMaster.cardinality() == 0 ) {
            General.showWarning("No atoms in master so not able to do calcBond.");
            return true;
        }
        if ( Defs.isNull(tolerance)) {
            tolerance = DEFAULT_TOLERANCE_CALCULATE_BONDS;
        }
        /** Calculated distance or an approximation of it        */
        float d, d_sibling;
        /** The maximum distance for a specific potential bond         */
        float distanceMax = 2f*Chemistry.ELEMENT_RADIUS_SULFUR + tolerance;
        /** The maximum distance for any potential bond*/
        float distance;
        int bond_rid = -1;

        int atomIElementId;
        int atomJElementId;
        float atomIRadius;
        float atomJRadius;
        BitSet atomsWithoutBond         = new BitSet();
        BitSet atomsInMasterTodo        = new BitSet();
        BitSet resInMasterTodo          = new BitSet();
        BitSet bondsPotentiallyTooMuch  = new BitSet();

        int i,j,ri,rj,k,l,x,in,jn;
        BitSet atomInFirstModelWithoutElement = PrimitiveArray.getRidsByValue(
            elementId, Chemistry.ELEMENT_ID_UNKNOWN );
        atomInFirstModelWithoutElement.and( atomsInMaster );
        atomsInMasterTodo.or( atomsInMaster );
        atomsInMasterTodo.andNot( atomInFirstModelWithoutElement );
        resInMasterTodo.or( resInMaster );

        int entryRID = gumbo.entry.getEntryId();
        if ( entryRID < 0 ) {
            General.showError("Failed to get single entry id");
            return false;
        }
        BitSet modelsInEntry = gumbo.entry.getModelsInEntry(entryRID);
        if ( modelsInEntry == null ) {
            General.showError("Failed to do calcBond because failed to get the models in this entry for rid: " + entryRID);
            return false;
        }
        int modelCount = modelsInEntry.cardinality();
        if ( modelCount == 0 ) {
            General.showWarning("No models in entry so not able to do calcBond.");
            return true;
        }

        try {
            int maxRes = gumbo.res.mainRelation.sizeMax;
            float[] resRadius   = new float[maxRes]; // (overestimate) of radius of sphere around residue.
            int[][] resCloseTo  = new int[maxRes][]; // list of residues within certain distance
            int[][] resAtoms    = new int[maxRes][]; // list of atoms making up the residue
            IndexHashedIntToMany indexAtomOnRes = (IndexHashedIntToMany) mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_HASHED);
            // first calculate some info for the residues
            for (ri=resInMasterTodo.nextSetBit(0);ri>=0;ri=resInMasterTodo.nextSetBit(ri+1)) {
                IntArrayList rAtoms = indexAtomOnRes.getRidList(ri);
                if ( rAtoms == null ) {
                    String msg = "failed to get list of atoms for residue: " + gumbo.res.toString(ri);
                    throw new Exception(msg);
                }
                if ( rAtoms.size() == 0 ) {
                    resInMasterTodo.clear(ri);
                    continue;
                }
                resAtoms[ri] = PrimitiveArray.toIntArray(rAtoms);
                BitSet resAtomSet = PrimitiveArray.toBitSet(resAtoms[ri],-1);
                //General.showDebug("Found atoms in residue:\n" + gumbo.res.toString(ri) + General.eol + toString(resAtomSet));
                float[] tempPosition = getCenter(resAtomSet);
                if ( tempPosition == null ) {
                    String msg = "failed to get average position for residue: " + gumbo.res.toString(ri);
                    throw new Exception(msg);
                }
                gumbo.res.setXYZ( ri, tempPosition );
                float diameter = getDiameter(resAtomSet);
                resRadius[ri] = diameter/2;
            }
            // Second calculate closeness residues using the radius of the residue (0 in case of a single atom)
            // and a measure for the longest bond to expect.
            IntArrayList resCloseToList = new IntArrayList();
            for (ri=resInMasterTodo.nextSetBit(0);ri>=0;ri=resInMasterTodo.nextSetBit(ri+1)) {
                resCloseToList.setSize(0);
                // do self comparison too so it gets stored
                for (rj=resInMasterTodo.nextSetBit(ri);rj>=0;rj=resInMasterTodo.nextSetBit(rj+1)) {
                    float dij = gumbo.res.calcDistanceFast(ri,rj);
                    if ( dij < ( resRadius[ri] + resRadius[rj] + distanceMax)) {
                        resCloseToList.add( rj );
                    }
                }
                resCloseTo[ri] = PrimitiveArray.toIntArray( resCloseToList );
                //General.showDebug("Found residues close:\n" + PrimitiveArray.toString( resCloseTo[ri]));
            }

            gumbo.bond.selected.clear();
            int countAtomsChecked = 0;
            for (ri=resInMasterTodo.nextSetBit(0);ri>=0;ri=resInMasterTodo.nextSetBit(ri+1)) {
                //General.showDebug("Looking at residue ri: " + ri );
                int[] resAtomListA = resAtoms[ri];
                int resAtomListASize = resAtomListA.length;
                int[] resCloseToListP = resCloseTo[ri]; // P for primitive to distinguish from the one above.
                int resCloseToListCount = resCloseToListP.length;
                for (x=0;x<resCloseToListCount;x++) {
                    rj = resCloseToListP[x];
                    //General.showDebug("Looking at residue rj: " + rj );
                    int[] resAtomListB = resAtoms[rj];
                    int resAtomListBSize = resAtomListB.length;
                    for (in=0;in<resAtomListASize;in++) {
                        i = resAtomListA[in];
                        //General.showDebug("Looking at atom i: " + i );
                        atomIElementId = elementId[i];
                        if ( Defs.isNull( atomIElementId)) {
                            continue;
                        }
                        atomIRadius = Chemistry.radii[atomIElementId];
                        if ( Defs.isNull( atomIRadius )) {
                            continue;
                        }
                        float atomIRadiusAndTolerance = atomIRadius + tolerance;
                        for (jn=0;jn<resAtomListBSize;jn++) {
                            j = resAtomListB[jn];
                            //General.showDebug("Looking at atom j: " + j );
                            if ( (ri==rj) && (i>=j) ) { // prevent self bonds.
                                continue;
                            }
                            atomJElementId = elementId[j];
                            if ( Defs.isNull( atomJElementId)) {
                                continue;
                            }
                            atomJRadius = Chemistry.radii[atomJElementId];
                            if ( Defs.isNull( atomJRadius )) {
                                continue;
                            }
                            // Might be faster to do a lookup in a pregenerated table
                            distance = atomIRadiusAndTolerance + atomJRadius;
                            /** prevent to do the multiplication and square root. */
                            if (
                                ( Math.abs( xList[i] - xList[j]) > distance ) ||
                                ( Math.abs( yList[i] - yList[j]) > distance ) ||
                                ( Math.abs( zList[i] - zList[j]) > distance )) { // checking 2 or 3 seems to be optimum for speed.
                                continue;
                            }
                            d = calcDistanceFast( i, j );
                            countAtomsChecked++;
                            /**
                            if ( printProgress ) {
                                if ( (countAtomsChecked % 1000 ) == 0 ) {
                                    General.showOutputNoEOL( "#" );
                                }
                            }
                             */
                            if (  d > distance ) {
                                continue;
                            }

                            // Now check the other models using the same criteria, if any of the models
                            // doesn't satisfy the criterium it's considered not a good bond.
                            isValid = true;
                            for (int m=2;m<=modelCount;m++) {
                                k = modelSiblingIds[i][m-1]; // m-1 because models start numbering at 1.
                                l = modelSiblingIds[j][m-1]; // m-1 because models start numbering at 1.
                                if ( Defs.isNull( k) ) {
                                    General.showError("Found a bad sibling for atom in first model for model: " + m + " " + toString(k));
                                    return false;
                                }
                                if ( Defs.isNull( l) ) {
                                    General.showError("Found a bad sibling for atom in first model for model: " + m + " " + toString(l));
                                    return false;
                                }
                                d_sibling = calcDistanceFast( k, l );
                                if ( d_sibling > distance ) {
                                    General.showDebug("Ignoring bond between atoms:\n" +
                                        toString(i) + General.eol +
                                        toString(j) + General.eol +
                                        "because in model " + m + " the distance " + d_sibling +
                                        " is larger than " + distance);
                                    isValid = false;
                                    break;
                                }
                            }
                            if ( ! isValid ) {
                                continue;
                            }
                            // Add a close contact. This is the only memory intensive part.
                            // Method could be more speedier when the new rows are prearranged. Let's see if it's needed first.
                            bond_rid = gumbo.bond.add( i, j, Defs.NULL_INT,  Defs.NULL_INT, Bond.BOND_TYPE_TENTATIVE );
                            if ( bond_rid < -1 ) {
                                String msg = "Failed to add a bond between atoms\n" +
                                                toString(i) + General.eol +
                                                toString(j) + General.eol;
                                throw new Exception(msg);
                            }
                            // Might as well assign the value while we're at it.
                            gumbo.bond.value_1[bond_rid] = d;
                        }
                    }
                }
            }
//            if ( printProgress ) {
//                General.showOutput( "\nDone. Checked number of potential pairs: " + countAtomsChecked);
//            }

//            General.showOutput("Found number of new bond candidates: " + gumbo.bond.selected.cardinality());
            //General.showOutput( gumbo.bond.toString(gumbo.bond.selected) );
            IndexHashedIntToMany onAtomA = (IndexHashedIntToMany) gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, Index.INDEX_TYPE_HASHED);
            IndexHashedIntToMany onAtomB = (IndexHashedIntToMany) gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, Index.INDEX_TYPE_HASHED);
            for (i=atomsInMasterTodo.nextSetBit(0);i>=0;i=atomsInMasterTodo.nextSetBit(i+1)) {
                IntArrayList bondListA = onAtomA.getRidList( i );
                IntArrayList bondListB = onAtomB.getRidList( i );
                // the method above returns null when the result is empty.
                // That's very fast and memory cheap but takes some more checking.
                if ( bondListA == null ) {
                    if (bondListB == null) {
                        bondListA = new IntArrayList();
                    } else {
                        bondListA = bondListB;
                    }
                } else {
                    if (bondListB != null) {
                        bondListA.addAllOf( bondListB );
                    }
                }

                if ( bondListA.size() > Chemistry.MAX_BONDS[elementId[i]] ) {
                    BitSet bondSet = PrimitiveArray.toBitSet(bondListA, -1);
                    String bondString = gumbo.bond.toString( bondSet );
                    General.showWarning("Got more bonds than allowed for this atom's ("+
                        nameList[i] + ") element type ("+Chemistry.MAX_BONDS[elementId[i]]+"). Bonds ("+bondListA.size()+") read: ");
                    General.showWarning(General.eol + bondString);
                    bondsPotentiallyTooMuch.or( bondSet );
                    //General.showWarning("Atom reads: ");
                    //General.showWarning(General.eol + toString(i));
                }
                // for printing later.
                if ( bondListA.size() == 0 ) {
                    atomsWithoutBond.set(i);
                }
            }

        } catch ( Throwable t ) {
            General.showThrowable(t);
            General.showError("Trying to restore state after exception");
            status = false;
        }

        if ( atomsWithoutBond.cardinality() != 0 ) {
            General.showWarning("Got no bonds for atoms: ");
            General.showWarning(General.eol + toString(atomsWithoutBond));
        }
        if ( bondsPotentiallyTooMuch.cardinality() != 0 ) {
            General.showWarning("Got bonds that are potentially too much: " + bondsPotentiallyTooMuch.cardinality());
            General.showWarning(General.eol + gumbo.bond.toString(bondsPotentiallyTooMuch));
        }



        /** Restore to state as to before calling this method.
        if ( ! PrimitiveArray.setValueByRids( elementId, atomInFirstModelWithoutElement, Chemistry.ELEMENT_ID_UNKNOWN)) {
            status = false;
        }
         */
        //General.showOutput("Found new bonds: " + gumbo.bond.toString( gumbo.bond.selected));

        gumbo.bond.resetConvenienceVariables();

        if ( ! status ) {
            General.showError("Failed to calcBond");
        }
        return status;
    }

    /** Returns those residues for which there are atoms as given in the argument
     */
    public BitSet getResidueList(BitSet atomSelection) {
        BitSet result = new BitSet();
        for (int rid=atomSelection.nextSetBit(0);rid>=0;rid=atomSelection.nextSetBit(rid+1)) {
            result.set( resId[rid] );
        }
        return result;
    }

    /** Using the sibling list to get the atom rids of model related siblings. Make sure
     *the sibling list is set. Make sure the model number is within range. No checks done
     *by this routine.
     */
    private IntArrayList getAtomsInModel(int modelNumber, IntArrayList ridList) {
        IntArrayList result = new IntArrayList();
        result.setSize(ridList.size());
        int ridListSize = ridList.size();
        int modelId = modelNumber-1; // -1 because the model numbers start with 1.
        for (int i=0;i<ridListSize;i++) {
            int newRid = modelSiblingIds[ridList.getQuick(i)][modelId];
            result.setQuick(i,newRid);
        }
        return result;
    }

    /** Using the sibling list to get the atom rid of master model.
     * Normally this should be inlined for speed but the method is
     * here to remind us.
     * */
    public int getMasterAtomRid(int atomRid) {
        return masterAtomId[atomRid];
    }

    /**
     * This method calculates distance shorter than a certain value.
     * <P>
     * The selected attribute of the Distance class will hold the newly added distances.
     * <P>The algorithm is similar as in calcBond.
     * NB all selected atoms in the first model of the selected entry will be
     * scanned for all models. E.g. if the atom isn't selected in
     *a consecutive model it will still be scanned for it.
     *<P>Make sure bonds are already calculated if you want to exclude bonded
     *contacts.
     * @return True for success (use showDistanceList for a printout of selected results)
     * @param intraMolecular Look for contacts within molecules.
     * @param interMolecular Look for contacts between atoms in different molecules.
     * @param minResDiff Minimum number of residues different. In order to prevent hits between atoms separated by
     *      only a few bonds, a value of 1 or larger is suggested.
     * @param minModels The minimum number of models in which the distance is smaller than the cutoff.
     * @param cutoff Distance under which contacts will be compiled.
     */
    public boolean calcDistance(float cutoff, int minResDiff, int minModels,
                boolean intraMolecular, boolean interMolecular) {
        General.showDebug("Starting calcDistance");
        boolean printProgress = false;
        boolean status = true;
//        boolean isValid;
        int i,j,ri,rj,x,in,jn;
//        int distance_rid = -1;
        float d;

        if ( (!intraMolecular)&&(!interMolecular) ) {
            General.showWarning("Nothing to calculate if both intra and inter molecular distances are to be ignored.");
            return true;
        }
        BitSet resInMasterTodo          = new BitSet();

        int entryRID = gumbo.entry.getEntryId();
        if ( entryRID < 0 ) {
            General.showError("Failed to get single entry id");
            return false;
        }
        BitSet atomsInMasterTodo = gumbo.entry.getAtomsInMasterModel();
        if ( atomsInMasterTodo == null ) {
            General.showError("Failed to get atoms in master model.");
            return false;
        }
        atomsInMasterTodo.and(selected);
        resInMasterTodo = getResidueList( atomsInMasterTodo );
        General.showDebug("Looking for closeness between " + resInMasterTodo.cardinality() + " residues in master model.");
        BitSet modelsInEntry = gumbo.entry.getModelsInEntry(entryRID);
        if ( modelsInEntry == null ) {
            General.showError("Failed to do calcDistance because failed to get the models in this entry for rid: " + entryRID);
            return false;
        }
        int modelCount = modelsInEntry.cardinality();
        BitSet[] resInModelMTodo = new BitSet[modelCount+1];
        for (int m=1;m<=modelCount;m++) {
            resInModelMTodo[m] = new BitSet();
        }
        /** Check if any bonds are present */
        if ( gumbo.bond.mainRelation.used.nextSetBit(0) < 0 ) {
            General.showError("Please calculate bonds before using routine to calculate distances.");
            return false;
        }
        IndexHashedIntToMany indexBondOnAtomA = (IndexHashedIntToMany) gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, Index.INDEX_TYPE_HASHED);
        IndexHashedIntToMany indexBondOnAtomB = (IndexHashedIntToMany) gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, Index.INDEX_TYPE_HASHED);

        try {
            int maxRes = gumbo.res.mainRelation.sizeMax;
            float[] resRadius   = new float[maxRes]; // (overestimate) of radius of sphere around residue.
            int[][] resAtoms    = new int[maxRes][]; // list of atoms making up the residue
            IndexHashedIntToMany indexAtomOnRes = (IndexHashedIntToMany) mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_HASHED);
            // first calculate some info for the residues
            for (ri=resInMasterTodo.nextSetBit(0);ri>=0;ri=resInMasterTodo.nextSetBit(ri+1)) {
                //General.showDebug("Looking at residue A : " + gumbo.res.toString(ri));
                IntArrayList rAtoms = indexAtomOnRes.getRidList(ri);
                if ( rAtoms == null ) {
                    String msg = "failed to get list of atoms for residue: " + gumbo.res.toString(ri);
                    throw new Exception(msg);
                }
                if ( rAtoms.size() == 0 ) {
                    String msg = "list of atoms todo is empty for residue: " + gumbo.res.toString(ri);
                    throw new Exception(msg);
                }
//                int ridFirstAtomInRes = rAtoms.getQuick(0);         // rid of the first atom in the residue
                for (int m=1;m<=modelCount;m++) {                   // m is the model number
                    rj = ri;                                        // rj is the rid of the residue
                    IntArrayList sAtoms = rAtoms;
                    if ( m != 1 ) {
                        sAtoms = getAtomsInModel(m, rAtoms);
                        rj = resId[ sAtoms.getQuick(0) ];           // use the reference to the residue rid from the first atom in the residue
                    }
                    resAtoms[rj] = PrimitiveArray.toIntArray(sAtoms);
                    BitSet resAtomSet = PrimitiveArray.toBitSet(resAtoms[rj],-1);
                    //General.showDebug("Found atoms in residue (A):\n" +   toString(resAtomSet));
                    float[] tempPosition = getCenter(resAtomSet);
                    if ( tempPosition == null ) {
                        String msg = "failed to get average position for residue: " + gumbo.res.toString(rj);
                        throw new Exception(msg);
                    }
                    gumbo.res.setXYZ( rj, tempPosition );
                    float diameter = getDiameter(resAtomSet);
                    resRadius[rj] = diameter/2;
                    resInModelMTodo[m].set(rj);
                }
            }
            // Second calculate closeness residues using the radius of the residue (0 in case of a single atom)
            IntArrayList resCloseToList = new IntArrayList();
            // Very compact data structure:
            HashOfHashes resCloseTo  = new HashOfHashes(); // list of residues within certain distance
            for (int m=1;m<=modelCount;m++) { // m is the model number
                BitSet resTodo = resInMasterTodo;
                if ( m != 1 ) {
                    resTodo = resInModelMTodo[m];
                }
                for (ri=resTodo.nextSetBit(0);ri>=0;ri=resTodo.nextSetBit(ri+1)) {
                    resCloseToList.setSize(0);
                    int molRidI = gumbo.res.molId[ri];
                    boolean inSameMol = true;
                    // do self comparison too so it gets stored
                    for (rj=resTodo.nextSetBit(ri+1);rj>=0;rj=resTodo.nextSetBit(rj+1)) {
                        // Are they in the same molecule?
                        if ( molRidI != gumbo.res.molId[rj] ) {
                            inSameMol = false;
                        }
                        // Only use those that are selected.
                        if ( (inSameMol &&  (!intraMolecular)) ||
                             (!inSameMol && (!interMolecular))) {
                            continue;
                        }
                        // Exclude contacts between close residues.
                        if ( inSameMol && (Math.abs(gumbo.res.number[rj]-gumbo.res.number[ri])<=minResDiff)) {
                            continue;
                        }

                        float dij = gumbo.res.calcDistanceFast(ri,rj);
                        if ( dij < ( resRadius[ri] + resRadius[rj] + cutoff)) {
                            resCloseToList.add( rj );
                        }
                    }
                    resCloseTo.put(new Integer(m), new Integer(ri), PrimitiveArray.toIntArray( resCloseToList ));
                    //General.showDebug("Found residues close:\n" + PrimitiveArray.toString( resCloseTo.get( new Integer(m), new Integer(ri))));
                }
            }

            gumbo.distance.selected.clear();
            int countAtomsChecked = 0;
            // Simply look for pairs of atoms in the master molecule that will be stored in:
            HashOfHashes goodPairs = new HashOfHashes();
            for (int m=1;m<=modelCount;m++) { // m is the model number
                BitSet resTodo = resInMasterTodo;
                if ( m != 1 ) {
                    resTodo = resInModelMTodo[m];
                }
                for (ri=resTodo.nextSetBit(0);ri>=0;ri=resTodo.nextSetBit(ri+1)) {
                    //General.showDebug("Looking at residue ri: " + ri );
                    int[] resAtomListA = resAtoms[ri];
                    int resAtomListASize = resAtomListA.length;
                    int[] resCloseToListP = (int[]) resCloseTo.get(new Integer(m), new Integer(ri)); // P for primitive to distinguish from the one above.
                    int resCloseToListCount = resCloseToListP.length;
                    for (x=0;x<resCloseToListCount;x++) {
                        rj = resCloseToListP[x];
                        //General.showDebug("Looking at residue rj: " + rj );
                        int[] resAtomListB = resAtoms[rj];
                        int resAtomListBSize = resAtomListB.length;
                        for (in=0;in<resAtomListASize;in++) {
                            i = resAtomListA[in];
                            //General.showDebug("Looking at atom i: " + i );
                            int iMaster = masterAtomId[i];
                            // bonds are only defined for the master model.
                            IntArrayList bondListAtomsA = indexBondOnAtomA.getRidList(iMaster);
                            IntArrayList bondedAtomsA = gumbo.bond.getAtomList(bondListAtomsA,1);
                            for (jn=0;jn<resAtomListBSize;jn++) {
                                j = resAtomListB[jn];
                                //General.showDebug("Looking at atom j: " + j );
                                if ( (ri==rj) && (i>=j) ) { // prevent self contacts?
                                    continue;
                                }
                                /** prevent to do the multiplication and square root. */
                                if (
                                    ( Math.abs( xList[i] - xList[j]) > cutoff ) ||
                                    ( Math.abs( yList[i] - yList[j]) > cutoff ) ||
                                    ( Math.abs( zList[i] - zList[j]) > cutoff )) { // checking 2 or 3 seems to be optimum for speed.
                                    continue;
                                }
                                d = calcDistanceFast( i, j );
                                countAtomsChecked++;
                                /**
                                if ( printProgress ) {
                                    if ( (countAtomsChecked % 1000 ) == 0 ) {
                                        General.showOutputNoEOL( "#" );
                                    }
                                }
                                 */
                                if (  d > cutoff ) {
                                    continue;
                                }
                                // exclude self contact (expensive so do last
                                int jMaster = masterAtomId[j];
                                IntArrayList bondListAtomsB = indexBondOnAtomB.getRidList(jMaster);
                                IntArrayList bondedAtomsB = gumbo.bond.getAtomList(bondListAtomsB,0);
                                /**
                                General.showDebug("Atoms with A: "+i+" "+iMaster+" "+toString(i)+" "+bondListAtomsA);
                                General.showDebug("Atoms with B: "+j+" "+jMaster+" "+toString(j)+" "+bondListAtomsB);
                                General.showDebug("Atoms bonded to A: "+toString(PrimitiveArray.toBitSet(bondedAtomsA,-1)));
                                General.showDebug("Atoms bonded to B: "+toString(PrimitiveArray.toBitSet(bondedAtomsB,-1)));
                                General.showDebug("");
                                 */
                                if ( bondedAtomsA.contains(jMaster) ||
                                     bondedAtomsB.contains(iMaster)) {
                                    /**
                                    General.showDebug("Skipping bonded atoms based on master: ");
                                    General.showDebug(toString(i));
                                    General.showDebug(toString(j));
                                     */
                                    continue;
                                }

                                // flip a bit for the model in the pair of the atoms in the master
                                Integer ii = new Integer(masterAtomId[i]);
                                Integer jj = new Integer(masterAtomId[j]);
                                BitSet goodPairModels = (BitSet) goodPairs.get(ii,jj);
                                if ( goodPairModels == null ) {
                                    goodPairModels = new BitSet();
                                    goodPairs.put( ii, jj, goodPairModels);
                                }
                                goodPairModels.set(m);
                            }
                        }
                    }
                }
            }
            if ( printProgress ) {
                General.showOutput( "\nDone. Checked number of potential pairs: " + countAtomsChecked);
            }
            ArrayList keysA = new ArrayList(goodPairs.keySet());
            Collections.sort(keysA);
            for (i=0;i<keysA.size();i++) {
                Integer ii = (Integer) keysA.get(i);
                int iii  = ii.intValue();
                HashMap map = (HashMap) goodPairs.get(ii);
                ArrayList keysB = new ArrayList(map.keySet());
                Collections.sort(keysB);
                for (j=0;j<keysB.size();j++) {
                    Integer jj = (Integer) keysB.get(j);
                    BitSet goodPairModels = (BitSet) map.get(jj);
                    int goodPairModelCount = goodPairModels.cardinality();
                    if ( goodPairModelCount < minModels ) {
                        continue;
                    }
                    int jjj  = jj.intValue();
                    // Add a close contact.
                    int dist_rid = gumbo.distance.add( iii, jjj, Defs.NULL_INT,  Defs.NULL_INT, Bond.BOND_TYPE_TENTATIVE );
                    if ( dist_rid < -1 ) {
                        String msg = "Failed to add a distance between atoms\n" +
                                        toString(iii) + General.eol +
                                        toString(jjj) + General.eol;
                        throw new Exception(msg);
                    }
                }
            }

            General.showOutput("Found number of new distances: " + gumbo.distance.selected.cardinality());
        } catch ( Throwable t ) {
            General.showThrowable(t);
            General.showError("exception in Atom.calcDistance");
            status = false;
        }

        if ( ! gumbo.distance.calculateValues(gumbo.distance.selected, true)) {
            General.showError("Failed in calculating distances.");
            return false;
        }
        String msg = gumbo.distance.toString(gumbo.distance.selected, true, cutoff);
        if ( msg == null ) {
            General.showError("Failed in showing distances.");
            return false;
        }
        General.showOutput("Found contacts below cutoff: " + cutoff + General.eol + msg);

        if ( ! status ) {
            General.showError("Failed to calcDistance");
        }
        return status;
    }


    /** Returns a list of int[2] objects that are the rid and original
     *location in the array. The second int might not be that useful but
     *the comparator expects an object with 2 ints anyway, so might as well
     *use it.
     *E.g. a list of atom rids:
     *91,90,92 would perhaps be returned ordered as: [90,1], [91,0], [92,2]
     */
    public ArrayList getSorted( IntArrayList a, Comparator c ) {
        int size = a.size();
        int aRid;
        ArrayList atomsInvolved = new ArrayList( size );
        for (int i=0;i<size;i++) {
            aRid = a.getQuick(i);
            if ( Defs.isNull( aRid ) ) {
                //General.showDebug("Trying to sort an array with an unknown atom rid at position in array: " + i);
            }
            atomsInvolved.add( new int[] { aRid, i } );
        }
        try {
            Collections.sort(atomsInvolved, c );
        } catch ( Exception e ) {
            General.showThrowable(e);
            return null;
        }
        return atomsInvolved;
    }

    /** Returns a map of the natural order of the atoms.
     *E.g. a list of atom ids:
     *91,90,93,92 would perhaps be ordered 90,91,92,93
     *for that list a map would be returned:
     *1,0,3,2 to be read as
     *original element at postion 0 (91) should go to new position 1 etc.
     */
    public int[] getOrderMap( IntArrayList a) {
        Comparator atomComparator = new ComparatorAtom(gumbo);
        ArrayList atomsInvolved = getSorted(a, atomComparator);
        if ( atomsInvolved == null ) {
            return null;
        }
        int size = a.size();
        int[] map = new int[size];
        int oldPosI;
        for (int i=0;i<size;i++) {
            oldPosI = ((int[])atomsInvolved.get(i))[1];
            map[ oldPosI ] = i;
        }
        return map;
    }


    /** Will order the atom rids in the input list.*/
    public boolean order( IntArrayList a) {
        if ( a == null ) {
            return false;
        }
        if ( a.size() <= 1 ) {
            return true;
        }
        int[] sortMap = getOrderMap( a );
        if ( sortMap == null ) {
            General.showError("Failed to get sortMap");
            return false;
        }
        //General.showDebug("Sort map is: " + PrimitiveArray.toString( sortMap ));
        if ( ! PrimitiveArray.sortTogether( sortMap, new Object[] { a } ) ) {
            General.showError("Failed to sort IntArrayLists together");
            return false;
        }
        return true;
    }

    /** Defaults to false for not having to consider all atoms. So will return 0 for
     *equal even though number of atoms in the list might be different.
     */
    public int compare(IntArrayList a1, IntArrayList a2) {
        return compare( a1, a2, false );
    }


   /** Comparison given a list of rids within the array. Null rids will be last in the list.
    *If the parameter considerAll is set to true then in case of equality on the
    *compared elements the longer list will last in the order.
     */
    public int compare(IntArrayList a1, IntArrayList a2, boolean considerAll ) {
        // compare each element until a difference was found.
        int minLength = Math.min( a1.size(), a2.size());
        if ( minLength == 0 ) {
            General.showWarning("Not sure if comparing empty atom rids array is useful: arrays are: " + a1.toString() + " and " + a2.toString() );
        }
        for (int i=0; i<minLength; i++) {
            int c = compare( a1.getQuick(i), a2.getQuick(i), false, false );
            if ( c != 0 ) {
                return c;
            }
        }
        if ( (!considerAll) || (minLength == Math.max( a1.size(), a2.size())) ) { // Even sizes were the same so they're really the same
            return 0;
        }
        if ( a1.size() > a2.size() ) { // The largest array will then be last.
            return 1;
        }
        return -1;
    }

    /** Comparison given the rid within the array. Null rids will be last in the list. */
    public int compare(int atomRid1, int atomRid2 ) {
        return compare(atomRid1, atomRid2, false, false);
    }

    /** Comparison given the rid within the array. Null rids will be last in the list.
     *Optionally the entry and atom name can be ignored in the comparison.
     *Note that this method uses a -very- different sorting than the method setAtomIdsPerModel.
     *The result is a negative integer if atomRid1 precedes atomRid2.
     */
    public int compare(int atomRid1, int atomRid2, boolean ignoreEntry, boolean ignoreAtomName) {

        //General.showDebug("**** Comparing atoms with rids: " + atomRid1 + ", " + atomRid2);
        if ( Defs.isNull( atomRid1 ) && Defs.isNull( atomRid2 ) ) {
            return 0;
        }
        if ( Defs.isNull( atomRid1 ) ) {
            return 1;
        }
        if ( Defs.isNull( atomRid2 ) ) {
            return -1;
        }
        int c1, c2;

        /** Some more simple tests; remove if too expensive */
        if ( ! used.get(atomRid1) ) {
            General.showWarning("Trying compare with unused atomRid1: " + atomRid1);
            if ( atomRid1 == atomRid2 ) {
                return 0;
            }
            if ( atomRid1 < atomRid2 ) {
                return -1;
            }
            return 1;
        }
        if ( ! used.get(atomRid2) ) {
            General.showWarning("Trying compare with unused atomRid2: " + atomRid2);
            if ( atomRid1 == atomRid2 ) {
                return 0;
            }
            if ( atomRid1 < atomRid2 ) {
                return -1;
            }
            return 1;
        }

        // entry
        if ( ! ignoreEntry ) {
            c1 = gumbo.entry.number[ entryId[ atomRid1 ] ];
            c2 = gumbo.entry.number[ entryId[ atomRid2 ] ];
            //General.showDebug("Entry: " + c1 + " and " + c2);
            if ( c1 < c2 ) {
                return -1;
            }
            if ( c1 > c2 ) {
                return 1;
            }
        }

        // model
        c1 = gumbo.model.number[ modelId[ atomRid1 ] ];
        c2 = gumbo.model.number[ modelId[ atomRid2 ] ];
        //General.showDebug("Model: " + c1 + " and " + c2);
        if ( c1 < c2 ) {
            return -1;
        }
        if ( c1 > c2 ) {
            return 1;
        }

        // mol
        c1 = gumbo.mol.number[ molId[ atomRid1 ] ];
        c2 = gumbo.mol.number[ molId[ atomRid2 ] ];
        //General.showDebug("Mol  : " + c1 + " and " + c2);
        if ( c1 < c2 ) {
            return -1;
        }
        if ( c1 > c2 ) {
            return 1;
        }

        // residue
        c1 = gumbo.res.number[ resId[ atomRid1 ] ];
        c2 = gumbo.res.number[ resId[ atomRid2 ] ];
        //General.showDebug("Res  : " + c1 + " and " + c2);
        if ( c1 < c2 ) {
            return -1;
        }
        if ( c1 > c2 ) {
            return 1;
        }

        // atom
        /**
        c1 = number[  atomRid1 ];
        c2 = number[  atomRid2 ];

        if ( Defs.isNull( c1 ) || Defs.isNull( c2 ) ) {
         */
        if ( ! ignoreAtomName ) {
            String s1 = nameList[ atomRid1 ];
            String s2 = nameList[ atomRid2 ];
            //General.showDebug("Atom  : " + s1 + " and " + s2);
            if ( Defs.isNullString( s1) || Defs.isNullString( s2 ) ) {
                return 0;
            }

            return s1.compareTo( s2 );
        }

        return 0;
        /**
        }

        General.showDebug("Atom  : " + c1 + " and " + c2);
        if ( c1 < c2 ) {
            return -1;
        }
        if ( c1 > c2 ) {
            return 1;
        }

        return 0;
         */
    }

    /** Comparison given the rid within the array. Null rids will be last in the list.
     *Note that this method uses the author chain, and res but regular atom name.
     *The result is a negative integer if atomRid1 precedes atomRid2.
     */
    public int compareAuthor(int atomRid1, int atomRid2, boolean ignoreEntry) {

//        General.showDebug("**** Comparing atoms with rids: " + atomRid1 + ", " + atomRid2);
//        General.showDebug(toString(atomRid1,true));
//        General.showDebug(toString(atomRid2,true));
        if ( Defs.isNull( atomRid1 ) && Defs.isNull( atomRid2 ) ) {
            return 0;
        }
        if ( Defs.isNull( atomRid1 ) ) {
            return 1;
        }
        if ( Defs.isNull( atomRid2 ) ) {
            return -1;
        }
        int c, c1, c2;

        /** Some more simple tests; remove if too expensive */
        if ( ! used.get(atomRid1) ) {
            General.showWarning("Trying compare with unused atomRid1: " + atomRid1);
            if ( atomRid1 == atomRid2 ) {
                return 0;
            }
            if ( atomRid1 < atomRid2 ) {
                return -1;
            }
            return 1;
        }
        if ( ! used.get(atomRid2) ) {
            General.showWarning("Trying compare with unused atomRid2: " + atomRid2);
            if ( atomRid1 == atomRid2 ) {
                return 0;
            }
            if ( atomRid1 < atomRid2 ) {
                return -1;
            }
            return 1;
        }

        // entry
        if ( ! ignoreEntry ) {
            c1 = gumbo.entry.number[ entryId[ atomRid1 ] ];
            c2 = gumbo.entry.number[ entryId[ atomRid2 ] ];
//            General.showDebug("In compareAuthor: Entry: " + c1 + " and " + c2);
            if ( c1 < c2 ) {
                return -1;
            }
            if ( c1 > c2 ) {
                return 1;
            }
        }

        // model
        c1 = gumbo.model.number[ modelId[ atomRid1 ] ];
        c2 = gumbo.model.number[ modelId[ atomRid2 ] ];
//        General.showDebug("In compareAuthor: Model: " + c1 + " and " + c2);
        if ( c1 < c2 ) {
            return -1;
        }
        if ( c1 > c2 ) {
            return 1;
        }

//        // mol
//        c1 = gumbo.mol.number[ molId[ atomRid1 ] ];
//        c2 = gumbo.mol.number[ molId[ atomRid2 ] ];
//        //General.showDebug("Mol  : " + c1 + " and " + c2);
//        if ( c1 < c2 ) {
//            return -1;
//        }
//        if ( c1 > c2 ) {
//            return 1;
//        }
//
//        // residue
//        c1 = gumbo.res.number[ resId[ atomRid1 ] ];
//        c2 = gumbo.res.number[ resId[ atomRid2 ] ];
//        //General.showDebug("Res  : " + c1 + " and " + c2);
//        if ( c1 < c2 ) {
//            return -1;
//        }
//        if ( c1 > c2 ) {
//            return 1;
//        }

        // mol
        String s1 = authMolNameList[ atomRid1 ];
        String s2 = authMolNameList[ atomRid2 ];
//        General.showDebug("In compareAuthor: Mol  : [" + s1 + "] and [" + s2 + "]");
        // Decide if only one or none of them are null. They're considered equal if both are null.
        if ( !(Defs.isNull( s1 ) && Defs.isNull( s2 )) ) {
            if ( Defs.isNull( s1 ) ) {
                return 1;
            }
            if ( Defs.isNull( s2 ) ) {
                return -1;
            }
            c = s1.compareTo(s2);
            if ( c != 0 ) {
                return c;
            }
        }

        // residue
        s1 = authResIdList[ atomRid1 ];
        s2 = authResIdList[ atomRid2 ];
//        General.showDebug("In compareAuthor: Res  : [" + s1 + "] and [" + s2 +"]");
        c = s1.compareTo(s2);
        if ( c != 0 ) {
            return c;
        }

        if ( DEBUG ) {
            if ( Defs.isNull(authAtomNameList[ atomRid1 ])) {
                General.showWarning("Comparing with unset author atom name for atom1: " +
                        toString(atomRid1));
            }
            if ( Defs.isNull(authAtomNameList[ atomRid2 ])) {
                General.showWarning("Comparing with unset author atom name for atom2: " +
                        toString(atomRid1));
            }
        }
//        return authAtomNameList[ atomRid1 ].compareTo(authAtomNameList[ atomRid2 ]);

        s1 = nameList[ atomRid1 ];
        s2 = nameList[ atomRid2 ];
//        General.showDebug("In compareAuthor: Atom : [" + s1 + "] and [" + s2 +"]");
        if ( Defs.isNullString( s1) || Defs.isNullString( s2 ) ) {
            return 0;
        }

        return s1.compareTo( s2 );
    }

    /** Order the atoms toDo according to a comparator class. This will create a column (if
     *not present already) with the name: DEFAULT_ATTRIBUTE_ORDER_ID. The models, mols, and residues
     *themselves need to be ordered first. Ordering always starts at startNumber, which
     *should be zero if compatibility with
     *the TagTable and Relations api is required so the column can be used for writing out the atoms to e.g.
     *STAR files.
     */
    public boolean orderPerModelMolResAtom(BitSet toDo, Comparator atomComparator, int startNumber) {

        // Keep one if it's already available.
        if ( ! mainRelation.containsColumn( DEFAULT_ATTRIBUTE_ORDER_ID )) {
            if ( ! mainRelation.addColumnForOverallOrder()) {
                General.showError("In Atom.orderPerModelMolResAtom failed to add column for overall order id");
                return false;
            }
        }

        int[] orderIdModel  = gumbo.model.mainRelation.getColumnInt( Relation.DEFAULT_ATTRIBUTE_NUMBER );
        int[] orderIdMol    =   gumbo.mol.mainRelation.getColumnInt( Relation.DEFAULT_ATTRIBUTE_NUMBER );
        int[] orderIdRes    =   gumbo.res.mainRelation.getColumnInt( Relation.DEFAULT_ATTRIBUTE_NUMBER );
        int[] orderIdAtom   =             mainRelation.getColumnInt( Relation.DEFAULT_ATTRIBUTE_NUMBER );
        int[] orderIdAtom2  =             mainRelation.getColumnInt( Relation.DEFAULT_ATTRIBUTE_ORDER_ID );
        if ( (orderIdModel == null ) || (orderIdMol == null ) || (orderIdRes == null )
            || (orderIdAtom == null ) || (orderIdAtom2 == null ) ) {
            General.showError("Failed to get required order columns from model, mol, res, and atom (2 ids) relations for sorting");
            return false;
        }
//        General.showDebug("Prepared for orderPerModelMolResAtom");
        /** Approach may be optimized by not using objects but right now, no
         *time to code it.*/
        // Fill arrays
        int toDoSize = toDo.cardinality();
        ArrayList list = new ArrayList( toDoSize ); /** Use capacity size for efficiency */

        for (int r=toDo.nextSetBit(0); r>=0; r=toDo.nextSetBit(r+1)) {
            // The 3 expensive new objects needed per atom: values, complexObject, ObjectIntPair
            int[] values = new int[4];
            // fill the values
            values[0]        = orderIdModel[    modelId[r]];
            values[1]        = orderIdMol[      molId[r]];
            values[2]        = orderIdRes[      resId[r]];
            values[3]        = orderIdAtom[     r];
            ObjectIntPair pair = new ObjectIntPair( values, r );
            if ( pair == null ) {
                General.showError("Code bug found in orderPerModelMolResAtom.");
                return false;
            }
            list.add( pair );
        }
        /** bogus check but heck, never hurts */
        if ( list.size() != toDoSize ) {
            General.showError("Code bug found in Atoms.orderPerModelMolResAtom sizes don't match.");
            General.showError("Rows in relation found are: " + toDoSize + " and: " + list.size() );
            return false;
        }

//        General.showDebug("Prepared for orderPerModelMolResAtom (B)");
        try {
            Collections.sort(list, atomComparator);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        // Transfer back to original; reversing map
        int n = startNumber + toDoSize - 1;
        for ( int r=toDoSize-1; r > -1; r-- ) { // r equals n shifted by startNumber but decrementing might be faster?
            ObjectIntPair pair = (ObjectIntPair) list.get(r);
            //General.showDebug("Renumbering atom with rid: " + pair.i);
            orderIdAtom2[pair.i] = n--;
        }
        return true;
    }

    /**Unique sequential number within entry. Number the atoms according to row order map
     *starting at 1 for each new model.
     *Static method because it acts on the argument relation, not this class.
     *Returns true on success.
     *Todo. Actually since the atoms can be scattered. The method needs to
     *renumber them per atomset of one model. No short cuts.
     *Note that this method uses -very- different sorting from the compare
     *method.
     */
    public static boolean setAtomIdsPerModel( Relation relation ) {

        // Overwrite the local convenience variables. Watch out can be confusing.
//        int[] modelId = relation.getColumnInt( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ]);
        int[] number  = relation.getColumnInt( Relation.DEFAULT_ATTRIBUTE_NUMBER );

//        if ( (modelId==null) ||(number==null)) {
//            General.showWarning("Failed to get required columns in setAtomIdsPerModel");
//            return false;
//        }

        int[] map = relation.getRowOrderMap(Relation.DEFAULT_ATTRIBUTE_ORDER_ID);
        if ( map == null ) {
            General.showWarning("Failed to get the row order sorted out; using physical ordering."); // not fatal
            map = relation.getUsedRowMapList();
            if ( map == null ) {
                General.showError("Failed to get the used row map list so not writing this table.");
                return false;
            }
        }

        // The atoms are already ordered by model, mol, res, atom so easy to check.
        // Just need to use the order column too.
//        int prevModelNum = -1; // never happens
        int atomId = 1; // starts at 1.
        int rowId = -1;
        // Loop requires only 3 array lookups per atom!
        //General.showDebug("map: " + PrimitiveArray.toString(map));
        int s=0;
        for (int r=relation.used.nextSetBit(0); r>=0; r=relation.used.nextSetBit(r+1))  {
            rowId = map[s];
//            if ( modelId[rowId] != prevModelNum ) { // infrequently true (true the first iteration though)
//                //General.showDebug("Starting renumber of atoms at: " + atomId + " after model num changed from: " + prevModelNum + " to: " + modelNumNew[rowId] );
//                atomId = 1;
//                prevModelNum = modelId[rowId];
//            }
            number[rowId] = atomId++;
            s++;
        }
        return true;
    }

    /** Collapses the atoms in the list to a reduced set of atoms with pseudos
     *representing some. The statusList and pseudoNameList will be modified by
     *this routine, but the atomList remains untouched.
     *Returns true for success.
     *Needs to be a fast and optimized algorithm.
     *The assumption is that the input statusList and pseudoNameList has size
     *the same as the atomList.
     *Will collaps to the biggest pseudo that has all atoms represented.
     *Will check if atoms are of the same residue.
     *
     *<PRE>
    public static final int DEFAULT_OK                    = 0;
    public static final int DEFAULT_REPLACED_BY_PSEUDO    = 1;
    public static final int DEFAULT_REDUNDANT_BY_PSEUDO   = 2;
     *E.g. input: atomList = RID of atoms[ VAL 1-HA, CYS 1-HB2, CYS 1-HB3,  XXX 9-XX]
     *    output: statusList =           [        0,         1,         2,         0]
     *            pseudoNameList =       [     null,        QB,      null,      null]
     *E.g. input: atomList = RID of atoms[CYS 1-HB2, CYS 1-HB3, ASP 9-HB2, ASP 9-HB3]
     *    output: statusList =           [        1,         2,         1,         2]
     *            pseudoNameList =       [       QB,        QB,        QB,        QB]
     *E.g. input: atomList = RID of atoms[  CYS 1-H,   GLY 2-H]
     *    output: statusList =           [        0,         0]
     *            pseudoNameList =       [     null,      null]
     *</PRE>
     *
     *Algorithm:
     *for each atom x except last:
     *  for each possible pseudo p representing x and ys starting with largest p:
     *      if all ys present (check sequentially from x+1 to last):
     *          update status of x and ys
     *          break to outer loop.
     */
    public boolean collapseToPseudo( IntArrayList atomList, IntArrayList statusList,
        ObjectArrayList pseudoNameList, PseudoLib pseudoLib ) {
        // There might not that much to be done.
        int atomListSize = atomList.size();
        if ( atomListSize < 2 ) {
            return true;
        }
//        General.showDebug("Looking for pseudos in atom list of length: " + atomListSize);
        int[] elements = statusList.elements();
        Arrays.fill( elements, 0, elements.length, PseudoLib.DEFAULT_OK );// For now assume it didn't get replaced by a pseudo.
        BitSet atomsPseudoToFind = new BitSet();
        BitSet matchedAtoms = new BitSet();

        /** Try each atom but the last*/
        for (int x=0;x<atomListSize-1;x++) {
            int atomRIDUp = atomList.get(x);
            if ( Defs.isNull( atomRIDUp ) ) {
                continue;
            }
            String atomNameUp   = nameList[ atomRIDUp ];
            int resRIDUp        = resId[    atomRIDUp ];
            String resNameUp    = gumbo.res.nameList[resRIDUp ];
//            General.showDebug("Looking at atom: " + x + " " + toString(atomRIDUp));
            ArrayList psNamesUp = (ArrayList) pseudoLib.fromAtoms.get(resNameUp, atomNameUp );
            // Does the atom occur in a known pseudo atom
            if ( psNamesUp == null ) {
//                General.showDebug("Atom doesn't have pseudo" );
                continue;
            }
//            General.showDebug("Found one or more pseudo atoms for atom: " + atomNameUp + " pseudos: " + Strings.toString(psNamesUp));
            // Try to find the corresponding atoms for the pseudo atom..
            // Pseudo atom are listed in the order of the number of constituting aotms (that's largest).
            // This garantees that e.g. VAL for HG11, QG will be chosen over MG1 if all 6 atoms
            // of QG are indeed listed too.
            boolean foundAGoodPseudo = false;
            for (int p=0;p<psNamesUp.size();p++) {
                String psName = (String) psNamesUp.get(p);
//                int pseudoAtomType = ((Integer)pseudoLib.pseudoAtomType.get( resNameUp, psName)).intValue();
//                General.showDebug("Checking with pseudo atom: " + psName );
                ArrayList atomNamesInPseudo = (ArrayList) pseudoLib.toAtoms.get( resNameUp, psName );
                atomsPseudoToFind.clear();
                for (int a=0;a<atomNamesInPseudo.size();a++) {
                    atomsPseudoToFind.set(a);
                }
                matchedAtoms.clear();
                // Scan the atomList from self to last (end -1)
                for (int i=x;i<=atomListSize-1;i++) {
                    int atomRID = atomList.get(i);
                    if ( Defs.isNull( atomRID ) ) {
                        continue;
                    }
                    // Next check typically fails on all but first iteration making double loop fast.
                    if ( statusList.getQuick(i) != PseudoLib.DEFAULT_OK ) {
//                        General.showDebug("Already used atom so skipping this one -1-");
                        continue;
                    }
                    int resRID = resId[ atomRID ];
                    if ( resRID != resRIDUp ) { // The atoms need to be in first atom's residue.
                        continue;
                    }
                    String atomName = nameList[ atomRID ];
                    int atomPseudoIndex = atomNamesInPseudo.indexOf( atomName ); // requires sequential scan of a list with less than 10 elements.
                    if ( atomPseudoIndex < 0 ) {
                        continue;
                    }
//                    General.showDebug("Found atom to be in the pseudo atom: " + atomName);
                    if ( ! atomsPseudoToFind.get(atomPseudoIndex)) {
                        General.showWarning("found duplicate atom in members of restraint: ");
                        General.showWarning(toString(PrimitiveArray.toBitSet(atomList)));
                    }
                    atomsPseudoToFind.clear(atomPseudoIndex);
                    matchedAtoms.set(i);
                }
                if ( atomsPseudoToFind.cardinality() == 0 ) { // pretty fast still on 64 members.
//                    General.showDebug("Found a good pseudo atom: " + psName);
                    // At this point p points to a pseudo atom for which all atoms were present.
                    pseudoNameList.setQuick(x, psName);
                    // Mark this atom as replaced by pseudo atom.
                    // Mark all atoms as redundant.
                    for (int j=matchedAtoms.nextSetBit(0);j>=0;j=matchedAtoms.nextSetBit(j+1)) {
                        statusList.setQuick( j, PseudoLib.DEFAULT_REDUNDANT_BY_PSEUDO );
                        pseudoNameList.setQuick(j, psName); // new in order to get to the name faster for some cases.
                    }
                    // Then, mark this one as the origin of the replacement.
                    statusList.setQuick( x, PseudoLib.DEFAULT_REPLACED_BY_PSEUDO );
                    foundAGoodPseudo = true;
                } else {
//                    General.showDebug("Failed to find all atoms for this pseudo atom" );
                }
                if ( foundAGoodPseudo ) {
                    break; // from loop over pseudos
                }
            } // END of loop over pseudos
            if ( ! foundAGoodPseudo ) {
//                General.showDebug("Didn't find any pseudo atom with all atoms present for atom: " + atomNameUp );
            }
        } // END of loop over atoms x
        return true;
    }


//    /** Return the SSA partner atomRid of a 2 atom pseudo if any. Look for a duplication of this functionality somewhere else.
//     * Returns NULL_INT when none exists or
//     * -1 for error.
//     */
////    public int getSsaPartner( int atomRid, boolean useSmallestAvailable ) {
//    public int getSsaPartner( int atomRid ) {
//        int resRid = resId[atomRid];
//        String resName = gumbo.res.nameList[resRid];
//        String atomName = nameList[atomRid];
//
//        PseudoLib pseudoLib = dbms.ui.wattosLib.pseudoLib; // Should be declared elsewhere or is this fast enough?
//        ArrayList pseudoAtomNameList = (ArrayList) pseudoLib.fromAtoms.get(resName, atomName);
//        if (pseudoAtomNameList == null) { // atoms is not in known pseudoatom.
//            return Defs.NULL_INT;
//        }
//        // Use first by default
//        int idxPseudoATom = 0;
//        // not needed for simple ones we use here.
////        if ( ! useSmallestAvailable ) {
////            if ( pseudoAtomNameList.size() > 1 ) {
////                idxPseudoATom = 1;
////            }
////        }
//        String pseudoAtomName = (String) pseudoAtomNameList.get(idxPseudoATom);
//        Integer pseudoTypeInteger = (Integer) pseudoLib.pseudoAtomType.get(resName, pseudoAtomName);
//        int pseudoType = pseudoTypeInteger.intValue();
//        if (!(pseudoType==PseudoLib.DEFAULT_PSEUDO_ATOM_ID_CH2_OR_NH2||
//              pseudoType==PseudoLib.DEFAULT_PSEUDO_ATOM_ID_AROMAT_2H)) {
//            return Defs.NULL_INT;
//        }
//
//        Triplet triplet = new Triplet(pseudoAtomName, pseudoType, atomRid);
//        if (!triplet.findOtherAtoms(dbms.ui)) {
//            General.showWarning("In Atom#getSsaPartner failed to triplet.findOtherAtoms; ignoring this triplet.");
//            return Defs.NULL_INT;
//        }
//        // Which one is result and which one is input?
//        int[] atomRids = triplet.atomRids;
//        if ( atomRids[1] == atomRid ) {
//            return atomRids[0];
//        }
//        return atomRids[1];
//        /** Alternatively, consider code like: */
//    }

    public int getSsaPartnerTry2( int atomRid ) {
        int resRid = resId[atomRid];
        String resName = gumbo.res.nameList[resRid];
        String atomName = nameList[atomRid];

        if ( !((resName.equals("PHE")||resName.equals("TYR"))&&atomName.equals("CD1")) ) {
            General.showError("Failed to getSsaPartnerTry2 because not PHE/TYR:CD1 as input");
            return -1;
        }
        String atomNameStereoSibling = "CD2";
//        String atomNameStereoSibling = pseudoLib.getStereoSibling(resName, atomName, triplet.name);
        if ( atomNameStereoSibling == null ) {
            General.showError("Failed getSsaPartnerTry2 0");
            return -1;
        }
        int atomRidStereoSibling = gumbo.atom.getRidByAtomNameAndResRid(atomNameStereoSibling,resRid);
        if ( atomRidStereoSibling < 0 ) {
            General.showError("Failed getSsaPartnerTry2 1");
            return -1;
        }
        return atomRidStereoSibling;
    }

    /**     */
    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();
        //General.showDebug("Now in resetConvenienceVariables in Atom");
        resId               = (int[])       mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_SET_RES[   RelationSet.RELATION_ID_COLUMN_NAME]);// Atom (starting with fkcs)
        molId               = (int[])       mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[   RelationSet.RELATION_ID_COLUMN_NAME]);
        modelId             = (int[])       mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME]);
        entryId             = (int[])       mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME]);
        modelSiblingIds     = (int[][])     mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_MODEL_SIBLINGS);
        masterAtomId        = (int[])       mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_MASTER_RID);
        elementId           = (int[])       mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID);                   // Atom (non fkcs)
        authMolNameList     = (String[])    mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME );
        authMolNameListNR   =               mainRelation.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME );
        authResNameList     = (String[])    mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME );
        authResNameListNR   =               mainRelation.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME );
        authResIdList       = (String[])    mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID );
        authResIdListNR     =               mainRelation.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID );
        authAtomNameList    = (String[])    mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME );
        authAtomNameListNR  =               mainRelation.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME);
        pdbInsertionCodeList    = (String[])    mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE );
        pdbInsertionCodeListNR  =               mainRelation.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE);
        occupancy           = (float[])     mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY);
        bfactor             = (float[])     mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_BFACTOR);
        canHydrogenBond     =               mainRelation.getColumnBit( CAN_HYDROGEN_BOND );
        isHBDonor           =               mainRelation.getColumnBit( IS_HB_DONOR );
        isHBAccep           =               mainRelation.getColumnBit( IS_HB_ACCEP );


        if ( resId == null || molId == null || modelId == null || entryId == null
                || modelSiblingIds == null
                || masterAtomId == null
                || elementId == null
          || authMolNameList == null || authMolNameListNR == null
          || authResNameList == null || authResNameListNR == null
          || authResIdList == null
          || authAtomNameList == null || authAtomNameListNR == null
          || pdbInsertionCodeList == null || pdbInsertionCodeListNR == null
          || occupancy == null || bfactor == null
          || canHydrogenBond == null
          || isHBDonor == null
          || isHBAccep == null
          ) {
            return false;
        }
        return true;
    }


    /** Return the entry, model, mol, res, and atom ids as a string.*/
    public String toString( ArrayList orgAtomsSorted, boolean showAuthorInfo ) {
        int rid;
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<orgAtomsSorted.size();i++) {
            Object atomObject = orgAtomsSorted.get(i);
            if ( atomObject instanceof int[] ) {
                rid = ((int[])atomObject)[0];
                sb.append(toString( rid, showAuthorInfo ));
            } else {
                String msg = "Unknown atom object can't be stringed";
                sb.append(msg);
                General.showError(msg);
            }
            sb.append(General.eol);
        }
        return sb.toString();
    }

    /** Convenience method.*/
    public String toString( BitSet todo) {
        return toString( todo, false );
    }

    /** Return the entry, model, mol, res, and atom ids as a string.
     * Returns null on error.*/
    public String toString( BitSet todo, boolean showAuthorInfo ) {
        if ( todo.cardinality() == 0 ) {
            return Defs.EMPTY_STRING;
        }
        // Order the atoms on models, mols, residues, and atoms.
        // Set the order column to nulls so the map later on will only contain the atoms needed.
        mainRelation.setValueByColumn(Relation.DEFAULT_ATTRIBUTE_ORDER_ID, Defs.NULL_INT);
        Comparator atomComparator = new ComparatorAtomPerModelMolResAtom();
        if ( ! orderPerModelMolResAtom(todo, atomComparator, 0)) {
            return null;
        }
        int[] map = mainRelation.getRowOrderMap(-1); // -1 for default ordering.
        if ( map == null )  {
            General.showError("Failed to get order map");

        }
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<map.length;i++) {
            int rid = map[i];
            sb.append(toString( rid, showAuthorInfo ));
            sb.append(General.eol);
        }
        return sb.toString();
    }

    /** Convenience method.*/
    public String toString( int atomRid ) {
        return toString( atomRid, false );
    }

    /** Convenience method.*/
    public String toString( int atomRid, boolean showAuthorInfo ) {
        return toString( atomRid, true, true, true, showAuthorInfo  );
    }

    /** Convenience method.*/
    public String toString( int atomRid, boolean showMeaning,
            boolean showModel, boolean showEntry ) {
        return toString( atomRid, showMeaning, showModel, showEntry, false  );
    }

    /** Return the entry, model, mol, res, and atom ids as a string.*/
    public String toString( int atomRid, boolean showMeaning,
            boolean showModel, boolean showEntry, boolean showAuthorInfo ) {
        if ( atomRid < 0 ) {
            General.showError("Can't do toString for atom rid < 0");
            return null;
        }
        int entryNumber = gumbo.entry.number[ entryId[ atomRid ]];
        int modelNumber = gumbo.model.number[ modelId[ atomRid ]];
        int molNumber   = gumbo.mol.number[   molId[ atomRid ]];
        int resNumber   = gumbo.res.number[   resId[ atomRid ]];
        String resName  = gumbo.res.nameList[ resId[ atomRid ]];
        String name     = nameList[ atomRid ];
        //sb.append( "Atom: " + name + " Res: " + resNumber + "(" + resName + ")  Mol: " + molNumber + " Model: " + modelNumber + " Entry: " + entryNumber);
        Parameters p = new Parameters(); // for printf
        p.add( name );
        p.add( resNumber);
        p.add( resName);
        p.add( molNumber);
        String fmt = "Atom: %-4s Res: %3d(%4s) Mol: %3d";
        if ( ! showMeaning ) {
            fmt = "%-4s %3d(%4s) %3d ";
        }
        StringBuffer sb = new StringBuffer();
        sb.append( Format.sprintf(fmt,p));

        if ( showAuthorInfo ) {
            String chainId  = authMolNameList[ atomRid ];
            String resNumb  = authResIdList[   atomRid ];
                   resName  = authResNameList[ atomRid ];
                   name     = authAtomNameList[atomRid ];
            if ( Defs.isNull(chainId)) {
                chainId = Defs.NULL_STRING_DOT;
            }
            p.add( name);
            p.add( resNumb);
            p.add( resName);
            p.add( chainId);
            fmt = " [%-4s %3s(%4s) %3s]";
            sb.append( Format.sprintf(fmt,p));
        }
        if ( showModel ) {
            p.add( modelNumber);
            fmt = " Model: %3d";
            if ( ! showMeaning ) {
                fmt = " %3d";
            }
            sb.append( Format.sprintf(fmt,p));
        }

        if ( showEntry ) {
            p.add( entryNumber);
            fmt = " Entry: %2d";
            if ( ! showMeaning ) {
                fmt = " %2d";
            }
            sb.append( Format.sprintf(fmt,p));
        }
        return sb.toString();
    }

    /** Return the atom info by residue.*/
    public String toStringAtom( int rid ) {
        if ( rid < 0 ) {
            General.showError("Can't do toString for atom rid < 0");
            return null;
        }
        String name     = nameList[ rid ];
        float  x        = xList[ rid ];
        float  y        = yList[ rid ];
        float  z        = zList[ rid ];
        //sb.append( "Atom: " + name + " Res: " + resNumber + "(" + resName + ")  Mol: " + molNumber + " Model: " + modelNumber + " Entry: " + entryNumber);
        Parameters p = new Parameters(); // for printf
        p.add( name );
        p.add( x );
        p.add( y );
        p.add( z );

        return Format.sprintf( "%4s %8.3f %8.3f %8.3f ", p);
    }

    /*
     *@see Biochemistry
     */
    public boolean isInStandardResidue(int rid) {
        return Biochemistry.commonResidueNameAA_NA.containsKey( gumbo.res.nameList[resId[rid]]);
    }


    /** Returns the set of atoms in todo that are in standard residues
     *@see Biochemistry
     */
    public BitSet getStandardResidueAtoms( BitSet todo ) {
        BitSet result = new BitSet();
        result.or(todo);
        for (int rid=todo.nextSetBit(0);rid>=0;rid=todo.nextSetBit(rid+1)) {
            if ( ! isInStandardResidue(rid)) {
                result.clear(rid); // infrequent
            }
        }
        return result;
    }


    /** Add atoms for regular amino (and nucleic acids perhaps)
     */
    public boolean addAtomsByEntry( int orgEntryId, int nomEntryId ) {
//        int countRenamed  = 0;
        int countNotFound = 0;
        int MAX_ATOMS_TO_SHOW = 50;

        BitSet orgAtoms = SQLSelect.selectBitSet(mainRelation.dbms, mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(orgEntryId), false);
        BitSet nomAtoms = SQLSelect.selectBitSet(mainRelation.dbms, mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(nomEntryId), false);
        int orgAtomsCount = orgAtoms.cardinality();
        int nomAtomsCount = nomAtoms.cardinality();

        General.showOutput("Will look for atoms to add all models.");
        General.showOutput("Entry to add to   has number of atoms : " + orgAtomsCount);
        General.showOutput("Entry to add from has number of atoms : " + nomAtomsCount);

        // filter out the atoms from non standard residues.
        BitSet nomAtomsStandardResidues = getStandardResidueAtoms(nomAtoms);
        int nomAtomsCountStandardResidues = nomAtomsStandardResidues.cardinality();
        int nomAtomsCountNonStandardResidues = nomAtomsCount - nomAtomsCountStandardResidues;
        if ( nomAtomsCountNonStandardResidues > 0 ) {
            General.showWarning("Skipping number of atoms in non-standard residues: " + nomAtomsCountNonStandardResidues);
            nomAtoms.and(nomAtomsStandardResidues);
        }

        // Get nom atoms already present as determined by coordinates xyz
        BitSet nomAtomsPresent = new BitSet();
        HashMap orgAtomMap = getMapOnXYZ(orgAtoms);
        if ( orgAtomMap == null ) {
            General.showError("Failed getMapOnXYZ");
            return false;
        }
        for (int rid=nomAtoms.nextSetBit(0);rid>=0;rid=nomAtoms.nextSetBit(rid+1)) {
            Object key = getKeyXYZ( rid );
            Integer orgRidInt = (Integer) orgAtomMap.get( key );
            if ( orgRidInt == null ) {
                //General.showDebug("atom not in original entry map: " + toString(rid) + " on xyz key: " + key);
                countNotFound++;
                continue;
            }
//            int orgRid = orgRidInt.intValue();
            nomAtomsPresent.set(rid);
        }
        int nomCountAtomsPresent = nomAtomsPresent.cardinality();
        if ( nomCountAtomsPresent > 0 ) {
            General.showOutput("Skipping atoms in new entry that are already present in old as determined by coordinates: " + nomCountAtomsPresent);
            nomAtoms.andNot(nomAtomsPresent);
        }

        /** Check to see if nom atoms are in old entry but perhaps just a bit
         *off on the coordinate. Those off by the coordinate will still not be added.
         *This requires an expensive flaky lookup and that's why the pre-filtering above is
         *done.
         */
        BitSet nomAtomsPresent2 = new BitSet();
        IntArrayList orgAtomsIntArrayList = PrimitiveArray.toIntArrayList( orgAtoms );
        Comparator atomComparator = new ComparatorAuthorAtomWithoutEntry(gumbo);
        ArrayList orgAtomsSorted = getSorted(orgAtomsIntArrayList, atomComparator);
//        boolean showAuthorInfo = true;
//        General.showDebug("Sorted org atoms (by author chain id and residue number[string], and regular atom name) are: \n" +
//                toString(orgAtomsSorted, showAuthorInfo));
        int[] nomAtomObject = new int[2];
        for (int rid=nomAtoms.nextSetBit(0);rid>=0;rid=nomAtoms.nextSetBit(rid+1)) {
            nomAtomObject[0] = rid;
            int insertPoint = Collections.binarySearch(orgAtomsSorted,nomAtomObject,atomComparator);
            // Return value >=0 means the object was found.
            if ( insertPoint >= 0 ) {
//                General.showDebug( "The new atom is already present at insert point: " +
//                        insertPoint + " , probably off on some by coordinate: " + toString(rid,true));
                nomAtomsPresent2.set(rid);
            } else {
//                General.showDebug( "New atom: " + toString(rid,true));
            }
        }
        int nomCountAtomsPresent2 = nomAtomsPresent2.cardinality();
        if ( nomCountAtomsPresent2 > 0 ) {
            General.showOutput("Skipping atoms in new entry that are present already as determined by name: " + nomCountAtomsPresent2 );
            General.showOutput("Coordinates are perhaps a bit off.");
            nomAtoms.andNot(nomAtomsPresent2);
        }
        nomAtomsCount = nomAtoms.cardinality();
        if ( nomAtomsCount == 0 ) {
            General.showOutput( "No atoms to add" );
            return true;
        }
        BitSet firstCouple = (BitSet) nomAtoms.clone();
        PrimitiveArray.truncate(firstCouple, MAX_ATOMS_TO_SHOW);
        int toShow = Math.min(MAX_ATOMS_TO_SHOW,nomAtomsCount);
        General.showOutput( "First " + toShow + " of " + nomAtomsCount + " atoms to add:\n" + toString(firstCouple));
        int totalAtoms = orgAtomsCount+nomAtomsCount;
        float perAdded = Defs.NULL_FLOAT;
        if ( totalAtoms != 0 ) {
            perAdded = 100f*nomAtomsCount/totalAtoms;
        }
        General.showOutput(
            "Initial total   : " + orgAtomsCount + " atoms" + General.eol +
            " will try adding: " + nomAtomsCount + " atoms, (" + perAdded + " % added atoms over the total number of atoms."
        );

        // Add atoms but do NOT create the residues even if that means not adding atoms.
        // So let's find the residue by looking for any existing atom in both
        // with the same coordinate.
        mainRelation.reserveRows(nomAtomsCount); // prevents the index from being regenerated.
        int org_rid_dst             = 0; // needs to be zero for starting the scan
//        String nomAtomNameStable    = null;
        int ridOrgFound             = -1;
        int nomResRid               = -1;
        Integer orgRidInt           = null;
        BitSet nomAtomsRes          = null;
        for (int rid=nomAtoms.nextSetBit(0);rid>=0;rid=nomAtoms.nextSetBit(rid+1)) {
            orgRidInt = null;
            nomResRid = resId[rid];
            nomAtomsRes = SQLSelect.selectBitSet(dbms, mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(nomResRid), false);
            if ( nomAtomsRes == null ) {
                General.showError("Failed to get list of atoms for residue: " + gumbo.res.toString(nomResRid));
                General.showWarning("While looking at atom              :\n" + toString(rid) );
                return false;
            }
            if ( nomAtomsRes.nextSetBit(0) < 0 ) {
                General.showWarning("Got empty list of atoms for residue: " + gumbo.res.toString(nomResRid));
                General.showWarning("Skipping addition of this atom              :\n" + toString(rid) );
                continue;
            }
            //General.showDebug("Got new atoms in same residue:\n" + toString(nomAtomsRes));
            // Loop usually executes only ones.
            for (int rid_stable_atom = nomAtomsRes.nextSetBit(0); rid_stable_atom >= 0; rid_stable_atom = nomAtomsRes.nextSetBit(rid_stable_atom+1)) {
                Object key = getKeyXYZ( rid_stable_atom );
                orgRidInt = (Integer) orgAtomMap.get( key );
                //General.showDebug("Checking new atom for any related atom in org set:\n" + toString(rid_stable_atom));
                if ( orgRidInt != null ) {
                    break;
                }
            }
            if ( orgRidInt == null ) {
                General.showWarning("Failed to match ANY org atom with new residue:\n" + gumbo.res.toString(resId[rid]));
                General.showWarning("Skipping addition of this atom              :\n" + toString(rid) );
                continue;
            }
            ridOrgFound = orgRidInt.intValue();
            org_rid_dst = mainRelation.getNextReservedRow(org_rid_dst); // would nill the index if not cached.
            //General.showDebug("Copying from atom:\n"+ toString(ridOrgFound));
            mainRelation.copyRow(ridOrgFound, org_rid_dst);
            entryId[            org_rid_dst]   = entryId[ ridOrgFound ];
            nameList[           org_rid_dst]   = nameListNR.intern( nameList[ rid ]);
            authAtomNameList[   org_rid_dst]   = null;
            pdbInsertionCodeList[   org_rid_dst]   = pdbInsertionCodeList[ rid ];
            xList[              org_rid_dst]   = xList[rid];
            yList[              org_rid_dst]   = yList[rid];
            zList[              org_rid_dst]   = zList[rid];
            bfactor[            org_rid_dst]   = bfactor[rid];
            occupancy[          org_rid_dst]   = occupancy[rid];
            elementId[          org_rid_dst]   = elementId[rid];
            modelSiblingIds[    org_rid_dst]   = null;
            int rid_entry = entryId[rid]; // could be taken out of the loop of course.
            gumbo.entry.modelsSynced.clear(rid_entry);
        }
        mainRelation.cancelAllReservedRows(); // not all atoms might have been added.

        // Final report.
        orgAtoms = SQLSelect.selectBitSet(mainRelation.dbms, mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(orgEntryId), false);
        General.showOutput( "Added " + (orgAtoms.cardinality()-orgAtomsCount) + " atoms of the attempted " + nomAtomsCount);
        return true;
    }


    /** Correct the atom nomenclature for regular amino and nucleic acids
     */
    public boolean renameByEntry( int orgEntryId, int nomEntryId ) {

        int countRenamed  = 0;
        int countNotFound = 0;

        BitSet orgAtoms = SQLSelect.selectBitSet(mainRelation.dbms, mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(orgEntryId), false);
        BitSet nomAtoms = SQLSelect.selectBitSet(mainRelation.dbms, mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(nomEntryId), false);
        int orgAtomsCount = orgAtoms.cardinality();
        int nomAtomsCount = nomAtoms.cardinality();

        if ( orgAtomsCount != nomAtomsCount ) {
            General.showWarning("Will consider to rename number of atoms in this entry: " + orgAtomsCount);
            General.showWarning("Using number of atoms in the other entry             : " + nomAtomsCount);
            General.showWarning("For correcting the atom nomenclature; the number of atoms aren't the same so check the results");
        }

        // filter out the atoms from non standard residues.
        BitSet nomAtomsStandardResidues = getStandardResidueAtoms(nomAtoms);
    int nomAtomsCountStandardResidues = nomAtomsStandardResidues.cardinality();
        int nomAtomsCountNonStandardResidues = nomAtomsCount - nomAtomsCountStandardResidues;
        if ( nomAtomsCountNonStandardResidues > 0 ) {
            General.showWarning("Skipping atoms of non-standard residues: " + nomAtomsCountNonStandardResidues);
        }
        nomAtoms.and(nomAtomsStandardResidues);

        HashMap nomAtomMap = getMapOnXYZ(nomAtoms);
        //General.showDebug( "Map: " + Strings.toString( nomAtomMap ));
        // Instead of doing one by one intern the whole non-redundant list.
        if ( authAtomNameListNR.intern( nameListNR ) == null ) {
            General.showError("failed to take original names to author names at once");
            return false;
        }

        for (int rid=orgAtoms.nextSetBit(0);rid>=0;rid=orgAtoms.nextSetBit(rid+1)) {
            Object key = getKeyXYZ( rid );
            //General.showDebug("Got key: " + key);
            Integer nomRidInt = (Integer) nomAtomMap.get( key );
            if ( nomRidInt == null ) {
//                General.showDebug("atom not in nomenclature map: " + toString(rid) + " on xyz key: " + key);
                countNotFound++;
                continue;
            }
            if ( ! nameList[rid].equals( nameList[ nomRidInt.intValue() ] )) {
                // Since the 'new' names are already in the nameList so no need to update
                General.showDebug("Renaming atom: " +
                    toString(rid) + " to: " + nameList[ nomRidInt.intValue() ]);
                authAtomNameList[rid] = nameList[rid];
                nameList[        rid] = nameList[ nomRidInt.intValue() ];
                countRenamed++;
            }
        }
        float perFound   = 100f * (orgAtomsCount-countNotFound) / (float) orgAtomsCount;
        float perRenamed = 100f * countRenamed                  / (float) orgAtomsCount;
        General.showOutput(
            "From total : " + orgAtomsCount + " atoms" + General.eol +
            "found      : " + (orgAtomsCount-countNotFound) + " atoms, (" + perFound   + " %)" + General.eol +
            "renamed    : " + countRenamed                  + " atoms, (" + perRenamed + " %)" + General.eol
        );
        return true;
    }

    /** Does a simple search by index and then by scan inside the residue.
     * Return int null in case for error or -1 for not found.
     */
    public int getRidByAtomNameAndResRid(String atomName, int resRid) {
        IndexHashedIntToMany indexAtomOnRes = (IndexHashedIntToMany) mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_HASHED);
        IntArrayList rAtoms = indexAtomOnRes.getRidList(resRid);
        if ( rAtoms == null ) {
            General.showError( "Failed to get list of atoms for residue: " +
                    gumbo.res.toString(resRid));
            return -1;
        }
        // now scan
        int rAtomsCount = rAtoms.size();
        for (int a=0;a<rAtomsCount;a++) {
            if ( atomName.equals( nameList[rAtoms.getQuick(a)])) {
                return rAtoms.getQuick(a);
            }
        }
        return -1; // Not found.
    }

    public float getMass(int i) {
        int elId = elementId[i];
        if ( Defs.isNull(elId)) {
            General.showWarning("Ignoring mass for unknown element of atom:"+toString(i));
            return 0f;
        }
        if ( elId < 0 || Chemistry.ELEMENT_MASSES.length <= elId ) {
            General.showWarning("Ignoring mass for unknown elementid of atom:"+toString(i));
            return 0f;
        }

        return Chemistry.ELEMENT_MASSES[elId];
    }

    /**
     * This method calculates and shows dihedrals for all selected atoms.
     * @return True for success
     */
    public boolean calcDihe(String summaryFileName) {
//        General.showDebug("Starting calcDihedral");
        String relName = "DihedralListByRes";
        if ( dbms.containsRelation(relName)) {
            dbms.removeRelation(dbms.getRelation(relName));
        }

        DihedralListByRes tT = null;
        try {
            tT = new DihedralListByRes(relName,dbms);
        } catch (Exception e) {
            General.showThrowable(e);
            return false;
        }

//        int maxRes = gumbo.res.mainRelation.sizeMax;

        int entryRID = gumbo.entry.getEntryId();
        if ( entryRID < 0 ) {
            General.showError("Failed to get single entry id");
            return false;
        }
        String entryName = gumbo.entry.nameList[entryRID];

        BitSet atomsInMasterTodo = gumbo.entry.getAtomsInMasterModel();
        if ( atomsInMasterTodo == null ) {
            General.showError("Failed to get atoms in master model.");
            return false;
        }
        atomsInMasterTodo.and(selected);
        BitSet resInMasterTodo = getResidueList( atomsInMasterTodo );
//        General.showDebug("Looking for dihedrals in " + resInMasterTodo.cardinality() + " residue(s) in master model.");
        BitSet modelsInEntry = gumbo.entry.getModelsInEntry(entryRID);
        if ( modelsInEntry == null ) {
            General.showError("Failed to do calcDihedral because failed to get the models in this entry for rid: " + entryRID);
            return false;
        }
        int modelCount = modelsInEntry.cardinality();
//        /** Check if any bonds are present */
//        if ( gumbo.bond.mainRelation.used.nextSetBit(0) < 0 ) {
//            General.showError("Please calculate bonds (for disulfides) before using routine to calculate distances.");
//            return false;
//        }
//        IndexHashedIntToMany indexBondOnAtomA = (IndexHashedIntToMany) gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, Index.INDEX_TYPE_HASHED);
//        IndexHashedIntToMany indexBondOnAtomB = (IndexHashedIntToMany) gumbo.bond.mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, Index.INDEX_TYPE_HASHED);

//        int numbRes = resInMasterTodo.cardinality();
        IndexHashedIntToMany indexAtomOnRes = (IndexHashedIntToMany) mainRelation.getIndex( Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_HASHED);
        // first calculate some info for the residues
        // Inefficient to have the outer loop over models but that's the output requirement and easiest
        //  to program.
        for (int m=1;m<=modelCount;m++) {                       // m is the model number
            for (int ri=resInMasterTodo.nextSetBit(0);ri>=0;ri=resInMasterTodo.nextSetBit(ri+1)) {
//                General.showDebug("Looking at residue: " + gumbo.res.toString(ri));
                IntArrayList rAtoms = indexAtomOnRes.getRidList(ri);
                if ( rAtoms == null ) {
                    General.showError("failed to get list of atoms for residue: " + gumbo.res.toString(ri));
                    return false;
                }
                if ( rAtoms.size() == 0 ) {
                    General.showError("list of atoms todo is empty for residue: " + gumbo.res.toString(ri));
                    return false;
                }
                int atom_rid = rAtoms.getQuick(0);
                int res_rid = gumbo.atom.resId[ atom_rid ];
                int mol_rid = gumbo.atom.molId[ atom_rid ];
                int resNumber   = gumbo.res.number[    res_rid];
                String resName  = gumbo.res.nameList[  res_rid];
                int molNumber   = gumbo.mol.number[    mol_rid];

                boolean isAA = Biochemistry.commonResidueNameAA.containsKey(resName);
                boolean isNA = Biochemistry.commonResidueNameNA.containsKey(resName);
                AtomLib atomLib = dbms.ui.wattosLib.atomLib;
                String[][] DIHEDRAL_LIST = atomLib.DIHEDRAL_LIST;
                for (int d=0;d<DIHEDRAL_LIST.length;d++) {
//                    General.showDebug("For angle ["+Strings.toString(DIHEDRAL_LIST[d]));
                    String polType      = DIHEDRAL_LIST[d][AtomLib.IDX_POL_TYPE];
                    String compId       = DIHEDRAL_LIST[d][AtomLib.IDX_COMP_ID];
                    String angleName    = DIHEDRAL_LIST[d][AtomLib.IDX_ANGLE_NAME];
                    boolean specialNAmatch = false;
                    if ( polType.equals("na") &&
                                 (compId.equals("py") && Biochemistry.ResiduePyrimidine.contains(resName)) ||
                                 (compId.equals("pu") && Biochemistry.ResiduePurine.contains(resName))) {
                        specialNAmatch = true;
                    }
//                    General.showDebug("specialNAmatch: " + specialNAmatch);
                    if (!   ( resName.equals(compId) ||
                            ( isAA && polType.equals("aa") && compId.equals(".")) ||
                            ( isNA && polType.equals("na") && compId.equals(".")) ||
                            specialNAmatch )) {
//                        General.showDebug("Skip those that do not apply: " + Strings.toString(DIHEDRAL_LIST[d]));
                        continue;
                    }


                    IntArrayList iAtoms = new IntArrayList();
                    // find the atoms
                    boolean atomsFound = true;
//                    int molNumber2 = molNumber;
//                    int resNumber2 = resNumber;
//                    String resName2 = resName;

                    for (int a=0;a<4;a++) {
//                        General.showDebug("Working on atom: " + a);
                        String atomName  = DIHEDRAL_LIST[d][AtomLib.IDX_ATOM_ID_1    +a];
                        String compSeqID = DIHEDRAL_LIST[d][AtomLib.IDX_COMP_SEQ_ID_1+a];
                        int res_rid_a = res_rid;
                        if ( ! Defs.isNullString(compSeqID)) {
                            char ch = compSeqID.charAt(0);
//                            General.showDebug("Looking at seq char: " + ch);
                            switch ( ch ) {
                                case '-': {
                                    res_rid_a = gumbo.res.getNeighbour(res_rid,-1);
                                    break;
                                }
                                case '+': {
                                    res_rid_a = gumbo.res.getNeighbour(res_rid,+1);
                                    break;
                                }
                                default: {
                                    General.showError("For angle ["+Strings.toString(DIHEDRAL_LIST[d]));
                                    General.showError("Residue not found: " + compSeqID);
                                    General.showError("Failed to find residue info for seq char: " + ch);
                                    return false;
                                }
                            }
                        }
                        if ( res_rid_a < 0 ) {
//                            General.showDebug("Failed to find neighbouring residue: " + compSeqID);
                            atomsFound = false;
                            break;
                        }
                        int a_idx = getRidByAtomNameAndResRid(atomName, res_rid_a);
                        if ( a_idx < 0 ) {
//                            General.showDebug("For angle ["+Strings.toString(DIHEDRAL_LIST[d]));
//                            General.showDebug("Atom ["+atomName+"] not found for residue: " + gumbo.res.toString(res_rid));
                            atomsFound = false;
                            break;
                        }
                        iAtoms.add(a_idx);
                    }
                    if ( ! atomsFound ) {
                        continue;
                    }

//                    General.showDebug("found first model atoms: "+toString(PrimitiveArray.toBitSet(iAtoms)));
                    IntArrayList sAtoms = getAtomsInModel(m, iAtoms);
                    if ( sAtoms == null ) {
                        General.showCodeBug("Failed to get atom idx for dihedral: " + Strings.toString(DIHEDRAL_LIST[d]));
                        return false;
                    }
                    float value = (float) ( Geometry.CF * gumbo.atom.calcDihedral(
                            sAtoms.getQuick(0),
                            sAtoms.getQuick(1),
                            sAtoms.getQuick(2),
                            sAtoms.getQuick(3)));

                    int row = tT.getNewRowId();
                    tT.setValue(row, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,           row);
//                    tT.setValue(row, "_DebugRows",          row);
                    tT.setValue(row, "_Entry_id",           entryName.substring(0,4));
                    tT.setValue(row, "_Model_id",           m);
                    tT.setValue(row, "_Seg_id",             molNumber);
                    tT.setValue(row, "_Comp_seq_ID",        resNumber);
                    tT.setValue(row, "_Comp_ID",            resName);
//                    tT.setValue(row, "_Seg_id2",            molNumber2);
//                    tT.setValue(row, "_Comp_seq_ID2",       resNumber2);
//                    tT.setValue(row, "_Comp_ID2",           resName2);
                    tT.setValue(row, "_Angle_name",         angleName);
                    tT.setValue(row, "_Angle_value",        value);
                }
            }
        }
        General.showOutput("Found number of dihedrals: " + tT.sizeRows );
        if ( tT.sizeRows == 0 ) {
            General.showOutput("Skipping write of empty list");
            return true;
        }
//        tT.sortByColumns(new String[] {"_Angle_value" });
        tT.convertDataTypeColumn("_Angle_value",Relation.DATA_TYPE_STRING,"%.3f");

        StarNode topNode = new StarNode();
        DataBlock db = new DataBlock();
        db.title = entryName;
        topNode.datanodes.add(db);
        SaveFrame sf = new SaveFrame();
        sf.title = "common_dihedral";
        db.datanodes.add(sf);
        sf.datanodes.add(tT);
        if ( ! topNode.toSTAR(summaryFileName) ) {
            General.showError("Failed to write to file: " + summaryFileName );
            return false;
        }
        return true;
    }


    /**
     * Modify the atom names from 1HD2 to HD21 and vica versa. See:
     * http://www.wwpdb.org/documentation/remediation_overview.pdf
     * """Atom names uniformly begin with their
element symbol. In PDB format (Appendix B), heavy atom names follow the traditional
PDB justification rules. Any 4-character names for atoms with 1-character element
symbols have been compressed. Hydrogen atoms names all begin with H and are not
subject to the justification rule. Therefore the PDB element column or mmCIF
type_symbol data item should be used to determine the element type, rather than using
the atom name."""
     * @param usePostFixedOrdinalsAtomName
     * @return
     */
    public boolean swapPostFixedOrdinalsAtomName(boolean usePostFixedOrdinalsAtomName) {
        //
        String name = null;
        StringBuffer sb = new StringBuffer();
        int atomRID=selected.nextSetBit(0);
        for (;atomRID>=0;atomRID=selected.nextSetBit(atomRID+1)) {
            name = nameList[atomRID];
            sb.setLength(name.length());
            if ( usePostFixedOrdinalsAtomName ) {
                if ( Character.isDigit( name.charAt(0))) {
                    sb.setCharAt(sb.length()-1, name.charAt(0));
                    sb.insert(0, name, 1, name.length());
                    nameList[atomRID] = nameListNR.intern( sb.toString() );
                }
            } else if ( Character.isDigit( name.charAt(name.length()-1))) {
                sb.setCharAt(0, name.charAt(name.length()-1));
                sb.insert(   1, name, 0, name.length()-1);
                nameList[atomRID] = nameListNR.intern( sb.toString() );
            }
        }
        return true;
    }

    /** Returns the number of atoms that are metals (and COULD PERHAPS be ions)
     * Very inefficient routine but...*/
    public int getMetalIons(BitSet atomSet) {
        int count = 0;
        for (int atomRid=atomSet.nextSetBit(0);atomRid>=0;atomRid=atomSet.nextSetBit(atomRid+1)) {
            if ( Chemistry.METAL_ION_ELEMENT_ID_Set.get(elementId[atomRid])) {
                count++;
            }
        }
        return count;
    }

    /** Input atoms and residues should only be in single model */
    public int getThiolState(BitSet atomSet, BitSet resInMaster ) {
        boolean hasFree         = false;
        boolean hasDisulfide    = false;
        boolean hasOtherBound   = false;

        calcBond(resInMaster, 0.1f);

        BitSet resRidCysSet = SQLSelect.selectBitSet(dbms, gumbo.res.mainRelation, Relation.DEFAULT_ATTRIBUTE_NAME,
                SQLSelect.OPERATION_TYPE_EQUALS, "CYS", false);
        resRidCysSet.and(resInMaster);
//        General.showDebug("Found number of cys in model: " + resRidCysSet.cardinality());
        if ( resRidCysSet.cardinality() == 0 ) {
            return Biochemistry.THIOL_STATE_NOT_PRESENT;
        }

        for (int resRidCys=resRidCysSet.nextSetBit(0);resRidCys>=0;resRidCys=resRidCysSet.nextSetBit(resRidCys+1)) {
            BitSet atomRidSGSet = SQLSelect.selectCombinationBitSet(dbms, mainRelation,
                    Relation.DEFAULT_ATTRIBUTE_NAME,
                    SQLSelect.OPERATION_TYPE_EQUALS, "SG",
                    Gumbo.DEFAULT_ATTRIBUTE_SET_RES[  RELATION_ID_COLUMN_NAME],
                    SQLSelect.OPERATION_TYPE_EQUALS, new Integer(resRidCys),
                    SQLSelect.OPERATOR_AND, false);
            atomRidSGSet.and(atomSet);
//            General.showDebug("Found number of SG in Cys residue (should be 1 or less): " + atomRidSGSet.cardinality());
            int atomRidSG = atomRidSGSet.nextSetBit(0);
//            General.showDebug("Looking at SG : " + toString(atomRidSG));
            if ( atomRidSG<0 ) {
                General.showDebug("No SG atom in Cys residue found; assuming the Cys thiol state of the entry is unknown");
                return Biochemistry.THIOL_STATE_UNKNOWN;
            }
            BitSet bondRidSetFromS = SQLSelect.selectBitSet(dbms, gumbo.bond.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID,
                    SQLSelect.OPERATION_TYPE_EQUALS, new Integer(atomRidSG), false);
            BitSet bondRidSetToS   = SQLSelect.selectBitSet(dbms, gumbo.bond.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID,
                    SQLSelect.OPERATION_TYPE_EQUALS, new Integer(atomRidSG), false);
//            General.showDebug("Found number of atom bound FROM this SG in residue rid: " + resRidCys +
//                    " is: " +bondRidSetFromS.cardinality());
//            General.showDebug("Found number of atom bound TO   this SG in residue rid: " + resRidCys +
//                    " is: " +bondRidSetToS.cardinality());
            int i=0;
            BitSet[] bondRidSetList = new BitSet[] { bondRidSetFromS, bondRidSetToS, null };
            BitSet bondRidSet=bondRidSetFromS;
            boolean thisResHasDisulfide = false;
            boolean thisResHasOtherBound = false;
            for ( ;i<2;bondRidSet=bondRidSetList[i]) {
                for (int bondRid=bondRidSet.nextSetBit(0);bondRid>=0;bondRid=bondRidSet.nextSetBit(bondRid+1)) {
                    int[] otherAtomList = gumbo.bond.atom_B_Id;
                    if ( i==1 ) {
                        otherAtomList = gumbo.bond.atom_A_Id;
                    }
                    int otherAtomRid = otherAtomList[bondRid];
                    int otherAtomResRid = resId[otherAtomRid];
                    String otherAtomName = nameList[otherAtomRid];
                    String otherAtomResName = gumbo.res.nameList[otherAtomResRid];
//                    General.showDebug("Looking at bonded atom: " + toString(otherAtomRid));
                    if ( otherAtomResRid == resId[ atomRidSG ] ) {
                        continue;
                    }
                    if ( otherAtomResName.equals("CYS") && otherAtomName.equals("SG")) {
//                        General.showDebug("Found disulfide thiol on: " + gumbo.res.toString(resRidCys));
                        hasDisulfide = true;
                        thisResHasDisulfide = true;
                    } else {
//                        General.showDebug("Found other bound thiol on: " + gumbo.res.toString(resRidCys));
                        hasOtherBound = true;
                        thisResHasOtherBound = true;
                    }
                }
                i++;
            }
            if (!( thisResHasDisulfide || thisResHasOtherBound )) {
                hasFree = true;
//                General.showDebug("Found free thiol on          : " + gumbo.res.toString(resRidCys));
            } else if ( thisResHasDisulfide ) {
//                General.showDebug("Found disulfide thiol on     : " + gumbo.res.toString(resRidCys));
            } else {
//                General.showDebug("Found other bound thiol on   : " + gumbo.res.toString(resRidCys));
            }
        }


//        General.showDebug("hasFree          : " + hasFree);
//        General.showDebug("hasDisulfide     : " + hasDisulfide);
//        General.showDebug("hasOtherBound    : " + hasOtherBound);

        if ( hasFree ) {
            if ( hasDisulfide ) {
                if ( hasOtherBound ) {
                    return Biochemistry.THIOL_STATE_FREE_DISULFIDE_AND_OTHER_BOUND;
                } else {
                    return Biochemistry.THIOL_STATE_FREE_AND_DISULFIDE_BOUND;
                }
            } else {
                if ( hasOtherBound ) {
                    return Biochemistry.THIOL_STATE_FREE_AND_OTHER_BOUND;
                } else {
//                    General.showDebug("Found THIOL_STATE_ALL_FREE");
                    return Biochemistry.THIOL_STATE_ALL_FREE;

                }
            }
        } else {
            if ( hasDisulfide ) {
                if ( hasOtherBound ) {
                    return Biochemistry.THIOL_STATE_DISULFIDE_AND_OTHER_BOUND;
                } else {
                    return Biochemistry.THIOL_STATE_ALL_DISULFIDE_BOUND;
                }
            } else {
                if ( hasOtherBound ) {
                    return Biochemistry.THIOL_STATE_ALL_OTHER_BOUND;
                } else {
                    General.showCodeBug("There are Cys but they are not any of the 3 states; an error for sure");
                    return -1;

                }
            }
        }
    }

    public void swapNames(int atomRid1, int atomRid2) {
        String tmpStr = nameList[ atomRid1 ];
        nameList[ atomRid1 ] = nameList[ atomRid2 ];
        nameList[ atomRid2 ] = tmpStr;
    }

    /** Takes the first numerical char at position 2 and swaps it
     * from 1 to 2 or from 2 to 1.
     * @param atomRidExtra
     */
    public boolean swapNameForStereo(int atomRidExtra) {
        String tmpStr = nameList[ atomRidExtra ];
        char c = tmpStr.charAt(2);
        StringBuffer sb = new StringBuffer(tmpStr);
        if ( c == '1' ) {
            sb.setCharAt(2, '2');
        } else if ( c == '2' ) {
            sb.setCharAt(2, '1');
        } else {
            General.showCodeBug("Expected a 1 or 2 as the third char in atom name: " + tmpStr);
            return false;
        }
        nameList[ atomRidExtra ] = nameListNR.intern( sb.toString());
        return true;
    }

    /** Convenience method */
    public int add(String name, double[] coor, int resId) {
        int elId = Chemistry.getElementId(name);
        float[] coorf = new float[] {
                (float) coor[0],
                (float) coor[1],
                (float) coor[2]};
        int atomRid = add(name,
                true, coorf, Defs.NULL_FLOAT,
                elId, 1.0f, Defs.NULL_FLOAT,
                null, null, null, null,
                resId, null);
//        General.showDebug("Added: " + toString(atomRid));
        return atomRid;
    }

    /** Convenience method */
    public boolean calcBond() {
        BitSet resInMaster = gumbo.entry.getResInMasterModel();
        if ( resInMaster == null ) {
            General.showError("Failed to get the master atoms");
            return false;
        }
        return calcBond(resInMaster, Defs.NULL_FLOAT);
    }

//    public boolean isTerminalAtomLikeNterminalH(int atomRid) {
//        General.showDebug("Determining terminalness of atom: "+toString(atomRid));
//        if ( gumbo.res.isTerminal(resId[atomRid]) ) {
//            if ( Biochemistry.TERMINAL_ATOM_NAME_MAP.containsKey( nameList[atomRid] )) {
//                return true;
//            }
//        }
//        return false;
//    }

    /** Order is lost */
    public BitSet getMasterRidSet(BitSet know) {
        BitSet result = new BitSet();
        for (int r=know.nextSetBit(0);r>=0;r=know.nextSetBit(r+1)) {
            result.set(masterAtomId[r]);
        }
        return result;
    }
    /** Starting at 1 for model 1
     * This method is just a reminder for how to do it; inlined usually.
     * */
    public int getModelIdForAtomRid(int atomRid) {
        return gumbo.model.number[ modelId[atomRid] ];
    }

    /** Starting at 1 for model 1
     * This method is just a reminder for how to do it; inlined usually.
     * returns -1 for error.
     * */
    public int getAtomRidFromMasterRidAndModelNumber(int atomRidMaster,
            int modelNumber) {
        int[] list = modelSiblingIds[atomRidMaster];
        if ( list == null ) {
            General.showCodeBug("failed to get modelSiblingIds for atom:");
            General.showCodeBug(toString(atomRidMaster));
            return -1;
        }
        return list[modelNumber-1];
    }

    /** Remove selected atoms and re-sync over models */
    public boolean removeAtoms() {
        BitSet toBeRemoved = (BitSet) selected.clone();
        General.showOutput("Removing number of atoms: " + toBeRemoved.cardinality());

        if ( ! mainRelation.removeRowsCascading(toBeRemoved,false)) {
            General.showCodeBug("Failed to removeRowsCascading at removeAtoms");
            return false;
        }
        if ( ! gumbo.entry.syncModels(gumbo.entry.getEntryId())) {
            General.showCodeBug("Failed to syncModels after removeAtoms");
            return false;
        }
        return true;
    }

    /** Selects atoms; algorithm can be speeded up much by only checking 1 model. */
    public boolean selectAtomsByNameRegExp(String nameRegExp) {
        selected.clear();
        for (int rid=used.nextSetBit(0);rid>=0;rid=used.nextSetBit(rid+1)) {
            if ( nameList[rid].matches(nameRegExp) ) {
                selected.set(rid);
            }
        }

        General.showOutput("Selected number of atoms: " + selected.cardinality());
        return true;
    }
}
