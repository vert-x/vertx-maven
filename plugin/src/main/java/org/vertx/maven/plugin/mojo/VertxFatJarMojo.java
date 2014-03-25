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

package org.vertx.maven.plugin.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.PlatformManager;

import java.util.concurrent.CountDownLatch;

import static java.lang.Long.MAX_VALUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.vertx.java.platform.PlatformLocator.factory;

@Mojo(name = "fatJar", requiresProject = true, threadSafe = false, requiresDependencyResolution =
    COMPILE_PLUS_RUNTIME)
public class VertxFatJarMojo extends BaseVertxMojo {

  @Parameter(property = "vertx.createFatJar", defaultValue = "false")
  protected Boolean createFatJar;

  @Override
  public void execute() throws MojoExecutionException {
    try {
      if (createFatJar) {
        System.setProperty("vertx.mods", modsDir.getAbsolutePath());
        final PlatformManager pm = factory.createPlatformManager();
        final CountDownLatch latch = new CountDownLatch(1);
        pm.makeFatJar(moduleName, project.getBuild().getDirectory(),
            new Handler<AsyncResult<Void>>() {
              @Override
              public void handle(final AsyncResult<Void> event) {
                if (event.succeeded()) {
                  latch.countDown();
                } else {
                  if (!event.succeeded()) {
                    getLog().error(event.cause());
                  }
                  latch.countDown();
                }
              }
            });
        latch.await(MAX_VALUE, MILLISECONDS);
      }
    } catch (final Exception e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }
}

