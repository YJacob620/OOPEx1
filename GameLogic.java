import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class GameLogic implements PlayableLogic {
    /* Player 1 is the defender, Player 2 is the attacker */
    public static final ConcretePlayer P1 = new ConcretePlayer(true), P2 = new ConcretePlayer(false);
    public static final int BOARD_SIZE = 11, p1NumOfPieces = 13, p2NumOfPieces = 24;
    public static final String P1_Pawn_Unicode = "♙", P1_King_Unicode = "♕", P2_Pawn_Unicode = "♟";
    private boolean isP2Turn;
    private boolean isOver;
    private static Piece[][] board = new Piece[BOARD_SIZE][BOARD_SIZE]; // the game board
    private static Stack<Position> pastMovements;   // keeps track of all the (valid) piece movements in the game
    private static Stack<Integer> killedPiecesCounts;    // keeps track of how many pieces were killed in each turn
    private static Stack<Piece> deadPawns; // stores pawns that were removed from the board
    private static Stack<Position> lastPositionOfDead;  // stores the positions in which pieces have been killed
    private static final String PARTITION = "***************************************************************************";
    public static final boolean CONSOLE_PRINT = false; //TODO

    public GameLogic() {
        isOver = false;
        isP2Turn = true;
        pastMovements = new Stack<>();
        killedPiecesCounts = new Stack<>();
        deadPawns = new Stack<>();
        lastPositionOfDead = new Stack<>();
        resetBoard();
    }

    @Override
    public boolean move(Position a, Position b) {
        Piece mover = getPieceAtPosition(a);
        if (mover != null && getPieceAtPosition(b) == null) {
            if (isSecondPlayerTurn() && mover.getOwner() == P2 || !isSecondPlayerTurn() && mover.getOwner() == P1) {
                if (b.isCorner() && !isKing(mover))
                    return false;
                if (!isPathValid(a, b))
                    return false;
                board[b.getX()][b.getY()] = mover;
                board[a.getX()][a.getY()] = null;
                ((ConcretePiece) mover).addMovement(a, b);  // updates positionHistory and totalDistance of the last piece moved
                pastMovements.push(a);
                pastMovements.push(b);
                checkKillerSurroundings(b);
                isP2Turn = !isP2Turn;   //changes turns
                return true;
            }
        }
        return false;
    }

    @Override
    public Piece getPieceAtPosition(Position position) {
        if (isOutOfBounds(position))
            return null;
        return board[position.getX()][position.getY()];
    }

    /**
     * Checks if a given position is inside the boundaries of the playing board.
     *
     * @param pos Given position.
     * @return True if the position is inside the board, False otherwise.
     */
    private boolean isOutOfBounds(Position pos) {
        return pos.getX() < 0 || pos.getY() < 0 || pos.getX() >= BOARD_SIZE || pos.getY() >= BOARD_SIZE;
    }

    @Override
    public boolean isSecondPlayerTurn() {
        return isP2Turn;
    }

    /**
     * Checks whether a given piece is the king.
     *
     * @param piece Given piece.
     * @return True if the piece is the king, False otherwise.
     */
    private boolean isKing(Piece piece) {
        if (piece != null)
            return piece.getType().equals(P1_King_Unicode);
        return false;
    }

    /**
     * Checks if the path between 2 given positions is on a diagonal or horizontal line and also if it's clear for a
     * piece to pass through.
     *
     * @param a Starting position
     * @param b End position
     * @return True if a piece can move along the path between a and b, False otherwise.
     */
    private boolean isPathValid(Position a, Position b) {
        int aX = a.getX(), aY = a.getY(), bX = b.getX(), bY = b.getY();
        if (aX == bX) {
            int yRunner = aY;
            int direction = (bY - aY) / Math.abs(bY - aY);  // -1 or 1
            while (yRunner != bY - direction) {
                if (board[aX][yRunner + direction] != null)
                    return false;
                yRunner += direction;
            }
            return true;
        } else if (aY == bY) {
            int xRunner = aX;
            int direction = (bX - aX) / Math.abs(bX - aX);  // -1 or 1
            while (xRunner != bX - direction) {
                if (board[xRunner + direction][aY] != null)
                    return false;
                xRunner += direction;
            }
            return true;
        }
        return false;
    }

    /**
     * Checks whether the last movement (of a piece) caused other enemy pieces to be killed, and updates the board accordingly.
     * Also updates the relevant game-tracking Stacks.
     * Also decides if the game is finished.
     *
     * @param killerPosition The position to which a piece has been moved in the last turn.
     */
    private void checkKillerSurroundings(Position killerPosition) {
        Piece killer = getPieceAtPosition(killerPosition);
        int i = 0, killedPiecesCount = 0;
        if (isKing(killer)) {
            if (killerPosition.isCorner())
                endGame(killer);    // end game if king reached corner
        } else {
            Position[] killersNeighbors = getNeighborPositions(killerPosition); // array with size 4
            while (i < 4) {
                Position toKillFrom = killersNeighbors[i];
                Piece toKill = getPieceAtPosition(toKillFrom);
                if (toKill != null) {
                    if (toKill.getOwner() != killer.getOwner()) {
                        boolean[] surrArr = surroundingEnemiesStatus(toKillFrom);
                        if (isKing(toKill)) {
                            boolean isKingDead = true;
                            for (int j = 0; j < 4; j++) {
                                if (!surrArr[j]) {
                                    isKingDead = false;
                                    break;
                                }
                            }
                            if (isKingDead) {
                                endGame(killer);    // end game if king is dead
                                break;
                            }
                        } else {
                            int xDiff = toKillFrom.getX() - killerPosition.getX();  // between -1 and 1
                            int yDiff = toKillFrom.getY() - killerPosition.getY();  // between -1 and 1
                            Position pincerPosition = new Position(toKillFrom.getX() + xDiff, toKillFrom.getY() + yDiff);
                            if (xDiff != 0) {
                                if (surrArr[1 + xDiff] || pincerPosition.isCorner()) {
                                    killPieceAtPosition(killer, toKillFrom);    // kill if surrounded horizontally (because of last turn)
                                    killedPiecesCount++;
                                }
                            } else if (yDiff != 0) {
                                if (surrArr[2 + yDiff] || pincerPosition.isCorner()) {
                                    killPieceAtPosition(killer, toKillFrom);    // kill if surrounded vertically (because of last turn)
                                    killedPiecesCount++;
                                }
                            }
                        }
                    }
                }
                i++;
            }
        }
        if (i == 4) // only if the game hasn't ended before exiting the loop
            killedPiecesCounts.push(killedPiecesCount);
    }

    /**
     * Returns the neighbor-positions of a given position in this order: left, up, right, down.
     *
     * @param center Given position.
     * @return A 4-cell array with the positions that are adjacent to the given position.
     */
    private Position[] getNeighborPositions(Position center) {
        Position[] neighbors = new Position[4];
        neighbors[0] = new Position(center.getX() - 1, center.getY());
        neighbors[1] = new Position(center.getX(), center.getY() - 1);
        neighbors[2] = new Position(center.getX() + 1, center.getY());
        neighbors[3] = new Position(center.getX(), center.getY() + 1);
        return neighbors;
    }

    /**
     * Checks if there are enemy pawns in the surroundings of a given piece.
     *
     * @param center The position in which the checked piece is.
     * @return A 4-cell boolean array that functions as the following:
     * For each direction - left=0, up=1, right=2, down=3 - the cell with the matching index will be set True if there is an enemy pawn
     * in its direction, or if 'center' is blocked by the edge of the board in this direction. Otherwise, the cell will be set False.
     */
    private boolean[] surroundingEnemiesStatus(Position center) {
        boolean[] ans = new boolean[4];
        Position[] neighbors = getNeighborPositions(center);
        for (int i = 0; i < 4; i++) {
            if (isOutOfBounds(neighbors[i]))
                ans[i] = true;
            else if (neighbors[i] != null) {
                Piece neighbor = getPieceAtPosition(neighbors[i]);
                if (neighbor != null && !isKing(neighbor))
                    if (neighbor.getOwner() != getPieceAtPosition(center).getOwner())
                        ans[i] = true;
            }
        }
        return ans;
    }

    /**
     * Removes a (now) dead piece from the board while increasing the kill count of the killer.
     *
     * @param killer     The piece that killed the other.
     * @param toKillFrom The position in which the given piece had died.
     */
    private void killPieceAtPosition(Piece killer, Position toKillFrom) {
        lastPositionOfDead.push(toKillFrom);
        int x = toKillFrom.getX(), y = toKillFrom.getY();
        deadPawns.push(board[x][y]);
        ((Pawn) killer).addKill();
        board[x][y] = null;
    }

    /**
     * Ends the game. Will be called after one of the players completed their objective for winning.
     * Update the winning player's win count accordingly and calls for game-stats prints.
     *
     * @param p The piece that moved last in the game.
     */
    private void endGame(Piece p) {
//        if (!isKing(p)) TODO maybe restore?
//            ((Pawn) p).addKill();
        isOver = true;
        Player winner = p.getOwner();
        ((ConcretePlayer) winner).addWin();
        printStats(winner);
    }

    /**
     * Prints the statistics of the recently finished game (according to the assignment).
     * Uses the comparator-implementing methods stats1, stats2, stats3, stats4.
     *
     * @param winner The winner of the last game.
     */
    private void printStats(Player winner) {
        /* Putting all the pieces (alive or dead) into 2 arrays - one for each player */
        ConcretePiece[] p1Arr = new ConcretePiece[p1NumOfPieces], p2Arr = new ConcretePiece[p2NumOfPieces];
        int t1 = 0, t2 = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                ConcretePiece temp = (ConcretePiece) board[i][j];
                if (temp != null) {
                    if (temp.getOwner() == P1)
                        p1Arr[t1++] = temp;
                    else
                        p2Arr[t2++] = temp;
                }
            }
        }
        while (!deadPawns.isEmpty()) {
            ConcretePiece temp2 = (ConcretePiece) deadPawns.pop();
            if (temp2.getOwner() == P1)
                p1Arr[t1++] = temp2;
            else
                p2Arr[t2++] = temp2;
        }
        /* Printing game stats */
        ConcretePiece[] allPieces = Arrays.copyOf(p1Arr, p1Arr.length + p2Arr.length);  // merges the arrays
        System.arraycopy(p2Arr, 0, allPieces, p1Arr.length, p2Arr.length);                      // p1Arr and p2Arr
        System.out.print(stats1(p1Arr, p2Arr, winner));
        System.out.print(PARTITION);
        System.out.println(stats2(allPieces, winner));
        System.out.print(PARTITION);
        System.out.println(stats3(allPieces, winner));
        System.out.print(PARTITION);
        System.out.println(stats4(allPieces));
        System.out.println(PARTITION);
    }

    private String stats1(ConcretePiece[] p1Arr, ConcretePiece[] p2Arr, Player winner) {
        class stepsCompareAscending implements Comparator<ConcretePiece> {
            @Override
            public int compare(ConcretePiece pi1, ConcretePiece pi2) {
                int size1 = pi1.getPositionHistory().size(), size2 = pi2.getPositionHistory().size();
                if (size1 == size2) {
                    String idStr1 = pi1.getId().substring(1), idStr2 = pi2.getId().substring(1);
                    int idInt1 = Integer.parseInt(idStr1), idInt2 = Integer.parseInt(idStr2);
                    return Integer.compare(idInt1, idInt2);
                }
                return Integer.compare(size1, size2);
            }
        }
        Arrays.sort(p1Arr, new stepsCompareAscending());
        Arrays.sort(p2Arr, new stepsCompareAscending());
        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        for (int i = 0; i < p2NumOfPieces; i++) {
            if (i < p1NumOfPieces) {
                if (!p1Arr[i].getPositionHistory().isEmpty())
                    s1.append(p1Arr[i].getId()).append(": ").append(p1Arr[i].getPositionHistory()).append('\n');
            }
            if (!p2Arr[i].getPositionHistory().isEmpty())
                s2.append(p2Arr[i].getId()).append(": ").append(p2Arr[i].getPositionHistory()).append("\n");
        }
        if (winner == P1)
            return s1.toString() + s2;
        else
            return s2 + s1.toString();
    }

    private String stats2(ConcretePiece[] pieces, Player winner) {
        class killsCompareDescending implements Comparator<ConcretePiece> {
            @Override
            public int compare(ConcretePiece pi1, ConcretePiece pi2) {
                if (isKing(pi1))
                    return -1;
                else if (isKing(pi2))
                    return 1;
                int pi1Kills = ((Pawn) pi1).getKills(), pi2Kills = ((Pawn) pi2).getKills();
                return stats2And3Help(pi1, pi2, pi1Kills, pi2Kills, winner);
            }
        }
        StringBuilder ans = new StringBuilder();
        Arrays.sort(pieces, new killsCompareDescending());
        for (ConcretePiece p : pieces) {
            if (!isKing(p) && ((Pawn) p).getKills() > 0)
                ans.append('\n').append(p.getId()).append(": ").append(((Pawn) p).getKills()).append(" kills");
        }
        return ans.toString();
    }

    private String stats3(ConcretePiece[] pieces, Player winner) {
        class stepsCompareDescending implements Comparator<ConcretePiece> {
            @Override
            public int compare(ConcretePiece pi1, ConcretePiece pi2) {
                int pi1Dist = pi1.getTotalDistance(), pi2Dist = pi2.getTotalDistance();
                return stats2And3Help(pi1, pi2, pi1Dist, pi2Dist, winner);
            }

        }
        StringBuilder ans = new StringBuilder();
        Arrays.sort(pieces, new stepsCompareDescending());
        for (ConcretePiece p : pieces) {
            if (p.getTotalDistance() > 0)
                ans.append('\n').append(p.getId()).append(": ").append(p.getTotalDistance()).append(" squares");
        }
        return ans.toString();
    }

    private String stats4(ConcretePiece[] pieces) {
        int[][] diffPiecesCounter = new int[BOARD_SIZE][BOARD_SIZE];
        for (ConcretePiece p : pieces) {
            ArrayList<Position> positionHistory = p.getPositionHistory();
            if (positionHistory != null) {
                boolean[][] wasOnPosition = new boolean[BOARD_SIZE][BOARD_SIZE];
                while (!positionHistory.isEmpty()) {
                    Position pos = positionHistory.remove(0);
                    wasOnPosition[pos.getX()][pos.getY()] = true;
                }
                for (int i = 0; i < BOARD_SIZE; i++) {
                    for (int j = 0; j < BOARD_SIZE; j++) {
                        if (wasOnPosition[i][j])
                            diffPiecesCounter[i][j]++;
                    }
                }
            }
        }
        ArrayList<Position> positionsToPrint = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (diffPiecesCounter[i][j] > 1)
                    positionsToPrint.add(new Position(i, j));
            }
        }
        class positionCompareDescending implements Comparator<Position> {
            @Override
            public int compare(Position pos1, Position pos2) {
                int x1 = pos1.getX(), y1 = pos1.getY(), x2 = pos2.getX(), y2 = pos2.getY();
                int steppedOnCount1 = diffPiecesCounter[x1][y1], steppedOnCount2 = diffPiecesCounter[x2][y2];
                if (steppedOnCount1 == steppedOnCount2) {
                    if (x1 == x2)
                        return Integer.compare(y1, y2);
                    return Integer.compare(x1, x2);
                }
                return -Integer.compare(steppedOnCount1, steppedOnCount2);
            }
        }
        StringBuilder ans = new StringBuilder();
        positionsToPrint.sort(new positionCompareDescending());
        while (!positionsToPrint.isEmpty()) {
            Position toPrint = positionsToPrint.remove(0);
            ans.append('\n').append(toPrint).append(diffPiecesCounter[toPrint.getX()][toPrint.getY()]).append(" pieces");
        }
        return ans.toString();
    }

    /**
     * Since the comparators in stats2 and stats3 may do the same thing at some point, this method prevents duplicate sections of the code
     * in each comparator.
     *
     * @param pi1     1st piece to compare.
     * @param pi2     2nd piece to compare.
     * @param pi1Dist totalDistance of pi1.
     * @param pi2Dist totalDistance of pi2.
     * @param winner  The winner of the last game.
     * @return 1 if pi1>pi2, 0 if pi1=pi2, -1 if pi1<pi2 (according to the assignment).
     */
    private int stats2And3Help(ConcretePiece pi1, ConcretePiece pi2, int pi1Dist, int pi2Dist, Player winner) {
        if (pi1Dist == pi2Dist) {
            String idStr1 = pi1.getId().substring(1), idStr2 = pi2.getId().substring(1);
            int idInt1 = Integer.parseInt(idStr1), idInt2 = Integer.parseInt(idStr2);
            if (idInt1 == idInt2) {
                if (pi1.getOwner() == winner)
                    return -1;
                else
                    return 1;
            }
            return Integer.compare(idInt1, idInt2);
        }
        return -Integer.compare(pi1Dist, pi2Dist);
    }

    @Override
    public Player getFirstPlayer() {
        return P1;
    }

    @Override
    public Player getSecondPlayer() {
        return P2;
    }

    @Override
    public boolean isGameFinished() {
        return isOver;
    }

    @Override
    public void reset() {
        resetBoard();
        isP2Turn = true;
        isOver = false;
        pastMovements = new Stack<>();
        killedPiecesCounts = new Stack<>();
        deadPawns = new Stack<>();
        lastPositionOfDead = new Stack<>();
    }

    /**
     * (Re)Creates the playing board efficiently, which is a 2D array of Pieces where null Pieces represent empty squares on the board.
     */
    private void resetBoard() {
        board = new Piece[BOARD_SIZE][BOARD_SIZE];

        /* Setting up P1's Pieces */
        for (int rows = 0, id = 1; rows <= BOARD_SIZE / 4; rows++) {
            for (int cols = rows * -1; cols <= rows; cols++) {
                board[cols + BOARD_SIZE / 2][rows + BOARD_SIZE / 4 + 1] = new Pawn(P1, P1_Pawn_Unicode, "D" + id);
                if (rows < BOARD_SIZE / 4)
                    board[BOARD_SIZE / 2 - cols][BOARD_SIZE * 3 / 4 - rows - 1] =
                            new Pawn(P1, P1_Pawn_Unicode, "D" + (p1NumOfPieces - id + 1));
                id++;
            }
        }
        board[BOARD_SIZE / 2][BOARD_SIZE / 2] = new King();

        /* Setting up P2's Pieces */
        for (int i = 1, j = 0; i < p2NumOfPieces / 4; i++) {
            board[BOARD_SIZE / 4 + i][0] = new Pawn(P2, P2_Pawn_Unicode, "A" + i);
            board[BOARD_SIZE * 3 / 4 - i][BOARD_SIZE - 1] = new Pawn(P2, P2_Pawn_Unicode, "A" + (p2NumOfPieces - i + 1));
            if (i > p2NumOfPieces / 8)
                j = 2;
            board[0][BOARD_SIZE / 4 + i] = new Pawn(P2, P2_Pawn_Unicode, "A" + (p2NumOfPieces / 4 + 2 * i - 1 + j));
            board[BOARD_SIZE - 1][BOARD_SIZE * 3 / 4 - i] = new Pawn(P2, P2_Pawn_Unicode, "A" + (p2NumOfPieces * 3 / 4 - 2 * i - j + 2));
        }
        board[BOARD_SIZE / 2][1] = new Pawn(P2, P2_Pawn_Unicode, "A" + (p2NumOfPieces / 4));
        board[1][BOARD_SIZE / 2] = new Pawn(P2, P2_Pawn_Unicode, "A" + (p2NumOfPieces / 2));
        board[BOARD_SIZE - 2][BOARD_SIZE / 2] = new Pawn(P2, P2_Pawn_Unicode, "A" + (p2NumOfPieces / 2 + 1));
        board[BOARD_SIZE / 2][BOARD_SIZE - 2] = new Pawn(P2, P2_Pawn_Unicode, "A" + (p2NumOfPieces * 3 / 4 + 1));
    }

    @Override
    public void undoLastMove() {
        if (!killedPiecesCounts.isEmpty()) {
            restoreLastKilledPawns(killedPiecesCounts.pop());
            moveBackLastPiece();
            isP2Turn = !isP2Turn;
        }
    }

    /**
     * Restores a given number of dead pawns to their final position. Also decreases the kill count of their killer accordingly.
     *
     * @param toRestore Number of dead pawns to restore.
     */
    private void restoreLastKilledPawns(int toRestore) {
        Piece killer = getPieceAtPosition(pastMovements.peek());
        if (!isKing(killer))
            ((Pawn) killer).decreaseKillsBy(toRestore);
        while (toRestore > 0) {
            Position restoreIn = lastPositionOfDead.pop();
            board[restoreIn.getX()][restoreIn.getY()] = deadPawns.pop();
            toRestore--;
        }
    }

    /**
     * Cancels the last movement of the last piece moved while updating its movement-tracking variables.
     */
    private void moveBackLastPiece() {
        Position moveFrom = pastMovements.pop(), moveTo = pastMovements.pop();
        int x1 = moveFrom.getX(), y1 = moveFrom.getY(), x2 = moveTo.getX(), y2 = moveTo.getY();
        ((ConcretePiece) board[x1][y1]).undoLastMovement();  // updates fields of the last piece moved
        board[x2][y2] = board[x1][y1];
        board[x1][y1] = null;
    }

    @Override
    public int getBoardSize() {
        return BOARD_SIZE;
    }
}
