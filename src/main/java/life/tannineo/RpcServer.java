package life.tannineo;

import com.github.tannineo.entity.Request;
import com.github.tannineo.entity.Response;

public interface RpcServer {

    void start();

    void stop();

    Response handlerRequest(Request request);

}
