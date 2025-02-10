package com.example.zpo_projekt_tablica_wspoldzielona;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ObslugaBazyDanych {
    // Ścieżka do bazy SQLite
    private static final String URL = "jdbc:sqlite:src/main/database/clinets.db";  // Ścieżka do bazy danych SQLite

    // Metoda do sprawdzania, czy użytkownik istnieje i czy hasło jest poprawne
    public static boolean sprawdzUzytkownika(String userNumber, String password) {
        String sql = "SELECT * FROM UZYTKOWNICY WHERE indeks = ? AND haslo = ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userNumber);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Jeśli zwróci wynik, to dane są poprawne

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Metoda zwracająca listę wszystkich użytkowników
    public static List<String> pobierzWszystkichUzytkownikow() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT indeks FROM UZYTKOWNICY";  // Pobieramy tylko numery użytkowników

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(rs.getString("indeks"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    public static void main(String[] args) {
        // Przykład sprawdzenia użytkownika
        boolean isValid = sprawdzUzytkownika("245835", "12344");
        System.out.println("Czy dane są poprawne? " + isValid);

        // Pobranie i wyświetlenie wszystkich użytkowników
        List<String> users = pobierzWszystkichUzytkownikow();
        System.out.println("Lista użytkowników: " + users);
    }
}
