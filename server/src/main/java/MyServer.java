import service.ServiceMessages;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServer {
    private final int PORT = 8189;

    private List<ClientHandler> clients;
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public MyServer() {
        try(ServerSocket server = new ServerSocket(PORT)) {
            authService = new BaseAuthService();
            authService.start();
            clients = new ArrayList<>();
            while (true) {
                System.out.println("Сервер ожидает подключения");
                Socket socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public boolean isNickBusy (String nick) {
        for (ClientHandler o: clients
        ) {
            if (o.getname().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void subscribe(ClientHandler o) {
        clients.add(o);
        updateSheet();
    }

    public synchronized void unsubscribe(ClientHandler o) {
        clients.remove(o);
        updateSheet();
    }

    public synchronized void broadcastMsg(String msg) {
        for (ClientHandler o: clients
        ) {
            o.sendMsg(msg);
        }
    }
    public void privateMsg(ClientHandler client, String msg) {
        if (client.getname().equals(msg.split(" ")[1])) {
            client.sendMsg(client.getname() + "->" + client.getname() + ": " + msg.split(" ", 3)[2]);
        } else {
            for (ClientHandler o : clients
            ) {
                if (o.getname().equals(msg.split(" ")[1])) {
                    o.sendMsg(client.getname() + "->" + o.getname() + ": " + msg.split(" ", 3)[2]);
                    client.sendMsg(client.getname() + "->" + o.getname() + ": " + msg.split(" ", 3)[2]);
                    break;
                }
            }
        }
    }

    public void changeNick(ClientHandler client, String msg) {
        if(authService.changeNick(msg.split(" ")[1], msg.split(" ")[2])) {
            client.setName(msg.split(" ")[2]);
            client.sendMsg("Nickname изменен.");
        } else {
            client.sendMsg("Неверный логин/ nickname занят");
        }
        updateSheet();
    }

    public void updateSheet() {
        StringBuilder str = new StringBuilder(ServiceMessages.CLIENTS.getCommand());
        for (ClientHandler a: clients
        ) {
            str.append(" "+a.getname());
        }
        broadcastMsg(str.toString());
    }
}
