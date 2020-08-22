/*
 * Completeness.java
 *
 * Created on February 3, 2004, 3:40 PM
 */

package Wattos.Soup.Constraint;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.text.*;
import cern.colt.list.*;
import hep.aida.ref.*;
import Wattos.Soup.*;
import Wattos.Utils.*;
import Wattos.Utils.Wiskunde.*;
import Wattos.CloneWars.*;
import Wattos.Database.*;
import Wattos.Database.Indices.*;
import Wattos.Episode_II.Globals;
import Wattos.Star.*;
import Wattos.Star.NMRStar.*;
import Wattos.Utils.Charts.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;

/**
 *NOE distance completeness. The calculated completeness can be useful in the initial
 * phases of structure determination using NMR by focusing on NOE contacts
 * in specific regions in a biomolecule or pinpointing problems to specific residues,
 * atoms or classes of NOE contacts. The completeness check and its application
 * to a large set of structures is described in:
 * <UL>
 * <LI>
 * J.F. Doreleijers, M.L. Raves, J.A.C. Rullmann & R. Kaptein.
 * "Completeness of NOEs in proteins: a statistical analysis of NMR data"
 * <I>J. Biomol. NMR</I> (1999) <B>14</B>, 123-132.
 * </LI>
 * </UL>
 *
 *@see <a href="dc_completeness.html">Verbose description</a>
 *@see DistConstr
 * @author Jurgen F. Doreleijers
 */
public class Completeness {

    UserInterface ui;
    DistConstr dc;
    DistConstrList dcList;
    Gumbo gumbo;
    boolean doFancyAveraging;
    float min_dist_observed       = 0f;
    float max_dist_observed       = 0f;
    int numb_shells_observed      = 0;

    float min_dist_expected       = 0f;
    float max_dist_expected       = 0f;
    int numb_shells_expected      = 0;

    float avg_power_models        = 0f;
    int avg_method                = 0;
    int monomers                  = 0;

    DataBlock db = null;
    int[] todoModelArray = null;

    /** Parallel to the Atom mainrelation rows. Don't delete atoms in the meanwhile!*/
    BitSet atomsObservableSet = null;
    /** Hashed by possible pseudoatom is the list of constituent atoms.
     *Elements of this list are of the type PseudoAtom (may or may not contain more than 1 atom).
     *They will be sorted according to the first atom and
     *the alphabetical order of the pseudo atom name.
     */
    ArrayList atomsObservableCombos = null;
    /** Index from atom Rid to combo list id. */
    HashMap indexToCombo = null;
    /** Rid of the dc list with split contributions and theoretical contributions.*/
    int newDCListId;
    /** entry */
    int currentEntryId;
    /** The residue rids in the first model */
    BitSet resInModel = null;
    /** The residue rids in the first model that have observable atoms */
    BitSet resAtomsObservableSet = null;
    private static final int NO_SIBLING_FOUND = -1;

    /** Keeping track of restraints */
    BitSet USet = new BitSet();
    BitSet VSet = new BitSet();
    BitSet WSet = new BitSet();
    BitSet XSet = new BitSet();
    BitSet ESet = new BitSet();
    BitSet OSet = new BitSet();
    BitSet ISet = new BitSet();
    BitSet SSet = new BitSet();
    BitSet ASet = new BitSet();
    BitSet BSet = new BitSet();
    BitSet MSet = new BitSet();
    BitSet CSet = new BitSet();
    BitSet DSet = new BitSet();
    BitSet LSet = new BitSet();
    BitSet PSet = new BitSet();

    public String tagNameNOE_compl_listSf_category;
//    public String tagNameNOE_compl_listEntry_ID;
//    public String tagNameNOE_compl_listCompl_list_ID;
    public String tagNameNOE_compl_listRestraint_count;
    public String tagNameNOE_compl_listModel_count;
    public String tagNameNOE_compl_listResidue_count;
    public String tagNameNOE_compl_listObserv_atoms;
    public String tagNameNOE_compl_listUse_intra_resid;
    public String tagNameNOE_compl_listThreshold_redun;
    public String tagNameNOE_compl_listAveraging_power;
    public String tagNameNOE_compl_listCompl_cutoff;
    public String tagNameNOE_compl_listCompl_cumul;
    public String tagNameNOE_compl_listPair_count;
    public String tagNameNOE_compl_listRst_match_count;
    public String tagNameNOE_compl_listRst_unmat_count;
    public String tagNameNOE_compl_listObs_atom_count;
    public String tagNameNOE_compl_listTot_atom_count;
    public String tagNameNOE_compl_listRst_unexp_count;
    public String tagNameNOE_compl_listRst_excep_count;
    public String tagNameNOE_compl_listRst_nonob_count;
    public String tagNameNOE_compl_listRst_intra_count;
    public String tagNameNOE_compl_listRst_surpl_count;
    public String tagNameNOE_compl_listRst_obser_count;
    public String tagNameNOE_compl_listRst_expec_count;
    public String tagNameNOE_compl_listRst_exnob_count;
    public String tagNameNOE_compl_listDetails;
    public String tagNameNOE_compl_clasType;
    public String tagNameNOE_compl_clasRst_obser_count;
    public String tagNameNOE_compl_clasRst_expec_count;
    public String tagNameNOE_compl_clasRst_match_count;
    public String tagNameNOE_compl_clasCompl_cumul;
    public String tagNameNOE_compl_clasStand_deviat;
    public String tagNameNOE_compl_clasDetails;
//    public String tagNameNOE_compl_clasEntry_ID;
//    public String tagNameNOE_compl_clasCompl_list_ID;
    public String tagNameNOE_compl_resEntity_ID;
    public String tagNameNOE_compl_resComp_index_ID;
    public String tagNameNOE_compl_resComp_ID;
    public String tagNameNOE_compl_resObs_atom_count;
    public String tagNameNOE_compl_resRst_obser_count;
    public String tagNameNOE_compl_resRst_expec_count;
    public String tagNameNOE_compl_resRst_match_count;
    public String tagNameNOE_compl_resCompl_cumul;
    public String tagNameNOE_compl_resStand_deviat;
    public String tagNameNOE_compl_resDetails;
//    public String tagNameNOE_compl_resEntry_ID;
//    public String tagNameNOE_compl_resCompl_list_ID;
    public String tagNameNOE_compl_shellShell_start;
    public String tagNameNOE_compl_shellShell_end;
    public String tagNameNOE_compl_shellExpected_NOEs;
    public String tagNameNOE_compl_shellObs_NOEs_total;
    public String tagNameNOE_compl_shellMatched_NOEs;
    public String tagNameNOE_compl_shellObs_NOEs_shl_1;
    public String tagNameNOE_compl_shellObs_NOEs_shl_2;
    public String tagNameNOE_compl_shellObs_NOEs_shl_3;
    public String tagNameNOE_compl_shellObs_NOEs_shl_4;
    public String tagNameNOE_compl_shellObs_NOEs_shl_5;
    public String tagNameNOE_compl_shellObs_NOEs_shl_6;
    public String tagNameNOE_compl_shellObs_NOEs_shl_7;
    public String tagNameNOE_compl_shellObs_NOEs_shl_8;
    public String tagNameNOE_compl_shellObs_NOEs_shl_9;
    public String tagNameNOE_compl_shellObs_NOEs_shl_o;
    public String tagNameNOE_compl_shellDetails;
    public String tagNameNOE_compl_shellCompl_shell;
    public String tagNameNOE_compl_shellCompl_cumul;
//    public String tagNameNOE_compl_shellEntry_ID;
//    public String tagNameNOE_compl_shellCompl_list_ID;

    public StarDictionary starDict;
    /** Note that the string is given in Unix flavor */
    public static final String explanation;
    public static final String OVER_NUMBER_OF_SIGMAS_STR    = ">sigma";
    public static final String NO_MULTIMER_STR              = "no multimer";
    public static final String NO_INTRAS_STR                = "no intras";

    /** The eight is used for overflow */
    public static final int MAX_SHELLS_OBSERVED = 9;

    static {
        int i=1;
        String expl_1 = "\n" +
                "A detailed methodology description is available at:" +
                "\n" +
                Globals.wattos_home_page+"/doc/Wattos/Soup/Constraint/dc_completeness.html\n" +
                "\n" +
                "Please note that the contributions in ambiguous restraints are considered\n" +
                "separate 'restraints' for the sets defined below.\n" +
                "The cut off for all statistics except those in the by-shell table is\n" +
                "given below by the above tag: _NOE_completeness_stats.Completeness_cutoff\n" +
                "\n" +
                "Description of the tags in this list:\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Number of models\n" +
                "*  "+i+++" * Number of residues\n" +
                "*  "+i+++" * Number of atoms\n" +
                "*  "+i+++" * Standard set name of observable atom definitions\n" +
                "see: Doreleijers et al., J.Biomol.NMR 14, 123-132 (1999).\n" +
                "*  "+i+++" * Observable atom(group)s\n" +
                "*  "+i+++" * Include intra residue restraints\n" +
                "*  "+i+++" * Surplus threshold for determining redundant restraints\n" +
                "*  "+i+++" * Power for averaging the distance over models\n" +
                "*  "+i+++" * Up to what distance are NOEs expected\n" +
                "*  "+i+++" * Cumulative completeness percentage\n" +
                "*  "+i+++" * Number of unexpanded restraints in restraint list.\n" +
                "*  "+i+++" * Number of restraints in restraint list.           Set U\n" +
                "*  "+i+++" * Expected restraints based on criteria in list.    Set V\n" +
                "Set V differs from set B only if intra residue restraints are analyzed.\n" +
                "*  "+i+++" * Exceptional restraints, i.e. with an unknown atom.Set E\n" +
                "*  "+i+++" * Not observable NOEs with e.g. hydroxyl Ser HG.    Set O\n" +
                "Even though restraints with these atom types might have been observed they are\n" +
                "excluded from the analysis.\n" +
                "*  "+i+++" * Intra-residue restraints if not to be analyzed.   Set I\n" +
                "*  "+i+++" * Surplus like double restraints.                   Set S\n" +
                "*  "+i+++" * Observed restraints.                              Set A = U - (E u O u I u S)\n" +
                "*  "+i+++" * Expected restraints based on criteria as in A.    Set B = V - (I u S)\n" +
                "*  "+i+++" * Observed restraints matched to the expected.      Set M = A n B\n" +
                "*  "+i+++" * Observed restraints that were not expected.       Set C = A - M\n" +
                "*  "+i+++" * Expected restraints that were not observed.       Set D = B - M\n" +
                "*  "+i+++" * This tag\n";
        i=1;
        String expl_2 = "\n" +
                "Description of the tags in the class table:\n" +
                "*  "+i+++" * Class of restraint. Note that 'medium-range' involves (2<=i<=4) contacts.\n" +
                "Possible values are: intraresidue,sequential,medium-range,long-range, and intermolecular.\n" +
                "*  "+i+++" * Observed restraints.                              Set A = U - (E u O u I u S)\n" +
                "*  "+i+++" * Expected restraints based on criteria as in A.    Set B = V - (I u S)\n" +
                "*  "+i+++" * Observed restraints matched to the expected.      Set M = A n B\n" +
                "*  "+i+++" * Completeness percentage\n" +
                "*  "+i+++" * Standard deviation from the average over the classes.\n" +
                "*  "+i+++" * Extra information\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n";
        i=1;
        String expl_3 = "\n" +
                "Description of the tags in the residue table:\n" +
                "*  "+i+++" * Chain identifier\n" +
                "*  "+i+++" * Residue number\n" +
                "*  "+i+++" * Residue name\n" +
                "*  "+i+++" * Observable atom(group)s for this residue.\n" +
                "*  "+i+++" * Observed restraints.                              Set A = U - (E u O u I u S)\n" +
                "*  "+i+++" * Expected restraints based on criteria as in A.    Set B = V - (I u S)\n" +
                "*  "+i+++" * Observed restraints matched to the expected.      Set M = A n B\n" +
                "*  "+i+++" * Completeness percentage\n" +
                "*  "+i+++" * Standard deviation from the average over the residues.\n" +
                "*  "+i+++" * Extra information\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag";
        i=1;
        int j=1;
        String expl_4 = "\n" +
                "Description of the tags in the shell table.\n" +
                "The first row shows the lower limit of the shells requested and\n" +
                "The last row shows the total number of restraints over the shells.\n" +
                "*  "+i+++" * Description of the content of the row: edges, shell, or sums.\n" +
                "The value determines the meaning of the values to the nine 'Matched_shell_x' tags among others.\n" +
                "*  "+i+++" * Lower limit of shell of expected restraints.\n" +
                "*  "+i+++" * Upper limit of shell of expected restraints.\n" +
                //"*  "+i+++" * Observed restraints.                              Set A = U - (E u O u I u S)\n" +
                "*  "+i+++" * Expected restraints based on criteria as in A.    Set B = V - (I u S)\n" +
                "*  "+i+++" * Observed restraints matched to the expected.      Set M = A n B\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints with experimental distance in shell "+j+++"\n" +
                "*  "+i+++" * Matched restraints overflowing the last shell\n" +
                "*  "+i+++" * Completeness percentage for this shell\n" +
                "*  "+i+++" * Completeness percentage up to upper limit of this shell\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n";
        explanation = expl_1 + expl_2 + expl_4 + expl_3;
    }
    /** Creates a new instance of Completeness */
    public Completeness(UserInterface ui ) {
        this.ui = ui;
        dc      = ui.constr.dc;
        dcList  = ui.constr.dcList;
        gumbo   = ui.gumbo;
        starDict = ui.wattosLib.starDictionary;
        if ( ! initConvenienceVariablesStar()) {
            General.showError("Failed: Completeness.initConvenienceVariablesStar");
        }
    }

    private boolean setTagTableClasses(SaveFrame sF) {
        TagTable tT = (TagTable) sF.get(1);
        BitSet setClassMod = null;
        int rowIdx = 0;
        for (int class_iteration_id =0; class_iteration_id<DistConstr.DEFAULT_CLASS_NAMES.length;class_iteration_id++) {
            //General.showDebug("doing class: " + class_iteration_id);
            String classificationName = DistConstr.DEFAULT_CLASS_NAMES[ class_iteration_id ];
            if ( ! dc.mainRelation.containsColumn( classificationName ) ) {
                General.showError("Failed to find attribute: " + classificationName);
                continue; // skip this one but try the next
            }
            if ( (class_iteration_id == DistConstr.DEFAULT_CLASS_UNDETERMINED)||
                    (class_iteration_id == DistConstr.DEFAULT_CLASS_MIXED)) {
                continue;
            }
            rowIdx = tT.getNewRowId(); // do no error handeling.
            //General.showDebug("Got row id: " + rowIdx);
            BitSet setClass = dc.mainRelation.getColumnBit( classificationName );

            setClassMod = (BitSet) setClass.clone();
            setClassMod.and( ASet );
            int ASetLocalcount = setClassMod.cardinality();

            setClassMod = (BitSet) setClass.clone();
            setClassMod.and( BSet );
            int BSetLocalcount = setClassMod.cardinality();

            setClassMod = (BitSet) setClass.clone();
            setClassMod.and( MSet );
            int MSetLocalcount = setClassMod.cardinality();

            float completenessPercentage = Defs.NULL_FLOAT;
            if ( BSetLocalcount != 0 ) {
                completenessPercentage = 100f * MSetLocalcount / BSetLocalcount;
            }
            //General.showDebug("completenessPercentage is:" + completenessPercentage);
            tT.setValue(rowIdx, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,    rowIdx);
            tT.setValue(rowIdx, tagNameNOE_compl_clasType,              classificationName);
            tT.setValue(rowIdx, tagNameNOE_compl_clasRst_obser_count,   ASetLocalcount);
            tT.setValue(rowIdx, tagNameNOE_compl_clasRst_expec_count,   BSetLocalcount);
            tT.setValue(rowIdx, tagNameNOE_compl_clasRst_match_count,   MSetLocalcount);
            tT.setValue(rowIdx, tagNameNOE_compl_clasCompl_cumul,       completenessPercentage );
        }
        float[] av_sd = tT.getAvSd(tagNameNOE_compl_clasCompl_cumul);
        if ( av_sd == null ) {
            General.showWarning("Failed to get the average and standard deviation of a column: " +
                    tagNameNOE_compl_clasCompl_cumul + " leaving it blank.");
        } else {
            float av = av_sd[0];
            float sd = av_sd[1];
            if ( Defs.isNull(av) ) {
                General.showDebug("Failed to get av which is normal if there is no values.");
            } else if ( Defs.isNull(sd) ) {
                General.showDebug("Failed to get sd which is normal if there is only 1 value.");
            } else {
                rowIdx = 0;
                for (int class_iteration_id =0; class_iteration_id<DistConstr.DEFAULT_CLASS_NAMES.length;class_iteration_id++) {
                    if ( (class_iteration_id == DistConstr.DEFAULT_CLASS_UNDETERMINED)||
                            (class_iteration_id == DistConstr.DEFAULT_CLASS_MIXED)) {
                        continue;
                    }
                    float completenessPercentage = tT.getValueFloat(rowIdx,tagNameNOE_compl_clasCompl_cumul);
                    float sdItem = Defs.NULL_FLOAT;
                    if ( ! Defs.isNull(completenessPercentage) ) {
                        sdItem = Statistics.getNumberOfStandardDeviation(completenessPercentage,
                                av,sd);
                        tT.setValue(rowIdx, tagNameNOE_compl_clasStand_deviat, sdItem);
                        if ( Math.abs(sdItem) >= 1f) {
                            tT.setValue(rowIdx, tagNameNOE_compl_clasDetails, OVER_NUMBER_OF_SIGMAS_STR);
                        }
                    }
                    if ( class_iteration_id == DistConstr.DEFAULT_CLASS_INTER ) {
                        if ( Defs.isNull(completenessPercentage)) {
                            tT.setValue(rowIdx, tagNameNOE_compl_clasDetails, NO_MULTIMER_STR);
                        }
                    }
                    if ( class_iteration_id == DistConstr.DEFAULT_CLASS_INTRA ) {
                        if ( Defs.isNull(completenessPercentage)) {
                            tT.setValue(rowIdx, tagNameNOE_compl_clasDetails, NO_INTRAS_STR);
                        }
                    }
                    rowIdx++;
                }
            }
        }
        /**
         * // Show sums.
         * rowIdx = tT.getNewRowId(); // do no error handeling.
         * tT.setValue(rowIdx, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,    rowIdx);
         * tT.setValue(rowIdx, tagNameNOE_compl_clasType,              "sums");
         * tT.setValue(rowIdx, tagNameNOE_compl_clasRst_obser_count,   tT.getIntSum(tagNameNOE_compl_clasRst_obser_count));
         * tT.setValue(rowIdx, tagNameNOE_compl_clasRst_expec_count,   tT.getIntSum(tagNameNOE_compl_clasRst_expec_count));
         * tT.setValue(rowIdx, tagNameNOE_compl_clasRst_match_count,   tT.getIntSum(tagNameNOE_compl_clasRst_match_count));
         */
        return true;
    }

    /** Get the elements Mij etc. into a table.
     */
    private boolean setTagTableShell(SaveFrame sF) {
        TagTable tT = (TagTable) sF.get(2);

        // BiSet contributions in B that fall within shell i (exp)/ j (obs) distance
        String[] dcSetNamesB  = new String[numb_shells_expected];
        String[] dcSetNamesMi = new String[numb_shells_expected];
        String[] dcSetNamesMj = new String[numb_shells_observed];
        for (int s=0;s<numb_shells_expected;s++) {
            dcSetNamesB[s]  = "B"  + new Integer( s ).toString() + "Set";
            dcSetNamesMi[s] = "Mi" + new Integer( s ).toString() + "Set";
        }
        for (int s=0;s<numb_shells_observed;s++) {
            dcSetNamesMj[s] = "Mj" + new Integer( s ).toString() + "Set";
        }
        String[] dcSetNames = new String[] {};
        dcSetNames = Strings.append( dcSetNames, dcSetNamesB);
        dcSetNames = Strings.append( dcSetNames, dcSetNamesMi);
        dcSetNames = Strings.append( dcSetNames, dcSetNamesMj);

        // MiSet contributions in M that fall within shell i for the distance in the ensemble
        // MijSet contributions in B that fall within shell i for the distance in the ensemble and in shell j for
        //      the observed NOE distance. Any constraint in Mij where i<j is a violated constraint.
        for (int c=0;c< dcSetNames.length;c++) {
            if ( dc.mainRelation.containsColumn( dcSetNames[ c ] ) ) {
                if ( dc.mainRelation.getColumnDataType( dcSetNames[ c ]  ) != Relation.DATA_TYPE_BIT ) {
                    General.showWarning("Existing column isn't of type BitSet from dc main relation with name: " + dcSetNames[ c ] );
                }
                Object tmpObject = dc.mainRelation.removeColumn( dcSetNames[ c ] );
                if ( tmpObject == null ) {
                    General.showError("Failed to remove existing column from dc main relation with name: " + dcSetNames[ c ] );
                    return false;
                }
            }
            if ( ! dc.mainRelation.insertColumn( dcSetNames[ c ], Relation.DATA_TYPE_BIT, null )) {
                General.showError("Failed to insert bitset column to dc main relation with name: " + dcSetNames[ c ] );
                return false;
            }
        }

        BitSet[] dcBitSetB = new BitSet[numb_shells_expected];
        for (int s=0;s<numb_shells_expected;s++) {
            dcBitSetB[s] = dc.mainRelation.getColumnBit(dcSetNamesB[s]);
            if ( dcBitSetB[s] == null ) {
                General.showError("Failed to get BitSet for: " + dcSetNamesB[s]);
                return false;
            }
        }
        Histogram1D a1 = new Histogram1D("A", numb_shells_expected, min_dist_expected, max_dist_expected);
        Histogram1D b1 = new Histogram1D("B", numb_shells_expected, min_dist_expected, max_dist_expected);
        Histogram1D m1 = new Histogram1D("M", numb_shells_expected, min_dist_expected, max_dist_expected);
        Histogram2D m2 = new Histogram2D("Mj",numb_shells_expected, min_dist_expected, max_dist_expected, numb_shells_observed, min_dist_observed, max_dist_observed );
        FixedAxis xAxis = (FixedAxis) m2.xAxis();
        FixedAxis yAxis = (FixedAxis) m2.yAxis();

        for (int rid=ASet.nextSetBit(0);rid>=0;rid=ASet.nextSetBit(rid+1)) {
            float[] d = dc.getLowTargetUppTheoBound(rid);
            a1.fill(d[DistConstr.THE_IDX]);
        }
        for (int rid=BSet.nextSetBit(0);rid>=0;rid=BSet.nextSetBit(rid+1)) {
            float[] d = dc.getLowTargetUppTheoBound(rid);
            b1.fill(d[DistConstr.THE_IDX]);
        }
        for (int rid=MSet.nextSetBit(0);rid>=0;rid=MSet.nextSetBit(rid+1)) {
            float[] d = dc.getLowTargetUppTheoBound(rid);
            m1.fill(d[DistConstr.THE_IDX]);
            m2.fill(d[DistConstr.THE_IDX],d[DistConstr.UPP_IDX]);
        }
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        int rid = tT.getNewRowId();
        tT.setValue(rid, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,       rid);
        tT.setValue(rid, tagNameNOE_compl_shellDetails,          "edges");
        for (int j=0;j<=numb_shells_observed;j++) {
            if ( j != numb_shells_observed) {
                String numb = Integer.toString(j+1);
                String colName = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_" + numb);
                if ( ! tT.containsColumn(colName)) {
                    General.showCodeBug("Failed to find correct tag name for per shell info. 1");
                    return false;
                }
                tT.setValue(rid, colName, nf.format( yAxis.binLowerEdge(j)));
            }
            /**else {
             * String colName = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_o");
             * tT.setValue(rid, colName, ">");
             * }
             */
        }

        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);
        int sumB = 0;
        int sumM = 0;
        for (int rowIdx=-1; rowIdx<numb_shells_expected;rowIdx++) {
            rid = tT.getNewRowId();
            int indexI = rowIdx;
            if ( rowIdx==-1) {
                indexI = Histogram1D.UNDERFLOW;
            }
            float lowEdge = (float) xAxis.binLowerEdge(indexI);
            float uppEdge = (float) xAxis.binUpperEdge(indexI);
            if ( indexI==Histogram1D.UNDERFLOW) {
                lowEdge = 0f;
            }
            for (int j=0;j<=numb_shells_observed;j++) {
                int matched = 0;
                String colName = null;
                if ( j != numb_shells_observed) {
                    colName = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_" + Integer.toString(j+1));
                    matched = m2.binEntries(indexI,j);
                } else {
                    colName = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_o");
                    matched = m2.binEntries(indexI,Histogram1D.OVERFLOW);
                }
                tT.setValue(rid, colName, nf.format(matched));
            }

            sumB += b1.binEntries(indexI);
            sumM += m1.binEntries(indexI);
            float compl_cumul = Defs.NULL_FLOAT;
            float compl_shell = Defs.NULL_FLOAT;
            if ( b1.binEntries(indexI) != 0 ) {
                compl_shell = 100f*m1.binEntries(indexI)/b1.binEntries(indexI);
            }
            if ( sumB != 0 ) {
                compl_cumul = 100f*sumM/sumB;
            }
            tT.setValue(rid, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,    rid);
            tT.setValue(rid, tagNameNOE_compl_shellDetails,          "shell");
            tT.setValue(rid, tagNameNOE_compl_shellShell_start,      lowEdge);
            tT.setValue(rid, tagNameNOE_compl_shellShell_end,        uppEdge);
            //tT.setValue(rid, tagNameNOE_compl_shellObs_NOEs_total,   a1.binEntries(indexI));
            tT.setValue(rid, tagNameNOE_compl_shellExpected_NOEs,    b1.binEntries(indexI));
            tT.setValue(rid, tagNameNOE_compl_shellMatched_NOEs,     m1.binEntries(indexI));
            tT.setValue(rid, tagNameNOE_compl_shellCompl_cumul,      compl_cumul);
            tT.setValue(rid, tagNameNOE_compl_shellCompl_shell,      compl_shell);
        }
//        String colNameAnn;

        // Get the sums
        rid = tT.getNewRowId();
        tT.setValue(rid, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,       rid);
        tT.setValue(rid, tagNameNOE_compl_shellExpected_NOEs,    b1.allEntries());
        tT.setValue(rid, tagNameNOE_compl_shellMatched_NOEs,     m1.allEntries());
        tT.setValue(rid, tagNameNOE_compl_shellDetails, "sums");
        for (int j=0;j<=numb_shells_observed;j++) {
            int matched = 0;
            String colName = null;
            if ( j != numb_shells_observed) {
                matched = m2.binEntriesY(j);
                String numb = Integer.toString(j+1);
                colName = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_" + numb);
            } else {
                colName = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_o");
                matched = m2.binEntriesY(Histogram1D.OVERFLOW);
            }
            tT.setValue(rid, colName, nf.format(matched));
        }

        /** Observed contacts
         * rid = tT.getNewRowId();
         * float lowEdge = (float) xAxis.binUpperEdge(numb_shells_expected-1); // could be simpler.
         * tT.setValue(rid, tagNameNOE_compl_shellDetails, "obsv");
         * tT.setValue(rid, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,       rid);
         * tT.setValue(rid, tagNameNOE_compl_shellShell_start,      lowEdge);
         * tT.setValue(rid, tagNameNOE_compl_shellExpected_NOEs,    0); // by definition
         * tT.setValue(rid, tagNameNOE_compl_shellMatched_NOEs,     m1.binEntries(Histogram1D.OVERFLOW));
         */


        // Remove the dcSets from dc.
        for (int c=0;c< dcSetNames.length;c++) {
            if ( dc.mainRelation.removeColumn( dcSetNames[ c ]) == null) {
                General.showError("Failed to remove bitset column to dc main relation with name: " + dcSetNames[ c ] );
                return false;
            }
        }

        return true;
    }


    /** Get the elements per residue into a table.
     */
    private boolean setTagTableRes(SaveFrame sF, String file_name_base_dc) {
        TagTable tT = (TagTable) sF.get(3);
//        String tagNameResRid = "Temp_Res_Rid"; // used for plotting data directly of off tT.
//        tT.insertColumn(tagNameResRid, Relation.DATA_TYPE_INT,null);

        // Get the (index -1) of last observable residue
        int resMax = resAtomsObservableSet.length();
        if ( resMax < 0 ) {
            General.showError("Failed to get at least 1 residue in setTagTableRes");
        }
        Histogram1D a1 = new Histogram1D("A", resMax+1, 0, resMax+1);
        Histogram1D b1 = new Histogram1D("B", resMax+1, 0, resMax+1);
        Histogram1D m1 = new Histogram1D("M", resMax+1, 0, resMax+1);
//        FixedAxis xAxis = (FixedAxis) m1.xAxis();

        BitSet dcToDo = ASet;
        Histogram1D hisToDo = a1;
        for (int s=0;s<3;s++) {
            if ( s == 1 ) {
                dcToDo = BSet;
                hisToDo = b1;
            }
            if ( s == 2 ) {
                dcToDo = MSet;
                hisToDo = m1;
            }
            for (int rid=dcToDo.nextSetBit(0);rid>=0;rid=dcToDo.nextSetBit(rid+1)) {
                BitSet atomRidSet = dc.getAtomRidSet(rid);
                BitSet resRidSet = gumbo.atom.getResidueList(atomRidSet);
                // Set all involved residues once per restraint.
                for (int rrid=resRidSet.nextSetBit(0);rrid>=0;rrid=resRidSet.nextSetBit(rrid+1)) {
                    double d = (double) rrid;
                    hisToDo.fill(d);
                    if ( hisToDo.binEntries(Histogram1D.OVERFLOW)>0) {
                        General.showError("Unexpected overflow for res rid: " + d);
                    }
                    if ( hisToDo.binEntries(Histogram1D.UNDERFLOW)>0) {
                        General.showError("Unexpected underflow for res rid: " + d);
                    }
                }
            }
        }
        int rid = 0;
        for (int rrid=resAtomsObservableSet.nextSetBit(0);rrid>=0;rrid=resAtomsObservableSet.nextSetBit(rrid+1)) {
            rid = tT.getNewRowId();
            float compl_cumul = Defs.NULL_FLOAT;
            if ( b1.binEntries(rrid) != 0 ) {
                compl_cumul = 100f*m1.binEntries(rrid)/b1.binEntries(rrid);
            }
            String resName = gumbo.res.nameList[rrid];
            ArrayList resObsAtomList = (ArrayList) ui.wattosLib.completenessLib.obs.get( resName );
            int resObsAtomCount = resObsAtomList.size();
            tT.setValue(rid, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,    rid);
//            tT.setValue(rid, tagNameResRid,                             rrid);
            tT.setValue(rid, tagNameNOE_compl_resEntity_ID,             gumbo.mol.number[gumbo.res.molId[rrid]]);
            tT.setValue(rid, tagNameNOE_compl_resComp_index_ID,         gumbo.res.number[rrid]);
            tT.setValue(rid, tagNameNOE_compl_resComp_ID,               resName);
            tT.setValue(rid, tagNameNOE_compl_resObs_atom_count,        resObsAtomCount);
            tT.setValue(rid, tagNameNOE_compl_resRst_obser_count,       a1.binEntries(rrid));
            tT.setValue(rid, tagNameNOE_compl_resRst_expec_count,       b1.binEntries(rrid));
            tT.setValue(rid, tagNameNOE_compl_resRst_match_count,       m1.binEntries(rrid));
            tT.setValue(rid, tagNameNOE_compl_resCompl_cumul,           compl_cumul);
        }

        float[] av_sd = tT.getAvSd(tagNameNOE_compl_resCompl_cumul);
        if ( av_sd == null ) {
            General.showWarning("Failed to get the average and standard deviation of a column: " +
                    tagNameNOE_compl_resCompl_cumul + " leaving it blank.");
        } else {
            float av = av_sd[0];
            float sd = av_sd[1];
            if ( Defs.isNull(av) ) {
                General.showDebug("Failed to get av which is normal if there is no values.");
            } else if ( Defs.isNull(sd) ) {
                General.showDebug("Failed to get sd which is normal if there is only 1 value.");
            } else {
                int rowIdx = 0;
                for (int rrid=resAtomsObservableSet.nextSetBit(0);rrid>=0;rrid=resAtomsObservableSet.nextSetBit(rrid+1)) {
                    float completenessPercentage = tT.getValueFloat(rowIdx,tagNameNOE_compl_resCompl_cumul);
                    float sdItem = Defs.NULL_FLOAT;
                    if ( ! Defs.isNull(completenessPercentage) ) {
                        sdItem = Statistics.getNumberOfStandardDeviation(completenessPercentage,
                                av,sd);
                        tT.setValue(rowIdx, tagNameNOE_compl_resStand_deviat, sdItem);
                        if ( Math.abs(sdItem) >= 1f) {
                            tT.setValue(rowIdx, tagNameNOE_compl_resDetails, OVER_NUMBER_OF_SIGMAS_STR);
                        }
                    }
                    rowIdx++;
                }
            }
        }
        return true;
    }

    /** Convenience method.
     */
    public boolean doCompletenessCheck(
            float max_dist_expectedOverall,
            float min_dist_observed,
            float max_dist_observed,
            int numb_shells_observed,
            float min_dist_expected,
            float max_dist_expected,
            int numb_shells_expected,
            float avg_power_models,
            int avg_method,
            int monomers,
            boolean use_intra,
            String ob_file_name,
            String summaryFileNameCompleteness,
            boolean write_dc_lists,
            String file_name_base_dc
            ) {
        boolean isPerShellRun = false;
        for (int r=0;r<2;r++) {
            if ( r==1 ) {
                isPerShellRun = true;
            }
            if ( ! doCompletenessCheck(
                    max_dist_expectedOverall,
                    min_dist_observed,
                    max_dist_observed,
                    numb_shells_observed,
                    min_dist_expected,
                    max_dist_expected,
                    numb_shells_expected,
                    isPerShellRun,
                    avg_power_models,
                    avg_method,
                    monomers,
                    use_intra,
                    ob_file_name,
                    summaryFileNameCompleteness,
                    write_dc_lists,
                    file_name_base_dc)) {
                General.showError("Failed to run " + r + " of completeness check.");
                return false;
            }
        }
        return true;
    }


    /** Analyzes the first selected entry for completeness of selected distance constraints
     *with respect to selected atoms.
     * Reset the completeness lib first in the ui if needed to change from standard.
     *If there are no observable atoms in the coordinate list (e.g. entry 8drh) no
     *results will be generated but the return status will still be true for success.
     *The same if no restraints were observed.
     */
    private boolean doCompletenessCheck(
            float max_dist_expectedOverall,
            float min_dist_observed,
            float max_dist_observed,
            int numb_shells_observed,
            float min_dist_expected,
            float max_dist_expected,
            int numb_shells_expected,
            boolean isPerShellRun,
            float avg_power_models,
            int avg_method,
            int monomers,
            boolean use_intra,
            String ob_file_name,
            String summaryFileNameCompleteness,
            boolean write_dc_lists,
            String file_name_base_dc
            ) {

        if ( numb_shells_observed > MAX_SHELLS_OBSERVED ) {
            numb_shells_observed = MAX_SHELLS_OBSERVED;
        }

        if ( ! isPerShellRun ) {
            max_dist_expected = max_dist_expectedOverall;
        }

        this.min_dist_expected      = min_dist_expected;
        this.max_dist_expected      = max_dist_expected;
        this.numb_shells_expected   = numb_shells_expected;

        this.min_dist_observed      = min_dist_observed;
        this.max_dist_observed      = max_dist_observed;
        this.numb_shells_observed   = numb_shells_observed;

        this.avg_power_models       = avg_power_models;
        this.avg_method             = avg_method;
        this.monomers               = monomers;

        boolean showValues = false;
        // Store the selections that can get changed by this code.
        BitSet atomSelectedSave     = (BitSet) gumbo.atom.selected.clone();
        BitSet molSelectedSave      = (BitSet) gumbo.mol.selected.clone();
        BitSet entrySelectedSave    = (BitSet) gumbo.entry.selected.clone();
        BitSet dcSelectedSave       = (BitSet) dc.selected.clone();
        BitSet dcListSelectedSave   = (BitSet) dcList.selected.clone();

        // find first entry and store variable in class instance.
        currentEntryId = dc.gumbo.entry.selected.nextSetBit(0);
        if ( currentEntryId < 0 ) {
            General.showError("No entries selected");
            return false;
        }
        Integer currentEntryRidInt = new Integer( currentEntryId );
        // find first model in entry
        BitSet modelFirstInEntry = SQLSelect.selectCombinationBitSet( dc.dbms, dc.gumbo.model.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME ], SQLSelect.OPERATION_TYPE_EQUALS, currentEntryRidInt,
                RelationSet.DEFAULT_ATTRIBUTE_NUMBER,                                              SQLSelect.OPERATION_TYPE_EQUALS, new Integer(1),
                SQLSelect.OPERATOR_AND, false );
        if ( modelFirstInEntry.cardinality() < 1 ) {
            General.showError("Failed to find at least one model in entry with the number 1");
            return false;
        }
        int currentModelRid = dc.gumbo.model.selected.nextSetBit(0);
        if ( modelFirstInEntry.cardinality() > 1 ) {
            General.showError("Found more than one model in entry with the number 1. Using first model rid: " + currentModelRid);
            return false;
        }
        Integer currentModelRidInt = new Integer( currentModelRid );
        // see which models need to be done.
        BitSet selectedModels  = gumbo.model.selected;
        // Don't do models that aren't in the same entry; duh.
        BitSet modelsInEntry = SQLSelect.selectBitSet(ui.dbms, gumbo.model.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(currentEntryId), false);
        if ( modelsInEntry == null || modelsInEntry.cardinality() < 1 ) {
            General.showError("failed to get a single model in entry");
            return false;
        }
        BitSet todoModels = (BitSet) selectedModels.clone();
        todoModels.and( modelsInEntry );
        todoModelArray = PrimitiveArray.toIntArray( todoModels ); // for efficiency.
        int modelCount = todoModelArray.length;
        doFancyAveraging = (avg_power_models!=1.0f); // faster to do simpel averaging.
        General.showDebug("doFancyAveraging: " + doFancyAveraging);

        // Find constraints
        BitSet dcInEntry = SQLSelect.selectBitSet( dc.dbms,
                dc.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS, currentEntryRidInt, false );
        BitSet dcSelectedInEntry = (BitSet) dcInEntry.clone();
        dcSelectedInEntry.and( dc.selected );
        int dcSelectedInEntryCount = dcSelectedInEntry.cardinality();
        if ( dcSelectedInEntryCount < 1 ) {
            General.showWarning("No selected dcs in first selected entry.");
            return true;
        }

        // Find selected atoms in first model.
        BitSet atomInModel = SQLSelect.selectBitSet( dc.dbms,
                dc.gumbo.atom.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS, currentModelRidInt, false );
        BitSet atomSelectedInModel = (BitSet) atomInModel.clone();
        atomSelectedInModel.and( dc.gumbo.atom.selected );
        int atomSelectedInModelCount = atomSelectedInModel.cardinality();

        // Get residue list
        resInModel = SQLSelect.selectBitSet( dc.dbms,
                dc.gumbo.res.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS, currentModelRidInt, false );

        // Find dc constraint list properties
        // First constraint:
//        int dcCurrentRID = dcSelectedInEntry.nextSetBit(0);
//        int dcListCurrentRID = dc.dcListIdMain[ dcCurrentRID ];
//        Constr constr = (Constr) dc.relationSoSParent;

        if ( atomSelectedInModelCount < 1 ) {
            General.showError("No selected atoms in first selected entry.");
            return false;
        }

        if ( ! getAtomsObservableObjects(currentModelRid, atomSelectedInModel)) {
            General.showError("Failed to get the observable atoms within the first model");
            return false;
        }
        if ( atomsObservableCombos == null ) {
            General.showCodeBug("atomsObservableCombos is null");
            return false;
        }
        if ( atomsObservableCombos.size() == 0 ) {
            General.showWarning("No observables.");
            return true;
        }


        // Set the newDCListId class attribute
        if ( ! splitDCs( dcSelectedInEntry )) { // input dcs may be spread over multiple dclists.
            General.showError("Failed to split the selected DCs in entry to a new list with one potentially observable contribution per constraint.");
            return false;
        }


        /** Create the sets anew */
        String[] dcSetNames = {
            "USet", // universe of experimental constraints
            "VSet", // universe of theoretical constraints
            "WSet", // union of U and V
            "XSet", // ((U - (E u O)) u V but keeps shrinking on removal of (I u S). Internal to Wattos.
            "ESet", // exceptional
            "OSet", // not observable
            "ISet", // intra residual not to be analyzed
            "SSet", // surplus
            "ASet", // set of observable experimental distance constraints
            "BSet", // set of observable theoretical ...
            "MSet", // A n B
            "CSet", // A - M
            "DSet", // B - M
            "LSet", // contributions with lower bounds
            "PSet"  // contributions with upper bounds
        };
        for (int c=0;c< dcSetNames.length;c++) {
            if ( dc.mainRelation.containsColumn( dcSetNames[ c ] ) ) {
                if ( dc.mainRelation.getColumnDataType( dcSetNames[ c ]  ) != Relation.DATA_TYPE_BIT ) {
                    General.showWarning("Existing column isn't of type BitSet from dc main relation with name: " + dcSetNames[ c ] );
                }
                Object tmpObject = dc.mainRelation.removeColumn( dcSetNames[ c ] );
                if ( tmpObject == null ) {
                    General.showError("Failed to remove existing column from dc main relation with name: " + dcSetNames[ c ] );
                    return false;
                }
            }
            if ( ! dc.mainRelation.insertColumn( dcSetNames[ c ], Relation.DATA_TYPE_BIT, null )) {
                General.showError("Failed to insert bitset column to dc main relation with name: " + dcSetNames[ c ] );
                return false;
            }
        }
        USet =  dc.mainRelation.getColumnBit( "USet" );
        VSet =  dc.mainRelation.getColumnBit( "VSet" );
        WSet =  dc.mainRelation.getColumnBit( "WSet" );
        XSet =  dc.mainRelation.getColumnBit( "XSet" );
        ESet =  dc.mainRelation.getColumnBit( "ESet" );
        OSet =  dc.mainRelation.getColumnBit( "OSet" );
        ISet =  dc.mainRelation.getColumnBit( "ISet" );
        SSet =  dc.mainRelation.getColumnBit( "SSet" );
        ASet =  dc.mainRelation.getColumnBit( "ASet" );
        BSet =  dc.mainRelation.getColumnBit( "BSet" );
        MSet =  dc.mainRelation.getColumnBit( "MSet" );
        CSet =  dc.mainRelation.getColumnBit( "CSet" );
        DSet =  dc.mainRelation.getColumnBit( "DSet" );
        LSet =  dc.mainRelation.getColumnBit( "LSet" );
        PSet =  dc.mainRelation.getColumnBit( "PSet" );

        BitSet[] setsToWrite    = {  USet,   VSet,   WSet,   ESet,   OSet,   ISet,   SSet,   ASet,   BSet,   MSet,   CSet,   DSet };
        String[] setsToWriteStr = { "USet", "VSet", "WSet", "ESet", "OSet", "ISet", "SSet", "ASet", "BSet", "MSet", "CSet", "DSet" }; // stupid

        BitSet splitDcs = SQLSelect.selectBitSet( dc.dbms,
                dc.mainRelation,
                Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(newDCListId), false );
        USet.or( splitDcs ); // All to be checked.
        ASet.clear();
        ASet.or( USet ); // keeps shrinking
// EXCEPTIONAL
        ESet.clear();
        ESet.or( ASet );
        ESet.and( dc.hasUnLinkedAtom ); // Exceptional constraints should be removed.
        General.showDebug("Constraints (E): " + PrimitiveArray.toString( ESet, showValues ));
        ASet.andNot( ESet ); // keeps shrinking
// UNOBSERVABLE
        boolean status = setSelectionObscured(ASet); // will work on OSet
        if ( ! status ) {
            General.showError("Failed to setSelectionObscured");
            return false;
        }
        General.showDebug("Constraints (O): " + PrimitiveArray.toString( OSet, showValues ));
        ASet.andNot( OSet ); // keeps shrinking
// THEO
        BitSet result = addTheoreticalConstraints(ASet, max_dist_expected, use_intra); // Looking only in ASet to match
        if ( result == null  ) {
            General.showError("Failed to addTheoreticalConstraints");
            return false;
        }
        General.showDebug("Found number of theo constraints: " + result.cardinality());
        VSet.or( result );
        XSet.or( VSet );    // keeps shrinking
        XSet.or( ASet);
// INTRAS
        status = dc.classify(XSet);
        if ( ! status ) {
            General.showError("Failed to dc.classify");
            return false;
        }
        if ( ! use_intra ) {
            ISet.or( dc.mainRelation.getColumnBit( DistConstr.DEFAULT_CLASS_NAMES[ DistConstr.DEFAULT_CLASS_INTRA]));
        }
        General.showDebug("Constraints (I): " + PrimitiveArray.toString( ISet, showValues  ));
        XSet.andNot( ISet ); // keeps shrinking
// SURPLUS
        /** Mark the surplus */
        Surplus surplus = new Surplus(ui);
        boolean updateOriginalConstraints = false;
        boolean onlyFilterFixed = false;
//        boolean append = true;

        result = surplus.getSelectionSurplus(
                XSet,
                Surplus.THRESHOLD_REDUNDANCY_DEFAULT,
                updateOriginalConstraints,
                onlyFilterFixed,
                avg_method,
                monomers,
                file_name_base_dc,              // for summary etc.
                true,                           // append
                false,                          // writeNonRedundant,
                false,                          // writeRedundant,
                false                           // removeSurplus
                );
        if ( result == null ) {
            General.showError( "Failed to get surplus.");
            return false;
        }

        SSet.or( result );
        General.showDebug("Constraints (S): " + PrimitiveArray.toString( SSet, showValues  ));
        XSet.andNot( SSet ); // keeps shrinking

        // DO THE LOGIC AGAIN.
        BitSet cleared = (BitSet) ESet.clone();
        cleared.or( OSet );
        cleared.or( ISet );
        cleared.or( SSet );
        ASet.clear();
        ASet.or(USet);
        ASet.andNot( cleared );

        cleared = (BitSet) ISet.clone();
        cleared.or( SSet );
        BSet.clear();
        BSet.or(VSet);
        BSet.andNot( cleared );

        MSet.or( ASet );
        MSet.and( BSet );
        CSet.or(ASet);
        CSet.andNot(MSet);
        DSet.or(BSet);
        DSet.andNot(MSet);

        WSet.clear();
        WSet.or( USet );
        WSet.or( VSet );

        int USetCount = USet.cardinality();
        int VSetCount = VSet.cardinality();
        int ESetCount = ESet.cardinality();
        int OSetCount = OSet.cardinality();
        int ISetCount = ISet.cardinality();
        int SSetCount = SSet.cardinality();
        int ASetCount = ASet.cardinality();
        int BSetCount = BSet.cardinality();
        int MSetCount = MSet.cardinality();
        int CSetCount = CSet.cardinality();
        int DSetCount = DSet.cardinality();

        float overall_completeness_factor = Defs.NULL_FLOAT;
        if ( !isPerShellRun ) {
            if ( BSetCount != 0 ) {
                overall_completeness_factor = 100f * MSetCount / BSetCount;
            } else {
                General.showWarning("The completeness is undefined because there aren't any expected constraints.");
            }

            if ( Defs.isNull( overall_completeness_factor )) {
                General.showWarning("Failed to calculate the completeness");
            } else {
                General.showOutput( "Overal completeness is " + overall_completeness_factor );
            }
        }

        gumbo.entry.selected.clear();
        gumbo.entry.selected.set(currentEntryId);   // only use the current entry
        gumbo.mol.selected.clear();  // clearing the molecules will disable writing them
        gumbo.atom.selected.clear(); // clearing the atoms     will disable writing them
        if ( write_dc_lists && (!isPerShellRun)) {
            for (int setId=0;setId<setsToWrite.length;setId++) {
                BitSet setToWrite = setsToWrite[setId];
                String setName = setsToWriteStr[setId];
                String fileName = file_name_base_dc+"_"+setName+".str";
                dc.selected.clear();
                dc.selected.or(setToWrite);
                if ( dc.selected.cardinality() > 0 ) {
                    General.showOutput("Writing the set: " + setName + " to file: " + fileName
                            + " with number of dcs: " + dc.selected.cardinality());
                    gumbo.entry.writeNmrStarFormattedFileSet(fileName,null,ui);
                } else {
                    General.showOutput("Not writing set: " + setName + " to file: " + fileName
                            + " because there are no dcs in it");
                    File oldDump = new File( fileName );
                    if ( oldDump.exists() && oldDump.isFile() && oldDump.canRead() ) {
                        General.showDebug("Removing old dump.");
                        if ( ! oldDump.delete() ) {
                            General.showWarning("Failed to remove old dump");
                        }
                    }
                }
            }
        }
        int entryId = gumbo.entry.getEntryId();

        if ( ! isPerShellRun ) {
            // Create star nodes
            db = new DataBlock();
            if ( db == null ) {
                General.showError( "Failed to init datablock.");
                return false;
            }
            db.title = gumbo.entry.nameList[entryId];
            SaveFrame sF = getSFTemplate();
            if ( sF == null ) {
                General.showError( "Failed to getSFTemplate.");
                return false;
            }
            db.add(sF);
            int rowIdx = 0;
            // INTRO

            TagTable tT = (TagTable) sF.get(0);
            tT.setValue(rowIdx, Relation.DEFAULT_ATTRIBUTE_ORDER_ID , 0);
            tT.setValue(rowIdx, tagNameNOE_compl_listModel_count,     modelCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listResidue_count,   resInModel.cardinality());
            tT.setValue(rowIdx, tagNameNOE_compl_listTot_atom_count,  atomSelectedInModel.cardinality());
            tT.setValue(rowIdx, tagNameNOE_compl_listObserv_atoms,    InOut.getFilenameBase(ob_file_name));
            tT.setValue(rowIdx, tagNameNOE_compl_listObs_atom_count,  atomsObservableCombos.size());
            tT.setValue(rowIdx, tagNameNOE_compl_listUse_intra_resid, use_intra);
            tT.setValue(rowIdx, tagNameNOE_compl_listThreshold_redun, Surplus.THRESHOLD_REDUNDANCY_DEFAULT);
            tT.setValue(rowIdx, tagNameNOE_compl_listAveraging_power, avg_power_models);
            tT.setValue(rowIdx, tagNameNOE_compl_listCompl_cutoff,    max_dist_expected);
            tT.setValue(rowIdx, tagNameNOE_compl_listCompl_cumul,     overall_completeness_factor);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_unexp_count, dcSelectedInEntry.cardinality());

            tT.setValue(rowIdx, tagNameNOE_compl_listRestraint_count, USetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listPair_count,      VSetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_excep_count, ESetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_nonob_count, OSetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_intra_count, ISetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_surpl_count, SSetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_obser_count, ASetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_expec_count, BSetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_match_count, MSetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_unmat_count, CSetCount);
            tT.setValue(rowIdx, tagNameNOE_compl_listRst_exnob_count, DSetCount);

            tT.setValue(rowIdx, tagNameNOE_compl_listDetails,         explanation);

            // CLASSES
            if ( ! setTagTableClasses( sF )) {
                General.showError("Failed setTagTableClasses");
                return false;
            }
            // RESIDUE
            if ( ! setTagTableRes( sF, file_name_base_dc )) {
                General.showError("Failed setTagTableRes");
                return false;
            }
        } else {
            SaveFrame sF = (SaveFrame) db.get(0);
            if ( sF == null ) {
                General.showError( "Failed to get old SF.");
                return false;
            }
            // SHELL
            if ( ! setTagTableShell( sF )) {
                General.showError("Failed setTagTableClasses");
                return false;
            }
            General.showDebug("Reformat the columns by dictionary defs");
            if ( ! sF.toStarTextFormatting(starDict)) {
                General.showWarning("Failed to format all columns as per dictionary this is not fatal however.");
            }
            General.showOutput("Writing completeness results to STAR file: " + summaryFileNameCompleteness);
            if ( ! db.toSTAR(summaryFileNameCompleteness)) {
                General.showError("Failed to write the file.");
                return false;
            }
        }

        // Restore the selections that can get changed by this code.
        gumbo.atom.selected.clear();
        gumbo.mol.selected.clear();
        gumbo.entry.selected.clear();
        dc.selected.clear();
        dcList.selected.clear();

        gumbo.atom.selected.or(     atomSelectedSave);
        gumbo.mol.selected.or(      molSelectedSave);
        gumbo.entry.selected.or(    entrySelectedSave);
        dc.selected.or(             dcSelectedSave);
        dcList.selected.or(         dcListSelectedSave);

        return true;
    }

    /**Make a list of restraints from the (collapsed) atoms of the
     *original restraints.
     *For atoms that are not stereospecifically observable but their representing
     *pseudo atom is, the first atom should be selected in the model otherwise the
     *whole pseudo atom isn't going to be included.
     */
    private boolean getAtomsObservableObjects(int currentModelRid, BitSet atomSelectedInModel) {
        atomsObservableSet = new BitSet();
        resAtomsObservableSet = new BitSet();
        atomsObservableCombos = new ArrayList();
        indexToCombo = new HashMap();

        String pseudoAtomName = null;
        int pseudoAtomType = Defs.NULL_INT;
//        int atomCountInPseudo = Defs.NULL_INT;
        // for each selected atom in given model
        for (int currentAtomRid=atomSelectedInModel.nextSetBit(0);currentAtomRid>=0;currentAtomRid=atomSelectedInModel.nextSetBit(currentAtomRid+1)) {
            int currentResRid   = gumbo.atom.resId[ currentAtomRid ];
            String resName      = gumbo.res.nameList[ currentResRid ];
            String atomName     = gumbo.atom.nameList[ currentAtomRid ];
            //General.showDebug("Considering selected atom: " + atomName + " in residue type: " + resName );

            ArrayList tmpObsAtomList = (ArrayList) ui.wattosLib.completenessLib.obs.get( resName );
            if ( tmpObsAtomList == null ) {
                continue; // Failure to get completeness library for this residue name means it's not considered observable.
            }

            pseudoAtomName = null;
            pseudoAtomType = Defs.NULL_INT;
//            atomCountInPseudo = Defs.NULL_INT;


            if ( tmpObsAtomList.contains( atomName )) {
                pseudoAtomName = atomName;
                //General.showDebug("It's a simple observable atom: " + atomName);
            } else {
                ArrayList tmpObsPseudoAtomList = (ArrayList) ui.wattosLib.pseudoLib.fromAtoms.get( resName, atomName );
                // Only the last in the list can be used. This implies that e.g. PHE QR can not be observable but QD can.
                if ( tmpObsPseudoAtomList == null ) {
                    //General.showDebug("Current atom isn't even in observable pseudo: " + atomName);
                    continue;
                }
                pseudoAtomName = (String) tmpObsPseudoAtomList.get(tmpObsPseudoAtomList.size()-1);
                //General.showDebug("using pseudo: " + pseudoAtomName);
                ArrayList tmpObsAtomListInPseudo = (ArrayList) ui.wattosLib.pseudoLib.toAtoms.get( resName, pseudoAtomName );
                if ( tmpObsAtomListInPseudo == null ) {
                    General.showError("Failed to find atoms for pseudo atom with name: " + pseudoAtomName + " in residue with name: " + resName);
                    continue;
                }
                // Check if it's the first atom in the definitions otherwise don't include it
                String firstAtomInPseudo = (String) tmpObsAtomListInPseudo.get(0);
                //General.showDebug("Using first atom in pseudo: " + firstAtomInPseudo);
                if ( !atomName.equals( firstAtomInPseudo)) {
                    //General.showDebug("Skipping atom because it isn't the first one in the list");
                    continue;
                }
                pseudoAtomType = ( (Integer) ui.wattosLib.pseudoLib.pseudoAtomType.get(resName, pseudoAtomName)).intValue();
                //General.showDebug("Found pseudo type: " + pseudoAtomType);
            }
            PseudoAtom ps = new PseudoAtom( pseudoAtomName, pseudoAtomType, currentAtomRid );
            if ( ! ps.findOtherAtoms(ui)) {
                //General.showWarning("Failed to find other atoms for pseudo atom with name: " + pseudoAtomName + " in residue with name: " + resName + " skipping this one for list of potentially observable resonances.");
                continue;
            }
            atomsObservableCombos.add( ps ); // ps is a single atom or a pseudo.
            PrimitiveArray.setValueByArray( ps.atomRids, atomsObservableSet );
            resAtomsObservableSet.set( ps.getResRid()); // mark the residue as having observable atoms
            //General.showDebug("Added atom: " + ps);
        }


        Collections.sort( atomsObservableCombos );
        Integer atomsObservableCombosIndex = null;
        PseudoAtom ps = null;
        for ( int i=0;i<atomsObservableCombos.size();i++) {
            atomsObservableCombosIndex = new Integer( i );
            ps = (PseudoAtom) atomsObservableCombos.get(i);
            for ( int j=0;j<ps.atomRids.length;j++) {
                indexToCombo.put( new Integer(ps.atomRids[j]), atomsObservableCombosIndex);
            }
        }
         General.showDebug("Found total number atoms selected in model                      : " + atomSelectedInModel.cardinality());
         General.showDebug("Found number of selected observable atoms in model              : " + atomsObservableSet.cardinality());
         General.showDebug("Found number of residues with selected observable atoms in model: " + resAtomsObservableSet.cardinality());
//         General.showOutput("Found residues with selected observable atoms in model          : " + PrimitiveArray.toString( resAtomsObservableSet));
         General.showDebug("Found number of selected observable (pseudo)atoms in model      : " + atomsObservableCombos.size());
//         General.showDebug("Found observable atom combos in model (sorted)                  : " + Strings.toString( atomsObservableCombos, true ) );
//         General.showOutput("Found observable atoms in model             : " + gumbo.atom.toString(atomsObservableSet));
//         General.showOutput("Index observable atoms to combo id          : " + Strings.toString(indexToCombo));

         if ( atomsObservableCombos.size() == 0) {
             General.showWarning("Failed to find any observable atom(-group)");
         }
        return true;
    }


    /**Make a new list of dc restraints from the selected dcs in the entry with only 1 potentially observable
     *contribution per constraint.
     *NOTES:
     *<PRE>
     *-1- this method will return success status but also set the new dc list rid variable: newDCListId
     *-2- The first selected constraint determines the dc list attributes like averaging method etc.
     *Pseudo code:
     *express each member into an array of observables (in atomsObservableCombos) OR original dcAtom refs.
     *expand by combinatorials the arrays against each other.
     *
     *E.g.
     *constr 1: (CX, HG12, HG13])              ( H, CY )       Node 2
     *          ( H )                          ( HB )          Node 3
     *          ( HG21 )                       ( H )           Node 4
     *          ( QB )                         ( QD )          Node 5 (say leucine where the obs atoms are HB2/HB3 and MD1/MD2)
     *CX etc are unmatched atoms.
     *expressed in observables if HG2 and HG3 aren't stereospecifically observable becomes:
     *constr 1: (CX, QG )                      ( H, CY )    Node 2
     *          ( H )                          ( HB )       Node 3
     *          ( MG )                         ( H )        Node 4
     *          ( HB3 )                        ( MD1 )      Node 5 (depending on the closest atom it will match that one)
     *expanded:
     *constr 1: (CX)                           ( H )        Node 1
     *constr 2: (CX)                           ( CY )       Node 1
     *constr 3: ( QG )                         ( H )        Node 1
     *constr 4: ( QG )                         ( CY )       Node 1
     *constr 5: ( H )                          ( HB )       Node 1
     *constr 5: ( MG )                         ( H )        Node 1
     *constr 6: ( HB3 )                        ( MD1 )      Node 1
     *
     *Notes: this method will order the atom rids for efficiency in the first model.
     */
    public boolean splitDCs(BitSet dcSelectedInEntry ) {
        /** Rids into the specific tables. Values set are to start search, will be reset later.*/
        int currentDCAtomId = 0;
//        int currentDCMembId = 0;
        int currentDCNodeId = 0;
        int currentDCId     = 0;
        /** Rids into the specific tables. Values set are to start search, will be reset later.*/
        int newDCAtomId = 0;
        int newDCMembId = 0;
        int newDCNodeId = 0;
        int newDCId     = 0;

        int dcCountTotal = dcSelectedInEntry.cardinality();
        General.showDetail("Number of distance constraints in list(s): " + dcCountTotal);
        if ( dcCountTotal < 1 ) {
            General.showWarning("Failed to find any distance constraint in list(s)" );
            return false;
        }
        // Quick optimalization:
        dc.mainRelation.reserveRows(   dcCountTotal * 5);
        dc.distConstrNode.reserveRows( dcCountTotal * 5);
        dc.distConstrMemb.reserveRows( dcCountTotal * 10);
        dc.distConstrAtom.reserveRows( dcCountTotal * 30);

        // IMPORTANT sets instance variable newDCListId
        newDCListId = dcList.mainRelation.getNewRowId();
        if ( newDCListId < 0 ) {
            General.showCodeBug("Failed to add next dc list.");
            return false;
        }
        // Need to reinitialize these again after this big reservation!
        if ( ! initConvenienceVariablesConstr() ) {
            General.showCodeBug("Failed to initConvenienceVariablesConstr.");
            return false;
        }
        // Set the dcList attributes.
        currentDCId=dcSelectedInEntry.nextSetBit(0);
        dcList.mainRelation.copyRow( dc.dcListIdMain[ currentDCId ], newDCListId);
        //dcList.entry_id[ newDCListId ]          = currentEntryId;
        dcList.nameList[ newDCListId ]          = "Completeness DCList for entry: " + gumbo.entry.nameList[ currentEntryId ];
        //dcList.avgMethod[ newDCListId ]         = DistConstrList.DEFAULT_AVERAGING_METHOD;
        //dcList.numberMonomers[ newDCListId ]    = DistConstrList.DEFAULT_AVERAGING_MONOMER_COUNT;
        dcList.selected.set( newDCListId );

        // These indexes don't need to be updated if they're only used to lookup rows that exist at this point
        // I.e. not the new ones added by this routine.
        IndexSortedInt indexMembAtom = (IndexSortedInt) dc.distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) dc.distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) dc.distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if (    indexMembAtom == null ||
                indexNodeMemb == null ||
                indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes to dc main in atom/memb/node");
            return false;
        }

        int newDCCount=0;

        // FOR EACH CONSTRAINT
        for (int dcCount = 1;currentDCId>=0;currentDCId=dcSelectedInEntry.nextSetBit(currentDCId+1),dcCount++) {
            if ( ! dc.mainRelation.used.get( currentDCId ) ) {
                General.showError("Trying an unused dc rid: " + currentDCId);
                return false;
            }
//            General.showDebug("Preparing distance constraint: " + dcCount + " at rid: " + currentDCId);
//            General.showDebug(dc.toString(currentDCId));
            IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  new Integer(currentDCId), Index.LIST_TYPE_INT_ARRAY_LIST, null);
//            General.showDebug("Found the following rids of nodes in constraint: " + PrimitiveArray.toString( dcNodes ));
            if ( dcNodes == null ) {
                General.showError("Failed to get nodes");
                return false;
            }
            if ( dcNodes.size() < 1 ) {
                General.showError("Failed to get at least one node");
                return false;
            }
            if ( ! PrimitiveArray.orderIntArrayListByIntArray( dcNodes, dc.numbNode )) {
                General.showError("Failed to order nodes by order column");
                return false;
            }

            // FOR EACH NODE
            for ( int currentDCNodeBatchId=0, dCNodeNumber=1;currentDCNodeBatchId<dcNodes.size();currentDCNodeBatchId++,dCNodeNumber++) {
                currentDCNodeId = dcNodes.getQuick( currentDCNodeBatchId );
                // Skip non constraint nodes
                if ( ! Defs.isNull( dc.logicalOp[currentDCNodeId] ) ) {
                    continue;
                }
                IntArrayList dcMembs = (IntArrayList) indexNodeMemb.getRidList(  new Integer(currentDCNodeId),
                        Index.LIST_TYPE_INT_ARRAY_LIST, null);
//                General.showDebug("Found the following rids of members in constraint node (" + currentDCNodeId + "): " + PrimitiveArray.toString( dcMembs ));
                if ( dcMembs.size() != 2 ) {
                    General.showError("Are we using a number different than 2 as the number of members in a node?");
                    return false;
                }
                if ( ! PrimitiveArray.orderIntArrayListByIntArray( dcMembs, dc.numbMemb )) {
                    General.showError("Failed to order members by order column");
                    return false;
                }
                // reference into foreign (not this dcs) dcatom for unlinked atoms and unobservable atoms
                // or the index into atomsObservableCombos. There should be no duplicates
                // in this list.
                IntArrayList dcAtomsI = null;
                IntArrayList dcAtomsJ = null;
                // Determines which of the two it is.
                BooleanArrayList isInComboListI = null;
                BooleanArrayList isInComboListJ = null;

                // FOR EACH MEMBER (usually just 2)
                for (int currentDCMembBatchId=0;currentDCMembBatchId<dcMembs.size(); currentDCMembBatchId++) {
                    int currentDCMembID = dcMembs.getQuick( currentDCMembBatchId );
//                    General.showDebug("Working on member with RID: " + currentDCMembID);
                    /** Reference to dcatomid OR ref to combo */
                    IntArrayList dcAtoms = (IntArrayList) indexMembAtom.getRidList(  new Integer(currentDCMembID),
                            Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    if ( (dcAtoms==null) || dcAtoms.size() < 1 ) {
                        General.showError("Didn't find a single atom for a member in constraint node (" + dCNodeNumber + "): for constraint number: " + dcCount);
                        return false;
                    }

                    if ( ! PrimitiveArray.orderIntArrayListByIntArray( dcAtoms, dc.orderAtom )) {
                        General.showError("Failed to order atoms by order column");
                        return false;
                    }
//                    General.showDebug("Found the following rids of dc atoms in constraint node (" + dCNodeNumber + "): " + PrimitiveArray.toString( dcAtoms ));
                    BooleanArrayList isInComboList = new BooleanArrayList( dcAtoms.size());
                    // FOR EACH ATOM
                    for ( int currentDCAtomBatchId=0; currentDCAtomBatchId<dcAtoms.size(); currentDCAtomBatchId++) {
                        currentDCAtomId = dcAtoms.getQuick( currentDCAtomBatchId );
                        if ( ! dc.distConstrAtom.used.get( currentDCAtomId )) {
                            General.showCodeBug("Got an currentDCAtomId for an unused row: " + currentDCAtomId);
                            return false;
                        }
                        int atomId  = dc.atomIdAtom[currentDCAtomId];
                        if ( Defs.isNull( atomId ) ) { // Was the atom actually matched in the structure?
                            isInComboList.add( false );
                            // dcAtoms can remain a ref to the dcAtoms
                        } else {
                            // lookup the position in the atomsObservableCombos by index
                            Integer idxInt = (Integer) indexToCombo.get( new Integer(atomId));
                            if ( idxInt == null ) {
//                                General.showDebug("The linked atom wasn't in the atomsObservableCombos. Keeping dcatomid as unobservable normal atom.");
                                isInComboList.add( false );
                            } else {
//                                General.showDebug("The linked atom was in the atomsObservableCombos. Keeping ref to Combo.");
                                isInComboList.add( true );
                                dcAtoms.setQuick( currentDCAtomBatchId, idxInt.intValue());
                            }
                        }
                    } // end of loop per atom
                    // Then remove exact duplicates.
                    if ( ! removeDuplicates( dcAtoms, isInComboList)) {
                        General.showCodeBug("Failed to remove Duplicate observable atoms in member: " + currentDCMembBatchId);
                        return false;
                    }
                    // Then remove any but the first of any pair like reduce
                    // the list of HB2/HB3 to HB2.
                    /**if ( ! PseudoAtom.removeAllButOneOfStereoPairThatsStereoSpecific( dcAtoms, isInComboList,
                     * atomsObservableCombos, ui)) {
                     * General.showCodeBug("Failed to removeAllButOneOfStereoPairThatsStereoSpecific atoms in member: " + currentDCMembBatchId);
                     * return false;
                     * }
                     * //General.showDebug("AFTER removeDuplicates atoms : " + PrimitiveArray.toString( dcAtoms ));
                     */
//                    General.showDebug("After lookup and remove duplicates; rids of dc atoms in constraint node (" + dCNodeNumber + "): " + PrimitiveArray.toString( dcAtoms ));
//                    General.showDebug("in combo?                    : " + PrimitiveArray.toString( isInComboList ));
                    if ( currentDCMembBatchId == 0) { // switch between the 2 members
                        dcAtomsI = dcAtoms;
                        isInComboListI = isInComboList;
                    } else {
                        dcAtomsJ = dcAtoms;
                        isInComboListJ = isInComboList;
                    }
                } // end of loop per member

                // match constraints to the closest permutation in the model(s).
                ArrayList matchedConstraintList = matchConstraints(dcAtomsI,dcAtomsJ,isInComboListI,isInComboListJ);
                if ( (matchedConstraintList == null) ||(matchedConstraintList.size()<1) ) {
                    General.showError("Failed to successfully match the constraint:\n"+
                            dc.toString(currentDCId));
                    continue;
//                    return false;
                }

                for ( int m=0;m<matchedConstraintList.size();m++ ) {
                    Object[] match = (Object[]) matchedConstraintList.get(m);
                    if ( match == null ) {
                        General.showCodeBug("Failed to find match of the constraints");
                        return false;
                    }
                    int dcAtomI         = dcAtomsI.getQuick( ((Integer) match[0]).intValue());
                    int dcAtomJ         = dcAtomsJ.getQuick( ((Integer) match[1]).intValue());
                    boolean isInComboI  = ((Boolean) match[2]).booleanValue();
                    boolean isInComboJ  = ((Boolean) match[3]).booleanValue();

                    // EACH NEW DC
                    // Now a new constraint will be created for each combination of member I dcAtom/combo and member J dcAtom/combo
                    newDCCount++;
                    //General.showDebug("Generating new dc for contribution membI: " + 1 + " membJ: " + 1 + " dc #: " + newDCCount);
                    newDCId = dc.mainRelation.getNextReservedRow(currentDCId);
                    if ( newDCId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                        newDCId = dc.mainRelation.getNextReservedRow(0); // now it should be fine.
                        if ( ! initConvenienceVariablesConstr()) {
                            General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                            return false;
                        }
                    }
                    if ( newDCId < 0 ) {
                        General.showCodeBug("Failed to get next reserved row in main distance constraint table.");
                        return false;
                    }
                    dc.mainRelation.copyRow(currentDCId, newDCId);
                    dc.number[                  newDCId ] = newDCCount;
                    dc.dcListIdMain[            newDCId ] = newDCListId;
                    //dc.entryIdMain[             newDCId ] = currentEntryId; // need to be the same.
                    dc.selected.set(            newDCId );
                    dc.hasUnLinkedAtom.clear(   newDCId ); //TODO figure out which need to be set !

                    // EACH NEW DC node
                    newDCNodeId = dc.distConstrNode.getNextReservedRow(currentDCNodeId);
                    if ( newDCNodeId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                        newDCNodeId = dc.distConstrNode.getNextReservedRow(0); // now it should be fine.
                        if ( ! initConvenienceVariablesConstr()) {
                            General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                            return false;
                        }
                    }
                    if ( newDCNodeId < 0 ) {
                        General.showCodeBug("Failed to get next reserved row in node distance constraint table.");
                        return false;
                    }
                    //General.showDebug( "**********for node rid: " + currentDCNodeId );
                    dc.distConstrNode.copyRow(currentDCNodeId, newDCNodeId);
                    dc.dcMainIdNode[    newDCNodeId ] = newDCId;
                    dc.dcListIdNode[    newDCNodeId ] = newDCListId;
                    //dc.entryIdNode[     newDCNodeId ] = currentEntryId;
                    dc.nodeId[          newDCNodeId ] = 1;
                    dc.downId[          newDCNodeId ] = Defs.NULL_INT; // Not needed because only 1 node per constraint.
                    dc.rightId[         newDCNodeId ] = Defs.NULL_INT;
                    dc.logicalOp[       newDCNodeId ] = Defs.NULL_INT;

                    // FOR EACH NEW member
                    for (int dCMembNumb = 0;dCMembNumb<2;dCMembNumb++) {
                        //General.showDebug("Generating new dc member: for contribution memb: " + dCMembNumb);
                        newDCMembId = dc.distConstrMemb.getNextReservedRow(newDCMembId);
                        if ( newDCMembId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                            newDCMembId = dc.distConstrMemb.getNextReservedRow(0); // now it should be fine.
                            if ( ! initConvenienceVariablesConstr()) {
                                General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                                return false;
                            }
                        }
                        if ( newDCMembId < 0 ) {
                            General.showCodeBug("Failed to get next reserved row in member distance constraint table.");
                            return false;
                        }

                        dc.dcNodeIdMemb[    newDCMembId ] = newDCNodeId;  // funny enough this table has only all-fkc columns
                        dc.dcMainIdMemb[    newDCMembId ] = newDCId;
                        dc.dcListIdMemb[    newDCMembId ] = newDCListId;
                        dc.entryIdMemb[     newDCMembId ] = currentEntryId;
                        dc.numbMemb[        newDCMembId ] = dCMembNumb+1;


                        int dcAtom = Defs.NULL_INT;
                        boolean isInCombo = false;
//                        int obs = Defs.NULL_INT;
                        if ( dCMembNumb == 0) { // switch between the 2 members
                            dcAtom = dcAtomI;
                            isInCombo = isInComboI;
                        } else {
                            dcAtom = dcAtomJ;
                            isInCombo = isInComboJ;
                        }
                        if ( isInCombo ) {
                            // Use the combo list for the new atoms not filling in anything but the atom_main_id
                            PseudoAtom ps = (PseudoAtom) atomsObservableCombos.get( dcAtom );
                            //General.showDebug("Generating new dc atom: " + ps.name + " for atom in combolist.");
                            // FOR EACH NEW atom
                            for (int a=0;a<ps.atomRids.length;a++) {
                                newDCAtomId = dc.distConstrAtom.getNextReservedRow(newDCAtomId);
                                if ( newDCAtomId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                                    newDCAtomId = dc.distConstrAtom.getNextReservedRow(0); // now it should be fine.
                                    if ( ! initConvenienceVariablesConstr() ) {
                                        General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                                        return false;
                                    }
                                }
                                if ( newDCAtomId < 0 ) {
                                    General.showCodeBug("Failed to get next reserved row in atom distance constraint table.");
                                    return false;
                                }
                                //General.showDebug( "filling info -1- into dc atom rid: " + newDCAtomId);
                                dc.atomIdAtom[      newDCAtomId ] = ps.atomRids[ a ];
                                dc.dcMembIdAtom[    newDCAtomId ] = newDCMembId;
                                dc.dcNodeIdAtom[    newDCAtomId ] = newDCNodeId;
                                dc.dcMainIdAtom[    newDCAtomId ] = newDCId;
                                dc.dcListIdAtom[    newDCAtomId ] = newDCListId;
                                dc.entryIdAtom[     newDCAtomId ] = currentEntryId;
                            } // end of loop per new atom
                        } else {
                            currentDCAtomId = dcAtom;
                            //General.showDebug("Generating new dc atom for atom NOT in combolist: " + dc.nameList[currentDCAtomId] );
                            // Simply copy the info from the original dc atom
                            newDCAtomId = dc.distConstrAtom.getNextReservedRow(newDCAtomId);
                            if ( newDCAtomId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                                newDCAtomId = dc.distConstrAtom.getNextReservedRow(0); // now it should be fine.
                                if ( ! initConvenienceVariablesConstr() ) {
                                    General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                                    return false;
                                }
                            }
                            if ( newDCAtomId < 0 ) {
                                General.showCodeBug("Failed to get next reserved row in atom distance constraint table.");
                                return false;
                            }
                            dc.distConstrAtom.copyRow(currentDCAtomId, newDCAtomId);
                            //General.showDebug( "filling info -2- into dc atom rid: " + newDCAtomId);
                            dc.dcMembIdAtom[    newDCAtomId ] = newDCMembId;
                            dc.dcNodeIdAtom[    newDCAtomId ] = newDCNodeId;
                            dc.dcMainIdAtom[    newDCAtomId ] = newDCId;
                            dc.dcListIdAtom[    newDCAtomId ] = newDCListId;
                            //dc.entryIdAtom[     newDCAtomId ] = currentEntryId;
                            if ( Defs.isNull( dc.atomIdAtom[newDCAtomId] )) { // Mark the atom as not linked if it isn't.
                                dc.hasUnLinkedAtom.set( newDCId );
                            }
                        }
                    } // end of loop per new member
                } // end of loop per match
            } // end of loop per node
        } // end of loop per constraint

        // Before returning, free some space; otherwise the rows are still marked -in use-
        boolean status_1 = true; //ui.constr.dcList.mainRelation.cancelAllReservedRows();
        boolean status_2 = dc.mainRelation.cancelAllReservedRows();
        boolean status_3 = dc.distConstrAtom.cancelAllReservedRows();
        boolean status_4 = dc.distConstrMemb.cancelAllReservedRows();
        boolean status_5 = dc.distConstrNode.cancelAllReservedRows();
        boolean status_6 = dc.distConstrViol.cancelAllReservedRows();
        boolean status_overall = status_1 && status_2 && status_3 && status_4 && status_5 && status_6;
        if ( ! status_overall ) {
            General.showError("Failed to cancel all reserved rows in the distance constraint tables that weren't needed.");
            General.showWarning("Removing all entities under newDCListId: " + newDCListId);
            ui.constr.dcList.mainRelation.removeRowCascading(newDCListId,true);
            return false;
        }
        return true;
    }


    /** Remove duplicates considering the same int for different type of refs arent' duplicates.
     */
    public boolean removeDuplicates(IntArrayList dcAtoms, BooleanArrayList isInComboList) {
        for (int i=0;i<dcAtoms.size();i++) {
            int dcAtomId = dcAtoms.getQuick(i);
            boolean isInComboListEach = isInComboList.getQuick(i);
            for (int j=i+1;j<dcAtoms.size();j++) {
                if ( (isInComboListEach == isInComboList.getQuick(j) &&
                        (dcAtomId == dcAtoms.getQuick(j)))) {
                    dcAtoms.remove(j);
                    isInComboList.remove(j);
                    j--; // try the next one which now has the same index
                }
            }
        }
        return true;
    }


    /** BEGIN BLOCK FOR SETTING LOCAL CONVENIENCE VARIABLES COPY FROM Wattos.Star.NMRStar.File31 */
    public boolean initConvenienceVariablesConstr() {
        if ( ! ( dc.resetConvenienceVariables() &&
                dcList.resetConvenienceVariables())) {
            General.showError("Failed to reset convenience variables on dc or dcList");
            return false;
        }
        return true;
    }
    /** END BLOCK */

    /** Mark the contributions with at least 1 not observable atom
     */
    public boolean setSelectionObscured(BitSet todo) {
        int todoCount = todo.cardinality();
        if ( todoCount == 0 ) {
            General.showWarning("No distance constraints selected in Completeness.setSelectionObscured().");
            return false;
        }
        BitSet OSet =  dc.mainRelation.getColumnBit( "OSet" );

        IndexSortedInt indexMainAtom = (IndexSortedInt) dc.distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( indexMainAtom == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return false;
        }

        int currentDCId             = Defs.NULL_INT;
//        int currentDCAtomId         = Defs.NULL_INT;

        // FOR EACH CONSTRAINT
        for (currentDCId = todo.nextSetBit(0);currentDCId>=0;currentDCId = todo.nextSetBit(currentDCId+1)) {
            Integer currentDCIdInteger = new Integer(currentDCId);
            IntArrayList dcAtoms = (IntArrayList) indexMainAtom.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            int atomCount = dcAtoms.size();
            int[] normalAtoms = new int[ atomCount ];
            for (int i=0;i<atomCount;i++) {
                normalAtoms[i] = dc.atomIdAtom[ dcAtoms.getQuick( i ) ];
            }
            boolean status = containsOnlyObservableAtoms( normalAtoms );
            if ( ! status ) {
                OSet.set( currentDCId );
            }
        }
        return true;
    }

    /** Returns true only if all atoms in list are observable. */
    public boolean containsOnlyObservableAtoms( int[] normalAtoms ) {
        int atomCount = normalAtoms.length;
        for (int i=0;i<atomCount;i++) {
            int a = normalAtoms[i];
            if ( ! atomsObservableSet.get( a )) {
                return  false;
            }
        }
        return true;
    }

    /** Look for close contacts between pairs of observable (pseudo)atoms
     *and create new constraints for them if they don't occur in ASet already.
     *
     ** Quick optimalization:
     * Estimate the total number of new dcs that will be added.
     * <BR>
     * The number increases approximately quadratic with the cut-off and
     * the number of observables but can never be more than n*(n-1)/2. The quadratic
     * increase with cut-off distance levels off pretty quickly as the molecule usually
     * isn't infinitely large.
     * <BR>
     * <PRE>
     * // Given the following facts for entry 1HUE:
     * // Number of residues:                      180
     * // Number of observables (pseudo)atoms:     916
     * $ DATA  COMPLETENESS_PER_SHELL
     * $ TABLE #RECORDS 6 #ITEMS 12
     * LowP   UppP     NP 2.0- 3.0- 4.0- 5.0- 6.0- 7.0-   N_RM      C  C_cum
     * 2.00   3.00   1300  206  116   84   67   22    6    501     39     39
     * 3.00   4.00   1705    2  242  221  230   59   18    772     45     42
     * 4.00   5.00   2912    0   14  167  473  100   42    796     27     35
     * 5.00   6.00   4428    0    0    0  230  109   29    368      8     24
     * 6.00   7.00   5631    0    0    0    4   22   17     43      1     16
     * 7.00   8.00   6608    0    0    0    0    0    6      6      0     11
     * $ END
     *
     *Solving the equation for c:
     *e = expected total number
     *n = number of observables
     *d = cutoff distance
     *c = fudge constant
     *e = c*n**2*d**2
     *
     *Gives 1.4e-4 and 1.7e-4 for 7 and 3 Ang respectively.
     *In order to take a little more: 5e-4 was assumed.
     *Underestimates for smaller molecules.
     * </PRE>
     */
    public BitSet addTheoreticalConstraints(BitSet ASet, float max_dist_expected, boolean use_intra) {
        // The returned new theoretical constraint rids
        BitSet result = new BitSet();

        int atomSetCount   = atomsObservableCombos.size();
        if ( avg_method < 0 ) {
            avg_method      = dcList.avgMethod[        newDCListId ];
            monomers = dcList.numberMonomers[   newDCListId ];
        }

        // Sync models if needed.
        if ( ! gumbo.entry.modelsSynced.get( currentEntryId ) ) {
            if ( ! gumbo.entry.syncModels( currentEntryId )) {
                General.showError("Failed to sync models as required for calculating distances for constraints");
            }
            //General.showDebug("Sync-ed models for calculating distances for constraints");
        }

        // Hash the current dcs.
        HashMap currentDCMap = dc.getHashMap(ASet);
        if ( currentDCMap == null ) {
            General.showError("Failed to hash the current set of dcs: dc.getHashMap(ASet)");
            return null;
        }
        //General.showDebug("hash map to dc is: " + Strings.toString( currentDCMap ));

        // precalculated.
        //General.showDebug("Models todo: " + PrimitiveArray.toString( todoModelArray ));

        /** Rids into the specific tables. Values set are to start search, will be reset later.*/
        int newDCAtomId = 0;
        int newDCMembId = 0;
        int newDCNodeId = 0;
        int newDCId     = 0;
        int newDCCount=0;
        int maxCount = atomSetCount*(atomSetCount-1)/2;
        int minCount = atomSetCount;
        float cEstimate = 5.0E-4f;
        int estCount = (int) (cEstimate * atomSetCount * atomSetCount * max_dist_expected * max_dist_expected);
        General.showDebug("Estimated number of theo contacts: " + estCount);
        estCount = Math.max( minCount, estCount );
        estCount = Math.min( maxCount, estCount );
        General.showDebug("Estimated number of theo contacts: " + estCount + " (after range check)");

        dc.mainRelation.reserveRows( estCount * 1);         // Was 5 before, no idea why though...
        dc.distConstrNode.reserveRows( estCount * 1);       // Was 5 before
        dc.distConstrMemb.reserveRows( estCount * 2);      // Was 10 before
        dc.distConstrAtom.reserveRows( estCount * 6);      // Was 30 before

        try {
            // Nothing fancy here just test the whole upper right triangle of the matrix trying not
            // to create too many objects per inner most loop (left/right/model: already 3 levels).
            float[] dist = new float[ todoModelArray.length ];
            for (int a=0;a<atomSetCount;a++) {
                PseudoAtom ps_a = (PseudoAtom) atomsObservableCombos.get( a );
                int ps_a_res_rid =  ps_a.getResRid();
                //General.showDebug("PseudoAtom ps_a: " + ps_a);
                IntArrayList atomRidsA = new IntArrayList( ps_a.atomRids );
                if ( atomRidsA.size() < 1 ) {
                    General.showError("Didn't find a single atom for A member in constraint");
                    return null;
                }
                int atomCountA = atomRidsA.size();
                IntArrayList atomsANew = new IntArrayList();
                atomsANew.setSize(atomCountA);
                for (int b=a+1;b<atomSetCount;b++) {
                    PseudoAtom ps_b = (PseudoAtom) atomsObservableCombos.get( b );
                    int ps_b_res_rid =  ps_b.getResRid();
                    if ( (!use_intra) && (ps_a_res_rid==ps_b_res_rid)) {
                        //General.showDebug("Skipping candidate intra residual contact because we don't want intra residual contacts.");
                        continue;
                    }
                    IntArrayList atomRidsB = new IntArrayList( ps_b.atomRids );
                    //General.showDebug("Found the following rids of atoms in member B: " + PrimitiveArray.toString( atomRidsB ));
                    if ( atomRidsB.size() < 1 ) {
                        General.showError("Didn't find a single atom for B member in constraint");
                        return null;
                    }
                    int atomCountB = atomRidsB.size();
                    IntArrayList atomsBNew = new IntArrayList(atomCountB);
                    atomsBNew.setSize(atomCountB);

                    // FOR EACH selected MODEL
                    for ( int currentModelId=0; currentModelId<todoModelArray.length; currentModelId++) {
                        //General.showDebug("Working on model: " + (currentModelId+1)); // used to seeing model numbers starting at 1.
                        // could be cached for speed...
                        for (int x=0;x<atomCountA;x++) {
                            atomsANew.setQuick( x, gumbo.atom.modelSiblingIds[ atomRidsA.getQuick(x) ][currentModelId]);
                        }
                        // can't be easily cached.
                        for (int x=0;x<atomCountB;x++) {
                            atomsBNew.setQuick( x, gumbo.atom.modelSiblingIds[ atomRidsB.getQuick(x) ][currentModelId]);
                        }
                        //General.showDebug("Found the following rids of atoms in model in member A: " + PrimitiveArray.toString( atomsANew ));
                        //General.showDebug("Found the following rids of atoms in model in member B: " + PrimitiveArray.toString( atomsBNew ));
                        ArrayList atomsInvolvedModel = new ArrayList(); // encapsulating object needed in order to reuse more general code.
                        atomsInvolvedModel.add(new IntArrayList[] {atomsANew, atomsBNew});
                        dist[currentModelId] = gumbo.atom.calcDistance( atomsInvolvedModel, avg_method, monomers );
                        if ( Defs.isNull( dist[currentModelId] ) ) {
                            General.showError("Failed to calculate the distance for constraint between obs: " + a + " and: " + b + " in model: " + (currentModelId+1) + " will try other models.");
                            return null;
                        }
                    }
                    float avgDist = Defs.NULL_FLOAT;
                    if ( doFancyAveraging ) {
                        // if we do use this put in an extra check to see if the
                        // smallest distance is below threshold. Most of the items checked will fail
                        // and this way the expensive R6 averaging doesn't need to be done.
                        avgDist = PrimitiveArray.getAverageR6( dist, avg_power_models ); // Can do any power but usually only power 1 is used.
                    } else {
                        avgDist = PrimitiveArray.getAverage(dist);
                    }
                    if ( Defs.isNull( avgDist)) {
                        General.showCodeBug("failed to PrimitiveArray.getAverage(dist) or PrimitiveArray.getAverageR6( dist, avg_power_models )");
                        return null;
                    }
                    if ( avgDist > max_dist_expected ) { // Stop with this candidate if the averaged distance is above threshold.
                        //General.showDebug("Avg. distance: " + avgDist + " is over threshold: " + max_dist_expected );
                        continue;
                    }
                    //General.showDebug("Avg. distance: " + avgDist + " is at or below threshold: " + max_dist_expected );
                    //General.showDebug("PseudoAtom ps_b: " + ps_b);
                    // todo: add the check to see if the constraint is already present
                    // Get the first atom rids back.
                    int currentModelId=0;
                    for (int x=0;x<atomCountA;x++) {
                        atomsANew.setQuick( x, gumbo.atom.modelSiblingIds[ atomRidsA.getQuick(x) ][currentModelId]);
                    }
                    for (int x=0;x<atomCountB;x++) {
                        atomsBNew.setQuick( x, gumbo.atom.modelSiblingIds[ atomRidsB.getQuick(x) ][currentModelId]);
                    }

                    ArrayList atomsInvolvedModel = new ArrayList(); // encapsulating object needed in order to reuse more general code.
                    atomsInvolvedModel.add(new IntArrayList[] {atomsANew, atomsBNew});
                    dist[currentModelId] = gumbo.atom.calcDistance( atomsInvolvedModel, avg_method, monomers );
                    if ( Defs.isNull( dist[currentModelId] ) ) {
                        General.showError("Failed to calculate the distance for constraint between obs: " + a + " and: " + b + " in model: " + (currentModelId+1) + " will try other models.");
                        return null;
                    }
                    IntArrayList atomsTemp = (IntArrayList) atomsANew.clone();
                    atomsTemp.addAllOf( atomsBNew );
                    int[] atoms = atomsTemp.elements();
                    atoms = PrimitiveArray.resizeArray(atoms, atomsTemp.size());


                    // BEGIN BLOCK MAKE SURE THE HASHING METHOD IS THE SAME AS ELSEWHERE FOR THIS OBJECT
                    Arrays.sort( atoms );
                    //General.showDebug("Using elements: " + PrimitiveArray.toString(atoms));
                    int hash = PrimitiveArray.hashCode(atoms); // precalculate the hash code.
                    //General.showDebug("Has hash code : " + hash);
                    // in java 1.5 use the Arrays.hashCode method.
                    // END BLOCK MAKE SURE THE HASHING METHOD IS THE SAME AS ELSEWHERE FOR THIS OBJECT

                    //dc.mainRelation.hash[ ridTemp ] = hash;
                    Integer hashInt = new Integer( hash );
                    /** Find one or more matching constraints and put them in newDCIdList
                     */
                    IntArrayList newDCIdList = new IntArrayList();
                    if ( currentDCMap.containsKey( hashInt ) ) { // uses the hash just set.
                        //General.showDebug("This is an OLD contribution. ");
                        ArrayList ridList = (ArrayList) currentDCMap.get( hashInt );
                        for (int r = 0; r<ridList.size(); r++ ) {
                            newDCIdList.add( ((Integer) ridList.get( r )).intValue());
                            /**
                             * if ( r > 0 ) {
                             * General.showWarning("Matching theo with more than one obs; they should be noted in set s and thus taken out in B");
                             * }
                             */
                        }
                    } else {
                        //General.showDebug("This is a new contribution");
                        // add the constraint if not present.
                        newDCId = dc.mainRelation.getNextReservedRow(0);  // optimize so doesn't need a scan.
                        if ( newDCId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                            newDCId = dc.mainRelation.getNextReservedRow(0); // now it should be fine.
                            if ( ! initConvenienceVariablesConstr()) {
                                General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                                return null;
                            }
                        }
                        if ( newDCId < 0 ) {
                            General.showCodeBug("Failed to get next reserved row in main distance constraint table.");
                            return null;
                        }
                        newDCIdList.add(newDCId);
                        newDCCount++;
                        dc.number[                  newDCId ] = newDCCount;
                        dc.dcListIdMain[            newDCId ] = newDCListId;
                        dc.entryIdMain[             newDCId ] = currentEntryId;
                        dc.selected.set(            newDCId );
                        dc.hasUnLinkedAtom.clear(   newDCId ); // this is false for the theos.

                        // EACH NEW DC node
                        newDCNodeId = dc.distConstrNode.getNextReservedRow(newDCNodeId);
                        if ( newDCNodeId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                            newDCNodeId = dc.distConstrNode.getNextReservedRow(0); // now it should be fine.
                            if ( ! initConvenienceVariablesConstr()) {
                                General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                                return null;
                            }
                        }
                        if ( newDCNodeId < 0 ) {
                            General.showCodeBug("Failed to get next reserved row in node distance constraint table.");
                            return null;
                        }
                        //General.showDebug( "**********for node rid: " + newDCNodeId );
                        dc.dcMainIdNode[    newDCNodeId ] = newDCId;
                        dc.dcListIdNode[    newDCNodeId ] = newDCListId;
                        dc.entryIdNode[     newDCNodeId ] = currentEntryId;
                        dc.nodeId[          newDCNodeId ] = 1;
                        dc.downId[          newDCNodeId ] = Defs.NULL_INT; // Not needed because only 1 node per constraint.
                        dc.rightId[         newDCNodeId ] = Defs.NULL_INT;
                        dc.logicalOp[       newDCNodeId ] = Defs.NULL_INT;
                        dc.target[          newDCNodeId ] = avgDist; // so they aren't redundant unless they're a fixed distance.

                        // FOR EACH NEW member
                        for (int dCMembNumb = 0;dCMembNumb<2;dCMembNumb++) {
                            //General.showDebug("Generating new dc member: for contribution memb: " + dCMembNumb);
                            newDCMembId = dc.distConstrMemb.getNextReservedRow(newDCMembId);
                            if ( newDCMembId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                                newDCMembId = dc.distConstrMemb.getNextReservedRow(0); // now it should be fine.
                                if ( ! initConvenienceVariablesConstr()) {
                                    General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                                    return null;
                                }
                            }
                            if ( newDCMembId < 0 ) {
                                General.showCodeBug("Failed to get next reserved row in member distance constraint table.");
                                return null;
                            }

                            dc.dcNodeIdMemb[    newDCMembId ] = newDCNodeId;  // funny enough this table has only all-fkc columns
                            dc.dcMainIdMemb[    newDCMembId ] = newDCId;
                            dc.dcListIdMemb[    newDCMembId ] = newDCListId;
                            dc.entryIdMemb[     newDCMembId ] = currentEntryId;
                            dc.numbMemb[        newDCMembId ] = dCMembNumb+1;

                            IntArrayList dcAtoms = null;
                            if ( dCMembNumb == 0) { // switch between the 2 members
                                dcAtoms = atomRidsA;
                            } else {
                                dcAtoms = atomRidsB;
                            }
                            // FOR EACH NEW atom
                            for (int dCAtomNumb = 0;dCAtomNumb<dcAtoms.size();dCAtomNumb++) {
                                // Simply copy the info from the original dc atom
                                newDCAtomId = dc.distConstrAtom.getNextReservedRow(newDCAtomId);
                                if ( newDCAtomId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                                    newDCAtomId = dc.distConstrAtom.getNextReservedRow(0); // now it should be fine.
                                    if ( ! initConvenienceVariablesConstr() ) {
                                        General.showCodeBug("Failed to initConvenienceVariablesConstr.");
                                        return null;
                                    }
                                }
                                if ( newDCAtomId < 0 ) {
                                    General.showCodeBug("Failed to get next reserved row in atom distance constraint table.");
                                    return null;
                                }
                                //General.showDebug( "filling info -3- into dc atom rid: " + newDCAtomId);
                                dc.dcMembIdAtom[    newDCAtomId ] = newDCMembId;
                                dc.dcNodeIdAtom[    newDCAtomId ] = newDCNodeId;
                                dc.dcMainIdAtom[    newDCAtomId ] = newDCId;
                                dc.dcListIdAtom[    newDCAtomId ] = newDCListId;
                                dc.entryIdAtom[     newDCAtomId ] = currentEntryId;
                                dc.atomIdAtom[      newDCAtomId ] = dcAtoms.getQuick(dCAtomNumb);
                                if ( Defs.isNull( dc.atomIdAtom[newDCAtomId] )) { // Mark the atom as not linked if it isn't.
                                    General.showCodeBug("filled dc atom with undefined atom for theo obs atom; that can't be true");
                                    return null;
                                }
                            } // end of loop per new atom
                        } // end of loop per new member
                    } // end of else making a new dc
                    if ( newDCIdList.size() < 1 ) {
                        General.showError("failed to find or create a single theo dc for a pair");
                        return null;
                    }
                    for (int i=0;i<newDCIdList.size();i++ ) {
                        newDCId = newDCIdList.getQuick(i);
                        dc.distTheo[ newDCId ]   = avgDist; // set the found distance in old or new dc id.
                        result.set( newDCId ); // mark as theo obs.
                    }
                } // end of loop per obs B
            } // end of loop per obs A
        } finally {
            // Before returning, free some space; otherwise the rows are still marked -in use-
            boolean status_1 = true; //ui.constr.dcList.mainRelation.cancelAllReservedRows();
            boolean status_2 = dc.mainRelation.cancelAllReservedRows();
            boolean status_3 = dc.distConstrAtom.cancelAllReservedRows();
            boolean status_4 = dc.distConstrMemb.cancelAllReservedRows();
            boolean status_5 = dc.distConstrNode.cancelAllReservedRows();
            boolean status_6 = dc.distConstrViol.cancelAllReservedRows();
            boolean status_overall = status_1 && status_2 && status_3 && status_4 && status_5 && status_6;
            if ( ! status_overall ) {
                General.showError("Failed to cancel all reserved rows in the distance constraint tables that weren't needed.");
                General.showWarning("Removing all entities under newDCListId: " + newDCListId);
                dcList.mainRelation.removeRowCascading(newDCListId,true);
                return null;
            }
        }
        return result;
    }

    /**
     * Reduce the number of new dcs by choosing the closest contacts in the models.
     * <PRE>
     * E.g.
     * HA,[HB2,HB3] with [MD1,MD2] should reduce to:
     * HA    - MD2       if that distance is shorter than HA MD1
     * HB2   - MD1       if that's the shortest distance among the permutations
     * Note that this assumes HB2 and HB3 aren't seperately observed. If they are they should be in a different
     * node or more likely still in a different contraint al toghether.
     *
     * The result will be an ArrayList of matchedContributions which is an Object[] of 2 Integers and 2 Booleans:
     * dcAtomI,dcAtomJ,isInComboI,isInComboJ. Kind of expensive at the price
     * of 5 extra encapsulating objects per matchedContribution.
     *
     * Pseudo code for doing this
     * for each left (pseudo)atom
     *     add related left atom to candiates if present
     *     for each right (pseudo)atom
     *         add related right atom to candiates if present
     *         for each of the combinations (1, 2, 2, or 4 combinations total depending on presence of related left/right atoms)
     *             calculate distance combination
     *         add the ONE shortest combination of them all to result
     * </PRE>
     *
     * Again, dcAtomsI and J are pointers to the combolist elements or if the atoms aren't in the combolist
     * they are pointers to a dcatom record.
     */
    private ArrayList matchConstraints(
            IntArrayList dcAtomsI,
            IntArrayList dcAtomsJ,
            BooleanArrayList isInComboListI,
            BooleanArrayList isInComboListJ) {

            ArrayList result = new ArrayList();
            // rid into dcAtomsI and J
            IntArrayList matchedAtomsI = new IntArrayList(); // contains pointers into dcAtomsI
            IntArrayList matchedAtomsJ = new IntArrayList();
            /** ? */
            BooleanArrayList skipDCAtomsI = new BooleanArrayList();
            skipDCAtomsI.setSize(dcAtomsI.size()); // will be initialized at false
            int[] shortestMatchIdices = new int[2]; // point to element in matchedAtomsI and J
            for (int i=0;i<dcAtomsI.size();i++ ) {
//                General.showDebug("matchConstraints at I: " + i);
                if ( skipDCAtomsI.getQuick(i) ) {
//                    General.showDebug("matchConstraints at I: " + i + " skipping because it was a sibling before");
                    continue; // the atom was a sibling and doesn't need to be considered again.
                }
                matchedAtomsI.setSize(0); // recycled for efficiency.
                matchedAtomsI.add( i );
                int sibling = getIdxSibling( dcAtomsI, isInComboListI, i ); // temp id into the dcAtomsI array.
                if ( Defs.isNull( sibling ) ) {
                    //General.showError("failed to do getIdxSibling");
                    return null;
                }
                if ( sibling == NO_SIBLING_FOUND ) {
//                    General.showDebug("No sibling I found");
                } else {
//                    General.showDebug("sibling I found at: " + sibling);
                    matchedAtomsI.add( sibling );
                    skipDCAtomsI.set( sibling, true ); // will be skipped next time around
                }

                BooleanArrayList skipDCAtomsJ = new BooleanArrayList(); // code might be optimize by caching
                skipDCAtomsJ.setSize(dcAtomsJ.size());
                for (int j=0;j<dcAtomsJ.size();j++ ) {
//                    General.showDebug("Trying dcAtomsJ: " + j + " with rid: " + dcAtomsJ.getQuick(j));
                    if ( skipDCAtomsJ.getQuick(j) ) {
                        continue; // the atom was a sibling and doesn't need to be considered again.
                    }
                    matchedAtomsJ.setSize(0); // recycled for efficiency.
                    matchedAtomsJ.add( j );
                    sibling = getIdxSibling( dcAtomsJ, isInComboListJ, j );
                    if ( Defs.isNull( sibling ) ) {
                        General.showError("failed to do getIdxSibling");
                        return null;
                    }
                    if ( sibling != NO_SIBLING_FOUND ) {
//                        General.showDebug("sibling J found at: " + sibling);
                        matchedAtomsJ.add( sibling );
                        skipDCAtomsJ.set( sibling, true ); // will be skipped next time around
                    } else {
//                        General.showDebug("No sibling J found");
                    }
                    int combinationCount = matchedAtomsI.size() * matchedAtomsJ.size(); // temp id
//                    General.showDebug("found number of combinations: " + combinationCount );
                    if ( combinationCount > 1 ) {
                        shortestMatchIdices = findShortestMatch(dcAtomsI, dcAtomsJ, isInComboListI, isInComboListJ,
                                matchedAtomsI, matchedAtomsJ); // returns pointers into matchedAtomsI and J
                        if ( shortestMatchIdices == null ) {
//                            General.showDebug("Failed to findShortestMatch");
                            return null;
                        }
                    } else { // int[] is always initialized so this is safe.
                        shortestMatchIdices[0] = 0;
                        shortestMatchIdices[1] = 0;
                    }
//                    General.showDebug("shortestMatchIdices now at: " + PrimitiveArray.toString(shortestMatchIdices) );
                    // damn expensive objects...
                    Object[] match = new Object[4] ;
                    int dcAtomIIdx = matchedAtomsI.getQuick( shortestMatchIdices[ 0 ]);
                    int dcAtomJIdx = matchedAtomsJ.getQuick( shortestMatchIdices[ 1 ]);
//                    General.showDebug("matchedAtomsI : " + PrimitiveArray.toString( matchedAtomsI ));
//                    General.showDebug("matchedAtomsJ : " + PrimitiveArray.toString( matchedAtomsJ ));
//                    General.showDebug("dcAtomIIdx at: " + dcAtomIIdx );
//                    General.showDebug("dcAtomJIdx at: " + dcAtomJIdx );
                    match[0] = new Integer( dcAtomIIdx);
                    match[1] = new Integer( dcAtomJIdx);
                    match[2] = Boolean.valueOf( isInComboListI.get(   dcAtomIIdx ));
                    match[3] = Boolean.valueOf( isInComboListJ.get(   dcAtomJIdx ));
                    result.add( match );
                }
            }
            return result;
    }

    /**Checks the combinations of matchedAtomsI and matchedAtomsJ for the shortest distance.
     *An unknown distance is assumed to be larger than any known distance.
     *Returns the index into matchedAtomsI and J or null on error.
     */
    private int[] findShortestMatch(
            IntArrayList dcAtomsI,           IntArrayList dcAtomsJ,
            BooleanArrayList isInComboListI, BooleanArrayList isInComboListJ,
            IntArrayList matchedAtomsI,      IntArrayList matchedAtomsJ) {

        int[] shortestMatchIdices = new int[2]; // points to
        shortestMatchIdices[0] = 0; // by default get's update on every iteration
        shortestMatchIdices[1] = 0;
//        int combinationCount = dcAtomsI.size()*dcAtomsJ.size();
        float shortestDistance = Float.MAX_VALUE;

        // Nothing fancy here just test the whole upper right triangle of the matrix trying not
        // to create too many objects per inner most loop (left/right/model: already 3 levels).
        float[] dist = new float[ todoModelArray.length ]; // distances per model recycled object per candidate match
        for (int i=0;i<matchedAtomsI.size();i++) {
            int dcAtomsIIdx = matchedAtomsI.getQuick(i);
            int dcATomI = dcAtomsI.getQuick( dcAtomsIIdx );
            if ( ! isInComboListI.get( dcAtomsIIdx )) {
                //General.showDebug("skipping unknown distance due to atom I");
                continue; // leads to defaulting to first candiate if nothing else.
            }

            PseudoAtom ps_i = (PseudoAtom) atomsObservableCombos.get( dcATomI );
            //General.showDebug("PseudoAtom ps_i: " + ps_i);
            IntArrayList atomRidsI = new IntArrayList( ps_i.atomRids );
            if ( atomRidsI.size() < 1 ) {
                General.showError("Didn't find a single atom for I member in constraint");
                return null;
            }
            int atomCountI = atomRidsI.size();
            IntArrayList atomsINew = new IntArrayList();
            atomsINew.setSize(atomCountI);
            for (int j=0;j<matchedAtomsJ.size();j++) {
                int dcAtomsJIdx = matchedAtomsJ.getQuick(j);
                int dcATomJ = dcAtomsJ.getQuick( dcAtomsJIdx );
                if ( ! isInComboListJ.get( dcAtomsJIdx )) {
                    //General.showDebug("skipping unknown distance due to atom J");
                    continue;
                }

                PseudoAtom ps_j = (PseudoAtom) atomsObservableCombos.get( dcATomJ );
                //General.showDebug("PseudoAtom ps_j: " + ps_j);
                IntArrayList atomRidsJ = new IntArrayList( ps_j.atomRids );
                //General.showDebug("Found the following rids of atoms in member J: " + PrimitiveArray.toString( atomRidsJ ));
                if ( atomRidsJ.size() < 1 ) {
                    General.showError("Didn't find a single atom for J member in constraint");
                    return null;
                }
                int atomCountJ = atomRidsJ.size();
                IntArrayList atomsJNew = new IntArrayList(atomCountJ);
                atomsJNew.setSize(atomCountJ);

                // FOR EACH selected MODEL
                for ( int currentModelId=0; currentModelId<todoModelArray.length; currentModelId++) {
                    //General.showDebug("Working on model: " + (currentModelId+1)); // used to seeing model numbers starting at 1.
                    // could be cached for speed...
                    for (int x=0;x<atomCountI;x++) {
                        atomsINew.setQuick( x, gumbo.atom.modelSiblingIds[ atomRidsI.getQuick(x) ][currentModelId]);
                    }
                    // can't be easily cached.
                    for (int x=0;x<atomCountJ;x++) {
                        atomsJNew.setQuick( x, gumbo.atom.modelSiblingIds[ atomRidsJ.getQuick(x) ][currentModelId]);
                    }
                    //General.showDebug("Found the following rids of atoms in model in member I: " + PrimitiveArray.toString( atomsINew ));
                    //General.showDebug("Found the following rids of atoms in model in member J: " + PrimitiveArray.toString( atomsJNew ));
                    ArrayList atomsInvolvedModel = new ArrayList(); // encapsulating object needed in order to reuse more general code.
                    atomsInvolvedModel.add(new IntArrayList[] {atomsINew, atomsJNew});
                    dist[currentModelId] = gumbo.atom.calcDistance( atomsInvolvedModel, avg_method, monomers );
                    if ( Defs.isNull( dist[currentModelId] ) ) {
                        General.showError("Failed to calculate the distance for constraint between obs: " + i + " and: " + j + " in model: " + (currentModelId+1) + " will try other models.");
                        return null;
                    }
                }
                float avgDist = Defs.NULL_FLOAT;
                if ( doFancyAveraging ) {
                    // if we do use this put in an extra check to see if the
                    // smallest distance is below threshold. Most of the items checked will fail
                    // and this way the expensive R6 averaging doesn't need to be done.
                    avgDist = PrimitiveArray.getAverageR6( dist, avg_power_models ); // Can do any power but usually only power 1 is used.
                } else {
                    avgDist = PrimitiveArray.getAverage(dist);
                }
                if ( Defs.isNull( avgDist)) {
                    General.showCodeBug("failed to PrimitiveArray.getAverage(dist) or PrimitiveArray.getAverageR6( dist, avg_power_models )");
                    return null;
                }
                //General.showDebug("Avg. distance: " + avgDist + " is perhaps below shortest distance so far: " + shortestDistance );
                if ( avgDist < shortestDistance ) {
                    shortestMatchIdices[0] = i;
                    shortestMatchIdices[1] = j;
                    shortestDistance = avgDist;
                }
                //General.showDebug("shortestDistance now: " + shortestDistance );
            }
        }
        return shortestMatchIdices;
    }

    /** Look for atoms that are in the same pseudoatom as the atom at prevIdx in dcAtoms
     * from prevIdx+1 and on.
     *Return NO_SIBLING_FOUND for success but no sibling encountered or
     *Return Defs.Null for error (never happens)
     *Return the idx of dcAtoms if a sibling was found.
     *Notes: dcAtoms and isInComboList run parallel.
     */
    private int getIdxSibling(IntArrayList dcAtoms, BooleanArrayList isInComboList, int prevIdx) {
        int dcAtomsSize = dcAtoms.size();
        if ( prevIdx >= (dcAtomsSize-1)) { // efficiency early abort check.
            return NO_SIBLING_FOUND;
        }
        if ( ! isInComboList.get( prevIdx ) ) { // efficiency early abort check.
            return NO_SIBLING_FOUND;
        }
        PseudoAtom psPrev = (PseudoAtom) atomsObservableCombos.get( dcAtoms.get( prevIdx ));
//        General.showDebug("(Pseudo)atom found: "+ psPrev);
        int firstAtomRid        = psPrev.atomRids[0];
        String firstAtomName    = gumbo.atom.nameList[firstAtomRid]; // can only be a regular atom like: HB3
        int currentResRid       = gumbo.atom.resId[ firstAtomRid ];
        String currentResName   = gumbo.res.nameList[ currentResRid ];
        String[] atom_name_list = new String[2];
        atom_name_list[0] = firstAtomName;

        for (int i=(prevIdx+1);i<dcAtoms.size();i++) { // usually 1 or 2 in length.
            boolean candidateInCombo = isInComboList.get( i );
//            General.showDebug("Looking for sibling in dcAtoms element: "+ i + " rid: " + dcAtoms.get( i ) + " which is in combo: " + candidateInCombo);
            // This check is new as introduced for entry 2djy in which a lot of stereo atom coordinates are absent.
            if ( ! candidateInCombo ) {
//                General.showDebug("Candidate is NOT in combo");
                return NO_SIBLING_FOUND;
            }
            PseudoAtom psNext = (PseudoAtom) atomsObservableCombos.get(dcAtoms.get( i ));
            int nextAtomRid         = psNext.atomRids[0];
            int nextResRid          = gumbo.atom.resId[ nextAtomRid ];
            if ( currentResRid != nextResRid ) {
                continue;
            }
            String nextAtomName     = gumbo.atom.nameList[nextAtomRid]; // can only be a regular atom like: HB2
            atom_name_list[1] = nextAtomName;
            String commonParent = ui.wattosLib.pseudoLib.getCommonPseudoParent( currentResName, atom_name_list, false, false); // there's a faster check needed perhaps
            if ( commonParent != null ) {
                //General.showDebug("Found a sibling in: " + psNext);
                return i;
            }
        }
        return NO_SIBLING_FOUND;
    }

    /**
     * @return <CODE>true</CODE> for success
     */
    public boolean initConvenienceVariablesStar() {
        // Please note that the following names are not hard-coded as star names.
        try {
            tagNameNOE_compl_listSf_category        = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Sf_category    ");
//            tagNameNOE_compl_listEntry_ID           = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Entry_ID       ");
//            tagNameNOE_compl_listCompl_list_ID      = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Compl_list_ID  ");
            tagNameNOE_compl_listRestraint_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Restraint_count");
            tagNameNOE_compl_listModel_count        = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Model_count    ");
            tagNameNOE_compl_listResidue_count      = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Residue_count  ");
            tagNameNOE_compl_listObserv_atoms       = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Observ_atoms   ");
            tagNameNOE_compl_listUse_intra_resid    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Use_intra_resid");
            tagNameNOE_compl_listThreshold_redun    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Threshold_redun");
            tagNameNOE_compl_listAveraging_power    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Averaging_power");
            tagNameNOE_compl_listCompl_cutoff       = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Compl_cutoff   ");
            tagNameNOE_compl_listCompl_cumul        = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Compl_cumul    ");
            tagNameNOE_compl_listRst_unexp_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_unexp_count");
            tagNameNOE_compl_listPair_count         = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Pair_count     ");
            tagNameNOE_compl_listRst_match_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_match_count");
            tagNameNOE_compl_listRst_unmat_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_unmat_count");
            tagNameNOE_compl_listObs_atom_count     = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Obs_atom_count ");
            tagNameNOE_compl_listTot_atom_count     = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Tot_atom_count ");
            tagNameNOE_compl_listRst_excep_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_excep_count");
            tagNameNOE_compl_listRst_nonob_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_nonob_count");
            tagNameNOE_compl_listRst_intra_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_intra_count");
            tagNameNOE_compl_listRst_surpl_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_surpl_count");
            tagNameNOE_compl_listRst_obser_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_obser_count");
            tagNameNOE_compl_listRst_expec_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_expec_count");
            tagNameNOE_compl_listRst_exnob_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Rst_exnob_count");
            tagNameNOE_compl_listDetails            = starDict.getTagName( "NOE_completeness","_NOE_compl_list.Details        ");
            tagNameNOE_compl_clasType               = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Type           ");
            tagNameNOE_compl_clasRst_obser_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Rst_obser_count");
            tagNameNOE_compl_clasRst_expec_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Rst_expec_count");
            tagNameNOE_compl_clasRst_match_count    = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Rst_match_count");
            tagNameNOE_compl_clasCompl_cumul        = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Compl_cumul    ");
            tagNameNOE_compl_clasStand_deviat       = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Stand_deviat   ");
            tagNameNOE_compl_clasDetails            = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Details        ");
//            tagNameNOE_compl_clasEntry_ID           = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Entry_ID       ");
//            tagNameNOE_compl_clasCompl_list_ID      = starDict.getTagName( "NOE_completeness","_NOE_compl_clas.Compl_list_ID  ");
            tagNameNOE_compl_resEntity_ID           = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Entity_ID       ");
            tagNameNOE_compl_resComp_index_ID       = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Comp_index_ID   ");
            tagNameNOE_compl_resComp_ID             = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Comp_ID         ");
            tagNameNOE_compl_resObs_atom_count      = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Obs_atom_count  ");
            tagNameNOE_compl_resRst_obser_count     = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Rst_obser_count");
            tagNameNOE_compl_resRst_expec_count     = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Rst_expec_count");
            tagNameNOE_compl_resRst_match_count     = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Rst_match_count");
            tagNameNOE_compl_resCompl_cumul         = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Compl_cumul     ");
            tagNameNOE_compl_resStand_deviat        = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Stand_deviat    ");
            tagNameNOE_compl_resDetails             = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Details         ");
//            tagNameNOE_compl_resEntry_ID            = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Entry_ID        ");
//            tagNameNOE_compl_resCompl_list_ID       = starDict.getTagName( "NOE_completeness","_NOE_compl_res.Compl_list_ID   ");
            tagNameNOE_compl_shellShell_start       = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Shell_start    ");
            tagNameNOE_compl_shellShell_end         = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Shell_end      ");
            tagNameNOE_compl_shellExpected_NOEs     = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Rst_expec_count");
            tagNameNOE_compl_shellObs_NOEs_total    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Rst_obser_count");
            tagNameNOE_compl_shellMatched_NOEs      = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Rst_match_count");
            tagNameNOE_compl_shellObs_NOEs_shl_1    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_1 ");
            tagNameNOE_compl_shellObs_NOEs_shl_2    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_2 ");
            tagNameNOE_compl_shellObs_NOEs_shl_3    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_3 ");
            tagNameNOE_compl_shellObs_NOEs_shl_4    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_4 ");
            tagNameNOE_compl_shellObs_NOEs_shl_5    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_5 ");
            tagNameNOE_compl_shellObs_NOEs_shl_6    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_6 ");
            tagNameNOE_compl_shellObs_NOEs_shl_7    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_7 ");
            tagNameNOE_compl_shellObs_NOEs_shl_8    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_8 ");
            tagNameNOE_compl_shellObs_NOEs_shl_9    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_9 ");
            tagNameNOE_compl_shellObs_NOEs_shl_o    = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Matched_shel_o ");
            tagNameNOE_compl_shellDetails           = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Details ");
            tagNameNOE_compl_shellCompl_shell       = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Compl_shell    ");
            tagNameNOE_compl_shellCompl_cumul       = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Compl_cumul    ");
//            tagNameNOE_compl_shellEntry_ID          = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Entry_ID       ");
//            tagNameNOE_compl_shellCompl_list_ID     = starDict.getTagName( "NOE_completeness","_NOE_compl_shel.Compl_list_ID  ");

        } catch ( Exception e ) {
            General.showThrowable(e);
            General.showError("Failed to get all the tag names from dictionary compare code with dictionary");
            return false;
        }
        if (
                tagNameNOE_compl_listSf_category == null ||
//                tagNameNOE_compl_listEntry_ID == null ||
//                tagNameNOE_compl_listCompl_list_ID == null ||
                tagNameNOE_compl_listRestraint_count == null ||
                tagNameNOE_compl_listModel_count == null ||
                tagNameNOE_compl_listResidue_count == null ||
                tagNameNOE_compl_listObserv_atoms == null ||
                tagNameNOE_compl_listUse_intra_resid == null ||
                tagNameNOE_compl_listThreshold_redun == null ||
                tagNameNOE_compl_listAveraging_power == null ||
                tagNameNOE_compl_listCompl_cutoff == null ||
                tagNameNOE_compl_listCompl_cumul == null ||
                tagNameNOE_compl_listPair_count == null ||
                tagNameNOE_compl_listRst_unexp_count==null||
                tagNameNOE_compl_listRst_match_count == null ||
                tagNameNOE_compl_listRst_unmat_count == null ||
                tagNameNOE_compl_listObs_atom_count == null ||
                tagNameNOE_compl_listTot_atom_count == null ||
                tagNameNOE_compl_listRst_excep_count==null||
                tagNameNOE_compl_listRst_nonob_count==null||
                tagNameNOE_compl_listRst_intra_count==null||
                tagNameNOE_compl_listRst_surpl_count==null||
                tagNameNOE_compl_listRst_obser_count==null||
                tagNameNOE_compl_listRst_expec_count==null||
                tagNameNOE_compl_listRst_exnob_count==null||
                tagNameNOE_compl_listDetails == null ||
                tagNameNOE_compl_clasType == null ||
                tagNameNOE_compl_clasRst_obser_count==null||
                tagNameNOE_compl_clasRst_expec_count==null||
                tagNameNOE_compl_clasRst_match_count==null||
                tagNameNOE_compl_clasCompl_cumul == null ||
                tagNameNOE_compl_clasStand_deviat == null ||
                tagNameNOE_compl_clasDetails == null ||
//                tagNameNOE_compl_clasEntry_ID == null ||
//                tagNameNOE_compl_clasCompl_list_ID == null ||
                tagNameNOE_compl_resEntity_ID == null ||
                tagNameNOE_compl_resComp_index_ID == null ||
                tagNameNOE_compl_resComp_ID == null ||
                tagNameNOE_compl_resObs_atom_count == null ||
                tagNameNOE_compl_resRst_obser_count==null||
                tagNameNOE_compl_resRst_expec_count==null||
                tagNameNOE_compl_resRst_match_count==null||
                tagNameNOE_compl_resCompl_cumul == null ||
                tagNameNOE_compl_resStand_deviat == null ||
                tagNameNOE_compl_resDetails == null ||
//                tagNameNOE_compl_resEntry_ID == null ||
//                tagNameNOE_compl_resCompl_list_ID == null ||
                tagNameNOE_compl_shellShell_start == null ||
                tagNameNOE_compl_shellShell_end == null ||
                tagNameNOE_compl_shellExpected_NOEs == null ||
                tagNameNOE_compl_shellObs_NOEs_total == null ||
                tagNameNOE_compl_shellMatched_NOEs == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_1 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_2 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_3 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_4 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_5 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_6 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_7 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_8 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_9 == null ||
                tagNameNOE_compl_shellObs_NOEs_shl_o == null ||
                tagNameNOE_compl_shellDetails        == null ||
                tagNameNOE_compl_shellObs_NOEs_total == null ||
                tagNameNOE_compl_shellCompl_shell == null ||
                tagNameNOE_compl_shellCompl_cumul == null
//                tagNameNOE_compl_shellEntry_ID == null ||
//                tagNameNOE_compl_shellCompl_list_ID == null
                ) {
            General.showError("Failed to get all the tag names from dictionary, compare code with dictionary.");
            return false;
        }

//        if ( false ) {
//            String[] tagNames = {
//                tagNameNOE_compl_listSf_category
//            };
//            General.showDebug("Tagnames:\n"+Strings.toString(tagNames,true));
//        }


        return true;
    }

    /** Returns a template with the star formatted output template
     */
    private SaveFrame getSFTemplate() {
        SaveFrame sF = new SaveFrame();
        sF.setTitle("NOE_Completeness");
        // Default variables.
        HashMap             namesAndTypes;
        ArrayList           order;
        HashMap             namesAndValues;
        TagTable            tT;
//        int                 RID;
        try {
            // INTRO
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            namesAndValues          = new HashMap();
            DBMS dbms = new DBMS(); // Use a temporary dbms for this because we don't
            // want to hold on to this data for ever.
            tT                      = new TagTable("NOE_compl_list", dbms);
            tT.isFree               = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            String cat = "NOE_completeness_statistics";
            namesAndValues.put( tagNameNOE_compl_listSf_category, cat);
//            namesAndValues.put( tagNameNOE_compl_listEntry_ID, new Integer(1));
//            namesAndValues.put( tagNameNOE_compl_listCompl_list_ID, new Integer(1));

            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listSf_category     );
//            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listEntry_ID        );
//            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listCompl_list_ID   );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listModel_count     );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listResidue_count   );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listTot_atom_count  );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listObserv_atoms    );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listObs_atom_count  );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listUse_intra_resid );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listThreshold_redun );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listAveraging_power );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listCompl_cutoff    );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listCompl_cumul     );

            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_unexp_count );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRestraint_count );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listPair_count      );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_excep_count ); //new
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_nonob_count ); //new
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_intra_count ); //new
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_surpl_count ); //new
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_obser_count ); //new
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_expec_count ); //new
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_match_count );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_unmat_count );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listRst_exnob_count ); //new
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_listDetails         );


            // Append columns after order id column.
            if ( ! tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add( tT );

            // CLASS
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            namesAndValues          = new HashMap();
            tT                      = new TagTable("NOE_compl_clas", dbms);
            tT.isFree = false;
//            namesAndValues.put( tagNameNOE_compl_clasEntry_ID, new Integer(1));
//            namesAndValues.put( tagNameNOE_compl_clasCompl_list_ID, new Integer(1));

            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasType            );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasRst_obser_count );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasRst_expec_count );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasRst_match_count );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasCompl_cumul     );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasStand_deviat    );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasDetails         );
//            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasEntry_ID        );
//            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_clasCompl_list_ID   );

            if ( ! tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add( tT );




            // SHELL
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            namesAndValues          = new HashMap();
            tT                      = new TagTable("NOE_compl_shell", dbms);
            tT.isFree = false;
//            namesAndValues.put( tagNameNOE_compl_shellEntry_ID, new Integer(1));
//            namesAndValues.put( tagNameNOE_compl_shellCompl_list_ID, new Integer(1));

            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellDetails        );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellShell_start    );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellShell_end      );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellExpected_NOEs  );
            //starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_total );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellMatched_NOEs   );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_1 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_2 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_3 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_4 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_5 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_6 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_7 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_8 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_9 );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellObs_NOEs_shl_o );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellCompl_shell    );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellCompl_cumul    );
//            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellEntry_ID       );
//            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_shellCompl_list_ID  );

            if ( ! tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add( tT );

            // RESIDUE
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            namesAndValues          = new HashMap();
            tT                      = new TagTable("NOE_compl_res", dbms);
            tT.isFree = false;
//            namesAndValues.put( tagNameNOE_compl_resEntry_ID, new Integer(1));
//            namesAndValues.put( tagNameNOE_compl_resCompl_list_ID, new Integer(1));

            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resEntity_ID        );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resComp_index_ID    );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resComp_ID          );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resObs_atom_count   );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resRst_obser_count);
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resRst_expec_count);
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resRst_match_count);
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resCompl_cumul      );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resStand_deviat     );
            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resDetails          );
//            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resEntry_ID         );
//            starDict.putFromDict( namesAndTypes, order, tagNameNOE_compl_resCompl_list_ID    );

            if ( ! tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add( tT );

        } catch ( Exception e ) {
            General.showThrowable(e);
            return null;
        }
        return sF;
    }

    public boolean showPlotPerResidue(URL url, boolean saveImage, String file_name_base_dc) {
        StarFileReader sfr = new StarFileReader(ui.dbms);
        StarNode sn = sfr.parse( url );
        if ( sn == null ) {
            General.showError("Parse not successful");
            return false;
        }
        General.showDebug("Parse successful");
        // data type modifications on column
        ArrayList tTList = (ArrayList) sn.getTagTableList(StarGeneral.WILDCARD,
                StarGeneral.WILDCARD, StarGeneral.WILDCARD, tagNameNOE_compl_resComp_index_ID);
        if ( tTList == null ) {
            General.showError("Expected a match but none found");
            return false;
        }
        if ( tTList.size() != 1 ) {
            General.showError("Expected exactly 1 match but found: " + tTList.size() );
            return false;
        }
        TagTable tT = (TagTable) tTList.get(0);
        if (
            (!tT.convertDataTypeColumn(tagNameNOE_compl_resCompl_cumul,       TagTable.DATA_TYPE_FLOAT, null ))||
            (!tT.convertDataTypeColumn(tagNameNOE_compl_resRst_obser_count,   TagTable.DATA_TYPE_INT, null ))||
            (!tT.convertDataTypeColumn(tagNameNOE_compl_resEntity_ID,         TagTable.DATA_TYPE_INT, null ))||
            (!tT.convertDataTypeColumn(tagNameNOE_compl_resComp_index_ID,     TagTable.DATA_TYPE_INT, null ))) {
            General.showError("Failed converting (some of) the data to the types expected.");
            return false;
        }

        // Create a nice plot
        try {
            StringArrayList columnNameListValue = new StringArrayList();
            columnNameListValue.add(tagNameNOE_compl_resCompl_cumul);
            columnNameListValue.add(tagNameNOE_compl_resRst_obser_count);
            StringArrayList seriesNameList = new StringArrayList();
            String complLabelName = "Completeness (%)";
            String restrLabelName = "Number of NOEs";
            seriesNameList.add(complLabelName);
            seriesNameList.add(restrLabelName);
            DefaultTableXYDataset dataSet = ResiduePlot.createDatasetFromRelation(
                    tT,
                    columnNameListValue,
                    seriesNameList);

            JFreeChart chart = ResiduePlot.createChart(dataSet,
                    tT,
                    tagNameNOE_compl_resEntity_ID,
                    tagNameNOE_compl_resComp_index_ID,
                    tagNameNOE_compl_resComp_ID);
            XYPlot plot = (XYPlot) chart.getPlot();
            int seriesCount = dataSet.getSeriesCount();
            for (int i=0;i<seriesCount;i++) {
                XYSeries series = dataSet.getSeries(i);
                String key = (String) series.getKey();
                General.showDebug("Changing series: " + key);
                NumberAxis axis = (NumberAxis) plot.getRangeAxisForDataset(i);
                if ( key.equals(complLabelName)) {
                    General.showDebug("Changing axis");
                    axis.setRange(0,100);
                    axis.setTickUnit(new NumberTickUnit(20));
                } else if ( key.equals(restrLabelName)) {
                    General.showDebug("Changing axis");
                    axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                }
            }
            if ( ui.hasGuiAttached() ) {
                ChartPanel chartPanel = new ChartPanel(chart);
                ui.gui.setJPanelToBeFilled( 0, chartPanel ); // executed on event-dispatching thread
            } else {
                General.showOutput("Skipping the actual show because there is not Gui attached.");
            }
//            ResiduePlot.show(chart);

            if ( saveImage ) {
                String fileName = file_name_base_dc+".jpg";
                int width = 1024;
                int height = 768;
                General.showOutput("Saving chart as JPEG");
                ChartUtilities.saveChartAsJPEG(new File(fileName), chart, width, height);
                General.showOutput("Saving chart as PDF");
                fileName = file_name_base_dc+".pdf";
                PdfGeneration.convertToPdf(chart, width, height, fileName);
            }
        } catch (Throwable t) {
            General.showThrowable(t);
            return false;
        }
        return true;
    }
}
