/*
 * AssignStereo.java
 *
 * Created on January 11, 2006, 9:50 AM
 */

package Wattos.Soup.Constraint;

import java.util.*;
import cern.colt.list.*;
import Wattos.Utils.*;
import Wattos.Soup.*;
import Wattos.Soup.Comparator.ComparatorTripletByRestraints;
import Wattos.CloneWars.*;
import Wattos.Database.*;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Star.*;

/**
 * Swaps or deassigns stereospecific protons. For an explanation see {@link #doAssignStereo}. Routines are tested by the
 * JUnit test for Wattos.Star.NMRStar.File31.
 *
 * @author jurgen
 * @see #doAssignStereo
 */
public class AssignStereo {

    public UserInterface ui;
    public DistConstr dc;
    public DistConstrList dcList;
    public Gumbo gumbo;
    public Atom atom;
    public PseudoLib pseudoLib;
    public StarDictionary starDict;

    public ComparatorTripletByRestraints comparatorTripletByRestraints = new ComparatorTripletByRestraints();

    // public String tagNameListEntryID;
    // public String tagNameListId;
    public String tagNameSFCategory;
    public String tagNameTriplet_count;
    public String tagNameSwap_count;
    public String tagNameSwap_percentage;
    public String tagNameDeassign_count;
    public String tagNameDeassign_percentage;
    public String tagNameModel_count;
    public String tagNameTotal_energy_low_states;
    public String tagNameTotal_energy_high_states;
    public String tagNameCriterium_absolute_energy_difference;
    public String tagNameCriterium_relative_energy_difference;
    public String tagNameCriterium_percentage_models_favoring;
    public String tagNameCriterium_deassign_single_model_violation;
    public String tagNameCriterium_deassign_multiple_model_violation;
    public String tagNameCriterium_deassign_multiple_model_percentage;
    public String tagNameExplanation;
    // public String tagNameEntryID;
    // public String tagNameId;
    public String tagNameLabel_pseudo_ID;
    public String tagNameLabel_comp_index_ID;
    public String tagNameLabel_comp_ID;
    public String tagNameLabel_entity_ID;
    public String tagNameAssignment_ID;
    public String tagNameSwapped;
    public String tagNamePercentage_models_favoring;
    public String tagNamePercentage_energy_difference;
    public String tagNameEnergy_difference;
    public String tagNameEnergy_high_state;
    public String tagNameEnergy_low_state;
    public String tagNameRestraint_count;
    public String tagNameRestraint_ambi_count;
    public String tagNameDeassigned;
    public String tagNameMaximum_violation;
    public String tagNameViolation_single_model_criterium_count;
    public String tagNameViolation_multi_model_criterium_count;

    public AssignStereo(UserInterface ui) {
        this.ui = ui;
        dc = ui.constr.dc;
        dcList = ui.constr.dcList;
        gumbo = ui.gumbo;
        atom = gumbo.atom;
        pseudoLib = ui.wattosLib.pseudoLib;
        starDict = ui.wattosLib.starDictionary;
    }

    /**
     * Swap stereospecific assignments if the swapped state has a lower energy (sum of the squared bound violations) in
     * more than a given percentage of models. An atom(-group) pair and the representing pseudo is called a triplet in
     * this routine.
     * <P>
     * Consecutively deassign the triplets if they meet the violation criteria set. If a single restraint in a single
     * model is violated more than the parameter single_model_violation_deassign_criterium Angstrom, then all restraints
     * involved with this triplet will be deassigned for the ambiguity in this triplet. If a single restraint violates
     * more than parameter <code>multi_model_rel_violation_deassign_criterium</code> percent of the models more than
     * <CODE>multi_model_violation_deassign_criterium</CODE> Angstrom then all restraints involved with this triplet
     * will be deassigned for the ambiguity in this triplet.
     *
     * <P>
     * The swapping algorithm is as follows:
     *
     * <PRE>
     *  find list of triplets
     *  sort according to -1- total number of restraints involved
     *                    -2- number of restraints with unique assignments
     *  for each triplet T in order
     *      find set of restraints S containing T
     *      E is energy of S for the different models
     *      Eflip is energy of S with swapped stereospecific assignment for T
     *      if Eflip &lt; E for &gt;XX% of the models then
     *          swap the assignment for T
     * </PRE>
     *
     * <UL>
     * <LI>The algorithm is akin to the C code written in Aqua (AquaAssign module) and the Fortran code that Alexandre
     * Bonvin (Utrecht University) wrote for the statement "aria flip ...".
     * <LI>Atoms/atomgroups A that are considered to be swappable are: CH2_OR_NH2, TWO_METHYL, AROMAT_2H, but not
     * METHYL, TWO_NH2_OR_CH2, and AROMAT_4H. See {@link Wattos.Soup.PseudoLib} for the exact definitons.
     * </UL>
     * <BR>
     * <P>
     * This implementation in Wattos is similar to but not the same as the one defined in:<BR>
     * Nederveen,A.J., Doreleijers,J.F., Vranken,W., Miller,Z., Spronk,C.A., Nabuurs,S.B., Guntert,P., Livny,M.,
     * Markley,J.L., Nilges,M., Ulrich,E.L., Kaptein,R. and Bonvin,A.M. (2005). RECOORD: a recalculated coordinate
     * database of 500+ proteins from the PDB using restraints from the BioMagResBank. <I>Proteins</I> <B>59</B>,
     * 662-672. See {@link <a
     * href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&list_uids=15822098&dopt=Abstract"
     * >here< /a>}. <BR>
     *
     * @param energy_abs_criterium
     *            Absolute energy difference between the two assignment states that will cause a swap to be made. The
     *            energy is averaged by the number of models. Recommended value is 0.1 Ang**2. Set to a negative number
     *            to prevent swaps.
     * @param energy_rel_criterium
     *            Relative energy difference (as a percentage) between the two assignment states that will cause a swap
     *            to be made. Recommended value is 0 % and let the absolute energy difference criterium
     *            <CODE>energy_abs_criterium</CODE> do its job.
     * @param model_criterium
     *            Percentage of models that need to have the state swapped by the energy difference between the two
     *            assignment states that will cause a swap to be made. Recommended value is 49 % (or higher).
     * @param single_model_violation_deassign_criterium
     *            Deassignment criterium for single model violation (1.0 Angstrom suggested). Set this value and the
     *            parameter <CODE>multi_model_violation_deassign_criterium</CODE> to a high positive number (e.g. 999.9)
     *            to prevent any deassignment.
     * @param multi_model_violation_deassign_criterium
     *            Deassignment criterium for multiple model violation (0.5 Angstrom suggested)
     * @param multi_model_rel_violation_deassign_criterium
     *            Deassignment criterium for multiple model violation percentage (50 % suggested)
     * @param outputFileName
     *            File name for the STAR formatted result file (stereo_assign.str suggested)
     * @return <CODE>true</CODE> for success
     * @see Wattos.Soup.PseudoLib
     */
    public boolean doAssignStereo(float energy_abs_criterium, float energy_rel_criterium, float model_criterium,
            float single_model_violation_deassign_criterium, float multi_model_violation_deassign_criterium,
            float multi_model_rel_violation_deassign_criterium, String outputFileName) {

        if (!initConvenienceVariablesStar()) {
            General.showError("Failed: AssignStereo.initConvenienceVariablesStar");
            return false;
        }
        // GET LIST OF TRIPLETS
        // Keep track of which atoms are already in a triplet.
        BitSet inTriplet = new BitSet();
        BitSet atomList = gumbo.entry.getAtomsInMasterModel();
        if (atomList == null) {
            General.showError("could get a atoms in master model.");
            return false;
        }
        int entryId = gumbo.entry.getEntryId();
        BitSet modelList = gumbo.entry.getModelsInEntry(entryId);
        if (modelList == null) {
            General.showError("could get a model list.");
            return false;
        }
        int modelCount = modelList.cardinality();

        ArrayList<Triplet> tripletList = new ArrayList();
        for (int atomRid = atomList.nextSetBit(0); atomRid >= 0; atomRid = atomList.nextSetBit(atomRid + 1)) {
            if (inTriplet.get(atomRid)) {
                continue;
            }
            int resRid = atom.resId[atomRid];
            String resName = gumbo.res.nameList[resRid];
            String atomName = atom.nameList[atomRid];
            ArrayList pseudoAtomNameList = (ArrayList) pseudoLib.fromAtoms.get(resName, atomName);
            if (pseudoAtomNameList == null) { // atoms is not in known pseudoatom.
                continue;
            }
            for (int p = 0; p < pseudoAtomNameList.size(); p++) {
                String pseudoAtomName = (String) pseudoAtomNameList.get(p);
                Integer pseudoTypeInteger = (Integer) pseudoLib.pseudoAtomType.get(resName, pseudoAtomName);
                int pseudoType = pseudoTypeInteger.intValue();
                if (!isValidPseudoType(pseudoType)) { // ignore all but PseudoLib.DEFAULT_PSEUDO_ATOM_ID_CH2_OR_NH2
                    continue; // PseudoLib.DEFAULT_PSEUDO_ATOM_ID_AROMAT_2H
                } // PseudoLib.DEFAULT_PSEUDO_ATOM_ID_TWO_METHYL
                Triplet triplet = new Triplet(pseudoAtomName, pseudoType, atomRid);
                if (!triplet.findOtherAtoms(ui)) {
                    General.showWarning("Failed to triplet.findOtherAtoms; ignoring this triplet.");
                    continue;
                }
                BitSet atomRidSet = PrimitiveArray.toBitSet(triplet.atomRids);
                inTriplet.or(atomRidSet); // Make sure they're not added again.
                BitSet restraintRidSet = dc.getWithAtoms(atomRidSet);
                triplet.restraintRidList = PrimitiveArray.toIntArrayList(restraintRidSet);
                if (!removeRestraintsAmbiInTriplet(triplet)) {
                    General.showError("Failed to removeRestraintsAmbiInTriplet for triplet: " + triplet);
                    return false;
                }
                triplet.countRestraints = triplet.restraintRidList.size();
                if (triplet.countRestraints > 0) { // Don't bother for triplets not represented in restraints.
                    tripletList.add(triplet);
                    // General.showDebug("Added triplet: " + triplet.toString());
                } else {
                    // General.showDebug("Ignoring triplet without restraints: " + triplet.toString());
                }
            }
        }
        for (int t = 0; t < tripletList.size(); t++) {
            Triplet triplet = (Triplet) tripletList.get(t);
            triplet.countRestraintsUniqueAssigned = getUniqueAssignRestraintsInTriplet(triplet);
            // General.showDebug("Set more data for triplet: " + triplet.toStringForAssignmentTable(atom,modelCount));
        }

        for (int t = 0; t < tripletList.size(); t++) {
            int tripletId = pickNextBestTriplet(tripletList);
            if (tripletId < 0) {
                General.showError("Failed to pickNextBestTriplet: " + t);
                return false;
            }
            Triplet triplet = (Triplet) tripletList.get(tripletId);
            General.showDebug("Picked triplet for swapping: " + triplet.toString());
            triplet.orderAssigned = t + 1;
            triplet.assigned = true;
            triplet.perModelEnergy[0] = new float[modelCount];
            triplet.perModelEnergy[1] = new float[modelCount];
            for (int state = 0; state < 2; state++) { /* Loop over states */
                // General.showDebug("Compiling energies for state: " + state);
                boolean swap_state = false;
                if (state == 1) {
                    swap_state = true;
                }
                triplet.totalEnergy[state] = 0;
                for (int r = 0; r < triplet.restraintRidList.size(); r++) { /* Loop over restraints */
                    int rstrt_rid = triplet.restraintRidList.getQuick(r);
                    if (!triplet.compileEnergyTripletPerRestraint(rstrt_rid, dc, swap_state, pseudoLib)) {
                        General.showError("Failed to compileEnergyTripletPerRestraint for restraint:\n"
                                + dc.toString(rstrt_rid));
                        return false;
                    }
                }
            }
            if (!triplet.compileEnergyTriplet()) {
                General.showError("Failed to compileEnergyTriplet for triplet:\n" + triplet.toString());
                return false;
            }
            if (!triplet.assign(energy_abs_criterium, energy_rel_criterium, model_criterium, dc, pseudoLib)) {
                General.showError("Failed to assign triplet:\n" + triplet.toString());
                return false;
            }
        }

        // Reset the assigned attribute for pickNextBestTriplet
        for (int t = 0; t < tripletList.size(); t++) {
            Triplet triplet = (Triplet) tripletList.get(t);
            triplet.assigned = false;
        }
        // Deassign if needed.
        for (int t = 0; t < tripletList.size(); t++) {
            int tripletId = pickNextBestTriplet(tripletList);
            if (tripletId < 0) {
                General.showError("Failed to pickNextBestTriplet: " + t);
                return false;
            }
            Triplet triplet = (Triplet) tripletList.get(tripletId);
            General.showDebug("Picked triplet for deassignment (if needed): " + triplet.toString());
            if (!triplet.deassign(single_model_violation_deassign_criterium, multi_model_violation_deassign_criterium,
                    multi_model_rel_violation_deassign_criterium, dc, pseudoLib)) {
                General.showError("Failed to deassign (if needed) for triplet:\n" + t);
                return false;
            }
            triplet.assigned = true;
        }

        // create star nodes
        DataBlock db = new DataBlock();
        if (db == null) {
            General.showError("Failed to init datablock.");
            return false;
        }
        db.title = gumbo.entry.nameList[entryId];
        SaveFrame sF = getSFTemplate();
        if (sF == null) {
            General.showError("Failed to getSFTemplateStereoAssignment.");
            return false;
        }
        db.add(sF);

        int i = 0;
        TagTable tTAssignIntro = (TagTable) sF.get(0);
        // TagTable tTAssign = (TagTable) sF.get(1);
        tTAssignIntro.setValue(i, tagNameCriterium_absolute_energy_difference, energy_abs_criterium);
        tTAssignIntro.setValue(i, tagNameCriterium_relative_energy_difference, energy_rel_criterium);
        tTAssignIntro.setValue(i, tagNameCriterium_percentage_models_favoring, model_criterium);
        tTAssignIntro.setValue(i, tagNameCriterium_deassign_single_model_violation,
                single_model_violation_deassign_criterium);
        tTAssignIntro.setValue(i, tagNameCriterium_deassign_multiple_model_violation,
                multi_model_violation_deassign_criterium);
        tTAssignIntro.setValue(i, tagNameCriterium_deassign_multiple_model_percentage,
                multi_model_rel_violation_deassign_criterium);

        if (!Triplet.toSTAR(tripletList, atom, modelCount, sF, this)) {
            General.showError("Failed to Triplet.toSTAR");
            return false;
        }

        // Show on screen:
        String msg = db.toSTAR();
        if (msg == null) {
            General.showError("Failed to render Assign datanode to STAR");
            return false;
        }
        // General.showOutput("Assignment data in star format:" + General.eol + msg);
        General.showOutput("Assignment data written to file:" + outputFileName);

        if (!db.toSTAR(outputFileName)) { // same method name but different instance class
            General.showError("Failed to write file for this entry; not attempting any other entries.");
            return false;
        }
        return true;
    }

    /**
     * Returns best triplet not picked yet or -1 if none are left.
     *
     * @return <CODE>-1</CODE> for failure or best triplet id
     */
    public int pickNextBestTriplet(ArrayList tripletList) {
        Collections.sort(tripletList, comparatorTripletByRestraints);
        for (int i = (tripletList.size() - 1); i >= 0; i--) {
            Triplet triplet = (Triplet) tripletList.get(i);
            if (!triplet.assigned) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Simple count of the restraints associated with this triplet that are uniquely assigned with the exception of the
     * ambiguity possible from the given triplet.
     *
     * @return number for success or -1 for failure.
     */
    public int getUniqueAssignRestraintsInTriplet(Triplet triplet) {
        int result = 0;
        for (int r = 0; r < triplet.restraintRidList.size(); r++) {
            int dcRid = triplet.restraintRidList.getQuick(r);
            int status = isUniqueAssignedRestraint(dcRid, triplet);
            if (status == -1) {
                return -1;
            }
            if (status == 1) {
                result++;
            }
        }
        return result;
    }

    /**
     * Remove all restraints from triplet that are ambi in the triplet because those types of restraints don't need to
     * be considered. E.g. for triplet VAL QG a restraint VAL QG <-> GLY H will be removed but a restraint VAL MG <->
     * GLY H will not.
     *
     * @param triplet
     * @return <CODE>true</CODE> for success
     */
    public boolean removeRestraintsAmbiInTriplet(Triplet triplet) {
        for (int r = 0; r < triplet.restraintRidList.size(); r++) {
            int dcRid = triplet.restraintRidList.getQuick(r);
            // Get list of atoms L involved in restraint.
            BitSet atomDCAtomRidSet = SQLSelect.selectBitSet(ui.dbms, dc.distConstrAtom,
                    Constr.DEFAULT_ATTRIBUTE_SET_DC[RelationSet.RELATION_ID_COLUMN_NAME], // selection column
                    SQLSelect.OPERATION_TYPE_EQUALS, // selection operation
                    new Integer(dcRid), false); // selection value/uniqueness.
            BitSet atomRidSet = new BitSet();
            // the next scan is of course slower than doing it on the list which in general is short.
            for (int rid = atomDCAtomRidSet.nextSetBit(0); rid >= 0; rid = atomDCAtomRidSet.nextSetBit(rid + 1)) {
                atomRidSet.set(dc.atomIdAtom[rid]);
            }
            // Exclude the atoms in this triplet
            BitSet atomRidSetTriplet = PrimitiveArray.toBitSet(triplet.atomRids);
            // Check if all atoms in triplet are in the restraint. If they are then
            // the restraint is not going to make a difference when stereospecifically
            // assigning it.
            atomRidSet.and(atomRidSetTriplet);
            if (atomRidSet.equals(atomRidSetTriplet)) {
                // General.showDebug("remove the restraint: " + dc.toString(dcRid));
                triplet.restraintRidList.remove(r);
                r--;
            }
        }
        return true;
    }

    /**
     * Returns true when the restraint is uniquely assigned with the exception of the ambiguity possible from the given
     * triplet.<BR>
     * The algorithm:<BR>
     *
     * <PRE>
     *  Get list of atoms L involved in restraint.
     *  Delete the atoms from L present in the triplet.
     *  for each atom A in L:
     *      if atom A is not unique return false
     *  return true
     * </PRE>
     *
     * Notes: An atom A is unique if it is not in any pseudoatom considered or if it is in such pseudoatom and all other
     * members of the pseudoatom are also in L.
     *
     * @param dcRid
     *            Distance constraint id
     * @return -1 for failure, 1 for true and 0 for false.
     */
    public int isUniqueAssignedRestraint(int dcRid, Triplet triplet) {
        // Get list of atoms L involved in restraint.
        BitSet atomRidSet = dc.getAtomRidSet(dcRid);
        /**
         * General.showDebug("Doing isUniqueAssignedRestraint on restraint: " + dc.toString( dcRid ));
         * General.showDebug("     for triplet: " + triplet.toString()); General.showDebug("Atom rids involved: " +
         * PrimitiveArray.toString( PrimitiveArray.toIntArrayList(atomRidSet)));
         */
        BitSet atomRidSetTriplet = PrimitiveArray.toBitSet(triplet.atomRids);
        // Exclude the atoms in this triplet
        atomRidSet.andNot(atomRidSetTriplet);
        IntArrayList atomRidList = PrimitiveArray.toIntArrayList(atomRidSet);
        // General.showDebug("Atom rids involved without those in triplet: " + PrimitiveArray.toString(atomRidList));

        // Check uniqueness of each atom in list.
        for (int i = 0; i < atomRidList.size(); i++) {
            int atomRid = atomRidList.getQuick(i);
            if (Defs.isNull(atomRid)) { // this happened after a bug before this code. Could probably be taken out.
                General.showError("Failed to get valid atom rid assuming atom keeps restraint unique for restraint:"
                        + dc.toString(dcRid));
                return -1;
            }
            // General.showDebug("Checking atom: " + atom.toString(atomRid));
            int resRid = atom.resId[atomRid];
            String resName = gumbo.res.nameList[resRid];
            String atomName = atom.nameList[atomRid];
            ArrayList pseudoAtomList = (ArrayList) pseudoLib.fromAtoms.get(resName, atomName);
            if (pseudoAtomList == null) { // continue because this one is unique
                continue;
            }
            for (int p = 0; p < pseudoAtomList.size(); p++) {
                String pseudoAtomName = (String) pseudoAtomList.get(p);
                int pseudoType = ((Integer) pseudoLib.pseudoAtomType.get(resName, pseudoAtomName)).intValue();
                // E.g. Leu HD11 may be present if and only if all 6 atoms of QD are present.
                // E.g. For PHE HD1 it would [QR, QD]. QR is not a pseudoatom type considered
                // but QD is. So all atoms for QD need to be present.
                if (!isValidPseudoType(pseudoType)) { // Continue for e.g. For PHE HD1 and QR
                    continue;
                }
                // We end up here for e.g. PHE HD1 and QD.
                // the next method will filter out any atoms in the list that are not
                // related to the given atom and doubly occuring atoms if any.
                if (!pseudoLib.containsAllAtomsOfPseudo(resRid, pseudoAtomName, atomRidList, ui)) {
                    // General.showDebug("pseudoLib.containsAllAtomsOfPseudo: false" );
                    return 0;
                }
            }
        }
        return 1;
    }

    /**
     * Return true for a pseudo type that is under consideration
     */
    public static boolean isValidPseudoType(int pseudoType) {
        if ((pseudoType == PseudoLib.DEFAULT_PSEUDO_ATOM_ID_CH2_OR_NH2)
                || (pseudoType == PseudoLib.DEFAULT_PSEUDO_ATOM_ID_AROMAT_2H)
                || (pseudoType == PseudoLib.DEFAULT_PSEUDO_ATOM_ID_TWO_METHYL)) {
            return true;
        }
        return false;
    }

    /**
     * BEGIN BLOCK FOR SETTING NMR-STAR CONVENIENCE VARIABLES COPY FROM Wattos.Star.NMRStar.File31
     *
     * @return <CODE>true</CODE> for success
     */
    public boolean initConvenienceVariablesStar() {
        // Please note that the following names are not hard-coded as star names.
        try {
            // tagNameListEntryID = (String) ((ArrayList)starDict.toStar2D.get(
            // "stereo_assignments","_Stereo_assignments.ListEntry_ID" )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameListId = (String) ((ArrayList)starDict.toStar2D.get(
            // "stereo_assignments","_Stereo_assignments.ListID" )).get(StarDictionary.POSITION_STAR_TAG_NAME);

            tagNameSFCategory = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Sf_category")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameTriplet_count = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Triplet_count")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameSwap_count = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Swap_count")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameSwap_percentage = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Swap_percentage")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDeassign_count = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Deassign_count")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDeassign_percentage = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Deassign_percentage")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameModel_count = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Model_count")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameTotal_energy_low_states = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Total_energy_low_states")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameTotal_energy_high_states = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Total_energy_high_states")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCriterium_absolute_energy_difference = (String) ((ArrayList) starDict.toStar2D.get(
                    "stereo_assignments", "_Stereo_assignments.Criterium_absolute_energy_difference"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCriterium_relative_energy_difference = (String) ((ArrayList) starDict.toStar2D.get(
                    "stereo_assignments", "_Stereo_assignments.Criterium_relative_energy_difference"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCriterium_percentage_models_favoring = (String) ((ArrayList) starDict.toStar2D.get(
                    "stereo_assignments", "_Stereo_assignments.Criterium_percentage_models_favoring"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCriterium_deassign_single_model_violation = (String) ((ArrayList) starDict.toStar2D.get(
                    "stereo_assignments", "_Stereo_assignments.Criterium_deassign_single_model_violation"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCriterium_deassign_multiple_model_violation = (String) ((ArrayList) starDict.toStar2D.get(
                    "stereo_assignments", "_Stereo_assignments.Criterium_deassign_multiple_model_violation"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCriterium_deassign_multiple_model_percentage = (String) ((ArrayList) starDict.toStar2D.get(
                    "stereo_assignments", "_Stereo_assignments.Criterium_deassign_multiple_model_percentage"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameExplanation = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Explanation")).get(StarDictionary.POSITION_STAR_TAG_NAME);

            // tagNameEntryID = (String) ((ArrayList)starDict.toStar2D.get(
            // "stereo_assignments","_Stereo_assignments.Entry_ID" )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameId = (String) ((ArrayList)starDict.toStar2D.get( "stereo_assignments","_Stereo_assignments.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameLabel_pseudo_ID = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Label_pseudo_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameLabel_comp_index_ID = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Label_comp_index_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameLabel_comp_ID = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Label_comp_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameLabel_entity_ID = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Label_entity_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssignment_ID = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Assignment_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameSwapped = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Swapped")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNamePercentage_models_favoring = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Percentage_models_favoring")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNamePercentage_energy_difference = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Percentage_energy_difference")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEnergy_difference = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Energy_difference")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEnergy_high_state = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Energy_high_state")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEnergy_low_state = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Energy_low_state")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRestraint_count = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Restraint_count")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRestraint_ambi_count = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Restraint_ambi_count")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDeassigned = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Deassigned")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMaximum_violation = (String) ((ArrayList) starDict.toStar2D.get("stereo_assignments",
                    "_Stereo_assignments.Maximum_violation")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameViolation_single_model_criterium_count = (String) ((ArrayList) starDict.toStar2D.get(
                    "stereo_assignments", "_Stereo_assignments.Violation_single_model_criterium_count"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameViolation_multi_model_criterium_count = (String) ((ArrayList) starDict.toStar2D.get(
                    "stereo_assignments", "_Stereo_assignments.Violation_multi_model_criterium_count"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
        } catch (Exception e) {
            General.showThrowable(e);
            General.showError("Failed to get all the tag names from dictionary compare code with dictionary -A-");
            return false;
        }

//        if (false) {
//            String[] tagNames = {
//                    // tagNameListEntryID,
//                    // tagNameListId,
//                    tagNameSFCategory,
//                    tagNameTriplet_count,
//                    tagNameSwap_count,
//                    tagNameSwap_percentage,
//                    tagNameDeassign_count,
//                    tagNameDeassign_percentage,
//                    tagNameModel_count,
//                    tagNameTotal_energy_low_states,
//                    tagNameTotal_energy_high_states,
//                    tagNameCriterium_absolute_energy_difference,
//                    tagNameCriterium_relative_energy_difference,
//                    tagNameCriterium_percentage_models_favoring,
//                    tagNameCriterium_deassign_single_model_violation,
//                    tagNameCriterium_deassign_multiple_model_violation,
//                    tagNameCriterium_deassign_multiple_model_percentage,
//                    tagNameExplanation,
//                    // tagNameEntryID,
//                    // tagNameId,
//                    tagNameLabel_pseudo_ID, tagNameLabel_comp_index_ID, tagNameLabel_comp_ID, tagNameLabel_entity_ID,
//                    tagNameAssignment_ID, tagNameSwapped, tagNamePercentage_models_favoring,
//                    tagNamePercentage_energy_difference, tagNameEnergy_difference, tagNameEnergy_high_state,
//                    tagNameEnergy_low_state, tagNameRestraint_count, tagNameRestraint_ambi_count, tagNameDeassigned,
//                    tagNameMaximum_violation, tagNameViolation_single_model_criterium_count,
//                    tagNameViolation_multi_model_criterium_count, };
//            General.showDebug("Tagnames:\n" + Strings.toString(tagNames, true));
//        }

        if (

        // tagNameListEntryID == null ||
        // tagNameListId == null ||
        tagNameSFCategory == null
                || tagNameTriplet_count == null
                || tagNameSwap_count == null
                || tagNameSwap_percentage == null
                || tagNameDeassign_count == null
                || tagNameDeassign_percentage == null
                || tagNameModel_count == null
                || tagNameTotal_energy_low_states == null
                || tagNameTotal_energy_high_states == null
                || tagNameCriterium_absolute_energy_difference == null
                || tagNameCriterium_relative_energy_difference == null
                || tagNameCriterium_percentage_models_favoring == null
                || tagNameCriterium_deassign_single_model_violation == null
                || tagNameCriterium_deassign_multiple_model_violation == null
                || tagNameCriterium_deassign_multiple_model_percentage == null
                || tagNameExplanation == null
                ||

                // tagNameEntryID== null||
                // tagNameId== null||
                tagNameLabel_pseudo_ID == null || tagNameLabel_comp_index_ID == null || tagNameLabel_comp_ID == null
                || tagNameLabel_entity_ID == null || tagNameAssignment_ID == null || tagNameSwapped == null
                || tagNamePercentage_models_favoring == null || tagNamePercentage_energy_difference == null
                || tagNameEnergy_difference == null || tagNameEnergy_high_state == null
                || tagNameEnergy_low_state == null || tagNameRestraint_count == null
                || tagNameRestraint_ambi_count == null || tagNameDeassigned == null || tagNameMaximum_violation == null
                || tagNameViolation_single_model_criterium_count == null
                || tagNameViolation_multi_model_criterium_count == null) {
            General.showError("Failed to get all the tag names from dictionary, compare code with dictionary. -B-");
            return false;
        }
        return true;
    }

    /**
     * Returns a template with the star formatted output template
     */
    public SaveFrame getSFTemplate() {
        SaveFrame sF = new SaveFrame();
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        TagTable tT;
        // int RID;
        try {
            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            DBMS dbms = new DBMS(); // Use a temporary dbms for this because we don't
            // want to hold on to this data for ever.
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            namesAndTypes.put(tagNameSFCategory, new Integer(Relation.DATA_TYPE_STRING));
            // namesAndTypes.put( tagNameListEntryID, new Integer(Relation.DATA_TYPE_INT));
            // namesAndTypes.put( tagNameListId, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameTriplet_count, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameSwap_count, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameSwap_percentage, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameDeassign_count, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDeassign_percentage, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameModel_count, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameTotal_energy_low_states, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameTotal_energy_high_states, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameCriterium_absolute_energy_difference, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameCriterium_relative_energy_difference, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameCriterium_percentage_models_favoring, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameCriterium_deassign_single_model_violation, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes
                    .put(tagNameCriterium_deassign_multiple_model_violation, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameCriterium_deassign_multiple_model_percentage,
                    new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameExplanation, new Integer(Relation.DATA_TYPE_STRING));

            order.add(tagNameSFCategory);
            // order.add(tagNameListEntryID);
            // order.add(tagNameListId);
            order.add(tagNameTriplet_count);
            order.add(tagNameSwap_count);
            order.add(tagNameSwap_percentage);
            order.add(tagNameDeassign_count);
            order.add(tagNameDeassign_percentage);
            order.add(tagNameModel_count);
            order.add(tagNameTotal_energy_low_states);
            order.add(tagNameTotal_energy_high_states);
            order.add(tagNameCriterium_absolute_energy_difference);
            order.add(tagNameCriterium_relative_energy_difference);
            order.add(tagNameCriterium_percentage_models_favoring);
            order.add(tagNameCriterium_deassign_single_model_violation);
            order.add(tagNameCriterium_deassign_multiple_model_violation);
            order.add(tagNameCriterium_deassign_multiple_model_percentage);
            order.add(tagNameExplanation);
            namesAndValues.put(tagNameSFCategory, "stereo_assignments");
            // namesAndValues.put( tagNameListEntryID, new Integer(1));
            // namesAndValues.put( tagNameListId, new Integer(1));
            // Append columns after order id column.
            if (!tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add(tT);

            // ENTITIES
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            namesAndTypes.put(tagNameLabel_pseudo_ID, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameLabel_comp_index_ID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameLabel_comp_ID, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameLabel_entity_ID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameAssignment_ID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameSwapped, new Integer(Relation.DATA_TYPE_BIT));
            namesAndTypes.put(tagNamePercentage_models_favoring, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNamePercentage_energy_difference, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameEnergy_difference, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameEnergy_high_state, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameEnergy_low_state, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameRestraint_count, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRestraint_ambi_count, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDeassigned, new Integer(Relation.DATA_TYPE_BIT));
            namesAndTypes.put(tagNameMaximum_violation, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameViolation_single_model_criterium_count, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameViolation_multi_model_criterium_count, new Integer(Relation.DATA_TYPE_INT));
            // namesAndTypes.put( tagNameEntryID, new Integer(Relation.DATA_TYPE_INT));
            // namesAndTypes.put( tagNameId, new Integer(Relation.DATA_TYPE_INT));
            order.add(tagNameLabel_entity_ID);
            order.add(tagNameLabel_comp_index_ID);
            order.add(tagNameLabel_comp_ID);
            order.add(tagNameLabel_pseudo_ID);
            order.add(tagNameAssignment_ID);
            order.add(tagNameSwapped);
            order.add(tagNamePercentage_models_favoring);
            order.add(tagNamePercentage_energy_difference);
            order.add(tagNameEnergy_difference);
            order.add(tagNameEnergy_high_state);
            order.add(tagNameEnergy_low_state);
            order.add(tagNameRestraint_count);
            order.add(tagNameRestraint_ambi_count);
            order.add(tagNameDeassigned);
            order.add(tagNameMaximum_violation);
            order.add(tagNameViolation_single_model_criterium_count);
            order.add(tagNameViolation_multi_model_criterium_count);
            // order.add( tagNameEntryID);
            // order.add( tagNameId);

            // namesAndValues.put( tagNameEntryID, new Integer(1));
            // namesAndValues.put( tagNameId, new Integer(1));

            if (!tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add(tT);
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }
        return sF;
    }
}
