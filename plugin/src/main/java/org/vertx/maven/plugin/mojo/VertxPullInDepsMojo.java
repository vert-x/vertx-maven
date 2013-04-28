package org.vertx.maven.plugin.mojo;

import static java.lang.Long.MAX_VALUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.vertx.java.platform.PlatformLocator.factory;

import java.util.concurrent.CountDownLatch;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.platform.PlatformManager;

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;
import static org.vertx.java.platform.PlatformLocator.factory;

@Mojo(name = "pullindeps", requiresProject = true, threadSafe = false, requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class VertxPullInDepsMojo extends BaseVertxMojo {

    @Parameter(property = "vertx.pullindeps", defaultValue = "false")
    protected Boolean pullindeps;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            if (pullindeps) {
                System.setProperty("vertx.mods", "target/mods");
                final PlatformManager pm = factory.createPlatformManager();

                final CountDownLatch latch = new CountDownLatch(1);
                pm.pullInDependencies(moduleName,
                        new Handler<AsyncResult<Void>>() {
                            @Override
                            public void handle(final AsyncResult<Void> event) {
                                if (event.succeeded()) {
                                    latch.countDown();
                                } else {
                                    getLog().info(
                                            "Cannot find the module. Did you forget to do mvn package?");
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
