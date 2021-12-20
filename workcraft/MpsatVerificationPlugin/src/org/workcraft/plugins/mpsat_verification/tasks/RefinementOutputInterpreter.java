package org.workcraft.plugins.mpsat_verification.tasks;

import org.workcraft.plugins.mpsat_verification.projection.Enabledness;
import org.workcraft.plugins.mpsat_verification.utils.CompositionUtils;
import org.workcraft.plugins.pcomp.ComponentData;
import org.workcraft.plugins.pcomp.CompositionData;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.tasks.ExportOutput;
import org.workcraft.traces.Solution;
import org.workcraft.traces.Trace;
import org.workcraft.utils.LogUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class RefinementOutputInterpreter extends AbstractCompositionOutputInterpreter {

    RefinementOutputInterpreter(WorkspaceEntry we, ExportOutput exportOutput,
            PcompOutput pcompOutput, MpsatOutput mpsatOutput, boolean interactive) {

        super(we, exportOutput, pcompOutput, mpsatOutput, interactive);
    }

    @Override
    public List<Solution> processSolutions(List<Solution> solutions) {
        String title = getWorkspaceEntry().getTitle();
        CompositionData compositionData = getCompositionData();
        ComponentData implementationData = compositionData.getComponentData(0);
        ComponentData specificationData = compositionData.getComponentData(1);

        HashSet<String> visitedTraces = new HashSet<>();
        boolean needsMultiLineMessage = solutions.size() > 1;
        if (needsMultiLineMessage) {
            LogUtils.logMessage("Unique projection(s) to '" + title + "':");
        }

        List<Solution> result = new LinkedList<>();
        for (Solution solution : solutions) {
            // Get unique projection trace
            Trace implementationTrace = CompositionUtils.projectTrace(solution.getMainTrace(), implementationData);
            String traceText = implementationTrace.toString();
            if (!visitedTraces.contains(traceText)) {
                visitedTraces.add(traceText);
                if (needsMultiLineMessage) {
                    LogUtils.logMessage("  " + traceText);
                } else {
                    LogUtils.logMessage("Projection to '" + title + "': " + traceText);
                }

                Set<Trace> compositionContinuations = solution.getContinuations();
                Enabledness implementationEnabledness = CompositionUtils.getEnabledness(compositionContinuations, implementationData);
                Enabledness specificationEnabledness = CompositionUtils.getEnabledness(compositionContinuations, specificationData);

                Set<String> unexpectedlyEnabledEvents = new HashSet<>(implementationEnabledness.keySet());
                unexpectedlyEnabledEvents.removeAll(specificationEnabledness.keySet());
                result.addAll(CompositionUtils.getEnabledViolatorSolutions(implementationTrace, unexpectedlyEnabledEvents,
                        implementationEnabledness));

                Set<String> unexpectedlyDisabledSignals = new HashSet<>(specificationEnabledness.keySet());
                unexpectedlyDisabledSignals.removeAll(implementationEnabledness.keySet());
                result.addAll(CompositionUtils.getDisabledViolatorSolutions(implementationTrace, unexpectedlyDisabledSignals));
            }
        }
        return result;
    }

}
