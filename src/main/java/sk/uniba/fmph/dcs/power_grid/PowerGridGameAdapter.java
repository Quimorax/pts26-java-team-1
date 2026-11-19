package sk.uniba.fmph.dcs.power_grid;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PowerGridGameAdapter {
    private final GameControllerPort controller;
    private final Map<Integer, PlayerColor> players;

    public PowerGridGameAdapter(final GameControllerPort controller, final Map<Integer, PlayerColor> players) {
        this.controller = Objects.requireNonNull(controller);
        this.players = Map.copyOf(players);
    }

    public Variant<SerializedGameState, RequestError> doNotWantPowerPlant(final int playerId) {
        PlayerColor player = players.get(playerId);

        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        Optional<RequestError> error = controller.doNotWantPowerPlant(player);
        return createResponse(error);
    }

    private Variant<SerializedGameState, RequestError> createResponse(final Optional<RequestError> error) {
        if (error.isPresent()) {
            return Variant.failure(error.get());
        }
        return Variant.success(new SerializedGameState(controller.getState()));
    }
}

