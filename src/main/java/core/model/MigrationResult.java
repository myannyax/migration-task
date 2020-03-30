package core.model;

import java.util.ArrayList;

public class MigrationResult {

    public MigrationResult(ArrayList<String> copied, ArrayList<String> deleted, ArrayList<String> finalOldServerState, boolean isSuccessful) {
        this.copied = copied;
        this.deleted = deleted;
        this.finalOldServerState = finalOldServerState;
        this.isSuccessful = isSuccessful;
    }

    private ArrayList<String> copied;

    private ArrayList<String> deleted;

    private ArrayList<String> finalOldServerState;

    private boolean isSuccessful;

    public ArrayList<String> getCopied() {
        return copied;
    }

    public ArrayList<String> getDeleted() {
        return deleted;
    }

    public ArrayList<String> getFinalOldServerState() {
        return finalOldServerState;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }
}
