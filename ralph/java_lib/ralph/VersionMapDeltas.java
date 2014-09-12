package ralph;

import java.util.List;

import RalphDataWrappers.ContainerOpTuple;

/**
   Class just tracks series of operations on an individual map.  Gets
   eventually passed to a VersionHelper that logs all version map
   changes.
 */
public class VersionMapDeltas<KeyType,ValueType,ValueDeltaType>
{
    // immutable list
    public final List<ContainerOpTuple<KeyType,ValueType,ValueDeltaType>> deltas;
    public VersionMapDeltas(
        List<ContainerOpTuple<KeyType,ValueType,ValueDeltaType>> _deltas)
    {
        deltas = _deltas;
    }
}
