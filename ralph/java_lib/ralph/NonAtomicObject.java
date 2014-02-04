package ralph;

import RalphExceptions.BackoutException;
import RalphDataWrappers.DataWrapperFactory;
import RalphDataWrappers.DataWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;


/**
 * 
 * @author bmistree
 *
 * @param <T> --- The java type of the internal data
 * @param <D> --- The type that gets returned from dewaldoify.  Not
 * entirely true If this is an internal container, then contains what
 * each value in map/list would dewaldoify to.
 */
public abstract class NonAtomicObject<T,D> extends RalphObject<T,D> {
	
    public String uuid = Util.generate_uuid();
    private DataWrapperFactory<T,D> data_wrapper_constructor;
    public DataWrapper<T,D> val = null;
	
    public NonAtomicObject(){}
	
    public void init(
        ValueTypeDataWrapperFactory<T,D> vtdwc,
        T init_val)
    {
        data_wrapper_constructor = vtdwc;
        val = data_wrapper_constructor.construct(init_val,false);
    }
	
	
    @Override
    public void update_event_priority(String uuid, String new_priority)
    {}

    @Override
    public T get_val(ActiveEvent active_event)
    {
        return val.val;
    }

    /**
     * Called as an active event runs code.
     * @param active_event
     * @param new_val
     */
    @Override
    public void set_val(ActiveEvent active_event, T new_val)
    {
        val.write(new_val);
    }

    @Override
    public boolean return_internal_val_from_container()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     *
     @returns {bool} --- True if the object has been written to
     since we sent the last message.  False otherwise.  (Including
     if event has been preempted.)
     * @param active_event
     * @return
     */
    @Override
    public boolean get_and_reset_has_been_written_since_last_msg(
        ActiveEvent active_event) 
    {
        // check if active event even has ability to write to variable
        boolean has_been_written =
            val.get_and_reset_has_been_written_since_last_msg();
        return has_been_written;
    }

    @Override
    public void complete_commit(ActiveEvent active_event)
    {
        // nothing to do when completing commit: do nothing
    }

    @Override
    public void backout(ActiveEvent active_event)
    {
        /*
         Do not actually need to remove changes to this variable: no
         other transaction will ever see the changes we made + this
         transaction will just create a new single threaded variable.
        */
        return;
    }

    @Override
    public D de_waldoify(ActiveEvent active_event) throws BackoutException
    {
        return val.de_waldoify(active_event);
    }
}
