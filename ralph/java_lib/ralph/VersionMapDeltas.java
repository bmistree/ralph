package ralph;


/**
   FIXME: must fill this class in.  Class just tracks series of
   operations on an individual map.  Gets eventually passed to a
   VersionHelper that logs all version map changes.
 */
public class VersionMapDeltas
{
    public final static
        VersionHelper<VersionMapDeltas> MAP_VERSION_HELPER = null;

    public final static
        VersionHelper<AtomicInternalMap> ATOMIC_INTERNAL_MAP_VERSION_HELPER =
            null;
    public final static
        VersionHelper<NonAtomicInternalMap> NON_ATOMIC_INTERNAL_MAP_VERSION_HELPER =
            null;
}