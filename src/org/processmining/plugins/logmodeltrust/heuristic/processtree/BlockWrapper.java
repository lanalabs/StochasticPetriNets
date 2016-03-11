package org.processmining.plugins.logmodeltrust.heuristic.processtree;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Expression;
import org.processmining.processtree.Node;

public class BlockWrapper extends NodeWrapper implements Block {
	
	protected Block realBlock;
	
	public BlockWrapper(Block block){
		super(block);
		this.realBlock = block;
	}
	
	public Object clone(){
		try {
			Constructor<? extends Block> constructor = realBlock.getClass().getConstructor(realNode.getClass());
			Block clone = constructor.newInstance(realBlock);
			return new BlockWrapper(clone);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			e1.printStackTrace();
		};
		return null;
	}

	public List<Edge> getOutgoingEdges() {
		return realBlock.getOutgoingEdges();
	}

	public void addOutgoingEdge(Edge edge) {
		realBlock.addOutgoingEdge(edge);
	}

	public void removeOutgoingEdge(Edge edge) {
		realBlock.removeOutgoingEdge(edge);
	}

	public void addOutgoingEdgeAt(Edge edge, int index) {
		realBlock.addOutgoingEdgeAt(edge, index);
	}

	public Edge addChild(Node child, Expression expression) {
		return realBlock.addChild(child, expression);
	}

	public Edge addChild(Node child) {
		Edge edge = realBlock.addChild(child);
		edge.setSource(this);
		return edge;
	}

	public Edge addChildAt(Node child, Expression expression, int index) {
		return realBlock.addChildAt(child, expression, index);
	}

	public Edge addChildAt(Node child, int index) {
		return realBlock.addChildAt(child, index);
	}

	public Edge swapChildAt(Node child, Expression expression, int index) {
		return realBlock.swapChildAt(child, expression, index);
	}

	public Edge swapChildAt(Node child, int index) {
		return realBlock.swapChildAt(child, index);
	}

	public List<Node> getChildren() {
		return realBlock.getChildren();
	}

	public Iterator<Node> iterator() {
		return realBlock.iterator();
	}

	public int numChildren() {
		return realBlock.numChildren();
	}

	public boolean orderingOfChildernMatters() {
		return realBlock.orderingOfChildernMatters();
	}

	public boolean expressionsOfOutgoingEdgesMatter() {
		return realBlock.expressionsOfOutgoingEdgesMatter();
	}

	public boolean isChangeable() {
		return realBlock.isChangeable();
	}

	public void setChangeable(boolean changeable) {
		realBlock.setChangeable(changeable);		
	}
	public String toString(){
		return "block("+realBlock.getClass().getSimpleName()+")";
	}
}
