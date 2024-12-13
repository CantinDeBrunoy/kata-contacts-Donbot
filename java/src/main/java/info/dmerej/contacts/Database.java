package info.dmerej.contacts;

import java.io.File;
import java.sql.*;
import java.util.stream.Stream;

public class Database {
    private Connection connection;
    private int insertedCount = 0;

    public Database(File databaseFile) {
        String databasePath = databaseFile.getPath();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        } catch (SQLException e) {
            throw new RuntimeException("Could not create connection: " + e.toString());
        }
    }

    public void migrate() {
        System.out.println("Migrating database ...");
        try {
            Statement statement = connection.createStatement();
            statement.execute("""
                    CREATE TABLE contacts(
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL
                    )
                    """
            );
            statement.execute("CREATE UNIQUE INDEX index_contacts_email ON contacts(email);");
        } catch (SQLException e) {
            throw new RuntimeException("Could not migrate db: " + e.toString());
        }
        System.out.println("Done migrating database");
    }

    public void insertContacts(Stream<Contact> contacts) {
        String query = "INSERT INTO contacts(name, email) VALUES(?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            // Disable auto-commit to insert batch of contacts
            connection.setAutoCommit(false);

            int batchSize = 10000;
            int batchCount = 0;

            var iterator = contacts.iterator();
            while (iterator.hasNext()) {
                Contact contact = iterator.next();
                try {
                    statement.setString(1, contact.name());
                    statement.setString(2, contact.email());
                    statement.addBatch();
                    batchCount++;

                    if (batchCount % batchSize == 0) {
                        statement.executeBatch();
                        connection.commit();
                        batchCount = 0;
                        System.out.println("Batch of " + batchSize + " contacts inserted.");
                    }
                } catch (SQLException e) {
                    connection.rollback();
                    throw new RuntimeException("Could not insert contact: " + e.toString());
                }
            }

            if (batchCount > 0) {
                statement.executeBatch();
                connection.commit();
                System.out.println("Final batch of " + batchCount + " contacts inserted.");
            }

            // Enable auto-commit after inserting all contacts
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Error preparing statement: " + e.toString());
        }
    }

    public String getContactNameFromEmail(String email) {
        String query = "SELECT name FROM contacts WHERE email = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, email);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString(1);
            } else {
                throw new RuntimeException("No match in the db for email: " + email);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error when looking up contacts from db: " + e.toString());
        }
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Could not close db: " + e.toString());
        }
    }

}
