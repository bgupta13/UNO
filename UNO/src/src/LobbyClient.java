/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package src;
import java.io.*;
import java.net.*;

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
            } catch (Exception ignored) {}
        }).start();

        String input;

        while ((input = user.readLine()) != null) {
            out.println(input);
        }
        socket.close();
    }
}