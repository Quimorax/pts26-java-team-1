package sk.uniba.fmph.dcs.power_grid;
import static org.junit.Assert.assertEquals;
import java.util.Map;
import org.junit.Test;

public class PowerGridGameAdapterTest {
    @Test
    public void returnsStateAfterSuccessfulAction() {
        GameControllerStub controller = new GameControllerStub();
        PowerGridGameAdapter adapter = new PowerGridGameAdapter(controller, Map.of(1, PlayerColor.RED));

        Variant<SerializedGameState, RequestError> result = adapter.doNotWantPowerPlant(1);

        assertEquals(Variant.success(new SerializedGameState("test state")), result);
        assertEquals(PlayerColor.RED, controller.receivedPlayer());
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

}

