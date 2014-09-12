package ralph;

import RalphVersions.ILocalDeltaSerializer;
import RalphVersions.ILocalVersionManager;

/**
   Each basic type should have a static VersionHelper that it shares.
   May need different approach for parameterized types.
 */
public class VersionHelper<DataType> implements Versionable<DataType>
{
    protected final ILocalDeltaSerializer<DataType> serializer;
    public VersionHelper(
        ILocalDeltaSerializer<DataType> _serializer)
    {
        serializer = _serializer;
    }

    public void save_version(
        String uuid, DataType to_version,CommitMetadata commit_metadata)
    {
        // perform this call dynamically so that can dynamically turn
        // versioning off and on.
        ILocalVersionManager local_version_manager =
            VersioningInfo.instance.local_version_manager;
        // not performing any local versioning
        if (local_version_manager == null)
            return;

        local_version_manager.save_version_data(
            uuid,serializer.serialize(to_version),commit_metadata);
    }
    
    public void close_version(String uuid)
    {
        // perform this call dynamically so that can dynamically turn
        // versioning off and on.
        ILocalVersionManager local_version_manager =
            VersioningInfo.instance.local_version_manager;
        
        // not performing any local versioning
        if (local_version_manager == null)
            return;

        local_version_manager.close_versioned_object(uuid);
    }
}