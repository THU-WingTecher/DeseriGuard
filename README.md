# DeseriGuard

This repository is for the paper "Automatic Policy Synthesis and Enforcement for Protecting Untrusted Deserialization".

## Usage Instructions

### Building the Java Agent with Maven

DeseriGuard is implemented as a Java agent. To build the agent, we use Maven for compilation and packaging. Execute the following command to build the Java agent, which bundles the agent along with all necessary libraries into a single JAR file:

```bash
mvn assembly:assembly
```

This command packages the Java agent into a JAR file, located in the `target` directory. Ensure that you build the Java agent using the same version of Java as the application you intend to protect.

### Launching the Protected Java Application with DeseriGuard

To safeguard your Java application against deserialization attacks, start the application along with the DeseriGuard agent. This allows the agent to analyze the application and perform runtime instrumentation for protection.

To attach the DeseriGuard agent to the JDK, include the following arguments when launching your Java application:

```bash
-javaagent:{PATH_TO_JAVA_AGENT}/agent-test-1.0-SNAPSHOT-jar-with-dependencies.jar={PATH_TO_POLICY}
-Xbootclasspath/a:{ALL_NECESSARY_LIBRARIES}:{PATH_TO_JAVA_AGENT}/agent-test-1.0-SNAPSHOT-jar-with-dependencies.jar
```

These arguments can be passed via a Maven or Ant configuration file, or you can use the environment variable `JAVA_TOOL_OPTIONS` to pass them directly to the JDK.
