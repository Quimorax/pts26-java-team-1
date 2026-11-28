package sk.uniba.fmph.dcs.power_grid;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UsePowerPlantsTest {

    private static final PowerPlant COAL_2 = plant(4, 2, 1, Resource.COAL);
    private static final PowerPlant OIL_2 = plant(3, 2, 1, Resource.OIL);
    private static final PowerPlant GARBAGE_1 = plant(6, 1, 1, Resource.GARBAGE);
    private static final PowerPlant URANIUM_1 = plant(11, 1, 2, Resource.URANIUM);
    private static final PowerPlant HYBRID_2 = plant(5, 2, 1, Resource.COAL, Resource.OIL);
    private static final PowerPlant ECO = plant(13, 0, 1);

    private final UsePowerPlants usePowerPlants = new UsePowerPlants();

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
        Variant<Integer, RequestError> result = usePowerPlants.numberOfSuppliableTowns(resources, List.of(plants));
        assertEquals(Variant.success(expected), result);
    }

    private void assertError(final List<Resource> resources, final PowerPlant... plants) {
        Variant<Integer, RequestError> result = usePowerPlants.numberOfSuppliableTowns(resources, List.of(plants));
        assertTrue("Expected an error but got " + result, result instanceof Variant.Failure<Integer, RequestError>);
    }

    @Test
    public void noPlantsAndNoResourcesSupplyNothing() {
        assertTowns(0, List.of());
    }

    @Test
    public void resourcesWithoutPlantsAreAnError() {
        assertError(resources(1, 0, 0, 0));
    }

    @Test
    public void ecologicalPlantNeedsNoResources() {
        assertTowns(1, List.of(), ECO);
        assertTowns(2, resources(2, 0, 0, 0), ECO, COAL_2);
        assertError(resources(1, 0, 0, 0), ECO);
    }

    @Test
    public void singlePlantNeedsExactAmount() {
        assertTowns(1, resources(2, 0, 0, 0), COAL_2);
        assertTowns(2, resources(0, 0, 0, 1), URANIUM_1);
        assertError(resources(1, 0, 0, 0), COAL_2);
        assertError(resources(3, 0, 0, 0), COAL_2);
    }

    @Test
    public void wrongResourceTypeIsAnError() {
        assertError(resources(0, 2, 0, 0), COAL_2);
        assertError(resources(1, 1, 0, 0), COAL_2);
        assertError(resources(0, 0, 1, 0), URANIUM_1);
        assertError(resources(0, 0, 2, 0), HYBRID_2);
    }

    @Test
    public void everyPlantMustBeFullyPowered() {
        assertTowns(4, resources(2, 2, 1, 0), COAL_2, OIL_2, GARBAGE_1, ECO);
        assertError(resources(3, 1, 1, 0), COAL_2, OIL_2, GARBAGE_1);
        assertError(resources(2, 2, 0, 1), COAL_2, OIL_2, GARBAGE_1);
    }

    @Test
    public void hybridAcceptsAnySplitOfCoalAndOil() {
        for (int coal = 0; coal <= 2; coal++) {
            assertTowns(1, resources(coal, 2 - coal, 0, 0), HYBRID_2);
        }
    }

    @Test
    public void hybridTakesWhatSingleTypePlantsCannot() {
        assertTowns(3, resources(3, 3, 0, 0), COAL_2, OIL_2, HYBRID_2);
        assertTowns(2, resources(4, 0, 0, 0), COAL_2, HYBRID_2);
        assertError(resources(5, 1, 0, 0), COAL_2, OIL_2, HYBRID_2);
        assertError(resources(1, 5, 0, 0), COAL_2, OIL_2, HYBRID_2);
    }

    @Test
    public void overlappingHybridsAreHandled() {
        PowerPlant coalOil = plant(20, 1, 1, Resource.COAL, Resource.OIL);
        PowerPlant oilGarbage = plant(21, 1, 1, Resource.OIL, Resource.GARBAGE);
        assertTowns(2, resources(1, 0, 1, 0), coalOil, oilGarbage);
        assertTowns(2, resources(0, 2, 0, 0), coalOil, oilGarbage);
        assertError(resources(2, 0, 0, 0), coalOil, oilGarbage);
        assertError(resources(0, 0, 2, 0), coalOil, oilGarbage);
    }

    @Test
    public void plantWithoutAcceptedTypesCannotBePowered() {
        assertError(resources(1, 0, 0, 0), plant(22, 1, 1));
    }

    @Test
    public void townsOfAllPlantsAreSummed() {
        PowerPlant big = plant(30, 3, 6, Resource.COAL);
        assertTowns(9, resources(3, 2, 0, 1), big, OIL_2, URANIUM_1);
    }

    @Test
    public void inputsAreNotModified() {
        List<Resource> resources = resources(2, 2, 0, 0);
        List<Resource> original = List.copyOf(resources);
        List<PowerPlant> plants = new ArrayList<>(List.of(COAL_2, HYBRID_2));
        usePowerPlants.numberOfSuppliableTowns(resources, plants);
        assertEquals(original, resources);
        assertEquals(List.of(COAL_2, HYBRID_2), plants);
    }
}
