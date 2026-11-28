package sk.uniba.fmph.dcs.power_grid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Routes player commands and runs the automatic transitions between phases of a game. */
public final class GameController implements GameControllerPort {
    private final AuctionPowerPlantsPort auction;
    private final BuyingResources buying;
    private final BuildingHouses building;
    private final Bureaucracy bureaucracy;
    private final DeterminePlayerOrder order;
    private final CheckAndBumpStep steps;
    private final TownsPort towns;
    private final ResourceMarket resources;
    private final PowerPlantMarketPort plants;
    private final Map<PlayerColor, PlayerBoardPort> players;

    private GamePhase gamePhase = GamePhase.AUCTION_POWER_PLANTS;
    private boolean isFirstTurn = true;
    private List<PlayerColor> playerOrder;
    private List<PlayerColor> gameResult = List.of();

    /**
     * Starts the first auction. All phases operate on the same boards, networks, and markets.
     * @param auction auction phase operating on this game's boards and plant market
     * @param towns connected cities
     * @param resources resource market
     * @param plants power plant market
     * @param players participating players and their boards
     * @param random source for the initial player order
     */
    public GameController(final AuctionPowerPlantsPort auction, final TownsPort towns, final ResourceMarket resources,
            final PowerPlantMarketPort plants, final Map<PlayerColor, PlayerBoardPort> players, final Random random) {
        this.auction = Objects.requireNonNull(auction);
        this.towns = Objects.requireNonNull(towns);
        this.resources = Objects.requireNonNull(resources);
        this.plants = Objects.requireNonNull(plants);
        this.players = Map.copyOf(players);
        buying = new BuyingResources(this.players);
        building = new BuildingHouses(towns, plants, this.players);
        bureaucracy = new Bureaucracy(towns, resources, plants, this.players);
        steps = new CheckAndBumpStep(towns, plants, resources, this.players.size());
        order = new DeterminePlayerOrder(towns, this.players);
        playerOrder = List.copyOf(order.initialOrder(Objects.requireNonNull(random)));
        auction.startPhase(playerOrder);
    }

    @Override
    public Optional<RequestError> selectPowerPlantToBid(final PlayerColor player, final PowerPlantNumber number,
            final int firstBid) {
        return act(GamePhase.AUCTION_POWER_PLANTS, () -> auction.selectPowerPlantToBid(player, number, firstBid));
    }

    @Override
    public Optional<RequestError> doNotWantPowerPlant(final PlayerColor player) {
        return act(GamePhase.AUCTION_POWER_PLANTS, () -> auction.doNotWantPowerPlant(player));
    }

    @Override
    public Optional<RequestError> bidForCurrentPowerPlant(final PlayerColor player, final int bid) {
        return act(GamePhase.AUCTION_POWER_PLANTS, () -> auction.bidForCurrentPowerPlant(player, bid));
    }

    @Override
    public Optional<RequestError> doNotBidForCurrentPowerPlant(final PlayerColor player) {
        return act(GamePhase.AUCTION_POWER_PLANTS, () -> auction.doNotBidForCurrentPowerPlant(player));
    }

    @Override
    public Optional<RequestError> removePowerPlant(final PlayerColor player, final PowerPlantNumber number,
            final List<Resource> removedResources) {
        return act(GamePhase.AUCTION_POWER_PLANTS, () -> auction.removePowerPlant(player, number, removedResources));
    }

    @Override
    public Optional<RequestError> buyResources(final PlayerColor player, final List<Resource> tokens) {
        return act(GamePhase.BUY_RESOURCES, () -> buying.buyResources(player, tokens));
    }

    @Override
    public Optional<RequestError> buyHouses(final PlayerColor player, final List<String> townNames) {
        return act(GamePhase.BUY_HOUSES, () -> building.buyHouses(player, townNames));
    }

    @Override
    public Optional<RequestError> usePowerPlants(final PlayerColor player, final List<PowerPlantNumber> numbers,
            final List<Resource> resourcesToBurn) {
        return act(GamePhase.BUREAUCRACY, () -> bureaucracy.usePowerPlants(player, numbers, resourcesToBurn));
    }

    private Optional<RequestError> act(final GamePhase required,
            final Supplier<Variant<PhaseStatus, RequestError>> action) {
        if (gamePhase != required) {
            return Optional.of(new RequestError("This action requires " + required + ". Current phase: " + gamePhase));
        }
        Variant<PhaseStatus, RequestError> result = action.get();
        if (result instanceof Variant.Failure<PhaseStatus, RequestError> failure) {
            return Optional.of(failure.error());
        }
        PhaseStatus status = ((Variant.Success<PhaseStatus, RequestError>) result).value();
        if (status == PhaseStatus.PHASE_ENDS) {
            advancePhase();
        } else if (status == PhaseStatus.GAME_ENDS) {
            gameResult = order.gameResult();
            gamePhase = GamePhase.GAME_END;
        }
        return Optional.empty();
    }

    private void advancePhase() {
        gamePhase = switch (gamePhase) {
            case AUCTION_POWER_PLANTS -> {
                if (isFirstTurn) {
                    playerOrder = order.reorder();
                }
                steps.checkAndBumpToStep3();
                buying.startPhase(playerOrder);
                yield GamePhase.BUY_RESOURCES;
            }
            case BUY_RESOURCES -> {
                building.startPhase(playerOrder);
                yield GamePhase.BUY_HOUSES;
            }
            case BUY_HOUSES -> {
                // A card drawn during building takes effect now. A draw from starting step 2
                // belongs to bureaucracy and takes effect at the start of the next round.
                steps.checkAndBumpToStep3();
                steps.checkAndBumpToStep2();
                bureaucracy.startPhase(playerOrder);
                yield GamePhase.BUREAUCRACY;
            }
            case BUREAUCRACY -> {
                steps.checkAndBumpToStep3();
                playerOrder = order.reorder();
                auction.startPhase(playerOrder);
                isFirstTurn = false;
                yield GamePhase.AUCTION_POWER_PLANTS;
            }
            case GAME_END -> throw new IllegalStateException("The game has ended.");
        };
    }

    @Override
    public String getState() {
        Object phaseState = switch (gamePhase) {
            case AUCTION_POWER_PLANTS -> embeddedState(auction.getState());
            case BUY_RESOURCES -> new JSONObject(buying.getState());
            case BUY_HOUSES -> new JSONObject(building.getState());
            case BUREAUCRACY -> new JSONObject(bureaucracy.getState());
            case GAME_END -> JSONObject.NULL;
        };
        JSONObject boards = new JSONObject();
        players.forEach((color, board) -> boards.put(color.name(), new JSONObject()
                .put("houses", towns.getNumberOfHouses(color))
                .put("board", embeddedState(board.state()))));
        return new JSONObject()
                .put("gamePhase", gamePhase.name())
                .put("isFirstTurn", isFirstTurn)
                .put("step", steps.getStep())
                .put("playerOrder", new JSONArray(playerOrder))
                .put("phaseState", phaseState)
                .put("players", boards)
                .put("resourceMarket", embeddedState(resources.toString()))
                .put("powerPlantMarket", new JSONObject()
                        .put("current", plantCards(plants.currentMarket()))
                        .put("future", plantCards(plants.futureMarket())))
                .put("gameResult", new JSONArray(gameResult))
                .toString();
    }

    private static JSONArray plantCards(final List<PowerPlant> market) {
        JSONArray cards = new JSONArray();
        for (PowerPlant plant : market) {
            cards.put(new JSONObject().put("number", plant.number().number())
                    .put("acceptedResources", new JSONArray(plant.acceptedResources()))
                    .put("resourceAmount", plant.resourceAmount())
                    .put("numberOfCities", plant.numberOfCities()));
        }
        return cards;
    }

    // Ports expose strings. Embed structured snapshots as JSON and preserve plain-text states.
    private static Object embeddedState(final String state) {
        if (state == null) {
            return JSONObject.NULL;
        }
        try {
            JSONTokener tokens = new JSONTokener(state);
            Object value = tokens.nextValue();
            if ((value instanceof JSONObject || value instanceof JSONArray) && tokens.nextClean() == 0) {
                return value;
            }
        } catch (JSONException ignored) {
            // An individual component may supply plain text.
        }
        return state;
    }
}
