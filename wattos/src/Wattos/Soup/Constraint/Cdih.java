package Wattos.Soup.Constraint;

import java.io.File;
import java.io.Serializable;
import java.util.BitSet;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexSortedInt;
import Wattos.Soup.Gumbo;
import Wattos.Star.DataBlock;
import Wattos.Star.SaveFrame;
import Wattos.Star.TagTable;
import Wattos.Utils.FloatIntPair;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringArrayList;
import Wattos.Utils.Strings;
import Wattos.Utils.Wiskunde.Geometry;
import cern.colt.list.IntArrayList;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 * Property of 3 atoms. Value contains the angle in degrees.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Cdih extends SimpleConstr implements Serializable {

    private static final long serialVersionUID = -1886027960322072182L;

    StringArrayList salChi2TyrOrPhe  = new StringArrayList( new String[] {"PHE", "TYR"} );

    public Cdih(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        // General.showDebug("back in angle constructor");
        resetConvenienceVariables();
    }

    /**
     * The relationSetName is a parameter so non-standard relation sets can be created; e.g. AtomTmp with a relation
     * named AtomTmpMain etc.
     */
    public Cdih(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        // General.showDebug("back in Cdih constructor");
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
        // General.showDebug("now in Cdih.init()");
        super.init(dbms);
        // General.showDebug("back in Cdih.init()");
        name = Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RELATION_ID_SET_NAME];
        // MAIN RELATION in addition to the ones in SimpleConstr item.
        DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME]);
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RELATION_ID_MAIN_RELATION_NAME] });

        relationName = Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RELATION_ID_MAIN_RELATION_NAME];
        atomRelationName = relationName + SimpleConstr.DEFAULT_ATTRIBUTE_ATOM_RELATION_EXTENSION;
        violRelationName = relationName + SimpleConstr.DEFAULT_ATTRIBUTE_VIOL_RELATION_EXTENSION;
        try {
            mainRelation = new Relation(relationName, dbms, this);
            simpleConstrAtom = new Relation(atomRelationName, dbms, this);
            simpleConstrViol = new Relation(violRelationName, dbms, this);
        } catch (Exception e) {
            General.showThrowable(e);
            return false;
        }

        // General.showDebug( "DEFAULT_ATTRIBUTES_TYPES in angle: " + Strings.toString(DEFAULT_ATTRIBUTES_TYPES));
        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO,
                relationName);
        mainRelation.insertColumnSet(0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, DEFAULT_ATTRIBUTE_VALUES,
                DEFAULT_ATTRIBUTE_FKCS);
        addRelation(mainRelation);

        // OTHER RELATIONS HERE
        // ..
        // General.showDebug( "DEFAULT_ATTRIBUTES_TYPES in angle: " + Strings.toString(DEFAULT_ATTRIBUTES_TYPES));
        // Create the fkcs without checking that the columns exist yet.
        A_DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME],
                new Integer(DATA_TYPE_INT));
        A_DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                new Integer(DATA_TYPE_INT));

        A_DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME]);
        A_DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME]);

        A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME],
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RELATION_ID_MAIN_RELATION_NAME] });
        A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RELATION_ID_MAIN_RELATION_NAME] });
        A_DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO,
                atomRelationName);
        simpleConstrAtom.insertColumnSet(0, A_DEFAULT_ATTRIBUTES_TYPES, A_DEFAULT_ATTRIBUTES_ORDER,
                A_DEFAULT_ATTRIBUTE_VALUES, A_DEFAULT_ATTRIBUTE_FKCS);
        addRelation(simpleConstrAtom);

        V_DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME],
                new Integer(DATA_TYPE_INT));
        V_DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                new Integer(DATA_TYPE_INT));
        V_DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME]);
        V_DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME]);
        V_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME],
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RELATION_ID_MAIN_RELATION_NAME] });
        V_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RELATION_ID_MAIN_RELATION_NAME] });

        simpleConstrViol.insertColumnSet(0, V_DEFAULT_ATTRIBUTES_TYPES, V_DEFAULT_ATTRIBUTES_ORDER,
                V_DEFAULT_ATTRIBUTE_VALUES, V_DEFAULT_ATTRIBUTE_FKCS);
        addRelation(simpleConstrViol);
        return true;
    }

    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();
        ATTRIBUTE_SET_SUB_CLASS = Constr.DEFAULT_ATTRIBUTE_SET_CDIH;
        ATTRIBUTE_SET_SUB_CLASS_LIST = Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST;
        scListIdMain = mainRelation.getColumnInt(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RELATION_ID_COLUMN_NAME]);
        scListIdAtom = simpleConstrAtom.getColumnInt(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RELATION_ID_COLUMN_NAME]);
        scIdAtom = simpleConstrAtom.getColumnInt(Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RELATION_ID_COLUMN_NAME]);

        scListIdViol = simpleConstrViol.getColumnInt(Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RELATION_ID_COLUMN_NAME]);
        scMainIdViol = simpleConstrViol.getColumnInt(Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RELATION_ID_COLUMN_NAME]);

        return true;
    }

    /**
     * Calculate the averaged value for the each given constraint in all selected models and puts all as new entries in
     * the violation relation. The violation will often be zero. Presumes the model sibling atoms are ok if initialized.
     * Skips constraints with unknown atoms.
     */
    public boolean calcViolation(BitSet todo, float cutoff) {
        if (todo.cardinality() == 0) {
            General.showWarning("No simple constraints selected in toSTAR.");
            return true;
        }
        BitSet scListRids = getSCListSetFromSCSet(todo);
        if (scListRids == null) {
            General.showError("Failed getSCListSetFromSCSet for CDIH");
            return false;
        }
        if (scListRids.cardinality() == 0) {
            General.showWarning("Got empty set from getSCListSetFromSCSet for CDIH.");
            return true;
        }
        for (int currentCDIHListId = scListRids.nextSetBit(0); currentCDIHListId >= 0; currentCDIHListId = scListRids.nextSetBit(currentCDIHListId + 1)) {
            if (!constr.cdihList.calcViolation(todo, currentCDIHListId, cutoff)) {
                General.showError("Failed to calcViol for cdih list: " + currentCDIHListId);
                return false;
            }
        }
        return true;
    }

    /**
     * Ambies are not allowed by XPLOR convention. ASSI { 6} (( segid "SH3 " and resid 53 and name N )) (( segid "SH3 "
     * and resid 53 and name CA )) { 6} (( segid "SH3 " and resid 53 and name C )) (( segid "SH3 " and resid 54 and name
     * C )) 1.00 -60.00 40.00 2 1st energy constant 2nd target 3rd range around target 4th exponent
     */
    public boolean toXplorOrSo(BitSet scSet, String fn, int fileCount, String atomNomenclature, boolean sortRestraints,
            String format) {
        int scCountTotal = scSet.cardinality();
        // General.showDebug( "Total number of  constraints todo: " + scCountTotal );
        if (scCountTotal == 0) {
            General.showWarning("No constraints selected in toXplor.");
            return true;
        }
        StringBuffer sb = new StringBuffer(scCountTotal * 80 * 5); // rough unimportant estimation of 80 chars per
                                                                   // restraint.

        // Write them in a sorted fashion if needed.
        int[] map = null;
        if (sortRestraints) {
            if (!sortAll(scSet, 0)) {
                General.showError("Couldn'sort the simple constraints");
                return false;
            }
            map = mainRelation.getRowOrderMap(Relation.DEFAULT_ATTRIBUTE_ORDER_ID); // Includes just the scs in this
                                                                                    // list
            if ((map != null) && (map.length != scCountTotal)) {
                General
                        .showWarning("Trying to get an order map but failed to give back the correct number of elements: "
                                + scCountTotal + " instead found: " + map.length);
                map = null;
            }
            if (map == null) {
                General
                        .showWarning("Failed to get the row order sorted out for  constraints; using physical ordering."); // not
                                                                                                                           // fatal
                map = PrimitiveArray.toIntArray(scSet);
                if (map == null) {
                    General.showError("Failed to get the used row map list so not writing this table.");
                    return false;
                }
            }
        } else {
            map = PrimitiveArray.toIntArray(scSet);
            if (map == null) {
                General.showError("Failed to get the used row map list so not writing this table.");
                return false;
            }
        }

        BitSet mapAsSet = PrimitiveArray.toBitSet(map, -1);
        if (mapAsSet == null) {
            General.showCodeBug("Failed to create bitset back from map.");
            return false;
        }
        BitSet mapXor = (BitSet) mapAsSet.clone();
        mapXor.xor(scSet);
        if (mapXor.cardinality() != 0) {
            General
                    .showError("The map after reordering doesn't contain all the elements in the original set or vice versa.");
            General.showError("In the original set:" + PrimitiveArray.toString(scSet));
            General.showError("In the ordered  set:" + PrimitiveArray.toString(mapAsSet));
            General.showError("Xor of both sets   :" + PrimitiveArray.toString(mapXor));
            General.showError("The order column   :"
                    + PrimitiveArray.toString(mainRelation.getColumnInt(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)));
            return false;
        }

        // Important to get the indexes after the sortAll.
        IndexSortedInt indexMainAtom = (IndexSortedInt) simpleConstrAtom.getIndex(
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME], Index.INDEX_TYPE_SORTED);
        if (indexMainAtom == null) {
            General.showCodeBug("Failed to get all indexes to sc main");
            return false;
        }

        StringBuffer sbRst = new StringBuffer();
        Parameters p = new Parameters();
        // FOR EACH CONSTRAINT
        int constraintId = 0;
        for (; constraintId < map.length; constraintId++) {
            sbRst.setLength(0);
            sbRst.append("assi{" + Format.sprintf("%5d", p.add((constraintId + 1))) + "}");
            int currentSCId = map[constraintId];
            // General.showDebug("Preparing  constraint: " + constraintId + " at rid: " + currentSCId);
            Integer currentSCIdInteger = new Integer(currentSCId);
            IntArrayList scAtoms = (IntArrayList) indexMainAtom.getRidList(currentSCIdInteger,
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
            // General.showDebug("Found the following rids of sc atoms in constraint: " +
            // PrimitiveArray.toString( scAtoms ));
            if (scAtoms == null) {
                General.showError("Failed to get any atoms");
                return false;
            }
            if (scAtoms.size() != 4) {
                General.showError("Failed to get exactly four atoms");
                return false;
            }
            if (!PrimitiveArray.orderIntArrayListByIntArray(scAtoms, orderAtom)) {
                General.showError("Failed to order atoms by order column");
                return false;
            }

            for (int atom_id = 0; atom_id < 4; atom_id++) {
                // FOR EACH ATOM in member
                int currentSCAtomId = scAtoms.getQuick(atom_id);
                int currentAtomId = atomIdAtom[currentSCAtomId];
                int currentResId = gumbo.atom.resId[currentAtomId];
                int currentMolId = gumbo.res.molId[currentResId];
                if (Defs.isNull(currentAtomId)) { // Was the atom actually matched in the structure?
                    General.showError("Got null for atomId in Cdih");
                    return false;
                }

                String atomName = gumbo.atom.nameList[currentAtomId];
                int resNum = gumbo.res.number[currentResId];
                String resName = gumbo.res.nameList[currentResId];
                int molNum = gumbo.mol.number[currentMolId];

                sbRst.append(constr.toXplorAtomSel(molNum, resNum, resName, atomName, atomNomenclature));
            } // end of loop per atom

            sbRst.append(" 1.0 "); // This number comes from xplor-nih 2.15/eginput/gb1_rcDih/dihed_g_all.tbl
            float[] xplorDistSet = toXplorSet((float) (lowBound[currentSCId] * Geometry.CF),
                    (float) (uppBound[currentSCId] * Geometry.CF));
            if (xplorDistSet == null) {
                General.showWarning("Failed to convert to xplor for restraint: " + toString(currentSCId));
                General.showWarning("Skipping restraint");
                continue;
            }
            for (int i = 0; i < 2; i++) {
                sbRst.append(Format.sprintf("%6.1f ", p.add(xplorDistSet[i])));
            }

            sbRst.append("2\n"); // This number comes from xplor-nih 2.15/eginput/gb1_rcDih/dihed_g_all.tbl
            sb.append(sbRst.toString()); // need to instantiate the string?
        } // end of loop over restraints.
        fn = fn + "_di";
        String outputFileName = InOut.addFileNumberBeforeExtension(fn, fileCount, true, 3);
        File f = new File(outputFileName + ".tbl");
        InOut.writeTextToFile(f, sb.toString(), true, false);
        General.showOutput("Written " + Strings.sprintf(constraintId, "%5d") + " cdihs to: " + f.toString());
        return true;
    }

    /** Add to the tagtable the info for the current CDIH */
    public boolean toSTAR(TagTable tT, int currentCDIHId, int currentCDIHListId, int cdihCount, int listNumber) {

        if (hasUnLinkedAtom.get(currentCDIHId)) {
            General.showWarning("Skipping toSTAR rendering of atom ids for constraint at rid: " + currentCDIHId
                    + " because not all atoms are linked.");
        }

        int rowIdx = tT.getNewRowId();
        if (rowIdx < 0) {
            General.showError("Failed tT.getNewRowId.");
            return false;
        }
        FloatIntPair fip = getMaxViolAndModelNumber(currentCDIHId);
        float maxViol = fip.f;
        int max_violation_model_number = fip.i;

        float[] valueList = getModelValuesFromViol(currentCDIHId);
        if (valueList == null) {
            General.showError("Failed to getModelValuesFromViol for toSTAR");
            return false;
        }
        double av = Geometry.averageAngles(PrimitiveArray.toDoubleArray(valueList));

        double[] minMaxDistDouble = Geometry.getMinMaxAngle(PrimitiveArray.toDoubleArray(valueList));
        float[] minMaxDist = PrimitiveArray.toFloatArray(minMaxDistDouble);
        float[] violList = getModelViolationsFromViol(currentCDIHId);
        if (violList == null) {
            General.showError("Failed to getModelViolationsFromViol for toSTAR");
            return false;
        }
        CdihList cdihList = constr.cdihList;
        boolean smallerThanCutoff = false;
        String above_cutoff_violation_per_model = PrimitiveArray.toStringMakingCutoff(violList,
                cdihList.cutoff[currentCDIHListId], smallerThanCutoff);
        if (above_cutoff_violation_per_model == null) {
            General.showError("Failed PrimitiveArray.toStringMakingCutoff in toSTAR");
            return false;
        }
        above_cutoff_violation_per_model = "[" + above_cutoff_violation_per_model + "]";

        int countMakingCutoff = PrimitiveArray.countMakingCutoff(above_cutoff_violation_per_model);

        if (maxViol < Geometry.ANGLE_EPSILON) { // really helps to focus the eye.
            maxViol = Defs.NULL_FLOAT;
        }

        float maxViolDeg = Defs.NULL_FLOAT;
        if (!Defs.isNull(maxViol)) {
            maxViolDeg = (float) (maxViol * Geometry.CF);
        }
        if (!Defs.isNull(maxViolDeg) && (maxViolDeg > 180.0f)) {
            General.showCodeBug("Dihedral angle violation is over half a turn.");
            General.showCodeBug("Believe it or not but I've seen it happen in some python code;-)");
            General.showCodeBug(toString(currentCDIHId));
            return false;
        }

        float lowDeg = (float) Geometry.to_180(lowBound[currentCDIHId] * Geometry.CF);
        float uppDeg = (float) Geometry.to_180(uppBound[currentCDIHId] * Geometry.CF);
        float avDeg = Defs.NULL_FLOAT;
        if (!Defs.isNull(av)) {
            avDeg = (float) Geometry.to_180(av * Geometry.CF);
        }

        float minDeg = (float) Geometry.to_180(minMaxDist[0] * Geometry.CF);
        float maxDeg = (float) Geometry.to_180(minMaxDist[1] * Geometry.CF);

        tT.setValue(rowIdx, Relation.DEFAULT_ATTRIBUTE_ORDER_ID, cdihCount);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Restraint_ID, cdihCount + 1);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Torsion_angle_name, nameList[currentCDIHId]);

        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Angle_lower_bound_val, lowDeg);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Angle_upper_bound_val, uppDeg);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Angle_average, avDeg);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Angle_minimum, minDeg);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Angle_maximum, maxDeg);

        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Max_violation, maxViolDeg);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Max_violation_model_number, max_violation_model_number);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Above_cutoff_violation_count, countMakingCutoff);
        tT.setValue(rowIdx, cdihList.tagNameTA_constraint_stats_Above_cutoff_violation_per_model,
                above_cutoff_violation_per_model);

        // I'm not going to repeat 4*4 tags here so we'll go by order
        // int c = tT.getColumnIdx(cdihList.tagNameTA_constraint_stats_Comp_index_ID_1);
        int c = tT.getColumnIdx(cdihList.tagNameTA_constraint_stats_Entity_assembly_ID_1);

        IntArrayList atomRidList = getAtomRidList(currentCDIHId);
        for (int i = 0; i < atomRidList.size(); i++) {
            int atomRid = atomRidList.getQuick(i);
            String atomName = gumbo.atom.nameList[atomRid];
            int resRid = gumbo.atom.resId[atomRid];
            int molRid = gumbo.atom.molId[atomRid];
            int molNumber = gumbo.mol.number[molRid];
            String resName = gumbo.res.nameList[resRid];
            int resNumber = gumbo.res.number[resRid];

            tT.setValue(rowIdx, c, molNumber);
            tT.setValue(rowIdx, c + 1, resNumber);
            tT.setValue(rowIdx, c + 2, resName);
            tT.setValue(rowIdx, c + 3, atomName);
            c += 4;
        }
        return true;
    }

    /** Push this method up to SimpleConstr.java */
    public boolean isChi2TyrOrPhe(int atomRid) {
//      lastAtom = self.atoms[3]
//      atomName = getDeepByKeysOrAttributes(lastAtom,'name')
//      if atomName != 'CD1':
//          return False
//      lastAtomRes = lastAtom._parent
//      lastAtomResType = getDeepByKeysOrAttributes(lastAtomRes, 'db', 'name')
//      if lastAtomResType in ['TYR', 'PHE']:
//          return True
//      return False
        try {
            int resRid = gumbo.atom.resId[atomRid];
            String atomName = gumbo.atom.nameList[atomRid];
            String resName = gumbo.res.nameList[resRid];
            if ( ! atomName.equals("CD1")) {
                return false;
            }
            if ( salChi2TyrOrPhe.contains(resName)) {
                return true;
            }
        } catch (Throwable t) {
            General.showThrowable(t);
            General.showDebug("Now that we know that the code fails; let's continue less fancy.");
            return false;
        }
        return false;
    }

    /** Push this method up to SimpleConstr.java */
    public IntArrayList getAtomRidList(int rid) {
        IndexSortedInt indexMainAtom = (IndexSortedInt) simpleConstrAtom.getIndex(
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME], Index.INDEX_TYPE_SORTED);
        if (indexMainAtom == null) {
            General.showCodeBug("Failed to get indexMainAtom index.");
            return null;
        }
        IntArrayList scAtoms = (IntArrayList) indexMainAtom.getRidList(new Integer(rid),
                Index.LIST_TYPE_INT_ARRAY_LIST, null);
        if (scAtoms.size() != 4) {
            General.showError("Expected 4 atoms in dihedral");
            return null;
        }
        IntArrayList result = new IntArrayList(4);
        for (int i = 0; i < 4; i++) {
            result.add(atomIdAtom[scAtoms.getQuick(i)]);
        }
        return result;
    }

    /**
     * Calculate the averaged value for the given constraint in all given models. Presumes the model sibling atoms are
     * ok if initialized. The parameter selectedModelArray needs to contain the rids. If the parameter is null then all
     * models will be used. The models are derived from the sibling list of the first atom in the restraint.
     *
     * A returned result with a Defs.NULL_FLOAT for the first/first element indicates the restraint has unlinked atoms or a failure to
     * find all atoms.
     */
    public float[][] calcValue(int currentCDIHId, int[] selectedModelArray) {

        IndexSortedInt indexMainAtom = (IndexSortedInt) simpleConstrAtom.getIndex(
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME], Index.INDEX_TYPE_SORTED);
        if (indexMainAtom == null) {
            General.showCodeBug("Failed to get indexMainAtom index.");
            return null;
        }
        IntArrayList scAtoms = (IntArrayList) indexMainAtom.getRidList(new Integer(currentCDIHId),
                Index.LIST_TYPE_INT_ARRAY_LIST, null);
        if (scAtoms.size() != 4) {
            General.showError("Expected 4 atoms in dihedral");
            return null;
        }

        if (selectedModelArray == null) {
            int atomRidFirstAtom = atomIdAtom[scAtoms.get(0)];
            if (Defs.isNull(atomRidFirstAtom)) {
                General.showError("Failed to calcDihedral for restraint: " + toString(currentCDIHId));
                return null;
            }
            int[] tmp = gumbo.atom.modelSiblingIds[atomRidFirstAtom];
            if (tmp == null) {
                General.showError("found an empty gumbo.atom.modelSiblingIds[" + atomRidFirstAtom + "]");
                General.showError("where the atom is: " + gumbo.atom.toString(atomRidFirstAtom));
                return null;
            }
            selectedModelArray = new int[tmp.length];
            for (int m = 0; m < tmp.length; m++) { // And really fill it.
                selectedModelArray[m] = gumbo.atom.modelId[tmp[m]];
            }
        }
        int modelCount = selectedModelArray.length;

        int atomRidLastAtom = atomIdAtom[scAtoms.get(3)];
        boolean considerSymmetry = isChi2TyrOrPhe(atomRidLastAtom); // Hack for Phe/Tyr CHI2
        int swapSize = 1; // DEFAULT: no swap.
        if ( considerSymmetry ) {
            swapSize = 2;
        }
        float[][] result = new float[swapSize][modelCount];
        result[0][0] = Defs.NULL_FLOAT; // Indication of the restraint has unlinked atoms or a failure to find all atoms.

        if (hasUnLinkedAtom.get(currentCDIHId)) {
            General.showDetail("Skipping dihedral calculation for constraint at rid: " + currentCDIHId  + " because not all atoms are linked.");
            return result;
        }

        for (int swapIdx = 0; swapIdx < swapSize; swapIdx++ ) {
            // When the set of atoms involved is collected for the first model, the other models can easily be done too.
            int[] atomsInvolvedModel = new int[4];
            for (int currentModelId = 0; currentModelId < modelCount; currentModelId++) {
//                 General.showDebug("Working on model: " + (currentModelId+1)); // used to seeing model numbers starting at 1.
                // Get a new list of atoms for other models
                // The atoms in the first model need not be special coded although it could be done faster.
                for (int i = 0; i < atomsInvolvedModel.length; i++) {
                    int atomIdFirstModel = atomIdAtom[scAtoms.get(i)];
                    atomsInvolvedModel[i] = gumbo.atom.modelSiblingIds[atomIdFirstModel][currentModelId];
                }
                if ( swapIdx == 1 ) {
                    int lastAtomIdx = atomsInvolvedModel[3];
                    int ssaPartner = gumbo.atom.getSsaPartnerTry2(lastAtomIdx);
                    if ( Defs.isNull(ssaPartner)) {
                        General.showError("In Cdih.calcValue failed to atom.getSsaPartner(lastAtomIdx) for atom:");
                        General.showError(gumbo.atom.toString(lastAtomIdx));
                        return null;
                    }
                    atomsInvolvedModel[3] = ssaPartner;
                }
                float v = (float) gumbo.atom.calcDihedral(atomsInvolvedModel);
                if (Defs.isNull(v)) {
                    General.showError("Failed to calculate the torsion angle for constraint: " + currentCDIHId
                            + " in model: " + (currentModelId + 1) + ".");
                    return null;
                }
                result[swapIdx][currentModelId] = v;
            }
        }
        return result;
    }

    /**
     * Returns the (target, range) from (lower-, upperbound) or null if undetermined. Both input and output are in
     * degrees.
     * */
    public static float[] toXplorSet(float low, float upp) {
        // General.showDebug("low is: " + low);
        // General.showDebug("upp is: " + upp);
        // double[] set = new double[] { Math.toRadians((double)low),
        // Math.toRadians((double)upp) };
        // double tarRad=Geometry.averageAngles(set); // Fails for directly opposing values.
        // if ( Defs.isNull( tarRad ) ) {
        // return null;
        // }
        // double tar = Math.toDegrees(tarRad);
        // General.showDebug("tarRad is: " + tarRad);
        // General.showDebug("tar is: " + tar);
        // * The result is signed going from t (low) to s (upp). E.g. a result matrix is:
        double rangeRad = Geometry.differenceAngles(Math.toRadians((double) low), Math.toRadians((double) upp));
        double range = Math.toDegrees(rangeRad);
        // if ( Defs.isNull( rangeRad ) ) {
        // return null;
        // }
        double deviation = range / 2;
        double target = low + deviation;
        // rangeRad=Math.abs(rangeRad);
        // double ran = Math.toDegrees(rangeRad);
        // General.showDebug("ran is: " + ran);
        // return new float[] {(float)Math.toDegrees(tarRad),
        // (float)Math.toDegrees(rangeRad)};
        return new float[] { (float) target, (float) deviation };
    }

    /**
     * Returns a list of saveframes for each sc list or the empty string if there are no given restraints.
     */
    public String toSTAR(BitSet todo, float cutoffValue, boolean showViolations) {
        int todoCount = todo.cardinality();
        if (todoCount == 0) {
            General.showOutput("No simple constraints selected in toSTAR.");
            return "";
        }
        BitSet scListRids = getSCListSetFromSCSet(todo);
        if (scListRids == null) {
            General.showError("Failed getSCListSetFromSCSet");
            return null;
        }
        if (scListRids.cardinality() == 0) {
            General.showCodeBug("Got empty set from getSCListSetFromSCSet.");
            return null;
        }
        // Create star nodes
        DataBlock db = new DataBlock();
        if (db == null) {
            General.showError("Failed to init datablock.");
            return null;
        }
        int entryId = gumbo.entry.getEntryId();
        db.title = gumbo.entry.nameList[entryId];
        int scListCount = 1;
        for (int currentSCListId = scListRids.nextSetBit(0); currentSCListId >= 0; currentSCListId = scListRids
                .nextSetBit(currentSCListId + 1)) {
            General.showDebug("Doing constr.scList.toSTAR for scListCount: " + scListCount);
            SaveFrame sF = constr.cdihList.toSTAR(todo, scListCount, currentSCListId, showViolations, cutoffValue);
            // General.showDebug("Done with constr.scList.toSTAR for scListCount: " + scListCount);
            if (sF == null) {
                General.showError("Failed to generate saveframe for sc list: " + scListCount);
                return null;
            }
            sF.setTitle("dihedral_constraint_statistics_" + scListCount);
            if (scListCount != 1) {
                TagTable tT = sF.getTagTable(constr.cdihList.tagNameTA_constraint_stats_list_Details, true);
                tT.setValue(0, constr.cdihList.tagNameTA_constraint_stats_list_Details, Defs.NULL_STRING_DOT);
            }
            db.add(sF);
            scListCount++;
        }
        if (!db.toStarTextFormatting(dbms.ui.wattosLib.starDictionary)) {
            General.showWarning("Failed to format all columns as per dictionary this is not fatal however.");
        }
        String result = db.toSTAR();
        if (result == null) {
            General.showError("Failed to render sc results to STAR text.");
            return null;
        }
//        General.showDebug("From Cdih.toSTAR: ["+result+"]");
        return result;
    }


}
