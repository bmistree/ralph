package ralph;

import ralph.RalphGlobals;
import RalphVersions.ILocalVersionSaver;

public abstract class EnumConstructorObj<EnumType extends Enum>
{
    public EnumConstructorObj()
    {
        ILocalVersionSaver local_version_saver =
            VersioningInfo.instance.local_version_saver;
        
        if (local_version_saver != null)
            local_version_saver.save_enum_constructor_obj(this);
    }

    public abstract Variables.AtomicEnumVariable<EnumType> construct(
        int ordinal,RalphGlobals ralph_globals);

    /**
       Just produces an enum instance, given the ordinal.
     */
    public abstract EnumType construct_enum(int ordinal);

}