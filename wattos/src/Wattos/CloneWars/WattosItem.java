/*
 * WattosItem.java
 *
 * Created on August 18, 2003, 3:20 PM
 */

package Wattos.CloneWars;

import java.io.*;
import java.util.*;
import Wattos.Utils.*;
import Wattos.Database.*;

/**
 *
 *Captures large collections of data in Wattos and is specialized by classes
 *such as Atom.
 *@see Wattos.Soup.Atom
 * @author Jurgen F. Doreleijers
 * @version 1
 */ 
public class WattosItem extends RelationSet implements Serializable {
       
    private static final long serialVersionUID = -1207795172754062330L;    
    
    /** Convenience variables */
    public BitSet      used;       // WattosItem
    public BitSet      selected;
    /** The number attribute is used to number from 1 to N within the next level. E.g.
     *In residue it numbers from 1 to the number of residues within the molecule; just
     *like star/mmcif/ccpn does.
     */
    public int[]       number;         
    /** The order attribute is used to order a (sub-) set of the data wrt some
     *algorithm. Sometimes simply the number.
     */
    public int[]       order;         
    public String[]    nameList;       
    public StringSet   nameListNR;       
    public int[]       type;         

    public WattosItem(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        //General.showDebug(" back in WattosItem constructor");
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets 
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public WattosItem(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug(" back in WattosItem constructor");
        name = relationSetName;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
        //General.showDebug(" now in WattosItem.init()");
        super.init(dbms);
        //General.showDebug(" back in WattosItem.init()");

        // MAIN RELATION COMMON COLUMNS
        DEFAULT_ATTRIBUTES_TYPES.put( DEFAULT_ATTRIBUTE_SELECTED,       new Integer(DATA_TYPE_BIT));     
        DEFAULT_ATTRIBUTES_TYPES.put( DEFAULT_ATTRIBUTE_NAME,           new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( DEFAULT_ATTRIBUTE_TYPE,           new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( DEFAULT_ATTRIBUTE_NUMBER,         new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( DEFAULT_ATTRIBUTE_ORDER_ID,       new Integer(DATA_TYPE_INT));
        
        
        DEFAULT_ATTRIBUTES_ORDER.add( DEFAULT_ATTRIBUTE_SELECTED);
        DEFAULT_ATTRIBUTES_ORDER.add( DEFAULT_ATTRIBUTE_NAME);
        DEFAULT_ATTRIBUTES_ORDER.add( DEFAULT_ATTRIBUTE_TYPE);
        DEFAULT_ATTRIBUTES_ORDER.add( DEFAULT_ATTRIBUTE_NUMBER);
        DEFAULT_ATTRIBUTES_ORDER.add( DEFAULT_ATTRIBUTE_ORDER_ID);
        //General.showDebug(" in WattosItem.init() order list has number of items: " + DEFAULT_ATTRIBUTES_ORDER.size());        
        return true;
    }

            
    /** Add a new wattos item to the list. Returns the index of the item's location in the
     *main relations array or -1 to indicate failure.
     */
    public int add(String itemName ) {
        int maxSize = mainRelation.sizeMax;
        int idx = mainRelation.getNewRowId();
        if ( idx < 0 ) {
            General.showCodeBug( "Failed to get a new row id for a WattosItem with name: " + name);
            return -1;
        }
        if ( maxSize != mainRelation.sizeMax) {
            resetConvenienceVariables();
        }
        setName( idx, itemName );
        // Always add new items to selection.
        selected.set( idx );
        return idx;
    }       
    
    /** Store only unique names 
     */
    public boolean setName( int idx, String itemName ) {
        if ( ! used.get(idx) ) {
            General.showWarning("Can't set name for item: " + itemName + " with rid: " + idx + " because not in use rid");
            return false;
        }
        // Store only unique names
        nameList[idx] = nameListNR.intern( itemName );        
        return true;
    }

    /** The column given is renumbered for all items in given in the argument bitset 
     *or to the physical order if no numbering is present.
     */
    public boolean renumberRows( String columnLabel, BitSet toDo, int startNumber ) {
        //General.showDebug("Renumbering elements according to physical order in table with name:" + name);
        return mainRelation.renumberRows(columnLabel, toDo, startNumber);
    }
 
    /** Some hardcoded variable names need to be reset when the columns resize. */
    public boolean resetConvenienceVariables() {
        // No super here because next one up doesn't do anything but throw a warning.
        //super.resetConvenienceVariables();
        //General.showDebug("Now in resetConvenienceVariables in WattosItem");
        used            =             mainRelation.used;                                                        // WattosItem
        selected        =             mainRelation.getColumnBit(        DEFAULT_ATTRIBUTE_SELECTED );         
        nameList        =             mainRelation.getColumnString(     DEFAULT_ATTRIBUTE_NAME );
        nameListNR      =             mainRelation.getColumnStringSet(  DEFAULT_ATTRIBUTE_NAME );
        type            =             mainRelation.getColumnInt(        DEFAULT_ATTRIBUTE_TYPE);                   // PropNAtom (non fkcs)
        number          =             mainRelation.getColumnInt(        DEFAULT_ATTRIBUTE_NUMBER );        
        order           =             mainRelation.getColumnInt(        DEFAULT_ATTRIBUTE_ORDER_ID );        
        if ( used == null || selected == null || nameList == null || nameListNR == null || type == null || number == null || order == null ) {
            return false;
        }
        return true;
    }
}
 
