package ralph;

import RalphVersions.ContainerDeltasToDelta;

public class InternalContainerTypeVersionHelper<KeyType>
    extends VersionHelper<VersionContainerDeltas>
{
    public InternalContainerTypeVersionHelper(
        ContainerDeltasToDelta<KeyType> _serializer)
    {
        super(_serializer);
    }
}