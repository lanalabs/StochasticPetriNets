package org.processmining.plugins.stochasticpetrinet.analyzer.export;

import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatistics;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Plugin(name = ".dat export for R (Log-likelihoods)", returnLabels = {}, returnTypes = {}, parameterLabels = {CaseStatisticsList.PARAMETER_LABEL,
        "File"}, userAccessible = true)
@UIExportPlugin(description = "List of Log-likelihoods (one per row)", extension = "dat")
public class ExportLogLikelihoodPlugin {

    @PluginVariant(variantLabel = ".dat export for R", requiredParameterLabels = {0, 1})
    public void exportPetriNetToPNMLFile(PluginContext context, CaseStatisticsList likelihoods, File file) throws Exception {
        if (file.canWrite()) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (CaseStatistics caseStatistics : likelihoods) {
                writer.write(caseStatistics.toString() + "\n");
            }
            writer.flush();
            writer.close();
        } else {
            throw new IOException("Cannot write to file " + file.getAbsolutePath());
        }
    }

}
