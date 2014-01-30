package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

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

public class AtomicActiveEvent extends ActiveEvent
{
    private enum State 
    {
        STATE_RUNNING, STATE_FIRST_PHASE_COMMIT,
        // enters this state from state_running (either when it fails
        // pushing its changes to its objects or if it is told to
        // backout because the overall active event was being backed
        // out).  while in this state cannot be preempted.  Leaves
        // this state after calls backout on all touched objects.
        STATE_BACKING_OUT,
        STATE_SECOND_PHASE_COMMITTED,
        // enters this state only after all touched objects have been
        // backed out.
        STATE_BACKED_OUT
    }

    public ActiveEventMap event_map = null;


    /**
       When we are inside of one atomic event and encounter another
       atomic block, we increase our reference count.  We decrement
       that reference count when we leave the atomic statement.  We
       ignore all requests to commit until our reference count is back
       down to zero.  This way, for each atomic statement, emitter can
       emit request to commit on atomic event regardless of how deeply
       nested our atomic statements are.
     */
    private int atomic_reference_counts = 0;
    ActiveEvent to_restore_from_atomic = null;
	
    //# FIXME: maybe can unmake this reentrant, but get deadlock
    //# from serializing data when need to add to touched objects.
    private ReentrantLock mutex = new ReentrantLock();
	
    State state = State.STATE_RUNNING;
	
    private ReentrantLock _nfmutex = new ReentrantLock();

    /**
       See note in _backout: can get a call to _backout twice.
     */
    private boolean received_backout_already = false;
    
    /**
     //# a dict containing all local objects that this event has
     //# touched while executing.  On commit, must run through each
     //# and complete commit.  On backout, must run through each and
     //# call backout.
     */
    HashMap<String,AtomicObject>touched_objs = 
        new HashMap<String,AtomicObject>();

    /**
     //# using a separate lock for touched objects so that if we are
     //# holding the commit lock on this event, we can still access
     //# the touched_objs map (for promoting priority).  note that
     //# the only time we will write to touched_objs is when we have
     //# already used the event's _lock method.  Therefore, if we are
     //# already inside of a _lock and just reading, we do not need
     //# to acquire _touched_objs_mutex.   
     */
    private ReentrantLock _touched_objs_mutex = new ReentrantLock(); 

    /**
     //# a dict.  keys are uuids, values are EventSubscribedTo
     //# objects, which contain all endpoints that this event has
     //# issued commands to while executing as well as the queues
     //# they may be waiting on to free.  On commit, must run through
     //# each and tell it to enter first phase commit.
     *  FIXME: Should get rid of
     */
    private HashMap<String,EventSubscribedTo> other_endpoints_contacted = 
        new HashMap<String,EventSubscribedTo>();

    /**
       This active event makes calls to other endpoints' partners.
       This contains all the endpoints we do that for so that we can
       forward promotion, abort, and two phase commit messages.

       NOTE: it deprecates other_endpoints_contacted.
     */
    private Set<Endpoint> local_endpoints_whose_partners_contacted =
        new HashSet<Endpoint>();
    
    
    /**
     //# using a separate lock for partner_contacted and
     //# other_endpoints_contacted so that if we are holding the
     //# commit lock on this event, we can still access
     //# other_endpoints_contaced and partner_contacted (for
     //# promoting priority).  note that the only time we will write
     //# to either variable is when we have already used the event's
     //# _lock method.  Therefore, if we are already inside of a
     //# _lock and just reading, we do not need to acquire
     //# _others_contacted_mutex_mutex.
     * 
     */
    private ReentrantLock _others_contacted_mutex = new ReentrantLock();

    
    /**
     //# When an active event sends a message to the partner
     //# endpoint, it blocks until the response.  The way it blocks
     //# is by calling get on an empty threadsafe queue.  There are
     //# two ways that data get put into the queue.  The first is if
     //# the partner endpoint completes its computation and replies.
     //# In this case, _Endpoint updates the associated ActiveEvent
     //# and puts a waldoCallResults._MessageFinishedCallResult
     //# object into the queue.  When the listening endpoint receives
     //# this result, it continues processing.  The other way is if
     //# the event is backed out while we're waiting.  In that case,
     //# the runtime puts a
     //# waldoCallResults._BackoutBeforeReceiveMessageResult object
     //# into the queue.
     //#
     //# We can be listening to more than one open threadsafe message
     //# queue.  If endpoint A waits on its partner, and, while
     //# waiting, its partner executes a series of endpoint calls so
     //# that another method on A is invoked, and that method calls
     //# its partner again, we could be waiting on 2 different
     //# message queues, each held by the same active event.  To
     //# determine which queue a message is a reply to, we read the
     //# message's reply_to field.  If the reply_to field matches one
     //# of the indices in the map below, we know the matching
     //# waiting queue.  If it does not match and is None, that means
     //# that it is the first message in a message sequence.  If it
     //# does not match and is not None, there must be some error.
     //# (@see
     //# waldoExecutingEvent._ExecutingEventContext.to_reply_with_uuid.)
     */
    HashMap<String,
    	ArrayBlockingQueue<MessageCallResultObject>> message_listening_queues_map = 
    	new HashMap<String, ArrayBlockingQueue<MessageCallResultObject>>();

    /**
     *  # Before we attempt to request a commit after a sequence, we need
     # to keep track of whether or not the network has failed; if it has
     # we will not be able to forward a request commit message to our 
     # partner. This variable is only set to True at runtime if a network
     # exception is caught during an event.

    */
    private boolean _network_failure = false;

    
    /**
       @param {ActiveEvent} _to_restore_from_atomic --- We frequently
       create atomic events from within nonatomicevents.  We want to
       be able to access the parente event that created it after the
       atomic event has completed.  Can do so using
       to_restore_from_atomic.
     */
    public AtomicActiveEvent(
        EventParent _event_parent, ActiveEventMap _event_map,
        ActiveEvent _to_restore_from_atomic)
    {
        event_parent = _event_parent;
        event_map = _event_map;
		
        uuid = event_parent.get_uuid();
        to_restore_from_atomic = _to_restore_from_atomic;
    }


    private void _lock()
    {
        mutex.lock();		
    }
	
    private void _unlock()
    {
        mutex.unlock();
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
        _lock();
        boolean still_running = (state == State.STATE_RUNNING);
        if (still_running)
        {
            _touched_objs_lock();
            touched_objs.put(obj.uuid, obj);
            _touched_objs_unlock();
        }
        _unlock();
        return still_running;
    }

    
    /**
     * 
     Gets called either from active event map or from a service
     action.  Used to update event priorities.

     1: Set new priority on event parent.

     2: Copy touched objects.  And run through copy.  For each, ask
     the obj to update its cached version of the event's
     priority.  And check for preemption if the event had been
     waiting (instead of if the event had been a lock holder).
     It's important to *copy* touched objs first.  This is
     because when we call update priority on the object, the
     object will acquire a lock on itself.  Ie, using this
     pattern, we're first acquiring a lock on touched_objs and
     then acquiring a lock on the obj itself.  However, there's
     another path in the code that first acquires a lock on the
     obj and then acquires a lock on touched obj.  (When an obj
     calls add_touced_obj.)  It is okay to make a copy of
     touched objs instead of using the real-time values of objs
     in the dict because by setting the priority first, we
     guarantee that any objects that are added to the dict after
     we make the copy will have the correct, new priority
     anyways.
        
     3: For each object in the copied touched obj, request it to
     update its priority for this event.

     4: Copy endpoints contacted and partner contacted and send a
     promotion message to each endpoint we've already contacted
     and partner.  (Note: lots of similar reasoning to 2.)

     * 
     * @param new_priority
     */
    public void promote_boosted(String new_priority)
    {
        if (! event_parent.set_new_priority(new_priority))
        {
            //# to avoid cycles, if the event's priority has already
            //# been increased, then do not continue to forward messages
            //# about it, notify touched objs, etc.
            return;
        }
         
        _touched_objs_lock();
        HashMap<String,AtomicObject> touched_objs_copy = 
            new HashMap<String,AtomicObject>(touched_objs);
        _touched_objs_unlock();

        for (AtomicObject obj : touched_objs_copy.values())
            obj.update_event_priority(uuid, new_priority);

        _others_contacted_lock();
        Set<Endpoint> copied_local_other_endpoints_contacted = new HashSet<Endpoint>(
            local_endpoints_whose_partners_contacted);
            
        HashMap<String,EventSubscribedTo> copied_other_endpoints_contacted = 
            new HashMap<String,EventSubscribedTo>(other_endpoints_contacted);
        _others_contacted_unlock();
		
        event_parent.send_promotion_messages(
            copied_local_other_endpoints_contacted,
            copied_other_endpoints_contacted,new_priority);
    }

    /**
       Nest transactions: when create a new atomic event inside of one
       atomic event, enclosing atomic event should just subsume
       original atomic event.
     */
    public ActiveEvent clone_atomic() throws StoppedException
    {
        _lock();
        ++ atomic_reference_counts;
        _unlock();
        return this; 
    }
    /**
       When reference count gets back to zero, restore from parent
       that created it.
     */
    public ActiveEvent restore_from_atomic()
    {
        _lock();
        -- atomic_reference_counts;
        boolean should_return_self = atomic_reference_counts >= 0;
        _unlock();
        if (should_return_self)
            return this;

        return to_restore_from_atomic;
    }
    
	
    /**
       @returns {bool} --- True if not in the midst of two phase
       commit.  False otherwise.

       If it is not in the midst of two phase commit, then does not
       return the lock that it is holding.  The lock must be released
       in obj_request_backout_and_release_lock or
       obj_request_no_backout_and_release_lock.
    */
    public boolean can_backout_and_hold_lock()
    {
        _lock();
        if ((state != State.STATE_RUNNING) &&
            (state != State.STATE_BACKED_OUT))
        {
            _unlock();
            return false;
        }
        return true;
    }
    
    public boolean immediate_complete()
    {
        return false;
    }

    @Override
    public FirstPhaseCommitResponseCode begin_first_phase_commit()
    {
        return begin_first_phase_commit(false);
    }
	
    /**
     * If can enter Should send a message back to parent that 
        
     * @param from_partner
     */
    @Override    
    public FirstPhaseCommitResponseCode begin_first_phase_commit(boolean from_partner)
    {
        _lock();
        if (atomic_reference_counts > 0)
        {
            _unlock();
            return FirstPhaseCommitResponseCode.SKIP;
        }

        if (state != State.STATE_RUNNING)
        {
            _unlock();
            //# note: do not need to respond negatively to first phase
            //# commit request if we already are backing out.  This is
            //# because we should have sent a message to all partners,
            //# etc. as soon as we backed out telling them that they
            //# should also back out.  Do not need to send the same
            //# message again.
            return FirstPhaseCommitResponseCode.FAILED;
        }
        


        // for any objects that require pushing changes to hardware,
        // ensure that they can before reporting success.
        Set<Future<Boolean>> obj_could_commit =
            new HashSet<Future<Boolean>>();
        _touched_objs_lock();
        {
            // Added issue #11: May want to perform this call in parallel
            for (AtomicObject obj : touched_objs.values())
                obj_could_commit.add(obj.first_phase_commit(this));
        }
        _touched_objs_unlock();

        boolean can_commit = true;
        for (Future<Boolean> could_commit : obj_could_commit)
        {
            try {
                // note ordering below: this ensures that all
                // sub-objects will have tried to push their changes
                // before we return whether or not we could apply
                // those changes.
                can_commit = could_commit.get().booleanValue() && can_commit;
            } catch (InterruptedException _ex) {
                // FIXME: should add logic to handle this case.  See
                // issue #34.
                _ex.printStackTrace();
                Util.logger_assert(
                    "Did not consider getting interrupted " +
                    "while committing values.");
            } catch (ExecutionException _ex) {
                // FIXME: can this case ever happen?
                _ex.printStackTrace();
                Util.logger_assert(
                    "Did not consider execution exception " +
                    "while committing values.");
            }
        }

        if (can_commit)
        {
            //# transition into first phase commit state        
            state = State.STATE_FIRST_PHASE_COMMIT;
        }
        else
        {
            // while in this state, cannot be preempted
            state = State.STATE_BACKING_OUT;
        }
        // Placed above inside _lock because do not want to backout
        // (eg., if another host issued backout call until pushed
        // changes to hardware) until push changes to hardware.
        _unlock();

        if (! can_commit)
        {
            // if we could not commit, then add a job to back this
            // event out.
            ServiceAction service_action =
                new RalphServiceActions.BackoutAtomicEventAction(this);
            event_parent.local_endpoint._thread_pool.add_service_action(
                service_action);
            return FirstPhaseCommitResponseCode.FAILED;
        }
        

        //# do not need to acquire locks on other_endpoints_contacted
        //# and partner_contacted because once enter first phase commit,
        //# these are immutable.
        //#forwards message on to others and
        //# affirmatively replies that now in first pahse of commit.
        event_parent.first_phase_transition_success(
            other_endpoints_contacted,
            local_endpoints_whose_partners_contacted,
            this);

        // FIXME: Handle network failure condition
        return FirstPhaseCommitResponseCode.SUCCEEDED;
    }


    /**
       @see comment in ActiveEvent.java.
     */
    @Override
    public void handle_backout_exception(BackoutException be)
        throws BackoutException
    {
        _lock();
        boolean should_reraise = (atomic_reference_counts != 0);
        if (should_reraise)
            --atomic_reference_counts;
        _unlock();

        if (should_reraise)
            throw be;
        else
            put_exception(be);
    }

    

    public void second_phase_commit()
    {
        _lock();

        if (state == State.STATE_SECOND_PHASE_COMMITTED)
    	{
            //# already committed, already forwarded names along.
            //# nothing left to do.
            _unlock();
            return;
    	}

        state = State.STATE_SECOND_PHASE_COMMITTED;
        _unlock();

        // complete commit on each individual object that we touched
        // note that by the time we get here, we know that we will not
        // be modifying touched_objs dict (event has completed), and
        // therefore can call this from outside of lock.  Similarly,
        // because changed state to STATE_SECOND_PHASE_COMMITTED, we
        // know that an AtomicObject will not succeed in backing out
        // this event, because can_backout_and_hold will return false.
        // It is important that this complete_commit occurs outside of
        // holding this lock however because the complete_commit call
        // to each of the objects in touched_objs attempts to acquire
        // the lock of each AtomicObject.
        for (AtomicObject obj : touched_objs.values())
            obj.complete_commit(this);

        
        event_map.remove_event(uuid);
        
        //# FIXME: which should happen first, notifying others or
        //# releasing locks locally?

        //# do not need to acquire locks for partner_contacted and
        //# other_endpoints_contacted because once entered commit, these
        //# values are immutable.
        
        // # notify other endpoints to also complete their commits
        event_parent.second_phase_transition_success(
            other_endpoints_contacted,
            local_endpoints_whose_partners_contacted);
        
    }


    /**
       MUST BE CALLED FROM WITHIN LOCK
        
       @param {uuid or None} backout_requester_endpoint_uuid --- If
       None, means that the call to backout originated on local
       endpoint.  Otherwise, means that call to backout was made by
       either endpoint's partner, an endpoint that we called an
       endpoint method on, or an endpoint that called an endpoint
       method on us.
        
       0) If we're already in backed out state, do nothing: we've
       already taken appropriate action.
        
       1) Change state to backed out.
        
       2) Run through all objects that this event has touched and
       backout from them.

       3) Unblock any queues that are waiting on results, with a
       message to quit.

       4) Remove from active event map

       5) Forward messages to all other endpoints in event to roll
       back.

       * @param backout_requester_endpoint_uuid
       * @param stop_request
       */
    private void _backout(String backout_requester_endpoint_uuid, boolean stop_request)
    {
        //# 0
        if (received_backout_already)
        {
            //# Can get multiple backout requests if, for instance,
            //# multiple partner endpoints get preempted and forward
            //# message to this node.  Do nothing: cannot backout twice.
            return;
        }
        
        //# 1
        received_backout_already = true;
        // transition to backout completed in backout_touched_objects.
        state = State.STATE_BACKING_OUT;
        
        
        //# 2: Using a separate thread to backout from objects.  This is
        //# because: 1) does not violate any correctness guarantees to
        //# backout individually and 2) prevents deadlock.  Can get a
        //# case where locked obj holds lock on obj and then tries to
        //# insert iteslf into touched objs, which acquires _lock on
        //# this event.  If, while we are trying to do this, we call
        //# backout (eg., if a backout exception is raised) we will hold
        //# a lock on this event, and then try to lock the object during
        //# backout.  These two together can cause deadlock.  Using a
        //# separate thread instead.
        ServiceAction service_action =
            new RalphServiceActions.EventBackoutTouchedObjs(this);
        event_parent.local_endpoint._thread_pool.add_service_action(
            service_action);
            
        //# 3
        rollback_unblock_waiting_queues(stop_request);

        //# 4 remove the event.
        event_map.remove_event(uuid);
        
        //# 5
        //# do not need to acquire locks on other_endpoints_contacted
        //# because the only place that it can be written to is when
        //# already holding _lock.  Therefore, we already have exclusive
        //# access to variable.
        event_parent.rollback(
            backout_requester_endpoint_uuid,other_endpoints_contacted,
            local_endpoints_whose_partners_contacted,stop_request);
    }

    /**
     * Called from a separate thread in waldoServiceActions.  Runs
     through all touched objects and backs out of them.
    */
    public void _backout_touched_objs()
    {
        _touched_objs_lock();
        HashMap<String,AtomicObject> copied_touched_objs =
            new HashMap<String,AtomicObject>(touched_objs);
        _touched_objs_unlock();
        
        for (AtomicObject touched_obj : copied_touched_objs.values())
            touched_obj.backout(this);

        _lock();
        state = State.STATE_BACKED_OUT;
        _unlock();
    }

    /**
     *  @param error {Exception}
     */
    public void put_exception(Exception error)
    {
        if (RalphExceptions.BackoutException.class.isInstance(error))
            backout(null,false);
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

        for (EventSubscribedTo subscribed_to_element :
                 other_endpoints_contacted.values())
        {
            for (ArrayBlockingQueue<EndpointCallResultObject> res_queue :
                     subscribed_to_element.result_queues)
            {	
                EndpointCallResultObject queue_feeder;
                if (stop_request)
                    queue_feeder = new StopAlreadyCalledEndpointCallResult();
                else
                    queue_feeder = new BackoutBeforeEndpointCallResult();
                res_queue.add(queue_feeder);
            }
        }
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
        _lock();
        _backout(backout_requester_endpoint_uuid,stop_request);
        _unlock();
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
        //# note that because _backout creates a new thread to run
        //# through each touched object and back them out separately, we
        //# backout the requesting object now.
        _touched_objs_lock();
        touched_objs.remove(obj_requesting.uuid);
        _touched_objs_unlock();
        
        _backout(null,false);
        
        //# unlock after method
        _unlock();
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
        _unlock();
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
        Endpoint endpoint, ExecutingEventContext ctx, String func_name,
        ArrayBlockingQueue<MessageCallResultObject>threadsafe_unblock_queue,
        boolean first_msg,ArrayList<RPCArgObject>args)
    {
        boolean partner_call_requested = false;
        _lock();

        if (state == State.STATE_RUNNING)
        {
            partner_call_requested = true;
            _others_contacted_lock();
            local_endpoints_whose_partners_contacted.add(endpoint);
            _others_contacted_unlock();
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
                    _unlock();
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
            endpoint._send_partner_message_sequence_block_request(
                func_name,uuid,get_priority(),reply_with_uuid,
                replying_to,this,serialized_arguments,
                first_msg,true);

        }
        
        _unlock();
        return partner_call_requested;
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
        event_parent.receive_successful_first_phase_commit_msg(
            event_uuid,msg_originator_endpoint_uuid,
            children_event_endpoint_uuids);
    }

    /**
       @param {bool} request_from_partner --- @see
       waldoEndpointServiceThread
       complete_commit_and_forward_complete_msg.
    */
    public void complete_commit_and_forward_complete_msg(
        boolean request_from_partner)
    {
        second_phase_commit();
    }


    public void forward_backout_request_and_backout_self()
    {
        forward_backout_request_and_backout_self(false,false,false);
    }
	
    public void forward_backout_request_and_backout_self(
        boolean skip_partner)
    {
        forward_backout_request_and_backout_self(skip_partner,false,false);
    }
	
    public void forward_backout_request_and_backout_self(
        boolean skip_partner, boolean already_backed_out)
    {
        forward_backout_request_and_backout_self(
            skip_partner,already_backed_out,false);
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
        //# FIXME: may be needlessly forwarding backouts to partners and
        //# back to the endpoints that requested us to back out.
        backout(null,stop_request);
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
        Endpoint endpt_recvd_on,
        PartnerRequestSequenceBlock msg, String name_of_block_to_exec_next)
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException
    {

        //#### DEBUG
        if( name_of_block_to_exec_next == null)
        {
            System.out.println(
                "\nThis was message that's causing issues: " + msg + "\n");
            System.out.println(event_parent);
            System.out.println("\n");
            
            
            Util.logger_assert(
                "Error in _ActiveEvent.  Should not receive the " +
                "beginning of a sequence message without some " +
                "instruction for what to do next.");
        }
        //#### END DEBUG


        // grab all arguments from message
        ArrayList <RPCArgObject> args =
            ExecutingEventContext.deserialize_rpc_args_list(
                msg.getArguments(),endpt_recvd_on._host_uuid);
        
        // create new ExecutingEventContext that copies current stack
        // and keeps track of which arguments need to be returned as
        // references.
        ExecutingEventContext ctx =
            endpt_recvd_on.create_context_for_recv_rpc(args);
        
        // know how to reply to this message.
        ctx.set_to_reply_with(msg.getReplyWithUuid().getData());

        // convert array list of args to optional array of arg objects.
        Object [] rpc_call_arg_array = new Object[args.size()];
        for (int i = 0; i < args.size(); ++i)
            rpc_call_arg_array[i] = args.get(i).arg_to_pass;

        boolean takes_args = args.size() != 0;
        ExecutingEvent to_return = new ExecutingEvent (
            endpt_recvd_on,
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


    /**
     * Exception that gets thrown is from executing internal code.
     * Could be backout, could be stopped, could be other errors (eg.,
     * div by zero).
     *
     * @param msg
     */
    @Override
    public void recv_partner_sequence_call_msg(
        Endpoint endpt_recvd_on,PartnerRequestSequenceBlock msg)
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

        _lock();
        if (! msg.hasReplyToUuid())
        {
            exec_event = handle_first_sequence_msg_from_partner(
                endpt_recvd_on,msg,name_of_block_to_exec_next);
        }
        else
        {
            handle_non_first_sequence_msg_from_partner(
                endpt_recvd_on,msg,name_of_block_to_exec_next);
        }
        _unlock();
        
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
        Endpoint endpt_recvd_on, PartnerRequestSequenceBlock msg, String name_of_block_to_exec_next)
    {
        String reply_to_uuid = msg.getReplyToUuid().getData();

        //#### DEBUG
        if (! message_listening_queues_map.containsKey(reply_to_uuid))
        {
            Util.logger_assert(
                "Error: partner response message responding to " +
                "unknown _ActiveEvent message in AtomicActiveEvent.");
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

    /**
       @param error GeneralMessage.error

       Places an ApplicationExceptionCallResult in the event complete queue to 
       indicate to the endpoint that an application exception has been raised 
       somewhere down the call graph.

       Note that the type of error is 
    */
    public void send_exception_to_listener(PartnerError error)
    {
        _lock();
        //# Send an ApplicationExceptionCallResult to each listening queue
        for (String reply_with_uuid : message_listening_queues_map.keySet())
        {
            //### FIXME: It probably isn't necessary to send an exception result to
            //### each queue.
            ArrayBlockingQueue<MessageCallResultObject> message_listening_queue = 
                message_listening_queues_map.get(reply_with_uuid);

            if (error.getType() == PartnerError.ErrorType.APPLICATION)
            {
                message_listening_queue.add(
                    MessageCallResultObject.application_exception(
                        error.getTrace()));
            }
            else if (error.getType() == PartnerError.ErrorType.NETWORK)
            {
                message_listening_queue.add(
                    MessageCallResultObject.network_failure(
                        error.getTrace()));
            }
        }
        _unlock();
    }

    private void _others_contacted_lock()
    {
        _others_contacted_mutex.lock();
    }

    private void _others_contacted_unlock()
    {
        _others_contacted_mutex.unlock();
    }
     
    private void _touched_objs_lock()
    {
        _touched_objs_mutex.lock();
    }
    private void _touched_objs_unlock()
    {
        _touched_objs_mutex.unlock();
    }
        
    private void _nflock()
    {
        _nfmutex.lock();
    }
    
    private void _nfunlock()
    {
        _nfmutex.unlock();
    }
     
    private void set_network_failure()
    {
        _nflock();
        _network_failure = true;
        _nfunlock();
        //# self._lock()
        //# if self.state == AtomicActiveEvent.STATE_FIRST_PHASE_COMMIT:
        //#     self.forward_backout_request_and_backout_self()
        //# self._unlock()
    }
    
    private boolean get_network_failure()
    {
        _nflock();
        boolean failure = _network_failure;
        _nfunlock();
        return failure;
    }


    public void receive_unsuccessful_first_phase_commit_msg(
        String event_uuid,
        String msg_originator_endpoint_uuid) 
    {
        forward_backout_request_and_backout_self();
    }
}



    
