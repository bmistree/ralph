package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphExceptions.BackoutException;

/**
 * @param <V> --- Values held in locked objects of list.
 * @param <D> --- What this object will dewaldoify into.
 */
public abstract class AtomicInternalListReference <V,D> 
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return AtomicInternalList.
    // when call set val, must pass in a AtomicInternalList
    AtomicInternalList<V,D>, 
    // what will return when call de_waldoify.
    D>
{
    public AtomicInternalListReference(
        String _host_uuid, boolean _peered, 
        AtomicInternalList<V,D> init_val, 
        AtomicInternalList<V,D> default_value,
        // using value type here: treating internal reference as value
        ValueTypeDataWrapperFactory<AtomicInternalList<V,D>,D>vtdwc)
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
