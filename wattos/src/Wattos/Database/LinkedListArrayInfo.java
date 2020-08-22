/*
 * LinkedListArray.java
 *
 * Created on April 4, 2003, 11:52 PM
 */

package Wattos.Database;

import java.util.*;
import java.io.*;
import Wattos.Utils.*;

/** 
 *Parallel info to the LinkedListArray class.
 *@see LinkedListArray
 * @author  Jurgen F. Doreleijers
 */
public class LinkedListArrayInfo implements Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;    

    /**A numeric value to indicate it's not a real index */
    static final int NOT_AN_INDEX = LinkedListArray.NOT_AN_INDEX;
    /**A string value to indicate it's not a real index. Note it isn't declared final but it is static */
    public static String NOT_AN_INDEX_STRING = LinkedListArray.NOT_AN_INDEX_STRING;

    /** first Row*/    
    public int[] firstList = null;
    /** last Row*/    
    public int[] lastList = null;
    /** count Rows */    
    public int[] countList = null;
    
    /** Standard constructor
     * @param maxRowSize  */    
    LinkedListArrayInfo (int maxRowSize) {
        init(maxRowSize);
    }

    
    /**Shrink or grow the capacity just like the main class's method namesake.
     * @param maxRowSize
     *   */    
    public boolean resizeCapacity(int maxRowSize) {
        int sizeOld = firstList.length;
        firstList   = PrimitiveArray.resizeArray( firstList,  maxRowSize);
        lastList    = PrimitiveArray.resizeArray( lastList, maxRowSize);
        countList   = PrimitiveArray.resizeArray( countList, maxRowSize);
        if ( firstList == null || lastList == null || countList == null ) {
            General.showError("resizeArray failed");
            return false;
        }
        // Fill with different standard values the extra spaces if any.
        int fromIdx = sizeOld;
        int toIdx = maxRowSize;
        if ( fromIdx < toIdx ) {
            setValuesToDefaultByRowRange(fromIdx,toIdx);
        }
        return true;
    }

    /** Default values are not indexes; i.e. not part of a list. And zero for
     *count of course.
     */
    public boolean setValuesToDefaultByRowRange(int idxStart, int idxEnd) {
        if ( idxStart >= idxEnd ) {
            General.showWarning("idxStart("+idxStart+") should be smaller than idxEnd("+idxEnd+")");
            return false;
        }
        Arrays.fill(firstList,  idxStart, idxEnd, NOT_AN_INDEX);
        Arrays.fill(lastList,   idxStart, idxEnd, NOT_AN_INDEX);
        Arrays.fill(countList,  idxStart, idxEnd, 0);
        return true;
    }
    
    
    /**Initialize the instance.
     * @param maxRowSize
     *  */    
    public boolean init(int maxRowSize) {
        firstList   = new int[maxRowSize];
        lastList    = new int[maxRowSize];        
        countList   = new int[maxRowSize];        
        setValuesToDefaultByRowRange(0,maxRowSize);
        return true;
    }


    public boolean createList(int[] list) {
        General.showError("In LinkedListArrayInfo.createList Still need to code.");        
        return true;
    }
    
    
    /** Remaps the indexes in the first and last indexes lists according
     * to the given map which contains the mapping (to->from) where
     * map[index_to] = index_from; see method reduceCapacity for an
     * example map.
     * @param map
      */
    public boolean remap(int[] map) {
        // Do the mapping from the end to beginning for efficiency reasons.
        for (int i=firstList.length-1;i>=0;i--) {
            // forward
            int oldIdx = firstList[i];
            if ( oldIdx == NOT_AN_INDEX ) {
                // element i was the last of the list
                continue;
            }
            int newIdx = map[oldIdx];
            if ( newIdx != NOT_AN_INDEX ) {
                // the forward element was moved.
                firstList[i] = newIdx;
            }
        }
        for (int i=firstList.length-1;i>=0;i--) {
            // backward
            int oldIdx = lastList[i];
            if ( oldIdx == NOT_AN_INDEX ) {
                // element i was the first of the list
                continue;
            }
            int newIdx = map[oldIdx];
            if ( newIdx != NOT_AN_INDEX ) {
                // the backward element was moved.
                lastList[i] = newIdx;
            }
        }
        return true;
    }
    
    /**
     * @param row */    
    public String toString(int row ) {
        int idx;
        StringBuffer sb = new StringBuffer();
        sb.append('(');
        sb.append(Integer.toString( countList[row] ));
        sb.append(',');
        idx = firstList[row];
        if ( idx != NOT_AN_INDEX ) {
            sb.append(Integer.toString( idx ));
        } else {
            sb.append(NOT_AN_INDEX_STRING);
        }
        sb.append(',');
        idx = lastList[row];
        if ( idx != NOT_AN_INDEX ) {
            sb.append(Integer.toString( idx ));
        } else {
            sb.append(NOT_AN_INDEX_STRING);
        }
        sb.append(')');
        return sb.toString();
    }
        
    /**
     * @param idxListOrg
     * @param idxListNew
     *   
     */
    public boolean moveRows(int[] idxListOrg, int[] idxListNew) {
        int idxListOrgSize = idxListOrg.length;
        for (int r=0;r<idxListOrgSize;r++) {
            int idxOrg = idxListOrg[r];
            int idxNew = idxListNew[r];
            firstList[ idxNew] = firstList[ idxOrg];
            lastList[  idxNew] = lastList[  idxOrg];
            countList[ idxNew] = countList[ idxOrg];
        }
        
        /** Removing of rows needs to be done after the linked lists have been
        updated otherwise they brake. */
        for (int r=0;r<idxListOrgSize;r++) {        
            int idxOrg = idxListOrg[r];
            firstList[ idxOrg] = NOT_AN_INDEX;
            lastList[  idxOrg] = NOT_AN_INDEX;        
            countList[ idxOrg] = 0;        
        }
        return true;
    }
    
    
    /** Just reset the values to default.
     * @param row
     **/     
    public boolean removeRow(int row) {
        General.showDebug("removing lla info row: " + row );        
        return setValuesToDefaultByRowRange(row,row+1);
    }
    
    /** Checks to see if the element is contained in a list. Can
     * be at the beginning, middle, or at the end of list.
     * @param rowList
     * */     
    public boolean removeRows(int[] rowList) {
        for (int i=rowList.length-1;i>=0;i--) {
            boolean status = removeRow(rowList[i]);
            if ( ! status ) {
                General.showError("Failed to remove row in LinkedListArrayInfo: " + i );
                return false;
            }
        }
        return true;
    }
    
    /** Checks to see if the element is contained in a list. Can
     * be at the beginning, middle, or at the end of list.
     *   */     
    public boolean set(int row, int[] childRowList) {
        if ( childRowList.length != 0 ) {
            firstList[row]  = childRowList[0];
            lastList[row]   = childRowList[childRowList.length-1];
            countList[row]  = childRowList.length;
        } else {
            firstList[row]  = NOT_AN_INDEX;
            lastList[row]   = NOT_AN_INDEX;
            countList[row]  = childRowList.length;
        }
        return true;
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }    
}
