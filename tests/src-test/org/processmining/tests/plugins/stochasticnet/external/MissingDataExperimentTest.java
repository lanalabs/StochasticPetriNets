package org.processmining.tests.plugins.stochasticnet.external;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.stochasticpetrinet.converter.ConvertDistributionsPlugin;
import org.processmining.plugins.stochasticpetrinet.external.Allocatable;
import org.processmining.plugins.stochasticpetrinet.external.Allocation;
import org.processmining.plugins.stochasticpetrinet.external.Allocation.AllocType;
import org.processmining.plugins.stochasticpetrinet.external.AllocationBasedNetGenerator;
import org.processmining.plugins.stochasticpetrinet.external.AllocationConfig;
import org.processmining.plugins.stochasticpetrinet.external.AllocationDistribution;
import org.processmining.plugins.stochasticpetrinet.external.Person;
import org.processmining.plugins.stochasticpetrinet.external.PetrinetModelAllocations;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class MissingDataExperimentTest {
	@Test
	public void testGenerator() throws Exception {
		
		int nResources = 1;
		
		Object[] modelAndMarking = TestUtils.loadModel("CAISE_16_main_process", true);
		Petrinet net = (Petrinet) modelAndMarking[0];
		Object[] stochNetAndMarking = ConvertDistributionsPlugin.enrichStochasticInformation(null, net, DistributionType.GAMMA);
		StochasticNet stochNet = (StochasticNet) stochNetAndMarking[0];
		stochNet.setTimeUnit(TimeUnit.MINUTES);
		stochNet.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		
		Set<Allocatable> allResources = new HashSet<Allocatable>();
		Set<Allocatable> phlebotomists = new HashSet<Allocatable>();
		Set<Allocatable> nurses = new HashSet<Allocatable>();
		Set<Allocatable> doctors = new HashSet<Allocatable>();
		
		for (int i = 1; i <= nResources; i++) {
			Person nurse = new Person("nurse" + i);
			nurses.add(nurse);
			Person phlebotomist = new Person("phlebotomist" + i);
			phlebotomists.add(phlebotomist);
			Person doctor = new Person("doctor" + i);
			doctors.add(doctor);
		}
		allResources.addAll(doctors);
		allResources.addAll(nurses);
		allResources.addAll(phlebotomists);
		

		Transition consulting=null,bloodDraw=null,examination=null,infusion = null;

		for (Transition t : stochNet.getTransitions()) {
			if (t.getLabel().equals("consulting")) {
				consulting = t;
			}
			if (t.getLabel().equals("blood draw")) {
				bloodDraw = t;
			}
			if (t.getLabel().equals("examination")) {
				examination = t;
			}
			if (t.getLabel().equals("infusion")) {
				infusion = t;
			}
		}
		Map<Transition, Integer> relevantActivityMapping = getRelevantActivities(consulting, bloodDraw, examination,
				infusion);
		Map<Integer, Transition> activityIds = handleRelevantActivities(stochNet, relevantActivityMapping);
		
		PetrinetModelAllocations modelAllocations = null;
		modelAllocations = generateNoiseAllocations(stochNet, phlebotomists, nurses, doctors, consulting,
				bloodDraw, examination, infusion);
		
		Object[] generatedNetAndMarking = AllocationBasedNetGenerator.generateObservationAwareNet(stochNet, modelAllocations, allResources, 0);
		
		StochasticNet generatedNet = (StochasticNet) generatedNetAndMarking[0];

		// visualize generated net!
//		TestUtils.showModel(net);
		TestUtils.showModel(generatedNet);
	}
	
	protected Map<Transition, Integer> getRelevantActivities(Transition...transitions) {
		Map<Transition, Integer> relevantActivityMapping = new HashMap<Transition, Integer>();
		for (int i = 0; i < transitions.length; i++){
			relevantActivityMapping.put(transitions[i], i);
		}
		return relevantActivityMapping;
	}
	protected Map<Integer, Transition> handleRelevantActivities(StochasticNet stochNet,
			Map<Transition, Integer> relevantActivityMapping) {
		Map<Integer, Transition> activityIds = new HashMap<Integer, Transition>();

		for (Transition t : relevantActivityMapping.keySet()) {
			activityIds.put(relevantActivityMapping.get(t), t);
			Assert.assertNotNull(t);
		}
		for (Transition t : stochNet.getTransitions()) {
			if (!relevantActivityMapping.containsKey(t)) {
				t.setInvisible(true);
			}
		}
		return activityIds;
	}
	
	protected PetrinetModelAllocations generateNoiseAllocations(StochasticNet stochNet,
			Set<Allocatable> phlebotomists,	Set<Allocatable> nurses, Set<Allocatable> doctors, Transition consulting, Transition bloodDraw,
			Transition examination, Transition infusion) {
		PetrinetModelAllocations modelAllocations = new PetrinetModelAllocations(stochNet);
		
		AllocationDistribution bloodDrawResourceAllocation = new AllocationDistribution(AllocType.RESOURCE);
//		AllocationDistribution bloodDrawLocationAllocation = new AllocationDistribution(AllocType.LOCATION);
		// *** RESOURCES ***
		// phlebotomist = 0.6
		addAlloc(bloodDrawResourceAllocation, new AllocationConfig(phlebotomists,1,0.6, AllocType.RESOURCE));
		// nurse = 0.2
		addAlloc(bloodDrawResourceAllocation, new AllocationConfig(nurses, 1, 0.2, AllocType.RESOURCE));
		// nurse, nurse = 0.2
		addAlloc(bloodDrawResourceAllocation, new AllocationConfig(nurses, 2, 0.2, AllocType.RESOURCE));
//		// *** LOCATIONS ***
//		// nurse room = 0.6
//		addAlloc(bloodDrawLocationAllocation, new AllocationConfig(nurseRooms, 1, 0.6, AllocType.LOCATION));
//		// infusion room = 0.4
//		addAlloc(bloodDrawLocationAllocation, new AllocationConfig(infusionRooms, 1, 0.4, AllocType.LOCATION));
		
		AllocationDistribution consultingResourceAllocation = new AllocationDistribution(AllocType.RESOURCE);
//		AllocationDistribution consultingLocationAllocation = new AllocationDistribution(AllocType.LOCATION);
		// *** RESOURCES ***
		// doctor = 0.95
		addAlloc(consultingResourceAllocation, new AllocationConfig(doctors, 1, 0.95, AllocType.RESOURCE));
		// doctor, doctor = 0.05
		addAlloc(consultingResourceAllocation, new AllocationConfig(doctors, 2, 0.05, AllocType.RESOURCE));
//		// *** LOCATIONS ***
//		// consulting room = 0.6
//		addAlloc(consultingLocationAllocation, new AllocationConfig(consultingRooms, 1, 0.8, AllocType.LOCATION));
//		// exam room = 0.4
//		addAlloc(consultingLocationAllocation, new AllocationConfig(examRooms, 1, 0.2, AllocType.LOCATION));
				
		AllocationDistribution examinationResourceAllocation = new AllocationDistribution(AllocType.RESOURCE);
//		AllocationDistribution examinationLocationAllocation = new AllocationDistribution(AllocType.LOCATION);
		// *** RESOURCES ***
		// doctor = 0.4
		addAlloc(examinationResourceAllocation, new AllocationConfig(doctors, 1, 0.4, AllocType.RESOURCE));
		// nurse = 0.2
		addAlloc(examinationResourceAllocation, new AllocationConfig(nurses, 1, 0.2, AllocType.RESOURCE));
		// doctor, nurse = 0.2
		addAlloc(examinationResourceAllocation, new AllocationConfig(doctors, nurses, 1, 1, 0.2, AllocType.RESOURCE));
		// doctor, doctor = 0.2
		addAlloc(examinationResourceAllocation, new AllocationConfig(doctors, 2, 0.2, AllocType.RESOURCE));
//		// *** LOCATIONS ***
//		// exam room = 0.9
//		addAlloc(examinationLocationAllocation, new AllocationConfig(examRooms, 1, 0.9, AllocType.LOCATION));
//		// consulting room = 0.1
//		addAlloc(examinationLocationAllocation, new AllocationConfig(consultingRooms, 1, 0.1, AllocType.LOCATION));
				
		
		AllocationDistribution infusionResourceAllocation = new AllocationDistribution(AllocType.RESOURCE);
//		AllocationDistribution infusionLocationAllocation = new AllocationDistribution(AllocType.LOCATION);
		// *** RESOURCES ***
		// nurse = 0.8
		addAlloc(infusionResourceAllocation, new AllocationConfig(nurses, 1, 0.8, AllocType.RESOURCE));
		// nurse, nurse = 0.2
		addAlloc(infusionResourceAllocation, new AllocationConfig(nurses, 2, 0.2, AllocType.RESOURCE));
//		// *** LOCATIONS ***
//		// infusion room = 0.95
//		addAlloc(infusionLocationAllocation, new AllocationConfig(infusionRooms, 1, 0.95, AllocType.LOCATION));
//		// nurse room = 0.05
//		addAlloc(infusionLocationAllocation, new AllocationConfig(nurseRooms, 1, 0.05, AllocType.LOCATION));

		
//		Set<Set<Allocatable>> doctorSubsets = StochasticNetUtils.generateAllSubsets(doctors);
//		Set<Allocatable> doctorsAndNurses = new HashSet<Allocatable>(doctors);
//		doctorsAndNurses.addAll(nurses);
//		Set<Set<Allocatable>> doctorNurseSubsets = StochasticNetUtils.generateAllSubsets(doctorsAndNurses);
//		Set<Set<Allocatable>> nurseSubsets = StochasticNetUtils.generateAllSubsets(nurses);
//		Set<Set<Allocatable>> phlebotomistSubsets = StochasticNetUtils.generateAllSubsets(phlebotomists);
//
//		Iterator<Set<Allocatable>> subSetIter = doctorSubsets.iterator();
//		while (subSetIter.hasNext()) {
//			Set<Allocatable> subset = subSetIter.next();
//			if (subset.size() > 0 && subset.size() <= 3) {
//				double probability = Math.pow(dropOffRate, subset.size() - 1);
//				consultingAllocation.addAllocationOption(subset, probability);
//				examinationAllocation.addAllocationOption(subset, probability);
//			}
//		}
//
//		Iterator<Set<Allocatable>> nurseSubSetIter = nurseSubsets.iterator();
//		while (nurseSubSetIter.hasNext()) {
//			Set<Allocatable> subset = nurseSubSetIter.next();
//			if (subset.size() > 0 && subset.size() <= 3) {
//				double probability = Math.pow(dropOffRate, subset.size() - 1);
//				if (mixedResources) {
//					bloodDrawAllocation.addAllocationOption(subset, probability);
//				}
//				infusionAllocation.addAllocationOption(subset, probability);
//			}
//		}
//		Iterator<Set<Allocatable>> phleboSubSetIter = phlebotomistSubsets.iterator();
//		while (phleboSubSetIter.hasNext()) {
//			Set<Allocatable> subset = phleboSubSetIter.next();
//			if (subset.size() > 0 && subset.size() <= 3) {
//				double probability = Math.pow(dropOffRate, subset.size() - 1);
//				bloodDrawAllocation.addAllocationOption(subset, probability);
//				if (mixedResources) {
//					infusionAllocation.addAllocationOption(subset, probability);
//				}
//			}
//		}
//
//		
		modelAllocations.addAllocation(infusion, infusionResourceAllocation);
		modelAllocations.addAllocation(consulting, consultingResourceAllocation);
		modelAllocations.addAllocation(bloodDraw, bloodDrawResourceAllocation);
		modelAllocations.addAllocation(examination, examinationResourceAllocation);
		
//		modelAllocations.addAllocation(infusion, infusionLocationAllocation);
//		modelAllocations.addAllocation(consulting, consultingLocationAllocation);
//		modelAllocations.addAllocation(bloodDraw, bloodDrawLocationAllocation);
//		modelAllocations.addAllocation(examination, examinationLocationAllocation);
//
//		modelAllocations.addAllocation(infusion, new UniformAllocation(infusionRooms, AllocType.LOCATION));
//		modelAllocations.addAllocation(consulting, new UniformAllocation(consultingRooms, AllocType.LOCATION));
//		modelAllocations.addAllocation(bloodDraw, new UniformAllocation(nurseRooms, AllocType.LOCATION));
//		modelAllocations.addAllocation(examination, new UniformAllocation(examRooms, AllocType.LOCATION));
		return modelAllocations;
	}
	

	/**
	 * Adds allocation options according to the give AllocationConfig. 
	 * @param distr the distribution to add the options to.
	 * @param allocationConfig the configuration to use 
	 */
	private void addAlloc(AllocationDistribution distr, AllocationConfig allocationConfig) {
		Allocation alloc = allocationConfig.getResultingAllocationDistribution();
		Map<String, Double> probs = alloc.getProbabilitiesOfAllocations();
		for (String allocString : probs.keySet()){
			Set<Allocatable> allocatables = alloc.getAllocation(allocString);
			distr.addAllocationOption(allocatables, probs.get(allocString)*allocationConfig.getProbability());
		}
	}
}
