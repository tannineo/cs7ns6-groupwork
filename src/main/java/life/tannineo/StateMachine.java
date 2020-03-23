package life.tannineo;

import java.io.File;

/**
 * The state machine for Raft
 * <p>
 * It is used to store data/log
 */
public interface StateMachine {

    boolean put(String key, String value);

    String get(String key);

    boolean del(String key);

    /**
     * Used when starting the server
     *
     * @param snapshotFile
     */
    boolean readSnapshot(File snapshotFile);

    /**
     * used periodically to save the progress
     *
     * @param snapshotFile
     */
    boolean writeSnapshot(File snapshotFile);

}
