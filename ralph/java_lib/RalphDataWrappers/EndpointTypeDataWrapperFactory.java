package RalphDataWrappers;

import ralph.Endpoint;

public class EndpointTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<Endpoint,Endpoint>
{	
    @Override
    public DataWrapper<Endpoint,Endpoint> construct(
        Endpoint _val, boolean log_changes)
    {        
        return new ValueTypeDataWrapper<Endpoint,Endpoint>(
            _val,log_changes);
    }
}
