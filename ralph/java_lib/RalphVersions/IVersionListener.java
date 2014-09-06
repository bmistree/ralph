package RalphVersions;

import ralph.CommitMetadata;

/**
   Committer should notify version service of changes using this
   interface.  Should use one per device.
 */
public interface IVersionListener<DataType>
{
    /**
       Called as a result of first_phase_commit.
     */
    public void stage_delta(DataType staged_data,CommitMetadata metadata);
    
    /**
       Do not require the completed data because should already have a
       record of it from stage_delta.
     */
    public void complete_delta(CommitMetadata metadata);
    
    /**
       Only called for versions that have been staged, and
       subsequently backed out.  Not for versions that have been
       backed out before being staged.

       Do not require the completed data because should already have a
       record of it from stage_delta.
     */
    public void backout_delta(CommitMetadata metadata);
}
