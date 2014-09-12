package ralph;

import static RalphVersions.ObjectToDelta.DOUBLE_SERIALIZER;
import static RalphVersions.ObjectToDelta.STRING_SERIALIZER;
import static RalphVersions.ObjectToDelta.BOOLEAN_SERIALIZER;
import static RalphVersions.ObjectToDelta.REFERENCE_SERIALIZER;

import static RalphVersions.MapDeltasToDelta.DOUBLE_KEYED_MAP_DELTA_SERIALIZER;
import static RalphVersions.MapDeltasToDelta.STRING_KEYED_MAP_DELTA_SERIALIZER;
import static RalphVersions.MapDeltasToDelta.BOOLEAN_KEYED_MAP_DELTA_SERIALIZER;

public class BaseTypeVersionHelpers
{
    public final static VersionHelper<Double> DOUBLE_VERSION_HELPER = 
        new VersionHelper<Double> (DOUBLE_SERIALIZER);
    public final static VersionHelper<String> STRING_VERSION_HELPER =
        new VersionHelper<String> (STRING_SERIALIZER);
    public final static VersionHelper<Boolean> BOOLEAN_VERSION_HELPER =
        new VersionHelper<Boolean> (BOOLEAN_SERIALIZER);
    public final static VersionHelper<IReference> REFERENCE_VERSION_HELPER =
        new VersionHelper<IReference> (REFERENCE_SERIALIZER);

    public final static InternalMapTypeVersionHelper<Double>
        DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER =
        new InternalMapTypeVersionHelper<Double>(
            DOUBLE_KEYED_MAP_DELTA_SERIALIZER);
    public final static InternalMapTypeVersionHelper<String>
        STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER =
        new InternalMapTypeVersionHelper<String>(
            STRING_KEYED_MAP_DELTA_SERIALIZER);
    public final static InternalMapTypeVersionHelper<Boolean>
        BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER =
        new InternalMapTypeVersionHelper<Boolean>(
                BOOLEAN_KEYED_MAP_DELTA_SERIALIZER);
    
    public final static VersionHelper MAP_VERSION_HELPER = null;
    public final static VersionHelper LIST_VERSION_HELPER = null;
    public final static VersionHelper ENUM_VERSION_HELPER = null;
    public final static VersionHelper INTERFACE_VERSION_HELPER = null;
    public final static VersionHelper SERVICE_FACTORY_VERSION_HELPER = null;
    public final static VersionHelper SERVICE_REFERENCE_VERSION_HELPER = null;
}
