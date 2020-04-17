package life.tannineo.cs7ns6.web;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style = Style.RPC)
public interface CustomWebService {
    @WebMethod(operationName = "get")
    String get(@WebParam(name = "host") String host, @WebParam(name = "key") String key);

    @WebMethod(operationName = "set")
    String set(@WebParam(name = "host") String host, @WebParam(name = "key") String key, @WebParam(name = "value") String value);

    @WebMethod(operationName = "delete")
    String delete(@WebParam(name = "host") String host, @WebParam(name = "key") String key);

    @WebMethod(operationName = "setFail")
    String setFail(@WebParam(name = "host") String host, @WebParam(name = "peer") String peer);

}
