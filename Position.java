public class Position {
    private final int x, y;

    public Position(int XPosition, int YPosition) {
        x = XPosition;
        y = YPosition;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    /**
     * @return True if this position is a corner on the game board, False otherwise.
     */
    public boolean isCorner() {
        int size = GameLogic.BOARD_SIZE - 1;
        if (x == 0 && y == 0)
            return true;
        else if (x == size && y == 0)
            return true;
        else if (x == size && y == size)
            return true;
        else return x == 0 && y == size;
    }

    /**
     * Measures the Manhattan-distance between this position and a given one.
     *
     * @param pos Position to measure distance to.
     * @return Manhattan-Distance between this position and the given one (can't be negative).
     */
    public int manhattanDistanceTo(Position pos) {
        return Math.abs(pos.x - x) + Math.abs(pos.y - y);
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
