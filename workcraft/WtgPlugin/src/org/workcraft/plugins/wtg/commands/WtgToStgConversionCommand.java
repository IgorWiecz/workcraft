package org.workcraft.plugins.wtg.commands;

import org.workcraft.commands.AbstractConversionCommand;
import org.workcraft.plugins.stg.StgDescriptor;
import org.workcraft.plugins.wtg.Wtg;
import org.workcraft.plugins.wtg.converter.WtgToStgConverter;
import org.workcraft.plugins.wtg.utils.VerificationUtils;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.ModelEntry;
import org.workcraft.workspace.WorkspaceEntry;

public class WtgToStgConversionCommand extends AbstractConversionCommand {

    @Override
    public Position getPosition() {
        return Position.TOP;
    }

    @Override
    public String getDisplayName() {
        return "Signal Transition Graph";
    }

    @Override
    public boolean isApplicableTo(WorkspaceEntry we) {
        return WorkspaceUtils.isApplicable(we, Wtg.class);
    }

    @Override
    public ModelEntry convert(ModelEntry me) {
        final Wtg wtg = me.getAs(Wtg.class);
        ModelEntry result = null;
        if (VerificationUtils.checkStructure(wtg) && VerificationUtils.checkNameCollisions(wtg)) {
            final WtgToStgConverter converter = new WtgToStgConverter(wtg);
            result = new ModelEntry(new StgDescriptor(), converter.getDstModel());
        }
        return result;
    }

}
