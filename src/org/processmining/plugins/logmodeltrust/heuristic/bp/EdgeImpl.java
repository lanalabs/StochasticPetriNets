//package org.processmining.plugins.logmodeltrust.heuristic.bp;
//
//import java.util.AbstractMap;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.UUID;
//
//import org.processmining.plugins.properties.processmodel.Property;
//import org.processmining.processtree.Block;
//import org.processmining.processtree.Edge;
//import org.processmining.processtree.Expression;
//import org.processmining.processtree.Node;
//import org.processmining.processtree.Variable;
//
//public class EdgeImpl extends ProcessTreeElementImpl implements Edge {
//
//	public static final Expression NOEXPRESSION = new Expression() {
//
//		public UUID getID() {
//			return null;
//		}
//
//		public String getName() {
//			return "";
//		}
//		
//		public void setName(String name){
//			
//		}
//		
//		public Collection<Variable> getVariables() {
//			return Collections.emptyList();
//		}
//
//		public String getExpression() {
//			return "";
//		}
//
//		public String toString() {
//			return "";
//		}
//		
//		public boolean equals(Object o) {
//			return (o instanceof Expression) && ((Expression)o).getID() == null;
//		}
//		
//		public boolean addVariable(Variable var) {
//			return false;
//		}
//
//		public boolean removeVariable(Variable var) {
//			return false;
//		}
//
//		public Object getIndependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException {
//			return null;
//		}
//		
//		public Object getIndependentProperty(Property<?> property) throws InstantiationException, IllegalAccessException {
//			return null;
//		}
//
//		public void setIndependentProperty(Class<? extends Property<?>> property, Object value)	throws InstantiationException, IllegalAccessException {
//			
//		}
//		
//		public void setIndependentProperty(Property<?> property, Object value)	throws InstantiationException, IllegalAccessException {
//			
//		}
//
//		public Object getDependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException {
//			return null;
//		}
//		
//		public Object getDependentProperty(Property<?> property) throws InstantiationException, IllegalAccessException {
//			return null;
//		}
//
//		public void setDependentProperty(Class<? extends Property<?>> property, Object value) throws InstantiationException, IllegalAccessException {			
//		}
//		
//		public void setDependentProperty(Property<?> property, Object value) throws InstantiationException, IllegalAccessException {			
//		}
//		
//		public void removeIndependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException{
//		}
//		
//		public void removeIndependentProperty(Property<?> property){
//		}
//		
//		public void removeDependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException{
//		}
//		
//		public void removeDependentProperty(Property<?> property){
//		}
//
//		public AbstractMap<Property<?>, Object> getIndependentProperties() {
//			return new HashMap<Property<?>, Object>();
//		}
//
//		public AbstractMap<Property<?>, Object> getDependentProperties() {
//			return new HashMap<Property<?>, Object>();
//		}
//		
//		public Object clone() {
//			return null;
//		}
//		
//		public String getLabel() {
//			return this.getName();
//		}
//	};
//
//	private Block source;
//	private Node target;
//	private Expression expression;
//	private boolean hideable;
//	private boolean blockable;
//	private Set<Expression> expressions;
//	private Set<Expression> remExpressions;
//
//	public EdgeImpl(Block source, Node target, Expression expression) {
//		this(UUID.randomUUID(), source, target, expression, false, false);
//	}
//
//	public EdgeImpl(Block source, Node target, Expression expression, boolean blockable, boolean hideable) {
//		this(UUID.randomUUID(), source, target, expression, blockable, hideable);
//	}
//
//	public EdgeImpl(UUID id, Block source, Node target, Expression expression) {
//		this(id, source, target, expression, false, false);
//	}
//
//	public EdgeImpl(UUID id, Block source, Node target, Expression expression, boolean blockable, boolean hideable) {
//		super(id, source.getName() + " -> " + target.getName());
//		assert source.getProcessTree() == target.getProcessTree();
//		this.source = source;
//		this.target = target;
//		source.addOutgoingEdge(this);
//		target.addIncomingEdge(this);
//		this.expression = expression;
//		this.blockable = blockable;
//		this.hideable = hideable;
//		this.expressions = new HashSet<Expression>();
//		this.remExpressions = new HashSet<Expression>();
//	}
//	
//	public EdgeImpl(Edge e, Block source, Node target, Expression expression){
//		super(e);
//		assert source.getProcessTree() == target.getProcessTree();
//		this.source = source;
//		this.target = target;
//		this.expression = expression;
//		this.blockable = e.isBlockable();
//		this.hideable = e.isHideable();
//		this.expressions = new HashSet<Expression>();
//		this.remExpressions = new HashSet<Expression>();
//	}
//
//	public EdgeImpl(Block source, Node target, boolean blockable, boolean hideable) {
//		this(source, target, NOEXPRESSION, blockable, hideable);
//	}
//
//	public EdgeImpl(Block source, Node target) {
//		this(source, target, NOEXPRESSION);
//	}
//
//	@Override
//	public Block getSource() {
//		return source;
//	}
//	
//	public void setSource(Block source){
//		assert source.getProcessTree() == target.getProcessTree();
//		this.source = source;
//		source.addOutgoingEdge(this);
//	}
//
//	@Override
//	public Node getTarget() {
//		return target;
//	}
//	
//	public void setTarget(Node target){
//		assert source.getProcessTree() == target.getProcessTree();
//		this.target = target;
//		target.addIncomingEdge(this);
//	}
//
//	@Override
//	public Expression getExpression() {
//		return expression;
//	}
//	
//	@Override
//	public void setExpression(Expression expression){
//		this.expression = expression;
//	}
//	
//	public Collection<Expression> getExpressions(){
//		return expressions;
//	}
//	
//	public Collection<Expression> getRemovableExpressions(){
//		return remExpressions;
//	}
//
//	@Override
//	public boolean isBlockable() {
//		return blockable;
//	}
//
//	@Override
//	public boolean isHideable() {
//		return hideable;
//	}
//
//	public String toString() {
//		return source.toString() + " -> " + target.toString();
//	}
//
//	public boolean hasExpression() {
//		return !NOEXPRESSION.equals(expression);
//	}
//
//	public void setBlockable(boolean blockable) {
//		this.blockable = blockable;
//	}
//
//	public void setHideable(boolean hideable) {
//		this.hideable = hideable;
//	}
//}
