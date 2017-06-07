package org.processmining.plugins.stochasticpetrinet.measures;

import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.*;

public class MeasureConfig {

    /**
     * The abstraction to apply to a trace of events
     */
    private Collection<AbstractionLevel> abstractionLevels;

    private Collection<MeasureProvider> measureProviders;

    public MeasureConfig() {
        this(getAbstractionLevelsDynamically());
    }

    public MeasureConfig(MeasureProvider... providers) {
        this(getAbstractionLevelsDynamically(), providers);
    }

    /**
     * Creates a configuration with only a given abstraction level
     *
     * @param levels
     */
    public MeasureConfig(AbstractionLevel... levels) {
        this(levels, getMeasureProvidersDynamically());
    }

    public MeasureConfig(AbstractionLevel[] levels, MeasureProvider... providers) {
        this.abstractionLevels = Arrays.asList(levels);
        this.measureProviders = Arrays.asList(providers);
    }


    private static MeasureProvider[] getMeasureProvidersDynamically() {
        Reflections reflections = new Reflections("org.processmining.plugins.stochasticpetrinet.measures");
        Set<Class<? extends MeasureProvider>> subTypes =
                reflections.getSubTypesOf(MeasureProvider.class);
        Vector<MeasureProvider> providers = new Vector<MeasureProvider>();
        for (Class<? extends MeasureProvider> providerClass : subTypes) {
            try {
                if (!Modifier.isAbstract(providerClass.getModifiers())) {
                    providers.add(providerClass.newInstance());
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return providers.toArray(new MeasureProvider[providers.size()]);
    }

    private static AbstractionLevel[] getAbstractionLevelsDynamically() {
        Reflections reflections = new Reflections("org.processmining.plugins.stochasticpetrinet.measures");
        Set<Class<? extends AbstractionLevel>> subTypes =
                reflections.getSubTypesOf(AbstractionLevel.class);
        AbstractionLevel[] levels = new AbstractionLevel[subTypes.size()];
        int i = 0;
        for (Class<? extends AbstractionLevel> levelClass : subTypes) {
            try {
                levels[i++] = levelClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        Arrays.sort(levels);
        return levels;
    }


    public Collection<AbstractionLevel> getAbstractionLevels() {
        return abstractionLevels;
    }

    public void setAbstractionLevel(Collection<AbstractionLevel> abstractionLevels) {
        this.abstractionLevels = abstractionLevels;
    }

    public Collection<MeasureProvider> getMeasureProviders() {
        return measureProviders;
    }

    public void setMeasureProviders(Collection<MeasureProvider> measureProviders) {
        this.measureProviders = measureProviders;
    }


}
