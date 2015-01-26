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
       @param endpt_uuid --- Return the endpoint associated with this
       uuid, if it exists.
     */
    public Endpoint get_endpt(String endpt_uuid);

    /**
       @returns The last timestamp for a committed lamport id.
     */
    public long last_committed_local_lamport_timestamp();
}