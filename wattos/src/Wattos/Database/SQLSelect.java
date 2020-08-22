/*
 * SQLSelect.java
 *
 * Created on August 25, 2003, 4:58 PM
 */

package Wattos.Database;

import Wattos.Utils.*;
import Wattos.Database.Indices.*;
import java.util.*;

import cern.colt.list.*;

/**
 *Code for structured query.
     *<P>
 *E.g.:
     *<PRE>
     *SELECT [DISTINCT] * FROM this_table
     *WHERE this_column == value
     *<PRE>
     *<P>
 * @author Jurgen F. Doreleijers
 */
public class SQLSelect {

    public static final int OPERATOR_AND = 0;
    public static final int OPERATOR_OR  = 1;
    public static final int OPERATOR_XOR = 2;

    public static final int OPERATION_TYPE_INVALID                  = 0;
    public static final int OPERATION_TYPE_EQUALS                   = 1;
    public static final int OPERATION_TYPE_UNEQUALS                 = 2;
    public static final int OPERATION_TYPE_GREATER_THAN             = 3;
    public static final int OPERATION_TYPE_SMALLER_THAN             = 4;
    public static final int OPERATION_TYPE_GREATER_THAN_OR_EQUAL    = 5;
    public static final int OPERATION_TYPE_SMALLER_THAN_OR_EQUAL    = 6;
    public static final int OPERATION_TYPE_EQUALS_REGULAR_EXPRESSION= 7; /** For string comparisons. */

    public static final String[] operation_type_names = {
        "OPERATION_TYPE_INVALID",                  
        "OPERATION_TYPE_EQUALS",                     
        "OPERATION_TYPE_UNEQUALS",                     
        "OPERATION_TYPE_GREATER_THAN",             
        "OPERATION_TYPE_SMALLER_THAN",             
        "OPERATION_TYPE_GREATER_THAN_OR_EQUAL",    
        "OPERATION_TYPE_SMALLER_THAN_OR_EQUAL",    
        "OPERATION_TYPE_EQUALS_REGULAR_EXPRESSION"};
    
/*    *//** Creates a new instance of SQLSelect *//*
    public SQLSelect() {
    }
*/
    
    /** Allows for a more complex query */
    public static BitSet selectCombinationBitSet( DBMS dbms, Relation relation, 
        String columnLabel_1,  int operationType_1, Object value_1, 
        String columnLabel_2,  int operationType_2, Object value_2, 
        int operator, boolean distinct ) {
        
        BitSet s_1 = SQLSelect.selectBitSet( dbms,
            relation, columnLabel_1, operationType_1, value_1, distinct );
        BitSet s_2 = SQLSelect.selectBitSet( dbms,
            relation, columnLabel_2, operationType_2, value_2, distinct );
        switch ( operator ) {
            case OPERATOR_AND: {
                s_1.and(s_2);
                break;
            }
            case OPERATOR_OR: {
                s_1.or(s_2);
                break;
            }
            case OPERATOR_XOR: {
                s_1.xor(s_2);
                break;
            }
            default: {
                General.showError("Unknown operator id: " + operator);
                return null;
            }
        }
        return s_1;
    }

    /**
     * 
     * @param dbms
     * @param relation
     * @param columnLabel
     * @param operationType
     * @param value
     * @param distinct
     * @param orderColumn
     * @return sorted list of rids that qualify
     */
    public static int[] selectList( DBMS dbms, 
            Relation relation, 
            String columnLabel, 
            int operationType, 
            Object value, 
            boolean distinct,
            String orderColumn ) {
        BitSet bs = selectBitSet( dbms, 
                relation, 
                columnLabel, 
                operationType, 
                value, 
                distinct);
        if ( bs == null ) {
            return null;
        }

        int[] map = relation.getRowOrderMap(orderColumn,bs);
        if ( map == null ) {
            General.showError("Failed to getRowOrderMap for relation: " + relation.name + " and column: " + orderColumn);
            return null;
        }        
        return map;
    }
    /** This Method implements a structured query like:
     *<P>
     *<PRE>
     *SELECT [DISTINCT] * FROM this_table
     *WHERE this_column == 7
     *<PRE>
     *<P>
     *In this example the following arguments need to be given:
     *operation: OPERATION_TYPE_EQUALS
     *threshold: 7.3 encapsulated in a Float object
     *For string operations sorted lists are less useful in general
     *but still possible with this method signature using operation type:
     *OPERATION_TYPE_EQUALS_REGULAR_EXPRESSION.
     *
     *
     *<P>The parameter 'value' is not changed. The parameter 'value' maybe a
     *BitSet itself in which see IndexSortedInt.getRidList
     */
    public static BitSet selectBitSet( DBMS dbms, 
        Relation relation, 
        String columnLabel, 
        int operationType, 
        Object value, 
        boolean distinct ) {
        
        
        //General.showDebug("Relation queried looks like: " + relation.toString(true, true, true, true, true, false ));
        
        if ( columnLabel == null ) {
            General.showCodeBug("In SQLSelect.selectBitSet. Null not allowed for column label for relation with name: " + relation.name);
            return null;
        }
        
        if ( ! relation.hasColumn(columnLabel)) {
            General.showCodeBug("In SQLSelect.selectBitSet. Non existing column label for relation with name: " + relation.name);
            return null;
        }
        Index index = relation.getIndex(columnLabel, Index.INDEX_TYPE_SORTED );
        if ( index == null ) {
            General.showWarning("Failed to get or create an index for column with name: " + columnLabel);
            General.showWarning("for relation with name: " + relation.name);
            return null;
        }
        
        BitSet result = null;
        if ( value instanceof BitSet ) {
            BitSet valueCopy = (BitSet) ((BitSet)value).clone();
            if ( operationType == OPERATION_TYPE_EQUALS ) {
                if ( index instanceof IndexSortedInt ) {
                    result = (BitSet) ((IndexSortedInt)index).getRidList( (BitSet) valueCopy );
                } else if ( index instanceof IndexSortedFloat ) {
                    result = index.getRidListForQuery( operationType, value );
                } else {
                    General.showCodeBug("When searching with bitsets the column to search should be of type int and the index on it of type IndexSortedInt");                    
                }
            } else {
                General.showCodeBug("When searching with bitsets on an int column the operation should be EQUALS");
            }    
            if ( ! valueCopy.equals(value)) {
                General.showCodeBug("There was a change in the valueCopy ");
                General.showCodeBug("value      : " + value);
                General.showCodeBug("valueCopy  : " + valueCopy);
                return null;
            }            
        } else {
            result = index.getRidListForQuery( operationType, value );
        }
        if ( result == null ) {
            General.showWarning("Failed to get result from query on column with name    : " + columnLabel);
            General.showWarning("Column data type                                       : " + Relation.dataTypeList[ relation.getColumnDataType( columnLabel)]);
            General.showWarning("for relation with name                                 : " + relation.name);
            General.showWarning("Operator                                               : " + operation_type_names[operationType]);
            General.showWarning("Value                                                  : " + value);
            return null;
        }
        
        /** Reduce the elements to only those that are different */
        if ( distinct ) {
            if ( index instanceof IndexSortedInt ) {
                // New object for the bitset is returned. The old one is not modified.
                result = ((IndexSortedInt)index).getDistinct( result );
            } else if ( index instanceof IndexSortedString ) {
                // New object for the bitset is returned. The old one is not modified.
                result = ((IndexSortedString)index).getDistinct( result );
            } else {
                General.showCodeBug("When trying to get a distinct set the index on the colum should be of type IndexSortedInt/IndexSortedString, in selectBitSet");                    
                return null;
            }
        }
        return result;
    }

    /** Using the index it is faster to determine the unique elements in the 
     *set toDo.
     */
    public static BitSet getDistinct( DBMS dbms, Relation relation, String columnLabel, 
        BitSet toDo) {
                
        if ( columnLabel == null ) {
            General.showCodeBug("In SQLSelect.getDistinct. Null not allowed for column label for relation with name: " + relation.name);
            return null;
        }
            
        Index index = relation.getIndex(columnLabel, Index.INDEX_TYPE_SORTED );
        if ( index == null ) {
            General.showWarning("Failed to get or create an index for column with name: " + columnLabel);
            General.showWarning("for relation with name: " + relation.name);
            return null;
        }
        
        BitSet result = null;
        if ( index instanceof IndexSortedInt ) {
            result = ((IndexSortedInt)index).getDistinct( toDo );
        } else if ( index instanceof IndexSortedString )  {
            result = ((IndexSortedString)index).getDistinct( toDo );
        } else {
            General.showCodeBug("When trying to get a distinct set the index on the colum should be of type IndexSortedInt/IndexSortedString, in getDistinct");                    
            return null;
        }
        // New object for the bitset is returned. The old one is not modified.        
        if ( result == null ) {
            General.showCodeBug("Failed to get distinct set in SQLSelect.getDistinct routine");
            return null;
        }            
        return result;
    }
    

    /** Using the index it is faster to determine the unique elements in the 
     *set toDo.     
    public static BitSet getDistinct( DBMS dbms, Relation relation, String[] columnLabelList, 
        BitSet toDo) {
                
        if ( columnLabelList == null ) {
            General.showCodeBug("In SQLSelect.getDistinct. Null not allowed for column label List for relation with name: " + relation.name);
            return null;
        }
        
        Index[] indexList = new Index[columnLabelList.length];
        for (int i=0;i<=columnLabelList.length;i++ ) {
            indexList[i] = relation.getIndex(columnLabelList[i], Index.INDEX_TYPE_SORTED);            
            if ( indexList[i] == null ) {
                General.showError("failed to get index for column: " + columnLabelList[i] +
                    " in relation: " + relation.name);
                return null;
            }
        }
        BitSet result = new BitSet( relation.used.size());
        Object value;
        BitSet otherRows = null;
        for (int r=relation.used.nextSetBit(0);r>=0;r=relation.used.nextSetBit(r+1)) {
            for (int i=0;i<=columnLabelList.length;i++ ) {
                value=relation.getValue(r,columnLabelList[i]);
                // Find same value on other rows.
                otherRows = indexList[i].getRidList(value,Index.LIST_TYPE_BITSET,otherRows);
                if (PrimitiveArray.cardinalityMoreThanOne(otherRows)) {
                    // Remove all but the first one. 
                    
                    //TODO
                }
            }
             
        }
        new relation.getIndex(columnLabel, Index.INDEX_TYPE_SORTED );
        if ( index == null ) {
            General.showWarning("Failed to get or create an index for column with name: " + columnLabel);
            General.showWarning("for relation with name: " + relation.name);
            return null;
        }
        
        BitSet result = null;
        if ( ! (index instanceof IndexSortedInt) ) {
            General.showCodeBug("When trying to get a distinct set the index on the colum should be of type IndexSortedInt");                    
            return null;
        }
        // New object for the bitset is returned. The old one is not modified.
        result = ((IndexSortedInt)index).getDistinct( toDo );
        if ( result == null ) {
            General.showCodeBug("Failed to get distinct set in SQLSelect routine");
            return null;
        }            
        return result;
    }
     */
    
    

    /** Performs an implicit correlated subselect sql like in MySQL:
     *UPDATE a, b SET a.column_old = value WHERE a.join_column_a = b.join_column_b
     *Only coded for where a.column_old and b.column_new are of type DATA_TYPE_STRINGNR for now
     *and value is of type BIT.
     */
    public static boolean update0( DBMS dbms, 
        Relation relationA, Relation relationB, 
        String aColumnOld, String joinColumnA, String joinColumnB, Object value) {        
        /**
        General.showDebug("Updating relation: " + relationA.name + " column: " + aColumnOld + " to value: " + value);
        General.showDebug("    joining on first column: " + joinColumnA + " and second column: " + joinColumnB);
        General.showDebug("    from relation: " + relationB.name);
         */

        if ( !relationA.containsColumn(aColumnOld)) {
            General.showError("Column [" + aColumnOld + "] not in relation: " + relationA.name );
            return false;
        }
        if ( relationA.getColumnDataType( joinColumnA ) != Relation.DATA_TYPE_STRINGNR ) {
            General.showError("Column [" + joinColumnA + "] should be of DATA_TYPE_STRINGNR but found: " + Relation.dataTypeList[relationA.getColumnDataType( joinColumnA )]);
            return false;
        }
        if ( relationB.getColumnDataType( joinColumnB ) != Relation.DATA_TYPE_STRINGNR ) {
            General.showError("Column [" + joinColumnB + "] should be of DATA_TYPE_STRINGNR but found: " + Relation.dataTypeList[relationB.getColumnDataType( joinColumnB )]);
            return false;
        }
        if ( ! (value instanceof Boolean) ) {
            General.showError("Value should be an instance of Boolean");
            return false;
        }
        boolean v = ((Boolean)value).booleanValue();
        
        BitSet aColumnOldList        = (BitSet) relationA.getColumn(aColumnOld);        
        String[] joinColumnBList    = (String[]) relationB.getColumn(joinColumnB);
        IndexSortedString indexOnJoinColumnA = (IndexSortedString) relationA.getIndex(
            joinColumnA,Index.INDEX_TYPE_SORTED);

        
        IntArrayList ridAList = new IntArrayList();
        int[] ridAL = null;
        int i= -1;
        for (int ridB = relationB.used.nextSetBit(0); ridB>=0; ridB = relationB.used.nextSetBit(ridB+1)) {
            ridAList = (IntArrayList) indexOnJoinColumnA.getRidList( 
                joinColumnBList[ridB], Index.LIST_TYPE_INT_ARRAY_LIST, null );            
            ridAL = ridAList.elements();
            //General.showOutput("using ridB: " + ridB + " found ridA list: " + PrimitiveArray.toString(ridAL));
            for (i=ridAList.size()-1;i>=0;i--) {
                aColumnOldList.set(  ridAL[i], v);                
            }
        }         
        return true;
    }

    
    /** Performs an implicit correlated subselect sql like in MySQL:
     *UPDATE a, b SET a.column_old = b.column_new WHERE a.join_column_a = b.join_column_b
     *Only coded for where a.column_old and b.column_new are of type String for now.
     */
    public static boolean update1( DBMS dbms, 
        Relation relationA, Relation relationB, 
        String aColumnOld, String bColumnNew, String joinColumnA, String joinColumnB) {        
        /**
        General.showDebug("Updating relation: " + relationA.name + " column: " + aColumnOld + " to second column: " + bColumnNew );
        General.showDebug("    joining on first column: " + joinColumnA + " and second column: " + joinColumnB);
        General.showDebug("    from relation: " + relationB.name);
         */
        if ( !relationA.containsColumn(aColumnOld)) {
            General.showError("Column [" + aColumnOld + "] not in relation: " + relationA.name );
            return false;
        }
        if ( !relationB.containsColumn(bColumnNew)) {
            General.showError("Column [" + bColumnNew + "] not in relation: " + relationB.name );
            return false;
        }
        if ( relationA.getColumnDataType( aColumnOld ) != Relation.DATA_TYPE_INT ) {
            General.showError("Column [" + aColumnOld + "] should be of type int but found: " + Relation.dataTypeList[relationA.getColumnDataType( aColumnOld )]);
            return false;
        }
        if ( relationB.getColumnDataType( bColumnNew ) != Relation.DATA_TYPE_INT ) {
            General.showError("Column [" + bColumnNew + "] should be of type int but found: " + Relation.dataTypeList[relationB.getColumnDataType( bColumnNew )]);
            return false;
        }
        if ( relationA.getColumnDataType( joinColumnA ) != Relation.DATA_TYPE_STRINGNR ) {
            General.showError("Column [" + joinColumnA + "] should be of DATA_TYPE_STRINGNR but found: " + Relation.dataTypeList[relationA.getColumnDataType( joinColumnA )]);
            return false;
        }
        if ( relationB.getColumnDataType( joinColumnB ) != Relation.DATA_TYPE_STRINGNR ) {
            General.showError("Column [" + joinColumnB + "] should be of DATA_TYPE_STRINGNR but found: " + Relation.dataTypeList[relationB.getColumnDataType( joinColumnB )]);
            return false;
        }
        
        int[] aColumnOldList        = (int[]) relationA.getColumn(aColumnOld);
        int[] bColumnNewList        = (int[]) relationB.getColumn(bColumnNew);
        
        String[] joinColumnBList    = (String[]) relationB.getColumn(joinColumnB);
        IndexSortedString indexOnJoinColumnA = (IndexSortedString) relationA.getIndex(
            joinColumnA,Index.INDEX_TYPE_SORTED);
        IntArrayList ridAList=null; // will be extensively recycled.
        int i,ridA;
        int[] ridAL;
        for (int ridB = relationB.used.nextSetBit(0); ridB>=0; ridB = relationB.used.nextSetBit(ridB+1)) {
            ridAList = (IntArrayList) indexOnJoinColumnA.getRidList( 
                joinColumnBList[ridB], Index.LIST_TYPE_INT_ARRAY_LIST, null );
            ridAL = ridAList.elements();
            for (i=ridAList.size()-1;i>=0;i--) {
                ridA = ridAL[i];
                aColumnOldList[ ridA ] = bColumnNewList[ ridB ];                
            }
        } 
        return true;
    }

    /** Like:
     *<PRE>
SELECT [DISTINCT] a.column1,...
FROM a,b
WHERE a.column_join=b.column_join
  AND b.column=value
     *</PRE>
     *Selected columns can be from both relations.
     *Join columns should be of type string. 
     *The join column of relation B should be the primary key for now. In other
     *words, the result list is merely a copy of the first relation with just
     *those rows that fullfill the requirements.
     *Value can be of any types. In case valueColumnB is given as null
     *the condition is dismissed.
     *Result relation C should exist and be named but columns will be inserted
     *if they don't exist yet.
     *The distinct is implemented as a simple remove on all rows in the result 
     *relation C.
     */
    public static boolean selectRelationJoin2Relations( DBMS dbms, 
        Relation relationA, Relation relationB, Relation relationC,
        String[] relationAselectColumnList, String[] relationBselectColumnList,
        String joinColumnA, String joinColumnB, String valueColumnB,
        Object value, boolean distinct
        ) {
        
        
        // Insert columns if needed. Even include another fkc if one was present before.
        for ( int relPick =0;relPick<2;relPick++) {
            Relation rel = relationA;
            String[] relationSelectColumnList = relationAselectColumnList;
            if ( relPick==1 ) {
                rel = relationB;
                relationSelectColumnList = relationBselectColumnList;
            }            
            for (int i=0;i<relationSelectColumnList.length;i++) {                
                String selectColumnName = relationSelectColumnList[i];
                if ( ! rel.containsColumn( selectColumnName)) {                    
                    int dataType = rel.getColumnDataType(selectColumnName);
                    ForeignKeyConstr fkc = dbms.foreignKeyConstrSet.getForeignKeyConstrFrom(rel.name,selectColumnName);
                    ForeignKeyConstr fkcNew = null;
                    if ( fkc != null ) {
                        fkcNew = new ForeignKeyConstr(dbms,
                            relationC.name,selectColumnName, 
                            fkc.toRelationName, fkc.toColumnLabel);
                    }
                    //General.showDebug("Adding column: " + selectColumnName + " of type: " + Relation.dataTypeList[dataType] +
                    //    " and fkc: " + fkcNew);
                    relationC.insertColumn( selectColumnName, dataType, fkcNew);
                }
            }
        }
        
        /** Get the row rids in relation B
         */
        BitSet selectedBitSetRelationB = null;
        if ( valueColumnB == null ) {
            selectedBitSetRelationB = new BitSet(relationB.sizeMax);
            selectedBitSetRelationB.or( relationB.used );
        } else {        
            selectedBitSetRelationB = selectBitSet(dbms, relationB, valueColumnB, 
                OPERATION_TYPE_EQUALS, value, false);           
            if ( selectedBitSetRelationB == null ) {
                General.showError("Failed to do selectBitSet on relation: " + relationB.name);
                General.showError(" from column: " + valueColumnB);
                General.showError(" for equality to value: " + value);
                return false;
            }
        }
        
        
        /** Do the actual join between relations A and B. The join looks up 
         elements in relation B by index AND only uses selected rids in B.
         * This can not lead to double elements in relation C. Remember
         *that the join column of relation B should its primary keys.
         */
        Index indexRelationBJoin = relationB.getIndex( joinColumnB, 
            Index.INDEX_TYPE_SORTED);
        if ( indexRelationBJoin == null ) {
            General.showError("Failed to relationB.getIndex.");
            return false;
        }
        int fromRid, ridB;
        String joinValue;
//        Object setValue;
        Relation rel;
        String selectColumnName;
        String[] relationSelectColumnList;
        String[] joinColumnAList = (String[]) relationA.getColumn( joinColumnA);
        for (int ridA=relationA.used.nextSetBit(0); ridA>=0; ridA=relationA.used.nextSetBit(ridA+1)) {
            joinValue = joinColumnAList[ridA];
            ridB = indexRelationBJoin.getRid(joinValue);
            if ( (ridB >= 0) && selectedBitSetRelationB.get(ridB) ) {
                // It's a match; copy the values over. This is an Object expensive part!
                int ridC = relationC.getNewRowId();
                if ( ridC<0 ) {
                    General.showError("Failed to relationC.getNewRowId ");
		    return false;
                }
                for ( int relPick =0;relPick<2;relPick++) {
                    rel = relationA;
                    relationSelectColumnList = relationAselectColumnList;
                    fromRid = ridA;
                    if ( relPick==1 ) {
                        rel = relationB;
                        relationSelectColumnList = relationBselectColumnList;
                        fromRid = ridB;
                    }            
                    for (int i=0;i<relationSelectColumnList.length;i++) {                
                        selectColumnName = relationSelectColumnList[i];
                        if ( ! relationC.copyValue( rel, fromRid, selectColumnName, ridC, selectColumnName)) {
                            General.showError("Failed to copyValue for relation: " + relationC.name +
                                " column: " + selectColumnName );
            		    return false;
                        }                        
                    }
                }                
            }            
        }
        // Check for double rows
        if (distinct) {
            String[] selectedColumnsRelationC = Strings.join( relationAselectColumnList, 
                                                              relationBselectColumnList);
            if ( ! relationC.removeDuplicates( selectedColumnsRelationC ) ) {
                General.showError("Failed to removeDuplicates for relation: " + relationC.name +
                    " columns: " + Strings.toString( selectedColumnsRelationC) );
                return false;
            }
        }
        
        return true;
    }
    
    /** Like the SQL equivelant:
     *<PRE>
INSERT INTO a [IGNORE] (a.column_1, ...)
  SELECT (b.column_1, ...)
  FROM b,c
  WHERE b.column_join=c.column_join
      AND c.column=value
     *</PRE>
     *
     *Columns can be selected from relations b anc c. They will be named
     *the same in relation a.
     *Join columns should be of type string.
     *Join column on c should be c's primary key; no expansion will be done.
     *Value can be of all types.
     *If 'ignore' is set then double values will not be inserted (TODO).
     */
    public static boolean insertInto1( DBMS dbms, 
        Relation relationA, Relation relationB, Relation relationC, 
        String joinColumnB, String joinColumnC,
        String[] relationBselectColumnList, 
        String[] relationCselectColumnList,
        String valueColumnC, Object value, boolean ignore
        ) {        
        if ( ! selectRelationJoin2Relations( dbms, 
            relationB,  relationC,  relationA,
            relationBselectColumnList, relationCselectColumnList,
            joinColumnB, joinColumnC, valueColumnC, value, ignore)) {
            General.showError("Failed to selectRelationJoin2Relations for: \n" + 
                "relation A:    " + relationA.name + General.eol +
                "relation B:    " + relationB.name + General.eol +
                "relation C:    " + relationC.name + General.eol +
                "relationBselectColumnList: " + Strings.toString(relationBselectColumnList) + General.eol +
                "relationCselectColumnList: " + Strings.toString(relationCselectColumnList) + General.eol +
                "joinColumnB:   " + joinColumnB + General.eol +
                "joinColumnC:   " + joinColumnC + General.eol +
                "valueColumnC:  " + valueColumnC + General.eol +
                "value:         " + value );
            return false;
        }
                
        
        return true;
    }
     
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }
    
}
