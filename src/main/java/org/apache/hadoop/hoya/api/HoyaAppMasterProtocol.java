/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.hadoop.hoya.api;

import org.apache.hadoop.hoya.exceptions.NoSuchNodeException;
import org.apache.hadoop.ipc.VersionedProtocol;
import org.apache.hadoop.yarn.api.records.ContainerId;

import java.io.IOException;
import java.util.List;

/**
 * API for AM operations.
 * 
 * The API references each cluster node with a UUID that will be
 * unique over all instances;when listing nodes the UUID is returned,
 * detail on nodes can be retrieved explicitly
 */
public interface HoyaAppMasterProtocol extends VersionedProtocol {
  public static final long versionID = 0x01;

  /**
   * Stop the cluster
   * @throws IOException IO problems
   */
  public void stopCluster() throws IOException;

  public boolean flexNodes(int workers) throws IOException;

  /**
   * Get the current cluster status
   * @return the {@link ClusterDescription} content as a JSON document
   * @throws IOException IO problems
   */
  public String getClusterStatus() throws IOException;

  /**
   * List all running nodes in a role
   * @param role node role
   * @return a possibly empty array of nodes
   * @throws IOException IO problems
   */
  public String[] listNodesByRole(String role) throws IOException;

  /**
   * Get the JSON-formatted {@link ClusterNode} details on a node
   * @param uuid the UUID
   * @return null if there is no such node
   * @throws NoSuchNodeException if the node cannot be found
   * @throws IOException IO problems
   */
  public String getNode(String uuid) throws IOException, NoSuchNodeException;

  public static final String STAT_CONTAINERS_REQUESTED = "containers.requested";
  public static final String STAT_CONTAINERS_ALLOCATED = "containers.allocated";
  public static final String STAT_CONTAINERS_COMPLETED = "containers.completed";
  public static final String STAT_CONTAINERS_FAILED = "containers.failed";
  public static final String STAT_CONTAINERS_STARTED =
    "containers.start.started";
  public static final String STAT_CONTAINERS_STARTED_FAILED =
    "containers.start.failed";

}
