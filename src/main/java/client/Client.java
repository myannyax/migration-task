package client;

import core.Migration;
import core.model.MigrationResult;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class Client {

    public static void main(String[] args) {
        Migration migration = new Migration();
        try {
            System.out.println("Transferring files...");

            Instant start = Instant.now();
            MigrationResult migrationResult = migration.transferFiles();
            Instant finish = Instant.now();

            System.out.println("Time: " + Duration.between(start, finish).getSeconds());

            if (migrationResult.isSuccessful()) {
                System.out.println("Migration was successful");
            } else {
                System.out.print("Migration wasn't successful\nNumber of files copied: ");
                System.out.println(migrationResult.getCopied().size());
                System.out.print("Number of files deleted: ");
                System.out.println(migrationResult.getDeleted().size());
                System.out.print("Number of files remaining: ");
                System.out.println(migrationResult.getFinalOldServerState().size());
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
