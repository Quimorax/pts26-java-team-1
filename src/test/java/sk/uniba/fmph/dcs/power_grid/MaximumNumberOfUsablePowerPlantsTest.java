package sk.uniba.fmph.dcs.power_grid;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MaximumNumberOfUsablePowerPlantsTest {

    private static final PowerPlant COAL_2 = plant(4, 2, 1, Resource.COAL);
    private static final PowerPlant OIL_2 = plant(3, 2, 1, Resource.OIL);
    private static final PowerPlant GARBAGE_1 = plant(6, 1, 1, Resource.GARBAGE);
    private static final PowerPlant URANIUM_1 = plant(11, 1, 2, Resource.URANIUM);
    private static final PowerPlant HYBRID_2 = plant(5, 2, 1, Resource.COAL, Resource.OIL);
    private static final PowerPlant ECO = plant(13, 0, 1);

    private final MaximumNumberOfUsablePowerPlants maximum = new MaximumNumberOfUsablePowerPlants();

    private static PowerPlant plant(final int number, final int amount, final int cities, final Resource... accepted) {
        return new PowerPlant(new PowerPlantNumber(number), List.of(accepted), amount, cities);
    }

    private static List<Resource> resources(final int coal, final int oil, final int garbage, final int uranium) {
        List<Resource> result = new ArrayList<>();
        result.addAll(Collections.nCopies(uranium, Resource.URANIUM));
        result.addAll(Collections.nCopies(oil, Resource.OIL));
        result.addAll(Collections.nCopies(garbage, Resource.GARBAGE));
        result.addAll(Collections.nCopies(coal, Resource.COAL));
        return result;
    }

    private void assertTowns(final int expected, final List<Resource> resources, final PowerPlant... plants) {
        assertEquals(expected, maximum.numberOfSuppliableTowns(resources, List.of(plants)));
    }

    @Test
    public void noPlantsSupplyNothing() {
        assertTowns(0, List.of());
        assertTowns(0, resources(5, 5, 5, 5));
    }

    @Test
    public void noResourcesPowerOnlyEcologicalPlants() {
        assertTowns(0, List.of(), COAL_2, HYBRID_2);
        assertTowns(1, List.of(), ECO, COAL_2);
    }

    @Test
    public void ecologicalPlantIsAlwaysUsed() {
        assertTowns(2, resources(2, 0, 0, 0), ECO, COAL_2);
        assertTowns(1, resources(0, 2, 0, 0), ECO, COAL_2);
    }

    @Test
    public void plantNeedsItsFullAmount() {
        assertTowns(0, resources(1, 0, 0, 0), COAL_2);
        assertTowns(1, resources(2, 0, 0, 0), COAL_2);
    }

    @Test
    public void leftoverResourcesAreAllowed() {
        assertTowns(1, resources(5, 0, 0, 0), COAL_2);
        assertTowns(2, resources(2, 3, 4, 1), URANIUM_1);
    }

    @Test
    public void wrongResourceTypesPowerNothing() {
        assertTowns(0, resources(0, 0, 3, 3), COAL_2, OIL_2, HYBRID_2);
        assertTowns(0, resources(1, 0, 0, 0), plant(22, 1, 1));
    }

    @Test
    public void allPlantsUsedWhenEnoughResources() {
        assertTowns(3, resources(2, 2, 1, 0), COAL_2, OIL_2, GARBAGE_1);
        assertTowns(4, resources(4, 0, 0, 1), COAL_2, HYBRID_2, URANIUM_1);
    }

    @Test
    public void choosesTheMostValuableSubset() {
        PowerPlant coalBig = plant(25, 3, 5, Resource.COAL);
        PowerPlant coalSmall = plant(8, 2, 2, Resource.COAL);
        assertTowns(5, resources(4, 0, 0, 0), coalBig, coalSmall, COAL_2);
        assertTowns(7, resources(5, 0, 0, 0), coalBig, coalSmall, COAL_2);
        // Greedy by towns would take the 3-town plant and stop at 3.
        assertTowns(4, resources(4, 0, 0, 0), plant(9, 3, 3, Resource.COAL), plant(10, 2, 2, Resource.COAL),
                plant(12, 2, 2, Resource.COAL));
    }

    @Test
    public void hybridUsesWhateverIsLeft() {
        assertTowns(3, resources(3, 3, 0, 0), COAL_2, OIL_2, HYBRID_2);
        assertTowns(2, resources(1, 3, 0, 0), COAL_2, OIL_2, HYBRID_2);
        assertTowns(2, resources(4, 0, 0, 0), COAL_2, OIL_2, HYBRID_2);
        assertTowns(1, resources(1, 1, 0, 0), COAL_2, OIL_2, HYBRID_2);
    }

    @Test
    public void hybridChoiceMustNotStarveSingleTypePlant() {
        PowerPlant coalBig = plant(25, 2, 5, Resource.COAL);
        assertTowns(6, resources(2, 2, 0, 0), coalBig, HYBRID_2);
        assertTowns(5, resources(3, 0, 0, 0), coalBig, HYBRID_2);
    }

    @Test
    public void overlappingHybridsAreHandled() {
        PowerPlant coalOil = plant(20, 1, 1, Resource.COAL, Resource.OIL);
        PowerPlant oilGarbage = plant(21, 1, 1, Resource.OIL, Resource.GARBAGE);
        assertTowns(2, resources(1, 1, 0, 0), coalOil, oilGarbage);
        assertTowns(1, resources(2, 0, 0, 0), coalOil, oilGarbage);
        assertTowns(2, resources(0, 2, 0, 0), coalOil, oilGarbage);
    }

    @Test
    public void inputsAreNotModified() {
        List<Resource> resources = resources(2, 2, 0, 0);
        List<Resource> original = List.copyOf(resources);
        List<PowerPlant> plants = new ArrayList<>(List.of(COAL_2, HYBRID_2));
        maximum.numberOfSuppliableTowns(resources, plants);
        assertEquals(original, resources);
        assertEquals(List.of(COAL_2, HYBRID_2), plants);
    }
}
