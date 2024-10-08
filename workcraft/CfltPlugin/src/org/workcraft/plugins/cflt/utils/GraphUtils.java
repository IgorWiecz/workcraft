package org.workcraft.plugins.cflt.utils;

import org.workcraft.plugins.cflt.graph.Edge;
import org.workcraft.plugins.cflt.graph.Graph;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GraphUtils {

    private GraphUtils() {
    }

    public static final String SPECIAL_CLONE_CHARACTER = "$";

    public static Graph disjointUnion(Graph firstGraph, Graph secondGraph) {
        List<Edge> newEdges = Stream.concat(firstGraph.getEdges().stream(),
                        secondGraph.getEdges().stream())
                .collect(Collectors.toList());

        List<String> newVertices = Stream.concat(firstGraph.getVertexNames().stream(),
                        secondGraph.getVertexNames().stream())
                .collect(Collectors.toList());

        return new Graph(newEdges, newVertices);
    }

    public static Graph join(Graph firstGraph, Graph secondGraph) {
        Graph newGraph = disjointUnion(firstGraph, secondGraph);

        firstGraph.getVertexNames().stream()
                .flatMap(firstVertex -> secondGraph.getVertexNames().stream()
                        .map(secondVertex -> new Edge(firstVertex, secondVertex)))
                .forEach(newGraph::addEdge);

        return newGraph;
    }
}
