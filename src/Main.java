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

            // Příprava databáze (postupná realizace všech 4 úkolů)
            inicializujDatabazi(connVytvoreni);

            // Hlavní interaktivní menu spojující všechny požadované funkce
            boolean pokracovat = true;
            while (pokracovat) {
                System.out.println("\n================ AUTOPŮJČOVNA ================");
                System.out.println("1. Vyhledat auto podle kritérií (značka, model, barva, rok, km)");
                System.out.println("2. Vyhledat všechny výpůjčky zákazníka (podle jména a příjmení)");
                System.out.println("3. Počet výpůjček pro každého zákazníka v časovém rozmezí");
                System.out.println("0. Ukončit program");
                System.out.print("Zvolte akci: ");

                String volba = sc.nextLine().trim();

                switch (volba) {
                    case "1" -> vyhledejAuta(connVytvoreni, sc);
                    case "2" -> vyhledejVypujckyZakaznika(connVytvoreni, sc);
                    case "3" -> vypisPocetVypujcekVObdobi(connVytvoreni, sc);
                    case "0" -> pokracovat = false;
                    default -> System.out.println("Neplatná volba, zkuste to prosím znovu.");
                }
            }

            System.out.println("Program byl ukončen.");

        } catch (SQLException e) {
            System.err.println("Problém při práci s databází: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void inicializujDatabazi(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Bezpečné smazání starých tabulek (nejprve tabulka s cizími klíči)
            stmt.executeUpdate("DROP TABLE IF EXISTS vypujcky");
            stmt.executeUpdate("DROP TABLE IF EXISTS auta");
            stmt.executeUpdate("DROP TABLE IF EXISTS zakaznici");

            // --- ÚKOL 1: Vytvoření základní tabulky 'auta' a vložení dat ---
            String createAutaSQL = """
                    CREATE TABLE IF NOT EXISTS auta (
                        id_auta INT AUTO_INCREMENT PRIMARY KEY,
                        spz VARCHAR(15) NOT NULL UNIQUE,
                        znacka VARCHAR(50) NOT NULL,
                        model VARCHAR(50) NOT NULL,
                        datum_vyroby DATE NOT NULL,
                        najete_km INT NOT NULL
                    )
                    """;
            stmt.executeUpdate(createAutaSQL);
            System.out.println("[Úkol 1] Tabulka 'auta' byla vytvořena.");
        }

        String insertAutaSQL = "INSERT INTO auta (spz, znacka, model, datum_vyroby, najete_km) VALUES (?, ?, ?, ?, ?)";
        Object[][] vzorovaAuta = {
                {"4P2 8451", "Škoda", "Octavia", Date.valueOf("2020-05-14"), 68500},
                {"1AB 3920", "Škoda", "Fabia", Date.valueOf("2019-09-22"), 94200},
                {"5L8 1104", "Škoda", "Octavia", Date.valueOf("2022-03-10"), 31000},
                {"2SA 7749", "Volkswagen", "Golf", Date.valueOf("2021-11-03"), 45800},
                {"8T1 6032", "Hyundai", "i30", Date.valueOf("2020-08-19"), 59000},
                {"3B9 4512", "Toyota", "Corolla", Date.valueOf("2023-01-25"), 18400}
        };

        try (PreparedStatement pstmt = conn.prepareStatement(insertAutaSQL)) {
            for (Object[] auto : vzorovaAuta) {
                pstmt.setString(1, (String) auto[0]);
                pstmt.setString(2, (String) auto[1]);
                pstmt.setString(3, (String) auto[2]);
                pstmt.setDate(4, (Date) auto[3]);
                pstmt.setInt(5, (Integer) auto[4]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            System.out.println("[Úkol 1] Tabulka 'auta' byla naplněna daty.");
        }

        // --- ÚKOL 2: Přidání sloupce 'barva' pomocí DDL a doplnění barev ---
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE auta ADD COLUMN barva VARCHAR(30)");
            System.out.println("[Úkol 2] Do tabulky 'auta' byl přidán sloupec 'barva'.");
        }

        String updateBarvySQL = "UPDATE auta SET barva = ? WHERE spz = ?";
        String[][] barvyAut = {
                {"Stříbrná", "4P2 8451"},
                {"Modrá", "1AB 3920"},
                {"Černá", "5L8 1104"},
                {"Bílá", "2SA 7749"},
                {"Červená", "8T1 6032"},
                {"Šedá", "3B9 4512"}
        };

        try (PreparedStatement pstmt = conn.prepareStatement(updateBarvySQL)) {
            for (String[] zaznam : barvyAut) {
                pstmt.setString(1, zaznam[0]);
                pstmt.setString(2, zaznam[1]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // --- ÚKOL 3: Změna datového typu sloupce pro rok výroby na VARCHAR(4) ---
        try (Statement stmt = conn.createStatement()) {
            // Bezpečné zkrácení existujícího data (YYYY-MM-DD -> YYYY) před zúžením na 4 znaky
            stmt.executeUpdate("ALTER TABLE auta MODIFY COLUMN datum_vyroby VARCHAR(10)");
            stmt.executeUpdate("UPDATE auta SET datum_vyroby = LEFT(datum_vyroby, 4)");
            stmt.executeUpdate("ALTER TABLE auta MODIFY COLUMN datum_vyroby VARCHAR(4)");
            System.out.println("[Úkol 3] Datový typ sloupce 'datum_vyroby' byl změněn na VARCHAR(4).");
        }

        // --- ÚKOL 4: Vytvoření tabulek 'zakaznici' a 'vypujcky' + naplnění daty ---
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS zakaznici (
                        id_zakaznika INT AUTO_INCREMENT PRIMARY KEY,
                        jmeno VARCHAR(50) NOT NULL,
                        prijmeni VARCHAR(50) NOT NULL,
                        telefon VARCHAR(20),
                        email VARCHAR(100)
                    )
                    """);

            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS vypujcky (
                        id_vypujcky INT AUTO_INCREMENT PRIMARY KEY,
                        id_zakaznika INT NOT NULL,
                        id_auta INT NOT NULL,
                        datum_od DATE NOT NULL,
                        datum_do DATE NOT NULL,
                        FOREIGN KEY (id_zakaznika) REFERENCES zakaznici(id_zakaznika),
                        FOREIGN KEY (id_auta) REFERENCES auta(id_auta)
                    )
                    """);

            stmt.executeUpdate("""
                    INSERT INTO zakaznici (jmeno, prijmeni, telefon, email) VALUES
                    ('Jan', 'Novák', '777111222', 'jan.novak@email.cz'),
                    ('Petra', 'Svobodová', '608333444', 'petra.svobodova@email.cz'),
                    ('Martin', 'Dvořák', '724555666', 'martin.dvorak@email.cz'),
                    ('Lucie', 'Černá', '602777888', 'lucie.cerna@email.cz')
                    """);

            stmt.executeUpdate("""
                    INSERT INTO vypujcky (id_zakaznika, id_auta, datum_od, datum_do) VALUES
                    (1, 1, '2026-01-10', '2026-01-15'),
                    (1, 3, '2026-03-01', '2026-03-05'),
                    (1, 6, '2026-06-12', '2026-06-20'),
                    (2, 4, '2026-02-14', '2026-02-18'),
                    (2, 1, '2026-05-01', '2026-05-10'),
                    (3, 5, '2026-04-03', '2026-04-07')
                    """);

            System.out.println("[Úkol 4] Tabulky 'zakaznici' a 'vypujcky' byly vytvořeny a naplněny daty.");
        }
    }

    // Vyhledávání aut podle zadaných kritérií (Úkoly 1, 2, 3)
    private static void vyhledejAuta(Connection conn, Scanner sc) throws SQLException {
        System.out.println("\n--- VYHLEDÁVÁNÍ VOZIDLA ---");
        System.out.println("(Pro přeskočení kritéria stiskněte Enter)");

        System.out.print("Značka auta (např. Škoda): ");
        String znacka = sc.nextLine().trim();

        System.out.print("Model auta (např. Octavia): ");
        String model = sc.nextLine().trim();

        System.out.print("Barva auta (např. Černá): ");
        String barva = sc.nextLine().trim();

        System.out.print("Rok výroby (např. 2022): ");
        String rokVyroby = sc.nextLine().trim();

        System.out.print("Maximální počet najetých km: ");
        String maxKmStr = sc.nextLine().trim();

        StringBuilder sql = new StringBuilder("SELECT * FROM auta WHERE 1=1");
        List<Object> parametry = new ArrayList<>();

        if (!znacka.isEmpty()) {
            sql.append(" AND znacka LIKE ?");
            parametry.add("%" + znacka + "%");
        }
        if (!model.isEmpty()) {
            sql.append(" AND model LIKE ?");
            parametry.add("%" + model + "%");
        }
        if (!barva.isEmpty()) {
            sql.append(" AND barva LIKE ?");
            parametry.add("%" + barva + "%");
        }
        if (!rokVyroby.isEmpty()) {
            sql.append(" AND datum_vyroby = ?");
            parametry.add(rokVyroby);
        }
        if (!maxKmStr.isEmpty()) {
            try {
                int maxKm = Integer.parseInt(maxKmStr);
                sql.append(" AND najete_km <= ?");
                parametry.add(maxKm);
            } catch (NumberFormatException e) {
                System.out.println("Neplatný formát kilometrů – kritérium bylo přeskočeno.");
            }
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametry.size(); i++) {
                pstmt.setObject(i + 1, parametry.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\nNalezená vozidla:");
                System.out.printf("%-4s | %-10s | %-11s | %-10s | %-10s | %-6s | %-10s%n",
                        "ID", "SPZ", "Značka", "Model", "Barva", "Rok", "Najeto km");
                System.out.println("-".repeat(78));

                boolean nalezeno = false;
                while (rs.next()) {
                    nalezeno = true;
                    System.out.printf("%-4d | %-10s | %-11s | %-10s | %-10s | %-6s | %d km%n",
                            rs.getInt("id_auta"),
                            rs.getString("spz"),
                            rs.getString("znacka"),
                            rs.getString("model"),
                            rs.getString("barva"),
                            rs.getString("datum_vyroby"),
                            rs.getInt("najete_km"));
                }

                if (!nalezeno) {
                    System.out.println("Zadaným kritériím neodpovídá žádné vozidlo.");
                }
            }
        }
    }

    // Vyhledání všech výpůjček zákazníka podle jména a příjmení (Úkol 4a)
    private static void vyhledejVypujckyZakaznika(Connection conn, Scanner sc) throws SQLException {
        System.out.println("\n--- VÝPŮJČKY ZÁKAZNÍKA ---");
        System.out.print("Zadejte jméno zákazníka (např. Jan): ");
        String jmeno = sc.nextLine().trim();

        System.out.print("Zadejte příjmení zákazníka (např. Novák): ");
        String prijmeni = sc.nextLine().trim();

        String sql = """
                SELECT z.jmeno, z.prijmeni, a.znacka, a.model, a.barva, a.spz, v.datum_od, v.datum_do
                FROM vypujcky v
                JOIN zakaznici z ON v.id_zakaznika = z.id_zakaznika
                JOIN auta a ON v.id_auta = a.id_auta
                WHERE z.jmeno LIKE ? AND z.prijmeni LIKE ?
                ORDER BY v.datum_od
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + jmeno + "%");
            pstmt.setString(2, "%" + prijmeni + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.printf("%-18s | %-20s | %-10s | %-10s | %-12s | %-12s%n",
                        "Zákazník", "Vozidlo", "Barva", "SPZ", "Od", "Do");
                System.out.println("-".repeat(93));

                boolean nalezeno = false;
                while (rs.next()) {
                    nalezeno = true;
                    String celyZakaznik = rs.getString("jmeno") + " " + rs.getString("prijmeni");
                    String vozidlo = rs.getString("znacka") + " " + rs.getString("model");
                    System.out.printf("%-18s | %-20s | %-10s | %-10s | %-12s | %-12s%n",
                            celyZakaznik,
                            vozidlo,
                            rs.getString("barva"),
                            rs.getString("spz"),
                            rs.getDate("datum_od"),
                            rs.getDate("datum_do"));
                }

                if (!nalezeno) {
                    System.out.println("Pro zadaného zákazníka nebyly nalezeny žádné výpůjčky.");
                }
            }
        }
    }

    // Počet výpůjček pro každého zákazníka v zadaném časovém rozmezí (Úkol 4b)
    private static void vypisPocetVypujcekVObdobi(Connection conn, Scanner sc) throws SQLException {
        System.out.println("\n--- POČET VÝPŮJČEK V OBDOBÍ ---");
        System.out.print("Počáteční datum (RRRR-MM-DD, např. 2026-01-01): ");
        String datumOdStr = sc.nextLine().trim();

        System.out.print("Koncové datum (RRRR-MM-DD, např. 2026-05-31): ");
        String datumDoStr = sc.nextLine().trim();

        Date datumOd;
        Date datumDo;
        try {
            datumOd = Date.valueOf(datumOdStr);
            datumDo = Date.valueOf(datumDoStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Chybný formát data. Použijte formát RRRR-MM-DD.");
            return;
        }

        String sql = """
                SELECT z.id_zakaznika, z.jmeno, z.prijmeni, COUNT(v.id_vypujcky) AS pocet_vypujcek
                FROM zakaznici z
                LEFT JOIN vypujcky v ON z.id_zakaznika = v.id_zakaznika
                    AND v.datum_od >= ? AND v.datum_do <= ?
                GROUP BY z.id_zakaznika, z.jmeno, z.prijmeni
                ORDER BY pocet_vypujcek DESC, z.prijmeni ASC
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, datumOd);
            pstmt.setDate(2, datumDo);

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.printf("\nPočet výpůjček v období %s až %s:%n", datumOd, datumDo);
                System.out.printf("%-4s | %-12s | %-15s | %-15s%n",
                        "ID", "Jméno", "Příjmení", "Počet výpůjček");
                System.out.println("-".repeat(54));

                while (rs.next()) {
                    System.out.printf("%-4d | %-12s | %-15s | %d%n",
                            rs.getInt("id_zakaznika"),
                            rs.getString("jmeno"),
                            rs.getString("prijmeni"),
                            rs.getInt("pocet_vypujcek"));
                }
            }
        }
    }
}