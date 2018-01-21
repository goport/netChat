package ru.geekbrains.chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {

    public TextArea chatArea;
    public TextField msgField;
    public HBox upperPanel;
    public TextField loginField;
    public PasswordField passwordField;
    public HBox bottomPanel;
    public ListView<String> clientsList;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    final String IP_ADDRESS = "localhost";
    final int PORT = 8189;

    private boolean isAuthorized;

    private PrivateMsg privateMsg;

    public PrivateMsg getPrivateMsg() {
        return privateMsg;
    }

    public void setPrivateMsg(PrivateMsg privateMsg) {
        this.privateMsg = privateMsg;
    }

    public void setAuthorized(boolean authorized) {
        isAuthorized = authorized;
        upperPanel.setVisible(!isAuthorized);
        upperPanel.setManaged(!isAuthorized);

        bottomPanel.setVisible(isAuthorized);
        bottomPanel.setManaged(isAuthorized);
    }

    public void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            setAuthorized(false);

            Thread worker = new Thread(() -> {
                try {

                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/authok")) {
                            setAuthorized(true);
                            break;
                        } else {
                            chatArea.appendText(str + "\n");
                        }
                    }

                    while (true) {
                        String str = null;

                        try {
                            str = in.readUTF();
                        } catch (IOException e) {
                            System.out.println("Socket closed");
                            break;
                        }

                        if (str.startsWith("/")) {
                            if (str.equals("/serverclosed")) break;

                            //Получение списка клиентов с сервера
                            if (str.startsWith("/clientslist")) {
                                String[] tokens = str.split(" ");

                                //!!! Данный метод вызывается при работе с интерфейсом из другого потока или внутри потока !!!
                                Platform.runLater(() -> {
                                    clientsList.getItems().clear();

                                    for (int i = 1; i < tokens.length; i++) {
                                        clientsList.getItems().add(tokens[i]);
                                    }
                                });

                            }
                        } else {
                            chatArea.appendText(str + "\n");
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    setAuthorized(false);
                }

            });

            worker.setDaemon(true);
            worker.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Послать сообщение
     */
    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отсылка приватного сообщения из отдельного окна
     * @param toNick кому шлем
     * @param msg что шлем
     */
    public void sendToMessage(String toNick, String msg) {
        try {
            out.writeUTF("/w " + toNick + " " + msg);
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация диалога отправки приватного сообщения
     * @param mouseEvent системное событие
     */
    public void sendPrivateMessage(MouseEvent mouseEvent) {
        //Обработка двойного клика
        if (mouseEvent.getClickCount() == 2) {

            //получение выбранной строки
            String toNickName = clientsList.getSelectionModel().getSelectedItem();

            if (toNickName == null || toNickName.isEmpty()) {
                return;
            }

            //инициализация формы отправки приватного сообщения
            PrivateMsg privateMsg = new PrivateMsg(this, toNickName);

            privateMsg.show();
        }
    }

    /**
     * Попытка авторизации
     *
     * @param actionEvent системное сообщение
     */
    public void tryToAuth(ActionEvent actionEvent) {

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Закрываем соединение при закрытии окна
     */
    public void closeConnection() {

        if (socket == null || socket.isClosed()) {
            setAuthorized(false);
            return;
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setAuthorized(false);
    }


}
