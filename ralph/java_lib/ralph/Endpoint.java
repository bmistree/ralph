package ralph;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
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

import RalphDurability.IDurabilityContext;
import ralph.MessageSender.IMessageSender;
import ralph.ExecutionContext.ExecutionContext;


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
    public final ExecutionContextMap exec_ctx_map;
    public final ThreadPool _thread_pool;
    public final String _uuid;
    
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
        
       @param{EndpointConstructorObj} endpoint_constructor_obj --- Can
       be used to instantiate a version of this class, using construct
       method.

       @param {DurabilityContext} durability_context --- Can be null,
       eg., if durability is off.
    */
    public Endpoint (
        RalphGlobals ralph_globals,
        EndpointConstructorObj endpoint_constructor_obj,
        IDurabilityContext durability_context)
    {
        this (
            ralph_globals, endpoint_constructor_obj,
            durability_context,ralph_globals.generate_local_uuid());
    }
    
    public Endpoint (
        RalphGlobals ralph_globals,
        EndpointConstructorObj endpoint_constructor_obj,
        IDurabilityContext durability_context,String endpt_uuid)
    {
        _uuid = endpt_uuid;
        this.ralph_globals = ralph_globals;

        _clock = ralph_globals.clock;
        exec_ctx_map = new ExecutionContextMap(ralph_globals,this);

        _thread_pool = ralph_globals.thread_pool;
        ralph_globals.all_endpoints.add_endpoint(this);

        _host_uuid = ralph_globals.host_uuid;

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
        ExecutionContext exec_ctx)
    {
        return new InternalServiceReference(
            ralph_globals.host_uuid, _uuid);
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
        String remote_host_uuid = general_msg.getSenderHostUuid().getData();
            
        long tstamp = general_msg.getTimestamp();
        _clock.check_update_timestamp(tstamp);

        if (general_msg.hasRequestSequenceBlock())
        {
            RalphServiceActions.ServiceAction service_action =  
                new RalphServiceActions.ReceivePartnerMessageRequestSequenceBlockAction(
                    this, general_msg.getRequestSequenceBlock(), remote_host_uuid);
            _thread_pool.add_service_action(service_action);
        }
        else if (general_msg.hasFirstPhaseResult())
        {
            PartnerFirstPhaseResultMessage fpr = general_msg.getFirstPhaseResult();
            String event_uuid = fpr.getEventUuid().getData();
            String result_initiator_host_uuid = fpr.getSendingHostUuid().getData();
            
            if (general_msg.getFirstPhaseResult().getSuccessful())
            {	
                Set<String> children_event_host_uuids = new HashSet<String>();
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
        Set<String> children_event_host_uuids)
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
        String to_exec_method_name, ExecutionContext exec_ctx,
        Object...to_exec_args)
        throws ApplicationException, BackoutException, NetworkException;

    /**
       Just calls into _handle_rpc_calls.
     */
    public void handle_rpc_call(
        String to_exec_method_name, ExecutionContext exec_ctx,
        Object...args)
        throws ApplicationException, BackoutException, NetworkException
    {
        RalphObject result = null;
        try
        {
            result = _handle_rpc_call(
                to_exec_method_name,exec_ctx,args);
        }
        catch (BackoutException _ex)
        {
            exec_ctx.curr_act_evt().put_exception(_ex);
            throw _ex;
        }
        catch (NetworkException _ex)
        {
            exec_ctx.curr_act_evt().put_exception(_ex);
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
            exec_ctx.curr_act_evt().put_exception(_ex);
            // FIXME: fill in backtrace for application exception.
            throw new ApplicationException("Caught application exception");
        }

        // tell other side that the rpc call has completed
        exec_ctx.message_sender().hide_sequence_completed_call(
            exec_ctx, result);
    }
}
