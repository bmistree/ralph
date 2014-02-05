package RalphDataWrappers;

import ralph.InternalServiceFactory;


public class ServiceFactoryTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<InternalServiceFactory,InternalServiceFactory>
{	
    @Override
    public DataWrapper<InternalServiceFactory, InternalServiceFactory> construct(
        InternalServiceFactory _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<InternalServiceFactory,InternalServiceFactory>(
            _val,log_changes);
    }
}
