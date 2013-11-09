package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import RalphConnObj.SingleSideConnection;

import ralph_protobuffs.PromotionProto.Promotion;
import ralph_protobuffs.UtilProto;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;
import ralph_protobuffs.PartnerBackoutCommitRequestProto.PartnerBackoutCommitRequest;
import ralph_protobuffs.PartnerCommitRequestProto.PartnerCommitRequest;
import ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest;
import ralph_protobuffs.PartnerErrorProto.PartnerError;
import ralph_protobuffs.PartnerFirstPhaseResultMessageProto.PartnerFirstPhaseResultMessage;
import ralph_protobuffs.PartnerNotifyReadyProto.PartnerNotifyReady;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.PartnerStopProto.PartnerStop;
import ralph_protobuffs.UtilProto.Timestamp;
import ralph_protobuffs.UtilProto.UUID;
import ralph_protobuffs.VariablesProto.Variables;


/**
 *
 All methods that begin with _receive, are called by other
 endpoints or from connection object's receiving a message from
 partner endpoint.

 All methods that begin with _forward or _send are called from
 active events on this endpoint.
 *
 */
public class Endpoint 
{
    public String _uuid = Util.generate_uuid();
    public String _host_uuid = null;
	
    private LamportClock _clock = null;
	
    private RalphConnObj.ConnectionObj _conn_obj = null;
    public ActiveEventMap _act_event_map = null;

    public VariableStack global_var_stack = new VariableStack();
	
    public ThreadPool _thread_pool = null;
    private AllEndpoints _all_endpoints = null;
	
    public ArrayBlockingQueue<SignalFunction> _signal_queue = 
        new ArrayBlockingQueue<SignalFunction>(Util.QUEUE_CAPACITIES);

    private boolean _this_side_ready_bool = false;
    private boolean _other_side_ready_bool = false;
	
    private ReentrantLock _ready_waiting_list_mutex = new ReentrantLock();
	
	
    private ArrayList<ArrayBlockingQueue<Object>> _ready_waiting_list =
        new ArrayList<ArrayBlockingQueue<Object>>();
	
    private ReentrantLock _stop_mutex = new ReentrantLock();
	
    //# has stop been called locally, on the partner, and have we
    //# performed cleanup, respectively
    private boolean _stop_called = false;
    private boolean _partner_stop_called = false;
    private boolean _stop_complete = false;
	
    private ArrayList<ArrayBlockingQueue<Object>> _stop_blocking_queues =
        new ArrayList<ArrayBlockingQueue<Object>>();
	
    //# holds callbacks to call when stop is complete
    private int _stop_listener_id_assigner = 0;
    private HashMap<Integer,StopListener> _stop_listeners =
        new HashMap<Integer,StopListener>();
    
    private boolean _conn_failed = false;
    private ReentrantLock _conn_mutex = new ReentrantLock();


	
	
    /**
     //# When go through first phase of commit, may need to forward
     //# partner's endpoint uuid back to the root, so the endpoint
     //# needs to keep track of its partner's uuid.  FIXME: right
     //# now, manually setting partner uuids in connection object.
     //# And not checking to ensure that the partner endpoint is set
     //# before doing additional work. should create a proper
     //# handshake instead.
     */
    public String _partner_uuid = null;
	
    /**
       # both sides should run their onCreate methods to entirety
       # before we can execute any additional calls.
    */
    private ReentrantLock _ready_lock_ = new ReentrantLock();
    
    /**
       @param {dict} waldo_classes --- Contains common utilities
       needed by emitted code, such as WaldoNumVariable
        
       @param {uuid} host_uuid --- The uuid of the host this endpoint
       lives on.
        
       @param {ConnectionObject} conn_obj --- Used to write messages
       to partner endpoint.

       @param {_VariableStore} global_var_store --- Contains the
       peered and endpoint global data for this endpoint.  Will not
       add or remove any peered or endpoint global variables.  Will
       only make calls on them.
    */
    public Endpoint (
        RalphGlobals waldo_classes,String host_uuid,
        RalphConnObj.ConnectionObj conn_obj,
        VariableStore global_var_store)
    {
        _clock = waldo_classes.clock;
        _act_event_map = new ActiveEventMap(this,_clock);
        _conn_obj = conn_obj;

        global_var_stack.push(global_var_store);
        
        _thread_pool = waldo_classes.thread_pool;
        _all_endpoints = waldo_classes.all_endpoints;
        _all_endpoints.add_endpoint(this);

        _host_uuid = host_uuid;

        _conn_obj.register_endpoint(this);
        
        Util.logger_warn("Must add heartbeat code back in.");
        Util.logger_warn("Skipping ready wait");
        /*
          # start heartbeat thread
          self._heartbeat = Heartbeat(socket=self._conn_obj, 
          timeout_cb=self.partner_connection_failure,*args)
          self._heartbeat.start()
          _send_clock_update();
        */
        
    }

    public ExecutingEventContext create_context()
    {
        return new ExecutingEventContext(global_var_stack);
    }

    private void _stop_lock()
    {
        _stop_mutex.lock();
    }
 
    private void _stop_unlock()
    {
        _stop_mutex.unlock();
    }

    private void _ready_waiting_list_lock()
    {
        _ready_waiting_list_mutex.lock();
    }

    private void _ready_waiting_list_unlock()
    {
        _ready_waiting_list_mutex.unlock();
    }

    /**
       Called when it has been determined that the connection to the partner
       endpoint has failed prematurely. Closes the socket and raises a network
       exception, thus backing out from all current events, and sets the 
       conn_failed flag, but only if this method has not been called before.
    */
    public void partner_connection_failure()
    {
        //# notify all_endpoints to remove this endpoint because it has
        //# been stopped
        _all_endpoints.network_exception(this);
		
        _conn_mutex.lock();
        _conn_obj.close();
        _raise_network_exception();
        _conn_failed = true;
        _conn_mutex.unlock();
    }
	
    /**
     * Returns true if the runtime has detected a network failure and false
     otherwise.
    */
    public boolean get_conn_failed()
    {
        _conn_mutex.lock();
        boolean conn_failed = _conn_failed;
        _conn_mutex.unlock();
        return conn_failed;
    }

	
    /**
     *  Grab timestamp from clock and send it over to partner.
     */
    public void _send_clock_update()
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
        _conn_obj.write(general_message.build(),this);
    }


    /**
     * Returns True if both sides are initialized.  Otherwise, blocks
     until initialization is complete
    */
    public boolean _block_ready()
    {
        Util.logger_warn("Not waiting on ready.");
        return true;
    }
        
    private void _ready_lock()
    {
        _ready_lock_.lock();
    }
	
    private void _ready_unlock()
    {
        _ready_lock_.unlock();
    }

    /**
       Gets called when the other side sends a message that its
       ready.
    */
    public void _other_side_ready()
    {
        _ready_lock();
        _other_side_ready_bool = true;
        boolean set_ready = (_this_side_ready_bool && _other_side_ready_bool);
        _ready_unlock();

        if (set_ready)
            _set_ready();
    }
	
    public void service_signal()
    {
        while (true)
        {
            SignalFunction signaler = _signal_queue.poll();
            if (signaler == null)
                break;
            signaler.fire(this);
        }
    }

    /**
     * Gets called when this side finishes its initialization
     * @return
     */
    public void _this_side_ready()
    {
        _ready_lock();
        _this_side_ready_bool = true;
        boolean set_ready = (_this_side_ready_bool && _other_side_ready_bool);
        _ready_unlock();

        //# send message to the other side that we are ready
        _notify_partner_ready();
        
        if (set_ready)
            _set_ready();
    }

    public boolean _swapped_in_block_ready()
    {
        return true;
    }
            
    private void _set_ready()
    {
        Util.logger_warn("Must fill in set_ready method");
    }

    /**
     * @see noe above _partner_uuid.
     * @param uuid
     */
    public void _set_partner_uuid(String uuid)
    {
        _partner_uuid = uuid;
    }

    /**
       @param {uuid} uuid --- The uuid of the _ActiveEvent that we
       want to backout.

       @param {either Endpoint object or
       util.PARTNER_ENDPOINT_SENTINEL} requesting_endpoint ---
       Endpoint object if was requested to backout by endpoint objects
       on this same host (from endpoint object calls).
       util.PARTNER_ENDPOINT_SENTINEL if was requested to backout
       by partner.
    
       Called by another endpoint on this endpoint (not called by
       external non-Waldo code).
    */
    public void _receive_request_backout(
        String uuid,Endpoint requesting_endpoint)
    {
        RalphServiceActions.ServiceAction req_backout_action = 
            new RalphServiceActions.ReceiveRequestBackoutAction(
                this,uuid,requesting_endpoint);
        _thread_pool.add_service_action(req_backout_action);
    }

    /**
     * Called by another endpoint on the same host as this endpoint to
     begin the first phase of the commit of the active event with uuid
     "uuid."

     @param {uuid} uuid --- The uuid of the _ActiveEvent that we
     want to commit.

     @param {Endpoint object} requesting_endpoint --- 
     Endpoint object if was requested to commit by endpoint objects
     on this same host (from endpoint object calls).
        
     Called by another endpoint on this endpoint (not called by
     external non-Waldo code).

     Forward the commit request to any other endpoints that were
     touched when the event was processed on this side.

    */
    public void _receive_request_commit(String uuid,Endpoint requesting_endpoint)
    {
        RalphServiceActions.ServiceAction endpoint_request_commit_action = 
            new RalphServiceActions.ReceiveRequestCommitAction(this,uuid,false);
        _thread_pool.add_service_action(endpoint_request_commit_action);
    }

    /**
       Called by another endpoint on the same host as this endpoint
       to finish the second phase of the commit of active event with
       uuid "uuid."

       Another endpoint (either on the same host as I am or my
       partner) asked me to complete the second phase of the commit
       for an event with uuid event_uuid.
    
       @param {uuid} event_uuid --- The uuid of the event we are
       trying to commit.
    */
    public void _receive_request_complete_commit(String event_uuid)
    {
        RalphServiceActions.ServiceAction req_complete_commit_action = 
            new RalphServiceActions.ReceiveRequestCompleteCommitAction(
                this,event_uuid,false);
        _thread_pool.add_service_action(req_complete_commit_action);
    }
	

    /**
     *  Called by the connection object when a network error is detected.

     Sends a message to each active event indicating that the connection
     with the partner endpoint has failed. Any corresponding endpoint calls
     waiting on an event involving the partner will throw a NetworkException,
     which will result in a backout (and be re-raised) if not caught by
     the programmer.

    */
    private void _raise_network_exception()
    {
        _act_event_map.inform_events_of_network_failure();
    }

    /**
     *  Called by the active event when an exception has occured in
     the midst of a sequence and it needs to be propagated back
     towards the root of the active event. Sends a partner_error
     message to the partner containing the event and endpoint
     uuids.
    */
    public void _propagate_back_exception(
        String event_uuid,String priority,Exception exception)
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
        PartnerError.Builder error = PartnerError.newBuilder();
        UUID.Builder msg_evt_uuid = UUID.newBuilder();
        msg_evt_uuid.setData(event_uuid);
        UUID.Builder msg_host_uuid = UUID.newBuilder();
        msg_host_uuid.setData(_uuid);
		
        error.setEventUuid(msg_evt_uuid);
        error.setHostUuid(msg_host_uuid);
		
        if (RalphExceptions.NetworkException.class.isInstance(exception))
        {
            error.setType(PartnerError.ErrorType.NETWORK);
            error.setTrace("Incorrect trace for now");
        }
        else if (RalphExceptions.ApplicationException.class.isInstance(exception))
        {
            error.setType(PartnerError.ErrorType.APPLICATION);
            error.setTrace("Incorrect trace for now");
        }
        else
        {
            error.setType(PartnerError.ErrorType.APPLICATION);
            error.setTrace("Incorrect trace for now");            
        }
        _conn_obj.write(general_message.build(),this);
    }

    /**
       @param {String} string_msg --- A raw byte string sent from
       partner.  Should be able to deserialize it, convert it into a
       message, and dispatch it as an event.

       Can receive a variety of messages from partner: request to
       execute a sequence block, request to commit a change to a
       peered variable, request to backout an event, etc.  In this
       function, we dispatch depending on message we receive.
    */
    public void _receive_msg_from_partner(GeneralMessage general_msg)
    {
        long tstamp = general_msg.getTimestamp();
        _clock.check_update_timestamp(tstamp);

        if (general_msg.hasNotifyReady())
        {
            String endpoint_uuid = general_msg.getNotifyReady().getEndpointUuid().getData();
            _receive_partner_ready(endpoint_uuid);
        }
        else if (general_msg.hasRequestSequenceBlock())
        {
            RalphServiceActions.ServiceAction service_action =  
                new RalphServiceActions.ReceivePartnerMessageRequestSequenceBlockAction(
                    this,general_msg.getRequestSequenceBlock());
            _thread_pool.add_service_action(service_action);
        }
        else if (general_msg.hasStop())
        {
            Util.logger_warn("Skipping stop");
        }
        else if (general_msg.hasFirstPhaseResult())
        {
            PartnerFirstPhaseResultMessage fpr = general_msg.getFirstPhaseResult();
            String event_uuid = fpr.getEventUuid().getData();
            String endpoint_uuid = fpr.getSendingEndpointUuid().getData();
            if (general_msg.getFirstPhaseResult().getSuccessful())
            {	
                ArrayList<String> children_event_endpoint_uuids = new ArrayList<String>();
                for (int i = 0; i < fpr.getChildrenEventEndpointUuidsCount(); ++i)
                {
                    String child_event_uuid = fpr.getChildrenEventEndpointUuids(i).getData();
                    children_event_endpoint_uuids.add(child_event_uuid);
                }
        		
                _receive_first_phase_commit_successful(
                    event_uuid,endpoint_uuid,children_event_endpoint_uuids);
            }
            else
                _receive_first_phase_commit_unsuccessful(event_uuid,endpoint_uuid);
        }
        else if (general_msg.hasPromotion())
        {
            String event_uuid =
                general_msg.getPromotion().getEventUuid().getData();
            String new_priority =
                general_msg.getPromotion().getNewPriority().getData();
            _receive_promotion(event_uuid,new_priority);
        }
        else if (general_msg.hasBackoutCommitRequest())
        {
            String event_uuid =
                general_msg.getBackoutCommitRequest().getEventUuid().getData();
            _receive_request_backout(
                event_uuid,Util.PARTNER_ENDPOINT_SENTINEL);
        }
        else if (general_msg.hasCompleteCommitRequest())
        {
            RalphServiceActions.ServiceAction service_action = 
                new RalphServiceActions.ReceiveRequestCompleteCommitAction(
                    this,general_msg.getCompleteCommitRequest().getEventUuid().getData(),
                    true);
            _thread_pool.add_service_action(service_action);
        }
        else if (general_msg.hasCommitRequest())
        {
            String event_uuid = general_msg.getCommitRequest().getEventUuid().getData();
            RalphServiceActions.ServiceAction service_action = 
                new RalphServiceActions.ReceiveRequestCommitAction(
                    this,event_uuid,true);
            _thread_pool.add_service_action(service_action);
        }
        else if (general_msg.hasError())
        {
            Util.logger_warn("Not handling error message.");
        }
        else if (general_msg.hasHeartbeat())
        {
            Util.logger_warn("Not handling heartbeat");
        }
        //#### DEBUG
        else
        {
            Util.logger_assert(
                "Do not know how to convert message to event action " +
                "in _receive_msg_from_partner.");
        }
        //#### END DEBUG

    }
	
    public void _receive_promotion(String event_uuid, String new_priority)
    {
        RalphServiceActions.ServiceAction promotion_action = 
            new RalphServiceActions.ReceivePromotionAction(
                this,event_uuid,new_priority);
        _thread_pool.add_service_action(promotion_action);
    }
        
    public void _receive_partner_ready()
    {
        _receive_partner_ready(null);
    }
    public void _receive_partner_ready(String partner_uuid)
    {
        _set_partner_uuid(partner_uuid);
        RalphServiceActions.ServiceAction service_action =
            new RalphServiceActions.ReceivePartnerReadyAction(this);
        _thread_pool.add_service_action(service_action);        
    }
	
    /**
     * Tell partner endpoint that I have completed my onReady action.
     */
    public void _notify_partner_ready()
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
		
        PartnerNotifyReady.Builder partner_notify_ready =
            PartnerNotifyReady.newBuilder();
		
		
        UtilProto.UUID.Builder endpoint_uuid_builder =
            UtilProto.UUID.newBuilder();
        endpoint_uuid_builder.setData(_uuid);
		
        partner_notify_ready.setEndpointUuid(endpoint_uuid_builder);
		
        general_message.setNotifyReady(partner_notify_ready);

        _conn_obj.write(general_message.build(),this);
    }
        
    /**
     * Send partner message that event has been promoted
     * @param uuid
     * @param new_priority
     */
    public void _forward_promotion_message(String uuid,String new_priority)
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
        Promotion.Builder promotion_message = Promotion.newBuilder();
		
        UtilProto.UUID.Builder event_uuid_builder = UtilProto.UUID.newBuilder();
        event_uuid_builder.setData(uuid);
		
        UtilProto.Priority.Builder new_priority_builder =
            UtilProto.Priority.newBuilder();
        new_priority_builder.setData(new_priority);
		
        promotion_message.setEventUuid(event_uuid_builder);
        promotion_message.setNewPriority(new_priority_builder);
		
        general_message.setPromotion(promotion_message);
		
        _conn_obj.write(general_message.build(),this);
    }

    /**
     * Send a message to partner that a subscriber is no longer
       holding a lock on a resource to commit it.
	
     * @param event_uuid
     * @param removed_subscriber_uuid
     * @param host_uuid
     * @param resource_uuid
     */
    public void _notify_partner_removed_subscriber(
        String event_uuid,String removed_subscriber_uuid,
        String host_uuid,String resource_uuid)
    {
        Util.logger_assert("Not filled in partner removed subscriber");
    }            

	
    /**
       @param {uuid} event_uuid
       @param {uuid} endpoint_uuid
    
       Partner endpoint is subscriber of event on this endpoint with
       uuid event_uuid.  Send to partner a message that the first
       phase of the commit was unsuccessful on endpoint with uuid
       endpoint_uuid (and therefore, it and everything along the path
       should roll back their commits).
    */
    public void _forward_first_phase_commit_unsuccessful(
        String event_uuid, String endpoint_uuid)
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
        PartnerFirstPhaseResultMessage.Builder first_phase_result =
            PartnerFirstPhaseResultMessage.newBuilder();
		
        UtilProto.UUID.Builder event_uuid_msg = UtilProto.UUID.newBuilder();
        event_uuid_msg.setData(event_uuid);
		
        UtilProto.UUID.Builder sending_endpoint_uuid_msg =
            UtilProto.UUID.newBuilder();
        sending_endpoint_uuid_msg.setData(endpoint_uuid);
		
        first_phase_result.setSuccessful(false);
        first_phase_result.setEventUuid(event_uuid_msg);
        first_phase_result.setSendingEndpointUuid(sending_endpoint_uuid_msg);
		
        general_message.setFirstPhaseResult(first_phase_result);
		
        _conn_obj.write(general_message.build(),this);
    }
	

    /**
     * @param {uuid} event_uuid

     @param {uuid} endpoint_uuid
        
     @param {array} children_event_endpoint_uuids --- 
        
     Partner endpoint is subscriber of event on this endpoint with
     uuid event_uuid.  Send to partner a message that the first
     phase of the commit was successful for the endpoint with uuid
     endpoint_uuid, and that the root can go on to second phase of
     commit when all endpoints with uuids in
     children_event_endpoint_uuids have confirmed that they are
     clear to commit.
    */
    public void _forward_first_phase_commit_successful(
        String event_uuid,String endpoint_uuid,
        ArrayList<String> children_event_endpoint_uuids)
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
        PartnerFirstPhaseResultMessage.Builder first_phase_result_msg =
            PartnerFirstPhaseResultMessage.newBuilder();
		
        UtilProto.UUID.Builder event_uuid_msg =
            UtilProto.UUID.newBuilder();
        event_uuid_msg.setData(event_uuid);
		
        UtilProto.UUID.Builder sending_endpoint_uuid_msg =
            UtilProto.UUID.newBuilder();
        sending_endpoint_uuid_msg.setData(endpoint_uuid);
		
        first_phase_result_msg.setSuccessful(true);
        first_phase_result_msg.setEventUuid(event_uuid_msg);
        first_phase_result_msg.setSendingEndpointUuid(
            sending_endpoint_uuid_msg);
		
        for (String child_event_uuid : children_event_endpoint_uuids)
        {
            UtilProto.UUID.Builder child_event_uuid_msg =
                UtilProto.UUID.newBuilder();
            child_event_uuid_msg.setData(child_event_uuid);
            first_phase_result_msg.addChildrenEventEndpointUuids(
                child_event_uuid_msg);
        }

        general_message.setFirstPhaseResult(first_phase_result_msg);
		
        _conn_obj.write(general_message.build(),this);
    }
	
    /**
       Send a message to partner that a subscriber has just started
       holding a lock on a resource to commit it.
       * @param event_uuid
       * @param additional_subscriber_uuid
       * @param host_uuid
       * @param resource_uuid
       */
    public void  _notify_partner_of_additional_subscriber(
        String event_uuid, String additional_subscriber_uuid, 
        String host_uuid, String resource_uuid)
    {
        Util.logger_assert("No longer building waits-for");
    }
	
    /**
     * @param {uuid} event_uuid --- The uuid of the event that also
     exists on this endpoint that is trying to subscribe to a
     resource (with uuid resource_uuid) that subscriber_event_uuid
     is also subscribed for.

     @param {uuid} subscriber_event_uuid --- UUID for an event that
     is not necesarilly on this host that holds a subscription on
     the same resource that we are trying to subscribe to.

     @see notify_additional_subscriber (in _ActiveEvent.py)
    */
    public void  _receive_additional_subscriber(
        String event_uuid,String subscriber_event_uuid,
        String host_uuid,String resource_uuid)
    {
    	Util.logger_assert("No longer building waits-for");
    }
    
    /**
       @see _receive_additional_subscriber
    */
    public void _receive_removed_subscriber(
        String event_uuid,String removed_subscriber_event_uuid,
        String host_uuid,String resource_uuid)
    {
    	Util.logger_assert("No longer building waits-for");
    }

    
    /**
       @param{_Endpoint object} endpoint_making_call --- The endpoint
       that made the endpoint call into this endpoint.

       @param {uuid} event_uuid --- 

       @param {priority} priority
        
       @param {string} func_name --- The name of the Public function
       to execute (in the Waldo source file).

       @param {Queue.Queue} result_queue --- When the function
       returns, wrap it in a
       waldoEndpointCallResult._EndpointCallResult object and put it
       into this threadsafe queue.  The endpoint that made the call
       is blocking waiting for the result of the call. 

       @param {*args} *args --- additional arguments that the
       function requires.

       Called by another endpoint on this endpoint (not called by
       external non-Waldo code).
        
       Non-blocking.  Requests the endpoint_service_thread to perform
       the endpoint function call listed as func_name.

    */
    public void _receive_endpoint_call(
        Endpoint endpoint_making_call,String event_uuid,
        String priority,String func_name,
        ArrayBlockingQueue<RalphCallResults.EndpointCallResultObject>result_queue,
        Object...args)
    {
        _stop_lock();
        //# check if should short-circuit processing 
        if (_stop_called)
        {
            result_queue.add(
                new RalphCallResults.StopAlreadyCalledEndpointCallResult());
            _stop_unlock();
            return;
        }
        
        _stop_unlock();
        
        RalphServiceActions.ServiceAction endpt_call_action =
            new RalphServiceActions.ReceiveEndpointCallAction(
                this,endpoint_making_call,event_uuid,priority,
                func_name,result_queue,args);
        
        _thread_pool.add_service_action(endpt_call_action);
    }
    
    /**
       One of the endpoints, with uuid endpoint_uuid, that we are
       subscribed to was able to complete first phase commit for
       event with uuid event_uuid.

       @param {uuid} event_uuid --- The uuid of the event associated
       with this message.  (Used to index into local endpoint's
       active event map.)
    
       @param {uuid} endpoint_uuid --- The uuid of the endpoint that
       was able to complete the first phase of the commit.  (Note:
       this may not be the same uuid as that for the endpoint that
       called _receive_first_phase_commit_successful on this
       endpoint.  We only keep track of the endpoint that originally
       committed.)

       @param {None or list} children_event_endpoint_uuids --- None
       if successful is False.  Otherwise, a set of uuids.  The root
       endpoint should not transition from being in first phase of
       commit to completing commit until it has received a first
       phase successful message from endpoints with each of these
       uuids.
    
       Forward the message on to the root.
    */    
    public void  _receive_first_phase_commit_successful(
        String event_uuid,String endpoint_uuid,
        ArrayList<String> children_event_endpoint_uuids)
    {
      
        RalphServiceActions.ServiceAction service_action = 
            new RalphServiceActions.ReceiveFirstPhaseCommitMessage(
                this,event_uuid,endpoint_uuid,true,children_event_endpoint_uuids);
        
        _thread_pool.add_service_action(service_action);
    }
    

    /**
       @param {uuid} event_uuid --- The uuid of the event associated
       with this message.  (Used to index into local endpoint's
       active event map.)

       @param {uuid} endpoint_uuid --- The endpoint
       that tried to perform the first phase of the commit.  (Other
       endpoints may have forwarded the result on to us.)
    */
    public void _receive_first_phase_commit_unsuccessful(
        String event_uuid,String endpoint_uuid)
    {
    	RalphServiceActions.ServiceAction service_action = 
            new RalphServiceActions.ReceiveFirstPhaseCommitMessage(
                this, event_uuid,endpoint_uuid,false,null);
                
        _thread_pool.add_service_action(service_action);
    }
    

    /**
     * 
     Sends a message using connection object to the partner
     endpoint requesting it to perform some message sequence
     action.
    
     @param {String or None} block_name --- The name of the
     sequence block we want to execute on the partner
     endpoint. (Note: this is how that sequence block is named in
     the source Waldo file, not how it is translated by the
     compiler into a function.)  It can also be None if this is the
     final message sequence block's execution.  

     @param {uuid} event_uuid --- The uuid of the requesting event.

     @param {uuid} reply_with_uuid --- When the partner endpoint
     responds, it should place reply_with_uuid in its reply_to
     message field.  That way, we can determine which message the
     partner endpoint was replying to.

     @param {uuid or None} reply_to_uuid --- If this is the
     beginning of a sequence of messages, then fill the reply_to
     field of the message with None (the message is not a reply to
     anything that we have seen so far).  Otherwise, put the
     reply_with message field of the last message that the partner
     said as part of this sequence in.

     @param {LockedActiveEvent} active_event --- The active event that
     is requesting the message to be sent.

     @param {VariableStack} sequence_local_var_stack --- We convert
     all changes that we have made to both peered data and sequence
     local data to maps of deltas so that the partner endpoint can
     apply the changes.  We use the sequence_local_store to get
     changes that invalidation_listener has made to sequence local
     data.  (For peered data, we can just use
     self._global_var_store.)

     @param {bool} first_msg --- If we are sending the first
     message in a sequence block, then we must force the sequence
     local data to be transmitted whether or not it was modified.
     
     @param {boolean} transactional --- True if this call should be
     part of a transaction.  False if it's just a regular rpc.  Only
     keeps track if this is not the first message sent.
    */
    public void _send_partner_message_sequence_block_request(
        String block_name,String event_uuid,String priority,
        String reply_with_uuid, String reply_to_uuid,
        LockedActiveEvent active_event, Variables.Builder rpc_variables,
        boolean first_msg,boolean transactional)
    {
    	GeneralMessage.Builder general_message =
            GeneralMessage.newBuilder();
    	general_message.setTimestamp(_clock.get_int_timestamp());

    	PartnerRequestSequenceBlock.Builder request_sequence_block_msg =
            PartnerRequestSequenceBlock.newBuilder();
    	request_sequence_block_msg.setTransaction(transactional);
    	
    	// event uuid + priority
    	UtilProto.UUID.Builder event_uuid_msg = UtilProto.UUID.newBuilder();
    	event_uuid_msg.setData(event_uuid);
    	
    	UtilProto.Priority.Builder priority_msg =
            UtilProto.Priority.newBuilder();
    	priority_msg.setData(priority);
    	
    	request_sequence_block_msg.setEventUuid(event_uuid_msg);
    	request_sequence_block_msg.setPriority(priority_msg);
    	
    	//name of block requesting
    	if (block_name != null)
            request_sequence_block_msg.setNameOfBlockRequesting(block_name);
    	
    	//reply with uuid
    	UtilProto.UUID.Builder reply_with_uuid_msg =
            UtilProto.UUID.newBuilder();
    	reply_with_uuid_msg.setData(reply_with_uuid);
    	
    	request_sequence_block_msg.setReplyWithUuid(reply_with_uuid_msg);
    	
    	//reply to uuid
    	if (reply_to_uuid != null)
    	{
            UtilProto.UUID.Builder reply_to_uuid_msg =
                UtilProto.UUID.newBuilder();
            reply_to_uuid_msg.setData(reply_to_uuid);
            request_sequence_block_msg.setReplyToUuid(reply_to_uuid_msg);
    	}
        
        request_sequence_block_msg.setArguments(rpc_variables);
    	general_message.setRequestSequenceBlock(request_sequence_block_msg);
    	
    	_conn_obj.write(general_message.build(),this);
    }    

    /**
       @param {UUID} active_event_uuid --- The uuid of the event we
       will forward a commit to our partner for.
    */
    public void _forward_commit_request_partner(String active_event_uuid)
    {
        //# FIXME: may be a way to piggyback commit with final event in
        //# sequence.
    	GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
    	general_message.setTimestamp(_clock.get_int_timestamp());
    	PartnerCommitRequest.Builder commit_request_msg =
            PartnerCommitRequest.newBuilder();
    	UtilProto.UUID.Builder event_uuid_msg = UtilProto.UUID.newBuilder();
    	event_uuid_msg.setData(active_event_uuid);
    	
    	commit_request_msg.setEventUuid(event_uuid_msg);
    	
    	general_message.setCommitRequest(commit_request_msg);
    	_conn_obj.write(general_message.build(),this);
    	
    }
    

    /**
     * @see waldoActiveEvent.wait_if_modified_peered
     * @return
     */
    public void _notify_partner_peered_before_return(
        String event_uuid,String reply_with_uuid,
        LockedActiveEvent active_event)
    {
    	Util.logger_assert("Not handling multithreaded peereds");
    }

    /**
     * @see PartnerNotifyOfPeeredModifiedResponse.proto
     */
    public void _notify_partner_peered_before_return_response(
        String event_uuid, String reply_to_uuid,boolean invalidated)
    {
    	Util.logger_assert("Not handling multithreaded peereds");
    }


    /**
       Active event uuid on this endpoint has completed its commit
       and it wants you to tell partner endpoint as well to complete
       its commit.
    */
    public void _forward_complete_commit_request_partner(
        String active_event_uuid)
    {
    	GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
    	general_message.setTimestamp(_clock.get_int_timestamp());
    	PartnerCompleteCommitRequest.Builder complete_commit_request_msg =
            PartnerCompleteCommitRequest.newBuilder();
    	
    	UtilProto.UUID.Builder active_event_uuid_msg =
            UtilProto.UUID.newBuilder();
    	active_event_uuid_msg.setData(active_event_uuid);
    	
    	complete_commit_request_msg.setEventUuid(active_event_uuid_msg);
    	
    	general_message.setCompleteCommitRequest(complete_commit_request_msg);
    	_conn_obj.write(general_message.build(),this);
    }
    		
    /**
       @param {UUID} active_event_uuid --- The uuid of the event we
       will forward a backout request to our partner for.
    */
    public void _forward_backout_request_partner(
        String active_event_uuid)
    {
    	GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
    	general_message.setTimestamp(_clock.get_int_timestamp());
    	PartnerBackoutCommitRequest.Builder backout_commit_request =
            PartnerBackoutCommitRequest.newBuilder();
    	UtilProto.UUID.Builder event_uuid_builder = UtilProto.UUID.newBuilder();
    	event_uuid_builder.setData(active_event_uuid);
    	
    	general_message.setBackoutCommitRequest(backout_commit_request);
    	_conn_obj.write(general_message.build(),this);
    }
    
    public void _notify_partner_stop()
    {
    	GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
    	general_message.setTimestamp(_clock.get_int_timestamp());
    	PartnerStop.Builder partner_stop_message = PartnerStop.newBuilder();
    	partner_stop_message.setDummy(false);
    	
    	general_message.setStop(partner_stop_message);

        _conn_obj.write_stop(general_message.build(),this);
    }
    
    /**
     * @param {callable} to_exec_on_stop --- When this endpoint
     stops, we execute to_exec_on_stop and any other
     stop listeners that were waiting.

     @returns {int or None} --- int id should be passed back inot
     remove_stop_listener to remove associated stop
     listener.
    */
    public Integer add_stop_listener(StopListener to_exec_on_stop)
    {
    	Util.logger_assert("Need to add stop listeners.");
    	return null;
    }

    /**
       int returned from add_stop_listener
    */
    public void remove_stop_listener(Integer stop_id)
    {
    	Util.logger_assert("Need to add stop listeners.");
    }
    	
    /**
     * Returns whether the endpoint is stopped.
     * 
     */
    public boolean is_stopped()
    {
        return _stop_complete;
    }

    public void stop()
    {
    	Util.logger_assert("Not handling stop");
    }
    
    public void stop(boolean skip_partner)
    {
    	Util.logger_assert("Not handling stop");
    }

    /**
       Passed in as callback arugment to active event map, which calls
       it.
    
       When this is executed:
       1) Stop was called on this side
       
       2) Stop was called on the other side
       
       3) There are no longer any running events in active event map

       Close the connection between both sides.  Unblock the stop call.
    */
    private void _stop_complete_cb()
    {
    	Util.logger_assert("Not handling stop");
    }
    
    /**
       @param {PartnerStop message object} --- Has a single boolean
       field, which is meaningless.
            
       Received a stop message from partner:
       1) Label partner stop as having been called
       2) Initiate stop locally
       3) If have already called stop myself then tell active event
       map we're ready for a shutdown when it is
    */
    private void _handle_partner_stop_msg(PartnerStop msg)
    {
    	Util.logger_assert("Not handling stop");
    }


    
    //# Builtin Endpoint methods
    
    /**
     * Builtin id method. Returns the endpoint's uuid.

     For use within Waldo code.
    */
    private String _endpoint_func_call_prefix__waldo__id(Object...args)
    {
        return _uuid;
    }

    /**
       Builtin id method. Returns the endpoint's uuid.

       For use on endpoints within Python code.
    */
    private String id()
    {
    	return _uuid;
    }
    
}
