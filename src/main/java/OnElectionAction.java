import cluster.management.OnElectionCallback;
import cluster.management.ServiceRegistry;
import networking.WebServer;
import org.apache.zookeeper.KeeperException;
import search.SearchWorker;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OnElectionAction implements OnElectionCallback {

    private final ServiceRegistry serviceRegistry;
    private final int port;
    private WebServer webServer;

    public OnElectionAction(ServiceRegistry workersServiceRegistry, int currentServerPort) {
        this.serviceRegistry = workersServiceRegistry;
        this.port = currentServerPort;
    }

    @Override
    public void onElectedToBeLeader() {
        try {
            serviceRegistry.unregisterFromCluster();
            serviceRegistry.registerForUpdates();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWorker() {
        String currentServerAddress = null;
        SearchWorker searchWorker = new SearchWorker();
        webServer = new WebServer(port, searchWorker);
        webServer.startServer();

        try {
            currentServerAddress = String.format("http://%s:%d%s", InetAddress.getLocalHost().getCanonicalHostName(), port, searchWorker.getEndpoint());
            serviceRegistry.registerToCluster(currentServerAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
}
