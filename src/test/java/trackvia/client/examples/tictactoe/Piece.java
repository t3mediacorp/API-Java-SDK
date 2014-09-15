package trackvia.client.examples.tictactoe;

public enum Piece {
	X("X", "X"),
	O("O", "O"),
	EMPTY(null, " ");
	
	protected String trackViaValue;
	
	protected String displayValue;
	
	Piece(String trackViaValue, String displayValue){
		this.trackViaValue = trackViaValue;
		this.displayValue = displayValue;
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
		return trackViaValue;
	}
	
	public String getDisplayValue() {
		return displayValue;
	}
}
