package Wattos.Gobbler;

import java.util.*;
import Wattos.Utils.*;
import Wattos.Common.*;

/**
 *
 * @author Jurgen F. Doreleijers
 */
public class ComparatorBlastMatchTargetDB implements Comparator {    
    
        /** From http://targetdb.rutgers.edu/target.dtd:
Cloned
Crystal Structure
Crystallized
Diffraction
Diffraction-quality Crystals
Expressed
HSQC
In PDB
NMR Assigned
NMR Structure
Other
Purified
Selected
Soluble
Test Target
Work Stopped
         *
         * from my own code:
         *bio@data/ pwd
         * /nfs/crane/scratch/BLAST-BMRB/bmrb/weekly.sg/data
         *bio@data/ grep ">" sg_db | gawk -F'|' '{print $3}' | sort -u | gawk -F';' '{for (i=1;i<=NF;i++) print $(i)}' | sort -u
Cloned
Crystal_Structure
Crystallized
Diffraction
Diffraction-quality_Crystals
Diffraction-quality_crystals
Expressed
HSQC
In_PDB
In_crystallization
NMR_Assigned
NMR_Structure
Native_diffraction-data
Phasing_diffraction-data
Purified
Selected
Soluble
Test_Target
Work_Stopped
Work_stopped
         */         

    /** Ranked according to furthest state for those at HSQC/Crystallized or beyond         
 */        
    private static final String[] o = new String[] {      
        "in_pdb",
        "nmr_structure",
        "crystal_structure",
        "nmr_assigned",
        "native_diffraction-data",
        "phasing_diffraction-data",
        "diffraction",
        "diffraction-quality_crystals",
        "crystallized",
        "hsqc",
// from here on they're just listed for completeness so errors can be flagged.
        "in_crystallization", // bad tag but occurs.
"purified",
"soluble", 
"expressed",
"cloned",
"test_target",
"selected",
"work_stopped"
};        
    private static final String[] k = new String[] {      
        "in_pdb",
        "nmr_structure",
        "crystal_structure",
        "nmr_assigned",
        "native_diffraction-data",
        "phasing_diffraction-data",
        "diffraction",
        "diffraction-quality_crystals"};
    public static StringArrayList order = null;
    public static StringArrayList killers = null;
    static {
        order = new StringArrayList(Arrays.asList( o ));
        killers = new StringArrayList(Arrays.asList( k ));
    }
    public ComparatorBlastMatchTargetDB() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {       
        //General.showDebug("doing compare");
        BlastMatch oip1= (BlastMatch) o1;        
        BlastMatch oip2= (BlastMatch) o2;
        
        OrfId oi1 = (OrfId) oip1.subject_orf_id_list.orfIdList.get(0);
        OrfId oi2 = (OrfId) oip2.subject_orf_id_list.orfIdList.get(0);
        
        String workDoneValue_1 = oi1.orf_db_subid.toLowerCase(); 
        String workDoneValue_2 = oi2.orf_db_subid.toLowerCase(); 
        boolean workStopped_1 = (workDoneValue_1.indexOf( "work_stopped" )>-1);
        boolean workStopped_2 = (workDoneValue_2.indexOf( "work_stopped" )>-1);

        if ( workStopped_1 && ! workStopped_2 ) {
            return 1;
        }
        if ( ! workStopped_1 && workStopped_2 ) {
            return -1;
        }
        int furthestPosition_1 = getFurthestPosition(workDoneValue_1);
        int furthestPosition_2 = getFurthestPosition(workDoneValue_2);

        
        if ( furthestPosition_1 < furthestPosition_2 ) {
            return -1;
        }
        if ( furthestPosition_1 > furthestPosition_2 ) {
            return 1;
        }      
        // Else use the expectation values still.
        if ( oip1.expectation_value < oip2.expectation_value) {
            return -1;
        }
        
        return 0;
    }  
    
    
    /** Don't trust that the string with the furthest position is at the end
     */
    public int getFurthestPosition(String workDoneValue) {
        StringTokenizer tokens = new StringTokenizer( workDoneValue, 
            ";",
            false); // do not return delims
        int furthestPosition = o.length; // indication it's not present.
        while ( tokens.hasMoreTokens() ) {
            String token = tokens.nextToken();
            int position = order.indexOf( token );
            if ( position < 0 ) {
                General.showWarning("Work done value item is new: " + token + " Assuming it is very early in pipeline.");
                position = o.length;
            }
            //General.showDebug("Work done value item is: " + token + " Found at pipeline position: " + position);
            if ( position < furthestPosition ) {
                furthestPosition = position;
            }
        }
        
        return furthestPosition;
    }
}
