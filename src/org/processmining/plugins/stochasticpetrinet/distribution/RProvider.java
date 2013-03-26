package org.processmining.plugins.stochasticpetrinet.distribution;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.swing.JFileChooser;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

public class RProvider {

	private static Rengine engine;
	
	public static synchronized Rengine getEngine(){
		if (engine == null){
			engine = new Rengine(new String[]{"--vanilla"}, false, new TextConsole());
	        System.out.println("Rengine created, waiting for R");
	        if (!engine.waitForR()) {
	            System.out.println("Cannot load R");
	            throw new UnsupportedOperationException("Cannot load R");
	        }
	        engine.eval("library(\"logspline\")",false);
		}
		return engine;
	}

	public static boolean getEngineAvailable() {
		return Rengine.jriLoaded;
	}
}

class TextConsole implements RMainLoopCallbacks
{
    public void rWriteConsole(Rengine re, String text, int oType) {
        System.out.print(text);
    }
    
    public void rBusy(Rengine re, int which) {
        System.out.println("rBusy("+which+")");
    }
    
    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        System.out.print(prompt);
        try {
            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String s=br.readLine();
            return (s==null||s.length()==0)?s:s+"\n";
        } catch (Exception e) {
            System.out.println("jriReadConsole exception: "+e.getMessage());
        }
        return null;
    }
    
    public void rShowMessage(Rengine re, String message) {
        System.out.println("rShowMessage \""+message+"\"");
    }
	
    public String rChooseFile(Rengine re, int newFile) {
    	JFileChooser fc = new JFileChooser(".");
    	int choice;
    	if (newFile == 0){
    		choice = fc.showOpenDialog(null);
    	} else {
    		choice = fc.showSaveDialog(null);
    	}
    	String res = null;
    	if (choice == JFileChooser.APPROVE_OPTION){
    		res = fc.getSelectedFile().getAbsolutePath();
    	}
    	return res;
    }
    
    public void   rFlushConsole (Rengine re) {
    }
	
    public void   rLoadHistory  (Rengine re, String filename) {
    }			
    
    public void   rSaveHistory  (Rengine re, String filename) {
    }			
}