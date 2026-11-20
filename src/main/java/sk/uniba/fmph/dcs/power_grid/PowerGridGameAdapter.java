package sk.uniba.fmph.dcs.power_grid;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public final class PowerGridGameAdapter implements PowerGridRulesGameInterface {
    private final GameControllerPort controller;
    private final Map<Integer, PlayerColor> players;

    public PowerGridGameAdapter(final GameControllerPort controller, final Map<Integer, PlayerColor> players) {
        this.controller = Objects.requireNonNull(controller);
        this.players = Map.copyOf(players);
    }

    @Override
    public Variant<SerializedGameState, RequestError> doNotWantPowerPlant(final int playerId) {
        PlayerColor player = players.get(playerId);

        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        Optional<RequestError> error = controller.doNotWantPowerPlant(player);
        return createResponse(error);
    }

    @Override
    public Variant<SerializedGameState, RequestError> selectPowerPlantToBid(final int playerId, final int powerPlantNumber, final int firstBid) {
        PlayerColor player = players.get(playerId);

        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        if (powerPlantNumber < 1) {
            return Variant.failure(new RequestError("A power plant number must be positive."));
        }

        PowerPlantNumber number = new PowerPlantNumber(powerPlantNumber);
        Optional<RequestError> error = controller.selectPowerPlantToBid(player, number, firstBid);
        return createResponse(error);
    }

    @Override
    public Variant<SerializedGameState, RequestError> bidForCurrentPowerPlant(final int playerId, final int bid) {
        PlayerColor player = players.get(playerId);

        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        Optional<RequestError> error = controller.bidForCurrentPowerPlant(player, bid);

        return createResponse(error);
    }

    @Override
    public Variant<SerializedGameState, RequestError> doNotBidForCurrentPowerPlant(final int playerId) {
        PlayerColor player = players.get(playerId);

        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        Optional<RequestError> error = controller.doNotBidForCurrentPowerPlant(player);

        return createResponse(error);
    }

    @Override
    public Variant<SerializedGameState, RequestError> buyResources(final int playerId, final String resources) {
        PlayerColor player = players.get(playerId);
        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        Optional<List<Resource>> parsed = parseResources(resources);
        if (parsed.isEmpty()) {
            return Variant.failure(new RequestError("Invalid resource string."));
        }

        return createResponse(controller.buyResources(player, parsed.get()));
    }

    @Override
    public Variant<SerializedGameState, RequestError> removePowerPlant(final int playerId, final int powerPlantNumber, final String removedResources) {
        PlayerColor player = players.get(playerId);
        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        if (powerPlantNumber < 1) {
            return Variant.failure(new RequestError("A power plant number must be positive."));
        }

        Optional<List<Resource>> parsed = parseResources(removedResources);
        if (parsed.isEmpty()) {
            return Variant.failure(new RequestError("Invalid resource string."));
        }

        PowerPlantNumber number = new PowerPlantNumber(powerPlantNumber);
        return createResponse(controller.removePowerPlant(player, number, parsed.get()));
    }

    @Override
    public Variant<SerializedGameState, RequestError> buyHouses(final int playerId, final List<String> townNames) {
        PlayerColor player = players.get(playerId);
        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        if (townNames == null) {
            return Variant.failure(new RequestError("Town list is missing."));
        }

        for (String townName : townNames) {
            if (townName == null) {
                return Variant.failure(new RequestError("Town name is missing."));
            }
        }

        return createResponse(controller.buyHouses(player, List.copyOf(townNames)));
    }

    @Override
    public Variant<SerializedGameState, RequestError> usePowerPlants(final int playerId, final List<Integer> powerPlantNumbers, final String resourcesToBurn) {
        PlayerColor player = players.get(playerId);
        if (player == null) {
            return Variant.failure(new RequestError("Unknown player."));
        }

        if (powerPlantNumbers == null) {
            return Variant.failure(new RequestError("Power plant list is missing."));
        }

        List<PowerPlantNumber> numbers = new ArrayList<>();
        for (Integer number : powerPlantNumbers) {
            if (number == null || number < 1) {
                return Variant.failure(new RequestError(
                        "A power plant number must be positive."));
            }
            numbers.add(new PowerPlantNumber(number));
        }

        Optional<List<Resource>> parsed = parseResources(resourcesToBurn);
        if (parsed.isEmpty()) {
            return Variant.failure(new RequestError("Invalid resource string."));
        }

        return createResponse(controller.usePowerPlants(player, numbers, parsed.get()));
    }

    private Optional<List<Resource>> parseResources(final String resources) {
        if (resources == null) {
            return Optional.empty();
        }

        List<Resource> result = new ArrayList<>();
        for (char code : resources.toCharArray()) {
            switch (code) {
                case 'C' -> result.add(Resource.COAL);
                case 'O' -> result.add(Resource.OIL);
                case 'G' -> result.add(Resource.GARBAGE);
                case 'U' -> result.add(Resource.URANIUM);
                default -> {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(result);
    }

    private Variant<SerializedGameState, RequestError> createResponse(final Optional<RequestError> error) {
        if (error.isPresent()) {
            return Variant.failure(error.get());
        }
        return Variant.success(new SerializedGameState(controller.getState()));
    }
}

