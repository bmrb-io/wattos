/*
 * TestCern.java
 *
 * Created on June 18, 2003, 8:39 AM
 */

package Wattos.Utils.TestRoutines;

import Wattos.Utils.*;

/**
 *
 * @author Jurgen F. Doreleijers
 */
public class TestCern {
    
    
    /**
     * Creates a new instance of TestCern
     */
    public TestCern() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
    // Gamma distribution

    // define distribution parameters
    double mean = 5;
    double variance = 1.5;
    double alpha = mean*mean / variance; 
    double lambda = 1 / (variance / mean); 

    // for tests and debugging use a random engine with CONSTANT seed --> deterministic and reproducible results
    cern.jet.random.engine.RandomEngine engine = new cern.jet.random.engine.MersenneTwister(); 

    // your favourite distribution goes here
    cern.jet.random.AbstractDistribution dist = new cern.jet.random.Gamma(alpha,lambda,engine);

    // collect random numbers and print statistics
    int size = 100000;
    cern.colt.list.DoubleArrayList numbers = new cern.colt.list.DoubleArrayList(size);
    for (int i=0; i < size; i++) numbers.add(dist.nextDouble());

    hep.aida.bin.DynamicBin1D bin = new hep.aida.bin.DynamicBin1D();
    bin.addAllOf(numbers);
    General.showOutput(bin.toString());        
    }    
}
