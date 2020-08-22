/*
 * Triplet.java
 *
 * Created on January 11, 2006, 10:35 AM
 *
 * Encapsulates a pseudo atom and info for AssignStereo.
 */

package Wattos.Soup.Constraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;

import Wattos.Database.Defs;
import Wattos.Database.Relation;
import Wattos.Soup.Atom;
import Wattos.Soup.PseudoAtom;
import Wattos.Soup.PseudoLib;
import Wattos.Soup.Comparator.ComparatorTripletByAtom;
import Wattos.Star.SaveFrame;
import Wattos.Star.TagTable;
import Wattos.Utils.General;
import cern.colt.list.IntArrayList;

/**
 * A pair of atom(group)s and the representing pseudoatom.
 * 
 * @author jurgen
 *@see Wattos.Soup.Constraint.AssignStereo
 */
public class Triplet extends PseudoAtom {

    public int countRestraints = Defs.NULL_INT;
    public int countRestraintsUniqueAssigned = Defs.NULL_INT;
    public IntArrayList restraintRidList = new IntArrayList();

    /** Energy of low state averaged over models. */
    public float energyLowState;
    public float energyHighState;
    public float energyDifference;
    public float energyDifferencePercentage;
    public boolean swapped = false;
    public boolean assigned = false;
    public boolean deassigned = false;
    public boolean needsDeassignmentByMultiModelCriteria = false;

    /**
     * True for the models in which the assignment with the lowest overall energy is favoured according to the set
     * criteria.
     */
    public BitSet modelsFavoured = new BitSet();
    public int countModelsFavoured;
    public float percentageModelFavoured;

    public int orderAssigned = -1;
    /** Energy of all involved restraints for the two states original and swapped */
    public float[] totalEnergy = new float[2];
    /** Energy of all involved restraints for the two states per model */
    public float[][] perModelEnergy = new float[2][];
    /** Number of violations above threshold for a single model before deassignment */
    public int countViolationsAboveThresSingleModel = 0;
    /** Number of violations above threshold multiple models before deassignment */
    public int countViolationsAboveThresMultiModel = 0;
    /** Maximum unaveraged violation in Ang. before deassignment */
    public float maxViolation = 0f;

    /** Note that the string is given in Unix flavor */
    public static final String explanation = "\n" + "Description of the tags in this list:\n"
            + "*  1 * NMR-STAR 3 administrative tag\n" + "*  2 * NMR-STAR 3 administrative tag\n"
            + "*  3 * NMR-STAR 3 administrative tag\n" + "*  4 * Number of triplets (atom-group pair and pseudo)\n"
            + "*  5 * Number of triplets that were swapped\n" + "*  6 * Percentage of triplets that were swapped\n"
            + "*  7 * Number of deassigned triplets\n" + "*  8 * Percentage of deassigned triplets\n"
            + "*  9 * Number of models in ensemble\n"
            + "* 10 * Energy of the states with the lower energies summed for all triplets (Ang.**2)\n"
            + "* 11 * Energy of the states with the higher energies summed for all triplets (Ang.**2)\n"
            + "* 12 * Item 9-8\n"
            + "* 13 * Criterium for swapping assignment on the absolute energy difference (Ang.**2)\n"
            + "* 14 * Criterium for swapping assignment on the relative energy difference (Ang.**2)\n"
            + "* 15 * Criterium for swapping assignment on the percentage of models favoring a swap\n"
            + "* 16 * Criterium for deassignment on a single model violation (Ang.)\n"
            + "* 17 * Criterium for deassignment on a multiple model violation (Ang.)\n"
            + "* 18 * Criterium for deassignment on a percentage of models\n" + "* 19 * this tag\n" + "\n"
            + "Description of the tags in the table below:\n"
            + "*  1 * Chain identifier (can be absent if none defined)\n" + "*  2 * Residue number\n"
            + "*  3 * Residue name\n" + "*  4 * Name of pseudoatom representing the triplet\n"
            + "*  5 * Ordinal number of assignment (1 is assigned first)\n"
            + "*  6 * 'yes' if assignment state is swapped with respect to restraint file\n"
            + "*  7 * Percentage of models in which the assignment with the lowest\n"
            + "        overall energy is favoured\n"
            + "*  8 * Percentage of difference between lowest and highest overall energy\n"
            + "        with respect to the highest overall energy\n"
            + "*  9 * Difference between lowest and highest overall energy\n"
            + "* 10 * Energy of the highest overall energy state (Ang.**2)\n"
            + "* 11 * Energy of the lowest overall energy state (Ang.**2)\n"
            + "* 12 * Number of restraints involved with the triplet. The highest ranking\n"
            + "        triplet on this number, is assigned first\n"
            + "* 13 * Number of restraints involved with the triplet that are ambiguous\n"
            + "        besides the ambiguity from this triplet\n"
            + "* 14 * 'yes' if restraints included in this triplet are deassigned\n"
            + "* 15 * Maximum unaveraged violation before deassignment (Ang.)\n"
            + "* 16 * Number of violated restraints above threshold for a single model\n"
            + "        before deassignment (given by Single_mdl_crit_count)\n"
            + "* 17 * Number of violated restraints above threshold for a multiple models\n"
            + "        before deassignment (given by Multi_mdl_crit_count)\n"
            + "* 18 * NMR-STAR 3.0 administrative tag\n" + "* 19 * NMR-STAR 3.0 administrative tag";

    public Triplet(String name, int type, int atomRidFirst) {
        super(name, type, atomRidFirst);
    }

    /**
     * Look at the data and deassign the triplet based on:
     * 
     * <PRE>
     *  if ( ( maxViolation &gt;= single_model_violation_deassign_criterium ) ||
     *       ( perViolationOverMultiModelCriteriumAnyIndividualRestraint &gt;= multi_model_rel_violation_deassign_criterium)) {
     *      deassigned = true;
     *</PRE>
     */
    public boolean deassign(float single_model_violation_deassign_criterium,
            float multi_model_violation_deassign_criterium, float multi_model_rel_violation_deassign_criterium,
            DistConstr dc, PseudoLib pseudoLib) {

        for (int r = 0; r < restraintRidList.size(); r++) {
            int rstrtIdx = restraintRidList.getQuick(r);
            if (!compileViolationsTripletPerRestraint(rstrtIdx, dc, single_model_violation_deassign_criterium,
                    multi_model_violation_deassign_criterium, multi_model_rel_violation_deassign_criterium)) {
                General.showError("Failed to compileViolationsTripletPerRestraint for restraint: "
                        + dc.toString(rstrtIdx));
                return false;
            }
        }
        // int modelCount = perModelEnergy[0].length;
        // float perViolationOverMultiModelCriterium = (100f*countViolationsAboveThresMultiModel)/modelCount;

        if ((maxViolation >= single_model_violation_deassign_criterium) || needsDeassignmentByMultiModelCriteria) {
            deassigned = true;
            for (int r = 0; r < restraintRidList.size(); r++) {
                int rstrtIdx = restraintRidList.getQuick(r);
                if (!dc.deassignStereoAssignment(rstrtIdx, this, pseudoLib)) {
                    General.showError("Failed deAssignStereoAssignment for restraint:\n" + dc.toString(rstrtIdx));
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Look at the data and assign the triplet based on:
     * 
     * <PRE>
     *  if ( ( percentageModelFavoured &gt;= model_criterium ) &amp;&amp;
     *              ( energyDifference &gt;= energy_abs_criterium) &amp;&amp;
     *              ( energyDifferencePercentage &gt;= energy_rel_criterium ) &amp;&amp;
     *              ( totalEnergy[0] &gt; totalEnergy[1] )) {
     *             swapped = true;
     *</PRE>
     */
    public boolean assign(float energy_abs_criterium, float energy_rel_criterium, float model_criterium, DistConstr dc,
            PseudoLib pseudoLib) {

        if (totalEnergy[0] <= totalEnergy[1]) {
            // General.showDebug("criterium not met: totalEnergy[0] > totalEnergy[1]: " + totalEnergy[0] + " and " +
            // totalEnergy[1]);
            return true;
        }
        if (percentageModelFavoured < model_criterium) {
            // General.showDebug("criterium not met: percentageModelFavoured >= model_criterium");
            return true;
        }
        if (energyDifference < energy_abs_criterium) {
            // General.showDebug("criterium not met: energyDifference >= energy_abs_criterium");
            return true;
        }
        if (energyDifferencePercentage < energy_rel_criterium) {
            // General.showDebug("criterium not met: energyDifferencePercentage >= energy_rel_criterium");
            return true;
        }
        swapped = true;
        for (int r = 0; r < restraintRidList.size(); r++) {
            int rstrtIdx = restraintRidList.getQuick(r);
            dc.swapStereoAssignment(rstrtIdx, this, pseudoLib);
        }
        return true;
    }

    /**
     * Transfer the info in perModelEnergy and totalEnergy to energyLowState, modelsFavoured etc.
     */
    public boolean compileEnergyTriplet() {
        boolean energyLowStateIsOriginalState = true;

        if (totalEnergy[0] < totalEnergy[1]) {
            energyLowState = totalEnergy[0];
            energyHighState = totalEnergy[1];
        } else {
            energyLowStateIsOriginalState = false;
            energyLowState = totalEnergy[1];
            energyHighState = totalEnergy[0];
        }
        energyDifference = energyHighState - energyLowState;
        int modelCount = perModelEnergy[0].length;
        for (int m = 0; m < modelCount; m++) {
            modelsFavoured.set(m);
            if (perModelEnergy[0][m] < perModelEnergy[1][m]) {
                if (!energyLowStateIsOriginalState) {
                    modelsFavoured.clear(m);
                }
            } else {
                if (energyLowStateIsOriginalState) {
                    modelsFavoured.clear(m);
                }
            }
        }
        if (modelsFavoured != null) {
            countModelsFavoured = modelsFavoured.cardinality();
        }
        percentageModelFavoured = (100 * (float) countModelsFavoured) / (float) modelCount;
        if ((percentageModelFavoured < 0) || (percentageModelFavoured > 100)) {
            General.showError("wrong percentageModelFavoured");
            return false;
        }
        float energyDifference = energyHighState - energyLowState;
        energyDifferencePercentage = 100 * (energyDifference / energyHighState);
        if ((energyDifferencePercentage < 0) || (energyDifferencePercentage > 100)) {
            General.showError("wrong energyDifferencePercentage");
            return false;
        }
        return true;
    }

    /**
     * Returns the assignment statistics given the number of models in entry. After this call the list will be sorted by
     * the first atom in each pseudo atom.
     */
    public static boolean toSTAR(ArrayList list, Atom atom, int model_count, SaveFrame sF, AssignStereo aS) {

        // Use assembly name twice.
        sF.setTitle("assign_stereo");
        TagTable tTAssignIntro = (TagTable) sF.get(0);
        // tTAssignIntro.setValue(0, aS.ttagNameEntryName, assemblyName);
        TagTable tTAssign = (TagTable) sF.get(1);

        ComparatorTripletByAtom comp = new ComparatorTripletByAtom(atom.gumbo);
        Collections.sort(list, comp);
        // Parameters p = new Parameters();

        int tot_swapped = 0;
        int tot_deassigned = 0;
        // float dif_energy;
        // float per_ass;
        float per_swap;
        float per_deassigned;
        float sum_hi = 0.0f;
        float sum_lo = 0.0f;

        int tri_count = list.size();
        for (int i = 0; i < tri_count; i++) {
            Triplet t = (Triplet) list.get(i);
            if (t.swapped) {
                tot_swapped++;
            }
            if (t.deassigned) {
                tot_deassigned++;
            }
        }
        per_swap = (100f * tot_swapped / (float) tri_count);
        per_deassigned = (100f * tot_deassigned / (float) tri_count);

        int i = 0;
        /**
         * if ( i != 0 ) { General.showCodeBug("Expected first row to be numbered zero but is: " + i); return false; }
         */
        tTAssignIntro.setValue(i, Relation.DEFAULT_ATTRIBUTE_ORDER_ID, i);
        tTAssignIntro.setValue(i, aS.tagNameTriplet_count, tri_count);
        tTAssignIntro.setValue(i, aS.tagNameSwap_count, tot_swapped);
        tTAssignIntro.setValue(i, aS.tagNameSwap_percentage, per_swap);
        tTAssignIntro.setValue(i, aS.tagNameDeassign_count, tot_deassigned);
        tTAssignIntro.setValue(i, aS.tagNameDeassign_percentage, per_deassigned);
        tTAssignIntro.setValue(i, aS.tagNameModel_count, model_count);

        for (i = 0; i < tri_count; i++) {
            Triplet t = (Triplet) list.get(i);
            if (!t.toSTAR(atom, model_count, tTAssign, aS)) {
                General.showError("Failed: Triplet.toSTAR for triplet: " + t.toString());
                return false;
            }
            sum_hi += t.energyHighState;
            sum_lo += t.energyLowState;
        }

        tTAssignIntro.setValue(0, aS.tagNameTotal_energy_high_states, sum_hi);
        tTAssignIntro.setValue(0, aS.tagNameTotal_energy_low_states, sum_lo);
        tTAssignIntro.setValue(0, aS.tagNameExplanation, explanation);

        /** Reformat the columns by dictionary defs */
        if (!sF.toStarTextFormatting(aS.ui.wattosLib.starDictionary)) {
            General.showWarning("Failed to format all columns as per dictionary this is not fatal however.");
        }

        return true;
    }

    public boolean toSTAR(Atom atom, int model_count, TagTable tT, AssignStereo aS) {

        int atomRid = atomRids[0];
        int resRid = atom.resId[atomRid];
        int molRid = atom.molId[atomRid];
        int molNumber = atom.gumbo.mol.number[molRid];
        String resName = atom.gumbo.res.nameList[resRid];
        int resNumber = atom.gumbo.res.number[resRid];
        int i = tT.getNewRowId();
        if (i < 0) {
            General.showError("Failed tT.getNewRowId.");
            return false;
        }

        tT.setValue(i, Relation.DEFAULT_ATTRIBUTE_ORDER_ID, i);
        tT.setValue(i, aS.tagNameLabel_pseudo_ID, name);
        tT.setValue(i, aS.tagNameLabel_comp_index_ID, resNumber);
        tT.setValue(i, aS.tagNameLabel_comp_ID, resName);
        tT.setValue(i, aS.tagNameLabel_entity_ID, molNumber);
        tT.setValue(i, aS.tagNameAssignment_ID, orderAssigned);
        tT.setValue(i, aS.tagNameSwapped, swapped);
        tT.setValue(i, aS.tagNamePercentage_models_favoring, percentageModelFavoured);
        tT.setValue(i, aS.tagNamePercentage_energy_difference, energyDifferencePercentage);
        tT.setValue(i, aS.tagNameEnergy_difference, energyDifference);
        tT.setValue(i, aS.tagNameEnergy_high_state, energyHighState);
        tT.setValue(i, aS.tagNameEnergy_low_state, energyLowState);
        tT.setValue(i, aS.tagNameRestraint_count, countRestraints);
        tT.setValue(i, aS.tagNameRestraint_ambi_count, countRestraints - countRestraintsUniqueAssigned);
        tT.setValue(i, aS.tagNameDeassigned, deassigned);
        tT.setValue(i, aS.tagNameMaximum_violation, maxViolation);
        tT.setValue(i, aS.tagNameViolation_single_model_criterium_count, countViolationsAboveThresSingleModel);
        tT.setValue(i, aS.tagNameViolation_multi_model_criterium_count, countViolationsAboveThresMultiModel);
        return true;
    }

    /**
     * Calculates the energy contribution of one restraint over all models given the assignment state of the triplet to
     * be assigned.
     */
    public boolean compileEnergyTripletPerRestraint(int rstrtIdx, DistConstr dc, boolean swapState, PseudoLib pseudoLib) {
        float distanceDifference, value, energyModel;
        int state = 0;
        if (swapState) {
            dc.swapStereoAssignment(rstrtIdx, this, pseudoLib);
            state = 1;
        }
        // float unassignedCorrection = 0;
        float[] lowTargetUppBound = dc.getLowTargetUppTheoBound(rstrtIdx);
        float lower = lowTargetUppBound[0];
        float upper = lowTargetUppBound[2];

        int modelCount = perModelEnergy[0].length;
        float[] valueList = dc.calcDistance(rstrtIdx, null);
        if (valueList == null) {
            General.showError("Failed: calcDistance for restraint:\n" + dc.toString(rstrtIdx));
            return false;
        }

        for (int modelId = 0; modelId < modelCount; modelId++) {
            value = valueList[modelId];
            energyModel = 0;
            if (!Defs.isNull(upper)) {
                if (value > upper) {
                    distanceDifference = value - upper;
                    energyModel = distanceDifference * distanceDifference;
                }
            }
            if (!Defs.isNull(lower)) {
                if (value < lower) {
                    distanceDifference = lower - value;
                    energyModel = distanceDifference * distanceDifference;
                }
            }
            /**
             * General.showDebug("triplet: " + orderAssigned + " restraint:   " + rstrtIdx + " state: " + state +
             * " model: " + modelId + " energyModel: " + energyModel);
             */
            float energyModelDividedByModelCount = energyModel / modelCount;
            totalEnergy[state] += energyModelDividedByModelCount;
            perModelEnergy[state][modelId] += energyModelDividedByModelCount;
        }
        // swap back to original state for now.
        if (swapState) {
            dc.swapStereoAssignment(rstrtIdx, this, pseudoLib);
        }
        return true;
    }

    /**
     * Calculates the energy contribution of one restraint over all models given the assignment state of the triplet to
     * be assigned.
     */
    public boolean compileViolationsTripletPerRestraint(int rstrtIdx, DistConstr dc,
            float single_model_violation_deassign_criterium, float multi_model_violation_deassign_criterium,
            float multi_model_rel_violation_deassign_criterium) {

        int countViolationsAboveThresMultiModelThisRestraint = 0;

        float[] lowTargetUppBound = dc.getLowTargetUppTheoBound(rstrtIdx);
        float lower = lowTargetUppBound[0];
        float upper = lowTargetUppBound[2];

        int modelCount = perModelEnergy[0].length;
        float[] valueList = dc.calcDistance(rstrtIdx, null);
        if (valueList == null) {
            General.showError("Failed: calcDistance for restraint:\n" + dc.toString(rstrtIdx));
            return false;
        }

        if ((!Defs.isNull(lower)) && (!Defs.isNull(upper)) && (lower > upper)) {
            General.showWarning("Found lower > upper: " + lower + " > " + upper
                    + " ignoring restraint in Triplet.compileViolationsTripletPerRestraint");
            return true;
        }

        for (int modelId = 0; modelId < modelCount; modelId++) {
            float viol = 0f;
            float value = valueList[modelId];
            // Use a simple algorithm that's valid because upper > lower
            if (!Defs.isNull(upper)) {
                if (value > upper) {
                    viol = value - upper;
                }
            }
            if (!Defs.isNull(lower)) {
                if (value < lower) {
                    viol = lower - value;
                }
            }
            if (viol < 0f) {
                General.showCodeBug("Unexpected negative violation");
                return false;
            }
            if (viol > maxViolation) {
                maxViolation = viol;
            }
            if (viol >= single_model_violation_deassign_criterium) {
                countViolationsAboveThresSingleModel++;
            }
            if (viol >= multi_model_violation_deassign_criterium) {
                countViolationsAboveThresMultiModel++;
                countViolationsAboveThresMultiModelThisRestraint++;
            }

            if (viol > multi_model_violation_deassign_criterium) {
                if (modelId == 0) {
                    General.showDebug("rstrtIdx: " + rstrtIdx + " modelId: " + modelId + " upper: " + upper
                            + " value: " + value + " viol: " + viol);
                }
            }
        }
        // Needs to be compiled per restraint. Might be set to true multiple times.
        float perViolationOverMultiModelCriteriumThisRestraint = (100f * countViolationsAboveThresMultiModelThisRestraint)
                / modelCount;
        if (perViolationOverMultiModelCriteriumThisRestraint >= multi_model_rel_violation_deassign_criterium) {
            needsDeassignmentByMultiModelCriteria = true;
        }
        return true;
    }
}
