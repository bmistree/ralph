package ralph;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
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
import ralph_protobuffs.UtilProto.Timestamp;
import ralph_protobuffs.UtilProto.UUID;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

import RalphAtomicWrappers.BaseAtomicWrappers;

import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;

import RalphDurability.DurabilityContext;
import ralph.MessageSender.IMessageSender;


/**
 *
 All methods that begin with _receive, are called by other
 endpoints or from connection object's receiving a message from
 partner endpoint.

 All methods that begin with _forward or _send are called from
 active events on this endpoint.
 *
 */
public abstract class Endpoint implements IReference
{
    public final String _host_uuid;
	
    private final LamportClock _clock;

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
	
    public final ThreadPool _thread_pool;
    private final AllEndpoints _all_endpoints;

    public final String _uuid;
    
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

    public final RalphGlobals ralph_globals;
    
    /**
       @param {RalphGlobals} ralph_globals --- Contains common utilities
       needed by emitted code, such as WaldoNumVariable
        
       @param {uuid} host_uuid --- The uuid of the host this endpoint
       lives on.
        
       @param {ConnectionObject} conn_obj --- Used to write messages
       to partner endpoint.

       @param{EndpointConstructorObj} endpoint_constructor_obj --- Can
       be used to instantiate a version of this class, using construct
       method.

       @param {DurabilityContext} durability_context --- Can be null,
       eg., if durability is off.
    */
    public Endpoint (
        RalphGlobals ralph_globals,RalphConnObj.ConnectionObj conn_obj,
        EndpointConstructorObj endpoint_constructor_obj,
        DurabilityContext durability_context)
    {
        this (
            ralph_globals, conn_obj, endpoint_constructor_obj,
            durability_context,ralph_globals.generate_local_uuid());
    }
    
    public Endpoint (
        RalphGlobals ralph_globals,RalphConnObj.ConnectionObj conn_obj,
        EndpointConstructorObj endpoint_constructor_obj,
        DurabilityContext durability_context,String endpt_uuid)
    {
        _uuid = endpt_uuid;
        this.ralph_globals = ralph_globals;

        _clock = ralph_globals.clock;
        _act_event_map =
            new ActiveEventMap(
                this,_clock,ralph_globals.deadlock_avoidance_algorithm,
                ralph_globals);
        _conn_obj = conn_obj;

        _thread_pool = ralph_globals.thread_pool;
        _all_endpoints = ralph_globals.all_endpoints;
        _all_endpoints.add_endpoint(this);

        _host_uuid = ralph_globals.host_uuid;

        _conn_obj.register_endpoint(this);

        // tell other side my uuid... skip initialization.
        _notify_partner_ready();
        
        if (durability_context != null)
        {
            durability_context.add_endpt_created_info(
                _uuid,endpoint_constructor_obj.get_canonical_name());
        }
    }

    
    public String uuid()
    {
        return _uuid;
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

       Note that the arguments to this method are unused.  This method
       has the signature that it does simply because it's being called
       from emitted code, which always expects to call with a message
       sender and active event.
     */
    public InternalServiceReference rpc_reference(
        IMessageSender message_sender,ActiveEvent active_event)
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

    /**
       FIXME: Should I remove this?  Is it still useful?
     */
    protected NonAtomicInternalList<Double,Double> _produce_range(
        Double start,Double end, Double increment)
    {
        List<RalphObject<Double,Double>> init_val =
            new ArrayList<RalphObject<Double,Double>>();
        for (int i = start.intValue(); i < end.intValue();
             i = i + increment.intValue())
        {
            init_val.add(
                new Variables.NonAtomicNumberVariable(
                    false,new Double(i),ralph_globals));
        }
        NonAtomicInternalList<Double,Double> to_return =
            new NonAtomicInternalList(ralph_globals,
            new ListTypeDataWrapperFactory<Double,Double>(java.lang.Double.class),
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
        else if (general_msg.hasFirstPhaseResult())
        {
            PartnerFirstPhaseResultMessage fpr = general_msg.getFirstPhaseResult();
            String event_uuid = fpr.getEventUuid().getData();
            String result_initiator_host_uuid = fpr.getSendingHostUuid().getData();
            
            if (general_msg.getFirstPhaseResult().getSuccessful())
            {	
                List<String> children_event_host_uuids = new ArrayList<String>();
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
            String event_uuid =
                general_msg.getCommitRequest().getEventUuid().getData();
            String root_host_uuid =
                general_msg.getCommitRequest().getRootHostUuid().getData();
            long root_timestamp =
                general_msg.getCommitRequest().getRootTimestamp();

            String application_uuid =
                general_msg.getCommitRequest().getApplicationUuid().getData();
            String event_name =
                general_msg.getCommitRequest().getEventName();
            
            RalphServiceActions.ServiceAction service_action = 
                new RalphServiceActions.ReceiveRequestCommitAction(
                    this,event_uuid,root_timestamp,root_host_uuid,
                    application_uuid,event_name);
            
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
        List<String> children_event_host_uuids)
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
        List<String> children_event_host_uuids)
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
    */
    public void _send_partner_message_sequence_block_request(
        PartnerRequestSequenceBlock request_sequence_block_msg)
    {
    	GeneralMessage.Builder general_message =
            GeneralMessage.newBuilder();
    	general_message.setTimestamp(_clock.get_int_timestamp());
        
    	general_message.setRequestSequenceBlock(request_sequence_block_msg);
    	_conn_obj.write(general_message.build(),this);
    }    

    /**
       @param {UUID} active_event_uuid --- The uuid of the event we
       will forward a commit to our partner for.
    */
    public void _forward_commit_request_partner(
        String active_event_uuid,long root_timestamp,
        String root_host_uuid,String application_uuid,
        String event_name)
    {
        //# FIXME: may be a way to piggyback commit with final event in
        //# sequence.
    	GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
    	general_message.setTimestamp(_clock.get_int_timestamp());
    	PartnerCommitRequest.Builder commit_request_msg =
            PartnerCommitRequest.newBuilder();

        // event uuid
    	UtilProto.UUID.Builder event_uuid_msg = UtilProto.UUID.newBuilder();
    	event_uuid_msg.setData(active_event_uuid);
    	commit_request_msg.setEventUuid(event_uuid_msg);

        // root host uuid
    	UtilProto.UUID.Builder root_host_uuid_msg =
            UtilProto.UUID.newBuilder();
    	root_host_uuid_msg.setData(root_host_uuid);
    	commit_request_msg.setRootHostUuid(root_host_uuid_msg);

        // root timestamp
        commit_request_msg.setRootTimestamp(root_timestamp);

        // application_uuid
        UtilProto.UUID.Builder application_uuid_msg =
            UtilProto.UUID.newBuilder();
        application_uuid_msg.setData(application_uuid);
        commit_request_msg.setApplicationUuid(application_uuid_msg);
        
        // event_name
        commit_request_msg.setEventName(event_name);
        
        // actually populate general message
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

    
    //# Builtin Endpoint methods

    /**
       This method takes in a name for a method to execute on a local
       endpoint and executes it.  The compiler should override this
       method so that it will call correct internal method name for
       each string (and pass correct arguments to it).

       @param {String} to_exec_method_name --- The name of the method
       to execute on this endpoint.  Note that there had originally
       been a distinction between internal, mangled, method names and
       external method names.  This distinction no longer applies, and
       can use one for the other.

       @param {_ActiveEvent object} active_event --- The active event
       object that to_exec should use for accessing endpoint data.

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
        String to_exec_method_name,ActiveEvent active_event,
        IMessageSender message_sender,  Object...to_exec_args)
        throws ApplicationException, BackoutException, NetworkException;

    /**
       Just calls into _handle_rpc_calls.
     */
    public void handle_rpc_call(
        String to_exec_method_name,ActiveEvent active_event,
        IMessageSender message_sender,  Object...args)
        throws ApplicationException, BackoutException, NetworkException
    {
        RalphObject result = null;
        try
        {
            result = _handle_rpc_call(
                to_exec_method_name,active_event, message_sender,args);
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
        message_sender.hide_sequence_completed_call(this, active_event,result);
    }
}
