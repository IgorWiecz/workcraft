package org.workcraft.plugins.cflt.algorithms;

import org.workcraft.plugins.cflt.AdvancedGraph;
import org.workcraft.plugins.cflt.Clique;
import org.workcraft.plugins.cflt.Edge;
import org.workcraft.plugins.cflt.Graph;

import java.util.ArrayList;
import java.util.List;

/**
 * This algorithm is not as efficient as it should be, it is merely a temporary placeholder
 * TODO: Replace this with a SAT Solver solution
 */
public class ExhaustiveSearch {

    public static List<Clique> getEdgeCliqueCover(Graph initialGraph, List<Edge> optionalEdges) {
        List<Clique> edgeCliqueCover = new ArrayList<>();
        List<Clique> allMaxCliques = MaxCliqueEnumerator.getAllMaxCliques(initialGraph);


        AdvancedGraph graph = null;
        if (!initialGraph.getEdges().isEmpty()) {
            int treeDepth = 0;
            while (edgeCliqueCover.isEmpty()) {
                graph = new AdvancedGraph(initialGraph, allMaxCliques);
                edgeCliqueCover = branch(graph, treeDepth, new ArrayList<>(), optionalEdges);
                treeDepth++;
            }
        }
        return edgeCliqueCover;
    }

    private static List<Clique> branch(AdvancedGraph graph, int treeDepth,
            List<Clique> edgeCliqueCover, List<Edge> optionalEdges) {

        if (graph.isCovered(edgeCliqueCover, optionalEdges)) {
            return edgeCliqueCover;
        }
        if (treeDepth < 0) {
            return new ArrayList<>();
        }
        Edge selectedEdge = graph.getNextEdge();
        if (selectedEdge == null) {
            return edgeCliqueCover;
        }

        for (Clique maxClique : graph.getMaximalCliques(selectedEdge)) {
            List<Clique> newEdgeCliqueCover = new ArrayList<>(edgeCliqueCover);
            newEdgeCliqueCover.add(maxClique);

            List<Clique> edgeCliqueCoverPrime = branch(graph, treeDepth - 1, newEdgeCliqueCover, optionalEdges);
            if (!edgeCliqueCoverPrime.isEmpty()) {
                return edgeCliqueCoverPrime;
            }
        }
        return new ArrayList<>();
    }

}
