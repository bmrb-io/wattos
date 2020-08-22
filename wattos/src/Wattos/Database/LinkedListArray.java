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
 *Like <code>java.util.LinkedList</code> but tightly linked to the <code>Relation</code> class.
 *A name collision with 
 *the regular LinkedList in java.util was avoided by choosing the slightly longer
 *name.
 *@see java.util.LinkedList
 *@see Relation
 * @author  Jurgen F. Doreleijers
 */
public class LinkedListArray implements Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;    

    /**A numeric value to indicate it's not a real index */
    static final int NOT_AN_INDEX = -1;
    /**A string value to indicate it's not a real index. Note it isn't declared final but it is static */
    public static String NOT_AN_INDEX_STRING = ".";

    /** Going forward*/    
    int[]  forwardList = null;
    /** Going back*/    
    int[] backwardList = null;
    
    /** Standard constructor
     * @param maxRowSize  */    
    LinkedListArray (int maxRowSize) {
        init(maxRowSize);
    }

    
    /**Shrink or grow the capacity just like the main class's method namesake.
     * @param maxRowSize
     *  */    
    public boolean resizeCapacity(int maxRowSize) {
        int sizeOld = forwardList.length;
        forwardList  = PrimitiveArray.resizeArray( forwardList,  maxRowSize);
        backwardList = PrimitiveArray.resizeArray( backwardList, maxRowSize);
        if ( forwardList == null || backwardList == null ) {
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

    /** Default values are not indexes; i.e. not part of a list.
     */
    public boolean setValuesToDefaultByRowRange(int idxStart, int idxEnd) {
        if ( idxStart >= idxEnd ) {
            General.showWarning("idxStart("+idxStart+") should be smaller than idxEnd("+idxEnd+")");
            return false;
        }
        Arrays.fill(forwardList,  idxStart, idxEnd, NOT_AN_INDEX);
        Arrays.fill(backwardList, idxStart, idxEnd, NOT_AN_INDEX);
        return true;
    }
    
    
    /**Initialize the instance.
     * @param maxRowSize
     *  */    
    public boolean init(int maxRowSize) {
        forwardList =  new int[maxRowSize];
        backwardList = new int[maxRowSize];        
        setValuesToDefaultByRowRange(0,maxRowSize);
        return true;
    }


    /** Elements in the list are not checked for their occurrence
     * in another list. This impossible in the current implementation
     * that doesn't have a method for removing rows.
     * @param list*/    
    public boolean setList(int[] list) {
        int lastIdx = list.length-1;
        if ( lastIdx == -1 ) {
            return true;
        }
        if ( lastIdx > 0 ) {
            forwardList[ list[0]]        = list[1];
            backwardList[list[0]]        = NOT_AN_INDEX;
            forwardList[ list[lastIdx]]  = NOT_AN_INDEX;
            backwardList[list[lastIdx]]  = list[lastIdx-1];
        } else {
            forwardList[ list[0]]        = NOT_AN_INDEX;
            backwardList[list[0]]        = NOT_AN_INDEX;
        }
        
        for (int i=lastIdx-1;i>0;i--) {
            forwardList[list[i]]  = list[i+1];
            backwardList[list[i]] = list[i-1];
        }
        return true;
    }
    
    /** Elements in the list are not checked for their occurrence
     * in another list. This impossible in the current implementation
     * that doesn't have a method for removing rows.
     *  */    
    public boolean getList(int firstIdx, int[] result) {
        for (int i = 0; i< result.length; i++ ) {            
            result[ i ] = firstIdx;
            firstIdx = forwardList[ firstIdx ];
            if ( firstIdx == NOT_AN_INDEX && i < (result.length-1) ) {
                General.showError("code bug in getList in LinkedListArray: list is smaller by ("+(result.length-i-1)+") than expected size: " + result.length);
            }
        }
        if ( firstIdx != NOT_AN_INDEX ) {
            General.showError("code bug in getList in LinkedListArray: list is larger by at least one than expected size: " + result.length);
            return false;
        }
        return true;
    }
    
    /** Remaps the indexes in the forward and backward lists according
     * to the given map which contains the mapping (to->from) where
     * map[index_to] = index_from; see method reduceCapacity for an
     * example map.
     * @param map
     *   */
    public boolean remap(int[] map) {
        // Do the mapping from the end to beginning for efficiency reasons.
        for (int i=forwardList.length-1;i>=0;i--) {
            // forward
            int oldIdx = forwardList[i];
            if ( oldIdx == NOT_AN_INDEX ) {
                // element i was the last of the list
                continue;
            }
            int newIdx = map[oldIdx];
            if ( newIdx != NOT_AN_INDEX ) {
                // the forward element was moved.
                forwardList[i] = newIdx;
            }
        }
        for (int i=forwardList.length-1;i>=0;i--) {
            // backward
            int oldIdx = backwardList[i];
            if ( oldIdx == NOT_AN_INDEX ) {
                // element i was the first of the list
                continue;
            }
            int newIdx = map[oldIdx];
            if ( newIdx != NOT_AN_INDEX ) {
                // the backward element was moved.
                backwardList[i] = newIdx;
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
        idx = forwardList[row];
        if ( idx != NOT_AN_INDEX ) {
            sb.append(Integer.toString( idx ));
        } else {
            sb.append(NOT_AN_INDEX_STRING);
        }
        sb.append(',');
        idx = backwardList[row];
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
     *   */    
    public boolean moveRows(int[] idxListOrg, int[] idxListNew) {
        int idxListOrgSize = idxListOrg.length;
        for (int r=0;r<idxListOrgSize;r++) {
            int idxOrg = idxListOrg[r];
            int idxNew = idxListNew[r];
            forwardList[ idxNew] = forwardList[idxOrg];
            backwardList[idxNew] = backwardList[idxOrg];
        }
        /** Materialize the mapping table; which might be wastefull for small capacity changes
         *but is very fast for usage. */
        int[] result = new int[ forwardList.length ];
        Arrays.fill(result, NOT_AN_INDEX);
        for (int i=idxListOrg.length-1;i>-1;i--){
            result[ idxListOrg[i] ] = idxListNew[i];
        }
        //General.showDebug("Mapping table:" + PrimitiveArray.toString(result));
        // Then correct the indexes in the maps.
        //General.showOutput("Linked list before remapLinkedLists: " + this.toString());
        boolean status = remap( result );
        if ( ! status ) {
            General.showError("Failed to remap the linked list array");
            return false;
        }
        
        /** Removing of rows needs to be done after the linked lists have been
        updated otherwise they brake. And! A special type of remove should be        
        nil these elements. */
        for (int r=0;r<idxListOrgSize;r++) {        
            int row = idxListOrg[r];
            forwardList[ row]  = NOT_AN_INDEX;
            backwardList[ row] = NOT_AN_INDEX;        
        }
        return true;
    }  
    
    
    /** Checks to see if the element is contained in a list. Can
     * be at the beginning, middle, or at the end of list.
     * @param row
     *   */     
    public boolean removeRow(int row) {
        //General.showDebug("removing lla row: " + row );
        int idxForward  = forwardList[ row];
        int idxBackward = backwardList[row];
        if ( idxForward == NOT_AN_INDEX && idxBackward == NOT_AN_INDEX ) {
            //it's not part of any list
            ;
        } else if ( idxForward != NOT_AN_INDEX && idxBackward != NOT_AN_INDEX ) {
            // it's in the middle
            //Adjust the idxForward 
            forwardList[ idxBackward ] = idxForward;
            backwardList[ idxForward ] = idxBackward;
        } else if ( idxForward != NOT_AN_INDEX ) {
            // it's the first element of a list
            backwardList[ idxForward ] = idxBackward;            
        } else {
            // it's the last element of a list
            forwardList[ idxBackward ] = idxForward;            
        }
        // nil these elements.
        forwardList[ row] = NOT_AN_INDEX;
        backwardList[row] = NOT_AN_INDEX;        
        return true;
    }
    
    /** Checks to see if the element is contained in a list. Can
     * be at the beginning, middle, or at the end of list.
     * @param rowList
     *  */     
    public boolean removeRows(int[] rowList) {
        for (int i=rowList.length-1;i>=0;i--) {
            boolean status = this.removeRow(rowList[i]);
            if ( ! status ) {
                General.showError("Failed to remove row in LinkedListArray: " + i );
                return false;
            }
        }
        return true;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }    
} 
