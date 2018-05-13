package main;

import MCTS.Strategy1.Agent1Factory;
import MCTS.Strategy2.Agent2Factory;
import MCTS.Strategy3.Agent3Factory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.martiansoftware.jsap.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Iterator;

public class Handler extends WebSocketServer {

    public Agent agent;
    public JsonParser parser = new JsonParser();
    private static int strategy_number = -1;

    public static void main(String[] args) {
        Handler.JSAPMain(args);
    }

    public static void JSAPMain(String args[]) {

        JSAP jsap = new JSAP();

        try {
            Switch h = new Switch("help")
                    .setShortFlag('h')
                    .setLongFlag("help");
            h.setHelp("Show usage information for this application.");
            jsap.registerParameter(h);

            FlaggedOption s = new FlaggedOption("strategy")
                    .setStringParser(JSAP.INTEGER_PARSER)
                    .setDefault("6")
                    .setShortFlag('s')
                    .setLongFlag("strategy")
                    .setAllowMultipleDeclarations(false);
            s.setHelp("Strategy that will be used by the agent.\n" +
                    "1: Monte Carlo tree search\n" +
                    "2: MCTS with early simulation termination\n" +
                    "3: the latter extended with search tree reuse\n" +
                    "4: the latter extended with optimal moves\n" +
                    "5: the latter extended with increased search time\n" +
                    "6: strategy 4 extended with a neural network heuristic\n"
            );
            jsap.registerParameter(s);

            FlaggedOption p = new FlaggedOption("port")
                    .setStringParser(JSAP.INTEGER_PARSER)
                    .setDefault("10000")
                    .setShortFlag('p')
                    .setLongFlag("port")
                    .setAllowMultipleDeclarations(false);
            p.setHelp("Port on which the agent will run.");
            jsap.registerParameter(p);
        } catch (JSAPException e) {
            e.printStackTrace();
        }

        JSAPResult config = jsap.parse(args);

        if (!config.success() || config.getBoolean("help")) {
            System.err.println();
            for (Iterator errs = config.getErrorMessageIterator(); errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }

            System.err.println();
            System.err.println("Usage: java -jar agent.jar");
            System.err.println("        " + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);
        }

        int config_strategy_number = config.getInt("strategy");
        if (config_strategy_number >= 0 && config_strategy_number <= 6) {
            strategy_number = config_strategy_number;
        } else {
            strategy_number = 6;
        }

        int port = config.getInt("port");
        WebSocketServer server = new Handler(new InetSocketAddress("localhost", port));
        System.out.println("Starting server on ws://127.0.0.1:" + Integer.toString(port));
        server.run();
    }

    public static void simpleMain() {
        // Please keep this one here, I can't get JSAP to work and just want to test agents sometimes :P
        int port = 10021;
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

            AgentFactory factory;
            switch (strategy_number) {
                case 1:
                    factory = new Agent1Factory();
                    break;
                case 2:
                    factory = new Agent2Factory();
                    break;
                case 3:
                    factory = new Agent3Factory();
                    break;
                case 4:
                    factory = new MCTS2.MCTSAgentFactory();
                    break;
                case 5:
                    factory = new MCTS2.AsyncSearchAgentFactory();
                    break;
                case 6:
                    factory = new MCTS3.MCTSAgentFactory();
                    break;
                case 0:
                    factory = (int player1, double timeLimit1, int rows1, int columns1, String gameId1) -> new TestAgent(player1, timeLimit1, rows1, columns1, gameId1);
                    break;
                default:
                    factory = new MCTS3.MCTSAgentFactory();
            }
            this.agent = factory.create(player, timeLimit, rows, columns, gameId);

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
            int opponentScore = scores.get((this.agent.player + 1) % 2).getAsInt();
            JsonArray location = jsonMessage.get("location").getAsJsonArray();
            String orientation = jsonMessage.get("orientation").getAsString();
            int x = 2 * location.get(1).getAsInt() + (orientation.equals("h") ? 1 : 0);
            int y = 2 * location.get(0).getAsInt() + (orientation.equals("v") ? 1 : 0);

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
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started successfully.");
    }

    private void replyMove(WebSocket conn) {
        // Queries the agent for its next move and writes it using the given connection
        try {
            int[] move = this.agent.getNextMove();
            int x = move[0];
            int y = move[1];
            String message = "{\"type\": \"action\", \"location\": [" + Integer.toString(y / 2) + ", " + Integer.toString(x / 2) + "], \"orientation\": " + (x % 2 == 0 ? "\"v\"" : "\"h\"") + "}";
            conn.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
