package ralph;

import java.util.List;

import RalphDurability.DurabilityContext;

public interface EndpointConstructorObj 
{
    /**
       @param durability_context --- Can be null: eg., if not supposed
       to be logging for durability.
     */
    public Endpoint construct(
        RalphGlobals globals, 
        RalphConnObj.ConnectionObj conn_obj,
        DurabilityContext durability_context);

    /**
       Constructs an endpoint/service object, while filling in its
       internal values from internal_values_list.  Note that order of
       internal_values_list is order that internal objects were
       declared inside of service/endpoint.
     */
    public Endpoint construct(
        RalphGlobals globals,RalphConnObj.ConnectionObj conn_obj,
        List<RalphObject> internal_values_list,
        DurabilityContext durability_context);
}
