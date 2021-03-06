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

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hoya.HoyaExitCodes;
import org.apache.hadoop.hoya.exceptions.HoyaException;
import org.apache.hadoop.hoya.yarn.client.HoyaClient;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a cluster specification; designed to be sendable over the wire
 * and persisted in JSON by way of Jackson.
 * As a wire format it is less efficient in both xfer and ser/deser than 
 * a binary format, but by having one unified format for wire and persistence,
 * the code paths are simplified.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterDescription {
  protected static final Logger
    LOG = LoggerFactory.getLogger(ClusterDescription.class);

  private static final String UTF_8 = "UTF-8";

  /**
   * version counter
   */
  public String version = "1.0";

  /**
   * Name of the cluster
   */
  public String name;

  /**
   * State of the cluster
   */
  public int state;
  
  /*
   State list for both clusters and nodes in them. Ordered so that destroyed follows
   stopped.
   
   Some of the states are only used for recording
   the persistent state of the cluster and are not
   seen in node descriptions
   */

  /**
   * Specification is incomplete & cannot
   * be used: {@value}
   */
  public static final int STATE_INCOMPLETE = 0;

  /**
   * Spec has been submitted: {@value}
   */
  public static final int STATE_SUBMITTED = 1;
  /**
   * Cluster created: {@value}
   */
  public static final int STATE_CREATED = 2;
  /**
   * Live: {@value}
   */
  public static final int STATE_LIVE = 3;
  /**
   * Stopped
   */
  public static final int STATE_STOPPED = 4;
  /**
   * destroyed
   */
  public static final int STATE_DESTROYED = 5;
  /**
   * When was the cluster created?
   */
  public long createTime;
  /**
   * When was the cluster last started?
   */
  public long startTime;

  /**
   * When was the cluster last stopped: should be 0 if the cluster is live
   */
  public long stopTime;
  /**
   * when was this status document created
   */
  public long statusTime;

  /**
   * The path to the original configuration
   * files; these are re-read when 
   * restoring a cluster
   */

  public String originConfigurationPath;
  public String generatedConfigurationPath;
  public int workers;
  public int workerHeap;
  public int masters;
  public int masterHeap;
  public int masterInfoPort;
  public int workerInfoPort;
  public String zkHosts;
  public int zkPort;
  public String zkPath;
  /**
   * This is where the data goes
   */
  public String hbaseDataPath;

  /**
   * HBase home: if non-empty defines where a copy of HBase is preinstalled
   */
  public String hbaseHome;

  /**
   * The path in HDFS where the HBase image must go
   */
  public String imagePath;


  public String xHBaseMasterCommand;

  /**
   * Roles mapping to sub-maps
   */
  public Map<String, Object> roles =
    new HashMap<String, Object>();
  
  /**
   * Extra flags that can be used to communicate
   * between client & server; and across versions.
   * Presence of a value is seen as marking the flag as true
   */
  public Map<String, Boolean > flags =
    new HashMap<String, Boolean>();

  /**
   * Statistics
   */
  public Map<String, Long> stats =
    new HashMap<String, Long>();
  
  /**
   * Instances: role->count
   */
  public Map<String, Integer> instances =
    new HashMap<String, Integer>();


  /**
   * Role options, 
   * role -> option -> value
   */
  public Map<String,Map<String, String>> roleopts =
    new HashMap<String, Map<String, String>>();

  /**
   * List of key-value pairs to add to a client config to set up the client
   */
  public Map<String, String> clientProperties =
    new HashMap<String, String>();

  /**
   * Verify that a cluster specification exists
   * @param clustername name of the cluster (For errors only)
   * @param fs filesystem
   * @param clusterSpecPath cluster specification path
   * @throws IOException IO problems
   * @throws HoyaException if the cluster is not present
   */
  public static void verifyClusterSpecExists(String clustername,
                                             FileSystem fs,
                                             Path clusterSpecPath) throws
                                                                      IOException,
                                                                      HoyaException {
      if (!fs.exists(clusterSpecPath)) {
        LOG.debug("Missing cluster specification file {}", clusterSpecPath);
        throw new HoyaException(HoyaExitCodes.EXIT_UNKNOWN_HOYA_CLUSTER,
                                HoyaClient.E_UNKNOWN_CLUSTER + clustername +
                                "\n (cluster definition not found at " +
                                clusterSpecPath);
      }
    }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Hoya Cluster ").append(name).append('\n');
    builder.append("State: ").append(state).append('\n');
    if (startTime > 0) {
      builder.append("Started: ");
      builder.append(new Date(startTime).toLocaleString());
    }
    if (stopTime > 0) {
      builder.append("Stopped: ");
      builder.append(new Date(startTime).toLocaleString());
    }
    builder.append("Workers: ")
           .append(workers)
           .append("- Masters")
           .append(masters)
           .append('\n');
    builder.append("ZK cluster: ").append(zkHosts).
      append(" : ").append(zkPort).append('\n');
    builder.append("ZK path: ").append(zkPath).append('\n');
    for (Map.Entry<String, Integer> entry: instances.entrySet()) {
      builder.append("Role")
             .append(entry.getKey()).append(": ")
             .append(entry.getValue()).append("\n");
    }
    return builder.toString();
  }

  /**
   * Save a cluster description to a hadoop filesystem
   * @param fs filesystem
   * @param path path
   * @param overwrite should any existing file be overwritten
   * @throws IOException IO excpetion
   */
  public void save(FileSystem fs, Path path, boolean overwrite) throws
                                                                IOException {
    String json = toJsonString();
    FSDataOutputStream dataOutputStream = fs.create(path, overwrite);
    byte[] b = json.getBytes(UTF_8);
    try {
      dataOutputStream.write(b);
    } finally {
      dataOutputStream.close();
    }
  }

  /**
   * Load from the filesystem
   * @param fs filesystem
   * @param path path
   * @return a loaded CD
   * @throws IOException IO problems
   */
  public static ClusterDescription load(FileSystem fs, Path path)
    throws IOException, JsonParseException, JsonMappingException {
    FileStatus status = fs.getFileStatus(path);
    byte[] b = new byte[(int) status.getLen()];
    FSDataInputStream dataInputStream = fs.open(path);
    int count = dataInputStream.read(b);
    String json = new String(b, 0, count, UTF_8);
    return fromJson(json);
  }


  /**
   * Convert to a JSON string
   * @return a JSON string description
   * @throws IOException Problems mapping/writing the object
   */
  public String toJsonString() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(this);
  }


  /**
   * Convert from JSON
   * @param json input
   * @return the parsed JSON
   * @throws IOException IO
   */
  public static ClusterDescription fromJson(String json)
    throws IOException, JsonParseException, JsonMappingException {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(json, ClusterDescription.class);
    } catch (IOException e) {
      LOG.error("Exception while parsing json : " + e + "\n" + json, e);
      throw e;
    }
  }
  
  public boolean getFlag(String flag, boolean defVal) {
    Boolean b = flags.get(flag);
    return (b != null)? b : defVal;
  }
  
  public void setFlag(String flag, boolean val) {
    flags.put(flag, val);
  }

  /**
   * Get a role option
   * @param role role to get from
   * @param option option name
   * @param defVal default value
   * @return resolved value
   */
  public String getRoleOpt(String role, String option, String defVal) {
    Map<String, String> options = roleopts.get(role);
    if (options==null) {
      return defVal;
    }
    String val = options.get(option);
    return val != null? val: defVal;
  }
  
  /**
   * Get a role opt; use {@link Integer#decode(String)} so as to take hex
   * oct and bin values too.
   * 
   * @param role role to get from
   * @param option option name
   * @param defVal default value
   * @return parsed value
   * @throws NumberFormatException if the role could not be parsed.
   */
  public int getRoleOptInt(String role, String option, int defVal) {
    String val = getRoleOpt(role, option, Integer.toString(defVal));
    return Integer.decode(val);
  }
  
  public void setRoleOpt(String role, String option, String val) {
    Map<String, String> options = roleopts.get(role);
    if (options == null) {
      options = new HashMap<String, String>();
      roleopts.put(role, options);
    }
    options.put(option, val);
  }
  
  public void setRoleOpt(String role, String option, int val) {
    Map<String, String> options = roleopts.get(role);
    if (options == null) {
      options = new HashMap<String, String>();
      roleopts.put(role, options);
    }
    options.put(option, Integer.toString(val));
  }
  
  
}
