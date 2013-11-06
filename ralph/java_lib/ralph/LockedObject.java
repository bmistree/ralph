package ralph;

import RalphExceptions.BackoutException;
import ralph_protobuffs.VariablesProto.Variables;

/**
 * 
 * @author bmistree
 *
 * @param <T> --- The internal, Java type of the data
 * @param <D> --- The java type that gets returned when call dewaldoify
 */
public abstract class LockedObject<T,D>
{
    private MultiThreadedConstructor multi_threaded_constructor = null;
    private SingleThreadedConstructor single_threaded_constructor = null;
	
    protected String host_uuid = null;
	
    public LockedObject<T,D> copy(
        LockedActiveEvent active_event, boolean peered,
        boolean multi_threaded) throws BackoutException
    {
        if (multi_threaded)
        {
            return multi_threaded_constructor.construct(
                host_uuid,peered,get_val(active_event));
        }
		
        return single_threaded_constructor.construct(
            host_uuid,peered,get_val(active_event));
    }

    /**
       Take internal data and add it as an arg to the
       Varialbes.Any.Builder.
     */
    public abstract void serialize_as_rpc_arg(
        LockedActiveEvent active_event,Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException;

	
    public abstract void write_if_different(
        LockedActiveEvent active_event, T new_val) throws BackoutException;
	
    /**
     * 
     Called when an event with uuid "uuid" is promoted to boosted
     with priority "priority"
     * @param uuid
     * @param new_priority
     */
    public abstract void update_event_priority(
        String uuid,String new_priority); 

    public abstract T get_val(
        LockedActiveEvent active_event) throws BackoutException;

    public abstract void set_val(
        LockedActiveEvent active_event, T new_val) throws BackoutException;

    /**
     * @returns {bool} --- True if when call get_val_from_key on a
       container should call get_val on it.  False otherwise.
    */
    public abstract boolean return_internal_val_from_container();

    /**
     * @returns {bool} --- True if the object has been written to
     since we sent the last message.  False otherwise.  (Including
     if event has been preempted.)
    */
    public abstract boolean get_and_reset_has_been_written_since_last_msg(
        LockedActiveEvent active_event);
    
    public abstract void complete_commit(LockedActiveEvent active_event);

    public abstract boolean is_peered();

    public abstract void backout(LockedActiveEvent active_event);

    public abstract D de_waldoify(
        LockedActiveEvent active_event) throws BackoutException;
}