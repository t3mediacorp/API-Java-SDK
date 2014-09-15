package trackvia.client.examples.tictactoe;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import trackvia.client.TrackviaClient;

public class GameController {
	
	public static final String CONFIG_PATH = "src/test/resources/trackvia.config";
	
	TrackViaConnector connector;

	int viewId;
	
	Piece piece = null;
	
	GameState gameState;

	public static void main(String[] args) throws IOException {
		
		GameController controller = new GameController();
		controller.setupClient(CONFIG_PATH);
		controller.selectView();
		controller.chosePiece();
		controller.chooseGame();
		controller.playGame();
		System.exit(0);
	}
	
	public GameController() {
		gameState = new GameState();
	}
	
	public void playGame() {
		connector.readInGameState(gameState, viewId);
		Piece winner = gameState.isWinner();
		while(winner == Piece.EMPTY) {
			while(gameState.getTurn() != piece) {
				System.out.println("It's not your turn");
				readLine("Press enter after your competitor has made a move");
				connector.readInGameState(gameState, viewId);
			}
			gameState.renderGame();
			makeMove();
			gameState.renderGame();
			
			winner = gameState.isWinner();
		}
		
		if(winner == null) {
			System.out.println("The Cat won");
		} else {
			System.out.println("The winner is: " + winner);
		}
		System.out.println("Thanks for playing TrackVia Tic-Tac-Toe");
	}
	
	protected void makeMove() {
		int row = Integer.parseInt(readLine("Which row do you want to place a piece in: "));
		int column = Integer.parseInt(readLine("Which column do you want to place a piece in: "));
		while(!gameState.isEmpty(row, column)) {
			System.out.println("Sorry that space is taken. Try again");
			gameState.renderGame();
			row = Integer.parseInt(readLine("Which row do you want to place a piece in: "));
			column = Integer.parseInt(readLine("Which column do you want to place a piece in: "));
		}
		gameState.setPiece(piece, row, column);
		if(piece == Piece.O) {
			gameState.setTurn(Piece.X);
		} else {
			gameState.setTurn(Piece.O);
		}
		connector.writeGameState(gameState, viewId);
	}
	
	public void chooseGame() {
		System.out.println("\n");
		String choice = readLine("Type 1 if you want to play a new game or 2 if you want to continue an existing game: ");
		if(choice.equals("1")) {
			createNewGame();
		} else {
			resumeExistingGame();
		}
	}
	
	protected void createNewGame() {
		System.out.println("\n");
		String name = readLine("What do you want to name this game: " );
		
		gameState.setRecordId(connector.createGame(viewId, name));
	}
	
	protected void resumeExistingGame() {
		List<String> existingGames = connector.getListOfExistingGames(viewId);
		System.out.println("Existing games:");
		for(String games : existingGames) {
			System.out.println(games);
		}
		System.out.println("");
		int gameId = Integer.parseInt(readLine("Type in the ID of the game you want to resume: "));
		System.out.println("Using game: " + gameId);
		gameState.setRecordId(gameId);
	}
	
	public void chosePiece() {
		System.out.println("\n");
		while(piece == null) {
			String str = readLine("Are you 'X' or 'O': ");
			if(str.toLowerCase().equals("o")) {
				piece = Piece.O;
			} else if(str.toLowerCase().equals("x")) {
				piece = Piece.X;
			} else {
				System.out.print("Unknown choice \"" + str + "\"");
			}
		}
		System.out.println(piece + " chosen.");
	}
	
	public void selectView() {
		List<String> views = connector.getListOfViews();
		System.out.println("Views in your account that you have access to");
		for(String view : views) {
			System.out.println(view);
		}
		System.out.println("");
		viewId = Integer.parseInt(readLine("Type in the ID of the view you want to play Tic-Tac-Toe with: "));
		System.out.println("Using view: " + viewId);
	}
	
	public void setupClient(String configPath) throws IOException {
		String email;
		String userKey;
		String password;
		String scheme;
		String hostName;
		int port;
		String path;
		
		Properties config = new Properties();
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(configPath);
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Config file ("+ configPath + ") not found. " + 
					"Did you remember to make a copy from the template file?");
		}
		config.load(inputStream);
		email = config.getProperty("email");
		userKey = config.getProperty("user_key");
		password = config.getProperty("password");
		scheme = config.getProperty("scheme");
		hostName = config.getProperty("hostname");
		port = Integer.parseInt(config.getProperty("port"));
		path = config.getProperty("path");
		
		TrackviaClient client = TrackviaClient.create(path, scheme, hostName, port, email, password, userKey);
		connector = new TrackViaConnector(client);
	}
	
	protected String readLine(String str) {

		if(str != null) {
			System.out.print(str);
		}
		try {
		    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		    String s = bufferRead.readLine();


		    return s;
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;

}
}
