/*
 * GumboItem.java
 *
 * Created on November 8, 2002, 4:41 PM
 */

package Wattos.Soup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

import Wattos.CloneWars.UserInterface;
import Wattos.CloneWars.WattosItem;
import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexSortedInt;
import Wattos.Soup.Constraint.DistConstrList;
import Wattos.Utils.General;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.Wiskunde.Geometry3D;
import cern.colt.list.IntArrayList;

/**
 *
 * Every gumbo item has properties like 3D location in x,y,z. For residue 
 *and up this is the average spatial location which may or may not be set.
 *
 * This class will server as the template for the relationsets in the Soup.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class GumboItem extends WattosItem implements Serializable {
       
    private static final long serialVersionUID = -1207795172754062330L;    
    public BitSet      hasCoor;
    public float[]     xList;
    public float[]     yList;
    public float[]     zList;    
    public float[]     charge;

    public Gumbo       gumbo;          // so cast doesn't need to be done.

    /** Instance variable for speed to get a xyz key for a hashmap */
    private StringBuffer sb = new StringBuffer(8*3);
    
    public GumboItem(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        gumbo = (Gumbo) relationSoSParent;
        //General.showDebug("back in GumboItem constructor");
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets 
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public GumboItem(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug("back in GumboItem constructor");
        name = relationSetName;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
        //General.showDebug("now in GumboItem.init()");
        super.init(dbms);
        //General.showDebug("back in GumboItem.init()");

        // MAIN RELATION COMMON COLUMNS in addition to those in WattosItem
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR, new Integer(DATA_TYPE_BIT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_COOR_X,   new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_COOR_Y,   new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_COOR_Z,   new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_CHARGE,   new Integer(DATA_TYPE_FLOAT));
        
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_COOR_X);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_COOR_Y);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_COOR_Z);
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_CHARGE);                            
        //General.showDebug("in GumboItem.init() order list has number of items: " + DEFAULT_ATTRIBUTES_ORDER.size());        
        return true;
    }

        
    
    /** Add a new gumboItem to the list of gumboItems. Returns the index of the gumboItem's location in the
     *array or -1 to indicate failure.
     */
    public int add(String itemName, boolean has_coor, float[] coordinates, float chg ) {                
        int maxSize = mainRelation.sizeMax;
        int idx = super.add( itemName );
        if ( idx < 0 ) {
            General.showError("Failed to add new Gumbo Item for name: " + itemName);
            return -1;
        }
        if ( maxSize != mainRelation.sizeMax) {
            resetConvenienceVariables();
        }
        hasCoor.set(idx, has_coor);
        if ( has_coor ) {
            // COORDINATES
            xList[idx] = coordinates[0];
            yList[idx] = coordinates[1];
            zList[idx] = coordinates[2];
        }
        charge[ idx ] = chg;
        // Even for all the optional properties.
        return idx;
    }    

    /** Some hardcoded variable names need to be reset when the columns resize. */
    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();
        hasCoor         = (BitSet)      mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_HAS_COOR );
        xList           = (float[])     mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_COOR_X);
        yList           = (float[])     mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_COOR_Y);
        zList           = (float[])     mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_COOR_Z);
        charge          = (float[])     mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_CHARGE);
        if ( hasCoor == null || xList == null || yList == null || zList == null || charge == null ) {
            return false;
        }
        return true;
    }
    
    /** Check in the atom table if there's a fkc to this gumbo item. If not return false.
     *Otherwise, for each row check if there's at least 1 atom refering to it, if not
     *the gumbo item doesn't father any atom and will be deleted in a cascading fashion. This 
     *method will request for an index so it costs some cpu.
     */
    public boolean removeWithoutAtom() {
        String columnNameAtomRelation2Parent = null;
        // Find out who we are: model, mol, or residue
        if ( mainRelation.name.equals( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_MAIN_RELATION_NAME] )) {
            columnNameAtomRelation2Parent = Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ];
        }
        if ( mainRelation.name.equals( Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_MAIN_RELATION_NAME] )) {
            columnNameAtomRelation2Parent = Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[ RelationSet.RELATION_ID_COLUMN_NAME ];
        }
        if ( mainRelation.name.equals( Gumbo.DEFAULT_ATTRIBUTE_SET_RES[RELATION_ID_MAIN_RELATION_NAME] )) {
            columnNameAtomRelation2Parent = Gumbo.DEFAULT_ATTRIBUTE_SET_RES[ RelationSet.RELATION_ID_COLUMN_NAME ];
        }
        if ( columnNameAtomRelation2Parent == null ) {
            General.showError("GumboItem.removeWithoutAtom not supported for class: " + getClass().getName());
            return false;
        }

        if ( ! gumbo.atom.mainRelation.containsColumn( columnNameAtomRelation2Parent ) ) {
            General.showError("GumboItem.removeWithoutAtom failed to find a column in atomMain relation to gumbo item of class: " + getClass().getName());
            return false;
        }
        //General.showDebug("Number of atoms in gumbo before removing items without atoms: " + gumbo.atom.mainRelation.used.cardinality());
        
        IndexSortedInt index = (IndexSortedInt) gumbo.atom.mainRelation.getIndex( columnNameAtomRelation2Parent, Index.INDEX_TYPE_SORTED );
        if ( index == null ) {
            General.showError("GumboItem.removeWithoutAtom failed to get an index in atomMain relation to gumbo item of class: " + getClass().getName());
            return false;
        }

        BitSet rowSet = new BitSet(mainRelation.used.length()); // rows to remove
        int idx;
        for (int r=mainRelation.used.nextSetBit(0); r>=0; r=mainRelation.used.nextSetBit(r+1))  {
            // Just get the first rid if any
            idx = Arrays.binarySearch(index.values, r); // inlined for speed.
            if ( idx ==  -1 ) { // If it isn't found in the atoms table mark the item for deletion later on.
                rowSet.set( r );
            }
        }
        // Actually do remove in batch.
//        int itemCountToDelete = rowSet.cardinality();
        if ( ! mainRelation.removeRowsCascading(rowSet, true) ) {
            General.showError("Failed to remove gumbo items for which no atom is refering to");
            return false;                
        }
        
        //General.showDebug("Removed number of items from relation with name: " + mainRelation.name + " without atoms is: " + itemCountToDelete);
        //General.showDebug("Number of atoms in gumbo after removing items without atoms: " + gumbo.atom.mainRelation.used.cardinality());
        
            
        return true;
    }
    
    /** Given the rids in a list, calculate the average position of the items.
     *Returns null if the position can not be calculated.
     *
     *Put here instead of the Utils.Wiskunde.Geometry class because of speed, no
     *time to waste by copying the original data.
     */
    public float[] getAveragePosition( IntArrayList rids ) {
        float av_x = 0f;
        float av_y = 0f;
        float av_z = 0f;
        if ( rids.size()==0 ) {
            return null;
        }
        for (int i=0;i<rids.size();i++) {
            int itemRid = rids.getQuick(i);
            if ( Defs.isNull( itemRid )) {
                General.showWarning("Can't find gumbo item in getAveragePosition; skipping calculation");
                return null;
            }
            if ( ! used.get( itemRid )) {
                General.showError("The following rid of gumbo item is not in use: " + itemRid );
                return null;
            }
            if ( ! hasCoor.get( itemRid )) {
                General.showError("Item at rid has no coordinates: " + itemRid);
                return null;
            }
            float x = xList[itemRid];
            float y = yList[itemRid];
            float z = zList[itemRid];
            if ( Defs.isNull( x ) ||
                 Defs.isNull( y ) ||
                 Defs.isNull( z ) ) {
                General.showError("Already checked if an item didn't have coordinates but when looking at the numbers it doesn't have coordinates for all dimensions");
                return null;
            }
            //General.showDebug("For averaging the position adding x: " + x + " y: " + y + " z: " + z);
            av_x += x;
            av_y += y;
            av_z += z;
        }
        av_x /= rids.size();
        av_y /= rids.size();
        av_z /= rids.size();
        
        return new float[] { av_x, av_y, av_z };        
    }
        
    /** Checks a lot of validity. 
        Returns Defs.NULL_FLOAT on error */
    public float calcDistance( int objRidA, int objRidB ) {
        if ( ! (used.get( objRidA ) && used.get( objRidB ))) {
            General.showError("One or both of the following obj rids is not in use: " + objRidA + " or " + objRidB );
            return Defs.NULL_FLOAT;
        }
        if ( ! (hasCoor.get( objRidA ) && hasCoor.get( objRidB ))) {
            General.showError("One or both of the following objs has no coordinates: " + objRidA + " or " + objRidB );
            General.showError("Coordinates present: " + hasCoor.get( objRidA ) + " for obj: " + objRidA );
            General.showError("Coordinates present: " + hasCoor.get( objRidB ) + " for obj: " + objRidB );
            return Defs.NULL_FLOAT;
        }
        float x_a = xList[objRidA];
        float y_a = yList[objRidA];
        float z_a = zList[objRidA];
        float x_b = xList[objRidB];
        float y_b = yList[objRidB];
        float z_b = zList[objRidB];
        if ( Defs.isNull( x_a ) ||
             Defs.isNull( y_a ) ||
             Defs.isNull( z_a ) ||
             Defs.isNull( x_b ) ||
             Defs.isNull( y_b ) ||
             Defs.isNull( z_b ) ) {
            General.showError("Already checked if an obj didn't have coordinates but when looking at the numbers one or both doesn't have coordinates for all dimensions");
            return Defs.NULL_FLOAT;
        }
        return (float) Math.sqrt( (x_a-x_b)*(x_a-x_b) + (y_a-y_b)*(y_a-y_b) + (z_a-z_b)*(z_a-z_b) );
    }

    /** No checks done.*/
    public double calcDistanceFast( int a, double[] coor ) {
        return calcDistanceFast( a, new float[] {
                (float) coor[0],
                (float) coor[1], 
                (float) coor[2]});
    }
    
    /** No checks done.*/
    public float calcDistanceFast( int a, float[] coor ) {
        float xa = xList[a];
        float ya = yList[a];
        float za = zList[a];
        float xb = coor[0];
        float yb = coor[1];
        float zb = coor[2];
        return (float) Math.sqrt( (xa-xb)*(xa-xb) + (ya-yb)*(ya-yb) + (za-zb)*(za-zb) );
    }
    
    /** No checks done.*/
    public float calcDistanceFast( int a, int b ) {
        float xa = xList[a];
        float ya = yList[a];
        float za = zList[a];
        float xb = xList[b];
        float yb = yList[b];
        float zb = zList[b];
        return (float) Math.sqrt( (xa-xb)*(xa-xb) + (ya-yb)*(ya-yb) + (za-zb)*(za-zb) );
    }
    
    /**
     *Calculates the torsionangle between 4 points around the 2nd and 3rd point.
     *Adapted from the routine C program GENC by JFD based on routines
     *by Chang-Shung Tung and Eugene S. Carter (T10, Los Alamos 1994).
     */
    public double calcDihedral( int rid_a, int rid_b, int rid_c, int rid_d ) {
        double[] vector_a = getVector( rid_a );
        double[] vector_b = getVector( rid_b );
        double[] vector_c = getVector( rid_c );
        double[] vector_d = getVector( rid_d );
        return Geometry3D.dihedral( vector_a, vector_b, vector_c, vector_d );
    }

    
    /** Get the vector from a to b */
    public double[] getVector(int rid_a, int rid_b) {
        return new double[] {
            xList[rid_a] - xList[rid_b],
            yList[rid_a] - yList[rid_b],
            zList[rid_a] - zList[rid_b]
        };
    }

    /** Get the vector from zero to a*/
    public double[] getVector(int rid) {
        double[] result = new double[Geometry3D.DIM];
        result[0] = xList[rid];
        result[1] = yList[rid];
        result[2] = zList[rid];
        return result;
    }

    
    /**
     *The second point is connected to a and c.
     */
    public double calcAngle( int rid_a, int rid_b, int rid_c ) {    
        double[] vector_a = getVector(rid_a);
        double[] vector_b = getVector(rid_b);
        double[] vector_c = getVector(rid_c);
        return Geometry3D.angle( vector_a, vector_b, vector_c);     
    }
    
    /**
     * <code>containAABB</code> creates a minimum-volume axis-aligned
     * bounding box of the points, then selects the smallest 
     * enclosing sphere of the box with the sphere centered at the
     * boxes center.
     * @param points the list of points.
     *Returns two opposite corners of the box
     */
    public double[][] getEnclosingBoxCorners(BitSet points) {
        
        int i = points.nextSetBit(0);
        
        double min_x = xList[ i ];
        double min_y = yList[ i ];
        double min_z = zList[ i ];
        double max_x = xList[ i ];
        double max_y = yList[ i ];
        double max_z = zList[ i ];
        
        for (;i>=0;i = points.nextSetBit(i+1)) {                                            
            if ( xList[ i ] < min_x ) {
                min_x = xList[ i ];
            } else if ( xList[ i ] > max_x ) {
                max_x = xList[ i ];
            }
            if ( yList[ i ] < min_y ) {
                min_y = yList[ i ];
            } else if ( yList[ i ] > max_y ) {
                max_y = yList[ i ];
            }
            if ( zList[ i ] < min_z ) {
                min_z = zList[ i ];
            } else if ( zList[ i ] > max_z ) {
                max_z = zList[ i ];
            }
        }
        return new double[][] { 
            new double[] {min_x, min_y, min_z},  
            new double[] {max_x, max_y, max_z } }; // x,y,z of smallest and largest point
    }    

    /** Will return    
     Defs.NULL_FLOAT if not at least two of the given atoms contain coordinates
     */     
    public float getDiameter( IntArrayList a) {       
        return getDiameter( PrimitiveArray.toBitSet( a, mainRelation.sizeMax ));
    }
    
    /** Will return    
     Defs.NULL_FLOAT if not at least two of the given atoms contain coordinates.
     *
     *Calls for the coordinats of the enclosing box that is alligned with the axes (
     *this gives an upper limit but is not necessarily the smallest enclosing box.)
     */     
    public float getDiameter( BitSet todo ) {
        BitSet todoNew = (BitSet) todo.clone();
        todoNew.and( hasCoor );
        double[][] corners = getEnclosingBoxCorners( todoNew );
        float distance = (float) Geometry3D.distance( corners[0], corners[1] );
        return distance;
    }

    /** Will return    
     null if not at least one atom contains coordinates.
     *
     *Calls for the coordinats of the enclosing box that is alligned with the axes (
     *this gives an upper limit but is not necessarily the smallest enclosing box.)
     */     
    public float[] getCenter( BitSet todo ) {
        BitSet todoNew = (BitSet) todo.clone();
        todoNew.and( hasCoor );
        if ( todo.cardinality() == 0 ) {
            return null;
        }
        double[][] corners = getEnclosingBoxCorners( todoNew );
        float[] c1 = PrimitiveArray.toFloatArray( corners[0] );
        float[] c2 = PrimitiveArray.toFloatArray( corners[1] );
        return getAveragePosition(c1,c2);
    }
    
    /** Will return average position of the two coordinates given. Very fast.
     */     
    public static float[] getAveragePosition( float[] pA, float[] pB) {
        return new float[] { 
            (pA[0]+pB[0])/2, 
            (pA[1]+pB[1])/2, 
            (pA[2]+pB[2])/2, 
            };
    }
    
    /** Returns Defs.NULL_FLOAT on error. Usually called for atoms only.  
     *The list given contains contributions of which each contain a
     *pair of atom sets. If even 1 atom in a set doesn't exist this method will return null.
     */
    public float calcDistance(ArrayList gumboItemsInvolved, int avgMethod, int numberMonomers) {                
        
        if ( gumboItemsInvolved == null ) {
            General.showError("Sets of gumboItems involved in distance can't be null");
            return Defs.NULL_FLOAT;
        }
        if ( gumboItemsInvolved.size() == 0 ) {
            General.showError("Sets of gumboItems involved in distance can't be of zero size");
            return Defs.NULL_FLOAT;
        }
        if (( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_CENTER ) &&
            ( gumboItemsInvolved.size() != 1 )) {
            General.showError("Center averaging is not coded for ambiguous constraints that can only be described by using the OR in the schema.");
            General.showError("Found number of constraint nodes: " + gumboItemsInvolved.size());
            return Defs.NULL_FLOAT;
        }
        try {
            if (( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_SUM ) ||
                ( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_R6 ) ) {
                int pairCount = 0;
                for (int setId=0; setId<gumboItemsInvolved.size();setId++) {
                    IntArrayList[] gumboItemsSet = (IntArrayList[]) gumboItemsInvolved.get(setId);
                    IntArrayList gumboItemRidsA = gumboItemsSet[0];
                    IntArrayList gumboItemRidsB = gumboItemsSet[1];
                    pairCount += gumboItemRidsA.size() * gumboItemRidsB.size();
                }
                //General.showDebug("Calculating distances on number of sets of constraint nodes: " + gumboItemsInvolved.size() + " that total to number of gumboItem pairs: " + pairCount);
                
                // Allocate the space.
                float[] dist = new float[ pairCount ];
                pairCount = 0;
                for (int setId=0; setId<gumboItemsInvolved.size();setId++) {
                    IntArrayList[] gumboItemsSet = (IntArrayList[]) gumboItemsInvolved.get(setId);
                    IntArrayList gumboItemRidsA = gumboItemsSet[0];
                    IntArrayList gumboItemRidsB = gumboItemsSet[1];
                    for ( int gumboItemIdA=0; gumboItemIdA<gumboItemRidsA.size(); gumboItemIdA++) {
                        int gumboItemRidA = gumboItemRidsA.getQuick( gumboItemIdA );
                        if ( Defs.isNull( gumboItemRidA )) {
                            General.showWarning("Can't find gumboItem in calculating distance; skipping calculation");
                            return Defs.NULL_FLOAT;
                        }
                        for ( int gumboItemIdB=0; gumboItemIdB<gumboItemRidsB.size(); gumboItemIdB++) {
                            int gumboItemRidB = gumboItemRidsB.getQuick( gumboItemIdB );
                            if ( Defs.isNull( gumboItemRidB )) {
                                General.showWarning("Can't find gumboItem in calculating distance; skipping calculation");
                                return Defs.NULL_FLOAT;
                            }
                            dist[ pairCount ] = calcDistance( gumboItemRidA, gumboItemRidB );
                            //General.showDebug("One combinations distance is: " + dist[ pairCount ] );
                            pairCount++;
                        }
                    }
                }
                if ( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_SUM) {
                    return PrimitiveArray.getAverageSum( dist, -6.0, numberMonomers );
                }
                if ( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_R6) {
                    return PrimitiveArray.getAverageR6( dist, -6.0 );
                }
            } else if ( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_CENTER ) { // has only one element as checked before
                IntArrayList[] gumboItemsSet = (IntArrayList[]) gumboItemsInvolved.get(0);
                IntArrayList gumboItemRidsA = gumboItemsSet[0];
                IntArrayList gumboItemRidsB = gumboItemsSet[1];
                float[] avgA = getAveragePosition( gumboItemRidsA ); // average of gumboItems in set A
                float[] avgB = getAveragePosition( gumboItemRidsB );
                return Geometry3D.distance( avgA, avgB );
            }
            General.showError("Type of averaging not allowed by id: " + avgMethod + " Look in DistConstrList code for definitions");
        } catch ( Exception e ) {
            General.showThrowable(e);
        }
        return Defs.NULL_FLOAT;
    }
    
    public static boolean createAtoms_example_1(Gumbo gumbo) {
        gumbo.atom.add("A", true, new float[] { 0.1f, 0.2f, 0.3f }, 0.0f );
        gumbo.atom.add("B", true, new float[] { 0.1f, 9.2f, 0.3f }, 0.0f );
        gumbo.atom.add("C", true, new float[] { 0.1f, 4.2f, 0.3f }, 0.0f );
        gumbo.atom.add("D", true, new float[] { 2.1f, 4.2f, 0.3f }, 0.0f );
        return true;
    }
    
    /** Returns the rids of added bonds for close contacts. 
     */
    public BitSet calcCloseContacts( BitSet atomsInMaster, float distance ) {
        BitSet closeContacts = new BitSet();
        float d = Defs.NULL_FLOAT;
        int bond_rid = -1;
        for (int i=atomsInMaster.nextSetBit(0);i>=0;i=atomsInMaster.nextSetBit(i+1)) {
            for (int j=atomsInMaster.nextSetBit(i+1);j>=0;j=atomsInMaster.nextSetBit(j+1)) {
                // prevent to do the multiplication and root.
                d = Math.abs( gumbo.atom.xList[i] - 
                              gumbo.atom.xList[j]);
                if (  d > distance ) {
                    continue;
                }
                d = Math.abs( gumbo.atom.yList[i] - 
                              gumbo.atom.yList[j]);
                if (  d > distance ) {
                    continue;
                }
                d = calcDistanceFast( i, j );
                if (  d > distance ) {
                    continue;
                }
                // Add a close contact. This is the only memory intensive part.
                // Method could be more speedier when the new rows are prearranged. Let's see if it's needed first.
                bond_rid = gumbo.bond.add( i, j, Defs.NULL_INT,  Defs.NULL_INT, Bond.BOND_TYPE_TENTATIVE );
                if ( bond_rid < -1 ) {
                    General.showError("Failed to add a bond between atoms");
                    General.showError(gumbo.atom.toString(i));
                    General.showError(gumbo.atom.toString(j));
                    return null;
                }
                closeContacts.set(bond_rid );
            }                
        }
        return closeContacts;
    }
    
    /**
     * @param args the command line arguments; none used.
     */
    public static void main(String[] args) {
        General.showOutput("Starting tests" );
        
        UserInterface ui = new UserInterface();
        ui.initResources();
        Gumbo gumbo = ui.gumbo;
//        Constr constr = ui.constr;
//        DBMS dbms = ui.dbms;
        General.showMemoryUsed();
        
        createAtoms_example_1(gumbo);
        gumbo.atom.used.clear( 1 );
        double[][] corners = gumbo.atom.getEnclosingBoxCorners( gumbo.atom.used );
        General.showOutput("Found corner A: " + PrimitiveArray.toString( corners[0] ));
        General.showOutput("Found corner B: " + PrimitiveArray.toString( corners[1] ));
        double dia = gumbo.atom.getDiameter( gumbo.atom.used );
        General.showOutput("Found diameter to be: " + dia);
        General.showOutput("Finished with tests.");
    }

    /**
     * Simple setter.
     */
    public void setXYZ(int rid, float[] coor) {
        xList[ rid ] = coor[0];
        yList[ rid ] = coor[1];
        zList[ rid ] = coor[2];
    }
    
    /**
     * Code should even care about the object type returned
     */
    public String getKeyXYZ(int gumboItemRid) {
        sb.setLength(0); // empty buffer
        // Put the floats into a stringbuffer and then into the buf
        
        /**
         * doubleToString.appendFormatted( sb, xList[atomRid], 3, '.', ',', 3, '-', '\uffff');
         * doubleToString.appendFormatted( sb, yList[gumboItemRid], 3, '.', ',', 3, '-', '\uffff');
         * doubleToString.appendFormatted( sb, zList[gumboItemRid], 3, '.', ',', 3, '-', '\uffff');
         */
        sb.append( xList[gumboItemRid] );
        sb.append( yList[gumboItemRid] );
        sb.append( zList[gumboItemRid] );
        //General.showDebug("Key inside gettter is: " + sb);
        return sb.toString();
    }
    
    public HashMap getMapOnXYZ(BitSet todo) {
        HashMap nomAtomMap = new HashMap( todo.cardinality() );
        for (int rid=todo.nextSetBit(0);rid>=0; rid=todo.nextSetBit(rid+1)) {
            Object key = getKeyXYZ( rid );
            //General.showDebug("Found key: " + key);
            nomAtomMap.put(key, new Integer(rid));
        }
        if ( todo.cardinality() != nomAtomMap.size() ) {
            General.showDebug("Number of gumboItems todo: " + todo.cardinality());
            General.showDebug("but number of gumboItems in map on xyz: " + nomAtomMap.size());
            General.showDebug("perhaps multiple gumboItems with the same xyz are present");
        }
        return nomAtomMap;
    }
    
    /** Convenience method
     */
    public double calcDihedral(int[] itemList) {
        return calcDihedral( itemList[0], itemList[1], itemList[2], itemList[3] );        
    }

    /** the int part will truncate. */    
    public static float[] roundToPdbPrecision(float[] c) {
        c[0] =  ((int) (c[0]*1000))/1000f; 
        c[1] =  ((int) (c[1]*1000))/1000f; 
        c[2] =  ((int) (c[2]*1000))/1000f; 
        return c;
    }
    
    public boolean isCloseTo(double[] coor, BitSet know, float cutoff) {
        for (int r=know.nextSetBit(0);r>=0;r=know.nextSetBit(r+1)) {
            double d = calcDistanceFast(r, coor);
//            General.showDebug("Found distance: "+d); 
            if ( d < cutoff ) {
                return true;
            }
        }
        return false;
    }
    
}
