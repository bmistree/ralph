package ralph;

import java.util.concurrent.locks.ReentrantLock;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;

import RalphExceptions.BackoutException;
import RalphVersions.ObjectHistory;
import RalphVersions.IReconstructionContext;

import ralph.ExecutionContext.ExecutionContext;


/**
 * @param <T> --- The internal, Java type of the data
 * @param <DeltaType> --- The type that gets passed to the version
 * helper for logging.
 */
public abstract class RalphObject<T,DeltaType> implements IReference
{
    private AtomicFactory atomic_factory = null;
    private NonAtomicFactory non_atomic_factory = null;
	
    protected String host_uuid = null;

    /**
       Returns the current contents of the object.

       @param active_event --- Can be null

       @param add_contents --- Metadata that an object may pass in for
       serialization.  Can be null.

       @param serialization_context --- So that references can add
       more internal objects to be serialized.
     */
    public abstract ObjectContents serialize_contents(
        ActiveEvent active_event, Object add_contents,
        SerializationContext serialization_context)
        throws BackoutException;
    
    /**
       May be null.  Gets set in initializer.  Used to save deltas of
       a version.
     */
    protected VersionHelper<DeltaType> version_helper = null;
    
    /**
       After regenerating an object from its constructor, need to be
       able to replay its changes on top of it.
     */
    public abstract void replay (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until);

    /**
       Used to deserialize rpc variables.  Likely should be very
       similar to replay.  Or replay will call it.
     */
    public abstract void deserialize (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until,
        ActiveEvent active_event) throws BackoutException;

    
    /**
       Assumes no other writers.  Set directly on internal value.
     */
    public abstract void direct_set_val(T new_val);
    
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
        ActiveEvent active_event) throws BackoutException;

    public abstract void set_val(
        ActiveEvent active_event, T new_val) throws BackoutException;

    /**
     * @returns {bool} --- True if when call get_val_from_key on a
       container should call get_val on it.  False otherwise.
    */
    public abstract boolean return_internal_val_from_container();
    
    public abstract void complete_commit(ActiveEvent active_event);

    public abstract void backout(ActiveEvent active_event);
}