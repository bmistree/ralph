package ralph;

import RalphVersions.ContainerDeltasToDelta;

public class InternalMapTypeVersionHelper<KeyType>
    extends VersionHelper<VersionContainerDeltas>
{
    public InternalMapTypeVersionHelper(
        ContainerDeltasToDelta<KeyType> _serializer)
    {
        super(_serializer);
    }
}