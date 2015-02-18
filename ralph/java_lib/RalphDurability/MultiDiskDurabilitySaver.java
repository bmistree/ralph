package RalphDurability;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.io.IOException;

import ralph.EndpointConstructorObj;
import ralph.Util;

public class MultiDiskDurabilitySaver implements IDurabilitySaver
{
    private final int num_synced_log_files;
    private final int num_non_synced_log_files;

    // disk durability savers
    private final List<IDurabilitySaver> list_of_synced_savers =
        new ArrayList<IDurabilitySaver>();
    private final AtomicInteger synced_index = new AtomicInteger(0);

    // lazy file writers
    private final List<IDurabilitySaver> list_of_non_synced_savers =
        new ArrayList<IDurabilitySaver>();
    private final AtomicInteger non_synced_index = new AtomicInteger(0);

    private final LRUCache<String,Boolean> constructor_obj_index =
        new LRUCache<String,Boolean>();
    
    public MultiDiskDurabilitySaver(
        String dir_to_save_to, int num_synced_log_files,
        int num_non_synced_log_files, IDurabilitySaverFactory synced_saver,
        IDurabilitySaverFactory non_synced_saver)
    {
        this.num_synced_log_files = num_synced_log_files;
        this.num_non_synced_log_files = num_non_synced_log_files;
        for (int i = 0; i < num_synced_log_files; ++i)
        {
            String log_filename =
                dir_to_save_to + File.separator +
                "durability_log_" + i + ".bin";

            IDurabilitySaver ids = synced_saver.construct(log_filename);
            list_of_synced_savers.add(ids);
        }

        for (int i = 0; i <num_non_synced_log_files; ++i)
        {
            // initialize lazy writers
            String lazy_filename =
                dir_to_save_to + File.separator +
                "durability_lazy_log_" + i + ".bin";

            IDurabilitySaver ids =
                non_synced_saver.construct(lazy_filename);
            list_of_non_synced_savers.add(ids);
        }
    }

    @Override
    public void prepare_operation(IDurabilityContext dc)
    {
        int index = synced_index.addAndGet(1) % num_synced_log_files;
        IDurabilitySaver ids = list_of_synced_savers.get(index);
        ids.prepare_operation(dc);
    }

    @Override
    public void ensure_logged_endpt_constructor(
        EndpointConstructorObj endpt_constructor_obj)
    {
        boolean should_log_to_disk = false;
        // require synchronized access to lru cache
        synchronized(this)
        {
            String const_name = endpt_constructor_obj.get_canonical_name();
            Boolean value =
                constructor_obj_index.get(const_name);
            if (value == null)
            {
                should_log_to_disk = true;
                constructor_obj_index.put(const_name,true);
            }
        }

        if (should_log_to_disk)
        {
            int index = synced_index.addAndGet(1) % num_synced_log_files;
            IDurabilitySaver ids = list_of_synced_savers.get(index);
            ids.ensure_logged_endpt_constructor(endpt_constructor_obj);
        }
    }

    @Override
    public void complete_operation(IDurabilityContext dc, boolean succeeded)
    {
        int index = non_synced_index.addAndGet(1) % num_non_synced_log_files;
        IDurabilitySaver ids = list_of_non_synced_savers.get(index);
        ids.complete_operation(dc,succeeded);
    }
}