import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Handler {

    public static void main(String args[]) {

        try {

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out));

            // Socket opened, stay in while-true loop while listening for messages
            Agent agent = null;
            while(true) {

                ArrayList<String> lines = new ArrayList<>();
                // End of message is detected by a single { on a new line
                while (lines.size() == 0 || !lines.get(lines.size() - 1).equals("}")) {
                    lines.add(reader.readLine());
                }

                // JSON parsing
                // Normally you use a library for this, but to save on installation mess and because of the simple
                // protocol, I'm just implementing a simple parser myself
                HashMap<String, String> message = new HashMap<>();
                for(String line : lines) {
                    String[] parts = line.split(":");
                    parts[0] = parts[0].substring(1, parts[1].length() - 1); // Remove quotation marks from parameter
                    parts[1] = parts[1].substring(1);
                    // Remove commas from ends of lines
                    if (parts[1].endsWith(","))
                        parts[1] = parts[1].substring(0, parts[1].length() - 1);
                    // Remove quotation marks from string fields
                    if (parts[1].startsWith("\""))
                        parts[1] = parts[1].substring(1);
                    if (parts[1].endsWith("\""))
                        parts[1] = parts[1].substring(0, parts[1].length() - 1);
                    message.put(parts[0], parts[1]);
                }

                String type = message.get("type");
                if (type.equals("start")) {

                    // Handle start message
                    System.err.println("Received start message!");

                    // Read variables
                    int player = Integer.parseInt(message.get("player"));
                    double timeLimit = Double.parseDouble(message.get("timeLimit"));
                    String[] grid = message.get("grid").split(",");
                    int rows = Integer.parseInt(grid[0].substring(1));
                    int columns = Integer.parseInt(grid[1].substring(0, grid[1].length() - 1));
                    String gameId = message.get("game");

                    agent = new TestAgent(player, timeLimit, rows, columns, gameId);

                    // If we are player 1, respond right away
                    if (agent.player == 1)
                        Handler.replyMove(writer, agent);

                } else if (type.equals("action")) {

                    // Handle action message
                    System.err.println("Received action message!");

                    // Read variables
                    int nextPlayer = Integer.parseInt(message.get("nextPlayer"));
                    String[] score = message.get("score").split(",");
                    int score1 = Integer.parseInt(score[0].substring(1));
                    int score2 = Integer.parseInt(score[1].substring(0, score[1].length() - 1));
                    int ownScore = (agent.player == 1 ? score1 : score2);
                    int opponentScore = (agent.player == 1 ? score2 : score1);
                    String[] location = message.get("location").split(",");
                    int x = 2*Integer.parseInt(location[0].substring(1)) + (message.get("orientation") == "h" ? 1 : 0);
                    int y = 2*Integer.parseInt(location[1].substring(0, location[1].length() - 1)) + (message.get("orientation") == "v" ? 1 : 0);

                    agent.registerAction(ownScore, opponentScore, x, y);

                    // If we are nextPlayer, respond
                    if (agent.player == nextPlayer)
                        Handler.replyMove(writer, agent);

                } else if (type.equals("end")) {
                    // Handle end message
                    System.err.println("Received end message! Aborting program");
                    break;
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void replyMove(PrintWriter writer, Agent agent) {
        // Queries the agent for its next move and writes it using the given writer
        int[] move = agent.getNextMove();
        int x = move[0];
        int y = move[1];
        writer.write("{\n" +
                "    \"type\": \"action\",\n" +
                "    \"location\": [" + Integer.toString(x/2) + ", " + Integer.toString(y/2) + "],\n" +
                "    \"orientation\": " + (x%2 == 0 ? "v" : "h") + "\n"
        );

    }

}
