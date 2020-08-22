package Wattos.Soup;

import Wattos.Database.DBMS;
import Wattos.Database.Relation;
import Wattos.Star.TagTable;

public class DihedralListByRes extends TagTable {
    /**
     * 
     */
    private static final long serialVersionUID = -9202386645627676710L;

    /** Creates a new instance of TagTable */
    public DihedralListByRes(String name, DBMS dbms) throws Exception {
        super(name, dbms);
        init(name, dbms);
        isFree = false;
        
//        insertColumn("_DebugRows",      Relation.DATA_TYPE_INT, null);
        insertColumn("_Entry_id",       Relation.DATA_TYPE_STRING, null);
        insertColumn("_Model_id",       Relation.DATA_TYPE_INT,null);
        insertColumn("_Seg_id",         Relation.DATA_TYPE_INT,null);
        insertColumn("_Comp_seq_ID",    Relation.DATA_TYPE_INT,null);
        insertColumn("_Comp_ID",        Relation.DATA_TYPE_STRING,null);
//        insertColumn("_Seg_id2",        Relation.DATA_TYPE_INT,null);
//        insertColumn("_Comp_seq_ID2",   Relation.DATA_TYPE_INT,null);
//        insertColumn("_Comp_ID2" ,      Relation.DATA_TYPE_STRING,null);
        insertColumn("_Angle_name" ,    Relation.DATA_TYPE_STRING,null);
        insertColumn("_Angle_value",    Relation.DATA_TYPE_FLOAT,null);
    }    
}
