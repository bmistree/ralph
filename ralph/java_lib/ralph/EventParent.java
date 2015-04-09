package ralph;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import RalphCallResults.MessageCallResultObject;
import java.util.Set;

/**
 *  
 Each event has an event parent.  The parent serves as a connection
 to whatever it was that began the event locally.  For instance, if
 this event was started by an event's partner, then, EventParent
 object would keep a reference to endpoint object so that it can
 forward responses back to partner.
 *
 */
public abstract class EventParent
{
    public final String uuid;
    
    // can be null (eg., if this is a root event)
    public final String spanning_tree_parent_uuid;
    public final RalphGlobals ralph_globals;
    private String priority = null;
    private final ReentrantLock _priority_mutex = new ReentrantLock();
    private boolean has_been_boosted = false;
    public final boolean is_root;

    public final Endpoint local_endpoint;
    
    /**
       The name of the event that caused the creation of the
       associated ActiveEvent.  (Ie., service/endpoint method name.)
     */
    public final String event_entry_point_name;

	
    public EventParent(
        String _uuid, String _priority,
        RalphGlobals _ralph_globals,boolean _is_root,
        Endpoint _local_endpoint, String _event_entry_point_name,
        String spanning_tree_parent_uuid)
    {
        ralph_globals = _ralph_globals;
        
        if (_uuid == null)
            uuid = ralph_globals.generate_uuid();
        else
            uuid = _uuid;

        priority = _priority;
        is_root = _is_root;
        local_endpoint = _local_endpoint;
        event_entry_point_name = _event_entry_point_name;
        this.spanning_tree_parent_uuid = spanning_tree_parent_uuid;
    }
    
    private void _priority_lock()
    {
        _priority_mutex.lock();
    }
    private void _priority_unlock()
    {
        _priority_mutex.unlock();
    }

    /**
     * @return --- The UUID of the event.
     */
    public String get_uuid()
    {
        return uuid;
    }

    public String get_priority()
    {
        _priority_lock();
        String priority_to_return = priority;
        _priority_unlock();
        return priority_to_return;
    }

    /**
       Only called once after object is constructed.
     */
    public void initialize_priority(String new_priority)
    {
        _priority_lock();
        // only reset priority if did not previously have priority.
        // This prevents overwriting a promoted priority.
        if (priority == null)
            priority = new_priority;
        _priority_unlock();
    }
    
	
    /**
       @returns {bool} --- True if the new_priority is actually
       different from the old one.  False otherwise.  Used to check
       whether forwarding cycles of promotion messages on to each
       other.
    */	
    public boolean set_new_priority(String new_priority)
    {
        _priority_lock();
        boolean is_new = false;
        if (! has_been_boosted)
        {
            has_been_boosted = true;
            is_new = true;
            priority = new_priority;
        }
            
        _priority_unlock();
        return is_new;
    }


    /**
       Places the appropriate call result in the event complete queue to 
       indicate to the endpoint that an error has occured and the event
       must be handled.
       * @param error
       * @param message_listening_queues_map
       */
    public abstract void put_exception(
        Exception error,
        Map<String,MVar<MessageCallResultObject>> message_listening_mvars_map);



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
        ActiveEvent event, String msg_originator_host_uuid,
        Set<String>children_event_host_uuids);


    /**
       @param {bool} partner_contacted --- True if the event has sent
       a message as part of a sequence to partner.  False otherwise.

       @param application_uuid --- The uuid of the application that
       began the event.

       @param event_name --- The name of the method on the application
       endpoint that began the event.
       
       Sends a message back to parent that the first phase lock was
       successful.  Message also includes a list of endpoints uuids
       that this endpoint may have called.  The root event cannot
       proceed to second phase of commit until it hears that each of
       the endpoints in this list have affirmed that they got into
       first phase.

       Does *not* forward a message on to other endpoints that this
       endpoint called/touched as part of its computation.  Requests
       each of them to enter first phase commit as well.
    */
    public abstract void first_phase_transition_success(
        Set<String> remote_hosts_contacted_uuid, ActiveEvent event,
        long root_commit_timestamp,
        String root_host_uuid,String application_uuid,
        String event_name);

    public abstract void second_phase_transition_success();
    
    public void rollback()
    {}
}
	
		
