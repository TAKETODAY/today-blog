#!/bin/bash

#
# Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
# Copyright © TODAY & 2017 - 2023 All Rights Reserved.
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see [http://www.gnu.org/licenses/]
#

current_path=$(pwd)
case "$(uname)" in
Linux)
  bin_abs_path=$(readlink -f $(dirname $0))
  ;;
*)
  bin_abs_path=$(
    cd $(dirname $0)
    pwd
  )
  ;;
esac
base=${bin_abs_path}/..
export LANG=en_US.UTF-8
export BASE=$base

if [ -f $base/bin/adapter.pid ]; then
  echo "found adapter.pid , Please run stop.sh first ,then startup.sh" 2>&2
  exit 1
fi

if [ ! -d $base/logs ]; then
  mkdir -p $base/logs
fi

## set java path
if [ -z "$JAVA" ]; then
  JAVA=$(which java)
fi

ALIBABA_JAVA="/usr/alibaba/java/bin/java"
TAOBAO_JAVA="/opt/taobao/java/bin/java"
if [ -z "$JAVA" ]; then
  if [ -f $ALIBABA_JAVA ]; then
    JAVA=$ALIBABA_JAVA
  elif [ -f $TAOBAO_JAVA ]; then
    JAVA=$TAOBAO_JAVA
  else
    echo "Cannot find a Java JDK. Please set either set JAVA or put java (>=1.5) in your PATH." 2>&2
    exit 1
  fi
fi

case "$#" in

0) ;;

2)
  if [ "$1" = "debug" ]; then
    DEBUG_PORT=$2
    DEBUG_SUSPEND="n"
    JAVA_DEBUG_OPT="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$DEBUG_SUSPEND"
  fi
  ;;
*)
  echo "THE PARAMETERS MUST BE TWO OR LESS.PLEASE CHECK AGAIN."
  exit
  ;;
esac

str=$(file -L $JAVA | grep 64-bit)
if [ -n "$str" ]; then
  JAVA_OPTS="-server -Xms2048m -Xmx3072m"
else
  JAVA_OPTS="-server -Xms1024m -Xmx1024m"
fi

JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=250 -XX:+UseGCOverheadLimit -XX:+ExplicitGCInvokesConcurrent"
JAVA_OPTS=" $JAVA_OPTS -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"

for i in $base/lib/*; do
  CLASSPATH=$i:"$CLASSPATH"
done

CLASSPATH="$base/conf:$CLASSPATH"

echo "cd to $bin_abs_path for workaround relative path"
cd $bin_abs_path

#echo CLASSPATH :$CLASSPATH
$JAVA $JAVA_OPTS $JAVA_DEBUG_OPT \
-classpath .:$CLASSPATH cn.taketoday.blog.BlogApplication 1>>/dev/null 2>&1 &

echo $! >$base/bin/app.pid

echo "cd to $current_path for continue"
cd $current_path
