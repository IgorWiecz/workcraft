package org.workcraft.plugins.cflt.utils;

import org.workcraft.plugins.cflt.algorithms.EdgeCliqueCoverHeuristic;
import org.workcraft.plugins.cflt.algorithms.EdgeCliqueCoverSolver;
import org.workcraft.plugins.cflt.algorithms.HeuristicType;
import org.workcraft.plugins.cflt.graph.Clique;
import org.workcraft.plugins.cflt.graph.Edge;
import org.workcraft.plugins.cflt.graph.Graph;
import org.workcraft.plugins.cflt.presets.ExpressionParameters.Mode;

import java.util.HashSet;
import java.util.List;

public final class EdgeCliqueCoverUtils {

    private EdgeCliqueCoverUtils() {
    }

    public static List<Clique> getEdgeCliqueCover(Graph inputGraph, Graph outputGraph, boolean isSequence, Mode mode) {

        Graph initialGraph = isSequence
                ? GraphUtils.join(inputGraph, outputGraph)
                : inputGraph;

        HashSet<Edge> optionalEdges = isSequence
                ? new HashSet<>(inputGraph.getEdges())
                : new HashSet<>();

        EdgeCliqueCoverHeuristic heuristic = new EdgeCliqueCoverHeuristic();
        return switch (mode) {
            case FAST_SEQ -> heuristic.getEdgeCliqueCover(initialGraph, optionalEdges, HeuristicType.SEQUENCE);
            case FAST_MAX -> heuristic.getEdgeCliqueCover(initialGraph, optionalEdges, HeuristicType.MAXIMAL);
            case FAST_MIN -> heuristic.getEdgeCliqueCover(initialGraph, optionalEdges, HeuristicType.MINIMAL);
            case SLOW_EXACT -> EdgeCliqueCoverSolver.getEdgeCliqueCover(initialGraph, optionalEdges);
            default -> throw new RuntimeException("Unsupported mode: " + mode);
        };
    }
}
