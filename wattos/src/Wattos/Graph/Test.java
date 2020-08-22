package Wattos.Graph;

import java.math.BigInteger;
import Wattos.Utils.*;
import java.io.*;

/**
 * A driver class to test the DirectedGraph class.
 * No CMD-line params, all constants are hard coded.
 * For your convenience. ;)
 */
public class Test implements Serializable {
    
        /*
                 GLOBAL VARS
         */
    /** Faking this variable makes the serializing not worry
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;
    // Buffered reader made static because it doesn't need to be (can't) be serialized.
    static BufferedReader in;
    
    
    ListElement graphList;
    ListElement lastGraph;
    ListElement vertexList;
    String      location_bin;
    int         graphCount;
    int         vertexCount;
    boolean     moo;
    String      gp;
    
    static {
        in = new BufferedReader(new InputStreamReader(System.in));
    }
    
    
    public Test(){
        init();
    }
    
    public void init() {
        location_bin = "C:\\Temp\\java\\examples\\graph.bin";
        graphCount = 0;
        vertexCount = 0;
        gp = "Enter a CMD:"+
        "\n\t\tq    =Quit"+
        "\n\t\tg    =New graph"+
        "\n\t\tv    =New vertex"+
        "\n\t\te    =New edge"+
        "\n\t\tc    =Inspect vertex"+
        "\n\t\t?    =Inspect graph"+
        "\n\t\tt    =Topological sort"+
        "\n\t\tt    =Test equality"+
        "\n\t\ts    =Serialize"+
        "\n\t\td    =Deserialize"+
        "\n\t\tb    =Do Breath First Search"+
        "\n\t\t1    =Fill system with graph example 1 random"+
        "\n\t\t2    =Fill system with graph example 2 bfs"+
        "\n\t\t3    =Fill system with graph example 3 cyclic"+
        General.eol;
    }
    
    
    /** Overwrite so the buffered reader doesn't get serialized. Upon
     *deserialization the reader just gets reinitialized
     *
     * private void writeObject( ObjectOutputStream out ) throws IOException {
     * }
     */
    
    public void exit(){
        General.showOutput("Done with driving graph module");
        System.exit(0);
    }
    
    public void createGraph(){
        DirectedGraph g = new DirectedGraph(11);
        ListElement hanger = new ListElement(g);
        if(graphList==null){
            graphList=hanger;
            lastGraph=hanger;
        }else{
            lastGraph.next=hanger;
            lastGraph=hanger;
        }
        graphCount++;
        General.showOutput("Created a new empty graph. We now have "+
        graphCount+" graphs total.");
    }
    
    public void createVertex(){
//        char c=' ';
        int n=1;
        ListElement target = graphList;
        if(graphCount>1){
            n=Strings.getInputInt(in, "Which graph? ("+graphCount+" total): ", null);
            for(int i=1;i<n;i++){
                target=target.next;
            }
        }
        if(graphCount==0){
            General.showOutput("No graphs created. Press 'g' to create one.");
            return;
        }
        DirectedGraph g =
        (DirectedGraph)target.hangingVertexOrEdge;
        BigInteger b =
        new BigInteger(Strings.getInputString(in, "Enter BigInteger value: ", null));
        boolean result = g.addVertex(b);
        if(result==false) return;
        ListElement hanger = new ListElement(b);
        hanger.next=vertexList;
        vertexList=hanger;
        vertexCount++;
        General.showOutput("Created a new vertex in graph "+n+". We now have "+
        vertexCount+" total vertices in all graphs.");
    }
    
    public void createEdge(){
        boolean b=false;
        int n=1;
        int k;
        ListElement target = graphList;
        if(graphCount>1){
            n=Strings.getInputInt(in, "Which graph? ("+graphCount+" total): ", null);
            for(int i=1;i<n;i++){
                target=target.next;
            }
        }
        if(graphCount==0){
            General.showOutput("No graphs created. Press 'g' to create one.");
            return;
        }
        DirectedGraph g =
        (DirectedGraph)target.hangingVertexOrEdge;
        
        //selecting vertex
        target=vertexList;
        do{
            if(g.hasVertex((BigInteger)target.hangingVertexOrEdge)){
                General.showOutputNoEol("Graph "+n+" has a vertex "+
                target.hangingVertexOrEdge+"  ");
                b=Strings.getInputBoolean(in,"Choose as source? ");
            }
            if(!b)target=target.next;
        }while((target!=null)&&(!b));
        BigInteger sourceV=(BigInteger)target.hangingVertexOrEdge;
        General.showOutput(sourceV+" : "+g.hasVertex(sourceV));
        //selecting vertex
        target=vertexList;
        do{
            if(g.hasVertex((BigInteger)target.hangingVertexOrEdge)){
                General.showOutputNoEol("Graph "+n+" has a vertex "+
                target.hangingVertexOrEdge+"  ");
                b=Strings.getInputBoolean(in,"Choose as target? ");
            }
            if(!b)target=target.next;
        }while((target!=null)&&(!b));
        BigInteger targetV=(BigInteger)target.hangingVertexOrEdge;
        General.showOutput(targetV+" : "+g.hasVertex(targetV));
        k=Strings.getInputInt(in,"Enter a weight for this edge: ", null);
        String name =Strings.getInputString("Enter a name for this edge: ");
        
        moo = g.addEdge(sourceV,targetV,k,name);
        if(moo==true){
            General.showOutput("We successfully added an edge from "+
            sourceV+" to "+targetV+" with weight "+k+" and name " + name);
        }else{
            General.showOutput("Not happening, sorry.");
        }
        
    }
    
    
    public void seeGraph(){
        seeGraph(-1); // ask for graph number
    }
    
    public void seeGraph(int n){
//        char c=' ';
        
        ListElement target = graphList;
        if ( n == -1 ) {
            n=1;
            if(graphCount>1){
                n=Strings.getInputInt(in, "Which graph? ("+graphCount+" total) ", null);
            }
        }
        for(int i=1;i<n;i++){
            target=target.next;
        }
        
        if(graphCount==0){
            General.showOutput("No graphs created. Press 'g' to create one.");
            return;
        }
        DirectedGraph g =
        (DirectedGraph)target.hangingVertexOrEdge;
        g.showGraph();
    }
    
    public void topoSort(){
//        char c=' ';
        int n=1;
        ListElement target = graphList;
        if(graphCount>1){
            n=Strings.getInputInt(in, "Which graph? ("+graphCount+" total) ", null);
            for(int i=1;i<n;i++){
                target=target.next;
            }
        }
        if(graphCount==0){
            General.showOutput("No graphs created. Press 'g' to create one.");
            return;
        }
        DirectedGraph g1 = (DirectedGraph)target.hangingVertexOrEdge;
        Object[] a1 = g1.topologicalSort();
        if(a1!=null){
            General.showOutput("Topological array:");
            for(int i=0;i<a1.length;i++){
                General.showOutput("\t\t"+(i+1)+": "+a1[i]);
            }
        }else{
            General.showOutput("Topological sort not possible.");
        }
    }
    
    public void testEqual(){
//        char c=' ';
        int n1=1;
        int n2=1;
        ListElement target = graphList;
        if(graphCount>1){
            n1=Strings.getInputInt(in, "First graph? ("+graphCount+" total)", null);
            for(int i=1;i<n1;i++){
                target=target.next;
            }
        }
        if(graphCount==0){
            General.showOutput("No graphs created. Press 'g' to create one.");
            return;
        }
        DirectedGraph g1 =
        (DirectedGraph)target.hangingVertexOrEdge;
        target = graphList;
        if(graphCount>1){
            n2=Strings.getInputInt(in, "Second graph? ("+graphCount+" total) ", null);
            for(int i=1;i<n2;i++){
                target=target.next;
            }
        }
        DirectedGraph g2 =
        (DirectedGraph)target.hangingVertexOrEdge;
        General.showOutput("-----------------------------");
        General.showOutput("      Equality testing:      ");
        General.showOutput("Graph1#"+n1+"  ==>  Graph2#"+n2);
        General.showOutput("         "+g1.equals(g2)+"      ");
        General.showOutput("Graph2#"+n2+"  <==  Graph1#"+n1);
        General.showOutput("         "+g2.equals(g1)+"      ");
        General.showOutput("-----------------------------");
    }
    
    
    public void seeVertex(){
        boolean b=false;
        int n=1;
        ListElement target = graphList;
        if(graphCount>1){
            n=Strings.getInputInt(in, "Which graph? ("+graphCount+") ", null);
            for(int i=1;i<n;i++){
                target=target.next;
            }
        }
        DirectedGraph g =
        (DirectedGraph)target.hangingVertexOrEdge;
        //selecting vertex
        target=vertexList;
        do{
            if(g.hasVertex((BigInteger)target.hangingVertexOrEdge)){
                General.showOutputNoEol("Graph "+n+" has a vertex "+
                target.hangingVertexOrEdge+"  ");
                b=Strings.getInputBoolean(in, "Choose to inspect? (y/n)? ");
            }
            if(!b)target=target.next;
        }while((target!=null)&&(!b));
        BigInteger targetV=(BigInteger)target.hangingVertexOrEdge;
        
        General.showOutput("-----------------------------");
        General.showOutput("Graph "+n+", vertex "+targetV+":");
        General.showOutput("-----------------------------");
        General.showOutput("g.hasVertex("+targetV+") : "+g.hasVertex(targetV));
        General.showOutput("g.inDegree("+targetV+") : "+g.inDegree(targetV));
        //	General.showOutput("g.inDegree(moo) : "+g.inDegree("moo"));
        General.showOutput("g.outDegree("+targetV+") : "+g.outDegree(targetV));
        //      General.showOutput("g.inDegree(moo) : "+g.outDegree("moo"));
        General.showOutput("g.isIsolated("+targetV+") : "+g.isIsolated(targetV));
        //      General.showOutput("g.isIsolated(moo) : "+g.isIsolated("moo"));
        
        Object[] a1 = g.adjacentsOf(targetV);
        General.showOutput("-----------------------------\n"+
        "Adjacents of this vertex:");
        for(int i=0;i<a1.length;i++){
            General.showOutput("\t\t"+(i+1)+":  "+targetV+" --> "+a1[i]+
            "  (weight="+g.edgeWeight(targetV,a1[i])+")");
        }
        
        General.showOutput("-----------------------------");
    }
    
    public void doBreathFirstSearch() {
        DirectedGraph x = (DirectedGraph) graphList.hangingVertexOrEdge;
        String startVertexObject = Strings.getInputString( in, "Give vertex id from which to start the search: ", null);
        x.doBreathFirstSearch(startVertexObject);
    }
    
    /** Example from Jonathan */
    public void fillWithExample_1() {
        createGraph();
        DirectedGraph x = (DirectedGraph) graphList.hangingVertexOrEdge;
        x.addVertex("a");
        x.addVertex("b");
        x.addVertex("c");
        x.addVertex("d");
        x.addVertex("e");
        x.addVertex("f");
        
        x.addEdge("a","d",1,"first edge");
        x.addEdge("a","e",1,null);
        x.addEdge("d","b",1,null);
        x.addEdge("b","e",1,null);
        x.addEdge("b","c",1,null);
        x.addEdge("c","e",1,null);
        x.addEdge("c","f",1,null);
        x.addEdge("e","f",1,null);
        
        seeGraph(1);
        
        /**
         * Object[] a1 = x.topologicalSort();
         * if(a1!=null){
         * General.showOutput("Topological array of graph "+x.hashCode()+":");
         * for(int i=0;i<a1.length;i++){
         * General.showOutput("\t\t"+(i+1)+": "+a1[i]);
         * }
         * }else{
         * General.showOutput("Topological sort not possible.");
         * }
         */
    }
    
    /** Example from book for Breath first search */
    public void fillWithExample_2_bfs() {
        createGraph();
        DirectedGraph x = (DirectedGraph) graphList.hangingVertexOrEdge;
        x.addVertex("r");
        x.addVertex("s");
        x.addVertex("t");
        x.addVertex("u");
        x.addVertex("v");
        x.addVertex("w");
        x.addVertex("x");
        x.addVertex("y");
        
        x.addEdge("r","v",1,null);
        x.addEdge("s","r",1,null);
        x.addEdge("s","w",1,null);
        x.addEdge("w","t",1,null);
        x.addEdge("w","x",1,null);
        
        x.addEdge("x","t",1,null);
        x.addEdge("t","u",1,null);
        x.addEdge("x","u",1,null);
        x.addEdge("x","y",1,null);
        x.addEdge("y","u",1,null);
        
        seeGraph(1);
    }
    
    /** Example of a cyclic graph; based on how proteins do what they
     * do on the basis of their structure and sequence
     */
    public void fillWithExample_3_cyclic() {
        createGraph();
        DirectedGraph x = (DirectedGraph) graphList.hangingVertexOrEdge;
        x.addVertex("structure");
        x.addVertex("function");
        x.addVertex("sequence");
        
        x.addEdge("structure","function",1,null);
        x.addEdge("function","sequence",1,null);
        x.addEdge("sequence","structure",1,null);
        
        x.addEdge("function","structure",1,null);
        x.addEdge("sequence","function",1,null);
        x.addEdge("function","sequence",1,null);
        
        seeGraph(1);
    }
    
    public void serialize() {
        
        // Link to the graph list gets written other elements can be set.
        boolean status = InOut.writeObject( this, location_bin );
        if (! status) {
            General.showError("writing graphList BIN file.");
            System.exit(1);
        }
        General.showOutput("Done with serializing.");
    }
    
    
    
    public static void main(String args[]){
        
        Test t = new Test();
        
        char c = Strings.getInputChar(in, t.gp, null);
        
        while(true){
            switch(c){
                case 'g':{t.createGraph();break;}
                case 'v':{t.createVertex();break;}
                case 'e':{t.createEdge();break;}
                case 'c':{t.seeVertex();break;}
                case '?':{t.seeGraph();break;}
                case 't':{t.topoSort();break;}
                case '=':{t.testEqual();break;}
                case 'b':{t.doBreathFirstSearch();break;}
                case 's':{t.serialize();break;}
                case 'd':{
                    t = (Test) InOut.readObjectOrEOF( t.location_bin );
                    if ( t == null ) {
                        General.showError("failed to read state back in");
                    } else {
                        General.showDebug("read state back in");
                    }
                    break;
                }
                case '1':{t.fillWithExample_1();break;}
                case '2':{t.fillWithExample_2_bfs();break;}
                case '3':{t.fillWithExample_3_cyclic();break;}
                case 'q':{t.exit();}
            }
            c=Strings.getInputChar(in, t.gp, null);
        }
    }    
}
