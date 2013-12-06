package ralph;


import RalphExceptions.BackoutException;


/**
 * @param <K> --- Keys of map
 * @param <V> --- Values held in locked objects of map.
 * @param <D> --- What this object will dewaldoify into.
 */
public abstract class SingleThreadedContainerReference <K,V,D> 
    extends SingleThreadedLockedValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return SingleThreadedLockedContainer.
    // when call set val, must pass in a SingleThreadedLockedContainer
    SingleThreadedLockedContainer<K,V,D>, 
    // what will return when call de_waldoify.
    D>
{
    public SingleThreadedContainerReference(
        String _host_uuid, boolean _peered, 
        SingleThreadedLockedContainer<K,V,D> init_val, 
        SingleThreadedLockedContainer<K,V,D> default_value,
        // using value type here: treating internal reference as value
        ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>vtdwc)
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
