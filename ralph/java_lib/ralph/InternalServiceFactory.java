package ralph;

/**
   Should cast to whatever type expect outside of construct.
 */
public class InternalServiceFactory
{
    private EndpointConstructorObj endpt_constructor;
    
    public InternalServiceFactory(EndpointConstructorObj endpt_constructor)
    {
        this.endpt_constructor = endpt_constructor;
    }

    public Endpoint construct(
        RalphGlobals ralph_globals, RalphConnObj.ConnectionObj conn_obj)
    {
        return this.endpt_constructor.construct(ralph_globals,conn_obj);
    }
}