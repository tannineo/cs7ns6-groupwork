package life.tannineo;

import com.github.tannineo.entity.Request;
import com.github.tannineo.entity.Response;

public interface RpcClient {

    Response send(Request request);

    Response send(Request request, int timeout);
}

