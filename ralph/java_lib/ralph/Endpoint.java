package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

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
import ralph_protobuffs.VariablesProto;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;

import RalphAtomicWrappers.BaseAtomicWrappers;

import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;


/**
 *
 All methods that begin with _receive, are called by other
 endpoints or from connection object's receiving a message from
 partner endpoint.

 All methods that begin with _forward or _send are called from
 active events on this endpoint.
 *
 */
public abstract class Endpoint 
{
    public String _host_uuid = null;
	
    private LamportClock _clock = null;

    /**
       Can update the connection object of a service factory.  This
       happens, for instance, if one endpoint sends a ServiceFactory
       to another and wants to create a local copy of it, which it can
       then call methods on.

       Endpoint Receiver
       {
           receive_and_instantiate(PartnerServiceFactory psf) returns ServiceReference
           {
               Endpoint ReceiverCreated receiver_created_endpoint =
                   dynamic_cast<Endpoint ReceiverCreated>(psf.construct());
               ...
               return created_endpoint.rpc_reference();
           }
       }

       Endpoint Sender
       {
           instantiate_remote(ServiceFactory remote_sf, ServiceFactory local_sf)
           {
               ServiceReference sr = @partner.receive_and_instantiate(remote_sf);

               Endpoint SenderCreated sender_created_endpoint =
                   dynamic_cast<Endpoint SenderCreated>(local_sf.construct(sr));
           }
       }
       
       partner is installed on a foreign 
     */
    private final ReentrantLock _conn_obj_mutex = new ReentrantLock();
    private RalphConnObj.ConnectionObj _conn_obj = null;
    public ActiveEventMap _act_event_map = null;

    public VariableStack global_var_stack = new VariableStack();
	
    public ThreadPool _thread_pool = null;
    private AllEndpoints _all_endpoints = null;

    public final String _uuid;
	
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
     //# partner's host's endpoint uuid back to the root, so the
     //# endpoint needs to keep track of its partner's uuid.  FIXME:
     //# right now, manually setting partner uuids in connection
     //# object.  And not checking to ensure that the partner endpoint
     //# is set before doing additional work. should create a proper
     //# handshake instead.
     */
    public String _partner_host_uuid = null;

    public RalphGlobals ralph_globals = null;
    
    /**
       @param {RalphGlobals} ralph_globals --- Contains common utilities
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
        RalphGlobals ralph_globals,
        RalphConnObj.ConnectionObj conn_obj,
        VariableStore global_var_store)
    {
        _uuid = ralph_globals.generate_local_uuid();
        this.ralph_globals = ralph_globals;
        
        _clock = ralph_globals.clock;
        _act_event_map =
            new ActiveEventMap(
                this,_clock,ralph_globals.deadlock_avoidance_algorithm,
                ralph_globals);
        _conn_obj = conn_obj;

        global_var_stack.push(global_var_store);
        
        _thread_pool = ralph_globals.thread_pool;
        _all_endpoints = ralph_globals.all_endpoints;
        _all_endpoints.add_endpoint(this);

        _host_uuid = ralph_globals.host_uuid;

        _conn_obj.register_endpoint(this);

        // tell other side my uuid... skip initialization.
        _notify_partner_ready();
        
        // FIXME: See issues #13 and #14 in Github.  Should have
        // ready-wait and heartbeat code in Endpoint constructor.
        // Util.logger_warn("Must add heartbeat code back in.");
        // Util.logger_warn("Skipping ready wait");
        /*
          # start heartbeat thread
          self._heartbeat = Heartbeat(socket=self._conn_obj, 
          timeout_cb=self.partner_connection_failure,*args)
          self._heartbeat.start()
          _send_clock_update();
        */
    }

    /**
       Used to construct empty contexts from non-rpc calls.
     */
    public ExecutingEventContext create_context()
    {
        return new ExecutingEventContext(global_var_stack);
    }
    
    /**
       Used to construct a context when receive rpc call from partner.
       
       @param {ArrayList<RalphObject>} rpc_args --- RPC arguments
       supplied by caller.
       
       @param {boolean} transactional --- Whether or not the rpc
       requested was transactional.
     */
    public ExecutingEventContext create_context_for_recv_rpc(
        ArrayList<RalphObject> rpc_args)
    {
        return new ExecutingEventContext(
            global_var_stack,rpc_args);
    }

    public void update_connection_obj(
        RalphConnObj.ConnectionObj conn_obj,String partner_host_uuid)
    {
        _conn_obj_mutex.lock();
        
        _conn_obj = conn_obj;
        _conn_obj.register_endpoint(this);
        
        _partner_host_uuid = partner_host_uuid;
        _conn_obj_mutex.unlock();
    }
    
    private void _stop_lock()
    {
        _stop_mutex.lock();
    }
 
    private void _stop_unlock()
    {
        _stop_mutex.unlock();
    }

    /**
       Using this mechanism, a service on a remote host can connect to
       this endpoint.  Series of required operations:

       // one host
       Service SomeService serv;
       ServiceReference sr = serv.rpc_reference();

       // send reference to other host, which receives it.
       ServiceFactory sf;
       Service OtherService other_service = sf.construct_from_reference(sr);

       other_service.method(); // where method can call methods on
                               // partner serv on other host.
       
     */
    public InternalServiceReference rpc_reference(
        ExecutingEventContext ctx, ActiveEvent active_event)
    {
        return new InternalServiceReference(
            ralph_globals.ip_addr_to_listen_for_connections_on,
            ralph_globals.tcp_port_to_listen_for_connections_on,
            _uuid);
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
     * @see noe above _partner_uuid.
     * @param uuid
     */
    public void _set_partner_host_uuid(String uuid)
    {
        _partner_host_uuid = uuid;
    }

    protected NonAtomicInternalList<Double,Double> _produce_range(
        Double start,Double end, Double increment)
    {
        NonAtomicInternalList<Double,Double> to_return =
            new NonAtomicInternalList(ralph_globals);

        ArrayList<RalphObject<Double,Double>> init_val =
            new ArrayList<RalphObject<Double,Double>>();
        for (int i = start.intValue(); i < end.intValue();
             i = i + increment.intValue())
        {
            init_val.add(
                new Variables.NonAtomicNumberVariable(
                    false,new Double(i),ralph_globals));
        }
        
        to_return.init(
            new ListTypeDataWrapperFactory<Double,Double>(),
            init_val,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER);

        return to_return;
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
        msg_host_uuid.setData(_host_uuid);
		
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
            String partner_host_uuid = general_msg.getNotifyReady().getHostUuid().getData();
            _receive_partner_ready(partner_host_uuid);
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
            String result_initiator_host_uuid = fpr.getSendingHostUuid().getData();
            if (general_msg.getFirstPhaseResult().getSuccessful())
            {	
                ArrayList<String> children_event_host_uuids = new ArrayList<String>();
                for (int i = 0; i < fpr.getChildrenEventHostUuidsCount(); ++i)
                {
                    String child_event_uuid = fpr.getChildrenEventHostUuids(i).getData();
                    children_event_host_uuids.add(child_event_uuid);
                }
        		
                _receive_first_phase_commit_successful(
                    event_uuid,result_initiator_host_uuid,
                    children_event_host_uuids);
            }
            else
            {
                _receive_first_phase_commit_unsuccessful(
                    event_uuid,result_initiator_host_uuid);
            }
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
        
    private void _receive_partner_ready(String partner_host_uuid)
    {
        _set_partner_host_uuid(partner_host_uuid);
    }
	
    /**
     * Tell partner endpoint that I have completed my onReady action.
     */
    private void _notify_partner_ready()
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
		
        PartnerNotifyReady.Builder partner_notify_ready =
            PartnerNotifyReady.newBuilder();
		
		
        UtilProto.UUID.Builder host_uuid_builder =
            UtilProto.UUID.newBuilder();
        host_uuid_builder.setData(_host_uuid);
        
        partner_notify_ready.setHostUuid(host_uuid_builder);
		
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
    
       Partner endpoint is subscriber of event on this endpoint with
       uuid event_uuid.  Send to partner a message that the first
       phase of the commit was unsuccessful on endpoint with uuid
       host_uuid (and therefore, it and everything along the path
       should roll back their commits).
    */
    public void _forward_first_phase_commit_unsuccessful(
        String event_uuid, String host_uuid)
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
        PartnerFirstPhaseResultMessage.Builder first_phase_result =
            PartnerFirstPhaseResultMessage.newBuilder();
		
        UtilProto.UUID.Builder event_uuid_msg = UtilProto.UUID.newBuilder();
        event_uuid_msg.setData(event_uuid);
		
        UtilProto.UUID.Builder sending_host_uuid_msg =
            UtilProto.UUID.newBuilder();
        sending_host_uuid_msg.setData(host_uuid);
		
        first_phase_result.setSuccessful(false);
        first_phase_result.setEventUuid(event_uuid_msg);
        first_phase_result.setSendingHostUuid(sending_host_uuid_msg);
		
        general_message.setFirstPhaseResult(first_phase_result);
		
        _conn_obj.write(general_message.build(),this);
    }
	

    /**
     * @param {uuid} event_uuid

     @param {uuid} host_uuid
        
     @param {array} children_event_host_uuids --- 
        
     Partner endpoint is subscriber of event on this endpoint with
     uuid event_uuid.  Send to partner a message that the first
     phase of the commit was successful for the endpoint with uuid
     host_uuid, and that the root can go on to second phase of
     commit when all endpoints with uuids in
     children_event_host_uuids have confirmed that they are
     clear to commit.
    */
    public void _forward_first_phase_commit_successful(
        String event_uuid,String host_uuid,
        ArrayList<String> children_event_host_uuids)
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        general_message.setTimestamp(_clock.get_int_timestamp());
        PartnerFirstPhaseResultMessage.Builder first_phase_result_msg =
            PartnerFirstPhaseResultMessage.newBuilder();
		
        UtilProto.UUID.Builder event_uuid_msg =
            UtilProto.UUID.newBuilder();
        event_uuid_msg.setData(event_uuid);
		
        UtilProto.UUID.Builder sending_host_uuid_msg =
            UtilProto.UUID.newBuilder();
        sending_host_uuid_msg.setData(host_uuid);
		
        first_phase_result_msg.setSuccessful(true);
        first_phase_result_msg.setEventUuid(event_uuid_msg);
        first_phase_result_msg.setSendingHostUuid(
            sending_host_uuid_msg);
		
        for (String child_event_uuid : children_event_host_uuids)
        {
            UtilProto.UUID.Builder child_event_uuid_msg =
                UtilProto.UUID.newBuilder();
            child_event_uuid_msg.setData(child_event_uuid);
            first_phase_result_msg.addChildrenEventHostUuids(
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
       One of the endpoints, with uuid host_uuid, that we are
       subscribed to was able to complete first phase commit for
       event with uuid event_uuid.

       @param {uuid} event_uuid --- The uuid of the event associated
       with this message.  (Used to index into local endpoint's
       active event map.)
    
       @param {uuid} host_uuid --- The uuid of the endpoint that
       was able to complete the first phase of the commit.  (Note:
       this may not be the same uuid as that for the endpoint that
       called _receive_first_phase_commit_successful on this
       endpoint.  We only keep track of the endpoint that originally
       committed.)

       @param {None or list} children_event_host_uuids --- None
       if successful is False.  Otherwise, a set of uuids.  The root
       endpoint should not transition from being in first phase of
       commit to completing commit until it has received a first
       phase successful message from endpoints with each of these
       uuids.
    
       Forward the message on to the root.
    */    
    public void  _receive_first_phase_commit_successful(
        String event_uuid,String host_uuid,
        ArrayList<String> children_event_host_uuids)
    {
      
        RalphServiceActions.ServiceAction service_action = 
            new RalphServiceActions.ReceiveFirstPhaseCommitMessage(
                this,event_uuid,host_uuid,true,children_event_host_uuids);
        
        _thread_pool.add_service_action(service_action);
    }
    

    /**
       @param {uuid} event_uuid --- The uuid of the event associated
       with this message.  (Used to index into local endpoint's
       active event map.)

       @param {uuid} host_uuid --- The endpoint
       that tried to perform the first phase of the commit.  (Other
       endpoints may have forwarded the result on to us.)
    */
    public void _receive_first_phase_commit_unsuccessful(
        String event_uuid,String host_uuid)
    {
    	RalphServiceActions.ServiceAction service_action = 
            new RalphServiceActions.ReceiveFirstPhaseCommitMessage(
                this, event_uuid,host_uuid,false,null);
                
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

     @param {ActiveEvent} active_event --- The active event that
     is requesting the message to be sent.

     @param {VariableStack} sequence_local_var_stack --- We convert
     all changes that we have made to both peered data and sequence
     local data to maps of deltas so that the partner endpoint can
     apply the changes.  We use the sequence_local_store to get
     changes that invalidation_listener has made to sequence local
     data.  (For peered data, we can just use
     self._global_var_store.)

     @param{Builder} serialized_results --- Can be null.  Serialized
     value of all returned objects.
     
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
        ActiveEvent active_event,
        VariablesProto.Variables.Builder rpc_variables,
        VariablesProto.Variables.Builder serialized_results,
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
        if (serialized_results != null)
            request_sequence_block_msg.setReturnObjs(serialized_results);
        
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
        ActiveEvent active_event)
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
        backout_commit_request.setEventUuid(event_uuid_builder);
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
       This method takes in a name for a method to execute on a local
       endpoint and executes it.  The compiler should override this
       method so that it will call correct internal method name for
       each string (and pass correct arguments to it).
       
       @param {String} to_exec_internal_name --- The internal
       name of the method to execute on this endpoint. 

       @param {_ActiveEvent object} active_event --- The active event
       object that to_exec should use for accessing endpoint data.

       @param {_ExecutingEventContext} ctx ---

       @param {result_queue or None} --- This value should be
       non-None for endpoint-call initiated events.  For endpoint
       call events, we wait for the endpoint to check if any of the
       peered data that it modifies also need to be modified on the
       endpoint's partner (and wait for partner to respond).  (@see
       discussion in waldoActiveEvent.wait_if_modified_peered.)  When
       finished execution, put wrapped result in result_queue.  This
       way the endpoint call that is waiting on the result can
       receive it.  Can be None only for events that were initiated
       by messages (in which the modified peered data would already
       have been updated).
    
       @param {*args} to_exec_args ---- Any additional arguments that
       get passed to the closure to be executed.
    */
    protected abstract RalphObject _handle_rpc_call(
        String to_exec_internal_name,ActiveEvent active_event,
        ExecutingEventContext ctx,
        Object...to_exec_args)
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException;

    /**
       Just calls into _handle_rpc_calls.
     */
    public void handle_rpc_call(
        String to_exec_internal_name,ActiveEvent active_event,
        ExecutingEventContext ctx,
        Object...args)
        throws ApplicationException, BackoutException, NetworkException,StoppedException
    {
        RalphObject result = null;
        try
        {
            result = _handle_rpc_call(
                to_exec_internal_name,active_event, ctx,args);
        }
        catch (BackoutException _ex)
        {
            active_event.put_exception(_ex);
            throw _ex;
        }
        catch (NetworkException _ex)
        {
            active_event.put_exception(_ex);
            throw _ex;
        }
        catch (Exception _ex)
        {
            //# ApplicationExceptions should be backed
            //# out and the partner should be
            //# notified
            System.out.println("\nHere was exception");
            _ex.printStackTrace();
            System.out.println("\n");
            active_event.put_exception(_ex);
            // FIXME: fill in backtrace for application exception.
            throw new ApplicationException("Caught application exception");
        }

        // tell other side that the rpc call has completed
        ctx.hide_sequence_completed_call(this, active_event,result);
    }
}
