package ralph;

public interface Versionable<DataType>
{
    /**
       Log version data for this commit.
     */
    void save_version(
        String uuid, DataType to_version,CommitMetadata commit_metadata);
    /**
       Indicate that no longer need to keep track of this versionable.
     */
    void close_version(String uuid);
}