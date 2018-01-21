package ru.geekbrains.chat.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class PrivateMsg extends Stage {

    public TextField pMsgField;
    public Button pMsgSend;

    private Controller controller;
    private String toNick;

    public PrivateMsg() {
    }


    /**
     * Инициализация окна приватного сообщения
     * @param controller
     * @param toNick
     */
    public PrivateMsg(Controller controller, String toNick) {
        super();

        this.controller = controller;
        this.toNick = toNick;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("privateMsg.fxml"));
            Parent msRoot = loader.load();

            setTitle("Личное сообщение для - " + toNick);
            setScene(new Scene(msRoot, 500, -1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Controller getController() {
        return controller;
    }

    public String getToNick() {
        return toNick;
    }

    /**
     * Отправить приватное сообщение
     * @param actionEvent системное событие
     */
    public void sendPrivateMessage(ActionEvent actionEvent) {

        //забавная штука, получить текущий экземпляр окна можно только через поле
        PrivateMsg stage = (PrivateMsg) pMsgField.getScene().getWindow();

        //получаем контроллер и nickname
        Controller controller = stage.getController();
        String toNick = stage.getToNick();

        if (controller == null) {
            stage.close();
            return;
        }

        //посылаем сообщение через базовый контроллер
        controller.sendToMessage(toNick, pMsgField.getText().trim());
        stage.close();

    }
}
