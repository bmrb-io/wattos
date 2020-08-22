/*
 * PseudoAtom.java
 *
 * Created on February 27, 2004, 11:05 AM
 */

package Wattos.Soup;

import java.util.*;
import cern.colt.list.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.CloneWars.*;

/**
 *An object describing the collection of atoms commonly referred to as pseudo
 *atom in NMR spectroscopy.
 *The res/mol/model/entry attributes can be derived from the first atom's attributes.
 * @author Jurgen F. Doreleijers
 *@see Wattos.Soup.PseudoLib
 */
public class PseudoAtom implements Comparable {
    
    /** Eg for Gly HA2 and HA3 its QA */
    public String name;
    public int type;
    /** List of atom rids composing the pseudo atom. Eg for Gly QA its HA2 and HA3 */
    public int[] atomRids;
    Gumbo gumbo;
    
    /** Creates a new instance of PseudoAtom */
    public PseudoAtom(String name, int type, int atomRidFirst ) {
        this.name = name;
        this.type = type;
        int count = 1;
        if ( ! Defs.isNull( type ) ) {
            count = PseudoLib.PSEUDO_ATOM_ATOM_COUNT[ type];
        }
        atomRids = new int[ count ];
        atomRids[0] = atomRidFirst;
    }
    
    /** Use only after method findOtherAtoms has been executed successfuly.
     *Method allows this class to be sortable.
     */
    public int compareTo( Object obj) {
        PseudoAtom other = (PseudoAtom) obj;
        int compareStatus = gumbo.atom.compare(atomRids[0], other.atomRids[0], false, false);
        if ( compareStatus != 0 ) {
            return compareStatus;
        }
        return name.compareTo( other.name );        
    }
    
    /** Returns the residue rid of the first atom */
    public int getResRid() {
        return gumbo.atom.resId[atomRids[0]];   
    }
    
    /** Official method to find out if this class represents just one atom
     *or a bunch (pseudo).
     */
    public boolean isPseudo() {
        return ! Defs.isNull(type);
    }
    
    /** Given the first atomRid find the others in the Soup and include them into the int[].
     *Returns false if not all other atoms were found and shows a warning.
     */
    public boolean findOtherAtoms(UserInterface ui) {
        gumbo = ui.gumbo; // important for other checks.
        if ( ! isPseudo() ) {
            return true;
        }
        int atomRidFirst = atomRids[0];
        int resRid = gumbo.atom.resId[ atomRidFirst ];
        String resName = gumbo.res.nameList[ resRid ];
        //General.showDebug("Looking for other atoms in pseudo: " + name + " in residue: " + resRid + " (" + resName + ")");        
        int atomCountInPseudo = atomRids.length;
        ArrayList tmpObsAtomListInPseudo = (ArrayList) ui.wattosLib.pseudoLib.toAtoms.get( resName, name );                
        if ( tmpObsAtomListInPseudo == null ) {
            General.showError("Failed to find atoms for pseudo atom with name: " + name + " in residue with name: " + resName);
            return false;
        }
        /** Cache them for speed. */
        BitSet atomsInSameRes = SQLSelect.selectBitSet(ui.dbms, gumbo.atom.mainRelation, 
            Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RelationSet.RELATION_ID_COLUMN_NAME ], SQLSelect.OPERATION_TYPE_EQUALS, new Integer(resRid), false);
        if ( atomsInSameRes == null ) {
            General.showError("-1- Failed to find atoms in same residue with name: " + resName + " for res rid: " + resRid);
            return false;
        }
        if ( atomsInSameRes.nextSetBit(0) < 0 ) {
            General.showCodeBug("-2- Failed to find atoms in same residue with name: " + resName + " for res rid: " + resRid);
            General.showCodeBug("This happened even though SQLSelect.selectBitSet is supposed return null for that case" );
            return false;
        }
        int[] atomsInResList = PrimitiveArray.toIntArray( atomsInSameRes ); // makes it faster to scan.
        if ( atomsInResList == null ) {
            General.showError("Failed to do toIntArray for atoms in same residue with name: " + resName + " for rid: " + resRid);
            return false;
        }
        
        for (int i=0;i<atomCountInPseudo;i++) { // Usually 2 or 3
            // Check if it's the first atom in the definitions otherwise don't include it
            String atomInPseudo = (String) tmpObsAtomListInPseudo.get(i); // e.g. HB1
            // scan the list of atoms in specific residue
            int atomJRid = Defs.NULL_INT;
            for (int j=0;j<atomsInResList.length;j++) { // Usually 20. Caching just within the pseudo doesn't make sens.
                atomJRid = atomsInResList[j];
                String atomJName = ui.gumbo.atom.nameList[atomJRid]; // e.g. CA
                if ( atomJName.equals( atomInPseudo ) ) {
                    break; // found and signalled by atomJRid
                }
                atomJRid = Defs.NULL_INT;          
            }
            if ( Defs.isNull( atomJRid)) {
                StringArrayList sal = new StringArrayList();            
                for (int j=0;j<atomsInResList.length;j++) { // Usually 20. Caching just within the pseudo doesn't make sens.
                    sal.add(ui.gumbo.atom.nameList[atomsInResList[j]]);
                }
                General.showWarning("-0- Failed to find sibling: " + atomInPseudo + 
                        " for pseudoatom: " + name + " for residue: " + gumbo.res.toString(resRid)+
                        ". Atoms present in this residue are: " + Strings.toString(sal));
                return false;
            }
            atomRids[i] = atomJRid;                
        }        
        return true;
    }
    
    /** Fixed format for printing to e.g. the assignment table.
     *Name, Res name/number, type, atom rids.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append( "[" );
        sb.append( name );
        sb.append( ", " );
        if ( gumbo != null ) {
            int resRid = gumbo.atom.resId[ atomRids[0] ];
            String resName = gumbo.res.nameList[ resRid ];
            int resNumb = gumbo.res.number[ resRid ];        
            sb.append( resName );
            sb.append( " (" );
            sb.append( resNumb );
            sb.append( "), " );
        }
        if ( ! Defs.isNull( type )) {
            sb.append( PseudoLib.PSEUDO_ATOM_TYPES[ type ] );
        } else {
            sb.append( "null" );
        }
        sb.append( ", " );
        sb.append( PrimitiveArray.toString( atomRids ) );
        sb.append( "]" );
        return sb.toString();
    }
    
    
    /** remove any but the first of any pair like reduce the list of VAL MG1/MG2 to MG1.
     *Watch out; dcAtoms aren't dcAtoms
     *they're pointers into the combolist if isInComboList is true for that atom
     *or they're dc atom rids.
     *Selects the first atom in the molcule when a choice needs to be made. 
     *
     *E.g. Leu MD1,  MD2  -> MD1
     *     Asp HB2,  HB3  -> HB2
     *     Val HG11, HG23 -> HG11, HG23 because they aren't usually observable.
     *     Asx Xx         -> Xx because it isn't matched in structure
     */    
    public static boolean removeAllButOneOfStereoPairThatsStereoSpecific(IntArrayList dcAtoms, BooleanArrayList isInComboList, 
        ArrayList atomsObservableCombos, UserInterface ui) {        
        
        for (int i=1;i<dcAtoms.size();i++) { // few elements expected usually 1 or 2.
            int dcAtomId_A = dcAtoms.getQuick(i-1);
            int dcAtomId_B = dcAtoms.getQuick(i);
            boolean isInComboLis_A = isInComboList.getQuick(i-1);
            boolean isInComboLis_B = isInComboList.getQuick(i);
            if ( ! ( isInComboLis_A && isInComboLis_B )) {
                continue;
            }
            PseudoAtom ps_A = (PseudoAtom) atomsObservableCombos.get( dcAtomId_A );
            PseudoAtom ps_B = (PseudoAtom) atomsObservableCombos.get( dcAtomId_B );
            if ( (ps_A==null) || (ps_A==null)) {
                General.showError("Failed to get both (pseudo) atoms in observable list in removeAllButOneOfStereoPairThatsStereoSpecific");
                return false;
            }
//            String ps_name_A = ps_A.name;
//            String ps_name_B = ps_B.name;
            int atomRid_A = ps_A.atomRids[0];
            int atomRid_B = ps_B.atomRids[0];
            int resRid_A = ui.gumbo.atom.resId[ atomRid_A ];
            int resRid_B = ui.gumbo.atom.resId[ atomRid_B ];
            
            General.showDebug("Comparing (pseudo) atom A: " + ps_A.toString());
            General.showDebug("with      (pseudo) atom B: " + ps_B.toString());
            
            if ( resRid_A != resRid_B ) { // fast check for efficiency.
                continue;
            }
            String atomName_A = ui.gumbo.atom.nameList[ atomRid_A ];
            String atomName_B = ui.gumbo.atom.nameList[ atomRid_B ];            
            String res_name_A = ui.gumbo.res.nameList[ resRid_A ];
            ArrayList ps_nameList_A = (ArrayList) ui.wattosLib.pseudoLib.fromAtoms.get( res_name_A, atomName_A );
            ArrayList ps_nameList_B = (ArrayList) ui.wattosLib.pseudoLib.fromAtoms.get( res_name_A, atomName_B );
            if ( (ps_nameList_A==null) || (ps_nameList_B==null)) {
                General.showDebug("Failed to get both representing (pseudo) atoms in removeAllButOneOfStereoPairThatsStereoSpecific");
                continue;
            }
            General.showDebug("found: ps_nameList_A" + Strings.toString(ps_nameList_A) );
            General.showDebug("found: ps_nameList_B" + Strings.toString(ps_nameList_B) );
            
            String psFirstName_A = (String) ps_nameList_A.get(0); // E.g. LEU QD from HD11
            String psFirstName_B = (String) ps_nameList_B.get(0); // E.g. LEU QD from HD23
            General.showDebug("found: psFirstName_A" + psFirstName_A);
            General.showDebug("found: psFirstName_B" + psFirstName_B);
            
            /** ps_A and ps_B are always observable e.g. Leu MD1 and MD2 or Asp HB2 and HB3 and 
             *they both have a common pseudoatom parent Leu QD and Asp QB
             
            String commonPsAtomName = ui.wattosLib.pseudoLib.getCommonPseudoParent( res_name_A, res_name_B, atomName_A, atomName_B );
            
            if ( commonPsAtomName==null ) {
                continue;
            }
             */
            
            if ( psFirstName_A.equals( psFirstName_B ) ) {
                General.showDebug("Deleting (pseudo) atom B: " + ps_B.toString());
                dcAtoms.remove(i);
                isInComboList.remove(i);                
                i--; // try the next one which now has the same index
            }
        }
        return true;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PseudoAtom pa = new PseudoAtom( "QD", 5, 99 );
        General.showOutput("Pseudo atom: " + pa );
    }    
    
}
