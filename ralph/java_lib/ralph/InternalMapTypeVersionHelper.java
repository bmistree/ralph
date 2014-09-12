package ralph;

import RalphVersions.MapDeltasToDelta;

public class InternalMapTypeVersionHelper<KeyType>
    extends VersionHelper<VersionMapDeltas<KeyType,Object,Object>>
{
    public InternalMapTypeVersionHelper(
        MapDeltasToDelta<KeyType> _serializer)
    {
        super(_serializer);
    }
}