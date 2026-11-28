package sk.uniba.fmph.dcs.power_grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

public class ResourcePoolTest {

    @Test
    public void testDefaultInitialization() {
        ResourcePool pool = new ResourcePool();
        assertEquals(24, pool.getResourceCount(Resource.COAL));
        assertEquals(24, pool.getResourceCount(Resource.OIL));
        assertEquals(24, pool.getResourceCount(Resource.GARBAGE));
        assertEquals(12, pool.getResourceCount(Resource.URANIUM));
        assertEquals(0, pool.getStorageResourceCount(Resource.COAL));
    }

    @Test
    public void testCustomInitialization() {
        ResourcePool pool = new ResourcePool(
                Map.of(Resource.COAL, 5, Resource.OIL, 3),
                Map.of(Resource.COAL, 2)
        );
        assertEquals(5, pool.getResourceCount(Resource.COAL));
        assertEquals(3, pool.getResourceCount(Resource.OIL));
        assertEquals(0, pool.getResourceCount(Resource.GARBAGE));
        assertEquals(2, pool.getStorageResourceCount(Resource.COAL));
        assertEquals(0, pool.getStorageResourceCount(Resource.OIL));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeResourceThrowsException() {
        new ResourcePool(Map.of(Resource.COAL, -1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeStorageThrowsException() {
        new ResourcePool(Map.of(), Map.of(Resource.COAL, -1));
    }

    @Test
    public void testTakeResourcesFullSupply() {
        ResourcePool pool = new ResourcePool(Map.of(Resource.COAL, 4, Resource.OIL, 2));

        List<Resource> taken = pool.takeResources(List.of(Resource.COAL, Resource.COAL, Resource.OIL));
        assertEquals(List.of(Resource.COAL, Resource.COAL, Resource.OIL), taken);
        assertEquals(2, pool.getResourceCount(Resource.COAL));
        assertEquals(1, pool.getResourceCount(Resource.OIL));
    }

    @Test
    public void testTakeResourcesPartialSupplyReturnsAvailableOnly() {
        ResourcePool pool = new ResourcePool(Map.of(Resource.COAL, 1, Resource.OIL, 0));

        // Request 3 coal and 2 oil, but only 1 coal available
        List<Resource> taken = pool.takeResources(List.of(
                Resource.COAL, Resource.COAL, Resource.COAL,
                Resource.OIL, Resource.OIL
        ));
        assertEquals(List.of(Resource.COAL), taken);
        assertEquals(0, pool.getResourceCount(Resource.COAL));
        assertEquals(0, pool.getResourceCount(Resource.OIL));
    }

    @Test
    public void testPutResourcesIncreasesSupply() {
        ResourcePool pool = new ResourcePool(Map.of(Resource.COAL, 1));
        pool.takeResources(List.of(Resource.COAL));
        assertEquals(0, pool.getResourceCount(Resource.COAL));

        pool.putResources(List.of(Resource.COAL, Resource.COAL, Resource.OIL));
        assertEquals(2, pool.getResourceCount(Resource.COAL));
        assertEquals(1, pool.getResourceCount(Resource.OIL));

        List<Resource> reTake = pool.takeResources(List.of(Resource.COAL, Resource.OIL));
        assertEquals(2, reTake.size());
        assertEquals(1, pool.getResourceCount(Resource.COAL));
        assertEquals(0, pool.getResourceCount(Resource.OIL));
    }

    @Test
    public void testStorageSpaceHandling() {
        ResourcePool pool = new ResourcePool(Map.of(), Map.of(Resource.COAL, 3));

        assertTrue(pool.canTakeResourcesFromStorageSpace(List.of(Resource.COAL, Resource.COAL)));
        assertFalse(pool.canTakeResourcesFromStorageSpace(List.of(Resource.COAL, Resource.COAL, Resource.COAL, Resource.COAL)));
        assertFalse(pool.canTakeResourcesFromStorageSpace(List.of(Resource.OIL)));
        assertFalse(pool.canTakeResourcesFromStorageSpace(null));

        pool.getFromStorageSpace(List.of(Resource.COAL, Resource.COAL));
        assertEquals(1, pool.getStorageResourceCount(Resource.COAL));

        pool.putIntoStorageSpace(List.of(Resource.OIL, Resource.COAL));
        assertEquals(2, pool.getStorageResourceCount(Resource.COAL));
        assertEquals(1, pool.getStorageResourceCount(Resource.OIL));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetFromStorageSpaceExceedingAvailableThrowsException() {
        ResourcePool pool = new ResourcePool(Map.of(), Map.of(Resource.COAL, 1));
        pool.getFromStorageSpace(List.of(Resource.COAL, Resource.COAL));
    }

    @Test
    public void testJsonStateString() {
        ResourcePool pool = new ResourcePool(
                Map.of(Resource.COAL, 4, Resource.URANIUM, 2),
                Map.of(Resource.COAL, 1)
        );
        String state = pool.getStateString();
        JSONObject obj = new JSONObject(state);

        assertTrue(obj.has("pool"));
        assertTrue(obj.has("storageSpace"));
        assertEquals(4, obj.getJSONObject("pool").getInt("COAL"));
        assertEquals(2, obj.getJSONObject("pool").getInt("URANIUM"));
        assertEquals(1, obj.getJSONObject("storageSpace").getInt("COAL"));
        assertEquals(state, pool.toString());
    }

    @Test
    public void testIntegrationWithResourceMarket() {
        // Test that ResourcePool works seamlessly with the teacher's ResourceMarket
        ResourcePool pool = new ResourcePool(Map.of(Resource.COAL, 10, Resource.OIL, 10, Resource.GARBAGE, 10, Resource.URANIUM, 10));
        ResourceMarket market = new ResourceMarket(
                pool,
                List.of(Resource.COAL, Resource.COAL),
                Map.of(
                        1, Map.of(Resource.COAL, 2, Resource.OIL, 1, Resource.GARBAGE, 1, Resource.URANIUM, 1),
                        2, Map.of(Resource.COAL, 2, Resource.OIL, 1, Resource.GARBAGE, 1, Resource.URANIUM, 1),
                        3, Map.of(Resource.COAL, 2, Resource.OIL, 1, Resource.GARBAGE, 1, Resource.URANIUM, 1)
                )
        );

        int initialCoalInPool = pool.getResourceCount(Resource.COAL);
        market.refill();
        assertEquals(initialCoalInPool - 2, pool.getResourceCount(Resource.COAL));
    }
}
