package Wattos.Utils;

import java.util.*;

/**
 * ArrayList of ArrayList construction. API needs to be completed with e.g. a put ;-)
 *
 * @author Jurgen F. Doreleijers
 */
public class ListOfLists extends ArrayList {
    private static final long serialVersionUID = 6841186534958697646L;

    public Object get( int i, int j ) {
        if ( i < 0 || i >= size() ) {
            General.showDebug("Invalid first index on LoL: " + i + " size is: " + size());
            return null;
        }
        ArrayList l_2 = (ArrayList) get(i);
        if ( j < 0 || j >= l_2.size() ) {
            General.showDebug("Invalid first index on LoL: " + j + " size is: " + l_2.size());
            return null;
        }                
        return l_2.get(j);
    }
}
