package mll;

import java.io.IOException;

public class Main {
	
	public static void main(String[] args) throws IOException {

		// Generate example DAG
        DAG dag = new DAG();
        Op x = dag.x();
        Op a = dag.lit(3.f);
        Op add = x.add(a);
        
        // Evaluate
        System.out.println(add.eval(2.f));
		
        // Save DOT as PNG
        MLLRunner.saveDot(add, "test");
         
        // Generate sample plot data
        float[] xValues = new float[5];
        float[] yValues = new float[5];
        for (int i = 0; i < 5; i++) {
            xValues[i] = i;
            yValues[i] = add.eval((float) i);
        }
        
        // Save function plot to disk
        MLLRunner.saveFunctionPlot(xValues, yValues, "plot");

        // Save LLVM to disk
        add.llvmToFile("program");
                
        // Save optimized LLVM to disk
        MLLRunner.saveOpt("program", 1);
                
	}
	
}
