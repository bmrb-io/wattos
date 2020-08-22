/*
 * ForeignKeyConstrSet.java
 *
 * Created on June 16, 2003, 8:16 AM
 */

package Wattos.Database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import Wattos.Utils.General;
import Wattos.Utils.StringArrayList;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 *Set of <code>ForeignKeyConstr</code>.
 * @author Jurgen F. Doreleijers
 */
public class ForeignKeyConstrSet implements Serializable {
    
    private static final long serialVersionUID = -1207795172754062330L;    

    /** Hashed by from Relation / column. So it's a 2D hashmap-hashmap; i.e. hashmap of hashmap of fkc.
     * The keys are relation name and column label.
     */
    HashMap foreignKeyConstrMapFrom;
    /** Hashed by to Relation / column (inverse indexed). This is a 3D structure;
     *i.e. hashmap(toRelation name) of hashmap(toColumnLabel) of ArrayList(fkc). A key can be specified
     *for the same to key multiple times but not the other way around.
     */
    HashMap foreignKeyConstrMapTo;

    private int fkcCount;
    DBMS dbms;

    /** Creates a new instance of ForeignKeyConstrSet.
     *Not sure if dbms reference is needed...
     */
    public ForeignKeyConstrSet(DBMS dbms) {
        init(dbms);
    }
    
    public boolean init(DBMS dbms) {
        fkcCount                = 0;
        this.dbms               = dbms;
        foreignKeyConstrMapFrom = new HashMap();
        foreignKeyConstrMapTo   = new HashMap();        
        return true;
    }


    /** Assumes that the tables have already been
     *registered with the dbms. But the columns need to exist.
     */
    public boolean addForeignKeyConstr( ForeignKeyConstr fkc ) {
        return addForeignKeyConstr( fkc, true );
    }
    
    /** Doesn't assume the referenced columns exist */
    public boolean addForeignKeyConstr( ForeignKeyConstr fkc, boolean checkExistanceColumns ) {
        
        
        /** Checks if both relations exist */
        if ( ! dbms.containsRelation( fkc.fromRelationName ) ) {
            General.showError("dbms doesn't contain from relation with name: " + fkc.fromRelationName);
            General.showError("fkc will not be added.");
            return false;
        }                        
        if ( ! dbms.containsRelation( fkc.toRelationName ) ) {
            General.showError("dbms doesn't contain to relation with name: " + fkc.toRelationName);
            General.showError("fkc will not be added.");
            return false;
        }                        
            
        Relation fromRelation   = dbms.getRelation( fkc.fromRelationName );
        Relation toRelation     = dbms.getRelation( fkc.toRelationName );
        
        if ( checkExistanceColumns ) {
            /** Checks if both columns in the relations exist */
            if ( ! fromRelation.containsColumn( fkc.fromColumnLabel )) {
                General.showError("from relation doesn't contain column with label: " + fkc.fromColumnLabel);
                General.showError("relation reads:\n" + fromRelation);
                General.showError("fkc will not be added.");
                return false;
            }         
            if ( fkc.toColumnLabel != null ) {
                // physical column
                if ( ! toRelation.containsColumn( fkc.toColumnLabel )) {
                    General.showError("to relation doesn't contain column with label: " + fkc.toColumnLabel);
                    General.showError("fkc will not be added.");
                    return false;
                }                        
            }
            
            /** Check for data type compatibility */
            /** Can't be checked if the columns don't need to exist;-) */
            int fromColumnType  = fromRelation.getColumnDataType(fkc.fromColumnLabel);
            int toColumnType    =   toRelation.getColumnDataType(fkc.toColumnLabel);
            if ( ! fkc.keysAreOfCompatibleTypes(fromColumnType, toColumnType) ) {
                General.showError("fkc not added because the data types are incompatible or so. Fkc:\n" + fkc);
                General.showError("fromColumnType " + fromColumnType );
                General.showError("toColumnType   "   + toColumnType );
                return false;
            }            
        }
        
        
        /** Actually add them to the set */
        HashMap hashMapFrom = (HashMap) foreignKeyConstrMapFrom.get(    fkc.fromRelationName );
        HashMap hashMapTo   = (HashMap) foreignKeyConstrMapTo.get(      fkc.toRelationName );
        // Create hashmap for this from relation yet.
        if ( hashMapFrom == null ) {
            hashMapFrom = new HashMap ();
            foreignKeyConstrMapFrom.put( fkc.fromRelationName, hashMapFrom );
        }
        // Create hashmap for this to relation yet.
        if ( hashMapTo == null ) {
            hashMapTo = new HashMap ();
            foreignKeyConstrMapTo.put( fkc.toRelationName, hashMapTo );
        }
        
        /** only 1 from constraint for each from column possible */
        if ( hashMapFrom.containsKey( fkc.fromColumnLabel ) ) {
            General.showError("fkc already present in from hashmap for fkc: " + fkc);                
            General.showError("fkc will not be added.");
            return false;
        }
        hashMapFrom.put( fkc.fromColumnLabel, fkc );
        
        /** Multiple keys for each to column possible */
        ArrayList listTo;
        if ( hashMapTo.containsKey( fkc.toColumnLabel ) ) {
            listTo = (ArrayList) hashMapTo.get(fkc.toColumnLabel );
        } else {
            listTo = new ArrayList();
            hashMapTo.put( fkc.toColumnLabel, listTo );
        }
        if ( listTo.contains( fkc ) ) {
            General.showError("code bug; equality is already checked in the for hashmap for fkc: " + fkc);            
            General.showError("fkc will not be added.");
            return false;            
        }
        listTo.add( fkc );
        
        fkcCount++;
        return true;
    }
    
    
    /** Returns ForeignKeyConstr if fkc from is present or null if not. */
    public ForeignKeyConstr getForeignKeyConstrFrom( String fromRelationName, String fromColumnLabel ) {
        if ( ! containsForeignKeyConstrFrom(fromRelationName, fromColumnLabel) ) {
            return null;
        }
        HashMap hashMapFrom = (HashMap) foreignKeyConstrMapFrom.get( fromRelationName );
        return (ForeignKeyConstr) hashMapFrom.get( fromColumnLabel );
    }

    
    /** Returns null if not present; 
     * Needs to iterate over the columns in the fromRelation for which there are fkcs. */
    public ArrayList getForeignKeyConstrListFromRelationToRelation(String fromRelationName, String toRelationName) {
        ArrayList result = new ArrayList();
        HashMap hashMapFrom = (HashMap) foreignKeyConstrMapFrom.get( fromRelationName );
        if ( hashMapFrom == null ) {
//            General.showDebug("no hashMapFrom for fromRelationName: " + fromRelationName);
            return null;
        }
        Set fromColumnLabelSet = hashMapFrom.keySet();
        for (Iterator it=fromColumnLabelSet.iterator();it.hasNext();) {
            String fromColumnLabel = (String) it.next();            
            ForeignKeyConstr fkc = (ForeignKeyConstr) hashMapFrom.get( fromColumnLabel );
//            General.showDebug("Looking for fkcs from: "+ fromRelationName+" to " +toRelationName + " and for fromColumnLabel: " + fromColumnLabel+" found fkc: " +fkc);
            if ( fkc.toRelationName.equals(  toRelationName )) {
                result.add(fkc);
//                General.showDebug("Added fkc: " +fkc);
            }        
        }
        if ( result.size() == 0 ) {
//            General.showDebug("no fkc for toRelationName: "  + toRelationName);
//            General.showDebug("in fromRelationName       : " + fromRelationName);
            return null;
        }
        return result;
    }
    
    /** Returns null if not present; 
     * Needs to iterate over the columns in the fromRelation for which there are fkcs. */
    public StringArrayList getFromColumnLabelListFromRelationToRelation(String fromRelationName, String toRelationName) {
        StringArrayList result = new StringArrayList();
        ArrayList fkcList = getForeignKeyConstrListFromRelationToRelation(fromRelationName, toRelationName);
//        General.showDebug("Found fkcList: " + fkcList);
        if ( fkcList == null ) {
            return null;
        }
        for (int i=0;i<fkcList.size();i++) {
            ForeignKeyConstr fkc =(ForeignKeyConstr) fkcList.get(i); 
            result.add(fkc.fromColumnLabel);
//            General.showDebug("Added fkc number: " + result.size());
        }
        return result;
    }
    
    /** checks */
    public boolean containsForeignKeyConstrFrom( String fromRelationName, String fromColumnLabel ) {
        HashMap hashMapFrom = (HashMap) foreignKeyConstrMapFrom.get( fromRelationName );
        if ( hashMapFrom == null ) {
            return false;
        }
        ForeignKeyConstr fkc = (ForeignKeyConstr) hashMapFrom.get( fromColumnLabel );
        if ( fkc == null ) {
            return false;
        }        
        return true;
    }
    
    /** Removes the fkc in both caching variables. Returns false on any error. 
     *If the fkc doesn't exist then this is considered an error too.
     */
    public boolean removeForeignKeyConstrFrom( String fromRelationName, String fromColumnLabel ) {
        ForeignKeyConstr fkc = getForeignKeyConstrFrom( fromRelationName, fromColumnLabel );
        if ( fkc == null ) {
            General.showWarning("No fkc removed from relation with name: " + fromRelationName + " and column label: " + fromColumnLabel);
            return false;
        }
        // Ok, we now have the object.
        // Remove in from
        HashMap hashMapFrom = (HashMap) foreignKeyConstrMapFrom.get( fromRelationName );
        if ( hashMapFrom == null ) {
            General.showError("Failed to find hashMapFrom for fromRelationName: " + fromRelationName);
            return false;
        }

        ForeignKeyConstr fkc_tmp = (ForeignKeyConstr) hashMapFrom.remove( fromColumnLabel );
        if ( !fkc_tmp.equals( fkc )) {
            General.showError("-1-fkc removed is not identical to fkc looked up by key in from hashmap; are they equal?");
            General.showError("-1-fkc removed: " + fkc_tmp);
            General.showError("-1-fkc of key : " + fkc);
            return false;
        }
        if ( hashMapFrom.size() == 0 ) {
            foreignKeyConstrMapFrom.remove(hashMapFrom);
        }
        
        // Remove the same fkc now in the to HoHoL
        HashMap hashMapTo = (HashMap) foreignKeyConstrMapTo.get( fkc.toRelationName );
        if ( hashMapTo == null ) {
            General.showError("Failed to find hashMapFrom for toRelation: " + fkc.toRelationName);
            return false;
        }        
        if ( hashMapTo.size() == 0 ) {
            foreignKeyConstrMapTo.remove(hashMapTo);
            General.showError("Found hashMapFrom for toRelation: " + fkc.toRelationName);
            return false;
        }
        ArrayList fkc_list = (ArrayList) hashMapTo.get( fkc.toColumnLabel );
        if ( fkc_list == null ) {
            General.showError("Failed to find a list of fkcs for toRelation: " + fkc.toRelationName + " and toColumnLabel: " + fkc.toColumnLabel);
            return false;
        }        
        if ( fkc_list.size() == 0 ) {
            foreignKeyConstrMapTo.remove(hashMapTo);
            General.showError("Found an empty list of fkcs for toRelation: " + fkc.toRelationName + " and toColumnLabel: " + fkc.toColumnLabel);
            return false;
        }
        int idx = fkc_list.indexOf( fkc );
        if ( idx < 0 ) {
            General.showError("Failed to find fkc in list of fkcs for toRelation: " + fkc.toRelationName + " and toColumnLabel: " + fkc.toColumnLabel);
            return false;
        }
        fkc_tmp = (ForeignKeyConstr) fkc_list.remove( idx );
        if ( fkc_tmp != fkc ) {
            General.showError("-2-fkc removed is not identical to fkc looked up by key in to hashmap; are they equal?");
            General.showError("-2-fkc removed: " + fkc_tmp);
            General.showError("-2-fkc of key: " + fkc);
            return false;
        }
        // Now there's no more pointer to the fkc and it will be gc-ed (we hope).
        fkcCount--;
        return true;
    }

    /** 
     *Algoritm: <BR>
     <UL>     
     *<LI>Look up any affected fkcs in this set.
     *<LI>If present clone them.
     *<LI>Remove originals from this set.
     *<LI>Add modified fkcs to set again.
     </UL>
     */
    public boolean renameColumn( String fromRelationName, String fromColumnLabelOld, String fromColumnLabelNew  ) {
                
        // Check for fkc from the fromRelation
        ForeignKeyConstr fkc = getForeignKeyConstrFrom(fromRelationName, fromColumnLabelOld);
        if ( fkc != null ) {
            // Make a new one that has a relabelled column
            ForeignKeyConstr fkc_new = new ForeignKeyConstr( dbms, 
                fromRelationName, fromColumnLabelNew, 
                fkc.toRelationName, fkc.toColumnLabel);
            if ( ! removeForeignKeyConstrFrom(fromRelationName,fromColumnLabelOld) ) {
                General.showError("Failed to remove original fkc for renaming: " + fkc );
                return false;
            }
            if ( ! addForeignKeyConstr(fkc_new,true)) {
                General.showError("Failed to add new fkc with renamed column: " + fkc_new );
                return false;
            }                                
        } else {
            //General.showDebug("No fkcs present in from relation: " + fromRelationName + " with column labelled: " + fromColumnLabelOld);
        }
        
        /**
        ArrayList fkcsToDo = new ArrayList();
        fkc = .getForeignKeyConstrFrom(fromRelationName, fromColumnLabelOld);
        if ( fkc != null ) {
            fkcsToDo.add( fkc );
        } else {
            General.showDebug("No fkcs present from relation with name: " + fromRelationName);
        }        
        
        if ( hashMapTo == null ) {
            General.showWarning("No fkcs present from relation with name: " + fromRelationName);
        }
        
        /** only 1 from constraint for each from column possible 
        if ( hashMapFrom.containsKey( fkc.fromColumnLabel ) ) {
            General.showError("fkc already present in from hashmap for fkc: " + fkc);                
            General.showError("fkc will not be added.");
            return false;
        }
        hashMapFrom.put( fkc.fromColumnLabel, fkc );
        .foreignKeyConstrMapFrom.g
        General.showWarning("In renameColumn Still need to code relabelling foreign key constraints if present");
        */
        return true;        
    }
        
    /** Default method */
    public String toString() {
        Parameters p = new Parameters(); // for printf
        StringBuffer sb = new StringBuffer();
        sb.append( "Foreign Key Constraints in from map:\n");
        Set keys1 = foreignKeyConstrMapFrom.keySet();
        ArrayList keyList1 = new ArrayList( keys1 );
        if ( keys1.size() == 0 ) {
            sb.append( "None\n");
            return sb.toString();
        }
        Collections.sort( keyList1 );
            
        sb.append( Format.sprintf( "%-20s%-20s%-20s%-20s\n", 
            p.add( "FROM relation name").add("FROM column name").add("TO relation name").add("TO column name" )));
        // From set
        for (Iterator it1= keyList1.iterator(); it1.hasNext(); ) {
            String fromRelationName = (String) it1.next();
            sb.append( Format.sprintf( "%-20s",  p.add( fromRelationName )));
            HashMap hashMapFrom = (HashMap) foreignKeyConstrMapFrom.get(fromRelationName);
            Set keys2 = hashMapFrom.keySet();
            ArrayList keyList2 = new ArrayList( keys2 );
            Collections.sort( keyList2 );
            if ( keys2.isEmpty() ) {
                sb.append(General.eol);
            } else {
                int count = 0;
                for (Iterator it2= keyList2.iterator(); it2.hasNext(); ) {
                    String fromColumnLabel = (String) it2.next();
                    ForeignKeyConstr fkc = (ForeignKeyConstr) hashMapFrom.get( fromColumnLabel );
                    if ( count != 0 ) {
                        sb.append( Format.sprintf( "%-20s", p.add("")));
                    }
                    sb.append( Format.sprintf( "%-20s%-20s%-20s\n", 
                        p.add( fromColumnLabel).add( fkc.toRelationName).add( fkc.toColumnLabel )));
                    count++;
                }
            }
        }
        return sb.toString();
    }

    /** Checks for each foreign key constraint that the target record exist and are in use.
     *Only returns true if all are well. Although slightly unhandy to code the fkcs will
     *be checked by toRelation.
     */
    public boolean checkConsistencySet( boolean showChecks, boolean showErrors ) {
        boolean overall_status = true; // All's well that ends well.
        Set keys1 = foreignKeyConstrMapTo.keySet();
        for (Iterator it1= keys1.iterator(); it1.hasNext(); ) {
            String toRelationName = (String) it1.next();
            HashMap hashMapTo = (HashMap) foreignKeyConstrMapTo.get(toRelationName);
            Set keys2 = hashMapTo.keySet();
            for (Iterator it2= keys2.iterator(); it2.hasNext(); ) {
                String toColumnName = (String) it2.next();
                ArrayList list = (ArrayList) hashMapTo.get(toColumnName);
                // Safety check
                if ( list == null ) {
                    break;
                }
                for (int i=0;i<list.size();i++) {
                    ForeignKeyConstr fkc = (ForeignKeyConstr) list.get(i);
                    //General.showDebug("checking fkc: " + fkc );
                    if ( showChecks ) {
                        General.showOutput("Checking fkc: " + fkc);
                    }
                    boolean status = fkc.checkConsistency(showChecks,showErrors);
                    if ( ! status ) {
                        overall_status = false;
                    }
                }
            }
        }
        return overall_status;
    }

    /** Create a fkc set on the basis of a list of tuples with the from column
     *name and the to Relation name assuming it will be a cascading fkc to the
     *physical rid. Returns null if list is empty.
     */
    public static ForeignKeyConstrSet createFromRelation( DBMS dbms, ArrayList list, String fromRelationName ) {
        if ( list == null || list.size() == 0 ) {
            return null;
        }
        ForeignKeyConstrSet fkcSet = new ForeignKeyConstrSet(dbms);
        
        String toColumnName = null; // Indicating the physical order.
        for (int i=0;i<list.size();i++) {
            String[] name = (String[]) list.get(i);
            /** Just for documentation we name them */
            String fromColumnName = name[0];        
            String toRelationName = name[1];        
            ForeignKeyConstr fkc = new ForeignKeyConstr(dbms,
                fromRelationName,fromColumnName,
                toRelationName, toColumnName);        
            fkcSet.addForeignKeyConstr(fkc,false);
        }
        
        return fkcSet;
    }
        
    
    /** Returns the number of fkcs in the set.
     */
    public int size() {
        return fkcCount;
    }
}
