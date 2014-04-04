#!/bin/sh

# Set paths
FL_HOME=`dirname $0`
FL_JAR="${FL_HOME}/target/floodlight.jar"
FL_LOGBACK="${FL_HOME}/mylogback.xml"

# Set JVM options
JVM_OPTS=""
JVM_OPTS="$JVM_OPTS -server -d64"
JVM_OPTS="$JVM_OPTS -Xmx2g -Xms2g -Xmn800m"
JVM_OPTS="$JVM_OPTS -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods"
JVM_OPTS="$JVM_OPTS -XX:MaxInlineSize=8192 -XX:FreqInlineSize=8192"
JVM_OPTS="$JVM_OPTS -XX:CompileThreshold=1500 -XX:PreBlockSpin=8"
JVM_OPTS="$JVM_OPTS -Dpython.security.respectJavaAccessibility=false"

echo "Starting floodlight server ..."
java ${JVM_OPTS} -Dlogback.configurationFile=${FL_LOGBACK} -jar ${FL_JAR}
