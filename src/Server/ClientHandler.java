package Server;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

public class ClientHandler {
    private ConsoleServer server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;

    private List<String> blackList;


    public ClientHandler(ConsoleServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                boolean isExit = false;
            try {
                while(true) {
                    try {
                        socket.setSoTimeout(120000);
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
                    if("/end".equals(str)) {
                        isExit = true;
                        break;
                    }
                    } catch (SocketTimeoutException e) {
                        isExit = true;
                        sendMsg("Time out");
                        socket.close();
                        break;
                    }
                }

                if(!isExit) {
                    showHistory("history_" + nickname + ".txt");
                    server.broadcastMessage(this, nickname + " joined chat");
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/") || str.startsWith("@")) {
                            if ("/end".equals(str)) {
                                server.broadcastMessage(this, nickname + " left chat");
                                out.writeUTF("/serverClosed");
                                System.out.printf("Client [%s] - disconnected\n", socket.getInetAddress());
                                break;
                            }
                            if (str.startsWith("@")) {
                                String[] tokens = str.split(" ", 2);
                                if (AuthService.doesUserExist(tokens[0].substring(1, tokens[0].length()))) {
                                    server.sendPrivateMsg(this, tokens[0].substring(1, tokens[0].length()), tokens[1]);
                                } else {
                                    sendMsg("User " + tokens[0].substring(1, tokens[0].length()) + " doesn't exist");
                                }

                            }
                            if (str.startsWith("/blacklist")) {
                                String[] tokens = str.split(" ");
                                if(AuthService.doesUserExist(tokens[1])) {
                                    int result = AuthService.blackListAdd(nickname, tokens[1]);
                                    if(result > 0) {
                                        sendMsg("You have added user " + tokens[1] + " to blacklist");
                                    } else  {
                                        sendMsg("User " + tokens[1] + " already blocked");
                                    }
                                } else {
                                    sendMsg("User " + tokens[1] + " doesn't exist");
                                }
                            }
                            if(str.startsWith("/removefromblacklist")) {
                                String[] tokens = str.split(" ");
                                AuthService.blackListRemove(nickname, tokens[1]);
                                sendMsg("User " + tokens[1] + " has been removed from blacklist");
                            }
                            if("/clientlist".equals(str)) {
                                server.broadcastClientsList();
                            }
                            if(str.startsWith("/changenickname")) {
                                String[] tokens = str.split(" ");
                                AuthService.changeNick(nickname, tokens[1]);
                                setNickname(tokens[1]);
                                this.sendMsg("Your nickname has been changed");
                            }
                        } else {
                            server.broadcastMessage(this, nickname + ": " + str);
                        }
                        System.out.printf("Client [%s]: %s\n", socket.getInetAddress(), str);
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

    public String getNickname() {
        return nickname;
    }

    public void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void saveHistory(String msg) {
        File historyFile = new File("history_" + nickname + ".txt");
        if (!historyFile.exists()) {
            try {
                historyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(historyFile, true)));) {
            bufferedWriter.write(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        public void showHistory(String historyFile) {
        int lastLines = 100;
        String line;
        int length = 0;
            try(BufferedReader reader = new BufferedReader(new FileReader(historyFile));){
                while (reader.readLine() != null) {
                    length++;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            try(BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(historyFile)));) {
                if(length <= lastLines) {
                    while ((line = bufferedReader.readLine()) != null) {
                        sendMsg(line);
                    }
                }
                if(length > lastLines) {
                    int n = length - lastLines;
                    for (int i = 0; i < n; i++) {
                        bufferedReader.readLine();
                    }
                    while ((line = bufferedReader.readLine()) != null) {
                        sendMsg(line);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
}
}
