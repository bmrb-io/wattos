/*
 * Created on January 13, 2004, 9:12 AM
 */

package Wattos.Soup.Constraint;

import Wattos.Utils.*;
import Wattos.Soup.*;
import Wattos.Database.*;
import cern.colt.map.*;
import cern.colt.list.*;
import cern.colt.matrix.*;
import cern.colt.matrix.impl.*;
import cern.colt.matrix.doublealgo.*;
import cern.jet.math.Functions;

import java.util.*;

/**
 * The idea to collapse contributions by first putting the info in a matrix
 * represenetation and then sorting columns and rows came from Hamid Eghbalnia.
 * @author Jurgen F. Doreleijers
 * @author Hamid Eghbalnia.
 */
public class DCContributionMatrix {
     public DoubleMatrix2D matrix;
     public DoubleMatrix2D subMatrix;     
     public int rowSize;
     public OpenIntIntHashMap atom2dcAtom;
     public IntArrayList atomRids;

     // Convenience variables.
     public OpenIntIntHashMap atom2MatrixRidRow; // Note that the matrix rid is one higher than stored.
     public OpenIntIntHashMap atom2MatrixRidCol;
     public IntArrayList atomRidsRow; // Doesn't include the zero-ed element which is not an atom rid.
     public IntArrayList atomRidsCol;

     public ArrayList contributions;
     public Gumbo gumbo;
     public static DoubleMatrix1DComparator comp;

     public static int DEFAULT_NUMBER_TOP_LEFT = -1; // the number indicating the top left corner of the matrix.
     // A comparator 
     static {
        comp = new DoubleMatrix1DComparator() {
            public int compare(DoubleMatrix1D a, DoubleMatrix1D b) {                
                //General.showDebug("Comparing rows: " + a.toString() + " and " + b.toString());
                if ( a.getQuick(0) == DEFAULT_NUMBER_TOP_LEFT ) { // first row should remain first
                    return -1;
                }
                if ( b.getQuick(0) == DEFAULT_NUMBER_TOP_LEFT ) { // first row should remain first
                    return 1;
                }
                for (int r=1;r<a.size();r++ ) {
                    if ( a.getQuick(r) != b.getQuick(r)) {
                        if ( a.getQuick(r) < b.getQuick(r) ) {
                            return 1;
                        }
                        return -1;
                    }
                }
                return 0; // they're the same
            }
        };
         
     }
    /** Creates a new instance of DCMatrixUtils 
     *The contributions are listed as: Object[] {a, a_Partner, dcA, dcA_Partner} );
     *with a and the others being IntArrayList.
     */
    public DCContributionMatrix( ArrayList contributions, Gumbo gumbo ) {
        this.contributions = contributions;
        this.gumbo = gumbo;
        atom2dcAtom = getMapAtom2dcAtom( contributions );
        //General.showDebug( "atom2dcAtom is: " + atom2dcAtom );
        // Count the number of different atoms as they will determine the dimensions of the array.
        // The extra row is to store the dc atom id. Note that the rows will be determined by
        // the number of different atoms and not dc atoms.
        //rowSize = getCountDifferentAtoms( contributions ) + 1;
        atomRids = atom2dcAtom.keys(); // These atoms are unique and an arbitrary map to the dcAtoms is maintained.
        //General.showDebug("AtomRids before sort: " + PrimitiveArray.toString( atomRids ));
        if ( ! gumbo.atom.order( atomRids )) {
            General.showError("Failed to order atoms in DCContributionMatrix");
        }
        //General.showDebug("AtomRids after sort: " + PrimitiveArray.toString( atomRids ));
        rowSize = atomRids.size() + 1;
        // A matrix of booleans would have done too but that implementation wasn't available.       
        //matrix = new DenseDoubleMatrix2D(3,4); 
        // has same interface but better performance for larger sparse connections as will        
        // likely be the limiting factor.        
        matrix = new SparseDoubleMatrix2D(rowSize,rowSize);                         
        matrix.setQuick(0,0, DEFAULT_NUMBER_TOP_LEFT );
        //General.showDebug(matrix.toString());                 
    }

    /** This routine is the most important one to be called from outside the class */
    public ArrayList getReorderedContributions() {
        
        int sum;
        //General.showDebug("Starting getReorderedContributions");
        atom2MatrixRidCol = PrimitiveMap.createMapValuesAreKeys( atomRids );
        //General.showDebug("atom2MatrixRidCol map: " + PrimitiveArray.toString( atom2MatrixRidCol ));
        if ( ! fillMatrix( matrix, contributions, atom2MatrixRidCol )) {
            General.showError("Failed to fill matrix in DCContributionMatrix");
        }
        //General.showDebug("Filled matrix");
        resetConvenienceVariables(); // resets atom2MatrixRidCol among others.
        //General.showDebug("Done with resetConvenienceVariables");

        //General.showDebug(matrix.toString());
        int sumContributionsTot = ((int) PrimitiveMatrix.getSum(matrix,1,matrix.rows(),1,matrix.columns()))/2;
        //sumContributions = sumContributions >>> 1; // divide by 2 by right shifting 1 position and adding zeros to the left
        //General.showDebug("Will try to fish out total number of individual contributions: " + sumContributionsTot);
        int sumContributionsLeft = sumContributionsTot;

        // the naming shortcut (alias) saves some keystrokes:
//        cern.jet.math.Functions F = cern.jet.math.Functions.functions;

        ArrayList contributionsNew = new ArrayList();
        int iteration = 0;
        while ( sumContributionsLeft > 0 ) {
            //General.showDebug("Starting iteration: " + iteration );
            if ( !sortMatrix() ) {
                General.showError("Failed to sort matrix in DCContributionMatrix: " + matrix);
                return null;
            }
            resetConvenienceVariables();
            sum = ((int) subMatrix.aggregate(Functions.plus,Functions.identity))/2;
            if ( sum == 0 ) {
                General.showError("Thought there were contributions left but apparently not");
                return null;
            }
            //General.showDebug( "There are a number of contributions still present: " + sum ); // there's a specialized function for this too.
            int[] rectCoor = getLargestFullRectangle();
            if ( rectCoor == null ) {
                General.showError("Failed to getLargestFullRectangle from matrix: " + matrix );
                return null;
            }
            //General.showDebug("Got largest full rectangle: " + rectCoor[0] + "x" + rectCoor[1] );
            int sumContributions = rectCoor[0] * rectCoor[1];                        
            //General.showDebug("Got atom rids in row: " + PrimitiveArray.toString( atomRidsRow ));
            //General.showDebug("Got atom rids in col: " + PrimitiveArray.toString( atomRidsCol ));
            IntArrayList atomRidsRowSel = (IntArrayList) atomRidsRow.partFromTo(0, rectCoor[0]-1);
            IntArrayList atomRidsColSel = (IntArrayList) atomRidsCol.partFromTo(0, rectCoor[1]-1);
            if (atomRidsRowSel == null ||
                atomRidsColSel == null ||
                atomRidsRowSel.size()==0 ||
                atomRidsColSel.size()==0 ) {
                //General.showDebug("Got no atom rids from row and/or col" );
                return null;
            }
            //General.showDebug("Got selected atom rids in row: " + PrimitiveArray.toString( atomRidsRowSel ));
            //General.showDebug("Got selected atom rids in col: " + PrimitiveArray.toString( atomRidsColSel ));

            IntArrayList dcAtomRidsRowSel = PrimitiveMap.getMappedList( atomRidsRowSel, atom2dcAtom );
            IntArrayList dcAtomRidsColSel = PrimitiveMap.getMappedList( atomRidsColSel, atom2dcAtom );
            if ( dcAtomRidsRowSel == null ||
                 dcAtomRidsColSel == null ) {
                General.showError("Failed to get mapped list for selected atom rids to dc atom rids in row and/or col");
                return null;
            }
            //General.showDebug("Got selected dc atom rids in row: " + PrimitiveArray.toString( dcAtomRidsRowSel ));
            //General.showDebug("Got selected dc atom rids in col: " + PrimitiveArray.toString( dcAtomRidsColSel ));
            contributionsNew.add( new Object[] { atomRidsRowSel, atomRidsColSel, dcAtomRidsRowSel, dcAtomRidsColSel}); 
            //DistConstr.showContributions(contributionsNew);
            if ( ! setMatrixByLabelValues( atomRidsRowSel, atomRidsColSel, 0d )) {
                General.showError("Failed to setMatrixByLabelValues atomRidsRowSel, atomRidsColSel");
                return null;
            }                
            //General.showDebug("After removing contributions already collected the matrix looks like:\n" + matrix);
            sumContributionsLeft -= sumContributions;
            iteration++;
        }
        sum = ((int) subMatrix.aggregate(Functions.plus,Functions.identity))/2;
        if ( sum != 0 ) {
            General.showError("There should not be any contributions but found: " + sum);
            return null;
        }
        if ( ! DistConstr.sortContributions(contributionsNew, gumbo)) { // still usefull for left/right swaps.
            return null;
        }
        return contributionsNew;
    }
        
    
    public boolean sortMatrix() {        
        // sort by sum of values in a row        
        matrix = Sorting.quickSort.sort(matrix,comp); // sort rows
        //General.showDebug(matrix.toString());             
        matrix = matrix.viewDice();                   // swap rows and columns
        matrix = Sorting.quickSort.sort(matrix,comp); // sort rows
        matrix = matrix.viewDice();                   // swap again
        //General.showDebug(matrix.toString());                 
        return true;        
    }
    
    /** Returns 2 ints for the inclusive end of the rectangle starting at 1,1 
     *going to the right and bottom.
     *Returns null if none where found. Will prefer the larger column count than row count.
     */    
    public int[] getLargestFullRectangle() {
        if ( matrix.get(1,1) == 0 ) {
            return null;
        }
        int r_max = 1;
        int c_max = 1;
        int s_max = 0;
        int r = 1;
//        int c = 1;
        int s = -1;
        int sum = 0;
        int rowSize = matrix.rows();
        while ( r < rowSize ) {
            // does the current row continue with as many ones as before or does it have less
            sum = getNonZerosFromLeft(r);
            if ( sum == 0 ) {
                break;
            }
            s = r * sum;
            //General.showDebug("Found a rectangular block of size: " + r + "x" + sum + " total: " + s);
            if ( s > s_max ) {
                r_max = r;
                c_max = sum;
                s_max = s;
            }
            r++;
        }
        return new int[] { r_max, c_max };
    }
    
    
    
        
    /** Given the lists for row and column label values set the elements to the given value */
    public boolean setMatrixByLabelValues( IntArrayList a_ids, IntArrayList b_ids, double value) {
        int a_id;
        int b_id;
        int ar_rid;
        int br_rid;
        int ac_rid;
        int bc_rid;
                
        resetConvenienceVariables();
        if ( a_ids.size() > b_ids.size() ) { // Method is more efficient if inner loop is largest.
            IntArrayList tmp = a_ids;
            a_ids = b_ids;
            b_ids = tmp;
        }
        for (int a=0;a<a_ids.size();a++) {
            a_id    = a_ids.get(a);
            ar_rid   = atom2MatrixRidRow.get( a_id ) + 1;
            ac_rid   = atom2MatrixRidCol.get( a_id ) + 1;
            for (int b=0;b<b_ids.size();b++) {                
                b_id    = b_ids.get(b);
                br_rid   = atom2MatrixRidRow.get( b_id ) + 1;
                bc_rid   = atom2MatrixRidCol.get( b_id ) + 1;
                //General.showDebug("Setting value in matrix at location: " + ar_rid + "x" + bc_rid);
                //General.showDebug("Setting value in matrix at location: " + br_rid + "x" + ac_rid);
                matrix.setQuick(ar_rid,bc_rid,value);
                matrix.setQuick(br_rid,ac_rid,value);
            }
        }
        return true;
    }
        
    /** Get hashmaps that allows fast lookup to go from atom id to position in matrix
    for both rows and columns mind you they are in a different order.
    Actually expected a standard conversion here but didn't find it.
     */    
    public boolean resetConvenienceVariables( ) {
        subMatrix = matrix.viewPart(1,1,matrix.rows()-1,matrix.columns()-1);
        DoubleMatrix1D atomRidsRowM = matrix.viewColumn(    0).viewPart(1,matrix.rows()-1);
        DoubleMatrix1D atomRidsColM = matrix.viewRow(       0).viewPart(1,matrix.columns()-1);        
        atomRidsRow = PrimitiveArray.toIntArrayList( atomRidsRowM.toArray());
        atomRidsCol = PrimitiveArray.toIntArrayList( atomRidsColM.toArray());
        atom2MatrixRidRow = PrimitiveArray.createHashMapByOrder( atomRidsRow );
        atom2MatrixRidCol = PrimitiveArray.createHashMapByOrder( atomRidsCol );
        return true;
    }

        
    public int getNonZerosFromLeft(int r) {
        int c = 0;
        int sum = 0;
        while ( c < matrix.columns() ) {
            c++;
            if ( matrix.getQuick(r,c) > 0 ) {
                sum++;
            } else {
                break; // exit on the first zero
            }
        }
        return sum;
    }
            
        
        
        
    /** Move the info in the contributions list to a matrix representation.
     */
    public static boolean fillMatrix( DoubleMatrix2D m, ArrayList contributions, OpenIntIntHashMap atom2MatrixRid ) {        

        // Set labels
        IntArrayList atomKeys = atom2MatrixRid.keys();
        for (int i=0;i<atomKeys.size();i++) {
            int key = atomKeys.getQuick(i);
            int value = atom2MatrixRid.get( key );
            m.setQuick( value+1, 0, key ); // to keep track of the ids when sorting.
            m.setQuick( 0, value+1, key );                
        }
        // Set elements
        for (int k=0;k<contributions.size();k++) {
            Object[] contrib_A = (Object[]) contributions.get(k);            
            IntArrayList atomRid_A               = (IntArrayList) contrib_A[0];
            IntArrayList atomRid_A_Partner       = (IntArrayList) contrib_A[1];
//            IntArrayList dcAtomRid_A             = (IntArrayList) contrib_A[2];
//            IntArrayList dcAtomRid_A_Partner     = (IntArrayList) contrib_A[3];
            for (int i=0;i<atomRid_A.size();i++) { // loop over rows                
                int atom_A = atomRid_A.get(i);
                int matrixId = atom2MatrixRid.get( atom_A );
                for (int j=0;j<atomRid_A_Partner.size();j++) { // loop over columns
                    int atom_A_Partner = atomRid_A_Partner.get(j);
                    //General.showDebug("Working on contribution : " + k + " atom " + atom_A + " and " + atom_A_Partner );
                    int matrixId_Partner = atom2MatrixRid.get( atom_A_Partner );
                    m.setQuick( matrixId+1, matrixId_Partner+1, 1.0d );
                    m.setQuick( matrixId_Partner+1, matrixId+1, 1.0d );
                }
            }                          
        }        
        return true;
    } 

    public static OpenIntIntHashMap getMapAtom2dcAtom( ArrayList contributions ) {
        OpenIntIntHashMap atom2dcAtom = new OpenIntIntHashMap();
//        IntArrayList presenceList = new IntArrayList();
        for (int k=0;k<contributions.size();k++) {
            Object[] contrib_A = (Object[]) contributions.get(k);
            //General.showDebug("Working on contribution : " + k );
            IntArrayList atomRid_A               = (IntArrayList) contrib_A[0];
            IntArrayList atomRid_A_Partner       = (IntArrayList) contrib_A[1];
            IntArrayList dcAtomRid_A             = (IntArrayList) contrib_A[2];
            IntArrayList dcAtomRid_A_Partner     = (IntArrayList) contrib_A[3];
            for (int n=0;n<atomRid_A.size();n++) {
                atom2dcAtom.put( atomRid_A.getQuick(n),         dcAtomRid_A.getQuick(n));
            }
            for (int n=0;n<atomRid_A_Partner.size();n++) {
                atom2dcAtom.put( atomRid_A_Partner.getQuick(n), dcAtomRid_A_Partner.getQuick(n));
            }
        }
        return atom2dcAtom;
    }
    
    /** Example like:
     * A   | E
     * C,B | D,A,E
     * A   | C
     *should be rewritten to:
     * A,D,E | B,C
     * A     | E
     */
    public static ArrayList getExample_1( ) {
        ArrayList contributions = new ArrayList();
        // contribution 1
        IntArrayList a               = new IntArrayList(1); 
        IntArrayList a_Partner       = new IntArrayList(1);
        IntArrayList dcA             = new IntArrayList(1);
        IntArrayList dcA_Partner     = new IntArrayList(1);
        contributions.add( new Object[] {a, a_Partner, dcA, dcA_Partner} );
        a.add(              11);
        a_Partner.add(      15);
        dcA.add(            91);
        dcA_Partner.add(    95);
        // contribution 2
        a               = new IntArrayList(1); 
        a_Partner       = new IntArrayList(1);
        dcA             = new IntArrayList(1);
        dcA_Partner     = new IntArrayList(1);
        contributions.add( new Object[] {a, a_Partner, dcA, dcA_Partner} );
        a.add(              13);
        a.add(              12);
        a_Partner.add(      14);
        a_Partner.add(      11);
        a_Partner.add(      15);
        dcA.add(            93);
        dcA.add(            92);
        dcA_Partner.add(    94);
        dcA_Partner.add(    91);
        dcA_Partner.add(    95);
        // contribution 3
        a               = new IntArrayList(1); 
        a_Partner       = new IntArrayList(1);
        dcA             = new IntArrayList(1);
        dcA_Partner     = new IntArrayList(1);
        contributions.add( new Object[] {a, a_Partner, dcA, dcA_Partner} );
        a.add(              11);
        a_Partner.add(      13);
        dcA.add(            91);
        dcA_Partner.add(    93);
        return contributions;
    }
    
    /** Example like:
     * A,B,E   |  C,D,F,G
     * A       |  E
     *should be rewritten to:
     * A,B,E   |  C,D,F,G
     * A       |  E
     */
    public static ArrayList getExample_2( ) {
        ArrayList contributions = new ArrayList();
        // contribution 1
        IntArrayList a               = new IntArrayList(3); 
        IntArrayList a_Partner       = new IntArrayList(4);
        IntArrayList dcA             = new IntArrayList(3);
        IntArrayList dcA_Partner     = new IntArrayList(4);
        contributions.add( new Object[] {a, a_Partner, dcA, dcA_Partner} );
        a.add(              11);
        a.add(              12);
        a.add(              15);
        a_Partner.add(      13);
        a_Partner.add(      14);
        a_Partner.add(      16);
        a_Partner.add(      17);
        dcA.add(            91);
        dcA.add(            92);
        dcA.add(            95);
        dcA_Partner.add(    93);
        dcA_Partner.add(    94);
        dcA_Partner.add(    96);
        dcA_Partner.add(    97);
        // contribution 2
        a               = new IntArrayList(1); 
        a_Partner       = new IntArrayList(1);
        dcA             = new IntArrayList(1);
        dcA_Partner     = new IntArrayList(1);
        contributions.add( new Object[] {a, a_Partner, dcA, dcA_Partner} );
        a.add(              11);
        a_Partner.add(      15);
        dcA.add(            91);
        dcA_Partner.add(    95);
        return contributions;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        for (int i=0;i<1;i++) {
            if ( true ) {
                DBMS dbms = new DBMS();
                Gumbo gumbo = new Gumbo(dbms);
                ArrayList contributions = getExample_2();
                DistConstr.showContributions(contributions);
                DCContributionMatrix m = new DCContributionMatrix( contributions, gumbo );
                ArrayList contributionsNew = m.getReorderedContributions();
                DistConstr.showContributions(contributionsNew);
            }    
        }
    }
}
