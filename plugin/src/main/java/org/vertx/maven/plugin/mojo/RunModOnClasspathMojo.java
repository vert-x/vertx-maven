package org.vertx.maven.plugin.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static java.lang.Long.MAX_VALUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
@Mojo(name = "runModOnCP", requiresProject = true, threadSafe = false, requiresDependencyResolution =
    COMPILE_PLUS_RUNTIME)
public class RunModOnClasspathMojo extends BaseVertxMojo {

  /**
   * The classpath
   */
  @Parameter(property = "classpath", defaultValue = "")
  protected String classpath;

  @Override
  public void execute() throws MojoExecutionException {
    List<URL> urls = new ArrayList<>();
    classpath = classpath.trim();
    if (!classpath.isEmpty()) {
      String[] arr = classpath.split(System.getProperty("path.separator"));
      for (String part: arr) {
        File file = new File(part);
        try {
          urls.add(file.toURI().toURL());
        } catch (MalformedURLException e) {
          throw new MojoExecutionException(e.getMessage());
        }
      }
    }
    URL[] urlArray = urls.isEmpty() ? null : urls.toArray(new URL[urls.size()]);
    doExecute(urlArray);
  }

  protected void doExecute(URL[] classpath) throws MojoExecutionException {

    try {
      for (final Map.Entry<String, String> entry : systemPropertyVariables.entrySet()) {
        System.setProperty(entry.getKey(), entry.getValue());
      }
      System.setProperty("vertx.mods", modsDir.getAbsolutePath());
      final PlatformManager pm = PlatformLocator.factory.createPlatformManager();
      final CountDownLatch latch = new CountDownLatch(1);
      pm.deployModuleFromClasspath(moduleName, getConf(), instances, classpath,
          new Handler<AsyncResult<String>>() {
            @Override
            public void handle(final AsyncResult<String> event) {
              if (event.succeeded()) {
                getLog().info("CTRL-C to stop server");
              } else {
                if (event.cause() != null) {
                  getLog().error(event.cause());
                } else {
                  getLog().info("Could not find the module. Did you forget to do mvn package?");
                }
                latch.countDown();
              }
            }
          });
      latch.await(MAX_VALUE, MILLISECONDS);
    } catch (final Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(e.getMessage());
    }
  }
}
