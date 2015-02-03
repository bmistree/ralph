package RalphServiceActions;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;

import ralph.Util;
import ralph.ActiveEvent;
import ralph.Endpoint;
import ralph.DurabilityInfo;
import ralph.ExecutionContext.ExecutionContext;
import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityContext;



/**
   @param {_Endpoint object} local_endpoint --- The endpoint that
   received a message requesting it to execute one of its
   sequence blocks.

   @param {PartnerRequestSequenceBlockMessage.proto}
   partner_request_block_msg
   *
   */
public class ReceivePartnerMessageRequestSequenceBlockAction
    extends ServiceAction 
{
    private final Endpoint local_endpoint;
    private final PartnerRequestSequenceBlock partner_request_block_msg;
	
    public ReceivePartnerMessageRequestSequenceBlockAction(
        Endpoint _local_endpoint,
        PartnerRequestSequenceBlock _partner_request_block_msg)
    {
        local_endpoint = _local_endpoint;
        partner_request_block_msg = _partner_request_block_msg;
    }
	
    @Override
    public void run() 
    {
        try
        {
            boolean atomic = partner_request_block_msg.getTransaction();
            String uuid = partner_request_block_msg.getEventUuid().getData();
            String priority = partner_request_block_msg.getPriority().getData();
            String event_entry_point_name = "- - finish block event name- - ";
            if (partner_request_block_msg.hasNameOfBlockRequesting())
            {
                event_entry_point_name =
                    partner_request_block_msg.getNameOfBlockRequesting();
            }


            ExecutionContext exec_ctx = null;
            if (partner_request_block_msg.hasReplyToUuid())
            {
                // means that this message was a response to an rpc
                // issued from an ActiveEvent on local endpoint.
                // Importantly, that ActiveEvent (if it was atomic)
                // may have been backed out by the time we got the
                // response.  in this case, we can just drop the
                // response instead of creating a new active event to
                // service it.
                exec_ctx = local_endpoint.exec_ctx_map.get_exec_ctx(uuid);

                if (exec_ctx == null)
                    return;
            }
            else
            {
                Util.logger_warn(
                    "FIXME: check if need to generate durability contexts " +
                    "in ReceivePartnerMessageRequestSequenceBlockAction.");

                IDurabilityContext durability_ctx = null;
                if (DurabilityInfo.instance.durability_saver != null)
                    durability_ctx = new DurabilityContext(uuid);
                
                exec_ctx =
                    local_endpoint.exec_ctx_map.get_or_create_partner_exec_ctx(
                        uuid,priority,atomic,event_entry_point_name,
                        durability_ctx);
            }

            exec_ctx.current_active_event().recv_partner_sequence_call_msg(
                local_endpoint,partner_request_block_msg);
        }
        catch (RalphExceptions.BackoutException _ex)
        {
            // do not have to do anything in this case.
            return;
        }
        catch (Exception _ex)
        {
            // should have already been caught.  Print stack trace,
            // just in case it isn't (for compiler debugging).
            Util.logger_warn(
                "Warning: ensure that caught and processed exception " +
                "with stack trace below: ");
            _ex.printStackTrace();
        }
    }
}
