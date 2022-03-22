package org.workcraft.plugins.cpog.tasks;

import org.workcraft.Framework;
import org.workcraft.dom.math.MathModel;
import org.workcraft.exceptions.VisualModelInstantiationException;
import org.workcraft.gui.Toolbox;
import org.workcraft.plugins.cpog.CpogDescriptor;
import org.workcraft.plugins.cpog.VisualCpog;
import org.workcraft.plugins.cpog.VisualCpogDescriptor;
import org.workcraft.plugins.cpog.tools.CpogSelectionTool;
import org.workcraft.tasks.BasicProgressMonitor;
import org.workcraft.tasks.ExternalProcessOutput;
import org.workcraft.tasks.Result;
import org.workcraft.utils.DialogUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class PGMinerResultHandler extends BasicProgressMonitor<ExternalProcessOutput> {

    private VisualCpog visualCpog;
    private final WorkspaceEntry we;
    private final boolean createNewWindow;
    private WorkspaceEntry weResult;

    public PGMinerResultHandler(final VisualCpog visualCpog, final WorkspaceEntry we, final boolean createNewWindow) {
        this.visualCpog = visualCpog;
        this.we = we;
        this.createNewWindow = createNewWindow;
        this.weResult = null;
    }

    @Override
    public void isFinished(final Result<? extends ExternalProcessOutput> result) {
        super.isFinished(result);
        if (result.isFailure()) {
            DialogUtils.showError("PGMiner could not run, concurrency extraction failed.");
        } else {
            try {
                ExternalProcessOutput output = result.getPayload();
                SwingUtilities.invokeAndWait(() -> handleSuccess(output));
            } catch (InvocationTargetException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSuccess(ExternalProcessOutput output) {
        final Framework framework = Framework.getInstance();
        if (createNewWindow) {
            final CpogDescriptor cpogModel = new CpogDescriptor();
            final MathModel mathModel = cpogModel.createMathModel();
            final VisualCpogDescriptor vmd = cpogModel.getVisualModelDescriptor();
            try {
                visualCpog = vmd.create(mathModel);
                final ModelEntry me = new ModelEntry(cpogModel, visualCpog);
                weResult = framework.createWork(me, we.getFileName());
            } catch (final VisualModelInstantiationException e) {
                DialogUtils.showError(e.getMessage());
            }
        }
        final String[] stdout = output.getStdoutString().split("\n");

        we.captureMemento();
        try {
            final Toolbox toolbox = framework.getMainWindow().getCurrentToolbox();
            final CpogSelectionTool tool = toolbox.getToolInstance(CpogSelectionTool.class);
            for (String exp : stdout) {
                tool.insertExpression(exp, visualCpog, false, true, false);
            }
            we.saveMemento();
        } catch (final Exception e) {
            we.cancelMemento();
        }
    }

    public WorkspaceEntry getResult() {
        return weResult;
    }

}

