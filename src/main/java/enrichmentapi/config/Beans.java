package enrichmentapi.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
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

import static java.util.Collections.singletonList;

@Configuration
public class Beans {

    @Value("${ignite.port:34500}")
    private Integer ignitePort;


    @Value("${ignite.storage}")
    private String igniteStorage;


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

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi().setLocalPort(ignitePort)
                .setLocalPortRange(20);

        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder().setAddresses(
                singletonList("127.0.0.1:" + ignitePort + ".." + (ignitePort + 20)));

        discoverySpi.setIpFinder(ipFinder);

        TcpCommunicationSpi commSpi = new TcpCommunicationSpi().setLocalPort(ignitePort - 400);

        cfg.setDiscoverySpi(discoverySpi);

        cfg.setCommunicationSpi(commSpi);

        Ignite ignite = Ignition.start(cfg);
        ignite.active(true);
        return ignite;
    }

}
