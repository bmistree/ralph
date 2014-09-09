package ralph;


/**
   FIXME: must fill this class in.  Class just tracks series of
   operations on an individual list.  Gets eventually passed to a
   VersionHelper that logs all version list changes.
 */
public class VersionListDeltas
{
    public final static
        VersionHelper<VersionListDeltas> LIST_VERSION_HELPER = null;
    
    public final static
        VersionHelper<AtomicInternalList> ATOMIC_INTERNAL_LIST_VERSION_HELPER =
            null;
    public final static
        VersionHelper<NonAtomicInternalList> NON_ATOMIC_INTERNAL_LIST_VERSION_HELPER =
            null;
}