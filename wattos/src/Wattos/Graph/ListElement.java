package Wattos.Graph;
import java.io.*;

/**
        Instances of this class act as elements of a linked list. It is used for keeping
        <CODE>Vertex</CODE>-instances and <CODE>Edge</CODE>-instances in order. 
	<UL><LI>The collision management strategy of the hash table,</LI>
	<LI>the general vertex list of the graph,</LI>
	<LI>and the edges of a single vertex</LI>
	</UL>are all kept in a linked list of <CODE>ListElement</CODE> instances.
    */
public class ListElement implements Serializable {
 
    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;    
        //-----------
        // Variables
        //-----------

        /**
		A link to the next element in the linked list.
        */
        public ListElement next;

        /**
		A link to the actual object represented by this "hanger" instance.
		Usually an instance <CODE>Vertex</CODE> or <CODE>Edge</CODE>.
        */
        public Object hangingVertexOrEdge;
 
        //--------------
        // Constructor
        //--------------

	/**
		The null constructor gets called when we 
		initialize the hash table. We attach null-
		containing-list elements to each hash index, for 
		ease of coding. (If you want to understand why
		this is important, try to visualize the return
		value of the method <CODE>findVertex</CODE>
		without guarantee that each index in the hash
		table will have at least one 
		<CODE>ListElement</CODE>...)
	*/
	
	public ListElement(){
	  next=null;
	  hangingVertexOrEdge=null;
	}

	/**
		The list element is a general linked list
		element. It contains a link to the next element,
		and a hanger for the vertex or the edge that
		it represents.
		@param objToHang The object that we wish to "hang"
		in this list element, usually an instance of 
		<CODE>Vertex</CODE> or <CODE>Edge</CODE>
	*/

        public ListElement(Object objToHang){
	  next=null;
	  hangingVertexOrEdge=objToHang;
        }

	//------------
	// Methods
	//------------
    }

