package ralph.MessageSender;

import java.util.List;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

import RalphCallResults.MessageCallResultObject;

import ralph.RalphObject;
import ralph.Endpoint;
import ralph.ExecutionContext.ExecutionContext;


public interface IMessageSender
{
    /**
       When a sequence completes not on the endpoint that began the
       sequence, we must send a parting message so that the root
       endpoint can continue running.  This method sends that
       message.

       @param result --- result may be null if rpc call does not
       return anything.
    */
    public void hide_sequence_completed_call(
        Endpoint endpoint, ExecutionContext exec_ctx, RalphObject result)
        throws NetworkException, ApplicationException, BackoutException;


    /**
       @param {String or None} func_name --- When func_name is None,
       then sending to the other side the message that we finished
       performing the requested block.  In this case, we do not need
       to add result_queue to waiting queues.

       @param {bool} first_msg --- True if this is the first message
       in a sequence that we're sending.  Necessary so that we can
       tell whether or not to force sending sequence local data.

       @param {List or null} args --- The positional arguments
       inserted into the call as an rpc.  Includes whether the
       argument is a reference or not (ie, we should update the
       variable's value on the caller).  Note that can be null if we
       have no args to pass back (or if is a sequence completed call).

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
        Endpoint endpoint, ExecutionContext exec_ctx,
        String func_name, boolean first_msg,List<RalphObject> args,
        RalphObject result)
        throws NetworkException, ApplicationException, BackoutException;
}