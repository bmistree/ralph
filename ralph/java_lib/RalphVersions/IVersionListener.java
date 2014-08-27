package RalphVersions;

import ralph.CommitMetadata;

/**
   Committer should notify version service of changes using this
   interface.
 */
public interface IVersionListener<VersionType>
{
    /**
       Called as a result of first_phase_commit.
     */
    public void stage_delta(
        String device_id, VersionType staged_version,CommitMetadata metadata);
    
    /**
       Do not require the completed data because should already have a
       record of it from stage_delta.
     */
    public void complete_delta(String device_id, CommitMetadata metadata);
    
    /**
       Only called for versions that have been staged, and
       subsequently backed out.  Not for versions that have been
       backed out before being staged.

       Do not require the completed data because should already have a
       record of it from stage_delta.
     */
    public void backout_delta(String device_id, CommitMetadata metadata);
}
