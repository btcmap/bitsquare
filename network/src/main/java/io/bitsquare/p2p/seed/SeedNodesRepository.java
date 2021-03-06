package io.bitsquare.p2p.seed;

import com.google.common.collect.Sets;
import io.bitsquare.p2p.NodeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class SeedNodesRepository {
    private static final Logger log = LoggerFactory.getLogger(SeedNodesRepository.class);

    // mainnet use port 8000
    // testnet use port 8001
    // regtest use port 8002
    private Set<NodeAddress> torSeedNodeAddresses = Sets.newHashSet(
            // mainnet
            new NodeAddress("oyyii5ogv7y7iadi.onion:8000"),
            new NodeAddress("ugcro2f5xnkguash.onion:8000"),
            new NodeAddress("qarhpdsl6mfhbnud.onion:8000"),
            
            // testnet
            new NodeAddress("znmy44wcstn2rkva.onion:8001"),
            new NodeAddress("zvn7umikgxml6x6h.onion:8001"),
            new NodeAddress("wnfxmrmsyeeos2dy.onion:8001"),

            // regtest
            new NodeAddress("rxdkppp3vicnbgqt.onion:8002"),
            new NodeAddress("brmbf6mf67d2hlm4.onion:8002"),
            new NodeAddress("mfla72c4igh5ta2t.onion:8002")
    );


    private Set<NodeAddress> localhostSeedNodeAddresses = Sets.newHashSet(
            // mainnet
            new NodeAddress("localhost:2000"),
            new NodeAddress("localhost:3000"),
            new NodeAddress("localhost:4000"),

            // testnet
            new NodeAddress("localhost:2001"),
            new NodeAddress("localhost:3001"),
            new NodeAddress("localhost:4001"),

            // regtest
            new NodeAddress("localhost:2002"),
            new NodeAddress("localhost:3002"),
            new NodeAddress("localhost:4002")
    );
    private NodeAddress nodeAddressToExclude;

    public Set<NodeAddress> getSeedNodeAddresses(boolean useLocalhost, int networkId) {
        String networkIdAsString = String.valueOf(networkId);
        Set<NodeAddress> nodeAddresses = useLocalhost ? localhostSeedNodeAddresses : torSeedNodeAddresses;
        Set<NodeAddress> filtered = nodeAddresses.stream()
                .filter(e -> String.valueOf(e.port).endsWith(networkIdAsString))
                .filter(e -> !e.equals(nodeAddressToExclude))
                .collect(Collectors.toSet());
        log.info("SeedNodeAddresses (useLocalhost={}) for networkId {}:\nnetworkId={}", useLocalhost, networkId, filtered);
        return filtered;
    }

    public void setTorSeedNodeAddresses(Set<NodeAddress> torSeedNodeAddresses) {
        this.torSeedNodeAddresses = torSeedNodeAddresses;
    }

    public void setLocalhostSeedNodeAddresses(Set<NodeAddress> localhostSeedNodeAddresses) {
        this.localhostSeedNodeAddresses = localhostSeedNodeAddresses;
    }

    public void setNodeAddressToExclude(NodeAddress nodeAddress) {
        this.nodeAddressToExclude = nodeAddress;
    }
}
