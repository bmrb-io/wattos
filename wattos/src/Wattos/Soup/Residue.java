/*
 * Residue.java
 *
 * Created on November 8, 2002, 4:41 PM
 */

package Wattos.Soup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexSortedString;
import Wattos.Utils.General;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringArrayList;
import Wattos.Utils.StringSet;
import Wattos.Utils.Strings;
import Wattos.Utils.Wiskunde.Geometry;
import Wattos.Utils.Wiskunde.Geometry3D;
import cern.colt.list.IntArrayList;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 * Residue contains one or more atoms.
 * <P>
 * Residue names are unique across the universe; Wattos uses the 1 and 3 letter
 * PDB abbreviations. Ignoring difference between OGUA and RGUA as WHAT IF
 * distinguishes for instance. Those differences can be derived from the atoms
 * set in the residues. Other differences like HIS+ and HIS or CYS and CYSS will
 * not be noted in the residue name that is kept as a key.
 * <P>
 * The author of this software will probably not win the Nobel price for solving
 * the residue numbering problem but will attempt to deal with it. The physical
 * row id is the primary key into the residues and gives an id that is unique
 * over all residues in the soup (entries/models/molecules).
 * <P>
 * Within one molecule there is the unique number that starts at 1 and is most
 * like NMR-STAR, mmCIF, and the EBI residue numbering. Then there are several
 * other numbering schemes that will be maintained for convenience of the users
 * basically to maintain the information like WHAT IF does with the
 * 'old/original residue number' concept.
 * <P>
 * Mappings<BR>
 * 
 * <PRE>
 * 
 * Wattos NMRSTAR 3.0 number _Entity_comp_index.Num pdb_number . id
 * _Entity_poly_seq.Comp_index_num (for polymers only)
 * 
 * </PRE>
 * 
 * <PRE>
 * 
 * Wattos PDB formatted files number created on the fly pdb_number pdb res
 * number id pdb res number
 * 
 * </PRE>
 * 
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Residue extends GumboItem implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    /** Convenience variables */
    public int[] molId;

    public int[] modelId;

    public int[] entryId;

    public String[] authResNameList;

    public StringSet authResNameListNR;

    public String[] authResIdList;

    public StringSet authResIdListNR;

    public Residue(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        // General.showDebug("back in Residue constructor");
    }

    public Residue(DBMS dbms, String relationSetName,
            RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
    }

    public boolean init(DBMS dbms) {
        super.init(dbms);

        name = Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RELATION_ID_SET_NAME];

        // MAIN RELATION in addition to the ones in gumbo item.
        DEFAULT_ATTRIBUTES_TYPES.put(
                Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_COLUMN_NAME],
                new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put(
                Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME],
                new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put(
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME],
                new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME,
                new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID,
                new Integer(DATA_TYPE_STRINGNR));

        DEFAULT_ATTRIBUTES_ORDER
                .add(Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_COLUMN_NAME]);
        DEFAULT_ATTRIBUTES_ORDER
                .add(Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME]);
        DEFAULT_ATTRIBUTES_ORDER
                .add(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME]);
        DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME);
        DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID);

        DEFAULT_ATTRIBUTE_FKCS_FROM_TO
                .add(new String[] {
                        Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_COLUMN_NAME],
                        Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_MAIN_RELATION_NAME] });
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO
                .add(new String[] {
                        Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME],
                        Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_MAIN_RELATION_NAME] });
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO
                .add(new String[] {
                        Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME],
                        Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME] });

        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RELATION_ID_MAIN_RELATION_NAME];
        try {
            relation = new Relation(relationName, dbms, this);
        } catch (Exception e) {
            General.showThrowable(e);
            return false;
        }

        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms,
                DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);
        relation.insertColumnSet(0, DEFAULT_ATTRIBUTES_TYPES,
                DEFAULT_ATTRIBUTES_ORDER, DEFAULT_ATTRIBUTE_VALUES,
                DEFAULT_ATTRIBUTE_FKCS);
        addRelation(relation);
        mainRelation = relation;

        // OTHER RELATIONS HERE

        return true;
    }

    /**
     * Adds a new res in the array, filling in all the required properties as
     * available from the parent. Make sure the number you insert is defined in
     * the same way Wattos does it: 1 through N within each molecule; no gaps!
     * Returns -1 for failure.
     */
    public int add(String name, int number, String auth_number,
            String auth_name, int parentId) {

        int result = super.add(name);
        if (result < 0) {
            General
                    .showCodeBug("Failed to get a new row id for a residue with name: "
                            + name);
            return -1;
        }
        authResNameList[result] = authResNameListNR.intern(auth_name);
        authResIdList[result] = authResIdListNR.intern(auth_number);
        this.number[result] = number;
        molId[result] = parentId;
        modelId[result] = gumbo.mol.modelId[parentId];
        entryId[result] = gumbo.mol.entryId[parentId];
        return result;
    }

    /**
     * Get a alphabetically sorted list of unique residue names in one model.
     * This is usefull to list unknown residue names.
     */
    public String[] getDistinctNamesInModel(int modelId) {

//        General.showDebug("Looking for residues in model with rid: " + modelId);
        /**
         * Something like the sql: SELECT DISTINCT name FROM RESIDUE r WHERE
         * r.entry_id = entryId
         */
        BitSet resultSelection = SQLSelect
                .selectBitSet(dbms,
                        mainRelation, // relation
                        Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME], // selection
                                                                                                // column
                        SQLSelect.OPERATION_TYPE_EQUALS, // selection
                                                            // operation
                        new Integer(modelId), false); // selection value
        if (resultSelection == null) {
            General
                    .showWarning("Failed to getDistinctNamesInEntry by SQLSelect method selectDistinct.");
            return null;
        }
        String[] result = Strings.getDistinctSorted(nameList, resultSelection);
        return result;
    }

    /**
     * All residues in bitset given are renumbered according to the order they
     * are numbered or to the physical order if no numbering is present.
     * <P>
     * Input: Set R the residues to do the residue renumbering on.<BR>
     * The algorithm goes like this:<BR>
     * Find the unique molecules for residues in R -> set M<BR>
     * For each model Mi:<BR>
     * Find residues in Mi AND R -> Set R2<BR>
     * Renumber residues in R2 using preordering if present.<BR>
     */
    public boolean renumberRows(String columnLabel, BitSet toDo, int startNumber) {

        int mol;
        String columnLabelMolId = Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME];

        // Find the unique molecules for the given residues to do.
        // Actually finding residues that have different molecules as parent
        BitSet selResSub = SQLSelect.getDistinct(dbms, mainRelation,
                columnLabelMolId, toDo);
        if (selResSub == null) {
            General
                    .showCodeBug("Failed to get a distince set of molecules for the given residues: "
                            + PrimitiveArray.toString(toDo));
            return false;
        }
        // For each molecule (through the resid)
        for (int res = selResSub.nextSetBit(0); res >= 0; res = selResSub
                .nextSetBit(res + 1)) {
            mol = molId[res];
            // Get the residues for this molecule that are also in toDo.
            BitSet selResSub2 = SQLSelect.selectBitSet(dbms, mainRelation,
                    columnLabelMolId, SQLSelect.OPERATION_TYPE_EQUALS,
                    new Integer(mol), false);
            if (selResSub2 == null) {
                General
                        .showCodeBug("Failed to get a residue set in molecule with rid: "
                                + mol);
                return false;
            }
            selResSub2.and(toDo);
            // General.showDebug("In mol with rid: " + mol + " found residues to
            // renumber: " + selResSub2.cardinality());
            // Renumber the resiudes in R2 starting at 1.
            mainRelation.renumberRows(Relation.DEFAULT_ATTRIBUTE_NUMBER,
                    selResSub2, 1);
        }
        // General.showDebug("Renumbered residues at this point");
        return true;
    }

    /**     */
    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();
        molId = (int[]) mainRelation
                .getColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME]);
        modelId = (int[]) mainRelation
                .getColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME]);
        entryId = (int[]) mainRelation
                .getColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME]);
        authResNameList = (String[]) mainRelation
                .getColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME);
        authResNameListNR = mainRelation
                .getColumnStringSet(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME);
        authResIdList = (String[]) mainRelation
                .getColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID);
        authResIdListNR = mainRelation
                .getColumnStringSet(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID);
        if (molId == null || modelId == null || entryId == null
                || authResNameList == null || authResNameListNR == null
                || authResIdList == null) {
            return false;
        }
        return true;
    }

    /**
     * Return the entry, model, mol, res, and atom ids as a string.
     */
    public String toString(int resRid) {
        if (resRid < 0) {
            General.showError("Can't do toString for residue rid < 0");
            return null;
        }
        int entryNumber = gumbo.entry.number[entryId[resRid]];
        int modelNumber = gumbo.model.number[modelId[resRid]];
        int molNumber = gumbo.mol.number[molId[resRid]];
        int resNumber = number[resRid];
        String resName = nameList[resRid];
        // sb.append( "Atom: " + name + " Res: " + resNumber + "(" + resName +
        // ") Mol: " + molNumber + " Model: " + modelNumber + " Entry: " +
        // entryNumber);
        Parameters p = new Parameters(); // for printf
        p.add(resNumber);
        p.add(resName);
        p.add(molNumber);
        p.add(modelNumber);
        p.add(entryNumber);
        return Format.sprintf(
                "Residue: %3d(%4s) Mol: %3d Model: %3d Entry: %2d", p);
    }

    /**
     * Returns nucleic acid residues
     */
    public BitSet getNAResidues(BitSet todo) {
        BitSet result = new BitSet();
        for (int i = todo.nextSetBit(0); i >= 0; i = todo.nextSetBit(i + 1)) {
            if (Biochemistry.commonResidueNameNA.containsKey(nameList[i])) {
                result.set(i);
            } else {
                // General.showDebug("Given residue isn't a NA: [" + nameList[i]
                // + "]");
            }
        }
        return result;
    }

    /**
     * Returns the list of atoms in given residue
     */
    public BitSet getAtoms(int rid) {
        return SQLSelect.selectBitSet(dbms, gumbo.atom.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(rid), false);
    }

    /**
     * Returns the list of atoms in given residue
     */
    public BitSet getAtoms(BitSet ridSet) {
        return SQLSelect.selectBitSet(dbms, gumbo.atom.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, ridSet, false);
    }
    
    /**
     * Convenience method. Returns NULL in case of none found.
     */
    public int getAtomRid (BitSet atomRidSetInRes, String atomName) {
        for (int i=atomRidSetInRes.nextSetBit(0);i>=0;i=atomRidSetInRes.nextSetBit(i+1)) {
            if ( gumbo.atom.nameList[i].equals(atomName) ) {
                return i;
            }
        }
        return Defs.NULL_INT;
    }

    /**
     * Calculate the angle between the normals of the two base pairs returning a
     * number between 0 and 90 degrees (or pi/2). Few checks are done for this
     * purpose. Result will be in radians (0-pi/2).
     */
    public float calcGAngleBasePair(int rid_i, int rid_j) {
        // the normals
        double[][] normal = new double[2][];
        int rid;
        int rid_n1;
        int rid_n3;
        int rid_c5;
        IndexSortedString indexName = (IndexSortedString) gumbo.atom.mainRelation
                .getIndex(Relation.DEFAULT_ATTRIBUTE_NAME,
                        Index.INDEX_TYPE_SORTED);

        BitSet atomsNameN1 = (BitSet) indexName.getRidList("N1",
                Index.LIST_TYPE_BITSET, null);
        BitSet atomsNameN3 = (BitSet) indexName.getRidList("N3",
                Index.LIST_TYPE_BITSET, null);
        BitSet atomsNameC5 = (BitSet) indexName.getRidList("C5",
                Index.LIST_TYPE_BITSET, null);
        if ((atomsNameN1 == null) || (atomsNameN3 == null)
                || (atomsNameC5 == null)) {
            General
                    .showError("Failed to get n1, n3, and c5 atoms in calcGAngleBasePair(1) for all residues.");
            return Defs.NULL_FLOAT;
        }
        // General.showDebug( "Found number of N1 atoms in residue: " +
        // atomsNameN1.cardinality());
        // General.showDebug( "N1 atoms are:\n" +
        // gumbo.atom.toString(atomsNameN1));
        BitSet tmp = new BitSet();

        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                rid = rid_i;
            } else {
                rid = rid_j;
            }
            // find the atoms N1, N3, and C5 in the six-membered (sub-)ring
            BitSet atomsResidues = getAtoms(rid);
            if (atomsResidues == null) {
                General.showError("Failed to get atoms in this residue:\n"
                        + toString(rid));
                return Defs.NULL_FLOAT;
            }
            // General.showDebug( "Found number of atoms in residue: " +
            // atomsResidues.cardinality());
            // General.showDebug( "Atoms are: " +
            // gumbo.atom.toString(atomsResidues));
            tmp.clear();
            tmp.or(atomsResidues);
            tmp.and(atomsNameN1);
            rid_n1 = tmp.nextSetBit(0);
            tmp.clear();
            tmp.or(atomsResidues);
            tmp.and(atomsNameN3);
            rid_n3 = tmp.nextSetBit(0);
            tmp.clear();
            tmp.or(atomsResidues);
            tmp.and(atomsNameC5);
            rid_c5 = tmp.nextSetBit(0);
            if ((rid_n1 < 0) || (rid_n3 < 0) || (rid_c5 < 0)) {
                General
                        .showError("Failed to get n1, n3, and c5 atoms in calcGAngleBasePair(2) for residue: "
                                + toString(rid));
                return Defs.NULL_FLOAT;
            }
            double[] n1 = gumbo.atom.getVector(rid_n1);
            double[] n3 = gumbo.atom.getVector(rid_n3);
            double[] c5 = gumbo.atom.getVector(rid_c5);
            double[] v1 = Geometry.sub(n1, n3);
            double[] v2 = Geometry.sub(n1, c5);
            normal[i] = Geometry3D.crossProduct(v1, v2);
        }
        double angle = Geometry3D.angle(normal[0], normal[1]);
        if (angle > (Math.PI / 2)) {
            angle = Math.PI - angle;
        }
        return (float) angle;
    }

    /**
     * This method requires the presence of hydrogen bonds before this call. The
     * hydrogen bonds don't need to be selected.<BR>
     * <OL>
     * <LI>A base (Bx) is in a set of co-planar bases (Si) if it has at least
     * two hydrogen bonds to one other base By in Si. The angle between the
     * normals of Bx and By should be less than the value given, or 45 degrees
     * (default) if the given value is negative.
     * <LI>A set is called a pair, triplet, or quartet for when there are 2,3,
     * or 4 members.
     * <LI>U is a superset of all bases.
     * <LI>V is a superset of all bases in any set Si so: V = Sum Si.
     * <LI>W is a superset of all bases not in any set Si so W = U - V.
     * <LI>Only 4 bases can be in Si. The bases with the least hydrogen bonds
     * to the other bases in Si will be deleted.
     * <LI>A base can be only in one set Si.
     * <LI>The rmsd from a plane defined by the bases in Si is below a given
     * threshold multiplied by the number of bases. (TODO).
     * <LI>Direct neighbours in sequence can not be in the same set (Check with
     * CST if this ever occurs).
     * <LI>Routine will print results but not store them for now.
     * </OL>
     * 
     * The algorithm is simple.
     * <OL>
     * <LI>Loop1 over all selected bases Bx of the one selected entry (using
     * first model only)
     * <LI> Add Bx to Si
     * <LI> Loop2 until no new bases are added
     * <LI> Loop3 over base pairs By in W
     * <LI> If By matches with any in Si then add By to Si (loop 4)
     * <LI> EndLoop3
     * <LI> EndLoop2
     * <LI> Remove the worst B's until no more than 4 are left in Si
     * <LI> If only Bx is Si then remove Si from V
     * <LI> Store the results
     * <LI>EndLoop1
     * <LI>
     * </OL>
     * 
     * <P>
     * The 45 degree maximum angle between the base normals is as suggested by:
     * Ban,N., Nissen,P., Hansen,J., Moore,P.B. and Steitz,T.A. (2000). The
     * complete atomic structure of the large ribosomal subunit at 2.4 A
     * resolution. Science 289, 905-920. NB
     * 
     * @see Entry#calcHydrogenBond
     * @param resInMaster
     *            residues in the master model
     * @param thresholdBasePairAngle
     * @param onlyWC
     *            ignore non-Watson/Crick Basepairs. Note that only 2 WC
     *            hydrogen bonds need to be present.
     * @return true for success
     */
    public boolean calcCoPlanarBasesSet(BitSet resInMaster,
            float thresholdBasePairAngle, boolean onlyWC, String location) {
        int resRidA = -1;
        int resRidB = -1;
        /** Order residue numbers in list of existing NAs */
        int resA, resB;
        int swap;

        int entryRid = gumbo.entry.getEntryId();
        if (entryRid < 0) {
            return false;
        }
        General
                .showDebug("Trying to match bases to eachother for number of residues: "
                        + resInMaster.cardinality());
        BitSet basesInMaster = getNAResidues(resInMaster);
        int basesInMasterCount = basesInMaster.cardinality();
        General
                .showOutput("Trying to match bases to eachother for number of bases: "
                        + basesInMasterCount);
        // General.showDebug("Bases:\n" + toString(basesInMaster));
        // BitSet S = new BitSet();
        BitSet U = new BitSet();
        BitSet V = new BitSet();
        BitSet W = new BitSet();
        ArrayList result = new ArrayList();

        /** Setup a square matrix with the number of HB between bases. */
        int[][] countHB = new int[basesInMasterCount][];
        for (int i = 0; i < basesInMasterCount; i++) {
            countHB[i] = new int[basesInMasterCount];
        }
        /** Define a map from res rid to res number considered */
        int[] mapResFromRid = new int[mainRelation.sizeMax];
        // Provoke errors in case of abuse by initializing the map with a bad
        // value.
        Arrays.fill(mapResFromRid, -1);
        int ii = 0;
        for (int i = basesInMaster.nextSetBit(0); i >= 0; i = basesInMaster
                .nextSetBit(i + 1)) {
            mapResFromRid[i] = ii;
            ii++;
        }
        // General.showDebug("Map:\n" + PrimitiveArray.toString(mapResFromRid));

        // Cache the number of hb between the bases in a matrix. No size
        // worries.
        // Reduce to short if needed.
        Index indexType = gumbo.bond.mainRelation.getIndex(
                Relation.DEFAULT_ATTRIBUTE_TYPE, Index.INDEX_TYPE_SORTED);
        Index indexEntry = gumbo.bond.mainRelation.getIndex(
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME],
                Index.INDEX_TYPE_SORTED);
        BitSet bondHBSet = (BitSet) indexType.getRidList(new Integer(
                Bond.BOND_TYPE_HYDROGEN), Index.LIST_TYPE_BITSET, null);
        BitSet bondEntrySet = (BitSet) indexEntry.getRidList(new Integer(
                entryRid), Index.LIST_TYPE_BITSET, null);
        bondHBSet.and(bondEntrySet);
        General.showOutput("Considering number of hydrogen bonds: "
                + bondHBSet.cardinality());
        int atomRidA = -1;
        int atomRidB = -1;
        String atomNameA = null;
        String atomNameB = null;
        int hbSkippedCount = 0;
        for (int bondRid = bondHBSet.nextSetBit(0); bondRid >= 0; bondRid = bondHBSet
                .nextSetBit(bondRid + 1)) {
            // General.showDebug("HB:" + gumbo.bond.toString(bondRid));
            atomRidA = gumbo.bond.atom_A_Id[bondRid];
            atomRidB = gumbo.bond.atom_B_Id[bondRid];
            atomNameA = gumbo.atom.nameList[atomRidA];
            atomNameB = gumbo.atom.nameList[atomRidB];
            if (!(basesInMaster.get(gumbo.atom.resId[atomRidA]) && basesInMaster
                    .get(gumbo.atom.resId[atomRidB]))) {
                // General.showDebug("Skipping hydrogen bonds other than
                // nucleotide to nucleotide: " + gumbo.bond.toString(bondRid));
                bondHBSet.clear(bondRid);
                hbSkippedCount++;
                continue;
            }
            if (Biochemistry.backboneNA.containsKey(atomNameA)
                    || Biochemistry.backboneNA.containsKey(atomNameB)) {
                // General.showDebug("Skipping hydrogen bonds other than base to
                // base: " + gumbo.bond.toString(bondRid));
                bondHBSet.clear(bondRid);
                hbSkippedCount++;
                continue;
            }

            resRidA = gumbo.atom.resId[atomRidA]; // donor residue
            resRidB = gumbo.atom.resId[atomRidB];
            String resNameA = gumbo.res.nameList[resRidA];
            String resNameB = gumbo.res.nameList[resRidB];
            if (onlyWC
                    && !Biochemistry.isWCBond(atomNameA, resNameA, atomNameB,
                            resNameB)) {
                General.showDebug("Skipping hydrogen bonds other than wc: "
                        + gumbo.bond.toString(bondRid));
                bondHBSet.clear(bondRid);
                hbSkippedCount++;
                continue;
            }

            resA = mapResFromRid[resRidA];
            resB = mapResFromRid[resRidB];
            if (resA > resB) {
                swap = resA;
                resA = resB;
                resB = swap;
            }
            // Now resA is the 'smaller' residue so the matrix will only be
            // filled
            // on the top right of the diagonal (NOT in Rid space).
            // General.showDebug("Adding hydrogen bond between bases: " + resA +
            // " and: " + resB);
            countHB[resA][resB]++;
        }
        General.showDebug("Skipped hydrogen bonds                : "
                + hbSkippedCount);
        General.showDebug("Number of hydrogen bonds in matrix (B):"
                + bondHBSet.cardinality());
//        if (General.verbosity >= General.verbosityDebug) { // inserted clause
//                                                            // for speed.
//            General.showDebug("Number of hydrogen bonds in matrix (A):\n"
//                    + PrimitiveMatrix.toString(countHB));
//        }

        U.or(basesInMaster);
        W.or(basesInMaster);
        BitSet Wtemp = new BitSet();
        int x, y, z, zz = 0;

        IntArrayList Slist = new IntArrayList();
        for (x = U.nextSetBit(0); x >= 0; x = U.nextSetBit(x + 1)) { // loop
                                                                        // 1
            Slist.setSize(0); // clears matrix without allocating a new
                                // object.
            Slist.add(x);
            Wtemp.clear(); // clears bits without allocating new data.
            Wtemp.or(W);
            Wtemp.clear(x);
            boolean baseAdded = true;
            int iterationCount = 0;
            // loop 2
            while (baseAdded && (iterationCount < 5)) { // try to add bases 5
                                                        // times
                iterationCount++;
                // General.showDebug("Now in iteration: " + iterationCount + "
                // for first base: " + x);
                baseAdded = false;
                // loop 3
                for (y = Wtemp.nextSetBit(0); y >= 0; y = Wtemp
                        .nextSetBit(y + 1)) { // y is the candidate (res rid)
                    // loop 4
                    for (zz = 0; zz < Slist.size(); zz++) { // to match with
                                                            // bases already in
                                                            // S which includes
                                                            // at least x.
                        // define the set candidate z (res rid)
                        z = Slist.getQuick(zz);
                        resA = mapResFromRid[y];
                        resB = mapResFromRid[z];
                        if (resA > resB) {
                            swap = resA;
                            resA = resB;
                            resB = swap;
                        }
                        if (countHB[resA][resB] > 1) {
                            float angle = calcGAngleBasePair(y, z);
                            if (Defs.isNull(angle)) {
                                General
                                        .showError("Failed calcGAngleBasePair between:\n"
                                                + toString(y)
                                                + General.eol
                                                + toString(z));
                                // General.showError("Ignoring this potential
                                // pair.");
                                return false;
                            }
                            // General.showDebug("Angle between bases is: " +
                            // Geometry.CF*angle);
                            if ((Geometry.CF * angle) > thresholdBasePairAngle) {
                                continue;
                            }
                            // Getting zzzzleepy.
                            boolean foundConsequetive = false;
                            for (int zzz = 0; zzz < Slist.size(); zzz++) {
                                int zzzz = Slist.getQuick(zzz);
                                int sep = getSeparation(y, zzzz);
                                if (Math.abs(sep) == 1) {
                                    General
                                            .showWarning("Ignoring potential pair between consequetive bases:\n"
                                                    + toString(y)
                                                    + General.eol
                                                    + toString(zzzz));
                                    foundConsequetive = true;
                                    break;
                                }
                            }
                            if (foundConsequetive) {
                                continue;
                            }

                            Slist.add(y);
                            Wtemp.clear(y);
                            baseAdded = true;
                            if (General.verbosity >= General.verbosityDebug) {
                                General
                                        .showDebug("Found pair between new base:\n"
                                                + toString(y)
                                                + "and base already in set:\n"
                                                + toString(z));
                                int[] residueList = new int[] { y, z };
                                BitSet newBondSet = gumbo.bond.scanForResidues(
                                        bondHBSet, residueList);
                                General
                                        .showDebug("Found hydrogen bonds from one or both of them:\n"
                                                + gumbo.bond
                                                        .toString(newBondSet));
                            }
                            break;
                        }
                    } // end of loop 4
                } // end of loop 3
            } // end of loop 2
            if (iterationCount == 5) {
                General
                        .showCodeBug("Unlikely that the routine had to cycle more than 4 iterations, check code");
                return false;
            }
            /**
             * todo while ( Slist.size() > 4 ) { General.showCodeBug("Please
             * check this code before continueing"); int Scount = Slist.size(); //
             * remove the 2's, 3's and even 4's at random until the set is ok. //
             * first assemble the totals for each residue. int[] hbBondInS = new
             * int[S.size()]; // of course a waste of space. for (int
             * z=S.nextSetBit(0); z>=0; z=S.nextSetBit(z+1)) { for (int
             * zz=S.nextSetBit(z); zz>=0; zz=S.nextSetBit(zz+1)) { // only do
             * upper right triangle if ( z == zz ) { continue; } hbBondInS[ z ] +=
             * countHB[z][zz]; } } int killerCount = 2; while ( (Scount > 4) &&
             * (killerCount < 5)) { for (int z=S.nextSetBit(0); z>=0;
             * z=S.nextSetBit(z+1)) { if ( hbBondInS[ z ] == killerCount ) {
             * General.showDebug("Killing base from too large list:\n" +
             * toString(z)); S.clear(z); Scount--; if ( Scount < 4 ) { break; } } }
             * killerCount++; } if ( killerCount == 5 ) {
             * General.showError("Killer count of 5 is unusual, again just check
             * the code as it's applied to base:\n" + toString(x)); return
             * false; } }
             */
            if (Slist.size() == 1) {
                continue;
            }
            BitSet S = PrimitiveArray.toBitSet(Slist, -1);
            // Now operate on the non-temp sets.
            V.or(S);
            W.andNot(S);
            result.add(S);
        } // end of loop 1 over all selected bases Bx of the one selected
            // entry (using first model only)

        /**
         * entry code entity code A residue number A residue name A
         * 
         * entity code B residue number B residue name B
         * 
         * 1jj2 1 19 G 2 469 C
         */
        Relation rel = null;
        try {
            rel = new Relation("basepairsets", dbms);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
//        rel.addColumnForOverallOrder();
        rel.insertColumn("entry_id");
        for (int i = 0; i < 5; i++) { // assume never more than 5 (4 is the most seen so far)
            rel.insertColumn("entity code "     + i);
            rel.insertColumn("residue number "  + i);
            rel.insertColumn("residue name "    + i);
        }
        int resultSize = result.size();
        if ( resultSize == 0 ) {
            General.showOutput("No sets of co-planar bases found", resultSize);
        } else {
            rel.getNewRows(resultSize); 
            General.showOutput("Results contains number of sets of co-planar bases: "
                            + resultSize);
    //        General.showOutput("Results with internal residue numbering:\n"
    //                + Strings.toString(result, true));
            General.showOutput("Results:");
            for (int i = 0; i < resultSize; i++) {
                BitSet bs = (BitSet) result.get(i);
                General.showOutput(toString(bs));
                int j = 0;
                rel.setValue(i, 0, gumbo.entry.nameList[entryId[bs.nextSetBit(0)]]);
                for (int resIdx = bs.nextSetBit(0); resIdx >= 0; resIdx = bs.nextSetBit(resIdx + 1)) {
                    rel.setValue(i, 1 + (j * 3), Integer.toString(molId[resIdx] + 1));
                    rel.setValue(i, 2 + (j * 3), Integer.toString(number[resIdx]));
                    rel.setValue(i, 3 + (j * 3), nameList[resIdx]);
                    j++;
                }
            }            
            General.showOutput("Writing results to file: " + location);
            if ( ! rel.writeCsvFile(location, true)) {
                General.showError("Failed to write");
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the signed difference between the two residue numbers (b-a) or
     * Defs.NULL_INT if they're not in the same molecule/model/entry
     */
    public int getSeparation(int rid_a, int rid_b) {
        int result = Defs.NULL_INT;
        if (molId[rid_a] == molId[rid_b]) {
            result = number[rid_b] - number[rid_a];
        }
        // General.showOutput("Separation between residues is: " + result);
        // General.showOutput("For residues:\n" + toString(rid_a) + General.eol
        // + toString(rid_b));
        return result;
    }

    public float getMass(int j) {
        float result = 0f;
        BitSet bs = getAtoms(j);
        for (int i=bs.nextSetBit(0);i>=0;i=bs.nextSetBit(i+1)) {
            float mass = gumbo.atom.getMass(i);
            result += mass;            
        }
        return result;
    }

    /** Finds rid of neighbour in residue number distance.
     * Needs to be in same molecule.
     * @param res_rid
     * @param distance in number of residues.
     * @return
     */
    public int getNeighbour(int res_rid, int distance) {
        int resNumber = number[res_rid] + distance;        
        int molRid = molId[res_rid];
        BitSet residuesInSameMol = SQLSelect.selectCombinationBitSet(
                gumbo.dbms,
                mainRelation,
                Relation.DEFAULT_ATTRIBUTE_NUMBER,                                    SQLSelect.OPERATION_TYPE_EQUALS, new Integer(resNumber),
                Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME], SQLSelect.OPERATION_TYPE_EQUALS, new Integer(molRid),
                   SQLSelect.OPERATOR_AND, false );                
        return residuesInSameMol.nextSetBit(0);
    }

    public int getOrganic_ligands(BitSet allResSet) {
        // TODO Auto-generated method stub
        return 0;
    }

    /** Thru the atom sibling list get another model's residue rid for the given residue rid
     * Remember first model is numbered zero.  
     * */ 
    public int getResRidFromModel( int resRid, int modelId ) {
        BitSet atomRidSet = getAtoms(resRid);
        if ( atomRidSet == null ) {
            General.showError("Failed to get atoms in this res in getResRidFromModel");
            return Defs.NULL_INT;
        }
//        General.showDebug(gumbo.atom.toString(atomRidSet));
        int atomRidFirstInRes = atomRidSet.nextSetBit(0);
        if ( atomRidFirstInRes < 0 ) {
            General.showError("Failed to get atomRidFirstInRes for residue:");
            General.showError(toString(resRid));
            return Defs.NULL_INT;
        }
        int atomRidSpecificModel = gumbo.atom.modelSiblingIds[ atomRidFirstInRes ][ modelId ]; 
        int  resRidSpecificModel = gumbo.atom.resId[ atomRidSpecificModel ]; 
//        General.showDebug("Found for resRid: "+resRid+" resRidSpecificModel: "+ resRidSpecificModel+" for modelId: " + modelId + " and atomRidFirstInRes " + atomRidFirstInRes);
        return resRidSpecificModel;                        
    }

    /** Adds missing atoms based on TopologyLib for common residues. 
     * Adapted from PyMol algorithm in modules/chempy/place.py I presume
     * Greg Warren wrote it some time ago and in 2007 he mentioned
     * it was actually not being used in PyMol.
     * <P>
     * The algorithm uses the topology library for expected atoms and names
     * and the Amber based atom type specifications.
     * <P>
     * The atoms must be synchronized over the models before calling this routine.
     * <P>
     * NB <li>Topology is nowhere near perfect and needs optimalization for
     * e.g. clashes aren't prevented.
     * <li> Algorithm could work for adding all atoms if just one atom in the residue is 
     * present but it's only tested on proteins and only adding protons.
     * <li> I think the algorithm could be much simpler and less repetitive
     * if only 1 atom in each round would be added. It would be a little slower
     * but probably not an issue.
     * 
     * TODO: fix the algorithm for:
     * - OPs to P in nucleic acids.
     * - HB Val  
     * */
    public boolean addMissingAtoms(BitSet resInMaster) {
        double TET_TAN = 1.41;
        double TRI_TAN = 1.732;

        DBMS    topDbms     = dbms.ui.wattosLib.topologyLib.dbms;
        Gumbo   topGumbo    = dbms.ui.wattosLib.topologyLib.gumbo;
        Bond    topBond     = topGumbo.bond;
        Atom    topAtom     = topGumbo.atom;
        Residue topRes      = topGumbo.res;
                
        int resRidMaster=resInMaster.nextSetBit(0);
        if ( resRidMaster <0 ) {
            General.showOutput("No residues found in master skipping checkAtomNomenclature in Residue");
            return true;
        }
        BitSet modelRidSet = gumbo.entry.getModelsInEntry( entryId[resRidMaster]);
        int modelCount = modelRidSet.cardinality();
        if ( modelCount == 0 ) {
            General.showOutput("No models found in master skipping checkAtomNomenclature in Residue");
            return true;
        }
        int firstModelRid = modelRidSet.nextSetBit(0);
        int entryRid = gumbo.model.entryId[ firstModelRid ];

        BitSet atomsAddedAllCycles = new BitSet();
        /** First cycle will be numbered one */
        int ncycle = 0;
                
        while ( true ) { // Iterate until no more atoms are added once.
            if ( ncycle != 0 ) {
                if ( ! gumbo.entry.syncModels(entryRid)) {
                    General.showError("Failed to sync models in subsequent iteration of addMissingAtoms");
                    break;
                }
                if ( ! gumbo.atom.calcBond()) {
                    General.showError("Failed to calc bonds in subsequent iteration of addMissingAtoms");
                    break;
                }
            }
            ncycle++;
            General.showOutput("Starting cycle: " + ncycle);
            /** The structure of this data is: ArrayList[4]
             *  Integer(atomRid), BitSet(miss), BitSet(know)*/
            ArrayList[] need = new ArrayList[4];
            for (int i=0;i<need.length;i++) {
                need[i] = new ArrayList();
            }
            
            for (resRidMaster=resInMaster.nextSetBit(0);resRidMaster>=0;resRidMaster=resInMaster.nextSetBit(resRidMaster+1)) {
//                General.showDebug("Working on master residue:");
//                General.showDebug(toString(resRidMaster));
                boolean isN_TerminalRes = isN_Terminal(resRidMaster);
                boolean isC_TerminalRes = isC_Terminal(resRidMaster);
                
                BitSet atomSetMasterRes = getAtoms(resRidMaster);
                if ( atomSetMasterRes.nextSetBit(0) < 0 ) {
    //                General.showDebug("Skipping residue without coordinate atoms");
                    continue;
                }
                HashMap knownAtomNamesThisResidue = new HashMap();
                for (int atomRid=atomSetMasterRes.nextSetBit(0);atomRid>=0;atomRid=atomSetMasterRes.nextSetBit(atomRid+1)) {
                    knownAtomNamesThisResidue.put(gumbo.atom.nameList[atomRid], null);
                }
                
                String resName = nameList[resRidMaster];
                boolean isCys_Res = resName.equals("CYS");
                boolean isHis_Res = resName.equals("HIS");
                BitSet resRidSetTop = SQLSelect.selectBitSet(topDbms, topRes.mainRelation, 
                        Relation.DEFAULT_ATTRIBUTE_NAME, SQLSelect.OPERATION_TYPE_EQUALS, 
                        resName, false);
                int resRidTop = resRidSetTop.nextSetBit(0);
                if ( resRidTop < 0 ) {
                    // Residue unknown to topology info
                    continue; 
                }
                /** Atoms in topologies residue */
                BitSet atomRidResListTop = SQLSelect.selectBitSet(topDbms, topAtom.mainRelation, 
                        Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RELATION_ID_COLUMN_NAME], SQLSelect.OPERATION_TYPE_EQUALS, 
                        new Integer(resRidTop), false);
                /** For all atoms in real residue */
                for (int atomRid=atomSetMasterRes.nextSetBit(0);atomRid>=0;atomRid=atomSetMasterRes.nextSetBit(atomRid+1)) {
//                    General.showDebug("Working from known atom: " + gumbo.atom.toString(atomRid));
                    String atomName1 = gumbo.atom.nameList[atomRid];
                    /** The equivalent atom in topology residue; small table gives fast look up. */
                    BitSet atomRidListTop = SQLSelect.selectBitSet(topDbms, topAtom.mainRelation, 
                            Relation.DEFAULT_ATTRIBUTE_NAME, SQLSelect.OPERATION_TYPE_EQUALS, 
                            atomName1, false);
                    atomRidListTop.and(atomRidResListTop);
                    int atomRidTop = atomRidListTop.nextSetBit(0);
                    if ( atomRidTop < 0 ) {
//                        General.showDebug("Failed to find equivalent atom in wi topology; skipping this atom");
//                        General.showDebug(gumbo.atom.toString(atomRid));
                        continue; 
                    }
                    BitSet bondRidListA = SQLSelect.selectBitSet(topDbms, topBond.mainRelation, 
                            Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, SQLSelect.OPERATION_TYPE_EQUALS, 
                            new Integer(atomRidTop), false);                
                    BitSet bondRidListB = SQLSelect.selectBitSet(topDbms, topBond.mainRelation, 
                            Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, SQLSelect.OPERATION_TYPE_EQUALS, 
                            new Integer(atomRidTop), false);
                    BitSet bondRidListBoth = (BitSet) bondRidListA.clone();
                    bondRidListBoth.or(bondRidListB);
                    if ( bondRidListBoth.nextSetBit(0)<0 ) {
                        General.showError("Working on a atom that according to topology lib isn't connected to anything in the topology lib.");
                        return false;
                    }
//                    General.showDebug("For this atom found bonds A: ");
//                    General.showDebug(topBond.toString(bondRidListA));
//                    General.showDebug("For this atom found bonds B: ");
//                    General.showDebug(topBond.toString(bondRidListB));
    
                    /** List of atom top rids with missing coordinates bonded to this atom */
                    BitSet miss = new BitSet(); 
                    /** List of regular atom rids known */
                    BitSet know = new BitSet(); //
                    BitSet[] bondRidLoL = new BitSet[] { bondRidListA, bondRidListB };
                    for (int bl=0;bl<2;bl++) {
                        BitSet bondRidList = bondRidLoL[bl];
                        int[] bonded_atom_id_list = topBond.atom_B_Id;
                        if ( bl == 1 ) {
                            bonded_atom_id_list = topBond.atom_A_Id;
                        }
                        for (int b=bondRidList.nextSetBit(0);b>=0;b=bondRidList.nextSetBit(b+1)) {
                            int bondedAtomTopRid = bonded_atom_id_list[b];
                            String atomBondedName = topAtom.nameList[bondedAtomTopRid];                            
                            if ( isN_TerminalRes && Biochemistry.N_TERMINAL_ATOM_NAME_MAP.containsKey( atomBondedName )) {
//                                General.showDebug("Skipping recalculation of below N terminal atoms for residue:");
//                                General.showDebug( toString(resRidMaster));
//                                General.showDebug( topAtom.toString(bondedAtomTopRid));
                                continue;
                            }                                
                            if ( isC_TerminalRes && Biochemistry.C_TERMINAL_ATOM_NAME_MAP.containsKey( atomBondedName )) {
//                                General.showDebug("Skipping recalculation of below C terminal atoms for residue:");
//                                General.showDebug( toString(resRidMaster));
//                                General.showDebug( topAtom.toString(bondedAtomTopRid));
                                continue;
                            }               
                            if ( isCys_Res && atomBondedName.equals("HG") ) {
                                if ( bondRidListBoth.cardinality()>=2 ) {
//                                    General.showDebug("Cys doesn't need extra HG");
                                    continue;
                                }
                            }
                            if ( isHis_Res && (atomBondedName.equals("HE1")|| atomBondedName.equals("HE2"))) {
                                if ( bondRidListBoth.cardinality()>=3 ) {
//                                    General.showDebug("HIS doesn't need extra he1 or 2");
                                    continue;
                                }
                            }
                            if ( isHis_Res && atomBondedName.equals("HD1") ) {
//                                General.showDebug("His gets no HD1 as to prevent unusual charged state");
                                continue;
                            }
                            
                            if ( knownAtomNamesThisResidue.containsKey(atomBondedName)) {                                
                                int bondedAtomRid = getAtomRid(atomSetMasterRes, atomBondedName);
                                know.set(bondedAtomRid);
                            } else {
                                miss.set(bondedAtomTopRid);
                            }
                        }
                    }
                    int nmiss = miss.cardinality();
                    if ( nmiss > 0 ) {                        
//                        General.showDebug("Finding atom with "+nmiss+" missing neighbor(s):");
//                        General.showDebug(gumbo.atom.toString(atomRid));
//                        General.showDebug("Missing:");
//                        General.showDebug(topAtom.toString(miss));
//                        General.showDebug("Known:");
//                        General.showDebug(gumbo.atom.toString(know));
                        ArrayList store = new ArrayList();
                        store.add(new Integer(atomRid));
                        store.add(miss);
                        store.add(know);
                        /**append the missing atom idx list to the right element in the need list. 
                        e.g. if 2 atoms are missing it will be added to the need[1]*/
                        need[nmiss-1].add(store);
                    }
                }
            } // done with getting 4 lists
            
//          For recalculating the backbone amide H rewrite the know list from CA to: CA and C from the preceding residue.
            for (int a=0;a<need[0].size();a++) {
                ArrayList store = (ArrayList) need[0].get(a);
                int atom1Rid = ((Integer) store.get(0)).intValue(); // atom idx of anchor; atom with known coordinates
                if (! gumbo.atom.nameList[atom1Rid].equals("N")) {
                    continue;
                }
                BitSet miss = (BitSet) store.get(1);
                int atomRidH = miss.nextSetBit(0);
                if (! topAtom.nameList[atomRidH].equals("H")) {
                    continue;
                }                
                BitSet know = (BitSet) store.get(2);
                int resPrevRid = getNeighbour(gumbo.atom.resId[atom1Rid], -1);
                if ( resPrevRid < 0 ) {
                    General.showCodeBug("Didn't expect no preceding residue and still calculating new backbone amide");
                    return false;
                }
                int atomRidC = gumbo.atom.getRidByAtomNameAndResRid("C", resPrevRid);
                if ( atomRidC < 0 ) {
                    General.showCodeBug("Didn't expect no C in preceding residue but I guess it's possible; coordinate for this backbone amide will be bad");                    
                    continue;
                }
                know.set(atomRidC);
//                General.showDebug("For backbone N atom missing neighbor(s):");
//                General.showDebug(gumbo.atom.toString(atom1Rid));
//                General.showDebug("Missing B:");
//                General.showDebug(topAtom.toString(miss));
//                General.showDebug("Known B:");
//                General.showDebug(gumbo.atom.toString(know));
            }
            
            
            /** At this point we know which atoms are missing for all models too because the
             * atoms were sync-ed. The easiest way with least amount of programming is to extend the need
             * list to include the other model's atom ids too.
             */
            for (int i=0;i<4;i++) {
                ArrayList needThis = need[i];
                int initialSizeNeedThis = needThis.size();
                for (int a=0;a<initialSizeNeedThis;a++) {
                    ArrayList store = (ArrayList) needThis.get(a);
                    int atom1Rid = ((Integer) store.get(0)).intValue(); // atom idx of anchor; atom with known coordinates
                    BitSet miss = (BitSet) store.get(1); // just copy
                    BitSet know = (BitSet) store.get(2); // expand
                    int[] modelSiblingAtomIds = gumbo.atom.modelSiblingIds[atom1Rid];
                    if ( modelSiblingAtomIds == null ) {
                        General.showError("For atom: " + gumbo.atom.toString(atom1Rid));
                        General.showError("Got no siblings in addMissingAtoms.");
                        return false;
                    }
                    int modelCountLocal = modelSiblingAtomIds.length;
                    if ( modelCountLocal != modelCount ) {
                        General.showError("For atom: " + gumbo.atom.toString(atom1Rid));
                        General.showError("modelCountLocal != modelCount:" + modelCountLocal +" and " + modelCount);
                        return false;
                    }
                    int nmiss = miss.cardinality();
                    if ( nmiss != (i+1) ) {
                        General.showCodeBug("Sanity check failed: nmiss != (i+1) nmiss="+nmiss+" and i=" +i);
                        return false;
                    }
                    // Skip first model (m=0) because it is already in; it's the base from which we derive.
                    for (int m=1;m<modelCountLocal;m++) {
                        ArrayList storeNextModel = new ArrayList();
                        needThis.add( storeNextModel );
                        storeNextModel.add( new Integer( modelSiblingAtomIds[m]));
                        storeNextModel.add( miss.clone());                        
                        BitSet knowNextModel = new BitSet();
                        storeNextModel.add( knowNextModel );                        
                        for (int k=know.nextSetBit(0);k>=0;k=know.nextSetBit(k+1)) {
                            knowNextModel.set( gumbo.atom.modelSiblingIds[k][m]);
                        }
//                        General.showDebug("Needed ["+i+"] extra for model: "+(m+1)+" atom: " + gumbo.atom.toString(modelSiblingAtomIds[m]));
                    }                    
                }
            }
            
            
            
            BitSet atomsAddedThisCycle = new BitSet();

            // ---1--- missing only ONE atom
            int ntodo = 1;
            for (int a=0;a<need[ntodo-1].size();a++) {
                ArrayList store = (ArrayList) need[0].get(a);
                int atom1Rid = ((Integer) store.get(0)).intValue(); // atom idx of anchor; atom with known coordinates
//                General.showDebug("Doing anchor atom " +a + " that needs only ONE atom added: " + gumbo.atom.toString(atom1Rid));
                BitSet miss = (BitSet) store.get(1);
                BitSet know = (BitSet) store.get(2);                
                int resRid = gumbo.atom.resId[atom1Rid];
                String resName = nameList[resRid];
                String atomName1 = gumbo.atom.nameList[atom1Rid];
                int atom2Rid = miss.nextSetBit(0); // FIRST atom idx with missing coor.
                String atomName2 = topAtom.nameList[atom2Rid];
                
                double bondLenght = topBond.getBondLength(resName, atomName1, atomName2);
                
                /** JFD Is the known coordinate atom's type in a nonlinear arrangement?
                E.g. OW is nonlinear but others are: tetrahedral and planar
                dictionaries are used for fast lookup
                Why tetrahedral and nonlinear are distinguished is unclear to me now.
                */
                int atom1Type = gumbo.atom.type[gumbo.atom.masterAtomId[ atom1Rid]];
//                General.showDebug("Atom 1 is of type: " + AtomLibAmber.DEFAULT_ATOM_TYPE_STRING_LIST[atom1Type]);
                if ( atom1Type == AtomLibAmber.DEFAULT_ATOM_TYPE_NONLINEAR_ID ) {
                    int[] near = findKnownSecondary(atom1Rid,know);
                    if ( near != null ) {
                        int atom3Rid = near[0];
                        int atom3Type = gumbo.atom.type[gumbo.atom.masterAtomId[ atom3Rid]];
//                        General.showDebug("Atom 3 is of type: " + AtomLibAmber.DEFAULT_ATOM_TYPE_STRING_LIST[atom3Type]);
                        int atom4Rid = near[1];
                        if (atom3Type==AtomLibAmber.DEFAULT_ATOM_TYPE_PLANAR_ID) {
//                          # At this point atoms could be in a TYR:
//                          #    1 OH the anchor             
//                          #    2 HH the unknown                 OH<-d1-CZ-d2->CE1
//                          #    3 CZ the secondary with          |
//                          #    4 CE1 helper both known.         |p2,v?
//                          #                                     HH
                            double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                            double[] p0 = Geometry3D.normalize(d1);
                            double[] d2 = gumbo.atom.getVector( atom4Rid, atom3Rid);
                            /** p1 is the vector defining the plane of all involved atoms */ 
                            double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                            /** p2 is the vector that almost points to the new bond; it's in the p1 defined plane */ 
                            double[] p2 = Geometry3D.normalize( Geometry3D.crossProduct(p0,p1));
                            /** Next op is needed for getting the 120 degree bond angles */ 
                            double[] v  = Geometry3D.scale( p2,TRI_TAN );
//                            General.showDebug("v : " + Geometry3D.vectorToString(v));
                                     v =  Geometry3D.normalize( Geometry3D.add( p0, v ));
                            double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                                    Geometry3D.scale(v,bondLenght));
                            int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                            atomsAddedThisCycle.set(newAtomIdx);
                        } else { // Ser, Cys, Thr hydroxyl hydrogens
//                             # At this point atoms could be in a CYS:
//                             #    1 SG Anchor           4      3      1     2  
//                             #    2 HG The unknown     CA--d2->CB-----SG----HG
//                             #    3 CB the secondary with          
//                             #    4 CA helper both known.          
                            /** # same direction as CA-CB */
                            double[] d2 = gumbo.atom.getVector( atom3Rid, atom4Rid);
                            double[] v  = Geometry3D.normalize( d2 );
                            double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                                    Geometry3D.scale(v,bondLenght));                            
                            int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                            atomsAddedThisCycle.set(newAtomIdx);
                        } // end of 3 is planar check
                    // when no known secondaries with it's neighbours known could be found but there are known
                    } else if ( know.nextSetBit(0) >= 0 ) { 
                        double[] d2 = new double[] { 1,0,0 };
                        int atom3Rid = know.nextSetBit(0);
//                        int atom3Type = gumbo.atom.type[gumbo.atom.masterAtomId[ atom3Rid]];
//                        General.showDebug("Atom 3 is of type: " + AtomLibAmber.DEFAULT_ATOM_TYPE_STRING_LIST[atom3Type]);
//                                         At this point atoms could be in a PHE:
//                                             1 CD1 the anchor             
//                                             2 HD1 the unknown                 CD1<p0-CE1
//                                             3 CE1 the secondary with          |
//                                                                               |p2,v?
//                                                                               HD1                        
                        double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                        double[] p0 = Geometry3D.normalize(d1);
                        /** # if p0 is same as assumed d2 this will give the zero vector */ 
                        double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                        /** Next op is needed for getting the 120 degree bond angles */ 
                        double[] v  = Geometry3D.scale( p1,TET_TAN );
                                 v  = Geometry3D.normalize( Geometry.sub( p0,v ));
                        /** above condition will place the HD1 on top of CD1. */
                        double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                                Geometry3D.scale(v,bondLenght));
                        int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx);
                    } else { // placed on a random position at bond length if no attached atom was known.
                        double[] coor = Geometry3D.randomSphere( gumbo.atom.getVector( atom1Rid), bondLenght);                        
                        int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx);
                    }
                } else if ( atom1Type == AtomLibAmber.DEFAULT_ATOM_TYPE_TETRAHEDRAL_ID ) {
//                  General.showDebug("Now in common case for like a MET HA, VAL HB, ..");
                  //                  # At this point atoms could be in a MET:
                  //                  #                                  CB or something 
                  //                  #    1 CA Anchor           3       1      4  
                  //                  #    2 HA  The unknown     N---d1--CA-d2--C
                  //                  #    3 CA                          |
                  //                  #    4 SG                          HA 2 
                  //                  # At this point atoms could be in a VAL:
                  //                  #                                  
                  //                  #    1 CB Anchor           3       1      4  
                  //                  #    2 HG  The unknown    CA---d1--CB-d2--CG1
                  //                  #    3 CA                          |
                  //                  #    4 CG1                         HG 2
                  int atom3Rid = know.nextSetBit(0);                    
                  int atom4Rid = know.nextSetBit(atom3Rid+1);
//                  General.showDebug("Atom 3: " + gumbo.atom.toString(atom3Rid));
//                  General.showDebug("Atom 4: " + gumbo.atom.toString(atom4Rid));
                  double[] v  = Geometry3D.getNullVector();
                  double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                  double[] d2 = gumbo.atom.getVector( atom1Rid, atom4Rid);
                           v  = Geometry3D.add( Geometry3D.normalize( d1),Geometry3D.normalize(d2));
                  double[] p0 = Geometry3D.normalize( v ); 
                  double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                           v  = Geometry3D.scale( p1,TET_TAN ); // TODO: for VAL HB this should be minus factor.
                  double[] v2  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                  double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                          Geometry3D.scale(v2,bondLenght));
                  if ( gumbo.atom.isCloseTo( coor, know, .8f)) { 
//                      General.showDebug("Found at least one atom too close; going opposite.");
                      v    = Geometry3D.scale( p1,-TET_TAN );
                      v2   = Geometry3D.normalize( Geometry3D.add( p0,v ));
                      coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),                      
                              Geometry3D.scale(v2,bondLenght));
                  }
                  // TODO: check for clash? and if so; pick minus factor?
                  int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                  atomsAddedThisCycle.set(newAtomIdx);
//                  General.showDebug("Added atom: " + gumbo.atom.toString(newAtomIdx));
//                } else if ( atom1Type == AtomLibAmber.DEFAULT_ATOM_TYPE_PLANAR_ID ) {
//                    General.showDebug("Now in common case for like a MET N for calculating it's amide proton");
//                    //                  # At this point atoms could be in a MET:
//                    //                  #    1 N Anchor                    1      3    4  
//                    //                  #    2 H   The unknown            -N-d2--CA----C
//                    //                  #    3 CA                          |
//                    //                  #    4 C                           H  2 
//                    int[] near = findKnownSecondary(atom1Rid,know);
//                    if ( near == null ) {
//                        General.showCodeBug("Didn't expect no nears for planar anchor: " + gumbo.atom.toString(atom1Rid));
//                        return false;
//                    }
//                    int atom3Rid = near[0];
//                    int atom4Rid = near[1];
//                    double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
//                    double[] p0 = Geometry3D.normalize(d1);
//                    double[] d2 = gumbo.atom.getVector( atom4Rid, atom3Rid);
//                    double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
//                    double[] p2 = Geometry3D.normalize( Geometry3D.crossProduct(p0,p1));
//                    double[] v  = Geometry3D.scale( p2,-TRI_TAN );
//                             v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
//                    double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
//                            Geometry3D.scale(v,bondLenght));
//                    int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
//                    atomsAddedThisCycle.set(newAtomIdx);
                } else if ( know.nextSetBit(0) > 0 ) { // linear sum...amide, tbu, etc
                    double[] v = Geometry3D.getNullVector();
                    for (int b=know.nextSetBit(0);b>=0;b=know.nextSetBit(b+1)) {
                        double[] d = gumbo.atom.getVector( atom1Rid, b); 
                        v  = Geometry3D.add( v,Geometry3D.normalize(d) );                        
                    }
                    v  = Geometry3D.normalize(v);
                    double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                            Geometry3D.scale(v,bondLenght));
                    int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                    atomsAddedThisCycle.set(newAtomIdx);                    
                } else {
                    double[] coor = Geometry3D.randomSphere( gumbo.atom.getVector( atom1Rid), bondLenght);                        
                    int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                    atomsAddedThisCycle.set(newAtomIdx);                    
                }
            } // end of adding ONE

            // ---2--- missing two atoms
            ntodo = 2;
            for (int a=0;a<need[ntodo-1].size();a++) {
//                General.showDebug("Doing atom " +a + " that needs TWO atoms added");
                ArrayList store = (ArrayList) need[1].get(a);
                int atom1Rid = ((Integer) store.get(0)).intValue(); // atom idx of anchor; atom with known coordinates
                BitSet miss = (BitSet) store.get(1);
                BitSet know = (BitSet) store.get(2);                
                int resRid = gumbo.atom.resId[atom1Rid];
                String resName = nameList[resRid];
                String atomName1 = gumbo.atom.nameList[atom1Rid];
                int atom2Rid = miss.nextSetBit(0); // FIRST atom idx with missing coor.
                String atomName2 = topAtom.nameList[atom2Rid];                
                double bondLenght = topBond.getBondLength(resName, atomName1, atomName2);
                
                int atom1Type = gumbo.atom.type[gumbo.atom.masterAtomId[ atom1Rid]];
//                General.showDebug(gumbo.atom.toString(atom1Rid));
                if ( Defs.isNull(atom1Type)) {
                    General.showCodeBug("Failed to find a known atom type for this atom");
                    return false;
                }
//                General.showDebug("Atom 1 is of type: " + AtomLibAmber.DEFAULT_ATOM_TYPE_STRING_LIST[atom1Type]);
                if (atom1Type == AtomLibAmber.DEFAULT_ATOM_TYPE_PLANAR_ID) {// guanido, etc
                    int[] near = findKnownSecondary(atom1Rid,know);
                    if ( near != null ) {
//                        General.showDebug("Found near: " + near );
                        int atom3Rid = near[0];
                        int atom4Rid = near[1];
                        double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                        double[] p0 = Geometry3D.normalize(d1);
                        double[] d2 = gumbo.atom.getVector( atom4Rid, atom3Rid);
                        double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                        double[] p2 = Geometry3D.normalize( Geometry3D.crossProduct(p0,p1));
                        double[] v  = Geometry3D.scale( p2,TRI_TAN );
                                 v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                        double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                                Geometry3D.scale(v,bondLenght));
                        int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx);
                                 v  = Geometry3D.scale( p2, -TRI_TAN );
                                 v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                        int atom2Rid2 = miss.nextSetBit(atom2Rid+1); // SECOND atom idx with missing coor.
                        String atomName22 = topAtom.nameList[atom2Rid2];                
                        double bondLenght2 = topBond.getBondLength(resName, atomName1, atomName22);                                 
                        double[] coor2 = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                             Geometry3D.scale(v,bondLenght2));
                        int newAtomIdx2 = gumbo.atom.add(atomName22, coor2, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx2);
                    } else if ( know.nextSetBit(0) >= 0 ) {// no 1-4 found
//                        General.showDebug("Found NO near but have a know.");                        
                        double[] d2 = new double[] { 1,0,0 };
                        int atom3Rid = know.nextSetBit(0);
                        double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                        double[] p0 = Geometry3D.normalize(d1);
                        double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                        double[] p2 = Geometry3D.normalize( Geometry3D.crossProduct(p0,p1));
                        double[] v  = Geometry3D.scale( p2,TRI_TAN );
                                 v  = Geometry3D.normalize( Geometry.sub( p0,v ));
                        double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                                Geometry3D.scale(v,bondLenght));
                        int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx);
                                v  = Geometry3D.scale( p2, -TRI_TAN );
                                v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                        int atom2Rid2 = miss.nextSetBit(atom2Rid+1); // SECOND atom idx with missing coor.
                        String atomName22 = topAtom.nameList[atom2Rid2];                
                        double bondLenght2 = topBond.getBondLength(resName, atomName1, atomName22);                                 
                        double[] coor2 = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                            Geometry3D.scale(v,bondLenght2));
                        int newAtomIdx2 = gumbo.atom.add(atomName22, coor2, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx2);
                    } else { // Doesn't calculate the second.
                        double[] coor = Geometry3D.randomSphere( gumbo.atom.getVector( atom1Rid), bondLenght);                        
                        int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx);
                    }                        
                } else if ( know.cardinality()>=2 ) {
//                    General.showDebug("Now in common case for like a Cys HB2");
//                        # At this point atoms could be in a CYS:
//                        #    1 CB Anchor           3       1      4  
//                        #    2 HB2 The unknown     CA--d1--CB-d2--SG
//                        #    3 CA                          |
//                        #    4 SG                          HB2 (and HB3 later)
                    int atom3Rid = know.nextSetBit(0);                    
                    int atom4Rid = know.nextSetBit(atom3Rid+1);
                    double[] v  = Geometry3D.getNullVector();
                    double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                    double[] d2 = gumbo.atom.getVector( atom1Rid, atom4Rid);
                             v  = Geometry3D.add( Geometry3D.normalize( d1),Geometry3D.normalize(d2));
                    double[] p0 = Geometry3D.normalize( v ); 
                    double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                             v  = Geometry3D.scale( p1,TET_TAN );
                             v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                    double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                            Geometry3D.scale(v,bondLenght));
                    int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                    atomsAddedThisCycle.set(newAtomIdx);
                             v  = Geometry3D.scale( p1, -TET_TAN );
                             v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                    int atom2Rid2 = miss.nextSetBit(atom2Rid+1); // SECOND atom idx with missing coor.
                    String atomName22 = topAtom.nameList[atom2Rid2];                
                    double bondLenght2 = topBond.getBondLength(resName, atomName1, atomName22);                                 
                    double[] coor2 = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                         Geometry3D.scale(v,bondLenght2));
                    int newAtomIdx2 = gumbo.atom.add(atomName22, coor2, resRid);   
                    atomsAddedThisCycle.set(newAtomIdx2);                    
                } else {
                    if ( know.cardinality()==1) {
                        double[] d2 = new double[] { 1,0,0 };
                        int atom3Rid = know.nextSetBit(0);
                        double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                        double[] p0 = Geometry3D.normalize(d1);
                        double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                        double[] v  = Geometry3D.scale( p1,TET_TAN );
                                 v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                        double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                                Geometry3D.scale(v,bondLenght));
                        int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx);
                        //TODO: add the second atom here e.g. for OP1/OP2 added on P?
                    } else { // blind
                        double[] coor = Geometry3D.randomSphere( gumbo.atom.getVector( atom1Rid), bondLenght);                        
                        int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                        atomsAddedThisCycle.set(newAtomIdx);                        
                    }
                    //#JFD not really needed to add second atom now right? It can be done in next round.                    
                }
            } // end of adding TWO
            
            // ---3--- missing three atoms
            ntodo = 3;
            for (int a=0;a<need[ntodo-1].size();a++) {
//                General.showOutput("Doing atom " +a + " that needs THREE atoms added");
                ArrayList store = (ArrayList) need[ntodo-1].get(a);
                int atom1Rid = ((Integer) store.get(0)).intValue(); // atom idx of anchor; atom with known coordinates
                BitSet miss = (BitSet) store.get(1);
                BitSet know = (BitSet) store.get(2);                
                int resRid = gumbo.atom.resId[atom1Rid];
                String resName = nameList[resRid];
                String atomName1 = gumbo.atom.nameList[atom1Rid];
                int atom2Rid = miss.nextSetBit(0); // FIRST atom idx with missing coor.
                String atomName2 = topAtom.nameList[atom2Rid];                
                double bondLenght = topBond.getBondLength(resName, atomName1, atomName2);
                
                int[] near = findKnownSecondary(atom1Rid,know);
                if ( near != null ) {
                    int atom3Rid = near[0];
                    int atom4Rid = near[1];
                    /**   # At this point atoms could be in a VAL:
//                        #    1 CG1 Anchor      4    3       1        
//                        #    2 HG11 Unknown    CA--CB--d1--CG1
//                        #    3 CB                          |
//                        #    4 CA                          HG11   2                     */
                    double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                    double[] p0 = Geometry3D.normalize(d1);
                    double[] d2 = gumbo.atom.getVector( atom4Rid, atom3Rid);
                    double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                    double[] p2 = Geometry3D.normalize( Geometry3D.crossProduct(p0,p1));
                    double[] v  = Geometry3D.scale( p2,-TET_TAN );
                             v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                    double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                            Geometry3D.scale(v,bondLenght));
                    int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                    atomsAddedThisCycle.set(newAtomIdx);
                } else if ( know.nextSetBit(0) >= 0 ) {// fall-back
                    /** Note the the python code referenced p0 which wasn't defined yet. */
                    double[] d2 = new double[] { 1,0,0 };
                    int atom3Rid = know.nextSetBit(0);
                    double[] d1 = gumbo.atom.getVector( atom1Rid, atom3Rid); 
                    double[] p0 = Geometry3D.normalize(d1);
                    double[] p1 = Geometry3D.normalize( Geometry3D.crossProduct(d2,p0)); 
                    double[] v  = Geometry3D.scale( p1,TET_TAN );
                             v  = Geometry3D.normalize( Geometry3D.add( p0,v ));
                    double[] coor = Geometry3D.add( gumbo.atom.getVector( atom1Rid),
                            Geometry3D.scale(v,bondLenght));
                    int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                    atomsAddedThisCycle.set(newAtomIdx);
                } else { //  worst case: add one and get rest next time around
                    double[] coor = Geometry3D.randomSphere( gumbo.atom.getVector( atom1Rid), bondLenght);                        
                    int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                    atomsAddedThisCycle.set(newAtomIdx);
                }                        
            } // end of adding THREE
            
            
            // ---4--- missing four atoms
            ntodo = 4;
            for (int a=0;a<need[ntodo-1].size();a++) {
//                General.showOutput("Doing atom " +a + " that needs FOUR atoms added");
                ArrayList store = (ArrayList) need[ntodo-1].get(a);
                int atom1Rid = ((Integer) store.get(0)).intValue(); // atom idx of anchor; atom with known coordinates
                BitSet miss = (BitSet) store.get(1);
                int resRid = gumbo.atom.resId[atom1Rid];
                String resName = nameList[resRid];
                String atomName1 = gumbo.atom.nameList[atom1Rid];
                int atom2Rid = miss.nextSetBit(0); // FIRST atom idx with missing coor.
                String atomName2 = topAtom.nameList[atom2Rid];                
                double bondLenght = topBond.getBondLength(resName, atomName1, atomName2);
                /** add coordinate and get the rest next time around */
                double[] coor = Geometry3D.randomSphere( gumbo.atom.getVector( atom1Rid), bondLenght);                        
                int newAtomIdx = gumbo.atom.add(atomName2, coor, resRid);   
                atomsAddedThisCycle.set(newAtomIdx);
            }                
            
            
            atomsAddedAllCycles.or(atomsAddedThisCycle);
            gumbo.resetConvenienceVariables();
            gumbo.removeAllIndices();
            General.showOutput("Added atoms in this cycle["+ncycle+"]:" + atomsAddedThisCycle.cardinality());
            int CUTOFF_TO_REPORT =100;
            if ( atomsAddedThisCycle.cardinality() > CUTOFF_TO_REPORT  ) {
                General.showOutput("Truncating the list here to: " + CUTOFF_TO_REPORT);
                PrimitiveArray.truncateElementsSet( atomsAddedThisCycle, CUTOFF_TO_REPORT);
            }
            General.showOutput(gumbo.atom.toString(atomsAddedThisCycle));

            if ( atomsAddedThisCycle.cardinality() == 0  ) {
//                General.showDebug("Last iteration done because no atoms were added.");
                break;
            }
            // Prevent infinite loop until debugged more.
            if ( ncycle >= 100 ) {
                General.showError("Failed to stop after 100 cycles of adding atoms");
                return false;
            }
        }
        General.showOutput("Added atoms in "+(ncycle-1)+" cycle(s):" + atomsAddedAllCycles.cardinality());
//        General.showOutput(gumbo.atom.toString(atomsAddedAllCycles));
        return true;
    }
    
    /** Returns null if none found 
    Finds none or two atoms with one of them being in the known list
    and the other one also has known coordinates and is preferably not
    a hydrogen.
    Note that special care needs to be given to the fact that bond info
    is defined on master atoms only; i.e. the first model in an entry.    
    */
    private int[] findKnownSecondary(int atomAnchorRid, BitSet know) {
        int atomAnchorMasterRid = gumbo.atom.masterAtomId[ atomAnchorRid ];
        BitSet knowMaster = gumbo.atom.getMasterRidSet(know);
//        General.showDebug("findKnownSecondary anchor        : " + gumbo.atom.toString(atomAnchorRid));
//        General.showDebug("findKnownSecondary known         : " + gumbo.atom.toString(know));
//        General.showDebug("findKnownSecondary anchor master : " + gumbo.atom.toString(atomAnchorMasterRid));
//        General.showDebug("findKnownSecondary known master  : " + gumbo.atom.toString(knowMaster));
        
        /** Store none preferred hydrogens as secondaries in case no other were found */
        ArrayList h_list = new ArrayList();
//        General.showDebug(gumbo.bond.toString(gumbo.bond.used));
        /** Starting at 1 for model 1 */
        int modelNumber = gumbo.model.number[ gumbo.atom.modelId[atomAnchorRid] ];
        for (int atomRidMasterKnown=knowMaster.nextSetBit(0);atomRidMasterKnown>=0;atomRidMasterKnown=knowMaster.nextSetBit(atomRidMasterKnown+1)) {            
            BitSet bondRidList = gumbo.bond.getBondListForAtomRid( atomRidMasterKnown );
//            General.showDebug("Found number of bonds: " + bondRidList.cardinality());
            int atomRidKnown = gumbo.atom.getAtomRidFromMasterRidAndModelNumber( atomRidMasterKnown, modelNumber);
            for (int bondRid=bondRidList.nextSetBit(0);bondRid>=0;bondRid=bondRidList.nextSetBit(bondRid+1)) {
//                General.showDebug(gumbo.bond.toString(bondRid));
                int atom2MasterRid = gumbo.bond.atom_A_Id[bondRid];
                if ( atom2MasterRid == atomRidMasterKnown ) {
                    atom2MasterRid = gumbo.bond.atom_B_Id[bondRid];
                }
                int atom2Rid = gumbo.atom.getAtomRidFromMasterRidAndModelNumber( atom2MasterRid, modelNumber);
//                General.showDebug("     to master atom: " + gumbo.atom.toString(atom2MasterRid));                
//                General.showDebug("     to        atom: " + gumbo.atom.toString(atom2Rid));                
                if ( atom2MasterRid != atomAnchorMasterRid ) {
                    // Obviously atom 2 has real coordinates already.
                    if ( gumbo.atom.elementId[atom2MasterRid] != Chemistry.ELEMENT_ID_HYDROGEN ) {
//                        General.showDebug("findKnownSecondary results preferred:");
//                        General.showDebug(gumbo.atom.toString(atomRidMasterKnown));
//                        General.showDebug(gumbo.atom.toString(atom2MasterRid));
                        return new int[] { atomRidKnown, atom2Rid };
                    } else {
                        h_list.add( new int[] { atomRidKnown, atom2Rid } );
                    }
                }                
            }                        
        }
        if (h_list.size()>0) {
            int[] ar = (int[] ) h_list.get(0);
//            General.showDebug("findKnownSecondary results with proton:");
//            General.showDebug(gumbo.atom.toString(ar[0]));
//            General.showDebug(gumbo.atom.toString(ar[1]));
            return ar;
        }
//        General.showDebug("findKnownSecondary --NO-- results.");
        return null;
    }

    /** Corrects when asked. All models will be done for the given master residues. 
     * */
    public boolean checkAtomNomenclature(boolean doCorrect, BitSet resInMaster) {
        int swapCount = 0;
        AtomNomenLib atomNomenLib = dbms.ui.wattosLib.atomNomenLib;
        int resRidMaster=resInMaster.nextSetBit(0);
        if ( resRidMaster <0 ) {
            General.showOutput("No residues found in master skipping checkAtomNomenclature in Residue");
            return true;
        }
        BitSet modelRidSet = gumbo.entry.getModelsInEntry( entryId[resRidMaster]);
        int modelCount = modelRidSet.cardinality();
        if ( modelCount == 0 ) {
            General.showOutput("No models found in master skipping checkAtomNomenclature in Residue");
            return true;
        }
        for (;resRidMaster>=0;resRidMaster=resInMaster.nextSetBit(resRidMaster+1)) {
            int modelId = -1;
//            General.showDebug("Working on master residue:");
//            General.showDebug(toString(resRidMaster));
            BitSet atomSetMasterRes = getAtoms(resRidMaster);
            if ( atomSetMasterRes.nextSetBit(0) < 0 ) {
//                General.showDebug("Skipping residue without coordinate atoms");
                continue;
            }
            for (int modelRid=modelRidSet.nextSetBit(0);modelRid>=0;modelRid=modelRidSet.nextSetBit(modelRid+1)) {
//                General.showDebug("Working on model:" );
//                General.showDebug(gumbo.model.toString(modelRid));
                modelId++; // will be zero at start.
                int resRid=getResRidFromModel(resRidMaster, modelId );
                if ( resRid <0 ) {
                    General.showError("Failed to find exact res rid");
                    return false;
                }
//                General.showDebug("Working on model residue:");
//                General.showDebug(toString(resRid));
                BitSet atomSet = getAtoms(resRid);
                String resName = nameList[resRid];
                
                HashMap recordListByResName = (HashMap) atomNomenLib.recordList.get(resName);
                if ( recordListByResName == null ) {
                    continue;
                }
                ArrayList stereoTypeList = new ArrayList(recordListByResName.keySet());
                Collections.sort( stereoTypeList );
                Collections.reverse( stereoTypeList );
                int sizeStereoTypeList = stereoTypeList.size();
                for (int i=0;i<sizeStereoTypeList;i++) {
                    Integer stereoTypeKey = (Integer) stereoTypeList.get(i);
                    int stereoType = stereoTypeKey.intValue();
                    ArrayList listOfSameStereoTypeAndSameResidueType = (ArrayList) recordListByResName.get(stereoTypeKey);
                    for (int j=0;j<listOfSameStereoTypeAndSameResidueType.size();j++) {
                        StringArrayList recordAtomList = (StringArrayList) listOfSameStereoTypeAndSameResidueType.get(j);
                        switch ( stereoType ) {
                            case AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_METHYLENE_CH2:
                            case AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_SEC_AMINO_NH2_ON_PRO_N_TERMINUS:
                            case AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_OXYGENS_ON_PHOSPHATES:
                            case AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_ISOPROPYL_CH3_2_ON_VAL_AND_LEU: {
                                // Example as if from e.g. PRO CB
                                int atomRidHB2  = getAtomRid (atomSet, recordAtomList.getString(0)); 
                                int atomRidHB3  = getAtomRid (atomSet, recordAtomList.getString(1)); 
                                int atomRidCB   = getAtomRid (atomSet, recordAtomList.getString(2)); // not really needed.
                                int atomRidCA   = getAtomRid (atomSet, recordAtomList.getString(3)); 
                                int atomRidCG   = getAtomRid (atomSet, recordAtomList.getString(4)); 
                                /** Special cased PRO non-N-term below to avoid warning on missing atoms.*/
                                if ( stereoType==AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_SEC_AMINO_NH2_ON_PRO_N_TERMINUS &&
                                        resName.equals("PRO") && (getNeighbour(resRid, -1)>=0)) {
                                    // not an N terminal Pro's to check.
                                    continue;
                                }
                                String perhapsAnO3Prime = recordAtomList.getString(3);
                                /** Special cased 5' end to avoid warning on missing atoms.*/
                                if ( stereoType==AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_OXYGENS_ON_PHOSPHATES &&
                                        perhapsAnO3Prime.equals("O3'") && (getNeighbour(resRid, -1)<0)) {
                                    // not an N terminal Pro's to check.
                                    continue;
                                }
                                if (    Defs.isNull(atomRidHB2) ||
                                        Defs.isNull(atomRidHB3) ||
                                        Defs.isNull(atomRidCB) ||
                                        Defs.isNull(atomRidCA) ||
                                        Defs.isNull(atomRidCG) ) {
                                    General.showWarning("Not all atoms found for determining this record for residue:");
                                    General.showWarning(toString(resRid));
                                    General.showWarning("This atom record list:");
                                    General.showWarning(recordAtomList.toString());
                                    continue;                        
                                }
                                if ( stereoType == AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_OXYGENS_ON_PHOSPHATES ) {
                                    // Make sure we get the previous atoms' O3' Not so easy.
                                    int resRidPrevious = getNeighbour(resRid, -1);
                                    if ( resRidPrevious < 0 ) {
                                        General.showDebug("Skipping OPs on residue without previous residue");
                                        continue;
                                    }
                                    BitSet atomSetPreviousRes = getAtoms(resRidPrevious);
                                    /** Missnamed as CA below; should be O3' */
                                    atomRidCA   = getAtomRid (atomSetPreviousRes, recordAtomList.getString(3));
                                    if (    Defs.isNull(atomRidCA) ) {
                                        General.showWarning("Atoms not found ["+recordAtomList.getString(3)+"] for determining this record for residue:");
                                        General.showWarning(toString(resRid));
                                        General.showWarning("This atom record list:");
                                        General.showWarning(recordAtomList.toString());
                                        continue;                        
                                    }
                                }
//                                General.showDebug("Checking stereo atom type: " + stereoType + " : " + gumbo.atom.toString(atomRidCB));
                                double correctAngle = gumbo.atom.calcDihedral(
                                        atomRidHB2, 
                                        atomRidHB3, 
                                        atomRidCA, 
                                        atomRidCG); 
                                double incorrectAngle = gumbo.atom.calcDihedral(
                                        atomRidHB3,
                                        atomRidHB2, 
                                        atomRidCA, 
                                        atomRidCG);
                                // toMinusPIPlusPIRange was called already
                                final double refAngleType0 = 72;
                                final double refAngleType5 = 65;
                                final double refAngleType7 = -70;
                                double refAngle = refAngleType0;
                                if ( stereoType == AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_ISOPROPYL_CH3_2_ON_VAL_AND_LEU) {
                                    refAngle = refAngleType5;
                                } else if ( stereoType == AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_OXYGENS_ON_PHOSPHATES) {
                                    refAngle = refAngleType7;
                                }
                                double diffCorrectAngle   = Geometry.differenceAngles(correctAngle,  refAngle);
                                double diffIncorrectAngle = Geometry.differenceAngles(incorrectAngle,refAngle);
                                
                                if ( Math.abs( diffCorrectAngle ) > Math.abs( diffIncorrectAngle ) ) {
//                                    General.showDebug(gumbo.atom.toString(atomRidHB2));
//                                    General.showDebug(gumbo.atom.toString(atomRidHB3));
//                                    General.showDebug(gumbo.atom.toString(atomRidCA));
//                                    General.showDebug(gumbo.atom.toString(atomRidCG));
//                                    General.showDebug("Improper angle tetrahedral center: " + Math.toDegrees(correctAngle) + " and " + Math.toDegrees(incorrectAngle));
//                                    General.showDebug("Swapping names for atoms:");
//                                    General.showDebug(gumbo.atom.toString(atomRidHB2));
//                                    General.showDebug(gumbo.atom.toString(atomRidHB3));
//                                    General.showDebug(General.eol);
                                    swapCount++;
                                    if ( doCorrect ) {
                                        gumbo.atom.swapNames( atomRidHB2, atomRidHB3 );
                                        for (int k=5;k<recordAtomList.size();k++) {
                                            String atomNameExtra = recordAtomList.getString(k);
                                            int atomRidExtra  = getAtomRid (atomSet, atomNameExtra); 
                                            if ( Defs.isNull(atomRidExtra) ) {
                                                General.showWarning("Atom not found for swappping: " +atomNameExtra);
                                                continue;
                                            } 
                                            General.showDebug(gumbo.atom.toString(atomRidExtra));
                                            gumbo.atom.swapNameForStereo( atomRidExtra );
                                        }
                                    }
                                }
                                break;
                            }                                                       
                            case AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_AROMATIC_C_AND_H_ON_PHE_AND_TYR: 
                            case AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_GUANIDINE_NH2_2_ON_ARG: 
                            case AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_OXYGENS_ON_ASP_GLU__AND_C_TERMINUS: 
                            case AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_AMIDE_NH2_ON_ASN_GLN_ARG_AND_NUCLEIC_ACIDS: {
                                // Example as if from GLN NE2
                                int atomRidHE21 = getAtomRid (atomSet, recordAtomList.getString(0)); 
                                int atomRidHE22 = getAtomRid (atomSet, recordAtomList.getString(1)); 
                                int atomRidCG   = getAtomRid (atomSet, recordAtomList.getString(2)); 
                                int atomRidCD   = getAtomRid (atomSet, recordAtomList.getString(3)); 
                                int atomRidNE2  = getAtomRid (atomSet, recordAtomList.getString(4));
                                if (    Defs.isNull(atomRidHE21) ||
                                        Defs.isNull(atomRidHE21) ||
                                        Defs.isNull(atomRidCG) ||
                                        Defs.isNull(atomRidCD) ||
                                        Defs.isNull(atomRidNE2)) {
                                    General.showWarning("Not all atoms found for determining this record for residue:");
                                    General.showWarning(toString(resRid));
                                    General.showWarning("This atom record list:");
                                    General.showWarning(recordAtomList.toString());
                                    continue;                        
                                }
                                if ( stereoType == AtomNomenLib.DEFAULT_PROCHIRAL_DEFINITIONS_OXYGENS_ON_ASP_GLU__AND_C_TERMINUS ) {
                                    String atomNameHydroxyl = "HD2";
                                    if ( resName.equals("GLU")) {
                                        atomNameHydroxyl = "HE2";
                                    }
                                    int atomHydroxylRid = gumbo.atom.getRidByAtomNameAndResRid(atomNameHydroxyl,resRid);
                                    if ( atomHydroxylRid >= 0 ) {
                                        General.showDebug("For a PROCHIRAL_DEFINITIONS_OXYGENS_ON_ASP_GLU__AND_C_TERMINUS with the hydroxyl present we skip checking stereospecificity");
                                        continue;
                                    }
                                }
                                
//                                General.showDebug("Checking stereo atom type: " + stereoType + " : " + gumbo.atom.toString(atomRidNE2));
                                double correctAngle = gumbo.atom.calcDihedral(
                                        atomRidCG, 
                                        atomRidCD, 
                                        atomRidNE2, 
                                        atomRidHE21); 
                                double incorrectAngle = gumbo.atom.calcDihedral(
                                        atomRidCG,
                                        atomRidCD, 
                                        atomRidNE2, 
                                        atomRidHE22);
                                if (  Math.abs( correctAngle ) > Math.abs( incorrectAngle ) ) {
//                                    General.showDebug(gumbo.atom.toString(atomRidCG));
//                                    General.showDebug(gumbo.atom.toString(atomRidCD));
//                                    General.showDebug(gumbo.atom.toString(atomRidNE2));
//                                    General.showDebug(gumbo.atom.toString(atomRidHE21));
//                                    General.showDebug("Angle planar center: " + Math.toDegrees(correctAngle) + " and " + Math.toDegrees(incorrectAngle));
//                                    General.showDebug("Swapping names for atoms:");
//                                    General.showDebug(gumbo.atom.toString(atomRidHE21));
//                                    General.showDebug(gumbo.atom.toString(atomRidHE22));
//                                    General.showDebug(General.eol);
                                    swapCount++;
                                    if ( doCorrect ) {
                                        gumbo.atom.swapNames( atomRidHE21, atomRidHE22 );
                                        for (int k=5;k<recordAtomList.size();k++) { // loop content not always executed.
                                            String atomNameExtra = recordAtomList.getString(k);
                                            int atomRidExtra  = getAtomRid (atomSet, atomNameExtra); 
                                            if ( Defs.isNull(atomRidExtra) ) {
                                                General.showWarning("Atom not found for swappping: " +atomNameExtra);
                                                continue;
                                            }
                                            General.showDebug(gumbo.atom.toString(atomRidExtra));
                                            gumbo.atom.swapNameForStereo( atomRidExtra );
                                        }
                                    }
                                }
                                break;
                            }
                            default: {
                                General.showWarning("Failed to code for defined stereo type: " + stereoType);
                                return false;
                            }
                        }
                    }
                }
            }
        }
        General.showOutput("Swapped     : " + swapCount + " times.");
        String perModelTimes = Strings.formatReal(swapCount/(float)modelCount, 2);
        General.showOutput("Per model   : " + perModelTimes + " times.");
        int resInMasterCount = resInMaster.cardinality();
        String perResTimes = Strings.formatReal(swapCount/((float)modelCount*resInMasterCount), 4);
        General.showOutput("Per residue : " + perResTimes + " times.");
        return true;
    }

    /** Simply looks for neighbours on both sides and does the logic */
    public boolean isTerminal(int rid) {
        return (getNeighbour(rid, -1) < 0) || 
               (getNeighbour(rid,  1) < 0);
    }
    /** Simply looks for N-term neighbours */
    public boolean isN_Terminal(int rid) {
        return getNeighbour(rid, -1) < 0;
    }
    /** Simply looks for C-term neighbours */
    public boolean isC_Terminal(int rid) {
        return getNeighbour(rid, 1) < 0;
    }
}
