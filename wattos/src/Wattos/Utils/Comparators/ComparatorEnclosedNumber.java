/*
 * Sorts on the first integer present in the names.
 *E.g. puts bmr4.str, bmr10.str in the right order.
 *
 * Created on July 31, 2003, 9:42 AM
 */

package Wattos.Utils.Comparators;

import Wattos.Utils.*;

import java.util.Comparator;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author Jurgen F. Doreleijers
 */
public class ComparatorEnclosedNumber implements Comparator {
    
    static private final String regexp_integer_number = ".*?(\\d+).*?"; 
    static private final Pattern p_integer_number;            
    static private       Matcher m_integer_number;
    
    static {
        p_integer_number = Pattern.compile(regexp_integer_number);
        m_integer_number = p_integer_number.matcher( "");
    }
    
    /** Creates a new instance of IntIntPairComparator */
    public ComparatorEnclosedNumber() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {       
        //General.showOutput("doing compare between: " + o1 + " and: " + o2);
        String oip1= (String) o1;        
        String oip2= (String) o2;        
        String integer_1 = "";
        String integer_2 = "";
        boolean matches;
        
        m_integer_number.reset( oip1 );
        matches = m_integer_number.matches();
        if ( ! matches ) {
            return oip1.compareTo(oip2);
        }
        integer_1 = m_integer_number.group(1);

        m_integer_number.reset( oip2 );
        matches = m_integer_number.matches();
        if ( ! matches ) {
            return oip1.compareTo(oip2);
        }
        integer_2 = m_integer_number.group(1);
        int int_1 = Integer.parseInt( integer_1 );
        int int_2 = Integer.parseInt( integer_2 );
        //General.showOutput("Doing compare between: " + int_1 + " and: " + int_2);
        if ( int_1 == int_2 ) {
            return 0;
        }
        if (int_1 < int_2  ) {
            return -1;
        }
        return 1;
    }          
    
    public static void main(String[] args) {
        String f2 = "bmr4.str";
        String f1 = "bmr10.str";
        String f3 = "pdb108d.ent";
        String[] list = new String[] { f1, f2, f3 };
        ComparatorEnclosedNumber c = new ComparatorEnclosedNumber();
        General.showOutput("Strings can be sorted from: " + Strings.toString( list ));
        Arrays.sort(list,c);
        General.showOutput("Strings can be sorted to: " + Strings.toString( list ));
    }
}
 
