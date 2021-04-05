package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class ConsoleServer {
    private Vector<ClientHandler> users;

    public ConsoleServer() {
        users = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;

        try {
            AuthService.connect();
            server = new ServerSocket(6000);
            System.out.println("Server started");


            while (true) {
                socket = server.accept();
                System.out.printf("Client [%s] connected\n", socket.getInetAddress());
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public void subscribe(ClientHandler client) {
        users.add(client);
    }

    public void unsubscribe(ClientHandler client) {
        users.remove(client);
    }

    public void broadcastMessage(String str) {
        for (ClientHandler c : users) {
            c.sendMsg(str);
        }
    }

    public void sendPrivateMsg(ClientHandler nickFrom, String nickTo, String msg) {
        for (ClientHandler c : users) {
            if(c.getNickname().equals(nickTo)) {
                if (!nickFrom.getNickname().equals(nickTo)) {
                    c.sendMsg(nickFrom.getNickname() + ": " + "[Private message]" + msg);
                    nickFrom.sendMsg(nickFrom.getNickname() + ": " + "[Private message]" + msg);
                }
            }
        }
    }

    public synchronized boolean isNickBusy(String nick) {
        for(ClientHandler c : users) {
            if (c.getNickname().equals(nick)) {
                return true;
            }
        }
        return false;
    }

}
