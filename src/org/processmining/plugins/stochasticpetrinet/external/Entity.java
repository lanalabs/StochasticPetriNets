package org.processmining.plugins.stochasticpetrinet.external;

/**
 * General superclass of everything that can be related to process models.
 */
public abstract class Entity {
    private String name;

    public Entity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
