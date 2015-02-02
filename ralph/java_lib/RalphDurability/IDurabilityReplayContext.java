package RalphDurability;

import java.util.List;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;

import ralph.Endpoint;
import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.ActiveEvent;
import ralph.ExecutionContext.ExecutionContext;


/**
   When we replay for durability, we need to ensure that replayed
   endpoints all have same uuids as they did the first time we went
   through the code.  This context keeps track of the UUIDs to assign
   to endpoints as we're constructing them.  Additionally, when
   executing rpcs to other sides, it produces the rpc arguments that
   should be returned.
 */
public interface IDurabilityReplayContext
{
    /**
       When code that we are replaying generates an endpoint, that
       endpoint registers itself with the replay context so that a
       call to all_generated_endpoints will return it.
     */
    public void register_endpoint(Endpoint new_endpoint);
    
    /**
       Returns the next endpoint uuid an endpoint should use during
       construction.  (Side effect is that we pop the next uuid from
       queue of uuids.  Ie., two consecutive calls to next_rpc_results
       could return different uuids.)
     */
    public String next_endpt_uuid();

    /**
       When issue RPC to other side, durability logs the results of
       that RPC call.  To replay, we return the rpc results generated
       in order.  (Side effect is that we pop the next rpc result from
       queue of rpc results.  Ie., two consecutive calls to
       next_rpc_results should return different objects.)
     */
    public RalphObject issue_rpc(
        RalphGlobals ralph_globals, ExecutionContext exec_ctx);
}