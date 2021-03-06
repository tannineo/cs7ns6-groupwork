package life.tannineo.cs7ns6.node.entity.network;

import life.tannineo.cs7ns6.node.entity.param.EntryParam;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * the raft communication request
 *
 * @param <T>
 */
@Getter
@Setter
@ToString
public class Request<T> implements Serializable {

    /**
     * request to vote
     */
    public static final int R_VOTE = 0;

    /**
     * append log entries
     */
    public static final int A_ENTRIES = 1;

    /**
     * client request
     */
    public static final int CLIENT_REQ = 2;

    /**
     * add peer
     */
    public static final int CHANGE_CONFIG_ADD = 3;

    /**
     * remove peer
     */
    public static final int CHANGE_CONFIG_REMOVE = 4;

    /**
     * get config (peers)
     */
    public static final int GET_CONFIG = 5;


    /**
     * set fail to true / false
     */
    public static final int SET_FAIL = 6;

    /**
     * the command type from above
     */
    private int cmd = -1;

    /**
     * param
     *
     * @see EntryParam
     * @see life.tannineo.cs7ns6.node.entity.param.RevoteParam
     * @see ClientKVReq
     */
    private T obj;

    String url;

    String fromServer;

    public Request() {
    }

    public Request(T obj) {
        this.obj = obj;
    }

    public Request(int cmd, T obj, String url, String fromServer) {
        this.cmd = cmd;
        this.obj = obj;
        this.url = url;
        this.fromServer = fromServer;
    }

    private Request(Builder builder) {
        setCmd(builder.cmd);
        setObj((T) builder.obj);
        setUrl(builder.url);
        setFromServer(builder.fromServer);
    }

    public static Builder newBuilder() {
        return new Builder<>();
    }


    public final static class Builder<T> {

        private int cmd;
        private Object obj;
        private String url;
        private String fromServer;

        private Builder() {
        }

        public Builder cmd(int val) {
            cmd = val;
            return this;
        }

        public Builder obj(Object val) {
            obj = val;
            return this;
        }

        public Builder url(String val) {
            url = val;
            return this;
        }

        public Builder fromServer(String val) {
            fromServer = val;
            return this;
        }

        public Request<T> build() {
            return new Request<T>(this);
        }
    }

}
