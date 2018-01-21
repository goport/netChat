package ru.geekbrains.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private Server server;
    private DataOutputStream out;
    private DataInputStream in;
    private String nick;

    public String getNick() {
        return nick;
    }

    public Socket getSocket() {
        return socket;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;

            //Сброс по таймауту
            this.socket.setSoTimeout(2 * 60 * 1000); // это таймаут чтения при подключении
            //this.socket.setSoTimeout(10000); // это таймаут чтения при подключении

            this.server = server;

            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {

                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/auth")) { // /auth login72 pass72
                            String[] token = str.split(" ");
                            String newNick = AuthService.getNickByLoginAndPass(token[1], token[2]);
                            if (newNick != null) {
                                if (!server.isNickBusy(newNick)) {
                                    sendMsg("/authok");
                                    nick = newNick;
                                    server.subscribe(this);

                                    //сбрасываем таймаут после подключения
                                    this.socket.setSoTimeout(0);
                                    break;
                                } else {
                                    sendMsg("Указанный логин уже авторизован!");
                                }

                            } else {
                                sendMsg("Неверный логин или пароль");
                            }
                        }
                    }

                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            out.writeUTF("/serverclosed");
                            break;
                        }

                        // Вызов приватного сообщения
                        if (str.startsWith("/w ")) {
                            server.privateMsg(this, str);
                        } else {
                            server.broadcastMsg(this, str);
                        }

                        System.out.println(getNick() + " : " + str);

                    }
                } catch (IOException e) {
                    System.out.println("Работа прервана по exception");
                    //e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
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

    /**
     * Послать сообщение
     * @param msg текст сообщения
     */
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
