package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import ralph.LockedVariables.LockedTextVariable;
import ralph.LockedVariables.LockedNumberVariable;
import ralph.LockedVariables.LockedTrueFalseVariable;
import ralph.LockedVariables.SingleThreadedLockedTextVariable;
import ralph.LockedVariables.SingleThreadedLockedNumberVariable;
import ralph.LockedVariables.SingleThreadedLockedTrueFalseVariable;

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
       # We can be listening to more than one open threadsafe message
       # queue.  If endpoint A waits on its partner, and, while
       # waiting, its partner executes a series of endpoint calls so
       # that another method on A is invoked, and that method calls
       # its partner again, we could be waiting on 2 different
       # message queues, each held by the same active event.  To
       # ensure that we can correctly demultiplex which queue a
       # message was intended for, each message that we send has two
       # fields, reply_with and reply_to.  reply_with tells the
       # partner endpoint that when it is done, send back a message
       # with uuid reply_with in the message's reply_to field.  (The
       # first message sent has a reply_to field of None.)  We use
       # the reply_to field to index into a map of message listening
       # queues in waldoActiveEvent._ActiveEvent.
    */
    String to_reply_with_uuid = null;
    
    boolean msg_send_initialized_bit = false;

    /**
       # if this function were called via an endpoint call on another
       # endpoint, then from_endpoint_call will be true.  We check
       # whether this function was called from another endpoint so
       # that we know whether we need to copy in reference
       # containers.  Pass lists, maps, and user structs by value
       # type across endpoint calls unless they're declared external.
    */
    boolean from_endpoint_call = false;

    
	
    public ExecutingEventContext (VariableStack _var_stack)
    {
        var_stack = _var_stack.fork_stack();
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


    public Object set_val (
        Object to_write_to, Object to_write,
        LockedActiveEvent active_event) throws BackoutException
    {
        if (LockedObject.class.isInstance(to_write_to))
        {
            ((LockedObject)to_write_to).set_val(active_event,to_write);
            return to_write_to;
        }
        return to_write;
    }

    //#### UTILITY FUNCTIONS  ####
    //# all of these could be static: they don't touch any internal
    //# state.

	
    /**
     *  @param {Anything} val --- If val is a waldo reference object,
     call get_val on it.  Otherwise, return val itself.

     Assume that we are emitting for 
     a = b
     and
     a = 4

     In the first case, we do not want to assign the reference of b
     to a, we want to assign its value.  In the second case, we
     want to assign the value of 4 to a.  We want to emit the
     following for each:

     a.set_val(_active_event,b.get_val(_active_event))
     a.set_val(_active_event,4)

     At compile time, we *do* have information on whether the rhs
     of the expression is a literal or a WaldoReference object.
     However, it's a pain to keep track of this information.
     Therefore, the compiler just uses this function at *runtime.*
     This function will either call get_val on the oject (if it's a
     WaldoReference) or just pass it through otherwise.

     @param {LockedActiveEvent or None} --- If None, then this
     is used during initialization of variables.  Means that the
     Waldo Reference should just return the actual committed value
     of the variable.  Don't wait for commit or anything else.
     * @throws BackoutException 

     */
    public Object get_val_if_waldo(
        Object val, LockedActiveEvent active_event) throws BackoutException
    {
        if (LockedObject.class.isInstance(val))
            return ((LockedObject)val).get_val(active_event);
		
        return val;		
    }


    /**
       @throws BackoutException 
       * @see turn_into_waldo_var, except that we will only turn into a
       Waldo variable if the previous value had been a Waldo variable.

       Otherwise, return the value and variable keeps Python form.
    */
    public Object turn_into_waldo_var_if_was_var(
        Object val, boolean force_copy, LockedActiveEvent active_event,
        String host_uuid, boolean new_peered,
        boolean new_multi_threaded) throws BackoutException
    {
        if (LockedObject.class.isInstance(val) ||
            HashMap.class.isInstance(val) ||
            ArrayList.class.isInstance(val))				
        {
		
            //# note: when converting from external, we may need to copy
            //# list and dict reference types so that changes to them do
            //# not interfere with actual values in Python.
            return turn_into_waldo_var(
                val,force_copy,active_event,host_uuid,new_peered,
                new_multi_threaded);
        }

        return val;
    }
		        

    /**
       @param {Anything} val

       @param {bool} force_copy

       @param {_ActiveEvent object} active_event
        
       @param {uuid} host_uuid

       @param {bool} new_peered --- True if in the case that we have
       to copy a value, the copy should be peered.  Used for loading
       arguments into sequence local data when message send is
       called.  @see convert_for_seq_local.
       * @throws BackoutException 

       @returns {WaldoVariable}

       Used when copying arguments in to function.  Compiler's caller
       can pass in Python literals or WaldoReference objects when it
       emits a function call.  However, the emitted function
       definition requires all arguments to be WaldoVariables.
       Therefore, at the beginning of each emitted function, copy all
       arguments in to WaldoVariables if they are not already.  

       If val is not a WaldoReference, then based on its type, assign
       it as the initial value of a Waldo Variable and return.

       If val is a WaldoReference, then check force_copy.  force_copy
       represents whether or not we want to make a copy of the
       WaldoReference object.  (It would be True for instance if we
       were passed in a WaldoNumber because numbers are passed by
       value; it would be false if the arg was external or if we were
       passed in a reference type.)

       If it is false, then just return val.  Otherwise, make copy.        
    */
    public LockedObject turn_into_waldo_var(
        Object val, boolean force_copy, LockedActiveEvent active_event,
        String host_uuid, boolean new_peered,
        boolean new_multi_threaded) throws BackoutException
    {
        //# FIXME: Start using some of the single threaded constructors
        //# as well.
        if (LockedObject.class.isInstance(val))
        {
            if (force_copy)
            {
                //# means that it was a WaldoVariable: just call its copy
                //# method
                LockedObject casted_val = (LockedObject)val;
                return (LockedObject) casted_val.copy(active_event, new_peered, new_multi_threaded);
            }
            //# otherwise, just return val
            return (LockedObject)val;
        }
		
        //# means that val was not a reference object.... turn it into one.
        if (new_multi_threaded)
        {
            if (String.class.isInstance(val))
            {
                return new LockedTextVariable(
                    host_uuid,new_peered,(String)val);
            }
            else if (Number.class.isInstance(val))
            {
                return new LockedNumberVariable(
                    host_uuid,new_peered,(Number)val);
            }
            else if (Boolean.class.isInstance(val))
            {
                return new LockedTrueFalseVariable(
                    host_uuid,new_peered,(Boolean)val);
            }
            else
            {
                Util.logger_assert("Still must add constructors for lists, maps, endpoints, etc.");
            }
        }
        else
        {
            if (String.class.isInstance(val))
            {
                return new SingleThreadedLockedTextVariable(
                    host_uuid, new_peered, (String)val);
            }
            else if (Number.class.isInstance(val))
            {
                return new SingleThreadedLockedNumberVariable(
                    host_uuid, new_peered, (Double)val);
            }
            else if (Boolean.class.isInstance(val))
            {
                return new SingleThreadedLockedTrueFalseVariable(
                    host_uuid,new_peered,(Boolean)val);
            }
            else if (HashMap.class.isInstance(val))
            {
                Util.logger_assert(
                    "Deprecated constructor for map");
            }
            else
            {
                Util.logger_assert(
                    "Still must add constructors for lists, endpoints, " +
                    "func objects, etc.");
            }
        }   
        return null;
    }		
	
    /**
     * turn_into_waldo_var works for all non-function types.
     function-types require additional information (which arguments
     are and are not external) to populate their ext_args_array.
     This is passed in in this function.
     * @return
     */
    public LockedObject func_turn_into_waldo_var(
        Object val, boolean force_copy, LockedActiveEvent active_event,
        String host_uuid, boolean new_peered,
        ArrayList<Object>ext_args_array, boolean new_multi_threaded)
    {
        Util.logger_assert("Not handling function objects for now.");
        return null;
    }

	
    /**
       @param {wVariable.WaldoFunctionVariable} func_obj --- The
       wrapped function that we are calling.

       @param {*args} --- The actual arguments that get passed to the
       function.
    */
    public Object call_func_obj(
        LockedActiveEvent active_event, Object func_obj,Object...args)
    {
        Util.logger_assert("Not handling function objects for now");
        return null;
    }
	

    /**
       Take whatever is in val and copy it into a sequence local
       piece of data (ie, change peered to True).  Return the copy.

       Used when loading arguments to message send into sequence
       local data space.
       * @throws BackoutException 
       */
    public LockedObject convert_for_seq_local(
        Object val, LockedActiveEvent active_event,
        String host_uuid) throws BackoutException
    {
        return turn_into_waldo_var(
            val,true,active_event,host_uuid,
            //# will be peered because used between both sides
            true,
            //# will not be multi-threaded, because only can be accessed
            //# from one thread of control at a time.
            false);
    }

    /**
     * When returning a value to non-Waldo code, need to convert the
     value to a regular python type.  This function handles that.
     * @throws BackoutException 
     */
    public Object de_waldoify(
        Object val,LockedActiveEvent active_event) throws BackoutException
    {
        if (LockedObject.class.isInstance(val))
            return ((LockedObject)val).de_waldoify(active_event);

        /*
          if isinstance(val,tuple):
          # means that we are trying to dewaldoify a function call's
          # return.  Need to dewaldo-ify each element of tuple
          return_list = []
          for item in val:
          return_list.append(de_waldoify(item,active_event))
          return tuple(return_list)
        */
        Util.logger_warn("May not be handling tuple de_waldoify");
        return val;		
    }

	
    /**
       @param *args 

       @returns {tuple or a single value}

       Take something like this: 
       1, (2,3), 4, ((5), (6))
       or
       (1, (2,3), 4, ((5), (6)))
       and turn it into
       (1, 2, 3, 4, 5, 6)

       If the length of the return tuple is just one, then just
       return that value directly.  This is so that when we are
       returning a single value, instead of returning a tuple
       containing a single value, we return the value.

       Ie, if we take in
       (1,)
       we return
       1
    */
    public ArrayList<Object> flatten_into_single_return_tuple(Object...args)
    {
        Util.logger_assert(
            "Not handling tuple return types in java branch for now.");
        return null;
    }


    /**
       If lhs is a Waldo variable, then set_val rhs's value into it
       and return True.  Otherwise, return False.  (if return False,
       then should do assignment in function calling from directly.)

       Note: to assign into a specific index of a map, list, or
       struct, use assign_on_key, below.
       * @throws BackoutException 
        
       @returns{bool} --- True if after this method lhs contains rhs.
       False otherwise.
    */
    public boolean assign(
        Object lhs, Object rhs, LockedActiveEvent active_event) throws BackoutException
    {
    	if (! LockedObject.class.isInstance(lhs))
            return false;
    	
    	((LockedObject)lhs).set_val(active_event, rhs);
    	return true;
    }

    /**
     * For bracket statements + struct statements
     * @throws BackoutException 
     */
    public boolean assign_on_key(
        Object lhs,Object key,Object rhs, LockedActiveEvent active_event) throws BackoutException
    {
        if (! LockedObject.class.isInstance(lhs))
            return false;
        
        Object raw_key = get_val_if_waldo(key,active_event);
        LockedObject locked_lhs = (LockedObject)lhs;
        
        if (LockedVarUtils.is_non_ext_text_var(lhs))
        {
            Object raw_rhs = get_val_if_waldo(rhs,active_event);
            String internal_string = (String)locked_lhs.get_val(active_event);
            String to_overwrite_string = 
                internal_string.substring(0,((Number)raw_key).intValue()) + 
                (String)raw_rhs +
                internal_string.substring(((Number) raw_key).intValue()+1);
            
            locked_lhs.set_val(active_event, to_overwrite_string);        	
        }
        else if (LockedVarUtils.is_external_text_variable(lhs))
        {
            Util.logger_assert(
                "Still have not enabled externals in java branch yet.");
        }
        else if (LockedVarUtils.is_reference_container(lhs))
        {
            //# just write the value explicitly for now.  Later, will
            //# need to check if we need to wrap it first.
            ContainerInterface ref_cont =
                (ContainerInterface) locked_lhs.get_val(active_event);
            ref_cont.set_val_on_key(active_event,raw_key,rhs);
        }
        else
        {
            ContainerInterface ref_cont =
                (ContainerInterface) locked_lhs.get_val(active_event);
            ref_cont.set_val_on_key(active_event,raw_key,rhs);
        }

        return true;
    }


    public Object get_val_on_key(
        Object to_get_from, Object key, LockedActiveEvent active_event)
        throws BackoutException
    {
    	Object raw_key = get_val_if_waldo(key,active_event);
        
    	if (String.class.isInstance(to_get_from))
            return ((String)to_get_from).charAt(((Number)key).intValue());
    	if (HashMap.class.isInstance(to_get_from))
            return ((HashMap)to_get_from).get(raw_key);
    	if (ArrayList.class.isInstance(to_get_from))
            return ((ArrayList)to_get_from).get(((Number)raw_key).intValue());
    	
    	LockedObject locked_to_get_from = (LockedObject) to_get_from;
    	//# handle text + ext text
        if (LockedVarUtils.is_non_ext_text_var(to_get_from))
        {	
            int index = ((Number)raw_key).intValue();
            String raw_string = (String)locked_to_get_from.get_val(active_event);
            return  raw_string.substring(index,index+1);
        }

        if (LockedVarUtils.is_external_text_variable(to_get_from))
        {
            /*
              if isinstance(lhs,WaldoExternalTextVariable):
              return to_get_from.get_val(active_event).get_val(active_event)[raw_key]
            */
            Util.logger_assert("Have not added externals back in");
        }
        

        //# handle internals containers
        if (LockedVarUtils.is_reference_container(to_get_from))
        {
            ContainerInterface cont = (ContainerInterface)to_get_from;
            return cont.get_val_on_key(active_event,raw_key);
        }

        // handle map, list, struct
        ContainerInterface cont =
            (ContainerInterface) locked_to_get_from.get_val(active_event);
        return cont.get_val_on_key(active_event,raw_key);
    }

    
    /**
     * When call for loop on Waldo variables, need to get item to
     iterate over
     * @return
     */
    public Iterator get_for_iter(
        Object to_iter_over, LockedActiveEvent active_event)
    {
    	Util.logger_assert("Have not converted iter in executing event yet");
    	return null;
    }
    
    
    public String to_text(
        Object what_to_call_to_text_on, LockedActiveEvent active_event)
    {
    	Util.logger_assert("Have not converted to text method in java branch yet.");
    	return null;
    }
    

    public void signal_call(
        LockedActiveEvent active_event, SignalFunction func ,Object...args)
    {
    	Util.logger_assert(
            "Have not converted signal call code in java branch yet.");
    }
    

    /**
     * Can support python lists, dicts, strings, or waldo lists, waldo maps,
     waldo texts.
     * @throws BackoutException 
        
     @returns {int}
    */
    public int handle_len(
        Object what_calling_len_on,
        LockedActiveEvent active_event) throws BackoutException
    {
    	if (HashMap.class.isInstance(what_calling_len_on))
            return ((HashMap)what_calling_len_on).size();
    	
    	if (ArrayList.class.isInstance(what_calling_len_on))
            return ((ArrayList)what_calling_len_on).size();
    	
    	if (String.class.isInstance(what_calling_len_on))
            return ((String)what_calling_len_on).length();
    	
    	LockedObject locked_what_calling_len_on =
            (LockedObject) what_calling_len_on;
    	
    	if (LockedVarUtils.is_non_ext_text_var(what_calling_len_on))
    	{
            String internal_string =
                (String)locked_what_calling_len_on.get_val(active_event);
            return internal_string.length();
    	}


    	ContainerInterface cont =
            (ContainerInterface) (locked_what_calling_len_on.get_val(active_event));
    	return cont.get_len(active_event);
    }    
    

    /**
       When a sequence completes not on the endpoint that began the
       sequence, we must send a parting message so that the root
       endpoint can continue running.  This method sends that
       message.
    */    
    public void hide_sequence_completed_call(
        Endpoint endpoint, LockedActiveEvent active_event)
    {    
        active_event.issue_partner_sequence_block_call(
            this,null,null,false,
            // no args to pass
            new ArrayList<RPCArgObject> ());
    }
        

    /**
       @param {String or None} func_name --- When func_name is None,
       then sending to the other side the message that we finished
       performing the requested block.  In this case, we do not need
       to add result_queue to waiting queues.

       @param {bool} first_msg --- True if this is the first message
       in a sequence that we're sending.  Necessary so that we can
       tell whether or not to force sending sequence local data.

       The local endpoint is requesting its partner to call some
       sequence block.
       * @throws NetworkException 
       * @throws ApplicationException 
       * @throws BackoutException 
       */
    public void hide_partner_call(
        Endpoint endpoint, LockedActiveEvent active_event,
        String func_name, boolean first_msg,ArrayList<RPCArgObject> args)
        throws NetworkException, ApplicationException, BackoutException
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
    	
    	if (func_name == null)
            return;

    	MessageCallResultObject queue_elem = null;
        try {
            queue_elem = threadsafe_unblock_queue.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

        //# apply changes to sequence variables.  Note: that the system
        //# has already applied deltas for global data.
        Util.logger_assert(
            "\nSkipped incorporating sequence local " +
            "deltas on hide_partner_call");
        // sequence_local_store.incorporate_deltas(
        //     active_event,queue_elem.sequence_local_var_store_deltas);

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
        LockedActiveEvent active_event,
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
       Call has form:
       lhs in rhs
        
       rhs can have three basic types: it can be a list, a map, or a
       string.  That means that it can either be a WaldoMapVariable,
       a WaldoListVariable, a WaldoStringVariable, or a Python
       string.

       Instead of using static type inference at compile time to
       determine, for sake of development, just doing dynamic check
       to determine which type it is and do the in processing here.

       FIXME: it is faster to do the static checks with type
       inference, etc. at compile time rather than at run time.
    */
    public boolean handle_in_check(
        Object lhs, Object rhs, LockedActiveEvent active_event)
    {
    	Util.logger_assert("Have not added in check yet");
    	return false;
    }

    
	
}
