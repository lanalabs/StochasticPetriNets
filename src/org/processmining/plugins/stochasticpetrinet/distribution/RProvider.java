package org.processmining.plugins.stochasticpetrinet.distribution;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.JRI.JRIEngine;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

public class RProvider {

    private static Rengine engine;

    public static String[] REQUIRED_LIBRARIES = {"logspline"};

    public static synchronized Rengine getEngine() {
        if (engine == null) {
            engine = new Rengine(new String[]{"--vanilla"}, false, new TextConsole());
            System.out.println("Rengine created, waiting for R");
            if (!engine.waitForR()) {
                System.out.println("Cannot load R");
                JOptionPane.showMessageDialog(null, "Could not start up R!\n" +
                        "Please make sure, R is installed and the the R_HOME environment variable points to the R installation directory.\n" +
                        "(in linux systems, this might be /usr/lib/R, or /usr/lib64/R, depending on your system.)\n" +
                        "\n" +
                        "You can install R from:\n" +
                        "http://www.r-project.org/");
                engine.end();
                engine = null;
                throw new UnsupportedOperationException("Cannot load R");
            }
            for (String library : REQUIRED_LIBRARIES) {
                engine.eval(String.format("library(\"%s\"", library), false);
            }

        }
        return engine;
    }

    public static synchronized REngine getREngine() {
        try {
            if (engine != null) {
                return new JRIEngine(engine);
            }
            if (REngine.getLastEngine() != null) {
                return REngine.getLastEngine();
            }
            return REngine.engineForClass(JRIEngine.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (REngineException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean getJRIAvailable() {
        try {
            System.setProperty("jri.ignore.ule", "yes");
            return Rengine.jriLoaded;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }
}

class TextConsole implements RMainLoopCallbacks {
    public void rWriteConsole(Rengine re, String text, int oType) {
        System.out.print(text);
    }

    public void rBusy(Rengine re, int which) {
        System.out.println("rBusy(" + which + ")");
    }

    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        System.out.print(prompt);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String s = br.readLine();
            return (s == null || s.length() == 0) ? s : s + "\n";
        } catch (Exception e) {
            System.out.println("jriReadConsole exception: " + e.getMessage());
        }
        return null;
    }

    public void rShowMessage(Rengine re, String message) {
        System.out.println("rShowMessage \"" + message + "\"");
    }

    public String rChooseFile(Rengine re, int newFile) {
        JFileChooser fc = new JFileChooser(".");
        int choice;
        if (newFile == 0) {
            choice = fc.showOpenDialog(null);
        } else {
            choice = fc.showSaveDialog(null);
        }
        String res = null;
        if (choice == JFileChooser.APPROVE_OPTION) {
            res = fc.getSelectedFile().getAbsolutePath();
        }
        return res;
    }

    public void rFlushConsole(Rengine re) {
    }

    public void rLoadHistory(Rengine re, String filename) {
    }

    public void rSaveHistory(Rengine re, String filename) {
    }
}