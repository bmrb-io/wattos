package Wattos.Graph;

import java.io.*;

/**
        Instances this class act as wrappers for the real vertex object, which
        can be for example java.lang.Integer, or any object. Instances this
        class always "hang" from a <CODE>JbListElement</CODE> instance.
    */
    
public class Vertex implements Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;    
        
        //-----------
        // Variables
        //-----------
	/**
		The number edges this vertex has coming in
		from another vertex.
	*/
        private int inEdges;

        /**
                The number edges this vertex has going out
                to another vertex. 
        */
        private int outEdges;

        /**
                A link to the beginning a linked list structure
		the edges that leave this vertex.
        */
        public ListElement firstEdge;

        /**
                A link to the actual abject, which can be any java.lang.Object,
		which this vertex instance "wraps".
        */
        public Object vertexObject;

        /**A link to an object whose use is especially for internal routines; however
         *the results may be used outside this package. An example object
         *is the BreathFirstSearchVertexObject in this package.
         */
        public Object internalVertexObject;

        /**
                A marker variable used for cycle detection, topological sorting
		and similar.
        */
	public int marker;
 
        //-------------
        // Constructor
        //-------------

        /**
                A wrapper class for the actual java.lang.Object to be
		situated in the graph.
		@param objToEmbed The java.lang.Object that we wish to have wrapped.
        */
        public Vertex(Object objToEmbed){
            init(null, objToEmbed);
	}

        Vertex(Object internalObject, Object objToEmbed){
            init(internalObject, objToEmbed);
	}

        public boolean init(Object internalObject, Object objToEmbed) {
	  // Initialize all vars..
          inEdges=0;
	  outEdges=0;
	  marker=0;
	  firstEdge=null;
	  vertexObject=objToEmbed;
          internalVertexObject=internalObject;
          return true;
        }
        //------------
        // Methods 
        //------------

        /**
		Returns the number of edges coming into this vertex.
		@return The in-degree of this vertex.
        */
	public int whatIsInDegree(){
		return this.inEdges;
	}

        /**
		Returns the number of edges leaving this vertex.
		@return The out-degree of this vertex.
        */
        public int whatIsOutDegree(){ 
                return this.outEdges;
        }

        /**
		Accessor method to increment in-degree by one.
        */
	public void addInDegree(){
		this.inEdges++;
	}

        /**
		Accessor method to increment out-degree by one.
        */
	public void addOutDegree(){
		this.outEdges++;
	}
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(vertexObject);
            if ( internalVertexObject != null ) {
                sb.append(", ");
                sb.append(internalVertexObject);
            }
            return sb.toString();
        }
    }
