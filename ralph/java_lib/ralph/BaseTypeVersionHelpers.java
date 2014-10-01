package ralph;

import static RalphVersions.ObjectToDelta.DOUBLE_SERIALIZER;
import static RalphVersions.ObjectToDelta.STRING_SERIALIZER;
import static RalphVersions.ObjectToDelta.BOOLEAN_SERIALIZER;
import static RalphVersions.ObjectToDelta.REFERENCE_SERIALIZER;

import static RalphVersions.ContainerDeltasToDelta.DOUBLE_KEYED_MAP_DELTA_SERIALIZER;
import static RalphVersions.ContainerDeltasToDelta.STRING_KEYED_MAP_DELTA_SERIALIZER;
import static RalphVersions.ContainerDeltasToDelta.BOOLEAN_KEYED_MAP_DELTA_SERIALIZER;
import static RalphVersions.ContainerDeltasToDelta.INTEGER_KEYED_LIST_DELTA_SERIALIZER;

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
    public final static VersionHelper INTERFACE_VERSION_HELPER =
        REFERENCE_VERSION_HELPER;
    
    public final static InternalContainerTypeVersionHelper<Double>
        DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER =
        new InternalContainerTypeVersionHelper<Double>(
            DOUBLE_KEYED_MAP_DELTA_SERIALIZER);
    public final static InternalContainerTypeVersionHelper<String>
        STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER =
        new InternalContainerTypeVersionHelper<String>(
            STRING_KEYED_MAP_DELTA_SERIALIZER);
    public final static InternalContainerTypeVersionHelper<Boolean>
        BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER =
        new InternalContainerTypeVersionHelper<Boolean>(
                BOOLEAN_KEYED_MAP_DELTA_SERIALIZER);

    public final static InternalContainerTypeVersionHelper
        INTERNAL_LIST_TYPE_VERSION_HELPER =
        new InternalContainerTypeVersionHelper<Integer>(
            INTEGER_KEYED_LIST_DELTA_SERIALIZER);

    public final static VersionHelper ENUM_VERSION_HELPER = null;
    public final static VersionHelper SERVICE_FACTORY_VERSION_HELPER = null;
    public final static VersionHelper SERVICE_REFERENCE_VERSION_HELPER = null;
}
