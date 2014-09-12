package ralph;

import RalphVersions.ILocalDeltaSerializer;

/**
   Each basic type should have a static VersionHelper that it shares.
   May need different approach for parameterized types.
 */
public class VersionHelper<DataType> implements Versionable<DataType>
{
    protected final RalphGlobals ralph_globals;
    protected final ILocalDeltaSerializer<DataType> serializer;

    public VersionHelper(
        RalphGlobals _ralph_globals,
        ILocalDeltaSerializer<DataType> _serializer)
    {
        ralph_globals = _ralph_globals;
        serializer = _serializer;
    }

    public void save_version(
        String uuid, DataType to_version,CommitMetadata commit_metadata)
    {
        // not performing any local versioning
        if (ralph_globals.local_version_manager == null)
            return;
        
        ralph_globals.local_version_manager.save_version_data(
            uuid,serializer.serialize(to_version),commit_metadata);
    }
    
    public void close_version(String uuid)
    {
        // not performing any local versioning
        if (ralph_globals.local_version_manager == null)
            return;

        ralph_globals.local_version_manager.close_versioned_object(uuid);
    }
}