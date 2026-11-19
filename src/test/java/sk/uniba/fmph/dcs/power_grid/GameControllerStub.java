package sk.uniba.fmph.dcs.power_grid;
import java.util.List;
import java.util.Optional;

public final class GameControllerStub implements GameControllerPort {
    private Optional<RequestError> error = Optional.empty();
    private PlayerColor receivedPlayer;
    private int callCount;

    public void setError(final RequestError error) {
        this.error = Optional.of(error);
    }

    public PlayerColor receivedPlayer() {
        return receivedPlayer;
    }

    public int callCount() {
        return callCount;
    }

    @Override
    public Optional<RequestError> doNotWantPowerPlant(final PlayerColor player) {
        receivedPlayer = player;
        callCount++;
        return error;
    }

    @Override
    public String getState() {
        return "test state";
    }

    @Override
    public Optional<RequestError> selectPowerPlantToBid(final PlayerColor player, final PowerPlantNumber number, final int firstBid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RequestError> bidForCurrentPowerPlant(final PlayerColor player, final int bid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RequestError> doNotBidForCurrentPowerPlant(final PlayerColor player) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RequestError> removePowerPlant(final PlayerColor player, final PowerPlantNumber number, final List<Resource> removedResources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RequestError> buyResources(final PlayerColor player, final List<Resource> resources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RequestError> buyHouses(final PlayerColor player, final List<String> townNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RequestError> usePowerPlants(final PlayerColor player, final List<PowerPlantNumber> numbers, final List<Resource> resourcesToBurn) {
        throw new UnsupportedOperationException();
    }
}

