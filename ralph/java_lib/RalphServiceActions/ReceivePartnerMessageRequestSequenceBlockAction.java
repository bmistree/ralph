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
public class ReceivePartnerMessageRequestSequenceBlockAction extends
                                                             ServiceAction 
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
            String uuid = partner_request_block_msg.getEventUuid().getData();
            String priority = partner_request_block_msg.getPriority().getData();
            ralph.ActiveEvent evt =
                local_endpoint._act_event_map.get_or_create_partner_event(uuid,priority);
            
            evt.recv_partner_sequence_call_msg(partner_request_block_msg);
			
            Util.logger_warn("May want to catch exceptions in Ralph to hide stack trace");
        }
        catch (RalphExceptions.StoppedException stopped_excep)
        {
            //  # FIXME: Think through, should I send message to other
            //  # side that I am stopped?  Right now, I don't think that I
            //  # need to.
            return;
        }
			
//        except Exception as ex:
//            # if hasattr(ex,'ralph_handled'):
//            #     # Already processed exception in put exception
//            #     return
//            raise
//

    }

}
