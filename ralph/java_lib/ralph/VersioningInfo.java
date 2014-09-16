package ralph;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

import RalphVersions.ILocalVersionManager;
import RalphVersions.InMemoryLocalVersionManager;


/**
   Singleton: keeps track of all the versioning data used by 
 */
public class VersioningInfo
{
    public static final VersioningInfo instance = new VersioningInfo();

    public final ILocalVersionManager local_version_manager;
    private VersioningInfo() 
    {
        Properties properties = new Properties();
        
        InputStream input_stream =
            getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input_stream == null)
            local_version_manager = null;
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
                local_version_manager = new InMemoryLocalVersionManager();
            }
            else
                local_version_manager = null;
        }
    }
}