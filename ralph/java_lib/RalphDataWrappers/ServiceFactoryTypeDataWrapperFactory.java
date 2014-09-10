package RalphDataWrappers;

import ralph.InternalServiceFactory;


public class ServiceFactoryTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<InternalServiceFactory>
{	
    @Override
    public DataWrapper<InternalServiceFactory> construct(
        InternalServiceFactory _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<InternalServiceFactory>(_val);
    }
}
