package Wattos.Graph;
import java.io.*;


/**
        Instances of this class act as edges in the graph. Instances of this
        class always "hang" from a <CODE>ListElement</CODE> instance. They
	have a <CODE>int weight</CODE> and a link to the vertex object to 
	which they lead. Null target vertices are not supported.
    */
public class Edge implements Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;    
        
        //-----------
        // Variables
        //-----------

	/**
		The weight of this edge.
	*/
        private int weight;

        /** Label the edge */
        private String label;
        
        /**
                The vertex where the edge leads to.
        */
        public Vertex targetVertex;
 
        //-------------
        // Constructor
        //-------------

        /**
                An edge object leading from one vertex to another.
		Edges are unique, there can only be one edge
		from vertex A to vertex B. The weight cannot be changed
		after the object is constructed.
		@param target The <CODE>Vertex</CODE> instance where
		this edge leads to.
		@param weightValue The integer weight of this edge
        */
        public Edge(Vertex target, int weightValue, String label){
          this.weight=weightValue;
          this.label=label;
          targetVertex=target; 
        }

        //------------
        // Methods 
        //------------
	
	/**
		Return the weight of the edge.
	*/
	
	public int whatIsWeight(){
		return this.weight;
	}
        
        public String getLabel() {
            return label;
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(targetVertex.vertexObject + ", " + weight + ", " + label );
            return sb.toString();
        }
    }

