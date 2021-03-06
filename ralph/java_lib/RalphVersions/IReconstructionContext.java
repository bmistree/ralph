package RalphVersions;

import ralph.RalphObject;
import ralph.Endpoint;

/**
   When replaying variables, provide this interface for objects to use
   to get eg., what internal variables should be pointing at.
 */

public interface IReconstructionContext
{
    /**
       Return a ralph object constructed immediately before or on the
       lamport_timestamp_before_or_during value.

       Returns null if none exists.

       Mostly used for reconstructing reference variables.
     */
    public RalphObject get_constructed_object(
        String obj_uuid, Long lamport_timestamp_before_or_during);

    public IVersionReplayer get_version_replayer();

    /**
       @returns null if does not exist.  Used primarily for
       deserializing endpoints during durability reconstruction.
     */
    public Endpoint get_endpt(String uuid);
}