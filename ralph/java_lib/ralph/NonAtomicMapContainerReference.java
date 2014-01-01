package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphExceptions.BackoutException;


/**
 * @param <K> --- Keys of map
 * @param <V> --- Values held in locked objects of map.
 * @param <D> --- What this object will dewaldoify into.
 */
public abstract class NonAtomicMapContainerReference <K,V,D> 
    extends NonAtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return NonAtomicMapContainer.
    // when call set val, must pass in a NonAtomicMapContainer
    NonAtomicMapContainer<K,V,D>, 
    // what will return when call de_waldoify.
    D>
{
    public NonAtomicMapContainerReference(
        String _host_uuid, boolean _peered, 
        NonAtomicMapContainer<K,V,D> init_val, 
        NonAtomicMapContainer<K,V,D> default_value,
        // using value type here: treating internal reference as value
        ValueTypeDataWrapperFactory<NonAtomicMapContainer<K,V,D>,D>vtdwc)
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
