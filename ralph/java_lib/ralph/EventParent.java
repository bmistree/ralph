package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
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
    public Endpoint local_endpoint = null;
    public String uuid = null;
    private String priority = null;
    private ReentrantLock _priority_mutex = new ReentrantLock();
	
    protected ActiveEvent event = null;
    private boolean has_been_boosted = false;
	
	
    public EventParent(Endpoint _local_endpoint,String _uuid, String _priority)
    {
        local_endpoint = _local_endpoint;
        uuid = _uuid;
		
        if (uuid == null)
            uuid = Util.generate_uuid();
		
        priority = _priority;
		
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
       @param {bool} copied_partner_contacted --- True if

       @param {dict} copied_other_endpoints_contacted --- indices are
       uuids, values are EventSubscribedTo
    */
    public void send_promotion_messages(
        Set<Endpoint> local_endpoints_whose_partners_contacted,
        HashMap<String,EventSubscribedTo> copied_other_endpoints_contacted,
        String new_priority)
    {
        for (Endpoint endpt : local_endpoints_whose_partners_contacted)
            endpt._forward_promotion_message(uuid,new_priority);
        
        for (EventSubscribedTo evt_subscribed_to :
                 copied_other_endpoints_contacted.values())
        {
            evt_subscribed_to.endpoint_object._receive_promotion(uuid,new_priority);
        }
		
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
        HashMap<String,ArrayBlockingQueue<MessageCallResultObject>> message_listening_queues_map);



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
        String event_uuid,String msg_originator_host_uuid,
        ArrayList<String>children_event_host_uuids);


    /**
       @param {dict} same_host_endpoints_contacted_dict --- Keys are
       uuids.  Values are EventSubscribedTo objects (which wrap
       endpoint objects).

       @param {bool} partner_contacted --- True if the event has sent
       a message as part of a sequence to partner.  False otherwise.

       Sends a message back to parent that the first phase lock was
       successful.  Message also includes a list of endpoints uuids
       that this endpoint may have called.  The root event cannot
       proceed to second phase of commit until it hears that each of
       the endpoints in this list have affirmed that they got into
       first phase.

       Also forwards a message on to other endpoints that this
       endpoint called/touched as part of its computation.  Requests
       each of them to enter first phase commit as well.
    */
    public void first_phase_transition_success(
        HashMap<String,EventSubscribedTo>same_host_endpoints_contacted_dict,
        Set<Endpoint> local_endpoints_whose_partners_contacted,
        ActiveEvent _event)
    {
        event = _event;
        //# first keep track of all events that we are waiting on
        //# hearing that first phase commit succeeded.
        for (Endpoint endpt : local_endpoints_whose_partners_contacted)
        {
            //# send message to partner telling it to enter first phase
            //# commit
            endpt._forward_commit_request_partner(uuid);
        }

            
        for (String waiting_on_uuid : same_host_endpoints_contacted_dict.keySet())
        {
            //# send message to all other endpoints that we made direct
            //# endpoint calls on that they should attempt first phase
            //# commit
            if (local_endpoint._host_uuid.equals(waiting_on_uuid))
                continue;

            EventSubscribedTo evt_subscribed_to =
                same_host_endpoints_contacted_dict.get(waiting_on_uuid);
            Endpoint endpoint = evt_subscribed_to.endpoint_object;
            endpoint._receive_request_commit(uuid,local_endpoint);
        }
    }

	
    /**
       @param {dict} same_host_endpoints_contacted_dict --- Keys are
       uuids.  Values are EventSubscribedTo objects (which wrap
       endpoint objects).

       @param {bool} partner_contacted --- True if the event has sent
       a message as part of a sequence to partner.  False otherwise.

       Forwards a message on to other endpoints that this endpoint
       called/touched as part of its computation.  Requests each of
       them to enter complete commit.
    */
    public void second_phase_transition_success(
        HashMap<String,EventSubscribedTo>same_host_endpoints_contacted_dict,
        Set<Endpoint> local_endpoints_whose_partners_contacted)

    {
            
        //# tell any endpoints that we had called endpoint methods on to
        //# run second phase of commit
        for (EventSubscribedTo endpoint_to_second_phase_commit :
                 same_host_endpoints_contacted_dict.values())
        {
            endpoint_to_second_phase_commit.endpoint_object._receive_request_complete_commit(uuid);
        }
        for (Endpoint endpt : local_endpoints_whose_partners_contacted)
            endpt._forward_complete_commit_request_partner(uuid);

    }

	
    /**
       @param {uuid or None} backout_requester_host_uuid --- If
       None, means that the call to backout originated on local
       endpoint.  Otherwise, means that call to backout was made by
       a remote host.

       @param {dict} same_host_endpoints_contacted_dict --- Keys are
       uuids.  Values are EventSubscribedTo objects (which wrap
       endpoint objects).

       @param {bool} partner_contacted --- True if the event has sent
       a message as part of a sequence to partner.  False otherwise.

       Tells all endpoints that we have contacted to rollback their
       events as well.  
    */
    public void rollback(
        String backout_requester_host_uuid, 
        HashMap<String,EventSubscribedTo> same_host_endpoints_contacted_dict,
        Set<Endpoint> local_endpoints_whose_partners_contacted,
        boolean stop_request)
    {
        //# tell any endpoints that we had called endpoint methods on to
        //# back out their changes.

        // FIXME: do I really need to rollback each endpoint separately here?
        
        for (EventSubscribedTo subscribed_elements_to_rollback :
                 same_host_endpoints_contacted_dict.values())
        {
            Endpoint endpoint_to_rollback =
                subscribed_elements_to_rollback.endpoint_object;
			
            if (!endpoint_to_rollback._host_uuid.equals(backout_requester_host_uuid))
            {
                endpoint_to_rollback._receive_request_backout(
                    uuid,local_endpoint);
            }
        }

        //# tell partners to backout their changes too
        for (Endpoint endpt : local_endpoints_whose_partners_contacted)
        {
            endpt._forward_backout_request_partner(uuid);
        }
    }
}
	
		
