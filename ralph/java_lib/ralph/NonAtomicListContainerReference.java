package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphExceptions.BackoutException;


/**
 * @param <V> --- Values held in ralph objects of list
 * @param <D> --- What each internal object in list dewaldoifies to.
 */
public abstract class NonAtomicListContainerReference <V,D> 
    extends NonAtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return NonAtomicListContainer.
    // when call set val, must pass in a NonAtomicListContainer
    NonAtomicListContainer<V,D>, 
    // what will return when call de_waldoify.
    D>
{
    public NonAtomicListContainerReference(
        String _host_uuid, boolean _peered, 
        NonAtomicListContainer<V,D> init_val, 
        NonAtomicListContainer<V,D> default_value,
        // using value type here: treating internal reference as value
        ValueTypeDataWrapperFactory<NonAtomicListContainer<V,D>,D>vtdwc)
    {
        super(_host_uuid,_peered,init_val,default_value,vtdwc);
    }
	
	
    public D de_waldoify(ActiveEvent active_event) throws BackoutException
    {
        return val.de_waldoify(active_event);
    }

    public boolean return_internal_val_from_container()
    {
        return false;
    }
}
