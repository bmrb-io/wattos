/*
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 * @version 0
 */
package Wattos.Converters.Common;

import java.util.jar.Attributes;
import java.util.*;
import Wattos.Converters.Common.Varia;
import Wattos.Utils.*;

/**Class is used to store info on 1 atom.
 */
public class AtomNode implements Cloneable {

    public static int SEGI_POS = 0;
    public static int RESI_POS = 1;
    public static int RESN_POS = 2;
    public static int NAME_POS = 3;
    public static String[] variable_names_atom = {
           "segi",
           "resi",
           "resn",
           "name"
    };

    public static AtomNodeComparator comparator = new AtomNodeComparator();

    public Attributes info;

    /**Construct an empty LogicalNode
     */
    public AtomNode() {
        info    = new Attributes();
        init();
    }

    public boolean init() {
        return init( Wattos.Utils.NmrStar.STAR_EMPTY );
    }

    public boolean init( String val ) {
        for (int i = 0; i < variable_names_atom.length; i++) {
            info.putValue(variable_names_atom[i], val);
        }
        return true;
    }

    public String toString() {

        String result = "";
        for (int i = 0; i < variable_names_atom.length; i++) {
            result = result + variable_names_atom[i] + ": " + info.getValue( variable_names_atom[i] ) + " ";
        }
        return result;
    }

    /** Call toString on a list of atom nodes
     */
    public static String toString( ArrayList atom_nodes ) {

        String t = "List of atoms is: [\n";
        for (int i = 0; i < atom_nodes.size(); i++) {
            t = t + (AtomNode) atom_nodes.get(i) + General.eol;
        }
        return t + "]\n";
    }

    /**
     * @param atom_1 atom 1
     * @param atom_2 atom 2
     * @return -1, 0, or 1 indicating order
     */
    public static int compare(AtomNode atom_1, AtomNode atom_2) {

        int comparison=0;

        for (int i = 0; i < variable_names_atom.length; i++) {
            String val_atom_1 = atom_1.info.getValue( variable_names_atom[i] );
            String val_atom_2 = atom_2.info.getValue( variable_names_atom[i] );
            // Try a numerical (integer) comparison
            if ( i == 1 ) {
                try {
                    int val_atom_1_int = Integer.parseInt(val_atom_1);
                    int val_atom_2_int = Integer.parseInt(val_atom_2);
                    if ( val_atom_1_int == val_atom_2_int ) {
                        comparison = 0;
                    } else if ( val_atom_1_int > val_atom_2_int ) {
                        comparison = 1;
                    } else {
                        comparison = -1;
                    }
                } catch ( NumberFormatException e ) {
                    //General.showWarning("NumberFormatException\n" + e.toString() );
                    comparison = val_atom_1.compareToIgnoreCase(val_atom_2);
                }
            } else {
                comparison = val_atom_1.compareToIgnoreCase(val_atom_2);
            }
            if ( comparison != 0 ) {
                return comparison;
            }
        }
        return 0;
    }

    /** Checks to see if the atoms are related and if so, which one is the more
     * restrictive of the two. E.g. (A,1) is more restrictive than (A,.).
     * E.g. (A,1) is unrelated to (.,2).
     * @param atom_1 First atom
     * @param atom_2 Second atom
     * @return 0 if the atoms are unrelated or if they are exactly the same.
     * 1 if first atom is more allowing
     * 2 if second atom is more allowing
     */
    public static int compareAllowance(AtomNode atom_1, AtomNode atom_2) {

        int more_allowing_atom = 0; // Unrelated or the same until proven guilty.

        for (int i = 0; i < variable_names_atom.length; i++) {

            String val_atom_1 = atom_1.info.getValue( variable_names_atom[i] );
            String val_atom_2 = atom_2.info.getValue( variable_names_atom[i] );

            boolean val_atom_1_is_dot = val_atom_1.equals(Wattos.Utils.NmrStar.STAR_EMPTY);
            boolean val_atom_2_is_dot = val_atom_2.equals(Wattos.Utils.NmrStar.STAR_EMPTY);
            if ( val_atom_1_is_dot && val_atom_2_is_dot ) {
                continue; // Check next attribute.
            }

            // Only atom 1 is dot
            if ( val_atom_1_is_dot ) {
                if ( more_allowing_atom == 1 || more_allowing_atom == 0 ) {
                    more_allowing_atom = 1;
                } else {
                    return 0; // more_allowing_atom has to be on all attributes.
                }
                continue;
            }
            // Only atom 2 is dot
            if ( val_atom_2_is_dot ) {
                if ( more_allowing_atom == 2 || more_allowing_atom == 0 ) {
                    more_allowing_atom = 2;
                } else {
                    return 0; // more_allowing_atom has to be on all attributes.
                }
                continue;
            }

            // Both are not dots, if they're not the same the atoms are unrelated.
            if (  ! val_atom_1.equalsIgnoreCase(val_atom_2) ) {
                return 0;
            }
        }
        return more_allowing_atom;
    }

    /** Remove atom nodes from the list that are contained by
     *each other selections. E.g. [(A,2),(.,2)] -> [(.,2)] because includes (A,2)
     */
    public static ArrayList removeRedundant( ArrayList atom_nodes ) {

        // Sort to simplify the search.
        Collections.sort(atom_nodes, comparator );

        // Remove only exactly identical ones
        for (int i = atom_nodes.size()-1; i > 0; i--) {
            AtomNode atom_1 = (AtomNode) atom_nodes.get(i);
            AtomNode atom_2 = (AtomNode) atom_nodes.get(i-1);
            if ( compare( atom_1, atom_2 ) == 0 ) {
                //General.showOutput("Removed identical atom node: " + atom_1 );
                atom_nodes.remove(i);
            }
        }

        // Remove those that are included by others..
        for (int i = atom_nodes.size()-1; i > 0; i--) {
            AtomNode atom_1 = (AtomNode) atom_nodes.get(i);
            AtomNode atom_2 = (AtomNode) atom_nodes.get(i-1);
            int allowance = compareAllowance( atom_1, atom_2 );
            if ( allowance != 0 ) {
                if ( allowance == 1 ) {
                    //General.showOutput("Removed more restrictive atom node: " + atom_2 );
                    atom_nodes.remove(i-1);
                    i--; // Notion of cursor changes too.
                } else {
                    //General.showOutput("Removed more restrictive atom node: " + atom_1 );
                    atom_nodes.remove(i);
                }
            }
        }
        return atom_nodes;
    }

    /** Do the logical binary operation between the individual atoms.
     * E.g.
     * <PRE>
     * For the AND operator:
     * Input name: Result
     * a_1 a_2
     * A   A       [(A)]
     * A   B       []
     *
     * For the OR operator:
     * Input name: Result
     * a_1 a_2
     * A   A       [(A)] not two occurrences of A!
     * A   B       [(A),(B)]
     *
     *
     * For the OR operator:
     * Input name: Input resi Result
     * a_1 a_2     a_1 a_2
     * A   A       1   1      [(A,1)] not two occurrences of (A,1)!
     * A   B       1   1      [(A,1),(B,1)]
     * A   B       1   2      [(A,1),(B,1)]
     * A   B       1   2      [(A,1),(B,2)]
     * </PRE>
     * @param operation_type AND or OR
     * @param a_1 atom 1
     * @param a_2 atom 2
     * @return List of atoms. In case of the AND operator this can be only 1 atom at most.
     */
    public static ArrayList combineAtom(int operation_type, AtomNode a_1, AtomNode a_2) {
        ArrayList result = new ArrayList();

        // Most frequent case first:
        if ( operation_type == Varia.OPERATION_TYPE_AND ) {

            // Can only return 1 atom at most.
            AtomNode a_result =  new AtomNode();
            for ( int i=0;i<AtomNode.variable_names_atom.length;i++ ) {
                String val_1 = a_1.info.getValue(AtomNode.variable_names_atom[i]);
                String val_2 = a_2.info.getValue(AtomNode.variable_names_atom[i]);
                // Most frequent case first: (both ".")
                if ( val_1.equals(val_2) ) {
                    a_result.info.putValue( AtomNode.variable_names_atom[i], val_1 );
                } else if ( val_1.equals( Wattos.Utils.NmrStar.STAR_EMPTY ) ) {
                    a_result.info.putValue( AtomNode.variable_names_atom[i], val_2 );
                } else if ( val_2.equals( Wattos.Utils.NmrStar.STAR_EMPTY ) ) {
                    a_result.info.putValue( AtomNode.variable_names_atom[i], val_1 );
                } else {
                    // Return empty list because atoms differ for this property.
                    return result;
                }
            }
            result.add( a_result );
        } else if ( operation_type == Varia.OPERATION_TYPE_OR ) {
            result.add( a_1 );
            result.add( a_2 );
        }
        return result;
    }

    /** Do the logical binary operation between the lists of atoms.
     * E.g.
     * <PRE>
     * For the AND operator:
     * [] indicates list.
     * () indicates set of attributes for each atom (e.g. name and segi).
     * sel_1                sel_2              result
     * [(A,1)]              [(A,1)]            [(A,1)] not two occurances of (A,1)
     * [(A,1),(B,1)]        [(A,1)]            [(A,1)]
     * [(A,1),(B,1)]        [(A,1),(B,1)]      [(A,1),(B,1)]
     * [(A,1),(B,1)]        [(A,1),(B,2)]      [(A,1)]
     * [(A,1),(.,1)]        [(A,1),(.,2)]      [(A,1)] not two occurances of (A,1) from (.,1) AND (A,1)
     * [(A,1),(.,2)]        [(A,2),(.,2)]      [(A,2),(.,2)] -> [(.,2)] because includes (A,2)
     * </PRE>
     * @param operation_type AND or OR
     * @param sel_1 List of atomnodes.
     * @param sel_2 List of atomnodes.
     * @return List of atomnodes after combination.
     */
    public static ArrayList combineAtomList(int operation_type, ArrayList sel_1, ArrayList sel_2) {
        ArrayList result = new ArrayList();

        // Before this might blow up see if we can get the redundant ones out.
        sel_1 = AtomNode.removeRedundant( sel_1 );
        sel_2 = AtomNode.removeRedundant( sel_2 );
        // Most frequent case first:
        if ( operation_type == Varia.OPERATION_TYPE_AND ) {
            for ( int i=0;i<sel_1.size();i++ ) {
                for ( int j=0;j<sel_2.size();j++ ) {
                    result.addAll( combineAtom( operation_type,
                    (AtomNode) sel_1.get(i),
                    (AtomNode) sel_2.get(j) ) );
                }
            }
        } else if ( operation_type == Varia.OPERATION_TYPE_OR ) {
            result.addAll(sel_1);
            result.addAll(sel_2);
        } else {
            General.showError("unsupported operation type: " + operation_type );
        }
        result = AtomNode.removeRedundant( result );
        return result;
    }


    /** First attempt at writing some cloning method. Taken from:
     * "The Java Programming Launguage, Third Edition" by Arnold, Gosling,
     * and Holmes.
     * @return  cloned object */
    public Object clone() {
        try {
            // This call has to be first command
            AtomNode nObj = (AtomNode) super.clone();
            /** Can't use the clone method of Attributes because it is shallow.*/
            nObj.info = (Attributes) info.clone();
            return nObj;
        } catch (CloneNotSupportedException e) {
            // Cannot happen -- we support clone, and so do Attributes
            throw new InternalError(e.toString());
        }
    }

    public static void main(String[] args) {

        AtomNode atom_node_1 = new AtomNode();
        AtomNode atom_node_2 = new AtomNode();
        AtomNode atom_node_3 = new AtomNode();
        AtomNode atom_node_4 = new AtomNode();
        AtomNode atom_node_5 = new AtomNode();

//        if ( false ) {
//            atom_node_1.info.putValue("resi", "1");
//            atom_node_1.info.putValue("name", "a");
//
//            atom_node_2.info.putValue("resi", "2");
//            atom_node_2.info.putValue("name", ".");
//
//            atom_node_3.info.putValue("resi", "1");
//            atom_node_3.info.putValue("name", ".");
//
//            atom_node_4.info.putValue("resi", "1");
//            atom_node_4.info.putValue("name", "a");
//
//            atom_node_5.info.putValue("resi", ".");
//            atom_node_5.info.putValue("name", "a");
//
//            ArrayList atoms_1 = new ArrayList();
//            ArrayList atoms_2 = new ArrayList();
//
//            atoms_1.add( atom_node_1 );
//            atoms_1.add( atom_node_2 );
//            atoms_2.add( atom_node_3 );
//            atoms_2.add( atom_node_4 );
//            atoms_2.add( atom_node_5 );
//
//            ArrayList atoms_result;
//
//            //atoms_result = combineAtom(OPERATION_TYPE_AND, atom_node_1, atom_node_2);
//            General.showOutput( atom_node_1.toString() );
//            General.showOutput( atom_node_2.toString() );
//            General.showOutput("allowance compares :" + compareAllowance(atom_node_1, atom_node_2 ));
//        }

//        if ( false ) {
//            atom_node_1.info.putValue("resi", "1");
//            atom_node_1.info.putValue("name", "a");
//
//            atom_node_2.info.putValue("resi", "2");
//            atom_node_2.info.putValue("name", ".");
//
//            atom_node_3.info.putValue("resi", "1");
//            atom_node_3.info.putValue("name", ".");
//
//            atom_node_4.info.putValue("resi", "1");
//            atom_node_4.info.putValue("name", "a");
//
//            atom_node_5.info.putValue("resi", ".");
//            atom_node_5.info.putValue("name", "a");
//
//            ArrayList atoms_1 = new ArrayList();
//            ArrayList atoms_2 = new ArrayList();
//
//            atoms_1.add( atom_node_1 );
//            //atoms_1.add( atom_node_2 );
//            atoms_2.add( atom_node_2 );
//            //atoms_2.add( atom_node_4 );
//            //atoms_2.add( atom_node_5 );
//
//            ArrayList atoms_result;
//
//            General.showOutput( atom_node_1.toString() );
//            General.showOutput( atom_node_2.toString() );
//            General.showOutput("combined by OR:" );
//            atoms_result = combineAtomList(Wattos.Converters.Common.Varia.OPERATION_TYPE_OR, atoms_1, atoms_2);
//            General.showOutput( AtomNode.toString( atoms_result ));
//    /*
//            General.showOutput( AtomNode.toString( atoms_1 ));
//            General.showOutput( AtomNode.toString( atoms_2 ));
//            atoms_result = combineAtomList(OPERATION_TYPE_AND, atoms_1, atoms_2);
//            General.showOutput( AtomNode.toString( atoms_result ));
//
//            /*
//            General.showOutput( AtomNode.toString( atoms_2 ));
//            atoms_result = AtomNode.removeRedundant(atoms_2);
//            General.showOutput( AtomNode.toString( atoms_result ));
//             */
//        }
    }
}
