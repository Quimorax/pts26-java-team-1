package sk.uniba.fmph.dcs.power_grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class CheckAndBumpStepTest {

    private FakeTowns towns;
    private FakeMarket market;
    private ResourceMarket resourceMarket;
    private List<String> eventLog;

    @Before
    public void setUp() {
        eventLog = new ArrayList<>();
        towns = new FakeTowns(eventLog);
        market = new FakeMarket(eventLog);
        resourceMarket = new ResourceMarket(
                new FakePool(),
                List.of(),
                Map.of(
                        1, Map.of(Resource.COAL, 1, Resource.OIL, 1, Resource.GARBAGE, 1, Resource.URANIUM, 1),
                        2, Map.of(Resource.COAL, 2, Resource.OIL, 2, Resource.GARBAGE, 2, Resource.URANIUM, 1),
                        3, Map.of(Resource.COAL, 3, Resource.OIL, 3, Resource.GARBAGE, 3, Resource.URANIUM, 2)
                )
        );
    }

    @Test
    public void testInitialStep() {
        CheckAndBumpStep checker = new CheckAndBumpStep(towns, market, resourceMarket, 4);
        assertEquals(1, checker.getStep());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPlayerCountLow() {
        new CheckAndBumpStep(towns, market, resourceMarket, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPlayerCountHigh() {
        new CheckAndBumpStep(towns, market, resourceMarket, 7);
    }

    @Test
    public void testBumpToStep2NormalThreshold() {
        CheckAndBumpStep checker = new CheckAndBumpStep(towns, market, resourceMarket, 4);

        towns.maxHouses = 6;
        assertFalse(checker.checkAndBumpToStep2());
        assertEquals(1, checker.getStep());
        assertTrue(eventLog.isEmpty());

        towns.maxHouses = 7;
        assertTrue(checker.checkAndBumpToStep2());
        assertEquals(2, checker.getStep());
        assertEquals(List.of("market:startStep2", "towns:startStep2"), eventLog);

        // Subsequent check should do nothing
        assertFalse(checker.checkAndBumpToStep2());
    }

    @Test
    public void testBumpToStep2SixPlayersThreshold() {
        CheckAndBumpStep checker = new CheckAndBumpStep(towns, market, resourceMarket, 6);

        towns.maxHouses = 5;
        assertFalse(checker.checkAndBumpToStep2());

        towns.maxHouses = 6;
        assertTrue(checker.checkAndBumpToStep2());
        assertEquals(2, checker.getStep());
    }

    @Test
    public void testBumpToStep3FromStep2() {
        CheckAndBumpStep checker = new CheckAndBumpStep(towns, market, resourceMarket, 3);
        towns.maxHouses = 7;
        checker.checkAndBumpToStep2();
        eventLog.clear();

        market.step3Start = false;
        assertFalse(checker.checkAndBumpToStep3());
        assertEquals(2, checker.getStep());

        market.step3Start = true;
        assertTrue(checker.checkAndBumpToStep3());
        assertEquals(3, checker.getStep());
        assertEquals(List.of("market:startStep3", "towns:startStep3"), eventLog);

        // Once in Step 3, calling again does nothing
        assertFalse(checker.checkAndBumpToStep3());
    }

    @Test
    public void testBumpToStep3DirectlyFromStep1ExecutesStep2First() {
        CheckAndBumpStep checker = new CheckAndBumpStep(towns, market, resourceMarket, 4);
        assertEquals(1, checker.getStep());

        market.step3Start = true;
        assertTrue(checker.checkAndBumpToStep3());
        assertEquals(3, checker.getStep());

        // Must execute step 2 notifications first, then step 3
        assertEquals(List.of(
                "market:startStep2",
                "towns:startStep2",
                "market:startStep3",
                "towns:startStep3"
        ), eventLog);
    }

    @Test
    public void testGetState() {
        CheckAndBumpStep checker = new CheckAndBumpStep(towns, market, resourceMarket, 5);
        String state = checker.getState();
        JSONObject obj = new JSONObject(state);

        assertEquals(1, obj.getInt("step"));
        assertEquals(5, obj.getInt("numberOfPlayers"));
        assertEquals(7, obj.getInt("step2Threshold"));
        assertEquals(state, checker.toString());
    }

    private static final class FakeTowns implements TownsPort {
        private final List<String> log;
        private int maxHouses;

        private FakeTowns(final List<String> log) {
            this.log = log;
        }

        @Override
        public Variant<Integer, RequestError> buyHouses(final PlayerColor player, final List<String> townNames, final int maxMoney) {
            return Variant.success(0);
        }

        @Override
        public int getNumberOfHouses(final PlayerColor player) {
            return maxHouses;
        }

        @Override
        public int getMaxNumberOfHouses() {
            return maxHouses;
        }

        @Override
        public void startStep2() {
            log.add("towns:startStep2");
        }

        @Override
        public void startStep3() {
            log.add("towns:startStep3");
        }
    }

    private static final class FakeMarket implements PowerPlantMarketPort {
        private final List<String> log;
        private boolean step3Start;

        private FakeMarket(final List<String> log) {
            this.log = log;
        }

        @Override
        public List<PowerPlant> currentMarket() {
            return List.of();
        }

        @Override
        public List<PowerPlant> futureMarket() {
            return List.of();
        }

        @Override
        public void startAuction() {
        }

        @Override
        public Optional<RequestError> canTakePowerPlant(final PowerPlantNumber number, final int price) {
            return Optional.empty();
        }

        @Override
        public PowerPlant takePowerPlant(final PowerPlantNumber number) {
            return null;
        }

        @Override
        public void removeDiscountedPowerPlant() {
        }

        @Override
        public void removePowerPlantsCheaperThan(final int value) {
        }

        @Override
        public void prepareNextTurn() {
        }

        @Override
        public void startStep2() {
            log.add("market:startStep2");
        }

        @Override
        public boolean isStep3Start() {
            return step3Start;
        }

        @Override
        public void startStep3() {
            log.add("market:startStep3");
        }
    }

    private static final class FakePool implements InterfaceResourcePoolResourceMarket {
        @Override
        public boolean canTakeResourcesFromStorageSpace(final List<Resource> resources) {
            return false;
        }

        @Override
        public void getFromStorageSpace(final List<Resource> resources) {
        }

        @Override
        public List<Resource> takeResources(final List<Resource> resources) {
            return List.of();
        }

        @Override
        public String getStateString() {
            return "{}";
        }
    }
}
