package RalphDurability;

import java.util.List;

import ralph.Endpoint;

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
       construction.
     */
    public String next_endpt_uuid();

    /**
       Return a list of all endpoints that were created as part of
       replaying this event.
     */
    public List<Endpoint> all_generated_endpoints();
}