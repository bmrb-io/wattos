/*
 * Topology.java
 *
 * Created on August 12, 2005, 10:20 AM
 */

package Wattos.Soup;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.StringTokenizer;

import Wattos.Database.DBMS;
import Wattos.Database.Relation;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.Strings;

/**
 * Based on the WHAT IF program's file called TOPOLOGY.H
 *reads into a separate soup in a separate dbms the reference values for common
 *residues.
 * @author  jurgen
 */
public class TopologyLib implements Serializable {
    
    private static final long serialVersionUID = -1207795172754062330L;    
    
    /** Local resource */
    static final String WIF_FILE_LOCATION = "Data/TOPOLOGY.H";
        
    public String fileName = null;
    /** Using a slightly different name to clearly distinguish from main dbms.
     */
    public DBMS dbms = new DBMS();
    /** Using a slightly different name to clearly distinguish from main soup.
     */
    public Gumbo gumbo = new Gumbo(dbms);

    public static HashMap resNameMap = new HashMap();
    
    public static final int ATOM_NAMES_PER_LINE     = 14;
    public static final int BONDS_PER_LINE          = 10;
    public static final int TORSION_ANGLES_PER_LINE =  5;

       
    static {
        /** nb this software only reads the first 4 chars from the lib file */
        resNameMap.put("DADE", "DA");
        resNameMap.put("DTHY", "DT");
        resNameMap.put("DGUA", "DG");
        resNameMap.put("DCYT", "DC");
        resNameMap.put("DINO", "DI");
        resNameMap.put("OADE", "A");
        resNameMap.put("OTHY", "T");
        resNameMap.put("OURA", "U");
        resNameMap.put("OGUA", "G");
        resNameMap.put("OCYT", "C");
        // etc.
    }
    /** Watch out with the added properties here. The convenience variables don't get automatically updated
     *as do the main attributes of these classes do.
     */
    public TopologyLib() {
    }
    
    public boolean readWIFFile( URL url, AtomLibAmber atomLibAmber) {
        if ( url == null ) {
            url = getClass().getResource(WIF_FILE_LOCATION);
        }
        fileName = url.toString();
        if ( ! InOut.availableUrl(url) ) {
            General.showWarning("Url doesn't exist for name: " + url.toString());
            return false;
        }
        
        // Make a first pass reading all but comments into a string.
        BufferedReader bf = InOut.getBufferedReader(url);
        StringBuffer sb = new StringBuffer(5000*80); // estimated size is 5,000 lines        
        try {
            String line=bf.readLine();
            while ( line!=null ) {
                //line = line.trim();
                if ( line.startsWith("#")) {
                    //General.showDebug("Skipping comment line:\n" + line); 
                    line=bf.readLine();
                    continue;
                }
                if ( line.length() == 0 ) {
                    line=bf.readLine();
                    continue;
                }
                sb.append(line);
                sb.append(General.eol);
                line=bf.readLine();
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        StringReader sr = new StringReader( sb.toString() ); 
        bf = new BufferedReader( sr );
        
        
        // brew a good stew
        int entry_rid = gumbo.entry.add(  "reference", null, null);
        int model_rid = gumbo.model.add(  1,entry_rid);
        int mol_rid   = gumbo.mol.add(    "reference", ' ', model_rid, null);
        int res_rid     = 0;
        int atom_rid    = 0;
        int bond_rid    = 0;
        int angle_rid   = 0;
        int dihedral_rid= 0;
        

        int atomCount                   = 0;
        int bondCount                   = 0;
        int sideChainRotatableBondCount = 0;
        int torsionCount                = 0;
        int hbGroupCount                = 0;
        int angleCount                  = 0;
        int parameterSetCount           = 0;
        int parameterGroupCount         = 0;
//        int residueType                 = 0;
        int atomNameSetCount            = 0;
        
        int resCount = 0;
        try {
            String line=bf.readLine();
            //General.showDebug("Read line: " + line);
            while ( line!=null ) {
                if ( line.startsWith("*")) {
                    if ( line.startsWith("*END")) {
                        break;
                    }
                    line=bf.readLine();
                    //General.showDebug("Read line: " + line);
                    // Read the names and mass in strict format
                    resCount++;
                    res_rid = gumbo.res.add( null, resCount, null, null, mol_rid );
                    String residueName = line.substring(0,4).trim();
                    if ( resNameMap.containsKey(residueName)) {
                        residueName = (String) resNameMap.get(residueName);
                    }
                    if (residueName.length() > 3 ) {
                        General.showError("Failed to get a residue name with max length 3: ["+residueName+"]");
                    }
                    gumbo.res.nameList[      res_rid ] = residueName;
                    gumbo.res.molId[         res_rid ] = mol_rid;
                    gumbo.res.modelId[       res_rid ] = model_rid;
                    gumbo.res.entryId[       res_rid ] = entry_rid;
                    //nameOneChar  = line.substring(4,5);
                    //massStr= line.substring(5,line.length());
                    //tr.mass = Float.parseFloat( massStr );

                    // Read the 15 integers in free format
                    line=bf.readLine();
                    //General.showDebug("Read line: " + line);
                    StringTokenizer tokens = new StringTokenizer( line, " ");
                    // See file for detailed description of content.
                    atomCount                   = Integer.parseInt( tokens.nextToken());// AT
                    bondCount                   = Integer.parseInt( tokens.nextToken());// BND
                    sideChainRotatableBondCount = Integer.parseInt( tokens.nextToken());// CHI
                    torsionCount                = Integer.parseInt( tokens.nextToken());// TRS
                    tokens.nextToken();                                                 // CRD
                    tokens.nextToken();                                                 // #BL
                    hbGroupCount                = Integer.parseInt( tokens.nextToken());// HGR
                    tokens.nextToken();                                                 // HAT                    
                    angleCount                  = Integer.parseInt( tokens.nextToken());// ANG
                    tokens.nextToken();                                                 // 1-3
                    parameterSetCount           = Integer.parseInt( tokens.nextToken());// PAR
                    parameterGroupCount         = Integer.parseInt( tokens.nextToken());// PC
                    tokens.nextToken();                                                 // HNG
                    Integer.parseInt( tokens.nextToken());// TY //residueType
                    if ( tokens.hasMoreTokens() ) {
                        atomNameSetCount            = Integer.parseInt( tokens.nextToken());// NNA
                    } else {
                        atomNameSetCount            = 1;
                    }
                    /** Map from whatif 1-n to wattos rid */
                    int[] atomMap = new int[atomCount+1]; // WHAT IF counts starting at 1.
                    
                    for (int i=1;i<=atomCount;i++) {
                        atom_rid = gumbo.atom.mainRelation.getNextReservedRow(atom_rid);
                        if ( atom_rid == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                            atom_rid = gumbo.atom.mainRelation.getNextReservedRow(0);
                        }
                        atomMap[i] = atom_rid;
                    }
                    
                    //ATOM NAMES
                    int countLinesAtoms = atomCount/ATOM_NAMES_PER_LINE;
                    if ( atomCount%ATOM_NAMES_PER_LINE > 0 ) {
                        countLinesAtoms++;
                    }
                    int countLinesAtomsTotal = atomNameSetCount * countLinesAtoms;
                    //General.showDebug("Expecting number of atoms: " + atomCount);
                    //General.showDebug("Expecting number of lines with atom names: " + countLinesAtomsTotal);
                    // Read atom names of the first schema in fixed format 14a4
                    int i = 0; // id of atom in whatif way
                    boolean enoughDone = false;
                    String atomName = null;
                    for (int l=1;l<=countLinesAtomsTotal;l++) {                        
                        line=bf.readLine();
                        //General.showDebug("Read atom line: " + line);
                        if ( enoughDone ) {
                            continue;
                        }
                        tokens = new StringTokenizer( line, " ");
                        while ( tokens.hasMoreTokens() ) {
                            i++;
                            atom_rid = atomMap[i];
                            atomName = tokens.nextToken();
                            atomName = PdbFile.translateAtomNameFromPdb(atomName);
                            gumbo.atom.nameList[atom_rid] = gumbo.atom.nameListNR.intern( atomName );
                            gumbo.atom.resId[         atom_rid ] = res_rid;
                            gumbo.atom.molId[         atom_rid ] = mol_rid;
                            gumbo.atom.modelId[       atom_rid ] = model_rid;
                            gumbo.atom.entryId[       atom_rid ] = entry_rid;
                            gumbo.atom.elementId[     atom_rid ] = Chemistry.getElementId( atomName );
                            if ( atomLibAmber != null ) {
                                gumbo.atom.type[          atom_rid ] = atomLibAmber.getAtomTypeId(residueName, atomName);
                            }
                            
                            if ( i==atomCount ) {
                                //General.showDebug("done reading atom names");
                                enoughDone = true;
                                break;
                            }
                        }                        
                    }
                    if ( i != atomCount ) {
                        General.showError("Expected to read number of atom names: " + atomCount + " but got: " + i);
                        return false;
                    }
                    //General.showDebug("done reading atom names really");
                    
                    //BONDS
                    int countLinesBonds = bondCount/BONDS_PER_LINE;
                    if ( bondCount%BONDS_PER_LINE > 0 ) {
                        countLinesBonds++;
                    }
                    // Read atom names of the first schema in fixed format 14a4
                    int atom_A_wif_id = -1;
                    int atom_B_wif_id = -1;
                    int atom_C_wif_id = -1;
                    int atom_D_wif_id = -1;
                    int atom_A_rid = -1;
                    int atom_B_rid = -1;
                    int atom_C_rid = -1;
                    int atom_D_rid = -1;
                    i = 0; // reset counter for number of bonds.
                    /** Map from whatif 1-n to wattos rid */
                    int[] bondMap = new int[bondCount+1]; // WHAT IF counts starting at 1.
                    
                    for (int l=1;l<=countLinesBonds;l++) {                        
                        line=bf.readLine();
                        //General.showDebug("Read bond line: " + line);
                        tokens = new StringTokenizer( line, " ");
                        while ( tokens.hasMoreTokens() ) {
                            i++; 
                            atom_A_wif_id = Integer.parseInt( tokens.nextToken());
                            atom_B_wif_id = Integer.parseInt( tokens.nextToken());
                            atom_A_rid = atomMap[atom_A_wif_id];
                            atom_B_rid = atomMap[atom_B_wif_id];
                            if ( ! gumbo.atom.used.get(atom_A_rid)) {
                                General.showError("Failed to locate atom A with wif id: " + atom_A_wif_id + " and rid: " + atom_A_rid );
                                return false;
                            }
                            if ( ! gumbo.atom.used.get(atom_B_rid)) {
                                General.showError("Failed to locate atom B with wif id: " + atom_B_wif_id + " and rid: " + atom_B_rid );
                                return false;
                            }
                            
                            bond_rid = gumbo.bond.mainRelation.getNextReservedRow(bond_rid);
                            if ( bond_rid == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                                bond_rid = gumbo.bond.mainRelation.getNextReservedRow(0);
                            }
                            gumbo.bond.atom_A_Id[ bond_rid ] = atom_A_rid;
                            gumbo.bond.atom_B_Id[ bond_rid ] = atom_B_rid;
                            gumbo.bond.type[      bond_rid ] = Bond.BOND_TYPE_UNKNOWN;
                            gumbo.bond.modelId[   bond_rid ] = model_rid;
                            gumbo.bond.entryId[   bond_rid ] = entry_rid;                            
                            bondMap[i] = bond_rid;
                        }
                    }
                    if ( i != bondCount ) {
                        General.showError("Expected to read number of bonds: " + bondCount + " but got: " + i);
                        return false;
                    }
                    //General.showDebug("done reading bonds really");
                    
                    
                    //ROTABLE BONDS (ignore)
                    //General.showDebug("Expecting number of lines with rotable bonds: " + sideChainRotatableBondCount);
                    for (int l=1;l<=sideChainRotatableBondCount;l++) {                        
                        line=bf.readLine();
                        //General.showDebug("Read rotable bonds line: " + line);
                    }
                    //General.showDebug("done reading rotable bonds really");                    

                    //TORSION ANGLES
                    int countLinesTorsionAngles = torsionCount/TORSION_ANGLES_PER_LINE;
                    if ( torsionCount%TORSION_ANGLES_PER_LINE > 0 ) {
                        countLinesTorsionAngles++;
                    }    
                    i=0;
                    //General.showDebug("Expecting number of torsion angle lines (10 per line): " + countLinesTorsionAngles);
                    for (int l=1;l<=countLinesTorsionAngles;l++) {                        
                        line=bf.readLine();
                        //General.showDebug("Read torsion line: " + line);
                        tokens = new StringTokenizer( line, " ");
                        while ( tokens.hasMoreTokens() ) {
                            i++; 
                            atom_A_wif_id = Integer.parseInt( tokens.nextToken());
                            atom_B_wif_id = Integer.parseInt( tokens.nextToken());
                            atom_C_wif_id = Integer.parseInt( tokens.nextToken());
                            atom_D_wif_id = Integer.parseInt( tokens.nextToken());
                            atom_A_rid = atomMap[atom_A_wif_id];
                            atom_B_rid = atomMap[atom_B_wif_id];
                            atom_C_rid = atomMap[atom_C_wif_id];
                            atom_D_rid = atomMap[atom_D_wif_id];
                            if ( ! gumbo.atom.used.get(atom_A_rid)) {
                                General.showError("Failed to locate atom A with wif id: " + atom_A_wif_id + " and rid: " + atom_A_rid );
                                return false;
                            }
                            if ( ! gumbo.atom.used.get(atom_B_rid)) {
                                General.showError("Failed to locate atom B with wif id: " + atom_B_wif_id + " and rid: " + atom_B_rid );
                                return false;
                            }
                            if ( ! gumbo.atom.used.get(atom_C_rid)) {
                                General.showError("Failed to locate atom C with wif id: " + atom_C_wif_id + " and rid: " + atom_C_rid );
                                return false;
                            }
                            if ( ! gumbo.atom.used.get(atom_D_rid)) {
                                General.showError("Failed to locate atom D with wif id: " + atom_D_wif_id + " and rid: " + atom_D_rid );
                                return false;
                            }
                            
                            dihedral_rid = gumbo.dihedral.mainRelation.getNextReservedRow(dihedral_rid);
                            if ( dihedral_rid == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                                dihedral_rid = gumbo.dihedral.mainRelation.getNextReservedRow(0);
                            }
                            gumbo.dihedral.atom_A_Id[ dihedral_rid ] = atom_A_rid;
                            gumbo.dihedral.atom_B_Id[ dihedral_rid ] = atom_B_rid;
                            gumbo.dihedral.atom_C_Id[ dihedral_rid ] = atom_C_rid;
                            gumbo.dihedral.atom_D_Id[ dihedral_rid ] = atom_D_rid;
                            gumbo.dihedral.modelId[   dihedral_rid ] = model_rid;
                            gumbo.dihedral.entryId[   dihedral_rid ] = entry_rid;                            
                            //dihedralMap[i] = dihedral_rid;
                        }
                    }
                    if ( i != torsionCount ) {
                        General.showError("Expected to read number of bonds: " + torsionCount + " but got: " + i);
                        return false;
                    }
                    //General.showDebug("done reading torsion angles really");                    

                    //ATOM COORDINATES and hydrogen bonding
                    for (int l=1;l<=atomCount;l++) {                        
                        line=bf.readLine();
                        //General.showDebug("Read atom coordinate line of length: " + line.length() + " " + line);
                        atom_rid = atomMap[l];             
                        if ( line.length() < 27 ) { // Very rare.
                            line = Strings.makeStringOfLength( line, 27 );
                        }
                        String textCoordinates = line.substring(0,27);
                        tokens = new StringTokenizer( textCoordinates, " ");
                        // Usually the coordinates are nicely separated.
                        if ( tokens.countTokens() == 3 ) {
                            gumbo.atom.xList[atom_rid] = Float.parseFloat( tokens.nextToken());
                            gumbo.atom.yList[atom_rid] = Float.parseFloat( tokens.nextToken());
                            gumbo.atom.zList[atom_rid] = Float.parseFloat( tokens.nextToken());
                        } else {
                            int idx = 0;
                            gumbo.atom.xList[atom_rid] = Float.parseFloat( line.substring(idx,idx+9)); idx+=9;
                            gumbo.atom.yList[atom_rid] = Float.parseFloat( line.substring(idx,idx+9)); idx+=9;
                            gumbo.atom.zList[atom_rid] = Float.parseFloat( line.substring(idx,idx+9)); idx+=9;
                        }
                        gumbo.atom.isHBDonor.clear(atom_rid);
                        gumbo.atom.isHBAccep.clear(atom_rid);
                        if ( line.length() >= 30 && line.charAt(29) == 'T' ) {
                            gumbo.atom.canHydrogenBond.set(atom_rid);    
                            // + donor
                            // - acceptor
                            
                            char type = ' ';
                            if ( line.length() >= 31) {
                                type = line.charAt(30);                            
                            }
                            switch ( type ) {
                                case ' ': { 
                                    gumbo.atom.isHBDonor.set(atom_rid);
                                    gumbo.atom.isHBAccep.set(atom_rid);
                                    break;
                                }
                                case '+': {
                                    gumbo.atom.isHBDonor.set(atom_rid);
                                    break;
                                }
                                case '-': { 
                                    gumbo.atom.isHBAccep.set(atom_rid);
                                    break;
                                }
                            }
                        } else {
                            gumbo.atom.canHydrogenBond.clear(atom_rid);  // default anyway.
                        }                            
                    }
                    //General.showDebug("done reading atom coordinates");                                        

                    //BOND PARAMETERES
                    for (int l=1;l<=bondCount;l++) {                        
                        line=bf.readLine();
                        //General.showDebug("Read bond parameters: " + line);
                        tokens = new StringTokenizer( line, " ");
                        bond_rid = bondMap[l];                      
                        gumbo.bond.referenceValue[bond_rid] = Float.parseFloat( tokens.nextToken());
                        gumbo.bond.referenceSD[   bond_rid] = Float.parseFloat( tokens.nextToken());
                    }
                    //General.showDebug("done reading bond parameters");                                        

                    //ANGLE PARAMETER
                    int[] angleMap = new int[angleCount+1]; // WHAT IF counts starting at 1.
                    for (int l=1;l<=angleCount;l++) {                        
                        line=bf.readLine();
                        //General.showDebug("Read angle parameters: " + line);
                        tokens = new StringTokenizer( line, " ");
                        atom_A_wif_id = Integer.parseInt( tokens.nextToken());
                        atom_B_wif_id = Integer.parseInt( tokens.nextToken());
                        atom_C_wif_id = Integer.parseInt( tokens.nextToken());
                        atom_A_rid = atomMap[atom_A_wif_id];
                        atom_B_rid = atomMap[atom_B_wif_id];
                        atom_C_rid = atomMap[atom_C_wif_id];                        
                                                
                        angle_rid = gumbo.angle.mainRelation.getNextReservedRow(angle_rid);
                        if ( angle_rid == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                            angle_rid = gumbo.angle.mainRelation.getNextReservedRow(0);
                        }
                        gumbo.angle.atom_A_Id[    angle_rid ] = atom_A_rid;
                        gumbo.angle.atom_B_Id[    angle_rid ] = atom_B_rid;
                        gumbo.angle.atom_C_Id[    angle_rid ] = atom_C_rid;
                        gumbo.angle.modelId[      angle_rid ] = model_rid;
                        gumbo.angle.entryId[      angle_rid ] = entry_rid;                            
                        angleMap[l] = angle_rid;                        
                        gumbo.angle.referenceValue[angle_rid] = Float.parseFloat( tokens.nextToken());
                        gumbo.angle.referenceSD[   angle_rid] = Float.parseFloat( tokens.nextToken());
                        // Ignoring the second set of reference values for CYS.
                    }
                    //General.showDebug("done reading angle parameters");                                        
                    // skip the 1-3 connections (1 per atom).
                    for (i=1;i<=atomCount;i++) {
                        line=bf.readLine();
                        //General.showDebug("Read AT line: " + line);
                    }
                    // skip the hb groups
                    for (i=1;i<=hbGroupCount;i++) {
                        line=bf.readLine();
                        //General.showDebug("Read HB group line: " + line);
                    }
                    // skip the parameters groups
                    for (i=1;i<=parameterSetCount;i++) {
                        for (int j=1;j<=parameterGroupCount;j++) {
                            line=bf.readLine();
                            //General.showDebug("Read parameter line: " + line);
                        }
                    }
                    //return true;
                }                
                line=bf.readLine();
                //General.showDebug("Read line: " + line);
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        gumbo.atom.mainRelation.cancelAllReservedRows();
        gumbo.bond.mainRelation.cancelAllReservedRows();
        gumbo.angle.mainRelation.cancelAllReservedRows();
        gumbo.dihedral.mainRelation.cancelAllReservedRows();
        gumbo.bond.calculateValues(       gumbo.bond.used);
        gumbo.angle.calculateValues(      gumbo.angle.used);
        gumbo.dihedral.calculateValues(   gumbo.dihedral.used);
        return true;
    }    
}
