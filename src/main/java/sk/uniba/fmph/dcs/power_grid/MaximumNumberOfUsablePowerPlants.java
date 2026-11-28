package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Computes the largest number of towns a player can supply with the resources they have. */
public final class MaximumNumberOfUsablePowerPlants {

    /**
     * Tries every subset of the power plants and returns the largest number of towns supplied by a subset that the
     * resources can fully power. Resources do not have to be used up.
     *
     * A subset can be powered if and only if, for every set of resource types, the plants accepting only types from
     * that set need no more resources than the player has of those types (Hall's theorem). This covers hybrid plants
     * and any other combination of accepted types.
     *
     * @param resources
     *            resources available for burning
     * @param powerplants
     *            power plants owned by the player
     * @return the maximum number of towns that can be supplied, 0 if no plant can be powered
     */
    public int numberOfSuppliableTowns(final List<Resource> resources, final List<PowerPlant> powerplants) {
        int best = 0;
        for (int plantMask = 0; plantMask < 1 << powerplants.size(); plantMask++) {
            List<PowerPlant> chosen = new ArrayList<>();
            for (int i = 0; i < powerplants.size(); i++) {
                if ((plantMask & 1 << i) != 0) {
                    chosen.add(powerplants.get(i));
                }
            }
            if (canPower(resources, chosen)) {
                best = Math.max(best, chosen.stream().mapToInt(PowerPlant::numberOfCities).sum());
            }
        }
        return best;
    }

    private static boolean canPower(final List<Resource> resources, final List<PowerPlant> plants) {
        Resource[] types = Resource.values();
        for (int typeMask = 0; typeMask < 1 << types.length; typeMask++) {
            Set<Resource> subset = subsetOf(types, typeMask);
            long available = resources.stream().filter(subset::contains).count();
            long needed = plants.stream().filter(plant -> subset.containsAll(plant.acceptedResources()))
                    .mapToLong(PowerPlant::resourceAmount).sum();
            if (needed > available) {
                return false;
            }
        }
        return true;
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
}
