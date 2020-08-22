/*
 * StringSet.java
 *
 * Created on March 21, 2003, 4:14 PM
 */

package Wattos.Utils;

import java.io.*;
import java.util.*;

/**
 * Mimics the internal string table addressed by using String.intern()
 *Repeated to have more control over it, e.g. dissect collections of
 *distinct type. E.g. individual stringsets might be used for the
 *list of residue names and atom names.
 *The keys in the map are composite Objects and are of the same size
 *of the large String Objects: 24 bytes on my JVM.
 * @author Jurgen F. Doreleijers
 */
public class StringSet implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;    
    
    /** Weird, the HashSet object works for this application
     *because the elements aren't accessible for a get like: hs.get( "my string");
     *to return a pointer to the string in the HashSet hs but HashMap will.
     *Anyway a HashSet is implemented with a HashMap so we can do without
     *the extra level of indirection.
     */
    public HashMap hm = null;
    
    /** Creates a new instance of StringSet */
    public StringSet( ) {
        init();
    }
    
    public void init( ) {
        hm = new HashMap();
    }
    
    public String intern( String str ) {
        if ( ! hm.containsKey( str ) ) {
            //General.showDebug(" added to internal list: [" + str + "]");
            hm.put( str, str );
        }
        return (String) hm.get( str );
    }
    
    /** Returns a same size string array with elements that are pointers to 
     *unique string objects. It is optimized in order to take advantage of the
     *fact that a string is often the same as the previously listed element.
     *The method will clear the current contents first if needed.
     */
    public String[] intern( String[] stringArray ) {
        String str;
        String previousStrUnique; // caching saves array bound check time.
        
        int stringArraySize = stringArray.length;
        // Assuming we have at least one in the array.
        String[] result = new String[stringArraySize];
        if ( stringArraySize == 0 ) {
            return result;
        }
        
        if ( hm.size() > 0 ) {
            hm.clear();
        }
        // Put the last element first
        str = stringArray[stringArraySize-1];
        previousStrUnique = str;
        hm.put( previousStrUnique, previousStrUnique );
        result[stringArraySize-1]=str;
        //General.showDebug("added to internal list: [" + str + "]");
        
        for (int i=stringArraySize-1;i>0;) {
            i--;
            str = stringArray[i];
            
            // Next check shouldn't be necessary????
            if ( str == null ) {
                previousStrUnique = str;
                hm.put( previousStrUnique, previousStrUnique ); // nulls are allowed
                //General.showDebug("added to internal list: [" + str + "]");
            } else {       
                // Do this check first to speed things up
                if ( str.equals( previousStrUnique ))  {
                    //General.showDebug("found in previous value: [" + str + "]");
                } else {           
                    // Do full search.
                    if ( hm.containsKey( str )) {
                        //General.showDebug(" found in previous set: [" + str + "]");
                        previousStrUnique = (String) hm.get( str );
                    } else {            
                        // It didn't exist in the set yet.
                        previousStrUnique = str;
                        hm.put( previousStrUnique, previousStrUnique );
                        //General.showDebug("added to internal list: [" + str + "]");
                    }
                }
            }
            result[i]=previousStrUnique;
        }
        //General.showDebug("found result: " + Strings.toString( result ));
        return result;
    }
    
    /** Convenience method around the same method with String[] as the argument.*/
    public String[] intern( StringSet set ) {
        return intern( Strings.toStringArray( set.hm.keySet()));
    }
        
    
    /** Reduces the redundancy completely. It's important to have unused rows nilled
     *so they easily reduce.
     */
    public static Object convertColumnString2StringNR(String[] stringArray ) {
        ArrayList object = new ArrayList(2); // Just a small one is enough.
        StringSet ss = new StringSet();        
        String[] newStringArray = ss.intern(stringArray);        
        object.add( newStringArray );
        object.add( ss );
        return object;        
    }

    /** Returns the keys in sorted order */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("The keys are shown in sorted order although they are not stored in that way\n");        
        Set keys = hm.keySet();
        Object[] resultObjectList = keys.toArray();
        Arrays.sort( resultObjectList );
        String[] result = Strings.toStringArray( resultObjectList );          
        for (int i=0;i<result.length;i++)  {
            sb.append(result[i]);        
            sb.append(General.eol);        
        }
        return sb.toString();                
    }
    
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if ( true ) {
            StringSet ss = new StringSet();
//            int i = 100;
            String str = "initial value";
            while ( ! str.equals( "" )) {
                str = Strings.getInputString( "Input: ");
                General.showOutput("Read: " + str + " which is a string with hashCode: " + str.hashCode() );
                String new_str = ss.intern( str );
                General.showOutput("Now:  " + new_str + " which is a string with hashCode: " + new_str.hashCode() );
            }
        }
    }  

    /** Convenience method */
    public String[] getStringArray() {
        return Strings.toStringArray( hm.keySet() );
    }

    /** Very fast routine intended */;
    public boolean containsQuotedValue() {
        Set keys = hm.keySet();
        Object[] result = keys.toArray();
        String v = null;
        char c = 0;
        for (int i=0;i<result.length;i++)  {
            v = (String) result[i];
            if ( v == null ) {
                continue;
            }
            if ( v.length() == 0 ) {
                continue;
            }
            c = v.charAt(0);
            if ( (c == '"') || (c == '\'') ) {
                return true;
            }
        }
        return false;
    }
}
