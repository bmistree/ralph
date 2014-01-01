package RalphAtomicWrappers;

import ralph.RalphObject;

/**
   When setting_val_on_key for a map, pass in internal value of
   object.

   Eg., for
   
   Map(from: Number, to: Number) m;
   m.set(3,4);

   Actually translates to

   m.get_val(active_event).set_val_on_key(
       new Double(3), new Double(4));

   However, the map stores the value as wrapped in a
   SingleThreadedNumberVariable or MultiThreadedNumberVariable.  So,
   need a way to convert from type Double to type
   Single/MultiThreadedVariable.

   This is the base class of something that does that.
   
 */
public interface EnsureAtomicWrapper<V,D>
{
    public RalphObject<V,D> ensure_atomic_object(V object_to_ensure);
}
