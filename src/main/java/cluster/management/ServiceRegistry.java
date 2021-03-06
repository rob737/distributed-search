package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {

    public static final String WORKERS_REGISTRY_ZNODE = "/workers_service_registry";
    public static final String COORDINATORS_REGISTRY_ZNODE = "/coordinators_service_registry";
    private final String serviceRegistryZnode;

    private final ZooKeeper zooKeeper;
    private String currentZnode = null;
    private List<String> allServiceAddresses = null;

    public ServiceRegistry(ZooKeeper zooKeeper, String serviceRegistryZnode) {
        this.zooKeeper = zooKeeper;
        this.serviceRegistryZnode = serviceRegistryZnode;
        createServiceRegistryNode();
    }

    private void createServiceRegistryNode() {
        try {
            if (zooKeeper.exists(serviceRegistryZnode, false) == null) {
                zooKeeper.create(serviceRegistryZnode, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    public void unregisterFromCluster() throws InterruptedException, KeeperException {
        if (currentZnode != null && zooKeeper.exists(currentZnode, false) != null) {
            zooKeeper.delete(currentZnode, -1);
        }
    }

    public void registerForUpdates() throws InterruptedException, KeeperException {
        updateAddresses();
    }

    public void registerToCluster(String metadata) throws KeeperException, InterruptedException {
        if (currentZnode != null) {
            System.out.println("Already registered to service registry");
            return;
        }
        this.currentZnode = zooKeeper.create(serviceRegistryZnode + "/n_", metadata.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to service registry");
    }

    public synchronized List<String> getAllServiceAddresses() throws KeeperException, InterruptedException {
        if (allServiceAddresses == null) {
            updateAddresses();
        }
        return allServiceAddresses;
    }

    private synchronized void updateAddresses() throws InterruptedException, KeeperException {
        List<String> workerZnodes = zooKeeper.getChildren(serviceRegistryZnode, this);
        List<String> addresses = new ArrayList<>(workerZnodes.size());

        for (String workerZnode : workerZnodes) {
            String workerZnodeFullPath = serviceRegistryZnode + "/" + workerZnode;
            System.out.println("update address : " + workerZnodeFullPath);
            Stat stat = zooKeeper.exists(workerZnodeFullPath, false);
            if (stat == null)
                continue;
            byte[] addressBytes = zooKeeper.getData(workerZnodeFullPath, false, stat);
            String address = new String(addressBytes);
            addresses.add(address);
        }
        this.allServiceAddresses = Collections.unmodifiableList(addresses);
        System.out.println("THe cluster addresses are : " + this.allServiceAddresses);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        try {
            updateAddresses();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }


}
