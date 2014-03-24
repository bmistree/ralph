
package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import RalphCallResults.MessageCallResultObject;
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
    protected ThreadPool thread_pool = null;
    
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
     *  @param {WaldoLockedObj} obj --- Whenever we try to perform a
     read or a write on a Waldo object, if this event has not
     previously performed a read or write on that object, then we
     check to ensure that this event hasn't already begun to
     backout.

     @returns {bool} --- Returns True if have not already backed
     out.  Returns False otherwise.
    */
    public abstract boolean add_touched_obj(AtomicObject obj);
    public abstract boolean remove_touched_obj(AtomicObject obj);

    
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
       SUCCEEDED, should read event parent's completion queue to see
       if commit was successful or unsuccessful.  If returns FAILED,
       means that the commit was already backed out and that it
       failed.  If returns SKIP, likely trying to commit a nested
       transaction, should not read from event parent queue because
       transaction is incomplete and will read it later.

       Note: just because the call to begin_first_phase_commit returns
       SUCCEEDED, does not mean that the actual commit succeeded, it
       just means that we were able to process the request to begin
       the first phase of the commit.  Check results of
       event_complete_queue in root object for whether the commit
       actually succeded.
     */
    public abstract FirstPhaseCommitResponseCode begin_first_phase_commit();
    public static enum FirstPhaseCommitResponseCode
    {
        FAILED, SUCCEEDED, SKIP
    }
    
    /**
     * If can enter Should send a message back to parent that 
        
     * @param from_partner
     */
    public abstract FirstPhaseCommitResponseCode begin_first_phase_commit(boolean from_partner);
    public abstract void second_phase_commit();

    /**
     * Called from a separate thread in waldoServiceActions.  Runs
     * through all touched objects and backs out of them.
    */
    public abstract void _backout_touched_objs();
    public abstract void put_exception(Exception error);
    public abstract void stop(boolean skip_partner);
    public abstract void backout(
        String backout_requester_host_uuid, boolean stop_request);
    
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
       When we are executing an atomically block, we may need to throw
       a backout exception.  If we are in the root atomically block,
       this exception should cause the entire event to backout and
       retry.  If we are not at the root atomically block, this should
       re-raise the exception until we get to the root atomically
       block, which should not re-raise the exception and continue
       with normal program flow.
     */
    public abstract void handle_backout_exception(BackoutException be)
        throws BackoutException;

    
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
        Endpoint endpt,ExecutingEventContext ctx, String func_name,
        ArrayBlockingQueue<MessageCallResultObject>threadsafe_unblock_queue,
        boolean first_msg,ArrayList<RPCArgObject>args);



    public abstract String get_priority();

    /**
       Using two phase commit.  All committers must report to root
       that they were successful in first phase of commit before root
       can tell everyone to complete the commit (second phase).

       In this case, received a message from endpoint that this
       active event is subscribed to that endpoint with uuid
       msg_originator_host_uuid was able to commit.  If we have
       not been told to backout, then forward this message on to the
       root.  (Otherwise, no point in sending it further and doing
       wasted work: can just drop it.)

       * @param event_uuid
       * @param msg_originator_host_uuid
       * @param children_event_host_uuids
       */
    public abstract void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_host_uuid,
        ArrayList<String> children_event_host_uuids);


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
        Endpoint endpt_recvd_on,
        PartnerRequestSequenceBlock msg)
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException;


    public abstract void receive_unsuccessful_first_phase_commit_msg(
        String event_uuid,
        String msg_originator_host_uuid);
}
