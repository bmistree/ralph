package ralph;

import RalphVersions.MapDeltasToDelta;

public class InternalMapTypeVersionHelper<KeyType>
    extends VersionHelper<VersionMapDeltas>
{
    public InternalMapTypeVersionHelper(
        MapDeltasToDelta<KeyType> _serializer)
    {
        super(_serializer);
    }
}