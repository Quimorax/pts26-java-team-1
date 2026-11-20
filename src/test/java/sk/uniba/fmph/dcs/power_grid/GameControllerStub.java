package sk.uniba.fmph.dcs.power_grid;
import java.util.List;
import java.util.Optional;

public final class GameControllerStub implements GameControllerPort {
    private Optional<RequestError> error = Optional.empty();
    private PlayerColor receivedPlayer;
    private int callCount;
    private PowerPlantNumber receivedNumber;
    private int receivedFirstBid;
    private int receivedBid;
    private List<Resource> receivedResources;
    private String lastAction;

    public void setError(final RequestError error) {
        this.error = Optional.of(error);
    }

    public PlayerColor receivedPlayer() {
        return receivedPlayer;
    }

    public int callCount() {
        return callCount;
    }

    public PowerPlantNumber receivedNumber() {
        return receivedNumber;
    }

    public int receivedFirstBid() {
        return receivedFirstBid;
    }

    public int receivedBid() {
        return receivedBid;
    }

    public List<Resource> receivedResources() {
        return receivedResources;
    }

    public String lastAction() {
        return lastAction;
    }

    @Override
    public Optional<RequestError> doNotWantPowerPlant(final PlayerColor player) {
        receivedPlayer = player;
        lastAction = "doNotWantPowerPlant";
        callCount++;
        return error;
    }

    @Override
    public String getState() {
        return "test state";
    }

    @Override
    public Optional<RequestError> selectPowerPlantToBid(final PlayerColor player, final PowerPlantNumber number, final int firstBid) {
        receivedPlayer = player;
        receivedNumber = number;
        receivedFirstBid = firstBid;
        callCount++;
        return error;
    }

    @Override
    public Optional<RequestError> bidForCurrentPowerPlant(final PlayerColor player, final int bid) {
        receivedPlayer = player;
        receivedBid = bid;
        lastAction = "bidForCurrentPowerPlant";
        callCount++;
        return error;
    }

    @Override
    public Optional<RequestError> doNotBidForCurrentPowerPlant(final PlayerColor player) {
        receivedPlayer = player;
        lastAction = "doNotBidForCurrentPowerPlant";
        callCount++;
        return error;
    }

    @Override
    public Optional<RequestError> removePowerPlant(final PlayerColor player, final PowerPlantNumber number, final List<Resource> removedResources) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RequestError> buyResources(final PlayerColor player, final List<Resource> resources) {
        receivedPlayer = player;
        receivedResources = List.copyOf(resources);
        lastAction = "buyResources";
        callCount++;
        return error;
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


