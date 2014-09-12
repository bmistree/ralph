package ralph;

import static RalphVersions.ObjectToDelta.DOUBLE_SERIALIZER;
import static RalphVersions.ObjectToDelta.STRING_SERIALIZER;
import static RalphVersions.ObjectToDelta.BOOLEAN_SERIALIZER;
import static RalphVersions.ObjectToDelta.REFERENCE_SERIALIZER;

public class BaseTypeVersionHelpers
{
    public final VersionHelper<Double> DOUBLE_VERSION_HELPER;
    public final VersionHelper<String> STRING_VERSION_HELPER;
    public final VersionHelper<Boolean> BOOLEAN_VERSION_HELPER;
    public final VersionHelper<IReference> REFERENCE_VERSION_HELPER;

    /**
       For now, just setting map, list, enum, and service factory
       version helpers to null.
     */
    public final VersionHelper MAP_VERSION_HELPER = null;
    public final VersionHelper LIST_VERSION_HELPER = null;
    public final VersionHelper ENUM_VERSION_HELPER = null;
    public final VersionHelper INTERFACE_VERSION_HELPER = null;
    public final VersionHelper SERVICE_FACTORY_VERSION_HELPER = null;
    public final VersionHelper SERVICE_REFERENCE_VERSION_HELPER = null;
    
    
    public BaseTypeVersionHelpers(RalphGlobals ralph_globals)
    {
        DOUBLE_VERSION_HELPER =
            new VersionHelper<Double> (ralph_globals,DOUBLE_SERIALIZER);

        STRING_VERSION_HELPER =
            new VersionHelper<String> (ralph_globals,STRING_SERIALIZER);
        
        BOOLEAN_VERSION_HELPER =
            new VersionHelper<Boolean> (ralph_globals,BOOLEAN_SERIALIZER);

        REFERENCE_VERSION_HELPER =
            new VersionHelper<IReference> (
                ralph_globals, REFERENCE_SERIALIZER);
    }
}
