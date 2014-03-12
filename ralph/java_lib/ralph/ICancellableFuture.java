package ralph;
import java.util.concurrent.Future;

public interface ICancellableFuture extends Future<Boolean>
{
    public void failed();
    public void succeeded();
}
