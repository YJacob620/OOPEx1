import java.util.ArrayList;

public abstract class ConcretePiece implements Piece {
    private final Player owner;
    private final String type;
    private final String id;
    private ArrayList<Position> positionHistory;
    private int totalDistance;

    protected ConcretePiece(Player owner, String type, String id) {
        this.owner = owner;
        this.type = type;
        this.id = id;
        positionHistory = new ArrayList<>();
        totalDistance = 0;
    }

    @Override
    public Player getOwner() {
        return owner;
    }

    @Override
    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    /* Stores visited positions (if moved at least once)*/
    public ArrayList<Position> getPositionHistory() {
        return positionHistory;
    }

    public void addToTotalDistance(int distance) {
        totalDistance += distance;
    }

    /**
     * Registers a movement for this piece: Updates its positionHistory and totalDistance.
     *
     * @param a Position before movement.
     * @param b Position after movement.
     */
    public void addMovement(Position a, Position b) {
        if (positionHistory.isEmpty()) {
            positionHistory.add(a);
            positionHistory.add(b);
        } else
            positionHistory.add(b);
        int distance = a.manhattanDistanceTo(b);
        addToTotalDistance(distance);
        if (GameLogic.CONSOLE_PRINT)
            System.out.println(id + ": " + positionHistory + " . Total distance = " + totalDistance);
    }

    /**
     * Un-registers the last movement of this piece: Updates positionHistory and totalDistance.
     */
    public void undoLastMovement() {
        if (getPositionHistory().size() <= 2) {
            positionHistory = new ArrayList<>();
            totalDistance = 0;
        } else {
            Position lastPos = positionHistory.remove(0);
            this.addToTotalDistance(-(lastPos.manhattanDistanceTo(positionHistory.get(positionHistory.size()-1))));
        }
    }
}
