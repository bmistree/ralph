package ralph;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

import RalphVersions.ILocalVersionSaver;
import RalphVersions.ILocalVersionReplayer;
import RalphVersions.InMemoryLocalVersionManager;


/**
   Singleton: keeps track of all the versioning data used by 
 */
public class VersioningInfo
{
    public static final VersioningInfo instance = new VersioningInfo();

    public final ILocalVersionSaver local_version_saver;
    public final ILocalVersionReplayer local_version_replayer;
    private VersioningInfo() 
    {
        Properties properties = new Properties();
        
        InputStream input_stream =
            getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input_stream == null)
        {
            local_version_saver = null;
            local_version_replayer = null;
        }
        else
        {
            try
            {
                properties.load(input_stream);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                System.exit (-1);
            }

            String which_versioner = (String)properties.get("versioner");
            if ((which_versioner != null) &&
                which_versioner.equals("in-memory"))
            {
                InMemoryLocalVersionManager local_version_manager =
                    new InMemoryLocalVersionManager();
                local_version_saver = local_version_manager;
                local_version_replayer = local_version_manager;
            }
            else
            {
                local_version_saver = null;
                local_version_replayer = null;
            }
        }
    }
}