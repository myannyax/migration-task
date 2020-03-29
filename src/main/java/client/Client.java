package client;

import core.Migration;
import core.model.MigrationResult;

import java.io.IOException;

public class Client {

    public static void main(String[] args) {
        Migration migration = new Migration();
        try {
            System.out.println("Transferring files...");

            migration.transferFiles();

            MigrationResult migrationResult = migration.getMigrationResult();

            if (migrationResult.getInitialOldServerState().size() == migrationResult.getNewServerState().size()
                    && migrationResult.getFinalOldServerState().size() == 0) {
                System.out.println("Migration was successful");
            } else {
                System.out.print("Migration wasn't successful\nNumber of files copied: ");
                System.out.println(migrationResult.getCopied().size());
                System.out.print("Number of files deleted from the old server: ");
                System.out.println(migrationResult.getDeleted().size());
                System.out.print("Number of files stored on the old server: ");
                System.out.println(migrationResult.getFinalOldServerState().size());
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
