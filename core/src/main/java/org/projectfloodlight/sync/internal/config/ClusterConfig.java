package org.projectfloodlight.sync.internal.config;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.projectfloodlight.sync.ClusterNode;
import org.projectfloodlight.sync.error.SyncException;


/**
 * Represent the configuration of a cluster in the sync manager
 * @author readams
 */
public class ClusterConfig {

    private HashMap<Short, ClusterNode> allNodes =
            new HashMap<Short, ClusterNode>();
    private HashMap<Short, List<ClusterNode>> localDomains =
            new HashMap<Short, List<ClusterNode>>();
    private ClusterNode thisNode;

    private AuthScheme authScheme;
    private String keyStorePath;
    private String keyStorePassword;
    private String listenAddress;
    private boolean leaderAllowed = true;
    
    public ClusterConfig() {
        super();
    }

    /**
     * Initialize a cluster config using a list of nodes
     * @param nodes the nodes to use
     * @param thisNodeId the node ID for the current node
     * @param authScheme the {@link AuthScheme}
     * @param keyStorePath the path to a java key store containing
     * credentials necessary for implementing the {@link AuthScheme}
     * @param keyStorePassword the password for the key store.
     * @throws SyncException
     */
    public ClusterConfig(List<ClusterNode> nodes, short thisNodeId,
                         AuthScheme authScheme,
                         String keyStorePath, 
                         String keyStorePassword)
            throws SyncException {
        init(nodes, thisNodeId, authScheme, keyStorePath, keyStorePassword);
    }

    /**
     * Initialize a cluster config using a list of nodes
     * @param nodes the nodes to use
     * @param thisNodeId the node ID for the current node
     * @param listenAddress String representing the address to listen on
     * @param authScheme the {@link AuthScheme}
     * @param keyStorePath the path to a java key store containing
     * credentials necessary for implementing the {@link AuthScheme}
     * @param keyStorePassword the password for the key store.
     * @throws SyncException
     */
    public ClusterConfig(List<ClusterNode> nodes, short thisNodeId,
                         String listenAddress,
                         AuthScheme authScheme,
                         String keyStorePath, 
                         String keyStorePassword,
                         boolean leaderAllowed)
            throws SyncException {
        init(nodes, thisNodeId, authScheme, keyStorePath, keyStorePassword);
        this.listenAddress = listenAddress;
        this.leaderAllowed = leaderAllowed;
    }
    
    /**
     * Get a collection containing all configured nodes
     * @return the collection of nodes
     */
    public Collection<ClusterNode> getNodes() {
        return Collections.unmodifiableCollection(allNodes.values());
    }

    /**
     * A collection of the nodes in the local domain for the current node
     * @return the list of nodes
     */
    public Collection<ClusterNode> getDomainNodes() {
        return getDomainNodes(thisNode.getDomainId());
    }

    /**
     * A collection of the nodes in the local domain specified
     * @param domainId the domain ID
     * @return the list of nodes
     */
    public Collection<ClusterNode> getDomainNodes(short domainId) {
        List<ClusterNode> r = localDomains.get(domainId);
        return Collections.unmodifiableCollection(r);
    }

    /**
     * Get the {@link ClusterNode} object for the current node
     */
    public ClusterNode getNode() {
        return thisNode;
    }

    /**
     * The a list of the nodes in the local domain specified
     * @param nodeId the node ID to retrieve
     * @return the node (or null if there is no such node
     */
    public ClusterNode getNode(short nodeId) {
        return allNodes.get(nodeId);
    }

    /**
     * Get a string representing the host/address on which the local 
     * node should listen
     * @return the listen address
     */
    public String getListenAddress() {
        return listenAddress;
    }

    /**
     * Get the authentication scheme to use for authenticating RPC connections
     * @return the {@link AuthScheme} object
     */
    public AuthScheme getAuthScheme() {
        return authScheme;
    }

    /**
     * Get the key store path where credentials can be found
     * @return the path to a java {@link KeyStore} containing necessary 
     * credentials
     * @see ClusterConfig#getKeyStorePassword()
     */
    public String getKeyStorePath() {
        return keyStorePath;
    }

    /**
     * Get the password for reading data from the {@link KeyStore} returned 
     * by {@link ClusterConfig#getKeyStorePath()}
     * @return the password
     * @see ClusterConfig#getKeyStorePath()
     */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * Return whether this node is allowed to become a leader in the leader
     * election.  If no node is allowed to become the leader then there will
     * be no leader
     * @return
     */
    public boolean isLeaderAllowed() {
        return leaderAllowed;
    }

    /**
     * Add a new node to the cluster
     * @param node the {@link ClusterNode} to add
     * @throws SyncException if the node already exists
     */
    private void addNode(ClusterNode node) throws SyncException {
        Short nodeId = node.getNodeId();
        if (allNodes.get(nodeId) != null) {
            throw new SyncException("Error adding node " + node +
                    ": a node with that ID already exists");
        }
        allNodes.put(nodeId, node);

        Short domainId = node.getDomainId();
        List<ClusterNode> localDomain = localDomains.get(domainId);
        if (localDomain == null) {
            localDomains.put(domainId,
                             localDomain = new ArrayList<ClusterNode>());
        }
        localDomain.add(node);
    }

    private void init(List<ClusterNode> nodes, short thisNodeId,
                      AuthScheme authScheme,
                      String keyStorePath, 
                      String keyStorePassword)
            throws SyncException {
        for (ClusterNode n : nodes) {
            addNode(n);
        }
        thisNode = getNode(thisNodeId);
        if (thisNode == null) {
            throw new SyncException("Cannot set thisNode " +
                    "node: No node with ID " + thisNodeId);
        }
        this.authScheme = authScheme;
        if (this.authScheme == null) 
            this.authScheme = AuthScheme.NO_AUTH;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
    }

    @Override
    public String toString() {
        return "ClusterConfig [allNodes=" + allNodes + ", authScheme="
               + authScheme + ", keyStorePath=" + keyStorePath
               + ", keyStorePassword is " + 
               (keyStorePassword == null ? "unset" : "set") + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((allNodes == null) ? 0 : allNodes.hashCode());
        result = prime * result
                 + ((authScheme == null) ? 0 : authScheme.hashCode());
        result = prime
                 * result
                 + ((keyStorePassword == null) ? 0
                                              : keyStorePassword.hashCode());
        result = prime * result
                 + ((keyStorePath == null) ? 0 : keyStorePath.hashCode());
        result = prime * result
                 + ((localDomains == null) ? 0 : localDomains.hashCode());
        result = prime * result
                 + ((thisNode == null) ? 0 : thisNode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ClusterConfig other = (ClusterConfig) obj;
        if (allNodes == null) {
            if (other.allNodes != null) return false;
        } else if (!allNodes.equals(other.allNodes)) return false;
        if (authScheme != other.authScheme) return false;
        if (keyStorePassword == null) {
            if (other.keyStorePassword != null) return false;
        } else if (!keyStorePassword.equals(other.keyStorePassword))
                                                                    return false;
        if (keyStorePath == null) {
            if (other.keyStorePath != null) return false;
        } else if (!keyStorePath.equals(other.keyStorePath)) return false;
        if (thisNode == null) {
            if (other.thisNode != null) return false;
        } else if (!thisNode.equals(other.thisNode)) return false;
        return true;
    }
}
