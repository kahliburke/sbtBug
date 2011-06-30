LAUNCHER=sbt-launch-0.10.1-20110629-052055.jar
JAVA_OPTS="-Xmx512M -XX:MaxPermSize=256M -Djava.awt.headless=true $SBT_OPTS"

if [[ $SBT_DEBUG = 'true' ]]
then
  JAVA_OPTS="$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8765"
fi

if [[ $USE_JREBEL = 'true' ]]
then
  JAVA_OPTS="$JAVA_OPTS -noverify -javaagent:$REBEL_HOME/jrebel.jar -Drebel.log=true"
fi

java $JAVA_OPTS -jar `dirname $0`/$LAUNCHER "$@"
