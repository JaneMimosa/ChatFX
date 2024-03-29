package Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;


public class ConsoleServer {
    private Vector<ClientHandler> users;

    private static final Logger LOG = LogManager.getLogger(ConsoleServer.class.getName());

    public ConsoleServer() {
        users = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;

        try {
            AuthService.connect();
            server = new ServerSocket(6000);
            //System.out.println("Server started");
            LOG.info("Server started");


            while (true) {
                socket = server.accept();
                //System.out.printf("Client [%s] try to connect\n", socket.getInetAddress());
                LOG.info("Client {} try to connect", socket.getInetAddress());
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            LOG.fatal("Server crashed");
            e.printStackTrace();
        } finally {
            try {
                //System.out.printf("Client [%s] disconnected\n", socket.getInetAddress());
                LOG.info("Client {} disconnected", socket.getInetAddress());
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public void subscribe(ClientHandler client) {
        users.add(client);
        //System.out.println(String.format("Client [%s] connected\n", client.getNickname()));
        LOG.info("Client {} connected", client.getLogin());
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler client) {
        users.remove(client);
        //System.out.println(String.format("Client [%s] disconnected\n", client.getNickname()));
        LOG.info("Client {} disconnected", client.getLogin());
        broadcastClientsList();
    }

    public void broadcastMessage(ClientHandler from, String str) {
        LOG.info("Client {} sent message", from.getLogin());
        for (ClientHandler c : users) {
            if (!AuthService.checkBlackList(c.getNickname(), from.getNickname())) {
                if (!AuthService.checkBlackList(from.getNickname(), c.getNickname())) {
                    c.sendMsg(str);
                    c.saveHistory(str);
                }
            }
        }
    }

    public void sendPrivateMsg(ClientHandler nickFrom, String nickTo, String msg) {
        LOG.info("Client {} tried sending a private message ", nickFrom.getNickname());
        for (ClientHandler c : users) {
            if (c.getNickname().equals(nickTo)) {
                if (!AuthService.checkBlackList(c.getNickname(), nickFrom.getNickname())) {
                    if (!AuthService.checkBlackList(nickFrom.getNickname(), c.getNickname())) {
                        if (!nickFrom.getNickname().equals(nickTo)) {

                            LOG.info("Client {} sent a private message ", nickFrom.getNickname());

                            c.sendMsg(nickFrom.getNickname() + ": " + "[Private message]" + msg);
                            nickFrom.sendMsg(nickFrom.getNickname() + ": " + "[Private message]" + msg);

                            c.saveHistory(nickFrom.getNickname() + ": " + "[Private message]" + msg);
                            nickFrom.saveHistory(nickFrom.getNickname() + ": " + "[Private message]" + msg);

                            return;
                        } else {
                            nickFrom.sendMsg("You can't send private message to yourself");
                            return;
                        }
                    } else {
                        nickFrom.sendMsg("User " + nickTo + " is blocked. Unblock to send messages");
                    }
                } else {
                    nickFrom.sendMsg("You can't send messages to user " + nickTo);
                    return;
                }
            }
        }
        nickFrom.sendMsg("User " + nickTo + " isn't in a chat room");
    }

    public synchronized boolean isNickBusy(String nick) {
        for (ClientHandler c : users) {
            if (c.getNickname().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientsList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientList ");
        for (ClientHandler c : users) {
            sb.append(c.getNickname() + " ");
        }

        String out = sb.toString();

        for (ClientHandler c : users) {
            c.sendMsg(out);
        }
    }
}
