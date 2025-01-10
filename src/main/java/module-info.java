module com.example.zpo_projekt_tablica_wspoldzielona {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.zpo_projekt_tablica_wspoldzielona to javafx.fxml;
    exports com.example.zpo_projekt_tablica_wspoldzielona;
}