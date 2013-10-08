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
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static java.nio.file.Files.readAllBytes;

public abstract class BaseVertxMojo extends AbstractMojo {

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
  protected Integer instances;

  /**
   * The mods directory.  The default is relative path target/mods.
   */
  @Parameter(defaultValue = "target/mods")
  protected File modsDir;

  /**
   * List of system properties to set when running a module.
   */
  protected Map<String, String> systemPropertyVariables = Collections.EMPTY_MAP;

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
}

