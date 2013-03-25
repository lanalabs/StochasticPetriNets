Added import + export of simple stochastic Petri nets supported by the simple framework.

To enrich a Petri net with stochastic data and turn it into a StochasticNet, you need to run the "Replay for Performance analysis" plugin by Arya Adriansyah first.
The manifest can then be used as input to the plugin.

Usually, these steps are necessary:

- Load a log file containing timestamps
- load a corresponding Petri net (or mine it with a good Petri net miner - works well for artificial logs or high quality logs produced by a workflow engine)
- define the final marking in the Petri net that indicate termination of the process
- define visibility of transitions in the Petri net to hide transitions not reflected in the log

- Run the "Replay for performance analysis" plugin
- Use the output as input to the "Enrich Petri net with stochastic data" plugin
- Select type of distributions, that should be approximated based on the observations
- Select the time units that shall be captured by the paremeters (e.g., mean of exponential distribution)
- View the resulting StochasticNet in the "StochasticNet Visualizer" and see the distributions.

- Save the enriched Model as "(stochastic) Petri net" in the .pnml format for later use. 