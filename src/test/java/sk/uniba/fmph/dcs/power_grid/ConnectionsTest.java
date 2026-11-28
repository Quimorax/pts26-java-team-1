package sk.uniba.fmph.dcs.power_grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ConnectionsTest {

    private Connections connections;

    @Before
    public void setUp() {
        // Build a sample map with several cities and routes
        // Berlin --(6)-- Leipzig --(8)-- Dresden
        //   |               |
        //  (12)            (7)
        //   |               |
        // Hamburg --(11)-- Hannover --(9)-- Kassel
        List<Connection> edges = List.of(
                new Connection("Berlin", "Leipzig", 6),
                new Connection("Leipzig", "Dresden", 8),
                new Connection("Berlin", "Hamburg", 12),
                new Connection("Leipzig", "Hannover", 7),
                new Connection("Hamburg", "Hannover", 11),
                new Connection("Hannover", "Kassel", 9)
        );
        connections = new Connections(edges);
    }

    @Test
    public void testEmptyToReturnsZero() {
        assertEquals(0, connections.calculateConnectingCost(List.of("Berlin"), List.of()));
        assertEquals(0, connections.calculateConnectingCost(List.of(), List.of()));
    }

    @Test
    public void testFirstCityCostsZeroWhenFromIsEmpty() {
        assertEquals(0, connections.calculateConnectingCost(List.of(), List.of("Berlin")));
        assertEquals(0, connections.calculateConnectingCost(List.of(), List.of("Hamburg")));
    }

    @Test
    public void testDirectConnectionFromSingleCity() {
        // Berlin -> Leipzig is 6
        int cost = connections.calculateConnectingCost(List.of("Berlin"), List.of("Leipzig"));
        assertEquals(6, cost);
    }

    @Test
    public void testTransitingThroughIntermediateCities() {
        // Berlin -> Dresden via Leipzig (6 + 8 = 14)
        int cost = connections.calculateConnectingCost(List.of("Berlin"), List.of("Dresden"));
        assertEquals(14, cost);

        // Berlin -> Hannover via Leipzig (6 + 7 = 13), shorter than via Hamburg (12 + 11 = 23)
        cost = connections.calculateConnectingCost(List.of("Berlin"), List.of("Hannover"));
        assertEquals(13, cost);
    }

    @Test
    public void testConnectingMultipleCitiesExpandingNetwork() {
        // from = Berlin. to = [Leipzig, Dresden].
        // Connect Leipzig for 6. Then Dresden can connect to Leipzig for 8. Total = 14.
        int cost = connections.calculateConnectingCost(List.of("Berlin"), List.of("Leipzig", "Dresden"));
        assertEquals(14, cost);

        // Even if order is reversed in to list:
        cost = connections.calculateConnectingCost(List.of("Berlin"), List.of("Dresden", "Leipzig"));
        assertEquals(14, cost);
    }

    @Test
    public void testFirstTurnMultipleCities() {
        // from = empty. to = [Berlin, Leipzig, Dresden]
        // First city is free (0). Leipzig connects for 6, Dresden connects for 8. Total = 14.
        int cost = connections.calculateConnectingCost(List.of(), List.of("Berlin", "Leipzig", "Dresden"));
        assertEquals(14, cost);

        // from = empty. to = [Leipzig, Hannover] -> first free (0), second is 7 -> total 7.
        cost = connections.calculateConnectingCost(List.of(), List.of("Leipzig", "Hannover"));
        assertEquals(7, cost);
    }

    @Test
    public void testConnectingFromMultipleExistingCitiesChoosesClosest() {
        // Player already has Berlin and Hamburg.
        // Leipzig connects to Berlin (6), Kassel connects to Hannover (9) -> Hamburg-Hannover (11) + Hannover-Kassel (9) = 20
        int cost = connections.calculateConnectingCost(List.of("Berlin", "Hamburg"), List.of("Hannover"));
        // From Hamburg to Hannover is 11, from Berlin to Hannover is 13. Minimum is 11.
        assertEquals(11, cost);
    }

    @Test
    public void testAlreadyConnectedCitiesIgnored() {
        // to contains a city already in from
        int cost = connections.calculateConnectingCost(List.of("Berlin", "Leipzig"), List.of("Berlin", "Dresden"));
        // Berlin already in network, only Dresden connects (to Leipzig for 8)
        assertEquals(8, cost);
    }

    @Test
    public void testDuplicateCitiesInToIgnored() {
        int cost = connections.calculateConnectingCost(List.of("Berlin"), List.of("Leipzig", "Leipzig"));
        assertEquals(6, cost);
    }

    @Test
    public void testParallelEdgesUsesCheapest() {
        Connections parallel = new Connections(List.of(
                new Connection("A", "B", 10),
                new Connection("A", "B", 5)
        ));
        assertEquals(5, parallel.calculateConnectingCost(List.of("A"), List.of("B")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnreachableCityThrowsException() {
        Connections disconnected = new Connections(List.of(
                new Connection("A", "B", 5),
                new Connection("Isolated", "Another", 4)
        ));
        disconnected.calculateConnectingCost(List.of("A"), List.of("Isolated"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullFromThrowsException() {
        connections.calculateConnectingCost(null, List.of("Berlin"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullToThrowsException() {
        connections.calculateConnectingCost(List.of("Berlin"), null);
    }

    @Test
    public void testConnectionRecordValidation() {
        try {
            new Connection(null, "B", 5);
            fail("Expected NullPointerException for null town1");
        } catch (NullPointerException expected) {
            // success
        }

        try {
            new Connection("A", null, 5);
            fail("Expected NullPointerException for null town2");
        } catch (NullPointerException expected) {
            // success
        }

        try {
            new Connection("A", "B", -1);
            fail("Expected IllegalArgumentException for negative cost");
        } catch (IllegalArgumentException expected) {
            // success
        }

        Connection valid = new Connection("A", "B", 10);
        assertEquals("A", valid.town1());
        assertEquals("B", valid.town2());
        assertEquals(10, valid.cost());
        assertNotNull(connections.distances());
    }
}
