package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;
import ralph.Variables.NonAtomicTextVariable;
import ralph.Variables.NonAtomicNumberVariable;
import ralph.Variables.NonAtomicTrueFalseVariable;
import ralph_protobuffs.VariablesProto;

import RalphCallResults.MessageCallResultObject;

import RalphCallResults.EndpointCallResultObject;
import RalphCallResults.ApplicationExceptionEndpointCallResult;
import RalphCallResults.NetworkFailureEndpointCallResult;
import RalphCallResults.EndpointCompleteCallResult;

public class ExecutingEventContext
{
    /**
       @param {VariableStack} var_stack --- Keeps track of all data
       used by executing event.
    */
    public VariableStack var_stack = null;

    /**
       We can be listening to more than one open threadsafe message
       queue.  If endpoint A waits on its partner, and, while
       waiting, its partner executes a series of endpoint calls so
       that another method on A is invoked, and that method calls
       its partner again, we could be waiting on 2 different
       message queues, each held by the same active event.  To
       ensure that we can correctly demultiplex which queue a
       message was intended for, each message that we send has two
       fields, reply_with and reply_to.  reply_with tells the
       partner endpoint that when it is done, send back a message
       with uuid reply_with in the message's reply_to field.  (The
       first message sent has a reply_to field of None.)  We use
       the reply_to field to index into a map of message listening
       queues in waldoActiveEvent._ActiveEvent.
    */
    private String to_reply_with_uuid = null;

    /**
       If this context was created from an rpc call from another
       endpoint, then we keep track of the RalphObjects that were
       passed into the RPC as arguments.  We do this because we may
       have to return one or more of these objects as references when
       return result of rpc.
     */
    private ArrayList<RPCArgObject> args_to_reply_with = null;
    
    boolean msg_send_initialized_bit = false;

    /**
       if this function were called via an endpoint call on another
       endpoint, then from_endpoint_call will be true.  We check
       whether this function was called from another endpoint so
       that we know whether we need to copy in reference
       containers.  Pass lists, maps, and user structs by value
       type across endpoint calls unless they're declared external.
    */
    boolean from_endpoint_call = false;


    /**
       Use this constructor when creating a new event context not in
       response to another endpoint's rpc request.
     */
    public ExecutingEventContext (VariableStack _var_stack)
    {
        var_stack = _var_stack.fork_stack();
    }


    /**
       Use this constructor when creating an event context as a result
       of an rpc call from another node.  Keeps track of which objects
       need to return from the rpc call (in array list).
     */
    public ExecutingEventContext (
        VariableStack _var_stack,
        ArrayList<RPCArgObject> _args_to_reply_with)
    {
        var_stack = _var_stack.fork_stack();
        args_to_reply_with = _args_to_reply_with;
    }
    
    public void set_from_endpoint_true()
    {
        from_endpoint_call =true;
    }
	
    public boolean check_and_set_from_endpoint_call_false ()
    {
        boolean to_return = from_endpoint_call;
        from_endpoint_call = false;
        return to_return;
    }
     
    /**
     * @param {uuid} to_reply_with_uuid --- @see comments above
     self.to_reply_with_uuid in __init__ method.
    */
    public void set_to_reply_with (String _to_reply_with_uuid)
    {
        to_reply_with_uuid = _to_reply_with_uuid;
    }
    public String get_to_reply_with()
    {
        return to_reply_with_uuid;
    }

    
    /**
     *  Each time we finish a message sequence, we reset
     set_to_reply_with.  This is so that if we start any new
     message sequences, the calls to the message sequences will be
     started fresh instead of viewed as a continuation of the
     previous sequence.
    */
    public void reset_to_reply_with()
    {
        set_to_reply_with(null);
    }
     
    /**
       @see emitter.emit_statement._emit_msg_seq_begin_call

       Essentially, it is difficult to keep track of whether we have
       initialized sequence local data in the presence of jumps.
       (What happens if we jump back into a message send function
       that was already initializing data?)  Use this value to test
       whether we need to initialize sequence local data or not.
    */
    public boolean set_msg_send_initialized_bit_false()
    {
        msg_send_initialized_bit = false;
        return true;
    }

    /**
       @see set_msg_send_initialized_bit_false

       @returns {Bool} --- The previous state of the initialized bit.
       Can use this to test whether to initialize sequence local data.
    */
    public boolean set_msg_send_initialized_bit_true()
    {
        boolean prev_initialized_bit = msg_send_initialized_bit;
        msg_send_initialized_bit = true;
        return prev_initialized_bit;
    }


    //#### UTILITY FUNCTIONS  ####
    //# all of these could be static: they don't touch any internal
    //# state.


    /**
       When a sequence completes not on the endpoint that began the
       sequence, we must send a parting message so that the root
       endpoint can continue running.  This method sends that
       message.
    */    
    public void hide_sequence_completed_call(
        Endpoint endpoint, ActiveEvent active_event)
        throws NetworkException, ApplicationException, BackoutException,
        StoppedException
    {
        // DEBUG: Should only call sequence completed if context was
        // begun from an rpc.  If it was begun from an rpc, should
        // have constructed it while passing in args_to_reply_with.
        if (args_to_reply_with == null)
        {
            Util.logger_assert(
                "Should not be completing a sequence call from " +
                "a context that has no remembered rpc arguments. " +
                "(Or did not use correct constructor.)");
        }
        // END DEBUG
        
        // when a sequence completes, we have to return all the rpc
        // arguments that were passed in by reference: filter out
        // args not passed by reference
        for (int i = 0; i < args_to_reply_with.size(); ++i)
        {
            RPCArgObject arg = args_to_reply_with.get(i);
            if (! arg.is_reference)
                args_to_reply_with.set(i,null);
        }

        hide_partner_call(
            endpoint,active_event,
            null,  // no function name
            false, // not first msg sent
            args_to_reply_with);
    }


    /**
       @param {String or None} func_name --- When func_name is None,
       then sending to the other side the message that we finished
       performing the requested block.  In this case, we do not need
       to add result_queue to waiting queues.

       @param {bool} first_msg --- True if this is the first message
       in a sequence that we're sending.  Necessary so that we can
       tell whether or not to force sending sequence local data.

       @param {ArrayList} args --- The positional arguments inserted
       into the call as an rpc.  Includes whether the argument is a
       reference or not (ie, we should update the variable's value on
       the caller).

       @param {boolean} transactional --- True if this call should be
       part of a transaction.  False if it's just a regular rpc.  Only
       matters if it's the first message in a sequence.  
       
       The local endpoint is requesting its partner to call some
       sequence block.
       * @throws NetworkException 
       * @throws ApplicationException 
       * @throws BackoutException 
       */
    public void hide_partner_call(
        Endpoint endpoint, ActiveEvent active_event,
        String func_name, boolean first_msg,ArrayList<RPCArgObject> args)
        throws NetworkException, ApplicationException, BackoutException,StoppedException
    {
    	ArrayBlockingQueue<MessageCallResultObject> threadsafe_unblock_queue = 
            new ArrayBlockingQueue<MessageCallResultObject> (Util.SMALL_QUEUE_CAPACITIES);

        boolean partner_call_requested =
            active_event.issue_partner_sequence_block_call(
                this, func_name, threadsafe_unblock_queue, first_msg,args);
        
    	if (! partner_call_requested)
    	{
            //# already backed out.  did not schedule message.  raise
            //# exception
            throw new BackoutException();
    	}
        
        // do not wait on result of call if it was the final return of
        // the call.
    	if (func_name == null)
            return; 

        // wait on result of call
    	MessageCallResultObject queue_elem = null;
        try {
            queue_elem = threadsafe_unblock_queue.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Util.logger_assert(
                "Did not consider effect of interruption while waiting");            
        }
    	if (queue_elem.result_type ==
            MessageCallResultObject.ResultType.BACKOUT_BEFORE_RECEIVE_MESSAGE)
        {
            throw new BackoutException();
        }
    	else if (queue_elem.result_type ==
                 MessageCallResultObject.ResultType.NETWORK_FAILURE)
        {
            throw new NetworkException("network failure");
        }
    	else if (queue_elem.result_type ==
                 MessageCallResultObject.ResultType.APPLICATION_EXCEPTION)
        {
            throw new ApplicationException("appliaction exception");
        }

    	//means that it must be a sequence message call result
    	set_to_reply_with(queue_elem.reply_with_msg_field);

        /*
        overwrite local variables with reference variables that got
        returned from variables.

        Step 1: grab arguments from message object
        Step 2: deserialize arguments
        Step 3: overwrite local arguments passed by reference.
        */

        // step 1: grab arguments from message object
        VariablesProto.Variables variables = queue_elem.returned_variables;

        // step 2: deserialize message variables
        ArrayList<RalphObject> returned_variables =
            ExecutingEventContext.deserialize_variables_list(
                variables,true,
                active_event.event_parent.local_endpoint._host_uuid);

        // step 3: actually overwrite local variables
        for (int i = 0; i < returned_variables.size(); ++i)
        {
            RalphObject lo = returned_variables.get(i);
            if (lo != null)
            {
                // will be null for arguments that did not pass as
                // references.
                RPCArgObject arg = args.get(i);
                arg.arg_to_pass.swap_internal_vals(active_event,lo);
            }
        }
        
        //# send more messages
        String to_exec_next = queue_elem.to_exec_next_name_msg_field;
        
        if (to_exec_next != null)
            ExecutingEvent.static_run(to_exec_next, active_event, this, null,false);
        else
        {
            //# end of sequence: reset to_reply_with_uuid in context.  we do
            //# this so that if we go on to execute another message sequence
            //# following this one, then the message sequence will be viewed as
            //# a new message sequence, rather than the continuation of a
            //# previous one.
            reset_to_reply_with();
        }
    }

    public Object hide_endpoint_call( 
        ActiveEvent active_event,
        ExecutingEventContext context, Endpoint endpoint_obj, String method_name,
        Object...args) throws BackoutException, NetworkException, ApplicationException
    {
    	ArrayBlockingQueue<EndpointCallResultObject> threadsafe_result_queue = 
            new ArrayBlockingQueue<EndpointCallResultObject>(Util.SMALL_QUEUE_CAPACITIES);

    	active_event.issue_endpoint_object_call(
            endpoint_obj,method_name,threadsafe_result_queue,args);

    	EndpointCallResultObject queue_elem = null;
    	
        try {
            queue_elem = threadsafe_result_queue.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Util.logger_assert(
                "Did not consider effect of interruption while waiting");            
        }

        if (ApplicationExceptionEndpointCallResult.class.isInstance(queue_elem))
        {
            ApplicationExceptionEndpointCallResult casted = 
                (ApplicationExceptionEndpointCallResult)queue_elem;
            throw new ApplicationException(casted.get_trace());
        }
        else if (NetworkFailureEndpointCallResult.class.isInstance(queue_elem))
        {
            NetworkFailureEndpointCallResult casted = 
                (NetworkFailureEndpointCallResult)queue_elem;
            throw new NetworkException("Network failure");
        }
        
        //# FIXME: there may be other errors that are not from
        //# backout...we shouldn't treat all cases of not getting a
        //# result as a backout exception
        else if (! EndpointCompleteCallResult.class.isInstance(queue_elem))
        {
            throw new BackoutException();
        }

        EndpointCompleteCallResult casted = 
            (EndpointCompleteCallResult) queue_elem;
        
        return casted.result;
    }
    

    /**
       Takes variables and returns their deserialized forms as a map.
       Index of map is variable name; value of map is object.
     */
    public static HashMap<String,RalphObject> deserialize_variables_map(
        VariablesProto.Variables variables,boolean references_only,String host_uuid)
    {
        HashMap<String,RalphObject> to_return =
            new HashMap<String,RalphObject>();
        
        
        // run through variables and turn into map
        for (VariablesProto.Variables.Any variable : variables.getVarsList() )
        {
            boolean is_reference = variable.getReference();

            if (references_only && (! is_reference))
                continue;
            
            String var_name = variable.getVarName();
            RalphObject lo = deserialize_any(variable,host_uuid);
            to_return.put(var_name,lo);
        }

        return to_return;
    }

    public static ArrayList<RPCArgObject> deserialize_rpc_args_list(
        VariablesProto.Variables variables,String host_uuid)
    {
        ArrayList<RPCArgObject> to_return = new ArrayList<RPCArgObject>();

        // run through variables and turn into map
        for (VariablesProto.Variables.Any variable : variables.getVarsList())
        {
            RalphObject lo = deserialize_any(variable,host_uuid);
            boolean is_reference = variable.getReference();
            to_return.add(new RPCArgObject(lo,is_reference));
        }

        return to_return;
    }
    
    /**
       Takes variables and returns their deserialized forms as a map.
       Index of map is variable name; value of map is object.

       Returns positional values.  If references_only is true, then
       put null values in for the non-references.
     */
    public static ArrayList<RalphObject> deserialize_variables_list(
        VariablesProto.Variables variables,boolean references_only,String host_uuid)
    {
        ArrayList<RalphObject> to_return = new ArrayList<RalphObject>();

        // run through variables and turn into map
        for (VariablesProto.Variables.Any variable : variables.getVarsList())
        {
            boolean is_reference = variable.getReference();
            
            RalphObject lo = null;
            if ((references_only &&  is_reference) ||
                (! references_only))
            {
                lo = deserialize_any(variable,host_uuid);
            }
            to_return.add(lo);
        }

        return to_return;
    }

    
    /**
       @returns {RalphObject or null} --- Returns null if the
       argument passed in was empty.  This could happen for instance
       if deserializing any corresponding to result for a
       non-passed-by-reference argument.
     */
    public static RalphObject deserialize_any(
        VariablesProto.Variables.Any variable, String host_uuid)
    {
        RalphObject lo = null;

        if (variable.hasNum())
        {
            lo = new NonAtomicNumberVariable(
                host_uuid,false,new Double(variable.getNum()));
        }
        else if (variable.hasText())
        {
            lo = new NonAtomicTextVariable(
                host_uuid,false,variable.getText());
        }
        else if (variable.hasTrueFalse())
        {
            lo = new NonAtomicTrueFalseVariable(
                host_uuid,false,new Boolean(variable.getTrueFalse()));
        }
        else if (variable.hasList())
        {
            Util.logger_assert("Have not added lists back to ralph yet.");
        }
        else if (variable.hasMap())
        {
            Util.logger_assert("Skipping locked maps");
        }
        else if (variable.hasStruct())
        {
            Util.logger_assert("Skipping locked structs.");
        }
        return lo;
    }
    
}
