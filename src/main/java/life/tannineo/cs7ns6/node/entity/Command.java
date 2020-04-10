package life.tannineo.cs7ns6.node.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;

/**
 * the command stored in the log
 */
@Getter
@Setter
@ToString
public class Command implements Serializable {

    String opt;

    String key;

    String value;

    public Command(String opt, String key, String value) {
        this.opt = opt;
        this.key = key;
        this.value = value;
    }

    private Command(Builder builder) {
        setOpt(builder.opt);
        setKey(builder.key);
        setValue(builder.value);
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Command command = (Command) o;
        return Objects.equals(key, command.key) &&
            Objects.equals(value, command.value) && Objects.equals(opt, command.opt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, key, value);
    }

    public static final class Builder {

        private String key;
        private String value;
        private String opt;

        private Builder() {
        }

        public Builder key(String val) {
            key = val;
            return this;
        }

        public Builder value(String val) {
            value = val;
            return this;
        }

        public Builder opt(String val) {
            opt = val;
            return this;
        }

        public Command build() {
            return new Command(this);
        }
    }
}
