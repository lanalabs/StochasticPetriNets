package org.processmining.plugins.stochasticpetrinet.enricher;

import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.Map;

public class TimeConstraintsPanel extends ProMPropertiesPanel {
    private static final long serialVersionUID = -7528603749293206893L;

    private Map<Transition, ProMComboBox<DistributionType>> selectedTypes;

    public TimeConstraintsPanel(String title, PetrinetGraph net) {
        super(title);
        this.init(net);
    }

    private void init(PetrinetGraph net) {
        this.addComboBox("General strategy/assumption", TimeConstraints.TimeAssumption.values());

        this.selectedTypes = new HashMap<Transition, ProMComboBox<DistributionType>>();
        for (Transition t : net.getTransitions()) {
            String tName = t.getLabel();
            selectedTypes.put(t, this.addComboBox(tName, DistributionType.values()));
        }
    }

    public void updateConstraints(TimeConstraints constraints) {
        for (Transition t : constraints.getTransitionContraints().keySet()) {
            selectedTypes.get(t).setSelectedItem(constraints.getTransitionContraints().get(t));
        }

    }

}
