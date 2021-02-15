package org.workcraft.plugins.petrify.tasks;

import org.workcraft.Framework;
import org.workcraft.dom.ModelDescriptor;
import org.workcraft.gui.dialogs.ExceptionDialog;
import org.workcraft.plugins.petri.PetriDescriptor;
import org.workcraft.plugins.petri.PetriModel;
import org.workcraft.plugins.stg.*;
import org.workcraft.plugins.stg.converters.StgToPetriConverter;
import org.workcraft.plugins.stg.utils.MutexUtils;
import org.workcraft.tasks.AbstractResultHandlingMonitor;
import org.workcraft.tasks.Result;
import org.workcraft.utils.DialogUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

import java.util.Collection;

public class TransformationResultHandlingMonitor extends AbstractResultHandlingMonitor<TransformationOutput, WorkspaceEntry> {
    private final WorkspaceEntry we;
    private final boolean convertToPetriNet;
    private final Collection<Mutex> mutexes;

    public TransformationResultHandlingMonitor(WorkspaceEntry we, boolean convertToPetriNet, Collection<Mutex> mutexes) {
        this.we = we;
        this.convertToPetriNet = convertToPetriNet;
        this.mutexes = mutexes;
    }

    @Override
    public WorkspaceEntry handle(final Result<? extends TransformationOutput> result) {
        WorkspaceEntry weResult = null;
        TransformationOutput output = result.getPayload();
        if (result.isSuccess()) {
            StgModel stgModel = output.getStg();
            MutexUtils.restoreMutexSignals(stgModel, mutexes);
            MutexUtils.restoreMutexPlacesByContext(stgModel, mutexes);
            PetriModel model = convertToPetriNet ? convertStgToPetriNet(stgModel) : stgModel;
            ModelDescriptor modelDescriptor = convertToPetriNet ? new PetriDescriptor() : new StgDescriptor();
            model.setTitle(we.getModelTitle());
            ModelEntry me = new ModelEntry(modelDescriptor, model);
            Framework framework = Framework.getInstance();
            weResult = framework.createWork(me, we.getFileName());
        } else if (result.isFailure()) {
            if (result.getCause() != null) {
                ExceptionDialog.show(result.getCause());
            } else {
                DialogUtils.showWarning("Transformation failed. Petrify output: \n\n" + output.getErrorsHeadAndTail());
            }
        }
        return weResult;
    }

    private PetriModel convertStgToPetriNet(StgModel srcModel) {
        StgToPetriConverter converter = new StgToPetriConverter(new VisualStg((Stg) srcModel));
        return converter.getDstModel().getMathModel();
    }

}
