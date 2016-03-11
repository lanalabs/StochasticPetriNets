package org.processmining.plugins.logmodeltrust.heuristic.processtree;

import java.util.UUID;

import org.processmining.plugins.logmodeltrust.mover.TreeUtils;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractTask;

public class TreeWrapper {

	public static ProcessTree getWrappedTree(ProcessTree tree){
		try {
			ProcessTree copyTree = TreeUtils.getClone(tree);
			Node wrappedRoot = wrap(copyTree, copyTree.getRoot());
			copyTree.setRoot(wrappedRoot);
			return copyTree;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

	private static Node wrap(ProcessTree newTree, Node node) throws Exception {
		NodeWrapper wrapper = null;
		Node newNode = null;
		try {
			if (node instanceof AbstractTask.Manual){
				newNode = new AbstractTask.Manual(node.getID(), node.getName());
			} else {
				newNode = node.getClass().getConstructor(UUID.class, String.class).newInstance(node.getID(), node.getName());
			}
			
		} catch (NoSuchMethodException e){
			System.out.println("Debug me!");
		}
		if (!node.isLeaf()){
			wrapper = new BlockWrapper((Block) newNode);
		} else {
			wrapper = new NodeWrapper(newNode);
		}
		newTree.addNode(wrapper);
		
		if (!node.isLeaf()){
			BlockWrapper bWrapper = (BlockWrapper) wrapper;
			for (Node child : ((Block)node).getChildren()){
				Node wrappedChild = wrap(newTree, child);
				bWrapper.addChild(wrappedChild);
			}
		}
		return wrapper;
	}
}
