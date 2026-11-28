package sk.uniba.fmph.dcs.power_grid;

import java.util.Objects;

import org.json.JSONObject;

/**
 * Manages game steps (Step 1, Step 2, and Step 3) and triggers step transitions.
 */
public final class CheckAndBumpStep {

    private final TownsPort towns;
    private final PowerPlantMarketPort market;
    private final ResourceMarket resourceMarket;
    private final int numberOfPlayers;
    private final int step2Threshold;
    private int step;

    /**
     * Constructs a CheckAndBumpStep controller for a specific number of players.
     *
     * @param towns           towns subsystem port
     * @param market          power plant market port
     * @param resourceMarket  resource market
     * @param numberOfPlayers number of players in the game (2 to 6)
     */
    public CheckAndBumpStep(final TownsPort towns, final PowerPlantMarketPort market,
            final ResourceMarket resourceMarket, final int numberOfPlayers) {
        this.towns = Objects.requireNonNull(towns, "towns must not be null");
        this.market = Objects.requireNonNull(market, "market must not be null");
        this.resourceMarket = Objects.requireNonNull(resourceMarket, "resourceMarket must not be null");
        this.numberOfPlayers = numberOfPlayers;
        this.step2Threshold = switch (numberOfPlayers) {
            case 2, 3, 4, 5 -> 7;
            case 6 -> 6;
            default -> throw new IllegalArgumentException("The game requires 2 to 6 players: " + numberOfPlayers);
        };
        this.step = 1;
    }

    /**
     * Constructs a CheckAndBumpStep controller defaulting to 4 players.
     *
     * @param towns          towns subsystem port
     * @param market         power plant market port
     * @param resourceMarket resource market
     */
    public CheckAndBumpStep(final TownsPort towns, final PowerPlantMarketPort market,
            final ResourceMarket resourceMarket) {
        this(towns, market, resourceMarket, 4);
    }

    /**
     * Returns the current game step (1, 2, or 3).
     *
     * @return current step
     */
    public int getStep() {
        return step;
    }

    /**
     * Checks if any player reached the house threshold for Step 2.
     * If so, transitions game to Step 2 and notifies markets and towns.
     *
     * @return true if game transitioned to Step 2, false otherwise
     */
    public boolean checkAndBumpToStep2() {
        if (step == 1 && towns.getMaxNumberOfHouses() >= step2Threshold) {
            step = 2;
            market.startStep2();
            resourceMarket.startStep2();
            towns.startStep2();
            return true;
        }
        return false;
    }

    /**
     * Checks if the Step 3 card has been drawn.
     * If so, transitions game to Step 3 and notifies markets and towns.
     * If Step 3 occurs while still on Step 1, Step 2 actions are executed first.
     *
     * @return true if game transitioned to Step 3, false otherwise
     */
    public boolean checkAndBumpToStep3() {
        if (step < 3 && market.isStep3Start()) {
            if (step == 1) {
                market.startStep2();
                resourceMarket.startStep2();
                towns.startStep2();
            }
            step = 3;
            market.startStep3();
            resourceMarket.startStep3();
            towns.startStep3();
            return true;
        }
        return false;
    }

    /**
     * Returns a JSON representation of the current step state.
     *
     * @return JSON state string
     */
    public String getState() {
        return new JSONObject()
                .put("step", step)
                .put("numberOfPlayers", numberOfPlayers)
                .put("step2Threshold", step2Threshold)
                .toString();
    }

    @Override
    public String toString() {
        return getState();
    }
}
