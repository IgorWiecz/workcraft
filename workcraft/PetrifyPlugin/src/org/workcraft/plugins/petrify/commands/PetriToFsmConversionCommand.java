package org.workcraft.plugins.petrify.commands;

import org.workcraft.Framework;
import org.workcraft.commands.AbstractConversionCommand;
import org.workcraft.dom.math.MathModel;
import org.workcraft.gui.dialogs.ExceptionDialog;
import org.workcraft.plugins.fsm.FsmDescriptor;
import org.workcraft.plugins.fst.VisualFst;
import org.workcraft.plugins.fst.converters.FstToFsmConverter;
import org.workcraft.plugins.petri.Petri;
import org.workcraft.plugins.petrify.tasks.WriteSgConversionOutput;
import org.workcraft.plugins.petrify.tasks.WriteSgConversionTask;
import org.workcraft.tasks.Result;
import org.workcraft.tasks.TaskManager;
import org.workcraft.utils.DialogUtils;
import org.workcraft.utils.Hierarchy;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

public class PetriToFsmConversionCommand extends AbstractConversionCommand {

    @Override
    public String getDisplayName() {
        return "Finite State Machine [Petrify]";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Petri.class);
    }

    @Override
    public WorkspaceEntry execute(WorkspaceEntry we) {
        if (Hierarchy.isHierarchical(we.getModelEntry())) {
            DialogUtils.showError("Finite State Machine cannot be derived from a hierarchical Petri Net.",
                    "Conversion error");
            return null;
        }
        TaskManager taskManager = Framework.getInstance().getTaskManager();
        WriteSgConversionTask task = new WriteSgConversionTask(we, false);
        Result<? extends WriteSgConversionOutput> result = taskManager.execute(task, "Building state graph");
        return processResult(result, we.getFileName());
    }

    private WorkspaceEntry processResult(Result<? extends WriteSgConversionOutput> result, String name) {
        WorkspaceEntry we = null;
        WriteSgConversionOutput output = result.getPayload();
        if (result.isSuccess()) {
            FstToFsmConverter converter = new FstToFsmConverter(new VisualFst(output.getFst()));
            MathModel model = converter.getDstModel().getMathModel();
            ModelEntry me = new ModelEntry(new FsmDescriptor(), model);
            we = Framework.getInstance().createWork(me, name);
        } else if (result.isFailure()) {
            if (result.getCause() != null) {
                ExceptionDialog.show(result.getCause());
            } else {
                DialogUtils.showWarning("Petrify output:\n" + output.getErrorsHeadAndTail());
            }
        }
        return we;
    }

    @Override
    public ModelEntry convert(ModelEntry me) {
        return null; // Conversion is performed by Petrify backend
    }

}
