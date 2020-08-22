/*
 * Created on August 27, 2003, 3:04 PM
 */

package Wattos.Soup;

import java.util.*;
import java.io.*;
import java.net.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.Star.*;
import Wattos.CloneWars.*;
import cern.colt.list.*;

/**
 * Contains pseudo-atom definitions
 *
 * @author Jurgen F. Doreleijers
 */
public class PseudoLib implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    /** Local resource */
    static final String STR_FILE_LOCATION = "Data/PseudoLib.str";

    public static final int DEFAULT_PSEUDO_ATOM_ID_UNDEFINED             = 0;
    public static final int DEFAULT_PSEUDO_ATOM_ID_CH2_OR_NH2            = 1;
    public static final int DEFAULT_PSEUDO_ATOM_ID_METHYL                = 2;
    public static final int DEFAULT_PSEUDO_ATOM_ID_TWO_NH2_OR_CH2        = 3;
    public static final int DEFAULT_PSEUDO_ATOM_ID_TWO_METHYL            = 4;
    public static final int DEFAULT_PSEUDO_ATOM_ID_AROMAT_2H             = 5;
    public static final int DEFAULT_PSEUDO_ATOM_ID_AROMAT_4H             = 6;

    /** Store how many atoms the pseudo atom represents */
    public static int[] PSEUDO_ATOM_ATOM_COUNT = { 0, 2, 3, 4, 6, 2, 4 };

    /** Needs to correspond to above definitions. */
    public static String[] PSEUDO_ATOM_TYPES = {
        "undefined",
        "CH2 or NH2",
        "methyl",
        "two NH2 or CH2",
        "two methyl",
        "aromat 2H",
        "atomat 4H"
    };

    public static final int DEFAULT_OK                    = 0;
    public static final int DEFAULT_REPLACED_BY_PSEUDO    = 1;
    public static final int DEFAULT_REDUNDANT_BY_PSEUDO   = 2;

    /** A map that's fast to do lookups like:<BR>
     *Give me all atom names constituting QD in residue PHE: HD1 and HD2
     *and the other way around:
     *Give me the pseudo atom names in resiude PHE that includes HD1: QD, QR.
     *Note that an atom can be in more than one pseudo atom. Use the attribute
     *pseudoAtomType to figure out which you want. They map to the default pseudo atom
     *ids defined above.
     */

    /** Maps to ArrayList of pseudo atom names. They are ordered with respect to
     *the number of regular atoms they represent.
     *A descending order by the number of atoms they represent. For PHE HD1 it would
     *[QR, QD].
     */
    public HashOfHashes fromAtoms;
    /** Maps to ArrayList of atom names */
    public HashOfHashes toAtoms;
    /** Maps to ArrayList of integer pseudo atom types*/
    public HashOfHashes pseudoAtomType;


    public PseudoLib() {
        init();
    }

    public boolean init() {
        fromAtoms       = new HashOfHashes();
        toAtoms         = new HashOfHashes();
        pseudoAtomType  = new HashOfHashes();
        return true;
    }

    public boolean readStarFile( URL url) {
        if ( url == null ) {
            url = getClass().getResource(STR_FILE_LOCATION);
        }
        DBMS dbms_local = new DBMS(); // Create a local copy so anything can be read in.
        StarFileReader sfr = new StarFileReader(dbms_local);
//        long start = System.currentTimeMillis();
        StarNode sn = sfr.parse( url );
//        long taken = System.currentTimeMillis() - start;
        //General.showOutput("STARLexer: " + taken + "(" + (taken/1000.0) + " sec)" );
        if ( sn == null ) {
            General.showError("parse unsuccessful");
            return false;
        }
        //General.showDebug("Parse successful");
        //Wattos.Utils.General.showDebug("DBMS: " + dbms_local.toString( true ));
        Object o_tmp_1 = sn.get(0);
        if ( !(o_tmp_1 instanceof DataBlock)) {
            General.showError("Expected top level object of type DataBlock but got: " + o_tmp_1.getClass().getName());
            return false;
        }
        DataBlock db = (DataBlock) o_tmp_1;

        SaveFrame sf = db.getSaveFrameByCategory( "pseudo_atom_lib", true );
        if ( sf == null ) {
            General.showError("Failed to find SaveFrame with category pseudo_atom_lib");
            return false;
        }
//        String name = sf.title;
        //General.showDebug("Found saveframe with name: " + name);

        ArrayList result = sf.getTagTableList("_Comp_ID");
        if ( result == null || (result.size() != 1) ) {
            if ( result == null ) {
                General.showWarning("Found number of loops: null");
            } else {
                General.showWarning("Found number of loops: " + result.size());
            }
            General.showError("Expected one loop defining the pseudo atom mapping but didn't found one.");
            return false;
        }
        Object o_tmp_3 = result.get(0);

        if ( !(o_tmp_3 instanceof TagTable)) {
            General.showError("Expected object of type TagTable as second node in saveframe but got type: " + o_tmp_3.getClass().getName());
            return false;
        }
        TagTable tT = (TagTable) o_tmp_3;
        String[] resName        = tT.getColumnString("_Comp_ID");
        String[] pseudoAtomName = tT.getColumnString("_Pseudo_atom_ID");
        String[] pseudoAtomt    = tT.getColumnString("_Pseudo_atom_type");
        String[] atomName_1     = tT.getColumnString("_Atom_ID_1");
        String[] atomName_2     = tT.getColumnString("_Atom_ID_2");
        String[] atomName_3     = tT.getColumnString("_Atom_ID_3");
        String[] atomName_4     = tT.getColumnString("_Atom_ID_4");
        String[] atomName_5     = tT.getColumnString("_Atom_ID_5");
        String[] atomName_6     = tT.getColumnString("_Atom_ID_6");

        if (    resName        == null ||
                pseudoAtomName == null ||
                pseudoAtomt    == null ||
                atomName_1     == null ||
                atomName_2     == null ||
                atomName_3     == null ||
                atomName_4     == null ||
                atomName_5     == null ||
                atomName_6     == null                 ) {
            General.showError("Failed to find String[] columns in tagtable as expected.");
            return false;
        }
        String[][] atomNames = {
            atomName_1,
            atomName_2,
            atomName_3,
            atomName_4,
            atomName_5,
            atomName_6 };

        //General.showDebug("Will read number of pseudo atom defs: " + tT.used.cardinality());
        for (int i=tT.used.nextSetBit(0); i>=0; i=tT.used.nextSetBit(i+1)) {
            //General.showDebug("Processing " + resName[i] + " " +  pseudoAtomName[i] + " " +  pseudoAtomt[i] );
            int pseudoAtomTypeA = (new Integer( pseudoAtomt[i])).intValue();
            int atomsRepresentedCountA = PSEUDO_ATOM_ATOM_COUNT[ pseudoAtomTypeA ];
            pseudoAtomType.put( resName[i], pseudoAtomName[i], new Integer( pseudoAtomTypeA ));
            ArrayList atomList = new ArrayList();
            toAtoms.put(        resName[i], pseudoAtomName[i], atomList);
            for ( int a=0;a<atomNames.length;a++ ) {
                //General.showDebug("Processing atom: " + atomNames[a][i] );
                if ( Defs.isNullString(atomNames[a][i])) {
                    break;
                }
                atomList.add( atomNames[a][i] );
            }
            for ( int a=0;a<atomNames.length;a++ ) {
                if ( Defs.isNullString(atomNames[a][i] )) {
                    break;
                }
                ArrayList fromListPseudo = (ArrayList) fromAtoms.get( resName[i], atomNames[a][i]);
                if ( fromListPseudo == null ) {
                    fromListPseudo = new ArrayList();
                    fromAtoms.put(      resName[i], atomNames[a][i],    fromListPseudo);
                }
                // Insert the new pseudo atom name to the appropriate position
                // they are to be in descending order by the number of atoms they represent
                int b=0;
                for ( ;b<fromListPseudo.size()-1;b++ ) {
                    int pseudoAtomTypeB = ((Integer)pseudoAtomType.get( resName[i], fromListPseudo.get(b))).intValue();
                    int atomsRepresentedCountB = PSEUDO_ATOM_ATOM_COUNT[ pseudoAtomTypeB ];
                    if ( atomsRepresentedCountA > atomsRepresentedCountB ) {
                        break;
                    }
                }
                fromListPseudo.add(b, pseudoAtomName[i] );
            }
        }
        return true;
    }

    /** Convenience method. Returns null in case the residue names differ or
     no common parent is defined.
     */
    public String getCommonPseudoParent( String res_name_A, String res_name_B, String atomName_A, String atomName_B ) {
        if ( ! res_name_A.equals(res_name_B)) {
            //General.showDebug("residues in getCommonPseudoParent aren't the same: " + res_name_A + " and " + res_name_B);
            return null;
        }
        return getCommonPseudoParent( res_name_A, new String[] { atomName_A, atomName_B }, true );
    }

    /** checks to see if all atoms defined by the pseudo are in the list.
     *Returns false on error.
     */
    public boolean containsAllAtomsOfPseudo( int resRid, String pseudoName,
            IntArrayList atomRidList, UserInterface ui) {
        BitSet presence = new BitSet();
        ArrayList atomNames = (ArrayList) toAtoms.get(ui.gumbo.res.nameList[ resRid ],pseudoName);
        Atom atom = ui.gumbo.atom;
        for ( int i=0;i<atomRidList.size();i++) {
            int atomRid = atomRidList.getQuick(i);
            if (atom.resId[atomRid] != resRid ) { // skip the atoms outside the residue.
                continue;
            }
            int idx = atomNames.indexOf( atom.nameList[atomRid]);
            if ( idx >= 0 ) {
                presence.set(idx);
            }
        }
        if ( presence.cardinality() == atomNames.size() ) {
            return true;
        }
        return false;
    }

    /** Returns the name of the common pseudo atom or null to indicate there was no such atom
     *in the lib. If multiple common pseudo atoms exist then the most specific one to this
     *pair will be returned.
     *
     *E.g. Leu HD12, HD13, HA will return MD1.
     *E.g. Leu HD12, HD13, HD21 will return QD.
     *Notes:    don't use pseudo atom names in the variable: atom_name_list
     *          Assumption in this code is that no atoms are listed twice.
     *          The atom for which the pseudo atom is to be found MUST be listed
     * first in the list.
     *
    public String getCommonPseudoParent( String res_name, String[] atom_name_list,
            boolean requireAllPresent , boolean allowUnrelatedAtoms) {
        StringArrayList psList = new StringArrayList();
        for (int i=0;i<atom_name_list.length;i++) {
            ArrayList ps = (ArrayList) fromAtoms.get(res_name, atom_name_list[i]);
            if ( ps == null) {
                General.showDebug("Failed to get any ps for res name: " + res_name + " and atom name: " + atom_name_list[i]);
                return null; // none common
            }
            StringArrayList pseudos = new StringArrayList( ps );
            //General.showDebug( "intersecting with pseudos: " + pseudos.toString());
            if ( psList.size() == 0 ) {
                psList = pseudos; // first round
            } else {
                psList = psList.intersection( pseudos );
            }
            //General.showDebug( "psList: " + psList.toString());
            if ( psList.size() == 0 ) {
                //General.showDebug("No more in common");
                return null;
            }
        }
        // At this point we're left with maximum two (depending on the number of levels used in the lib (currently 2)
        // The element with the highest index has the fewest atoms representing it so is
        // the most specific.
        String psName = psList.getString(psList.size()-1);
        if ( ! requireAllPresent ) {
            return psName;
        }
        int psType = ((Integer)pseudoAtomType.get( res_name, psName )).intValue();
        int psAtomCount = PSEUDO_ATOM_ATOM_COUNT[ psType ];
        if ( atom_name_list.length == psAtomCount ) {
            return psName;
        }
        //General.showDebug("Failed to find the right number of atoms for parent");
        return null;
    }
     */


    /** Convenience method.
     *This method will filter out any atom which is in the list but is not in the same residue or
     *even not in a shared pseudo atom with the given atom.
     *The pseudoparent of the atom for which the id in the atomsInMemberList is
     *given will be searched. This is not necessarily the first in the list.
     *This code will filter out atoms that are listed twice.
     * @see #getCommonPseudoParent(String,String[],boolean)
     */
    public String getCommonPseudoParent( int atomIdInList,
            IntArrayList atomsInMemberList,
            UserInterface ui,
            boolean requireAllPresent) {
//        General.showDebug("Doing getCommonPseudoParent for atoms:\n" +
//                ui.gumbo.atom.toString(PrimitiveArray.toBitSet(atomsInMemberList)));
        int atomsInMemberListSize = atomsInMemberList.size();
        if ( (atomsInMemberList == null ) || (atomsInMemberListSize < 1) ) {
            General.showError("Failed to getPseudoParent because no  atomsInMemberList");
            return null;
        }
        if ( (atomIdInList < 0 ) || (atomIdInList >= atomsInMemberListSize)) {
            General.showError("Failed to getPseudoParent because invalid atomIdInList: " + atomIdInList);
            return null;
        }
        // Store for later use:
        int atomRid = atomsInMemberList.getQuick(atomIdInList);
        // Clone it before mutilation.
        atomsInMemberList = (IntArrayList) atomsInMemberList.clone();
        if (!PrimitiveArray.removeDuplicatesBySort(atomsInMemberList)) {
            General.showError("Failed to PrimitiveArray.removeDuplicatesBySort");
            return null;
        }
        int idxOfOriginalAtomRid = atomsInMemberList.indexOf(atomRid);
        if ( idxOfOriginalAtomRid < 0) {
            General.showError("Failed to indexOf for idxOfOriginalAtomRid");
            return null;
        }
        if ( idxOfOriginalAtomRid != 0 ) {
            if ( ! PrimitiveArray.swap( atomsInMemberList, 0, idxOfOriginalAtomRid)) {
                General.showError("Failed to getPseudoParent because failed to swap elements to first position.");
                return null;
            }
        }
        Atom atom = ui.gumbo.atom;
        int resRid = atom.resId[atomRid];
        String resName = ui.gumbo.res.nameList[ resRid ];
        String atomName = atom.nameList[ atomRid ];
        ArrayList ps = (ArrayList) fromAtoms.get(resName, atomName);
        if ( ps == null) {
            General.showDebug("Failed to get any ps for res name: " + resName + " and atom name: " + atomName);
            return null; // none common
        }
        StringArrayList pseudos = new StringArrayList( ps );


        // Make certain they're all in the same residue and share 1 or 2 pseudos with the first atom.
        IntArrayList atomsInMemberListSamePseudo = new IntArrayList(atomsInMemberListSize);
        for (int i=0;i<atomsInMemberListSize;i++) {
            int atomRidFiltered = atomsInMemberList.getQuick(i);
            if ( resRid != atom.resId[atomRidFiltered ] ) {
                continue;
            }
            String atomNameSameRes = atom.nameList[ atomRidFiltered ];
            ArrayList psSameRes = (ArrayList) fromAtoms.get(resName, atomNameSameRes);
            if ( psSameRes == null ) {
                continue; // no common pseudo
            }
            StringArrayList pseudosSameRes = new StringArrayList( psSameRes );
            pseudosSameRes = pseudosSameRes.intersection( pseudos );
            if ( pseudosSameRes == null ) {
                General.showError("Failed to take intersection.");
                return null;
            }
            if ( pseudosSameRes.size() < 1 ) {
                continue;  // no common pseudo with this atom.
            }
            atomsInMemberListSamePseudo.add(atomRidFiltered);
        }
        return getCommonPseudoParent( atomsInMemberListSamePseudo, ui, requireAllPresent );
    }

    /** Convenience method to go from atom relation to simply the atom
     *names.
     */
    public String getCommonPseudoParent(
            IntArrayList atomsRids,
            UserInterface ui,
            boolean requireAllPresent ) {
        String[] atomNames = new String[atomsRids.size()];
        for (int i=(atomNames.length-1);i>=0;i--) {
            atomNames[i] = ui.gumbo.atom.nameList[ atomsRids.get(i)];
        }
        int atomRid = atomsRids.get(0);
        int resRid = ui.gumbo.atom.resId[atomRid];
        String resName = ui.gumbo.res.nameList[ resRid ];
        return getCommonPseudoParent( resName, atomNames, requireAllPresent );
    }

    /** Convenience method */
    public String getCommonPseudoParent(
            String resName,
            String[] atomsInMemberList,
            boolean requireAllPresent) {
        return getCommonPseudoParent(
            resName,
            atomsInMemberList,
            requireAllPresent,
            true );
    }

    /** The caller makes sure the atom list contains no atoms other than related
     *to the first in a common pseudo atom.
     *<BR>
     *E.g. Leu HD12, HD13                will return Defs.EMPTY_STRING  or MD1 (depending on requireAllPresent).
     *<BR>
     *E.g. Leu HD21, HD12 and HD13       will return Defs.EMPTY_STRING  or QD (depending on requireAllPresent).
     *<BR>
     *E.g. Leu HD11, HD12, HD13 and HD21 will return Defs.EMPTY_STRING  or QD (depending on requireAllPresent).
     *<BR>
     *E.g. Leu HD11,     , HD13 and HD21 will return Defs.EMPTY_STRING  or QD (depending on requireAllPresent).
     *<BR>
     *E.g. Leu HB2, HB3 will return QB.
     *E.g. Leu HB2, HG will return null (error). If showWarning is set then a message is shown.
     *E.g. Asn HA will return Defs.EMPTY_STRING.
     */
    public String getCommonPseudoParent(
            String resName,
            String[] atomsInMemberList,
            boolean requireAllPresent,
            boolean showWarning ) {

        int atomsInMemberListSize = atomsInMemberList.length;

        String atomName = atomsInMemberList[ 0 ];
        ArrayList ps = (ArrayList) fromAtoms.get(resName, atomName);
        if ( ps == null) {
            //General.showError("In getCommonPseudoParent: Failed to get any ps for res name: " + resName + " and atom name: " + atomName);
            return Defs.EMPTY_STRING; // none common
        }
        StringArrayList pseudos = new StringArrayList( ps );
        //General.showDebug("Considering pseudos: " + ps);

        // Find one or two common pseudos
        for (int i=1;i<atomsInMemberListSize;i++) {
            String atomNameOther = atomsInMemberList[ i ];
            ArrayList psOther = (ArrayList) fromAtoms.get(resName, atomNameOther);
            if ( psOther == null) {
                if ( showWarning ) {
                    General.showWarning("in getCommonPseudoParent: Failed to get any ps for res name: " + resName + " and atom name: " + atomNameOther);
                    General.showWarning("   that is looking for common pseudo parent for atoms with names: " + Strings.toString(atomsInMemberList));
                    General.showWarning("   based on first atom found pseudo candidates: " + Strings.toString(ps));
                }
                return null;
            }
            StringArrayList pseudosOther = new StringArrayList( psOther );
            pseudos = pseudos.intersection(pseudosOther);
            if ( pseudos.size() < 1 ) {
                //General.showDebug("in getCommonPseudoParent: no common pseudo for residue: " + resName + " and atom names: " + atomName + " and: " + atomNameOther);
                return Defs.EMPTY_STRING;
            }
        }

        // Most specific pseudo is last in list (E.g. Val QG, MG1)
        String psAtomName = pseudos.getString( pseudos.size() -1 );
        if ( ! requireAllPresent ) {
            //General.showDebug("Without checking presence of all constituents, returning most specific pseudo in common:"  + psAtomName );
            return psAtomName;
        }

        int pseudoTypeAtomCount = getAtomCountPseudo( resName, psAtomName);
        if ( pseudoTypeAtomCount < 0 ) {
            General.showError("Failed to get pseudoTypeAtomCount for: " + resName + " and: " + psAtomName);
            return null;
        }

        if ( atomsInMemberListSize == pseudoTypeAtomCount ) {
            return psAtomName;
        }

        //General.showDebug("Not all atoms are present. Expected: " + pseudoTypeAtomCount + " but had: " + atomsInMemberListSize);
        return Defs.EMPTY_STRING;
    }

    /** If given pseudo is unknown then the return value will be -1
     */
    public int getAtomCountPseudo(String resName, String pseudoName) {
        Integer pseudoTypeInteger = (Integer) pseudoAtomType.get( resName, pseudoName );
        if ( pseudoTypeInteger == null ) {
            General.showWarning("In getCommonPseudoParent: Found unknown pseudoAtomName: " + pseudoName +
                    " for residue: " + resName);
            return -1;
        }
        int pseudoType = pseudoTypeInteger.intValue();
        return PSEUDO_ATOM_ATOM_COUNT[ pseudoType ];
    }
    /** TODO make sure the hasSibling returns true for pseudo atoms like VAL MG1 etc.
     */
    public boolean hasSibling( String resName, String atomName) {
        //General.showDebug("Checking to see if atom: " + atomName + " in res: " + resName + " has a sibling");
        Object out = fromAtoms.get( resName, atomName);
        return (out != null);
    }

    /** Returns the name of the sibling of an atom with respect to the pseudo given.
     *<PRE>
     *E.g. VAL QG  the sibling of HG11 is HG21.
     *E.g. VAL MG1 the sibling of HG11 is null. A warning will be issued.
     *E.g. VAL QD  the sibling of HD1  is HD2.
     *</PRE>
     */
    public String getStereoSibling( String resName, String atomName, String pseudoAtomName) {
        Integer pseudoTypeInteger = (Integer) pseudoAtomType.get( resName, pseudoAtomName );
        if ( pseudoTypeInteger == null ) {
            General.showWarning("In getStereoSibling found unknown pseudoAtomName: " + pseudoAtomName +
                    " for residue: " + resName);
            return null;
        }
        int pseudoType = pseudoTypeInteger.intValue();
        if (!((pseudoType == PseudoLib.DEFAULT_PSEUDO_ATOM_ID_CH2_OR_NH2 )||
              (pseudoType == PseudoLib.DEFAULT_PSEUDO_ATOM_ID_AROMAT_2H )||
              (pseudoType == PseudoLib.DEFAULT_PSEUDO_ATOM_ID_TWO_METHYL ))) {
            General.showWarning("In getStereoSibling found invalid pseudoAtomName: " + pseudoAtomName +
                    " for residue: " + resName);
            return null;
        }
        ArrayList atomNameList = (ArrayList) toAtoms.get( resName, pseudoAtomName );
        if ( atomNameList == null ) {
            General.showCodeBug("In getStereoSibling found unknown pseudoAtomName: " + pseudoAtomName +
                    " for residue: " + resName);
            return null;
        }
        int idxatomName = atomNameList.indexOf(atomName);
        if ( idxatomName < 0 ) {
            General.showCodeBug("In getStereoSibling found atom not in pseudoAtom: " + pseudoAtomName +
                    " for residue: " + resName + " and atom: " + atomName);
            return null;
        }
        int idxatomNameSibling = idxatomName;
        switch ( pseudoType ) {
            case PseudoLib.DEFAULT_PSEUDO_ATOM_ID_CH2_OR_NH2: {
                idxatomNameSibling = 1;
                if ( idxatomName == 1 ) {
                    idxatomNameSibling = 0;
                }
                break;
            }
            case PseudoLib.DEFAULT_PSEUDO_ATOM_ID_AROMAT_2H: {
                idxatomNameSibling = 1;
                if ( idxatomName == 1 ) {
                    idxatomNameSibling = 0;
                }
                break;
            }
            case PseudoLib.DEFAULT_PSEUDO_ATOM_ID_TWO_METHYL: {
                idxatomNameSibling = idxatomName+3;
                if ( idxatomName > 2  ) {
                    idxatomNameSibling = idxatomName-3;
                }
                break;
            }
        }
        return (String) atomNameList.get(idxatomNameSibling);
    }
}
