package RalphServiceActions;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;

import ralph.Util;
import ralph.ActiveEvent;
import ralph.Endpoint;
import ralph.RalphGlobals;
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
    private final RalphGlobals ralph_globals;
    private final PartnerRequestSequenceBlock partner_request_block_msg;
    private final String remote_host_uuid;

    public ReceivePartnerMessageRequestSequenceBlockAction(
        RalphGlobals ralph_globals,
        PartnerRequestSequenceBlock partner_request_block_msg,
        String remote_host_uuid)
    {
        this.ralph_globals = ralph_globals;
        this.partner_request_block_msg = partner_request_block_msg;
        this.remote_host_uuid = remote_host_uuid;
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
                exec_ctx = ralph_globals.all_ctx_map.get(uuid);
                if (exec_ctx == null)
                    return;
            }
            else
            {
                Endpoint local_endpt =
                    ralph_globals.all_endpoints.get_endpoint_if_exists(
                        partner_request_block_msg.getTargetEndpt().getData());

                if (local_endpt == null)
                {
                    Util.logger_assert(
                        "Received a message request for an unknown endpoint.");
                }

                exec_ctx =
                    local_endpt.exec_ctx_map.get_or_create_partner_live_exec_ctx(
                        uuid, priority, atomic, event_entry_point_name,
                        remote_host_uuid);
            }
            String target_endpt_uuid =
                partner_request_block_msg.getTargetEndpt().getData();

            exec_ctx.curr_act_evt().recv_partner_sequence_call_msg(
                target_endpt_uuid, partner_request_block_msg,
                remote_host_uuid);
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
