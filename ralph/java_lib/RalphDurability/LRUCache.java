package RalphDurability;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache <KeyType,ValueType>
    extends LinkedHashMap <KeyType, ValueType>
{
    public static final int DEFAULT_MAX_LRU_ENTRIES = 10000;
    public final int max_entries;

    public LRUCache(int max_entries)
    {
        super(max_entries + 1);
        this.max_entries = max_entries;
    }

    public LRUCache()
    {
        this(DEFAULT_MAX_LRU_ENTRIES);
    }

    /**
       @returns true if we're supposed to remove this entry.  In this
       case, if we're larger than our target size.
     */
    public boolean removeEldestEntry(Map.Entry eldest)
    {
        return size() > max_entries;
    }
}