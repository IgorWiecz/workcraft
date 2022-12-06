package org.workcraft.plugins.circuit.commands;

import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.utils.ScanUtils;
import org.workcraft.utils.WorkspaceUtils;
import org.workcraft.workspace.WorkspaceEntry;

// This is not a test class, the matching name pattern is just a coincidence
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestableGateInsertionCommand extends AbstractInsertionCommand {

    @Override
    public String getDisplayName() {
        return "Insert testable buffers/inverters for path breaker components";
    }

    @Override
    public void insert(WorkspaceEntry we) {
        we.saveMemento();
        VisualCircuit circuit = WorkspaceUtils.getAs(we, VisualCircuit.class);
        ScanUtils.insertTestableGates(circuit);
    }

}
