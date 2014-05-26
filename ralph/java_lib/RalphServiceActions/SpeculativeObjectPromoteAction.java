package RalphServiceActions;

import ralph.SpeculativeAtomicObject;

public class SpeculativeObjectPromoteAction extends ServiceAction
{
    private final SpeculativeAtomicObject object_to_promote_on;
	
    public SpeculativeObjectPromoteAction(SpeculativeAtomicObject object_to_promote_on)
    {
        this.object_to_promote_on = object_to_promote_on;
    }
	
    @Override
    public void run() {
        object_to_promote_on.try_promote_speculated();
    }
}
