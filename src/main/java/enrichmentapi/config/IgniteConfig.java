package enrichmentapi.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Collection;
import java.util.List;

@Configuration
public class IgniteConfig {

  @Value("${ignite.storage}")
  private String igniteStorage;

  @Value("${ignite.addresses}")
  private List<String> igniteAddresses;

  @Value("${ignite.discoverySPIPort:8446}")
  private Integer discoverySPIPort;

  @Value("${ignite.communicationSPIPort:8444}")
  private Integer communicationSPIPort;

  @Value("${ignite.restPort:8445}")
  private Integer restAccessPort;

  @Value("${ignite.clientPort:8443}")
  private Integer igniteClientPort;

  @Value("${ignite.jettyPort:8450}")
  private String igniteJettyPort;

  @Value("${ignite.addToBaselineTopology:true}")
  private Boolean addNodeToBaselineTopology;

  @Bean
  public Ignite igniteInstance() {
    IgniteConfiguration cfg = new IgniteConfiguration();
    String igniteStorageDir = igniteStorage + File.separator;
    cfg.setDataStorageConfiguration(new DataStorageConfiguration()
        .setDefaultDataRegionConfiguration(
            new DataRegionConfiguration().setPersistenceEnabled(true))
        .setStoragePath(igniteStorageDir + "persistence")
        .setWalPath(igniteStorageDir + "wal")
        .setWalArchivePath(igniteStorageDir + "walArchive"));

    cfg.setClientConnectorConfiguration(new ClientConnectorConfiguration().setPort(igniteClientPort));

    TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi().setLocalPort(discoverySPIPort);

    TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder()
        .setAddresses(
            igniteAddresses);

    discoverySpi.setIpFinder(ipFinder);

    TcpCommunicationSpi commSpi = new TcpCommunicationSpi().setLocalPort(communicationSPIPort);

    cfg.setDiscoverySpi(discoverySpi);
    cfg.getConnectorConfiguration().setPort(restAccessPort);

    cfg.setCommunicationSpi(commSpi);

    System.setProperty("IGNITE_JETTY_PORT", igniteJettyPort);

    Ignite ignite = Ignition.start(cfg);
    ignite.cluster().active(true);

    if (addNodeToBaselineTopology) {
      Collection<ClusterNode> nodes = ignite.cluster().forServers().nodes();
      ignite.cluster().setBaselineTopology(nodes);
    }

    return ignite;
  }

}
