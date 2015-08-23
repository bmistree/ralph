package ralph.MessageSender;

import java.util.List;
import java.util.Stack;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

import RalphCallResults.MessageCallResultObject;

import ralph.Util;
import ralph.RalphObject;
import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.RPCDeserializationHelper;
import ralph.ExecutingEvent;
import ralph.MVar;
import ralph.PartnerRequestSequenceBlockProducer;
import ralph.ExecutionContext.ExecutionContext;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;



/**
   Sends rpc calls through active events instead of simulating their
   results during durability replay.
 */
public class LiveMessageSender implements IMessageSender
{
    /** When we receive an RPC request and have to issue a response,
     * we need to keep track of who we're supposed to reply to.  See
     * additional notes on message_reply_stack.
     */
    private class MessageStackElement
    {
        public final String to_reply_with_uuid;
        public final String remote_host_uuid;

        public MessageStackElement(
            String to_reply_with_uuid, String remote_host_uuid)
        {
            this.to_reply_with_uuid = to_reply_with_uuid;
            this.remote_host_uuid = remote_host_uuid;
        }
    }

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
    private final Stack<MessageStackElement> message_reply_stack =
        new Stack<MessageStackElement>();
    private boolean msg_send_initialized_bit = false;

    public void push_message_reply_stack(
        String to_reply_with_uuid, String remote_host_uuid)
    {
        message_reply_stack.push(
            new MessageStackElement(to_reply_with_uuid, remote_host_uuid));
    }

    public MessageStackElement get_message_stack_element()
    {
        return message_reply_stack.peek();
    }

    /**
     * Each time we finish a message sequence, we reset
     message_reply_stack.  This is so that if we start any new message
     sequences, the calls to the message sequences will be started
     fresh instead of viewed as a continuation of the previous
     sequence.
    */
    private void pop_message_reply_stack()
    {
        if (! message_reply_stack.empty())
            message_reply_stack.pop();
    }

    /**
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

       @returns {boolean} --- The previous state of the initialized
       bit.  Can use this to test whether to initialize sequence local
       data.
    */
    public boolean set_msg_send_initialized_bit_true()
    {
        boolean prev_initialized_bit = msg_send_initialized_bit;
        msg_send_initialized_bit = true;
        return prev_initialized_bit;
    }

    @Override
    public void hide_sequence_completed_call(
        ExecutionContext exec_ctx, RalphObject result)
        throws NetworkException, ApplicationException, BackoutException
    {
        hide_partner_call(
            null, null,
            exec_ctx,
            null,  // no function name
            false, // not first msg sent
            null, // send no arguments back to other side
            result);
    }

    /**
       @param remote_host_uuid --- Can be null (eg., if this is a
       reply to an rpc).

       @param target_endpt_uuid --- Can be null (eg., if this is
       replying to an rpc).
     */
    @Override
    public RalphObject hide_partner_call(
        String remote_host_uuid, String target_endpt_uuid,
        ExecutionContext exec_ctx, String func_name,
        boolean first_msg, List<RalphObject> args,
        RalphObject result)
        throws NetworkException, ApplicationException, BackoutException
    {
    	MVar<MessageCallResultObject> result_mvar =
            new MVar<MessageCallResultObject>();

        // If the other side responds to this rpc, it will contain
        // this uuid.
        String other_side_reply_with_uuid =
            exec_ctx.ralph_globals.generate_local_uuid();

        ActiveEvent act_evt = exec_ctx.curr_act_evt();
        boolean can_issue_rpc = act_evt.note_issue_rpc(
            remote_host_uuid, other_side_reply_with_uuid, result_mvar);

        if (! can_issue_rpc)
        {
            // already backed out.  did not schedule message.  raise
            // exception
            throw new BackoutException();
        }

        String this_is_replying_to_uuid = null;
        if (! first_msg)
        {
            MessageStackElement elem = get_message_stack_element();
            this_is_replying_to_uuid = elem.to_reply_with_uuid;
            remote_host_uuid = elem.remote_host_uuid;
            pop_message_reply_stack();
        }

        PartnerRequestSequenceBlock request_sequence_block =
            PartnerRequestSequenceBlockProducer.produce_request_block(
                this_is_replying_to_uuid, func_name, args, result, act_evt,
                other_side_reply_with_uuid, target_endpt_uuid);

        exec_ctx.ralph_globals.message_manager.send_sequence_block_request_msg(
            remote_host_uuid, request_sequence_block);


        // do not wait on result of call if it was the final return of
        // the call.
    	if (func_name == null)
            return null;

        // wait on result of call
    	MessageCallResultObject mvar_elem = result_mvar.blocking_take();
    	if (mvar_elem.result_type ==
            MessageCallResultObject.ResultType.BACKOUT_BEFORE_RECEIVE_MESSAGE)
        {
            throw new BackoutException();
        }
    	else if (mvar_elem.result_type ==
                 MessageCallResultObject.ResultType.NETWORK_FAILURE)
        {
            throw new NetworkException("network failure");
        }
    	else if (mvar_elem.result_type ==
                 MessageCallResultObject.ResultType.APPLICATION_EXCEPTION)
        {
            throw new ApplicationException("application exception");
        }

    	//means that it must be a sequence message call result
        push_message_reply_stack(
            mvar_elem.reply_with_msg_field, mvar_elem.remote_host_uuid);

        //# send more messages
        String to_exec_next = mvar_elem.to_exec_next_name_msg_field;
        String next_target_endpt_uuid = mvar_elem.target_endpt_uuid;
        if (to_exec_next != null)
        {
            Endpoint endpt =
                exec_ctx.ralph_globals.all_endpoints.get_endpoint_if_exists(
                    next_target_endpt_uuid);

            ExecutingEvent.static_run(
                endpt, to_exec_next, exec_ctx, false);
        }
        else
        {
            // end of sequence: reset message_reply_stack in context.
            // we do this so that if we go on to execute another
            // message sequence following this one, then the message
            // sequence will be viewed as a new message sequence,
            // rather than the continuation of a previous one.
            pop_message_reply_stack();
        }

        // if this was the response to an rpc that returned a value,
        // then return it here.
        return RPCDeserializationHelper.return_args_to_ralph_object(
            mvar_elem.returned_objs, exec_ctx.ralph_globals,
            exec_ctx);
    }
}
