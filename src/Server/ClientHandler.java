package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private ConsoleServer server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private boolean isNickBusy;

    public String getNickname() {
        return nickname;
    }


    public ClientHandler(ConsoleServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
            try {
                while(true) {
                    String str = in.readUTF();
                    if (str.startsWith("/auth")) {
                        String[] tokens = str.split(" ");
                        String nick = AuthService.getNicknameByLoginAndPassword(tokens[1], tokens[2]);
                        if(nick != null) {
                            if (!server.isNickBusy(nick)) {
                                sendMsg("/auth-OK");
                                setNickname(nick);
                                server.subscribe(ClientHandler.this);
                                break;
                            } else {
                                sendMsg("User already logged");
                            }
                        } else {
                            sendMsg("Wrong login or password");
                        }
                    }
                }

                while(true) {
                    String str = in.readUTF();
                    if("/end".equals(str)) {
                        out.writeUTF("/serverClosed");
                        server.broadcastMessage(nickname + " disconnected" );
                        System.out.printf("Client [%s] - disconnected\n", socket.getInetAddress());
                        break;
                    }
                    if(str.startsWith("@")) {
                        String[] tokens = str.split(" ", 2);
                        server.sendPrivateMsg(this,tokens[0].substring(1, tokens[0].length()), tokens[1]);
                    } else {
                        System.out.printf("Client [%s]: %s\n", socket.getInetAddress(), str);
                        server.broadcastMessage(nickname + ": " + str);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                server.unsubscribe(this);
            }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setNickname(String nick) {
        this.nickname = nick;
    }

    public void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

}