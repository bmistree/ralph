package ralph;


import RalphExceptions.BackoutException;


/**
 * @param <K> --- Keys of map
 * @param <V> --- Values held in locked objects of map.
 * @param <D> --- What this object will dewaldoify into.
 */
public abstract class AtomicMapContainerReference <K,V,D> 
    extends LockedValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return AtomicMapContainer.
    // when call set val, must pass in a AtomicMapContainer
    AtomicMapContainer<K,V,D>, 
    // what will return when call de_waldoify.
    D>
{
    public AtomicMapContainerReference(
        String _host_uuid, boolean _peered, 
        AtomicMapContainer<K,V,D> init_val, 
        AtomicMapContainer<K,V,D> default_value,
        // using value type here: treating internal reference as value
        ValueTypeDataWrapperConstructor<AtomicMapContainer<K,V,D>,D>vtdwc)
    {
        super(_host_uuid,_peered,init_val,default_value,vtdwc);
    }
	
	
    public D de_waldoify(ActiveEvent active_event) throws BackoutException
    {
        Util.logger_warn("Must acquire read lock when de waldoifying.");
        return val.de_waldoify(active_event);
    }

    public boolean return_internal_val_from_container()
    {
        return false;
    }
}
