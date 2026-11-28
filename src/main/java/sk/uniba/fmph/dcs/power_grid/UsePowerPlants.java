package sk.uniba.fmph.dcs.power_grid;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Checks that resources power all given plants exactly and computes the number of supplied towns. */
public final class UsePowerPlants {

    /**
     * Burns the resources in the given plants. Every plant must receive exactly its {@code resourceAmount} of
     * accepted resources and no resource may be left over.
     *
     * Since total supply must equal total demand, by Hall's theorem a valid distribution exists if and only if, for
     * every subset of resource types, the resources of those types do not exceed the total demand of the plants
     * accepting at least one of them. This covers hybrid plants and any other combination of accepted types.
     *
     * @param resources
     *            resources to burn
     * @param powerplants
     *            power plants to power
     * @return total number of towns the plants supply, or an error if the resources do not match the plants
     */
    public Variant<Integer, RequestError> numberOfSuppliableTowns(final List<Resource> resources,
            final List<PowerPlant> powerplants) {
        long demand = powerplants.stream().mapToLong(PowerPlant::resourceAmount).sum();
        if (resources.size() != demand) {
            return Variant.failure(new RequestError(
                    "The power plants need " + demand + " resources, but " + resources.size() + " were given."));
        }

        Resource[] types = Resource.values();
        for (int mask = 1; mask < 1 << types.length; mask++) {
            Set<Resource> subset = subsetOf(types, mask);
            long supplied = resources.stream().filter(subset::contains).count();
            long consumable = powerplants.stream().filter(plant -> acceptsAny(plant, subset))
                    .mapToLong(PowerPlant::resourceAmount).sum();
            if (supplied > consumable) {
                return Variant.failure(new RequestError("The resources " + subset
                        + " cannot be distributed so that every power plant is fully powered."));
            }
        }

        return Variant.success(powerplants.stream().mapToInt(PowerPlant::numberOfCities).sum());
    }

    private static Set<Resource> subsetOf(final Resource[] types, final int mask) {
        Set<Resource> subset = EnumSet.noneOf(Resource.class);
        for (Resource type : types) {
            if ((mask & 1 << type.ordinal()) != 0) {
                subset.add(type);
            }
        }
        return subset;
    }

    private static boolean acceptsAny(final PowerPlant plant, final Set<Resource> subset) {
        return plant.acceptedResources().stream().anyMatch(subset::contains);
    }
}
