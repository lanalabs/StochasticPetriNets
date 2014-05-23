package org.processmining.tests.plugins.stochasticnet.data.convert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.out.XSerializer;
import org.deckfour.xes.out.XSerializerRegistry;
import org.junit.Ignore;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.pnml.exporting.PnmlExportStochasticNet;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.tests.plugins.stochasticnet.data.IbmModel;
import org.processmining.tests.plugins.stochasticnet.data.IbmProcess;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class ConversionTest {

	@Ignore
	@Test
	public void testConversion() throws Exception {
		
		//String[] names = new String[]{"A"};
		String[] names = new String[]{"A","B1","B2","B3","C"};
		
//		List<IbmProcess> processes = new LinkedList<IbmProcess>();
		
		int ignored = 0;
		
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
					
					File file = new File("tests/testfiles/ibm/converted/"+name+"_"+process.getName()+".pnml");
					if (!file.exists()){
						file.createNewFile();
					}
					if (file.canWrite()){
						FileWriter writer = new FileWriter(file);
						
						PnmlExportStochasticNet exporter = new PnmlExportStochasticNet();
						exporter.exportPetriNetToPNMLFile(null, net, writer);
						writer.flush();
						writer.close();
						
					} else {
						System.err.println("can't write to file "+file.getAbsolutePath());
					}
				} catch (IllegalArgumentException e){
					System.out.println("Ignored "+(++ignored)+" models, due to inclusive or splits.");
				}
				System.out.println("...done.");
			}
			
//			processes.addAll(model.getProcessModel().getProcesses());
//			
//			
//			
//			System.out.println("adding "+model.getProcessModel().getProcesses().size()+" processes from "+name+".xml");	
		}
//		System.out.println("----\nTotal: "+processes.size());
	}

	@Ignore
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
				if (net != null){
					PNSimulator simulator = new PNSimulator();
					PNSimulatorConfig simConfig = new PNSimulatorConfig(100, TimeUnit.MINUTES);
					simConfig.setDeterministicBoundedStateSpaceExploration(true);
					
					StochasticNetSemantics semantics = new EfficientStochasticNetSemanticsImpl();
					Marking initialMarking = StochasticNetUtils.getInitialMarking(null,net);
					semantics.initialize(net.getTransitions(), initialMarking);
					long beforeSimulation = System.currentTimeMillis();
					
					try {
						XLog log = simulator.simulate(null, net, semantics, simConfig, initialMarking);
						
						
						File logFile = new File("tests/testfiles/ibm/converted/"+name+"_"+process.getName()+".xes");
						if (!logFile.exists()){
							logFile.createNewFile();
						}
						if (logFile.canWrite()){
							XSerializer xserializer = XSerializerRegistry.instance().currentDefault();
							xserializer.serialize(log, new FileOutputStream(logFile));
						}
						System.out.println(process.getName()+": simulated "+log.size()+" traces in "+(System.currentTimeMillis()-beforeSimulation)+"ms.");
					} catch (IllegalArgumentException e) {
						System.err.println(process.getName()+" simulation error: "+e.getMessage());
					}
				} 
				System.out.println("...done.");
			}
		}
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
				}
				if (net != null && new File("tests/testfiles/ibm/converted/"+name+"_"+process.getName()+".xes").exists()){
					
				}
			}
		}
	}
}
