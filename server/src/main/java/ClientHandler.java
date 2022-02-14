import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import service.ServiceMessages;

public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String name;

    private static final Logger LOGGER = Logger.getLogger(MyServer.class.getName());

    public String getname() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public ClientHandler(MyServer myServer, final Socket socket) {
        try {
            LogManager manager = LogManager.getLogManager();
            manager.readConfiguration(new FileInputStream("server/src/main/resources/logging.properties"));
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "";

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //почему-то таймер запускается только на шаге readMsg, а во время authentication времени неограничено
                        socket.setSoTimeout(120000);
                        authentication();
                        socket.setSoTimeout(0);
                        readMsg();
                    } catch (SocketException e) {
                        sendMsg(ServiceMessages.END.getCommand());
                    } finally {
                        closeConnection();
                    }

                }
            }).start();
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public void authentication() {
        while (true) {
            try {
                String str = in.readUTF();
                if(str.startsWith("/")) {
                    if(str.startsWith(ServiceMessages.REG.getCommand())) {
                        String[] parts = str.split(" ");
                        if(myServer.getAuthService().reg(parts[1], parts[2], parts[3])) {
                            sendMsg(ServiceMessages.REG_OK.getCommand());
                        } else {
                            sendMsg(ServiceMessages.REG_NOT.getCommand());
                        }
                    }
                    if (str.startsWith(ServiceMessages.AUTH.getCommand())) {
                        String[] parts = str.split(" ");
                        String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                        if (nick != null) {
                            if (!myServer.isNickBusy(nick)) {
                                sendMsg(ServiceMessages.AUTHOK.getCommand()+ " " + nick);
                                name = nick;
                                myServer.broadcastMsg(name + " зашел в чат");
                                myServer.subscribe(this);
                                return;
                            } else {
                                sendMsg("Учетная запись уже используется");
                            }
                        } else {
                            sendMsg("Неверные логин/пароль");
                        }
                    }
                }

            }  catch (IOException e) {
                LOGGER.warning(e.getMessage());
            }
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public void readMsg() {
        try {
            while (true) {
                String strFromServer = in.readUTF();
                LOGGER.info(name+": "+strFromServer);
                if(strFromServer.equals(ServiceMessages.END.getCommand())) {
                    sendMsg(strFromServer);
                    return;
                }
                if(strFromServer.startsWith(ServiceMessages.CHANGE.getCommand())) {
                    myServer.changeNick(this, strFromServer);
                } else if(strFromServer.startsWith("/w")) {
                    myServer.privateMsg(this, strFromServer);
                } else {
                    myServer.broadcastMsg(name + ": " + strFromServer);
                }
            }
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(name + " вышел из чата");
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
    }
}