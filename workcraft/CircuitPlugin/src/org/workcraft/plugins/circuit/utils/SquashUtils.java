package org.workcraft.plugins.circuit.utils;

import org.workcraft.dom.Container;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.references.Identifier;
import org.workcraft.dom.visual.ConnectionHelper;
import org.workcraft.dom.visual.Replica;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.VisualFunctionComponent;
import org.workcraft.plugins.circuit.VisualFunctionContact;
import org.workcraft.plugins.circuit.VisualReplicaContact;
import org.workcraft.utils.LogUtils;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public final class SquashUtils {

    private SquashUtils() {
    }

    public static boolean checkInterfaceConsistency(VisualCircuit circuit, VisualFunctionComponent component,
            VisualCircuit componentModel) {

        boolean result = true;
        String componentRef = circuit.getMathModel().getComponentReference(component.getReferencedComponent());
        if (RefinementUtils.hasInconsistentSignalNames(component.getReferencedComponent(), componentModel.getMathModel())) {
            result = false;
            LogUtils.logWarning("Inconsistent interface signals for component '" + componentRef
                    + "' and its circuit implementation");
        }

        Map<String, Boolean> componentState = RefinementUtils.getComponentInterfaceInitialState(
                circuit.getMathModel(), component.getReferencedComponent());
        Map<String, Boolean> modelState = RefinementUtils.getInterfaceInitialState(componentModel.getMathModel());
        if (RefinementUtils.isInconsistentInitialStates(componentState, modelState)) {
            result = false;
            LogUtils.logWarning("Inconsistent initial state for component '" + componentRef
                    + "' and its circuit implementation");
        }
        return result;
    }

    public static void squashComponent(VisualCircuit circuit, VisualFunctionComponent component,
            VisualCircuit componentModel) {

        String pageName = circuit.getMathName(component);
        circuit.setMathName(component, Identifier.getTemporaryName());
        Container container = (Container) component.getParent();
        VisualPage page = circuit.createVisualPage(container);
        circuit.setMathName(page, pageName);
        circuit.reparent(page, componentModel, componentModel.getRoot(), null);
        page.setPosition(component.getPosition());

        for (VisualFunctionContact pin : component.getVisualFunctionContacts()) {
            String pinName = pin.getName();
            String pageRef = circuit.getMathReference(page);
            String portRef = NamespaceHelper.getReference(pageRef, pinName);
            VisualFunctionContact port = circuit.getVisualComponentByMathReference(portRef, VisualFunctionContact.class);
            mergeConnections(circuit, pin, port);
        }
        circuit.remove(component);
    }

    private static void mergeConnections(VisualCircuit circuit, VisualFunctionContact pin, VisualFunctionContact port) {
        if ((pin != null) && (port != null) && (pin.isDriver() == port.isDriven())) {
            VisualFunctionContact drivenContact = pin.isDriven() ? pin : port;
            VisualFunctionContact driverContact = port.isDriver() ? port : pin;

            // Collapse adjacent replicas before processing connections
            collapseReplicaContacts(circuit, drivenContact, driverContact);

            Map<VisualNode, LinkedList<Point2D>> fromNodeConnectionShapes = new HashMap<>();
            for (VisualNode fromNode : circuit.getPreset(drivenContact)) {
                VisualConnection connection = circuit.getConnection(fromNode, drivenContact);
                if (connection != null) {
                    fromNodeConnectionShapes.put(fromNode, ConnectionHelper.getControlPoints(connection));
                }
            }

            Map<VisualNode, LinkedList<Point2D>> toNodeConnectionShapes = new HashMap<>();
            for (VisualNode toNode : circuit.getPostset(driverContact)) {
                VisualConnection connection = circuit.getConnection(driverContact, toNode);
                if (connection != null) {
                    toNodeConnectionShapes.put(toNode, ConnectionHelper.getControlPoints(connection));
                }
            }
            circuit.remove(pin);
            circuit.remove(port);
            mergeConnections(circuit, fromNodeConnectionShapes, toNodeConnectionShapes);
        }
    }

    private static void collapseReplicaContacts(VisualCircuit circuit,
            VisualFunctionContact drivenContact, VisualFunctionContact driverContact) {

        for (VisualNode predNode : new ArrayList<>(circuit.getPreset(drivenContact))) {
            if (predNode instanceof VisualReplicaContact) {
                VisualReplicaContact predReplicaContact = (VisualReplicaContact) predNode;
                ConversionUtils.collapseReplicaContact(circuit, predReplicaContact);
            }
        }
        for (Replica replica : new ArrayList<>(driverContact.getReplicas())) {
            if (replica instanceof VisualReplicaContact) {
                VisualReplicaContact replicaContact = (VisualReplicaContact) replica;
                ConversionUtils.collapseReplicaContact(circuit, replicaContact);
            }
        }
    }

    private static void mergeConnections(VisualCircuit circuit,
            Map<VisualNode, LinkedList<Point2D>> fromNodeConnectionShapes,
            Map<VisualNode, LinkedList<Point2D>> toNodeConnectionShapes) {

        for (VisualNode fromNode : fromNodeConnectionShapes.keySet()) {
            LinkedList<Point2D> shape = new LinkedList<>(fromNodeConnectionShapes.get(fromNode));
            for (VisualNode toNode : toNodeConnectionShapes.keySet()) {
                shape.addAll(toNodeConnectionShapes.get(toNode));
                try {
                    VisualConnection connection = circuit.connect(fromNode, toNode);
                    ConnectionHelper.addControlPoints(connection, shape);
                } catch (InvalidConnectionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
