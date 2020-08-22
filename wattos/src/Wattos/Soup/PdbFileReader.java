/*
 * PdbFileReader.java
 *
 * Created on March 6, 2003, 4:24 PM
 */

package Wattos.Soup;

import java.io.*;
import java.net.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.Common.*;

/**Reads a PDB formatted coordinate file. 
 *@see Wattos.Utils.FastFileReader
 * @author Jurgen F. Doreleijers
 */
public class PdbFileReader extends FastFileReader {
        
    /** BEGIN BLOCK COPY FROM Wattos.Soup.PdbFile */
    public DBMS         dbms;
    public Gumbo        gumbo;    
    public Atom         atom;
    public Residue      res;
    public Molecule     mol;
    public Model        model;
    public Entry        entry;
    public Relation     atomMain;
    public Relation     resMain;
    public Relation     molMain;
    public Relation     modelMain;
    public Relation     entryMain;
    
    int currentAtomId   = Defs.NULL_INT;
    int currentResId    = Defs.NULL_INT;
    int currentMolId    = Defs.NULL_INT;
    int currentModelId  = Defs.NULL_INT;
    int currentEntryId  = Defs.NULL_INT;
    /** END BLOCK */

    public int      MAX_ATOM_ERRORS_TO_REPORT = 2;    
    public int      error_count               = 0;
    public char     chainId;
    public char     chainIdOld                = Defs.NULL_CHAR;    
    public String   insertionCodeResidue      = Defs.NULL_STRING_NULL;    
    public String   insertionCodeResidueOld   = "ImpossibleInsertionCode";    
    public int      modelNumber               = Defs.NULL_INT;
    public float    coor_x                    = Defs.NULL_FLOAT;
    public float    coor_y                    = Defs.NULL_FLOAT;
    public float    coor_z                    = Defs.NULL_FLOAT;
    public float    occupancy                 = Defs.NULL_FLOAT;
    public float    charge                    = Defs.NULL_FLOAT;
    public float    bfactor                   = Defs.NULL_FLOAT;
    public int      elementId                 = Defs.NULL_INT;

    public String   atomName                  = Defs.NULL_STRING_NULL;
    public String   resName                   = Defs.NULL_STRING_NULL;
    
    public String   authMolName               = Defs.NULL_STRING_NULL;  // String representation of chain id pre-iterned.
    public String   authResName               = Defs.NULL_STRING_NULL;
    public int      authResIdOld              = Defs.NULL_INT;
    public int      authResId                 = Defs.NULL_INT;
    public String   atomNamePreFixed          = Defs.NULL_STRING_NULL;

    public boolean  hasCoor                   = true;
    public boolean  needToSkipFirstModelRecord;
    public boolean  needNewMolecule           = true; // Implies needNewResidue
    public boolean  needNewResidue            = true; 
    
    public              int countResidueInsertionCodeReported = 0;
    public static final int   maxResidueInsertionCodeReported = 100;
    public              int newResNumber;
    /** Creates a new instance of PdbFileReader */
    public PdbFileReader( DBMS dbms ) {        
        super(); 
        this.dbms = dbms;        
        boolean status = initConvenienceVariables();        
        if ( ! status ) {
            General.showError("failed to initConvenienceVariables");
        }
        countResidueInsertionCodeReported = 0;
    }

    /** BEGIN BLOCK FOR SETTING LOCAL CONVENIENCE VARIABLES COPY FROM Wattos.Soup.PdbFile */
    public boolean initConvenienceVariables() {
        
        atomMain = dbms.getRelation( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_MAIN_RELATION_NAME] );        
        if ( atomMain == null ) {
            General.showError("failed to find the atom main relation");
            return false;
        }
        atom = (Atom) atomMain.getRelationSetParent();
        if ( atom == null ) {
            General.showError("failed to find atom RelationSet");
            return false;
        }        
        gumbo = (Gumbo) atom.getRelationSoSParent();
        if ( gumbo == null ) {
            General.showError("failed to find the gumbo RelationSoS");
            return false;
        }        
        atom    = gumbo.atom;
        res     = gumbo.res;
        mol     = gumbo.mol;
        model   = gumbo.model;
        entry   = gumbo.entry;
        atomMain   = atom.mainRelation;
        resMain    = res.mainRelation;
        molMain    = mol.mainRelation;
        modelMain  = model.mainRelation;
        entryMain  = entry.mainRelation;
        return true;
    }
    /** END BLOCK */
    boolean shownSeqResWarning = false;
    
    public boolean processSeqRes(char buf[], int offset, int length_line) {
        if ( ! shownSeqResWarning ) {
//            General.showDebug("No code for processing seqres records yet.");        
            shownSeqResWarning=true;
        }
        return true;
    }

    /** Parse according to:
COLUMNS       DATA TYPE      FIELD         DEFINITION
----------------------------------------------------------------------
 1 -  6       Record name    "MODEL "
11 - 14       Integer        serial        Model serial number.
     */
    public boolean processModel(char buf[], int offset, int length_line) {        
        int modelNumberNew = CharArray.parseIntSimple(buf, offset+10, 4);
        /** First model is always automatically created; nothing needs to be done.*/
        if ( needToSkipFirstModelRecord && modelNumber == 1 ) {
            needToSkipFirstModelRecord = false;
            return true;
        }
        
        modelNumber += 1;
        if ( modelNumberNew != modelNumber ) {
            General.showWarning("Model number parsed isn't one higher than previous model number in entry");        
            General.showWarning("Model number parsed is: " + modelNumberNew);
            General.showWarning("Model number taken is: "  + modelNumber);
        }

        currentModelId = model.add(modelNumber, currentEntryId);
        if ( currentModelId < 0 ) {
            General.showCodeBug("Failed to add a model into dbms.");
            return false;
        }
        //General.showDebug("Added new model with rid:" + currentModelId);        
        model.selected.set(currentModelId);        
        // Signal that a new molecule needs to be made.
        needNewMolecule = true;
        return true;
    }
    
    /** Signal that upon reading the NEXT atom a new molecule should be made
     *There might not be a next atom though.
     */
    public boolean processMol(char buf[], int offset, int length_line) {                
        //General.showDebug("Found a ter in the pdb file.");
        // Signal that a new molecule and residue need to be made.
        needNewMolecule = true;
        return true;
    }
    
    
    
    /** Only method to override from super class:*/
    public boolean doSomethingWith(char buf[], int offset, int length_line) {                
        String elementStr = null;
        char firstChar;
        try {
            //ATOM
            /** Just reading atomS for the moment. Most lines start with ATOM */
            if ( !( CharArray.matches( buf, offset, PdbFile.RECORD_ATOM ) ||
                    CharArray.matches( buf, offset, PdbFile.RECORD_HETATM ))) {
                //SEQRES
                if ( CharArray.matches( buf, offset, PdbFile.RECORD_SEQRES )) {
                    return processSeqRes(buf, offset, length_line);
                //MODEL
                } else if ( CharArray.matches( buf, offset, PdbFile.RECORD_MODEL )) {
                    return processModel(buf, offset, length_line);
                } else if ( CharArray.matches( buf, offset, PdbFile.RECORD_TER )) {
                    return processMol(buf, offset, length_line);
                } else {
                    //General.showDebug("Ignoring line: " + new String(buf, offset, length_line));        
                    return true;
                }
            }

            if ( length_line < 54 ) {
                General.showWarning("ATOM/HETATM record line shorter than 54 characters ignored: " + 
                        new String(buf, offset, length_line));
                return false;
            }
                
            // Fill the atomMain relation first            
            //General.showDebug("Reading atom off line: " + new String(buf, offset, length_line));
            
            //int    atomNumber               = CharArray.parseIntSimple(buf, offset+6, 5);
            char   alternateLocation        = buf[ offset + 16 ];            // Uses x additional chars w.r.t. specs.
            if ( alternateLocation != ' ' ) {
                General.showWarning("Skipping line with alternate location char: " + alternateLocation );
                General.showWarning("On line: " + new String(buf, offset, length_line) );
                return true;
            }
            //PdbFile.translateResidueNameFromPDB( buf, offset+17); // Use the four characters for things like HIS+.
            authResName              = (new String(buf, offset+17, 4)).trim(); 
            chainId                  = buf[ offset + 21 ];
            authResId                = CharArray.parseIntSimple(buf, offset+22, 4);
            insertionCodeResidue     = (new String(buf, offset+26, 1));  // Usually it's " "            
            if ( chainId == ' ' ) {
                chainId = Defs.NULL_CHAR;
            }                        
            if ( insertionCodeResidue.equals(" ") ) {
                insertionCodeResidue = Defs.NULL_STRING_NULL;
            }                        
            coor_x = Float.parseFloat( new String( buf, offset+30, 8));
            coor_y = Float.parseFloat( new String( buf, offset+38, 8));
            coor_z = Float.parseFloat( new String( buf, offset+46, 8));

            //occupancy   = CharArray.parseFloatSimple(buf, offset+54, 6); //Not present in all files so deleted.
            //bfactor     = CharArray.parseFloatSimple(buf, offset+60, 6); //Not present in all files so deleted.         
            
            
            /** Can't process the atom charge because info is sometimes overwritten:
            // E.g. entry 1hue:
            // ATOM  36089  N   MET A   1     -13.364   7.174  -0.470  1.00  0.00      1HUEA  3
            if ( length_line < 79 ) {
                charge = Defs.NULL_FLOAT;
            } else {                
                charge = PdbFile.translateAtomCharge(buf,offset+78);
            }*/            

            // SEE WHERE DOES THE ATOM GOES IN THE GUMBO
            
            //NEW MOLECULE
            if ( chainId != chainIdOld ) {
                needNewMolecule = true;
            }            
            if ( needNewMolecule ) {
                needNewMolecule = false;
                needNewResidue = true;
                chainIdOld = chainId;                
                currentMolId = mol.add(null, chainId, currentModelId, null);
                if ( currentMolId < 0 ) {
                    General.showCodeBug( "Failed to add a new molecule; stop reading");
                    General.showCodeBug( "PDB chain id is: " + chainId);
                    return false;
                }
                //General.showDebug("Added new molecule with rid:" + currentMolId);        
                mol.selected.set( currentMolId );
                if ( Defs.isNull(chainId)) {
                    // important to make it a dot because comparisons will be done later assuming so
                    /** @see Atom#compareAuthor */
                    authMolName = Defs.NULL_STRING_DOT;
                } else {
                    authMolName = String.valueOf( chainId );
                }
                authMolName = atom.authMolNameListNR.intern( authMolName ); // short list for entries read from PDB formatted files.
                currentResId = Defs.NULL_INT; // Indicates to the residue creator that numbering should restart.
            }
            
            
            //NEW RESIDUE
            if (( authResId            != authResIdOld ) ||
                ( insertionCodeResidue != insertionCodeResidueOld )) {
                needNewResidue = true;
            }

            //General.showDebug("authResId: " + authResId + "authResIdOld: " + authResIdOld );
            /** Check for new residue */
            if ( needNewResidue ) {
                needNewResidue = false;
                authResIdOld            = authResId;
                insertionCodeResidueOld = insertionCodeResidue;               
                if ( Defs.isNull( currentResId )) {
                    newResNumber = 1; // Overwritten in all but first residue case
                } else {
                    // Take residue number from previous residue and increment
                    newResNumber = res.number[currentResId] + 1; 
                }
                resName = authResName;                
                currentResId = res.add( resName, newResNumber, Integer.toString( authResId), authResName, currentMolId);
                if ( currentResId < 0 ) {
                    General.showCodeBug( "Failed to add a new residue; stop reading");
                    General.showCodeBug( "PDB residue number is: " + authResId);
                    return false;
                }
                //General.showDebug("Added new residue with rid:" + currentResId);        
                res.selected.set( currentResId );                
            }
            
            String atomNamePreFixed = new String(buf, offset+12, 4);
            // No trimming because element id needs to be derived first.
//            atomNamePreFixed = atomNamePreFixed.trim();
            atomName =  mapAtomToPostfixedNumber( atomNamePreFixed.trim() );
            atomName = atomName.replace('*', '\'');
            // Process the element Id. Can be speeded up by caching too.
            boolean elementIdPresent = false;
            if ( length_line < 78 ) {
                elementId = Defs.NULL_INT;
            } else {                
                char LChar = buf[offset+76];
                char RChar = buf[offset+77];
                if ( (LChar == ' ') && ( RChar == ' ') ) {
                elementId = Defs.NULL_INT;
                // Cover the case where there is no element id but a number like in:          ^^
                //ATOM      1  N   LYS A 319      14.420  24.448   8.520  1.00  4.54      1OLH 319
                // instead of:
                //ATOM      2  CA  VAL   171       3.495   2.127   4.420  1.00  0.00           C                  
                } else if ( Character.isDigit(LChar) || Character.isDigit(RChar) ) {
                    elementId = Defs.NULL_INT;
                } else {
                    elementStr = new String(buf, offset+76, 2);
                    if ( elementStr.equals(Chemistry.ELEMENT_SYMBOL_UPPER_CASE_RIGHT_JUSTIFIED_UNKNOWN_1) ||
                         elementStr.equals(Chemistry.ELEMENT_SYMBOL_UPPER_CASE_RIGHT_JUSTIFIED_UNKNOWN_2)) {
                         elementId = Defs.NULL_INT;
                    } else {
                        // Some PDB files use the wrong justification: "HG11" in LEU iso "1HG1" Catch it here.
                        if ( (atomName.length()==4) && (elementStr.charAt(0)=='H')) {
                            elementId = Chemistry.ELEMENT_ID_HYDROGEN;
                        } else {
                            // If the elementStr can't be found then assume it's unknown. getInt returns                        
                            // Defs.NULL_INT when it can't be found.
                            elementId = Chemistry.ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED_MAP.getInt(elementStr);
                        }
                    }
                    //General.showDebug("elementStr: " + elementStr + " elementId: " + elementId);
                    elementIdPresent = true;
                }
            }            
            // Last chance is to derive it from the author name.
            // Note that this algorithm isn't correct for all cases in PDB, but what is?
            if ( Defs.isNull( elementId ) && (!elementIdPresent)) {
                //General.showDebug("Looking at 4 char atomNamePreFixed: [" + atomNamePreFixed + "]");
                firstChar = atomNamePreFixed.charAt(0);
                if ( firstChar == ' ' || Character.isDigit(firstChar)) {
                    elementStr = " " + atomNamePreFixed.substring(1,2);
                } else {
                    elementStr = atomNamePreFixed.substring(0,2);                    
                }
                if ( elementStr.equals(Chemistry.ELEMENT_SYMBOL_UPPER_CASE_RIGHT_JUSTIFIED_UNKNOWN_1) ||
                     elementStr.equals(Chemistry.ELEMENT_SYMBOL_UPPER_CASE_RIGHT_JUSTIFIED_UNKNOWN_2)) {
                     elementId = Defs.NULL_INT;
                } else {
                    // Some PDB files use the wrong justification: "HG11" in LEU iso "1HG1" Catch it here.
                    if ( (atomName.length()==4) && (elementStr.charAt(0)=='H')) {
                        elementId = Chemistry.ELEMENT_ID_HYDROGEN;
                    } else {
                        // If the elementStr can't be found then assume it's unknown.
                        elementId = Chemistry.ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED_MAP.getInt(elementStr);
                    }
                }
            }
            currentAtomId = atomMain.getNextReservedRow(currentAtomId);
            if ( currentAtomId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {                            
                // Should be very rare case as the sizes are guessed before hand.
                currentAtomId = atomMain.getNextReservedRow(0); // now it should be fine.
                if ( ! atom.resetConvenienceVariables()) {
                    General.showError("Coulnd't resetConvenienceVariables for atom");
                    return false;                    
                }
            }                            
            if ( currentAtomId < 0 ) {
                General.showCodeBug("Failed to add next atom.");
                return false;
            }            
            
            atom.resId[              currentAtomId ] = currentResId;                           // Atom attributes. (fkcs)
            atom.molId[              currentAtomId ] = gumbo.res.molId[     currentResId ];
            atom.modelId[            currentAtomId ] = gumbo.res.modelId[   currentResId ];
            atom.entryId[            currentAtomId ] = gumbo.res.entryId[   currentResId ];

            atom.elementId[     currentAtomId ] = elementId;                                   // Atom attributes. (non-fkcs)
            atom.occupancy[     currentAtomId ] = occupancy;                                     
            atom.bfactor[       currentAtomId ] = bfactor;                                                     
            //atom.charge[        currentAtomId ] = charge;                                                     
            atom.hasCoor.set(   currentAtomId   , hasCoor);

            atom.xList[ currentAtomId ]         = coor_x;
            atom.yList[ currentAtomId ]         = coor_y;
            atom.zList[ currentAtomId ]         = coor_z;
            // Next interns are expensive and might be deleted...
            atom.nameList[           currentAtomId ] = atom.nameListNR.intern( atomName );
            atom.authAtomNameList[   currentAtomId ] = atom.authAtomNameListNR.intern( atomNamePreFixed.trim() );                      
            atom.pdbInsertionCodeList[currentAtomId] = atom.pdbInsertionCodeListNR.intern( insertionCodeResidue );                                  
            atom.authMolNameList[    currentAtomId ] = atom.authMolNameListNR.intern(  authMolName );          
            atom.authResNameList[    currentAtomId ] = atom.authResNameListNR.intern(  authResName );          
            atom.authResIdList[      currentAtomId ] = atom.authResNameListNR.intern(  Integer.toString( authResId ));          
            atom.selected.set( currentAtomId );            
        // Simply catch any error that might be thrown; parse errors include the out
        // of bounds exception java might throw because of buggy code in the simplyfied
        // parser routines. 
        } catch ( Throwable t ) { 
            error_count++;
            // Early abort condition
            if ( error_count > MAX_ATOM_ERRORS_TO_REPORT ) {
                return false;
            }
            General.showError("Failed to parse PDB file line:");
            General.showError( new String(buf, offset, length_line));                
            General.showThrowable(t);
            if ( error_count == MAX_ATOM_ERRORS_TO_REPORT ) {
                General.showError( "Stopping to report more errors over : " + MAX_ATOM_ERRORS_TO_REPORT);
            }            
            return false;
        }        
        return true;
    }
        
    
    /** Create at least the entry and a first model before any molecules, residues and atoms are 
     *added.
     */
    public boolean preprocess( URL url ) {        
                
        if ( ! preprocessForFileCharacteristics(url) ) {
            return false;
        }
        // ENTRY
        String entryName = InOut.getFilenameBase( new File(url.getFile()));
        OrfIdList orfIdList = null;
        String assemblyName = "assembly_" + entryName;
        currentEntryId = entry.add(entryName, orfIdList, assemblyName);
        if ( currentEntryId < 0 ) {
            General.showCodeBug("Failed to add an entry into dbms.");
            return false;
        }
        entry.selected.set(currentEntryId); // Select the entry upon reading.

        // MODEL
        modelNumber = 1;        
        currentModelId = model.add(modelNumber, currentEntryId);
        if ( currentModelId < 0 ) {
            General.showCodeBug("Failed to add model 1 into dbms.");
            return false;
        }
        needToSkipFirstModelRecord = true;
        model.selected.set(currentModelId); // Select the entry upon reading.        
        return true;
    }
    
    
    public boolean preprocessForFileCharacteristics( URL url ) {                
        /** Get file size using a corrected number if compressed */
        int file_size = InOut.getContentLength(url, true); // Wastes a perfect good connection but heck it's java.
//        General.showDebug("Estimating the file size at: " +  file_size + " bytes");
        // -2 signals a regular file without a retrievable size.
        if ( file_size == -2 ) {
            General.showError("Failed to get size from file.");
            return false;
        }
        // Unknown file lenght is a problem
        if ( file_size < 0 ) {
            General.showWarning("Estimating the unknown file size at 10 Mb");
            file_size = 10 * 1024 * 1024;
        }
        int atom_size_estimate = (int) (1.5 * (file_size / PdbFile.LENGTH_MAX_LINE));
//        General.showDebug("The number of atoms in file is most likely below : " + atom_size_estimate);
//        General.showDebug("Entities grow as needed beyond initial capacity.");

        if ( atom == null ) {
            General.showCodeBug("atom not initialized in preprocessForFileCharacteristics");
            return false;
        }
        
        if ( atomMain == null ) {
            General.showCodeBug("atomMain not initialized in preprocessForFileCharacteristics");
            return false;
        }
        
        /** This will automatically and efficiently call a resize if needed */
        atomMain.reserveRows(atom_size_estimate);
        atom.resetConvenienceVariables(); // Very important as columns are likely to have grown.
        General.showDebug("Reading file from url: " + url );                    
        return true;
    }

    
    /** Post process depending on overall status */
    public boolean postProcess( boolean status ) {
        /** Mark the unused atoms for future use. No need to reset to
         *default values because they have not been touched.
         */
//        General.showDebug("Post processing records in pdb file");
        boolean status_1 = atomMain.cancelAllReservedRows();        
        boolean status_2 = resMain.cancelAllReservedRows();        
        boolean status_3 = molMain.cancelAllReservedRows();        
        boolean status_4 = modelMain.cancelAllReservedRows();        
        boolean status_5 = entryMain.cancelAllReservedRows();        

        /** Check the consistency of the data as it's read in.
         */
        boolean status_6 = dbms.foreignKeyConstrSet.checkConsistencySet(false,false);        
//        General.showDebug("Fkcs in dbms check out: " + status_6);
        if ( ! status_6 ) {
            General.showError("DBMS is NOT consistent after post process in reading PDB file.");
        }
        
        boolean status_overall = status && status_1 && status_2 && status_3 && status_4 && status_5 && status_6;

        /** If the overall success status is false then we try
         *to cascadingly remove the entry and it's children*/
        if ( ! status_overall ) {
            General.showWarning("Removing all entities under entry with name: " + entry.nameList[currentEntryId]);
            entryMain.removeRowCascading(currentEntryId,true);
        }
                
        String[] diffRes = res.getDistinctNamesInModel(currentModelId);
        if ( (diffRes == null) || (diffRes.length == 0) ) {
            General.showError("Didn't read any residue?");
            return false;
        }
//        General.showDebug("Unique residue names in this model: " + Strings.toString(diffRes));
        //General.showDebug( dbms.toSTAR());
        return status_overall;
    }
    
    /** Moves a possible leading digit to the back. Can be speedier by using a map.
     */
    public static String mapAtomToPostfixedNumber( String in ) {
        if ( Character.isDigit( in.charAt(0) )) {
            return in.substring(1) + in.charAt(0);            
        }
        if ( in.charAt(0) ==  '\'' ) {
            return in.substring(1) + '\'';            
        }
        return in;        
    }
}
