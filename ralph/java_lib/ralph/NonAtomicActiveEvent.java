package ralph;

import java.util.List;
import java.util.ArrayList;
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

import ralph_protobuffs.PartnerErrorProto.PartnerError;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;



public class NonAtomicActiveEvent extends ActiveEvent
{
    public ActiveEventMap event_map = null;

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
        EventParent _event_parent, ActiveEventMap _event_map,
        RalphGlobals _ralph_globals)
    {
        super(_event_parent,null,_ralph_globals);
        event_map = _event_map;
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
        if (durability_context != null)
            durability_context.add_rpc_arg(prsb,endpoint_uuid);
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
    
    public ActiveEvent clone_atomic() 
    {
        atomic_child_lock();
        atomic_child = event_map.create_root_atomic_event(
            this,event_parent.local_endpoint,
            event_parent.event_entry_point_name,durability_context);
        atomic_child_unlock();
        return atomic_child;
    }
    public ActiveEvent restore_from_atomic()
    {
        Util.logger_assert(
            "Should never reach a case where restoring " +
            "an atomic from non-atomic");
        return null;
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
        event_map.remove_event(uuid);

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
    
	
    /**
       @param {String or None} func_name --- When func_name is None,
       then sending to the other side the message that we finished
       performing the requested block.  In this case, we do not need
       to add result_mvar to waiting mvars.

       @param {bool} first_msg --- True if this is the first message
       in a sequence that we're sending.  Necessary so that we can
       tell whether or not to force sending sequence local data.

       @param {MVar or null} result_mvar --- None if this was the last
       message sent in a sequence and we're not waiting on a reply.

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
    public boolean issue_partner_sequence_block_call(
        Endpoint endpoint, LiveMessageSender message_sender, String func_name,
        MVar<MessageCallResultObject>result_mvar,
        boolean first_msg,List<RalphObject>args,RalphObject result)
    {
        
        //# code is listening on threadsafe result_mvar.  when we
        //# receive a response, put it inside of the result mvar.
        //# put result mvar in map so that can demultiplex messages
        //# from partner to determine which result mvar is finished
        String reply_with_uuid = event_parent.ralph_globals.generate_uuid();
                
        if (result_mvar != null)
        {
            //# may get None for result mvar for the last message
            //# sequence block requested.  It does not need to await
            //# a response.
            message_listening_mvars_map.put(reply_with_uuid, result_mvar);
        }

        // changed to have rpc semantics: this means that if it's not
        // the first message, then it is a reply to another message.
        // if it is a first message, then should not be replying to
        // anything.
        String replying_to = null;
        if (! first_msg)
            replying_to = message_sender.get_to_reply_with();

        PartnerRequestSequenceBlock request_sequence_block = null;

        try
        {
            request_sequence_block =
                PartnerRequestSequenceBlockProducer.produce_request_block(
                    replying_to,func_name,args,result,this,false,
                    reply_with_uuid);
        }
        catch (BackoutException ex)
        {
            return false;
        }

        // request endpoint to send message to partner
        endpoint._send_partner_message_sequence_block_request(
            request_sequence_block);
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
        List<String> children_event_host_uuids)
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
        Endpoint endpt_recvd_msg_on, PartnerRequestSequenceBlock msg)
        throws ApplicationException, BackoutException, NetworkException
    {
        //# can be None... if it is means that the other side wants us
        //# to decide what to do next (eg, the other side performed its
        //# last message sequence action)
        String name_of_block_to_exec_next = null;
        if (msg.hasNameOfBlockRequesting())
            name_of_block_to_exec_next = msg.getNameOfBlockRequesting();

        //# update peered data based on data contents of message.
        //# (Note: still must update sequence local data from deltas
        //# below.)

        ExecutingEvent exec_event = null;

        if (! msg.hasReplyToUuid())
        {
            exec_event = handle_first_sequence_msg_from_partner(
                endpt_recvd_msg_on,msg,name_of_block_to_exec_next);
        }
        else
        {
            handle_non_first_sequence_msg_from_partner(
                endpt_recvd_msg_on,msg,name_of_block_to_exec_next);
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
}



    
