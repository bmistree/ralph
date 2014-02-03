package ralph;

public interface EndpointConstructorObj 
{
    public Endpoint construct(
        RalphGlobals globals, 
        RalphConnObj.ConnectionObj conn_obj);
}
