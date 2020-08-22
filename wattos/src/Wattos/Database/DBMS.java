/*
 * DBMS.java
 *
 * Created on June 13, 2003, 2:39 PM
 */

package Wattos.Database;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import Wattos.CloneWars.UserInterface;
import Wattos.Graph.BreathFirstSearchVertexObject;
import Wattos.Graph.DirectedGraph;
import Wattos.Graph.Vertex;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.Objects;
import Wattos.Utils.StringArrayList;

/**
 * Contains a collection of tables which can be viewed as a graph where the
 * vertices are the tables and the edges are the foreign key constraints
 * from one table to another table's rows.
 *
 *The reference to a UserInterface is crucial for Wattos.Soup etc. but this
 *package can be used fine without it (not tested though).
 *
 * @author Jurgen F. Doreleijers
 */
public class DBMS implements Serializable {
    
    private static final long serialVersionUID = -1207795172754062330L;
    
    public UserInterface ui = null;
    /** The relation name will be key in the tables.
     */
    public HashMap tables;
    
    /** Set of fks with many methods. */
    public ForeignKeyConstrSet foreignKeyConstrSet;
    
    /** Directed, unweighted, and not cyclic. Cyclic example would be
     *be structure<->sequence<->function<->structure.
     * The graph does not need to be:
     * rooted, or connected.
     *The graph is valid and filled if it isn't null. In other words it needs to be nilled
     *if any operation is done on it that invalidates it.
     *The graph's vertex's external object is always the relation name.
     *The graph's edge's external object is always the from Column name.
     */
    public DirectedGraph graph;
    
    
    public static String DEFAULT_SQL_NULL_STRING_REPRESENTATION = "\\N";
    
    /** Creates a new instance of DBMS */
    public DBMS() {
        init();
    }
    
    /** Destroys all references to the tables so they can be gc-ed */
    public boolean init() {
        tables              = new HashMap();
        foreignKeyConstrSet = new ForeignKeyConstrSet(this);
        graph               = null;
        return true;
    }
    
    
    /** Make a deep copy of relation and call addRelation
     */
    public boolean copyRelation( Relation r, String name, boolean overwrite ) {
        if ( tables.containsKey( name ) ) {
            if ( ! overwrite ) {
                General.showWarning("dbms already contains a table with name: [" + name + "]");
                return false;
            } else {
                removeRelation(getRelation(name));
            }
        }
        Relation s = (Relation) Objects.deepCopy(r);
        s.name = name;
        tables.put( name, s );
        graph = null;
        return true;
    }
    
    /** Adds a relation, making sure it is rooted. By default to the ROOT. */
    public boolean addRelation( Relation r ) {
        if ( tables.containsKey( r.name ) ) {
            General.showWarning("dbms already contains a table with name: [" + r.name + "]");
            return false;
        }
        tables.put( r.name, r );
        graph = null;
        return true;
    }
    
    public boolean removeRelation( Relation r ) {
        if ( r == null ) {
            General.showError("Failed to remove relation given as null pointer.");
            return false;
        }
        if ( ! tables.containsKey( r.name ) ) {
            return false;
        }
        tables.remove( r.name );
        graph = null; // Needs to be reconstructed.
        return true;
    }
    
    /**
     *Foreign keys constraints to the table in which the row is being removed will
     *be checked. If the constraint is set to cascade, it will recursively call the
     *relation's removeRow methods. Returns true if all where done successfully.
     *In pseudo code following conventions of "Introduction to algorithms, 2nd edition",
     *Cormen, Leiserson, Rivest, and Stein, p.19.
     *<P>The index/indices of all involved relations will be nilled by this method.
     *<P>Note that the input BitSet will not be changed by this method.
     *<PRE>
     *G is a graph
     *s, u, v, and parentVertex are vertices (relations)
     *rowIds is a list of row ids
     *affected_rows is a list of affected row_ids
     *rowId is a row Id
     *GET_RIDS is a method to get row ids in a relation on the basis of multiple values
     *DELETE_RIDS is a method to nil the rows with the given set of record ids,
     *  NOT cascading because that's what this method does already.
     *
     *REMOVE_ROWS( G, s, rowIds )
     *  affected_rows[ s ] <- rowIds
     *  BFS(G, s)                            # as on page 532 and in package Wattos.Graph
     *  L <- { v elementOf V[G] }            # sorted on the distance to s as set by the BFS routine
     *  # This puts s first in the list and doesn't exclude unconnected vertices.
     *  affected_rows[ s ] <- rowIds
     *  DELETE_RIDS( s, affected_rows[ s ] )
     *  L <- { v elementOf L | v != s }     # delete s which is first in the list.
     *  for each u elementOf L
     *      parentVertex <- p[u]
     *      affected_rows[ u ] <- GET_RIDS( u, columnLabel, affected_rows[ parentVertex ] ) )
     *      DELETE_RIDS( u, affected_rows[ u ])
     *
     *
     *For this algorithm to be efficient it is crucial not to have to do GET_RIDS too often.
     *Even though it has access to a hashed AND a sorted index it is still not feasible
     *to do 10,000 times for 1 delete.
     */
    public boolean removeRowsCascading(String relationName, BitSet rowList) {
        rowList = (BitSet) rowList.clone(); // Make sure the input doesn't get overwritten.
        
        if ( ! foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            General.showError("DBMS is NOT consistent before removeRowsCascading.");
            General.showError(toString(true));
            return false;
        } else {
            //General.showDebug("DBMS is consistent before removeRowsCascading.");
        }
        
        if ( ! containsRelation(relationName)) {
            General.showError("in removeRowsCascading dbms doesn't contain relation with name: " + relationName);
            return false;
        }
        
        /** Create the graph if it isn't null; */
        if ( graph == null ) {
            createGraph();
        }
        
        // Fills the BreathFirstSearchVertexObject s
        graph.doBreathFirstSearch(relationName);
        //graph.showGraph();
        // Sort vertices on the distance
        // Natural order of the objects is on the distance; lucky us.
        ArrayList bfsObjectList = new ArrayList( Arrays.asList( graph.verticesInternalObjectToArray()));
        Collections.sort( bfsObjectList );
        
        int vertexCount = graph.vertexCount();
        // Create a map from a relation to the affected rows for each relation.
        HashMap affectedRowMap = new HashMap();
        
        // First do s itself.
        Relation s = getRelation( relationName );
        // First check if s really became the first element
        BreathFirstSearchVertexObject SbfsObjectList = (BreathFirstSearchVertexObject) bfsObjectList.get(0);
        if ( ! relationName.equals(SbfsObjectList.myVertex.vertexObject) ) {
            General.showError("in removeRowsCascading sort unsuccessfully because relation with name: " + relationName);
            General.showError("should have been first in bfs but instead found: " + SbfsObjectList.myVertex.vertexObject);
            return false;
        }
//        General.showDebug("Rows to delete in "+relationName+" are numbered: " + rowList.cardinality());
//        General.showDebug("rows to delete in "+relationName+" are: " + PrimitiveArray.toString( rowListCopy));
        affectedRowMap.put( s , rowList);
//        General.showDebug("rowList      : " + rowList);
//        General.showDebug("rowListCopy  : " + rowListCopy);
        if ( ! s.removeRows(rowList, true, true) ) {
            General.showError("failed in removeRowsCascading/dbms to delete rows in relation: " + relationName);
            return false;
        }                
        
        /** Do all connected nodes in order that they were found by the bfs. Skip the first one (self with distance 0) and
         *stop the loop when the first vertex with distance infinite has been found.
         */
        for (int u=1;u<vertexCount;u++) {
//            General.showDebug("Doing UVertex : " + u);
            BreathFirstSearchVertexObject UbfsObjectList = (BreathFirstSearchVertexObject) bfsObjectList.get(u);
            
            if ( UbfsObjectList.distance == BreathFirstSearchVertexObject.VERTEX_DISTANCE_INFINITE ) {
//                General.showDebug("this vertex is not connected so we stop: " + u);
                // No more vertices to do.
                break;
            }
            
            // Get U info
            Vertex UVertex = UbfsObjectList.myVertex;
            if ( UVertex == null ) {
                General.showError("UVertex null for BreathFirstSearchVertexObject: " + UbfsObjectList);
                return false;
            }
            String URelationName = (String) UVertex.vertexObject;
            Relation URelation = getRelation( URelationName );
            
            // Get U's parent info
            Vertex parentVertex = UbfsObjectList.parent;
            if ( parentVertex == null ) {
                General.showError("parent vertex null for BreathFirstSearchVertexObject: " + UbfsObjectList);
                return false;
            }
            String parentRelationName = (String) parentVertex.vertexObject;
//            General.showDebug("U relation: " + URelationName + " has parent relation: " + parentRelationName);
            
            Relation parentRelation = getRelation( parentRelationName );
            if ( parentRelation == null ) {
                General.showError("parentRelation null for parentRelationName: " + parentRelationName);
                return false;
            }
            BitSet affectedRowsParent = (BitSet) affectedRowMap.get( parentRelation );
            if ( affectedRowsParent == null ) {
                General.showError("affectedRowsParent null for parentVertex: " + parentVertex);
                return false;
            }
//            General.showDebug("affectedRowsParent numbered: " + affectedRowsParent.cardinality());
            // Get the columnLabel in U that points to the parent
//            TODO: debug code here 
            StringArrayList UcolumnLabelList = foreignKeyConstrSet.getFromColumnLabelListFromRelationToRelation(URelationName, parentRelationName);
//            General.showDebug("Found fkcs total: " + UcolumnLabelList.toString());
            URelation.removeIndices(); 
            BitSet UaffectedRows = new BitSet();
            for (int i=0;i<UcolumnLabelList.size();i++) {
                String UcolumnLabel = UcolumnLabelList.getString(i);
                //  the next method creates the indices again.
                BitSet UaffectedRowsSingle = SQLSelect.selectBitSet(this, URelation, UcolumnLabel,  SQLSelect.OPERATION_TYPE_EQUALS,
                        affectedRowsParent, false );
                if ( UaffectedRowsSingle == null ) {
                    General.showError("URelation.selectBitSet failed for UcolumnLabel: " + UcolumnLabel);
                    return false;
                } 
                URelation.removeIndices();
//                General.showDebug("found number of rows on label number: " + i + " == " + UaffectedRowsSingle.cardinality());
                UaffectedRows.or( UaffectedRowsSingle );
            }
//            General.showDebug("rows to delete in "+URelationName+" are numbered: " + UaffectedRows.cardinality());
//            General.showDebug("rows to delete in "+URelationName+" are: " + PrimitiveArray.toString( UaffectedRows));
            if ( ! URelation.removeRows( UaffectedRows, false, true ) ) {
                General.showError("URelation.removeRows failed for URelation: " + URelation);
                return false;
            }            
//            General.showDebug( "DEBUG: Done with removeRows for URelation: " + URelation.name);
            affectedRowMap.put( URelation, UaffectedRows);
        }
        if ( ! foreignKeyConstrSet.checkConsistencySet(false, true) ) {
            General.showError("DBMS is NOT consistent after removeRowsCascading.");
            return false;
        } else {
            //General.showDebug("DBMS is consistent after removeRowsCascading.");
        }        
        return true;
    }
    
    /** Handy for e.g. the common columns like selected
     */
    public boolean setValuesInAllTablesInColumn( String label, Object value ) {
        Set tableNames = tables.keySet();
        Iterator it=tableNames.iterator();
        for (;it.hasNext();) {
            String relationName = (String) it.next();
            Relation r = getRelation(relationName);
            if ( ! r.hasColumn(label)) {
                //General.showDebug("Skipping relation: " + relationName + " without column: " + label );
                continue;
            }
            if ( ! r.setValueByColumn(label, value)) {
                General.showError("Failed setValueByColumn for relation, label, value: " +
                        r.name + ", " + label + ", " + value);
                return false;
            }
            //General.showDebug("Done relation: " + relationName + " with column: " + label );
        }
        return true;
    }
    
    /** Do in topological order a removeRows cascading from each of the relations
     *down. Kinda expensive for larger systems.
     */
    public boolean removeUnselectedRowsInAllRelationsWithSelectionCapability() {
        /** Create the graph if it isn't null; */
        if ( graph == null ) {
            createGraph();
        }
        
        // Fills the BreathFirstSearchVertexObject s
        Object[] vertices = graph.topologicalSort();
        if (vertices==null) {
            General.showDebug("Topological sort not possible.");
            vertices = graph.verticesToArray(); // Check if this ever occurs; if so optimize.
        }
        /**
         * General.showDebug("Array of vertices in order of todo:");
         * for(int i=0;i<vertices.length;i++) {
         * General.showDebug("\t\t"+(i+1)+": "+vertices[i]);
         * }
         */
        
        int vertexCount = graph.vertexCount();
        if ( vertexCount != vertices.length ) {
            General.showError("Sanity check failed: vertexCount != vertices.length: " + vertexCount +" " + vertices.length );
        }
        
        for ( int r=0; r< vertexCount; r++ ) {
            Relation relation = getRelation( (String) vertices[r]);
            //General.showDebug("Starting remove unselected rows from relation: " + relation.name);
            if ( ! relation.containsColumn( Relation.DEFAULT_ATTRIBUTE_SELECTED )) {
                //General.showDebug("Skipping remove because no selected column");
                continue;
            }
            BitSet selected = relation.getColumnBit( Relation.DEFAULT_ATTRIBUTE_SELECTED );
            BitSet used     = relation.used;
            if ( selected == null ) {
                General.showDebug("Skipping remove because failed to get selected column even though it should exist.");
                continue;
            }
            BitSet usedAndNotSelected = (BitSet) used.clone();
            usedAndNotSelected.andNot(selected);
            //General.showDebug("Doing remove on relation with name: " + relation.name + " of rows: " + PrimitiveArray.toString( usedAndNotSelected ));
            if ( usedAndNotSelected.nextSetBit(0) > 0 ) { // There's at least one that needs to be done.
                boolean status = removeRowsCascading(relation.name, usedAndNotSelected );
                if ( ! status ) {
                    General.showError("in removeRowsCascading dbms unsuccessfully delete attempted for relation with name: " + relation.name);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    
    /** Returns null if there is no relation with the given name */
    public Relation getRelation( String relationName ) {
        if ( containsRelation( relationName ) ) {
            return (Relation) tables.get( relationName);
        }
        General.showWarning("Failed to find relation in dbms: " + relationName);
        return null;
    }
    
    /** */
    public boolean containsRelation( String relationName ) {
        if ( tables.containsKey( relationName ) ) {
            return true;
        }
        return false;
    }
    
    
    /** Returns a new relation name, i.e. one that doesn't yet exist. Up to
     *1,000,000 relation names can be created like this. 
     *It will start trying names at the beginning everytime so as long
     *as old ones are deleted really a million concurrent ones can be created.
     *Returns null on failure.
     */
    public String getNextRelationName() {
        int MAX_AUTOMATIC_NAMES = 1000 * 1000;
        for (int i=0;i<MAX_AUTOMATIC_NAMES;i++) {
            String name = "Relation name " + i;
            if ( ! containsRelation( name )) {
                //General.showDebug("found unique relation name: " + name);
                return name;
            }
            // keep trying
        }
        return null; // failed
    }
    
    public boolean createGraph() {
        graph = new DirectedGraph(tables.size());
        
        // Add tables.
        Set relations = tables.keySet();
        ArrayList relationList = new ArrayList( relations );
//        General.showDebug("relations: " + Strings.toString(relationList));
        Collections.sort( relationList );
        for (Iterator it=relationList.iterator();it.hasNext();) {
            String key = (String) it.next();
//            General.showDebug("Adding table to graph with name: " + key);
            graph.addVertex(key); // they are unique
        }
        
        // Add edges.
        // Get table names
        Set tableNames = foreignKeyConstrSet.foreignKeyConstrMapFrom.keySet();
        //General.showDebug("starting on vertices: " + foreignKeyConstrSet.size() );
        for (Iterator it= tableNames.iterator();it.hasNext();) {
            // Get column names
            String tableName = (String) it.next();
            //General.showDebug("found fromTableName: " + tableName );
            HashMap hashMap = (HashMap) foreignKeyConstrSet.foreignKeyConstrMapFrom.get( tableName );
            Set columnLabels = hashMap.keySet();
            //General.showDebug("found number of fkcs from this table: " + columnLabels.size() );
            for (Iterator it2= columnLabels.iterator();it2.hasNext();) {
                String columnLabel = (String) it2.next();
                ForeignKeyConstr fkc = (ForeignKeyConstr) hashMap.get( columnLabel );
                String toTableName = fkc.toRelationName;
                // NOTE: the nomenclature to/from is reversed in the graph
                // Where in the fkc it's from child/to parent in the graph it
                // is from parent to child.
                // The column label is of the vertex child
                //General.showDebug("adding directed edge from parent vertex: " + toTableName + " to child vertex: " + tableName + " and (from)column: " + columnLabel);
                graph.addEdge( toTableName, tableName, 1, columnLabel );
            }
        }
        return true;
    }
    
    
    /** Represents all the relations in the dbms. This might be feasible for
     *small tables but don't try it with showing rows for larger tables.
     */
    public String toSTAR( ) {
        StringBuffer sb = new StringBuffer();
        // Tables
        sb.append( "data_dbms_dump\n" );
        
        Set keys = tables.keySet();
        ArrayList keyList = new ArrayList( keys );
        Collections.sort( keyList );
        for (Iterator it=keyList.iterator();it.hasNext();) {
            String label = (String) it.next();
            Relation relation = (Relation) tables.get(label);
            sb.append( "\nsave_" + relation.name + General.eol );
            String table = relation.toSTAR();
            if ( table == null ) {
                General.showError("Failed to render to STAR the relation with name: " + label);
                return null;
            }
            sb.append( table );
            sb.append( "save_\n" );
        }
        // ForeignKeyConstraints will be shown at each table.
        return sb.toString();
    }
    
    
    public String toString() {
        return toString( false );
    }
    
    /** Represents all the relations in the dbms. This might be feasible for
     *small tables but don't try it with showing rows for larger tables.
     */
    public String toString( boolean showRows ) {
        
        StringBuffer sb = new StringBuffer();
        // Tables
        sb.append( "DBMS contains:\n" );
        sb.append( "Tables:\n" );
        Set keys = tables.keySet();
        ArrayList keyList = new ArrayList( keys );
        Collections.sort( keyList );
        for (Iterator it=keyList.iterator();it.hasNext();) {
            String label = (String) it.next();
            Relation relation = (Relation) tables.get(label);
            // show all but fkcs because they will be shown together below
            // show rows if needed but don't limit to selected rows.
            sb.append( relation.toString(true, true, false, false, showRows, false) );
            sb.append( General.eol );
        }
        sb.append( "Foreign Key Constraints:" + General.eol);
        sb.append( foreignKeyConstrSet );
        
        // ForeignKeyConstraints will be shown at each table.
        return sb.toString();
    }
    
    /** Tests if the set of relations is not cyclic etc.
     */
    public boolean graphIsOkay() {
        if ( ! createGraph() ) {
            General.showError("Graph could not even be created");
            return false;
        }
        return ! graph.hasCycle();
    }
    
    /** Create if needed and then show a graph of the relations */
    public boolean showGraph() {
        if ( graph == null ) {
            if ( ! createGraph() ) {
                General.showError("Graph could not even be created");
                return false;
            }
        }
        graph.showGraph();
        return true;
    }
    
    /** Writes the SQL commands required to create the tables (if requested) and
     *the SQL commands to read in the tables from CSV files.
     *Finally it writes the CSV values themselves.
     *Currently the specs are for a MySQL database version 4.1 as the output from the
     *program mysqldump have been tried to match.
     *
     *Notes:
     *Supports referential integrity if present.
     *The order and selected-ness IS dumped.
     * @see #writeCsvRelationList
     *
     *TODO finish.
     */
    public boolean dumpSQL( String sqldumpCmdsFileName, String outputDir,
            boolean containsHeaderRow) {
        
        if ( true ) {
            General.showWarning("Code WriteSQLDump is unfinished; debug start from macro file.");
            return true;
        }
        
        if ( ! foreignKeyConstrSet.checkConsistencySet(false, true)) {
            General.showError("DBMS finds the DB not consistent so no dump was attempted.");
            return false;
        }
        
        File outputDirFile = new File(outputDir);
        if ( !outputDirFile.exists()) {
            if ( ! outputDirFile.mkdir()) {
                General.showError("Failed to create not existing dir: " + outputDir);
                return false;
            }
        }
        StringBuffer sqldumpCmds = new StringBuffer();
        HashMap fkcFrom = foreignKeyConstrSet.foreignKeyConstrMapFrom;
        if ( ! createGraph()) {
            General.showError("Failed to createGraph");
            return false;
        }
        //General.showDebug("Graph of tables is:\n" + graph);
        Object[] a = graph.topologicalSort();
        if( a == null ) {
            General.showError("Topological sort of DBMS not possible.");
            return false;
        }
        
        /** Reverse remove is needed for dependancies.
         */
        for (int i=(a.length-1);i>=0;i--) {
            String relationName = (String) a[i];
            sqldumpCmds.append("DROP TABLE IF EXISTS `"+relationName+"`;\n");
        }
        /**
         * General.showOutput("Topological array with element count: " + a.length);
         * for(int i=0;i<a.length;i++){
         * String relationName = (String) a[i];
         * General.showOutput("\t\t"+(i+1)+": "+a[i]);
         * }
         * General.showOutput("Writing SQL code for all relation");
         */
        for (int i=0;i<a.length;i++) {
            String relationName = (String) a[i];
            //General.showOutput("Writing SQL code for relation: " + relationName);
            Relation r = getRelation(relationName);
            File f = new File( outputDir, relationName + ".csv");
            String file_name = f.toString();
            sqldumpCmds.append("CREATE TABLE `"+relationName+"` (\n");
            // Add the physical column id which becomes the primary id just as in Wattos.
            int rColumnCount = r.columnOrder.size();
            ArrayList columnOrderActual = new ArrayList();
            int actualCount = 0;
            for (int c=0;c<rColumnCount;c++) {
                String rColumnName = r.getColumnLabel(c);
                /** were not included in previous version of code.
                 * if ( rColumnName.equals( r.DEFAULT_ATTRIBUTE_ORDER_ID )) {
                 * continue;
                 * }
                 * if ( rColumnName.equals( r.DEFAULT_ATTRIBUTE_SELECTED )) {
                 * continue;
                 * }
                 */
                columnOrderActual.add(rColumnName);
            }
            rColumnCount = columnOrderActual.size();
            sqldumpCmds.append("    `"+relationName+Relation.DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN_ID_POSTFIX+
                    "` INT NOT NULL PRIMARY KEY,\n");
            for (int c=0;c<rColumnCount;c++) {
                String rColumnName = (String) columnOrderActual.get(c);
                String rColumnDataTypeSQL = r.getColumnDataTypeSQL(rColumnName);
                if ( rColumnDataTypeSQL == null ) {
                    General.showDebug("Failed to get the SQL data type, assuming "+
                    		Relation.dataTypeListSQL[Relation.DATA_TYPE_STRING] + " for relation: " + relationName + " column: " + rColumnName);
                    rColumnDataTypeSQL = Relation.dataTypeListSQL[Relation.DATA_TYPE_STRING];
                }
                sqldumpCmds.append("    `"+rColumnName+"` "+rColumnDataTypeSQL+" DEFAULT NULL");
                if ( c!=(rColumnCount-1) ) {
                    sqldumpCmds.append(',');
                }
                sqldumpCmds.append(General.eol);
            }
            // Add the foreign key constraints from this table to the parent.
            HashMap fkcFromRelation = (HashMap) fkcFrom.get( relationName );
            if ( fkcFromRelation != null ) {
                Set keys = fkcFromRelation.keySet();
                ArrayList keyList = new ArrayList(keys);
                Collections.sort( keyList );
                if ( keyList.size() != 0 ) {
                    sqldumpCmds.append(",\n");
                }
                for (Iterator it = keyList.iterator();it.hasNext();) {
                    String rColumnName = (String) it.next();
                    ForeignKeyConstr fkc = (ForeignKeyConstr) fkcFromRelation.get(rColumnName);
                    String relationNameTo = fkc.toRelationName;
                    String rColumnNameTo  = fkc.toColumnLabel;
                    if ( rColumnNameTo == null ) {
                        rColumnNameTo = relationNameTo + Relation.DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN_ID_POSTFIX;
                    }
                    String constraintName = relationName+"_"+rColumnName;
                    sqldumpCmds.append("    CONSTRAINT `"+constraintName+"` ");
                    sqldumpCmds.append("FOREIGN KEY (`"+rColumnName+"`)\n");
                    sqldumpCmds.append("        REFERENCES   `"+relationNameTo+"` ");
                    sqldumpCmds.append("            (`"+rColumnNameTo+"`) ");
                    if ( fkc.onDeleteDo == ForeignKeyConstr.ACTION_TYPE_CASCADE ) {
                        sqldumpCmds.append("ON DELETE CASCADE");
                    }
                    if ( it.hasNext()) {
                        sqldumpCmds.append(',');
                    }
                    sqldumpCmds.append(General.eol);
                }
            }
            sqldumpCmds.append(") ENGINE=InnoDB DEFAULT CHARSET=latin1;\n");
            File csvFile = new File( outputDir, relationName + ".csv");
            String csvFileDoubleQuotes = csvFile.toString();
            if ( InOut.isOSWindows ) {
                csvFileDoubleQuotes = InOut.getFileDoubleBackSlash(csvFile);
            }
            sqldumpCmds.append("LOAD DATA LOCAL INFILE '" + csvFileDoubleQuotes + "'\n" +
                    "   INTO TABLE " + relationName + General.eol +
                    "   FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'" + General.eol +
                    "   LINES  TERMINATED BY '\\n'");
            if ( containsHeaderRow ) {
                sqldumpCmds.append(" IGNORE 1 LINES");
            }
            sqldumpCmds.append(";\n\n");
        }
        
        try {
            File sqldumpCmdsFile = new File( outputDir, sqldumpCmdsFileName );
            FileWriter fw = new FileWriter(sqldumpCmdsFile);
            if ( sqldumpCmdsFile.exists() ) {
                General.showOutput("Overwriting current file: " + sqldumpCmdsFile);
            } else {
                General.showOutput("Writing SQL dump command file: " + sqldumpCmdsFile);
            }
            fw.write(sqldumpCmds.toString());
            fw.close();
        } catch ( Throwable t ) {
            General.showError(t.toString());
            return false;
        }
        General.showOutput("Writing csv files to directory: " + outputDir);
        boolean containsPhysicalColumn  = true;
        boolean containsSelected        = true;
        boolean containsOrder           = true;
        boolean useActualNULL           = true;
        
        if ( ! writeCsvRelationList( outputDir, containsHeaderRow, containsPhysicalColumn, containsSelected, containsOrder, useActualNULL )) {
            General.showError("Failed to writeCsvRelationList");
            return false;
        }
        
        return true;
    }
    
    /** Convenience method.
     */
    public boolean writeCsvRelationList( String csvFileDir, boolean containsHeaderRow ) {
        boolean containsPhysicalColumn  = true;
        boolean containsSelected        = false;
        boolean containsOrder           = true;
        boolean useActualNULL           = false;
        return writeCsvRelationList( csvFileDir,
                containsHeaderRow, containsPhysicalColumn,
                containsSelected,  containsOrder,
                useActualNULL);
    }
    
    /**
     * Returns true only if all is done successfully. Dtds not produced yet.
     * @param csvFileDir
     * @param containsHeaderRow Write column names in header.
     * @param containsPhysicalColumn Write an id of the number of the location of the row's data.
     * @param containsSelected Skip those not selected.
     * @param containsOrder Use the order column for sortng the output.
     * @param useActualNULL Writes a \\N which can be interpreted as an actual null by
     * e.g. MySQL.
     *
     */
    public boolean writeCsvRelationList( String csvFileDir,
            boolean containsHeaderRow, boolean containsPhysicalColumn,
            boolean containsSelected,  boolean containsOrder, boolean useActualNULL ) {
        
        ArrayList relationNameList = new ArrayList( tables.keySet());
        Collections.sort( relationNameList );
        for (int i=0;i<relationNameList.size();i++) {
            String relationName = (String)relationNameList.get(i);
            Relation r = getRelation(relationName);
            File f = new File( csvFileDir, relationName + ".csv");
            String file_name = f.toString();
            General.showOutput("Write csv file: [" + file_name + "]");
            if ( ! r.writeCsvFile(file_name,
                    containsHeaderRow, containsPhysicalColumn,
                    containsSelected,  containsOrder,
                    useActualNULL)) {
                General.showError("Failed to write csv.");
                return false;
            }
        }
        return true;
    }
    
    /** Returns true only if all is done successfully.
     *DTD dir can be ommited by giving a null value.
     */
    public boolean readCsvRelationList( String[] relationNames, String csvFileDir,
            String csvDtdFileDir, boolean checkConsistency, boolean showChecks ) {
        boolean containsHeaderRow = true;
        
        final int csvFilesRead = relationNames.length;
        for (int i=0;i<csvFilesRead;i++) { // skip blast for now.
            Relation relation = null;
            try {
                relation = new Relation(relationNames[i], this);
            } catch ( Exception e ) {
                General.showThrowable(e);
                return false;
            }
            General.showOutput("Reading relation : " + relation.name );
            File csv_f = new File( csvFileDir, relation.name + ".csv");
            String csv_fileName = csv_f.toString();
            File dtd_f;
            String dtd_fileName = null;
            if ( csvDtdFileDir != null ) {
                dtd_f = new File( csvDtdFileDir, relation.name + "_dtd.csv");
                dtd_fileName = dtd_f.toString();
            }
            if ( ! relation.readCsvFile(  csv_fileName, containsHeaderRow, dtd_fileName)) {
                General.showError("Failed to read csv file: " + csv_fileName);
                return false;
            }
            if ( checkConsistency ) {
                // Check the new fkcs for consistency
                // and correct by removing rows that are in violatin.
                for (int c=0;c<relation.sizeColumns();c++) {
                    String fromColumnLabel = relation.getColumnLabel(c);
                    ForeignKeyConstr fkc = foreignKeyConstrSet.getForeignKeyConstrFrom(relationNames[i], fromColumnLabel);
                    if ( fkc != null ){
                        if ( ! fkc.checkConsistency(showChecks, false) ) {
                            General.showWarning("The dbms isn't consistent for fkc: " + fkc);
                            if ( ! fkc.makeConsistentByRemoveInconsistentRows(showChecks, true)) {
                                General.showError("Failed to makeConsistentByRemoveInconsistentRows.");
                                return false;
                            }
                        }
                    }
                }
            }
        }
        
        if ( checkConsistency && (! foreignKeyConstrSet.checkConsistencySet(showChecks, true))) {
            General.showError("The dbms isn't consistent; giving up.");
            return false;
        }
        return true;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        DBMS dbms = new DBMS();
        dbms.createGraph();
        General.showOutput("Graph of tables is:\n" + dbms.graph);
    }
}
