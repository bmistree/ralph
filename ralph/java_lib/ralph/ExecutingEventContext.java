package ralph;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import java.util.concurrent.ArrayBlockingQueue;

import RalphDeserializer.Deserializer;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;
import ralph_protobuffs.VariablesProto;

import RalphCallResults.MessageCallResultObject;


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
    private java.util.Stack<String> to_reply_with_uuid = new java.util.Stack<String>();
    
    /**
       If this context was created from an rpc call from another
       endpoint, then we keep track of the RalphObjects that were
       passed into the RPC as arguments.  We do this because we may
       have to return one or more of these objects as references when
       return result of rpc.
     */
    private ArrayList<RalphObject> args_to_reply_with = null;
    
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

    public static void serialize_as_rpc_arg(
        ActiveEvent active_event, RalphObject to_serialize,
        VariablesProto.Variables.Any.Builder any_builder) throws BackoutException
    {
        if (to_serialize == null)
        {
            any_builder.setVarName("");
            any_builder.setIsTvar(false);
            return;
        }

        to_serialize.serialize_as_rpc_arg(active_event,any_builder);
    }
    
    /**
       Use this constructor when creating an event context as a result
       of an rpc call from another node.  Keeps track of which objects
       need to return from the rpc call (in array list).
     */
    public ExecutingEventContext (
        VariableStack _var_stack,
        ArrayList<RalphObject> _args_to_reply_with)
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
        to_reply_with_uuid.push(_to_reply_with_uuid);
    }
    public String get_to_reply_with()
    {
        return to_reply_with_uuid.peek();
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
        if (! to_reply_with_uuid.empty())
            to_reply_with_uuid.pop();
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

       @param result --- result may be null if rpc call does not
       return anything.
    */
    public void hide_sequence_completed_call(
        Endpoint endpoint, ActiveEvent active_event,RalphObject result)
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
            RalphObject arg = args_to_reply_with.get(i);
            args_to_reply_with.set(i,null);
        }
        
        hide_partner_call(
            endpoint,active_event,
            null,  // no function name
            false, // not first msg sent
            args_to_reply_with, result);
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

       @param {RalphObject} result --- If this is the reply to an RPC
       that returns a value, then result is non-null.  If it's the
       first request, then this value is null.

       The local endpoint is requesting its partner to call some
       sequence block.

       @returns {RalphObject} --- If this is not the reply to an rpc
       request and the method called returns a value, then this
       returns it.  Otherwise, null.
       
       * @throws NetworkException 
       * @throws ApplicationException 
       * @throws BackoutException 
       */
    public RalphObject hide_partner_call(
        Endpoint endpoint, ActiveEvent active_event,
        String func_name, boolean first_msg,ArrayList<RalphObject> args,
        RalphObject result)
        throws NetworkException, ApplicationException, BackoutException,StoppedException
    {
    	ArrayBlockingQueue<MessageCallResultObject> threadsafe_unblock_queue = 
            new ArrayBlockingQueue<MessageCallResultObject> (Util.SMALL_QUEUE_CAPACITIES);

        boolean partner_call_requested =
            active_event.issue_partner_sequence_block_call(
                endpoint,this, func_name, threadsafe_unblock_queue, first_msg,args,
                result);
        
    	if (! partner_call_requested)
    	{
            //# already backed out.  did not schedule message.  raise
            //# exception
            throw new BackoutException();
    	}
        
        // do not wait on result of call if it was the final return of
        // the call.
    	if (func_name == null)
            return null;
        
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
        
        //# send more messages
        String to_exec_next = queue_elem.to_exec_next_name_msg_field;

        if (to_exec_next != null)
        {
            ExecutingEvent.static_run(
                endpoint,to_exec_next, active_event, this,false);
        }
        else
        {
            //# end of sequence: reset to_reply_with_uuid in context.  we do
            //# this so that if we go on to execute another message sequence
            //# following this one, then the message sequence will be viewed as
            //# a new message sequence, rather than the continuation of a
            //# previous one.
            reset_to_reply_with();
        }

        // if this was the response to an rpc that returned a value,
        // then return it here.
        if (queue_elem.returned_objs != null)
        {
            VariablesProto.Variables returned_objs_proto =
                queue_elem.returned_objs;
            
            // FIXME: probably do not need to use a list for returned
            // objects.  Could just use a single return value.
            List<RalphObject> returned_objs =
                ExecutingEventContext.deserialize_variables_list(
                    returned_objs_proto,endpoint.ralph_globals);
            
            if (returned_objs.size() != 1)
            {
                Util.logger_assert(
                    "If RPC returns variable, should only return one.  " +
                    returned_objs.size());
            }

            RalphObject to_return = returned_objs.get(0);
            if (to_return == null)
            {
                // when other side returns a value, the caller expects
                // the returned value to be wrapped (ie, accessible
                // only after a call to get_val).  In this case then
                // we can just wrap the null in a simple
                // NonAtomicNumberVariable... lightweight and will
                // still return null when its get_val is called.
                to_return = new Variables.NonAtomicNumberVariable(
                    false,null,endpoint.ralph_globals);
            }
            return to_return;
        }
        return null;
    }

    /**
       Takes variables and returns their deserialized forms as a map.
       Index of map is variable name; value of map is object.
     */
    public static HashMap<String,RalphObject> deserialize_variables_map(
        VariablesProto.Variables variables, RalphGlobals ralph_globals)
    {
        HashMap<String,RalphObject> to_return =
            new HashMap<String,RalphObject>();
        
        // run through variables and turn into map
        for (VariablesProto.Variables.Any variable : variables.getVarsList() )
        {
            String var_name = variable.getVarName();
            RalphObject lo = deserialize_any(variable,ralph_globals);
            to_return.put(var_name,lo);
        }

        return to_return;
    }

    public static ArrayList<RalphObject> deserialize_rpc_args_list(
        VariablesProto.Variables variables,RalphGlobals ralph_globals)
    {
        ArrayList<RalphObject> to_return = new ArrayList<RalphObject>();

        // run through variables and turn into map
        for (VariablesProto.Variables.Any variable : variables.getVarsList())
        {
            RalphObject lo = deserialize_any(variable,ralph_globals);
            to_return.add(lo);
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
        VariablesProto.Variables variables, RalphGlobals ralph_globals)
    {
        ArrayList<RalphObject> to_return = new ArrayList<RalphObject>();

        // run through variables and turn into map
        for (VariablesProto.Variables.Any variable : variables.getVarsList())
        {
            RalphObject lo = deserialize_any(variable,ralph_globals);
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
        VariablesProto.Variables.Any variable,RalphGlobals ralph_globals)
    {
        Deserializer deserializer = Deserializer.get_instance();
        return deserializer.deserialize(variable,ralph_globals);
    }
}
