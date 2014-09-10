package ralph;

import java.util.List;

import RalphDataWrappers.ContainerOpTuple;

/**
   FIXME: must fill this class in.  Class just tracks series of
   operations on an individual list.  Gets eventually passed to a
   VersionHelper that logs all version list changes.
 */
public class VersionListDeltas<ValueType,ValueDeltaType>
{
    public final static VersionHelper LIST_VERSION_HELPER = null;

    public final static
        VersionHelper<AtomicInternalList> ATOMIC_INTERNAL_LIST_VERSION_HELPER =
            null;
    public final static
        VersionHelper<NonAtomicInternalList> NON_ATOMIC_INTERNAL_LIST_VERSION_HELPER =
            null;

    // immutable list
    public final List<ContainerOpTuple<Integer,ValueType,ValueDeltaType>> deltas;
    public VersionListDeltas(
        List<ContainerOpTuple<Integer,ValueType,ValueDeltaType>> _deltas)
    {
        deltas = _deltas;
    }
}