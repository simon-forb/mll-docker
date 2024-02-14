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
        String plot = MLLRunner.saveFunctionPlot(xValues, yValues, "plot");
        System.out.println("Plot saved as: " + plot);

        // Save LLVM to disk
        String llvm = MLLRunner.saveLLVM(add, "program");
        System.out.println("Program saved as: " + llvm);

        // Save optimized LLVM to disk
        String opt = MLLRunner.saveOpt("program", 1);
        System.out.println("Program saved as: " + opt);
        
        // Run LLVM
        MLLRunner.runLLVM("program");
                
	}
	
}
