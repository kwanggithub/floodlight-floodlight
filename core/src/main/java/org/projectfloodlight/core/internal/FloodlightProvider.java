/**
 *    Copyright 2013, Big Switch Networks, Inc.
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

package org.projectfloodlight.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.core.IFloodlightProviderService;
import org.projectfloodlight.core.module.FloodlightModuleContext;
import org.projectfloodlight.core.module.FloodlightModuleException;
import org.projectfloodlight.core.module.IFloodlightModule;
import org.projectfloodlight.core.module.IFloodlightService;
import org.projectfloodlight.counter.ICounterStoreService;
import org.projectfloodlight.db.IBigDBService;
import org.projectfloodlight.debugcounter.IDebugCounterService;
import org.projectfloodlight.debugevent.IDebugEventService;
import org.projectfloodlight.perfmon.IPktInProcessingTimeService;
import org.projectfloodlight.sync.IClusterService;
import org.projectfloodlight.sync.ISyncService;
import org.projectfloodlight.threadpool.IThreadPoolService;

public class FloodlightProvider implements IFloodlightModule {
    Controller controller;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> services =
                new ArrayList<Class<? extends IFloodlightService>>(1);
        services.add(IFloodlightProviderService.class);
        return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>,
               IFloodlightService> getServiceImpls() {
        controller = new Controller();

        Map<Class<? extends IFloodlightService>,
            IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>,
                            IFloodlightService>();
        m.put(IFloodlightProviderService.class, controller);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> 
            getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> dependencies =
            new ArrayList<Class<? extends IFloodlightService>>(4);
        dependencies.add(IPktInProcessingTimeService.class);
        dependencies.add(IBigDBService.class);
        dependencies.add(ICounterStoreService.class);
        dependencies.add(IDebugCounterService.class);
        dependencies.add(IDebugEventService.class);
        dependencies.add(IThreadPoolService.class);
        dependencies.add(ISyncService.class);
        return dependencies;
    }

    @Override
    public void init(FloodlightModuleContext context) 
            throws FloodlightModuleException {
       controller.setPktInProcessingService(
           context.getServiceImpl(IPktInProcessingTimeService.class));
       controller.setCounterStore(
           context.getServiceImpl(ICounterStoreService.class));
       controller.setDebugCounter(
           context.getServiceImpl(IDebugCounterService.class));
       controller.setDebugEvent(
           context.getServiceImpl(IDebugEventService.class));
       controller.setBigDBService(
           context.getServiceImpl(IBigDBService.class));
       controller.setThreadPoolService(
           context.getServiceImpl(IThreadPoolService.class));
       controller.setSyncService(
           context.getServiceImpl(ISyncService.class));
       controller.setClusterService(
           context.getServiceImpl(IClusterService.class));
       controller.init(context.getConfigParams(this));
    }

    @Override
    public void startUp(FloodlightModuleContext context) 
            throws FloodlightModuleException {
        controller.startupComponents(context.getModuleLoader());
    }
}
