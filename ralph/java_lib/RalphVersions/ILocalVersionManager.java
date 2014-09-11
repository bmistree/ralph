package RalphVersions;

import ralph.CommitMetadata;

public interface ILocalVersionManager
{
    /**
       @param commit_metadata --- Multiple objects will simultaneously
       point to the same commit_metadata object when being saved.  Do
       not want to save a separate commit_metadata object for each
       one.  Instead, saving one version of commit_metadata and
       objects will point at this version of commit_metadata.
     */
    public void save_commit_metadata(CommitMetadata commit_metadata);

    /**
       @param object_uuid --- The unique name of a versioned object.
       
       @param delta --- The information we should save for the update
       of this version.
       
       @param commit_metadata --- See note above save_commit_metadata.
     */
    public void save_version_data(
        String object_uuid, byte[] delta, CommitMetadata commit_metadata);

    /**
       Want to explicitly preserve mapping between endpoint variable
       names and the ralph objects that they point to.  Whenever we
       initially create an endpoint, we call this function to log the
       newly-created data.  Note that there are two differences
       between the data that this method stores and the data stored by
       save_version_data.  The first is that the mapping stored here
       has not completed its transaction.  Ie., the changes here may
       never actually commit.  Secondly, because the mapping stored
       here is not part of a transaction, we do not have a
       commit_metadata object to log.
     */
    public void save_endpoint_global_mapping(
        String variable_name, String object_uuid,String endpoint_uuid,
        long local_lamport_time);
    
    /**
       No longer will receive udpates from this versioned object.
       Note: not guaranteed to be called by object.  Similarly, may
       not be called as soon as versioned object is no longer
       reachable.
     */
    public void close_versioned_object(String object_uuid);
}