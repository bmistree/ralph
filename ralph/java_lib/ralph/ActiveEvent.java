package ralph;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;

import RalphCallResults.MessageCallResultObject;

import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityContext;
import RalphDurability.DurabilityReplayContext;
import RalphDurability.IDurabilityReplayContext;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

import ralph.MessageSender.LiveMessageSender;
import ralph.MessageSender.DurabilityReplayMessageSender;
import ralph.MessageSender.IMessageSender;
import ralph.ExecutionContext.ExecutionContext;

public abstract class ActiveEvent
{
    public final String uuid;
    public final EventParent event_parent;

    /**
      When an active event sends a message to the partner endpoint, it
      blocks until the response.  The way it blocks is by calling get
      on an empty mvar.  There are two ways that data get put into the
      mvar.  The first is if the partner endpoint completes its
      computation and replies.  In this case, _Endpoint updates the
      associated ActiveEvent and puts a
      waldoCallResults._MessageFinishedCallResult object into the
      mvar.  When the listening endpoint receives this result, it
      continues processing.  The other way is if the event is backed
      out while we're waiting.  In that case, the runtime puts a
      waldoCallResults._BackoutBeforeReceiveMessageResult object into
      the mvar.
     
      We can be listening to more than one open threadsafe message
      mvar.  If endpoint A waits on its partner, and, while waiting,
      its partner executes a series of endpoint calls so that another
      method on A is invoked, and that method calls its partner again,
      we could be waiting on 2 different message mvars, each held by
      the same active event.  To determine which mvar a message is a
      reply to, we read the message's reply_to field.  If the reply_to
      field matches one of the indices in the map below, we know the
      matching waiting mvar.  If it does not match and is None, that
      means that it is the first message in a message sequence.  If it
      does not match and is not None, there must be some error.  (@see
      waldoExecutingEvent._ExecutingEventContext.to_reply_with_uuid.)

      For AtomicActiveEvents, must synchronize access to this queue
      map.
     */
    protected final Map<String,
    	MVar<MessageCallResultObject>> message_listening_mvars_map = 
    	new HashMap<String, MVar<MessageCallResultObject>>();

    /**
       Starts as null, but eventually gets set when create wrapping
       ExecutionContext.
     */
    public ExecutionContext exec_ctx = null;
    
    /**
       FIXME.
       
       Note: this really should be final.  But deserialization event
       needs to set it because it may not have RalphGlobal at correct
       time.  This is because do not have singleton for ralph globals
       to allow simpler testing (instantiating multiple partners with
       different ralph globals).
     */
    protected RalphGlobals ralph_globals;
    
    /**
       Want to start adding version control into RalphObjects.  Keep
       track of timestamps during commit to ensure that can establish
       order that changes are made to objects so that we get a proper
       object history.  These fields should be populated in
       begin_first_phase_commit.
     */
    public CommitMetadata commit_metadata = null;
    
    public ActiveEvent(
        EventParent _event_parent, RalphGlobals _ralph_globals)
    {
        this(
            _event_parent.get_uuid(),_event_parent,_ralph_globals);
    }

    public ActiveEvent(
        String _uuid, EventParent _event_parent,  RalphGlobals _ralph_globals)
    {
        uuid = _uuid;
        event_parent = _event_parent;
        ralph_globals = _ralph_globals;
    }

    public final void init_execution_context(
        ExecutionContext exec_ctx)
    {
        this.exec_ctx = exec_ctx;
    }
    
    /**
       FIXME: See note above ralph_globals.
     */
    public void set_ralph_globals(RalphGlobals _ralph_globals)
    {
        ralph_globals = _ralph_globals;
    }
    public RalphGlobals get_ralph_globals()
    {
        return ralph_globals;
    }
    
    /**
       NonatomicActive events that immediately commit their changes to
       a tvar-d object must log their version change.  To do so, they
       must have a non-null commit_metadata object.  This method
       rebuilds commit_metadata object for commit to tvar.
     */
    public void update_commit_metadata()
    {
        long local_timestamp =
            ralph_globals.clock.get_and_increment_int_timestamp();
        String root_host_uuid = ralph_globals.host_uuid;
        String root_application_uuid = event_parent.local_endpoint._uuid;
        String root_event_name = event_parent.event_entry_point_name;

        commit_metadata =
            new CommitMetadata(
                local_timestamp,root_application_uuid,root_event_name,uuid);

        // log commit_metadata in case need to replay
        if (VersioningInfo.instance.version_saver != null)
        {
            VersioningInfo.instance.version_saver.save_commit_metadata(
                commit_metadata);
        }
    }

    public abstract boolean rpc_should_be_atomic();

    public void only_remove_touched_obj(AtomicObject obj)
    {}
    
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
       SUCCEEDED, should read event parent's completion mvar to see
       if commit was successful or unsuccessful.  If returns FAILED,
       means that the commit was already backed out and that it
       failed.

       Importantly, nested transactions will receive calls to
       local_root_begin_first_phase_commit whether or not the real
       root of all events is local or not.  Ie., if we generate an
       atomic event remotely and then issue an rpc to another endpoint
       that has a nested atomically block, when that endpoint exits
       its atomically block, that endpoint will call a
       local_root_begin_first_phase_commit, even though the true root
       of the entire event is the first endpoint.  

       Note: just because the call to begin_first_phase_commit returns
       SUCCEEDED, does not mean that the actual commit succeeded, it
       just means that we were able to process the request to begin
       the first phase of the commit.  Check results of
       event_complete_mvar in root object for whether the commit
       actually succeded.
     */
    public abstract FirstPhaseCommitResponseCode local_root_begin_first_phase_commit();
    /**
     * If another node initiates first phase commit, it should pass in
     * the timestamp of the root event and the root host uuid.  The
     * AtomicActiveEvent keeps track of these timestamps and ids so
     * that later modules can add object versioning for atomic
     * variables.
     */
    public abstract FirstPhaseCommitResponseCode non_local_root_begin_first_phase_commit(
        Long root_first_phase_commit_timestamp,
        String root_first_phase_commit_host_uuid,
        String application_uuid, String event_name);

    
    public static enum FirstPhaseCommitResponseCode
    {
        FAILED, SUCCEEDED
    }
    
    
    public abstract void second_phase_commit();

    /**
     * Called from a separate thread in waldoServiceActions.  Runs
     * through all touched objects and backs out of them.
    */
    public abstract void _backout_touched_objs();
    public abstract void put_exception(Exception error);
    public abstract void blocking_backout(
        String backout_requester_host_uuid);
    public abstract void non_blocking_backout(
        String backout_requester_host_uuid);
    
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
       to add result_mvar to waiting mvars.

       @param {bool} first_msg --- True if this is the first message
       in a sequence that we're sending.  Necessary so that we can
       tell whether or not to force sending sequence local data.

       @param {MVar or null} result_mvar --- None if
       this was the last message sent in a sequence and we're not
       waiting on a reply.

       @param {List or null} args --- The positional arguments
       inserted into the call as an rpc.  Includes whether the
       argument is a reference or not (ie, we should update the
       variable's value on the caller).  Note that can be null if we
       have no args to pass back (or if is a sequence completed call).

       @param {RalphObject} result --- If this is a reply to an rpc
       and the called method had a return value, then we return it in
       result.
       
       The local endpoint is requesting its partner to call some
       method on itself.
    */
    public abstract boolean issue_partner_sequence_block_call(
        Endpoint endpoint, LiveMessageSender msg_sender, String func_name,
        MVar<MessageCallResultObject>result_mvar,
        boolean first_msg,List<RalphObject>args,RalphObject result);


    /**
       @param {Endpoint} endpt
       
       @param {String} other_side_reply_with_uuid --- If the other
       side responds to this rpc, it will contain this uuid.
              
       @param {MVar or null} result_mvar --- null if this was the last
       message sent in a sequence and we're not waiting on a reply.

       @returns true if still running and can send rpc
     */
    public abstract boolean note_issue_rpc(
        Endpoint endpt, String other_side_reply_with_uuid,
        MVar<MessageCallResultObject> result_mvar);



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
        List<String> children_event_host_uuids);


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

        /**
       @param {bool} skip_partner --- @see forward_commit_request

       @param {bool} already_backed_out --- Caller has already backed
       out the commit through commit manager, and is calling this
       function primarily to forward the backout message.  No need to
       do so again inside of function.

       When this is called, we want to disable all further additions
       to self.subscribed_to and self.partner_contacted.  (Ie, after we
       have requested to backout, we should not execute any further
       endpoint object calls or request partner to do any additional
       work for this event.)

       * @param skip_partner
       * @param already_backed_out
       */
    public abstract void forward_backout_request_and_backout_self(
        boolean skip_partner, boolean already_backed_out);

    
    public void recv_partner_sequence_call_msg(
        Endpoint endpt_recvd_on,
        PartnerRequestSequenceBlock msg)
        throws ApplicationException, BackoutException, NetworkException
    {
        exec_ctx.add_rpc_arg(msg,endpt_recvd_on.uuid());
        internal_recv_partner_sequence_call_msg(endpt_recvd_on,msg);
    }

    public void replay_rpc(
        Endpoint endpt_recvd_on, PartnerRequestSequenceBlock msg,
        ExecutionContext exec_ctx)
        throws ApplicationException, BackoutException, NetworkException
    {
        //// DEBUG
        if (! msg.hasNameOfBlockRequesting())
        {
            Util.logger_assert(
                "When replaying RPC, must know which block to exec.");
        }
        //// END DEBUG
        String name_of_block_to_exec_next = msg.getNameOfBlockRequesting();

        ExecutingEvent exec_event = rpc_request_to_exec_evt(
            endpt_recvd_on, msg, name_of_block_to_exec_next, exec_ctx);
        exec_event.run();

        local_root_begin_first_phase_commit();
        ((RootEventParent)event_parent).event_complete_mvar.blocking_take();
    }
    
    protected abstract void internal_recv_partner_sequence_call_msg(
        Endpoint endpt_recvd_on, PartnerRequestSequenceBlock msg)
        throws ApplicationException, BackoutException, NetworkException;
    
    public abstract void receive_unsuccessful_first_phase_commit_msg(
        String event_uuid,  String msg_originator_host_uuid);

    protected ExecutingEvent rpc_request_to_exec_evt(
        Endpoint endpt_recvd_msg_on,PartnerRequestSequenceBlock msg,
        String name_of_block_to_exec_next, ExecutionContext exec_ctx)
    {
        //#### DEBUG
        if( name_of_block_to_exec_next == null)
        {
            Util.logger_assert(
                "Error in _NonAtomicActiveEvent.  Should not receive the " +
                "beginning of a sequence message without some " +
                "instruction for what to do next.");
        }
        //#### END DEBUG
        
        // grab all arguments from message
        List <RalphObject> args = null;
        if (msg.hasArguments())
        {
            args =
                RPCDeserializationHelper.deserialize_arguments_list(
                    event_parent.ralph_globals,msg.getArguments(),exec_ctx);
        }
        

        // convert array list of args to optional array of arg objects.
        Object [] rpc_call_arg_array = new Object[args.size()];
        for (int i = 0; i < args.size(); ++i)
            rpc_call_arg_array[i] = args.get(i);
        
        boolean takes_args = args.size() != 0;

        
        ExecutingEvent to_return = new ExecutingEvent (
            endpt_recvd_msg_on,
            name_of_block_to_exec_next, exec_ctx,
            // whether has arguments
            takes_args,
            // what those arguments are.
            rpc_call_arg_array);
        
        return to_return;
    }


    /**
       @param {PartnerMessageRequestSequenceBlock.proto} msg ---

       @param {string} name_of_block_to_exec_next --- the name of the
       sequence block to execute next.

       @returns {Executing event}
    
       means that the other side has generated a first message create
       a new context to execute that message and do so in a new
       thread.
    */
    protected ExecutingEvent handle_first_sequence_msg_from_partner(
        Endpoint endpt_recvd_msg_on,PartnerRequestSequenceBlock msg,
        String name_of_block_to_exec_next)
    {
        LiveMessageSender msg_sender =
            (LiveMessageSender) exec_ctx.message_sender();
        
        // know how to reply to this message.
        msg_sender.set_to_reply_with(msg.getReplyWithUuid().getData());
        
        return rpc_request_to_exec_evt(
            endpt_recvd_msg_on, msg,name_of_block_to_exec_next,
            exec_ctx);
    }

    /**
     * ASSUMES ALREADY WITHIN LOCK on AtomicActiveEvent.
     
     @param {PartnerMessageRequestSequenceBlock.proto} msg ---

     @param {string or None} name_of_block_to_exec_next --- the
     name of the sequence block to execute next. None if nothing to
     execute next (ie, last sequence message).
     * 
     */
    protected void handle_non_first_sequence_msg_from_partner(
        Endpoint endpt_recvd_on, PartnerRequestSequenceBlock msg,
        String name_of_block_to_exec_next)
    {
        String reply_to_uuid = msg.getReplyToUuid().getData();

        //#### DEBUG
        if (! message_listening_mvars_map.containsKey(reply_to_uuid))
        {
            Util.logger_assert(
                "Error: partner response message responding to " +
                "unknown _ActiveEvent message in AtomicActiveEvent.");
        }
        //#### END DEBUG
        
        String reply_with_uuid = msg.getReplyWithUuid().getData();
        
        Arguments returned_objs = null;
        if (msg.hasReturnObjs())
            returned_objs = msg.getReturnObjs();
        
        //# unblock waiting listening mvar.
        message_listening_mvars_map.get(reply_to_uuid).put(
            RalphCallResults.MessageCallResultObject.completed(
                reply_with_uuid,name_of_block_to_exec_next,
                // result of rpc
                returned_objs));
        
        //# no need holding onto mvar waiting on a message response.
        message_listening_mvars_map.remove(reply_to_uuid);
    }


    /**
       ASSUMES ALREADY WITHIN LOCK on AtomicActiveEvent.
        
       To provide blocking, whenever issue an endpoint call or partner
       call, thread of execution blocks, waiting on a read into a
       mvar.  When we rollback, we must put a sentinel into the mvar
       indicating that the event has been rolled back and to not
       proceed further.

       */
    protected void rollback_unblock_waiting_mvars()
    {
        for (MVar<MessageCallResultObject> msg_mvar_to_unblock :
                 message_listening_mvars_map.values())
        {
            MessageCallResultObject mvar_feeder = null;
            mvar_feeder =
                MessageCallResultObject.backout_before_receive_message();

            msg_mvar_to_unblock.put(mvar_feeder);
        }
    }
}
