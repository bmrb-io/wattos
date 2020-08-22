package Wattos.Soup.Constraint;

import java.io.Serializable;

import Wattos.Database.DBMS;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Soup.Molecule;
import Wattos.Utils.General;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 * The superclass of distance restraints.
 *It's setup in a flexible way so the levels can be changed by just
 *changing the implementations in this package; hopefully.
 *
 * @author Jurgen F. Doreleijers
 */
public class Constr extends RelationSoS implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    public static String      DEFAULT_ATTRIBUTE_TREE_NODE_ID    = "tree_node_id";
    public static String      DEFAULT_ATTRIBUTE_DOWN_ID         = "down_id";
    public static String      DEFAULT_ATTRIBUTE_RIGHT_ID        = "right_id";
    public static String      DEFAULT_ATTRIBUTE_LOGIC_OP        = "logic_op";

    public static String      DEFAULT_ATTRIBUTE_DC_ATOM_ID                        = "dc_atom_id";
    public static String      DEFAULT_ATTRIBUTE_DC_MEMB_ID                        = "dc_member_id";
    public static String      DEFAULT_ATTRIBUTE_DC_VIOL_ID                        = "dc_viol_id";
    public static String      DEFAULT_ATTRIBUTE_DC_NODE_ID                        = "dc_node_id";

    public static String      DEFAULT_ATTRIBUTE_SC_ATOM_ID                  = "sc_atom_id";

    public static String      DEFAULT_ATTRIBUTE_SUB_TYPE          = "sub_type";
    public static String      DEFAULT_ATTRIBUTE_FORMAT            = "format";
    public static String      DEFAULT_ATTRIBUTE_PROGRAM           = "program";
    public static String      DEFAULT_ATTRIBUTE_POSITION          = "position";

    public static String      DEFAULT_ATTRIBUTE_AVG_METHOD          = "avg_method";
    public static String      DEFAULT_ATTRIBUTE_NUMBER_MONOMERS     = "number_monomers";
    public static String      DEFAULT_ATTRIBUTE_PSEUDO_COR_NEEDED   = "pseudo_cor_needed";
    public static String      DEFAULT_ATTRIBUTE_FLOATING_CHIRALITY  = "floating_chirality";

    public static String      DEFAULT_ATTRIBUTE_CUTOFF  = "cutoff";

    public static String      DEFAULT_ATTRIBUTE_MEMBER_ID                                = "member_id";
    public static String      DEFAULT_ATTRIBUTE_CONTRIBUTION                             = "contribution";
    public static String      DEFAULT_ATTRIBUTE_TARGET                                   = "target";
    public static String      DEFAULT_ATTRIBUTE_TARGET_ERR                               = "target_err";
    public static String      DEFAULT_ATTRIBUTE_UPP_BOUND                                = "upp_bound";
    public static String      DEFAULT_ATTRIBUTE_LOW_BOUND                                = "low_bound";
    public static String      DEFAULT_ATTRIBUTE_PEAK_ID                                  = "peak_id";
    public static String      DEFAULT_ATTRIBUTE_WEIGHT                                   = "weight";
    public static String      DEFAULT_ATTRIBUTE_VALUE                                    = "value";
    public static String      DEFAULT_ATTRIBUTE_DISTANCE                                 = "distance";
    public static String      DEFAULT_ATTRIBUTE_VIOLATION                                = "violation";
    public static String      DEFAULT_ATTRIBUTE_VIOL_MAX                                 = "viol_upp_max";
    public static String      DEFAULT_ATTRIBUTE_VIOL_UPP_MAX_MODEL_NUM                   = "viol_upp_max_model_num";
    public static String      DEFAULT_ATTRIBUTE_VIOL_LOW_MAX                             = "viol_low_max";
    public static String      DEFAULT_ATTRIBUTE_VIOL_LOW_MAX_MODEL_NUM                   = "viol_low_max_model_num";
    public static String      DEFAULT_ATTRIBUTE_HAS_UNLINKED_ATOM                        = "has_unlinked_atom";
    public static String      DEFAULT_ATTRIBUTE_DIST_THEORETICAL                         = "dist_theoretical";

    public static String      DEFAULT_ATTRIBUTE_CONSTR_COUNT            = "constr_count";
    public static String      DEFAULT_ATTRIBUTE_VIOL_COUNT              = "viol_count";
    public static String      DEFAULT_ATTRIBUTE_VIOL_TOTAL              = "viol_total";
    public static String      DEFAULT_ATTRIBUTE_VIOL_RMS                = "viol_rms";
    public static String      DEFAULT_ATTRIBUTE_VIOL_ALL                = "viol_all";
    public static String      DEFAULT_ATTRIBUTE_VIOL_AV_VIOL            = "viol_av_viol";

    public static String      DEFAULT_ATTRIBUTE_CONSTR_LOW_COUNT            = "constr_low_count";
    public static String      DEFAULT_ATTRIBUTE_VIOL_LOW_COUNT              = "viol_low_count";
    public static String      DEFAULT_ATTRIBUTE_VIOL_LOW_TOTAL              = "viol_low_total";
    public static String      DEFAULT_ATTRIBUTE_VIOL_LOW_RMS                = "viol_low_rms";
    public static String      DEFAULT_ATTRIBUTE_VIOL_LOW_ALL                = "viol_low_all";
    public static String      DEFAULT_ATTRIBUTE_VIOL_LOW_AV_VIOL            = "viol_low_av_viol";

    /** The different levels in the gumbo: the physical rid. Array elements:
     set name; main relation name, main relation default column for id*/
    public static String[]      DEFAULT_ATTRIBUTE_SET_DC              = {"dc",         null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_DC_LIST         = {"dc_list",    null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_CDIH            = {"cdih",       null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_CDIH_LIST       = {"cdih_list",  null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_RDC             = {"rdc",        null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_RDC_LIST        = {"rdc_list",   null,null};

    public DistConstr           dc;
    public DistConstrList       dcList;
    public Cdih                 cdih;
    public CdihList             cdihList;
    public Rdc                  rdc;
    public RdcList              rdcList;

    public static Parameters p = new Parameters();

    static {
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_DC );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_DC_LIST );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_CDIH );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_CDIH_LIST );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_RDC );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_RDC_LIST );
    }

    public Constr(DBMS dbms) {
        super(dbms);
    }

    public void init(DBMS dbms) {
        super.init(dbms);
        //General.showDebug("Adding relation set for distance constraint lists");
        dcList     = new DistConstrList(dbms, this);        addRelationSet( dcList );
        //General.showDebug("Adding relation set for distance constraints");
        dc         = new DistConstr(dbms, this);            addRelationSet( dc );
//        General.showDebug("Adding relation set for cdih constraint lists");
        cdihList     = new CdihList(dbms, this);        addRelationSet( cdihList );
//        General.showDebug("Adding relation set for cdih constraints");
        cdih         = new Cdih(dbms, this);            addRelationSet( cdih );
//        General.showDebug("Adding relation set for RDC constraint lists");
        rdcList     = new RdcList(dbms, this);        addRelationSet( rdcList );
//        General.showDebug("Adding relation set for RDC constraints");
        rdc         = new Rdc(dbms, this);            addRelationSet( rdc );
    }


    /** Renumber all the rows in the relations in the constr.
     */
    public boolean renumberRows( ) {
        if ( ! dcList.renumberRows(Relation.DEFAULT_ATTRIBUTE_NUMBER, dcList.used, 1)) { // pure method
            General.showCodeBug("Failed to reset the numbering of entries.");
            return false;
        }
        if ( ! dc.renumberRows(Relation.DEFAULT_ATTRIBUTE_NUMBER, dc.used, 1)) { // pure method
            General.showCodeBug("Failed to reset the numbering of models.");
            return false;
        }
        return true;
    }

    public String toXplorAtomSel( int molNum, int resNum, String resName, String atomName,
            String atomNomenclature) {
        String segiStr     = Format.sprintf( "%4s", p.add( Molecule.toChain( molNum )));
        String resNumStr   = Format.sprintf( "%3d", p.add( resNum ));
        String atomNameStrXplor = dbms.ui.wattosLib.atomMap.atomNameToXplor(atomName,atomNomenclature,resName);
        String atomNameStr = Format.sprintf( "%-4s", p.add( atomNameStrXplor ));
//        return "(segi\"" +segiStr+"\"and resi "+resNumStr+" and name "+atomNameStr+")";

        // terse notation because of xplor 132 char per line limit.
        return "(atom \""+segiStr+"\" "+resNumStr+" "+atomNameStr+")";
    }


    public SimpleConstr getSimpleConstrByAttributeSet( String[] set ) {
//        General.showDebug("set: " + Strings.toString(set));
//        General.showDebug("DEFAULT_ATTRIBUTE_SET_CDIH: " + Strings.toString(DEFAULT_ATTRIBUTE_SET_CDIH));
        if ( set == DEFAULT_ATTRIBUTE_SET_CDIH ) {
            return cdih;
        }
        if ( set == DEFAULT_ATTRIBUTE_SET_RDC ) {
            return rdc;
        }
        General.showCodeBug("Failed to find SimpleConstr class");
        return null;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        General.showOutput("Starting tests" );
        General.setVerbosityToDebug();
        DBMS dbms = new DBMS();
//        Gumbo gumbo = new Gumbo(dbms);
        new Constr(dbms);

        // Test the atoms
//        if ( false ) {
//            float[] c1 = { 0.1234567890123456789f, 0.2f, 0.3f };
//            gumbo.atom.add("atomName1", true, c1, Defs.NULL_FLOAT);
//            gumbo.atom.add("atomName2", true, c1, Defs.NULL_FLOAT);
//            gumbo.atom.add("atomName3", true, c1, Defs.NULL_FLOAT);
//            String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_MAIN_RELATION_NAME];
//            Relation atomMain = gumbo.atom.getRelation( relationName );
//            if ( atomMain == null ) {
//                dbms.showGraph();
//                General.doCodeBugExit("No relation with name: " + relationName);
//            }
//
//            float[]     coor_x              = (float[])     atomMain.attr.get(  Gumbo.DEFAULT_ATTRIBUTE_COOR_X );
//            int[] rids = (int[]) atomMain.attr.get( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME] );
//            rids[0] = 0;
//            rids[1] = 1; // inserted before the entry list is actually created.
//            rids[2] = 2; // invalid key that will be noted.
//            coor_x[1] = 1.0f;
//            //General.showOutput( atomMain);
//        }
        // Test the graph
        if ( true ) {
            General.showOutput("DBMS graph test; is graph ok? : " + dbms.graphIsOkay());
            General.showOutput("Graph: ");
            dbms.graph.showGraph();
            dbms.foreignKeyConstrSet.checkConsistencySet(true,true);
            General.showDebug( "DBMS: " + dbms.toString());
        }
        General.showOutput("Done successfully with tests." );
    }
}
