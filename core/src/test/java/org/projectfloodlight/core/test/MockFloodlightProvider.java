/**
*    Copyright 2011, Big Switch Networks, Inc.
*    Originally created by David Erickson, Stanford University
*
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package org.projectfloodlight.core.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.protocol.factory.BasicFactory;
import org.projectfloodlight.core.FloodlightContext;
import org.projectfloodlight.core.HAListenerTypeMarker;
import org.projectfloodlight.core.IFloodlightProviderService;
import org.projectfloodlight.core.IHAListener;
import org.projectfloodlight.core.IInfoProvider;
import org.projectfloodlight.core.IOFMessageListener;
import org.projectfloodlight.core.IOFSwitch;
import org.projectfloodlight.core.IOFSwitchDriver;
import org.projectfloodlight.core.IOFSwitchListener;
import org.projectfloodlight.core.IReadyForReconcileListener;
import org.projectfloodlight.core.RoleInfo;
import org.projectfloodlight.core.IListener.Command;
import org.projectfloodlight.core.internal.Controller.Counters;
import org.projectfloodlight.core.module.FloodlightModuleContext;
import org.projectfloodlight.core.module.FloodlightModuleException;
import org.projectfloodlight.core.module.IFloodlightModule;
import org.projectfloodlight.core.module.IFloodlightService;
import org.projectfloodlight.core.util.ListenerDispatcher;
import org.projectfloodlight.packet.Ethernet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class MockFloodlightProvider implements IFloodlightModule, IFloodlightProviderService {
    protected static Logger log = LoggerFactory.getLogger(MockFloodlightProvider.class);
    protected ConcurrentMap<OFType, ListenerDispatcher<OFType,IOFMessageListener>> listeners;
    protected List<IOFSwitchListener> switchListeners;
    protected ListenerDispatcher<HAListenerTypeMarker, IHAListener> haListeners;
    protected Map<Long, IOFSwitch> switches;
    protected BasicFactory factory;
    private Role role;

    /**
     *
     */
    public MockFloodlightProvider() {
        listeners = new ConcurrentHashMap<OFType, ListenerDispatcher<OFType,
                                   IOFMessageListener>>();
        switches = new ConcurrentHashMap<Long, IOFSwitch>();
        switchListeners = new CopyOnWriteArrayList<IOFSwitchListener>();
        haListeners =
                new ListenerDispatcher<HAListenerTypeMarker, IHAListener>();
        factory = BasicFactory.getInstance();
        role = null;
    }

    @Override
    public synchronized void addOFMessageListener(OFType type,
                                                  IOFMessageListener listener) {
        ListenerDispatcher<OFType, IOFMessageListener> ldd =
                listeners.get(type);
        if (ldd == null) {
            ldd = new ListenerDispatcher<OFType, IOFMessageListener>();
            listeners.put(type, ldd);
        }
        ldd.addListener(type, listener);
    }

    @Override
    public synchronized void removeOFMessageListener(OFType type,
                                                     IOFMessageListener listener) {
        ListenerDispatcher<OFType, IOFMessageListener> ldd =
                listeners.get(type);
        if (ldd != null) {
            ldd.removeListener(listener);
        }
    }

    /**
     * @return the listeners
     */
    @Override
    public Map<OFType, List<IOFMessageListener>> getListeners() {
        Map<OFType, List<IOFMessageListener>> lers =
                new HashMap<OFType, List<IOFMessageListener>>();
        for(Entry<OFType, ListenerDispatcher<OFType, IOFMessageListener>> e :
            listeners.entrySet()) {
            lers.put(e.getKey(), e.getValue().getOrderedListeners());
        }
        return Collections.unmodifiableMap(lers);
    }

    public void clearListeners() {
        this.listeners.clear();
    }

    @Override
    public Map<Long,IOFSwitch> getAllSwitchMap() {
        return Collections.unmodifiableMap(this.switches);
    }

    @Override
    public Set<Long> getAllSwitchDpids() {
        // the contract for getAllSwitchDpids says the caller will own the
        // returned set
        return new HashSet<Long>(this.switches.keySet());
    }

    @Override
    public IOFSwitch getSwitch(long dpid) {
        return this.switches.get(dpid);
    }

    public void setSwitches(Map<Long, IOFSwitch> switches) {
        this.switches = switches;
    }
    
    public void addSwitch(IOFSwitch sw) {
        this.switches.put(sw.getId(), sw);
    }
    
    public void addSwitch(IOFSwitch sw, long id) {
        this.switches.put(id, sw);
    }

    @Override
    public void addOFSwitchListener(IOFSwitchListener listener) {
        switchListeners.add(listener);
    }

    @Override
    public void removeOFSwitchListener(IOFSwitchListener listener) {
        switchListeners.remove(listener);
    }

    public void dispatchMessage(IOFSwitch sw, OFMessage msg) {
        dispatchMessage(sw, msg, new FloodlightContext());
    }

    public void dispatchMessage(IOFSwitch sw, OFMessage msg, FloodlightContext bc) {
        List<IOFMessageListener> theListeners = listeners.get(msg.getType()).getOrderedListeners();
        if (theListeners != null) {
            Command result = Command.CONTINUE;
            Iterator<IOFMessageListener> it = theListeners.iterator();
            if (OFType.PACKET_IN.equals(msg.getType())) {
                OFPacketIn pi = (OFPacketIn)msg;
                Ethernet eth = new Ethernet();
                eth.deserialize(pi.getPacketData(), 0, pi.getPacketData().length);
                IFloodlightProviderService.bcStore.put(bc,
                        IFloodlightProviderService.CONTEXT_PI_PAYLOAD,
                        eth);
            }
            while (it.hasNext() && !Command.STOP.equals(result)) {
                result = it.next().receive(sw, msg, bc);
            }
        }
    }

    @Override
    public void handleOutgoingMessage(IOFSwitch sw, OFMessage m, FloodlightContext bc) {
        List<IOFMessageListener> msgListeners = null;
        if (listeners.containsKey(m.getType())) {
            msgListeners = listeners.get(m.getType()).getOrderedListeners();
        }

        if (msgListeners != null) {
            for (IOFMessageListener listener : msgListeners) {
                if (Command.STOP.equals(listener.receive(sw, m, bc))) {
                    break;
                }
            }
        }
    }

    public void handleOutgoingMessages(IOFSwitch sw, List<OFMessage> msglist, FloodlightContext bc) {
        for (OFMessage m:msglist) {
            handleOutgoingMessage(sw, m, bc);
        }
    }

    /**
     * @return the switchListeners
     */
    public List<IOFSwitchListener> getSwitchListeners() {
        return switchListeners;
    }

    @Override
    public void terminate() {
    }

    @Override
    public boolean injectOfMessage(IOFSwitch sw, OFMessage msg) {
        dispatchMessage(sw, msg);
        return true;
    }

    @Override
    public boolean injectOfMessage(IOFSwitch sw, OFMessage msg,
                                   FloodlightContext bContext) {
        dispatchMessage(sw, msg, bContext);
        return true;
    }

    @Override
    public BasicFactory getOFMessageFactory() {
        return factory;
    }

    @Override
    public void run() {
        logListeners();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>(1);
        services.add(IFloodlightProviderService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {
        Map<Class<? extends IFloodlightService>,
            IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>,
                        IFloodlightService>();
        m.put(IFloodlightProviderService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>>
            getModuleDependencies() {
        return null;
    }

    @Override
    public void init(FloodlightModuleContext context)
                                                 throws FloodlightModuleException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addInfoProvider(String type, IInfoProvider provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeInfoProvider(String type, IInfoProvider provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getControllerInfo(String type) {
        // mock up something
        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("test-summary-1", 2);
        summary.put("test-summary-2", 5);
        return summary;
    }

    @Override
    public void addHAListener(IHAListener listener) {
        haListeners.addListener(null,listener);
    }

    @Override
    public void removeHAListener(IHAListener listener) {
        haListeners.removeListener(listener);
    }

    @Override
    public Role getRole() {
        /* DISABLE THIS CHECK FOR NOW. OTHER UNIT TESTS NEED TO BE UPDATED
         * FIRST
        if (this.role == null)
            throw new IllegalStateException("You need to call setRole on "
                       + "MockFloodlightProvider before calling startUp on "
                       + "other modules");
        */
        return this.role;
    }

    @Override
    public void setRole(Role role, String roleChangeDescription) {
        this.role = role;
    }

    /**
     * Dispatches a new role change notification
     * @param oldRole
     * @param newRole
     */
    public void transitionToMaster() {
        for (IHAListener rl : haListeners.getOrderedListeners()) {
            rl.transitionToMaster();
        }
    }


    @Override
    public Map<String, String> getControllerNodeIPs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getSystemStartTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    private void logListeners() {
        for (Map.Entry<OFType,
                       ListenerDispatcher<OFType,
                                          IOFMessageListener>> entry
             : listeners.entrySet()) {

            OFType type = entry.getKey();
            ListenerDispatcher<OFType, IOFMessageListener> ldd =
                    entry.getValue();

            StringBuffer sb = new StringBuffer();
            sb.append("OFListeners for ");
            sb.append(type);
            sb.append(": ");
            for (IOFMessageListener l : ldd.getOrderedListeners()) {
                sb.append(l.getName());
                sb.append(",");
            }
            log.debug(sb.toString());
        }
    }

    @Override
    public void setAlwaysClearFlowsOnSwActivate(boolean value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addOFSwitchDriver(String desc, IOFSwitchDriver driver) {
        // TODO Auto-generated method stub

    }

    @Override
    public RoleInfo getRoleInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Long> getMemory() {
        Map<String, Long> m = new HashMap<String, Long>();
        m.put("total", 1000000000L);
        m.put("free", 20000000L);
        return m;
    }

    @Override
    public Long getUptime() {
        return 1000000L;
    }

    @Override
    public void addReadyForReconcileListener(IReadyForReconcileListener l) {
        // do nothing.
    }

    @Override
    public void addSwitchEvent(long switchDPID, String reason, boolean flushNow) {
        // TODO Auto-generated method stub
    }

    @Override
    public Set<String> getUplinkPortPrefixSet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getControllerId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Counters getCounters() {
        // TODO Auto-generated method stub
        return null;
    }
}
