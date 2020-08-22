/*
 * Objects.java
 *
 * Created on February 11, 2003, 3:58 PM
 */

package Wattos.Utils;

import java.io.*;
import Wattos.Database.*;
import Wattos.Soup.*;
/**
 *Allows to take deep copies of objects. That's all.
 * @author Jurgen F. Doreleijers
 */
public class Objects {

    /** Creates a new instance of Objects */
    public Objects() {
    }

    /** Make a deep copy the io intensive way for cases where pointers or so might get messed up.
     */
    public static Object deepCopy( Object object ) {
        Object copy = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream( baos );
            oos.writeObject( object );
            oos.close();
            byte[] byte_array = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byte_array);
            ObjectInputStream ois = new ObjectInputStream( bais );
            copy = ois.readObject();
            ois.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return null;
        }
        return copy;
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        if ( false ) {
//            Table t = new Table(2,3);
//            Table s = (Table) deepCopy(t);
//            s.setValueByColumn(1,"test");
//            General.showOutput("Table t: " + t);
//            General.showOutput("Table s: " + s);
//        }
        if ( true ) {
            DBMS dbms = new DBMS();
            new Gumbo(dbms);
            DBMS dbmsTemp = (DBMS) deepCopy(dbms);
            General.showDebug( dbmsTemp.toString());
        }

    }

}
