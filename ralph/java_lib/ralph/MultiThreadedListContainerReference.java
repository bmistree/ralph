package ralph;

import RalphExceptions.BackoutException;


/**
 * @param <V> --- Values held in locked objects of list.
 * @param <D> --- What this object will dewaldoify into.
 */
public abstract class MultiThreadedListContainerReference <V,D> 
    extends LockedValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return MultiThreadedLockedContainer.
    // when call set val, must pass in a MultiThreadedLockedContainer
    MultiThreadedListContainer<V,D>, 
    // what will return when call de_waldoify.
    D>
{
    public MultiThreadedListContainerReference(
        String _host_uuid, boolean _peered, 
        MultiThreadedListContainer<V,D> init_val, 
        MultiThreadedListContainer<V,D> default_value,
        // using value type here: treating internal reference as value
        ValueTypeDataWrapperConstructor<MultiThreadedListContainer<V,D>,D>vtdwc)
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
