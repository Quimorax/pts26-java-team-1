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
}
