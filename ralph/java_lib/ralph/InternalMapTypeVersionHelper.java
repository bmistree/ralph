package ralph;

import RalphVersions.MapDeltasToDelta;

public class InternalMapTypeVersionHelper<KeyType>
    extends VersionHelper<VersionMapDeltas<KeyType,Object,Object>>
{
    public InternalMapTypeVersionHelper(
        RalphGlobals _ralph_globals,
        MapDeltasToDelta<KeyType> _serializer)
    {
        super(_ralph_globals,_serializer);
    }
}