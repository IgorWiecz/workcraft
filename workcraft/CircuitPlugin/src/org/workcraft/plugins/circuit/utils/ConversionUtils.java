package org.workcraft.plugins.circuit.utils;

import org.workcraft.dom.Container;
import org.workcraft.dom.visual.ConnectionHelper;
import org.workcraft.dom.visual.Replica;
import org.workcraft.dom.visual.VisualComponent;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.connections.VisualConnection;
import org.workcraft.exceptions.InvalidConnectionException;
import org.workcraft.plugins.circuit.*;
import org.workcraft.plugins.circuit.commands.DissolveJointTransformationCommand;
import org.workcraft.utils.Hierarchy;
import org.workcraft.utils.ModelUtils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

public class ConversionUtils {

    public static void collapseReplicaContact(VisualCircuit circuit, VisualReplicaContact replica) {
        VisualComponent firstNode = replica.getMaster();
        Point2D replicaPositionInRootSpace = replica.getRootSpacePosition();
        Container container = (Container) replica.getParent();
        Set<VisualConnection> outgoingConnections = circuit.getConnections(replica).stream()
                .filter(connection -> connection.getFirst() == replica).collect(Collectors.toSet());

        if (outgoingConnections.size() > 1) {
            VisualJoint joint = circuit.createJoint(container);
            joint.setRootSpacePosition(replicaPositionInRootSpace);
            try {
                circuit.connect(firstNode, joint);
            } catch (InvalidConnectionException ignored) {
            }
            firstNode = joint;
        }

        for (VisualConnection outgoingConnection : outgoingConnections) {
            VisualNode secondNode = outgoingConnection.getSecond();
            LinkedList<Point2D> locationsInRootSpace
                    = ConnectionHelper.getSuffixControlPoints(outgoingConnection, replicaPositionInRootSpace);

            if (!(firstNode instanceof VisualJoint)) {
                locationsInRootSpace.addFirst(replicaPositionInRootSpace);
            }
            circuit.remove(outgoingConnection);
            try {
                VisualConnection newConnection = circuit.connect(firstNode, secondNode);
                ConnectionHelper.addControlPoints(newConnection, locationsInRootSpace);
            } catch (InvalidConnectionException ignored) {
            }
        }
    }

    public static VisualConnection replicateDriverContact(VisualCircuit circuit, VisualContact drivenContact) {
        return replicateDriverContact(circuit, drivenContact, 0.5);
    }

    public static VisualConnection replicateDriverContact(VisualCircuit circuit, VisualContact drivenContact, double offset) {
        Set<VisualConnection> connections = circuit.getConnections(drivenContact);
        if (connections.size() == 1) {
            VisualConnection connection = connections.iterator().next();
            if (connection.getSecond() == drivenContact) {
                VisualContact driverContact = CircuitUtils.findDriver(circuit, drivenContact, false);
                Container container = Hierarchy.getNearestContainer(driverContact, drivenContact);
                if (container instanceof VisualComponent) {
                    container = (Container) container.getParent();
                }
                VisualReplicaContact replicaDriverContact
                        = circuit.createVisualReplica(driverContact, VisualReplicaContact.class, container);

                Point2D pos = drivenContact.getRootSpacePosition();
                VisualContact.Direction direction = drivenContact.getDirection();
                int sign = drivenContact.isPort() ? -1 : 1;
                ModelUtils.refreshBoundingBox(circuit, replicaDriverContact);
                Rectangle2D replicaBox = replicaDriverContact.getBoundingBoxInLocalSpace();
                double xOffset = sign * direction.getGradientX() * (offset + 0.5 * replicaBox.getWidth());
                double yOffset = sign * direction.getGradientY() * (offset + 0.5 * replicaBox.getHeight());
                replicaDriverContact.setRootSpacePosition(new Point2D.Double(pos.getX() + xOffset, pos.getY() + yOffset));

                VisualNode firstNode = connection.getFirst();
                circuit.remove(connection);
                if (firstNode instanceof VisualJoint) {
                    int size = circuit.getConnections(firstNode).size();
                    if (size < 2) {
                        circuit.remove(firstNode);
                    } else if (size == 2) {
                        new DissolveJointTransformationCommand().transformNodes(circuit, Collections.singleton(firstNode));
                    }
                }

                try {
                    return circuit.connect(replicaDriverContact, drivenContact);
                } catch (InvalidConnectionException ignored) {
                }
            }
        }
        return null;
    }

    public static void updateReplicas(VisualCircuit circuit, VisualContact oldContact, VisualContact newContact) {
        ArrayList<Replica> oldReplicas = new ArrayList<>(oldContact.getReplicas());
        for (Replica oldReplica : oldReplicas) {
            if (oldReplica instanceof VisualReplicaContact) {
                VisualReplicaContact oldReplicaContact = (VisualReplicaContact) oldReplica;
                Container container = (Container) oldReplicaContact.getParent();
                VisualReplicaContact newReplicaContact
                        = circuit.createVisualReplica(newContact, VisualReplicaContact.class, container);

                newReplicaContact.copyStyle(oldReplicaContact);
                newReplicaContact.copyPosition(oldReplicaContact);

                Set<VisualConnection> oldReplicaConnections = circuit.getConnections(oldReplicaContact).stream()
                        .filter(connection -> connection.getFirst() == oldReplicaContact).collect(Collectors.toSet());

                for (VisualConnection oldReplicaConnection : oldReplicaConnections) {
                    VisualNode secondNode = oldReplicaConnection.getSecond();
                    circuit.remove(oldReplicaConnection);
                    try {
                        VisualConnection newReplicaConnection = circuit.connect(newReplicaContact, secondNode);
                        newReplicaConnection.copyStyle(oldReplicaConnection);
                        newReplicaConnection.copyShape(oldReplicaConnection);
                    } catch (InvalidConnectionException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
        }
    }

    public static void removeComponentConnections(VisualCircuit circuit, VisualFunctionComponent component) {
        Set<VisualConnection> unneededConnections = new HashSet<>();
        for (VisualContact contact : component.getVisualContacts()) {
            unneededConnections.addAll(circuit.getConnections(contact));
        }
        circuit.remove(unneededConnections);
    }

}
