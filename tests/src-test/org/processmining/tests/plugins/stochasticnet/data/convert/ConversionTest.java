package org.processmining.tests.plugins.stochasticnet.data.convert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XSerializerRegistry;
import org.junit.Ignore;
import org.junit.Test;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.pnml.exporting.PnmlExportStochasticNet;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.tests.plugins.stochasticnet.data.IbmFlowContent;
import org.processmining.tests.plugins.stochasticnet.data.IbmModel;
import org.processmining.tests.plugins.stochasticnet.data.IbmProcess;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class ConversionTest {

//	@Ignore
	@Test
	public void testConversion() throws Exception {
		
		//String[] names = new String[]{"A"};
		//String[] names = new String[]{"A","B1","B2","B3","C"};
		String[] names = new String[]{"A","B3","C"};
//		String[] names = new String[]{"C"};
		
//		List<IbmProcess> processes = new LinkedList<IbmProcess>();
		
		String outputPath = "tests/testfiles/ibm/converted_expanded2";
		
		int ignored = 0;
		int empty = 0;
		int unconnected = 0;
		int successes = 0;
		
		for (String name : names){
			Serializer serializer = new Persister();
			File source = new File("tests/testfiles/ibm/"+name+".xml");

			IbmModel model = serializer.read(IbmModel.class, source);
			
			
			for (IbmProcess process : model.getProcessModel().getProcesses()){
				System.out.println("Handling process "+process.getName()+"...");
				try{
					
					StochasticNet net = IbmToStochasticNetConverter.convertFromIbmProcess(process);
					net.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
					net.setTimeUnit(TimeUnit.MINUTES);
					
					if(isEmpty(net)){
						empty++;
						System.err.println("Ignoring "+empty+". empty net "+net.getLabel()+".");
					} else if (isConnected(net)){
						StochasticNetUtils.exportAsDOTFile(net, outputPath , name+"_"+process.getName());
						
						File file = new File(outputPath+"/"+name+"_"+process.getName()+".pnml");
						if (!file.exists()){
							file.createNewFile();
						}
						if (file.canWrite()){
							FileWriter writer = new FileWriter(file);
							successes++;
							PnmlExportStochasticNet exporter = new PnmlExportStochasticNet();
							exporter.exportPetriNetToPNMLFile(null, net, writer);
							writer.flush();
							writer.close();
							
						} else {
							System.err.println("can't write to file "+file.getAbsolutePath());
						}
					} else {
						System.err.println("Net "+ net.getLabel()+" is "+(++unconnected)+" not connected net!");
					}
				} catch (IllegalArgumentException e){
					System.out.println("Ignored "+(++ignored)+" models, due to inclusive or splits.\n"
							+ e.getMessage());
					
				}
				System.out.println("...done.");
			}
			System.out.println("Converted successfully: "+successes);
			System.out.println("Empty: "+empty);
			System.out.println("Ignored: "+ignored);
			System.out.println("Unconnected: "+unconnected);
			
//			processes.addAll(model.getProcessModel().getProcesses());
//			
//			
//			
//			System.out.println("adding "+model.getProcessModel().getProcesses().size()+" processes from "+name+".xml");	
		}
//		System.out.println("----\nTotal: "+processes.size());
	}

	private boolean isEmpty(StochasticNet net) {
		return net.getPlaces().size()==0 || net.getTransitions().size()==0;
	}

	private boolean isConnected(StochasticNet net) {
		Set<DirectedGraphNode> markedNodes = new HashSet<DirectedGraphNode>();
		Place p = net.getPlaces().iterator().next();
		visit(p, net, markedNodes);
		return markedNodes.size() == net.getPlaces().size()+net.getTransitions().size();
	}

	private void visit(DirectedGraphNode node, Petrinet net, Set<DirectedGraphNode> markedNodes) {
		// visit neighbors:
		markedNodes.add(node);
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getOutEdges(node)){
			PetrinetNode target = edge.getTarget();
			if (!markedNodes.contains(target)){
				visit(target,net,markedNodes);
			} 
		}
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getInEdges(node)){
			PetrinetNode source = edge.getSource();
			if (!markedNodes.contains(source)){
				visit(source,net,markedNodes);
			}
		}
	}

	//	@Ignore
	@Test
	public void testMappingConstraints() throws Exception {
		//String[] names = new String[]{"A"};
		String[] names = new String[]{"A","B1","B2","B3","C"};
		
//				List<IbmProcess> processes = new LinkedList<IbmProcess>();
		
		int ignored = 0;
		
		for (String name : names){
			Serializer serializer = new Persister();
			File source = new File("tests/testfiles/ibm/"+name+".xml");

			IbmModel model = serializer.read(IbmModel.class, source);
			
			
			for (IbmProcess process : model.getProcessModel().getProcesses()){
				System.out.println("Handling process "+process.getName()+"...");
				StochasticNet net = null;
				try{
					net = IbmToStochasticNetConverter.convertFromIbmProcess(process);
					net.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
					net.setTimeUnit(TimeUnit.MINUTES);
					
				} catch (IllegalArgumentException e){
					System.out.println("Ignored model "+process.getName()+", due to inclusive or splits.");
				}
				if (net != null && net.getTransitions() != null && net.getTransitions().size() > 0 && isConnected(net)){
					PNSimulator simulator = new PNSimulator();
					PNSimulatorConfig simConfig = new PNSimulatorConfig(1000, TimeUnit.MINUTES);
					simConfig.setDeterministicBoundedStateSpaceExploration(true);
					simConfig.setAllowUnbounded(false);
					
					StochasticNetSemantics semantics = new EfficientStochasticNetSemanticsImpl();
					Marking initialMarking = StochasticNetUtils.getInitialMarking(null,net);
					semantics.initialize(net.getTransitions(), initialMarking);
					long beforeSimulation = System.currentTimeMillis();
					
					try {
						XLog log = simulator.simulate(null, net, semantics, simConfig, initialMarking);
						
						if (!log.isEmpty() && !containsEmptyTraces(log)){
							
							File logFile = new File("tests/testfiles/ibm/converted4/"+name+"_"+process.getName()+".xes.gz");
							if (!logFile.exists()){
								logFile.createNewFile();
							}
							if (logFile.canWrite()){
								XSerializer xserializer = XSerializerRegistry.instance().currentDefault();
								xserializer.serialize(log, new FileOutputStream(logFile));
							}
							System.out.println(process.getName()+": simulated "+log.size()+" traces in "+(System.currentTimeMillis()-beforeSimulation)+"ms.");
						}
						System.err.println(process.getName()+" simulation error: empty log, or empty traces!");
					} catch (IllegalArgumentException e) {
						System.err.println(process.getName()+" simulation error: "+e.getMessage());
					} catch (OutOfMemoryError oome){
						System.err.println(process.getName()+" simulation error: "+oome.getMessage());
					}
				} 
				System.out.println("...done.");
			}
		}
	}
	private boolean containsEmptyTraces(XLog log) {
		for (XTrace t : log){
			if (t.size() == 0){
				return true;
			}
		}return false;
	}

	@Ignore
	@Test
	public void testMapper() throws Exception {
		//String[] names = new String[]{"A"};
		String[] names = new String[]{"A","B1","B2","B3","C"};
		
//				List<IbmProcess> processes = new LinkedList<IbmProcess>();
		
		int ignored = 0;
		
		for (String name : names){
			Serializer serializer = new Persister();
			File source = new File("tests/testfiles/ibm/"+name+".xml");

			IbmModel model = serializer.read(IbmModel.class, source);
			
			
			for (IbmProcess process : model.getProcessModel().getProcesses()){
				System.out.println("Handling process "+process.getName()+"...");
				StochasticNet net = null;
				try{
					net = IbmToStochasticNetConverter.convertFromIbmProcess(process);
					net.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
					net.setTimeUnit(TimeUnit.MINUTES);
				} catch (IllegalArgumentException e){
					System.out.println("Ignored model "+process.getName()+", due to inclusive or splits.");
					ignored++;
				}
				if (net != null && new File("tests/testfiles/ibm/converted/"+name+"_"+process.getName()+".xes").exists()){
					
				}
			}
		}
	}
	
	@Test
	public void testBPMNProcess() throws Exception {
		convertOneFile("IBM_C_s0000001##s00000519");
	}
	@Test
	public void testBPMNProcess2() throws Exception {
		convertOneFile("IBM_C_s0000001##s00003867");
	}
	@Test
	public void testBPMNProcess3() throws Exception {
		convertOneFile("IBM_C_s0000003##s00000812");
	}
	@Test
	public void testBPMNProcess4() throws Exception {
		convertOneFile("IBM_C_s0000001##s00017029");
	}
	

	private void convertOneFile(String name) throws Exception {
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/ibm/"+name+".xml");

		IbmProcess model = serializer.read(IbmProcess.class, source);
		IbmFlowContent content = model.getFlowContent();
		model.getInputs();
		model.getOutputs();
		System.out.println(content.getHumanTasks());
		StochasticNet net = IbmToStochasticNetConverter.convertFromIbmProcess(model, true);
		StochasticNetUtils.exportAsDOTFile(net, "tests/testfiles/ibm/", name);
	}
}
