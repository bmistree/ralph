package ralph;

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

    public abstract EnumType construct(int ordinal);
}