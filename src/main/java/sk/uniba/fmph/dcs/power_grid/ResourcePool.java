package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;

/**
 * Manages the off-market resource supply and storage space.
 * Implements {@link InterfaceResourcePoolResourceMarket}.
 */
public final class ResourcePool implements InterfaceResourcePoolResourceMarket {

    private final Map<Resource, Integer> pool = new EnumMap<>(Resource.class);
    private final Map<Resource, Integer> storageSpace = new EnumMap<>(Resource.class);

    /**
     * Default constructor initializing with full standard game supply:
     * 24 Coal, 24 Oil, 24 Garbage, and 12 Uranium.
     */
    public ResourcePool() {
        this(ResourceMarket.MAX_RESOURCES, Map.of());
    }

    /**
     * Constructs a ResourcePool with specific initial counts in the pool.
     *
     * @param initialPool initial counts of resources in the pool
     */
    public ResourcePool(final Map<Resource, Integer> initialPool) {
        this(initialPool, Map.of());
    }

    /**
     * Constructs a ResourcePool with specific initial counts in both pool and storage space.
     *
     * @param initialPool    initial counts of resources in the pool
     * @param initialStorage initial counts of resources in the storage space
     */
    public ResourcePool(final Map<Resource, Integer> initialPool, final Map<Resource, Integer> initialStorage) {
        Objects.requireNonNull(initialPool, "initialPool must not be null");
        Objects.requireNonNull(initialStorage, "initialStorage must not be null");

        for (Resource resource : Resource.values()) {
            pool.put(resource, 0);
            storageSpace.put(resource, 0);
        }

        for (Map.Entry<Resource, Integer> entry : initialPool.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException("Resource count cannot be negative: " + entry.getValue());
            }
            pool.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Resource, Integer> entry : initialStorage.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException("Storage resource count cannot be negative: " + entry.getValue());
            }
            storageSpace.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean canTakeResourcesFromStorageSpace(final List<Resource> resources) {
        if (resources == null) {
            return false;
        }
        Map<Resource, Integer> needed = count(resources);
        for (Map.Entry<Resource, Integer> entry : needed.entrySet()) {
            if (storageSpace.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void getFromStorageSpace(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        if (!canTakeResourcesFromStorageSpace(resources)) {
            throw new IllegalStateException("Not enough resources in storage space to satisfy request: " + resources);
        }
        for (Resource resource : resources) {
            storageSpace.merge(resource, -1, Integer::sum);
        }
    }

    @Override
    public List<Resource> takeResources(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        List<Resource> result = new ArrayList<>();
        for (Resource resource : resources) {
            int current = pool.getOrDefault(resource, 0);
            if (current > 0) {
                pool.put(resource, current - 1);
                result.add(resource);
            }
        }
        return result;
    }

    /**
     * Returns consumed or discarded resources back to the pool.
     *
     * @param resources resources to put back
     */
    public void putResources(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        for (Resource resource : resources) {
            pool.merge(resource, 1, Integer::sum);
        }
    }

    /**
     * Adds resources to the storage space (e.g. for special USA map rules).
     *
     * @param resources resources to put into storage space
     */
    public void putIntoStorageSpace(final List<Resource> resources) {
        Objects.requireNonNull(resources, "resources must not be null");
        for (Resource resource : resources) {
            storageSpace.merge(resource, 1, Integer::sum);
        }
    }

    /**
     * Returns the current count of a specific resource in the pool.
     *
     * @param resource the resource to check
     * @return count in the pool
     */
    public int getResourceCount(final Resource resource) {
        return pool.getOrDefault(resource, 0);
    }

    /**
     * Returns the current count of a specific resource in storage space.
     *
     * @param resource the resource to check
     * @return count in storage space
     */
    public int getStorageResourceCount(final Resource resource) {
        return storageSpace.getOrDefault(resource, 0);
    }

    @Override
    public String getStateString() {
        JSONObject obj = new JSONObject();
        JSONObject poolObj = new JSONObject();
        for (Resource r : Resource.values()) {
            poolObj.put(r.name(), pool.getOrDefault(r, 0));
        }
        obj.put("pool", poolObj);

        JSONObject storageObj = new JSONObject();
        for (Resource r : Resource.values()) {
            storageObj.put(r.name(), storageSpace.getOrDefault(r, 0));
        }
        obj.put("storageSpace", storageObj);

        return obj.toString();
    }

    @Override
    public String toString() {
        return getStateString();
    }

    private static Map<Resource, Integer> count(final List<Resource> list) {
        Map<Resource, Integer> counts = new EnumMap<>(Resource.class);
        for (Resource r : list) {
            counts.merge(r, 1, Integer::sum);
        }
        return counts;
    }
}
