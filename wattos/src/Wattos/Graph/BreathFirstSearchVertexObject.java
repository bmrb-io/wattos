/*
 * BreathFirstSearchVertexObject.java
 *
 * Created on July 24, 2003, 9:28 AM
 */

package Wattos.Graph;
import java.io.*;

/**
 *A point in the graph.
 *@see Wattos.Graph
 * @author Jurgen F. Doreleijers
 */
public class BreathFirstSearchVertexObject implements Comparable, Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;    
    
    /* Colors are easy to think of. White means undiscovered in
     *breath-first search.
     */
    public static final int VERTEX_COLOR_TYPE_WHITE    = 0;
    public static final int VERTEX_COLOR_TYPE_GRAY     = 1;
    public static final int VERTEX_COLOR_TYPE_BLACK    = 2;
    
    public static final String[] vertices_color_list = { "White", "Gray", "Black" };
    
    public static final int VERTEX_DISTANCE_INFINITE   = Integer.MAX_VALUE;
    public static final Vertex VERTEX_PARENT_NULL      = null;
    
    public static final int VERTEX_COLOR_TYPE_DEFAULT  = VERTEX_COLOR_TYPE_WHITE;    
    public static final int VERTEX_DISTANCE_DEFAULT    = VERTEX_DISTANCE_INFINITE;
    public static final Vertex VERTEX_PARENT_DEFAULT   = VERTEX_PARENT_NULL;

    public int color;
    public int distance;
    public Vertex parent;
    public Vertex myVertex;

    /** Creates a new instance of BreathFirstSearchVertexObject.
     * Color is white to reflect undiscovered state
     * distance is set to infinite as in undiscovered
     * parent is set to null as in none.
     */
    public BreathFirstSearchVertexObject( Vertex me ) {
        color       =VERTEX_COLOR_TYPE_DEFAULT;
        distance    =VERTEX_DISTANCE_DEFAULT;        
        parent      =VERTEX_PARENT_DEFAULT;
        myVertex    = me;
    }        
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(vertices_color_list[color]);
        sb.append(", ");
        if ( distance == VERTEX_DISTANCE_INFINITE ) {
            sb.append("Infinite");
        } else {
            sb.append(distance);
        }
        sb.append(", ");
        if ( parent == VERTEX_PARENT_NULL ) {
            sb.append("NIL");
        } else {
            sb.append(parent);
        }
        return sb.toString();
    }
    
    
    /** Natural order of this object is defined on the basis of the distance.
     *That should work even if it's set to VERTEX_DISTANCE_INFINITE.
     */
    public int compareTo( Object other ) {
        BreathFirstSearchVertexObject o = (BreathFirstSearchVertexObject) other;
        if (distance < o.distance ) {
            return -1;
        } else if (distance > o.distance ) {
            return 1;
        }
        return 0;
    }
}
