/*
 * File31.java
 * Created on September 12, 2003, 3:53 PM
 */
package Wattos.Star.NMRStar;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import Wattos.CloneWars.UserInterface;
import Wattos.Common.OrfIdList;
import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.SQLSelect;
import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexSortedInt;
import Wattos.Soup.Atom;
import Wattos.Soup.AtomMap;
import Wattos.Soup.Biochemistry;
import Wattos.Soup.Chemistry;
import Wattos.Soup.Entry;
import Wattos.Soup.Gumbo;
import Wattos.Soup.Model;
import Wattos.Soup.Molecule;
import Wattos.Soup.PseudoLib;
import Wattos.Soup.Residue;
import Wattos.Soup.Comparator.ComparatorAtomPerModelMolResAtom;
import Wattos.Soup.Constraint.Cdih;
import Wattos.Soup.Constraint.CdihList;
import Wattos.Soup.Constraint.Constr;
import Wattos.Soup.Constraint.DistConstr;
import Wattos.Soup.Constraint.DistConstrList;
import Wattos.Soup.Constraint.Rdc;
import Wattos.Soup.Constraint.RdcList;
import Wattos.Soup.Constraint.SimpleConstr;
import Wattos.Soup.Constraint.SimpleConstrList;
import Wattos.Star.DataBlock;
import Wattos.Star.SaveFrame;
import Wattos.Star.StarFileReader;
import Wattos.Star.StarGeneral;
import Wattos.Star.StarNode;
import Wattos.Star.TagTable;
import Wattos.Utils.General;
import Wattos.Utils.HashOfHashesOfHashes;
import Wattos.Utils.InOut;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringArrayList;
import Wattos.Utils.StringIntMap;
import Wattos.Utils.Strings;
import Wattos.Utils.Wiskunde.Geometry;
import cern.colt.list.IntArrayList;
import cern.colt.list.ObjectArrayList;

/**
 *Code for reading/writing NMR-STAR 3.1 according to the tag names as defined
 * in an external dictionary.
 *
 *Note that standard IDs (those that can are inserted by method
 * enterStandardIDs) are omitted from the external dictionary and this code in
 * order to simplify them. Note the exceptions listed for that method. No other
 * exception exist.
 *
 * @author Jurgen F. Doreleijers
 */
public class File31 {

    StarNode topNode;
    UserInterface ui;

    /** BEGIN BLOCK COPY FROM Wattos.Star.NMRStar.File31 not found elsewhere yet */
    public Constr constr;
    public DistConstr dc;
    public DistConstrList dcList;
    public Cdih cdih;
    public CdihList cdihList;
    public Rdc rdc;
    public RdcList rdcList;

    int currentDCAtomId = Defs.NULL_INT;
    int currentDCMembId = Defs.NULL_INT;
    int currentDCViolId = Defs.NULL_INT;
    int currentDCNodeId = Defs.NULL_INT;
    int currentDCId = Defs.NULL_INT;
    int currentDCListId = Defs.NULL_INT;
    int currentSCAtomId = Defs.NULL_INT;
    int currentSCId = Defs.NULL_INT;
    int currentSCListId = Defs.NULL_INT;
    /** END BLOCK */

    /** BEGIN BLOCK COPY FROM Wattos.Soup.PdbFile */
    public DBMS dbms;
    public Gumbo gumbo;
    public Atom atom;
    public Residue res;
    public Molecule mol;
    public Model model;
    public Entry entry;
    public Relation atomMain;
    public Relation resMain;
    public Relation molMain;
    public Relation modelMain;
    public Relation entryMain;

    int previousAtomId = Defs.NULL_INT;
    int currentAtomId = Defs.NULL_INT;
    int currentResId = Defs.NULL_INT;
    int currentMolId = Defs.NULL_INT;
    int currentModelId = Defs.NULL_INT;
    int currentEntryId = Defs.NULL_INT;
    /** END BLOCK */

    /** BEGIN BLOCK COPY FROM Wattos.Star.NMRStar.File31 */
    public TagTable tTIntro = null;
    public TagTable tTTree = null;
    public TagTable tTAtom = null;
    public TagTable tTDist = null;
    // public TagTable tTComm = null;
    // public TagTable tTPar1 = null;
    // public TagTable tTPar2 = null;
    public TagTable tTMain = null;

    public String tagNameEntrySFCategory;
    // public String tagNameEntryId;
    public String tagNameEntryName;
    // public String tagNameMolEntryId;
    public String tagNameMolId;
    public String tagNameMolAssEntityId;
    public String tagNameMolAssEntityLabel;
    public String tagNameMolSFCategory;

    public String tagNamePDBX_nonpoly_schemeEntity_assembly_ID;
    public String tagNamePDBX_nonpoly_schemeEntity_ID;
    public String tagNamePDBX_nonpoly_schemeMon_ID;
    public String tagNamePDBX_nonpoly_schemeComp_index_ID;
    public String tagNamePDBX_nonpoly_schemeComp_ID;
    public String tagNamePDBX_nonpoly_schemeAuth_seq_num;

    public String tagNameMolName;
    public String tagNameMolType;
    public String tagNameMolPolType;
    public String tagNameMolSeqLength;
    public String tagNameMolSeq;
    public String tagNameResEntityId;
    public String tagNameResNum_1;
    public String tagNameResCompId;
    public String tagNameResCompLabel;
    public String tagNameResMolId;
    public String tagNameResResId;
    public String tagNameResNum_2;
    public String tagNameResName;

    public String tagNameChem_compSFCategory;
    public String tagNameChem_compId;
    public String tagNameChem_compType;

    public String tagNameAtomSFCategory;
    // public String tagNameAtomSFId_1;
    public String tagNameAtomSFId_2;
    public String tagNameAtomModelId;
    public String tagNameAtomId;
    public String tagNameAtomMolId1;
    public String tagNameAtomMolId2;
    public String tagNameAtomAsym_ID;
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
    public String tagNameAtomAssembly_atom_ID;
    public String tagNameAtomSeq_ID;
    public String tagNameAtomDetails;

    public String tagNameDCSfcategory;
    // public String tagNameDCID;
    public String tagNameDCMRfileblockposition;
    public String tagNameDCDetails;
    // public String tagNameDCProgram;
    public String tagNameDCType;
    public String tagNameDCConstraint_file_ID;
    // public String tagNameDCSubtype;
    // public String tagNameDCFormat;
    public String tagNameDCtreeConstraintsID;
    // public String tagNameDCtreeID;
    public String tagNameDCtreeNodeID;
    public String tagNameDCtreeDownnodeID;
    public String tagNameDCtreeRightnodeID;
    public String tagNameDCtreeLogicoperation;
    public String tagNameDCConstraintsID;
    // public String tagNameDCDistconstrainttreeID;
    public String tagNameDCTreenodemembernodeID;
    // public String tagNameDCContributionfractionalval;
    public String tagNameDCConstrainttreenodememberID;
    public String tagNameDCEntityassemblyID;
    public String tagNameDCEntityID;
    public String tagNameDCCompindexID;
    public String tagNameDCSeqID;
    public String tagNameDCCompID;
    public String tagNameDCAtomID;
    public String tagNameDCResonanceID;
    public String tagNameDCAuthsegmentcode;
    public String tagNameDCAuthseqID;
    public String tagNameDCAuthcompID;
    public String tagNameDCAuthatomID;
    // public String tagNameDCvalueConstraintsID;
    public String tagNameDCvalueConstraintID;
    public String tagNameDCvalueTreenodeID;
    public String tagNameDCvalueSourceexperimentID;
    public String tagNameDCvalueSpectralpeakID;
    public String tagNameDCvalueIntensityval;
    public String tagNameDCvalueIntensitylowervalerr;
    public String tagNameDCvalueIntensityuppervalerr;
    public String tagNameDCvalueDistanceval;
    public String tagNameDCvalueDistancelowerboundval;
    public String tagNameDCvalueDistanceupperboundval;
    // public String tagNameDCvalueWeight;
    // public String tagNameDCvalueSpectralpeakppm1;
    // public String tagNameDCvalueSpectralpeakppm2;
    // public String tagNameDCcommentorgConstraintsID;
    // public String tagNameDCcommentorgID;
    // public String tagNameDCcommentorgCommentbeginline;
    // public String tagNameDCcommentorgCommentbegincolumn;
    // public String tagNameDCcommentorgCommentendline;
    // public String tagNameDCcommentorgCommentendcolumn;
    // public String tagNameDCcommentorgComment;
    // public String tagNameDCparsefileConstraintsID;
    // public String tagNameDCparsefileID;
    // public String tagNameDCparsefileName;
    // public String tagNameDCparsefileconverrConstraintsID;
    // public String tagNameDCparsefileconverrID;
    // public String tagNameDCparsefileconverrParsefileID;
    // public String tagNameDCparsefileconverrParsefilesflabel;
    // public String tagNameDCparsefileconverrParsefileconstraintID;
    // public String tagNameDCparsefileconverrConverrortype;
    // public String tagNameDCparsefileconverrConverrornote;

    public String tagNameCDIH_Sf_category;
    // public String tagNameCDIH_ID1;
    public String tagNameCDIH_Constraint_file_ID;
    public String tagNameCDIH_MR_file_block_position;
    public String tagNameCDIH_Details;
    // public String tagNameCDIH_Program;
    // public String tagNameCDIH_Type;
    // public String tagNameCDIH_Subtype;
    // public String tagNameCDIH_Format;
    // public String tagNameCDIH_Constraints_ID;
    public String tagNameCDIH_ID2;
    public String tagNameCDIH_Torsion_angle_name;
    public String tagNameCDIH_Entity_assembly_ID_1;
    public String tagNameCDIH_Entity_ID_1;
    public String tagNameCDIH_Comp_index_ID_1;
    public String tagNameCDIH_SeqID_1;
    public String tagNameCDIH_Comp_ID_1;
    public String tagNameCDIH_Atom_ID_1;
    public String tagNameCDIH_Resonance_ID_1;
    public String tagNameCDIH_Entity_assembly_ID_2;
    public String tagNameCDIH_Entity_ID_2;
    public String tagNameCDIH_Comp_index_ID_2;
    public String tagNameCDIH_SeqID_2;
    public String tagNameCDIH_Comp_ID_2;
    public String tagNameCDIH_Atom_ID_2;
    public String tagNameCDIH_Resonance_ID_2;
    public String tagNameCDIH_Entity_assembly_ID_3;
    public String tagNameCDIH_Entity_ID_3;
    public String tagNameCDIH_Comp_index_ID_3;
    public String tagNameCDIH_SeqID_3;
    public String tagNameCDIH_Comp_ID_3;
    public String tagNameCDIH_Atom_ID_3;
    public String tagNameCDIH_Resonance_ID_3;
    public String tagNameCDIH_Entity_assembly_ID_4;
    public String tagNameCDIH_Entity_ID_4;
    public String tagNameCDIH_Comp_index_ID_4;
    public String tagNameCDIH_SeqID_4;
    public String tagNameCDIH_Comp_ID_4;
    public String tagNameCDIH_Atom_ID_4;
    public String tagNameCDIH_Resonance_ID_4;
    public String tagNameCDIH_Auth_segment_code_1;
    public String tagNameCDIH_Auth_seq_ID_1;
    public String tagNameCDIH_Auth_comp_ID_1;
    public String tagNameCDIH_Auth_atom_ID_1;
    public String tagNameCDIH_Auth_segment_code_2;
    public String tagNameCDIH_Auth_seq_ID_2;
    public String tagNameCDIH_Auth_comp_ID_2;
    public String tagNameCDIH_Auth_atom_ID_2;
    public String tagNameCDIH_Auth_segment_code_3;
    public String tagNameCDIH_Auth_seq_ID_3;
    public String tagNameCDIH_Auth_comp_ID_3;
    public String tagNameCDIH_Auth_atom_ID_3;
    public String tagNameCDIH_Auth_segment_code_4;
    public String tagNameCDIH_Auth_seq_ID_4;
    public String tagNameCDIH_Auth_comp_ID_4;
    public String tagNameCDIH_Auth_atom_ID_4;
    public String tagNameCDIH_Angle_upper_bound_val;
    public String tagNameCDIH_Angle_lower_bound_val;
    // public String tagNameCDIH_Force_constant_value;
    // public String tagNameCDIH_Potential_function_exponent;
    public String tagNameRDC_Sf_category;
    // public String tagNameRDC_ID1;
    public String tagNameRDC_Constraint_file_ID;
    public String tagNameRDC_MR_file_block_position;
    public String tagNameRDC_Details;

    // public String tagNameRDC_Program;
    // public String tagNameRDC_Type;
    // public String tagNameRDC_Subtype;
    // public String tagNameRDC_Format;
    // public String tagNameRDC_Constraints_ID;
    public String tagNameRDC_ID2;
    public String tagNameRDC_Entity_assembly_ID_1;
    public String tagNameRDC_Entity_ID_1;
    public String tagNameRDC_Comp_index_ID_1;
    public String tagNameRDC_Seq_ID_1;
    public String tagNameRDC_Comp_ID_1;
    public String tagNameRDC_Atom_ID_1;
    public String tagNameRDC_Resonance_ID_1;
    public String tagNameRDC_Entity_assembly_ID_2;
    public String tagNameRDC_Entity_ID_2;
    public String tagNameRDC_Comp_index_ID_2;
    public String tagNameRDC_Seq_ID_2;
    public String tagNameRDC_Comp_ID_2;
    public String tagNameRDC_Atom_ID_2;
    public String tagNameRDC_Resonance_ID_2;
    public String tagNameRDC_Auth_segment_code_1;
    public String tagNameRDC_Auth_seq_ID_1;
    public String tagNameRDC_Auth_comp_ID_1;
    public String tagNameRDC_Auth_atom_ID_1;
    public String tagNameRDC_Auth_segment_code_2;
    public String tagNameRDC_Auth_seq_ID_2;
    public String tagNameRDC_Auth_comp_ID_2;
    public String tagNameRDC_Auth_atom_ID_2;
    public String tagNameRDC_RDC_val;
    public String tagNameRDC_RDC_lower_bound;
    public String tagNameRDC_RDC_upper_bound;
    public String tagNameRDC_RDC_val_err;

    public String tagNameStudy_listSf_category;
    // public String tagNameStudy_listEntry_ID;
    // public String tagNameStudy_listID;
    public String tagNameStudyID;
    public String tagNameStudyName;
    public String tagNameStudyType;
    public String tagNameStudyDetails;
    // public String tagNameStudyEntry_ID;
    // public String tagNameStudyStudy_list_ID;
    public String tagNameEntrySf_category;
    public String tagNameEntryID;
    public String tagNameEntryTitle;
    public String tagNameEntryNMR_STAR_version;
    public String tagNameEntryPDB_coordinate_file_version;
    public String tagNameEntryExperimental_method;
    public String tagNameEntryDetails;

    // public String tagNameAssemblyEntry_ID ;
    public String tagNameAssemblyNumber_of_components;
    public String tagNameAssemblyOrganic_ligands;
    public String tagNameAssemblyMetal_ions;
    public String tagNameAssemblyParamagnetic;
    public String tagNameAssemblyThiol_state;
    public String tagNameAssemblyMolecular_mass;
    public String tagNameEntity_assemblyEntity_assembly_name;
    public String tagNameEntity_assemblyAsym_ID;
    public String tagNameEntity_assemblyDetails;
    // public String tagNameEntity_assemblyEntry_ID ;

    /**
     * Define varXXX for those tags that are actually going to be read from or
     * writen to with a non-null value.
     */
    String[] varDCSfcategory = null;
    // int[] varDCID = null;
    int[] varDCMRfileblockposition = null;
    // String[] varDCProgram = null;
    String[] varDCType = null;
    int[] varDCFileID = null;
    // String[] varDCSubtype = null;
    // String[] varDCFormat = null;
    int[] varDCtreeConstraintsID = null;
    // int[] varDCtreeID = null;
    int[] varDCtreeNodeID = null;
    int[] varDCtreeDownnodeID = null;
    int[] varDCtreeRightnodeID = null;
    String[] varDCtreeLogicoperation = null;
    int[] varDCConstraintsID = null;
    // int[] varDCDistconstrainttreeID = null;
    int[] varDCTreenodemembernodeID = null;
    // float[] varDCContributionfractionalval = null;
    int[] varDCConstrainttreenodememberID = null;
    int[] varDCEntityassemblyID = null;
    int[] varDCEntityID = null;
    int[] varDCCompindexID = null;
    String[] varDCCompID = null;
    String[] varDCAtomID = null;
    String[] varDCAuthsegmentcode = null;
    String[] varDCAuthseqID = null;
    String[] varDCAuthcompID = null;
    String[] varDCAuthatomID = null;
    // int[] varDCvalueConstraintsID = null;
    int[] varDCvalueConstraintID = null;
    int[] varDCvalueTreenodeID = null;
    String[] varDCvalueSourceexperimentID = null;
    String[] varDCvalueSpectralpeakID = null;
    //
    // float[] varDCvalueIntensityval = null;
    // float[] varDCvalueIntensitylowervalerr = null;
    // float[] varDCvalueIntensityuppervalerr = null;
    float[] varDCvalueDistanceval = null;
    float[] varDCvalueDistancelowerboundval = null;
    float[] varDCvalueDistanceupperboundval = null;
    // float[] varDCvalueWeight = null;
    // float[] varDCvalueSpectralpeakppm1 = null;
    // float[] varDCvalueSpectralpeakppm2 = null;

    // int[] varDCcommentorgConstraintsID = null;
    // int[] varDCcommentorgID = null;
    // int[] varDCcommentorgCommentbeginline = null;
    // int[] varDCcommentorgCommentbegincolumn = null;
    // int[] varDCcommentorgCommentendline = null;
    // int[] varDCcommentorgCommentendcolumn = null;
    // String[] varDCcommentorgComment = null;
    // int[] varDCparsefileConstraintsID = null;
    // int[] varDCparsefileID = null;
    // String[] varDCparsefileName = null;
    // int[] varDCparsefileconverrConstraintsID = null;
    // int[] varDCparsefileconverrID = null;
    // int[] varDCparsefileconverrParsefileID = null;
    // String[] varDCparsefileconverrParsefilesflabel = null;
    // int[] varDCparsefileconverrParsefileconstraintID=null;
    // String[] varDCparsefileconverrConverrortype = null;
    // String[] varDCparsefileconverrConverrornote = null;

    String[] varCDIH_Sf_category = null;
    // int[] varCDIH_ID1=null;
    int[] varCDIH_MR_file_block_position = null;
    // String[] varCDIH_Program=null;
    // String[] varCDIH_Type=null;
    // String[] varCDIH_Subtype=null;
    // String[] varCDIH_Format=null;
    // int[] varCDIH_Constraints_ID=null;
    int[] varCDIH_ID2 = null;
    String[] varCDIH_Torsion_angle_name = null;
    int[] varCDIH_Label_entity_assembly_ID_1 = null;
    int[] varCDIH_Label_entity_ID_1 = null;
    int[] varCDIH_Label_comp_index_ID_1 = null;
    String[] varCDIH_Label_comp_ID_1 = null;
    String[] varCDIH_Label_atom_ID_1 = null;
    int[] varCDIH_Label_entity_assembly_ID_2 = null;
    int[] varCDIH_Label_entity_ID_2 = null;
    int[] varCDIH_Label_comp_index_ID_2 = null;
    String[] varCDIH_Label_comp_ID_2 = null;
    String[] varCDIH_Label_atom_ID_2 = null;
    int[] varCDIH_Label_entity_assembly_ID_3 = null;
    int[] varCDIH_Label_entity_ID_3 = null;
    int[] varCDIH_Label_comp_index_ID_3 = null;
    String[] varCDIH_Label_comp_ID_3 = null;
    String[] varCDIH_Label_atom_ID_3 = null;
    int[] varCDIH_Label_entity_assembly_ID_4 = null;
    int[] varCDIH_Label_entity_ID_4 = null;
    int[] varCDIH_Label_comp_index_ID_4 = null;
    String[] varCDIH_Label_comp_ID_4 = null;
    String[] varCDIH_Label_atom_ID_4 = null;
    String[] varCDIH_Auth_segment_code_1 = null;
    String[] varCDIH_Auth_seq_ID_1 = null;
    String[] varCDIH_Auth_comp_ID_1 = null;
    String[] varCDIH_Auth_atom_ID_1 = null;
    String[] varCDIH_Auth_segment_code_2 = null;
    String[] varCDIH_Auth_seq_ID_2 = null;
    String[] varCDIH_Auth_comp_ID_2 = null;
    String[] varCDIH_Auth_atom_ID_2 = null;
    String[] varCDIH_Auth_segment_code_3 = null;
    String[] varCDIH_Auth_seq_ID_3 = null;
    String[] varCDIH_Auth_comp_ID_3 = null;
    String[] varCDIH_Auth_atom_ID_3 = null;
    String[] varCDIH_Auth_segment_code_4 = null;
    String[] varCDIH_Auth_seq_ID_4 = null;
    String[] varCDIH_Auth_comp_ID_4 = null;
    String[] varCDIH_Auth_atom_ID_4 = null;
    float[] varCDIH_Angle_upper_bound_val = null;
    float[] varCDIH_Angle_lower_bound_val = null;
    // float[] varCDIH_Force_constant_value=null;
    // float[] varCDIH_Potential_function_exponent=null;

    String[] varRDC_Sf_category = null;
    // int[] varRDC_ID1=null;
    int[] varRDC_MR_file_block_position = null;
    // String[] varRDC_Program=null;
    // String[] varRDC_Type=null;
    // String[] varRDC_Subtype=null;
    // String[] varRDC_Format=null;
    // int[] varRDC_Constraints_ID=null;
    int[] varRDC_ID2 = null;
    int[] varRDC_Label_entity_assembly_ID_1 = null;
    int[] varRDC_Label_entity_ID_1 = null;
    int[] varRDC_Label_comp_index_ID_1 = null;
    String[] varRDC_Label_comp_ID_1 = null;
    String[] varRDC_Label_atom_ID_1 = null;
    int[] varRDC_Label_entity_assembly_ID_2 = null;
    int[] varRDC_Label_entity_ID_2 = null;
    int[] varRDC_Label_comp_index_ID_2 = null;
    String[] varRDC_Label_comp_ID_2 = null;
    String[] varRDC_Label_atom_ID_2 = null;
    String[] varRDC_Auth_segment_code_1 = null;
    String[] varRDC_Auth_seq_ID_1 = null;
    String[] varRDC_Auth_comp_ID_1 = null;
    String[] varRDC_Auth_atom_ID_1 = null;
    String[] varRDC_Auth_segment_code_2 = null;
    String[] varRDC_Auth_seq_ID_2 = null;
    String[] varRDC_Auth_comp_ID_2 = null;
    String[] varRDC_Auth_atom_ID_2 = null;
    float[] varRDC_val = null;
    float[] varRDC_lower_bound = null;
    float[] varRDC_upper_bound = null;
    float[] varRDC_val_err = null;

    // String[] varStudy_listSf_category = null;
    // String[] varStudy_listEntry_ID = null;
    // String[] varStudy_listID = null;
    // String[] varStudyID = null;
    // String[] varStudyName = null;
    // String[] varStudyType = null;
    // String[] varStudyDetails = null;
    // // String[] varStudyEntry_ID = null;
    // // String[] varStudyStudy_list_ID = null;
    // String[] varEntrySf_category = null;
    // // String[] varEntryID = null;
    // String[] varEntryTitle = null;
    // String[] varEntryNMR_STAR_version = null;
    // String[] varEntryExperimental_method = null;
    // String[] varEntryDetails = null;
    //
    // // String[] varAssemblyEntry_ID = null;
    // String[] varAssemblyNumber_of_components = null;
    // String[] varAssemblyOrganic_ligands = null;
    // String[] varAssemblyMetal_ions = null;
    // String[] varAssemblyParamagnetic = null;
    // String[] varAssemblyThiol_state = null;
    // String[] varAssemblyMolecular_mass = null;
    // String[] varEntity_assemblyEntity_assembly_name = null;
    // String[] varEntity_assemblyAsym_ID = null;
    // String[] varEntity_assemblyDetails = null;
    // // String[] varEntity_assemblyEntry_ID = null;

    // int[] varPDBX_nonpoly_schemeEntity_assembly_ID=null;
    // int[] varPDBX_nonpoly_schemeEntity_ID=null;
    // String[] varPDBX_nonpoly_schemeMon_ID=null;
    // int[] varPDBX_nonpoly_schemeComp_index_ID=null;
    // String[] varPDBX_nonpoly_schemeComp_ID=null;
    // int[] varPDBX_nonpoly_schemeAuth_seq_num=null;
    /** END BLOCK */

    static final int LOC_ENTITY_ASSEMBLY_ID = 0;
    static final int LOC_ENTITY_ID = 1;
    static final int LOC_COMP_INDEX_ID = 2;
    static final int LOC_COMP_ID = 3;
    static final int LOC_ATOM_ID = 4;

    static final int LOC_AUTHOR_SEGMENT_CODE = 0;
    static final int LOC_AUTHOR_SEQ_ID = 1;
    static final int LOC_AUTHOR_COMP_ID = 2;
    static final int LOC_AUTHOR_ATOM_ID = 3;

    /** Creates a new instance of File31 */
    public File31(DBMS dbms, UserInterface ui) throws Exception {
        // this.topNode = topNode; had no effect according to eclipse
        this.dbms = dbms;
        this.ui = ui;
        if (!initConvenienceVariables()) {
            throw new Exception("Failed to initConvenienceVariables");
        }
        if (!initConvenienceVariablesStar()) {
            throw new Exception("Failed to initConvenienceVariablesStar");
        }
        if (!initConvenienceVariablesConstr()) {
            throw new Exception("Failed to initConvenienceVariablesStar");
        }
    }

    /**
     * BEGIN BLOCK FOR SETTING LOCAL CONVENIENCE VARIABLES COPY FROM
     * Wattos.Star.NMRStar.File31
     */
    public boolean initConvenienceVariablesConstr() {
        constr = ui.constr;
        dc = constr.dc;
        dcList = constr.dcList;
        cdih = constr.cdih;
        cdihList = constr.cdihList;
        rdc = constr.rdc;
        rdcList = constr.rdcList;
        return true;
    }

    /** END BLOCK */

    /**
     * BEGIN BLOCK FOR SETTING LOCAL CONVENIENCE VARIABLES COPY FROM
     * Wattos.Soup.PdbFile
     */
    public boolean initConvenienceVariables() {

        atomMain = dbms.getRelation(Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_MAIN_RELATION_NAME]);
        if (atomMain == null) {
            General.showError("failed to find the atom main relation");
            return false;
        }
        atom = (Atom) atomMain.getRelationSetParent();
        if (atom == null) {
            General.showError("failed to find atom RelationSet");
            return false;
        }
        gumbo = (Gumbo) atom.getRelationSoSParent();
        if (gumbo == null) {
            General.showError("failed to find the gumbo RelationSoS");
            return false;
        }
        atom = gumbo.atom;
        res = gumbo.res;
        mol = gumbo.mol;
        model = gumbo.model;
        entry = gumbo.entry;
        atomMain = atom.mainRelation;
        resMain = res.mainRelation;
        molMain = mol.mainRelation;
        modelMain = model.mainRelation;
        entryMain = entry.mainRelation;
        return true;
    }

    /** END BLOCK */

    /**
     * BEGIN BLOCK FOR SETTING NMR-STAR CONVENIENCE VARIABLES COPY FROM
     * Wattos.Star.NMRStar.File31
     */
    public boolean initConvenienceVariablesStar() {
        StarDictionary starDict = ui.wattosLib.starDictionary;
        // Please note that the following names are not hard-coded as star names
        // necessarily (like in mol_id)
        try {
            tagNameEntrySFCategory = (String) ((ArrayList) starDict.toStar2D.get("entry_main", "_Assembly.Sf_category"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameEntryId = (String) ((ArrayList)starDict.toStar2D.get(
            // "entry_main", "entry_id"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntryName = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    Relation.DEFAULT_ATTRIBUTE_NAME)).get(StarDictionary.POSITION_STAR_TAG_NAME);

            // tagNameAssemblyEntry_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "entry_main",
            // "_Assembly.Entry_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyNumber_of_components = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Number_of_components")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyOrganic_ligands = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Organic_ligands")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyMetal_ions = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Metal_ions")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyParamagnetic = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Paramagnetic")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyThiol_state = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Thiol_state")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyMolecular_mass = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Molecular_mass")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntity_assemblyEntity_assembly_name = (String) ((ArrayList) starDict.toStar2D.get("mol_main",
                    "_Entity_assembly.Entity_assembly_name")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntity_assemblyAsym_ID = (String) ((ArrayList) starDict.toStar2D.get("mol_main",
                    "_Entity_assembly.Asym_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntity_assemblyDetails = (String) ((ArrayList) starDict.toStar2D.get("mol_main",
                    "_Entity_assembly.Details")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameEntity_assemblyEntry_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "mol_main",
            // "_Entity_assembly.Entry_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);

            // tagNameMolEntryId = (String) ((ArrayList)starDict.toStar2D.get(
            // "mol_main", "entry_id"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolId = (String) ((ArrayList) starDict.toStar2D.get("mol_main", "mol_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolAssEntityId = (String) ((ArrayList) starDict.toStar2D.get("mol_main",
                    "_Entity_assembly.Entity_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolAssEntityLabel = (String) ((ArrayList) starDict.toStar2D.get("mol_main",
                    "_Entity_assembly.Entity_label")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolSFCategory = (String) ((ArrayList) starDict.toStar2D.get("mol_main", "_Entity.Sf_category"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameMolEntityId = (String) ((ArrayList)starDict.toStar2D.get(
            // "mol_main", "_Entity.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolType = (String) ((ArrayList) starDict.toStar2D.get("mol_main", "type"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolName = (String) ((ArrayList) starDict.toStar2D.get("mol_main", "name"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolPolType = (String) ((ArrayList) starDict.toStar2D.get("mol_main", "pol_type"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolSeqLength = (String) ((ArrayList) starDict.toStar2D.get("mol_main", "_Entity.Seq_length"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameMolSeq = (String) ((ArrayList) starDict.toStar2D.get("mol_main", "_Entity.Seq"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameResEntityId = (String) ((ArrayList) starDict.toStar2D
                    .get("res_main", "_Entity_comp_index.Entity_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameResNum_1 = (String) ((ArrayList) starDict.toStar2D.get("res_main", "_Entity_comp_index.ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameResCompId = (String) ((ArrayList) starDict.toStar2D.get("res_main", "_Entity_comp_index.Comp_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameResCompLabel = (String) ((ArrayList) starDict.toStar2D.get("res_main",
                    "_Entity_comp_index.Comp_label")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameResMolId = (String) ((ArrayList) starDict.toStar2D.get("res_main", "mol_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameResResId = (String) ((ArrayList) starDict.toStar2D.get("res_main", "res_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameResNum_2 = (String) ((ArrayList) starDict.toStar2D
                    .get("res_main", Relation.DEFAULT_ATTRIBUTE_NUMBER)).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameResName = (String) ((ArrayList) starDict.toStar2D.get("res_main", Relation.DEFAULT_ATTRIBUTE_NAME))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);

            tagNameChem_compSFCategory = (String) ((ArrayList) starDict.toStar2D.get("res_main",
                    "_Chem_comp.Sf_category")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameChem_compId = (String) ((ArrayList) starDict.toStar2D.get("res_main", "_Chem_comp.ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameChem_compType = (String) ((ArrayList) starDict.toStar2D.get("res_main", "_Chem_comp.Type"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);

            tagNameAtomSFCategory = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    "_Conformer_family_coord_set.Sf_category")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameAtomSFId_1 = (String) ((ArrayList)starDict.toStar2D.get(
            // "atom_main",
            // "_Conformer_family_coord_set.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomSFId_2 = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    "_Atom_site.Conformer_family_coord_set_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomModelId = (String) ((ArrayList) starDict.toStar2D.get("atom_main", "_Atom_site.Model_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomId = (String) ((ArrayList) starDict.toStar2D.get("atom_main", "_Atom_site.ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomMolId1 = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    "_Atom_site.Label_entity_assembly_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomMolId2 = (String) ((ArrayList) starDict.toStar2D.get("atom_main", "_Atom_site.Label_entity_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomResId = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    "_Atom_site.Label_comp_index_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomResName = (String) ((ArrayList) starDict.toStar2D.get("atom_main", "_Atom_site.Label_comp_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomName = (String) ((ArrayList) starDict.toStar2D.get("atom_main", Relation.DEFAULT_ATTRIBUTE_NAME))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomAuthMolId = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME)).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomAuthResId = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID)).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomAuthResName = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME)).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomAuthName = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME)).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomPdbInsertionCode = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    Gumbo.DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE)).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomElementId = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID)).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomCoorX = (String) ((ArrayList) starDict.toStar2D.get("atom_main", Gumbo.DEFAULT_ATTRIBUTE_COOR_X))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomCoorY = (String) ((ArrayList) starDict.toStar2D.get("atom_main", Gumbo.DEFAULT_ATTRIBUTE_COOR_Y))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomCoorZ = (String) ((ArrayList) starDict.toStar2D.get("atom_main", Gumbo.DEFAULT_ATTRIBUTE_COOR_Z))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomBFactor = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    Gumbo.DEFAULT_ATTRIBUTE_BFACTOR)).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomOccupancy = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY)).get(StarDictionary.POSITION_STAR_TAG_NAME);

            tagNameAtomAssembly_atom_ID = (String) ((ArrayList) starDict.toStar2D.get("atom_main",
                    "_Atom_site.Assembly_atom_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomAsym_ID = (String) ((ArrayList) starDict.toStar2D.get("atom_main", "_Atom_site.Label_asym_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomSeq_ID = (String) ((ArrayList) starDict.toStar2D.get("atom_main", "_Atom_site.Label_seq_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAtomDetails = (String) ((ArrayList) starDict.toStar2D.get("atom_main", "_Atom_site.Details"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            //
            tagNameDCSfcategory = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Distance_constraint_list.Sf_category")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCID = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown", "_Distance_constraint_list.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCMRfileblockposition = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Distance_constraint_list.Block_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCType = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Distance_constraint_list.Constraint_type")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCConstraint_file_ID = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Distance_constraint_list.Constraint_file_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCDetails = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Distance_constraint_list.Details")).get(StarDictionary.POSITION_STAR_TAG_NAME);

            tagNameDCtreeConstraintsID = (String) ((ArrayList) starDict.toStar2D.get("dc_main", "number"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCtreeID = (String) ((ArrayList)starDict.toStar2D.get(
            // "dc_node",
            // "_Dist_constraint_tree.Constraints_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCtreeNodeID = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "node_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCtreeDownnodeID = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "down_node_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCtreeRightnodeID = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "right_node_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCtreeLogicoperation = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "logic_op"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);

            tagNameDCConstraintsID = (String) ((ArrayList) starDict.toStar2D.get("dc_node",
                    "_Dist_constraint.Tree_node_member_constraint_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCDistconstrainttreeID = (String)
            // ((ArrayList)starDict.toStar2D.get( "dc_node", "number"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCTreenodemembernodeID = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "number"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCContributionfractionalval = (String)
            // ((ArrayList)starDict.toStar2D.get( "dc_node",
            // "contribution" )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCConstrainttreenodememberID = (String) ((ArrayList) starDict.toStar2D.get("dc_member", "member_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCEntityassemblyID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom",
                    "_Atom_site.Label_entity_assembly_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCEntityID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "_Atom_site.Label_entity_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCCompindexID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom",
                    "_Atom_site.Label_comp_index_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCSeqID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "_Dist_constraint.Seq_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCCompID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "_Atom_site.Label_comp_ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCAtomID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "name"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCResonanceID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "resonance_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCAuthsegmentcode = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "auth_mol_name"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCAuthseqID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "auth_res_id"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCAuthcompID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "auth_res_name"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCAuthatomID = (String) ((ArrayList) starDict.toStar2D.get("dc_atom", "auth_atom_name"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCvalueConstraintsID = (String)
            // ((ArrayList)starDict.toStar2D.get( "dc_node",
            // "_Dist_constraint_value.Constraints_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueConstraintID = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "number_2"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueTreenodeID = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "node_id_3"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueSourceexperimentID = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "sourceExpId"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueSpectralpeakID = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "peakId"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueIntensityval = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "peakIntensity"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueIntensitylowervalerr = (String) ((ArrayList) starDict.toStar2D.get("dc_node",
                    "peakIntensityLowerValueError")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueIntensityuppervalerr = (String) ((ArrayList) starDict.toStar2D.get("dc_node",
                    "peakIntensityUpperValueError")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueDistanceval = (String) ((ArrayList) starDict.toStar2D.get("dc_node", "distanceValue"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueDistancelowerboundval = (String) ((ArrayList) starDict.toStar2D.get("dc_node",
                    "distanceLowerBound")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameDCvalueDistanceupperboundval = (String) ((ArrayList) starDict.toStar2D.get("dc_node",
                    "distanceUpperBound")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCvalueWeight = (String)
            // ((ArrayList)starDict.toStar2D.get( "dc_node", "peakWeight"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCvalueSpectralpeakppm1 = (String)
            // ((ArrayList)starDict.toStar2D.get( "dc_node", "peakPpm1"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCvalueSpectralpeakppm2 = (String)
            // ((ArrayList)starDict.toStar2D.get( "dc_node", "peakPpm2"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCcommentorgConstraintsID = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_comment_org.Constraints_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCcommentorgID = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_comment_org.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCcommentorgCommentbeginline = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_comment_org.Comment_begin_line"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCcommentorgCommentbegincolumn = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_comment_org.Comment_begin_column"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCcommentorgCommentendline = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_comment_org.Comment_end_line"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCcommentorgCommentendcolumn = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_comment_org.Comment_end_column"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCcommentorgComment = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_comment_org.Comment"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileConstraintsID = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file.Constraints_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileID = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileName = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file.Name"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileconverrConstraintsID = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file_conv_err.Constraints_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileconverrID = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file_conv_err.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileconverrParsefileID = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file_conv_err.Parse_file_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileconverrParsefilesflabel = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file_conv_err.Parse_file_sf_label"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileconverrParsefileconstraintID = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file_conv_err.Parse_file_constraint_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileconverrConverrortype = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file_conv_err.Conv_error_type"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameDCparsefileconverrConverrornote = (String)
            // ((ArrayList)starDict.toStar2D.get( "unknown",
            // "_Dist_constraint_parse_file_conv_err.Conv_error_note"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);

            tagNameCDIH_Sf_category = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Torsion_angle_constraint_list.Sf_category")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameCDIH_ID1 = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown",
            // "_Torsion_angle_constraint_list.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Constraint_file_ID = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Torsion_angle_constraint_list.Constraint_file_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_MR_file_block_position = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Torsion_angle_constraint_list.MR_file_block_position"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Details = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_Torsion_angle_constraint_list.Details")).get(StarDictionary.POSITION_STAR_TAG_NAME);

            // tagNameCDIH_Program = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown",
            // "_Torsion_angle_constraints.Program"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameCDIH_Type = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown",
            // "_Torsion_angle_constraints.Type"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameCDIH_Subtype = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown",
            // "_Torsion_angle_constraints.Subtype"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameCDIH_Format = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown",
            // "_Torsion_angle_constraints.Format"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameCDIH_Constraints_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "cdih_main" ,
            // "_Torsion_angle_constraint.Constraints_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_ID2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_main", "number"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Torsion_angle_name = (String) ((ArrayList) starDict.toStar2D.get("cdih_main", "name"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Entity_assembly_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Entity_assembly_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Entity_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Entity_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Comp_index_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Comp_index_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_SeqID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Seq_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Comp_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Comp_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Atom_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Atom_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Resonance_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Resonance_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Entity_assembly_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Entity_assembly_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Entity_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Entity_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Comp_index_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Comp_index_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_SeqID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Seq_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Comp_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Comp_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Atom_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Atom_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Resonance_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Resonance_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Entity_assembly_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Entity_assembly_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Entity_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Entity_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Comp_index_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Comp_index_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_SeqID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Seq_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Comp_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Comp_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Atom_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Atom_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Resonance_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Resonance_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Entity_assembly_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Entity_assembly_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Entity_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Entity_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Comp_index_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Comp_index_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_SeqID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Seq_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Comp_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Comp_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Atom_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Atom_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Resonance_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Resonance_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_segment_code_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_segment_code_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_seq_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_seq_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_comp_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_comp_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_atom_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_atom_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_segment_code_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_segment_code_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_seq_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_seq_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_comp_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_comp_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_atom_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_atom_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_segment_code_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_segment_code_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_seq_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_seq_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_comp_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_comp_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_atom_ID_3 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_atom_ID_3")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_segment_code_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_segment_code_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_seq_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_seq_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_comp_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_comp_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Auth_atom_ID_4 = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom",
                    "_Torsion_angle_constraint.Auth_atom_ID_4")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Angle_upper_bound_val = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom", "uppBound"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameCDIH_Angle_lower_bound_val = (String) ((ArrayList) starDict.toStar2D.get("cdih_atom", "lowBound"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameCDIH_Force_constant_value = (String)
            // ((ArrayList)starDict.toStar2D.get( "cdih_atom" ,
            // "forceConstant" )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameCDIH_Potential_function_exponent = (String)
            // ((ArrayList)starDict.toStar2D.get( "cdih_atom" ,
            // "potentialFunction"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Sf_category = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_RDC_constraint_list.Sf_category")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameRDC_ID1 = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown", "_RDC_constraint_list.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Constraint_file_ID = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_RDC_constraint_list.Constraint_file_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_MR_file_block_position = (String) ((ArrayList) starDict.toStar2D.get("unknown",
                    "_RDC_constraint_list.MR_file_block_position")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Details = (String) ((ArrayList) starDict.toStar2D.get("unknown", "_RDC_constraint_list.Details"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);

            // tagNameRDC_Program = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown",
            // "_RDC_constraint_list.Program"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameRDC_Type = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown", "_RDC_constraint_list.Type"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameRDC_Subtype = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown",
            // "_RDC_constraint_list.Subtype"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameRDC_Format = (String) ((ArrayList)starDict.toStar2D.get(
            // "unknown", "_RDC_constraint_list.Format"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameRDC_Constraints_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "rdc_main" ,
            // "_RDC_constraint.Constraints_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_ID2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_main", "number"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Entity_assembly_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Entity_assembly_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Entity_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Entity_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Comp_index_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Comp_index_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Seq_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom", "_RDC_constraint.Seq_ID_1"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Comp_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom", "_RDC_constraint.Comp_ID_1"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Atom_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom", "_RDC_constraint.Atom_ID_1"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Resonance_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Atom_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Entity_assembly_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Entity_assembly_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Entity_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Entity_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Comp_index_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Comp_index_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Seq_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom", "_RDC_constraint.Seq_ID_2"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Comp_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom", "_RDC_constraint.Comp_ID_2"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Atom_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom", "_RDC_constraint.Atom_ID_2"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Resonance_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Atom_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Auth_segment_code_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Auth_segment_code_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Auth_seq_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Auth_seq_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Auth_comp_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Auth_comp_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Auth_atom_ID_1 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Auth_atom_ID_1")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Auth_segment_code_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Auth_segment_code_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Auth_seq_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Auth_seq_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Auth_comp_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Auth_comp_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_Auth_atom_ID_2 = (String) ((ArrayList) starDict.toStar2D.get("rdc_atom",
                    "_RDC_constraint.Auth_atom_ID_2")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_RDC_val = (String) ((ArrayList) starDict.toStar2D.get("rdc_main", "target"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_RDC_lower_bound = (String) ((ArrayList) starDict.toStar2D.get("rdc_main",
                    "_RDC_constraint.RDC_lower_bound")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_RDC_upper_bound = (String) ((ArrayList) starDict.toStar2D.get("rdc_main",
                    "_RDC_constraint.RDC_upper_bound")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameRDC_RDC_val_err = (String) ((ArrayList) starDict.toStar2D.get("rdc_main", "targetError"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);

            tagNameStudy_listSf_category = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Study_list.Sf_category")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameStudy_listEntry_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "entry_main" ,
            // "_Study_list.Entry_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameStudy_listID = (String) ((ArrayList)starDict.toStar2D.get(
            // "entry_main" , "_Study_list.ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameStudyID = (String) ((ArrayList) starDict.toStar2D.get("entry_main", "_Study.ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameStudyName = (String) ((ArrayList) starDict.toStar2D.get("entry_main", "_Study.Name"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameStudyType = (String) ((ArrayList) starDict.toStar2D.get("entry_main", "_Study.Type"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameStudyDetails = (String) ((ArrayList) starDict.toStar2D.get("entry_main", "_Study.Details"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameStudyEntry_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "entry_main" ,
            // "_Study.Entry_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameStudyStudy_list_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "entry_main" ,
            // "_Study.Study_list_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntrySf_category = (String) ((ArrayList) starDict.toStar2D.get("entry_information",
                    "_Entry.Sf_category")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntryID = (String) ((ArrayList) starDict.toStar2D.get("entry_information", "_Entry.ID"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntryTitle = (String) ((ArrayList) starDict.toStar2D.get("entry_information", "_Entry.Title"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntryNMR_STAR_version = (String) ((ArrayList) starDict.toStar2D.get("entry_information",
            "_Entry.NMR_STAR_version")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntryPDB_coordinate_file_version = (String) ((ArrayList) starDict.toStar2D.get("entry_information",
            "_Entry.PDB_coordinate_file_version")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntryExperimental_method = (String) ((ArrayList) starDict.toStar2D.get("entry_information",
                    "_Entry.Experimental_method")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntryDetails = (String) ((ArrayList) starDict.toStar2D.get("entry_information", "_Entry.Details"))
                    .get(StarDictionary.POSITION_STAR_TAG_NAME);

            // tagNameAssemblyEntry_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "entry_main" ,
            // "_Assembly.Entry_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyNumber_of_components = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Number_of_components")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyOrganic_ligands = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Organic_ligands")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyMetal_ions = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Metal_ions")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyParamagnetic = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Paramagnetic")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyThiol_state = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Thiol_state")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameAssemblyMolecular_mass = (String) ((ArrayList) starDict.toStar2D.get("entry_main",
                    "_Assembly.Molecular_mass")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntity_assemblyEntity_assembly_name = (String) ((ArrayList) starDict.toStar2D.get("mol_main",
                    "_Entity_assembly.Entity_assembly_name")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntity_assemblyAsym_ID = (String) ((ArrayList) starDict.toStar2D.get("mol_main",
                    "_Entity_assembly.Asym_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNameEntity_assemblyDetails = (String) ((ArrayList) starDict.toStar2D.get("mol_main",
                    "_Entity_assembly.Details")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            // tagNameEntity_assemblyEntry_ID = (String)
            // ((ArrayList)starDict.toStar2D.get( "mol_main" ,
            // "_Entity_assembly.Entry_ID"
            // )).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNamePDBX_nonpoly_schemeEntity_assembly_ID = (String) ((ArrayList) starDict.toStar2D.get("res_main",
                    "_PDBX_nonpoly_scheme.Entity_assembly_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNamePDBX_nonpoly_schemeEntity_ID = (String) ((ArrayList) starDict.toStar2D.get("res_main",
                    "_PDBX_nonpoly_scheme.Entity_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNamePDBX_nonpoly_schemeMon_ID = (String) ((ArrayList) starDict.toStar2D.get("res_main",
                    "_PDBX_nonpoly_scheme.Mon_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNamePDBX_nonpoly_schemeComp_index_ID = (String) ((ArrayList) starDict.toStar2D.get("res_main",
                    "_PDBX_nonpoly_scheme.Comp_index_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNamePDBX_nonpoly_schemeComp_ID = (String) ((ArrayList) starDict.toStar2D.get("res_main",
                    "_PDBX_nonpoly_scheme.Comp_ID")).get(StarDictionary.POSITION_STAR_TAG_NAME);
            tagNamePDBX_nonpoly_schemeAuth_seq_num = (String) ((ArrayList) starDict.toStar2D.get("res_main",
                    "_PDBX_nonpoly_scheme.Auth_seq_num")).get(StarDictionary.POSITION_STAR_TAG_NAME);
        } catch (Exception e) {
            General.showThrowable(e);
            General.showError("Failed to get all the tag names from dictionary compare code with dictionary");
            return false;
        }
        if (true) {
            @SuppressWarnings("unused")
            String[] tagNames = new String[] {
                    tagNameEntrySFCategory,
                    // tagNameEntryId,
                    tagNameEntryName,
                    // tagNameMolEntryId,
                    tagNameMolId,
                    tagNameMolAssEntityId,
                    tagNameMolAssEntityLabel,
                    tagNameMolSFCategory,
                    // tagNameMolEntityId,
                    tagNameMolType,
                    tagNameMolPolType,
                    tagNameMolSeqLength,
                    tagNameMolSeq,
                    tagNameResEntityId,
                    tagNameResNum_1,
                    tagNameResCompId,
                    tagNameResMolId,
                    tagNameResResId,
                    tagNameResNum_2,
                    tagNameResName,
                    tagNameChem_compSFCategory,
                    tagNameChem_compId,
                    tagNameChem_compType,
                    tagNameAtomSFCategory,
                    // tagNameAtomSFId_1,
                    tagNameAtomSFId_2,
                    tagNameAtomModelId,
                    tagNameAtomId,
                    tagNameAtomMolId1,
                    tagNameAtomMolId2,
                    tagNameAtomResId,
                    tagNameAtomResName,
                    tagNameAtomName,
                    tagNameAtomAuthMolId,
                    tagNameAtomAuthResId,
                    tagNameAtomAuthResName,
                    tagNameAtomAuthName,
                    tagNameAtomElementId,
                    tagNameAtomCoorX,
                    tagNameAtomCoorY,
                    tagNameAtomCoorZ,
                    tagNameAtomBFactor,
                    tagNameDCSfcategory,
                    // tagNameDCID,
                    tagNameDCMRfileblockposition,
                    // tagNameDCProgram,
                    tagNameDCType,
                    // tagNameDCSubtype,
                    // tagNameDCFormat,
                    tagNameDCtreeConstraintsID,
                    // tagNameDCtreeID,
                    tagNameDCtreeNodeID,
                    tagNameDCtreeDownnodeID,
                    tagNameDCtreeRightnodeID,
                    tagNameDCtreeLogicoperation,
                    tagNameDCConstraintsID,
                    // tagNameDCDistconstrainttreeID,
                    tagNameDCTreenodemembernodeID,
                    // tagNameDCContributionfractionalval,
                    tagNameDCConstrainttreenodememberID,
                    tagNameDCEntityassemblyID,
                    tagNameDCEntityID,
                    tagNameDCCompindexID,
                    tagNameDCCompID,
                    tagNameDCAtomID,
                    tagNameDCAuthsegmentcode,
                    tagNameDCAuthseqID,
                    tagNameDCAuthcompID,
                    tagNameDCAuthatomID,
                    // tagNameDCvalueConstraintsID,
                    tagNameDCvalueConstraintID,
                    tagNameDCvalueTreenodeID,
                    tagNameDCvalueSourceexperimentID,
                    tagNameDCvalueSpectralpeakID,
                    tagNameDCvalueIntensityval,
                    tagNameDCvalueIntensitylowervalerr,
                    tagNameDCvalueIntensityuppervalerr,
                    tagNameDCvalueDistanceval,
                    tagNameDCvalueDistancelowerboundval,
                    tagNameDCvalueDistanceupperboundval,
                    // tagNameDCvalueWeight,
                    // tagNameDCvalueSpectralpeakppm1,
                    // tagNameDCvalueSpectralpeakppm2,
                    // tagNameDCcommentorgConstraintsID,
                    // tagNameDCcommentorgID,
                    // tagNameDCcommentorgCommentbeginline,
                    // tagNameDCcommentorgCommentbegincolumn,
                    // tagNameDCcommentorgCommentendline,
                    // tagNameDCcommentorgCommentendcolumn,
                    // tagNameDCcommentorgComment,
                    // tagNameDCparsefileConstraintsID,
                    // tagNameDCparsefileID,
                    // tagNameDCparsefileName,
                    // tagNameDCparsefileconverrConstraintsID,
                    // tagNameDCparsefileconverrID,
                    // tagNameDCparsefileconverrParsefileID,
                    // tagNameDCparsefileconverrParsefilesflabel,
                    // tagNameDCparsefileconverrParsefileconstraintID,
                    // tagNameDCparsefileconverrConverrortype,
                    // tagNameDCparsefileconverrConverrornote,

                    tagNameCDIH_Sf_category,
                    // tagNameCDIH_ID1,
                    tagNameCDIH_MR_file_block_position,
                    // tagNameCDIH_Program,
                    // tagNameCDIH_Type,
                    // tagNameCDIH_Subtype,
                    // tagNameCDIH_Format,
                    // tagNameCDIH_Constraints_ID,
                    tagNameCDIH_ID2,
                    tagNameCDIH_Torsion_angle_name,
                    tagNameCDIH_Entity_assembly_ID_1,
                    tagNameCDIH_Entity_ID_1,
                    tagNameCDIH_Comp_index_ID_1,
                    tagNameCDIH_Comp_ID_1,
                    tagNameCDIH_Atom_ID_1,
                    tagNameCDIH_Entity_assembly_ID_2,
                    tagNameCDIH_Entity_ID_2,
                    tagNameCDIH_Comp_index_ID_2,
                    tagNameCDIH_Comp_ID_2,
                    tagNameCDIH_Atom_ID_2,
                    tagNameCDIH_Entity_assembly_ID_3,
                    tagNameCDIH_Entity_ID_3,
                    tagNameCDIH_Comp_index_ID_3,
                    tagNameCDIH_Comp_ID_3,
                    tagNameCDIH_Atom_ID_3,
                    tagNameCDIH_Entity_assembly_ID_4,
                    tagNameCDIH_Entity_ID_4,
                    tagNameCDIH_Comp_index_ID_4,
                    tagNameCDIH_Comp_ID_4,
                    tagNameCDIH_Atom_ID_4,
                    tagNameCDIH_Auth_segment_code_1,
                    tagNameCDIH_Auth_seq_ID_1,
                    tagNameCDIH_Auth_comp_ID_1,
                    tagNameCDIH_Auth_atom_ID_1,
                    tagNameCDIH_Auth_segment_code_2,
                    tagNameCDIH_Auth_seq_ID_2,
                    tagNameCDIH_Auth_comp_ID_2,
                    tagNameCDIH_Auth_atom_ID_2,
                    tagNameCDIH_Auth_segment_code_3,
                    tagNameCDIH_Auth_seq_ID_3,
                    tagNameCDIH_Auth_comp_ID_3,
                    tagNameCDIH_Auth_atom_ID_3,
                    tagNameCDIH_Auth_segment_code_4,
                    tagNameCDIH_Auth_seq_ID_4,
                    tagNameCDIH_Auth_comp_ID_4,
                    tagNameCDIH_Auth_atom_ID_4,
                    tagNameCDIH_Angle_upper_bound_val,
                    tagNameCDIH_Angle_lower_bound_val,
                    // tagNameCDIH_Force_constant_value,
                    // tagNameCDIH_Potential_function_exponent,
                    tagNameRDC_Sf_category,
                    // tagNameRDC_ID1,
                    tagNameRDC_MR_file_block_position,
                    // tagNameRDC_Program,
                    // tagNameRDC_Type,
                    // tagNameRDC_Subtype,
                    // tagNameRDC_Format,
                    // tagNameRDC_Constraints_ID,
                    tagNameRDC_ID2, tagNameRDC_Entity_assembly_ID_1, tagNameRDC_Entity_ID_1,
                    tagNameRDC_Comp_index_ID_1, tagNameRDC_Comp_ID_1, tagNameRDC_Atom_ID_1,
                    tagNameRDC_Entity_assembly_ID_2, tagNameRDC_Entity_ID_2, tagNameRDC_Comp_index_ID_2,
                    tagNameRDC_Comp_ID_2, tagNameRDC_Atom_ID_2, tagNameRDC_Auth_segment_code_1,
                    tagNameRDC_Auth_seq_ID_1, tagNameRDC_Auth_comp_ID_1, tagNameRDC_Auth_atom_ID_1,
                    tagNameRDC_Auth_segment_code_2, tagNameRDC_Auth_seq_ID_2,
                    tagNameRDC_Auth_comp_ID_2,
                    tagNameRDC_Auth_atom_ID_2,
                    tagNameRDC_RDC_val,
                    tagNameRDC_RDC_lower_bound,
                    tagNameRDC_RDC_upper_bound,
                    tagNameRDC_RDC_val_err,

                    // tagNameAssemblyEntry_ID,
                    tagNameAssemblyNumber_of_components, tagNameAssemblyOrganic_ligands, tagNameAssemblyMetal_ions,
                    tagNameAssemblyParamagnetic, tagNameAssemblyThiol_state, tagNameAssemblyMolecular_mass,
                    tagNameEntity_assemblyEntity_assembly_name,
                    tagNameEntity_assemblyAsym_ID,
                    tagNameEntity_assemblyDetails,
                    // tagNameEntity_assemblyEntry_ID,
                    tagNamePDBX_nonpoly_schemeEntity_assembly_ID, tagNamePDBX_nonpoly_schemeEntity_ID,
                    tagNamePDBX_nonpoly_schemeMon_ID, tagNamePDBX_nonpoly_schemeComp_index_ID,
                    tagNamePDBX_nonpoly_schemeComp_ID, tagNamePDBX_nonpoly_schemeAuth_seq_num, };
//			General.showDebug("Tagnames:\n" + Strings.toString(tagNames, true));
        }
        return true;
    }

    /** END BLOCK */

    /**
     * BEGIN BLOCK FOR SETTING NMR-STAR CONVENIENCE VARIABLES COPY FROM
     * Wattos.Star.NMRStar.File31 Will return true if tree with actual
     * restraints was found.
     */
    boolean initConvenienceVariablesStarWattosDC(SaveFrame sFDC) {

        // General.showOutput("TESTING initConvenienceVariablesStarWattos when did it happen?");
        tTIntro = sFDC.getTagTable(tagNameDCSfcategory, true);
        if (tTIntro == null) {
            General.showError("Failed to get DC intro tT by tag name:" + tagNameDCSfcategory);
            return false;
        }
        tTTree = sFDC.getTagTable(tagNameDCtreeLogicoperation, true);
        if (tTTree == null) {
            General.showWarning("Failed to get tree tT by tag name:" + tagNameDCtreeLogicoperation);
            return false;
        }
        tTAtom = sFDC.getTagTable(tagNameDCTreenodemembernodeID, true);
        if (tTAtom == null) {
            General.showWarning("Failed to get atom tT by tag name:" + tagNameDCTreenodemembernodeID);
            return false;
        }
        tTDist = sFDC.getTagTable(tagNameDCvalueDistanceval, true);
        if (tTDist == null) {
            General.showWarning("Failed to get distance tT by tag name:" + tagNameDCvalueDistanceval);
            return false;
        }
        // tTComm = sFDC.getTagTable(tagNameDCcommentorgConstraintsID, true);
        // if ( tTComm == null ) {
        // ;//General.showDebug("Failed to get comment info tT by tag name:" +
        // tagNameDCcommentorgConstraintsID );
        // }
        // tTPar1 = sFDC.getTagTable(tagNameDCparsefileConstraintsID, true);
        // if ( tTPar1 == null ) {
        // ;//General.showDebug("Failed to get parsed file info tT 1 by tag name:"
        // + tagNameDCparsefileConstraintsID );
        // }
        // tTPar2 = sFDC.getTagTable(tagNameDCparsefileconverrConstraintsID,
        // true);
        // if ( tTPar2 == null ) {
        // ;//General.showDebug("Failed to get parsed file info tT 2 by tag name:"
        // +
        // tagNameDCparsefileconverrConstraintsID );
        // }

        // intro, not required anymore beyond the first 2 tags.
        varDCSfcategory = tTIntro.getColumnString(tagNameDCSfcategory);
        // varDCID = tTIntro.getColumnInt( tagNameDCID );
        varDCType = null;
        varDCMRfileblockposition = null;
        varDCFileID = null;
        if (tTIntro.containsColumn(tagNameDCType)) { // Prevent some error
            // messages.
            varDCMRfileblockposition = tTIntro.getColumnInt(tagNameDCMRfileblockposition);
            varDCType = tTIntro.getColumnString(tagNameDCType);
            varDCFileID = tTIntro.getColumnInt(tagNameDCConstraint_file_ID);
        }

        // tree
        varDCtreeConstraintsID = tTTree.getColumnInt(tagNameDCtreeConstraintsID);
        // varDCtreeID = tTTree.getColumnInt( tagNameDCtreeID );
        varDCtreeNodeID = tTTree.getColumnInt(tagNameDCtreeNodeID);

        varDCtreeDownnodeID = tTTree.getColumnInt(tagNameDCtreeDownnodeID);
        varDCtreeRightnodeID = tTTree.getColumnInt(tagNameDCtreeRightnodeID);
        varDCtreeLogicoperation = tTTree.getColumnString(tagNameDCtreeLogicoperation);

        // atom
        varDCConstraintsID = tTAtom.getColumnInt(tagNameDCConstraintsID);
        // varDCDistconstrainttreeID = tTAtom.getColumnInt(
        // tagNameDCDistconstrainttreeID );
        varDCTreenodemembernodeID = tTAtom.getColumnInt(tagNameDCTreenodemembernodeID);
        // varDCContributionfractionalval = tTAtom.getColumnFloat(
        // tagNameDCContributionfractionalval );
        varDCConstrainttreenodememberID = tTAtom.getColumnInt(tagNameDCConstrainttreenodememberID);
        varDCEntityassemblyID = tTAtom.getColumnInt(tagNameDCEntityassemblyID);
        varDCEntityID = tTAtom.getColumnInt(tagNameDCEntityID);
        varDCCompindexID = tTAtom.getColumnInt(tagNameDCCompindexID);
        varDCCompID = tTAtom.getColumnString(tagNameDCCompID);
        varDCAtomID = tTAtom.getColumnString(tagNameDCAtomID);
        varDCAuthsegmentcode = tTAtom.getColumnString(tagNameDCAuthsegmentcode);
        varDCAuthseqID = tTAtom.getColumnString(tagNameDCAuthseqID);
        varDCAuthcompID = tTAtom.getColumnString(tagNameDCAuthcompID);
        varDCAuthatomID = tTAtom.getColumnString(tagNameDCAuthatomID);

        // distance
        // varDCvalueConstraintsID = tTDist.getColumnInt(
        // tagNameDCvalueConstraintsID );
        varDCvalueConstraintID = tTDist.getColumnInt(tagNameDCvalueConstraintID);
        varDCvalueTreenodeID = tTDist.getColumnInt(tagNameDCvalueTreenodeID);
        varDCvalueSourceexperimentID = tTDist.getColumnString(tagNameDCvalueSourceexperimentID);
        varDCvalueSpectralpeakID = tTDist.getColumnString(tagNameDCvalueSpectralpeakID);
        // varDCvalueIntensityval = tTDist.getColumnFloat(
        // tagNameDCvalueIntensityval );
        // varDCvalueIntensitylowervalerr = tTDist.getColumnFloat(
        // tagNameDCvalueIntensitylowervalerr );
        // varDCvalueIntensityuppervalerr = tTDist.getColumnFloat(
        // tagNameDCvalueIntensityuppervalerr );
        varDCvalueDistanceval = tTDist.getColumnFloat(tagNameDCvalueDistanceval);
        varDCvalueDistancelowerboundval = tTDist.getColumnFloat(tagNameDCvalueDistancelowerboundval);
        varDCvalueDistanceupperboundval = tTDist.getColumnFloat(tagNameDCvalueDistanceupperboundval);
        // varDCvalueWeight = tTDist.getColumnFloat( tagNameDCvalueWeight );
        // varDCvalueSpectralpeakppm1 = tTDist.getColumnFloat(
        // tagNameDCvalueSpectralpeakppm1 );
        // varDCvalueSpectralpeakppm2 = tTDist.getColumnFloat(
        // tagNameDCvalueSpectralpeakppm2 );

        // if ( tTComm != null ) {
        // varDCcommentorgConstraintsID = tTComm.getColumnInt(
        // tagNameDCcommentorgConstraintsID );
        // varDCcommentorgID = tTComm.getColumnInt( tagNameDCcommentorgID );
        // varDCcommentorgCommentbeginline = tTComm.getColumnInt(
        // tagNameDCcommentorgCommentbeginline );
        // varDCcommentorgCommentbegincolumn = tTComm.getColumnInt(
        // tagNameDCcommentorgCommentbegincolumn );
        // varDCcommentorgCommentendline = tTComm.getColumnInt(
        // tagNameDCcommentorgCommentendline );
        // varDCcommentorgCommentendcolumn = tTComm.getColumnInt(
        // tagNameDCcommentorgCommentendcolumn );
        // varDCcommentorgComment = tTComm.getColumnString(
        // tagNameDCcommentorgComment );
        // }
        // if ( tTPar1 != null ) {
        // varDCparsefileConstraintsID = tTPar1.getColumnInt(
        // tagNameDCparsefileConstraintsID );
        // varDCparsefileID = tTPar1.getColumnInt( tagNameDCparsefileID );
        // varDCparsefileName = tTPar1.getColumnString( tagNameDCparsefileName
        // );
        // }
        // if ( tTPar2 != null ) {
        // varDCparsefileconverrConstraintsID = tTPar2.getColumnInt(
        // tagNameDCparsefileconverrConstraintsID );
        // varDCparsefileconverrID = tTPar2.getColumnInt(
        // tagNameDCparsefileconverrID );
        // varDCparsefileconverrParsefileID = tTPar2.getColumnInt(
        // tagNameDCparsefileconverrParsefileID );
        // varDCparsefileconverrParsefilesflabel = tTPar2.getColumnString(
        // tagNameDCparsefileconverrParsefilesflabel );
        // varDCparsefileconverrParsefileconstraintID=tTPar2.getColumnInt(
        // tagNameDCparsefileconverrParsefileconstraintID );
        // varDCparsefileconverrConverrortype = tTPar2.getColumnString(
        // tagNameDCparsefileconverrConverrortype );
        // varDCparsefileconverrConverrornote = tTPar2.getColumnString(
        // tagNameDCparsefileconverrConverrornote );
        // }
        if (varDCSfcategory == null || varDCType == null) {
            General.showError("Failed to find all required columns in the distance constraint saveframe (A).");
            return false;
        }
        if (
        // varDCSubtype == null ||
        // varDCFormat == null ||
        varDCtreeConstraintsID == null
                ||
                // varDCtreeID == null ||
                varDCtreeNodeID == null || varDCtreeDownnodeID == null || varDCtreeRightnodeID == null
                || varDCtreeLogicoperation == null || varDCConstraintsID == null) {
            General.showError("Failed to find all required columns in the distance constraint saveframe (B).");
            return false;
        }
        if (
        // varDCDistconstrainttreeID == null ||
        varDCTreenodemembernodeID == null
                ||
                // varDCContributionfractionalval == null ||
                varDCConstrainttreenodememberID == null || varDCEntityassemblyID == null
                || varDCEntityID == null
                || varDCCompindexID == null
                || varDCCompID == null
                || varDCAtomID == null
                || varDCAuthsegmentcode == null
                || varDCAuthseqID == null
                || varDCAuthcompID == null
                || varDCAuthatomID == null
                ||
                // varDCvalueConstraintsID == null ||
                varDCvalueConstraintID == null
                || varDCvalueTreenodeID == null
                || varDCvalueSourceexperimentID == null
                ||
                // varDCvalueSpectralpeakID == null || not all of them are
                // needed and should be commented out like this
                // one.
                // varDCvalueIntensityval == null || // 3 absent in Wim's FC.
                // varDCvalueIntensitylowervalerr == null ||
                // varDCvalueIntensityuppervalerr == null ||
                varDCvalueDistanceval == null || varDCvalueDistancelowerboundval == null
                || varDCvalueDistanceupperboundval == null
        // varDCvalueWeight == null ||
        // varDCvalueSpectralpeakppm1 == null ||
        // varDCvalueSpectralpeakppm2 == null
        // (( tTComm != null ) && (
        // varDCcommentorgConstraintsID == null ||
        // varDCcommentorgID == null ||
        // varDCcommentorgCommentbeginline == null ||
        // varDCcommentorgCommentbegincolumn == null ||
        // varDCcommentorgCommentendline == null ||
        // varDCcommentorgCommentendcolumn == null ||
        // varDCcommentorgComment == null
        // )) ||
        // (( tTPar1 != null ) && (
        // varDCparsefileConstraintsID == null ||
        // varDCparsefileID == null ||
        // varDCparsefileName == null
        // )) ||
        // (( tTPar2 != null ) && (
        // varDCparsefileconverrConstraintsID == null ||
        // varDCparsefileconverrID == null ||
        // varDCparsefileconverrParsefileID == null ||
        // varDCparsefileconverrParsefilesflabel == null ||
        // varDCparsefileconverrParsefileconstraintID== null ||
        // varDCparsefileconverrConverrortype == null ||
        // varDCparsefileconverrConverrornote == null
        // ))
        ) {
            General.showError("Failed to find all required columns in the distance constraint saveframe (C).");
            return false;
        }
        /**
         * General.showWarning("Testing: length of table dc tree is: " +
         * varDCtreeConstraintsID.length);
         * General.showWarning("Testing: length of table dc atom is: " +
         * varDCConstraintsID.length);
         * General.showWarning("Testing: length of table dc dist is: " +
         * varDCvalueConstraintsID.length);
         */
        return true;
    }

    /**
     * BEGIN BLOCK FOR SETTING NMR-STAR CONVENIENCE VARIABLES COPY FROM
     * Wattos.Star.NMRStar.File31 Will return true if tree with actual
     * restraints was found.
     */
    private boolean initConvenienceVariablesStarWattosCDIH(SaveFrame sFCDIH) {

        tTIntro = sFCDIH.getTagTable(tagNameCDIH_Sf_category, true);
        if (tTIntro == null) {
            General.showError("Failed to get CDIH intro tT by tag name:" + tagNameCDIH_Sf_category);
            return false;
        }
        tTMain = sFCDIH.getTagTable(tagNameCDIH_ID2, true);
        if (tTMain == null) {
            General.showWarning("Failed to get tree tT by tag name:" + tagNameCDIH_ID2);
            return false;
        }

        /*
         * tTPar1 = sFCDIH.getTagTable(tagNameCDIHparsefileConstraintsID, true);
         * if ( tTPar1 == null ) {
         * ;//General.showDebug("Failed to get parsed file info tT 1 by tag name:"
         * + tagNameCDIHparsefileConstraintsID ); } tTPar2 =
         * sFCDIH.getTagTable(tagNameCDIHparsefileconverrConstraintsID, true);
         * if ( tTPar2 == null ) {
         * ;//General.showDebug("Failed to get parsed file info tT 2 by tag name:"
         * + tagNameCDIHparsefileconverrConstraintsID ); }
         */
        // intro, not required anymore beyond the first 2 tags.
        varCDIH_Sf_category = tTIntro.getColumnString(tagNameCDIH_Sf_category);
        // varCDIH_ID1 = tTIntro.getColumnInt( tagNameCDIH_ID1 );
        if (tTIntro.containsColumn(tagNameCDIH_MR_file_block_position)) { // Prevent
            // some
            // error
            // messages.
            varCDIH_MR_file_block_position = tTIntro.getColumnInt(tagNameCDIH_MR_file_block_position);
            // varCDIH_Program = tTIntro.getColumnString( tagNameCDIH_Program );
            // varCDIH_Type = tTIntro.getColumnString( tagNameCDIH_Type );
            // varCDIH_Subtype = tTIntro.getColumnString( tagNameCDIH_Subtype );
            // varCDIH_Format = tTIntro.getColumnString( tagNameCDIH_Format );
        } else {
            varCDIH_MR_file_block_position = null;
            // varCDIH_Program = null;
            // varCDIH_Type = null;
            // varCDIH_Subtype = null;
            // varCDIH_Format = null;
        }

        // tree
        // varCDIH_Constraints_ID = tTMain.getColumnInt(
        // tagNameCDIH_Constraints_ID);
        varCDIH_ID2 = tTMain.getColumnInt(tagNameCDIH_ID2);
        varCDIH_Torsion_angle_name = tTMain.getColumnString(tagNameCDIH_Torsion_angle_name);
        varCDIH_Label_entity_assembly_ID_1 = tTMain.getColumnInt(tagNameCDIH_Entity_assembly_ID_1);
        varCDIH_Label_entity_ID_1 = tTMain.getColumnInt(tagNameCDIH_Entity_ID_1);
        varCDIH_Label_comp_index_ID_1 = tTMain.getColumnInt(tagNameCDIH_Comp_index_ID_1);
        varCDIH_Label_comp_ID_1 = tTMain.getColumnString(tagNameCDIH_Comp_ID_1);
        varCDIH_Label_atom_ID_1 = tTMain.getColumnString(tagNameCDIH_Atom_ID_1);
        varCDIH_Label_entity_assembly_ID_2 = tTMain.getColumnInt(tagNameCDIH_Entity_assembly_ID_2);
        varCDIH_Label_entity_ID_2 = tTMain.getColumnInt(tagNameCDIH_Entity_ID_2);
        varCDIH_Label_comp_index_ID_2 = tTMain.getColumnInt(tagNameCDIH_Comp_index_ID_2);
        varCDIH_Label_comp_ID_2 = tTMain.getColumnString(tagNameCDIH_Comp_ID_2);
        varCDIH_Label_atom_ID_2 = tTMain.getColumnString(tagNameCDIH_Atom_ID_2);
        varCDIH_Label_entity_assembly_ID_3 = tTMain.getColumnInt(tagNameCDIH_Entity_assembly_ID_3);
        varCDIH_Label_entity_ID_3 = tTMain.getColumnInt(tagNameCDIH_Entity_ID_3);
        varCDIH_Label_comp_index_ID_3 = tTMain.getColumnInt(tagNameCDIH_Comp_index_ID_3);
        varCDIH_Label_comp_ID_3 = tTMain.getColumnString(tagNameCDIH_Comp_ID_3);
        varCDIH_Label_atom_ID_3 = tTMain.getColumnString(tagNameCDIH_Atom_ID_3);
        varCDIH_Label_entity_assembly_ID_4 = tTMain.getColumnInt(tagNameCDIH_Entity_assembly_ID_4);
        varCDIH_Label_entity_ID_4 = tTMain.getColumnInt(tagNameCDIH_Entity_ID_4);
        varCDIH_Label_comp_index_ID_4 = tTMain.getColumnInt(tagNameCDIH_Comp_index_ID_4);
        varCDIH_Label_comp_ID_4 = tTMain.getColumnString(tagNameCDIH_Comp_ID_4);
        varCDIH_Label_atom_ID_4 = tTMain.getColumnString(tagNameCDIH_Atom_ID_4);
        varCDIH_Auth_segment_code_1 = tTMain.getColumnString(tagNameCDIH_Auth_segment_code_1);
        varCDIH_Auth_seq_ID_1 = tTMain.getColumnString(tagNameCDIH_Auth_seq_ID_1);
        varCDIH_Auth_comp_ID_1 = tTMain.getColumnString(tagNameCDIH_Auth_comp_ID_1);
        varCDIH_Auth_atom_ID_1 = tTMain.getColumnString(tagNameCDIH_Auth_atom_ID_1);
        varCDIH_Auth_segment_code_2 = tTMain.getColumnString(tagNameCDIH_Auth_segment_code_2);
        varCDIH_Auth_seq_ID_2 = tTMain.getColumnString(tagNameCDIH_Auth_seq_ID_2);
        varCDIH_Auth_comp_ID_2 = tTMain.getColumnString(tagNameCDIH_Auth_comp_ID_2);
        varCDIH_Auth_atom_ID_2 = tTMain.getColumnString(tagNameCDIH_Auth_atom_ID_2);
        varCDIH_Auth_segment_code_3 = tTMain.getColumnString(tagNameCDIH_Auth_segment_code_3);
        varCDIH_Auth_seq_ID_3 = tTMain.getColumnString(tagNameCDIH_Auth_seq_ID_3);
        varCDIH_Auth_comp_ID_3 = tTMain.getColumnString(tagNameCDIH_Auth_comp_ID_3);
        varCDIH_Auth_atom_ID_3 = tTMain.getColumnString(tagNameCDIH_Auth_atom_ID_3);
        varCDIH_Auth_segment_code_4 = tTMain.getColumnString(tagNameCDIH_Auth_segment_code_4);
        varCDIH_Auth_seq_ID_4 = tTMain.getColumnString(tagNameCDIH_Auth_seq_ID_4);
        varCDIH_Auth_comp_ID_4 = tTMain.getColumnString(tagNameCDIH_Auth_comp_ID_4);
        varCDIH_Auth_atom_ID_4 = tTMain.getColumnString(tagNameCDIH_Auth_atom_ID_4);
        varCDIH_Angle_upper_bound_val = tTMain.getColumnFloat(tagNameCDIH_Angle_upper_bound_val);
        varCDIH_Angle_lower_bound_val = tTMain.getColumnFloat(tagNameCDIH_Angle_lower_bound_val);
        // varCDIH_Force_constant_value = tTMain.getColumnFloat(
        // tagNameCDIH_Force_constant_value);
        // varCDIH_Potential_function_exponent = tTMain.getColumnFloat(
        // tagNameCDIH_Potential_function_exponent);

        if (varCDIH_Sf_category == null
                ||
                // varCDIH_ID1 == null ||
                /**
                 * varCDIHMRfileblockposition == null || varCDIHProgram == null
                 * || varCDIHType == null || varCDIHSubtype == null ||
                 * varCDIHFormat == null ||
                 */
                // varCDIH_Constraints_ID ==null|
                varCDIH_ID2 == null
                |
                // varCDIH_Torsion_angle_name ==null| // not present in Wim's
                varCDIH_Label_entity_assembly_ID_1 == null | varCDIH_Label_entity_ID_1 == null
                | varCDIH_Label_comp_index_ID_1 == null | varCDIH_Label_comp_ID_1 == null
                | varCDIH_Label_atom_ID_1 == null | varCDIH_Label_entity_assembly_ID_2 == null
                | varCDIH_Label_entity_ID_2 == null | varCDIH_Label_comp_index_ID_2 == null
                | varCDIH_Label_comp_ID_2 == null | varCDIH_Label_atom_ID_2 == null
                | varCDIH_Label_entity_assembly_ID_3 == null | varCDIH_Label_entity_ID_3 == null
                | varCDIH_Label_comp_index_ID_3 == null | varCDIH_Label_comp_ID_3 == null
                | varCDIH_Label_atom_ID_3 == null | varCDIH_Label_entity_assembly_ID_4 == null
                | varCDIH_Label_entity_ID_4 == null | varCDIH_Label_comp_index_ID_4 == null
                | varCDIH_Label_comp_ID_4 == null | varCDIH_Label_atom_ID_4 == null |
                // varCDIH_Auth_segment_code_1 ==null|
                // varCDIH_Auth_seq_ID_1 ==null|
                // varCDIH_Auth_comp_ID_1 ==null|
                // varCDIH_Auth_atom_ID_1 ==null|
                // varCDIH_Auth_segment_code_2 ==null|
                // varCDIH_Auth_seq_ID_2 ==null|
                // varCDIH_Auth_comp_ID_2 ==null|
                // varCDIH_Auth_atom_ID_2 ==null|
                // varCDIH_Auth_segment_code_3 ==null|
                // varCDIH_Auth_seq_ID_3 ==null|
                // varCDIH_Auth_comp_ID_3 ==null|
                // varCDIH_Auth_atom_ID_3 ==null|
                // varCDIH_Auth_segment_code_4 ==null|
                // varCDIH_Auth_seq_ID_4 ==null|
                // varCDIH_Auth_comp_ID_4 ==null|
                // varCDIH_Auth_atom_ID_4 ==null|
                varCDIH_Angle_upper_bound_val == null | varCDIH_Angle_lower_bound_val == null
        // varCDIH_Force_constant_value ==null|
        // varCDIH_Potential_function_exponent ==null
        ) {
            General.showError("Failed to find all required columns in CDIH constraint saveframe.");
            return false;
        }
        return true;
    }

    /**
     * BEGIN BLOCK FOR SETTING NMR-STAR CONVENIENCE VARIABLES COPY FROM
     * Wattos.Star.NMRStar.File31 Will return true if tree with actual
     * restraints was found.
     */
    private boolean initConvenienceVariablesStarWattosRDC(SaveFrame sFRDC) {

        tTIntro = sFRDC.getTagTable(tagNameRDC_Sf_category, true);
        if (tTIntro == null) {
            General.showError("Failed to get RDC intro tT by tag name:" + tagNameRDC_Sf_category);
            return false;
        }
        tTMain = sFRDC.getTagTable(tagNameRDC_ID2, true);
        if (tTMain == null) {
            General.showWarning("Failed to get tree tT by tag name:" + tagNameRDC_ID2);
            return false;
        }

        /*
         * tTPar1 = sFRDC.getTagTable(tagNameRDCparsefileConstraintsID, true);
         * if ( tTPar1 == null ) {
         * ;//General.showDebug("Failed to get parsed file info tT 1 by tag name:"
         * + tagNameRDCparsefileConstraintsID ); } tTPar2 =
         * sFRDC.getTagTable(tagNameRDCparsefileconverrConstraintsID, true); if
         * ( tTPar2 == null ) {
         * ;//General.showDebug("Failed to get parsed file info tT 2 by tag name:"
         * + tagNameRDCparsefileconverrConstraintsID ); }
         */
        // intro, not required anymore beyond the first 2 tags.
        varRDC_Sf_category = tTIntro.getColumnString(tagNameRDC_Sf_category);
        // varRDC_ID1 = tTIntro.getColumnInt( tagNameRDC_ID1 );
        if (tTIntro.containsColumn(tagNameRDC_MR_file_block_position)) { // Prevent
            // some
            // error
            // messages.
            varRDC_MR_file_block_position = tTIntro.getColumnInt(tagNameRDC_MR_file_block_position);
            // varRDC_Program = tTIntro.getColumnString( tagNameRDC_Program );
            // varRDC_Type = tTIntro.getColumnString( tagNameRDC_Type );
            // varRDC_Subtype = tTIntro.getColumnString( tagNameRDC_Subtype );
            // varRDC_Format = tTIntro.getColumnString( tagNameRDC_Format );
        } else {
            varRDC_MR_file_block_position = null;
            // varRDC_Program = null;
            // varRDC_Type = null;
            // varRDC_Subtype = null;
            // varRDC_Format = null;
        }

        // tree
        // varRDC_Constraints_ID = tTMain.getColumnInt(
        // tagNameRDC_Constraints_ID);
        varRDC_ID2 = tTMain.getColumnInt(tagNameRDC_ID2);
        varRDC_Label_entity_assembly_ID_1 = tTMain.getColumnInt(tagNameRDC_Entity_assembly_ID_1);
        varRDC_Label_entity_ID_1 = tTMain.getColumnInt(tagNameRDC_Entity_ID_1);
        varRDC_Label_comp_index_ID_1 = tTMain.getColumnInt(tagNameRDC_Comp_index_ID_1);
        varRDC_Label_comp_ID_1 = tTMain.getColumnString(tagNameRDC_Comp_ID_1);
        varRDC_Label_atom_ID_1 = tTMain.getColumnString(tagNameRDC_Atom_ID_1);
        varRDC_Label_entity_assembly_ID_2 = tTMain.getColumnInt(tagNameRDC_Entity_assembly_ID_2);
        varRDC_Label_entity_ID_2 = tTMain.getColumnInt(tagNameRDC_Entity_ID_2);
        varRDC_Label_comp_index_ID_2 = tTMain.getColumnInt(tagNameRDC_Comp_index_ID_2);
        varRDC_Label_comp_ID_2 = tTMain.getColumnString(tagNameRDC_Comp_ID_2);
        varRDC_Label_atom_ID_2 = tTMain.getColumnString(tagNameRDC_Atom_ID_2);
        varRDC_Auth_segment_code_1 = tTMain.getColumnString(tagNameRDC_Auth_segment_code_1);
        varRDC_Auth_seq_ID_1 = tTMain.getColumnString(tagNameRDC_Auth_seq_ID_1);
        varRDC_Auth_comp_ID_1 = tTMain.getColumnString(tagNameRDC_Auth_comp_ID_1);
        varRDC_Auth_atom_ID_1 = tTMain.getColumnString(tagNameRDC_Auth_atom_ID_1);
        varRDC_Auth_segment_code_2 = tTMain.getColumnString(tagNameRDC_Auth_segment_code_2);
        varRDC_Auth_seq_ID_2 = tTMain.getColumnString(tagNameRDC_Auth_seq_ID_2);
        varRDC_Auth_comp_ID_2 = tTMain.getColumnString(tagNameRDC_Auth_comp_ID_2);
        varRDC_Auth_atom_ID_2 = tTMain.getColumnString(tagNameRDC_Auth_atom_ID_2);

        varRDC_val = tTMain.getColumnFloat(tagNameRDC_RDC_val);
        varRDC_lower_bound = tTMain.getColumnFloat(tagNameRDC_RDC_lower_bound);
        varRDC_upper_bound = tTMain.getColumnFloat(tagNameRDC_RDC_upper_bound);
        varRDC_val_err = tTMain.getColumnFloat(tagNameRDC_RDC_val_err);

        if (varRDC_Sf_category == null
                ||
                // varRDC_ID1 == null ||
                /**
                 * varRDCMRfileblockposition == null || varRDCProgram == null ||
                 * varRDCType == null || varRDCSubtype == null || varRDCFormat
                 * == null ||
                 */
                // varRDC_Constraints_ID ==null|
                varRDC_ID2 == null | varRDC_Label_entity_assembly_ID_1 == null | varRDC_Label_entity_ID_1 == null
                | varRDC_Label_comp_index_ID_1 == null | varRDC_Label_comp_ID_1 == null
                | varRDC_Label_atom_ID_1 == null | varRDC_Label_entity_assembly_ID_2 == null
                | varRDC_Label_entity_ID_2 == null | varRDC_Label_comp_index_ID_2 == null
                | varRDC_Label_comp_ID_2 == null | varRDC_Label_atom_ID_2 == null | varRDC_Auth_segment_code_1 == null
                | varRDC_Auth_seq_ID_1 == null | varRDC_Auth_comp_ID_1 == null | varRDC_Auth_atom_ID_1 == null
                | varRDC_Auth_segment_code_2 == null | varRDC_Auth_seq_ID_2 == null | varRDC_Auth_comp_ID_2 == null
                | varRDC_Auth_atom_ID_2 == null | varRDC_val == null | varRDC_lower_bound == null
                | varRDC_upper_bound == null | varRDC_val_err == null) {
            General.showError("Failed to find all required columns in RDC constraint saveframe.");
            return false;
        }
        return true;
    }

    /**
     * Convert the data in the file to components in the gumbo etc. Will list
     * max of one conversion error per column in db. Deselects all other entries
     * in DBMS until now. If atoms in restraint data can not be linked to atoms
     * in coordinate list they will be marked in e.g. dc.hasUnLinkedAtom.
     *
     * @return true for success.
     * @param matchRestraints2SoupByAuthorDetails
     *            Normally atoms are linked by the regular NMR-STAR tags. The
     *            info defined in the author details will be used to link atoms
     *            when this parameter is set.
     * @param matchRestraints2SoupByDefaultDetails
     *            Try to link the atoms in restraints to the molecules in the
     *            soup.
     * @param url
     * @param doEntry
     *            Absorb the molecular system description and the coordinates
     *            into Wattos.
     * @param doRestraints
     *            Absorb the restraints.
     * @param removeUnlinkedRestraints
     *            After possibly matching the atoms in the restraints to the
     *            soup remove any that have unmatched atoms.
     * @param syncModels
     *            Remove any unsynced atoms. Ie atoms that are not present in
     *            all models.
     */
    public boolean toWattos(URL url, boolean doEntry, boolean doRestraints,
            boolean matchRestraints2SoupByDefaultDetails, boolean matchRestraints2SoupByAuthorDetails,
            boolean removeUnlinkedRestraints, boolean syncModels) {

        // Use a temporary dbms so the data doesn't clobber the regular one.
        DBMS dbmsTemp = new DBMS();
        StarFileReader sfr = new StarFileReader(dbmsTemp);
        StarNode sn = sfr.parse(url);
        if (sn == null) {
            General.showError("entry.readNmrStarFormattedFile was unsuccessful. Failed to read nmrstar formatted file");
            return false;
        }
        Object o_tmp_1 = sn.get(0);
        if (!(o_tmp_1 instanceof DataBlock)) {
            General.showError("Expected top level object of type DataBlock but got: " + o_tmp_1.getClass().getName());
            return false;
        }
        DataBlock db = (DataBlock) o_tmp_1;

        // General.showDebug( "Read star file was: "+db.toSTAR());

        StarDictionary starDict = ui.wattosLib.starDictionary;
        if (starDict == null) {
            General.showCodeBug("Couldn find the star dictionary in ui.wattosLib");
            return false;
        }

        boolean status = db.translateToNativeTypesByDict(starDict, false);
        if (!status) {
            General
                    .showError("Failed to automatically convert to native types as defined in the star dictionary. Check input and try again please.");
            return false;
        }
        // General.showDebug("Automatically converted to native types as defined in the star dictionary.");

        /**
         * Get file characteristics ArrayList tTList = db.getTagTableList(
         * StarGeneral.WILDCARD, "file_characteristics", StarGeneral.WILDCARD,
         * StarGeneral.WILDCARD); if ( tTList == null ) { General.showWarning("Failed to find saveframe of category: file_characteristics. Not setting database and db id."
         * ); } else if ( tTList.size() != 1 ) { General.showWarning(
         * "Found number of saveframes of category: file_characteristics not 1 but: "
         * + tTList.size() + " Not setting database and db id."); } else { OrfId
         * orfId = new OrfId(); TagTable tT = (TagTable) tTList.get(0);
         * orfId.orf_db_id = tT.getColumn( starDict.toStar2D.get( ); }
         */

        if (doEntry) {
            SaveFrame sFAssembly = db.getSaveFrameByCategory("assembly", false);
            SaveFrame sfCoor = db.getSaveFrameByCategory("conformer_family_coord_set", false);
            ArrayList sfEntityList = db.getSaveFrameListByCategory("entity"); // ordered
            // list
            if (sFAssembly == null) {
                General.showError("Failed to find assembly sf.");
                return false;
            }
            if (sfCoor == null) {
                General
                        .showError("Failed to find conformer_family_coord_set sf. If you wanted to continue making 1 model based on molecular system description only, then modify code.");
                return false;
            }
            if (sfEntityList == null) {
                General.showError("Failed to find any entity sf.");
                return false;
            }

            // ENTRY
            TagTable tTAssemblyIntro = sFAssembly.getTagTable(tagNameEntrySFCategory, false);
            if (tTAssemblyIntro == null) {
                General.showError("Failed to find Assembly intro tT.");
                return false;
            }

            // MODEL
            int modelCountMax = 1;
            if (sfCoor != null) { // Make just one model if there's no
                // coordinates.
                TagTable tTCoor = sfCoor.getTagTable(tagNameAtomModelId, true);
                // Check to see the first is 1
                if (tTCoor.getValueInt(0, tagNameAtomModelId) != 1) {
                    General.showError("The models need to be numbered starting at 1 but found:"
                            + tTCoor.getValueInt(0, tagNameAtomModelId));
                    return false;
                }
                // Last model's number
                // this wouldn't work with gaps in relation but since the data
                // is read consecutively from
                // the star file it always works.
                modelCountMax = tTCoor.getValueInt(tTCoor.sizeRows - 1, tagNameAtomModelId);
            }

            // MOLECULES
            TagTable tTAssembly = sFAssembly.getTagTable(tagNameMolId, false);
            if (tTAssembly == null) {
                General.showError("Failed to find Assembly tT.");
                return false;
            }
            /**
             * We got for 1ai0:
             *
             * _Entity_assembly.ID _Entity_assembly.Entity_assembly_name
             * _Entity_assembly.Entity_ID _Entity_assembly.Entity_label
             * _Entity_assembly.Asym_ID _Entity_assembly.Details
             * _Entity_assembly.Entry_ID _Entity_assembly.Assembly_ID
             *
             * 1 . 1 $R6_INSULIN_HEXAMER A . 1ai0 1 2 . 2 $R6_INSULIN_HEXAMER_2
             * B . 1ai0 1 3 . 1 $R6_INSULIN_HEXAMER C . 1ai0 1 4 . 2
             * $R6_INSULIN_HEXAMER_2 D . 1ai0 1 5 . 1 $R6_INSULIN_HEXAMER E .
             * 1ai0 1 6 . 2 $R6_INSULIN_HEXAMER_2 F . 1ai0 1 7 . 1
             * $R6_INSULIN_HEXAMER G . 1ai0 1 8 . 2 $R6_INSULIN_HEXAMER_2 H .
             * 1ai0 1 9 . 1 $R6_INSULIN_HEXAMER I . 1ai0 1 10 . 2
             * $R6_INSULIN_HEXAMER_2 J . 1ai0 1 11 . 1 $R6_INSULIN_HEXAMER K .
             * 1ai0 1 12 . 2 $R6_INSULIN_HEXAMER_2 L . 1ai0 1 13 . 3 $ZINC_ION M
             * . 1ai0 1 14 . 3 $ZINC_ION N . 1ai0 1 15 . 4 $PHENOL O . 1ai0 1 16
             * . 4 $PHENOL P . 1ai0 1 17 . 4 $PHENOL Q . 1ai0 1 18 . 4 $PHENOL R
             * . 1ai0 1 19 . 4 $PHENOL S . 1ai0 1 20 . 4 $PHENOL T . 1ai0 1 21 .
             * 5 $water U . 1ai0 1
             *
             * Only allow sequential _Entity_assembly.ID in the table.
             */
            if (!tTAssembly.isSortedFromOneInColumn(tagNameMolId)) {
                General.showError("The Assembly tT needs to be ordered on the entity id.");
                return false;
            }
            // Assume no rows are deleted (Safe after test above)
            int molCountMax = tTAssembly.sizeRows;

            int[] entityIdList = (int[]) tTAssembly.getColumn(tagNameMolAssEntityId);
            // Cache the rids so we don't need fancy lookup below.
            // Cost are reasonable: 40*2*200*(4 bytes for int)=64kb
            int[] rid_model = new int[modelCountMax + 1];
            int[][] rid_mol = new int[modelCountMax + 1][molCountMax + 1];
            int[][][] rid_res = new int[modelCountMax + 1][molCountMax + 1][];

            // ENTRY
            String entryName = InOut.getFilenameBase(new File(url.getFile()));
            if (entryName.length() > 4) {
                entryName = entryName.substring(0, 4);
            }
            OrfIdList orfIdList = null;
            currentEntryId = entry.add(entryName, orfIdList, entryName);
            if (currentEntryId < 0) {
                General.showCodeBug("Failed to add an entry into dbms.");
                return false;
            }
            // General.showDebug("Unselecting all entries, models, mols, etc. in DBMS"
            // );
            if (!dbms.setValuesInAllTablesInColumn(Relation.DEFAULT_ATTRIBUTE_SELECTED, new Boolean(false))) {
                General.showError("Failed dbms.setValuesInAllTablesInColumn");
                return false;
            }
            General.showDebug("Setting selected for entry with rid: " + currentEntryId);
            entry.selected.set(currentEntryId);

            // MODEL; this loop could be redone so not all code needs to be
            // repeated for each model.
            // we'll keep an eye for it in the profiler.
            for (int modelCount = 1; modelCount <= modelCountMax; modelCount++) {
                currentModelId = model.add(modelCount, currentEntryId);
                if (currentModelId < 0) {
                    General.showCodeBug("Failed to add model number:" + modelCount + " into dbms.");
                    return false;
                }
                model.selected.set(currentModelId);
                // cache rid model
                rid_model[modelCount] = currentModelId;

                int tTAssemblyRID = 0;
                // XXX mark of MOLECULE
                for (int molCount = 1; molCount <= molCountMax; molCount++) {
                    int entityId = entityIdList[tTAssemblyRID];
                    // SaveFrame sFRes =
                    // SaveFrame.selectSaveFrameByTagNameAndFreeValue(
                    // sfEntityList,
                    // tagNameMolEntityId, new Integer(entityId));
                    // Assumes they are written in order of appearance in the
                    // assembly.
                    SaveFrame sFRes = (SaveFrame) sfEntityList.get(entityId - 1);
                    if (sFRes == null) {
                        // General.showError("Failed to get entity sf by tag name and free value for column name:"
                        // +
                        // tagNameMolEntityId + " and value: " + molCount);
                        General.showError("Failed to get entity sf by order for molcount value: " + molCount);
                        return false;
                    }
                    /**
                     * Got: _Entity.Sf_category entity _Entity.Entry_ID 1ai0
                     * _Entity.ID 5 _Entity.Name water _Entity.Type water
                     *
                     * loop_ _Entity_comp_index.ID _Entity_comp_index.Comp_ID
                     * _Entity_comp_index.Comp_label _Entity_comp_index.Entry_ID
                     * _Entity_comp_index.Entity_ID
                     *
                     * 1 HOH $HOH 1ai0 5 2 HOH $HOH 1ai0 5
                     */
                    currentMolId = mol.add(null, Defs.NULL_CHAR, currentModelId, null);
                    if (currentMolId < 0) {
                        General.showCodeBug("Failed to add mol number:" + currentMolId + " into dbms.");
                        return false;
                    }
                    mol.selected.set(currentMolId);

                    // cache rid molecule
                    rid_mol[modelCount][molCount] = currentMolId;

                    String molType = Molecule.typeEnum[Molecule.UNKNOWN_TYPE];
                    String molPolType = Molecule.polTypeEnum[Molecule.UNKNOWN_POL_TYPE];
                    TagTable tTResHeader = sFRes.getTagTable(tagNameMolType, true);
                    if (tTResHeader == null) {
                        General.showError("Failed to get tTResHeader by tag name:" + tagNameMolType
                                + " for saveframe: " + sFRes.title + " and entity id: " + entityId
                                + " for all models, assuming unknown mol type and mol pol types.");
                        return false;
                    }
                    molType = tTResHeader.getValueString(0, tagNameMolType);
                    if (molType.equals(Molecule.typeEnum[Molecule.POLYMER_TYPE])) {
                        molPolType = tTResHeader.getValueString(0, tagNameMolPolType);
                    } else {
                        molPolType = Molecule.polTypeEnum[Molecule.NON_POLYMER_POL_TYPE];
                    }

                    // bad 3 lines of code following.
                    if (molType.equals(Molecule.typeEnum[Molecule.NON_POLYMER_TYPE])) {
                        molPolType = Molecule.polTypeEnum[Molecule.NON_POLYMER_POL_TYPE];
                    }
                    String molName = tTAssembly.getValueString(tTAssemblyRID, tagNameMolAssEntityLabel);
                    // TODO skip next 3 lines when FC updates.
                    if (tTResHeader.hasColumn(tagNameMolName)) {
                        molName = tTResHeader.getValueString(0, tagNameMolName);
                    }
                    molName = convertEntityName2MolName(molName);
                    mol.setName(currentMolId, convertEntityName2MolName(molName));
                    mol.setType(currentMolId, molType);
                    mol.setPolType(currentMolId, molPolType);

                    /**
                     * First residue table that's always there even in case of
                     * water
                     */
                    TagTable tTRes1 = sFRes.getTagTable(tagNameResNum_1, false);
                    /**
                     * Not encouraged to use TagTable tTRes =
                     * sFRes.getTagTable(tagNameResResId, false);
                     */
                    int resCountMax = 1; // in case of non-polymer
                    if (tTRes1 == null) {
                        General.showDebug("No tT by tag name:" + tagNameResNum_1
                                + " ; aborting as it should be present.");
                        return false;
                    }
                    // Check to see the first is 1
                    if (tTRes1.getValueInt(0, tagNameResNum_1) != 1) {
                        General.showError("The residue's ids need to be numbered starting at 1 but at start found:"
                                + tTRes1.getValueInt(0, tagNameResNum_1));
                        General.showError("Table is: " + tTRes1);
                        return false;
                    }
                    // Last residue's id
                    resCountMax = tTRes1.getValueInt(tTRes1.sizeRows - 1, tagNameResNum_1);

                    if (resCountMax < 1 || Defs.isNull(resCountMax)) {
                        General.showError("Expected a positive resCountMax (not null either) but found:" + resCountMax);
                        return false;
                    }
                    rid_res[modelCount][molCount] = new int[resCountMax + 1]; // assign
                    // last
                    // dimension
                    // within.

                    // RESIDUES
                    String[] resNameList = (String[]) tTRes1.getColumn(tagNameResCompId);
                    int[] resSeqId = (int[]) tTRes1.getColumn(tagNameResNum_1);
                    int tTResRID = 0;
                    for (int resCount = 1; resCount <= resCountMax; resCount++) {
                        // 16,000 iterations is still limited for execution so
                        // that not all optimized
                        // routines are needed.
                        currentResId = res.add(resNameList[tTResRID], resSeqId[tTResRID], Defs.NULL_STRING_NULL,
                                Defs.NULL_STRING_NULL, currentMolId);
                        if (currentResId < 0) {
                            General.showCodeBug("Failed to add res number:" + currentResId + " into dbms.");
                            return false;
                        }
                        res.selected.set(currentResId);
                        // cache rid residue
                        rid_res[modelCount][molCount][resCount] = currentResId;
                        tTResRID++;
                    }
                    tTAssemblyRID++;
                }
            }
            // XXX mark of ATOMS
            /**
             * _Atom_site.Model_ID _Atom_site.ID
             * _Atom_site.Label_entity_assembly_ID _Atom_site.Label_entity_ID
             * _Atom_site.Label_comp_index_ID _Atom_site.Label_comp_ID
             * _Atom_site.Label_atom_ID _Atom_site.Type_symbol
             * _Atom_site.Cartn_x _Atom_site.Cartn_y _Atom_site.Cartn_z
             * _Atom_site.Occupancy _Atom_site.Auth_asym_ID
             * _Atom_site.Auth_seq_ID _Atom_site.Auth_comp_ID
             * _Atom_site.Auth_atom_ID _Atom_site.Entry_ID
             * _Atom_site.Conformer_family_coord_set_ID 1 1 1 1 1 GLY N N 3.445
             * 18.796 -24.642 0.0 A 1 GLY N 1ai0 1 1 2 1 1 1 GLY CA C 3.375
             * 18.022 -25.915 0.0 A 1 GLY CA 1ai0 1 ... 2 9601 21 5 1 HOH H2 H
             * 0.862 -2.010 -23.901 0.0 . 9 HOH H2 1ai0 1 2 9602 21 5 2 HOH O O
             * 0.272 -1.726 -3.829 0.0 . 10 HOH O 1ai0 1 2 9603 21 5 2 HOH H1 H
             * 0.698 -1.088 -3.255 0.0 . 10 HOH H1 1ai0 1 2 9604 21 5 2 HOH H2 H
             * 0.960 -2.359 -4.041 0.0 . 10 HOH H2 1ai0 1
             */
            if (sfCoor != null) { // Make just one model without atoms if
                // there're no coordinates.
                TagTable tTCoor = sfCoor.getTagTable(tagNameAtomModelId, true);
                // Already checked to see the first is 1
                int atomCountMax = tTCoor.sizeRows;

                /**
                 * Strategy will be to add columns to the original table in STAR
                 * and then copy the complete set to the dbms main table for
                 * atoms. The copy can be batched and will be faster than copy
                 * one by one row.
                 */
                String[] newbies = { Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RelationSet.RELATION_ID_COLUMN_NAME],
                        Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME],
                        Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
                        Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                        Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR, Relation.DEFAULT_ATTRIBUTE_SELECTED,
                        Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID };
                if (!(tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RelationSet.RELATION_ID_COLUMN_NAME],
                        Relation.DATA_TYPE_INT, null)
                        && tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME],
                                Relation.DATA_TYPE_INT, null)
                        && tTCoor.insertColumn(0,
                                Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
                                Relation.DATA_TYPE_INT, null)
                        && tTCoor.insertColumn(0,
                                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                                Relation.DATA_TYPE_INT, null)
                        && tTCoor.insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR, Relation.DATA_TYPE_BIT, null)
                        && tTCoor.insertColumn(0, Relation.DEFAULT_ATTRIBUTE_SELECTED, Relation.DATA_TYPE_BIT, null) && tTCoor
                        .insertColumn(0, Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID, Relation.DATA_TYPE_INT, null))) {
                    General.showError("Failed to insert one or more required columns");
                    return false;
                }

                // Select and set 'hasCoor' for all atoms in bulk
                BitSet selected = tTCoor.getColumnBit(Relation.DEFAULT_ATTRIBUTE_SELECTED); // all
                // false
                // at
                // this
                // point
                selected.or(tTCoor.used);
                BitSet hasCoor = tTCoor.getColumnBit(Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR);
                hasCoor.or(tTCoor.used);

                // Rename some.
                String[] equivalents = {
                        Relation.DEFAULT_ATTRIBUTE_NAME,
                        Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME,
                        Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID,
                        Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME,
                        Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME,
                        Gumbo.DEFAULT_ATTRIBUTE_COOR_X,
                        Gumbo.DEFAULT_ATTRIBUTE_COOR_Y,
                        Gumbo.DEFAULT_ATTRIBUTE_COOR_Z,
                        Gumbo.DEFAULT_ATTRIBUTE_BFACTOR,
                        Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY
                };
                String[] optionalEquivalents = {
                        Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME,
                        Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID,
                        Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME,
                        Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME
                };
                StringArrayList optionalEquivalentList = new StringArrayList(optionalEquivalents);
                for (int n = 0; n < equivalents.length; n++) {
                    String equivalent = equivalents[n];
                    String columnName = (String) ((ArrayList) starDict.toStar2D.get("atom_main", equivalent))
                            .get(StarDictionary.POSITION_STAR_TAG_NAME);
                    if (columnName == null) {
                        General.showCodeBug("Failed to find definition for Wattos column: " + equivalent
                                + "in dictionary.");
                        return false;
                    }
                    boolean hasColumnName = tTCoor.hasColumn(columnName);
//                    General.showDebug("In File31.java hasColumnName: " + columnName + " is: " + hasColumnName);
                    if ( hasColumnName ) {
                        if (!tTCoor.renameColumn(columnName, equivalent)) {
                            General.showError("In File31.java failed to rename column: " + columnName + " to: " + equivalent
                                    + ". Is it present?");
                            return false;
                        }
                    } else { // was it an optionally present column?
//                        boolean isOptional = equivalent.equals(Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME);
                        boolean isOptional = optionalEquivalentList.contains(equivalent);
//                        General.showDebug("In File31.java isOptional: " + columnName + " is: " + isOptional);
                        if ( isOptional ) {
                            int dataType = Relation.DATA_TYPE_STRINGNR;
                            if (!tTCoor.insertColumn(equivalent, dataType, null)) {
                                General.showError("In File31.java failed to insert NEW column (was optional): " + equivalent);
                                return false;
                            }
                        } else {
                            General.showError("In File31.java missing mandatory column: " + columnName + " to: " + equivalent
                                    + ". Is it present?");
                            return false;
                        }
                    }
                }
                /**
                 * Translate the string elements to int elements using an
                 * slightly optimized routine.
                 */
                int[] elementIds = (int[]) tTCoor.getColumn(Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID);
                String[] elementIdsTmp = (String[]) tTCoor.getColumn(tagNameAtomElementId);
                if (elementIdsTmp == null) {
                    General.showWarning("No element ids tag  present in input at with name: " + tagNameAtomElementId
                            + " ; setting all to null.");
                    tTCoor.setValueByColumn(Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID, new Integer(Defs.NULL_INT));
                } else {
                    // Only need to do for sizeRows; not maxRows because we are
                    // garanteed they're not all used.
                    if (!Chemistry.translateElementNameToIdInArrays(elementIdsTmp, elementIds, false, 0,
                            tTCoor.sizeRows)) {
                        General
                                .showError("Failed to translate all element names to ids in arrays; setting all to null. Shouldn't be fatal but it might.");
                        tTCoor.setValueByColumn(Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID, new Integer(Defs.NULL_INT));
                        return false;
                    }
                }
                tTCoor.setValueByColumn(Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR, Boolean.valueOf(true));

                /** Set fkcs */
                tTCoor.setValueByColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                        new Integer(currentEntryId));
                int[] atomModelIdList = (int[]) tTCoor.getColumn(tagNameAtomModelId);
                int[] atomMolIdList = (int[]) tTCoor.getColumn(tagNameAtomMolId1);
                int[] atomResIdList = (int[]) tTCoor.getColumn(tagNameAtomResId);
                int[] model_main_id = (int[]) tTCoor
                        .getColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME]);
                int[] mol_main_id = (int[]) tTCoor
                        .getColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME]);
                int[] res_main_id = (int[]) tTCoor
                        .getColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RelationSet.RELATION_ID_COLUMN_NAME]);
                if ((atomModelIdList == null) || (atomMolIdList == null) || (atomResIdList == null)
                        || (model_main_id == null) || (mol_main_id == null) || (res_main_id == null)) {
                    General.showWarning("Failed to get all the required columns; skipping the read of all atoms.");
                    sfCoor = null;
                }

                int modelNum;
                int molNum;
                int resNum;
                int atomCount = 0;
                try {
                    for (; atomCount < atomCountMax; atomCount++) { // This loop
                        // might
                        // want to
                        // be
                        // optimized
                        modelNum = atomModelIdList[atomCount];
                        molNum = atomMolIdList[atomCount];
                        resNum = atomResIdList[atomCount];
                        model_main_id[atomCount] = rid_model[modelNum];
                        mol_main_id[atomCount] = rid_mol[modelNum][molNum];
                        res_main_id[atomCount] = rid_res[modelNum][molNum][resNum];
                    }
                } catch (Exception e) {
                    General.showThrowable(e);
                    General.showError("For atom: " + atomCount + " in the file, failed to set the RIDs");
                    General.showError("Model number: " + atomModelIdList[atomCount] + ", molecule number: "
                            + atomMolIdList[atomCount] + ", residue number: " + atomResIdList[atomCount]);
                    General
                            .showError("Is the atom in known topology. E.g. does the residue occur in the system description.");
                    return false;
                }

                ArrayList columnsToCopy = new ArrayList();
                columnsToCopy.addAll(Arrays.asList(equivalents));
                columnsToCopy.addAll(Arrays.asList(newbies));
                if (!atomMain.append(tTCoor, 0, atomCountMax, columnsToCopy, false)) {
                    General
                            .showCodeBug("Failed to append from modifed tag table in STAR file to Wattos atom main relation");
                    return false;
                }
            }

            // Sync the atoms over the models in the entry. Note that this will
            // remove unsynced atoms.
            if (syncModels) {
                if ((!entry.modelsSynced.get(currentEntryId)) && (!entry.syncModels(currentEntryId))) {
                    General
                            .showError("Failed to sync models after reading in the coordinates. Deleting the whole entry again.");
                    if (!entry.mainRelation.removeRowsCascading(currentEntryId, true)) {
                        General.showError("Failed to deleting the whole entry.");
                    }
                    return false;
                }
            } else {
                General.showWarning("Disabled syncing over models.");
                General.showWarning("This might lead to inconsistencies in Wattos internal data model.");
                General.showWarning("Needs testing for sure.");
                General
                        .showWarning("The code will definitely not work when reading in restraints on top of partily missing atoms.");
            }
            // Select all newly added atoms; code added later.
            BitSet orgAtoms = SQLSelect.selectBitSet(dbms, atom.mainRelation,
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                    SQLSelect.OPERATION_TYPE_EQUALS, new Integer(currentEntryId), false);
            atom.selected.or(orgAtoms);
        } // end of block on check doEntry

        if (!doRestraints) {
            return true;
        }
        // Make sure one or the other type of matching is selected but disallow
        // both and neither.
        if (matchRestraints2SoupByDefaultDetails && matchRestraints2SoupByAuthorDetails) {
            General.showError("Selecting both types of matching is not allowed.");
            return false;
        }
        if (!matchRestraints2SoupByDefaultDetails && !matchRestraints2SoupByAuthorDetails) {
            General.showWarning("Selecting no type of matching is not allowed.");
            return false;
        }

        currentEntryId = entry.getEntryId();
        if (currentEntryId < 0) {
            General.showError("Failed to get a single selected entry");
            if (!doEntry) {
                General.showError("Perhaps no entry was read before, it isn't read now.");
            }
            return false;
        }

        // Data for matching by regular tags.
        HashOfHashesOfHashes atomFirstRID = null;
        int[] molID2RID = null;
        if (matchRestraints2SoupByDefaultDetails || matchRestraints2SoupByAuthorDetails) {
            /**
             * The map has the following keys in order: mol number, res number,
             * atom name which ar key to an Integer representing the atom rid of
             * the first model.
             */
            atomFirstRID = (HashOfHashesOfHashes) entry.atomsHash[currentEntryId];
            if (atomFirstRID == null) {
                General.showError("Failed to get map of atoms");
                return false;
            }
            currentModelId = entry.getMasterModelId(currentEntryId);
            if (currentModelId < 0) {
                General.showError("Failed to get master model rid for entry rid: " + currentEntryId);
                return false;
            }
            // General.showDebug("atom map is: " + atomFirstRID);
            molID2RID = model.createMapMolId2Rid(currentModelId);
            // General.showDebug("mol id 2 rid map is: " +
            // PrimitiveArray.toString(molID2RID));
        }

        // XXX mark of DISTANCE CONSTRAINTS
        // probably too complicate of a code
        ArrayList sfDCList = db.getSaveFrameListByCategory("distance_constraints");
        if (sfDCList == null) {
            General.showDebug("No distance constraint list.");
        } else {
            Integer atomRID = null;
            /**
             * Rids into the specific tables. Values set are to start search,
             * will be reset later.
             */
            currentDCAtomId = 0;
            currentDCMembId = 0;
            currentDCViolId = 0;
            currentDCNodeId = 0;
            currentDCId = 0;
            currentDCListId = 0;

            int dcCountListTotal = sfDCList.size();
            General.showDebug("Found number of dc lists: " + dcCountListTotal);
            for (int dcCountList = 1; dcCountList <= dcCountListTotal; dcCountList++) {
                // General.showDebug( "Working on dc list: " + dcCountList);
                /** Collect unique unlinked atoms to show as a warning later on */
                HashOfHashesOfHashes unlinkedAtoms = new HashOfHashesOfHashes();
                // SaveFrame sFDC =
                // SaveFrame.selectSaveFrameByTagNameAndFreeValue( sfDCList,
                // tagNameDCID, new
                // Integer(dcCountList));
                SaveFrame sFDC = (SaveFrame) sfDCList.get(dcCountList - 1);
                if (sFDC == null) {
                    // General.showError(
                    // "Failed to select distance constraints sf by tag name and free value for column name:"
                    // +
                    // tagNameDCID + " and value: " + dcCountList);
                    General.showError("Failed to select distance constraints sf by order for dcCountList value: "
                            + dcCountList);
                    return false;
                }
                currentDCListId = dcList.mainRelation.getNextReservedRow(currentDCListId);
                // Check if the relation grew in size because not all relations
                // can be adaquately estimated.
                if (currentDCListId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                    currentDCListId = dcList.mainRelation.getNextReservedRow(0); // now
                    // it
                    // should
                    // be
                    // fine.
                }
                if (currentDCListId < 0) {
                    General.showCodeBug("Failed to add next dc list.");
                    return false;
                }
                // Set the dcList attributes.
                dcList.entry_id[currentDCListId] = currentEntryId;
                dcList.nameList[currentDCListId] = dcList.nameListNR.intern(sFDC.title);
                dcList.avgMethod[currentDCListId] = DistConstrList.DEFAULT_AVERAGING_METHOD;
                dcList.numberMonomers[currentDCListId] = DistConstrList.DEFAULT_AVERAGING_MONOMER_COUNT;
                dcList.selected.set(currentDCListId);

                General.showDebug("dc sf title is: " + dcList.nameList[currentDCListId] + " for dcList with RID: "
                        + currentDCListId);

                /** BEGIN BLOCK Convenience variables. File3.0 */
                if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                    General
                            .showWarning("Not all the necessary components in the distance constraint saveframe are present, skipping this one");
                    continue;
                }
                if (varDCType == null) {
                    dcList.type[currentDCListId] = DistConstrList.DEFAULT_TYPE_UNKNOWN;
                } else {
                    String dCtype = varDCType[0];
                    if (Defs.isNull(dCtype)) {
                        dcList.type[currentDCListId] = DistConstrList.DEFAULT_TYPE_UNKNOWN;
                    } else {
                        int idx = DistConstrList.DEFAULT_TYPE_ARRAYLIST.indexOf(dCtype);
                        if (idx < 0) {
                            dcList.type[currentDCListId] = DistConstrList.DEFAULT_TYPE_UNKNOWN;
                        } else {
                            dcList.type[currentDCListId] = idx;
                        }
                    }
                }

                // END BLOCK

                /**
                 * The following algorithm assumes that the rows in the 3 tables
                 * are sorted by constraint number, tree node, and member id. It
                 * will check but on violation of the assumption return false
                 * from this routine.
                 */
                int starDCTreeRId = 0; // RID into the 1st star loop with the
                // tree.
                int starDCAtomRid = 0; // RID into the 2nd star loop with the
                // atom.
                int starDCDistRId = 0; // RID into the 3rd star loop with the
                // dist.
                int dCNumb = 0;
                int dCMembNumb = 0;
                int dCNodeNumb = 0;

                int logicalOperationInt = Defs.NULL_INT;
                String logicalOperationString = null;
                Integer logicalOperationInteger = null;

                while (starDCTreeRId < tTTree.sizeRows) { // scan the whole tree
                    // tagtable.
//					General.showDebug("Working on dc tree rid: " + starDCTreeRId);
                    boolean atomFoundForAllInDC = true;
                    if (dCNumb != varDCtreeConstraintsID[starDCTreeRId]) { // start
                        // a
                        // new
                        // constraint;
                        // will
                        // be
                        // executed
                        // on first tree row.
                        if (dCNumb != (varDCtreeConstraintsID[starDCTreeRId] - 1)) {
                            General.showWarning("Jumped dc number with a number different from +1. Jumped from "
                                    + dCNumb + " to " + varDCtreeConstraintsID[starDCTreeRId]);
                        }
                        dCNumb++; // safe after test above and faster because no
                        // array lookup.
//						General.showDebug("Working on dc number: " + dCNumb);
                        currentDCId = dc.mainRelation.getNextReservedRow(currentDCId);
                        if (currentDCId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                            currentDCId = dc.mainRelation.getNextReservedRow(0); // now
                            // it
                            // should
                            // be
                            // fine.
                            if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                                return false;
                            }
                        }
                        if (currentDCId < 0) {
                            General.showCodeBug("Failed to get next reserved row in main distance constraint table.");
                            return false;
                        }
                        dc.number[currentDCId] = dCNumb;
                        dc.dcListIdMain[currentDCId] = currentDCListId;
                        dc.entryIdMain[currentDCId] = currentEntryId;
                        dc.selected.set(currentDCId);
                        dc.hasUnLinkedAtom.clear(currentDCId); // presume that
                        // all atoms can
                        // be linked to
                        // those in the
                        // definition. Actually already clear by default.
                        // Reset the other numbers.
                        dCMembNumb = 0;
                        dCNodeNumb = 0;
                    }
                    // Always create a new node per row in tree table.
                    currentDCNodeId = dc.distConstrNode.getNextReservedRow(currentDCNodeId);
                    if (currentDCNodeId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                        currentDCNodeId = dc.distConstrNode.getNextReservedRow(0); // now
                        // it
                        // should
                        // be
                        // fine.
                        if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                            return false;
                        }
                    }
                    if (currentDCNodeId < 0) {
                        General.showCodeBug("Failed to get next reserved row in node distance constraint table.");
                        return false;
                    }
                    if (dCNodeNumb != (varDCtreeNodeID[starDCTreeRId] - 1)) {
                        General.showError("Jumped dc node number with a number different from +1. Jumped from "
                                + dCNodeNumb + " to " + varDCtreeNodeID[starDCTreeRId]);
                        return false;
                    }
                    dCNodeNumb++; // safe after test above
//					General.showDebug("Working on dc node number: " + dCNodeNumb);
//					General.showDebug("Working on dc node rid   : " + currentDCNodeId);
                    if (logicalOperationString != varDCtreeLogicoperation[starDCTreeRId]) { // fast
                        // equality
                        // op
                        // possible
                        // because of nr list.
                        logicalOperationString = varDCtreeLogicoperation[starDCTreeRId];
                        logicalOperationInteger = (Integer) DistConstr.logicalOperationString2Int
                                .get(logicalOperationString);
                        if (logicalOperationInteger != null) {
                            logicalOperationInt = logicalOperationInteger.intValue();
                            // General.showDebug( "Found a logical operation: "
                            // +
                            // DistConstr.DEFAULT_LOGICAL_OPERATION_NAMES[
                            // logicalOperationInt ]);
                        } else {
                            logicalOperationInt = Defs.NULL_INT;
                            // General.showDebug(
                            // "Found no logical operation; int is: " +
                            // logicalOperationInt);
                        }
                    }
                    // General.showDebug( "**********for node rid: " +
                    // currentDCNodeId );
                    dc.dcMainIdNode[currentDCNodeId] = currentDCId;
                    dc.dcListIdNode[currentDCNodeId] = currentDCListId;
                    dc.entryIdNode[currentDCNodeId] = currentEntryId;
                    dc.nodeId[currentDCNodeId] = dCNodeNumb; // the numbers and
                    // not the node
                    // rids
                    dc.downId[currentDCNodeId] = varDCtreeDownnodeID[starDCTreeRId];
                    dc.rightId[currentDCNodeId] = varDCtreeRightnodeID[starDCTreeRId];
                    dc.logicalOp[currentDCNodeId] = logicalOperationInt;

                    // For each constraint node (those with distance etc but
                    // without logic) add the distance info from
                    // the distance star loop into the dc_node table and look
                    // for members and atoms.
                    if (Defs.isNull(logicalOperationInt)) {
                        // General.showDebug(
                        // "Dc node is a constraint node and not a logical node."
                        // );
                        if (starDCDistRId >= tTDist.sizeRows) {
                            General.showError("The starDCDistRid is equal or above the tTDist.sizeRows; meaning no distance row for constraint was found.");
                            return false;
                        }
                        // CHECKS on validity of ONE distance tT row
                        if (varDCvalueConstraintID[starDCDistRId] != dCNumb) {
                            General.showError("The varDCvalueConstraintID (" + varDCvalueConstraintID[starDCDistRId]
                                    + ") should be the number of the distance constraint: " + dCNumb);
                            return false;
                        }
                        if (varDCvalueTreenodeID[starDCDistRId] != dCNodeNumb) {
                            General.showError("The varDCvalueTreenodeID (" + varDCvalueTreenodeID[starDCDistRId]
                                    + ") should be the number of the node of the distance constraint: " + dCNodeNumb);
                            return false;
                        }

                        // General.showDebug(
                        // "Getting info from star dist rid: " + starDCDistRId);
                        dc.target[currentDCNodeId] = varDCvalueDistanceval[starDCDistRId];
                        dc.uppBound[currentDCNodeId] = varDCvalueDistanceupperboundval[starDCDistRId];
                        dc.lowBound[currentDCNodeId] = varDCvalueDistancelowerboundval[starDCDistRId];
                        dc.peakIdList[currentDCNodeId] = dc.peakIdListNR
                                .intern(varDCvalueSpectralpeakID[starDCDistRId]);
                        // dc.weight[ currentDCNodeId ] = varDCvalueWeight[
                        // starDCDistRId ];
                        starDCDistRId++;
//						General.showDebug("---- incremented rid on star dc dist loop to: " + starDCDistRId);

                        // For each constraint node scan for the members;
                        // usually there are two.
                        // Scan that part of the star distance loop applicable
                        // to this node.
                        // Early abort for not running out of bounds.
                        dCMembNumb = 0; // reset to one before the next expected
                        // one for each node when useful.
                        while (starDCAtomRid < tTAtom.sizeRows) {
                            // breaks exist too for:
                            // -1- when atom is in different member, node, or
                            // restraint.
                            /**
                             * Indicates if all atoms within a member where
                             * found
                             */
                            boolean atomFound = true;
                            // General.showDebug(
                            // "Getting info from star atom rid: " +
                            // starDCAtomRid);
                            // CHECKS on validity of ONE atom tT row
                            if (varDCConstraintsID[starDCAtomRid] != dCNumb) {
                                if (varDCConstraintsID[starDCAtomRid] != (dCNumb + 1)) {
                                    General
                                            .showError("The varDCDistconstrainttreeID ("
                                                    + varDCConstraintsID[starDCAtomRid]
                                                    + ") should be the number of the distance constraint or 1 higher than that: "
                                                    + dCNumb);
                                    return false;
                                }
                                break; // loop over starDCAtomRid
                            }
                            if (varDCTreenodemembernodeID[starDCAtomRid] != dCNodeNumb) {
                                if (varDCTreenodemembernodeID[starDCAtomRid] != (dCNodeNumb + 1)) {
                                    General
                                            .showError("The varDCTreenodemembernodeID ("
                                                    + varDCTreenodemembernodeID[starDCAtomRid]
                                                    + ") should be the number of the distance constraint node or 1 higher for simple pot. functions now in place but is: "
                                                    + dCNodeNumb);
                                    return false;
                                }
                                break; // loop over starDCAtomRid
                            }
                            if (varDCConstrainttreenodememberID[starDCAtomRid] != dCMembNumb) { // atom
                                // in
                                // same
                                // member
                                if (varDCConstrainttreenodememberID[starDCAtomRid] != (dCMembNumb + 1)) { // atom
                                    // in
                                    // next member
                                    General.showError("The varDCConstrainttreenodememberID ("
                                            + varDCConstrainttreenodememberID[starDCAtomRid]
                                            + ") should be the dCMembNumb(" + dCMembNumb + ") + 1 or the same.");
                                    return false;
                                }
                            }
                            // Now that starDCAtomRid is known well, set the
                            // contribution if available.
                            // if ( ! Defs.isNull(
                            // varDCContributionfractionalval[ starDCAtomRid ]
                            // )) {
                            // dc.contribution[ currentDCNodeId] =
                            // varDCContributionfractionalval[ starDCAtomRid ];
                            // //to
                            // a different relation.
                            // }
                            if (varDCConstrainttreenodememberID[starDCAtomRid] != dCMembNumb) {
                                dCMembNumb++; // safe after tests above.
//								General.showDebug("Getting info from star member number: " + dCMembNumb);
                                currentDCMembId = dc.distConstrMemb.getNextReservedRow(currentDCMembId);
                                if (currentDCMembId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                                    currentDCMembId = dc.distConstrMemb.getNextReservedRow(0); // now
                                    // it
                                    // should
                                    // be
                                    // fine.
                                    if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                                        return false;
                                    }
                                }
                                if (currentDCMembId < 0) {
                                    General
                                            .showCodeBug("Failed to get next reserved row in member distance constraint table.");
                                    return false;
                                }
                                dc.dcNodeIdMemb[currentDCMembId] = currentDCNodeId; // funny
                                // enough
                                // this
                                // table
                                // has
                                // only
                                // all-fkc columns
                                dc.dcMainIdMemb[currentDCMembId] = currentDCId;
                                dc.dcListIdMemb[currentDCMembId] = currentDCListId;
                                dc.entryIdMemb[currentDCMembId] = currentEntryId;
                                dc.numbMemb[currentDCMembId] = dCMembNumb;
                            }
                            // Now do the hard part in looking up the atom ids.
                            // Tie the atoms in the restraints to the atoms in
                            // the soup or leave them unlinked.
                            ArrayList equiAtomList = new ArrayList();
                            String atomName = varDCAtomID[starDCAtomRid];
                            equiAtomList.add(atomName);
                            HashMap res2AtomMap = null;
                            if (matchRestraints2SoupByDefaultDetails) {
                                // Convenience variables.
                                int molId = varDCEntityassemblyID[starDCAtomRid];
                                if (Defs.isNull(molId)) {
                                    General.showWarning("Skipping reading DC: " + dCNumb
                                            + ": Coulnd't find mol perhaps no info from FC?");
                                    atomFoundForAllInDC = false;
                                    starDCAtomRid++;
                                    continue;
                                }
                                int molRID = molID2RID[molId];
                                Integer molNumb = new Integer(mol.number[molRID]);
                                Integer resNumb = new Integer(varDCCompindexID[starDCAtomRid]);
                                res2AtomMap = (HashMap) atomFirstRID.get(molNumb, resNumb);
                                // When there are nulls for molNumb and/or
                                // resNumb the map returned will also be null
                                if (res2AtomMap == null) {
                                    General.showWarning("While reading DCs: Coulnd't find residue with number: "
                                            + resNumb + " in a molecule with number: " + molNumb
                                            + " when looking for atom with name: " + atomName);
                                    atomFoundForAllInDC = false;
                                    starDCAtomRid++;
                                    continue;
                                }
                                // General.showDebug("Looking for mol: " + molId
                                // + " res: " + resNumb + " atom: " +
                                // atomName);
                                atomRID = (Integer) res2AtomMap.get(atomName);
                                // General.showDebug("Looking for atom in res2AtomMap: "
                                // +
                                // Strings.toString(res2AtomMap));
                                if (atomRID == null) {
                                    // General.showDebug("Did not find atom in res2AtomMap; hopefully a pseudo"
                                    // );
                                    // Get the residue name from any atom in the
                                    // residue
                                    Set atomSet = res2AtomMap.keySet();
                                    if (atomSet == null || atomSet.isEmpty()) {
                                        General
                                                .showDetail("While reading DCs: Coulnd't find atom (empty residue) in the residue with number: "
                                                        + resNumb
                                                        + " in a molecule with number: "
                                                        + molNumb
                                                        + " for atom with name: " + atomName);
                                        atomFound = false;
                                    } else {
                                        Object[] tmpje = atomSet.toArray();
                                        int ridAnyAtomInSameRes = ((Integer) res2AtomMap.get(tmpje[0])).intValue(); // too
                                        // expensive of course
                                        String resName = res.nameList[atom.resId[ridAnyAtomInSameRes]];
                                        // General.showDebug("Using residue name for pseudo atom lookup: "
                                        // + resName);
                                        ArrayList list = (ArrayList) ui.wattosLib.pseudoLib.toAtoms.get(resName,
                                                atomName); // Query with key of
                                        // presumed pseudo
                                        // atom.
                                        if (list == null || list.size() < 1) {
                                            General.showDetail("While reading DCs: Coulnd't find atom (assumed a pseudo but apparently not) in residue: "
                                                            + resNumb
                                                            + " in mol: "
                                                            + molNumb
                                                            + " for atom: "
                                                            + atomName);
                                            atomFound = false;
                                        } else {
                                            equiAtomList = list;
                                        }
                                    }
                                }
                                if (!atomFound) {
                                    atomFoundForAllInDC = false;
                                    starDCAtomRid++;
                                    continue;
                                }
                                // TODO: finish FC replacement code here.
                            } else if (matchRestraints2SoupByAuthorDetails) {
                                // Convenience variables.
                                String atomNameAuthor = varDCAuthatomID[starDCAtomRid];
                                // General.showDebug("working on: atomName: [" +
                                // atomNameAuthor +"]");
                                int offsetSequence = 171 - 13 - 1; // only valid
                                // for 1brv;
                                // mmCIF
                                // starts at
                                // ASN 1 and
                                // VAL
                                // is 14; in restraints VAL is 171 TODO: fix
                                // code
                                // int molId = varDCEntityassemblyID[
                                // starDCAtomRid ];
                                int molId = 1; // valid for 1brv
                                int molRID = molID2RID[molId];
                                Integer molNumb = new Integer(mol.number[molRID]);
                                // Integer resNumb = new
                                // Integer(varDCCompindexID[ starDCAtomRid ] -
                                // offsetSequence);
                                String resNumbAuthStr = varDCAuthseqID[starDCAtomRid];
                                int resNumbAuthInt = Defs.NULL_INT;
                                try {
                                    resNumbAuthInt = Integer.parseInt(resNumbAuthStr);
                                } catch (Exception e) {
                                    General.showThrowable(e);
                                    General.showError("Failed to convert author residue number to an int: ["
                                            + resNumbAuthStr + "]");
                                    starDCAtomRid++;
                                    continue;
                                }
                                Integer resNumb = new Integer(resNumbAuthInt - offsetSequence);
                                // General.showDebug("molNumb: [" + molNumb +
                                // "]");
                                // General.showDebug("resNumb: [" + resNumb +
                                // "]");
                                res2AtomMap = (HashMap) atomFirstRID.get(molNumb, resNumb);
                                // When there are nulls for molNumb and/or
                                // resNumb the map returned will also be null
                                if (res2AtomMap == null) {
                                    General.showWarning("While reading DCs: Coulnd't find residue with number: "
                                            + resNumb + "" + " in a molecule with number: " + molNumb);
                                    atomFoundForAllInDC = false;
                                    starDCAtomRid++;
                                    continue;
                                }
                                String resName = null;
                                Set atomSet = res2AtomMap.keySet();
                                Object[] tmpje = null;
                                int ridAnyAtomInSameRes = Defs.NULL_INT;
                                if (atomSet == null || atomSet.isEmpty()) {
                                    General
                                            .showDetail("While reading DCs: Coulnd't find any atom (empty residue) in the residue with number: "
                                                    + resNumb + " in a molecule with number: " + molNumb);
                                    atomFound = false;
                                } else {
                                    tmpje = atomSet.toArray();
                                    ridAnyAtomInSameRes = ((Integer) res2AtomMap.get(tmpje[0])).intValue(); // too
                                    // expensive
                                    // of
                                    // course.
                                    // General.showDebug("Using residue name for pseudo atom lookup: "
                                    // + resName);
                                    resName = res.nameList[atom.resId[ridAnyAtomInSameRes]];
                                }
                                String atomNameAuthorTranslated = atomNameAuthor;
                                if (atomFound) {
                                    // Note that the res name of the main model
                                    // is used to derive the mapped name.
                                    atomNameAuthorTranslated = ui.wattosLib.atomMap.atomNameToIupac(
                                            AtomMap.NOMENCLATURE_ID_DISCOVER, // FIXME:
                                            atomNameAuthor, resName);
                                    if (atomNameAuthorTranslated == null) {
                                        atomNameAuthorTranslated = atomNameAuthor;
                                    }
                                }
                                equiAtomList.clear();
                                equiAtomList.add(atomNameAuthorTranslated);
                                General.showDebug("Looking for translated atom: [" + atomNameAuthorTranslated
                                        + "] in res2AtomMap: " + Strings.toString(res2AtomMap));
                                atomRID = (Integer) res2AtomMap.get(atomNameAuthorTranslated);
                                if (atomRID == null) {
                                    General.showDebug("Did not find atom in res2AtomMap; hopefully a pseudo");
                                    // Get the residue name from any atom in the
                                    // residue
                                    if (atomFound) {
                                        ArrayList list = (ArrayList) ui.wattosLib.pseudoLib.toAtoms.get(resName,
                                                atomNameAuthorTranslated); // Query
                                        // with
                                        // key
                                        // of
                                        // presumed
                                        // pseudo
                                        // atom.
                                        if (list == null || list.size() < 1) {
                                            General.showDetail("While reading DCs: Coulnd't find atom (assumed a pseudo but apparently not) in residue: "
                                                            + resNumb
                                                            + " in mol: "
                                                            + molNumb
                                                            + " for atom: "
                                                            + atomName);
                                            atomFound = false;
                                        } else {
                                            equiAtomList = list;
                                        }
                                    }
                                }
                                if (!atomFound) {
                                    atomFoundForAllInDC = false;
                                    starDCAtomRid++;
                                    continue;
                                }

                            }

                            // At this point we know the name(s) of the atom(s)
                            // to point to; let's find their rid(s).
                            // General.showDebug("Found atoms (for pseudo atom): "
                            // + equiAtomList);
                            for (int at_no = 0; at_no < equiAtomList.size(); at_no++) {
                                int atomRIDint = Defs.NULL_INT;
                                if (matchRestraints2SoupByDefaultDetails || matchRestraints2SoupByAuthorDetails) {
                                    String atomNameLocal = (String) equiAtomList.get(at_no);
                                    atomRID = (Integer) res2AtomMap.get(atomNameLocal);
                                    if (atomRID == null || Defs.isNull(atomRID.intValue()) || atomRID.intValue() < 0) {
                                        General
                                                .showDetail("Coulnd't find atom (rid for atom name that is normally present) in the residue for atom which is part of a pseudo atom.");
                                        General.showDetail("For atom record: " + tTAtom.toStringRow(starDCAtomRid));
                                        atomFound = false;
                                        // break; // This break is no longer
                                        // allowed
                                    } else {
                                        atomRIDint = atomRID.intValue();
                                    }
                                } else {
                                    atomFound = false;
                                }
                                // Each line in the atom tT is at least 1 row in
                                // the dc_atom table.
                                currentDCAtomId = dc.distConstrAtom.getNextReservedRow(currentDCAtomId);
                                if (currentDCAtomId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                                    currentDCAtomId = dc.distConstrAtom.getNextReservedRow(0); // now
                                    // it
                                    // should
                                    // be
                                    // fine.
                                    if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                                        return false;
                                    }
                                }
                                if (currentDCAtomId < 0) {
                                    General
                                            .showCodeBug("Failed to get next reserved row in atom distance constraint table.");
                                    return false;
                                }
                                // General.showDebug(
                                // "filling info into dc atom rid: " +
                                // currentDCAtomId);
                                dc.dcMembIdAtom[currentDCAtomId] = currentDCMembId;
                                dc.dcNodeIdAtom[currentDCAtomId] = currentDCNodeId;
                                dc.dcMainIdAtom[currentDCAtomId] = currentDCId;
                                dc.dcListIdAtom[currentDCAtomId] = currentDCListId;
                                dc.entryIdAtom[currentDCAtomId] = currentEntryId;

                                dc.authMolNameList[currentDCAtomId] = dc.authMolNameListNR
                                        .intern(varDCAuthsegmentcode[starDCAtomRid]);
                                dc.authResIdList[currentDCAtomId] = dc.authResIdListNR
                                        .intern(varDCAuthseqID[starDCAtomRid]);
                                dc.authResNameList[currentDCAtomId] = dc.authResNameListNR
                                        .intern(varDCAuthcompID[starDCAtomRid]);
                                dc.authAtomNameList[currentDCAtomId] = dc.authAtomNameListNR
                                        .intern(varDCAuthatomID[starDCAtomRid]);
                                dc.atomIdAtom[currentDCAtomId] = atomRIDint;
                                // if ( Defs.isNull( atomRIDint ) ) { // store
                                // the original values in stead of the
                                // reference.
                                // dc.molIdList[ currentDCAtomId ] =
                                // varDCLabelentityassemblyID[ starDCAtomRid];
                                // dc.entityIdList[ currentDCAtomId ] =
                                // varDCLabelentityID[ starDCAtomRid];
                                // dc.resIdList[ currentDCAtomId ] =
                                // varDCLabelcompindexID[ starDCAtomRid];
                                // dc.resNameList[ currentDCAtomId ] =
                                // dc.resNameListNR.intern(varDCLabelcompID[
                                // starDCAtomRid] );
                                // dc.atomNameList[ currentDCAtomId ] =
                                // dc.atomNameListNR.intern(varDCLabelatomID[
                                // starDCAtomRid] );
                                // /** Store the atom ids for reporting later on
                                // */
                                // Integer unlinkedAtomCount = (Integer)
                                // unlinkedAtoms.get(
                                // new Integer( varDCLabelentityID[
                                // starDCAtomRid]),
                                // new Integer( varDCLabelcompindexID[
                                // starDCAtomRid]),
                                // dc.atomNameList[ currentDCAtomId ]);
                                // Integer unlinkedAtomCountNew = new Integer( 1
                                // );
                                // if ( unlinkedAtomCount != null ) {
                                // unlinkedAtomCountNew = new Integer(
                                // unlinkedAtomCount.intValue() + 1 );
                                // }
                                // unlinkedAtoms.put(new Integer(
                                // varDCLabelentityID[ starDCAtomRid]),
                                // new Integer( varDCLabelcompindexID[
                                // starDCAtomRid]),
                                // dc.atomNameList[ currentDCAtomId ],
                                // unlinkedAtomCountNew);
                                // } else {
                                dc.molIdList[currentDCAtomId] = Defs.NULL_INT;
                                dc.entityIdList[currentDCAtomId] = Defs.NULL_INT;
                                dc.resIdList[currentDCAtomId] = Defs.NULL_INT;
                                dc.resNameList[currentDCAtomId] = Defs.NULL_STRING_NULL;
                                dc.atomNameList[currentDCAtomId] = Defs.NULL_STRING_NULL;
                                // }
                            } // End of loop per atom in atom(group)
                            if (!atomFound) {
                                atomFoundForAllInDC = false;
                            }
                            starDCAtomRid++;
                        } // End of loop for rows in atom tagtable
                        if (!atomFoundForAllInDC) {
                            dc.hasUnLinkedAtom.set(currentDCId); // set the bit
                            // for this
                            // constraint
                            // at least
                            // once.
                        }
                    } // End of loop for constraint nodes.
                    starDCTreeRId++;
                }// End of loop for distance constraints.
                /** Report unique unlinked atoms per list. */
                if (unlinkedAtoms.cardinality() > 0) {
                    General.showWarning("The number of different atoms in the distance constraint list(" + dcCountList
                            + ") not recognized is: " + unlinkedAtoms.cardinality());
                    General
                            .showWarning("1st is molecule,2nd is residue,3rd is atom name,4th is the number of times it failed to be matched.");
                    General.showWarning(unlinkedAtoms.toString());
                }
            }// End of loop for dc lists.

            boolean status_1 = dcList.mainRelation.cancelAllReservedRows();
            boolean status_2 = dc.mainRelation.cancelAllReservedRows();
            boolean status_3 = dc.distConstrAtom.cancelAllReservedRows();
            boolean status_4 = dc.distConstrMemb.cancelAllReservedRows();
            boolean status_5 = dc.distConstrNode.cancelAllReservedRows();
            boolean status_6 = dc.distConstrViol.cancelAllReservedRows();
            boolean status_overall = status && status_1 && status_2 && status_3 && status_4 && status_5 && status_6;
            if (!status_overall) {
                General
                        .showError("Failed to cancel all reserved rows in the distance constraint tables that weren't needed.");
                General.showWarning("Removing all entities under entry with name: " + entry.nameList[currentEntryId]);
                entryMain.removeRowCascading(currentEntryId, true);
                return false;
            }
            if (removeUnlinkedRestraints && dc.hasUnLinkedAtom.cardinality() > 0) {
                General.showOutput("Removing " + dc.hasUnLinkedAtom.cardinality()
                        + " distance restraint(s) with unlinked atom(s).");
                if (!dc.mainRelation.removeRowsCascading(dc.hasUnLinkedAtom, false)) {
                    General.showError("Failed to remove unlinked atoms");
                    return false;
                }
            }
        } // end of block for presence of dc.

        // XXX mark of DIHEDRAL CONSTRAINTS
        // Note that as much as possible is specified in terms of Cdih's
        // superclass SimpleConstr
        // so that copying this code to rdcs etc. will be easy.
        ArrayList sfSCList = db.getSaveFrameListByCategory("torsion_angle_constraints");
        if (sfSCList == null) {
            General.showDebug("No simple constraint list (CDIH).");
        } else {
            Integer atomRID = null;
            // Rids into the specific tables. Values set are to start search,
            // will be reset later.
            currentSCAtomId = 0;
            currentSCId = 0;
            currentSCListId = 0;
            SimpleConstrList scList = cdihList;
            SimpleConstr sc = cdih;
            int scCountListTotal = sfSCList.size();
            General.showDebug("Found number of sc lists: " + scCountListTotal);
            for (int scCountList = 1; scCountList <= scCountListTotal; scCountList++) {
                // General.showDebug( "Working on sc list: " + scCountList);
                // Collect unique unlinked atoms to show as a warning later on
                HashOfHashesOfHashes unlinkedAtoms = new HashOfHashesOfHashes();
                // SaveFrame sFSC =
                // SaveFrame.selectSaveFrameByTagNameAndFreeValue( sfSCList,
                // tagNameCDIH_ID1, new
                // Integer(scCountList));
                SaveFrame sFSC = (SaveFrame) sfSCList.get(scCountList - 1);
                if (sFSC == null) {
                    // General.showError("Failed to select constraints sf by tag name and free value for column name:"
                    // +
                    // tagNameCDIH_ID1 + " and value: " + scCountList);
                    General.showError("Failed to select constraints sf by order for scCountList value: " + scCountList);
                    return false;
                }
                currentSCListId = scList.mainRelation.getNextReservedRow(currentSCListId);
                // Check if the relation grew in size because not all relations
                // can be adaquately estimated.
                if (currentSCListId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                    currentSCListId = scList.mainRelation.getNextReservedRow(0); // now
                    // it
                    // should
                    // be
                    // fine.
                }
                if (currentSCListId < 0) {
                    General.showCodeBug("Failed to add next sc list.");
                    return false;
                }
                // Set the scList attributes.
                scList.entry_id[currentSCListId] = currentEntryId;
                scList.nameList[currentSCListId] = scList.nameListNR.intern(sFSC.title);
                scList.selected.set(currentSCListId);

                General.showDebug("CDIH sf title is: " + scList.nameList[currentSCListId] + " for scList with RID: "
                        + currentSCListId);

                if (!initConvenienceVariablesStarWattosCDIH(sFSC)) {
                    General
                            .showWarning("Not all the necessary components in the constraint saveframe are present, skipping this one");
                    continue;
                }

                /**
                 * Scan all the tagtables for the column with the distance list
                 * ids and make sure that all are the same as assumed. This can
                 * be done more efficiently by a scan than by a sort.
                 * Optimalization todo if needed.
                 */
                // if (! tTIntro.areAllElementsOfIntValue(tagNameCDIH_ID1,
                // scCountList ) ||
                // ! tTMain.areAllElementsOfIntValue(
                // tagNameCDIH_Constraints_ID, scCountList ) ) {
                // General.showError("scCountList id should have been: " +
                // scCountList +
                // " but was found to be different in at least one of the tables on at least one of the rows");
                // return false;
                // }
                int starSCMainId = 0; // RID into the main star loop
                while (starSCMainId < tTMain.sizeRows) { // scan the whole
                    // tagtable.
                    boolean atomFoundForAllInSC = true;
                    // General.showDebug( "Working on sc main rid: " +
                    // starSCMainId);
                    currentSCId = sc.mainRelation.getNextReservedRow(currentSCId);
                    if (currentSCId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                        currentSCId = sc.mainRelation.getNextReservedRow(0); // now
                        // it
                        // should
                        // be
                        // fine.
                        if (!initConvenienceVariablesStarWattosCDIH(sFSC)) {
                            return false;
                        }
                    }
                    if (currentSCId < 0) {
                        General.showCodeBug("Failed to get next reserved row in main distance constraint table.");
                        return false;
                    }
                    sc.number[currentSCId] = starSCMainId + 1;
                    sc.scListIdMain[currentSCId] = currentSCListId;
                    sc.entryIdMain[currentSCId] = currentEntryId;
                    sc.selected.set(currentSCId);
                    sc.hasUnLinkedAtom.clear(currentSCId); // presume that all
                    // atoms can be
                    // linked to those
                    // in the
                    // definition. Actually already clear by default.
                    String angleName = tTMain.getValueString(starSCMainId, tagNameCDIH_Torsion_angle_name);
                    if (!Defs.isNull(angleName)) {
                        sc.nameList[currentSCId] = sc.nameListNR.intern(angleName);
                    }

                    // General.showDebug( "Getting info from star dist rid: " +
                    // starCDIHDistRId);
                    sc.target[currentSCId] = Defs.NULL_FLOAT;
                    // Store angles in radians in Wattos.
                    sc.uppBound[currentSCId] = varCDIH_Angle_upper_bound_val[starSCMainId] / Geometry.fCF;
                    sc.lowBound[currentSCId] = varCDIH_Angle_lower_bound_val[starSCMainId] / Geometry.fCF;
                    // General.showDebug("Read upper bound: " +
                    // sc.uppBound[currentSCId] + "(in degrees: "
                    // + Math.toDegrees(sc.uppBound[currentSCId]));
                    // Now do the hard part in looking up the atom ids.
                    // Tie the atoms in the restraints to the atoms in the soup
                    // or leave them unlinked.
                    Object[][] varAtomXLol = new Object[][] { // needs to match
                            // order in e.g.
                            // LOC_ENTITY_ASSEMBLY_ID
                            { varCDIH_Label_entity_assembly_ID_1, varCDIH_Label_entity_ID_1,
                                    varCDIH_Label_comp_index_ID_1, varCDIH_Label_comp_ID_1, varCDIH_Label_atom_ID_1 },
                            { varCDIH_Label_entity_assembly_ID_2, varCDIH_Label_entity_ID_2,
                                    varCDIH_Label_comp_index_ID_2, varCDIH_Label_comp_ID_2, varCDIH_Label_atom_ID_2 },
                            { varCDIH_Label_entity_assembly_ID_3, varCDIH_Label_entity_ID_3,
                                    varCDIH_Label_comp_index_ID_3, varCDIH_Label_comp_ID_3, varCDIH_Label_atom_ID_3 },
                            { varCDIH_Label_entity_assembly_ID_4, varCDIH_Label_entity_ID_4,
                                    varCDIH_Label_comp_index_ID_4, varCDIH_Label_comp_ID_4, varCDIH_Label_atom_ID_4 } };
                    Object[][] varAtomXLolAuthor = new Object[][] { // needs to
                            // match
                            // order in
                            // e.g.
                            // LOC_ENTITY_ASSEMBLY_ID
                            { varCDIH_Auth_segment_code_1, varCDIH_Auth_seq_ID_1, varCDIH_Auth_comp_ID_1,
                                    varCDIH_Auth_atom_ID_1 },
                            { varCDIH_Auth_segment_code_2, varCDIH_Auth_seq_ID_2, varCDIH_Auth_comp_ID_2,
                                    varCDIH_Auth_atom_ID_2 },
                            { varCDIH_Auth_segment_code_3, varCDIH_Auth_seq_ID_3, varCDIH_Auth_comp_ID_3,
                                    varCDIH_Auth_atom_ID_3 },
                            { varCDIH_Auth_segment_code_4, varCDIH_Auth_seq_ID_4, varCDIH_Auth_comp_ID_4,
                                    varCDIH_Auth_atom_ID_4 } };

                    for (int atomId = 0; atomId < varAtomXLol.length; atomId++) {
                        int[] var_Label_entity_assembly_ID_x = (int[]) varAtomXLol[atomId][LOC_ENTITY_ASSEMBLY_ID];
                        // int[] var_Label_entity_ID_x = (int[])
                        // varAtomXLol[atomId] [LOC_ENTITY_ID];
                        int[] var_Label_comp_index_ID_x = (int[]) varAtomXLol[atomId][LOC_COMP_INDEX_ID];
                        // String[] var_Label_comp_ID_x = (String[])
                        // varAtomXLol[atomId][LOC_COMP_ID];
                        String[] var_Label_atom_ID_x = (String[]) varAtomXLol[atomId][LOC_ATOM_ID];

                        String[] var_StarAuth_segment_code_x = (String[]) varAtomXLolAuthor[atomId][LOC_AUTHOR_SEGMENT_CODE];
                        String[] var_StarAuth_seq_ID_x = (String[]) varAtomXLolAuthor[atomId][LOC_AUTHOR_SEQ_ID];
                        String[] var_StarAuth_comp_ID_x = (String[]) varAtomXLolAuthor[atomId][LOC_AUTHOR_COMP_ID];
                        String[] var_StarAuth_atom_ID_x = (String[]) varAtomXLolAuthor[atomId][LOC_AUTHOR_ATOM_ID];

                        // General.showDebug( "Getting info from star atom: " +
                        // atomId);
                        String atomName = var_Label_atom_ID_x[starSCMainId];
                        // Convenience variables.
                        int molId = var_Label_entity_assembly_ID_x[starSCMainId];
                        int molRID = molID2RID[molId];
                        Integer molNumb = new Integer(mol.number[molRID]);
                        Integer resNumb = new Integer(var_Label_comp_index_ID_x[starSCMainId]);
                        HashMap res2AtomMap = (HashMap) atomFirstRID.get(molNumb, resNumb);
                        // When there are nulls for molNumb and/or resNumb the
                        // map returned will also be null
                        if (res2AtomMap == null) {
                            General.showWarning("While reading SCs: Coulnd't find residue with number: " + resNumb
                                    + " in a molecule with number: " + molNumb + " when looking for atom with name: "
                                    + atomName);
                            atomFoundForAllInSC = false;
                            break; // loop over atoms
                        }
                        // General.showDebug("Looking for mol: " + molId +
                        // " res: " + resNumb + " atom: " + atomName);
                        atomRID = (Integer) res2AtomMap.get(atomName);
                        // General.showDebug("Looking for atom in res2AtomMap: "
                        // + Strings.toString(res2AtomMap));
                        if (atomRID == null) {
                            General.showWarning("While reading SCs: Did not find atom in res2AtomMap");
                            atomFoundForAllInSC = false;
                            break; // loop over atoms
                        }
                        int atomRIDint = atomRID.intValue();
                        // int resRIDint = gumbo.atom.resId[atomRIDint];

                        // Each line in the atom tT is at least 1 row in the
                        // sc_atom table.
                        currentSCAtomId = sc.simpleConstrAtom.getNextReservedRow(currentSCAtomId);
                        if (currentSCAtomId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                            currentSCAtomId = sc.simpleConstrAtom.getNextReservedRow(0); // now
                            // it
                            // should
                            // be
                            // fine.
                            if (!initConvenienceVariablesStarWattosCDIH(sFSC)) {
                                General.showCodeBug("Failed to initConvenienceVariablesStarWattosCDIH");
                                return false;
                            }
                        }
                        if (currentSCAtomId < 0) {
                            General.showCodeBug("Failed to get next reserved row in atom distance constraint table.");
                            return false;
                        }

                        // General.showDebug( "filling info into sc atom rid: "
                        // + currentSCAtomId);
                        sc.scListIdAtom[currentSCAtomId] = currentSCListId;
                        sc.scIdAtom[currentSCAtomId] = currentSCId;
                        sc.entryIdAtom[currentSCAtomId] = currentEntryId;
                        if (var_StarAuth_segment_code_x == null) {
                            // sc.authMolNameList[ currentSCAtomId ] =
                            // sc.authMolNameListNR.intern(
                            // var_StarAuth_segment_code_x[ starSCMainId] );
                            // sc.authResIdList[ currentSCAtomId ] =
                            // sc.authResIdListNR.intern( var_StarAuth_seq_ID_x[
                            // starSCMainId] );
                            // sc.authResNameList[ currentSCAtomId ] =
                            // sc.authResNameListNR.intern(
                            // var_StarAuth_comp_ID_x[ starSCMainId] );
                            // sc.authAtomNameList[currentSCAtomId ] =
                            // sc.authAtomNameListNR.intern(var_StarAuth_atom_ID_x[
                            // starSCMainId] );
                        } else {
                            sc.authMolNameList[currentSCAtomId] = sc.authMolNameListNR
                                    .intern(var_StarAuth_segment_code_x[starSCMainId]);
                            sc.authResIdList[currentSCAtomId] = sc.authResIdListNR
                                    .intern(var_StarAuth_seq_ID_x[starSCMainId]);
                            sc.authResNameList[currentSCAtomId] = sc.authResNameListNR
                                    .intern(var_StarAuth_comp_ID_x[starSCMainId]);
                            sc.authAtomNameList[currentSCAtomId] = sc.authAtomNameListNR
                                    .intern(var_StarAuth_atom_ID_x[starSCMainId]);
                        }
                        sc.atomIdAtom[currentSCAtomId] = atomRIDint;
                        // // Next assignment assumed the atom was found.
                        // sc.atomNameList[ currentSCAtomId ] =
                        // gumbo.atom.nameList[atomRIDint];;
                        // sc.resIdList[ currentSCAtomId ] = gumbo.atom.resId[
                        // atomRIDint];
                        // sc.resNameList[ currentSCAtomId ] =
                        // gumbo.res.nameList[ resRIDint ];
                        // sc.molIdList[ currentSCAtomId ] = gumbo.atom.molId[
                        // atomRIDint];
                        // sc.entityIdList[ currentSCAtomId ] =
                        // gumbo.atom.molId[ atomRIDint]; // correct right?
                    } // End of loop over atoms
                    if (!atomFoundForAllInSC) {
                        sc.hasUnLinkedAtom.set(currentSCId); // set the bit for
                        // this
                        // constraint at
                        // least once.
                    }
                    starSCMainId++;
                }// End of loop for distance constraints.
                /** Report unique unlinked atoms per list. */
                if (unlinkedAtoms.cardinality() > 0) {
                    General.showWarning("The number of different atoms in the distance constraint list(" + scCountList
                            + ") not recognized is: " + unlinkedAtoms.cardinality());
                    General
                            .showWarning("1st is molecule,2nd is residue,3rd is atom name,4th is the number of times it failed to be matched.");
                    General.showWarning(unlinkedAtoms.toString());
                }
            }// End of loop for lists.
            if (!(status && scList.mainRelation.cancelAllReservedRows() && sc.mainRelation.cancelAllReservedRows() && sc.simpleConstrAtom
                    .cancelAllReservedRows())) {
                General.showError("Failed to cancel all reserved rows in the constraint tables that weren't needed.");
                General.showWarning("Removing all entities under entry with name: " + entry.nameList[currentEntryId]);
                entryMain.removeRowCascading(currentEntryId, true);
                return false;
            }
            if (removeUnlinkedRestraints && sc.hasUnLinkedAtom.cardinality() > 0) {
                General.showOutput("Removing " + sc.hasUnLinkedAtom.cardinality()
                        + " cdih restraint(s) with unlinked atom(s).");
                if (!sc.mainRelation.removeRowsCascading(sc.hasUnLinkedAtom, false)) {
                    General.showError("Failed to remove unlinked atoms");
                    return false;
                }
            }
        } // end of block for presence of sc.

        // XXX mark of RDC CONSTRAINTS
        // Note that as much as possible is specified in terms of Rdc's
        // superclass SimpleConstr
        // so that copying this code to rdcs etc. will be easy.
        sfSCList = db.getSaveFrameListByCategory("RDC_constraints");
        if (sfSCList == null) {
            General.showDebug("No simple constraint list (RDC).");
        } else {
            Integer atomRID = null;
            // Rids into the specific tables. Values set are to start search,
            // will be reset later.
            currentSCAtomId = 0;
            currentSCId = 0;
            currentSCListId = 0;
            SimpleConstrList scList = rdcList;
            SimpleConstr sc = rdc;
            int scCountListTotal = sfSCList.size();
            General.showDebug("Found number of sc lists (RDC): " + scCountListTotal);
            for (int scCountList = 1; scCountList <= scCountListTotal; scCountList++) {
                General.showDebug("Working on sc list: " + scCountList);
                // Collect unique unlinked atoms to show as a warning later on
                HashOfHashesOfHashes unlinkedAtoms = new HashOfHashesOfHashes();
                SaveFrame sFSC = (SaveFrame) sfSCList.get(scCountList - 1);
                if (sFSC == null) {
                    General.showError("Failed to select constraints sf by order for scCountList (RDC) value: "
                            + scCountList);
                    return false;
                }
                currentSCListId = scList.mainRelation.getNextReservedRow(currentSCListId);
                // Check if the relation grew in size because not all relations
                // can be adaquately estimated.
                if (currentSCListId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                    currentSCListId = scList.mainRelation.getNextReservedRow(0); // now
                    // it
                    // should
                    // be
                    // fine.
                }
                if (currentSCListId < 0) {
                    General.showCodeBug("Failed to add next sc list.");
                    return false;
                }
                // Set the scList attributes.
                scList.entry_id[currentSCListId] = currentEntryId;
                scList.nameList[currentSCListId] = scList.nameListNR.intern(sFSC.title);
                scList.selected.set(currentSCListId);

                General.showDebug("RDC sf title is: " + scList.nameList[currentSCListId] + " for scList with RID: "
                        + currentSCListId);

                if (!initConvenienceVariablesStarWattosRDC(sFSC)) {
                    General
                            .showWarning("Not all the necessary components in the constraint saveframe are present, skipping this one");
                    continue;
                }

                int starSCMainId = 0; // RID into the main star loop
                while (starSCMainId < tTMain.sizeRows) { // scan the whole
                    // tagtable.
                    boolean atomFoundForAllInSC = true;
                    // General.showDebug("Working on sc main rid: " +
                    // starSCMainId);
                    currentSCId = sc.mainRelation.getNextReservedRow(currentSCId);
                    if (currentSCId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                        currentSCId = sc.mainRelation.getNextReservedRow(0); // now
                        // it
                        // should
                        // be
                        // fine.
                        if (!initConvenienceVariablesStarWattosRDC(sFSC)) {
                            return false;
                        }
                    }
                    if (currentSCId < 0) {
                        General.showCodeBug("Failed to get next reserved row in main distance constraint table.");
                        return false;
                    }
                    sc.number[currentSCId] = starSCMainId + 1;
                    sc.scListIdMain[currentSCId] = currentSCListId;
                    sc.entryIdMain[currentSCId] = currentEntryId;
                    sc.selected.set(currentSCId);
                    sc.hasUnLinkedAtom.clear(currentSCId); // presume that all
                    // atoms can be
                    // linked to those
                    // in the
                    // definition. Actually already clear by default.

                    sc.target[currentSCId] = varRDC_val[starSCMainId];
                    sc.targetError[currentSCId] = varRDC_val_err[starSCMainId];
                    // General.showDebug("Size sc.targetError     " +
                    // sc.targetError.length);
                    // General.showDebug("Size sc.uppBound        " +
                    // sc.uppBound.length);
                    // General.showDebug("Size varRDC_val_err     " +
                    // varRDC_val_err.length);
                    // General.showDebug("Size varRDC_upper_bound " +
                    // varRDC_upper_bound.length);
                    sc.uppBound[currentSCId] = varRDC_upper_bound[starSCMainId];
                    sc.lowBound[currentSCId] = varRDC_lower_bound[starSCMainId];
                    // Now do the hard part in looking up the atom ids.
                    // Tie the atoms in the restraints to the atoms in the soup
                    // or leave them unlinked.
                    Object[][] varAtomXLol = new Object[][] { // needs to match
                            // order in e.g.
                            // LOC_ENTITY_ASSEMBLY_ID
                            { varRDC_Label_entity_assembly_ID_1, varRDC_Label_entity_ID_1,
                                    varRDC_Label_comp_index_ID_1, varRDC_Label_comp_ID_1, varRDC_Label_atom_ID_1 },
                            { varRDC_Label_entity_assembly_ID_2, varRDC_Label_entity_ID_2,
                                    varRDC_Label_comp_index_ID_2, varRDC_Label_comp_ID_2, varRDC_Label_atom_ID_2 } };
                    Object[][] varAtomXLolAuthor = new Object[][] { // needs to
                            // match
                            // order in
                            // e.g.
                            // LOC_ENTITY_ASSEMBLY_ID
                            { varRDC_Auth_segment_code_1, varRDC_Auth_seq_ID_1, varRDC_Auth_comp_ID_1,
                                    varRDC_Auth_atom_ID_1 },
                            { varRDC_Auth_segment_code_2, varRDC_Auth_seq_ID_2, varRDC_Auth_comp_ID_2,
                                    varRDC_Auth_atom_ID_2 } };

                    for (int atomId = 0; atomId < varAtomXLol.length; atomId++) {
                        int[] var_Label_entity_assembly_ID_x = (int[]) varAtomXLol[atomId][LOC_ENTITY_ASSEMBLY_ID];
                        int[] var_Label_comp_index_ID_x = (int[]) varAtomXLol[atomId][LOC_COMP_INDEX_ID];
                        String[] var_Label_atom_ID_x = (String[]) varAtomXLol[atomId][LOC_ATOM_ID];

                        String[] var_StarAuth_segment_code_x = (String[]) varAtomXLolAuthor[atomId][LOC_AUTHOR_SEGMENT_CODE];
                        String[] var_StarAuth_seq_ID_x = (String[]) varAtomXLolAuthor[atomId][LOC_AUTHOR_SEQ_ID];
                        String[] var_StarAuth_comp_ID_x = (String[]) varAtomXLolAuthor[atomId][LOC_AUTHOR_COMP_ID];
                        String[] var_StarAuth_atom_ID_x = (String[]) varAtomXLolAuthor[atomId][LOC_AUTHOR_ATOM_ID];

                        // General.showDebug( "Getting info from star atom: " +
                        // atomId);
                        String atomName = var_Label_atom_ID_x[starSCMainId];
                        // Convenience variables.
                        int molId = var_Label_entity_assembly_ID_x[starSCMainId];
                        int molRID = molID2RID[molId];
                        Integer molNumb = new Integer(mol.number[molRID]);
                        Integer resNumb = new Integer(var_Label_comp_index_ID_x[starSCMainId]);
                        HashMap res2AtomMap = (HashMap) atomFirstRID.get(molNumb, resNumb);
                        // When there are nulls for molNumb and/or resNumb the
                        // map returned will also be null
                        if (res2AtomMap == null) {
                            General.showWarning("While reading SCs2: Coulnd't find residue with number: " + resNumb
                                    + " in a molecule with number: " + molNumb + " when looking for atom with name: "
                                    + atomName);
                            atomFoundForAllInSC = false;
                            break; // loop over atoms
                        }
                        // General.showDebug("Looking for mol: " + molId +
                        // " res: " + resNumb + " atom: " + atomName);
                        atomRID = (Integer) res2AtomMap.get(atomName);
                        // General.showDebug("Looking for atom in res2AtomMap: "
                        // + Strings.toString(res2AtomMap));
                        if (atomRID == null) {
                            General.showWarning("While reading SCs2: Did not find atom in res2AtomMap");
                            atomFoundForAllInSC = false;
                            break; // loop over atoms
                        }
                        int atomRIDint = atomRID.intValue();
                        // int resRIDint = gumbo.atom.resId[atomRIDint];

                        // Each line in the atom tT is at least 1 row in the
                        // sc_atom table.
                        currentSCAtomId = sc.simpleConstrAtom.getNextReservedRow(currentSCAtomId);
                        if (currentSCAtomId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                            currentSCAtomId = sc.simpleConstrAtom.getNextReservedRow(0); // now
                            // it
                            // should
                            // be
                            // fine.
                            if (!initConvenienceVariablesStarWattosRDC(sFSC)) {
                                General.showCodeBug("Failed to initConvenienceVariablesStarWattosRDC");
                                return false;
                            }
                        }
                        if (currentSCAtomId < 0) {
                            General.showCodeBug("Failed to get next reserved row in atom distance constraint table.");
                            return false;
                        }

                        // General.showDebug( "filling info into sc atom rid: "
                        // + currentSCAtomId);
                        sc.scListIdAtom[currentSCAtomId] = currentSCListId;
                        sc.scIdAtom[currentSCAtomId] = currentSCId;
                        sc.entryIdAtom[currentSCAtomId] = currentEntryId;
                        sc.authMolNameList[currentSCAtomId] = sc.authMolNameListNR
                                .intern(var_StarAuth_segment_code_x[starSCMainId]);
                        sc.authResIdList[currentSCAtomId] = sc.authResIdListNR
                                .intern(var_StarAuth_seq_ID_x[starSCMainId]);
                        sc.authResNameList[currentSCAtomId] = sc.authResNameListNR
                                .intern(var_StarAuth_comp_ID_x[starSCMainId]);
                        sc.authAtomNameList[currentSCAtomId] = sc.authAtomNameListNR
                                .intern(var_StarAuth_atom_ID_x[starSCMainId]);
                        sc.atomIdAtom[currentSCAtomId] = atomRIDint;
                        // // Next assignment assumed the atom was found.
                        // sc.atomNameList[ currentSCAtomId ] =
                        // gumbo.atom.nameList[atomRIDint];;
                        // sc.resIdList[ currentSCAtomId ] = gumbo.atom.resId[
                        // atomRIDint];
                        // sc.resNameList[ currentSCAtomId ] =
                        // gumbo.res.nameList[ resRIDint ];
                        // sc.molIdList[ currentSCAtomId ] = gumbo.atom.molId[
                        // atomRIDint];
                        // sc.entityIdList[ currentSCAtomId ] =
                        // gumbo.atom.molId[ atomRIDint]; // correct right?
                    } // End of loop over atoms
                    if (!atomFoundForAllInSC) {
                        sc.hasUnLinkedAtom.set(currentSCId); // set the bit for
                        // this
                        // constraint at
                        // least once.
                    }
                    starSCMainId++;
                }// End of loop for distance constraints.
                /** Report unique unlinked atoms per list. */
                if (unlinkedAtoms.cardinality() > 0) {
                    General.showWarning("The number of different atoms in the distance constraint list(" + scCountList
                            + ") not recognized is: " + unlinkedAtoms.cardinality());
                    General
                            .showWarning("1st is molecule,2nd is residue,3rd is atom name,4th is the number of times it failed to be matched.");
                    General.showWarning(unlinkedAtoms.toString());
                }
            }// End of loop for lists.
            if (!(status && scList.mainRelation.cancelAllReservedRows() && sc.mainRelation.cancelAllReservedRows() && sc.simpleConstrAtom
                    .cancelAllReservedRows())) {
                General.showError("Failed to cancel all reserved rows in the constraint tables that weren't needed.");
                General.showWarning("Removing all entities under entry with name: " + entry.nameList[currentEntryId]);
                entryMain.removeRowCascading(currentEntryId, true);
                return false;
            }
            if (removeUnlinkedRestraints && sc.hasUnLinkedAtom.cardinality() > 0) {
                General.showOutput("Removing the " + sc.hasUnLinkedAtom.cardinality()
                        + " rdc restraint(s) with unlinked atom(s).");
                if (!sc.mainRelation.removeRowsCascading(sc.hasUnLinkedAtom, false)) {
                    General.showError("Failed to remove unlinked atoms");
                    return false;
                }
            }
        } // end of block for presence of sc.

        return true;
    } // end toWattos

    /**
     * remove any $ if present and replace _ by space
     *
     * @param molName
     * @return
     */
    private String convertEntityName2MolName(String molName) {
        molName = molName.replaceAll("[$]", ""); // only at start if present
        molName = molName.replaceAll("[_]", " ");
        return molName;
    }

    /**
     * Convert the data in Wattos in components in the gumbo etc. General
     * strategy is: -1- for each entry ask for a file name -2- copy all data in
     * DBMS (maybe be refined later on to do only certain tables) -3- remove all
     * data not selected (top-down and cascading) -4- create required star
     * tables from relations in DBMS -5- reformat values in star tag tables to
     * formatting as specified in star dictionary. -6- write star data node to
     * file.
     *
     *NOTES: selection determines what's written:
     *<UL>
     *<LI>molecule selection determines molecular system description section
     * (not model/residue). If no molecules are selected no Molecular System
     * Description (MSD) is written. If at least one is selected (even if it's
     * not in the same entry) the whole entry's MSD is written.
     *<LI>constraint selection determines constraints section (not constraint
     * list)
     *<LI>atom selection determines coordinates section
     *</UL>
     */
    public boolean toSTAR(String outputFileName, boolean usePostFixedOrdinalsAtomName) {
        // create star nodes
        DataBlock db = toSTARDataBlock(usePostFixedOrdinalsAtomName);
        if (db == null) {
            General.showError("Failed to get datablock in STAR in-memory tree for this entry;"
                    + "not attempting any other entries.");
            return false;
        }
        // db.general.setStarFlavor( StarGeneral.STANDARD_FLAVOR_NMRSTAR );
        if (!db.toSTAR(outputFileName)) { // same method name but different
            // instance class
            General.showError("Failed to write file for this entry; not attempting any other entries.");
            return false;
        }
        return true;
    }

    public SaveFrame getSFTemplateAssembly() {
        SaveFrame sF = new SaveFrame();
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // Tagtables

            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.

            namesAndTypes.put(tagNameEntrySFCategory, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameEntryName, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameAssemblyNumber_of_components, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameAssemblyOrganic_ligands, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameAssemblyMetal_ions, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameAssemblyParamagnetic, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameAssemblyThiol_state, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameAssemblyMolecular_mass, new Integer(Relation.DATA_TYPE_FLOAT));

            order.add(tagNameEntrySFCategory);
            order.add(tagNameEntryName);
            order.add(tagNameAssemblyNumber_of_components);
            order.add(tagNameAssemblyOrganic_ligands);
            order.add(tagNameAssemblyMetal_ions);
            order.add(tagNameAssemblyParamagnetic);
            order.add(tagNameAssemblyThiol_state);
            order.add(tagNameAssemblyMolecular_mass);

            // Append columns after order id column.
            namesAndValues.put(tagNameEntrySFCategory, "assembly");
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // ENTITIES
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            /**
             * _Entity_assembly.ID _Entity_assembly.Entity_assembly_name
             * _Entity_assembly.Entity_ID _Entity_assembly.Entity_label
             * _Entity_assembly.Asym_ID _Entity_assembly.Details
             * _Entity_assembly.Entry_ID _Entity_assembly.Assembly_ID 1
             * "MONOCYTE CHEMOATTRACTANT PROTEIN-3" 1
             * $MONOCYTE_CHEMOATTRACTANT_PROTEIN-3 . . 1 1
             */

            namesAndTypes.put(tagNameMolId, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameEntity_assemblyEntity_assembly_name, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameMolAssEntityId, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameMolAssEntityLabel, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameEntity_assemblyAsym_ID, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameEntity_assemblyDetails, new Integer(Relation.DATA_TYPE_STRING));

            order.add(tagNameMolId);
            order.add(tagNameEntity_assemblyEntity_assembly_name);
            order.add(tagNameMolAssEntityId);
            order.add(tagNameMolAssEntityLabel);
            order.add(tagNameEntity_assemblyAsym_ID);
            order.add(tagNameEntity_assemblyDetails);

            namesAndValues.put(tagNameMolId, new Integer(1));
            namesAndValues.put(tagNameMolAssEntityId, new Integer(1));

            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // Additional info mostly to get Water in. Maybe absent so nill it
            // if no rows.
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            /**
             * _PDBX_nonpoly_scheme.Entity_assembly_ID EntityAssemblyID INTEGER
             * no no yes _PDBX_nonpoly_scheme.Entity_ID EntityID INTEGER no no
             * no _PDBX_nonpoly_scheme.Mon_ID MonID CHAR(12) no no no
             * _PDBX_nonpoly_scheme.Comp_index_ID # should start at one so from
             * _pdbx_nonpoly_scheme.ndb_seq_num _PDBX_nonpoly_scheme.Comp_ID
             * _PDBX_nonpoly_scheme.Auth_seq_num
             * _pdbx_nonpoly_scheme.auth_seq_num
             */

            namesAndTypes.put(tagNamePDBX_nonpoly_schemeEntity_assembly_ID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNamePDBX_nonpoly_schemeEntity_ID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNamePDBX_nonpoly_schemeMon_ID, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNamePDBX_nonpoly_schemeComp_index_ID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNamePDBX_nonpoly_schemeComp_ID, new Integer(Relation.DATA_TYPE_STRING));
            // namesAndTypes.put( tagNamePDBX_nonpoly_schemeAuth_seq_num , new
            // Integer(Relation.DATA_TYPE_INT));

            order.add(tagNamePDBX_nonpoly_schemeEntity_assembly_ID);
            order.add(tagNamePDBX_nonpoly_schemeEntity_ID);
            order.add(tagNamePDBX_nonpoly_schemeMon_ID);
            order.add(tagNamePDBX_nonpoly_schemeComp_index_ID);
            order.add(tagNamePDBX_nonpoly_schemeComp_ID);
            // order.add(tagNamePDBX_nonpoly_schemeAuth_seq_num );

            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }

        return sF;
    }

    public SaveFrame getSFTemplateStudyList() {
        SaveFrame sF = new SaveFrame();
        sF.title = "Conversion_project";

        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // Tagtables

            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.

            namesAndTypes.put(tagNameStudy_listSf_category, new Integer(Relation.DATA_TYPE_STRING));
            order.add(tagNameStudy_listSf_category);
            namesAndValues.put(tagNameStudy_listSf_category, "study_list");

            // Append columns after order id column.
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // LOOP
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;

            namesAndTypes.put(tagNameStudyID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameStudyName, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameStudyType, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameStudyDetails, new Integer(Relation.DATA_TYPE_STRING));
            order.add(tagNameStudyID);
            order.add(tagNameStudyName);
            order.add(tagNameStudyType);
            order.add(tagNameStudyDetails);
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }

        return sF;
    }

    public SaveFrame getSFTemplateEntryInfo() {
        SaveFrame sF = new SaveFrame();
        sF.title = "originalConstraints";
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // Tagtables

            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            namesAndTypes.put(tagNameEntrySf_category, new Integer(Relation.DATA_TYPE_STRING));
            // namesAndTypes.put( tagNameEntryID , new
            // Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameEntryTitle, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameEntryNMR_STAR_version, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameEntryPDB_coordinate_file_version, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameEntryExperimental_method, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameEntryDetails, new Integer(Relation.DATA_TYPE_STRING));
            order.add(tagNameEntrySf_category);
            // order.add(tagNameEntryID );
            order.add(tagNameEntryTitle);
            order.add(tagNameEntryNMR_STAR_version);
            order.add(tagNameEntryPDB_coordinate_file_version);
            order.add(tagNameEntryExperimental_method);
            order.add(tagNameEntryDetails);
            namesAndValues.put(tagNameEntrySf_category, "entry_information");
            // "3.0.8.88"
            namesAndValues.put(tagNameEntryNMR_STAR_version, ui.wattosLib.validDictionary.NMR_STAR_version);
            namesAndValues.put(tagNameEntryExperimental_method, "NMR");
            // Append columns after order id column.
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }

        return sF;
    }

    public SaveFrame getSFTemplateNonStandardResidue(String resName) {
        SaveFrame sF = new SaveFrame();
        sF.title = resName;
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // Tagtables

            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            namesAndTypes.put(tagNameChem_compSFCategory, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameChem_compId, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameChem_compType, new Integer(Relation.DATA_TYPE_STRING));
            order.add(tagNameChem_compSFCategory);
            order.add(tagNameChem_compId);
            order.add(tagNameChem_compType);
            namesAndValues.put(tagNameChem_compSFCategory, "chem_comp");
            namesAndValues.put(tagNameChem_compId, resName);
            namesAndValues.put(tagNameChem_compType, "non-polymer");
            // Append columns after order id column.
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }
        return sF;
    }

    public SaveFrame getSFTemplateEntity() {
        SaveFrame sF = new SaveFrame();
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // Tagtables

            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            namesAndTypes.put(tagNameMolSFCategory, new Integer(Relation.DATA_TYPE_STRING));
            // namesAndTypes.put( tagNameMolEntityId, new
            // Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameMolType, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameMolName, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameMolPolType, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameMolSeqLength, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameMolSeq, new Integer(Relation.DATA_TYPE_STRING));
            order.add(tagNameMolSFCategory);
            // order.add(tagNameMolEntityId);
            order.add(tagNameMolType);
            order.add(tagNameMolName);
            order.add(tagNameMolPolType);
            order.add(tagNameMolSeqLength);
            order.add(tagNameMolSeq);
            namesAndValues.put(tagNameMolSFCategory, "entity");
            // namesAndValues.put( tagNameMolType, "polymer");
            // namesAndValues.put( tagNameMolPolType, "polypeptide(L)");
            // Append columns after order id column.
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // COMPONENTS (unsorted in principal)
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            // namesAndTypes.put( tagNameResEntityId, new
            // Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameResNum_1, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameResCompId, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameResCompLabel, new Integer(Relation.DATA_TYPE_STRING));
            // order.add(tagNameResEntityId);
            order.add(tagNameResNum_1);
            order.add(tagNameResCompId);
            order.add(tagNameResCompLabel);
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // SEQUENCE
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            namesAndTypes.put(tagNameResResId, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameResNum_2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameResName, new Integer(Relation.DATA_TYPE_STRING));
            order.add(tagNameResResId);
            order.add(tagNameResNum_2);
            order.add(tagNameResName);
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }

        return sF;
    }

    /**
     * Note that the large coor table hasn't been exercised/tested It is
     * generated from internal tables for speed reasons in toSTARDatablock()
     *
     * @return
     */
    public SaveFrame getSFTemplateCoor() {
        SaveFrame sF = new SaveFrame();
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // Tagtables

            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            namesAndTypes.put(tagNameAtomSFCategory, new Integer(Relation.DATA_TYPE_STRING));
            // namesAndTypes.put( tagNameAtomSFId_1, new
            // Integer(Relation.DATA_TYPE_STRING));
            order.add(tagNameAtomSFCategory);
            // order.add(tagNameAtomSFId_1);
            namesAndValues.put(tagNameAtomSFCategory, "conformer_family_coord_set");
            // namesAndValues.put( tagNameAtomSFId_1, "1");
            // Append columns after order id column.
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // COOR
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            // namesAndTypes.put( tagNameAtomSFId_2, new
            // Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameAtomModelId, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameAtomId, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameAtomMolId1, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameAtomMolId2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameAtomResId, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameAtomResName, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameAtomName, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameAtomAuthMolId, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameAtomAuthResName, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameAtomAuthName, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameAtomElementId, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameAtomPdbInsertionCode, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameAtomCoorX, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameAtomCoorY, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameAtomCoorZ, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameAtomBFactor, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameAtomOccupancy, new Integer(Relation.DATA_TYPE_FLOAT));
            order.add(tagNameAtomModelId);
            order.add(tagNameAtomId);
            order.add(tagNameAtomMolId1);
            order.add(tagNameAtomMolId2);
            order.add(tagNameAtomResId);
            order.add(tagNameAtomResName);
            order.add(tagNameAtomName);
            order.add(tagNameAtomAuthMolId);
            order.add(tagNameAtomAuthResName);
            order.add(tagNameAtomAuthName);
            order.add(tagNameAtomElementId);
            order.add(tagNameAtomPdbInsertionCode);
            order.add(tagNameAtomCoorX);
            order.add(tagNameAtomCoorY);
            order.add(tagNameAtomCoorZ);
            order.add(tagNameAtomBFactor);
            order.add(tagNameAtomOccupancy);
            // order.add(tagNameAtomSFId_2 ); // changed order for NMR-STAR v3.1
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }

        return sF;
    }

    public SaveFrame getSFTemplateDC() {
        SaveFrame sF = new SaveFrame();
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            namesAndTypes.put(tagNameDCSfcategory, new Integer(Relation.DATA_TYPE_STRING));
            // namesAndTypes.put( tagNameDCID, new
            // Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCMRfileblockposition, new Integer(Relation.DATA_TYPE_INT));
            // namesAndTypes.put( tagNameDCProgram, new
            // Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameDCType, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameDCConstraint_file_ID, new Integer(Relation.DATA_TYPE_INT));
            // namesAndTypes.put( tagNameDCSubtype, new
            // Integer(Relation.DATA_TYPE_STRING));
            // namesAndTypes.put( tagNameDCFormat, new
            // Integer(Relation.DATA_TYPE_STRING));
            order.add(tagNameDCSfcategory);
            // order.add(tagNameDCID);
            order.add(tagNameDCMRfileblockposition);
            // order.add(tagNameDCProgram);
            order.add(tagNameDCType);
            order.add(tagNameDCConstraint_file_ID);
            // order.add(tagNameDCSubtype);
            // order.add(tagNameDCFormat);
            namesAndValues.put(tagNameDCSfcategory, "distance_constraints");
            // namesAndValues.put( tagNameDCType, "distance");
            // Append columns after order id column.
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // TREE
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            namesAndTypes.put(tagNameDCtreeConstraintsID, new Integer(Relation.DATA_TYPE_INT));
            // namesAndTypes.put( tagNameDCtreeID, new
            // Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCtreeNodeID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCtreeDownnodeID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCtreeRightnodeID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCtreeLogicoperation, new Integer(Relation.DATA_TYPE_STRINGNR));
            order.add(tagNameDCtreeConstraintsID);
            // order.add(tagNameDCtreeID);
            order.add(tagNameDCtreeNodeID);
            order.add(tagNameDCtreeDownnodeID);
            order.add(tagNameDCtreeRightnodeID);
            order.add(tagNameDCtreeLogicoperation);
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // ATOM
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            namesAndTypes.put(tagNameDCConstraintsID, new Integer(Relation.DATA_TYPE_INT));
            // namesAndTypes.put( tagNameDCDistconstrainttreeID, new
            // Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCTreenodemembernodeID, new Integer(Relation.DATA_TYPE_INT));
            // namesAndTypes.put( tagNameDCContributionfractionalval, new
            // Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameDCConstrainttreenodememberID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCEntityassemblyID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCEntityID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCCompindexID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCCompID, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameDCAtomID, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameDCAuthsegmentcode, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameDCAuthseqID, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameDCAuthcompID, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameDCAuthatomID, new Integer(Relation.DATA_TYPE_STRINGNR));
            order.add(tagNameDCConstraintsID);
            // order.add(tagNameDCDistconstrainttreeID);
            order.add(tagNameDCTreenodemembernodeID);
            // order.add(tagNameDCContributionfractionalval);
            order.add(tagNameDCConstrainttreenodememberID);
            order.add(tagNameDCEntityassemblyID);
            order.add(tagNameDCEntityID);
            order.add(tagNameDCCompindexID);
            order.add(tagNameDCCompID);
            order.add(tagNameDCAtomID);
            order.add(tagNameDCAuthsegmentcode);
            order.add(tagNameDCAuthseqID);
            order.add(tagNameDCAuthcompID);
            order.add(tagNameDCAuthatomID);
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // VALUE
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            // namesAndTypes.put( tagNameDCvalueConstraintsID, new
            // Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCvalueConstraintID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCvalueTreenodeID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameDCvalueSourceexperimentID, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameDCvalueSpectralpeakID, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameDCvalueIntensityval, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameDCvalueIntensitylowervalerr, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameDCvalueIntensityuppervalerr, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameDCvalueDistanceval, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameDCvalueDistancelowerboundval, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameDCvalueDistanceupperboundval, new Integer(Relation.DATA_TYPE_FLOAT));
            // namesAndTypes.put( tagNameDCvalueWeight, new
            // Integer(Relation.DATA_TYPE_FLOAT));
            // namesAndTypes.put( tagNameDCvalueSpectralpeakppm1, new
            // Integer(Relation.DATA_TYPE_FLOAT));
            // namesAndTypes.put( tagNameDCvalueSpectralpeakppm2, new
            // Integer(Relation.DATA_TYPE_FLOAT));
            // order.add(tagNameDCvalueConstraintsID);
            order.add(tagNameDCvalueConstraintID);
            order.add(tagNameDCvalueTreenodeID);
            order.add(tagNameDCvalueSourceexperimentID);
            order.add(tagNameDCvalueSpectralpeakID);
            order.add(tagNameDCvalueIntensityval);
            order.add(tagNameDCvalueIntensitylowervalerr);
            order.add(tagNameDCvalueIntensityuppervalerr);
            order.add(tagNameDCvalueDistanceval);
            order.add(tagNameDCvalueDistancelowerboundval);
            order.add(tagNameDCvalueDistanceupperboundval);
            // order.add(tagNameDCvalueWeight);
            // order.add(tagNameDCvalueSpectralpeakppm1);
            // order.add(tagNameDCvalueSpectralpeakppm2);
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);
        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }

        // General.showDebug("DC SF is:[" + sF.toSTAR() + General.eol);
        return sF;
    }

    /**
     * This method does the actual work. Assumes that items in the DBMS are all
     * in one entry. It will take only the first entry (by physical address
     * ordering) if more than one is still present. STAR entities are created on
     * the basis of their sequence. If molecules have the same sequence they
     * will be combined and referred to as 1 entity. The instance of an entity
     * is called a molecule here.
     *
     *TODO: not loose the author names in case multiple atoms are specified on
     * input but Wattos would normally combine them and show the author info of
     * one of the input atoms.
     */
    private DataBlock toSTARDataBlock(boolean usePostFixedOrdinalsAtomName) {
        DataBlock db = new DataBlock();
        if (!usePostFixedOrdinalsAtomName) {
            atom.swapPostFixedOrdinalsAtomName(false);
        }

        // FIRST do the operations within Wattos data model
        if (!model.removeWithoutAtom()) {
            General.showError("Failed to remove model without coordinates.");
            return null;
        }
        // General.showDebug("Number of models after removing those without atoms referring to them: "
        // +
        // modelMain.used.cardinality());

        // ENTRY
        int entryRID = entry.selected.nextSetBit(0);
        if (entryRID < 0) {
            General.showCodeBug("Failed to find any entry to write again");
            return null;
        }
        String assemblyName = entryMain.getValueString(entryRID, Gumbo.DEFAULT_ATTRIBUTE_ASSEMBLY_NAME);
        String pdb_coordinate_file_version = entryMain.getValueString(entryRID, Gumbo.DEFAULT_ATTRIBUTE_PDB_COORDINATE_FILE_VERSION);

        String entryName = assemblyName;
        entryName = "1"; // TODO: delete this line after FC updates.
        int endIndex = 4;
        if (entryName.length() > endIndex) {
            entryName = entryName.substring(0, endIndex);
        }
        db.title = entryName;

        SaveFrame sFStudyList = getSFTemplateStudyList();
        if (sFStudyList == null) {
            General.showError("Failed to get a template saveframe for the study.");
            return null;
        }
        db.add(sFStudyList);
        sFStudyList.setTitle(sFStudyList.title + "_for_entry_Name_" + entryName);
        TagTable tTStudyLoop = (TagTable) sFStudyList.get(1);
        int studyRID = tTStudyLoop.getNewRowId();
        if (studyRID < 0) {
            General.showError("Failed to get a new row in the table for study.");
            return null;
        }
        tTStudyLoop.setValue(studyRID, tagNameStudyName, "Conversion project for entry " + entryName);
        tTStudyLoop.setValue(studyRID, tagNameStudyID, new Integer(1));
        tTStudyLoop.setValue(studyRID, tagNameStudyType, "NMR");
        tTStudyLoop.setValue(studyRID, Relation.DEFAULT_ATTRIBUTE_ORDER_ID, 0);

        SaveFrame sFEntryInfo = getSFTemplateEntryInfo();
        if (sFEntryInfo == null) {
            General.showError("Failed to get a template saveframe for the entry.");
            return null;
        }
        db.add(sFEntryInfo);
        sFEntryInfo.setTitle(sFEntryInfo.title + "_" + entryName);
        TagTable tTEntry = (TagTable) sFEntryInfo.get(0);
        tTEntry.setValue(0, tagNameEntryExperimental_method, "NMR");
        tTEntry.setValue(0, tagNameEntryTitle, "Data for entry " + entryName);
        tTEntry.setValue(0, tagNameEntryPDB_coordinate_file_version, pdb_coordinate_file_version);
        // tTEntry.setValue(0, tagNameEntryDetails,
        // "Contains restraints for entry " +entryName);

        // Molecular SYSTEM
        // MOLECULE
        int molRID = mol.selected.nextSetBit(0);
        boolean molecularSystemDescriptionToWrite = false;
        if (molRID < 0) {
            General.showWarning("No molecules selected so no molecular system description written.");
        } else {
            molecularSystemDescriptionToWrite = true;
        }
        // Go through the whole trouble anyway because molNumber2EntityNumberMap
        // needs to be populated
        // This can obviously be improved.

        // MODEL
        // Get model 1 from this entry regardless of it being selected.
        BitSet modelSet = SQLSelect.selectCombinationBitSet(dbms, model.mainRelation,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(entryRID), Relation.DEFAULT_ATTRIBUTE_NUMBER,
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(1), SQLSelect.OPERATOR_AND, false);
        if (modelSet == null) {
            General.showError("Failed to get set of models in selected entry to write again.");
            return null;
        }
        int modelRID = modelSet.nextSetBit(0); // The first model will determine
        // the description of the
        // molecular
        // system.
        if (modelRID < 0) {
            General.showError("Failed to find any molecular system (model) to write again");
            return null;
        }
        // int molCount = mol.selected.cardinality();
        // General.showDebug("Number of molecules in selection: " + molCount);
        // Get the molecule descriptions for first model in selection and assume
        // the others are the same.
        BitSet molSet = SQLSelect.selectBitSet(dbms, molMain,
                Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(modelRID), false);
        if (molSet == null) {
            General.showError("Failed to get set of molecules in first model to write again.");
            return null;
        }
        int molCount = molSet.cardinality();
        // General.showDebug("Number of molecules in first selected model: " +
        // molCount);
        if (molCount < 1) {
            General.showError("Failed to find any molecule in first selected model to write again");
            return null;
        }
        // RESIDUE
        BitSet allResSet = SQLSelect.selectBitSet(dbms, resMain,
                Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(modelRID), false);
        if (allResSet == null) {
            General.showError("Failed to get set of residues in first model to write again.");
            return null;
        }
        int resRID = allResSet.nextSetBit(0);
        if (resRID < 0) {
            General.showCodeBug("Failed to find any component (residue) to write again");
            return null;
        }
        // int resCount = allResSet.cardinality();

        // ASSEMBLY
        /**
         * _Assembly.Sf_category assembly _Assembly.ID 1 _Assembly.Name 1bo0
         * _Assembly.Entry_ID 1 _Assembly.Number_of_components 1
         * _Assembly.Organic_ligands 0 _Assembly.Metal_ions 0
         * _Assembly.Paramagnetic no _Assembly.Thiol_state "all free"
         * _Assembly.Molecular_mass 9373.9151
         */

        SaveFrame sFAssembly = getSFTemplateAssembly();
        if (sFAssembly == null) {
            General.showError("Failed to get a template saveframe for the assembly.");
            return null;
        }
        // Use assembly name twice.
        sFAssembly.setTitle("assembly_" + assemblyName);
        BitSet atomSet = mol.getAtoms(molSet);
        // General.showDebug("Looking at number of residues: " +
        // allResSet.cardinality());
        // General.showDebug("Looking at number of atoms   : " +
        // atomSet.cardinality());
        int thiolState = Biochemistry.THIOL_STATE_UNKNOWN;
        if (entry.getModelsSynced(entryRID)) {
            thiolState = atom.getThiolState(atomSet, allResSet);
            // General.showDebug("Found THIOL_STATE: " +
            // Biochemistry.thiolStateEnumerationAsInStar[thiolState]);
            if (thiolState < 0) {
                General.showCodeBug("Let's mention it's unknown and don't crash");
                thiolState = Biochemistry.THIOL_STATE_UNKNOWN;
            }
        } else {
//            General.showWarning("Not deducing thiol state of molecule since it was not synced");
        }
        TagTable tTAssemblyIntro = (TagTable) sFAssembly.get(0);
        tTAssemblyIntro.setValue(0, tagNameAssemblyNumber_of_components, molSet.cardinality());
//        tTAssemblyIntro.setValue(0, tagNameAssemblyOrganic_ligands, res.getOrganic_ligands(allResSet));
//        tTAssemblyIntro.setValue(0, tagNameAssemblyMetal_ions, atom.getMetalIons(atomSet));
        tTAssemblyIntro.setValue(0, tagNameAssemblyMolecular_mass, mol.getMass(molSet));
        tTAssemblyIntro.setValue(0, tagNameAssemblyParamagnetic, Defs.NULL_STRING_DOT);
        tTAssemblyIntro.setValue(0, tagNameAssemblyThiol_state, Biochemistry.thiolStateEnumerationAsInStar[thiolState]);
        tTAssemblyIntro.setValue(0, tagNameEntryName, assemblyName);
        TagTable tTAssemblyEntity = (TagTable) sFAssembly.get(1);
        // Add saveframe to datablock.
        // Next object maps molecule rid to entity number for molecules in first
        // model.
        int[] mol2EntityMap = createMol2EntityMap(molSet); // big array.
        // Create a map to be used in the coordinate table. It maps molecule
        // numbers to
        // entity numbers which if all models are the same this is correct.
        HashMap molNumber2EntityNumberMap = createMolNumber2EntityNumberMap(mol2EntityMap, molSet); // small
        // map
        // Next map is from entity number to entity name (which is the name of
        // the first molecule).
        HashMap entityNameMap = createEntityNameMap(mol2EntityMap, molSet);
        if (mol2EntityMap == null) {
            General.showError("Failed to create a map from the Wattos molecules to the STAR entities.");
            return null;
        }
        if (entityNameMap == null) {
            General.showError("Failed to create a map from the STAR entity numbers to names.");
            return null;
        }

        int counterEntityAssembly = 1;
        for (molRID = molSet.nextSetBit(0); molRID >= 0; molRID = molSet.nextSetBit(molRID + 1)) {
            int entityRID = tTAssemblyEntity.getNewRowId();
            if (entityRID < 0) {
                General.showError("Failed to get a new row in the table for assembly entities.");
                return null;
            }
            Integer entityNumber = new Integer(mol2EntityMap[molRID]);
            String molLabel = "$" + entityNameMap.get(entityNumber);
            // General.showDebug("Created molLabel: " + molLabel);
            tTAssemblyEntity.setValue(entityRID, tagNameMolId, new Integer(counterEntityAssembly));
            tTAssemblyEntity.setValue(entityRID, tagNameMolAssEntityId, entityNumber);
            tTAssemblyEntity.setValue(entityRID, tagNameMolAssEntityLabel, molLabel);
            // tTAssemblyEntity.setValue( entityRID,
            // tagNameEntity_assemblyAsym_ID, mol.asymId[entityRID]); # was a
            // bug?
            // entityRID should be molRID?
            tTAssemblyEntity.setValue(entityRID, tagNameEntity_assemblyAsym_ID, mol.asymId[molRID]);
            counterEntityAssembly++;
        }
        // Set all ids for assembly to 1 after rows are claimed.
        // tTAssemblyEntity.setValueByColumn(tagNameMolEntryId,new Integer(1));
        int shift = -1; // Order in source column starts at 1 whereas ordering
        // in target should start at 0
        if (!tTAssemblyEntity.copyToOrderColumn(tagNameMolId, shift)) { // will
            // take
            // care
            // of
            // required
            // shifting
            // too
            General.showError("Failed to set the order for the ordered entity instances in the assembly tag table.");
            return null;
        }

        // Set the ligand and water info.
        TagTable tTAssemblyPDBX_nonpoly_scheme = (TagTable) sFAssembly.get(2);
        counterEntityAssembly = 1;
        for (molRID = molSet.nextSetBit(0); molRID >= 0; molRID = molSet.nextSetBit(molRID + 1)) {
            if (mol.type[molRID] == Molecule.POLYMER_TYPE) {
                counterEntityAssembly++;
                continue;
            }
            Integer entityNumber = new Integer(mol2EntityMap[molRID]);
            BitSet resSet = SQLSelect.selectBitSet(dbms, res.mainRelation,
                    Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME],
                    SQLSelect.OPERATION_TYPE_EQUALS, new Integer(molRID), false);
            if (resSet == null) {
                General.showError("Failed to get a residue(s) for nonpoly molecule: " + mol.nameList[molRID]);
                return null;
            }
            for (resRID = resSet.nextSetBit(0); resRID >= 0; resRID = resSet.nextSetBit(resRID + 1)) {
                int entityRID = tTAssemblyPDBX_nonpoly_scheme.getNewRowId();
                if (entityRID < 0) {
                    General.showError("Failed to get a new row in the table for assembly entities.");
                    return null;
                }
                tTAssemblyPDBX_nonpoly_scheme.setValue(entityRID, Relation.DEFAULT_ATTRIBUTE_ORDER_ID, entityRID);
                tTAssemblyPDBX_nonpoly_scheme.setValue(entityRID, tagNamePDBX_nonpoly_schemeEntity_assembly_ID,
                        counterEntityAssembly);
                tTAssemblyPDBX_nonpoly_scheme.setValue(entityRID, tagNamePDBX_nonpoly_schemeEntity_ID, entityNumber);
                tTAssemblyPDBX_nonpoly_scheme.setValue(entityRID, tagNamePDBX_nonpoly_schemeMon_ID,
                        res.nameList[resRID]);
                tTAssemblyPDBX_nonpoly_scheme.setValue(entityRID, tagNamePDBX_nonpoly_schemeComp_index_ID,
                        res.number[resRID]);
                tTAssemblyPDBX_nonpoly_scheme.setValue(entityRID, tagNamePDBX_nonpoly_schemeComp_ID,
                        res.nameList[resRID]);
                // tTAssemblyPDBX_nonpoly_scheme.setValue( entityRID,
                // tagNamePDBX_nonpoly_schemeAuth_seq_num,
                // res.authResNameList[resRID]);
            }
            counterEntityAssembly++;
        }
        if (tTAssemblyPDBX_nonpoly_scheme.sizeRows == 0) {
            // General.showDebug("Removing empty tT: " +
            // ((TagTable)sFAssembly.get(2)).toSTAR());
            sFAssembly.remove(2);
        }

        // General.showDebug("Done with sFAssembly");
        if (molecularSystemDescriptionToWrite) {
            db.add(sFAssembly);
        }

        // ENTITIES
        HashSet molDone = new HashSet();
        HashMap non_standard_residues_to_write = new HashMap();
        for (molRID = molSet.nextSetBit(0); molRID >= 0; molRID = molSet.nextSetBit(molRID + 1)) {
            int entityId = mol2EntityMap[molRID];
            if (molDone.contains(new Integer(entityId))) {
                continue;
            }
            molDone.add(new Integer(entityId));
            SaveFrame sFEntity = getSFTemplateEntity();
            if (sFEntity == null) {
                General.showError("Failed to get a template saveframe for the entity.");
                return null;
            }
            String molLabel = molMain.getValueString(molRID, Relation.DEFAULT_ATTRIBUTE_NAME);
            ;
            String sFTitle = molName2STAR(molLabel);
            sFEntity.setTitle(sFTitle);
            TagTable tTIntro = (TagTable) sFEntity.get(0);
            // tTIntro.setValue(0, tagNameMolEntityId, new
            // Integer(mol2EntityMap[molRID]));
            int mol_type = mol.type[molRID];
            if (Defs.isNull(mol_type)) {
                mol_type = Molecule.UNKNOWN_TYPE;
            }
            tTIntro.setValue(0, tagNameMolType, Molecule.typeEnum[mol_type]);
            tTIntro.setValue(0, tagNameMolName, molLabel);
            int seqLength = 0;
            int pol_type = mol.polType[molRID];
            if (Defs.isNull(pol_type)) {
                pol_type = Molecule.UNKNOWN_POL_TYPE;
            }
            tTIntro.setValue(0, tagNameMolPolType, Molecule.polTypeEnum[pol_type]);
            String sequence = mol.getSequence(molRID, false, 0, Defs.NULL_CHAR, "\n");
            tTIntro.setValue(0, tagNameMolSeq, sequence);
            TagTable tTUnordered = (TagTable) sFEntity.get(1);
            TagTable tTOrdered = (TagTable) sFEntity.get(2);
            counterEntityAssembly = 1;
            // Get the molecule descriptions for first model in selection and
            // assume the others are the same.
            BitSet resSet = SQLSelect.selectBitSet(dbms, resMain,
                    Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME],
                    SQLSelect.OPERATION_TYPE_EQUALS, new Integer(molRID), false);
            if (resSet == null) {
                General.showError("Failed to get a residues in molecule.");
                return null;
            }
            seqLength = resSet.cardinality();
            tTIntro.setValue(0, tagNameMolSeqLength, new Integer(seqLength));

            for (resRID = resSet.nextSetBit(0); resRID >= 0; resRID = resSet.nextSetBit(resRID + 1)) {
                int entityResRID = tTUnordered.getNewRowId();
                entityResRID = tTOrdered.getNewRowId(); // make distinct later.
                if (entityResRID < 0) {
                    General.showError("Failed to get a new row in the table for residues.");
                    return null;
                }
                String resName = resMain.getValueString(resRID, Relation.DEFAULT_ATTRIBUTE_NAME);
                non_standard_residues_to_write.put(resName, null);
                int resNum = resMain.getValueInt(resRID, Relation.DEFAULT_ATTRIBUTE_NUMBER);
                if (Defs.isNull(resNum)) {
                    General.showError("Failed to get residue number: output contains STAR nills for residue numbers.");
                }
                // tTUnordered.setValue( entityResRID, tagNameResEntityId, new
                // Integer(mol2EntityMap[molRID]));
                tTUnordered.setValue(entityResRID, tagNameResNum_1, new Integer(counterEntityAssembly));
                tTUnordered.setValue(entityResRID, tagNameResCompId, resName);
                if (!Biochemistry.commonResidueNameAA_NA.containsKey(resName)) {
                    tTUnordered.setValue(entityResRID, tagNameResCompLabel, "$" + resName);
                }

                // tTOrdered.setValue( entityResRID, tagNameResMolId, new
                // Integer(mol2EntityMap[molRID]));
                tTOrdered.setValue(entityResRID, tagNameResResId, new Integer(counterEntityAssembly));
                tTOrdered.setValue(entityResRID, tagNameResNum_2, new Integer(resNum));
                tTOrdered.setValue(entityResRID, tagNameResName, resName);
                counterEntityAssembly++;
            }
            if (!tTUnordered.copyToOrderColumn(tagNameResNum_1, shift)) {
                General.showError("Failed to set the order for the ordered residues tag table.");
                return null;
            }
            if (!tTOrdered.copyToOrderColumn(tagNameResNum_2, shift)) {
                General.showError("Failed to set the order for the ordered residues tag table.");
                return null;
            }

            if (mol_type == Molecule.NON_POLYMER_TYPE || mol_type == Molecule.WATER_TYPE) {
                tTIntro.removeColumn(tagNameMolPolType);
                tTIntro.removeColumn(tagNameMolSeq);
                tTIntro.removeColumn(tagNameMolSeqLength);
                sFEntity.remove(2);
            }
            // Don't use the loops if they don't have rows.
            if (seqLength == 0) {
                sFEntity.remove(1);
            }
            if (molecularSystemDescriptionToWrite) {
                db.add(sFEntity);
            }
        }
        // General.showDebug("Done with one or more sFEntity");

        // NON-STANDARD RESIDUES
        ArrayList non_standard_residues_to_write_list = new ArrayList(non_standard_residues_to_write.keySet());
        for (int i = 0; i < non_standard_residues_to_write_list.size(); i++) {
            String resName = (String) non_standard_residues_to_write_list.get(i);
            if (Biochemistry.commonResidueNameAA_NA.containsKey(resName)) {
                continue;
            }
            SaveFrame sFNonStandardResidue = getSFTemplateNonStandardResidue(resName);
            if (sFNonStandardResidue == null) {
                General.showError("Failed to get a template saveframe for the entity.");
                return null;
            }

            TagTable tTIntro = (TagTable) sFNonStandardResidue.get(0);
            tTIntro.setValue(0, Relation.DEFAULT_ATTRIBUTE_ORDER_ID, 0);
            tTIntro.setValue(0, tagNameChem_compId, resName);

            db.add(sFNonStandardResidue);
        }
        // General.showDebug("Done with zero or more sFNonStandardResidue");

        // XXX writing DISTANCE CONSTRAINTS
        dcList.selected.and(dcList.used); // Just to make sure.
        BitSet dcSetDistinctList = SQLSelect.getDistinct(dbms, dc.mainRelation,
                Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[RelationSet.RELATION_ID_COLUMN_NAME], dc.selected);
        if (dcSetDistinctList == null) {
            General.showError("Failed to get dcSetDistinctList");
            return null;
        }
        int dcListCountTotal = dcSetDistinctList.cardinality();
        if (dcListCountTotal < 1) {
            // General.showDebug("No dcSetDistinctList");
        } else {
            General.showDebug("Will write number of dc lists: " + dcListCountTotal);
            int dcListCount = 1;
            for (int currentDCListDCId = dcSetDistinctList.nextSetBit(0); currentDCListDCId >= 0; currentDCListDCId = dcSetDistinctList
                    .nextSetBit(currentDCListDCId + 1)) {
                currentDCListId = dc.dcListIdMain[currentDCListDCId];

                SaveFrame sFDC = getSFTemplateDC();
                if (sFDC == null) {
                    General.showError("Failed to get a template saveframe for the dc list.");
                    return null;
                }
                sFDC.setTitle(dcList.nameList[currentDCListId]);
                if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                    General.showCodeBug("Failed to initConvenienceVariablesStarWattos.");
                    return null;
                }
                tTIntro.setValue(0, tagNameDCType, DistConstrList.DEFAULT_TYPE_LIST[dcList.type[currentDCListId]]);

                BitSet dcSet = SQLSelect.selectBitSet(dbms, dc.mainRelation,
                        Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                        SQLSelect.OPERATION_TYPE_EQUALS, new Integer(currentDCListId), false);
                if (dcSet == null) {
                    General.showError("Failed to get set of distance constraints in list: " + dcListCount
                            + " to write again.");
                    return null;
                }
                dcSet.and(dc.selected); // this reduces it nicely.
                int dcCount = 1;
                int dcCountTotal = dcSet.cardinality();
                General.showDebug("Number of distance constraints in list: " + dcCountTotal);
                if (dcCountTotal < 1) {
                    General.showWarning("Failed to find any distance constraint in list:" + dcListCount
                            + ". Skipping list.");
                    dcListCount++;
                    continue;
                } else {
                    db.add(sFDC);
                }
                // Quick optimalization but small size estimates; rest will be
                // allocated slightly less optimal.
                // Can be estimated / calculated much better of course.
                tTTree.reserveRows(dcCountTotal * 2);
                tTAtom.reserveRows(dcCountTotal * 2 * 3);
                tTDist.reserveRows(dcCountTotal * 2);
                // Need to reinitialize these again after this big reservation!
                if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                    General.showCodeBug("Failed to initConvenienceVariablesStarWattos.");
                    return null;
                }

                // Important to get the indexes after the dc.sortAll.
                IndexSortedInt indexMembAtom = (IndexSortedInt) dc.distConstrAtom.getIndex(
                        Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID, Index.INDEX_TYPE_SORTED);
                IndexSortedInt indexNodeMemb = (IndexSortedInt) dc.distConstrMemb.getIndex(
                        Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID, Index.INDEX_TYPE_SORTED);
                IndexSortedInt indexMainNode = (IndexSortedInt) dc.distConstrNode.getIndex(
                        Constr.DEFAULT_ATTRIBUTE_SET_DC[RelationSet.RELATION_ID_COLUMN_NAME], Index.INDEX_TYPE_SORTED);
                if (indexMembAtom == null || indexNodeMemb == null || indexMainNode == null) {
                    General.showCodeBug("Failed to get all indexes to dc main in atom/memb/node");
                    return null;
                }
                int currentStarDCTreeId = 0;
                int currentStarDCAtomId = 0;
                int currentStarDCDistId = 0;
                // FOR EACH CONSTRAINT
                // Write them in a sorted fashion
                int[] map = dc.mainRelation.getRowOrderMap(Relation.DEFAULT_ATTRIBUTE_ORDER_ID); // Includes
                // just
                // the
                // dcs in this list
                if ((map != null) && (map.length != dcCountTotal)) {
                    General
                            .showWarning("Trying to get an order map but failed to give back the correct number of elements: "
                                    + dcCountTotal + " instead found: " + map.length);
                    map = null;
                }
                if (map == null) {
                    General
                            .showWarning("Failed to get the row order sorted out for distance constraints; using physical ordering."); // not
                    // fatal
                    map = PrimitiveArray.toIntArray(dcSet);
                    if (map == null) {
                        General.showError("Failed to get the used row map list so not writing this table.");
                        return null;
                    }
                }
                BitSet mapAsSet = PrimitiveArray.toBitSet(map, -1);
                if (mapAsSet == null) {
                    General.showCodeBug("Failed to create bitset back from map.");
                    return null;
                }
                BitSet mapXor = (BitSet) mapAsSet.clone();
                mapXor.xor(dcSet);
                if (mapXor.cardinality() != 0) {
                    General
                            .showError("The map after reordering doesn't contain all the elements in the original set or vice versa.");
                    General.showError("In the original set:" + PrimitiveArray.toString(dcSet));
                    General.showError("In the ordered  set:" + PrimitiveArray.toString(mapAsSet));
                    General.showError("Xor of both sets   :" + PrimitiveArray.toString(mapXor));
                    General.showError("The order column   :"
                            + PrimitiveArray
                                    .toString(dc.mainRelation.getColumnInt(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)));
                    return null;
                }

                for (int d = 0; d < map.length; d++) {
                    currentDCId = map[d];
                    // extra checks for safety.
                    if (!dcSet.get(currentDCId)) { // this is actually check
                        // above so shouldn't occur.
                        General.showError("Trying an unselected dc rid: " + currentDCId);
                        return null;
                    }
                    if (!dc.mainRelation.used.get(currentDCId)) {
                        General.showCodeBug("Trying an unused dc rid; that was checked earlier.");
                        return null;
                    }

                    // General.showDebug("Preparing distance constraint: " +
                    // dcCount + " at rid: " + currentDCId);
                    // General.showDebug(dc.toString(currentDCId));

                    Integer currentDCIdInteger = new Integer(currentDCId);
                    IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(currentDCIdInteger,
                            Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    // General.showDebug("Found the following rids of nodes in constraint: "
                    // + PrimitiveArray.toString(
                    // dcNodes ));
                    if (dcNodes == null) {
                        General.showError("Failed to get nodes");
                        return null;
                    }
                    if (dcNodes.size() < 1) {
                        General.showError("Failed to get at least one node");
                        return null;
                    }
                    if (!PrimitiveArray.orderIntArrayListByIntArray(dcNodes, dc.numbNode)) {
                        General.showError("Failed to order nodes by order column");
                        return null;
                    }

                    int dCNodeNumber = 1;
                    // FOR EACH NODE
                    for (int currentDCNodeBatchId = 0; currentDCNodeBatchId < dcNodes.size(); currentDCNodeBatchId++) {
                        int currentDCNodeId = dcNodes.getQuick(currentDCNodeBatchId); // quick
                        // enough?;-)
                        currentStarDCTreeId = tTTree.getNextReservedRow(currentStarDCTreeId);
                        // Check if the relation grew in size because not all
                        // relations can be adaquately estimated.
                        if (currentStarDCTreeId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                            currentStarDCTreeId = tTTree.getNextReservedRow(0); // now
                            // it
                            // should
                            // be
                            // fine.
                            if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                                General.showCodeBug("Failed to initConvenienceVariablesStarWattos.");
                                return null;
                            }
                        }
                        if (currentStarDCTreeId < 0) {
                            General
                                    .showCodeBug("Failed to get next reserved row in STAR distance constraint tree table.");
                            return null;
                        }
                        // General.showDebug("Preparing node: " + dCNodeNumber +
                        // " at rid: " + currentDCNodeId +
                        // " to star rid: " + currentStarDCTreeId);
                        varDCtreeConstraintsID[currentStarDCTreeId] = dcCount; // can
                        // be
                        // optimized
                        // by
                        // array
                        // fill.
                        // varDCtreeID[ currentStarDCTreeId ] = dcCount;
                        varDCtreeNodeID[currentStarDCTreeId] = dCNodeNumber; // todo
                        // increment
                        varDCtreeDownnodeID[currentStarDCTreeId] = dc.downId[currentDCNodeId];
                        varDCtreeRightnodeID[currentStarDCTreeId] = dc.rightId[currentDCNodeId];
                        varDCtreeLogicoperation[currentStarDCTreeId] = DistConstr
                                .getLogicalOperation(dc.logicalOp[currentDCNodeId]);

                        // For each constraint node (those with distance etc but
                        // without logic) add the distance info to
                        // the distance star loop.
                        if (!Defs.isNull(dc.logicalOp[currentDCNodeId])) {
                            dCNodeNumber++;
                            continue;
                        }
                        // General.showDebug(
                        // "Dc node is a constraint node and not a logical node."
                        // );
                        currentStarDCDistId = tTDist.getNextReservedRow(currentStarDCDistId);
                        // Check if the relation grew in size because not all
                        // relations can be adaquately estimated.
                        if (currentStarDCDistId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                            currentStarDCDistId = tTDist.getNextReservedRow(0); // now
                            // it
                            // should
                            // be
                            // fine.
                            if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                                General.showCodeBug("Failed to initConvenienceVariablesStarWattos.");
                                return null;
                            }
                        }
                        if (currentStarDCDistId < 0) {
                            General
                                    .showError("Failed to get next reserved row in STAR distance constraint dist table.");
                            return null;
                        }
                        // varDCvalueConstraintsID[ currentStarDCDistId ] =
                        // dcListCount;
                        varDCvalueConstraintID[currentStarDCDistId] = dcCount;
                        varDCvalueTreenodeID[currentStarDCDistId] = dCNodeNumber;
                        // varDCvalueSourceexperimentID[ currentStarDCDistId ] =
                        // ;
                        varDCvalueSpectralpeakID[currentStarDCDistId] = dc.peakIdList[currentDCNodeId];
                        // varDCvalueIntensityval[ currentStarDCDistId ] = ;
                        // varDCvalueIntensitylowervalerr[ currentStarDCDistId ]
                        // = ;
                        // varDCvalueIntensityuppervalerr[ currentStarDCDistId ]
                        // = ;
                        varDCvalueDistanceval[currentStarDCDistId] = dc.target[currentDCNodeId];
                        varDCvalueDistancelowerboundval[currentStarDCDistId] = dc.lowBound[currentDCNodeId];
                        varDCvalueDistanceupperboundval[currentStarDCDistId] = dc.uppBound[currentDCNodeId];
                        // varDCvalueWeight[ currentStarDCDistId ] = dc.weight[
                        // currentDCNodeId];
                        // varDCvalueSpectralpeakppm1[ currentStarDCDistId ] =
                        // varDCvalueSpectralpeakppm2[ currentStarDCDistId ] =

                        // General.showDebug("Looking for members in constraint node rid:"
                        // + currentDCNodeId );
                        IntArrayList dcMembs = (IntArrayList) indexNodeMemb.getRidList(new Integer(currentDCNodeId),
                                Index.LIST_TYPE_INT_ARRAY_LIST, null);
                        // General.showDebug("Found the following rids of members in constraint node ("
                        // +
                        // currentDCNodeId + "): " + PrimitiveArray.toString(
                        // dcMembs ));
                        if (dcMembs.size() != 2) {
                            General
                                    .showError("Are we using a number different than 2 as the number of members in a node?");
                            return null;
                        }
                        if (!PrimitiveArray.orderIntArrayListByIntArray(dcMembs, dc.numbMemb)) {
                            General.showError("Failed to order members by order column");
                            return null;
                        }
                        // FOR EACH MEMBER (usually just 2)
                        for (int currentDCMembBatchId = 0; currentDCMembBatchId < dcMembs.size(); currentDCMembBatchId++) {
                            int currentDCMembID = dcMembs.getQuick(currentDCMembBatchId);
                            // General.showDebug("Working on member with RID: "
                            // + currentDCMembID);

                            int molNum = Defs.NULL_INT;
                            int prevMolNum = Defs.NULL_INT;
                            int entityNum = Defs.NULL_INT;
                            String atomName = null;

                            /**
                             * Next objects can be reused if code needs
                             * optimalization.
                             */
                            IntArrayList dcAtoms = (IntArrayList) indexMembAtom.getRidList(
                                    new Integer(currentDCMembID), Index.LIST_TYPE_INT_ARRAY_LIST, null);
                            if (dcAtoms.size() < 1) {
                                General.showError("No atoms in member");
                                return null;
                            }

                            if (!PrimitiveArray.orderIntArrayListByIntArray(dcAtoms, dc.orderAtom)) {
                                General.showError("Failed to order atoms by order column");
                                return null;
                            }
                            // Get the real atom ids into a new list.
                            IntArrayList atomRids = new IntArrayList();
                            // atomRids.ensureCapacity( dcAtoms.size() );
                            atomRids.setSize(dcAtoms.size());
                            for (int i = 0; i < dcAtoms.size(); i++) {
                                atomRids.setQuick(i, dc.atomIdAtom[dcAtoms.getQuick(i)]);
                            }
                            // General.showDebug("Found the following rids of atoms in constraint node ("
                            // + dCNodeNumber
                            // + "): " + PrimitiveArray.toString( atomRids ));
                            if (atomRids.size() < 1) {
                                General.showError("Didn't find a single atom for a member in constraint node ("
                                        + dCNodeNumber + "): for constraint number: " + dcCount);
                                return null;
                            }
                            IntArrayList statusList = new IntArrayList(); // List
                            // of
                            // status
                            // (like
                            // ok,
                            // pseudo,
                            // deleted)
                            // for the original atoms
                            ObjectArrayList pseudoNameList = new ObjectArrayList(); // List
                            // of
                            // pseudo
                            // atom
                            // names
                            // also
                            // parrallel to dcAtoms.
                            statusList.setSize(dcAtoms.size());
                            pseudoNameList.setSize(dcAtoms.size());
                            if (!atom.collapseToPseudo(atomRids, statusList, pseudoNameList, ui.wattosLib.pseudoLib)) {
                                General.showError("Failed to collapse atoms to pseudo atom names.");
                                return null;
                            }
                            // FOR EACH ATOM in member
                            for (int currentDCAtomBatchId = 0; currentDCAtomBatchId < dcAtoms.size(); currentDCAtomBatchId++) {
                                int statusAtom = statusList.getQuick(currentDCAtomBatchId);
                                if (statusAtom == PseudoLib.DEFAULT_REDUNDANT_BY_PSEUDO) {
                                    // General.showDebug(
                                    // "Skipping atom because status is PseudoLib.DEFAULT_REDUNDANT_BY_PSEUDO."
                                    // );
                                    // // next 3 lines are also redundant
                                    // outside debugging.
                                    // int currentDCAtomId = dcAtoms.getQuick(
                                    // currentDCAtomBatchId );
                                    // int atomId =
                                    // dc.atomIdAtom[currentDCAtomId];
                                    // General.showDebug(atom.toString(atomId));
                                    continue;
                                }
                                int currentDCAtomId = dcAtoms.getQuick(currentDCAtomBatchId);
                                if (!dc.distConstrAtom.used.get(currentDCAtomId)) {
                                    General.showCodeBug("Got an currentDCAtomId for an unused row: " + currentDCAtomId);
                                    return null;
                                }

                                // Reconstruct the dc node number from the fa
                                currentStarDCAtomId = tTAtom.getNextReservedRow(currentStarDCAtomId);
                                // General.showDebug("currentStarDCAtomId : " +
                                // currentStarDCAtomId);
                                // Check if the relation grew in size because
                                // not all relations can be adaquately
                                // estimated.
                                if (currentStarDCAtomId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                                    currentStarDCAtomId = tTAtom.getNextReservedRow(0); // now
                                    // it
                                    // should
                                    // be
                                    // fine.
                                    if (!initConvenienceVariablesStarWattosDC(sFDC)) {
                                        General.showCodeBug("Failed to initConvenienceVariablesStarWattos.");
                                        return null;
                                    }
                                }
                                if (currentStarDCAtomId < 0) {
                                    General
                                            .showCodeBug("Failed to get next reserved row in STAR distance constraint atom table.");
                                    return null;
                                }
                                // Optimize further perhaps.
                                // Make sure that they're always sorted in
                                // memory or rewrite code here.
                                int nodeId = dc.dcNodeIdAtom[currentDCAtomId];
                                int membId = dc.dcMembIdAtom[currentDCAtomId];
                                int atomId = dc.atomIdAtom[currentDCAtomId];
                                boolean atomFound = true;
                                if (Defs.isNull(atomId)) { // Was the atom
                                    // actually matched
                                    // in the structure?
                                    atomFound = false;
                                }

                                if (Defs.isNull(nodeId)) {
                                    General.showCodeBug("Got a null value for nodeId in dc atom with rid: "
                                            + currentDCAtomId);
                                    return null;
                                }
                                if (Defs.isNull(membId)) {
                                    General.showCodeBug("Got a null value for membId in dc atom with rid: "
                                            + currentDCAtomId);
                                    return null;
                                }

                                int resNum = Defs.NULL_INT;
                                String resName = Defs.NULL_STRING_NULL;
                                if (atomFound) {
                                    int resId = atom.resId[atomId];
                                    int molId = atom.molId[atomId];
                                    if (Defs.isNull(resId)) {
                                        General.showError("Didn't find a resId from atom id: " + atomId
                                                + " for atom with name: " + atomName);
                                        return null;
                                    }
                                    if (Defs.isNull(molId)) {
                                        General.showError("Didn't find a resId from mol id: " + atomId
                                                + " for atom with name: " + atomName);
                                        return null;
                                    }

                                    molNum = mol.number[molId];
                                    if (molNum != prevMolNum) {
                                        entityNum = ((Integer) molNumber2EntityNumberMap.get(new Integer(molNum)))
                                                .intValue(); // expensive so
                                        // only do when
                                        // changes are
                                        // possible.
                                    }
                                    if (statusAtom == PseudoLib.DEFAULT_REPLACED_BY_PSEUDO) {
                                        atomName = (String) pseudoNameList.getQuick(currentDCAtomBatchId); // get
                                        // the
                                        // next one
                                        // from the
                                        // list.
                                    } else {
                                        if (statusAtom != PseudoLib.DEFAULT_OK) {
                                            General.showCodeBug("Didn't expect this one in File31 for atom name: "
                                                    + atomName);
                                            return null;
                                        }
                                        atomName = atom.nameList[atomId];
                                    }
                                    resNum = res.number[resId];
                                    resName = res.nameList[resId];
                                } else {
                                    molNum = dc.molIdList[currentDCAtomId];
                                    entityNum = dc.entityIdList[currentDCAtomId];
                                    resNum = dc.resIdList[currentDCAtomId];
                                    resName = dc.resNameList[currentDCAtomId];
                                    atomName = dc.atomNameList[currentDCAtomId];
                                }

                                varDCConstraintsID[currentStarDCAtomId] = dcCount;
                                // varDCDistconstrainttreeID[
                                // currentStarDCAtomId ] = dcCount;
                                varDCTreenodemembernodeID[currentStarDCAtomId] = dc.nodeId[nodeId];
                                // varDCContributionfractionalval[
                                // currentStarDCAtomId ] = dc.contribution[
                                // nodeId ];
                                varDCConstrainttreenodememberID[currentStarDCAtomId] = dc.numbMemb[membId];
                                varDCEntityassemblyID[currentStarDCAtomId] = molNum;
                                varDCEntityID[currentStarDCAtomId] = entityNum;
                                varDCCompindexID[currentStarDCAtomId] = resNum;
                                varDCCompID[currentStarDCAtomId] = resName;
                                varDCAtomID[currentStarDCAtomId] = atomName;
                                varDCAuthsegmentcode[currentStarDCAtomId] = dc.authMolNameList[currentDCAtomId];
                                varDCAuthseqID[currentStarDCAtomId] = dc.authResIdList[currentDCAtomId];
                                varDCAuthcompID[currentStarDCAtomId] = dc.authResNameList[currentDCAtomId];
                                varDCAuthatomID[currentStarDCAtomId] = dc.authAtomNameList[currentDCAtomId];
                            } // end of loop per atom
                        } // end of loop per member
                        dCNodeNumber++;
                    } // end of loop per node
                    dcCount++;
                }
                dcListCount++;
                // Make it into a valid saveframe by cancelling reservations on
                // all the tag tables.
                tTTree.cancelAllReservedRows();
                tTAtom.cancelAllReservedRows();
                tTDist.cancelAllReservedRows();
            }
        }

        // XXX writing simple (CDIH) CONSTRAINTS
        cdihList.selected.and(cdihList.used); // Just to make sure.
        BitSet cdihSetDistinctList = SQLSelect.getDistinct(dbms, cdih.mainRelation,
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME], cdih.selected);
        if (cdihSetDistinctList == null) {
            General.showError("Failed to get cdihSetDistinctList");
            return null;
        }
        int cdihListCountTotal = cdihSetDistinctList.cardinality();
        if (cdihListCountTotal < 1) {
            // General.showDebug("No cdihSetDistinctList");
        } else {
            General.showDebug("Will write number of sc (CDIH) lists: " + cdihListCountTotal);
            int cdihListCount = 1;
            for (int currentSCListSCId = cdihSetDistinctList.nextSetBit(0); currentSCListSCId >= 0; currentSCListSCId = cdihSetDistinctList
                    .nextSetBit(currentSCListSCId + 1)) {
                currentSCListId = cdih.scListIdMain[currentSCListSCId];

                SaveFrame sFCDIH = getSFTemplateCDIH();
                if (sFCDIH == null) {
                    General.showError("Failed to get a template saveframe for the cdih list.");
                    return null;
                }
                sFCDIH.setTitle(cdihList.nameList[currentSCListId]);
                if (!initConvenienceVariablesStarWattosCDIH(sFCDIH)) {
                    General.showCodeBug("Failed to initConvenienceVariablesStarWattosCDIH.");
                    return null;
                }
                BitSet cdihSet = SQLSelect.selectBitSet(dbms, cdih.mainRelation,
                        Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                        SQLSelect.OPERATION_TYPE_EQUALS, new Integer(currentSCListId), false);
                if (cdihSet == null) {
                    General.showError("Failed to get set of simple (CDIH) constraints in list: " + cdihListCount
                            + " to write again.");
                    return null;
                }
                cdihSet.and(cdih.selected); // this reduces it nicely.
                int scCount = 1;
                int scCountTotal = cdihSet.cardinality();
                General.showDebug("Number of simple (CDIH) constraints in list: " + scCountTotal);
                if (scCountTotal < 1) {
                    General.showWarning("Failed to find any selected simple (CDIH) constraint in list:" + cdihListCount
                            + ". Skipping list.");
                    cdihListCount++;
                    continue;
                }
                db.add(sFCDIH);
                // Need to reinitialize these again after this big reservation!
                if (!initConvenienceVariablesStarWattosCDIH(sFCDIH)) {
                    General.showCodeBug("Failed to initConvenienceVariablesStarWattos.");
                    return null;
                }
                tTMain.reserveRows(scCountTotal);

                // Important to get the indexes after the sc.sortAll (not done
                // anymore).
                IndexSortedInt indexMainAtom = (IndexSortedInt) cdih.simpleConstrAtom
                        .getIndex(Constr.DEFAULT_ATTRIBUTE_SET_CDIH[RelationSet.RELATION_ID_COLUMN_NAME],
                                Index.INDEX_TYPE_SORTED);
                if (indexMainAtom == null) {
                    General.showCodeBug("Failed to get all indexes to sc main in atom");
                    return null;
                }

                // int currentStarSC_Id = 0;
                // FOR EACH CONSTRAINT
                // Write them in a sorted fashion
                int[] map = cdih.mainRelation.getRowOrderMap(Relation.DEFAULT_ATTRIBUTE_ORDER_ID); // Includes
                // just
                // the
                // scs in this list
                if ((map != null) && (map.length != scCountTotal)) {
                    General
                            .showWarning("Trying to get an order map but failed to give back the correct number of elements: "
                                    + scCountTotal + " instead found: " + map.length);
                    map = null;
                }
                if (map == null) {
                    General
                            .showWarning("Failed to get the row order sorted out for simple (CDIH) constraints; using physical ordering."); // not
                    // fatal
                    map = PrimitiveArray.toIntArray(cdihSet);
                    if (map == null) {
                        General.showError("Failed to get the used row map list so not writing this table.");
                        return null;
                    }
                }
                BitSet mapAsSet = PrimitiveArray.toBitSet(map, -1);
                if (mapAsSet == null) {
                    General.showCodeBug("Failed to create bitset back from map.");
                    return null;
                }
                BitSet mapXor = (BitSet) mapAsSet.clone();
                mapXor.xor(cdihSet);
                if (mapXor.cardinality() != 0) {
                    General
                            .showError("The map after reordering doesn't contain all the elements in the original set or vice versa.");
                    General.showError("In the original set:" + PrimitiveArray.toString(cdihSet));
                    General.showError("In the ordered  set:" + PrimitiveArray.toString(mapAsSet));
                    General.showError("Xor of both sets   :" + PrimitiveArray.toString(mapXor));
                    General.showError("The order column   :"
                            + PrimitiveArray.toString(cdih.mainRelation
                                    .getColumnInt(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)));
                    return null;
                }

                int currentStarSCId = 0;
                for (int d = 0; d < map.length; d++) {
                    currentSCId = map[d];
                    // extra checks for safety.
                    if (!cdihSet.get(currentSCId)) { // this is actually check
                        // above so shouldn't
                        // occur.
                        General.showError("Trying an unselected sc rid: " + currentSCId);
                        return null;
                    }
                    if (!cdih.mainRelation.used.get(currentSCId)) {
                        General.showCodeBug("Trying an unused sc rid; that was checked earlier.");
                        return null;
                    }

                    // General.showDebug("Preparing simple (CDIH) constraint: "
                    // + scCount + " at rid: " + currentSCId);
                    // BitSet bs = new BitSet();
                    // bs.set(currentSCId);
                    // General.showDebug(cdih.toSTAR(bs));

                    Integer currentSCIdInteger = new Integer(currentSCId);

                    IntArrayList scAtoms = (IntArrayList) indexMainAtom.getRidList(currentSCIdInteger,
                            Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    // General.showDebug("Found the following rids of sc atoms in constraint: "
                    // +
                    // PrimitiveArray.toString( scAtoms ));
                    if (scAtoms == null) {
                        General.showError("Failed to get any atoms");
                        return null;
                    }
                    if (scAtoms.size() != 4) {
                        General.showError("Failed to get exactly four atoms");
                        return null;
                    }
                    if (!PrimitiveArray.orderIntArrayListByIntArray(scAtoms, cdih.orderAtom)) {
                        General.showError("Failed to order atoms by order column");
                        return null;
                    }

                    currentStarSCId = tTMain.getNextReservedRow(currentStarSCId);
                    if (currentStarSCId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                        currentStarSCId = tTMain.getNextReservedRow(0); // now
                        // it
                        // should
                        // be
                        // fine.
                        if (!initConvenienceVariablesStarWattosCDIH(sFCDIH)) {
                            General.showCodeBug("Failed to initConvenienceVariablesStarWattosCDIH.");
                            return null;
                        }
                    }
                    if (currentStarSCId < 0) {
                        General
                                .showCodeBug("Failed to get next reserved row in STAR simple (CDIH) constraint atom table.");
                        return null;
                    }
                    String angleName = cdih.nameList[currentSCId];
                    if (!Defs.isNull(angleName)) {
                        tTMain.setValue(currentStarSCId, tagNameCDIH_Torsion_angle_name, angleName);
                    }
                    tTMain.setValue(currentStarSCId, tagNameCDIH_Angle_lower_bound_val, (float) Math
                            .toDegrees(cdih.lowBound[currentSCId]));
                    tTMain.setValue(currentStarSCId, tagNameCDIH_Angle_upper_bound_val, (float) Math
                            .toDegrees(cdih.uppBound[currentSCId]));
                    tTMain.setValue(currentStarSCId, tagNameCDIH_ID2, scCount);

                    int columnsPerAtom = 5;
                    int columnsPerAuthorAtom = 4;
                    int offsetToFirstAtom = 3;
                    int offsetToFirstAuthorAtom = 25;
                    int prevMolNum = Defs.NULL_INT;
                    int entityNum = Defs.NULL_INT;
                    for (int atom_id = 0; atom_id < 4; atom_id++) {
                        // FOR EACH ATOM in member
                        int offsetAtom = offsetToFirstAtom + columnsPerAtom * atom_id;
                        int offsetAuthorAtom = offsetToFirstAuthorAtom + columnsPerAuthorAtom * atom_id;
                        int currentSCAtomId = scAtoms.getQuick(atom_id);
                        int currentAtomId = cdih.atomIdAtom[currentSCAtomId];
                        int currentResId = gumbo.atom.resId[currentAtomId];
                        int currentMolId = gumbo.res.molId[currentResId];
                        if (Defs.isNull(currentAtomId)) { // Was the atom
                            // actually matched
                            // in the structure?
                            General.showError("Got null for atomId in Cdih");
                            return null;
                        }

                        String atomName = gumbo.atom.nameList[currentAtomId];
                        int resNum = gumbo.res.number[currentResId];
                        String resName = gumbo.res.nameList[currentResId];
                        int molNum = gumbo.mol.number[currentMolId];
                        if (molNum != prevMolNum) {
                            entityNum = ((Integer) molNumber2EntityNumberMap.get(new Integer(molNum))).intValue(); // expensive
                            // so
                            // only
                            // do
                            // when
                            // changes
                            // are
                            // possible
                            // .
                        }
                        tTMain.setValue(currentStarSCId, offsetAtom + 0, molNum);
                        tTMain.setValue(currentStarSCId, offsetAtom + 1, entityNum);
                        tTMain.setValue(currentStarSCId, offsetAtom + 2, resNum);
                        tTMain.setValue(currentStarSCId, offsetAtom + 3, resName);
                        tTMain.setValue(currentStarSCId, offsetAtom + 4, atomName);

                        tTMain.setValue(currentStarSCId, offsetAuthorAtom + 0, cdih.authMolNameList[currentSCAtomId]);
                        tTMain.setValue(currentStarSCId, offsetAuthorAtom + 1, cdih.authResIdList[currentSCAtomId]);
                        tTMain.setValue(currentStarSCId, offsetAuthorAtom + 2, cdih.authResNameList[currentSCAtomId]);
                        tTMain.setValue(currentStarSCId, offsetAuthorAtom + 3, cdih.authAtomNameList[currentSCAtomId]);
                    } // end of loop per atom

                    scCount++;
                }
                cdihListCount++;
                // Make it into a valid saveframe by cancelling reservations on
                // all the tag tables.
                tTMain.cancelAllReservedRows();
            }
        }

        // XXX writing simple (RDC) CONSTRAINTS
        rdcList.selected.and(rdcList.used); // Just to make sure.
        BitSet rdcSetDistinctList = SQLSelect.getDistinct(dbms, rdc.mainRelation,
                Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[RelationSet.RELATION_ID_COLUMN_NAME], rdc.selected);
        if (rdcSetDistinctList == null) {
            General.showError("Failed to get rdcSetDistinctList");
            return null;
        }
        int rdcListCountTotal = rdcSetDistinctList.cardinality();
        if (rdcListCountTotal < 1) {
            // General.showDebug("No rdcSetDistinctList");
        } else {
            General.showDebug("Will write number of sc (RDC) lists: " + rdcListCountTotal);
            int rdcListCount = 1;
            for (int currentSCListSCId = rdcSetDistinctList.nextSetBit(0); currentSCListSCId >= 0; currentSCListSCId = rdcSetDistinctList
                    .nextSetBit(currentSCListSCId + 1)) {
                currentSCListId = rdc.scListIdMain[currentSCListSCId];

                SaveFrame sFRDC = getSFTemplateRDC();
                if (sFRDC == null) {
                    General.showError("Failed to get a template saveframe for the rdc list.");
                    return null;
                }
                sFRDC.setTitle(rdcList.nameList[currentSCListId]);
                if (!initConvenienceVariablesStarWattosRDC(sFRDC)) {
                    General.showCodeBug("Failed to initConvenienceVariablesStarWattosRDC.");
                    return null;
                }
                BitSet rdcSet = SQLSelect.selectBitSet(dbms, rdc.mainRelation,
                        Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                        SQLSelect.OPERATION_TYPE_EQUALS, new Integer(currentSCListId), false);
                if (rdcSet == null) {
                    General.showError("Failed to get set of simple (RDC) constraints in list: " + rdcListCount
                            + " to write again.");
                    return null;
                }
                rdcSet.and(rdc.selected); // this reduces it nicely.
                int scCount = 1;
                int scCountTotal = rdcSet.cardinality();
                General.showDebug("Number of simple (RDC) constraints in list: " + scCountTotal);
                if (scCountTotal < 1) {
                    General.showWarning("Failed to find any selected simple (RDC) constraint in list:" + rdcListCount
                            + ". Skipping list.");
                    rdcListCount++;
                    continue;
                }
                db.add(sFRDC);
                // Need to reinitialize these again after this big reservation!
                if (!initConvenienceVariablesStarWattosRDC(sFRDC)) {
                    General.showCodeBug("Failed to initConvenienceVariablesStarWattos.");
                    return null;
                }
                tTMain.reserveRows(scCountTotal);

                // Important to get the indexes after the sc.sortAll (not done
                // anymore).
                IndexSortedInt indexMainAtom = (IndexSortedInt) rdc.simpleConstrAtom.getIndex(
                        Constr.DEFAULT_ATTRIBUTE_SET_RDC[RelationSet.RELATION_ID_COLUMN_NAME], Index.INDEX_TYPE_SORTED);
                if (indexMainAtom == null) {
                    General.showCodeBug("Failed to get all indexes to sc main in atom");
                    return null;
                }

                // int currentStarSC_Id = 0;
                // FOR EACH CONSTRAINT
                // Write them in a sorted fashion
                int[] map = rdc.mainRelation.getRowOrderMap(Relation.DEFAULT_ATTRIBUTE_ORDER_ID); // Includes
                // just
                // the
                // scs in this list
                if ((map != null) && (map.length != scCountTotal)) {
                    General
                            .showWarning("Trying to get an order map but failed to give back the correct number of elements: "
                                    + scCountTotal + " instead found: " + map.length);
                    map = null;
                }
                if (map == null) {
                    General
                            .showWarning("Failed to get the row order sorted out for simple (RDC) constraints; using physical ordering."); // not
                    // fatal
                    map = PrimitiveArray.toIntArray(rdcSet);
                    if (map == null) {
                        General.showError("Failed to get the used row map list so not writing this table.");
                        return null;
                    }
                }
                BitSet mapAsSet = PrimitiveArray.toBitSet(map, -1);
                if (mapAsSet == null) {
                    General.showCodeBug("Failed to create bitset back from map.");
                    return null;
                }
                BitSet mapXor = (BitSet) mapAsSet.clone();
                mapXor.xor(rdcSet);
                if (mapXor.cardinality() != 0) {
                    General
                            .showError("The map after reordering doesn't contain all the elements in the original set or vice versa.");
                    General.showError("In the original set:" + PrimitiveArray.toString(rdcSet));
                    General.showError("In the ordered  set:" + PrimitiveArray.toString(mapAsSet));
                    General.showError("Xor of both sets   :" + PrimitiveArray.toString(mapXor));
                    General.showError("The order column   :"
                            + PrimitiveArray.toString(rdc.mainRelation
                                    .getColumnInt(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)));
                    return null;
                }

                int currentStarSCId = 0;
                for (int d = 0; d < map.length; d++) {
                    currentSCId = map[d];
                    // extra checks for safety.
                    if (!rdcSet.get(currentSCId)) { // this is actually check
                        // above so shouldn't occur.
                        General.showError("Trying an unselected sc rid: " + currentSCId);
                        return null;
                    }
                    if (!rdc.mainRelation.used.get(currentSCId)) {
                        General.showCodeBug("Trying an unused sc rid; that was checked earlier.");
                        return null;
                    }

                    // General.showDebug("Preparing simple (RDC) constraint: " +
                    // scCount + " at rid: " + currentSCId);
                    // BitSet bs = new BitSet();
                    // bs.set(currentSCId);
                    // General.showDebug(rdc.toSTAR(bs));

                    Integer currentSCIdInteger = new Integer(currentSCId);

                    IntArrayList scAtoms = (IntArrayList) indexMainAtom.getRidList(currentSCIdInteger,
                            Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    // General.showDebug("Found the following rids of sc atoms in constraint: "
                    // +
                    // PrimitiveArray.toString( scAtoms ));
                    if (scAtoms == null) {
                        General.showError("Failed to get any atoms");
                        return null;
                    }
                    if (scAtoms.size() != 2) {
                        General.showError("Failed to get exactly two atoms");
                        return null;
                    }
                    if (!PrimitiveArray.orderIntArrayListByIntArray(scAtoms, rdc.orderAtom)) {
                        General.showError("Failed to order atoms by order column");
                        return null;
                    }

                    currentStarSCId = tTMain.getNextReservedRow(currentStarSCId);
                    if (currentStarSCId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                        currentStarSCId = tTMain.getNextReservedRow(0); // now
                        // it
                        // should
                        // be
                        // fine.
                        if (!initConvenienceVariablesStarWattosRDC(sFRDC)) {
                            General.showCodeBug("Failed to initConvenienceVariablesStarWattosRDC.");
                            return null;
                        }
                    }
                    if (currentStarSCId < 0) {
                        General
                                .showCodeBug("Failed to get next reserved row in STAR simple (RDC) constraint atom table.");
                        return null;
                    }
                    tTMain.setValue(currentStarSCId, tagNameRDC_RDC_lower_bound, rdc.lowBound[currentSCId]);
                    tTMain.setValue(currentStarSCId, tagNameRDC_RDC_upper_bound, rdc.uppBound[currentSCId]);
                    tTMain.setValue(currentStarSCId, tagNameRDC_RDC_val, rdc.target[currentSCId]);
                    tTMain.setValue(currentStarSCId, tagNameRDC_RDC_val_err, rdc.targetError[currentSCId]);
                    tTMain.setValue(currentStarSCId, tagNameRDC_ID2, scCount);

                    int columnsPerAtom = 5;
                    int columnsPerAuthorAtom = 4;
                    int offsetToFirstAtom = 2;
                    int offsetToFirstAuthorAtom = 16;
                    int prevMolNum = Defs.NULL_INT;
                    int entityNum = Defs.NULL_INT;
                    for (int atom_id = 0; atom_id < scAtoms.size(); atom_id++) {
                        // FOR EACH ATOM in member
                        int offsetAtom = offsetToFirstAtom + columnsPerAtom * atom_id;
                        int offsetAuthorAtom = offsetToFirstAuthorAtom + columnsPerAuthorAtom * atom_id;
                        int currentSCAtomId = scAtoms.getQuick(atom_id);
                        int currentAtomId = rdc.atomIdAtom[currentSCAtomId];
                        int currentResId = gumbo.atom.resId[currentAtomId];
                        int currentMolId = gumbo.res.molId[currentResId];
                        if (Defs.isNull(currentAtomId)) { // Was the atom
                            // actually matched
                            // in the structure?
                            General.showError("Got null for atomId in Rdc");
                            return null;
                        }

                        String atomName = gumbo.atom.nameList[currentAtomId];
                        int resNum = gumbo.res.number[currentResId];
                        String resName = gumbo.res.nameList[currentResId];
                        int molNum = gumbo.mol.number[currentMolId];
                        if (molNum != prevMolNum) {
                            entityNum = ((Integer) molNumber2EntityNumberMap.get(new Integer(molNum))).intValue(); // expensive
                            // so
                            // only
                            // do
                            // when
                            // changes
                            // are
                            // possible
                            // .
                        }
                        tTMain.setValue(currentStarSCId, offsetAtom + 0, molNum);
                        tTMain.setValue(currentStarSCId, offsetAtom + 1, entityNum);
                        tTMain.setValue(currentStarSCId, offsetAtom + 2, resNum);
                        tTMain.setValue(currentStarSCId, offsetAtom + 3, resName);
                        tTMain.setValue(currentStarSCId, offsetAtom + 4, atomName);

                        tTMain.setValue(currentStarSCId, offsetAuthorAtom + 0, rdc.authMolNameList[currentSCAtomId]);
                        tTMain.setValue(currentStarSCId, offsetAuthorAtom + 1, rdc.authResIdList[currentSCAtomId]);
                        tTMain.setValue(currentStarSCId, offsetAuthorAtom + 2, rdc.authResNameList[currentSCAtomId]);
                        tTMain.setValue(currentStarSCId, offsetAuthorAtom + 3, rdc.authAtomNameList[currentSCAtomId]);
                    } // end of loop per atom

                    scCount++;
                }
                rdcListCount++;
                // Make it into a valid saveframe by cancelling reservations on
                // all the tag tables.
                tTMain.cancelAllReservedRows();
            }
        }

        // COORDINATES
        int atomRID = atom.selected.nextSetBit(0);
        if (atomRID < 0) {
            General.showDebug("No coordinates selected so none to write.");
        } else {
            int atomCount = atom.selected.cardinality();
            // int modelCount = model.selected.cardinality();
            // General.showDebug("Number of coordinates (atoms)                               in selection: "
            // +
            // atomCount);
            // General.showDebug("Number of coordinates (averaged/rounded over models)        in selection: "
            // +
            // atomCount/modelCount);
            // General.showDebug("Number of coordinates (averaged/rounded over models/mols)   in selection: "
            // +
            // atomCount/(modelCount*molCount));
            if (atomCount > 0) {
                // Order the atoms for output to STAR based on the ordering
                // previously
                // done on models, mols, residues, and atoms.
                Comparator atomComparator = new ComparatorAtomPerModelMolResAtom();
                // General.showDebug("Before orderPerModelMolResAtom");
                if (!atom.orderPerModelMolResAtom(atom.selected, atomComparator, 0)) {
                    General.showError("Failed to reorder the rows in the atom table.");
                    return null;
                }
                BitSet toShowAtoms = new BitSet();
                toShowAtoms.set(0);
                // General.showDebug("Atoms after row reorder per model/mol/res/atom: "
                // +
                // atom.mainRelation.toStringRow(0,true));

                SaveFrame sFCoor = getSFTemplateCoor(); // For efficiency not
                // using template defs
                // of the second part of
                // this sf.
                sFCoor.setTitle("conformer_family_coord_set_1"); // To be
                // extended
                // later to
                // include
                // more than
                // 1 set.
                /**
                 * Cheapest thing to do is to link the original relation in
                 * which all non-selected rows have already been deleted.
                 */
                TagTable tTCoor = null;
                try {
                    tTCoor = new TagTable(dbms.getNextRelationName(), dbms);
                } catch (Exception e) {
                    General.showThrowable(e);
                    General.showError("Failed to construct a new tagtable for the coordinates");
                    return null;
                }
                tTCoor.init(atomMain); // With the lack of a casting operation
                // we defined an init that takes the
                // relation itself.
                sFCoor.remove(1); // removes the large coordinate loop
                // definitions
                sFCoor.add(1, tTCoor); // adds them from atomMain.

                // General.showOutput("tTCoor: " + tTCoor.toStringRow(0,true));
                /**
                 * Translate the string elements to int elements using an
                 * slightly optimized routine.
                 */
                int[] elementIds = tTCoor.getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID);
                if (elementIds == null) {
                    General.showError("Failed to get native column for elements");
                    return null;
                }
                if (!tTCoor.insertColumn(tagNameAtomElementId, Relation.DATA_TYPE_STRINGNR, null)) {
                    General.showError("Failed to insert a column with name: " + tagNameAtomElementId);
                    return null;
                }
                String[] elementIdsTmp = tTCoor.getColumnString(tagNameAtomElementId);
                if (elementIdsTmp == null) {
                    General.showError("Failed to get newly inserted column for elements");
                    return null;
                }
                if (!Chemistry.translateElementIdToNameInArrays(elementIds, elementIdsTmp, 0, elementIds.length)) {
                    General.showCodeBug("Failed to translate all element ids to names in arrays; setting all to null.");
                    tTCoor.setValueByColumn(tagNameAtomElementId, Defs.NULL_STRING_DOT);
                    return null;
                }

                /** Insert new columns required */
                if (!(
                tTCoor.insertColumn(tagNameAtomModelId, Relation.DATA_TYPE_INT, null)
                        & tTCoor.insertColumn(tagNameAtomAsym_ID, Relation.DATA_TYPE_STRINGNR, null)
                        & tTCoor.insertColumn(tagNameAtomMolId1, Relation.DATA_TYPE_INT, null)
                        & tTCoor.insertColumn(tagNameAtomMolId2, Relation.DATA_TYPE_INT, null)
                        & tTCoor.insertColumn(tagNameAtomResId, Relation.DATA_TYPE_INT, null) & tTCoor.insertColumn(
                        tagNameAtomResName, Relation.DATA_TYPE_STRINGNR, null))) {
                    General.showError("Failed to insert all columns at once.");
                    return null;
                }
                // General.showDebug(" tTCoor after  new columns insert: " +
                // tTCoor.toSTAR(toShowAtoms));

                /** Set new column's values */
                // if ( ! tTCoor.setValueByColumn(tagNameAtomSFId_2, "1") ) {
                // General.showError("Failed to set values for sf id column at once.");
                // return null;
                // }
                if (!copyModelNumbers(tTCoor)) {
                    General.showError("Failed to copy model numbers for all columns at once.");
                    return null;
                }
                if (!setEntityNumber(tTCoor, molNumber2EntityNumberMap)) {
                    General.showError("Failed to set values for entity and molecule columns at once.");
                    return null;
                }
                if (!copyResidueIdAndName(tTCoor)) {
                    General.showError("Failed to set residue properties for all columns at once.");
                    return null;
                }
                if (!Atom.setAtomIdsPerModel(tTCoor)) {
                    General.showError("Failed to set values for atom id column at once.");
                    return null;
                }

                // General.showDebug(" tTCoor after set values: " +
                // tTCoor.toSTAR(toShowAtoms));

                /** Modify some column labels */
                if (!(tTCoor.renameColumn(Relation.DEFAULT_ATTRIBUTE_NAME, tagNameAtomName)
                        & tTCoor.renameColumn(Relation.DEFAULT_ATTRIBUTE_NUMBER, tagNameAtomId)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME, tagNameAtomAuthMolId)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME, tagNameAtomAuthResName)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID, tagNameAtomAuthResId)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME, tagNameAtomAuthName)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE, tagNameAtomPdbInsertionCode)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_COOR_X, tagNameAtomCoorX)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_COOR_Y, tagNameAtomCoorY)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_COOR_Z, tagNameAtomCoorZ)
                        & tTCoor.renameColumn(Gumbo.DEFAULT_ATTRIBUTE_BFACTOR, tagNameAtomBFactor) & tTCoor
                        .renameColumn(Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY, tagNameAtomOccupancy))) {
                    General.showError("Failed to rename all columns at once.");
                    return null;
                }
                // General.showDebug(" tTCoor after rename: " +
                // tTCoor.toSTAR(toShowAtoms));

                /** Remove a bunch of columns. */
                if (!((tTCoor.removeColumn(Gumbo.DEFAULT_ATTRIBUTE_ELEMENT_ID) != null)
                // (tTCoor.removeColumn( Gumbo.DEFAULT_ATTRIBUTE_OCCUPANCY ) !=
                // null)
                )) {
                    General.showError("Failed to remove all columns at once.");
                    return null;
                }

                /** Order the columns */
                ArrayList columnListToKeep = new ArrayList();
                columnListToKeep.add(Relation.DEFAULT_ATTRIBUTE_ORDER_ID);
                // columnOrder.add(tagNameAtomSFId_2 );
                columnListToKeep.add(tagNameAtomModelId);
                columnListToKeep.add(tagNameAtomId);
                columnListToKeep.add(tagNameAtomMolId1);
                columnListToKeep.add(tagNameAtomMolId2);
                columnListToKeep.add(tagNameAtomAsym_ID); // new
                columnListToKeep.add(tagNameAtomResId);
                columnListToKeep.add(tagNameAtomResName);
                columnListToKeep.add(tagNameAtomName);
                columnListToKeep.add(tagNameAtomAuthMolId);
                columnListToKeep.add(tagNameAtomAuthResId);
                columnListToKeep.add(tagNameAtomAuthResName);
                columnListToKeep.add(tagNameAtomAuthName);
                columnListToKeep.add(tagNameAtomPdbInsertionCode);
                columnListToKeep.add(tagNameAtomElementId);
                columnListToKeep.add(tagNameAtomCoorX);
                columnListToKeep.add(tagNameAtomCoorY);
                columnListToKeep.add(tagNameAtomCoorZ);
                columnListToKeep.add(tagNameAtomBFactor);
                columnListToKeep.add(tagNameAtomOccupancy);
                // General.showDebug("Keeping columns ["+columnListToKeep.size()+"[: "
                // + Strings.toString(
                // columnListToKeep));

                if (!tTCoor.removeColumnsExcept(columnListToKeep)) {
                    General.showError("Failed to remove unlisted columns in the atom table.");
                    return null;
                }

                // Ordering is done from dictionary.
                // General.showOutput("tTCoor: " + tTCoor.toStringRow(0,true));
                // if ( ! tTCoor.reorderColumns( columnListToKeep )) {
                // General.showError("Failed to order the columns in the atom table.");
                // return null;
                // }
                // General.showDebug(" tTCoor after column reorder: " +
                // tTCoor.toSTAR());
                db.add(sFCoor);
            }
        }

        /** Reformat the columns by dictionary defs */
        StarDictionary starDict = ui.wattosLib.starDictionary;
        if (!db.toStarTextFormatting(starDict)) {
            General.showWarning("Failed to format all columns as per dictionary this is not fatal however.");
        }

        if (!usePostFixedOrdinalsAtomName) {
            atom.swapPostFixedOrdinalsAtomName(true);
        }

        enterStandardIDs(db, entryName);
        ui.wattosLib.validDictionary.sortTagNames(db);

        return db;
    }

    /**
     * Based on full sequence (not the 1 character ids) of the given selection
     * of moleculse, determine the different entities and map the molecules to
     * them. The returned map has keys for the molecule RID and values for the
     * entity numbers. Molecules without any residues are considered to be
     * derived from the same entity.
     */
    public int[] createMol2EntityMap(BitSet molSet) {
        int[] mol2EntityMap = new int[molSet.length()];
        ArrayList entitySequence = new ArrayList();
        int entityNumberMinOne;
        String sequence;

        for (int molRID = molSet.nextSetBit(0); molRID >= 0; molRID = molSet.nextSetBit(molRID + 1)) {
            sequence = mol.getSequence(molRID, true, 0, ',', "\n");
            if (sequence == null) {
                General.showError("Failed to get sequence for molecule");
                return null;
            }
            if (sequence == Defs.NULL_STRING_DOT) {
                General.showWarning("Got empty sequence for molecule");
            }
            // General.showDebug("Got sequence for molecule: " + sequence);
            entityNumberMinOne = entitySequence.indexOf(sequence); // requires
            // expensive
            // scan.
            if (entityNumberMinOne == -1) { // But sequential scan should be no
                // performance issue.
                entitySequence.add(sequence);
                entityNumberMinOne = entitySequence.size() - 1;
            }
            mol2EntityMap[molRID] = entityNumberMinOne + 1;
        }
        return mol2EntityMap;
    }

    /**
     * Create a map from entity number to entity name.
     */
    public HashMap createEntityNameMap(int[] mol2EntityMap, BitSet molSet) {
        HashMap result = new HashMap();
        try {
            int prevEntityNumb = Defs.NULL_INT;
            for (int molRID = molSet.nextSetBit(0); molRID >= 0; molRID = molSet.nextSetBit(molRID + 1)) {
                int entityNumb = mol2EntityMap[molRID];
                if (prevEntityNumb != entityNumb) {
                    prevEntityNumb = entityNumb;
                    String molNameOrg = molMain.getValueString(molRID, Relation.DEFAULT_ATTRIBUTE_NAME);
                    // General.showDebug("Found org molName: ["+molNameOrg+"]");
                    String molName = molName2STAR(molNameOrg);
                    // General.showDebug("Found molName: ["+molName+"]");
                    result.put(new Integer(entityNumb), molName);
                }
            }
        } catch (Exception e) {
            General.showThrowable(e);
            General.showError("Failed to do createEntityNameMap, probably a array access problem right?");
            return null;
        }
        return result;
    }

    /**
     * replaces any invalid character in input with an underscore to make it
     * suitable for use as a saveframe label.
     *
     * @param valueString
     * @return
     */
    public String molName2STAR(String valueString) {
        char newChar = '_';
        final String badChars = " !@#$%^&*()-=+{}[]:\"\\|;'<>?,./`~";
        for (int c = 0; c < badChars.length(); c++) {
            char ch = badChars.charAt(c);
            valueString = valueString.replace(ch, newChar);
        }
        return valueString;
    }

    /**
     * Create a map to be used in the coordinate table. It maps molecule numbers
     * to entity numbers which if all models are the same this is correct.
     */
    public HashMap createMolNumber2EntityNumberMap(int[] molRid2EntityMap, BitSet molSet) {
        HashMap result = new HashMap();
        try {
            for (int molRID = molSet.nextSetBit(0); molRID >= 0; molRID = molSet.nextSetBit(molRID + 1)) {
                result.put(new Integer(mol.number[molRID]), new Integer(molRid2EntityMap[molRID]));
            }
        } catch (Exception e) {
            General.showThrowable(e);
            General.showError("Failed to do createMolNumber2EntityNumberMap, probably a array access problem right?");
            return null;
        }
        return result;
    }

    /**
     * Routine copies the model numbers to the star tagtable.
     */
    public boolean copyModelNumbers(TagTable tTCoor) {
        int[] modelNumOrg = model.number; // Org number
        int[] modelNumNew = tTCoor.getColumnInt(tagNameAtomModelId);// New
        // number
        int[] modelIdOrg = tTCoor.getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME]);// Org
        // rid
        if ((modelNumOrg == null) || (modelNumNew == null) || (modelIdOrg == null)) {
            General.showError("Failed to get required columns in setModelNumbers");
            return false;
        }

        for (int r = tTCoor.used.nextSetBit(0); r >= 0; r = tTCoor.used.nextSetBit(r + 1)) {
            modelNumNew[r] = modelNumOrg[modelIdOrg[r]];
        }
        return true;
    }

    /**
     */
    public boolean setEntityNumber(TagTable tTCoor, HashMap molNumber2EntityNumberMap) {

        int[] molNumber = mol.number;
        int[] molId = tTCoor.getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RelationSet.RELATION_ID_COLUMN_NAME]);
        int[] molNumNew = tTCoor.getColumnInt(tagNameAtomMolId1);
        int[] entityNumNew = tTCoor.getColumnInt(tagNameAtomMolId2);
        String[] asymId = tTCoor.getColumnString(tagNameAtomAsym_ID);
        if ((molNumber == null) || (molNumNew == null) || (entityNumNew == null)) {
            General.showWarning("Failed to get required columns in setEntityNumbers");
            return false;
        }
        // Entity numbers
        int prevMolId = -1;
        int prevMolNum = -1;
        int prevEntityNumNew = -1;
        String prevAsymId = Defs.EMPTY_STRING;
        int molRID;
        try {
            // Because they're sorted by mol and entity; this would actually be
            // a prime
            // candidate for Array block fills but for now the stupid way is:
            for (int r = tTCoor.used.nextSetBit(0); r >= 0; r = tTCoor.used.nextSetBit(r + 1)) {
                molRID = molId[r]; // cache it even for the few times used
                // below.
                /**
                 * If mol rid is the same then the entity number is the same for
                 * sure.
                 */
                if (molRID != prevMolId) { // most uncommon situation
                    prevMolId = molRID;
                    prevMolNum = molNumber[molRID];
                    // Get new entity num even though it might be the same as
                    // before.
                    prevEntityNumNew = ((Integer) molNumber2EntityNumberMap.get(new Integer(prevMolNum))).intValue(); // expensive
                    // .
                    prevAsymId = mol.asymId[molRID];
                }
                molNumNew[r] = prevMolNum; // quite fast.
                entityNumNew[r] = prevEntityNumNew;
                asymId[r] = prevAsymId;
            }
        } catch (Exception e) {
            General.showThrowable(e);
            General
                    .showCodeBug("Failed to set molecule and entity numbers. Most likely a cast error after hashmap access right?");
            return false;
        }
        return true;
    }

    /**
     * Copy the residue id (not the sequence number) and name from the residue
     * table to the atom table.
     */
    public boolean copyResidueIdAndName(TagTable tTCoor) {
        int[] resId = tTCoor.getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RelationSet.RELATION_ID_COLUMN_NAME]);
        int[] resNumOrg = res.number;
        int[] resNumNew = tTCoor.getColumnInt(tagNameAtomResId);
        String[] resNameOrg = res.nameList;
        String[] resNameNew = tTCoor.getColumnString(tagNameAtomResName);
        if ((resId == null) || (resNumOrg == null) || (resNumNew == null) || (resNameOrg == null)
                || (resNameNew == null)) {
            General.showWarning("Failed to get required columns in setResidueProps");
            return false;
        }

        int ri;
        for (int r = tTCoor.used.nextSetBit(0); r >= 0; r = tTCoor.used.nextSetBit(r + 1)) {
            ri = resId[r];
            resNumNew[r] = resNumOrg[ri];
            resNameNew[r] = resNameOrg[ri];
        }
        return true;
    }

    public SaveFrame getSFTemplateCDIH() {
        SaveFrame sF = new SaveFrame();
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            namesAndTypes.put(tagNameCDIH_Sf_category, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameCDIH_Constraint_file_ID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_MR_file_block_position, new Integer(Relation.DATA_TYPE_INT));
            order.add(tagNameCDIH_Sf_category);
            order.add(tagNameCDIH_Constraint_file_ID);
            order.add(tagNameCDIH_MR_file_block_position);
            namesAndValues.put(tagNameCDIH_Sf_category, "torsion_angle_constraints");
            // Append columns after order id column.
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // LOOP
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            namesAndTypes.put(tagNameCDIH_ID2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Torsion_angle_name, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Entity_assembly_ID_1, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Entity_ID_1, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Comp_index_ID_1, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Comp_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Atom_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Entity_assembly_ID_2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Entity_ID_2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Comp_index_ID_2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Comp_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Atom_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Entity_assembly_ID_3, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Entity_ID_3, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Comp_index_ID_3, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Comp_ID_3, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Atom_ID_3, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Entity_assembly_ID_4, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Entity_ID_4, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Comp_index_ID_4, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameCDIH_Comp_ID_4, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Atom_ID_4, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Angle_lower_bound_val, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameCDIH_Angle_upper_bound_val, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameCDIH_Auth_segment_code_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_seq_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_comp_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_atom_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_segment_code_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_seq_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_comp_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_atom_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_segment_code_3, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_seq_ID_3, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_comp_ID_3, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_atom_ID_3, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_segment_code_4, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_seq_ID_4, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_comp_ID_4, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameCDIH_Auth_atom_ID_4, new Integer(Relation.DATA_TYPE_STRINGNR));

            order.add(tagNameCDIH_ID2);
            order.add(tagNameCDIH_Torsion_angle_name);
            order.add(tagNameCDIH_Entity_assembly_ID_1);
            order.add(tagNameCDIH_Entity_ID_1);
            order.add(tagNameCDIH_Comp_index_ID_1);
            order.add(tagNameCDIH_Comp_ID_1);
            order.add(tagNameCDIH_Atom_ID_1);
            order.add(tagNameCDIH_Entity_assembly_ID_2);
            order.add(tagNameCDIH_Entity_ID_2);
            order.add(tagNameCDIH_Comp_index_ID_2);
            order.add(tagNameCDIH_Comp_ID_2);
            order.add(tagNameCDIH_Atom_ID_2);
            order.add(tagNameCDIH_Entity_assembly_ID_3);
            order.add(tagNameCDIH_Entity_ID_3);
            order.add(tagNameCDIH_Comp_index_ID_3);
            order.add(tagNameCDIH_Comp_ID_3);
            order.add(tagNameCDIH_Atom_ID_3);
            order.add(tagNameCDIH_Entity_assembly_ID_4);
            order.add(tagNameCDIH_Entity_ID_4);
            order.add(tagNameCDIH_Comp_index_ID_4);
            order.add(tagNameCDIH_Comp_ID_4);
            order.add(tagNameCDIH_Atom_ID_4);
            order.add(tagNameCDIH_Angle_lower_bound_val);
            order.add(tagNameCDIH_Angle_upper_bound_val);
            order.add(tagNameCDIH_Auth_segment_code_1);
            order.add(tagNameCDIH_Auth_seq_ID_1);
            order.add(tagNameCDIH_Auth_comp_ID_1);
            order.add(tagNameCDIH_Auth_atom_ID_1);
            order.add(tagNameCDIH_Auth_segment_code_2);
            order.add(tagNameCDIH_Auth_seq_ID_2);
            order.add(tagNameCDIH_Auth_comp_ID_2);
            order.add(tagNameCDIH_Auth_atom_ID_2);
            order.add(tagNameCDIH_Auth_segment_code_3);
            order.add(tagNameCDIH_Auth_seq_ID_3);
            order.add(tagNameCDIH_Auth_comp_ID_3);
            order.add(tagNameCDIH_Auth_atom_ID_3);
            order.add(tagNameCDIH_Auth_segment_code_4);
            order.add(tagNameCDIH_Auth_seq_ID_4);
            order.add(tagNameCDIH_Auth_comp_ID_4);
            order.add(tagNameCDIH_Auth_atom_ID_4);

            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }

        // General.showDebug("SC (CDIH) SF is:[" + sF.toSTAR() + General.eol);
        return sF;
    }

    public SaveFrame getSFTemplateRDC() {
        SaveFrame sF = new SaveFrame();
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        ForeignKeyConstrSet foreignKeyConstrSet = null;
        TagTable tT;
        try {
            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            namesAndTypes.put(tagNameRDC_Sf_category, new Integer(Relation.DATA_TYPE_STRING));
            namesAndTypes.put(tagNameRDC_Constraint_file_ID, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRDC_MR_file_block_position, new Integer(Relation.DATA_TYPE_INT));
            order.add(tagNameRDC_Sf_category);
            order.add(tagNameRDC_Constraint_file_ID);
            order.add(tagNameRDC_MR_file_block_position);
            namesAndValues.put(tagNameRDC_Sf_category, "RDC_constraints");
            // Append columns after order id column.
            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

            // LOOP
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable(dbms.getNextRelationName(), dbms);
            tT.isFree = false;
            namesAndTypes.put(tagNameRDC_ID2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRDC_Entity_assembly_ID_1, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRDC_Entity_ID_1, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRDC_Comp_index_ID_1, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRDC_Comp_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Atom_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Entity_assembly_ID_2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRDC_Entity_ID_2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRDC_Comp_index_ID_2, new Integer(Relation.DATA_TYPE_INT));
            namesAndTypes.put(tagNameRDC_Comp_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Atom_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_RDC_val, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameRDC_RDC_val_err, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameRDC_RDC_lower_bound, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameRDC_RDC_upper_bound, new Integer(Relation.DATA_TYPE_FLOAT));
            namesAndTypes.put(tagNameRDC_Auth_segment_code_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Auth_seq_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Auth_comp_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Auth_atom_ID_1, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Auth_segment_code_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Auth_seq_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Auth_comp_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));
            namesAndTypes.put(tagNameRDC_Auth_atom_ID_2, new Integer(Relation.DATA_TYPE_STRINGNR));

            order.add(tagNameRDC_ID2);
            order.add(tagNameRDC_Entity_assembly_ID_1);
            order.add(tagNameRDC_Entity_ID_1);
            order.add(tagNameRDC_Comp_index_ID_1);
            order.add(tagNameRDC_Comp_ID_1);
            order.add(tagNameRDC_Atom_ID_1);
            order.add(tagNameRDC_Entity_assembly_ID_2);
            order.add(tagNameRDC_Entity_ID_2);
            order.add(tagNameRDC_Comp_index_ID_2);
            order.add(tagNameRDC_Comp_ID_2);
            order.add(tagNameRDC_Atom_ID_2);
            order.add(tagNameRDC_RDC_val);
            order.add(tagNameRDC_RDC_val_err);
            order.add(tagNameRDC_RDC_lower_bound);
            order.add(tagNameRDC_RDC_upper_bound);
            order.add(tagNameRDC_Auth_segment_code_1);
            order.add(tagNameRDC_Auth_seq_ID_1);
            order.add(tagNameRDC_Auth_comp_ID_1);
            order.add(tagNameRDC_Auth_atom_ID_1);
            order.add(tagNameRDC_Auth_segment_code_2);
            order.add(tagNameRDC_Auth_seq_ID_2);
            order.add(tagNameRDC_Auth_comp_ID_2);
            order.add(tagNameRDC_Auth_atom_ID_2);

            tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, foreignKeyConstrSet);
            sF.add(tT);

        } catch (Exception e) {
            General.showThrowable(e);
            return null;
        }

        // General.showDebug("SC (RDC) SF is:[" + sF.toSTAR() + General.eol);
        return sF;
    }

    /**
     * Adds the .Entry_ID and .ID tags to the first tt (tag table) in each sf
     * (save frame) and .Entry_ID and .XXXX_ID tags to any further tts in each
     * sf. Keeps track of the ids of some sf category instances as they appear
     * in sequential order. The only exceptions are for: _Entry.Sf_category
     * entry_information _Chem_comp.Sf_category chem_comp where there is no
     * .Entry_ID in first tt and no .XXXX_ID in the further tts. Even for the
     * exception above; the correct tags will be added and filled.
     *
     * @return true for success.
     */
    public static boolean enterStandardIDs(StarNode sn, String entry_id) {
        StringIntMap map = new StringIntMap();
        ArrayList sfList = sn.getSaveFrameList();
        for (int i = 0; i < sfList.size(); i++) {
            // General.showDebug("working on sf: " + i);
            SaveFrame sf = (SaveFrame) sfList.get(i);
            String sFcategory = sf.getCategory();
            int value = 1;
            if (map.containsKey(sFcategory)) {
                map.increment(sFcategory);
                value = map.getInt(sFcategory);
            } else {
                map.addString(sFcategory, value);
            }

            TagTable tTFirst = sf.getTagTable(StarGeneral.WILDCARD, false);
            if (tTFirst == null) {
                continue;
            }
            int aTagId = tTFirst.getColumnIdForRegExp("_.*"); // any tag name
            if (aTagId < 0) {
                General.showError("Failed to find any tag name in relation: " + tTFirst.toString());
                return false;
            }
            String aTagName = tTFirst.getColumnLabel(aTagId);
            // General.showDebug("working with first tT with a tagname: " +
            // aTagName );
            String[] tagNameSplit = splitTagName(aTagName); // Just splits on
            // the dot.
            if (tagNameSplit == null) {
                General.showError("Failed to find sf category or base tag name");
                return false;
            }
            String localTagNameEntryId = tagNameSplit[0] + ".Entry_ID";
            String tagNameListId = tagNameSplit[0] + ".ID";
            if (sFcategory.equals("entry_information")) {
                localTagNameEntryId = "_Entry.ID";
            }
            tTFirst.insertColumn(localTagNameEntryId);
            tTFirst.setValue(0, localTagNameEntryId, entry_id);
            if (!(sFcategory.equals("entry_information") || sFcategory.equals("chem_comp"))) {
                tTFirst.insertColumn(tagNameListId, Relation.DATA_TYPE_INT, null);
                tTFirst.setValue(0, tagNameListId, value);
            }

            ArrayList tTList = sf.getTagTableList(StarGeneral.WILDCARD);
            // Do on all but first tT.
            for (int j = 1; j < tTList.size(); j++) {
                // General.showDebug("working on tT: " + j);
                TagTable tTEcho = (TagTable) tTList.get(j);

                aTagId = tTEcho.getColumnIdForRegExp("_.*"); // any tag name
                if (aTagId < 0) {
                    General.showError("Failed to find any tag name in relation: " + tTEcho.toString());
                    return false;
                }
                aTagName = tTEcho.getColumnLabel(aTagId);
                String[] tagNameSplit2 = splitTagName(aTagName);
                if (tagNameSplit2 == null) {
                    General.showError("Failed to find sf category or base tag name");
                    return false;
                }

                String tagNameBaseEcho = tagNameSplit2[0];
                localTagNameEntryId = tagNameBaseEcho + ".Entry_ID"; // overloaded
                tTEcho.insertColumn(localTagNameEntryId);
                tTEcho.setValueByColumn(localTagNameEntryId, entry_id);

                tagNameListId = tagNameBaseEcho + "." + tagNameSplit[0].substring(1) + "_ID";// overloaded
                if (!(sFcategory.equals("entry_information") || sFcategory.equals("chem_comp"))) {
                    tTEcho.insertColumn(tagNameListId, Relation.DATA_TYPE_INT, null);
                    tTEcho.setValueByColumn(tagNameListId, value);
                }
            }
        }
        return true;
    }

    /**
     * @param label
     *            e.g. _Distance_constraint_list.Constraint_type
     * @return Constraint_type for e.g.
     */
    private static String[] splitTagName(String label) {
        if (label == null) {
            General.showError("Failed to getBaseTagName for null name");
            return null;
        }
        int idxDot = label.indexOf('.');
        if (idxDot < 0) {
            General.showError("Failed to getBaseTagName for name: [" + label + "]");
            return null;
        }
        return label.split("\\.");
    }
}
