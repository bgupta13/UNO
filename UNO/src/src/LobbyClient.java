package src;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LobbyClient {

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 5000);

        BufferedReader server =
                new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

        PrintWriter out =
                new PrintWriter(socket.getOutputStream(), true);

        BufferedReader user =
                new BufferedReader(
                        new InputStreamReader(System.in));

        new Thread(() -> {
            try {
                String msg;
                while ((msg = server.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (Exception ignored) {
            }
        }).start();

        String input;

        while ((input = user.readLine()) != null) {
            out.println(input);

            if (input.equalsIgnoreCase("QUIT")) {
                break;
            }
        }

        socket.close();
    }
}
