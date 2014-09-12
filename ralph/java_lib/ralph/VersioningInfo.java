package ralph;

import RalphVersions.ILocalVersionManager;

/**
   Singleton: keeps track of all the versioning data used by 
 */
public class VersioningInfo
{
    public ILocalVersionManager local_version_manager = null;

    public static final VersioningInfo instance = new VersioningInfo();

    private VersioningInfo()
    {}
}