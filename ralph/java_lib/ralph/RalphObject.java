package ralph;

import java.util.concurrent.locks.ReentrantLock;

import RalphExceptions.BackoutException;
import ralph_protobuffs.VariablesProto;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import RalphVersions.ObjectHistory;
import RalphVersions.IReconstructionContext;


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
       serialization.
     */
    public abstract ObjectContents serialize_contents(
        ActiveEvent active_event,Object add_contents)
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
    // public abstract void replay (
    //     IReconstructionContext reconstruction_context,
    //     ObjectHistory obj_history,Long to_play_until);
    public void replay (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until)
    {
        // Note: should be abstract, but implementing this method as
        // assert --- allows compiling and testing even though replay
        // isn't implemented for a number of objects.
        Util.logger_assert(
            "Replay not overridden in atomicvariable.");
    }

    
    /**
       Mostly used when deserializing one locked object (to_swap_with)
       into another.  this.
     */
    public abstract void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException;
    
    /**
       Take internal data and add it as an arg to the
       Varialbes.Any.Builder.
     */
    public abstract void serialize_as_rpc_arg(
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException;
    /**
       Object has already been constructed.  Deserialize contents of
       any into it.
     */
    public void deserialize_rpc(
        RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
    {
        // FIXME: should just declare this abstract.
        Util.logger_assert("FIXME: objects override deserialize_rpc.");
    }

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