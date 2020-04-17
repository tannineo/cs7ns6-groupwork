package life.tannineo.cs7ns6.web;

import life.tannineo.cs7ns6.App;

import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(endpointInterface = "life.tannineo.cs7ns6.web.CustomWebService")
public class KVNodeWebService implements CustomWebService {

    @Override
    public String get(@WebParam(name = "host") String host, @WebParam(name = "key") String key) {
        return App.operationGet(host, key);
    }

    @Override
    public String set(@WebParam(name = "host") String host, @WebParam(name = "key") String key, @WebParam(name = "value") String value) {
        return App.operationSet(host, key, value);
    }

    @Override
    public String delete(@WebParam(name = "host") String host, @WebParam(name = "key") String key) {
        return App.operationDel(host, key);
    }

    @Override
    public String setFail(@WebParam(name = "host") String host, @WebParam(name = "peer") String peer) {
        return App.operationNoResponseTo(host, peer);
    }
}
