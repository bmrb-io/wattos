/*
 * PrimitiveMatrix.java
 *
 * Created on January 14, 2004, 8:25 AM
 */

package Wattos.Utils;

import cern.colt.matrix.*;

/**
 *Extension of the cern.colt.matrix API. Unused so far.
 * @author Jurgen F. Doreleijers
 */
public class PrimitiveMatrix {
    
    /** Creates a new instance of PrimitiveMatrix */
    public PrimitiveMatrix() {
    }
    
    public static double getSum( DoubleMatrix2D matrix, int startRow, int endRow, int startCol, int endCol ) {
        double sum = 0d;
        for (int r=startRow;r<endRow;r++) {
            for (int c=startCol;c<endCol;c++) {
                sum += matrix.getQuick(r,c);
            }
        }
        return sum;        
    }
    
    public static String toString(int[][] in) {
        StringBuffer sb = new StringBuffer();
        sb.append( '[' );
        for (int r=0;r<in.length;r++) {
            sb.append( PrimitiveArray.toString( in[r] ));
            if ( r!=(in.length-1)) {
                sb.append( ',' );
            } else {
                sb.append( ']' );
            }
            sb.append( General.eol );
        }
        return sb.toString();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if ( true ) {
            int[][] a = new int[2][];
            a[0] = new int[3];
            a[1] = new int[4];
            a[0][0] = -1;
            a[1][1] = 1;
            General.showOutput("a values are  :\n" + PrimitiveMatrix.toString(a));
        }        
    }    
}
