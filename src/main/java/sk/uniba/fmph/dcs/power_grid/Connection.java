package sk.uniba.fmph.dcs.power_grid;

import java.util.Objects;

/**
 * Represents an undirected connection (pipeline) between two towns with a specified cost.
 *
 * @param town1 first town name
 * @param town2 second town name
 * @param cost  cost of connecting the two towns
 */
public record Connection(String town1, String town2, int cost) {
    public Connection {
        Objects.requireNonNull(town1, "town1 must not be null");
        Objects.requireNonNull(town2, "town2 must not be null");
        if (cost < 0) {
            throw new IllegalArgumentException("Connection cost cannot be negative: " + cost);
        }
    }
}
