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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.vertx.java.core.json.JsonObject;

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
    protected String configFile;

    /**
     * The number of instances of the verticle to instantiate in the vert.x
     * server. The default is 1.
     */
    @Parameter(property = "run.instances", defaultValue = "1")
    protected Integer instances;

    protected JsonObject getConf() {
        JsonObject config = null;
        final String confContent = readConfigFile(configFile);
        if (confContent != null && !confContent.isEmpty()) {
            config = new JsonObject(confContent);
        }
        return config;
    }

    private String readConfigFile(final String strFile) {
        if (strFile == null || strFile.isEmpty()) {
            return null;
        }

        try {
            final File file = new File(strFile);
            final URI uri = file.toURI();
            return new String(readAllBytes(java.nio.file.Paths.get(uri)));
        } catch (final IOException e) {
            // just returns an empty string. Nothing to be thrown
        }

        return null;
    }
}

