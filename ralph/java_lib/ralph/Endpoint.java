package ralph;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

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
    public final String _uuid;
    
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
        exec_ctx_map = new ExecutionContextMap(ralph_globals,this);

        ralph_globals.all_endpoints.add_endpoint(this);

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
