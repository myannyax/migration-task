package core.model;

import java.util.ArrayList;

public class MigrationResult {

    public MigrationResult(ArrayList<String> initialOldServerState, ArrayList<String> finalOldServerState, ArrayList<String> newServerState, ArrayList<String> copied, ArrayList<String> deleted) {
        this.initialOldServerState = initialOldServerState;
        this.finalOldServerState = finalOldServerState;
        this.newServerState = newServerState;
        this.copied = copied;
        this.deleted = deleted;
    }

    private ArrayList<String> initialOldServerState;

    private ArrayList<String> finalOldServerState;

    private ArrayList<String> newServerState;

    private ArrayList<String> copied;

    private ArrayList<String> deleted;

    public ArrayList<String> getInitialOldServerState() {
        return initialOldServerState;
    }

    public ArrayList<String> getFinalOldServerState() {
        return finalOldServerState;
    }

    public ArrayList<String> getNewServerState() {
        return newServerState;
    }

    public ArrayList<String> getCopied() {
        return copied;
    }

    public ArrayList<String> getDeleted() {
        return deleted;
    }
}
