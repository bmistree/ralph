package RalphDurability;

import java.util.List;
import java.util.ArrayList;

import ralph.Endpoint;

import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.EndpointUUIDConstructorNamePair;


public class DurabilityReplayContext implements IDurabilityReplayContext
{
    private final List<Endpoint> endpts_created_during_event =
        new ArrayList<Endpoint>();

    private final DurabilityPrepare prepare_msg;
    private int next_endpt_uuid_index = 0;
    
    public DurabilityReplayContext(DurabilityPrepare prepare_msg)
    {
        this.prepare_msg = prepare_msg;
    }
    
    @Override
    public void register_endpoint(Endpoint new_endpt)
    {
        endpts_created_during_event.add(new_endpt);
    }
    
    /**
       Returns the next endpoint uuid an endpoint should use during
       construction.
     */
    @Override
    public String next_endpt_uuid()
    {
        EndpointUUIDConstructorNamePair pair =
            prepare_msg.getEndpointsCreated(next_endpt_uuid_index);
        ++next_endpt_uuid_index;

        return pair.getEndptUuid().getData();
    }

    /**
       Return a list of all endpoints that were created as part of
       replaying this event.
     */
    @Override
    public List<Endpoint> all_generated_endpoints()
    {
        return endpts_created_during_event;
    }
}