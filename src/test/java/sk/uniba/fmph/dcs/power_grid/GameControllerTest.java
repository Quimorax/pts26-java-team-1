package sk.uniba.fmph.dcs.power_grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static sk.uniba.fmph.dcs.power_grid.GamePhase.*;
import static sk.uniba.fmph.dcs.power_grid.PlayerColor.BLUE;
import static sk.uniba.fmph.dcs.power_grid.PlayerColor.RED;

public class GameControllerTest {
    private final Board red = new Board(11);
    private final Board blue = new Board(7);
    private final Towns towns = new Towns();
    private final Market market = new Market();
    private final Auction auction = new Auction();
    private GameController controller;
    private PowerGridGameAdapter adapter;

    @Before
    public void startGame() {
        Map<Integer, Map<Resource, Integer>> refill = Map.of(1, refillCoal(1), 2, refillCoal(2), 3, refillCoal(3));
        ResourceMarket resources = new ResourceMarket(new ResourcePool(), List.of(), refill);
        controller = new GameController(auction, towns, resources, market, Map.of(RED, red, BLUE, blue),
                new Random(4096));
        adapter = new PowerGridGameAdapter(controller, Map.of(1, RED, 2, BLUE));
    }

    private static Map<Resource, Integer> refillCoal(int coal) {
        return Map.of(Resource.COAL, coal, Resource.OIL, 0, Resource.GARBAGE, 0, Resource.URANIUM, 0);
    }

    private JSONObject state() {
        return new JSONObject(controller.getState());
    }

    private void assertPhase(GamePhase expected) {
        assertEquals(expected.name(), state().getString("gamePhase"));
    }

    private static JSONObject accepted(Variant<SerializedGameState, RequestError> result) {
        assertTrue(result.toString(), result instanceof Variant.Success);
        return new JSONObject(((Variant.Success<SerializedGameState, RequestError>) result).value().state());
    }

    private JSONObject finishAuction() {
        auction.response = Variant.success(PhaseStatus.PHASE_ENDS);
        JSONObject next = accepted(adapter.doNotBidForCurrentPowerPlant(2));
        auction.response = Variant.success(PhaseStatus.PHASE_CONTINUES);
        return next;
    }

    private void reachBuilding() {
        finishAuction();
        accepted(adapter.buyResources(2, ""));
        accepted(adapter.buyResources(1, ""));
        assertPhase(BUY_HOUSES);
    }

    @Test
    public void firstAuctionUsesShuffledOrderAndPublishesTheGameState() {
        assertEquals(List.of(List.of(BLUE, RED)), auction.roundOrders);
        JSONObject state = state();
        assertEquals(AUCTION_POWER_PLANTS.name(), state.getString("gamePhase"));
        assertTrue(state.getBoolean("isFirstTurn"));
        assertEquals(1, state.getInt("step"));
        assertEquals(List.of("BLUE", "RED"), state.getJSONArray("playerOrder").toList());
        assertEquals("initial auction", state.getString("phaseState"));
        JSONObject player = state.getJSONObject("players").getJSONObject("RED");
        assertEquals(50, player.getJSONObject("board").getInt("money"));
        assertEquals(0, player.getInt("houses"));
        JSONObject plantMarket = state.getJSONObject("powerPlantMarket");
        JSONObject card = plantMarket.getJSONArray("current").getJSONObject(0);
        assertEquals(7, card.getInt("number"));
        assertEquals(List.of("OIL"), card.getJSONArray("acceptedResources").toList());
        assertEquals(3, card.getInt("resourceAmount"));
        assertEquals(2, card.getInt("numberOfCities"));
        assertEquals(13, plantMarket.getJSONArray("future").getJSONObject(0).getInt("number"));
        assertEquals(0, state.getJSONObject("resourceMarket").getInt("coal"));
        assertEquals(0, state.getJSONArray("gameResult").length());
    }

    @Test
    public void routesEveryAuctionActionAndWaitsForTheAuctionToReportCompletion() {
        PowerPlantNumber number = new PowerPlantNumber(13);
        List<Resource> discarded = List.of(Resource.COAL, Resource.COAL);
        assertEquals(Optional.empty(), controller.selectPowerPlantToBid(BLUE, number, 14));
        assertEquals(Optional.empty(), controller.bidForCurrentPowerPlant(RED, 15));
        assertEquals(Optional.empty(), controller.doNotBidForCurrentPowerPlant(BLUE));
        RequestError error = new RequestError("Every player must buy a plant in the first auction.");
        auction.response = Variant.failure(error);
        assertEquals(Optional.of(error), controller.doNotWantPowerPlant(RED));
        assertPhase(AUCTION_POWER_PLANTS);
        auction.response = Variant.success(PhaseStatus.PHASE_ENDS);
        assertEquals(Optional.empty(), controller.removePowerPlant(RED, number, discarded));
        assertPhase(BUY_RESOURCES);
        assertEquals(List.of(List.of("select", BLUE, number, 14), List.of("bid", RED, 15),
                List.of("passBid", BLUE), List.of("passAuction", RED), List.of("remove", RED, number, discarded)),
                auction.actions);
    }

    @Test
    public void wrongPhaseCommandsAreRejectedBeforeDelegation() {
        JSONObject before = state();
        assertTrue(controller.buyResources(BLUE, List.of(Resource.OIL)).isPresent());
        assertTrue(controller.buyHouses(RED, List.of("Essen")).isPresent());
        assertTrue(controller.usePowerPlants(BLUE, List.of(), List.of()).isPresent());
        assertTrue(before.similar(state()));
        assertEquals(0, blue.resourceCalls);
        assertEquals(0, towns.buyCalls);

        finishAuction();
        before = state();
        assertAuctionClosed();
        assertTrue(before.similar(state()));
    }

    private void assertAuctionClosed() {
        int count = auction.actions.size();
        PowerPlantNumber number = new PowerPlantNumber(13);
        assertTrue(controller.selectPowerPlantToBid(RED, number, 13).isPresent());
        assertTrue(controller.bidForCurrentPowerPlant(RED, 14).isPresent());
        assertTrue(controller.doNotBidForCurrentPowerPlant(RED).isPresent());
        assertTrue(controller.doNotWantPowerPlant(RED).isPresent());
        assertTrue(controller.removePowerPlant(RED, number, List.of()).isPresent());
        assertEquals(count, auction.actions.size());
    }

    @Test
    public void adapterPlaysARoundAndReturnsTheNewPhaseAfterItsLastPlayer() {
        JSONObject buying = finishAuction();
        assertEquals(List.of("RED", "BLUE"), buying.getJSONArray("playerOrder").toList());
        assertEquals("BLUE", buying.getJSONObject("phaseState").getString("currentPlayer"));
        accepted(adapter.buyResources(2, "OO"));
        assertEquals(List.of(Resource.OIL, Resource.OIL), blue.boughtResources);
        JSONObject building = accepted(adapter.buyResources(1, ""));
        assertEquals(BUY_HOUSES.name(), building.getString("gamePhase"));
        assertEquals("BLUE", building.getJSONObject("phaseState").getString("currentPlayer"));
        accepted(adapter.buyHouses(2, List.of("Essen")));
        JSONObject paying = accepted(adapter.buyHouses(1, List.of()));
        assertEquals(BUREAUCRACY.name(), paying.getString("gamePhase"));
        assertEquals("RED", paying.getJSONObject("phaseState").getString("currentPlayer"));
        accepted(adapter.usePowerPlants(1, List.of(), ""));
        assertEquals(0, market.preparations);
        JSONObject next = accepted(adapter.usePowerPlants(2, List.of(7), "OOO"));
        assertEquals(List.of(1), blue.paidFor);
        assertEquals(List.of(0), red.paidFor);
        assertEquals(1, market.preparations);
        assertEquals(1, next.getJSONObject("resourceMarket").getInt("coal"));
        assertFalse(next.getBoolean("isFirstTurn"));
        assertEquals(AUCTION_POWER_PLANTS.name(), next.getString("gamePhase"));
        assertEquals(List.of(BLUE, RED), auction.roundOrders.get(1));
    }

    @Test
    public void laterAuctionsKeepTheOrderSetAtTheBeginningOfTheRound() {
        reachBuilding();
        accepted(adapter.buyHouses(2, List.of()));
        accepted(adapter.buyHouses(1, List.of()));
        accepted(adapter.usePowerPlants(1, List.of(), ""));
        accepted(adapter.usePowerPlants(2, List.of(), ""));
        assertEquals(List.of(RED, BLUE), auction.roundOrders.get(1));
        blue.bestPlant = 20; // Bought in the second auction, while house counts are still tied.
        JSONObject buying = finishAuction();
        assertEquals(List.of("RED", "BLUE"), buying.getJSONArray("playerOrder").toList());
        assertEquals("BLUE", buying.getJSONObject("phaseState").getString("currentPlayer"));
    }

    @Test
    public void activePhaseErrorsReachTheAdapterAndAllowTheSamePlayerToRetry() {
        finishAuction();
        blue.resourceError = Optional.of(new RequestError("Cannot store more oil."));
        assertEquals(Variant.failure(blue.resourceError.get()), adapter.buyResources(2, "O"));
        assertEquals("BLUE", state().getJSONObject("phaseState").getString("currentPlayer"));
        blue.resourceError = Optional.empty();
        accepted(adapter.buyResources(2, "O"));
        accepted(adapter.buyResources(1, ""));

        accepted(adapter.buyHouses(2, List.of()));
        towns.error = new RequestError("Cannot build here.");
        assertEquals(Variant.failure(towns.error), adapter.buyHouses(1, List.of("Essen")));
        assertPhase(BUY_HOUSES);
        accepted(adapter.buyHouses(1, List.of()));
        accepted(adapter.usePowerPlants(1, List.of(), ""));
        RequestError fuelError = new RequestError("Not enough oil.");
        blue.production = Variant.failure(fuelError);
        assertEquals(Variant.failure(fuelError), adapter.usePowerPlants(2, List.of(7), "O"));
        assertPhase(BUREAUCRACY);
        assertEquals(0, market.preparations);
        blue.production = Variant.success(0);
        accepted(adapter.usePowerPlants(2, List.of(), ""));
        assertPhase(AUCTION_POWER_PLANTS);
    }

    @Test
    public void stepThreeDrawnAtAuctionTakesEffectBeforeResourceBuying() {
        market.step3Drawn = true;
        accepted(adapter.bidForCurrentPowerPlant(1, 15));
        assertEquals(1, state().getInt("step"));
        assertEquals(3, finishAuction().getInt("step"));
        assertEquals(3, towns.step);
    }

    @Test
    public void buildingStartsStepTwoAndTheNextAuctionUsesStepThreeDrawnInBureaucracy() {
        reachBuilding();
        towns.houses.put(BLUE, 6);
        accepted(adapter.buyHouses(2, List.of("Essen")));
        assertEquals(1, state().getInt("step"));
        assertEquals(2, accepted(adapter.buyHouses(1, List.of())).getInt("step"));
        market.drawAtPreparation = true;
        accepted(adapter.usePowerPlants(1, List.of(), ""));
        JSONObject next = accepted(adapter.usePowerPlants(2, List.of(), ""));
        assertEquals(3, next.getInt("step"));
        assertEquals(2, next.getJSONObject("resourceMarket").getInt("coal"));
    }

    @Test
    public void aStepThreeCardAlreadyDrawnDuringBuildingAppliesAtTheNextPhase() {
        reachBuilding();
        accepted(adapter.buyHouses(2, List.of("Essen")));
        market.step3Drawn = true;
        assertEquals(1, state().getInt("step"));
        JSONObject paying = accepted(adapter.buyHouses(1, List.of()));
        assertEquals(BUREAUCRACY.name(), paying.getString("gamePhase"));
        assertEquals(3, paying.getInt("step"));
    }

    @Test
    public void drawingTheCardWhenStepTwoStartsKeepsItsRefillUntilTheNextRound() {
        reachBuilding();
        towns.houses.put(BLUE, 6);
        market.drawAtStepTwo = true;
        accepted(adapter.buyHouses(2, List.of("Essen")));
        JSONObject paying = accepted(adapter.buyHouses(1, List.of()));
        assertTrue(market.step3Drawn);
        assertEquals(2, paying.getInt("step"));
        accepted(adapter.usePowerPlants(1, List.of(), ""));
        JSONObject next = accepted(adapter.usePowerPlants(2, List.of(), ""));
        assertEquals(2, next.getJSONObject("resourceMarket").getInt("coal"));
        assertEquals(3, next.getInt("step"));
    }

    @Test
    public void terminalBuildPublishesTheRankingWithoutPayingIncomeOrStartingAnotherRound() {
        reachBuilding();
        towns.houses.put(RED, 17);
        towns.houses.put(BLUE, 6);
        red.capacity = 2;
        blue.capacity = 6;
        accepted(adapter.buyHouses(2, List.of()));
        JSONObject end = accepted(adapter.buyHouses(1, List.of("Essen")));
        assertEquals(GAME_END.name(), end.getString("gamePhase"));
        assertEquals(List.of("BLUE", "RED"), end.getJSONArray("gameResult").toList());
        assertTrue(end.isNull("phaseState"));
        assertEquals(1, towns.step);
        assertAuctionClosed();
        assertTrue(controller.buyResources(BLUE, List.of()).isPresent());
        assertTrue(controller.buyHouses(RED, List.of()).isPresent());
        assertTrue(controller.usePowerPlants(RED, List.of(), List.of()).isPresent());
        assertTrue(end.similar(state()));
        assertEquals(List.of(), red.paidFor);
        assertEquals(List.of(), blue.paidFor);
        assertEquals(0, market.preparations);
        assertEquals(1, auction.roundOrders.size());
    }

    private static final class Auction implements AuctionPowerPlantsPort {
        private final List<List<PlayerColor>> roundOrders = new ArrayList<>();
        private final List<List<?>> actions = new ArrayList<>();
        private Variant<PhaseStatus, RequestError> response = Variant.success(PhaseStatus.PHASE_CONTINUES);

        @Override
        public void startPhase(List<PlayerColor> order) {
            roundOrders.add(List.copyOf(order));
        }

        @Override
        public Variant<PhaseStatus, RequestError> selectPowerPlantToBid(PlayerColor player, PowerPlantNumber number,
                int firstBid) {
            actions.add(List.of("select", player, number, firstBid));
            return response;
        }

        @Override
        public Variant<PhaseStatus, RequestError> doNotWantPowerPlant(PlayerColor player) {
            actions.add(List.of("passAuction", player));
            return response;
        }

        @Override
        public Variant<PhaseStatus, RequestError> bidForCurrentPowerPlant(PlayerColor player, int bid) {
            actions.add(List.of("bid", player, bid));
            return response;
        }

        @Override
        public Variant<PhaseStatus, RequestError> doNotBidForCurrentPowerPlant(PlayerColor player) {
            actions.add(List.of("passBid", player));
            return response;
        }

        @Override
        public Variant<PhaseStatus, RequestError> removePowerPlant(PlayerColor player, PowerPlantNumber number,
                List<Resource> removedResources) {
            actions.add(List.of("remove", player, number, List.copyOf(removedResources)));
            return response;
        }

        @Override
        public String getState() {
            return "initial auction";
        }
    }

    private static final class Board extends PhaseTestDoubles.Board {
        private final List<Integer> paidFor = new ArrayList<>();
        private int bestPlant;
        private int capacity;
        private Variant<Integer, RequestError> production = Variant.success(2);

        Board(int bestPlant) {
            this.bestPlant = bestPlant;
        }

        @Override
        public PowerPlantNumber bestPowerPlantNumber() {
            return new PowerPlantNumber(bestPlant);
        }

        @Override
        public int numberOfConnectableCities() {
            return capacity;
        }

        @Override
        public Variant<Integer, RequestError> usePowerPlants(List<PowerPlantNumber> numbers, List<Resource> fuel) {
            return numbers.isEmpty() ? Variant.success(0) : production;
        }

        @Override
        public void receiveMoney(int suppliedTowns) {
            paidFor.add(suppliedTowns);
        }

        @Override
        public String state() {
            return new JSONObject().put("money", money).toString();
        }
    }

    private static final class Towns extends PhaseTestDoubles.Towns {
        private int step = 1;

        @Override
        public void startStep2() {
            step = 2;
        }

        @Override
        public void startStep3() {
            step = 3;
        }
    }

    private static final class Market extends PhaseTestDoubles.Market {
        private boolean step3Drawn;
        private boolean drawAtPreparation;
        private boolean drawAtStepTwo;
        private int preparations;

        @Override
        public List<PowerPlant> currentMarket() {
            return List.of(new PowerPlant(new PowerPlantNumber(7), List.of(Resource.OIL), 3, 2));
        }

        @Override
        public List<PowerPlant> futureMarket() {
            return List.of(new PowerPlant(new PowerPlantNumber(13), List.of(), 0, 1));
        }

        @Override
        public boolean isStep3Start() {
            return step3Drawn;
        }

        @Override
        public void prepareNextTurn() {
            preparations++;
            step3Drawn |= drawAtPreparation;
        }

        @Override
        public void startStep2() {
            step3Drawn |= drawAtStepTwo;
        }

        @Override
        public void startStep3() {
        }
    }
}
