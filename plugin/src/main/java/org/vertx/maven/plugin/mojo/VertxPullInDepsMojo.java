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

@Mojo(name = "pullInDeps", requiresProject = true, threadSafe = false, requiresDependencyResolution =
    COMPILE_PLUS_RUNTIME)
public class VertxPullInDepsMojo extends BaseVertxMojo {

  @Parameter(property = "vertx.pullInDeps", defaultValue = "false")
  protected Boolean pullInDeps;

  protected Throwable error;
  
  @Override
  public void execute() throws MojoExecutionException {
    if (pullInDeps) {
      ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
      try {
        setVertxMods();
        Thread.currentThread().setContextClassLoader(createClassLoader());
        PlatformManager pm = factory.createPlatformManager();

        final CountDownLatch latch = new CountDownLatch(1);
        pm.pullInDependencies(moduleName,
          new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(final AsyncResult<Void> event) {
              if (!event.succeeded()) {
                error = event.cause();
              }
              latch.countDown();
            }
          });
        latch.await(MAX_VALUE, MILLISECONDS);
      } catch (final Exception e) {
        throw new MojoExecutionException(e.getMessage());
      } finally {
        Thread.currentThread().setContextClassLoader(oldTCCL);
        if (error != null)
        	throw new MojoExecutionException("pullInDeps failed", error);
      }
    }
  }
}
