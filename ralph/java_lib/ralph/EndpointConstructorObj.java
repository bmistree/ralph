package ralph;

public interface EndpointConstructorObj 
{
    public Endpoint construct(
        WaldoGlobals globals, String host_uuid, WaldoConnObj.ConnectionObj conn_obj);
}
