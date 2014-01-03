
package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import RalphCallResults.MessageCallResultObject;
import RalphCallResults.EndpointCallResultObject;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import RalphExceptions.StoppedException;
import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;


public abstract class ActiveEvent
{
    public String uuid = null;
    public EventParent event_parent = null;

    /**
       When we enter an atomic block from a non-atomic block, we
       create a new atomic active event and return it.  The new atomic
       active event should keep track of the parent that created it.
       Calling restore_from_atomic should return the parent that
       cloned the atomic event.
     */
    public abstract ActiveEvent clone_atomic() throws StoppedException;
    
    public abstract ActiveEvent restore_from_atomic();

    /**
       If an event fails and is going to be retried, we clone relevant
       internal state using this method into a new ActiveEvent.

       Note: only retrying for AtomicActiveEvents.
     */
    public abstract ActiveEvent create_new_event_for_retry(
        RootEventParent rep, ActiveEventMap act_event_map);
    
    /**
     *  @param {WaldoLockedObj} obj --- Whenever we try to perform a
     read or a write on a Waldo object, if this event has not
     previously performed a read or write on that object, then we
     check to ensure that this event hasn't already begun to
     backout.

     @returns {bool} --- Returns True if have not already backed
     out.  Returns False otherwise.
    */
    public abstract boolean add_touched_obj(AtomicObject obj);
    public abstract void promote_boosted(String new_priority);
    /**
       @returns {bool} --- True if not in the midst of two phase
       commit.  False otherwise.

       If it is not in the midst of two phase commit, then does not
       return the lock that it is holding.  The lock must be released
       in obj_request_backout_and_release_lock or
       obj_request_no_backout_and_release_lock.
    */
    public abstract boolean can_backout_and_hold_lock();

    /**
       For each atomic block, emitter creates an atomic event from
       existing event.  Then, at end of atomic block, call
       begin_first_phase_commit.  If begin_first_phase_commit returns
       true, should read event parent's completion queue to see if
       commit was successful or unsuccessful.  If returns false,
       likely trying to commit a nested transaction, should not read
       from event parent queue because transaction is incomplete and
       will read it later.
     */
    public abstract boolean begin_first_phase_commit();
    
    /**
     * If can enter Should send a message back to parent that 
        
     * @param from_partner
     */
    public abstract boolean begin_first_phase_commit(boolean from_partner);
    public abstract void second_phase_commit();

    public abstract void add_signal_call(SignalFunction signaler);
    /**
     * Called from a separate thread in waldoServiceActions.  Runs
     * through all touched objects and backs out of them.
    */
    public abstract void _backout_touched_objs();
    public abstract void put_exception(Exception error);
    public abstract void stop(boolean skip_partner);
    public abstract void backout(
        String backout_requester_endpoint_uuid, boolean stop_request);

    /**
       Either this or obj_request_no_backout_and_release_lock
       are called after can_backout_and_hold_lock returns
       True.  

       Called by an Atomicbject to preempt this event.

       * @param obj_requesting
       */
    public abstract void obj_request_backout_and_release_lock(
        AtomicObject obj_requesting);

    /**
       Either this or obj_request_backout_and_release_lock
       are called after can_backout_and_hold_lock returns
       True.  

       Called by an AtomicObject.  AtomicObject will not
       preempt this event.
        
       Do not have backout event.  Just release lock.
    */
    public abstract void obj_request_no_backout_and_release_lock();

    /**
       Immediately after getting or setting a value, should a locked
       object commit the active event's operation?  (True for
       non-atomics, false for atomics.)
     */
    public abstract boolean immediate_complete();

    
    /**
       @param {String or None} func_name --- When func_name is None,
       then sending to the other side the message that we finished
       performing the requested block.  In this case, we do not need
       to add result_queue to waiting queues.

       @param {bool} first_msg --- True if this is the first message
       in a sequence that we're sending.  Necessary so that we can
       tell whether or not to force sending sequence local data.

       @param {Queue or null} threadsafe_unblock_queue --- None if
       this was the last message sent in a sequence and we're not
       waiting on a reply.

       @param {ArrayList} args --- The positional arguments inserted
       into the call as an rpc.  Includes whether the argument is a
       reference or not (ie, we should update the variable's value on
       the caller).
       
       The local endpoint is requesting its partner to call some
       method on itself.
    */
    public abstract boolean issue_partner_sequence_block_call(
        ExecutingEventContext ctx, String func_name,
        ArrayBlockingQueue<MessageCallResultObject>threadsafe_unblock_queue,
        boolean first_msg,ArrayList<RPCArgObject>args);


    /**
       @param {Endpoint object} endpoint_calling --- The endpoint to
       execute the endpoint object call on.
	
       @param {String} func_name --- The name of the function to
       execute on the endpoint object.
	
       @param {Queue.Queue} result_queue --- Threadsafe queue that
       stores the result 
	    
       @returns {bool} --- True if the endpoint object call could go
       through (ie, we were not already requested to backout the
       event).  False otherwise.
	
       Adds endpoint as an Endpoint object that we are subscribed to.
       (We need to keep track of all the endpoint objects that we are
       subscribed to [ie, have requested endpoint object calls on] so
       that we know who to forward our commit requests and backout
       requests to.)

    */
    public abstract boolean issue_endpoint_object_call(
        Endpoint endpoint_calling,String func_name,
        ArrayBlockingQueue<EndpointCallResultObject>result_queue,
        Object...args);

    public abstract String get_priority();

    /**
       Using two phase commit.  All committers must report to root
       that they were successful in first phase of commit before root
       can tell everyone to complete the commit (second phase).

       In this case, received a message from endpoint that this
       active event is subscribed to that endpoint with uuid
       msg_originator_endpoint_uuid was able to commit.  If we have
       not been told to backout, then forward this message on to the
       root.  (Otherwise, no point in sending it further and doing
       wasted work: can just drop it.)

       * @param event_uuid
       * @param msg_originator_endpoint_uuid
       * @param children_event_endpoint_uuids
       */
    public abstract void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_endpoint_uuid,
        ArrayList<String> children_event_endpoint_uuids);


    /**
       @param {bool} request_from_partner --- @see
       waldoEndpointServiceThread
       complete_commit_and_forward_complete_msg.
    */
    public abstract void complete_commit_and_forward_complete_msg(
        boolean request_from_partner);
    public abstract void forward_backout_request_and_backout_self();
    public abstract void forward_backout_request_and_backout_self(
        boolean skip_partner);
    public abstract void forward_backout_request_and_backout_self(
        boolean skip_partner, boolean already_backed_out);

    /**
       @param {bool} skip_partner --- @see forward_commit_request

       @param {bool} already_backed_out --- Caller has already backed
       out the commit through commit manager, and is calling this
       function primarily to forward the backout message.  No need to
       do so again inside of function.

       @param {bool} stop_request --- True if this backout is a
       product of a stop request.  False otherwise.
        
       When this is called, we want to disable all further additions
       to self.subscribed_to and self.partner_contacted.  (Ie, after we
       have requested to backout, we should not execute any further
       endpoint object calls or request partner to do any additional
       work for this event.)

       * @param skip_partner
       * @param already_backed_out
       * @param stop_request
       */
    public abstract void forward_backout_request_and_backout_self(
        boolean skip_partner, boolean already_backed_out,
        boolean stop_request);
    
    public abstract void recv_partner_sequence_call_msg(
        PartnerRequestSequenceBlock msg)
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException;


    public abstract void receive_unsuccessful_first_phase_commit_msg(
        String event_uuid,
        String msg_originator_endpoint_uuid);
}
