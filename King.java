public class King extends ConcretePiece {

    public King() {
        super(GameLogic.P1, GameLogic.P1_King_Unicode, "K"+(((GameLogic.BOARD_SIZE / 2 + 1) * 4)/ 4 + 1));
    }
}