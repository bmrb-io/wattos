/*
 * Model.java
 *
 * Created on November 8, 2002, 4:41 PM
 */

package Wattos.Soup;

import java.io.*;
import java.util.*;

import com.braju.format.Format;
import com.braju.format.Parameters;

import Wattos.Utils.*;
import Wattos.Database.*;

/**
 *Model contains one or more molecules.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Model extends GumboItem implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;        

    /** Convenience variables */
    public int[]       entryId;          
        
    public static float DEFAULT_DIAMETER = 999.9f;

    public Model(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
    }

    public Model(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
    }

    public boolean init(DBMS dbms) {
        super.init(dbms);

        name = Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_SET_NAME];

        // MAIN RELATION
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RELATION_ID_COLUMN_NAME ]);         
        
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME],  
            Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME]});
            
        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_MAIN_RELATION_NAME];
        try {
            relation = new Relation(relationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);        
        relation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, 
            DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS);
        addRelation( relation );
        mainRelation = relation;

        // OTHER RELATIONS HERE
        
        return true;
    }            

    
    /** All selected models are renumbered according to (TODO the order they are numbered or to) the physical order.
     *It will be done for all selected entries.
    public boolean resetModelNumbers() {        
        BitSet selEntries = gumbo.entry.mainRelation.getColumnBit( DEFAULT_ATTRIBUTE_SELECTED );
        int[] modelNumber = mainRelation.getColumnInt( Relation.DEFAULT_ATTRIBUTE_NUMBER );
        String columnLabel = Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME];
        
        for (int r=selEntries.nextSetBit(0); r>=0; r=selEntries.nextSetBit(r+1))  {
            // Get the rids of the models in the entry.
            BitSet uniqueModels = SQLSelect.selectBitSet(dbms, mainRelation, 
                columnLabel, SQLSelect.OPERATION_TYPE_EQUALS, new Integer(r), false);
            // Just operate on the selected models.
            uniqueModels.and( mainRelation.getColumnBit(DEFAULT_ATTRIBUTE_SELECTED) );
            General.showDebug("For entry: " + gumbo.entry.mainRelation.getValueString(r, DEFAULT_ATTRIBUTE_NAME ) +
                " found number of models: " + uniqueModels.cardinality());
            int modelNum = 1;
            // TODO use the actual model numbers first if available.
            for (int rid=uniqueModels.nextSetBit(0); rid>=0; rid=uniqueModels.nextSetBit(rid+1))  {                
                modelNumber[rid] = modelNum++;
            }
            if ( modelNum != uniqueModels.cardinality() + 1) {
                General.showError("For entry: " + gumbo.entry.mainRelation.getValueString(r, DEFAULT_ATTRIBUTE_NAME ) +
                    " did number of models " + (modelNum-1) + " unequal to expected: " + uniqueModels.cardinality());
                return false;
            }                
        }      
        return true;
    }            
     */
    
    
   /** Adds a new model in the array, filling in all the required properties
     *as available from the parent.
     *Returns -1 for failure.
     */
    public int add(int modelNumber, int parentId) {
        int result = super.add( null ); // No name in use now for model
        if ( result < 0 ) {
            General.showCodeBug( "Failed to get a new row id for a model with number: " + modelNumber);
            return -1;
        }        
        number[     result ] = modelNumber;
        entryId[    result ] = parentId;
        return result;
    }    

    /** Looks for any model with given number and then does a logical and
     *on the set of those models with the given set and returns the one
     *model's rid or -1 in case there's not exactly 1.
     * */    
    public int getModelRidWithNumber ( BitSet toDo, int startNumber ) {
        BitSet selModelSub = SQLSelect.selectBitSet(dbms, mainRelation, 
            Relation.DEFAULT_ATTRIBUTE_NUMBER, SQLSelect.OPERATION_TYPE_EQUALS, new Integer(startNumber), false);
        if ( selModelSub == null ) {
            General.showCodeBug("Failed to get a models with number: " + startNumber);
            return -1;
        }
        selModelSub.and( toDo );
        int modelCount = selModelSub.cardinality(); // cache it.
        if ( modelCount != 1 ) {
            General.showCodeBug("Expected only 1 model with given number ("+startNumber+") but found: " + startNumber);
            return -1;
        }
        return selModelSub.nextSetBit(0);
    }

    /** For given model create a map from molecule id to molecule rid. Should be pretty small
     *and efficient.
     */
    public int[] createMapMolId2Rid( int modelRID ) {
        BitSet selMolSub = SQLSelect.selectBitSet(dbms, gumbo.mol.mainRelation, 
            Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ], 
            SQLSelect.OPERATION_TYPE_EQUALS, new Integer(modelRID), false);
        if ( selMolSub == null ) {
            General.showCodeBug("Failed to get molecules in model with rid: " + modelRID);
            return null;
        }
        int molCount = selMolSub.cardinality(); // cache it.
        int[] map = new int[molCount+1]; // presuming they are ordered from 1 to n (and not the usual 0 to n-1
        try {// use the try block in order to catch the java out of bounds that might happen if they're
            // not in order or start from a number different than 1.
            for (int r=selMolSub.nextSetBit(0);r>=0;r=selMolSub.nextSetBit(r+1)) {
                map[ gumbo.mol.number[r] ] = r;
            }
        } catch ( Exception e ) {
            General.showThrowable(e);
            return null;
        }
        return map;
    }

   /** All models in bitset given are renumbered within entry.
     * See same method in Residue class.*/    
    public boolean renumberRows( String columnLabel, BitSet toDo, int startNumber ) {

        int entry;
        String columnLabelEntryId   = Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[   RelationSet.RELATION_ID_COLUMN_NAME];

        // Find the unique entries for the given models to do.
        // Actually finding models that have different entries as parent
        BitSet selModelSub = SQLSelect.getDistinct(dbms, mainRelation, columnLabelEntryId, toDo);
        if ( selModelSub == null ) {
            General.showCodeBug("Failed to get a distince set of entries for the given models: " + PrimitiveArray.toString(toDo));
            return false;
        }
        // For each entry (through the modelid)
        for (int model=selModelSub.nextSetBit(0);   model>=0; model=selModelSub.nextSetBit(model+1)) {
            entry = entryId[model];                   
            // Get the models for this entry that are also in toDo.
            BitSet selModelSub2 = SQLSelect.selectBitSet(dbms, mainRelation, 
                columnLabelEntryId, SQLSelect.OPERATION_TYPE_EQUALS, new Integer(entry), false);
            if ( selModelSub2 == null ) {
                General.showCodeBug("Failed to get a model set in entry with rid: " + entry);
                return false;
            }
            selModelSub2.and( toDo );
            //General.showDebug("In entry with rid: " + entry + " found models to renumber: " + selModelSub2.cardinality());
            // Renumber the modeliudes in R2 starting at 1.
            mainRelation.renumberRows( Relation.DEFAULT_ATTRIBUTE_NUMBER, selModelSub2, 1);
        }
        //General.showDebug("Renumbered models at this point");
        return true;
    }            

    /** Checks if each of the models in the given argument are the same with respect
     *to the molecules, residues, and atom names. Attributes of these entities can
     *be different here are the ones that need to be the same:
     *
     *molecules:    -1- same sequence
     *residues:     -1- same name
     *              -2- same number
     *atoms:        -1- same name
     *
     *Because for 10^6 atoms there are 10^5 residues that need to be checked
     *this routine doesn't use just BitSets but also uses and reuses the Colt IntArrayList
     *object for returning rids on atoms. 
    public boolean checkSimilarityModels( BitSet modelsToConsider ) {
        firtsModelRID = modelsToConsider.nextSetBit(0);
        for (int modelRID = firtsModelRID; modelsToConsider >= 0; modelRID = modelsToConsider.nextSetBit(modelRID)) {
            // check to see if the molecules are the same by sequence.
            // Lets number the molecules at least for that...
            etc
        }            
    }
     */
    
    /**Within a model do atoms have the same coordinates? And other checks in the future. 
     */
    public boolean checkSoup( int m, boolean makeCorrections ) {
        // 
//        BitSet atomRids = SQLSelect.selectBitSet( dbms, 
//            gumbo.atom.mainRelation,                                                    // Relation
//            Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME],    // column
//            SQLSelect.OPERATION_TYPE_EQUALS,                                            // operation
//            new Integer( m ), false);                                            	// value, distinct
        /** TODO
        if ( atomRids == null ) {
            General.showError("Failed to get the atoms in this model by SQLSelect method: ", m);
            return false;
        }        
        int atomCount = atomRids.cardinality(); // new since 1.4; relies on complete scan; no problem for molecules though.
        molCount++;

        int atomRid=model.selected.nextSetBit(0);
        for (;m>=0;m=model.selected.nextSetBit(m+1)) {
            if ( ! model.checkSoup( m, makeCorrections )) {
                status = false;
            }
        }
         */
        return false;
    }
    
    /**     */
    public boolean resetConvenienceVariables() {        
        super.resetConvenienceVariables();
        entryId         = (int[])   mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME]);
        return true;
    }            

    /**
     * Return the entry, model, and the number of mol, res, and atom ids contained.
     */
    public String toString(int modelRrid) {
        if (modelRrid < 0) {
            General.showError("Can't do toString for model rid < 0");
            return null;
        }
        int entryNumber = gumbo.entry.number[entryId[modelRrid]];
        int modelNumber = number[modelRrid];
        
        Parameters p = new Parameters(); // for printf
        p.add(modelNumber);
        p.add(entryNumber);
        return Format.sprintf(
                "Model: %3d Entry: %2d", p);
    }
}
 
