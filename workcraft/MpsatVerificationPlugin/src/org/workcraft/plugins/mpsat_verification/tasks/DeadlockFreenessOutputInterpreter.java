package org.workcraft.plugins.mpsat_verification.tasks;

import org.workcraft.Framework;
import org.workcraft.gui.MainWindow;
import org.workcraft.gui.dialogs.ReachabilityDialog;
import org.workcraft.plugins.mpsat_verification.utils.CompositionUtils;
import org.workcraft.plugins.mpsat_verification.utils.OutcomeUtils;
import org.workcraft.plugins.pcomp.ComponentData;
import org.workcraft.plugins.pcomp.tasks.PcompOutput;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.utils.PetriUtils;
import org.workcraft.plugins.stg.Signal;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.StgModel;
import org.workcraft.tasks.ExportOutput;
import org.workcraft.traces.Solution;
import org.workcraft.traces.Trace;
import org.workcraft.utils.LogUtils;
import org.workcraft.workspace.WorkspaceEntry;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

class DeadlockFreenessOutputInterpreter extends ReachabilityOutputInterpreter {

    DeadlockFreenessOutputInterpreter(WorkspaceEntry we, ExportOutput exportOutput,
            PcompOutput pcompOutput, MpsatOutput mpsatOutput, boolean interactive) {

        super(we, exportOutput, pcompOutput, mpsatOutput, interactive);
    }

    @Override
    public String extendMessage(String message) {
        return "<html><br>&#160;" + message + " after the following trace(s):<br></html>";
    }

    @Override
    public void reportSolutions(String message, List<Solution> solutions) {
        Framework framework = Framework.getInstance();
        if (isInteractive() && framework.isInGuiMode()) {
            MainWindow mainWindow = framework.getMainWindow();
            ReachabilityDialog solutionsDialog = new ReachabilityDialog(
                    mainWindow, getWorkspaceEntry(), OutcomeUtils.TITLE, message, solutions);

            solutionsDialog.reveal();
        }
    }

    @Override
    public List<Solution> processSolutions(List<Solution> solutions) {
        List<Solution> result = new LinkedList<>();

        StgModel stg = getStg();
        ComponentData data = getComponentData();

        for (Solution solution : solutions) {
            Trace trace = solution.getMainTrace();
            LogUtils.logMessage("Processing reported trace: " + trace);
            Trace projectedTrace = CompositionUtils.projectTrace(trace, data);
            // Execute trace to potentially interesting state
            HashMap<Place, Integer> marking = PetriUtils.getMarking(stg);
            if (!PetriUtils.fireTrace(stg, projectedTrace)) {
                PetriUtils.setMarking(stg, marking);
                throw new RuntimeException("Cannot execute projected trace: " + projectedTrace);
            }
            // Check if any output can be fired that is not enabled in the composition
            boolean isConformantTrace = true;
            for (SignalTransition transition : stg.getSignalTransitions()) {
                Signal.Type type = transition.getSignalType();
                if (stg.isEnabled(transition) && (type == Signal.Type.OUTPUT)) {
                    String signal = stg.getSignalReference(transition);
                    LogUtils.logWarning("Deadlock trace is spurious because it leads to non-conformant output '" + signal + "'");
                    isConformantTrace = false;
                    break;
                }
            }
            if (isConformantTrace) {
                result.add(new Solution(projectedTrace));
            }
            PetriUtils.setMarking(stg, marking);
        }
        return result;
    }

    @Override
    public Boolean interpret() {
        if (getOutput() == null) {
            return null;
        }
        boolean propertyHolds = !getOutput().hasSolutions();
        if (propertyHolds) {
            OutcomeUtils.showOutcome(true, "The system is deadlock-free", isInteractive());
        } else {
            List<Solution> solutions = getOutput().getSolutions();
            List<Solution> processedSolutions = processSolutions(solutions);
            if (processedSolutions.isEmpty()) {
                OutcomeUtils.showOutcome(false,
                        "Deadlock freeness cannot be reliably verified because of conformation violation",
                        isInteractive());

                return null;
            }
            String message = "The system has a deadlock";
            OutcomeUtils.logOutcome(false, message);
            reportSolutions(extendMessage(message), processedSolutions);
        }
        return propertyHolds;
    }

}
