/*
 * RelationTest.java
 * JUnit based test
 *
 * Created on November 30, 2005, 2:12 PM
 */

package Wattos.Database;

import junit.framework.*;
import java.util.*;
import java.io.*;
import Wattos.Database.Indices.*;
import Wattos.Utils.*;
import cern.colt.list.*;

/**
 *
 * @author jurgen
 */
public class RelationTest extends TestCase {
    
    static DBMS dbms = new DBMS();
    static String wattosRoot   = null;
    static File inputDir       = null;
    static File outputDir      = null;
    
    static {
        wattosRoot   = InOut.getEnvVar("WATTOSROOT");
        inputDir       = new File( wattosRoot,"data"+File.separator+"test_data" );
        outputDir      = new File( wattosRoot,"tmp_dir" );                
    }
    
    public RelationTest(String testName) {
        super(testName);
        General.verbosity = General.verbosityDebug;        
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(RelationTest.class);        
        return suite;
    }    
    
     public static void testInit() throws Throwable {
         
         if ( createExample_1( dbms ) == null ) {
             fail("createExample_1");
         }
         if ( createExample_2( dbms ) == null ) {
             fail("createExample_2");
         }
         if ( ! createExample_3( dbms ) ) {
             fail("createExample_3");
         }
         if ( ! createExample_4( dbms ) ) {
             fail("createExample_4");
         }
         if ( ! createExample_5( dbms ) ) {
             fail("createExample_5");
         }
         if ( ! createExample_6( dbms ) ) {
             fail("createExample_6");
         }
         if ( ! createExample_7( dbms ) ) {
             fail("createExample_7");
         }
         if ( ! createExample_8( dbms ) ) {
             fail("createExample_8");
         }
         if ( ! createExample_9( dbms ) ) {
             fail("createExample_9");
         }
         if ( ! createExample_10( dbms ) ) {
             fail("createExample_10");
         }
     }
            
    /** Examples for testing stringsets, int[], 
        - removing of rows by lists
        - resizing of physical table in combination with the linkedlist     
     */
   public static Relation createExample_1(DBMS dbms) {
       General.showDebug("createExample_1");
        try {
            Relation t = new Relation("Test Relation", 99, dbms);
            t.insertColumn(Relation.DEFAULT_ATTRIBUTE_SELECTED,Relation.DATA_TYPE_BIT             , null);
            t.insertColumn("test a char",                   Relation.DATA_TYPE_CHAR               , null);
            t.insertColumn("test a short",                  Relation.DATA_TYPE_SHORT              , null);
            t.insertColumn("id",                            Relation.DATA_TYPE_INT                , null);
            t.insertColumn("float",                         Relation.DATA_TYPE_FLOAT              , null);
            t.insertColumn("double",                        Relation.DATA_TYPE_DOUBLE             , null);
            t.insertColumn("ll",                            Relation.DATA_TYPE_LINKEDLIST         , null);
            t.insertColumn("lli",                           Relation.DATA_TYPE_LINKEDLISTINFO     , null);
            t.insertColumn("test a StringNR",               Relation.DATA_TYPE_STRINGNR           , null);
            t.insertColumn("test a String",                 Relation.DATA_TYPE_STRING             , null);
            t.insertColumn("test an Object[]",              Relation.DATA_TYPE_OBJECT             , null);
            t.insertColumn("test a int list",               Relation.DATA_TYPE_ARRAY_OF_INT       , null);
            t.insertColumn("test a float list",             Relation.DATA_TYPE_ARRAY_OF_FLOAT     , null);
            StringSet ss = t.getColumnStringSet("test a StringNR");
            if ( ss == null ) {
                fail( "getColumnStringSet");
            }
            t.renameColumn("id", "Atom ID");
            int[] idx = t.getNewRowIdList(10);
            int[] temp = (int[]) t.getColumn( "Atom ID" );
            for (int i=0;i<10;i++) {
                temp[idx[i]]=i;
            }
            General.showDebug("After filling rows:\n" + t);            

            t.setValueByColumn("test a StringNR", "a string");
            t.setValue(0, "test a char", new Character('a'));          
            t.setValue(2, "test a char", new Character('b'));
            t.setValue(5, "test a char", new Character('A'));
            t.setValue(7, "test a char", new Character('B'));
            t.setValue(7, Relation.DEFAULT_ATTRIBUTE_SELECTED, Boolean.valueOf(true));
            float[][] tmp = t.getColumnFloatList("test a float list");
            if ( tmp == null ) {
                fail("getColumnFloatList");
            }
            //LinkedListArray ll = (LinkedListArray) t.getColumn("ll");
            //int[] list2 = { 0,2,5,7 };
            //ll.setList( list2 );

            int[] list3 = {1,3,4,6,8};
            t.removeRows(list3,true);            
            int[] list4 = {9};
            t.removeRows(list4,true);            
            //t.removeColumn("test an int");
            General.showDebug("After removed rows:\n" + t);            

            //int[] map = t.reduceCapacity(5);            
            //General.showDebug("After reduceCapacity:\n" + t);            

            //t.removeRow(3,true);
            //General.showDebug("Removed row 3:\n" + t);            

            t.ensureCapacity(1000);
            General.showDebug("After resizing to 1000:");            
            General.showDebug(t.toString());            

            idx = t.getNewRowIdList(2);
            General.showDebug("After getting 2 new rows:\n" + t); 
            return t;
        } catch ( Exception e ) {fail(e.getMessage());}
        return null;
    }
    
    /** Examples for testing different data types. */
    public static Relation createExample_2(DBMS dbms) {
       General.showDebug("createExample_2");
       dbms.init();
        try {
            Relation t = new Relation("Test Relation", 99, dbms);
            t.insertColumn(0,"id",                          Relation.DATA_TYPE_STRING             , null);
            t.insertColumn("float",                         Relation.DATA_TYPE_STRING             , null);
            t.insertColumn("test a char",                   Relation.DATA_TYPE_CHAR             , null);
            t.insertColumn(Relation.DEFAULT_ATTRIBUTE_SELECTED,      Relation.DATA_TYPE_BIT             , null);
            t.insertColumn("test a String",                 Relation.DATA_TYPE_STRING             , null);
            t.insertColumn("test a list",                   Relation.DATA_TYPE_ARRAY_OF_INT             , null);
            t.renameColumn("id", "Atom ID");
            
            t.getNewRowIdList(10);
            String[] temp = (String[]) t.getColumn( "Atom ID" );
            String[] temp2= (String[]) t.getColumn( "float" );
            for (int i=0;i<10;i++) {
                temp[i]=Integer.toString(i);
                temp2[i]=Float.toString(i);
            }
            
            General.showDebug("Table:\n" + t.toString(true,true,true,true,true,false));                    
            
            t.setValueByColumn("test a String", "a string");
            t.setValue(0, "test a String", "A");          
            t.setValue(1, "test a String", "A");          
            t.setValue(2, "test a String", "B");          
            t.setValue(3, "test a String", "A");          

            t.setValue(0, "test a char", new Character('a'));          
            t.setValue(2, "test a char", new Character('b'));
            t.setValue(5, "test a char", new Character('A'));
            t.setValue(7, "test a char", new Character('B'));
            t.setValue(7, Relation.DEFAULT_ATTRIBUTE_SELECTED, Boolean.valueOf(true));

            //t.removeRow(5,true);
            //General.showDebug("Removed row 5");            
            General.showDebug("Table:\n" + t.toString(true,true,true,true,true,false));                    
            return t;
        } catch ( Exception e ) {fail(e.getMessage());}
        return null;
    }
    
    /** Tests indexing */
    public static boolean createExample_3(DBMS dbms) {
       //General.verbosity = General.verbosityDebug;
       General.showDebug("createExample_3");
       dbms.init();
        try {            
            Relation s = new Relation("S", 99, dbms);
            Relation t = new Relation("T", 99, dbms);
            Relation u = new Relation("U", 99, dbms);
            int idx;
            ForeignKeyConstr fkc = new ForeignKeyConstr(dbms, t.name,"id",s.name,null);
            t.insertColumn(0,"id",                          Relation.DATA_TYPE_INT             , fkc);
            // OBJECT fkc is stored now and we can use the variable fkc to point at a different one.
            fkc = new ForeignKeyConstr(dbms, u.name,"id",s.name,null);
            u.insertColumn(0,"id",                          Relation.DATA_TYPE_INT             , fkc);
            u.insertColumn("stringie",                      Relation.DATA_TYPE_STRINGNR        , null);
            u.insertColumn("floaty",                        Relation.DATA_TYPE_FLOAT           , null);
            //int idx = u.getNewRowId();
            idx = u.getNewRowId();
            u.setValue(idx, "stringie", "A" );
            u.setValue(idx, "id", new Integer(91) );  // This deliberately violates the fkc but is checked later
            u.setValue(idx, "floaty", new Float(123.456) );
            idx = u.getNewRowId();
            u.setValue(idx, "stringie", "A" );
            u.setValue(idx, "id", new Integer(92) );  
            u.setValue(idx, "floaty", new Float(12.3456) );            
            idx = u.getNewRowId();
            u.setValue(idx, "stringie", "B" );
            u.setValue(idx, "id", new Integer(91) );  
            u.setValue(idx, "floaty", new Float(123.456) );
            idx = u.getNewRowId();
            u.setValue(idx, "stringie", "A" );
            u.setValue(idx, "floaty", new Float(12.3456) );
            
            boolean status = u.addIndex("stringie", Index.INDEX_TYPE_HASHED );
            if ( ! status ) {
                General.showError("Failed to create index for stringie");
                return false;
            }
            status = u.addIndex("stringie", Index.INDEX_TYPE_SORTED );
            if ( ! status ) {
                General.showError("Failed to create index for stringie");
                return false;
            }
            //General.showDebug("Index.INDEX_TYPE_SORTED is: " + Index.INDEX_TYPE_SORTED);
            status = u.addIndex("floaty", Index.INDEX_TYPE_SORTED );
            if ( ! status ) {
                General.showError("Failed to create index for stringie floaty");
                return false;
            }
            
            status = u.addIndex("id", Index.INDEX_TYPE_SORTED );
            if ( ! status ) {
                General.showError("Failed to create index for int id");
                return false;
            }

            BitSet list = SQLSelect.selectBitSet(dbms, u, "id", 
                        SQLSelect.OPERATION_TYPE_EQUALS, new Integer(91),false);
            if ( list == null ) {
                General.showError("List is empty for column id and key 91\n");
                return false;
            }
            General.showDebug("Found bitset  A: " + PrimitiveArray.toString( list ) );
            IntArrayList selection  = PrimitiveArray.toIntArrayList( list );
            General.showDebug("Found ints    B: " + PrimitiveArray.toString( selection ) );            
        } catch ( Exception e ) {
            e.printStackTrace();
            fail();
            return false;
        }
        General.showDebug("DBMS:\n" + dbms);  
        boolean showChecks = false;
        boolean showErrors = false;
        if ( dbms.foreignKeyConstrSet.checkConsistencySet(showChecks,showErrors)) {
            fail("dbms.foreignKeyConstrSet.checkConsistencySet should have shown an inconsistent set.");
            return false;
        }
        return true;
    }    
    
    /** Tests foreign key constraints in graph with cascading remove */
    public static boolean createExample_4(DBMS dbms) {
       General.showDebug("createExample_4");
       dbms.init();
        try {
            
            Relation l_0 = new Relation("l_0", 10, dbms);
            Relation l_1 = new Relation("l_1", 10, dbms);
            Relation l_2 = new Relation("l_2", 10, dbms);

            int level_0_idx;
            int level_1_idx;
            int level_2_idx;
            
            ForeignKeyConstr fkc_a;
            ForeignKeyConstr fkc_b;
            
            fkc_a = new ForeignKeyConstr(dbms, l_1.name,"level_2",l_2.name,null);
            fkc_b = new ForeignKeyConstr(dbms, l_0.name,"level_1",l_1.name,null);

            l_1.insertColumn("level_2",                          Relation.DATA_TYPE_INT             , fkc_a);
            l_0.insertColumn("level_1",                          Relation.DATA_TYPE_INT             , fkc_b);
            //l_2.insertColumn("level_3",                          Relation.DATA_TYPE_INT             , null);

            //l_1.insertColumn("name",                          Relation.DATA_TYPE_STRINGNR             , null);
            l_2.insertColumn("name",                          Relation.DATA_TYPE_STRINGNR             , null);
            //l_3.insertColumn("name",                          Relation.DATA_TYPE_STRINGNR             , null);
            
            level_2_idx = l_2.getNewRowId();
            l_2.setValue(level_2_idx, "name", "darth vader" );            
            
            level_1_idx = l_1.getNewRowId();
            l_1.setValue(level_1_idx, "level_2", new Integer(level_2_idx) );
            
            level_0_idx = l_0.getNewRowId();
            l_0.setValue(level_0_idx, "level_1", new Integer(level_1_idx) );

            General.showDebug("DBMS:\n" + dbms.toString(true));        
            dbms.createGraph(); 
            dbms.graph.toString(); // ignore it for now.
            if ( ! dbms.foreignKeyConstrSet.checkConsistencySet(false,false)) {
                fail("dbms.foreignKeyConstrSet.checkConsistencySet should show a consistent DBMS.");
            }

            if ( ! l_1.renameColumn( "level_2", "level_x" ) ) {
                General.showWarning("Failed to rename column");
            }
        } catch ( Exception e ) { 
            e.printStackTrace(); 
            return false;
        }
        
        // Delete darth vader as the second element in the list
        /**
        Relation relation = dbms.getRelation( "l_2" );
        BitSet rowSet = new BitSet();
        rowSet.set(1); 
        boolean status = relation.removeRowsCascading(rowSet, true);
        if ( ! status ) {
            General.showError("Failed to removeRowsCascading");
        }
        General.showDebug("done removeRowsCascading");
         */
        General.showDebug("DBMS:\n" + dbms);        
        return true;
    }

    /** Tests serializability of dbms and all involved */
    public static boolean createExample_5(DBMS dbms) {
       dbms.init();
       //General.verbosity = General.verbosityDebug;        
       General.showDebug("createExample_5");
       General.verbosity = General.verbosityOutput;        
        createExample_4(dbms);
       //General.verbosity = General.verbosityDebug;        
        String location_bin = "WattosMenu.bin";
        location_bin = new File( outputDir, location_bin).toString();
        boolean status = InOut.writeObject( dbms, location_bin ); 
        if (! status) {
            General.showError("writing dbms BIN file.");
            System.exit(1);
        }
        
        dbms = (DBMS) InOut.readObjectOrEOF( location_bin );
        if ( dbms == null ) {
            General.doCodeBugExit("Reading dbms BIN file found null for dbms object.");
        }
        
        General.showDebug("Done with example 5.");
        return true;
    }

    /** Tests append on relations */
    public static boolean createExample_6(DBMS dbms) {
        dbms.init();
        General.showDebug("createExample_6");
        try {
            Relation t = new Relation("Relation T", 99, dbms);
            Relation u = new Relation("Relation U", 99, dbms);
            t.insertColumn(0,"id",                          Relation.DATA_TYPE_STRING             , null);
            t.insertColumn("test a char",                   Relation.DATA_TYPE_CHAR             , null);
            t.insertColumn(Relation.DEFAULT_ATTRIBUTE_SELECTED,      Relation.DATA_TYPE_BIT             , null);
            t.insertColumn("test a String",                 Relation.DATA_TYPE_STRING             , null);

            u.insertColumn(0,"id",                          Relation.DATA_TYPE_STRING             , null);
            u.insertColumn("test a char",                   Relation.DATA_TYPE_CHAR             , null);
            u.insertColumn(Relation.DEFAULT_ATTRIBUTE_SELECTED,      Relation.DATA_TYPE_BIT             , null);
            u.insertColumn("test a String",                 Relation.DATA_TYPE_STRING             , null);

            t.getNewRowIdList(10);
            t.setValueByColumn("test a String", "a string");
            t.setValue(0, "test a String", "A");          
            t.setValue(1, "test a String", "A");          
            t.setValue(2, "test a String", "B");          
            t.setValue(3, "test a String", "A");          

            t.setValue(0, "test a char", new Character('a'));          
            t.setValue(2, "test a char", new Character('b'));
            t.setValue(5, "test a char", new Character('A'));
            t.setValue(7, "test a char", new Character('B'));
            t.setValue(7, Relation.DEFAULT_ATTRIBUTE_SELECTED, Boolean.valueOf(true));
            t.removeRow(6,false);
            t.removeRow(8,false);

            u.getNewRowIdList(10);
            u.setValueByColumn("test a String", "a string");
            u.setValue(0, "test a String", "0");          
            u.setValue(1, "test a String", "1");          
            u.setValue(2, "test a String", "2");          
            u.setValue(3, "test a String", "3");          
            u.setValue(4, "test a String", "4");          
            u.setValue(5, "test a String", "5");          
            u.setValue(6, "test a String", "6");          
            u.setValue(7, "test a String", "7");          
            u.setValue(8, "test a String", "8");          
            u.setValue(9, "test a String", "9");          

            u.setValue(0, "test a char", new Character('a'));          
            u.setValue(2, "test a char", new Character('b'));
            u.setValue(5, "test a char", new Character('A'));
            u.setValue(7, "test a char", new Character('B'));
            u.setValue(7, Relation.DEFAULT_ATTRIBUTE_SELECTED, Boolean.valueOf(false));
            u.removeRow(2,false);
            u.removeRow(3,false);
            u.removeRow(4,false);

            ArrayList columnOrderOther = new ArrayList();
            columnOrderOther.add("test a String");
            boolean allowNewColumns = false;

            General.showDebug("Table T:\n" + t.toString(true,true,true,true,true,false));                    
            General.showDebug("Table U:\n" + u.toString(true,true,true,true,true,false));                    
            if ( ! t.append(u,1,8, columnOrderOther, allowNewColumns) ) {
                fail("Failed to append relation");
            } else {
                General.showDebug("Appended relation");
                General.showDebug("Table:\n" + t.toString(true,true,true,true,true,false));                    
            }
        } catch ( Exception e ) {fail(e.getMessage());}        
        General.showDebug("Done with example 6.");
        return true;
    }

    /** Tests select distinct on relations */
    public static boolean createExample_7(DBMS dbms) {
       dbms.init();
       General.showDebug("createExample_7");
        try {
            Relation t = new Relation("Relation T", 99, dbms);
            t.insertColumn(0,"id",Relation.DATA_TYPE_INT, null);

            t.getNewRowIdList(8);
            t.setValueByColumn("id", 999);
            t.setValue(0, "id", 1);          
            t.setValue(1, "id", 4);          
            t.setValue(2, "id", 2);          
            t.setValue(3, "id", 3);          
            t.setValue(4, "id", 5);          
            t.setValue(5, "id", 0);          
            t.setValue(6, "id", 3);          
            t.setValue(7, "id", 0);          
            BitSet selection = new BitSet();
            selection.set( 6 );
            BitSet result = SQLSelect.selectBitSet(dbms,t,"id",SQLSelect.OPERATION_TYPE_EQUALS,new Integer(3),true);
            General.showDebug("Selection: " + PrimitiveArray.toString( selection ) );
            assertEquals("select distinct on relations failed: ",selection,result);
        } catch ( Exception e ) {fail(e.getMessage());}
        return true;
    }

    /** Tests reorder rows on relations */
    public static boolean createExample_8(DBMS dbms) {
       dbms.init();
       General.showDebug("createExample_8");
        try {
            Relation t = new Relation("Relation T", 99, dbms);
            t.insertColumn(0,"id",Relation.DATA_TYPE_INT, null);

            t.getNewRowIdList(9);
            t.setValueByColumn("id", -1);
            t.setValue(0, "id", 1);          
            t.setValue(1, "id", 4);          
            t.setValue(2, "id", 2);          
            t.setValue(3, "id", 3);          
            t.setValue(4, "id", 5);          
            t.setValue(5, "id", 0);          
            t.setValue(6, "id", 3);          
            t.setValue(7, "id", 0);          
            t.setValue(8, "id", Defs.NULL_INT);
            
            BitSet selection = new BitSet();
            selection.set( 0,9 );
            General.showDebug("Selection     : " + PrimitiveArray.toString( selection ) );
            General.showDebug("Before reorder: " + t);
            t.renumberRows( "id", selection, 1);
            General.showDebug("After reorder: " + t);
            int[] result = t.getColumnInt("id");
            int[] resultExp = (int[]) result.clone();
            resultExp[0] =3;
            resultExp[1] =7;
            resultExp[2] =4;
            resultExp[3] =5;
            resultExp[4] =8;
            resultExp[5] =1;
            resultExp[6] =6;
            resultExp[7] =2;
            resultExp[8] =9;            
            IntArrayList r = new IntArrayList(result);
            IntArrayList rExp = new IntArrayList(resultExp);
            assertEquals("renumberRows failed", r, rExp);
        } catch ( Exception e ) {fail(e.getMessage());}
        return true;
    }

    /** Tests copy row on relations */
    public static boolean createExample_9(DBMS dbms) {
       dbms.init();
       General.showDebug("createExample_9");
        try {
            Relation t = new Relation("Relation T", 99, dbms);
            t.insertColumn(0,"id",Relation.DATA_TYPE_INT, null);

            t.getNewRowIdList(9);
            //t.setValueByColumn("id", 999);
            t.setValue(0, "id", 1);          
            t.setValue(1, "id", 4);          
            t.setValue(2, "id", 2);          
            t.setValue(3, "id", 3);          
            t.setValue(4, "id", 5);          
            t.setValue(5, "id", 0);          
            t.setValue(6, "id", 3);          
            t.setValue(7, "id", 0);          
             
            General.showDebug("Before copy: " + t);
            General.verbosity = General.verbosityError;        
            if ( t.copyRow( 1, 9 ) ) {
                fail("Allowed an illegal copy");
            }
            General.verbosity = General.verbosityOutput;        
            if ( ! t.copyRow( 1, 3 ) ) {
                fail("copy row");
            }
            General.showDebug("After  copy: " + t);
        } catch ( Exception e ) {fail(e.getMessage());}
        return true;
    }

    /** Tests unparse to STAR */
    public static boolean createExample_10(DBMS dbms) {
       //General.verbosity = General.verbosityDebug;        
       dbms.init();
       General.showDebug("createExample_10");
        try {
            Relation t = new Relation("Relation T", 99, dbms);
            t.insertColumn(0,"test",Relation.DATA_TYPE_STRING, null);

            t.getNewRowIdList(3);
            //t.setValueByColumn("id", 999);
            t.setValue(0, "test", "1");          
            t.setValue(2, "test", "2");          
            String outStar = t.toSTAR();
            if ( outStar == null ) {
                fail("Relation.toSTAR 1");
            }
            General.showDebug(outStar);
            t.setValue(1, "test", "foo\n;bar"); 
            // The toSTAR method will issue a warning for the unrepresentable ; at the beginning of a line but proceed.
            General.verbosity = General.verbosityError;        
            outStar = t.toSTAR();
            General.verbosity = General.verbosityOutput;        
            if ( outStar == null ) {
                fail("Relation.toSTAR 2");
            }
            General.showDebug(outStar);            
        } catch ( Exception e ) {fail(e.getMessage());}
        return true;
    }

    
}
