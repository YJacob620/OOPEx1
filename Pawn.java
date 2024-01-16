public class Pawn extends ConcretePiece {
    private int kills;

    public Pawn(Player owner, String type, String id) {
        super(owner, type, id);
        kills = 0;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        kills++;
        if (GameLogic.CONSOLE_PRINT)
            System.out.println(super.getId() + " total kills: " + kills);
    }

    /**
     * Subtracts from 'kills' (may be used when undoLastMove is called).
     * @param n The amount to decrease from 'kills'.
     */
    public void decreaseKillsBy(int n) {
        kills -= n;
    }
}
