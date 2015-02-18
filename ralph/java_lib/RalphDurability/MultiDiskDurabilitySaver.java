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
    private final int num_log_files;

    // disk durability savers
    private final List<DiskDurabilitySaver> list_of_dur_savers =
        new ArrayList<DiskDurabilitySaver>();
    private final AtomicInteger saver_index = new AtomicInteger(0);

    // lazy file writers
    private final List<NonSyncedWrites> list_of_lazy_writers =
        new ArrayList<NonSyncedWrites>();
    private final AtomicInteger lazy_index = new AtomicInteger(0);

    private final LRUCache<String,Boolean> constructor_obj_index =
        new LRUCache<String,Boolean>();
    
    public MultiDiskDurabilitySaver(String dir_to_save_to,int num_log_files)
    {
        this.num_log_files = num_log_files;
        try
        {
            for (int i = 0; i <num_log_files; ++i)
            {
                // initialize durability writers
                String log_filename =
                    dir_to_save_to + File.separator +
                    "durability_log_" + i + ".bin";
                DiskDurabilitySaver dds =
                    new DiskDurabilitySaver (log_filename,false);
                list_of_dur_savers.add(dds);

                // initialize lazy writers
                String lazy_filename =
                    dir_to_save_to + File.separator +
                    "durability_lazy_log_" + i + ".bin";
                NonSyncedWrites nsw =
                    new NonSyncedWrites (lazy_filename);
                list_of_lazy_writers.add(nsw);
            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "File exception opening durability loggers");
        }
    }

    @Override
    public void prepare_operation(IDurabilityContext dc)
    {
        int index = saver_index.addAndGet(1) % num_log_files;
        DiskDurabilitySaver dds = list_of_dur_savers.get(index);
        dds.prepare_operation(dc);
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
            int index = saver_index.addAndGet(1) % num_log_files;
            DiskDurabilitySaver dds = list_of_dur_savers.get(index);
            dds.ensure_logged_endpt_constructor(endpt_constructor_obj);
        }
    }

    @Override
    public void complete_operation(IDurabilityContext dc, boolean succeeded)
    {
        int index = lazy_index.addAndGet(1) % num_log_files;
        NonSyncedWrites nsw = list_of_lazy_writers.get(index);
        nsw.complete_operation(dc,succeeded);
    }
}