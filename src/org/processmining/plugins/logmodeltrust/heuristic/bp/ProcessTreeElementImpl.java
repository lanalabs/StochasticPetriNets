//package org.processmining.plugins.logmodeltrust.heuristic.bp;
//
//import java.util.AbstractMap;
//import java.util.HashMap;
//import java.util.UUID;
//
//import org.processmining.plugins.properties.processmodel.Property;
//
//public abstract class ProcessTreeElementImpl implements ProcessTreeElement {
//	protected UUID id = null;
//	protected String name = null;
//	protected HashMap<Property<?>, Object> propertyIndependent = null;
//	protected HashMap<Property<?>, Object> propertyDependent = null;
//	
//	public UUID getID() {
//		return id;
//	}
//	
//	public String getName() {
//		return name;
//	}
//	
//	public void setName(String name){
//		this.name = name;
//	}
//	
//	public ProcessTreeElementImpl(){
//		this(UUID.randomUUID());
//	}
//	
//	public ProcessTreeElementImpl(UUID id){
//		this(id, id.toString());
//	}
//	
//	public ProcessTreeElementImpl(UUID id, String name){
//		this.id = id;
//		this.name = name;
//		this.propertyIndependent = new HashMap<Property<?>, Object>();
//		this.propertyDependent = new HashMap<Property<?>, Object>();
//	}
//	
//	public ProcessTreeElementImpl(UUID id, ProcessTreeElement elem){
//		this.id = id;
//		this.name = elem.getName();
//		this.propertyIndependent = new HashMap<Property<?>, Object>();
//		this.propertyDependent = new HashMap<Property<?>, Object>();
//		// now clone the properties
//		if(elem.getIndependentProperties() != null){
//			for(Property<?> prop: elem.getIndependentProperties().keySet()){
//				this.propertyIndependent.put(prop, elem.getIndependentProperties().get(prop));
//			}
//		}
//		if(elem.getDependentProperties() != null){
//			for(Property<?> prop: elem.getDependentProperties().keySet()){
//				this.propertyDependent.put(prop, elem.getDependentProperties().get(prop));
//			}
//		}
//	}
//	
//	public ProcessTreeElementImpl(ProcessTreeElement elem){
//		this(elem.getID(), elem);
//	}
//	
//	public Object getIndependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException{
//		for(Property<?> prop: propertyIndependent.keySet()){
//			if(property.isInstance(prop)){
//				return propertyIndependent.get(prop);
//			}
//		}
//		return null;
//	}
//	
//	public Object getIndependentProperty(Property<?> property) throws InstantiationException, IllegalAccessException{
//		for(Property<?> prop: propertyIndependent.keySet()){
//			if(property.equals(prop)){
//				return propertyIndependent.get(prop);
//			}
//		}
//		return null;
//	}
//	
//	public void setIndependentProperty(Class<? extends Property<?>> property, Object value) throws InstantiationException, IllegalAccessException{
//		for(Property<?> prop: propertyIndependent.keySet()){
//			if(property.isInstance(prop)){
//				propertyIndependent.put(prop, value);
//				return;
//			}
//		}
//		// we do not have this property yet so add it
//		propertyIndependent.put(property.newInstance(), value);	
//	}
//	
//	
//	public void setIndependentProperty(Property<?> property, Object value) throws InstantiationException, IllegalAccessException{
//		for(Property<?> prop: propertyIndependent.keySet()){
//			if(property.equals(prop)){
//				propertyIndependent.put(prop, value);
//				return;
//			}
//		}
//		// we do not have this property yet so add it
//		propertyIndependent.put(property, value);	
//	}
//	
//	public Object getDependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException{
//		for(Property<?> prop: propertyDependent.keySet()){
//			if(property.isInstance(prop)){
//				return propertyDependent.get(prop);
//			}
//		}
//		return null;
//	}
//	
//	public Object getDependentProperty(Property<?> property) throws InstantiationException, IllegalAccessException{
//		for(Property<?> prop: propertyDependent.keySet()){
//			if(property.equals(prop)){
//				return propertyDependent.get(prop);
//			}
//		}
//		return null;
//	}
//	
//	public void setDependentProperty(Class<? extends Property<?>> property, Object value) throws InstantiationException, IllegalAccessException{
//		for(Property<?> prop: propertyDependent.keySet()){
//			if(property.isInstance(prop)){
//				propertyDependent.put(prop, value);
//				return;
//			}
//		}
//		// we do not have this property yet so add it
//		propertyDependent.put(property.newInstance(), value);
//	}
//	
//	public void setDependentProperty(Property<?> property, Object value) throws InstantiationException, IllegalAccessException{
//		for(Property<?> prop: propertyDependent.keySet()){
//			if(property.equals(prop)){
//				propertyDependent.put(prop, value);
//				return;
//			}
//		}
//		// we do not have this property yet so add it
//		propertyDependent.put(property, value);
//	}
//	
//	public void removeIndependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException{
//		propertyIndependent.remove(property.newInstance());
//	}
//	
//	public void removeIndependentProperty(Property<?> property){
//		propertyIndependent.remove(property);
//	}
//	
//	public void removeDependentProperty(Class<? extends Property<?>> property) throws InstantiationException, IllegalAccessException{
//		propertyDependent.remove(property.newInstance());
//	}
//	
//	public void removeDependentProperty(Property<?> property){
//		propertyDependent.remove(property);
//	}
//	
//	public AbstractMap<Property<?>, Object> getIndependentProperties(){
//		return propertyIndependent;
//	}
//	
//	public AbstractMap<Property<?>, Object> getDependentProperties(){
//		return propertyDependent;
//	}
//	
//	public boolean equals(Object o) {
//		return o instanceof ProcessTreeElement ? ((ProcessTreeElement) o).getID().equals(id) : false;
//	}
//	
//	public int hashCode() {
//		return id.hashCode();
//	}
//	
//	public String toString(){
//		return name;
//	}
//	
//	public Object clone() {
//		return null;
//	}
//	
//	public String getLabel() {
//		return this.getName();
//	}
//
//
//
//}
