package ralph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import RalphExceptions.BackoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * @author bmistree
 *
 * @param <T> --- The java type of the internal data
 * @param <D> --- The type that gets returned from dewaldoify.  Not entirely true
 * If this is an internal container, then contains what each value in map/list would dewaldoify to.
 */
public abstract class MultiThreadedLockedObject<T,D> extends LockedObject<T,D> 
{
    public String uuid = Util.generate_uuid();
    public String host_uuid = null;
    public boolean peered;
    private DataWrapperConstructor<T,D> data_wrapper_constructor;
    public DataWrapper<T,D> val = null;
    private ReentrantLock _mutex = new ReentrantLock();
    public DataWrapper<T,D> dirty_val = null;
		
    //# If write_lock_holder is not None, then the only element in
    //# read_lock_holders is the write lock holder.
    //# read_lock_holders maps from uuids to EventCachedPriorityObj.
    private HashMap<String, EventCachedPriorityObj>read_lock_holders =
        new HashMap<String,EventCachedPriorityObj>();
    
    //# write_lock_holder is EventCachedPriorityObj
    private EventCachedPriorityObj write_lock_holder = null; 

    
    //# A dict of event uuids to WaitingEventTypes
    private HashMap<String,WaitingElement<T,D>>waiting_events =
        new HashMap<String, WaitingElement<T,D>>();

    //# In try_next, can cause events to backout.  If we do cause
    //# other events to backout, then backout calls try_next.  This
    //# (in some cases) can invalidate state that we're already
    //# dealing with in the parent try_next.  Use this flag to keep
    //# track of whether already in try next.  If are, then return
    //# out immediately from future try_next calls.
    private boolean in_try_next = false;
    
    
    /**
     * Used by 
     */
    private class EventCachedPriorityObj
        implements Comparable <EventCachedPriorityObj>
    {
    	String cached_priority = "";
    	ActiveEvent event = null;
    	public EventCachedPriorityObj (
            ActiveEvent active_event,String _cached_priority)
    	{
            event = active_event;
            cached_priority = _cached_priority;
    	}

        /**
           Compares by event priority
         */
        public int compareTo(EventCachedPriorityObj ob)
        {
            return ob.cached_priority.compareTo(ob.cached_priority);
        }
    }
	
	
    public MultiThreadedLockedObject(){}
	
    public void init(
        ValueTypeDataWrapperConstructor<T,D> vtdwc, String _host_uuid,
        boolean _peered, T init_val)
    {
        data_wrapper_constructor = vtdwc;
        host_uuid = _host_uuid;
        peered = _peered;
        val = data_wrapper_constructor.construct(init_val,peered);
    }
	
    private void _lock()
    {
        _mutex.lock();
    }
    private void _unlock()
    {
        _mutex.unlock();
    }
	
	
    /**
     * 
     DOES NOT ASSUME ALREADY WITHIN LOCK
     * @throws BackoutException 

     @returns {DataWrapper object}

     Algorithms:

     0) If already holds a write lock on the variable, then
     return the dirty value associated with event.
           
     1) If already holds a read lock on variable, returns the value
     immediately.

     2) If does not hold a read lock on variable, then attempts
     to acquire one.  If worked, then return the variable.
     When attempting to acquire read lock:

     a) Checks if there is any event holding a write lock.
     If there is not, then adds itself to read lock
     holder dict.
              
     b) If there is another event holding a write lock,
     then check if uuid of the read lock requester is
     >= uuid of the write lock.  If it is, then try to
     backout the holder of write lock.

     c) If cannot backout or have a lesser uuid, then
     create a waiting event and block while listening
     to queue.  (same as #3)
              
     3) If did not work, then create a waiting event and a queue
     and block while listening on that queue.

     Blocks until has acquired.
    */
    private DataWrapper<T,D> acquire_read_lock(
        ActiveEvent active_event) throws BackoutException
    {
        // FIXME: finish
		
        _lock();
        //# Each event has a priority associated with it.  This priority
        //# can change when an event gets promoted to be boosted.  To
        //# avoid the read/write conflicts this might cause, at the
        //# beginning of acquring read lock, get priority and use that
        //# for majority of time trying to acquire read lock.  If cached
        //# priority ends up in WaitingElement, another thread can later
        //# update it.
        String cached_priority = active_event.get_priority();

        //# must be careful to add obj to active_event's touched_objs.
        //# That way, if active_event needs to backout, we're guaranteed
        //# that the state we've allocated for accounting the
        //# active_event is cleaned up here.
        if (! insert_in_touched_objs(active_event))
        {
            _unlock();
            throw new RalphExceptions.BackoutException();
        }

        //# check 0 above
        if ((write_lock_holder != null) &&
            (active_event.uuid == write_lock_holder.event.uuid))
        {
            DataWrapper<T,D> to_return = dirty_val;
            _unlock();
            return to_return;
        }

        //# also check 1 above
        if (read_lock_holders.containsKey(active_event.uuid))
        {
            //# already allowed to read the variable
            DataWrapper<T,D> to_return = val;
            _unlock();
            return to_return;
        }

        //# must be careful to add obj to active_event's touched_objs.
        //# That way, if active_event needs to backout, we're guaranteed
        //# that the state we've allocated for accounting the
        //# active_event is cleaned up here.
        if (! insert_in_touched_objs(active_event))
        {
            _unlock();
            throw new RalphExceptions.BackoutException();
        }

        //# Check 2 from above
        
        //# check 2a
        if (write_lock_holder == null)
        {
            DataWrapper<T,D> to_return = val;
            read_lock_holders.put(
            	active_event.uuid,
            	new EventCachedPriorityObj(active_event,cached_priority));
            _unlock();
            return to_return;
        }

        //# check 2b
        if (EventPriority.gte_priority(cached_priority, write_lock_holder.cached_priority))
        {
            //# backout write lock if can
            if (write_lock_holder.event.can_backout_and_hold_lock())
            {
                //# actually back out the event
                obj_request_backout_and_release_lock(write_lock_holder.event);

                //# add active event as read lock holder and return
                dirty_val = null;
                write_lock_holder = null;
                read_lock_holders = 
                    new HashMap<String,EventCachedPriorityObj>();
                read_lock_holders.put(
                    active_event.uuid,
                    new EventCachedPriorityObj(active_event,cached_priority));
                		
                DataWrapper<T,D> to_return = val;
                _unlock();
                return to_return;
            }
        }
                		

        //# Condition 2c + 3
        //
        //# create a waiting read element
        WaitingElement<T,D> waiting_element = new WaitingElement(
            active_event,cached_priority,true,data_wrapper_constructor,peered);

        waiting_events.put(active_event.uuid, waiting_element);
        _unlock();

        
        DataWrapper<T,D> to_return = null;
        try {
            to_return = waiting_element.queue.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return to_return;
    }
		
	
	
	
    /**
     * DOES NOT ASSUME ALREADY LOCK
     * 
     0) If already holding a write lock, then return the dirty value

     1) If there are no read or write locks, then just copy the
     data value and set read and write lock holders for it.

     2) There are existing read and/or write lock holders.  Check
     if our uuid is larger than their uuids.
        
     * @param active_event
     * @return
     * @throws BackoutException 
     */
    protected DataWrapper<T,D> acquire_write_lock(
        ActiveEvent active_event) throws BackoutException
    {
        _lock();
        //# Each event has a priority associated with it.  This priority
        //# can change when an event gets promoted to be boosted.  To
        //# avoid the read/write conflicts this might cause, at the
        //# beginning of acquring write lock, get priority and use that
        //# for majority of time trying to acquire read lock.  If cached
        //# priority ends up in WaitingElement, another thread can later
        //# update it.        
        String cached_priority = active_event.get_priority();

        //# must be careful to add obj to active_event's touched_objs.
        //# That way, if active_event needs to backout, we're guaranteed
        //# that the state we've allocated for accounting the
        //# active_event is cleaned up here.
        if (! insert_in_touched_objs(active_event))
        {
            _unlock();
            throw new RalphExceptions.BackoutException();
        }

        //# case 0 above
        if ((write_lock_holder != null) &&
            (active_event.uuid == write_lock_holder.event.uuid))
        {
            DataWrapper<T,D> to_return = dirty_val;
            _unlock();
            return to_return;
        }
        
        //# case 1 above
        if ((write_lock_holder != null) && 
            read_lock_holders.isEmpty())
        {
            dirty_val = data_wrapper_constructor.construct(val.val, peered);
            write_lock_holder =
                new EventCachedPriorityObj(active_event,cached_priority);
            
            read_lock_holders.put(
                active_event.uuid,
                new EventCachedPriorityObj(active_event,cached_priority));
            DataWrapper<T,D> to_return = dirty_val;
            _unlock();
            return to_return;
        }

        if (is_gte_than_lock_holding_events(cached_priority))
        {
            //# Stage 2 from above
            if (test_and_backout_all(active_event.uuid))
            {
                //# Stage 3 from above
                //# actually update the read/write lock holders
                read_lock_holders.put(
                    active_event.uuid, 
                    new EventCachedPriorityObj(active_event,cached_priority));

                write_lock_holder =
                    new EventCachedPriorityObj(active_event,cached_priority);
                dirty_val = data_wrapper_constructor.construct(val.val,peered);
                DataWrapper<T,D> to_return = dirty_val;
                _unlock();
                return to_return;
            }
        }
        
        //# case 3: add to wait queue and wait
        WaitingElement <T,D> write_waiting_event = new WaitingElement<T,D>(
            active_event,cached_priority,false,data_wrapper_constructor,
            peered);
        waiting_events.put(active_event.uuid, write_waiting_event);        
        _unlock();

        DataWrapper<T,D> to_return = null;
        try {
            to_return = write_waiting_event.queue.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return to_return;
    }

    /**
       Called when an event with uuid "uuid" is promoted to boosted
       with priority "priority"
    */
    public void update_event_priority(String uuid, String new_priority)
    {
        _lock();
        boolean may_require_update = false;
        
        if ((write_lock_holder != null) && 
            (write_lock_holder.event.uuid == uuid))
        {
            write_lock_holder.cached_priority = new_priority;
        }


        if (read_lock_holders.containsKey(uuid))
        {
            read_lock_holders.get(uuid).cached_priority = new_priority;
        }

        if (waiting_events.containsKey(uuid))
        {
            waiting_events.get(uuid).cached_priority = new_priority;
            may_require_update = true;
        }

        _unlock();
        
        if (may_require_update)
        {
            // start anonymous thread to try next
            new Thread()
            {
                public void run()
                {
                    try_next();
                }        		
            }.start();
        }
    }
	
    public D de_waldoify(ActiveEvent active_event) throws BackoutException
    {
        DataWrapper<T,D> wrapped_val = acquire_read_lock(active_event);
        return wrapped_val.de_waldoify(active_event);
    }	

    /**
       ASSUMES CALLED FROM WITHIN LOCK
        
       When preempting a lock holder, we first call
       can_backout_and_hold_lock on the method.  If this returns
       True, then it means that the active event is holding its lock
       on the event and preparing for a command to complete its
       backout (via obj_request_backout_and_release_lock).

       The active event's obj_request_backout_and_release_lock method
       will not call backout back on this object.  This means that we
       must remove active_event from the read lock holders, write
       lock holders, and waiting elements.

       Note: Previously, immediately after calling
       request_and_release_lock on each obj, we just overwrote
       write_lock_holders and read_lock holders.  This is
       insufficient however because the active event may also be a
       waiting element.  Consider the case where we have a read lock
       holder that is waiting to become a write lock holder.  If we
       then backout of the read lock holder, we should also back out
       of its waiting element.  Otherwise, the waiting element will
       eventually be scheduled.

       * @param active_event
       */
    private void obj_request_backout_and_release_lock(
        ActiveEvent active_event)
    {
        active_event.obj_request_backout_and_release_lock(this);

        //# remove write lock holder if it was one
        read_lock_holders.remove(active_event.uuid);
        if ((write_lock_holder != null) &&
            (write_lock_holder.event.uuid == active_event.uuid))
        {
            write_lock_holder = null;
        }
		
        //# un-jam threadsafe queue waiting for event
        WaitingElement<T,D> waiting_event =
            waiting_events.remove(active_event.uuid);
        if (waiting_event != null)
            waiting_event.unwait(this);
    }

    /**
     * 
     When serializing data to send to other side for peered
     variables, need to get deltas across lifetime of variable,
     this method returns a data wrapper that can be used to get
     those deltas.
     * @param active_event
     */
    public DataWrapper<T,D> get_dirty_wrapped_val(
        ActiveEvent active_event)
    {
        Util.logger_assert(
            "Have not determined how to serialize multithreaded peered data.");
        return null;
    }

    /**
       @returns {bool} --- True if the object has been written to
       since we sent the last message.  False otherwise.  (Including
       if event has been preempted.)
    */
    public boolean get_and_reset_has_been_written_since_last_msg(
        ActiveEvent active_event)
    {
        boolean has_been_written = false;
        _lock();

        //# check if active event even has ability to write to variable
        if (write_lock_holder != null)
        {
            if (write_lock_holder.event.uuid == active_event.uuid)
            {
                has_been_written =
                    dirty_val.get_and_reset_has_been_written_since_last_msg();
            }
        }
        _unlock();
        return has_been_written;
    }

    /**
       Both readers and writers can complete commits.  If it's a
       reader, we do not update the internal value of the object.  If
       it's a writer, we do.  In either case, we remove the event
       from holding a lock and check if any other events can be
       scheduled.
    */
    public void complete_commit(ActiveEvent active_event)
    {
        _lock();
        if ((write_lock_holder != null) &&
            (active_event.uuid == write_lock_holder.event.uuid))
        {
            val.write(dirty_val.val);
            write_lock_holder= null;
            read_lock_holders =
                new HashMap<String,EventCachedPriorityObj>();
        }
        else
        {
            EventCachedPriorityObj read_lock_holder =
                read_lock_holders.remove(active_event.uuid);
            //#### DEBUG
            if (read_lock_holder == null)
            {
                Util.logger_assert(
                    "Should not be completing a commit on a missing event");
            }
            //#### END DEBUG
        }
        _unlock();

        //# FIXME: may want to actually check whether the change could
        //# have caused another read/write to be scheduled.
        try_next();
    }
	
    public boolean is_peered()
    {
        return peered;
    }
	

    /**
       When an event backs out, it calls this method on all the
       objects it holds read/write locks on.  This just removes
       active_event from our lists of read/write lock holders.
    
       If not already holding lock, then delete it from waiting
       events
    */
    public void backout (ActiveEvent active_event)
    {        
        //# FIXME: Are there chances that could process a stale backout?
        //# I think so.
        _lock();
        //# note: there are cases where an active event may try to
        //# backout after it's
        read_lock_holders.remove(active_event.uuid);
        if ((write_lock_holder != null) &&
            (write_lock_holder.event.uuid == active_event.uuid))
        {
            write_lock_holder = null;
        }

        //# un-jam threadsafe queue waiting for event
        WaitingElement<T,D> waiting_event =
            waiting_events.remove(active_event.uuid);
        
        if (waiting_event != null)
            waiting_event.unwait(this);

        _unlock();
        
        //# check if removal might have freed up preforming another
        //# operation that had blocked on it.

        //# FIXME: may want to actually check whether the change could
        //# have caused another read/write to be scheduled.
        try_next();
    }

	
    private boolean test_and_backout_all()
    {
        return test_and_backout_all(null);
    }
	

    /**
       ASSUMES ALREADY WITHIN LOCK

       Checks if can backout all events that currently hold
       read/write locks on this object.
    
       @param {uuid} event_to_not_backout_uuid --- If we had an event
       that was going from being a reader of the data to a writer on
       the data, it might call this method to upgrade itself.  In
       that case, we do not want to back it out itself.  So we skip
       over it.
    
       Happens in two phases:
    
       Phase 1: Iterate through each event holding a lock.  Request
       that event to take a lock on itself so that the event cannot
       transition into a different state (eg., begin two phase
       commit).  Then it returns a bool for whether it's
       backout-able.  (True, if it had not started 2-phase commit,
       False otherwise).

       Note: it is important to sort the list of events by uuid
       before iterating through them.  This is so that we can
       prevent deadlock when two different objects are iterating
       through their lists.
      

       Phase 2:
      
       * If any return that they are not backout-able, then break
       there and tell any that we had already held locks for to
       release their locks and continue on about their
       business.

       * If all return that they are backout-able, then run
       through all and back them out.

       Return True if all were backed out.  False if they weren't.
    */
    private boolean test_and_backout_all(String event_to_not_backout_uuid)
    {
        //# Phase 1:
        //    
        //# note: do not have to explicitly include the write lock key
        //# here because the event that is writing will be included in
        //# read locks
        //
        //# Note: it is important to sort the list of events by uuid
        //# before iterating through them.  This is so that we can
        //# prevent deadlock when two different objects are iterating
        //# through their lists.
		
        ArrayList<EventCachedPriorityObj> read_lock_holder_event_cached_priorities =
            new ArrayList<EventCachedPriorityObj>(read_lock_holders.values());
        Collections.sort(read_lock_holder_event_cached_priorities);

        ArrayList<ActiveEvent> to_backout_list =
            new ArrayList<ActiveEvent>();
        boolean can_backout_all = true;
        for (EventCachedPriorityObj event_cached_priority_obj :
                 read_lock_holder_event_cached_priorities)
        {
            ActiveEvent read_event = event_cached_priority_obj.event;
            String read_uuid = read_event.uuid;
            if (read_uuid != event_to_not_backout_uuid)
            {
                if (read_event.can_backout_and_hold_lock())
                    to_backout_list.add(read_event);
                else
                {
                    can_backout_all = false;
                    break;
                }
            }
        }

		
        //# Phase 2:
        if (can_backout_all)
        {
            for (ActiveEvent event_to_backout : to_backout_list)
                obj_request_backout_and_release_lock(event_to_backout);
 
            EventCachedPriorityObj event_cached_priority =
                read_lock_holders.get(event_to_not_backout_uuid);
            read_lock_holders = new HashMap<String,EventCachedPriorityObj>();
            write_lock_holder = null;
            if (event_cached_priority != null)
                read_lock_holders.put(event_cached_priority.event.uuid, event_cached_priority);
        }
        else
        {
            for (ActiveEvent event_not_to_backout : to_backout_list)
                event_not_to_backout.obj_request_no_backout_and_release_lock();
        }
        return can_backout_all;		
    }


    /**
       @param {priority} waiting_event_priority --- 
    
       @returns {bool} --- Returns True if waiting_event_uuid is
       greater than or equal to all other events that are currently
       holding read or read/write locks on data.
    */
    private boolean is_gte_than_lock_holding_events(
        String waiting_event_priority)
    {
        //# check write lock
        if ((write_lock_holder != null) &&
            (! EventPriority.gte_priority(
                waiting_event_priority,write_lock_holder.cached_priority)))
            return false;
		

        //# check read locks
        for (Entry<String,EventCachedPriorityObj> entry :
                 read_lock_holders.entrySet())
        {
            String cached_priority = entry.getValue().cached_priority;
            if (! EventPriority.gte_priority(
                    waiting_event_priority,cached_priority))
                return false;
        }
        return true;	
    }

	
    /**
     * CALLED FROM WITHIN LOCK
     * 
     *  Check if can schedule the read event waiting_event

     Should be able to schedule if:

     1) There is not a write lock holder or 

     2) There is a write lock holder that is not currently in two
     phase commit, and has a uuid that is less than our uuid.

     a) check if write lock holder is younger (ie, it should
     be preempted).
                 
     b) If it is younger and it's not in two phase commit,
     then go ahead and revoke it.
             
     @returns {bool} --- Returns True if could schedule read
     waiting event.  False if could not.

     * 
     * @param waiting_event
     * @return
     */
    private boolean try_schedule_read_waiting_event(
        WaitingElement waiting_event)
    {
        //# CASE 1
        if (write_lock_holder == null)
        {
            read_lock_holders.put(
                waiting_event.event.uuid, 
                new EventCachedPriorityObj(
                    waiting_event.event,waiting_event.cached_priority));
            waiting_event.unwait(this);
            return true;
        }
        //# CASE 2
        //#   b) If it is younger and it's not in two phase commit, 
        //#      then go ahead and revoke it.
        //
        //# 2 a --- check if write lock holder is younger (ie, it should be
        //#         preempted).
        if (EventPriority.gte_priority(
                write_lock_holder.cached_priority, waiting_event.cached_priority))
        {
            //# do not preempt write lock: it has been around longer
            return false;
        }

        //# 2 b --- If it is younger and it's not in two phase commit, 
        //#         then go ahead and revoke it.
        if (! write_lock_holder.event.can_backout_and_hold_lock())
        {
            //# cannot backout write lock holder
            return false;
        }

		
        //# Can backout write lock holder:
        //#    1) Actually perform backout of writing event
        //#    2) Clean up write lock holder and read lock holder state
        //#       associated with write lock
        //#    3) Waiting event gets included in read lock holder
        //#    4) Unjam waiting event's read queue, which returns value.

        //# 1
        obj_request_backout_and_release_lock(write_lock_holder.event);
        
        //# following code will remove all write lock holders and read
        //# lock holders.
        
        //# 2
        write_lock_holder = null;
        read_lock_holders = new HashMap<String,EventCachedPriorityObj>();
        
        //# 3
        read_lock_holders.put(
            waiting_event.event.uuid, 
            new EventCachedPriorityObj(waiting_event.event,waiting_event.cached_priority));

        //# 4
        waiting_event.unwait(this);
        return true;
    }

    /**
     * Called as an active event runs code.
     * @param active_event
     * @param new_val
     * @throws BackoutException 
     */
    public void set_val(
        ActiveEvent active_event,T new_val) throws BackoutException
    {
        DataWrapper<T,D> to_write_on = acquire_write_lock(active_event);
        to_write_on.write(new_val);
        if (active_event.immediate_complete())
        {
            // nonatomics only operate for a single get or set and do not
            // backout their changes.
            complete_commit(active_event);
        }
    }
    

    /**
       CALLED FROM WITHIN LOCK HOLDER
    
       Gets called when an event that had not been holding a write
       lock tries to begin holding a write lock.

       Three things must happen.
    
       1) Check that the event that is trying to assume the write
       lock has a higher uuid than any other event that is
       currently holding a lock (read or read/write).  

       2) If 1 succeeded, then try to backout all events that
       currently hold locks.  (May not be able to if an event is
       in the midst of a commit.)  If can, roll back changes to
       events that currently hold locks.
    
       3) If 1 and 2 succeeded, then updates self.write_lock_holder
       and self.read_lock_holders.  Also, unjams
       waiting_event's queue.

       @param {Waiting Event object} --- Should be 
    
       @returns {bool} --- True if could successfully schedule the
       waiting write.  False otherwise.
    */
    private boolean try_schedule_write_waiting_event(
        WaitingElement<T,D> waiting_event)
    {    
    	//#### DEBUG
        if (! waiting_event.is_write())
    	{
            Util.logger_assert(
                "Should only pass writes into try_schedule_write_waiting_event");
    	}
        //#### END DEBUG

            
        //# Stage 1 from above
        if (is_gte_than_lock_holding_events(waiting_event.cached_priority))
        {
            //# Stage 2 from above
            if (test_and_backout_all(waiting_event.event.uuid))
            {
                //# Stage 3 from above
                //# actually update the read/write lock holders
                read_lock_holders.put(
                    waiting_event.event.uuid, 
                    new EventCachedPriorityObj(
                        waiting_event.event,waiting_event.cached_priority));
                write_lock_holder =
                    new EventCachedPriorityObj(
                        waiting_event.event,waiting_event.cached_priority);
                waiting_event.unwait(this);
                return true;
            }
        }

        return false;    
    }

    /**
       Check if any events that have been waiting on read/write locks
       can now be scheduled.
    
       All events that are not currently running, but waiting to be
       scheduled on the Waldo object are in the self.waiting_events
       dict.

       #1: Sort all waiting events by uuid (part of time/wait
       algo).

       #2: Keep grabbing elements from the sorted list and trying
       to apply them until:

       a) We hit a write (we know that reads+writes cannot
       function simultaneously) or

       b) The waiting event that we try to schedule fails to
       schedule.  (Eg., it is blocked by a higher-priority
       event that is holding a write lock.)
    */
    private void try_next()
    {
        _lock();

        if (waiting_events.isEmpty())
        {
            _unlock();
            return;
        }

        //# see comment in class' __init__ for in_try_next.
        if (in_try_next)
        {
            _unlock();
            return;
        }
        in_try_next = true;

        
        //# Phase 1 from above:
        //# sort event uuids from high to low to determine if should add
        //# them.
        ArrayList<WaitingElement<T,D>> _waiting_events =
            new ArrayList<WaitingElement<T,D>>(waiting_events.values());
    	Collections.sort(_waiting_events);
        
        //# Phase 2 from above
        //# Run through all waiting events.  If the waiting event is a
        //# write, first check that

        for (WaitingElement<T,D> waiting_event : _waiting_events)
        {
            if (waiting_event.is_write())
            {
            	if (try_schedule_write_waiting_event(waiting_event))
                    waiting_events.remove(waiting_event.event.uuid);
                break;
            }           
            else
            {
            	if (try_schedule_read_waiting_event(waiting_event))
                    waiting_events.remove(waiting_event.event.uuid);
            	else
                    break;
            }
        }
        in_try_next = false;
        _unlock();
    }

    public T get_val(ActiveEvent active_event) throws BackoutException
    {
    	if (active_event == null)
    	{
            //# used for debugging: allows python code to read into and
            //# check the value of an external reference.
            return val.val;
    	}

        DataWrapper<T,D>data_wrapper = acquire_read_lock(active_event);
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
        return data_wrapper.val;
    }
    
    
    /**
       ASSUMES ALREADY HOLDING LOCK

       @param {WaldoActiveEvent} active_event --- 

       @returns {bool} --- True if obj has been inserted into
       active_event's touched_obj dict or already existed there.
       False if did not exist there and the event has been backed
       out.

       This method ensures that this object is in the dict,
       touched_objs, that the event active_event is holding.  It
       tries to add self to that dict.  If the event has already been
       backed out and we try to add self to event's touched_objs, we
       do not add to touched objs and return false.

       * @param active_event
       * @return
       */
    private boolean insert_in_touched_objs(ActiveEvent active_event)
    {
        if (active_event.immediate_complete())
            return true;
        
    	if (waiting_events.containsKey(active_event.uuid) ||
            read_lock_holders.containsKey(active_event.uuid))
            return true;
    	
    	boolean in_running_state = active_event.add_touched_obj(this);
        return in_running_state;
    }
    
}