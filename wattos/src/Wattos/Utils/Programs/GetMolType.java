package Wattos.Utils.Programs;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import Wattos.Database.DBMS;
import Wattos.Star.SaveFrame;
import Wattos.Star.StarFileReader;
import Wattos.Star.StarNode;
import Wattos.Star.TagTable;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.ObjectIntMap;
import Wattos.Utils.StringArrayList;
import Wattos.Utils.Strings;

public class GetMolType {
    
    static String urlDB = "http://tang.bmrb.wisc.edu/servlet_data/viavia/mr_mysql_backup/";        
//    static File linkDir = new File( "M:\\jurgen\\DOCR_big_tmp_\\link" );
    static File linkDir = new File( "/big/jurgen/DOCR_big_tmp_/link" );
        
    
    public static ArrayList getBmrbNmrGridEntries() {
        ArrayList result = new ArrayList();
        String urlLocation = urlDB+"/entry.txt";
//      4583    \N    108d    \N    \N
//      4584    \N    149d    \N    \N
        URL url = null;
        try {
            url = new URL(urlLocation);
        } catch (MalformedURLException e) {
           return null;
        }
        String r1 = InOut.readTextFromUrl(url);
        String[] dataLines = Strings.splitWithAllReturned(r1, '\n');
        for (int i=0;i<dataLines.length;i++) {
//        for (int i=0;i<2;i++) {
            String[] t = Strings.splitWithAllReturned(dataLines[i], '\t');
            if ( t.length < 5 ) {
                General.showDebug("Skipping: " + Strings.toString(t));
                continue;
            }
            result.add( t[2] );    
        }
        return result;
    }
    
    public static void run() {
        ArrayList pdbList = getBmrbNmrGridEntries();
        General.showOutput( "Read pdb entries from NMR Restraints Grid:"+ pdbList.size() );
//        pdbList = new ArrayList();
//        pdbList.add("6gat"); // has 2 dna, 1 protein, and 1 other.
        HashMap molTypes = new HashMap();
        for (int i=0;i<pdbList.size();i++) {
            String entry = (String) pdbList.get(i);
            File d = new File( linkDir,entry );
            File f = new File( d,entry+"_full.str");
            if ( ! f.exists()) {
                General.showWarning( "Skipping file: " + f);
                continue;
            }
            URL url = null;
            try {
                url = f.toURL();
            } catch (MalformedURLException e) {
               return;
            }
            DBMS dbms = new DBMS();  
            StarFileReader sfr = new StarFileReader(dbms); 
            StarNode sn = sfr.parse( url );
            if ( sn == null ) {
                General.showError("parse unsuccessful.");
                return;
            }               
            ObjectIntMap molTypesPerEntry = new ObjectIntMap();
            molTypes.put(entry,molTypesPerEntry);        
            ArrayList sfList = sn.getSaveFrameListByCategory("entity");
            for (int j=0;j<sfList.size();j++) {
                SaveFrame sf = (SaveFrame) sfList.get(j);
                TagTable tT = sf.getTagTable("_Entity.Type", true);
                String type = tT.getValueString(0, "_Entity.Type");
                String poltype = "";
                if ( tT.containsColumn("_Entity.Pol_type")) {                    
                    poltype = tT.getValueString(0, "_Entity.Pol_type");
                }
                    
//                General.showDebug("type "+ type+ ", and poltype "+ poltype);
                String key = type +"/" + poltype;
                molTypesPerEntry.increment(key);
            }
            StringArrayList set = new StringArrayList( molTypesPerEntry.keySet());
            for (int j=0;j<set.size();j++) {
                String key = set.getString(j);
                General.showOutput(entry+","+key+","+molTypesPerEntry.getValueInt(key));
            }
        } // end loop over entries.
        
        General.showOutput(Strings.toString(molTypes));
        StringBuffer sb = new StringBuffer();
        StringArrayList set1 = new StringArrayList( molTypes.keySet());
        for (int j=0;j<set1.size();j++) {
            String entry = set1.getString(j);
            ObjectIntMap molTypesPerEntry = (ObjectIntMap) molTypes.get(entry);
            StringArrayList seti = new StringArrayList( molTypesPerEntry.keySet());
            for (int i=0;i<seti.size();i++) {
                String key = seti.getString(i);
                sb.append(entry+","+key+","+molTypesPerEntry.getValueInt(key)+"\n");
            }
        }
        InOut.writeTextToFile(new File("out"),sb.toString(),true,false);
    }
    
    public static void main(String[] args) {
        General.setVerbosityToDebug();
        run();
    }       
}
