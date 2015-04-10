package ralph.MessageManager;

import ralph_protobuffs.InstallProto.Install;

public interface IInstallMessageListener
{
    public void recv_install_msg(Install msg);
}