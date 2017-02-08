package org.processmining.tests.plugins.stochasticnet;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.log.OpenNaiveLogFilePlugin;
import org.processmining.plugins.log.logfilters.impl.EventLogFilter;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentPlugin;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentPlugin.ExperimentType;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentResult;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Contains shared functionality of the test cases.
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class TestUtils {

	public static final String TEST_FOLDER = "tests/testfiles/";
	
	/**
	 * Runs a performance enricher experiment on a given Model.
	 *  
	 * @param experimentType {@link ExperimentType} specifying the varying input (trace size, or noise)
	 * @param fileName String the model file name
	 * @throws Exception
	 * @throws IOException
	 */
	public static void runExperimentAndSaveOutput(ExperimentType experimentType, String fileName) throws Exception,
			IOException {
		Object[] netAndMarking = TestUtils.loadModel(fileName,true);
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, net);

		PerformanceEnricherExperimentPlugin enrichmentPlugin = new PerformanceEnricherExperimentPlugin();

		PerformanceEnricherExperimentResult result = enrichmentPlugin.performExperiment(null, net, initialMarking,
				experimentType);
		saveCSV(result.getResultsCSV(experimentType), fileName + "_" + experimentType + "_"+System.currentTimeMillis()+".csv");
	}
	/**
	 * Loads a Petri net model from the test files folder.
	 * 
	 * @param name String filename without the suffix ".pnml" tries to load a {@link StochasticNet} from
	 * the tests/testfiles folder of the installation.
	 *  
	 * @return size 2 Object[] containing the {@link StochasticNet} and the initial {@link Marking} of the net.  
	 * @throws Exception
	 */
	public static Object[] loadModel(String name, boolean addMarkingsToCache) throws Exception {
		Serializer serializer = new Persister();
		if (!name.endsWith(".pnml")){
			name = name + ".pnml";
		}
		File source = new File(TEST_FOLDER+name);

		PNMLRoot pnml = serializer.read(PNMLRoot.class, source);

		StochasticNetDeserializer converter = new StochasticNetDeserializer();
		Object[] netAndMarking = converter.convertToNet(null, pnml, name, false);
		if (addMarkingsToCache){
			if (netAndMarking[1] != null){
				StochasticNetUtils.cacheInitialMarking((StochasticNet)netAndMarking[0], (Marking) netAndMarking[1]);
			} 
			if (netAndMarking[2] != null){
				StochasticNetUtils.cacheFinalMarking((StochasticNet)netAndMarking[0], (Marking) netAndMarking[2]);
			}
		}
		return netAndMarking;
	}
	
	public static void showModel(Petrinet net){
		JComponent comp = getVisualization(StochasticNetUtils.getDummyConsoleProgressContext(), net);
		comp.setPreferredSize(new Dimension(900, 500));
		showComponent(comp);
	}
	
	public static void showComponent(JComponent comp) {
		JFrame frame = new JFrame("Test Visualization");
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setContentPane(comp);
		
		frame.pack(); 
		frame.setVisible(true);
		try {
			Thread.sleep(600000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			frame.setVisible(false);
			frame.dispose();
		}
	}

	private static JComponent getVisualization(PluginContext context, Petrinet net) {
		ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();
		
		for (Transition t : net.getTransitions()){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition)t;
				map.putViewSpecific(t, AttributeMap.LABEL, t.getLabel()+" ("+tt.getWeight()+")");
				String parameters = Arrays.toString(tt.getDistributionParameters());
				if (parameters.length()>101){
					parameters = parameters.substring(0, 100)+"..."+"("+tt.getDistributionParameters().length+" parameters in total)";
				}
				map.putViewSpecific(t, AttributeMap.TOOLTIP, "<html>"+t.getLabel()+"\n<br>" +
						"priority: "+tt.getPriority()+"\n<br>" +
						"weight: "+tt.getWeight()+"\n<br>" +
						"type: "+tt.getDistributionType().toString()+"\n<br>" +
						"parameters: "+parameters+"</html>");
			} else {
				map.putViewSpecific(t, AttributeMap.TOOLTIP, t.getLabel());
			}
		}
		
		ProMJGraphPanel graphPanel = ProMJGraphVisualizer.instance().visualizeGraph(context, net, map);
		return graphPanel;
	}
		
	/**
	 * Keep only events supplied as selected objects.
	 * 
	 * @param log the event log to filter
	 * @param classifier the classifier to use to extract a string of the events
	 * @param selectedObjects the array of event classes that we keep - the rest is filtered out.
	 * @return
	 */
	public static XLog filter(XLog log, XEventClassifier classifier, String...selectedObjects){
		//FinalEventLogFilter filter = new FinalEventLogFilter();
		EventLogFilter filter = new EventLogFilter();
		return filter.filterWithClassifier(null, log, classifier, selectedObjects);
	}
	
	/**
	 * Loads a log from the tests/testfiles folder of the plugin.
	 * 
	 * @param name the file name (including the suffix such as .xes, .xes.gz, or .mxml)
	 * @return the loaded {@link XLog}
	 * @throws Exception
	 */
	public static XLog loadLog(String name) throws Exception {
		return loadLog(new File(TEST_FOLDER+name));
	}
	
	/**
	 * Opens a Log from a given file.
	 * 
	 * @param file {@link File} containing the log.
	 * @return the loaded {@link XLog} 
	 * @throws Exception
	 */
	public static XLog loadLog(File file) throws Exception {
		OpenNaiveLogFilePlugin loader = new OpenNaiveLogFilePlugin();
		return (XLog) loader.importFile(StochasticNetUtils.getDummyUIContext(), file);
//		XUniversalParser parser = new XUniversalParser();
//		Collection<XLog> logs = parser.parse(file);
//		if (logs.size() > 0){
//			return logs.iterator().next();
//		}
//		return null;
	}
	
	/**
	 * Shorthand to add an event with a given concept:name, a timestamp, and a lifecycle information to a trace.
	 *  
	 * @param name the name of the event (representing the event type)
	 * @param trace the {@link XTrace} to add to
	 * @param time the time stamp of the new event
	 * @param lifecycleTransition the lifecycle transition of the event (e.g., "start", "complete") 
	 * @return {@link XEvent} the newly created and added event.
	 */
	public static XEvent addEvent(String name, XTrace trace, long time, StandardModel lifecycleTransition){
		XEvent e = addEvent(name, trace, time);
		XLifecycleExtension.instance().assignStandardTransition(e, lifecycleTransition);
		return e;
	}
	
	/**
	 * Shorthand to add an event with a given concept:name, a timestamp, and a lifecycle information to a trace.
	 *  
	 * @param name the name of the event (representing the event type)
	 * @param trace the {@link XTrace} to add to
	 * @param time the time stamp of the new event
	 * @param lifecycleTransition the lifecycle transition of the event (e.g., "start", "complete") 
	 * @return {@link XEvent} the newly created and added event.
	 */
	public static XEvent addEvent(String name, XTrace trace, long time, String lifecycleTransition){
		XEvent e = addEvent(name, trace, time);
		XLifecycleExtension.instance().assignTransition(e, lifecycleTransition);
		return e;
	}
	
	/**
	 * Shorthand to add an event with a given concept:name and a timestamp to a trace.
	 *  
	 * @param name the name of the event (representing the event type)
	 * @param trace the {@link XTrace} to add to
	 * @param time the time stamp of the new event 
	 * @return {@link XEvent} the newly created and added event.
	 */
	public static XEvent addEvent(String name, XTrace trace, long time){
		XEvent e = addEvent(name, trace);
		XTimeExtension.instance().assignTimestamp(e, time);
		return e;
	}
	
	/**
	 * Shorthand to add an event with a given concept:name
	 * @param name the name of the event (representing the event type)
	 * @param trace the {@link XTrace} to add to
	 * @return {@link XEvent} the newly created and added event.
	 */
	public static XEvent addEvent(String name, XTrace trace) {
		XEvent e = XFactoryRegistry.instance().currentDefault().createEvent();
		XConceptExtension.instance().assignName(e, name);
		trace.add(e);
		return e;
	}
	
	/**
	 * Exports the results of an experiment into the experimentResults folder, into a file with a given name.
	 * @param csvContent String the file content to be exported.
	 * @param fileName String the file name
	 * @throws IOException
	 */
	public static void saveCSV(String csvContent, String fileName) throws IOException{
		File resultsFolder = new File("./experimentResults");
		if (!resultsFolder.exists()){
			resultsFolder.mkdir();
		}
		File resultsFile = new File(resultsFolder, fileName);
		if (!resultsFile.exists()){
			resultsFile.createNewFile();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile));
		writer.write(csvContent);
		writer.flush();
		writer.close();
	}
}
