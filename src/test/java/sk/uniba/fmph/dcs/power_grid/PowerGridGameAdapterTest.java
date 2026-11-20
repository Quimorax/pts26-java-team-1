package sk.uniba.fmph.dcs.power_grid;
import static org.junit.Assert.assertEquals;
import java.util.Map;
import java.util.List;
import org.junit.Test;

public class PowerGridGameAdapterTest {
    @Test
    public void returnsStateAfterSuccessfulAction() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(1, PlayerColor.RED));

        Variant<SerializedGameState, RequestError> result = adapter.doNotWantPowerPlant(1);

        assertEquals(Variant.success(new SerializedGameState("test state")), result);
        assertEquals(PlayerColor.RED, controller.receivedPlayer());
        assertEquals("doNotWantPowerPlant", controller.lastAction());
        assertEquals(1, controller.callCount());
    }

    @Test
    public void returnsControllerError() {
        GameControllerStub controller = new GameControllerStub();
        RequestError error = new RequestError("It is not your turn.");
        controller.setError(error);

        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(1, PlayerColor.RED));

        Variant<SerializedGameState, RequestError> result = adapter.doNotWantPowerPlant(1);

        assertEquals(Variant.failure(error), result);
        assertEquals(PlayerColor.RED, controller.receivedPlayer());
        assertEquals(1, controller.callCount());
    }

    @Test
    public void rejectsUnknownPlayerWithoutCallingController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(1, PlayerColor.RED));

        Variant<SerializedGameState, RequestError> result = adapter.doNotWantPowerPlant(99);

        assertEquals(Variant.failure(new RequestError("Unknown player.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void passesPlantNumberAndBidToController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLUE));

        Variant<SerializedGameState, RequestError> result = adapter.selectPowerPlantToBid(42, 7, 10);

        assertEquals(PlayerColor.BLUE, controller.receivedPlayer());
        assertEquals(new PowerPlantNumber(7), controller.receivedNumber());
        assertEquals(10, controller.receivedFirstBid());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void returnsErrorWhenControllerRejectsPlantSelection() {
        GameControllerStub controller = new GameControllerStub();
        RequestError error = new RequestError("The plant is not available.");
        controller.setError(error);

        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLUE));

        Variant<SerializedGameState, RequestError> result = adapter.selectPowerPlantToBid(42, 7, 10);

        assertEquals(Variant.failure(error), result);
        assertEquals(1, controller.callCount());
    }

    @Test
    public void rejectsPlantSelectionForUnknownPlayer() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLUE));

        Variant<SerializedGameState, RequestError> result = adapter.selectPowerPlantToBid(99, 7, 10);

        assertEquals(Variant.failure(new RequestError("Unknown player.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void rejectsZeroPlantNumber() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLUE));

        Variant<SerializedGameState, RequestError> result = adapter.selectPowerPlantToBid(42, 0, 10);

        assertEquals(Variant.failure(new RequestError("A power plant number must be positive.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void rejectsNegativePlantNumber() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLUE));

        Variant<SerializedGameState, RequestError> result = adapter.selectPowerPlantToBid(42, -5, 10);

        assertEquals(Variant.failure(new RequestError("A power plant number must be positive.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void passesBidToController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.PURPLE));

        Variant<SerializedGameState, RequestError> result = adapter.bidForCurrentPowerPlant(42, 15);

        assertEquals("bidForCurrentPowerPlant", controller.lastAction());
        assertEquals(PlayerColor.PURPLE, controller.receivedPlayer());
        assertEquals(15, controller.receivedBid());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void returnsControllerErrorForBid() {
        GameControllerStub controller = new GameControllerStub();
        RequestError error = new RequestError("The bid is too low.");
        controller.setError(error);

        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLUE));

        Variant<SerializedGameState, RequestError> result = adapter.bidForCurrentPowerPlant(42, 15);

        assertEquals(Variant.failure(error), result);
        assertEquals("bidForCurrentPowerPlant", controller.lastAction());
        assertEquals(1, controller.callCount());
    }

    @Test
    public void rejectsBidFromUnknownPlayer() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.YELLOW));

        Variant<SerializedGameState, RequestError> result = adapter.bidForCurrentPowerPlant(99, 15);

        assertEquals(Variant.failure(new RequestError("Unknown player.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void passesDecisionNotToBidToController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLUE));

        Variant<SerializedGameState, RequestError> result = adapter.doNotBidForCurrentPowerPlant(42);

        assertEquals("doNotBidForCurrentPowerPlant", controller.lastAction());
        assertEquals(PlayerColor.BLUE, controller.receivedPlayer());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void returnsControllerErrorForDecisionNotToBid() {
        GameControllerStub controller = new GameControllerStub();
        RequestError error = new RequestError("It is not your turn.");
        controller.setError(error);

        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLUE));

        Variant<SerializedGameState, RequestError> result = adapter.doNotBidForCurrentPowerPlant(42);

        assertEquals(Variant.failure(error), result);
        assertEquals("doNotBidForCurrentPowerPlant", controller.lastAction());
        assertEquals(1, controller.callCount());
    }

    @Test
    public void rejectsDecisionNotToBidFromUnknownPlayer() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.GREEN));

        Variant<SerializedGameState, RequestError> result = adapter.doNotBidForCurrentPowerPlant(99);

        assertEquals(Variant.failure(new RequestError("Unknown player.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void convertsResourceCodesAndKeepsRepeatedResources() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.GREEN));

        Variant<SerializedGameState, RequestError> result = adapter.buyResources(42, "CCOGU");

        assertEquals("buyResources", controller.lastAction());
        assertEquals(PlayerColor.GREEN, controller.receivedPlayer());
        assertEquals(List.of(Resource.COAL, Resource.COAL, Resource.OIL, Resource.GARBAGE, Resource.URANIUM), controller.receivedResources());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void rejectsUnknownResourceWithoutCallingController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.GREEN));

        Variant<SerializedGameState, RequestError> result = adapter.buyResources(42, "CX");

        assertEquals(Variant.failure(new RequestError("Invalid resource string.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void passesEmptyResourceListToController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.GREEN));

        Variant<SerializedGameState, RequestError> result = adapter.buyResources(42, "");

        assertEquals(List.of(), controller.receivedResources());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void passesPlantAndRemovedResourcesToController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(
                controller, Map.of(42, PlayerColor.GREEN));

        Variant<SerializedGameState, RequestError> result =
                adapter.removePowerPlant(42, 7, "CO");

        assertEquals("removePowerPlant", controller.lastAction());
        assertEquals(PlayerColor.GREEN, controller.receivedPlayer());
        assertEquals(new PowerPlantNumber(7), controller.receivedNumber());
        assertEquals(List.of(Resource.COAL, Resource.OIL), controller.receivedResources());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void rejectsInvalidPlantNumberBeforeRemoval() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.GREEN));

        Variant<SerializedGameState, RequestError> result = adapter.removePowerPlant(42, 0, "C");

        assertEquals(Variant.failure(new RequestError("A power plant number must be positive.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void rejectsInvalidResourcesBeforeRemovingPlant() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.GREEN));

        Variant<SerializedGameState, RequestError> result = adapter.removePowerPlant(42, 7, "CX");

        assertEquals(Variant.failure(new RequestError("Invalid resource string.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void passesTownNamesToController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLACK));
        List<String> towns = List.of("Berlin", "Hamburg");

        Variant<SerializedGameState, RequestError> result = adapter.buyHouses(42, towns);

        assertEquals("buyHouses", controller.lastAction());
        assertEquals(PlayerColor.BLACK, controller.receivedPlayer());
        assertEquals(towns, controller.receivedTownNames());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void passesEmptyTownListToController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLACK));

        Variant<SerializedGameState, RequestError> result = adapter.buyHouses(42, List.of());

        assertEquals(List.of(), controller.receivedTownNames());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void rejectsMissingTownNameBeforeBuyingHouses() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.BLACK));
        List<String> towns = java.util.Arrays.asList("Berlin", null);

        Variant<SerializedGameState, RequestError> result = adapter.buyHouses(42, towns);

        assertEquals(Variant.failure(new RequestError("Town name is missing.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void passesPlantNumbersAndFuelToController() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridRulesGameInterface adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.YELLOW));

        Variant<SerializedGameState, RequestError> result = adapter.usePowerPlants(42, List.of(7, 12), "CCO");

        assertEquals("usePowerPlants", controller.lastAction());
        assertEquals(PlayerColor.YELLOW, controller.receivedPlayer());
        assertEquals(List.of(new PowerPlantNumber(7), new PowerPlantNumber(12)), controller.receivedNumbers());
        assertEquals(List.of(Resource.COAL, Resource.COAL, Resource.OIL), controller.receivedResources());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

    @Test
    public void rejectsInvalidNumberBeforeUsingAnyPlant() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.YELLOW));

        Variant<SerializedGameState, RequestError> result = adapter.usePowerPlants(42, List.of(7, 0), "C");

        assertEquals(Variant.failure(new RequestError("A power plant number must be positive.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void rejectsInvalidFuelBeforeUsingAnyPlant() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.YELLOW));

        Variant<SerializedGameState, RequestError> result = adapter.usePowerPlants(42, List.of(7, 12), "CX");

        assertEquals(Variant.failure(new RequestError("Invalid resource string.")), result);
        assertEquals(0, controller.callCount());
    }

    @Test
    public void passesSelectedPlantWithEmptyFuelList() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridRulesGameInterface adapter = new PowerGridGameAdapter(controller, Map.of(42, PlayerColor.GREEN));

        Variant<SerializedGameState, RequestError> result = adapter.usePowerPlants(42, List.of(13), "");

        assertEquals("usePowerPlants", controller.lastAction());
        assertEquals(List.of(new PowerPlantNumber(13)), controller.receivedNumbers());
        assertEquals(List.of(), controller.receivedResources());
        assertEquals(1, controller.callCount());
        assertEquals(Variant.success(new SerializedGameState("test state")), result);
    }

}



