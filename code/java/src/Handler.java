import java.net.InetSocketAddress;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.gson.JsonParser;

public class Handler extends WebSocketServer {

    public Agent agent;
    public JsonParser parser = new JsonParser();

    public static void main(String args[]) {

        int port = 10017;
        WebSocketServer server = new Handler(new InetSocketAddress("localhost", port));
        System.out.println("Starting server on ws://127.0.0.1:" + Integer.toString(port));
        server.run();

    }

    public Handler(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Opened new connection.");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection.");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        JsonObject jsonMessage = this.parser.parse(message).getAsJsonObject(); // Should always be a dict/map
        String type = jsonMessage.get("type").getAsString();
        if (type.equals("start")) {

            // Handle start message

            // Read variables
            int player = jsonMessage.get("player").getAsInt() - 1;
            double timeLimit = jsonMessage.get("timelimit").getAsDouble();
            JsonArray grid = jsonMessage.get("grid").getAsJsonArray();
            int rows = grid.get(0).getAsInt();
            int columns = grid.get(1).getAsInt();
            String gameId = jsonMessage.get("game").getAsString();

            //this.agent = new TestAgent(player, timeLimit, rows, columns, gameId);
            this.agent = new MCTSAgent2(player, timeLimit, rows, columns, gameId);

            // If we are player 1, respond right away
            if (this.agent.player == 0)
                replyMove(conn);

        } else if (type.equals("action")) {

            // Handle action message

            // Read variables
            int nextPlayer = jsonMessage.get("nextplayer").getAsInt() - 1;
            JsonArray scores = jsonMessage.get("score").getAsJsonArray();
            int score1 = scores.get(0).getAsInt();
            int score2 = scores.get(1).getAsInt();
            int ownScore = scores.get(this.agent.player).getAsInt();
            int opponentScore = scores.get((this.agent.player + 1)%2).getAsInt();
            JsonArray location = jsonMessage.get("location").getAsJsonArray();
            String orientation = jsonMessage.get("orientation").getAsString();
            int x = 2*location.get(1).getAsInt() + (orientation.equals("h") ? 1 : 0);
            int y = 2*location.get(0).getAsInt() + (orientation.equals("v") ? 1 : 0);

            this.agent.registerAction(ownScore, opponentScore, x, y);

            // If we are nextPlayer, respond
            if (this.agent.player == nextPlayer)
                replyMove(conn);

        } else if (type.equals("end")) {
            // Handle end message
            System.out.println("Received end message!");
        }

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("An error occurred on the connection.");
        System.out.println(ex.toString());
    }

    @Override
    public void onStart() {
        System.out.println("Server started successfully.");
    }

    private void replyMove(WebSocket conn) {
        // Queries the agent for its next move and writes it using the given connection
        int[] move = this.agent.getNextMove();
        int x = move[0];
        int y = move[1];
        String message = "{\"type\": \"action\", \"location\": [" + Integer.toString(y/2) + ", " + Integer.toString(x/2) + "], \"orientation\": " + (x%2 == 0 ? "\"v\"" : "\"h\"") + "}";
        conn.send(message);

    }

}
