package ralph;

import static RalphVersions.SerializableToByteArray.DOUBLE_SERIALIZER;
import static RalphVersions.SerializableToByteArray.STRING_SERIALIZER;
import static RalphVersions.SerializableToByteArray.BOOLEAN_SERIALIZER;

public class BaseTypeVersionHelpers
{
    public final VersionHelper<Double> DOUBLE_VERSION_HELPER;
    public final VersionHelper<String> STRING_VERSION_HELPER;
    public final VersionHelper<Boolean> BOOLEAN_VERSION_HELPER;
    
    public BaseTypeVersionHelpers(RalphGlobals ralph_globals)
    {
        DOUBLE_VERSION_HELPER =
            new VersionHelper<Double> (ralph_globals,DOUBLE_SERIALIZER);

        STRING_VERSION_HELPER =
            new VersionHelper<String> (ralph_globals,STRING_SERIALIZER);
        
        BOOLEAN_VERSION_HELPER =
            new VersionHelper<Boolean> (ralph_globals,BOOLEAN_SERIALIZER);
    }
}
