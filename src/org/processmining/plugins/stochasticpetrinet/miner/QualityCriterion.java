package org.processmining.plugins.stochasticpetrinet.miner;

public enum QualityCriterion {
    FITNESS, // measures log replay fitness, i.e., indicates the number traces that can be replayed correctly.
    PRECISION, // measures excess of modeled behavior over the one encountered in the log.
    PRECISION1,
    GENERALIZATION, // measures the chance of the model to account for new traces.
    GENERALIZATION1,
    SIMPLICITY, // measures the model complexity -> mostly counts the number of nodes and arcs in the model.
    FITNESS_TREE,
    PRECISION_TREE,
    GENERALIZATION_TREE,
    SIMPLICITY_TREE,

    // log - log distance:
    LOG_SIMILARITY,
    // model - model similarity:
    MODEL_SIMILARITY,

    // similarities between the profiles of M, M*, and M(L)
    SIM_M_MSTAR, SIM_MSTAR_MOFL, SIM_M_MOFL,

    // the string representation of the tree
    TREE_TRACE

}
