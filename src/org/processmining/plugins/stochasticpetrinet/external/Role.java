package org.processmining.plugins.stochasticpetrinet.external;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Role extends Entity implements Allocatable {

    private List<Role> canPerformRoles;
    private List<Role> canBePerformedByRoles;

    private List<Person> roleOwners;

    public Role(String name) {
        super(name);
        canPerformRoles = new ArrayList<Role>();
        canBePerformedByRoles = new ArrayList<Role>();
        roleOwners = new ArrayList<Person>();
    }

    public void addCanPerform(Role roleToPerform) {
        canPerformRoles.add(roleToPerform);
        roleToPerform.canBePerformedByRoles.add(this);
    }

    public List<Role> getCanBePerformedByRoles() {
        return ImmutableList.copyOf(canBePerformedByRoles);
    }

    public void addPersonToRole(Person person) {
        roleOwners.add(person);
    }

    public List<Person> getDirectPerformers() {
        return ImmutableList.copyOf(roleOwners);
    }

    public List<Person> getAllTransitivePerformers() {
        Set<Person> potentialPerformers = new HashSet<Person>();
        for (Role r : canBePerformedByRoles) {
            potentialPerformers.addAll(r.getAllTransitivePerformers());
        }
        potentialPerformers.addAll(roleOwners);
        return new ArrayList<Person>(potentialPerformers);
    }

    /**
     * Roles cannot perform anything by themselves!
     */
    public int getCapacity() {
        return 0;
    }
}
