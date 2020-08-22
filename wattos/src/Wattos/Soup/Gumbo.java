/*
 * Gumbo.java
 *
 * Created on March 11, 2003, 4:50 PM
 */

package Wattos.Soup;

import java.io.*;
import Wattos.Database.*;
import Wattos.Utils.*;

/**
 * Atoms and molecules and all organizational levels they bring about.
 *It's setup in a flexible way so the levels can be changed by just
 *changing the implementations in this package; hopefully.
 *
 * @author Jurgen F. Doreleijers
 */
public class Gumbo extends RelationSoS implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    /** Name as observed in the pdb file; can be considered an author name in some cases
    public static String      DEFAULT_ATTRIBUTE_PDB_NAME         = "pdb_name";
    public static String      DEFAULT_ATTRIBUTE_PDB_NUMBER       = "pdb_number";
    public static String      DEFAULT_ATTRIBUTE_PDB_CHAIN_ID     = "pdb_chain_id";
     */
    public static String      DEFAULT_ATTRIBUTE_AUTH_MOL_NAME       = "auth_mol_name";
    public static String      DEFAULT_ATTRIBUTE_AUTH_RES_NAME       = "auth_res_name";
    public static String      DEFAULT_ATTRIBUTE_AUTH_RES_ID         = "auth_res_id";    // author residue number not of type int but is a string!
    public static String      DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME      = "auth_atom_name";
    public static String      DEFAULT_ATTRIBUTE_PDB_INSERTION_CODE  = "_Atom_site.PDB_ins_code";

    /** Used in e.g. distance constraints for when atoms can't be matched to
     *molecular description.
     */
    public static String      DEFAULT_ATTRIBUTE_ATOM_NAME       = "atom_name";
    public static String      DEFAULT_ATTRIBUTE_RES_ID          = "res_id";
    public static String      DEFAULT_ATTRIBUTE_RES_NAME        = "res_name";
    public static String      DEFAULT_ATTRIBUTE_MOL_ID          = "mol_id";
    public static String      DEFAULT_ATTRIBUTE_ENTITY_ID       = "entity_id";

    public static String      DEFAULT_ATTRIBUTE_ATOM_IDS   = "atom_ids";

    /** Does the object (like atom or residue) have valid coordinates. The default value is like
     *any boolean: false.*/
    public static String      DEFAULT_ATTRIBUTE_HAS_COOR          = "has_coor";
    /** The x,y,z if present or null otherwise. */
    public static String      DEFAULT_ATTRIBUTE_COOR_X           = "coor_x";     // float
    public static String      DEFAULT_ATTRIBUTE_COOR_Y           = "coor_y";     // float
    public static String      DEFAULT_ATTRIBUTE_COOR_Z           = "coor_z";     // float
    public static String      DEFAULT_ATTRIBUTE_CHARGE           = "charge";
    public static String      DEFAULT_ATTRIBUTE_OCCUPANCY        = "occupancy";
    public static String      DEFAULT_ATTRIBUTE_BFACTOR          = "b_factor";
    public static String      DEFAULT_ATTRIBUTE_ELEMENT_ID       = "element_id";
    public static String      DEFAULT_ATTRIBUTE_POL_TYPE         = "pol_type";
    public static String      DEFAULT_ATTRIBUTE_ASYM_ID          = "asym_id"; // under molecule

    /** The different levels in the gumbo: the physical rid. Array elements:
     set name; main relation name, main relation default column for id*/
    public static String[]      DEFAULT_ATTRIBUTE_SET_ATOM         = {"atom",   null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_RES          = {"res",    null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_MOL          = {"mol",    null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_MODEL        = {"model",  null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_ENTRY        = {"entry",  null,null};
    // Associated objects.
    public static String[]      DEFAULT_ATTRIBUTE_SET_BOND         = {"bond",       null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_ANGLE        = {"angle",      null,null};
    public static String[]      DEFAULT_ATTRIBUTE_SET_DIHEDRAL     = {"dihedral",   null,null};
    // Associated objects.
    public static String[]      DEFAULT_ATTRIBUTE_SET_DISTANCE     = {"distance",       null,null};

    /** If a relation column refers to an orfIdList use this column name */
    public static String      DEFAULT_ATTRIBUTE_ORF_ID_LIST          = "orf_id_list";
    /** Is the save frame label in star files or a generic name */
    public static String      DEFAULT_ATTRIBUTE_ASSEMBLY_NAME        = "assembly_name";

    /** Are the models synchronized, meaning do the same mols/res/atoms occur in
     *each model as in the first model. This allows an atom property of model_siblings to
     *be maintained that is an ordered list of atom RIDs to the same atoms in different
     *molecules. The Entry variable should be set to false whenever there are updates or
     *deletions in mols/res/atoms within the entry. Next time around when an entry needs to
     *be synchronized the atom property will be regenerated.
     */
    public static String      DEFAULT_ATTRIBUTE_MODELS_SYNCED        = "models_synchronized";
    public static String      DEFAULT_ATTRIBUTE_MODEL_SIBLINGS       = "model_siblings";
    public static String      DEFAULT_ATTRIBUTE_MASTER_RID           = "atom_id_master_model";

    public static String      DEFAULT_ATTRIBUTE_ATOMS_HASH           = "atoms_hash";
    public static String      DEFAULT_ATTRIBUTE_PDB_COORDINATE_FILE_VERSION = "pdb_coordinate_file_version";
    public static String      DEFAULT_ATTRIBUTE_ATOM_A_ID            = "atom_A_id";
    public static String      DEFAULT_ATTRIBUTE_ATOM_B_ID            = "atom_B_id";
    public static String      DEFAULT_ATTRIBUTE_ATOM_C_ID            = "atom_C_id";
    public static String      DEFAULT_ATTRIBUTE_ATOM_D_ID            = "atom_D_id";


    static {
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_ATOM );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_RES );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_MOL );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_MODEL );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_ENTRY );

        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_BOND );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_ANGLE );
        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_DIHEDRAL );

        RelationSet.setDerivedNames( DEFAULT_ATTRIBUTE_SET_DISTANCE );
    }

    /** Store the atom leveled properties */
    public Atom     atom;
    public Residue  res;
    public Molecule mol;
    public Model    model;
    public Entry    entry;

    public Bond     bond;
    public Angle    angle;
    public Dihedral dihedral;

    public Distance distance;

    /** Creates a new instance of Gumbo (jummie)*/
    public Gumbo(DBMS dbms) {
        super(dbms);
    }

    public void init(DBMS dbms) {
        super.init(dbms);
        entry         = new Entry(dbms, this);      addRelationSet( entry );
        model         = new Model(dbms, this);      addRelationSet( model );
        mol           = new Molecule(dbms, this);   addRelationSet( mol );
        res           = new Residue(dbms, this);    addRelationSet( res );
        atom          = new Atom(dbms, this);       addRelationSet( atom );
        bond          = new Bond(dbms, this);       addRelationSet( bond );
        angle         = new Angle(dbms, this);      addRelationSet( angle );
        dihedral      = new Dihedral(dbms, this);   addRelationSet( dihedral );
        distance      = new Distance(dbms, this);   addRelationSet( distance );
    }


    /** Renumber all the rows in the relations in the gumbo. Only do the atoms if
     *specified because they're not always usefull and kind of expensive to do.
     */
    public boolean renumberRows( boolean doAtoms ) {
        if ( ! entry.renumberRows(Relation.DEFAULT_ATTRIBUTE_NUMBER, entry.used, 1)) { // pure method
            General.showCodeBug("Failed to reset the numbering of entries.");
            return false;
        }
        if ( ! model.renumberRows(Relation.DEFAULT_ATTRIBUTE_NUMBER, model.used, 1)) { // overwritten to renumber within entry DONE
            General.showCodeBug("Failed to reset the numbering of models.");
            return false;
        }
        if ( ! mol.renumberRows(Relation.DEFAULT_ATTRIBUTE_NUMBER, mol.used, 1)) { // overwritten to renumber within model DONE
            General.showCodeBug("Failed to reset the numbering of molecules.");
            return false;
        }
        if ( ! res.renumberRows(Relation.DEFAULT_ATTRIBUTE_NUMBER, res.used, 1)) { // overwritten to renumber within mol DONE
            General.showCodeBug("Failed to reset the numbering of residues.");
            return false;
        }
        if ( doAtoms ) {
            if ( ! atom.renumberRows(Relation.DEFAULT_ATTRIBUTE_NUMBER, atom.used, 1)) {// pure method
                General.showCodeBug("Failed to reset the numbering of atoms.");
                return false;
            }
        }
        return true;
    }


    /** Does a variety of check on the healthiness of the soup. E.g. multiple instances of the
     *atoms, residues, etc. Tries to correct the situation if asked to do so.
     */
    public boolean check( boolean makeCorrections ) {
	// Overall checking status.
	boolean status = true;

        // Within a model do atoms have the same coordinates? And other checks in the future.
        int m=model.selected.nextSetBit(0);
        for (;m>=0;m=model.selected.nextSetBit(m+1)) {
            if ( ! model.checkSoup( m, makeCorrections )) {
                status = false;
            }
        }

        // Within a residue do atoms have the same name? And other checks in the future.
        /** TODO finish
        int m=res.selected.nextSetBit(0);
        for (;m>=0;m=res.selected.nextSetBit(m+1)) {
            if ( ! res.checkSoup( m, makeCorrections )) {
                status = false;
            }
        }
         */
        if ( ! status ) {
            General.showError("Soup check overall not successful");
        }
        return status;
    }

    /** Render the soup into a more or less human readible list of tables.
     *Convenience method that simply calls the dbms namesake method.
     */
    public String toSTAR() {
        return dbms.toSTAR();
    }

    public void removeAllIndices() {
        entry.removeAllIndices();
        model.removeAllIndices();
        mol.removeAllIndices();
        res.removeAllIndices();
        atom.removeAllIndices();
        bond.removeAllIndices();
        angle.removeAllIndices();
        dihedral.removeAllIndices();
        distance.removeAllIndices();
    }

    public void resetConvenienceVariables() {
        entry.resetConvenienceVariables();
        model.resetConvenienceVariables();
        mol.resetConvenienceVariables();
        res.resetConvenienceVariables();
        atom.resetConvenienceVariables();
        bond.resetConvenienceVariables();
        angle.resetConvenienceVariables();
        dihedral.resetConvenienceVariables();
        distance.resetConvenienceVariables();
    }

}
