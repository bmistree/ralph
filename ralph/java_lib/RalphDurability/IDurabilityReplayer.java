package RalphDurability;

import ralph.Endpoint;
import ralph.RalphGlobals;

public interface IDurabilityReplayer
{
    /**
       @return true if step succeeded.  false, if no more version info
       to replay.
     */
    public boolean step(RalphGlobals ralph_globals);

    /**
       @param uuid --- Return the endpoint associated with this
       uuid, if it exists.
     */
    public Endpoint get_endpoint_if_exists(String uuid);
    
    /**
       @returns The last timestamp for a committed lamport id.
     */
    public long last_committed_local_lamport_timestamp();
}