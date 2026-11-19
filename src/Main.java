import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String urlVytvoreni = "jdbc:mysql://localhost/vytvorenitabulky";
        String username = "root";
        String password = "";

        try (Connection connVytvoreni = DriverManager.getConnection(urlVytvoreni, username, password);
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Připojení k databázi bylo úspěšné.\n");

            vytvorTabulku(connVytvoreni);

            naplnData(connVytvoreni);

        } catch (SQLException e) {
            System.err.println("Problém při práci s databází: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void vytvorTabulku(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS auta");

            String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS auta (
                        id_auta INT AUTO_INCREMENT PRIMARY KEY,
                        spz VARCHAR(15) NOT NULL UNIQUE,
                        znacka VARCHAR(50) NOT NULL,
                        model VARCHAR(50) NOT NULL,
                        datum_vyroby DATE NOT NULL,
                        najete_km INT NOT NULL
                    )
                    """;
            stmt.executeUpdate(createTableSQL);
            System.out.println("Tabulka 'auta' byla úspěšně vytvořena.");
        }
    }

    private static void naplnData(Connection conn) throws SQLException {
        String insertSQL = "INSERT INTO auta (spz, znacka, model, datum_vyroby, najete_km) VALUES (?, ?, ?, ?, ?)";

        Object[][] vzorovaAuta = {
                {"4P2 8451", "Škoda", "Octavia", Date.valueOf("2020-05-14"), 68500},
                {"1AB 3920", "Škoda", "Fabia", Date.valueOf("2019-09-22"), 94200},
                {"5L8 1104", "Škoda", "Octavia", Date.valueOf("2022-03-10"), 31000},
                {"2SA 7749", "Volkswagen", "Golf", Date.valueOf("2021-11-03"), 45800},
                {"8T1 6032", "Hyundai", "i30", Date.valueOf("2020-08-19"), 59000},
                {"3B9 4512", "Toyota", "Corolla", Date.valueOf("2023-01-25"), 18400},
                {"6M4 9087", "Ford", "Focus", Date.valueOf("2018-06-30"), 124000}
        };

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            for (Object[] auto : vzorovaAuta) {
                pstmt.setString(1, (String) auto[0]);
                pstmt.setString(2, (String) auto[1]);
                pstmt.setString(3, (String) auto[2]);
                pstmt.setDate(4, (Date) auto[3]);
                pstmt.setInt(5, (Integer) auto[4]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            System.out.println("Do tabulky bylo vloženo " + vzorovaAuta.length + " vozidel.\n");
        }
    }

}