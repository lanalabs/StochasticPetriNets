package org.processmining.tests.plugins.stochasticnet.external;

import java.awt.BorderLayout;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.math3.util.Pair;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.converter.ConvertDistributionsPlugin;
import org.processmining.plugins.stochasticpetrinet.external.Allocatable;
import org.processmining.plugins.stochasticpetrinet.external.AllocationBasedNetGenerator;
import org.processmining.plugins.stochasticpetrinet.external.AllocationDistribution;
import org.processmining.plugins.stochasticpetrinet.external.Building;
import org.processmining.plugins.stochasticpetrinet.external.Compartment;
import org.processmining.plugins.stochasticpetrinet.external.Person;
import org.processmining.plugins.stochasticpetrinet.external.PetrinetModelAllocations;
import org.processmining.plugins.stochasticpetrinet.external.Role;
import org.processmining.plugins.stochasticpetrinet.external.Room;
import org.processmining.plugins.stochasticpetrinet.external.UniformAllocation;
import org.processmining.plugins.stochasticpetrinet.external.sensor.LogToSensorIntervalConverter;
import org.processmining.plugins.stochasticpetrinet.external.sensor.SensorIntervalVisualization;
import org.processmining.plugins.stochasticpetrinet.external.sensor.SortedSensorIntervals;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNUnfoldedSimulator;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class AllocationExperimentTest {

	@Test
	public void testAllocationExperiment() throws Exception {
		Object[] modelAndMarking = TestUtils.loadModel("CAISE_16_main_process", true);
		Petrinet net = (Petrinet) modelAndMarking[0];
		
		Object[] stochNetAndMarking = ConvertDistributionsPlugin.enrichStochasticInformation(null, net);
		StochasticNet stochNet = (StochasticNet) stochNetAndMarking[0];
		stochNet.setTimeUnit(TimeUnit.MINUTES);
		stochNet.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		
		
		
		boolean mixedResources = true; // to be tweaked by experiment
		int rooms = 1; // to be tweaked by experiment
		int maxCapacity = 1; // to be tweaked by experiment
		int nResources = 1;  // to be tweaked by experiment
		
		// dropOffRate = probability for adding more resources to an allocation set
		double dropOffRate = 0.5; // to be tweaked by experiment
		double arrivalRate = 1. / 5; // 1 per 5 minutes (to be tweaked by experiment)
		double meanTimeBetweenArrivals = 1 / arrivalRate; 
		
		int numCases = 6; // the number of patients to create lanes for
		
		
		Set<Allocatable> allResources = new HashSet<Allocatable>();
		
		// build Structure:
		Building hospital = new Building("hospital");
		Set<Allocatable> examRooms = addCompartment(rooms, maxCapacity, hospital, "exam_room", allResources);
		Set<Allocatable> bloodDrawRooms = addCompartment(rooms, maxCapacity, hospital, "blood_draw_room", allResources);
		Set<Allocatable> infusionRooms = addCompartment(rooms, maxCapacity, hospital, "infusion_room", allResources);
		Set<Allocatable> consultingRooms = addCompartment(rooms, maxCapacity, hospital, "consulting_room", allResources);
		
		// build resources:
		Role hospitalStaff = new Role("staff");
		Role phlebotomistRole = new Role("phlebotomist");
		phlebotomistRole.addCanPerform(hospitalStaff);
		Set<Allocatable> phlebotomists = new HashSet<Allocatable>();
		
		Role nurseRole = new Role("nurse");
		nurseRole.addCanPerform(phlebotomistRole);
		nurseRole.addCanPerform(hospitalStaff);
		Set<Allocatable> nurses = new HashSet<Allocatable>();
		
		Role doctorRole = new Role("doctor");
		doctorRole.addCanPerform(hospitalStaff);
		doctorRole.addCanPerform(nurseRole);
		doctorRole.addCanPerform(phlebotomistRole);
		Set<Allocatable> doctors = new HashSet<Allocatable>();
		
		for (int i = 1; i <= nResources; i++){
			Person nurse = new Person("nurse"+i);
			nurse.addRole(nurseRole);
			nurses.add(nurse);
			allResources.add(nurse);
			
			Person phlebotomist = new Person("phlebotomist"+i);
			phlebotomist.addRole(phlebotomistRole);
			phlebotomists.add(phlebotomist);
			allResources.add(phlebotomist);
			
			Person doctor = new Person("doctor"+i);
			doctor.addRole(doctorRole);
			doctors.add(doctor);
			allResources.add(doctor);
		}
		
		// add rooms
		
		Transition consulting = null;
		Transition bloodDraw = null;
		Transition examination = null;
		Transition infusion = null;
		
		for (Transition t : stochNet.getTransitions()){
			if (t.getLabel().equals("consulting")){
				consulting = t;
			}
			if (t.getLabel().equals("blood draw")){
				bloodDraw = t;
			}
			if (t.getLabel().equals("examination")){
				examination = t;
			}
			if (t.getLabel().equals("infusion")){
				infusion = t;
			}
		}
		Assert.assertNotNull(consulting);
		Assert.assertNotNull(bloodDraw);
		Assert.assertNotNull(examination);
		Assert.assertNotNull(infusion);
		
		
		
		// hard code allocation patterns
		AllocationDistribution consultingAllocation = new AllocationDistribution();
		AllocationDistribution examinationAllocation = new AllocationDistribution();
		AllocationDistribution bloodDrawAllocation = new AllocationDistribution();
		AllocationDistribution infusionAllocation = new AllocationDistribution();
		
		Set<Set<Allocatable>> doctorSubsets = TestUtils.generateAllSubsets(doctors);
		Set<Set<Allocatable>> nurseSubsets = TestUtils.generateAllSubsets(nurses);
		Set<Set<Allocatable>> phlebotomistSubsets = TestUtils.generateAllSubsets(phlebotomists);
		
		Iterator<Set<Allocatable>> subSetIter = doctorSubsets.iterator();
		while (subSetIter.hasNext()){
			Set<Allocatable> subset = subSetIter.next();
			if (subset.size()>0 && subset.size() <= 3){
				double probability = Math.pow(dropOffRate,subset.size()-1);
				consultingAllocation.addAllocationOption(subset, probability);
				examinationAllocation.addAllocationOption(subset, probability);
			}
		}
		
		Iterator<Set<Allocatable>> nurseSubSetIter = nurseSubsets.iterator();
		while (nurseSubSetIter.hasNext()){
			Set<Allocatable> subset = nurseSubSetIter.next();
			if (subset.size()>0 && subset.size() <= 3){
				double probability = Math.pow(dropOffRate,subset.size()-1);
				if (mixedResources){
					bloodDrawAllocation.addAllocationOption(subset, probability);
				}
				infusionAllocation.addAllocationOption(subset, probability);
			}
		}
		Iterator<Set<Allocatable>> phleboSubSetIter = phlebotomistSubsets.iterator();
		while (phleboSubSetIter.hasNext()){
			Set<Allocatable> subset = phleboSubSetIter.next();
			if (subset.size()>0 && subset.size() <= 3){
				double probability = Math.pow(dropOffRate,subset.size()-1);
				bloodDrawAllocation.addAllocationOption(subset, probability);
				if (mixedResources){
					infusionAllocation.addAllocationOption(subset, probability);
				}
			}
		}
		
		PetrinetModelAllocations modelAllocations = new PetrinetModelAllocations(stochNet);
		modelAllocations.addAllocation(infusion, infusionAllocation);
		modelAllocations.addAllocation(consulting, consultingAllocation);
		modelAllocations.addAllocation(bloodDraw, bloodDrawAllocation);
		modelAllocations.addAllocation(examination, examinationAllocation);
		
		modelAllocations.addAllocation(infusion, new UniformAllocation(infusionRooms));
		modelAllocations.addAllocation(consulting, new UniformAllocation(consultingRooms));
		modelAllocations.addAllocation(bloodDraw, new UniformAllocation(bloodDrawRooms));
		modelAllocations.addAllocation(examination, new UniformAllocation(examRooms));
		
		System.out.println("\n\n");
		List<Pair<Transition,Transition>> orderedTransitions = modelAllocations.getOrderRelation(stochNet);
		for (Pair<Transition,Transition> orderedPair : orderedTransitions){
			System.out.println(orderedPair.getFirst().getLabel()+" -> "+orderedPair.getSecond().getLabel());
		}
		System.out.println("\n\n");

		//TODO: add lane information to be able to distinguish traces
		Object[] generatedNetAndMarking = AllocationBasedNetGenerator.generateNet(stochNet, modelAllocations, allResources, numCases, meanTimeBetweenArrivals); 
		
		PNUnfoldedSimulator simulator = new PNUnfoldedSimulator();
		
		PNSimulatorConfig config = new PNSimulatorConfig(1);
		StochasticNet generatedNet = (StochasticNet) generatedNetAndMarking[0];
		
		// visualize generated net!
//		TestUtils.showModel(generatedNet);
		
		XLog log = simulator.simulate(null, generatedNet, StochasticNetUtils.getSemantics(generatedNet), config, (Marking) generatedNetAndMarking[1]);
		StochasticNetUtils.writeLogToFile(log, new File("tests/testfiles/out/richLog"+numCases+".xes"));
		for (XTrace trace : log){
			System.out.println(StochasticNetUtils.debugTrace(trace));
		}
		
		System.out.println("\n\n-----------------\n\n");
		
		SortedSensorIntervals intervals = LogToSensorIntervalConverter.convertLog(log, generatedNet.getTimeUnit(), false, 1); 
		System.out.println(intervals.toString());
		
		JComponent orig = SensorIntervalVisualization.visualize(null, intervals);
		
		SortedSensorIntervals intervalsAdjusted = LogToSensorIntervalConverter.convertLog(log, generatedNet.getTimeUnit(), true, 1);
		
		JComponent adjusted = SensorIntervalVisualization.visualize(null, intervalsAdjusted);
		
		JComponent comparison = new JPanel();
		comparison.setLayout(new BorderLayout());
		comparison.add(orig, BorderLayout.NORTH);
		comparison.add(adjusted, BorderLayout.SOUTH);
		
		TestUtils.showComponent(comparison);
		
		StochasticNetUtils.writeStringToFile(intervals.toString(), "tests/testfiles/out/intervalLog"+numCases+".csv");
		
	}

	protected Set<Allocatable> addCompartment(int nRooms, int maxCapacity, Building hospital, String name, Set<Allocatable> allResources) {
		Compartment rooms = new Compartment(name+"s");
		Set<Allocatable> allocationRooms = new HashSet<Allocatable>();
		rooms.setParentLocation(hospital);
		// 
		for (int i = 0; i < nRooms; i++){
			Room room = new Room(name+"_"+i, maxCapacity);
			room.setParentLocation(rooms);
			allResources.add(room);
			allocationRooms.add(room);
		}
		return allocationRooms;
	}
}
