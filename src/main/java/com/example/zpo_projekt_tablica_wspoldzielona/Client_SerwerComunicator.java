package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client_SerwerComunicator {
    private static final int PORT = 5000;
    private Socket socket;
    private List<ChangePrint> tablica = new ArrayList<ChangePrint>();

    private ObjectInputStream in;
    private ObjectOutputStream out;


    public Client_SerwerComunicator(String login, String passoword) throws IOException, ClassNotFoundException {
        connectServer();
        sendClinetsData(login, passoword);
        loadImageFromServer();

    }

    private void connectServer() throws IOException{
        socket = new Socket("localhost", 5000);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    private void sendClinetsData(String login, String password) throws IOException{
        UserData userData = new UserData(password, login);
        out.writeObject(userData);
        out.flush();
    }

    private void loadImageFromServer() throws IOException, ClassNotFoundException {
       synchronized (tablica) {
           tablica = (List<ChangePrint>) in.readObject();

       }
    }
    List<ChangePrint> getTablica(){return tablica;}



    public void disconnectServer() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("Błąd podczas zamykania połączenia: " + e.getMessage());
        }
        System.out.println("Rozłączono się z serwerem");
    }

    public static void main(String[] args) {
        try {
            Client_SerwerComunicator client = new Client_SerwerComunicator("245835", "1234");


            client.disconnectServer();
        } catch (IOException e) {
            System.out.println("Klient nie połączył się z serwerem");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static javafx.scene.image.Image convertBufferedImageToFX(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                pixelWriter.setArgb(x, y, argb);
            }
        }
        return writableImage;
    }
}
