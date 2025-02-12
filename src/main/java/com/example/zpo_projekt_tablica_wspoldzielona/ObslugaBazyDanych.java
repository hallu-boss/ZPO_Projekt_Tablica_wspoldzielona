package com.example.zpo_projekt_tablica_wspoldzielona;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages database operations such as user authentication and fetching user data.
 */
public class ObslugaBazyDanych {
    // Ścieżka do bazy SQLite
    private static final String URL = "jdbc:sqlite:src/main/database/clinets.db";  // Ścieżka do bazy danych SQLite

    /**
     * Checks if a user exists in the database with the correct password.
     *
     * @param userNumber The user's identifier.
     * @param password   The user's password.
     * @return true if the user exists and the password is correct, false otherwise.
     */
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

    /**
     * Retrieves a list of all users from the database.
     *
     * @return A list of user identifiers.
     */
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

    /**
     * The entry point for testing user authentication and retrieving all users from the database.
     * This method checks whether certain user credentials are valid and prints the result. It also fetches
     * and displays a list of all user identifiers stored in the database.
     *
     * @param args The command-line arguments (not used in this implementation).
     */
    public static void main(String[] args) {
        // Przykład sprawdzenia użytkownika
        boolean isValid = sprawdzUzytkownika("245835", "1234");
        System.out.println("Czy dane są poprawne? " + isValid);

         isValid = sprawdzUzytkownika("245838", "1234");
        System.out.println("Czy dane są poprawne? " + isValid);

         isValid = sprawdzUzytkownika("245835", "12354");
        System.out.println("Czy dane są poprawne? " + isValid);

        // Pobranie i wyświetlenie wszystkich użytkowników
        List<String> users = pobierzWszystkichUzytkownikow();
        System.out.println("Lista użytkowników: " + users);
    }
}
