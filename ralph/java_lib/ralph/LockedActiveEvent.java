package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import RalphServiceActions.ServiceAction;

import ralph_protobuffs.PartnerErrorProto.PartnerError;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import RalphCallResults.MessageCallResultObject;
import java.util.concurrent.locks.ReentrantLock;

import RalphCallResults.StopAlreadyCalledMessageCallResult;
import RalphCallResults.BackoutBeforeReceiveMessageResult;
import RalphCallResults.EndpointCallResultObject;

import RalphCallResults.StopAlreadyCalledEndpointCallResult;
import RalphCallResults.BackoutBeforeEndpointCallResult;

import RalphCallResults.ApplicationExceptionCallResult;
import RalphCallResults.NetworkFailureCallResult;

public class LockedActiveEvent
{
	
    private enum State 
    {
        STATE_RUNNING, STATE_FIRST_PHASE_COMMIT,
        STATE_SECOND_PHASE_COMMITTED, STATE_BACKED_OUT
    }
	
    public String uuid;

    public EventParent event_parent = null;
    public ActiveEventMap event_map = null;
	
	
    //# FIXME: maybe can unmake this reentrant, but get deadlock
    //# from serializing data when need to add to touched objects.
    private ReentrantLock mutex = new ReentrantLock();
	
    State state = State.STATE_RUNNING;
	
    private ReentrantLock _nfmutex = new ReentrantLock();

    /**
     //# a dict containing all local objects that this event has
     //# touched while executing.  On commit, must run through each
     //# and complete commit.  On backout, must run through each and
     //# call backout.
     */
    HashMap<String,MultiThreadedLockedObject>touched_objs = 
        new HashMap<String,MultiThreadedLockedObject>();

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
     * 
     */
    private HashMap<String,EventSubscribedTo> other_endpoints_contacted = 
        new HashMap<String,EventSubscribedTo>();

    private boolean partner_contacted = false;
    
    /**
     //# using a separate lock for partner_contaced and
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
     * # keeps a list of functions that we should exec when we
     # complete.  Starts as None, because unlikely to use.  If we
     # do use, then turn it into a proper Queue.Queue().  
     # self.signal_queue = Queue.Queue()
    */
    private ArrayBlockingQueue<SignalFunction>signal_queue = null;

    /**
     *  # Before we attempt to request a commit after a sequence, we need
     # to keep track of whether or not the network has failed; if it has
     # we will not be able to forward a request commit message to our 
     # partner. This variable is only set to True at runtime if a network
     # exception is caught during an event.

    */
    private boolean _network_failure = false;

    /**
     *  # If this is a root event and it gets backed out, we want to
     # reuse the current root event's priority/uuid.  when we
     # backout then, we ask the active event map to generate a new
     # root event with the correct uuid for us.  we put that event
     # in retry_event so that it can be acccessed by emitted code.

    */
    public LockedActiveEvent retry_event = null;
        
    public LockedActiveEvent(
        EventParent _event_parent, ActiveEventMap _event_map)
    {
        event_parent = _event_parent;
        event_map = _event_map;
		
        uuid = event_parent.get_uuid();
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
    public boolean add_touched_obj(MultiThreadedLockedObject obj)
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
        HashMap<String,MultiThreadedLockedObject> touched_objs_copy = 
            new HashMap<String,MultiThreadedLockedObject>(touched_objs);
        _touched_objs_unlock();

        for (MultiThreadedLockedObject obj : touched_objs_copy.values())
            obj.update_event_priority(uuid, new_priority);

        _others_contacted_lock();
        boolean copied_partner_contacted = partner_contacted;
        HashMap<String,EventSubscribedTo> copied_other_endpoints_contacted = 
            new HashMap<String,EventSubscribedTo>(other_endpoints_contacted);
        _others_contacted_unlock();
		
        event_parent.send_promotion_messages(
            copied_partner_contacted,copied_other_endpoints_contacted,new_priority);
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

	
    public void begin_first_phase_commit()
    {
        begin_first_phase_commit(false);
    }
	
    /**
     * If can enter Should send a message back to parent that 
        
     * @param from_partner
     */
    public void begin_first_phase_commit(boolean from_partner)
    {
        _lock();

        if (state != State.STATE_RUNNING)
        {
            _unlock();
            //# note: do not need to respond negatively to first phase
            //# commit request if we already are backing out.  This is
            //# because we should have sent a message to all partners,
            //# etc. as soon as we backed out telling them that they
            //# should also back out.  Do not need to send the same
            //# message again.
            return;
        }
        
        
        //# transition into first phase commit state        
        state = State.STATE_FIRST_PHASE_COMMIT;
        _unlock();


        //# do not need to acquire locks on other_endpoints_contacted
        //# and partner_contacted because once enter first phase commit,
        //# these are immutable.
        //#forwards message on to others and
        //# affirmatively replies that now in first pahse of commit.
        event_parent.first_phase_transition_success(
            other_endpoints_contacted,
            //# If we had a network failure, then we shouldn't try to
            //# forward commit to partner.
            partner_contacted && (! get_network_failure()),
            this);
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
        // # complete commit on each individual object that we touched
        complete_commit_local();
        _unlock();

        event_map.remove_event(uuid,false);
        
        //# FIXME: which should happen first, notifying others or
        //# releasing locks locally?

        //# do not need to acquire locks for partner_contacted and
        //# other_endpoints_contacted because once entered commit, these
        //# values are immutable.
        
        // # notify other endpoints to also complete their commits
        event_parent.second_phase_transition_success(
            other_endpoints_contacted,partner_contacted);
        
    }
	
    /**
       For certain topologies, can have a problem with updating
       peered data:

       root <------> root partner
       a    <------> a partner
        
       root and a are on the same host.  root_partner and a_partner
       are on the same host.  Could get into a situation in which do
       not update partner data.  Specifically, if root updates peered
       data and makes an endpoint call to a, which makes a message
       call to a partner, which makes an endpoint call to root
       partner, which modifies peered data, we need a mechanism for
       root and root partner to exchange the peered data updates.  If
       we wait until first phase of commit, then can get into trouble
       because root locks its variables before knowing it also needs
       to lock root partner's peered variables.  Which could lead to
       deadlock.

       To avoid this, after every endpoint call and root call, we
       send a message to our partners with all updated data.  We then
       wait until we recieve an ack of that message before returning
       back to the endpoint that called us.

       This function checks if we've modified any peered data.  If we
       have, then it sends that message to partner and blocks,
       waiting on a response from partner.

       FIXME: there are (hopefully) better ways to do this.

       FIXME: do I also need to update sequence local data (eg., for
       oncompletes?)

       @returns {bool} --- True if did not get backed out in the
       middle.
 	
       * @return
       */
    public boolean  wait_if_modified_peered()
    {
        //# FIXME: actually fill this method in
        return true;
    }

    public void add_signal_call(SignalFunction signaler)
    {
        if (signal_queue == null)
        {
            signal_queue =
                new ArrayBlockingQueue<SignalFunction>(
                    Util.MEDIUM_QUEUE_CAPACITIES);
        }
        signal_queue.add(signaler);
    }

	
    /**
       ASSUMES ALREADY WITHIN _LOCK
        
       Runs through all touched objects and calls their
       complete_commit methods.  These just remove this event from
       list of lock holders, and, if we wrote, to the object,
       exchanges the dirty cell holding the write with a clean cell.
    */
    private void complete_commit_local()
    {
        //# note that by the time we get here, we know that we will not
        //# be modifying touched_objs dict.  Therefore, we do not need
        //# to take any locks.
        for (MultiThreadedLockedObject obj : touched_objs.values())
            obj.complete_commit(this);

        _touched_objs_lock();
        touched_objs = new HashMap<String,MultiThreadedLockedObject>();
        _touched_objs_unlock();        
        
        if (signal_queue != null)
        {
            while (true)
            {
                SignalFunction signaler = signal_queue.poll();
                if (signaler == null)
                    break;
                event_parent.local_endpoint._signal_queue.add(signaler);
            }
        }
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
        if (state == State.STATE_BACKED_OUT)
        {
            //# Can get multiple backout requests if, for instance,
            //# multiple partner endpoints get preempted and forward
            //# message to this node.  Do nothing: cannot backout twice.
            return;
        }
        
        //# 1
        state = State.STATE_BACKED_OUT;

        
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

        //# 4 ignore the first return arguemnt.  Second return argument
        //# is either None (if this is not a root event) or a new root
        //# event (if this is a root event).  @see comments on
        //# retry_event in constructor.
        ActiveEventTwoTuple event_map_remove_result =
            event_map.remove_event(uuid,true);
        retry_event = event_map_remove_result.b;
        
        //# 5
        //# do not need to acquire locks on other_endpoints_contacted
        //# because the only place that it can be written to is when
        //# already holding _lock.  Therefore, we already have exclusive
        //# access to variable.
        event_parent.rollback(
            backout_requester_endpoint_uuid,other_endpoints_contacted,
            partner_contacted,stop_request);
    }

    /**
     * Called from a separate thread in waldoServiceActions.  Runs
     through all touched objects and backs out of them.
    */
    public void _backout_touched_objs()
    {
        _touched_objs_lock();
        HashMap<String,MultiThreadedLockedObject> copied_touched_objs =
            new HashMap<String,MultiThreadedLockedObject>(touched_objs);
        _touched_objs_unlock();
        
        for (MultiThreadedLockedObject touched_obj : copied_touched_objs.values())
            touched_obj.backout(this);
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
                queue_feeder = new StopAlreadyCalledMessageCallResult();
            else
                queue_feeder = new BackoutBeforeReceiveMessageResult();
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

       Called by a WaldoLockedObject to preempt this event.

       * @param obj_requesting
       */
    public void obj_request_backout_and_release_lock(
        MultiThreadedLockedObject obj_requesting)
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

       Called by a WaldoLockedObject.  WaldoLockedObject will not
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

       @param {Queue or None} threadsafe_unblock_queue --- None if
       this was the last message sent in a sequence and we're not
       waiting on a reply.
    
       The local endpoint is requesting its partner to call some
       sequence block.

    */
    public boolean issue_partner_sequence_block_call(
        ExecutingEventContext ctx, String func_name,
        ArrayBlockingQueue<MessageCallResultObject>threadsafe_unblock_queue,
        boolean first_msg)
    {
        boolean partner_call_requested = false;
        _lock();

        if (state == State.STATE_RUNNING)
        {
            partner_call_requested = true;
            _others_contacted_lock();
            partner_contacted = true;
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

            // FIXME: Must produce variables proto buff from context.
            Util.logger_assert(
                "\nNot actually sending partner message: " +
                "require collecting variables.\n");
            
            // //# here, the local endpoint uses the connection object to
            // //# actually send the message.
            // event_parent.local_endpoint._send_partner_message_sequence_block_request(
            //     func_name, uuid, get_priority(),reply_with_uuid,
            //     ctx.to_reply_with_uuid, this,
            //     //# sending sequence_local_store so that can determine
            //     //# deltas in sequence local state made from this call.
            //     //# do not need to add global store, because
            //     //# self.local_endpoint already has a copy of it.
            //     ctx.sequence_local_store,
            //     first_msg);
        }

        
        _unlock();
        return partner_call_requested;
    }

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
    public boolean issue_endpoint_object_call(
        Endpoint endpoint_calling,String func_name,
        ArrayBlockingQueue<EndpointCallResultObject>result_queue,
        Object...args)            
    {

        boolean endpoint_call_requested = false;
        _lock();
    
        //#### DEBUG
        if ((state == State.STATE_FIRST_PHASE_COMMIT) ||
            (state == State.STATE_SECOND_PHASE_COMMITTED))
        {
            //# when we have been requested to commit, it means that all
            //# events should have run to completion.  Therefore, it
            //# would not make sense to receive an endpoint call when we
            //# were in the request commit state (it would mean that all
            //# events had not run to completion).
            Util.logger_assert(
                "Should not be requesting to issue an endpoint " +
                "object call when in request commit phase.");
        }
        //#### END DEBUG

        if (state == State.STATE_RUNNING)
        {
            //# we can only execute endpoint object calls if we are
            //# currently running.  Note: we may issue an endpoint call
            //# when we are in the backout phase (if, for instance, we
            //# detected a conflict early and wanted to backout).  In
            //# this case, do not make additional endpoint calls.  
            
            endpoint_call_requested = true;

            //# perform the actual endpoint function call.  note that this
            //# does not block until it completes.  It just schedules the 
            endpoint_calling._receive_endpoint_call(
                event_parent.local_endpoint,uuid,
                event_parent.get_priority(),func_name,result_queue,
                args);

            _others_contacted_lock();
            //# add the endpoint to subscribed to
            if (! other_endpoints_contacted.containsKey(endpoint_calling._uuid))
            {
                other_endpoints_contacted.put(
                    endpoint_calling._uuid, 
                    new EventSubscribedTo(endpoint_calling,result_queue));
            }
            else
            {
                other_endpoints_contacted.get(endpoint_calling._uuid).add_result_queue(
                    result_queue);
            }
            
            _others_contacted_unlock();
        } 
        _unlock();
        return endpoint_call_requested;
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
        PartnerRequestSequenceBlock msg, String name_of_block_to_exec_next)
    {
        //### FIGURE OUT WHAT TO EXECUTE NEXT

        //#### DEBUG
        if( name_of_block_to_exec_next == null)
        {
            Util.logger_assert(
                "Error in _ActiveEvent.  Should not receive the " +
                "beginning of a sequence message without some " +
                "instruction for what to do next.");
        }
        //#### END DEBUG

        String block_to_exec_internal_name =
            Util.partner_endpoint_msg_call_func_name(
                name_of_block_to_exec_next);

        // FIXME
        Util.logger_assert(
            "FIXME: must enable handling rpc call from partner");
        
        return null;
    }

	
    /**
     * 
     * @param msg
     */
    public void recv_partner_sequence_call_msg(
        PartnerRequestSequenceBlock msg)
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

        //# FIXME: need to take arguments out of vector.
        Util.logger_assert(
            "\nNeed to incorporate arguments into stack.\n");

        ExecutingEvent exec_event = null;

        _lock();
        if (! msg.hasReplyToUuid())
        {
            exec_event = handle_first_sequence_msg_from_partner(
                msg,name_of_block_to_exec_next);
        }
        else
        {
            handle_non_first_sequence_msg_from_partner(
                msg,name_of_block_to_exec_next);
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
        PartnerRequestSequenceBlock msg, String name_of_block_to_exec_next)
    {
        String reply_to_uuid = msg.getReplyToUuid().getData();
		
        //#### DEBUG
        if (! message_listening_queues_map.containsKey(reply_to_uuid))
        {
            Util.logger_assert(
                "Error: partner response message responding to " +
                "unknown _ActiveEvent message.");
        }
        //#### END DEBUG

        // FIXME
        Util.logger_assert(
            "Must handle receiving rpc from partner.");
    }	

    /**
       @param     error     GeneralMessage.error

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
                    new ApplicationExceptionCallResult(error.getTrace()));
            }
            else if (error.getType() == PartnerError.ErrorType.NETWORK)
            {
                message_listening_queue.add(
                    new NetworkFailureCallResult(error.getTrace()));
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
        //# if self.state == LockedActiveEvent.STATE_FIRST_PHASE_COMMIT:
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



    
