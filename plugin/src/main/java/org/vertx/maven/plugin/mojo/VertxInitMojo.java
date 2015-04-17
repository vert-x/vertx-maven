package org.vertx.maven.plugin.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.PlatformManager;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CountDownLatch;

import static java.lang.Long.MAX_VALUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.vertx.java.platform.PlatformLocator.factory;

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

/**
 * @description Creates a module link for the module
 */
@Mojo(name = "init", requiresProject = true, threadSafe = false, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class VertxInitMojo extends BaseVertxMojo {

	private static final Object lock = new Object();

	@Override
	public void execute() throws MojoExecutionException {
		synchronized (lock) {
			try {
				setVertxMods();

				File cpFile = new File("vertx_classpath.txt");
				if (!cpFile.exists()) {
					cpFile.createNewFile();
					String defaultCp = "src/main/resources\r\n" +
							"target/classes\r\n" +
							"target/dependencies\r\n" +
							"bin\r\n";
					try (FileWriter writer = new FileWriter(cpFile)) {
						writer.write(defaultCp);
					}
				}

				final PlatformManager pm = factory.createPlatformManager();

				final CountDownLatch latch = new CountDownLatch(1);
				pm.createModuleLink(moduleName, new Handler<AsyncResult<Void>>() {
					@Override
					public void handle(AsyncResult<Void> asyncResult) {
						if (!asyncResult.succeeded()) {
							getLog().info(asyncResult.cause().getMessage());
						}
						latch.countDown();
					}
				});
				latch.await(MAX_VALUE, MILLISECONDS);
			} catch (final Exception e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}
		}
	}
}
