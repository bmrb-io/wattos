package Wattos.Utils;
/**
 * Used to create sorted lists maintaining the original order like a recorded id.
 * @author Jurgen F. Doreleijers
 */
public class ObjectIntPair {
    
    public Object o;
    public int    i;
    
    /** Creates a new instance of ObjectIntPair */
    public ObjectIntPair(Object firstObject,  int    intObject ) {
         o =  firstObject;
         i =  intObject;
    }
}
