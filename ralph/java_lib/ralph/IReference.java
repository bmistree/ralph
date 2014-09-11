package ralph;

/**
   Maps and lists hold references to internal ralph values:
   (Non)AtomicInternalMap/Lists.  All these reference holders should
   implement this interface, which supplies information for
   VersionHelpers to log changes.
 */
public interface IReference
{
    /**
       @returns the uuid of the object reference holder is pointing
       at.  Or null, if holding null object.  Note that this already
       assumes that we're holding lock on object.
     */
    public String uuid();
}
