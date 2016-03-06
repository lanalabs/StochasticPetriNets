package org.processmining.plugins.logmodeltrust.converter;

import java.util.HashMap;
import java.util.Map;

import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import treedist.LabeledTree;

public class ProcessTreeConverter {

	private BiMap<String, Integer> labelMapping;
	
	private Map<Node, Integer> nodeMapping;
	
	public ProcessTreeConverter(){
		labelMapping = HashBiMap.create();
		nodeMapping = new HashMap<>();
	}
	
	private int getLabelKey(String label){
		if (!labelMapping.containsKey(label)){
			labelMapping.put(label, labelMapping.size());
		} 			
		return labelMapping.get(label);
	}
	
	public String getLabel(int labelKey){
		return labelMapping.inverse().get(labelKey);
	}
	
	public LabeledTree getLabeledTree(ProcessTree processTree){
		int[] parents = new int[processTree.size()];
		int[] labels = new int[processTree.size()];
		Map<Node, Integer> mapping = new HashMap<>();
		Node root = processTree.getRoot();
		
		int index = 0;
		index = addNodesAndChildren(parents, labels, root, mapping, index, -1);
		
		return new LabeledTree(parents, labels);
	}

	private int addNodesAndChildren(int[] parents, int[] labels, Node root, Map<Node, Integer> mapping, int index, int parentId) {
		parents[index] = parentId;
		labels[index] = getLabelKey(root.getName());
		mapping.put(root, mapping.size());
		nodeMapping.put(root, mapping.get(root));
		if (root instanceof Block){
			Block b = (Block) root;
			for (Node child : b.getChildren()){
				index = addNodesAndChildren(parents, labels, child, mapping, index+1, mapping.get(root));
			}
		}
		return index;
	}

	public Map<String,Integer> getMapping() {
		return labelMapping;
	}
	
	public Map<Node, Integer> getNodeMapping() {
		return nodeMapping;
	}
	
//	public static ProcessTree convertFromLblTree(LblTree input){
//		// todo...
//		
//		return null;
//	}
//	
//	public static LblTree toLblTree(ProcessTree tree, int n) {
//		Node treeNode = tree.getRoot();
//		return getLblTree(treeNode);
//	}
//	
//	public static LblTree getLblTree(Node node){
//		LblTree treeNode =  new LblTree(node.getName(), -1);
//		if (node instanceof Block){
//			Block b = (Block) node;
//			for (Node child : b.getChildren()){
//				treeNode.add(getLblTree(child));
//			}	
//		} 
//		return treeNode;
//	}
}
