package ralph;

import RalphVersions.IDeltaSerializer;
import RalphVersions.IVersionSaver;

/**
   Each basic type should have a static VersionHelper that it shares.
   May need different approach for parameterized types.
 */
public class VersionHelper<DataType> implements Versionable<DataType>
{
    protected final IDeltaSerializer<DataType> serializer;
    public VersionHelper(
        IDeltaSerializer<DataType> _serializer)
    {
        serializer = _serializer;
    }

    public void save_version(
        String uuid, DataType to_version,CommitMetadata commit_metadata)
    {
        // perform this call dynamically so that can dynamically turn
        // versioning off and on.
        IVersionSaver version_saver =
            VersioningInfo.instance.version_saver;
        // not performing any local versioning
        if (version_saver == null)
            return;

        version_saver.save_version_data(
            uuid,serializer.serialize(to_version),commit_metadata);
    }
    
    public void close_version(String uuid)
    {
        // perform this call dynamically so that can dynamically turn
        // versioning off and on.
        IVersionSaver version_saver =
            VersioningInfo.instance.version_saver;
        
        // not performing any local versioning
        if (version_saver == null)
            return;

        version_saver.close_versioned_object(uuid);
    }
}