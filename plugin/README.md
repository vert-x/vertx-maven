vertx-maven-plugin
==================

Maven Plugin for running verticles in their own vert.x instance.

Install
-----
This version of the plugin is not yet available on Maven Central.

Group ID: io.vertx

Artifact ID: vertx-maven-plugin


Usage
-----

### vertx:init

Create a Vert.x module link

### vertx:runmod

Deploys the current module and starts the module. The module will continue to run until the plugin is explicitly stopped with CTRL-C.  
To start it, type:

	mvn vertx:runMod

You can of course, just use `vertx runmod` to run a module, but using the maven task doesn't require Vert.x to be pre-installed on
your system and uses the versions of Vert.x declared in your pom.xml


### vertx:pullindeps

Sometimes users want the transitive closure of all the modules that your module depends on to be included and packaged inside your module
itself. This means that Vert.x won't need to download the modules at run-time when they are first used (which is default behaviour).

Pulls in all module dependencies. Can be hooked to a maven phase in the pom file or called directly from the command line.
To start it, type:

	mvn vertx:pullindeps


##Example configuration:

	<plugin>
		<groupId>io.vertx</groupId>
		<artifactId>vertx-maven-plugin</artifactId>
		<version>1.0.0-CR1</version>
	</plugin>  


Configuration Options
---------------------

	<configuration>
		<configFile>/path/to/MyVerticle.conf</configFile>
		<instances>1</instances>
	</configuration>

* configFile: The config file to be used.
* instances: The number of verticle instances.
