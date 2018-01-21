package ru.geekbrains.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Server {

    private Vector<ClientHandler> clients;

    public Server() {

        clients = new Vector<>();
        ServerSocket server = null;
        try {
            AuthService.connect();
            server = new ServerSocket(8189);
            System.out.println("Сервер запущен");

            Socket socket = null;

            //Запуск теста клиентов
            heartbeatStart();

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (server != null) server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            AuthService.disconnect();
        }
    }

    /**
     * Широковещательная рассылка
     * @param msg сообщение
     */
    public void broadcastMsg(ClientHandler from, String msg) {
        for (ClientHandler client : clients) {
            try {
                client.sendMsg(from.getNick() + ": " + msg);
            } catch (Exception e) {
                try {
                    if (!client.getSocket().isConnected()) {
                        unsubscribe(client);
                    }
                } catch (Exception ex) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Приватное сообщение
     */
    public void privateMsg(ClientHandler from, String msg) {
        String[] msgArray = msg.split(" ", 3);

        if (msgArray.length < 3) {
            return;
        }

        String toNick = msgArray[1];
        String resultMessage = msgArray[2].trim();

        if (toNick.equals(from.getNick())) {
            from.sendMsg("Вы пытаетесь послать сообщение себе!");
            return;
        }

        for (ClientHandler client : clients) {
            try {
                if (client.getNick().equals(toNick)) {
                    client.sendMsg(from.getNick() + " говорит вам: " + resultMessage);
                    from.sendMsg(from.getNick() + ": " + resultMessage);
                    return;
                }
            } catch (Exception e) {
                //может быть ошибка, если вдруг сокет отвалился
                try {
                    if (!client.getSocket().isConnected()) {
                        unsubscribe(client);
                    }
                } catch (Exception ex) {
                    e.printStackTrace();
                }
            }
        }

        from.sendMsg("Клиент с ником - " + toNick + " не найден в чате");
    }

    /**
     * Подписать клиента
     * @param client клиент
     */
    public void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastClients();
    }

    /**
     * Отписать клиента
     * @param client клиент
     */
    public void unsubscribe(ClientHandler client) {
        System.out.println(client.getNick() + " : отключился от сервера");
        clients.remove(client);
        broadcastClients();
    }

    public void broadcastClients() {

        List<String> clientsList = new ArrayList<>();
        for (ClientHandler client : clients) {
            clientsList.add(client.getNick());
        }

        String out = "/clientslist " + String.join(" ", clientsList);

        for (ClientHandler client : clients) {
            client.sendMsg(out);
        }
    }

    /**
     * Проверка ника на занятость
     * @param nick проверяемый ник
     * @return истина если такой уже подключился
     */
    public boolean isNickBusy(String nick) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nick)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Сердцебиение клиентов, это бесполезный код :-)
     */
    private void heartbeatStart() {

        Thread pingThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println("Server was broken:");
                    e.printStackTrace();
                    break;
                }

                for (ClientHandler client : clients) {
                    if (!client.getSocket().isConnected()) {
                        unsubscribe(client);
                    }
                }
            }
        });

        pingThread.setDaemon(true);
        pingThread.start();
    }
}
