package org.processmining.tests.plugins.stochasticnet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.impl.ToStochasticNet;
import org.processmining.plugins.stochasticpetrinet.generator.Generator;
import org.processmining.plugins.stochasticpetrinet.generator.GeneratorConfig;

public class GeneratorTest {

	@Test
	public void testNaming() throws Exception {
		for(int i = 0; i < 1500; i++){
			System.out.println(i+" = "+Generator.getNameForNumber(i));
		}
	}
	
	@Test
	public void testVariance() {
		Generator gen = new Generator(2);
		
		DistributionType[] types = new DistributionType[]{DistributionType.GAUSSIAN_KERNEL, DistributionType.EXPONENTIAL, DistributionType.NORMAL, DistributionType.UNIFORM, DistributionType.LOGNORMAL};
		
		for (DistributionType type : types){
				
			DescriptiveStatistics expStats = new DescriptiveStatistics();
			DescriptiveStatistics expVariance = new DescriptiveStatistics();
			
			TimedTransition tt = new TimedTransition("", null);
			
			for (int i = 0; i < 100; i++){
				double[] expStat = gen.getRandomParameters(type);
				tt.setDistribution(null);
				tt.setDistributionType(type);
				tt.setDistributionParameters(expStat);
				RealDistribution dist = tt.initDistribution(0);
				double[] samples = dist.sample(1000);
				DescriptiveStatistics sampleStats = new DescriptiveStatistics(samples);
				expStats.addValue(sampleStats.getMean());
				expVariance.addValue(sampleStats.getVariance());
			}
			System.out.println("\nType: "+ type);
			System.out.println("mean: \t\t" +expStats.getMean());
			System.out.println("variance: \t" +expVariance.getMean());
		}
	}
	
	@Test
	public void testDotExport(){
		Generator generator = new Generator(2);
		
		int size = 10;
		
		GeneratorConfig config = new GeneratorConfig();
		config.setContainsLoops(true);
		config.setDegreeOfLoops(0); // 10 %
		config.setDegreeOfExclusiveChoices(3); // 10 %
		config.setDegreeOfSequences(4);  // 60%
		config.setDegreeOfParallelism(3);  // 30%
		config.setDistributionType(DistributionType.NORMAL);
		config.setTimeUnit(TimeUnit.MINUTES);
		config.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		config.setTransitionSize(size);
		config.setCreateDedicatedImmediateStartTransition(true);
		
		Object[] netAndMarking = generator.generateStochasticNet(config);
		StochasticNet net = (StochasticNet) netAndMarking[0];
		
		String fileName = "generatedTestNet"+size+".dot";
		String fileNamePS = "currentGeneratedNet"+size+".ps";
		String dotString = ToStochasticNet.convertPetrinetToDOT(net);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
			writer.write(dotString);
			writer.flush();
			writer.close();
			
			Process p = Runtime.getRuntime().exec("dot -Tps "+fileName+" -o "+fileNamePS);
			p.waitFor();
			
			p = Runtime.getRuntime().exec("rm "+fileName);
			p.waitFor();
			
			p = Runtime.getRuntime().exec("evince "+fileNamePS);
			p.waitFor();
			
			p = Runtime.getRuntime().exec("rm "+fileNamePS);
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
