package org.processmining.plugins.logmodeltrust.heuristic.processtree;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jbpt.hypergraph.abs.IEntity;
import org.processmining.plugins.properties.processmodel.Property;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Expression;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.Variable;

/**
 * Decorator class for nodes in a process tree.
 * 
 * @author andreas
 *
 */
public class NodeWrapper implements Node, IEntity{
	
	protected Node realNode;
	
	public NodeWrapper(Node realNode){
		this.realNode = realNode;
	}
	
	public UUID getID() {
		return realNode.getID();
	}

	public String getName() {
		return realNode.getName();
	}

	public void setName(String name) {
		realNode.setName(name);
	}

	public Object getIndependentProperty(Class<? extends Property<?>> property)
			throws InstantiationException, IllegalAccessException {
		return realNode.getIndependentProperty(property);
	}

	public Object getIndependentProperty(Property<?> property) throws InstantiationException, IllegalAccessException {
		return realNode.getIndependentProperty(property);
	}

	public void setIndependentProperty(Class<? extends Property<?>> property, Object value)
			throws InstantiationException, IllegalAccessException {
		realNode.setIndependentProperty(property, value);
	}

	public void setIndependentProperty(Property<?> property, Object value)
			throws InstantiationException, IllegalAccessException {
		realNode.setIndependentProperty(property, value);
	}

	public Object getDependentProperty(Class<? extends Property<?>> property)
			throws InstantiationException, IllegalAccessException {
		return realNode.getDependentProperty(property);
	}

	public Object getDependentProperty(Property<?> property) throws InstantiationException, IllegalAccessException {
		return realNode.getDependentProperty(property);
	}

	public void setDependentProperty(Class<? extends Property<?>> property, Object value)
			throws InstantiationException, IllegalAccessException {
		realNode.setDependentProperty(property, value);		
	}

	public void setDependentProperty(Property<?> property, Object value)
			throws InstantiationException, IllegalAccessException {
		realNode.setDependentProperty(property, value);
	}

	public void removeIndependentProperty(Class<? extends Property<?>> property)
			throws InstantiationException, IllegalAccessException {
		realNode.removeIndependentProperty(property);
	}

	public void removeIndependentProperty(Property<?> property) {
		realNode.removeIndependentProperty(property);
		
	}

	public void removeDependentProperty(Class<? extends Property<?>> property)
			throws InstantiationException, IllegalAccessException {
		realNode.removeDependentProperty(property);
	}

	public void removeDependentProperty(Property<?> property) {
		realNode.removeDependentProperty(property);
	}

	public AbstractMap<Property<?>, Object> getIndependentProperties() {
		return realNode.getIndependentProperties();
	}

	public AbstractMap<Property<?>, Object> getDependentProperties() {
		return realNode.getDependentProperties();
	}

	public Collection<Variable> getReadVariables() {
		return realNode.getReadVariables();
	}

	public boolean addReadVariable(Variable var) {
		return realNode.addReadVariable(var);
	}

	public boolean removeReadVariable(Variable var) {
		return realNode.removeReadVariable(var);
	}

	public Collection<Variable> getRemovableReadVariables() {
		return realNode.getRemovableReadVariables();
	}

	public boolean addRemovableReadVariable(Variable var) {
		return realNode.addRemovableReadVariable(var);
	}

	public boolean removeRemovableReadVariable(Variable var) {
		return realNode.removeRemovableReadVariable(var);
	}

	public Collection<Variable> getReadVariablesRecursive() {
		return realNode.getReadVariablesRecursive();
	}

	public Collection<Variable> getWrittenVariables() {
		return realNode.getWrittenVariables();
	}

	public boolean addWriteVariable(Variable var) {
		return realNode.addWriteVariable(var);
	}

	public boolean removeWriteVariable(Variable var) {
		return realNode.removeWriteVariable(var);
	}

	public Collection<Variable> getRemovableWrittenVariables() {
		return realNode.getRemovableWrittenVariables();
	}

	public boolean addRemovableWriteVariable(Variable var) {
		return realNode.addRemovableWriteVariable(var);
	}

	public boolean removeRemovableWriteVariable(Variable var) {
		return realNode.removeRemovableWriteVariable(var);
	}

	public Collection<Variable> getWrittenVariablesRecursive() {
		return realNode.getWrittenVariablesRecursive();
	}

	public void setProcessTree(ProcessTree tree) {
		realNode.setProcessTree(tree);
	}

	public ProcessTree getProcessTree() {
		return realNode.getProcessTree();
	}

	public Edge addParent(UUID id, Block parent, Expression expression) {
		return realNode.addParent(id, parent, expression);
	}

	public Edge addParent(Block parent, Expression expression) {
		return realNode.addParent(parent, expression);
	}

	public Edge addParent(Block parent) {
		return realNode.addParent(parent);
	}

	public Collection<Block> getParents() {
		return realNode.getParents();
	}

	public boolean removeIncomingEdge(Edge edge) {
		return realNode.removeIncomingEdge(edge);
	}

	public int numParents() {
		return realNode.numParents();
	}

	public List<Edge> getIncomingEdges() {
		return realNode.getIncomingEdges();
	}

	public void addIncomingEdge(Edge edge) {
		realNode.addIncomingEdge(edge);
	}

	public boolean isRoot() {
		return realNode.isRoot();
	}

	public String toStringShort() {
		return realNode.toStringShort();
	}

	public boolean isLeaf() {
		return realNode.isLeaf();
	}

	public String getLabel() {
		return realNode.getName();
	}
	
	public Object clone(){
		try {
			Constructor<? extends Node> constructor = realNode.getClass().getConstructor(realNode.getClass());
			Node clone = constructor.newInstance(realNode);
			return new NodeWrapper(clone);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			e1.printStackTrace();
		};
		return null;
	}
	
	public String toString(){
		return "wrap("+getName()+")";
	}

	public Node getRealNode() {
		return realNode;
	}
}
