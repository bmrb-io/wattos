/*
 * FastResultList.java
 * Created on January 26, 2005, 11:18 AM
 */

package Wattos.Common;

import Wattos.Utils.*;
import Wattos.Soup.Biochemistry;

import java.io.*;
import java.util.*;

/**
 * A sequence list file object.
 * @see  <a href="FastaSpecs.html"Data on a sequence from a fasta file following the definitions</a>
 * @author Jurgen F. Doreleijers
 */
public class FastaList extends ArrayList {    
    private static final long serialVersionUID = 2256730768930193896L;

    /** Creates a new instance of FastResult */
    public FastaList() {
        super();
    }
        

    /**
     * Read the file into memory after init.
     * @param input_filename
     * 
     */
    public boolean readFastaFile( String input_filename ) {

        Orf orf = new Orf();         
        int count_read = 0;
        
        try {
            FileReader fr = new FileReader( input_filename );
            LineNumberReader inputReader = new LineNumberReader( fr );
            if ( inputReader == null ) {
                General.showError("initializing LineNumberReader");
                return false;
            }
            if ( ! inputReader.ready() ) {
                General.showDebug("input not ready or just empty..");
                return false; 
            }
            
            String line = inputReader.readLine();
            while ( line != null ) {
                //General.showDebug("Processing line: " + (inputReader.getLineNumber()-1));            
                /**Does the line start a second new query output? */                
                if ( ( line.length() > 0 ) && 
                     ( line.charAt(0) == FastaDefinitions.FASTA_START_CHAR )) {
                    if ( inputReader.getLineNumber() != 1 ) {
                        // First line doesn't need a save of previous orf.
                        orf.sequence = orf.sequence.replaceAll("\\s",""); // do here for efficiency
                        add(orf);
                        orf = new Orf();
                    }
                    boolean status = orf.orf_id_list.readFasta( line ); // Read header.
                    if ( ! status ) {
                        General.showError("Failed to orf.orf_id_list.readFasta( line from line:");
                        General.showError(line);
                        General.showError("Not parsing file");
                        return false;
                    }
                    count_read++;
                } else {
                    orf.sequence = orf.sequence + line;
                }
                line = inputReader.readLine();
            }
            // Handle last sequence in file.
            orf.sequence = orf.sequence.replaceAll("\\s",""); // do here for efficiency
            add(orf);
            orf = new Orf();
            inputReader.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }      
        General.showOutput("Read sequences                            : " + count_read);
        return true;        
    }
    
    
    
    public boolean filterDuplicatesBetweenFastaFiles( FastaList other ) {
        HashMap otherMap = other.createHashMap(true,0);
//        int indexOther = -1;
        for (int i=0;i<size();i++) {
            //General.showDebug("Doing orf with id: " + i + " size: " + size());
            Orf orf = (Orf) get(i);   
            OrfId orf_id = (OrfId) orf.orf_id_list.orfIdList.get(0);
            String orf_db_id = orf_id.orf_db_id;
            
            Object o = otherMap.get( orf_db_id );
            // Is the id in the other fasta list?
            if ( o == null ) {
                //General.showDebug("Removing orf with id: " + i);
                remove(i);
                i--;
                continue;
            }
        }
        return true;
    }

    
    public boolean removeEmptySequence( ) {
        for (int i=0;i<size();i++) {
            //General.showDebug("Doing orf with id: " + i + " size: " + size());
            Orf orf = (Orf) get(i);   
            // Is the id in the other fasta list?
            if ( orf.sequence.equals("")) {
                General.showWarning("Removing empty sequence from orf with id: " + orf.orf_id_list.toFasta());
                remove(i);
                i--;
                continue;
            }
        }
        return true;
    }

    /** See: Biochemistry class.
     */    
    public boolean translateSequence(boolean fromDNAtoProtein ) {
        String newSequence = null;
        for (int i=0;i<size();i++) {
            //General.showDebug("Processing fast result number: " + 1);            
            /**Does the line start a second new query output? */   
            Orf orf = (Orf) get(i);
            newSequence = Biochemistry.translateSequence(orf.sequence, fromDNAtoProtein);
            if ( newSequence == null ) {
                return false; 
            }
            orf.sequence = newSequence;
        }      
        return true;        
    }

    
    /**
     *
     * @param output_filename
     */    
    public boolean writeFastaFile( String output_filename ) {
        int count_written=0;
        try {
            BufferedWriter bw = new BufferedWriter( new FileWriter( output_filename));
            PrintWriter outputWriter = new PrintWriter( bw );
            if ( outputWriter == null ) {
                General.showError("initializing PrintWriter");
                return false;
            }
            if ( outputWriter.checkError() ) {
                General.showError("output got an error before starting.");
                return false; 
            }

            for (int i=0;i<size();i++) {
                //General.showDebug("Processing fast result number: " + 1);            
                /**Does the line start a second new query output? */   
                Orf orf = (Orf) get(i);
                outputWriter.write( orf.toFasta() );
                count_written++;
            }
            outputWriter.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }      
        General.showOutput("Wrote sequences                           : " + count_written);
        return true;        
    }

    public boolean replaceOrfIdListFrom( FastaList other ) {
        HashMap otherMap = other.createHashMapOnSequence();
        int indexOther = -1;
        for (int i=0;i<size();i++) {
            //General.showDebug("Doing orf with id: " + i);
            Orf orf = (Orf) get(i);            
            Object o = otherMap.get( orf.sequence );
            // Is the sequence in the other fasta list?
            if ( o == null ) {
                //General.showDebug("Removing orf with id: " + i);
                remove(i);
                i--;
                continue;
            }
            indexOther = ((Integer)o).intValue();
            Orf orfOther = (Orf) other.get(indexOther);
            orf.orf_id_list = orfOther.orf_id_list;
        }
        return false;
    }
    
    public FastResultList combineOnSequenceToFastResultList( FastaList other ) {
        HashMap otherMap = other.createHashMapOnSequence();
        FastResultList frl = new FastResultList();
        int indexOther = -1;
        for (int i=0;i<size();i++) {
            //General.showDebug("Doing orf with id: " + i);
            Orf orf = (Orf) get(i);            
            Object o = otherMap.get( orf.sequence );
            // Is the sequence in the other fasta list?
            if ( o == null ) {
                //General.showDebug("Removing orf with id: " + i);
                remove(i);
                i--;
                continue;
            }
            indexOther = ((Integer)o).intValue();
            Orf orfOther = (Orf) other.get(indexOther);
            FastResult fr = new FastResult();
            fr.oiList.orfIdList.add(      orf.orf_id_list.orfIdList.get(0));
            fr.oiList.orfIdList.add( orfOther.orf_id_list.orfIdList.get(0));
            frl.add( fr );
        }
        return frl;
    }
    
    
    public boolean removePartOrfId( String part ) {
        ArrayList partList = new ArrayList( Arrays.asList( OrfId.parts ));
        int idx = partList.indexOf( part );
        if ( idx < 0 ) {
            General.showError("Given part isn't in: " + Strings.toString( OrfId.parts ));
            return false;
        }
        // Duplicated the code for speed or can the compiler figure it out?
        switch ( idx ) {
            case 0: {
                for (int i=0;i<size();i++) {
                    Orf orf = (Orf) get(i);            
//                    FastResult fr = new FastResult();
                    OrfId orfId = (OrfId) orf.orf_id_list.orfIdList.get(0);
                    orfId.orf_db_name = "";
                }
                break;
            }
            case 1: {
                for (int i=0;i<size();i++) {
                    Orf orf = (Orf) get(i);            
//                    FastResult fr = new FastResult();
                    OrfId orfId = (OrfId) orf.orf_id_list.orfIdList.get(0);
                    orfId.orf_db_id = "";
                }
                break;
            }
            case 2: {
                for (int i=0;i<size();i++) {
                    Orf orf = (Orf) get(i);            
//                    FastResult fr = new FastResult();
                    OrfId orfId = (OrfId) orf.orf_id_list.orfIdList.get(0);
                    orfId.orf_db_subid = "";
                }
                break;
            }
            case 3: {
                for (int i=0;i<size();i++) {
                    Orf orf = (Orf) get(i);            
//                    FastResult fr = new FastResult();
                    OrfId orfId = (OrfId) orf.orf_id_list.orfIdList.get(0);
                    orfId.molecule_name = "";
                }
                break;
            }
        }
        return true;
    }
    
    /** Empty elements will not be mapped */
    private HashMap createHashMapOnSequence() {
        int size = size();    
        HashMap hm = new HashMap( size() );
        for (int i=0;i<size;i++) {
            Orf orf = (Orf) get(i);            
            if ( !(( orf.sequence == null ) || orf.sequence.equals(""))) {
                hm.put( orf.sequence, new Integer(i));
                
            }
        }
        return hm;
    }   
    
    /** Empty elements will not be mapped. 
     *If useId is true the orf_db_id's will be hashed otherwise
     *the orf_db_subid's will be hashed.
     *Make sure that if orf_id_index is set to non-zero that
     *there are non-zero elements in the orf_id_list!
     */
    public HashMap createHashMap( boolean useId, int orf_id_index ) {
        HashMap hm = new HashMap( size() );
        int size = size();    
        for (int i=0;i<size;i++) {
            Orf orf = (Orf) get(i);
            if ( orf_id_index >= orf.orf_id_list.orfIdList.size() ) {
                General.showError("orf_id_index used                    : " + orf_id_index);
                General.showError("but orf.orf_id_list.orfIdList.size() : " + orf.orf_id_list.orfIdList.size());
                return null;
            }
            OrfId oi = (OrfId) orf.orf_id_list.orfIdList.get(orf_id_index);
            String id = null;
            if ( useId ) {
                id = oi.orf_db_id;
            } else {
                id = oi.orf_db_subid;
            }            
            if ( !(( id == null ) || id.equals(""))) {
                hm.put( id, new Integer(i));
            }
        }
        return hm;
    }
    
    
}
