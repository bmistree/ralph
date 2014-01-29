package RalphServiceActions;

import ralph.Util;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;

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
    private ralph.Endpoint local_endpoint = null;
    private PartnerRequestSequenceBlock partner_request_block_msg;
	
    public ReceivePartnerMessageRequestSequenceBlockAction(
        ralph.Endpoint _local_endpoint,
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
            ralph.ActiveEvent evt = null;

            if (partner_request_block_msg.hasReplyToUuid())
            {
                // means that this message was a response to an rpc
                // issued from an ActiveEvent on local endpoint.
                // Importantly, that ActiveEvent (if it was atomic)
                // may have been backed out by the time we got the
                // response.  in this case, we can just drop the
                // response instead of creating a new active event to
                // service it.
                evt = local_endpoint._act_event_map.get_event(uuid);

                if (evt == null)
                    return;
            }
            else
            {
                evt = local_endpoint._act_event_map.get_or_create_partner_event(
                    uuid,priority,atomic);
            }
            
            evt.recv_partner_sequence_call_msg(local_endpoint,partner_request_block_msg);
        }
        catch (RalphExceptions.StoppedException stopped_excep)
        {
            //  # FIXME: Think through, should I send message to other
            //  # side that I am stopped?  Right now, I don't think that I
            //  # need to.
            return;
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
			
//        except Exception as ex:
//            # if hasattr(ex,'ralph_handled'):
//            #     # Already processed exception in put exception
//            #     return
//            raise
//
    }

}
