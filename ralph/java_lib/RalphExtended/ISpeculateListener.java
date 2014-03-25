package RalphExtended;

import ralph.ActiveEvent;


/**
   Notified by ExtendedHardwareOverrides when it should call speculate
   on object.
 */
public interface ISpeculateListener
{
    public void speculate(ActiveEvent active_event);
}