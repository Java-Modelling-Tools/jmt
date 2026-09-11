/**
 * Copyright (C) 2016, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package NodeSections;

import jmt.common.exception.NetException;
import NetStrategies.QueuePutStrategy;
import NetStrategies.ServiceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static QueueNet.EngineNodeConstants.*;

public class PreemptiveServer extends Server {

    public PreemptiveServer(Integer numberOfServers,
                            Integer[] numberOfVisits,
                            ServiceStrategy[] serviceStrategies,
                            QueuePutStrategy[] preemptiveStrategies) throws NetException {
        this(numberOfServers, numberOfVisits, serviceStrategies, preemptiveStrategies, null, null, null, null, null);
    }

    public PreemptiveServer(Integer numberOfServers,
                            Integer[] numberOfVisitsPerClass,
                            ServiceStrategy[] serviceStrategies,
                            QueuePutStrategy[] preemptiveStrategies,
                            Integer[] serverNumRequired,
                            String[] serverNames,
                            Integer[] serversPerServerType,
                            Object[] serverCompatibilities,
                            String schedulingPolicy) throws NetException {
        this(numberOfServers, numberOfVisitsPerClass, serviceStrategies,
                ((Supplier<Map<String, Object>>) () -> {
                    Map<String, Object> advancedFeatures = new HashMap<>();
                    advancedFeatures.put(SERVER_FEATURE_PREEMPTIVE_STRATEGIES, preemptiveStrategies);
                    advancedFeatures.put(SERVER_FEATURE_CLASS_PARALLELISM, serverNumRequired);
                    advancedFeatures.put(SERVER_FEATURE_SERVER_NAMES, serverNames);
                    advancedFeatures.put(SERVER_FEATURE_SERVERS_PER_SERVER_TYPE, serversPerServerType);
                    advancedFeatures.put(SERVER_FEATURE_SERVER_COMPATIBILITIES, serverCompatibilities);
                    advancedFeatures.put(SERVER_FEATURE_SCHEDULING_POLICY, schedulingPolicy);
                    return advancedFeatures;
                }).get());
    }

    public PreemptiveServer(Integer numberOfServers,
                            Integer[] numberOfVisitsPerClass,
                            ServiceStrategy[] serviceStrategies,
                            Map<String, Object> advancedFeatures) throws NetException {
        super(numberOfServers, numberOfVisitsPerClass, serviceStrategies, advancedFeatures);
    }
}
