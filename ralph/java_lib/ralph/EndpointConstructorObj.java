package ralph;

public interface EndpointConstructorObj 
{
    public Endpoint construct(
        RalphGlobals globals, String host_uuid,
        RalphConnObj.ConnectionObj conn_obj);
}
