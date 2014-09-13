package trackvia.client.examples.tictactoe;

public enum Piece {
	X("X"),
	O("O"),
	EMPTY(" ");
	
	protected String representation;
	
	Piece(String representation){
		this.representation = representation;
	}
	
	public static Piece getPiece(String value) {
		if(value == null) {
			return EMPTY;
		}else if(value.toUpperCase().equals(X.toString())) {
			return X;
		} else if(value.toUpperCase().equals(O.toString())) {
			return O;
		} else {
			return EMPTY;
		}
	}
	
	@Override
	public String toString() {
		return representation;
	}
}
