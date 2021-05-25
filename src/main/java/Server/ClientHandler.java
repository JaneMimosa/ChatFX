package Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private String login;

    private List<String> blackList;

    private static final Logger LOG = LogManager.getLogger(ClientHandler.class.getName());


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
                        setLogin(tokens[1]);
                        if(nick != null) {
                            if (!server.isNickBusy(nick)) {
                                sendMsg("/auth-OK");
                                setNickname(nick);
                                server.subscribe(ClientHandler.this);
                                break;
                            } else {
                                sendMsg("User already logged");
                                LOG.info("Client {} tried to log in under already logged user", socket.getInetAddress());
                            }
                        } else {
                            sendMsg("Wrong login or password");
                            LOG.info("Client {} entered wrong password or login", socket.getInetAddress());
                        }
                        }
                    if("/end".equals(str)) {
                        isExit = true;
                        break;
                    }
                    } catch (SocketTimeoutException e) {
                        LOG.info("Client {} did not log in in time", socket.getInetAddress());
                        isExit = true;
                        sendMsg("Time out");
                        socket.close();
                        break;
                    }
                }

                if(!isExit) {
                    showHistory("src/History/history_" + login + ".txt");
                    server.broadcastMessage(this, nickname + " joined chat");
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/") || str.startsWith("@")) {
                            if ("/end".equals(str)) {
                                server.broadcastMessage(this, nickname + " left chat");
                                out.writeUTF("/serverClosed");
                                break;
                            }
                            if (str.startsWith("@")) {
                                String[] tokens = str.split(" ", 2);
                                if (AuthService.doesUserExist(tokens[0].substring(1))) {
                                    server.sendPrivateMsg(this, tokens[0].substring(1), tokens[1]);
                                } else {
                                    sendMsg("User " + tokens[0].substring(1) + " doesn't exist");
                                }

                            }
                            if (str.startsWith("/blacklist")) {
                                String[] tokens = str.split(" ");
                                if(AuthService.doesUserExist(tokens[1])) {
                                    int result = AuthService.blackListAdd(nickname, tokens[1]);
                                    if(result > 0) {
                                        sendMsg("You have added user " + tokens[1] + " to blacklist");
                                        LOG.info("Client {} blacklisted {}", login, tokens[1]);
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
                                LOG.info("Client {} removed from blacklist {}", login, tokens[1]);
                            }
                            if("/clientlist".equals(str)) {
                                server.broadcastClientsList();
                            }
                            if(str.startsWith("/changenickname")) {
                                String[] tokens = str.split(" ");
                                AuthService.changeNick(nickname, tokens[1]);
                                setNickname(tokens[1]);
                                this.sendMsg("Your nickname has been changed");
                                LOG.info("Client {} changed their nickname", login);
                            }
                        } else {
                            server.broadcastMessage(this, nickname + ": " + str);
                        }
                        //System.out.printf("Client [%s]: %s\n", socket.getInetAddress(), str);

                    }
                }
            } catch (IOException e) {
                LOG.error("Exception: '{}' While handling client {} with InetAddress {}", e.toString(), login, socket.getInetAddress());
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


    public void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void saveHistory(String msg) {
        File historyFile = new File("src/History/history_" + login + ".txt");
        if (!historyFile.exists()) {
            try {
                historyFile.createNewFile();
            } catch (IOException e) {
                LOG.error("Exception: '{}' Couldn't create history file for client {}", e.toString(), login);
                e.printStackTrace();
            }
        }
        try (BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(historyFile, true)))) {
            bufferedWriter.write(msg + "\n");
        } catch (IOException e) {
            LOG.error("Exception: '{}' Couldn't append message to history file for client {}", e.toString(),login);
            e.printStackTrace();
        }
    }

        public void showHistory(String historyFile) {
        int lastLines = 100;
        String line;
        int length = 0;
            try(BufferedReader reader = new BufferedReader(new FileReader(historyFile))){
                while (reader.readLine() != null) {
                    length++;
                }
                LOG.debug(length);
            } catch (IOException e) {
                LOG.error("Exception: '{}' Error in reading file {}", e.toString(), historyFile);
                e.printStackTrace();
            }


            try(BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(historyFile)))) {
                if(length <= lastLines) {
                    while ((line = bufferedReader.readLine()) != null) {
                        sendMsg(line);
                    }
                }
                if(length > lastLines) {
                    int n = length - lastLines;
                    LOG.debug(n);
                    for (int i = 0; i < n; i++) {
                        bufferedReader.readLine();
                    }
                    while ((line = bufferedReader.readLine()) != null) {
                        sendMsg(line);
                    }
                }
            } catch (IOException e) {
                LOG.error("Exception: '{}' Error in reading file {}", e.toString(), historyFile);
                e.printStackTrace();
            }
}

    public String getNickname() {
        return nickname;
    }

    private void setNickname(String nick) {
        this.nickname = nick;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getLogin() {
        return login;
    }

}
