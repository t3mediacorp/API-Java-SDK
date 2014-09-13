package trackvia.client.examples.tictactoe;

public class GameState {
	
	protected Piece[][] board = new Piece[3][3];
	
	protected long recordId;
	
	public GameState() {
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				board[i][j] = Piece.NULL; 
			}
		}
	}

	public long getRecordId() {
		return recordId;
	}

	public void setRecordId(long recordId) {
		this.recordId = recordId;
	}
	
	public boolean isEmpty(int row, int column) {
		return Piece.NULL == board[row][column];
	}
	
	public void setPiece(Piece piece, int row, int column) {
		board[row][column] = piece;
	}
	
	public void renderGame() {
		System.out.print("\n");
		System.out.println("  -0-1-2-");
		System.out.println("  -------");
		System.out.println("0 |" + board[0][0] + "|" + board[0][1] + "|" + board[0][2] + "|");
		System.out.println("  -------");
		System.out.println("1 |" + board[1][0] + "|" + board[1][1] + "|" + board[1][2] + "|");
		System.out.println("  -------");
		System.out.println("2 |" + board[2][0] + "|" + board[2][1] + "|" + board[2][2] + "|");
		System.out.println("  -------");
	}
	
	public Piece isWinner() {
		//check rows
		for(int i = 0; i < 3; i++) {
			Piece p = Piece.NULL;
			for(int j = 0; j < 3; j++) {
				if(board[i][j] == Piece.NULL) {
					p = Piece.NULL;
					break; //can't be empty
				} else if(p == Piece.NULL) {
					p = board[i][j];
				} else if(p != board[i][j]) {
					p = Piece.NULL;
					break;
				}
			}
			if(p != Piece.NULL) {
				return p;
			}
		}
		//check columns
		for(int i = 0; i < 3; i++) {
			Piece p = Piece.NULL;
			for(int j = 0; j < 3; j++) {
				if(board[j][i] == Piece.NULL) {
					p = Piece.NULL;
					break; //can't be empty
				} else if(p == Piece.NULL) {
					p = board[j][i];
				} else if(p != board[j][i]) {
					p = Piece.NULL;
					break;
				}
			}
			if(p != Piece.NULL) {
				return p;
			}
		}
		//check the diagonals
		Piece p = Piece.NULL;
		for(int j = 0; j < 3; j++) {
			if(board[j][j] == Piece.NULL) {
				p = Piece.NULL;
				break; //can't be empty
			} else if(p == Piece.NULL) {
				p = board[j][j];
			} else if(p != board[j][j]) {
				p = Piece.NULL;
				break;
			}
		}
		if(p != Piece.NULL) {
			return p;
		}
		
		p = Piece.NULL;
		for(int j = 0; j < 3; j++) {
			int i = 2 - j;
			if(board[j][i] == Piece.NULL) {
				p = Piece.NULL;
				break; //can't be empty
			} else if(p == Piece.NULL) {
				p = board[j][i];
			} else if(p != board[j][i]) {
				p = Piece.NULL;
				break;
			}
		}
		if(p != Piece.NULL) {
			return p;
		}
		return Piece.NULL;
	}
	
	
}
