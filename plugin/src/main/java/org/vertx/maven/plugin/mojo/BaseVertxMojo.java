package org.vertx.maven.plugin.mojo;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManager;

import java.io.File;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static java.nio.file.Files.readAllBytes;
import static org.vertx.java.platform.PlatformLocator.factory;

public abstract class BaseVertxMojo extends AbstractMojo {

  @Component
  protected MavenProject project;

  /**
   * The name of the module to run.
   * <p/>
   * If you're running a module, it's the name of the module to be run.
   */
  @Parameter(property = "moduleName", defaultValue = "${project.groupId}~${project.artifactId}~${project.version}")
  protected String moduleName;

  /**
   * <p>
   * The config file for this verticle.
   * </p>
   * <p>
   * If the path is relative (does not start with / or a drive letter like
   * C:), the path is relative to the directory containing the POM.
   * </p>
   * <p>
   * An example value would be src/main/resources/com/acme/MyVerticle.conf
   * </p>
   */
  @Parameter
  protected File configFile = null;

  /**
   * The number of instances of the verticle to instantiate in the vert.x
   * server. The default is 1.
   */
  @Parameter(defaultValue = "1")
  protected Integer instances = 1;

  /**
   * The mods directory.  The default is relative path target/mods.
   */
  @Parameter(defaultValue = "target/mods")
  protected File modsDir;

  /**
   * If specified then the vert.x instance will form a cluster with any other
   * vert.x instances on the network. The default is false.
   */
  @Parameter(defaultValue = "false")
  protected Boolean cluster;

  /**
   * Host to bind to for cluster communication. If this is not specified vert.x
   * will attempt to choose one from the available interfaces.
   */
  @Parameter
  protected String clusterHost;

  /**
   * The default is org.vertx.java.spi.cluster.impl.hazelcast.HazelcastClusterManagerFactory.
   */
  @Parameter(defaultValue = "org.vertx.java.spi.cluster.impl.hazelcast.HazelcastClusterManagerFactory")
  protected String clusterManagerFactory;

  /**
   * Port to use for cluster communication. Default is 0 which means chose a
   * spare random port.
   */
  @Parameter(defaultValue = "0")
  protected Integer clusterPort;

  /**
   * If specified the module will be deployed as a high availability (HA)
   * deployment. This means it can fail over to any other nodes in the cluster
   * started with the same HA group.
   */
  @Parameter(defaultValue = "false")
  protected Boolean ha;

  /**
   * Uused in conjunction with -ha this specifies the HA group this node will
   * join. There can be multiple HA groups in a cluster. Nodes will only
   * failover to other nodes in the same group. Defaults to __DEFAULT__
   */
  @Parameter(defaultValue = "__DEFAULT__")
  protected String hagroup;

  /**
   * Used in conjunction with -ha this specifies the minimum number of nodes
   * in the cluster for any HA deployments to be active. Defaults to 0.
   */
  @Parameter(defaultValue = "0")
  protected Integer quorum;


  protected JsonObject getConf() {
    JsonObject config = null;
    final String confContent = readConfigFile(configFile);
    if (confContent != null && !confContent.isEmpty()) {
      config = new JsonObject(confContent);
    }
    return config;
  }

  private String readConfigFile(final File file) {
    if (file == null || !file.exists() || !file.isFile()) {
      return null;
    }

    try {
      final URI uri = file.toURI();
      return new String(readAllBytes(java.nio.file.Paths.get(uri)));
    } catch (final IOException e) {
      e.printStackTrace();
      // just returns an empty string. Nothing to be thrown
    }

    return null;
  }

  protected PlatformManager createPlatformManager() {
    PlatformManager mgr;

    if (cluster || ha) {
      getLog().info("Starting clustering...");

      if (System.getProperty("vertx.clusterManagerFactory", null) == null) {
          System.setProperty("vertx.clusterManagerFactory", clusterManagerFactory);
      }
      getLog().debug("clusterManagerFactory: " + System.getProperty("vertx.clusterManagerFactory"));

      if (clusterHost == null) {
        clusterHost = getDefaultAddress();
        if (clusterHost == null) {
          getLog().error("Unable to find a default network interface for clustering. Please specify one using cluster-host configuration parameter");
          return null;
        } else {
          getLog().info("No cluster-host specified so using address " + clusterHost);
        }
      }

      if (ha) {
        int quorumSize = quorum == null ? 0 : Integer.valueOf(quorum);
        mgr = factory.createPlatformManager(clusterPort, clusterHost, quorumSize, hagroup);
      } else {
        mgr = factory.createPlatformManager(clusterPort, clusterHost);
      }
    } else {
      mgr = factory.createPlatformManager();
    }
    return mgr;
  }

  private String getDefaultAddress() {
    Enumeration<NetworkInterface> nets;
    try {
      nets = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      return null;
    }

    NetworkInterface netinf;
    while (nets.hasMoreElements()) {
      netinf = nets.nextElement();
      Enumeration<InetAddress> addresses = netinf.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress address = addresses.nextElement();
        if (!address.isAnyLocalAddress() && !address.isMulticastAddress() && !(address instanceof Inet6Address)) {
          return address.getHostAddress();
        }
      }
    }

    return null;
  }

  protected ClassLoader createClassLoader() throws Exception {
    // We need to add some extra entries to the classpath so that any overrridden Vert.x platform config,
    // e.g. cluster.xml, langs.properties etc can picked up when running the module
    // Users can put such config either in a src/main/platform_lib directory (if they don't want it in the module)
    // or in a src/main/resources/platform_lib directory (if they want it in the module, e.g. for fatjars)
    List<URL> urls = new ArrayList<>();
    addURLs(urls, "src/main/platform_lib");
    addURLs(urls, "src/main/resources/platform_lib");

    return new LoadFirstClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
  }

  protected void setVertxMods() throws Exception {
    String vertxMods = System.getenv("VERTX_MODS");
    if (vertxMods != null) {
      modsDir = new File(vertxMods);
    }
    System.setProperty("vertx.mods", modsDir.getCanonicalPath());
  }

  private void addURLs(List<URL> urls, String dirName) throws IOException {
    File dir = new File(dirName);
    if (dir.exists()) {
      urls.add(dir.getCanonicalFile().toURI().toURL());
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file: files) {
          String path = file.getCanonicalPath();
          if (path.endsWith(".jar") || path.endsWith(".zip")) {
            urls.add(file.getCanonicalFile().toURI().toURL());
          }
        }
      }
    }
  }

  private static class LoadFirstClassLoader extends URLClassLoader {

    private final ClassLoader parent;

    private LoadFirstClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
      this.parent = parent;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class<?> c = findLoadedClass(name);
      if (c != null) {
        return c;
      } else {
        // Try and load with this first
        try {
          c = findClass(name);
          if (resolve) {
            resolveClass(c);
          }
          return c;
        } catch (ClassNotFoundException e) {
          return parent.loadClass(name);
        }
      }
    }
  }
}

