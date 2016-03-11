package org.processmining.plugins.logmodeltrust.heuristic.bp;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jbpt.bp.RelSetType;
import org.processmining.plugins.logmodeltrust.heuristic.processtree.BlockWrapper;
import org.processmining.plugins.logmodeltrust.heuristic.processtree.NodeWrapper;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;

public class ProcessTreeHandler {

	private ProcessTree tree = null;

	public ProcessTreeHandler(ProcessTree pt) {
		this.tree = pt;
	}
	
	private boolean areInSeries(NodeWrapper lca, NodeWrapper a, NodeWrapper b) {
		
		List<NodeWrapper> pathA = this.getDownwardPath(lca, a);
		List<NodeWrapper> pathB = this.getDownwardPath(lca, b);
		
		if (pathA.size()<2 || pathB.size()<2) return false;
		
		List<Node> children = ((BlockWrapper)lca).getChildren();
//		System.out.println(children.indexOf(pathA.get(1)));
//		System.out.println(children.indexOf(pathB.get(1)));
		
		if (children.indexOf(pathA.get(1))<children.indexOf(pathB.get(1)))
			return true;
		
		return false;
	}
	
	
	/**
	 * Check if two nodes are in strict order relation (->)
	 * @param t1 node
	 * @param t2 node
	 * @return true if t1->t2, false otherwise
	 */
	public boolean areInStrictOrder(NodeWrapper t1, NodeWrapper t2) {
		if (t1.equals(t2)) return false; // as easy as that
		NodeWrapper gamma = this.getLCA(t1, t2);
		
		// check path from ROOT to gamma
		List<NodeWrapper> path = this.getDownwardPath((NodeWrapper)this.tree.getRoot(), gamma);
		
		// check path from ROOT to parent of gamma
		for (int i=0; i<path.size()-1; i++) 
			if (ProcessTree.Type.getTypeOf(path.get(i).getRealNode()) == ProcessTree.Type.LOOPXOR) return false;
		
		// check gamma
		if (ProcessTree.Type.getTypeOf(gamma.getRealNode())!=ProcessTree.Type.SEQ) return false;
		if (areInSeries(gamma, t1, t2)) return true;
		
		return false;
	}
	
	/**
	 * Check if two nodes are in order relation
	 * @param t1 node
	 * @param t2 node
	 * @return true if t1->t2 or t2->t1, false otherwise
	 */
	public boolean areInOrder(NodeWrapper t1, NodeWrapper t2) {
		return areInStrictOrder(t1, t2) || areInStrictOrder(t2, t1);
	}
	
	/**
	 * Check if two nodes are in exclusive relation (+)
	 * @param t1 node
	 * @param t2 node
	 * @return true if t1+t2, false otherwise
	 */
	public boolean areExclusive(NodeWrapper t1, NodeWrapper t2) {
		NodeWrapper gamma = this.getLCA(t1, t2);
		
		// check path from ROOT to gamma
		List<NodeWrapper> path = this.getDownwardPath((NodeWrapper) this.tree.getRoot(), gamma);
		
		// check path from ROOT to parent of gamma
		for (int i=0; i<path.size()-1; i++) 
			if (ProcessTree.Type.getTypeOf(path.get(i).getRealNode()) == ProcessTree.Type.LOOPXOR) return false;
		
		// check gamma
		if (ProcessTree.Type.getTypeOf(gamma.getRealNode())==ProcessTree.Type.XOR) return true;
		
		// handle alpha == beta == gamma case 
		if (t1.equals(t2)) return true;
		
		return false;
	}
	
	/**
	 * Check if two nodes are in interleaving relation (||)
	 * @param t1 node
	 * @param t2 node
	 * @return true if t1||t2, false otherwise
	 */
	public boolean areInterleaving(NodeWrapper t1, NodeWrapper t2) {
		NodeWrapper gamma = this.getLCA(t1, t2);
		
		// Get path from ROOT to gamma
		List<NodeWrapper> path = this.getDownwardPath((NodeWrapper) this.tree.getRoot(), gamma);
		
		// check path from ROOT to the parent of gamma
		for (int i=0; i<path.size()-1; i++) 
			if (ProcessTree.Type.getTypeOf(path.get(i).getRealNode()) == ProcessTree.Type.LOOPXOR) return true;
		
		// check gamma
		ProcessTree.Type gammaBlockType = ProcessTree.Type.getTypeOf(gamma.getRealNode());
		if (gammaBlockType==ProcessTree.Type.AND || gammaBlockType==ProcessTree.Type.LOOPXOR) return true;
		
		return false;
	}
	
	public RelSetType getRelationForNodes(NodeWrapper t1, NodeWrapper t2) {
		if (areExclusive(t1, t2))
			return RelSetType.Exclusive;
		if (areInterleaving(t1, t2))
			return RelSetType.Interleaving;
		if (areInStrictOrder(t1, t2))
			return RelSetType.Order;
		if (areInStrictOrder(t2, t1))
			return RelSetType.ReverseOrder;
		return RelSetType.None;
	}

	public NodeWrapper getLCA(NodeWrapper v1, NodeWrapper v2) {
		
		if (v1.equals(v2)) return v1;
		
		List<NodeWrapper> path1 = this.getDownwardPath((NodeWrapper) this.tree.getRoot(),v1);
		List<NodeWrapper> path2 = this.getDownwardPath((NodeWrapper) this.tree.getRoot(),v2);
		
		NodeWrapper result = null;
		for (int i=0; i<path1.size(); i++) {
			if (i>=path2.size()) break;
			if (path1.get(i).equals(path2.get(i))) result = path1.get(i);
			else break;
		}
		
		return result;
	}

	public List<NodeWrapper> getDownwardPath(NodeWrapper v1, NodeWrapper v2) {		
		List<NodeWrapper> result = new ArrayList<NodeWrapper>();
		
		NodeWrapper v = v2;
		result.add(v);
		while (!v.getIncomingEdges().isEmpty() && !result.contains(v1)) {
			v = (NodeWrapper) v.getIncomingEdges().get(0).getSource();
			result.add(v);
		}
		
		if (!result.contains(v1)) return new ArrayList<NodeWrapper>();
		Collections.reverse(result);
		return result;
	}

}
