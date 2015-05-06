package ralph;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import RalphDurability.DurabilityContext;

import RalphCallResults.MessageCallResultObject;

import RalphServiceActions.ServiceAction;

import RalphExceptions.BackoutException;
import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

import ralph.ActiveEvent.FirstPhaseCommitResponseCode;
import ralph.MessageSender.LiveMessageSender;
import ralph.ExecutionContext.ExecutionContext;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;


public class NonAtomicActiveEvent extends ActiveEvent
{
    public ExecutionContextMap exec_ctx_map = null;

    /**
       Root AtomicActiveEvents can only be created by root
       NonAtomicActiveEvents.  That means that a root
       AtomicActiveEvent will never receive a promotion message
       directly from boosted manager.  Instead, it's parent
       NonAtomicActiveEvent will.  Therefore, to do priorities
       correctly, we forward on promotion messages from a
       NonAtomicActiveEvent to its child AtomicActiveEvent.  To do
       this properly, keep track of atomic_child.
     */
    private AtomicActiveEvent atomic_child = null;
    private final ReentrantLock _atomic_child_mutex = new ReentrantLock();
        
    public NonAtomicActiveEvent(
        EventParent _event_parent, ExecutionContextMap _exec_ctx_map,
        RalphGlobals _ralph_globals)
    {
        super(_event_parent,_ralph_globals);
        exec_ctx_map = _exec_ctx_map;
    }

    /**
       If we are logging for durability, when we first enter a call,
       we log the call, the endpoint it was made on, and the arguments
       to it.  The first and the third of these are contained in prsb,
       the second is contained in endpoint_uuid.
     */
    public void durability_entry_call(
        PartnerRequestSequenceBlock prsb,String endpoint_uuid)
    {
        exec_ctx.add_rpc_arg(prsb,endpoint_uuid);
    }
    
    /**
     *  @param {WaldoLockedObj} obj --- Whenever we try to perform a
     read or a write on a Waldo object, if this event has not
     previously performed a read or write on that object, then we
     check to ensure that this event hasn't already begun to
     backout.

     @returns {bool} --- Returns True if have not already backed
     out.  Returns False otherwise.
    */
    @Override
    public boolean add_touched_obj(AtomicObject obj)
    {
        return true;
    }
    @Override
    public boolean remove_touched_obj(AtomicObject obj)
    {
        return true;
    }
    

    private void atomic_child_lock()
    {
        _atomic_child_mutex.lock();
    }
    private void atomic_child_unlock()
    {
        _atomic_child_mutex.unlock();
    }
    
    /**
     * @param new_priority
     */
    public void promote_boosted(String new_priority)
    {
        // see promote_boosted in AtomicActiveEvent.  Do not want
        // cycles in promotion messages.
        if (! event_parent.set_new_priority(new_priority))
            return;

        // Copying child atomic to avoid any deadlock when promote
        // boosted.  Doesn't matter if get a new AtomicActiveEvent
        // after copied old one, because new one will have updated
        // priority (set new priority in line above).
        AtomicActiveEvent atomic_child_copy = null;
        atomic_child_lock();
        atomic_child_copy = atomic_child;
        atomic_child_unlock();
        if (atomic_child_copy != null)
            atomic_child_copy.promote_boosted(new_priority);
    }

    public void set_atomic_child(AtomicActiveEvent atom_evt)
    {
        atomic_child_lock();
        atomic_child = atom_evt;
        atomic_child_unlock();
    }

    
    /**
       @returns {bool} --- False.  Will never backout a non-atomic
       event.  A non-atomic event only performs single atomic reads
       and writes.  In this case, just let the entire operation
       complete, instead of rescheduling it.
    */
    public boolean can_backout_and_hold_lock()
    {
        // will never backout a non-atomic event that 
        return false;
    }

    @Override
    public FirstPhaseCommitResponseCode local_root_begin_first_phase_commit()
    {
        // a non-atomic can only be started from root.
        ((RootEventParent)event_parent).non_atomic_completed();
        // remove non-atomic's uuid from event map.
        exec_ctx_map.remove_exec_ctx(uuid);

        long root_timestamp =
            event_parent.ralph_globals.clock.get_and_increment_int_timestamp();
        String root_host_uuid = event_parent.ralph_globals.host_uuid;

        String root_application_uuid = event_parent.local_endpoint._uuid;
        String root_event_name = event_parent.event_entry_point_name;
        
        this.commit_metadata = new CommitMetadata(
            root_timestamp,
            root_host_uuid,root_event_name,root_application_uuid);

        // log commit_metadata in case need to replay
        if (VersioningInfo.instance.version_saver != null)
        {
            VersioningInfo.instance.version_saver.save_commit_metadata(
                commit_metadata);
        }
        
        return FirstPhaseCommitResponseCode.SUCCEEDED;
    }


    @Override
    public void handle_backout_exception(BackoutException be)
        throws BackoutException
    {
        Util.logger_assert(
            "\nShould never get a backout exception in NonAtomic event.\n");
    }
    
    /**
     * If can enter Should send a message back to parent that 
        
     * @param from_partner
     */
    @Override
    public FirstPhaseCommitResponseCode non_local_root_begin_first_phase_commit(
        Long root_first_phase_commit_timestamp,
        String root_first_phase_commit_host_uuid,
        String application_uuid, String event_name)
    {
        this.commit_metadata = new CommitMetadata(
            root_first_phase_commit_timestamp,
            root_first_phase_commit_host_uuid,
            event_name,application_uuid);
        
        return local_root_begin_first_phase_commit();
    }

    public void second_phase_commit()
    {
        // nothing to do because non-atomic does not need to commit.
    }

    /**
     * Called from a separate thread in waldoServiceActions.  Runs
     through all touched objects and backs out of them.
    */
    public void _backout_touched_objs()
    {}
    
    /**
     *  @param error {Exception}
     */
    public void put_exception(Exception error)
    {
        if (RalphExceptions.BackoutException.class.isInstance(error))
        {
            // do nothing: non-atomics cannot be backed out.
        }
        else
            event_parent.put_exception(error,message_listening_mvars_map);
    }

    /**
       @param {uuid or None} backout_requester_host_uuid --- If
       None, means that the call to backout originated on local
       endpoint.  Otherwise, means that call to backout was made by
       either endpoint's partner, an endpoint that we called an
       endpoint method on, or an endpoint that called an endpoint
       method on us.
       */
    @Override
    public void blocking_backout(
        String backout_requester_host_uuid)
    {
        Util.logger_assert(
            "Should not call backout on non-atomic active event.");
    }
    @Override
    public void non_blocking_backout(
        String backout_requester_host_uuid)
    {
        // can get called by speculative obj, eg., if backing out a
        // derived from.
    }
        

    /**
       Either this or obj_request_no_backout_and_release_lock
       are called after can_backout_and_hold_lock returns
       True.  

       Called by an AtomicObject to preempt this event.

       * @param obj_requesting
       */
    public void obj_request_backout_and_release_lock(
        AtomicObject obj_requesting)
    {
        Util.logger_assert(
            "Non-atomic should never be asked to backout and release lock.");
    }

    /**
       Either this or obj_request_backout_and_release_lock
       are called after can_backout_and_hold_lock returns
       True.  

       Called by an AtomicObject.  AtomicObject will not
       preempt this event.
        
       Do not have backout event.  Just release lock.
    */
    public void obj_request_no_backout_and_release_lock()
    {
        Util.logger_assert(
            "Non-atomic should never be asked to backout and release lock.");
    }

    @Override
    public boolean note_issue_rpc(
        String remote_host_uuid, String other_side_reply_with_uuid,
        MVar<MessageCallResultObject> result_mvar)
    {
        // code is listening on result_mvar.  when we
        // receive a response, put it inside of the mvar.
        // put result queue in map so that can demultiplex messages
        // from partner to determine which result queue is finished
        if (result_mvar != null)
        {
            //# may get None for result queue for the last message
            //# sequence block requested.  It does not need to await
            //# a response.
            message_listening_mvars_map.put(
                other_side_reply_with_uuid, result_mvar);
        }
        return true;
    }
    
    public String get_priority()
    {
        return event_parent.get_priority();
    }
    
	
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
    public void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_host_uuid,
        Set<String> children_event_host_uuids)
    {
        Util.logger_assert(
            "Non-atomic should not receive first phase commit.");
    }

    /**
       @param {bool} request_from_partner --- @see
       waldoEndpointServiceThread
       complete_commit_and_forward_complete_msg.
    */
    public void complete_commit_and_forward_complete_msg(
        boolean request_from_partner)
    {
        Util.logger_assert(
            "Non-atomic should not receive complit_commit_and_forward");
    }

    public boolean immediate_complete()
    {
        return true;
    }
    

    public void forward_backout_request_and_backout_self()
    {
        Util.logger_assert(
            "Non-atomic should not receive forward_backout_request.");
    }
	
    public void forward_backout_request_and_backout_self(
        boolean skip_partner)
    {
        Util.logger_assert(
            "Non-atomic should not receive forward_backout_request.");
    }
	
    public void forward_backout_request_and_backout_self(
        boolean skip_partner, boolean already_backed_out)
    {
        Util.logger_assert(
            "Non-atomic should not receive forward_backout_request.");
    }

    
    @Override
    protected void internal_recv_partner_sequence_call_msg(
        String endpt_recvd_msg_on_uuid, PartnerRequestSequenceBlock msg,
        String remote_host_uuid)
        throws ApplicationException, BackoutException, NetworkException
    {
        //# can be None... if it is means that the other side wants us
        //# to decide what to do next (eg, the other side performed its
        //# last message sequence action)
        String name_of_block_to_exec_next = null;
        if (msg.hasNameOfBlockRequesting())
            name_of_block_to_exec_next = msg.getNameOfBlockRequesting();

        ExecutingEvent exec_event = null;

        if (! msg.hasReplyToUuid())
        {
            exec_event = handle_first_sequence_msg_from_partner(
                endpt_recvd_msg_on_uuid, msg, name_of_block_to_exec_next,
                remote_host_uuid);
        }
        else
        {
            handle_non_first_sequence_msg_from_partner(
                endpt_recvd_msg_on_uuid, msg, name_of_block_to_exec_next,
                remote_host_uuid);
        }
        
        if (exec_event != null)
        {
            //### ACTUALLY START EXECUTION CONTEXT THREAD
            exec_event.run();
        }
    }

    public void receive_unsuccessful_first_phase_commit_msg(
        String event_uuid,
        String msg_originator_host_uuid) 
    {
        Util.logger_assert(
            "Non-atomic active event should never receive " +
            "an unsuccessful first phase message.");
    }

    @Override
    public boolean rpc_should_be_atomic()
    {
        return false;
    }
}
