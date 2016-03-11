//package org.processmining.plugins.logmodeltrust.heuristic.bp;
//
//import java.util.AbstractMap;
//import java.util.UUID;
//
//import org.jbpt.hypergraph.abs.IEntity;
//import org.processmining.plugins.properties.processmodel.PropertableElement;
//import org.processmining.plugins.properties.processmodel.Property;
//
///**
// * Common class for the different elements in the processtree, i.e., Nodes, Edges, Originators, Expressions, Variables, and the ProcessTree itself
// * All elements are identified by their ID
// * 
// * @author DSchunse
// *
// */
//public interface ProcessTreeElement extends PropertableElement, IEntity {
//	public UUID getID();
//
//	public String getName();
//	
//	public void setName(String name);
//	
//	/**
//	 * @param property the property of which we want the value
//	 * @return the independent property of which we want the value
//	 * @throws IllegalAccessException 
//	 * @throws InstantiationException 
//	 */
//	public Object getIndependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException;
//	
//	/**
//	 * @param property the property of which we want the value
//	 * @return the independent property of which we want the value
//	 * @throws IllegalAccessException 
//	 * @throws InstantiationException 
//	 */
//	public Object getIndependentProperty(Property<?> property) throws InstantiationException, IllegalAccessException;
//	
//	/**
//	 * @param property the property we want to set
//	 * @param value the value of the independent property we want to set
//	 * @throws IllegalAccessException 
//	 * @throws InstantiationException 
//	 */
//	public void setIndependentProperty(Class<? extends Property<?>> property, Object value) throws InstantiationException, IllegalAccessException;
//	
//	/**
//	 * @param property the property we want to set
//	 * @param value the value of the independent property we want to set
//	 * @throws IllegalAccessException 
//	 * @throws InstantiationException 
//	 */
//	public void setIndependentProperty(Property<?> property, Object value) throws InstantiationException, IllegalAccessException;
//	
//	/**
//	 * @param property the property of which we want the value
//	 * @return the dependent property of which we want the value
//	 * @throws IllegalAccessException 
//	 * @throws InstantiationException 
//	 */
//	public Object getDependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException;
//	
//	/**
//	 * @param property the property of which we want the value
//	 * @return the dependent property of which we want the value
//	 * @throws IllegalAccessException 
//	 * @throws InstantiationException 
//	 */
//	public Object getDependentProperty(Property<?> property) throws InstantiationException, IllegalAccessException;
//	
//	/**
//	 * @param property the property we want to set
//	 * @param value the value of the dependent property we want to set
//	 * @throws IllegalAccessException 
//	 * @throws InstantiationException 
//	 */
//	public void setDependentProperty(Class<? extends Property<?>> property, Object value) throws InstantiationException, IllegalAccessException;
//	
//	/**
//	 * @param property the property we want to set
//	 * @param value the value of the dependent property we want to set
//	 * @throws IllegalAccessException 
//	 * @throws InstantiationException 
//	 */
//	public void setDependentProperty(Property<?> property, Object value) throws InstantiationException, IllegalAccessException;
//	
//	public void removeIndependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException;
//	
//	public void removeIndependentProperty(Property<?> property);
//	
//	public void removeDependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException;
//	
//	public void removeDependentProperty(Property<?> property);
//	
//	/**
//	 * @return Gives all the independent properties
//	 */
//	public AbstractMap<Property<?>, Object> getIndependentProperties();
//	
//	/**
//	 * @return Gives all the dependent properties
//	 */
//	public AbstractMap<Property<?>, Object> getDependentProperties();
//}
