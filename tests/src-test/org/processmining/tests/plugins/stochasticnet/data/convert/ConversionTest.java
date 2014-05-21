package org.processmining.tests.plugins.stochasticnet.data.convert;

import java.io.File;
import java.io.FileWriter;

import org.junit.Ignore;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.plugins.pnml.exporting.PnmlExportStochasticNet;
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
		
		for (String name : names){
			Serializer serializer = new Persister();
			File source = new File("tests/testfiles/ibm/"+name+".xml");

			IbmModel model = serializer.read(IbmModel.class, source);
			
			
			for (IbmProcess process : model.getProcessModel().getProcesses()){
				System.out.println("Handling process "+process.getName()+"...");
				
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
}
