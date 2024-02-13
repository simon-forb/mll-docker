package mll;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MLLRunner {

	/**
	 * Runs the provided LLVM program.
	 */
	public static void runLLVM(String filename) {
		runLLVM(filename, false);
	}
	
	/**
	 * Runs the provided LLVM program.
	 */
	public static void runLLVM(String filename, boolean insideJupyter) {
		
		String[] command;
		if (insideJupyter) {
			command = new String[]{"lli", "/home/jovyan/llvm/" + filename + ".ll"};
		} else {
			command = new String[]{"docker","exec", "-t", "mll_docker", "lli", "/home/jovyan/mll/llvm/" + filename + ".ll"};
		}

	    runCommand(command);
	}
	
    
   /**
     * Stores a single function plot on disk.
     */
    public static String saveFunctionPlot(float[] x, float[] y, String plotName) {
        try {
            XYChart chart = getChart(x, y, plotName);
            
            Files.createDirectories(Paths.get("plots"));
            
            String filepath = Paths.get("plots", plotName + ".png").toString(); 
            
            BitmapEncoder.saveBitmapWithDPI(
            		chart, 
            		filepath, 
            		BitmapEncoder.BitmapFormat.PNG, 300
            		);
            
            return filepath;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stores a plot with two functions on disk.
     */
    public static String saveFunctionPlot(float[] x, float[] y1, float[] y2, String plotName) {
        try {
            XYChart chart = getDoubleChart(x, y1, y2, plotName);
            
            Files.createDirectories(Paths.get("plots"));
            
            String filepath = Paths.get("plots", plotName + ".png").toString(); 
 
            
            BitmapEncoder.saveBitmapWithDPI(
            		chart,
            		filepath,
            		BitmapEncoder.BitmapFormat.PNG, 300
            		);
            
            return filepath;
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * Executes LLVM optimization and saves output to disk.
	 */
	public static String saveOpt(String filename, int optLevel) throws IOException {
		return saveOpt(filename, optLevel, false);
	}
	

    /**
	 * Executes LLVM optimization and saves output to disk.
	 */
	public static String saveOpt(String filename, int optLevel, boolean insideJupyter) throws IOException {
	
		Files.createDirectories(Paths.get("llvm"));
		
		String filepath = Paths.get("llvm", filename + getOptLevelCode(optLevel) + ".png").toString();
		
		String[] command;
		if (insideJupyter) {
			command = new String[]{"opt", getOptLevelCode(optLevel), 
		    		"/home/jovyan/llvm/" + filename + ".ll", "-So", 
		    		"/home/jovyan/llvm/" + (filename + getOptLevelCode(optLevel) + ".ll")};
		} else {
			command = new String[]{"docker", "exec", "-t", "mll_docker", "opt", getOptLevelCode(optLevel), 
		    		"/home/jovyan/mll/llvm/" + filename + ".ll", "-So", 
		    		"/home/jovyan/mll/llvm/" + (filename + getOptLevelCode(optLevel) + ".ll")};
		}

	    runCommand(command);
	    
	    return filepath;
	}
	

	/**
	 * Displays plot of a single function in a Jupyter-Notebook.
	 */
	public static BufferedImage viewFunctionPlot(float[] x, float[] y, String plotName) {
	    XYChart chart = getChart(x, y, plotName);
	    return BitmapEncoder.getBufferedImage(chart);
	}


	/**
	 * Displays a plot with two functions y1(x) and y2(x).
	 */
	public static BufferedImage viewFunctionPlot(float[] x, float[] y1, float[] y2, String plotName) {
        XYChart chart = getDoubleChart(x, y1, y2, plotName);
        return BitmapEncoder.getBufferedImage(chart);
    }





    private static XYChart getChart(float[] x, float[] y, String plotName) {
	    return QuickChart.getChart(plotName, "", "", "f", getDoubleArray(x), getDoubleArray(y));
	}


	private static double[] getDoubleArray(float[] arr) {
	    double[] arr2 = new double[arr.length];
	    for (int i = 0; i < arr.length; i++) {
	        arr2[i] = arr[i];
	    }
	    return arr2;
	}


	private static XYChart getDoubleChart(float[] x, float[] y1, float[] y2, String plotName) {
	    XYChart chart = getChart(x, y1, plotName);
	    chart.addSeries("g", getDoubleArray(x), getDoubleArray(y2));
	    chart.getStyler().setMarkerSize(0);
	    return chart;
	}


    private static String getOptLevelCode(int optLevel) {
		return switch (optLevel) {
	        case 0 -> "-O0";
	        case 1 -> "-O1";
	        case 2 -> "-O2";
	        case 3 -> "-O3";
	        default -> "-O3";
	    };
	}


    private static void runCommand(String[] command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();

            int character;
            while ((character = inputStream.read()) != -1) {
                System.out.print((char) character);
            }
            inputStream.close();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Program execution failed with exit code " + exitCode);
            } 

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}