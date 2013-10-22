package ralph;

public interface EndpointConstructorObj 
{
    public Endpoint construct(
        WaldoGlobals globals, String host_uuid,
        RalphConnObj.ConnectionObj conn_obj);
}
