package ralph;

import java.util.List;

public interface EndpointConstructorObj 
{
    public Endpoint construct(
        RalphGlobals globals, 
        RalphConnObj.ConnectionObj conn_obj);

    /**
       Constructs an endpoint/service object, while filling in its
       internal values from internal_values_list.  Note that order of
       internal_values_list is order that internal objects were
       declared inside of service/endpoint.
     */
    public Endpoint construct(
        RalphGlobals globals,RalphConnObj.ConnectionObj conn_obj,
        List<RalphObject> internal_values_list);
}
