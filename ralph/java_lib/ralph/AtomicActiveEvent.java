package ralph;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import RalphServiceActions.ServiceAction;

import RalphCallResults.MessageCallResultObject;

import RalphExceptions.BackoutException;
import RalphExceptions.StoppedException;
import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;

import RalphDurability.DurabilityContext;

import ralph.ActiveEvent.FirstPhaseCommitResponseCode;

import ralph_protobuffs.PartnerErrorProto.PartnerError;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;

/**
   Interface between AtomicActiveEvent and SpeculativeAtomicObject:

   Locks in system:
   
     touched_objs_lock --- Protects touched_objs map that contains all
     atomic objects that this event has read or written to.
     
     general_lock --- Protects event's state variable (which
     represents state transition machine).
     
   
   Deadlock prevention invariants:
     1) Object can hold its internal lock and then try to assume
        event's general_lock.  Should never hold event's general lock
        and then try to assume object's lock.

     2) Can hold general_lock and then acquire touched_objs_lock.  Can
        also acquire touched_objs_lock directly.  If acquire
        touched_objs_lock, *cannot* then acquire general_lock.

        
   Preemption from speculative object:

     When:
     
     An object can try to preempt an event that is holding a lock on
     it if another event with higher priority attempts to acquire a
     conflicting lock on the same object.  The object is not
     guaranteed to be successful in preempting an event holding its
     lock.  For instance, if an event has entered
     state_first_phase_commit (see below), it may refuse preemption.

     How:
     
     The preempting object locks itself first.  It then issues
     can_backout_and_hold_lock requests on all events that hold locks
     on it.  These assume general_locks on all targetted events,
     returning whether the event can be preempted or cannot be
     preempted.  If the event can be preempted, it does not release
     its general_lock; if it cannot be preempted, it releases its
     general_lock.  If all events can be preempted, the object calls
     obj_request_backout_and_release_lock on all events, aborting them
     and releasing their general_locks.  If any event cannot be
     preempted, the object calls
     obj_request_no_backout_and_release_lock on all events that
     succeeded in response to can_backout_and_hold_lock.  This unlocks
     the held lock.

         
   Backout of object from event:
   
     When:
     
     An event on one host can backout of a lock on an object if its
     sister event on another host is preempted, if one of the commits
     it is pushing to hardware cannot be set, or if another object
     preempts one of its locks.

     How:

     Through some path, backout gets called.  The event acquires
     general_lock and changes its state (see comments about state
     transitions below).  It then schedules another service thread to
     acquire touched_objs_lock, copy touched_objs, release
     touched_objs_lock, and run through all touched objects and call
     backout on them individually.  Note: performing a copy here is
     okay.  We just need to ensure that all objects get backout called
     on them.  In this case, after transitioning state in backout,
     will never *add* any new objects to touched_objs (but may remove
     them if another object tries to preempt this event while it is
     backing out).

   ------
   Avoiding deadlock 2:

   Thread 1:
   
     In begin_first_phase_commit, event acquires general_lock, then
     touched_objs lock.  It makes a copy of the map touched_objs,
     copy_touched_objs, and then releases touched_objs lock and
     general_lock.

     After releasing, it issues first_phase_commit calls to each
     object in copy_touched_objs, registering all of their futures in
     a set.

     Following, we try to read from each objects future.

   Thread 2:
   
     If, between the first and second paragraphs of Thread 1, the
     event backs out, we send a backout message to the object.  The
     object will not invalidate any future because we have not yet
     issued first_phase_commit calls to each object.  If the event
     then issues its first_phase_commit, at this point, the object
     returns an always false future if doesn't have associated read
     lock.

  -----------
  Deadlock + Speculate.
  
  Deadlock can occur if programmers are permitted to call speculate
  before their events have completed.  Consider two events, One and
  Two, and two atomic variables A and B.

  One:                          Two:
    w_lock(A)                     w_lock(B)
    speculate(A)                  speculate(B)
    w_lock(B)                     w_lock(A)

  One waits for B to commit before unspeculating A.  Two waits for A
  to commit before unspeculating B.

  ------------
  Dirty speculate

  Assume an event, Evt, is running.  Evt speculates on some object, A,
  producing A'.  Then Evt backs out, and a new event, Evt' takes its
  place.  It is important that in this case Evt' cannot access A'.
  (Example reason, Evt is removing from an array with 1 element; when
  Evt' operates on A', A' is empty and causes an exception.)

  What this means is that when we handle a backout exception on Evt or
  if we receive a request to backout because a partner event has
  backed out, we must delay returning from this until all touched
  objects have been backed out.  Use method backout to handle these.
  Use internal method _backout to not block on all events' backing
  out.

  This means that cannot call backout while holding a lock on
  AtomicActiveEvent to avoid deadlocks.
  */

public class AtomicActiveEvent extends ActiveEvent
{
    private enum State 
    {
        // Non-terminal, initial state.  Can transition to
        // STATE_BACKING_OUT and STATE_PUSHING_TO_HARDWARE.
        STATE_RUNNING,

        // Non-terminal state.  Enters this state either from:
        //  1) STATE_RUNNING --- when it is told to backout
        //     because the overall active event was being backed
        //     out)
        //  2) STATE_PUSHING_TO_HARDWARE --- when it fails pushing
        //     its changes to its objects or if received a backout
        //     while pushing changes to hardware).
        //  3) STATE_FIRST_PHASE_COMMIT --- when told by an event
        //     on another host that although its changes are
        //     staged, it should back them out because the other
        //     host couldn't stage its changes.
        // While in this state cannot be preempted.  Leaves this
        // state after calls backout on all touched objects and
        // enters STATE_BACKED_OUT.
        STATE_BACKING_OUT,
            
        // Non-terminal state.  Enters this state from
        // STATE_RUNNING.  Transitions either to
        // STATE_FIRST_PHASE_COMMIT or to STATE_BACKING_OUT.
        // While in this state, have submitted requests to objects
        // to stage changes.  To backout, need to issue commands
        // to backout all of these changes.
        STATE_PUSHING_TO_HARDWARE,

        // Non-terminal state.  Enters this state from
        // STATE_PUSHING_TO_HARDWARE.  Means that changes for all
        // objects have been staged.  Transitions to
        // STATE_BACKING_OUT or STATE_SECOND_PHASE_COMMITTED.
        // While in this state, event cannot be backed out
        // locally, can only be backed out because another host
        // processing the same event could not stage its changes.
        STATE_FIRST_PHASE_COMMIT,

        // Terminal state.  Enters this state only after going
        // through STATE_RUNNING -> STATE_PUSHING_TO_HARDWRE ->
        // STATE_FIRST_PHASE_COMMIT, with no intermediate states.
        // Means that all object updates have been written to
        // canonical object values.
        STATE_SECOND_PHASE_COMMITTED,

        // Terminal state.  Enters this state only after all
        // touched objects have been backed out.  Only set in one
        // place: _backout_touched_objs.
        STATE_BACKED_OUT
    }

    public final ActiveEventMap event_map;
    
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
    private final ActiveEvent to_restore_from_atomic;
	
    private final ReentrantLock mutex = new ReentrantLock();
    /**
       Occasionally, we need to wait until an event has completely
       backed out of all its touched objects.  As an example, what if
       this event has speculated on an object? Then, may request a
       retry of event before this event has completely backed out of
       its touched objects. This can mean that the retry request
       begins operating over the speculated value that this
       still-backing-out event used. For instance, if an array has a
       single element and an event's purpose is to remove from that
       array, then the first event will leave the array empty, the
       retried event will speculate on this empty array, removing it
       again. This causes an out-of-bounds index exception.

       When we receive a backout request, we block until the backout
       has completed (individually backed out all touched objects)
       before returning.  This prevents errors where a retried event
       operates on the speculated value an event holds.
     */
    private final Condition completely_backed_out_condition =
        mutex.newCondition();
    
	
    private State state = State.STATE_RUNNING;
	
    /**
       See note in locked_non_blocking_backout: can get a call to
       locked_non_blocking_backout twice.
     */
    private boolean received_backout_already = false;
    
    /**
     //# a dict containing all local objects that this event has
     //# touched while executing.  On commit, must run through each
     //# and complete commit.  On backout, must run through each and
     //# call backout.
     */
    Map<String,AtomicObject>touched_objs = 
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
       This active event makes calls to other endpoints' partners.
       This contains all the endpoints we do that for so that we can
       forward promotion, abort, and two phase commit messages.
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
    private final Map<String,
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

       @param _durability_context --- The durability context that this
       atomic active event should use.  (Ie., if inherited from
       NonAtomicEvent, should already be cloned.)
     */
    public AtomicActiveEvent(
        EventParent _event_parent, ThreadPool _thread_pool,
        ActiveEventMap _event_map,
        ActiveEvent _to_restore_from_atomic,RalphGlobals _ralph_globals,
        DurabilityContext _durability_context)
    {
        super(_event_parent,_thread_pool,_ralph_globals,_durability_context);
        event_map = _event_map;
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
     * @param {WaldoLockedObj} obj --- Whenever we try to perform a
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
       A speculative object can remove itself from an event's list of
       touched objects, thereby stopping directly receiving commands
       from the event (eg., commit, backout, etc.)  This happens when
       the speculative object calls its speculate method.  In essence,
       it is telling the event to send all control messages (commit,
       backout, etc.) to the derived object.  The derived object can
       decide whether or not to forward the messages back or reply to
       them itself.

       If the root object did not unsubscribe from commands, it could
       get into cases where it received duplicate commands from the
       event (one through derived and one directly).
     */
    @Override
    public boolean remove_touched_obj(AtomicObject obj)
    {
        _lock();
        boolean still_running = (state == State.STATE_RUNNING);
        if (still_running)
        {
            _touched_objs_lock();
            touched_objs.remove(obj.uuid);
            _touched_objs_unlock();
        }
        _unlock();
        return still_running;
    }

    @Override
    public void only_remove_touched_obj(AtomicObject obj)
    {
        _touched_objs_lock();
        touched_objs.remove(obj.uuid);
        _touched_objs_unlock();
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
        Map<String,AtomicObject> touched_objs_copy = 
            new HashMap<String,AtomicObject>(touched_objs);
        _touched_objs_unlock();

        for (AtomicObject obj : touched_objs_copy.values())
            obj.update_event_priority(uuid, new_priority);

        _others_contacted_lock();
        Set<Endpoint> copied_local_other_endpoints_contacted = new HashSet<Endpoint>(
            local_endpoints_whose_partners_contacted);
            
        _others_contacted_unlock();
		
        event_parent.send_promotion_messages(
            copied_local_other_endpoints_contacted,new_priority);
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
            (state != State.STATE_BACKED_OUT) &&
            (state != State.STATE_PUSHING_TO_HARDWARE))
        {
            _unlock();
            return false;
        }

        // note: we are still holding lock here.  The object itself
        // must tell us to release lock in
        // obj_request_backout_and_release_lock or
        // obj_request_no_backout_and_release_lock.
        return true;
    }
    
    public boolean immediate_complete()
    {
        return false;
    }

    @Override
    public FirstPhaseCommitResponseCode local_root_begin_first_phase_commit()
    {
        // Unless this is a nested atomically blcok means that this
        // atomic active event should be a root event.  Debugging
        // statement at bottom of method checks this.

        long root_timestamp =
            event_parent.ralph_globals.clock.get_and_increment_int_timestamp();
        String root_host_uuid = event_parent.ralph_globals.host_uuid;

        String root_application_uuid = event_parent.local_endpoint._uuid;
        String root_event_name = event_parent.event_entry_point_name;

        FirstPhaseCommitResponseCode to_return =
            non_local_root_begin_first_phase_commit(
                root_timestamp,root_host_uuid,root_application_uuid,
                root_event_name);

        //// DEBUG
        // performing this check here because an event generated by a
        // partner can make call to
        // local_root_begin_first_phase_commit if there was a nesting
        // atomic block locally.  In that case, we skip the commit,
        // instead of beginning it, and we don't need to actually
        // ensure that event_parent is a root parent.
        if (to_return != FirstPhaseCommitResponseCode.SKIP)
        {
            if (! event_parent.is_root)
            {
                Util.logger_assert(
                    "Shold not receive begin_first_phase_commit " +
                    "on non-root event");
            }
        }
        //// END DEBUG

        return to_return;
    }

    @Override
    public FirstPhaseCommitResponseCode non_local_root_begin_first_phase_commit(
        Long root_first_phase_commit_timestamp,
        String root_first_phase_commit_host_uuid,
        String application_uuid, String event_name)
    {
        
        // set of objects trying to push to hardware.
        Set<ICancellableFuture> obj_could_commit =
            new HashSet<ICancellableFuture>();

        Map<String,AtomicObject> touched_objs_copy = null;
        
        try
        {
            _lock();
            if (atomic_reference_counts > 0)
                return FirstPhaseCommitResponseCode.SKIP;
            
            if (state != State.STATE_RUNNING)
            {
                //# note: do not need to respond negatively to first phase
                //# commit request if we already are backing out.  This is
                //# because we should have sent a message to all partners,
                //# etc. as soon as we backed out telling them that they
                //# should also back out.  Do not need to send the same
                //# message again.
                return FirstPhaseCommitResponseCode.FAILED;
            }

            // update data after skip to prevent overwriting values on
            // root in case of cycles.
            this.commit_metadata = new CommitMetadata(
                root_first_phase_commit_timestamp,
                application_uuid,event_name,uuid);
            
            state = State.STATE_PUSHING_TO_HARDWARE;
            
            // Note: actually pushing individual touched objects
            // outside of lock.  This is to allow the following
            // potential sequences:
            //
            //    evt1 enters first phase commit, tries to commit to obj,
            //    acquiring obj's lock.
            //
            //    evt2 tries to acquire write lock on obj.  This acquires
            //    obj's lock as well as evt1's lock.
            //
            // if first item was still holding lock on evt1, could get
            // deadlock (evt1 holds evt1.lock and tries to acquire
            // obj.lock; evt2 holds obj.lock and tries to acquire
            // evt1.lock).
            //
            // Creating a copy of touched_objs is safe.  By the time
            // we enter first phase commit, we know that we will never
            // *add* to touched_objs.  We may remove from touched_objs
            // (if we are asked to backout while we're in
            // STATE_PUSHING_TO_HARDWARE).  But speculative objects
            // will ignore a first_phase_commit request for a
            // non-reader/non-writer, and so the extra call is safe.
            _touched_objs_lock();
            touched_objs_copy = new HashMap<String,AtomicObject> (touched_objs);
            _touched_objs_unlock();
        }
        finally
        {
            _unlock();
        }

        // first_phase_commit actually initiates pushing changes to
        // hardware, if necessary.
        for (AtomicObject obj : touched_objs_copy.values())
            obj_could_commit.add(obj.first_phase_commit(this));

        // After calling backout on an object, it should unwait this
        // thread calling get on future booleans.
        boolean can_commit = true;
        for (ICancellableFuture could_commit : obj_could_commit)
        {
            try
            {
                // FIXME: Is this condition still necessary?  
                
                // note ordering of && below: this ensures that all
                // sub-objects will have tried to push their changes
                // before we return whether or not we could apply
                // those changes.
                can_commit = could_commit.get().booleanValue() && can_commit;
            }
            catch (InterruptedException _ex)
            {
                // FIXME: should add logic to handle this case.  See
                // issue #34.
                _ex.printStackTrace();
                Util.logger_assert(
                    "Did not consider getting interrupted " +
                    "while committing values.");
            }
            catch (ExecutionException _ex)
            {
                // FIXME: can this case ever happen?
                _ex.printStackTrace();
                Util.logger_assert(
                    "Did not consider execution exception " +
                    "while committing values.");
            }
        }


        try
        {
            _lock();
            boolean preempted =
                (state == State.STATE_BACKING_OUT) || (state == State.STATE_BACKED_OUT);
            if (preempted)
                return FirstPhaseCommitResponseCode.FAILED;

            //// DEBUG: If not in back-ing/-ed out state, then must be
            //// in STATE_PUSHING_TO_HARDWARE.
            if (state != State.STATE_PUSHING_TO_HARDWARE)
            {
                Util.logger_assert(
                    "Incorrect state in first phase commit of AtomicActiveEvent.");
            }
            //// END DEBUG

            
            if (! can_commit)
            {
                // event could not push one of the changes to hardware
                // (was not preempted). Add a job to back this event
                // out.
                ServiceAction service_action =
                    new RalphServiceActions.BackoutAtomicEventAction(this);
                thread_pool.add_service_action(
                    service_action);
                return FirstPhaseCommitResponseCode.FAILED;
            }


            // all changes are staged on objects, can transition
            // into first phase commit state
            state = State.STATE_FIRST_PHASE_COMMIT;
        }
        finally
        {
            _unlock();
        }

        // should only get here if we were able to enter
        // first_phase_commit.  do not need to acquire locks on
        // local_endpoints_whose_partners_contacted and because once
        // enter first phase commit, these are immutable.  forwards
        // message on to others and affirmatively replies that now in
        // first pahse of commit.
        event_parent.first_phase_transition_success(
            local_endpoints_whose_partners_contacted,
            this,commit_metadata.root_commit_lamport_time,
            root_first_phase_commit_host_uuid,
            commit_metadata.root_application_uuid,
            commit_metadata.event_name);
        
        // FIXME: Handle network failure condition
        return FirstPhaseCommitResponseCode.SUCCEEDED;
    }


    /**
       @see comment in ActiveEvent.java.

       Will block until event has backed out from all objects.
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

        // log commit_metadata in case need to replay
        if (VersioningInfo.instance.version_saver != null)
        {
            VersioningInfo.instance.version_saver.save_commit_metadata(
                this.commit_metadata);
        }
        
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
        List<AtomicObject> t_obj = new ArrayList(touched_objs.values());
        for (AtomicObject obj : t_obj)
            obj.complete_commit(this);

        // important: if we do not clear touched objects and enable
        // speculation, we may hold a reference to a speculative
        // object.  That speculative object may hold a reference to
        // outstanding commit requests, which includes additional
        // events, which still hold even more references, etc.... This
        // means that we're keeping objects that we never need
        // reachable.  And eventually we run out of memory.
        touched_objs.clear();
        event_map.remove_event(uuid);
        
        //# FIXME: which should happen first, notifying others or
        //# releasing locks locally?

        //# do not need to acquire locks for partner_contacted and
        //# local_endpoints_whose_partners_contacted because once
        //# entered commit, these values are immutable.
        
        // # notify other endpoints to also complete their commits
        event_parent.second_phase_transition_success(
            local_endpoints_whose_partners_contacted);
        
    }

    /**
       Useful to ensure expectation that certain methods are called
       while holding lock.
     */
    protected int lock_hold_count()
    {
        return mutex.getHoldCount();
    }
    /**
       Throws an assertion if thread that calls this is not currently
       holding lock.
     */
    protected void assert_if_not_holding_lock(String msg)
    {
        if (lock_hold_count() == 0)
            Util.logger_assert(msg);
    }

    protected void assert_if_holding_lock(String msg)
    {
        if (lock_hold_count() != 0)
            Util.logger_assert(msg);
    }

    @Override
    public void non_blocking_backout(
        String backout_requester_host_uuid, boolean stop_request)
    {
        _lock();
        locked_non_blocking_backout(backout_requester_host_uuid,stop_request);
        _unlock();
    }
    
    /**
       MUST BE CALLED FROM WITHIN LOCK
        
       @param {uuid or None} backout_requester_host_uuid --- If
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

       * @param backout_requester_host_uuid
       * @param stop_request

       Changes internal state to BACKING_OUT.  Schedules an event to
       backout all remaining touched objects, but (unlike _backout)
       does not actually wait for them to do so.  Forwards rollbacks
       to other hosts.
       */
    private void locked_non_blocking_backout(
        String backout_requester_host_uuid, boolean stop_request)
    {
        //// DEBUG
        assert_if_not_holding_lock(
            "locked_non_blocking_backout in AtomicActiveEvent should " +
            "be holding lock.");
        //// END DEBUG
        
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
        thread_pool.add_service_action(
            service_action);
            
        //# 3
        rollback_unblock_waiting_queues(stop_request);

        //# 4 remove the event.
        event_map.remove_event(uuid);
        
        //# 5
        //# do not need to acquire locks on
        //# local_endpoints_whose_partners_contacted because the only
        //# place that it can be written to is when already holding
        //# _lock.  Therefore, we already have exclusive access to
        //# variable.
        event_parent.rollback(
            backout_requester_host_uuid,
            local_endpoints_whose_partners_contacted,stop_request);
    }

    /**
     Called from a separate thread in waldoServiceActions.  Runs
     through all touched objects and backs out of them.

     Once object has transitioned into STATE_BACKED_OUT, it notifies
     all conditions that it has now backed out of all objects.
    */
    public void _backout_touched_objs()
    {
        _touched_objs_lock();
        Map<String,AtomicObject> copied_touched_objs = touched_objs;
        touched_objs = new HashMap<String,AtomicObject>();
        _touched_objs_unlock();
        
        for (AtomicObject touched_obj : copied_touched_objs.values())
            touched_obj.backout(this);

        _lock();
        
        state = State.STATE_BACKED_OUT;
        completely_backed_out_condition.signalAll();
        _unlock();
    }

    /**
     *  @param error {Exception}
     * 
     *  Note that if error is a BackoutException, this will block
     *  until has backed out of all objects.
     */
    public void put_exception(Exception error)
    {
        if (RalphExceptions.BackoutException.class.isInstance(error))
            blocking_backout(null,false);
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
        //// DEBUG
        assert_if_not_holding_lock(
            "rollback_unblock_waiting_queues in AtomicActiveEvent " +
            "should be holding lock.");
        //// END DEBUG

        
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
    }

    /**
       Must be called OUTSIDE of lock.

       Unlike locked_non_blocking_backout, blocks until all touched objects
       have processed backout (ie, we've transitioned into
       STATE_BACKED_OUT).  See note "Dirty speculate" at top of file
       for why we do this.
       
       
       @param {uuid or None} backout_requester_host_uuid --- If None,
       means that the call to backout originated on local endpoint.
       Otherwise, means that call to backout was made by either
       endpoint's partner, an endpoint that we called an endpoint
       method on, or an endpoint that called an endpoint method on us.
       * @param stop_request
       */
    @Override
    public void blocking_backout(
        String backout_requester_host_uuid, boolean stop_request)
    {
        assert_if_holding_lock(
            "Cannot be holding lock when enter backout.");
        
        _lock();
        locked_non_blocking_backout(backout_requester_host_uuid,stop_request);
        _unlock();

        _lock();
        while(state != State.STATE_BACKED_OUT)
        {
            try
            {
                completely_backed_out_condition.await();
            }
            catch (InterruptedException ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Should never receive interrupted exceptions " +
                    "while in backout.");
            }
        }
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
        // note that because _backout creates a new thread to run
        // through each touched object and back them out separately,
        // we remove the requesting object from our map of touched
        // objects now.  Importantly, obj_requesting itself performs
        // cleanup on itself.  For instance, removing this event from
        // its read/write lock holders and unblocking any speculative
        // futures.
        _touched_objs_lock();
        touched_objs.remove(obj_requesting.uuid);
        _touched_objs_unlock();

        locked_non_blocking_backout(null,false);
        
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

       @param {List} args --- The positional arguments inserted
       into the call as an rpc.  Includes whether the argument is a
       reference or not (ie, we should update the variable's value on
       the caller).

       @param {RalphObject} result --- If this is a reply to an rpc
       and the called method had a return value, then we return it in
       result.
       
       The local endpoint is requesting its partner to call some
       method on itself.
    */
    @Override
    public boolean issue_partner_sequence_block_call(
        Endpoint endpoint, ExecutingEventContext ctx, String func_name,
        ArrayBlockingQueue<MessageCallResultObject>threadsafe_unblock_queue,
        boolean first_msg,List<RalphObject>args,RalphObject result)
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
        	
            String reply_with_uuid =
                event_parent.ralph_globals.generate_uuid();

            if (threadsafe_unblock_queue != null)
            {
                //# may get None for result queue for the last message
                //# sequence block requested.  It does not need to await
                //# a response.
                message_listening_queues_map.put(
                    reply_with_uuid, threadsafe_unblock_queue);
            }

            // changed to have rpc semantics: this means that if it's not
            // the first message, then it is a reply to another message.
            // if it is a first message, then should not be replying to
            // anything.
            String replying_to = null;
            if (! first_msg)
                replying_to = ctx.get_to_reply_with();

            
            PartnerRequestSequenceBlock request_sequence_block = null;
            try
            {
                request_sequence_block =
                    PartnerRequestSequenceBlockProducer.produce_request_block(
                        replying_to,func_name,args,result,this,true,
                        reply_with_uuid);
            }
            catch (BackoutException ex)
            {
                _unlock();
                return false;
            }
            
            // request endpoint to send message to partner
            endpoint._send_partner_message_sequence_block_request(
                request_sequence_block);
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
        event_parent.receive_successful_first_phase_commit_msg(
            event_uuid,msg_originator_host_uuid,
            children_event_host_uuids);
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
        blocking_backout(null,stop_request);
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

        //// DEBUG
        assert_if_not_holding_lock(
            "handle_first_sequence_msg_from_partner in AtomicActiveEvent " +
            "should be holding lock.");
        //// END DEBUG

        
        //#### DEBUG
        if( name_of_block_to_exec_next == null)
        {
            Util.logger_assert(
                "Error in _ActiveEvent.  Should not receive the " +
                "beginning of a sequence message without some " +
                "instruction for what to do next.");
        }
        //#### END DEBUG

        // grab all arguments from message
        
        // FIXME: Check if necessary for args to be non-null;
        List <RalphObject> args = new ArrayList<RalphObject>();
        if (msg.hasArguments())
        {
            args =
                RPCDeserializationHelper.deserialize_arguments_list(
                    event_parent.ralph_globals,msg.getArguments(),this);
        }

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
            rpc_call_arg_array[i] = args.get(i);

        boolean takes_args = args.size() != 0;
        ExecutingEvent to_return = new ExecutingEvent (
            endpt_recvd_on,
            name_of_block_to_exec_next,this,ctx,
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
    protected void internal_recv_partner_sequence_call_msg(
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
        Endpoint endpt_recvd_on, PartnerRequestSequenceBlock msg,
        String name_of_block_to_exec_next)
    {

        //// DEBUG
        assert_if_not_holding_lock(
            "handle_non_first_sequence_msg_from_partner in AtomicActiveEvent " +
            "should be holding lock.");
        //// END DEBUG

        
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
        
        Arguments returned_objs = null;
        if (msg.hasReturnObjs())
            returned_objs = msg.getReturnObjs();
        
        //# unblock waiting listening queue.
        message_listening_queues_map.get(reply_to_uuid).add(
            RalphCallResults.MessageCallResultObject.completed(
                reply_with_uuid,name_of_block_to_exec_next,
                // result of rpc
                returned_objs));


        
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

    public void receive_unsuccessful_first_phase_commit_msg(
        String event_uuid,
        String msg_originator_host_uuid) 
    {
        forward_backout_request_and_backout_self();
    }
}



    
