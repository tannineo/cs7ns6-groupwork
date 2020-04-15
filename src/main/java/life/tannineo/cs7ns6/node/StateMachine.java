package life.tannineo.cs7ns6.node;

import com.alibaba.fastjson.JSON;
import life.tannineo.cs7ns6.node.entity.Command;
import life.tannineo.cs7ns6.node.entity.LogEntry;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class StateMachine {

    private Logger logger;


    public static RocksDB machineDb;
    public String dbDir;

    public StateMachine(String dbDir) {
        this.logger = LoggerFactory.getLogger(StateMachine.class);

        this.dbDir = dbDir;

        RocksDB.loadLibrary();

        synchronized (this) {
            try {
                File file = new File(dbDir);
                boolean success = false;
                if (!file.exists()) {
                    success = file.mkdirs();
                }
                if (success) {
                    logger.warn("make a new dir : " + dbDir);
                }
                Options options = new Options();
                options.setCreateIfMissing(true);
                machineDb = RocksDB.open(options, dbDir);
            } catch (RocksDBException e) {
                logger.info(e.getMessage());
            }
        }
    }


    public LogEntry get(String key) {
        try {
            byte[] result = machineDb.get(key.getBytes());
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            logger.info(e.getMessage());
        }
        return null;
    }

    public String getString(String key) {
        try {
            byte[] bytes = machineDb.get(key.getBytes());
            if (bytes != null) {
                return new String(bytes);
            }
        } catch (RocksDBException e) {
            logger.info(e.getMessage());
        }
        return "";
    }

    public void setString(String key, String value) {
        try {
            machineDb.put(key.getBytes(), value.getBytes());
        } catch (RocksDBException e) {
            logger.info(e.getMessage());
        }
    }

    public void delString(String... key) {
        try {
            for (String s : key) {
                machineDb.delete(s.getBytes());
            }
        } catch (RocksDBException e) {
            logger.info(e.getMessage());
        }
    }

    public synchronized void apply(LogEntry logEntry) {
        try {
            Command command = logEntry.getCommand();

            if (command == null) {
                throw new IllegalArgumentException("command can not be null, logEntry : " + logEntry.toString());
            }
            String key = command.getKey();
            machineDb.put(key.getBytes(), JSON.toJSONBytes(logEntry));
        } catch (RocksDBException e) {
            logger.info(e.getMessage());
        }
    }

}

