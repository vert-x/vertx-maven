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
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static java.nio.file.Files.readAllBytes;

public abstract class BaseVertxMojo extends AbstractMojo {

  /**
   * The name of the module to run.
   * <p/>
   * If you're running a module, it's the name of the module to be run.
   */
  @Parameter(property = "run.moduleName", defaultValue = "${project.groupId}~${project.artifactId}~${project.version}")
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
  @Parameter(property = "run.configFile")
  protected File configFile;

  /**
   * The number of instances of the verticle to instantiate in the vert.x
   * server. The default is 1.
   */
  @Parameter(property = "run.instances", defaultValue = "1")
  protected Integer instances;

  /**
   * The mods directory.  The default is relative path target/mods.
   */
  @Parameter(property = "vertx.modsdir", defaultValue = "target/mods")
  protected File modsdir;

  protected JsonObject getConf() {
    JsonObject config = null;
    final String confContent = readConfigFile();
    if (confContent != null && !confContent.isEmpty()) {
      config = new JsonObject(confContent);
    }
    return config;
  }

  private String readConfigFile() {
    if (configFile == null) {
      return null;
    }
    if (!configFile.exists()) {
      getLog().error("Config file '" + configFile.getAbsolutePath() + "' does not exist.");
      return null;
    }

    try {
      return new String(readAllBytes(configFile.toPath()));
    } catch (final IOException e) {
      getLog().error(e);
      // just returns an empty string. Nothing to be thrown
    }

    return null;
  }
}

