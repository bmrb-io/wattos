/*
 * ForeignKey.java
 *
 * Created on June 13, 2003, 11:02 AM
 */

package Wattos.Database;

import java.io.Serializable;
import java.util.BitSet;

import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexSortedInt;
import Wattos.Database.Indices.IndexSortedString;
import Wattos.Utils.General;

/**
 * Implements an object to enforce a foreign key reference with associated behavior as
 *specified by onUpdateDo and onDeleteDo.
 *Current implementation only allows singular foreign keys; i.e. not superkeys (a set of
 *keys making the primary key). 
 TODO:
 Also limited to the physical row index for the moment;
 *specified by a null pointer for the columnLabel. Please change this code if keys to
 *non-physical row ids (any column) will be supported. Then the remove/relabel of
 *columns needs to be recoded in the fkcSet class.
 **********************
 * @author Jurgen F. Doreleijers
 */
public class ForeignKeyConstr implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;    

    /** NOT implemented */
    public static final int ACTION_TYPE_NO_ACTION    = 1; // Default as in SQL-92
    /** Only one actually implemented */
    public static final int ACTION_TYPE_CASCADE      = 2;
    /** NOT implemented */
    public static final int ACTION_TYPE_SET_DEFAULT  = 3;
    /** NOT implemented */
    public static final int ACTION_TYPE_SET_NULL     = 4;
    /** NOT implemented */
    public static final int ACTION_TYPE_DEFAULT      = ACTION_TYPE_NO_ACTION;
    
    public static final String[] ACTION_DESCR = { "INVALID ACTION", "NO ACTION", "CASCADE", "SET DEFAULT", "SET NULL" };
    
    public static final int MAXIMUM_ERRORS_TO_SHOW = 10;
    /** By default a reference can not be nulled */
    public static final boolean DEFAULT_REFERENCE_NULLABLE = false;

    public String   fromRelationName;
    public String   fromColumnLabel;
    public String   toRelationName;
    /** If the columnLabel is null then the physical row index list is used as the primary key in the toRelation
     */
    public String   toColumnLabel;
    /** Choose from action types above */
    public int      onDeleteDo;
    /** Choose from action types above */
    public int      onUpdateDo;
    /** Is the ref null-able? */
    public boolean  refNullable;
    public DBMS     dbms;
        

    /** Allows fkc to physical column with cascading behavior.
     */
    public ForeignKeyConstr( DBMS dbms,
                             String fromRelationName, String fromColumnLabel, 
                             String toRelationName, String toColumnLabel) {
        this.dbms   = dbms;
        this.fromRelationName   = fromRelationName;
        this.fromColumnLabel    = fromColumnLabel;
        this.toRelationName     = toRelationName;
        this.toColumnLabel      = toColumnLabel;
        this.onDeleteDo     = ACTION_TYPE_CASCADE;        
        this.onUpdateDo     = ACTION_TYPE_CASCADE;        
        this.refNullable    = DEFAULT_REFERENCE_NULLABLE;
    }    
        
    /** Checks the key data types and returns true only if they're the same
     */
    public boolean keysAreOfCompatibleTypes( int fromDataType, int toDataType) {

        if ( toDataType == Relation.DATA_TYPE_INVALID ) {
            return false;
        }
    
        if ( fromDataType == toDataType ) {
            return true;
        }
    
        if ( fromDataType == Relation.DATA_TYPE_ARRAY_OF_INT &&
               toDataType == Relation.DATA_TYPE_INT ) {
            return true;
        }
        if (   toDataType == Relation.DATA_TYPE_ARRAY_OF_INT &&
             fromDataType == Relation.DATA_TYPE_INT ) {
            return true;
        }
        return false;
    }
    
    public String toString() {
        return toString(false);
    }
    
    public String toString( boolean showActionTypesEtc ) {
        StringBuffer sb = new StringBuffer();
        sb.append( "ForeignKeyConstraint from relation: [");
        sb.append( fromRelationName );
        sb.append( "] at column: [");
        sb.append( fromColumnLabel );
        sb.append( "] references relation: [" );
        sb.append( toRelationName );
        sb.append( "] at column: [");
        sb.append( toColumnLabel );
        sb.append( "]");
        if ( showActionTypesEtc ) {
            sb.append( " at column: [");
            sb.append( toColumnLabel );
            sb.append( ']' );
            if ( onDeleteDo != ACTION_TYPE_DEFAULT ) {
                sb.append( " ON DELETE ");
                sb.append( ACTION_DESCR[onDeleteDo] );
                sb.append( "; " );
            }
            if ( onUpdateDo != ACTION_TYPE_DEFAULT ) {
                sb.append( " ON UPDATE ");
                sb.append( ACTION_DESCR[onDeleteDo] );
                sb.append( "; " );
            }
        }
        return sb.toString();
    }
    

    /** Checks for each foreign key constraint that the target record exist and are in use.
     *If any errors need to be reported they will be reported as warnings because
     *the reason this check is executed is to check for possible inconsistencies.
     *Only returns true if all are well.
     */
    public boolean checkConsistency( boolean showChecks, boolean showErrors ) {
        BitSet badRows = getInconsistentRows( showChecks, showErrors );
        if ( badRows == null ) {
            General.showError("Failed to do checkConsistency because getInconsistentRows failed");
            return false;            
        }
        if ( badRows.cardinality() == 0 ) {
            return true;
        }
        General.showDebug("fkc not consistent because it had bad rows numbered:" + badRows.cardinality() );
        General.showDebug("fkc: " + this);
        return false;
    }
    

    /** Checks for each foreign key constraint that the target record exist and are in use.
     *If any errors need to be reported they will be reported as warnings because
     *the reason this check is executed is to check for possible inconsistencies.
     *Only returns true if all are well.
     */
    public boolean makeConsistentByRemoveInconsistentRows( boolean showChecks, boolean showErrors ) {
        BitSet badRows = getInconsistentRows( showChecks, showErrors );
        if ( badRows == null ) {
            return false;            
        }
        Relation relation = dbms.getRelation( fromRelationName );        
        return relation.removeRows( badRows, false, true);
    }
    
    
    
    /** Checks for each row that the target records exist and is in use.
     *If any errors need to be reported they will be reported as warnings because
     *the reason this check is executed is to check for possible inconsistencies.
     *Returns null on error and a BitSet with all false values for no errors.
     *Inconsistent rows are identified by a true value in the result.
     */
    public BitSet getInconsistentRows( boolean showChecks, boolean showErrors ) {
        int reported_errors = 0;
        Relation fromRelation = dbms.getRelation( fromRelationName );
        Relation toRelation = dbms.getRelation( toRelationName );
        BitSet fromUsed = fromRelation.used;
        BitSet toUsed = toRelation.used;
        BitSet result = new BitSet(fromUsed.size());
        result.clear();
        
        /** Can rows be checked ? */
        if ( fromRelation.sizeRows == 0 ) {
            if ( showChecks ) {
                General.showDebug("No rows in from Relation so no problems." );
            }
            return result;
        }
        // Support fkcs on INT and STRINGNR only.
        int dataTypeFkc = fromRelation.getColumnDataType(fromColumnLabel);
        // Never assume the other one is the same.
        if ( toColumnLabel == null ) {
            if (dataTypeFkc != Relation.DATA_TYPE_INT) {
                 General.showError("-1- Data types for columns aren't the same for fkc: " + this);
                 return null;
            } 
        } else if (dataTypeFkc != toRelation.getColumnDataType(toColumnLabel)) {
             General.showError("-2- Data types for columns aren't the same for fkc: " + this);
             return null;
        }
                
        switch ( dataTypeFkc ) {
            case Relation.DATA_TYPE_INT: {
                int[] fromColumn = (int[]) fromRelation.getColumn(fromColumnLabel);
                if ( toColumnLabel == null ) { // FKC points to physical rows
                    /** Very fast checks */
                    for (int r=fromUsed.nextSetBit(0); r>=0; r=fromUsed.nextSetBit(r+1)) {
                        //General.showDebug("Checking from row id: " + r + " to rid: " + fromColumn[r]);
                        // Pretend that every column can be nullable so don't error about null values just warn.
                        if ( Defs.isNull( fromColumn[r] )) {
                            if ( refNullable ) {
//                                General.showDebug("In getInconsistentRows (1): found nullable ref to be null in fromRelation("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: NULL"  );
                            } else {
                                if ( showErrors ) {
                                    if ( reported_errors <= MAXIMUM_ERRORS_TO_SHOW ) {
                                            General.showWarning("Null reference in fromRelation("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: NULL"  );
                                        if ( reported_errors == MAXIMUM_ERRORS_TO_SHOW ) {
                                            General.showWarning("and perhaps more than these " + MAXIMUM_ERRORS_TO_SHOW);
                                        }
                                    }                    
                                }
                                reported_errors++;                
                                result.set(r);         
                            }
                            continue; // don't check if it is a valid index because we already know that.
                        }            
                        // Found an inconsistency because the row doesn't exist that is referred to.            
                        if ( ! toUsed.get( fromColumn[r] ) ) {
                            // Let's see if we want to (keep) reporting errors
                            if ( showErrors ) {
                                if ( reported_errors <= MAXIMUM_ERRORS_TO_SHOW ) {
                                        General.showWarning("Invalid index fromRelation A ("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: " + fromColumn[r]  );
                                    if ( reported_errors == MAXIMUM_ERRORS_TO_SHOW ) {
                                            General.showWarning("and perhaps more than these " + MAXIMUM_ERRORS_TO_SHOW);
                                    }
                                }

                            }
                            reported_errors++;                
                            result.set(r);         
                        }
                    }
                } else {
                    // The fkc was to an actual column and not to the row idx.                   
                    IndexSortedInt index = (IndexSortedInt) toRelation.getIndex(toColumnLabel, Index.INDEX_TYPE_SORTED);
                    if ( index == null ) {
                        General.showError("When doing: getInconsistentRows for fkc: " + this);
                        General.showError("Can't get index for column: " + toColumnLabel);
                        return null;
                    }
		    int rid = -1;
                    int value = Defs.NULL_INT;
                    /** Very fast checks */
                    for (int r=fromUsed.nextSetBit(0); r>=0; r=fromUsed.nextSetBit(r+1)) {
                        value = fromColumn[r];
                        //General.showDebug("Checking from row id: " + r + " to value: " + value);
                        if ( Defs.isNull( value )) {
                            if ( refNullable ) {
                                //General.showDebug("In getInconsistentRows (2): found nullable ref to be null in fromRelation("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: NULL"  );
                            } else {
                                if ( showErrors ) {
                                    if ( reported_errors <= MAXIMUM_ERRORS_TO_SHOW ) {
                                            General.showWarning("Null reference in fromRelation("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: NULL"  );
                                        if ( reported_errors == MAXIMUM_ERRORS_TO_SHOW ) {
                                            General.showWarning("and perhaps more than these " + MAXIMUM_ERRORS_TO_SHOW);
                                        }
                                    }                    
                                }
                                reported_errors++;                
                                result.set(r);         
                            }
                            continue; // don't check if it is a valid index because we already know that.
                        }            
                        rid = index.getRid(value);
                        // Found an inconsistency because the row doesn't exist that is referred to.            
                        if ( rid < 0 ) {
                            // Let's see if we want to (keep) reporting errors
                            if ( showErrors ) {
                                if ( reported_errors <= MAXIMUM_ERRORS_TO_SHOW ) {
                                        General.showWarning("Invalid index fromRelation B ("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: " + fromColumn[r]  );
                                    if ( reported_errors == MAXIMUM_ERRORS_TO_SHOW ) {
                                        General.showWarning("and perhaps more than these " + MAXIMUM_ERRORS_TO_SHOW);
                                    }
                                }

                            }
                            reported_errors++;                
                            result.set(r);         
                        }
                    } 
                }
                break;
            }
            case Relation.DATA_TYPE_STRING:
            case Relation.DATA_TYPE_STRINGNR: {
                // The fkc was to an actual column and not to the row idx.                   
                IndexSortedString index = (IndexSortedString) toRelation.getIndex(toColumnLabel, Index.INDEX_TYPE_SORTED);
                if ( index == null ) {
                    General.showError("When doing: getInconsistentRows for fkc: " + this);
                    General.showError("Can't get index for column: " + toColumnLabel);
                    return null;
                }
                String[] fromColumn = (String[]) fromRelation.getColumn(fromColumnLabel);
                int rid = -1;
                String value = Defs.NULL_STRING_NULL;
                /** Very fast checks */
                for (int r=fromUsed.nextSetBit(0); r>=0; r=fromUsed.nextSetBit(r+1)) {
                    value = fromColumn[r];
                    //General.showDebug("Checking from row id: " + r + " to value: " + value);
                    if ( Defs.isNullString( value )) {
                        if ( refNullable ) {
                            General.showDebug("In getInconsistentRows (3): found nullable ref to be null in fromRelation("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: NULL"  );
                        } else {
                            if ( showErrors ) {
                                if ( reported_errors <= MAXIMUM_ERRORS_TO_SHOW ) {
                                        General.showWarning("Null reference in fromRelation("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: NULL"  );
                                    if ( reported_errors == MAXIMUM_ERRORS_TO_SHOW ) {
                                            General.showWarning("and perhaps more than these " + MAXIMUM_ERRORS_TO_SHOW);
                                    }
                                }                    
                            }
                            reported_errors++;                
                            result.set(r);         
                        }
                        continue; // don't check if it is a valid index because we already know that.
                    }            
                    rid = index.getRid(value);
                    // Found an inconsistency because the row doesn't exist that is referred to.            
                    if ( rid < 0 ) {
                        // Let's see if we want to (keep) reporting errors
                        if ( showErrors ) {
                            if ( reported_errors <= MAXIMUM_ERRORS_TO_SHOW ) {
                                    General.showWarning("Invalid index fromRelation C ("+fromRelationName+","+fromColumnLabel+") on row: " + r + " toRelation("+toRelationName+") at row: " + fromColumn[r]  );
                                if ( reported_errors == MAXIMUM_ERRORS_TO_SHOW ) {
                                            General.showWarning("and perhaps more than these " + MAXIMUM_ERRORS_TO_SHOW);
                                }
                            }

                        }
                        reported_errors++;                
                        result.set(r);         
                    }
                }
                break;
            }
            default: {
                General.showError("Only INT, STRING, and STRINGNR data types for fkcs supported for now.");
                return null;
            }
        }
                        
        if ( showChecks ) {
            int found_errors = result.cardinality();
            if ( found_errors > 0 ) {
                General.showOutput("Found number of invalid keys: " + found_errors );
            } else {
                General.showOutput("All keys are valid" );
            }
        }        
        return result;
    }
    

    /** Compares the individual components including the dbms */
    public boolean equals( ForeignKeyConstr other ) {
        if(!(   fromRelationName.equals( other.fromRelationName ) &&
                fromColumnLabel.equals( other.fromColumnLabel ) &&
                toRelationName.equals( other.toRelationName ) &&
                (dbms == other.dbms ) )) {
            General.showDebug("Fkcs are different");
            return false;
        }
        //General.showDebug("Fkcs are equal");        
        return true;
    }    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }    
}
