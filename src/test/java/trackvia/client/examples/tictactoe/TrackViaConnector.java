package trackvia.client.examples.tictactoe;

import java.util.ArrayList;
import java.util.List;

import trackvia.client.TrackviaClient;
import trackvia.client.model.Record;
import trackvia.client.model.RecordData;
import trackvia.client.model.RecordDataBatch;
import trackvia.client.model.RecordSet;
import trackvia.client.model.View;

/**
 * Class that handles interacting with TrackVia
 * @author etherton
 *
 */
public class TrackViaConnector {

	TrackviaClient trackVia;
	
	public TrackViaConnector (TrackviaClient trackVia) {
		this.trackVia = trackVia;
	}
	
	public void readInGameState(GameState gameState, int viewId) {
		Record record = trackVia.getRecord(viewId, gameState.recordId);
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				String pieceStr = record.getData().get(i+","+j) == null ? null : record.getData().get(i+","+j).toString();
				Piece piece = Piece.getPiece(pieceStr);
				gameState.board[i][j] = piece;
			}
		}
		String pieceStr = record.getData().get("Turn") == null ? null : record.getData().get("Turn").toString();
		Piece turn = Piece.getPiece(pieceStr);
		gameState.setTurn(turn);
	}
	
	public void writeGameState(GameState gameState, int viewId) {
		RecordData data = new RecordData();
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				Piece piece = gameState.board[i][j];
				data.put(i+","+j, piece.toString());
			}
		}
		data.put("Turn", gameState.getTurn().toString());
		trackVia.updateRecord(viewId, gameState.recordId, data);
	}
	
	public List<String> getListOfViews(){
		List<View> views = trackVia.getViews();
		List<String> retVal = new ArrayList<>(views.size()); 
		for(View view : views) {
			retVal.add(view.getId() + " - " + view.getName());
		}
		return retVal;
	}
	
	public List<String> getListOfExistingGames(int viewId){
		RecordSet recordSet = trackVia.getRecords(viewId);
		List<String> retVal = new ArrayList<>(recordSet.getData().size()); 
		for(RecordData gameData : recordSet.getData()) {
			retVal.add(gameData.getId() + " - " + gameData.get("Name"));
		}
		return retVal;
	}
	
	public long createGame(int viewId, String name) {
		//set the name in the record
		RecordData data = new RecordData();
		data.put("Name", name);
		data.put("Turn", Piece.X.toString()); //X always goes first
		
		//add the record to a record list
		List<RecordData> recordList = new ArrayList<>(1);
		recordList.add(data);
		RecordDataBatch rdb = new RecordDataBatch(recordList);
		
		RecordSet records = trackVia.createRecords(viewId, rdb);
		
		return records.getData().get(0).getId();
	}
	
}
