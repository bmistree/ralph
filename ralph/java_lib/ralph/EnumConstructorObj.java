package ralph;

import ralph.RalphGlobals;
import RalphVersions.IVersionSaver;

public abstract class EnumConstructorObj<EnumType extends Enum>
{
    public EnumConstructorObj()
    {
        IVersionSaver version_saver =
            VersioningInfo.instance.version_saver;
        
        if (version_saver != null)
            version_saver.save_enum_constructor_obj(this);
    }

    public abstract Variables.AtomicEnumVariable<EnumType> construct(
        int ordinal,RalphGlobals ralph_globals);

    /**
       Just produces an enum instance, given the ordinal.
     */
    public abstract EnumType construct_enum(int ordinal);

}