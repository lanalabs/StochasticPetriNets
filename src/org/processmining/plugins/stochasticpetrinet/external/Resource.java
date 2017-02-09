package org.processmining.plugins.stochasticpetrinet.external;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public abstract class Resource extends Entity implements Allocatable {

    private List<Role> roles;

    public Resource(String name) {
        super(name);
        this.roles = new ArrayList<Role>();
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public void removeRole(Role role) {
        roles.remove(role);
    }

    public List<Role> getRoles() {
        return ImmutableList.copyOf(roles);
    }


}
