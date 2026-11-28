package sk.uniba.fmph.dcs.power_grid;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Manages the map network graph and calculates minimum connection costs between towns.
 */
public final class Connections {

    private final List<Connection> distances;
    private final Map<String, Map<String, Integer>> adjacency = new HashMap<>();

    /**
     * Constructs a Connections graph with the given list of connections.
     *
     * @param distances list of connections between towns
     */
    public Connections(final List<Connection> distances) {
        this.distances = List.copyOf(Objects.requireNonNull(distances, "distances must not be null"));
        for (Connection edge : this.distances) {
            Objects.requireNonNull(edge, "connection edge must not be null");
            adjacency.computeIfAbsent(edge.town1(), k -> new HashMap<>()).merge(edge.town2(), edge.cost(), Math::min);
            adjacency.computeIfAbsent(edge.town2(), k -> new HashMap<>()).merge(edge.town1(), edge.cost(), Math::min);
        }
    }

    /**
     * Returns an unmodifiable list of all connections in this graph.
     *
     * @return unmodifiable list of connections
     */
    public List<Connection> distances() {
        return Collections.unmodifiableList(distances);
    }

    /**
     * Calculates the minimum connection cost to connect all towns in {@code to}
     * to the player's existing network in {@code from}.
     *
     * If {@code from} is empty, the first connected town costs 0, and subsequent
     * towns in {@code to} are connected to the network formed so far.
     * Intermediate towns may be passed through freely by paying the edge costs along the path.
     *
     * @param from list of town names where player already has houses
     * @param to   list of new town names to connect
     * @return minimum total connection cost
     */
    public int calculateConnectingCost(final List<String> from, final List<String> to) {
        Objects.requireNonNull(from, "from list must not be null");
        Objects.requireNonNull(to, "to list must not be null");

        Set<String> remaining = new LinkedHashSet<>();
        for (String town : to) {
            Objects.requireNonNull(town, "town in to list must not be null");
            if (!from.contains(town)) {
                remaining.add(town);
            }
        }

        if (remaining.isEmpty()) {
            return 0;
        }

        Set<String> connected = new HashSet<>();
        for (String town : from) {
            Objects.requireNonNull(town, "town in from list must not be null");
            connected.add(town);
        }

        int totalCost = 0;

        if (connected.isEmpty()) {
            String firstTown = remaining.iterator().next();
            remaining.remove(firstTown);
            connected.add(firstTown);
        }

        while (!remaining.isEmpty()) {
            Map<String, Integer> dists = dijkstraFrom(connected);

            String bestTown = null;
            int bestCost = Integer.MAX_VALUE;

            for (String candidate : remaining) {
                int cost = dists.getOrDefault(candidate, Integer.MAX_VALUE);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestTown = candidate;
                }
            }

            if (bestTown == null || bestCost == Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Cannot reach all towns in target set from network: " + remaining);
            }

            totalCost += bestCost;
            connected.add(bestTown);
            remaining.remove(bestTown);
        }

        return totalCost;
    }

    private Map<String, Integer> dijkstraFrom(final Set<String> sources) {
        Map<String, Integer> distancesFromSources = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));

        for (String source : sources) {
            distancesFromSources.put(source, 0);
            pq.add(new NodeDistance(source, 0));
        }

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String u = current.town();
            int d = current.distance();

            if (d > distancesFromSources.getOrDefault(u, Integer.MAX_VALUE)) {
                continue;
            }

            Map<String, Integer> neighbors = adjacency.get(u);
            if (neighbors != null) {
                for (Map.Entry<String, Integer> edge : neighbors.entrySet()) {
                    String v = edge.getKey();
                    int weight = edge.getValue();
                    int newDist = d + weight;
                    if (newDist < distancesFromSources.getOrDefault(v, Integer.MAX_VALUE)) {
                        distancesFromSources.put(v, newDist);
                        pq.add(new NodeDistance(v, newDist));
                    }
                }
            }
        }

        return distancesFromSources;
    }

    private record NodeDistance(String town, int distance) {
    }
}
