package ralph;

import RalphConnObj.SingleSideConnection;

/**
   Should cast to whatever type expect outside of construct.
 */
public class InternalServiceFactory
{
    private EndpointConstructorObj endpt_constructor = null;
    private RalphGlobals ralph_globals = null;

    
    public InternalServiceFactory(
        EndpointConstructorObj endpt_constructor,RalphGlobals ralph_globals)
    {
        this.endpt_constructor = endpt_constructor;
        this.ralph_globals = ralph_globals;
    }

    public Endpoint construct(ActiveEvent active_event)
    {
        SingleSideConnection ssc = new SingleSideConnection();
        return this.endpt_constructor.construct(ralph_globals,ssc);
    }
}