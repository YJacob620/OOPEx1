
public class ConcretePlayer implements Player {
    private final boolean is1st;
    private int wins = 0;

    public ConcretePlayer(boolean isPlayerOne) {
        is1st = isPlayerOne;
    }

    @Override
    public boolean isPlayerOne() {
        return is1st;
    }

    @Override
    public int getWins() {
        return wins;
    }

    public void addWin() {
        wins++;
    }
}
