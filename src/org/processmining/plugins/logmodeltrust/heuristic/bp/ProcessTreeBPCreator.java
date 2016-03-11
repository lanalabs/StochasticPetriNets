package org.processmining.plugins.logmodeltrust.heuristic.bp;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jbpt.bp.BehaviouralProfile;
import org.jbpt.bp.RelSetType;
import org.jbpt.bp.construct.AbstractRelSetCreator;
import org.processmining.plugins.logmodeltrust.heuristic.processtree.BlockWrapper;
import org.processmining.plugins.logmodeltrust.heuristic.processtree.NodeWrapper;
import org.processmining.plugins.logmodeltrust.heuristic.processtree.TreeWrapper;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;



public class ProcessTreeBPCreator extends AbstractRelSetCreator {
	
	private static ProcessTreeBPCreator eInstance;
	
	public static ProcessTreeBPCreator getInstance() {
		if (eInstance == null)
			eInstance  = new ProcessTreeBPCreator();
		return eInstance;
	}
	
	private ProcessTreeBPCreator() {
		
	}
	
	public BehaviouralProfile<ProcessTree, NodeWrapper> deriveBehaviouralProfile(ProcessTree pt) {
		ProcessTree wrappedTree = TreeWrapper.getWrappedTree(pt);
		
		List<NodeWrapper> wrappedNodes = new ArrayList<NodeWrapper>();
		for (Node node : wrappedTree.getNodes()){
			if (node instanceof NodeWrapper){
				NodeWrapper wrapper = (NodeWrapper) node;
				if (! (wrapper instanceof BlockWrapper)){
					if (!wrapper.getName().isEmpty() && !wrapper.getName().equals("tau") && !wrapper.getName().startsWith("tau ")){
						wrappedNodes.add(wrapper);	
					}	
				}
			}
		}
		
		return deriveBehaviouralProfile(wrappedTree, wrappedNodes);
	}
	
	public BehaviouralProfile<ProcessTree, NodeWrapper> deriveBehaviouralProfile(ProcessTree pt, Collection<NodeWrapper> nodes) {

		ProcessTreeHandler processTreeHandler = new ProcessTreeHandler(pt);
		
		BehaviouralProfile<ProcessTree, NodeWrapper> profile = new BehaviouralProfile<ProcessTree, NodeWrapper>(pt,nodes);
		RelSetType[][] matrix = profile.getMatrix();

		for(NodeWrapper t1 : profile.getEntities()) {
			int index1 = profile.getEntities().indexOf(t1);
			for(NodeWrapper t2 : profile.getEntities()) {
				int index2 = profile.getEntities().indexOf(t2);
				
				/*
				 * The behavioural profile matrix is symmetric. Therefore, we 
				 * need to traverse only half of the entries.
				 */
				if (index2 > index1)
					continue;
				
				if (processTreeHandler.areExclusive(t1, t2)) {
					super.setMatrixEntry(matrix, index1, index2, RelSetType.Exclusive);
				}
				else if (processTreeHandler.areInterleaving(t1, t2)) {
					super.setMatrixEntry(matrix, index1, index2, RelSetType.Interleaving);
				}
				else if (processTreeHandler.areInOrder(t1, t2)) {
					if (processTreeHandler.areInStrictOrder(t1, t2))
						super.setMatrixEntryOrder(matrix, index1, index2);
					else
						super.setMatrixEntryOrder(matrix, index2, index1);
				}
			}
		}		
		
		
		return profile;
	}

}
