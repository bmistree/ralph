package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import RalphServiceActions.ServiceAction;

import ralph_protobuffs.PartnerErrorProto.PartnerError;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.VariablesProto;
import RalphCallResults.MessageCallResultObject;
import java.util.concurrent.locks.ReentrantLock;

import RalphCallResults.EndpointCallResultObject;
import RalphExceptions.BackoutException;
import RalphExceptions.StoppedException;
import RalphCallResults.StopAlreadyCalledEndpointCallResult;
import RalphCallResults.BackoutBeforeEndpointCallResult;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;
import ralph.ActiveEvent.FirstPhaseCommitResponseCode;


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
    private ReentrantLock _atomic_child_mutex = new ReentrantLock();
    
    HashMap<String,
    	ArrayBlockingQueue<MessageCallResultObject>> message_listening_queues_map = 
    	new HashMap<String, ArrayBlockingQueue<MessageCallResultObject>>();
    
    public NonAtomicActiveEvent(
        EventParent _event_parent, ActiveEventMap _event_map)
    {
        event_parent = _event_parent;
        event_map = _event_map;

        uuid = event_parent.get_uuid();
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
    public boolean add_touched_obj(AtomicObject obj)
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
    
    public ActiveEvent clone_atomic() throws StoppedException
    {
        atomic_child_lock();
        atomic_child = event_map.create_root_atomic_event(this);
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
    public FirstPhaseCommitResponseCode begin_first_phase_commit()
    {
        // a non-atomic can only be started from root.
        ((RootEventParent)event_parent).non_atomic_completed();
        // remove non-atomic's uuid from event map.
        event_map.remove_event(uuid);
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
    public FirstPhaseCommitResponseCode begin_first_phase_commit(boolean from_partner)
    {
        return begin_first_phase_commit();
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
            event_parent.put_exception(error,message_listening_queues_map);
    }

    public void stop(boolean skip_partner)
    {
        Util.logger_assert(
            "\nError: must fill in stop method on event.\n");
    }
    
    /**
       ASSUMES ALREADY WITHIN _LOCK
        
       To provide blocking, whenever issue an endpoint call or
       partner call, thread of execution blocks, waiting on a read
       into a threadsafe queue.  When we rollback, we must put a
       sentinel into the threadsafe queue indicating that the event
       has been rolled back and to not proceed further.

       * @param stop_request
       */
    private void rollback_unblock_waiting_queues(boolean stop_request)
    {
        for (ArrayBlockingQueue<MessageCallResultObject> msg_queue_to_unblock :
                 message_listening_queues_map.values())
        {
            MessageCallResultObject queue_feeder = null;
            if (stop_request)
            {
                queue_feeder = MessageCallResultObject.stop_already_called();
            }
            else
            {
                queue_feeder =
                    MessageCallResultObject.backout_before_receive_message();
            }
            msg_queue_to_unblock.add(queue_feeder);
        }

        //# do not need to acquire locks on other_endpoints_contacted
        //# because the only place that it can be written to is when
        //# already holding _lock.  Therefore, we already have exclusive
        //# access to variable.

        Util.logger_warn(
            "Need to unblock waiting queues for subscribed to events.");
        // for (EventSubscribedTo subscribed_to_element :
        //          other_endpoints_contacted.values())
        // {
        //     for (ArrayBlockingQueue<EndpointCallResultObject> res_queue :
        //              subscribed_to_element.result_queues)
        //     {	
        //         EndpointCallResultObject queue_feeder;
        //         if (stop_request)
        //             queue_feeder = new StopAlreadyCalledEndpointCallResult();
        //         else
        //             queue_feeder = new BackoutBeforeEndpointCallResult();
        //         res_queue.add(queue_feeder);
        //     }
        // }
    }

    /**
       @param {uuid or None} backout_requester_endpoint_uuid --- If
       None, means that the call to backout originated on local
       endpoint.  Otherwise, means that call to backout was made by
       either endpoint's partner, an endpoint that we called an
       endpoint method on, or an endpoint that called an endpoint
       method on us.
       * @param stop_request
       */
    public void backout(
        String backout_requester_endpoint_uuid, boolean stop_request)
    {
        Util.logger_assert(
            "Should not call backout on non-atomic active event.");
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
    public boolean issue_partner_sequence_block_call(
        Endpoint endpt, ExecutingEventContext ctx, String func_name,
        ArrayBlockingQueue<MessageCallResultObject>threadsafe_unblock_queue,
        boolean first_msg,ArrayList<RPCArgObject>args)
    {
        
        //# code is listening on threadsafe result_queue.  when we
        //# receive a response, put it inside of the result queue.
        //# put result queue in map so that can demultiplex messages
        //# from partner to determine which result queue is finished
        String reply_with_uuid = Util.generate_uuid();
                
        if (threadsafe_unblock_queue != null)
        {
            //# may get None for result queue for the last message
            //# sequence block requested.  It does not need to await
            //# a response.
            message_listening_queues_map.put(
                reply_with_uuid, threadsafe_unblock_queue);
        }

        // construct variables for arg messages
        VariablesProto.Variables.Builder serialized_arguments = VariablesProto.Variables.newBuilder();
        for (RPCArgObject arg : args)
        {
            try
            {
                // arg may be null: for instance if sending a
                // sequence complete call to an endpoint where the
                // passed in argument was not passed in by
                // reference.
                VariablesProto.Variables.Any.Builder any_builder = VariablesProto.Variables.Any.newBuilder();

                if (arg == null)
                {
                    any_builder.setVarName("");
                    any_builder.setReference(false);
                }
                else
                {
                    arg.arg_to_pass.serialize_as_rpc_arg(
                        this,any_builder,arg.is_reference);
                }
                serialized_arguments.addVars(any_builder);
            }
            catch (BackoutException excep)
            {
                return false;
            }
        }
        
        // changed to have rpc semantics: this means that if it's not
        // the first message, then it is a reply to another message.
        // if it is a first message, then should not be replying to
        // anything.
        String replying_to = null;
        if (! first_msg)
            replying_to = ctx.get_to_reply_with();

        
        // request endpoint to send message to partner
        endpt._send_partner_message_sequence_block_request(
            func_name,uuid,get_priority(),reply_with_uuid,
            replying_to,this,serialized_arguments,
            first_msg,false);
        
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
       msg_originator_endpoint_uuid was able to commit.  If we have
       not been told to backout, then forward this message on to the
       root.  (Otherwise, no point in sending it further and doing
       wasted work: can just drop it.)

       * @param event_uuid
       * @param msg_originator_endpoint_uuid
       * @param children_event_endpoint_uuids
       */
    public void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_endpoint_uuid,
        ArrayList<String> children_event_endpoint_uuids)
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
    public void forward_backout_request_and_backout_self(
        boolean skip_partner, boolean already_backed_out, boolean stop_request)
    {
        Util.logger_assert(
            "Non-atomic should not receive forward_backout_request.");
    }


    /**
       ASSUMES ALREADY WITHIN LOCK

       @param {PartnerMessageRequestSequenceBlock.proto} msg ---

       @param {string} name_of_block_to_exec_next --- the name of the
       sequence block to execute next.

       @returns {Executing event}
    
       means that the other side has generated a first message create
       a new context to execute that message and do so in a new
       thread.
    */
    private ExecutingEvent handle_first_sequence_msg_from_partner(
        Endpoint endpt_recvd_msg_on,PartnerRequestSequenceBlock msg,
        String name_of_block_to_exec_next)
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
        ArrayList <RPCArgObject> args =
            ExecutingEventContext.deserialize_rpc_args_list(
                msg.getArguments(),event_parent.local_endpoint._host_uuid);
        
        // create new ExecutingEventContext that copies current stack
        // and keeps track of which arguments need to be returned as
        // references.
        ExecutingEventContext ctx =
            endpt_recvd_msg_on.create_context_for_recv_rpc(args);
        
        // know how to reply to this message.
        ctx.set_to_reply_with(msg.getReplyWithUuid().getData());

        // convert array list of args to optional array of arg objects.
        Object [] rpc_call_arg_array = new Object[args.size()];
        for (int i = 0; i < args.size(); ++i)
            rpc_call_arg_array[i] = args.get(i).arg_to_pass;
        
        boolean takes_args = args.size() != 0;

        ExecutingEvent to_return = new ExecutingEvent (
            endpt_recvd_msg_on,
            name_of_block_to_exec_next,this,ctx,
            // using null here means that we do not need to bother
            // with waiting for modified peered-s to update.
            null,
            // whether has arguments
            takes_args,
            // what those arguments are.
            rpc_call_arg_array);
        
        return to_return;
    }

    public void recv_partner_sequence_call_msg(
        Endpoint endpt_recvd_msg_on, PartnerRequestSequenceBlock msg)
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException
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


    /**
     * ASSUMES ALREADY WITHIN LOCK
     @param {PartnerMessageRequestSequenceBlock.proto} msg ---

     @param {string or None} name_of_block_to_exec_next --- the
     name of the sequence block to execute next. None if nothing to
     execute next (ie, last sequence message).
     * 
     */
    private void handle_non_first_sequence_msg_from_partner(
        Endpoint endpoint_recvd_msg_on,PartnerRequestSequenceBlock msg,
        String name_of_block_to_exec_next)
    {
        String reply_to_uuid = msg.getReplyToUuid().getData();
		
        //#### DEBUG
        if (! message_listening_queues_map.containsKey(reply_to_uuid))
        {
            Util.logger_assert(
                "Error: partner response message responding to " +
                "unknown _ActiveEvent message in NonAtomic.");
        }
        //#### END DEBUG
        
        String reply_with_uuid = msg.getReplyWithUuid().getData();
        VariablesProto.Variables returned_variables = msg.getArguments();

        //# unblock waiting listening queue.
        message_listening_queues_map.get(reply_to_uuid).add(
            RalphCallResults.MessageCallResultObject.completed(
                reply_with_uuid,name_of_block_to_exec_next,
                // contain returned results.
                returned_variables));

        //# no need holding onto queue waiting on a message response.
        message_listening_queues_map.remove(reply_to_uuid);
    }	

    public void receive_unsuccessful_first_phase_commit_msg(
        String event_uuid,
        String msg_originator_endpoint_uuid) 
    {
        Util.logger_assert(
            "Non-atomic active event should never receive " +
            "an unsuccessful first phase message.");
    }
	
}



    
