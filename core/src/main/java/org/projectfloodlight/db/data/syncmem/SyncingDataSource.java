package org.projectfloodlight.db.data.syncmem;

import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.db.BigDBException;
import org.projectfloodlight.db.auth.AuthContext;
import org.projectfloodlight.db.data.DataNode;
import org.projectfloodlight.db.data.DataNodeFactory;
import org.projectfloodlight.db.data.MutationListener;
import org.projectfloodlight.db.data.TransactionalDataSource;
import org.projectfloodlight.db.data.TreespaceAware;
import org.projectfloodlight.db.data.DataNode.DataNodeWithPath;
import org.projectfloodlight.db.data.memory.DelegatableDataSource;
import org.projectfloodlight.db.data.persistmem.PersistMemDataSource;
import org.projectfloodlight.db.query.Query;
import org.projectfloodlight.db.schema.Schema;
import org.projectfloodlight.db.service.Treespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncingDataSource implements TransactionalDataSource, TreespaceAware  {
    private static Logger logger = LoggerFactory.getLogger(SyncingDataSource.class);

    private static final String DEFAULT_URI_TEMPLATE = "http://%s:8098/config";
    private static final int DEFAULT_SERVER_PORT = 8098;
    private final DelegatableDataSource delegate;
    private final MiniSync sync;

    public SyncingDataSource(DelegatableDataSource delegate, MiniSync syncer) {
        this.delegate = delegate;
        this.sync = syncer;
    }

    public SyncingDataSource(String name, boolean config, Schema schema, Map<String, String> properties)
            throws BigDBException {
        this.delegate = new PersistMemDataSource(name, config, schema, properties);

        String uriTemplate = properties.containsKey("uriTemplate") ? properties.get("uriTemplate") : DEFAULT_URI_TEMPLATE;
        int serverPort = properties.containsKey("serverPort") ? Integer.parseInt(properties.get("serverPort")) : DEFAULT_SERVER_PORT;
        this.sync = new MiniSync(delegate, uriTemplate, serverPort);
        if(properties.containsKey("slaveIps")) {
            int i = 0;
            Map<String, String> slaveMap = new HashMap<String, String>();
            for(String ip:  properties.get("slaveIps").split("\\s*,\\s*")) {
                slaveMap.put("slave"+i, ip);
                i++;
            }
            sync.setControllerNodeIPs(slaveMap.entrySet());
        }

        if(logger.isDebugEnabled())
            logger.debug("Creating syncingDataSource. serverPort"+serverPort+", uriTemplate="+uriTemplate+"");

        MiniSyncModule.getDefault().addMiniSync(sync);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isConfig() {
        return delegate.isConfig();
    }

    @Override
    public DataNodeFactory getDataNodeFactory() {
        return delegate.getDataNodeFactory();
    }

    @Override
    public void setTreespace(Treespace treespace) {
        if(delegate instanceof TreespaceAware)
            ((TreespaceAware) delegate).setTreespace(treespace);
    }

    @Override
    public void setMutationListener(MutationListener listener)
            throws BigDBException {
        delegate.setMutationListener(listener);
    }

    @Override
    public Iterable<DataNodeWithPath> queryData(Query query,
            AuthContext authContext) throws BigDBException {
        return delegate.queryData(query, authContext);
    }

    @Override
    public void insertData(Query query, DataNode data,
            AuthContext authContext) throws BigDBException {
        checkRole();
        delegate.insertData(query, data, authContext);
        sync.update(delegate.getRoot());
    }

    private void checkRole() throws MutationOnSlaveException {
        if(sync.getRole() != SyncRole.MASTER) {
            throw new MutationOnSlaveException();
        }

    }

    @Override
    public void replaceData(Query query, DataNode data,
            AuthContext authContext) throws BigDBException {
        checkRole();
        delegate.replaceData(query, data, authContext);
        sync.update(delegate.getRoot());
    }
    @Override
    public void updateData(Query query, DataNode data,
            AuthContext authContext) throws BigDBException {
        checkRole();
        delegate.updateData(query, data, authContext);
        sync.update(delegate.getRoot());
    }

    @Override
    public void
            deleteData(Query query, AuthContext authContext)
                    throws BigDBException {
        checkRole();
        delegate.deleteData(query, authContext);
        sync.update(delegate.getRoot());
    }

    @Override
    public DataNode getRoot() throws BigDBException {
        return delegate.getRoot();
    }

    @Override
    public DataNode getRoot(AuthContext authContext, Query query)
            throws BigDBException {
        return delegate.getRoot(authContext, query);
    }

    @Override
    public State getState() {
        return delegate.getState();
    }

    @Override
    public synchronized void startup() throws BigDBException {
        delegate.startup();
        this.sync.update(getRoot());
        this.sync.start();
    }

    @Override
    public synchronized void shutdown() throws BigDBException {
        delegate.shutdown();
        this.sync.shutdown();
    }

    /* (non-Javadoc)
     * @see org.projectfloodlight.db.data.TransactionalDataSource#addPreCommitListener(org.projectfloodlight.db.data.TransactionalDataSource.PreCommitListener)
     */
    @Override
    public void addPreCommitListener(PreCommitListener listener) {
        if (this.delegate instanceof TransactionalDataSource)
            ((TransactionalDataSource)
                    this.delegate).addPreCommitListener(listener);

    }

    /* (non-Javadoc)
     * @see org.projectfloodlight.db.data.TransactionalDataSource#addPostCommitListener(org.projectfloodlight.db.data.TransactionalDataSource.PostCommitListener)
     */
    @Override
    public void addPostCommitListener(PostCommitListener listener) {
        if (this.delegate instanceof TransactionalDataSource)
            ((TransactionalDataSource)
                    this.delegate).addPostCommitListener(listener);
    }

}
