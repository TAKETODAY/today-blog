#!/bin/bash

#
# Copyright 2017 - 2024 the original author or authors.
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
# along with this program. If not, see [https://www.gnu.org/licenses/]
#

cygwin=false
linux=false
case "$(uname)" in
CYGWIN*)
  cygwin=true
  ;;
Linux*)
  linux=true
  ;;
esac

get_pid() {
  STR=$1
  PID=$2
  if $cygwin; then
    JAVA_CMD="$JAVA_HOME/bin/java"
    JAVA_CMD=$(cygpath --path --unix $JAVA_CMD)
    JAVA_PID=$(ps | grep $JAVA_CMD | awk '{print $1}')
  else
    if $linux; then
      if [ ! -z "$PID" ]; then
        JAVA_PID=$(ps -C java -f --width 1000 | grep "$STR" | grep "$PID" | grep -v grep | awk '{print $2}')
      else
        JAVA_PID=$(ps -C java -f --width 1000 | grep "$STR" | grep -v grep | awk '{print $2}')
      fi
    else
      if [ ! -z "$PID" ]; then
        JAVA_PID=$(ps aux | grep "$STR" | grep "$PID" | grep -v grep | awk '{print $2}')
      else
        JAVA_PID=$(ps aux | grep "$STR" | grep -v grep | awk '{print $2}')
      fi
    fi
  fi
  echo $JAVA_PID
}

base=$(dirname $0)/..
pidfile=$base/bin/app.pid
if [ ! -f "$pidfile" ]; then
  echo "today-blog is not running. exists"
  exit
fi

pid=$(cat $pidfile)
if [ "$pid" == "" ]; then
  pid=$(get_pid "appName=today-blog")
fi

echo -e "$(hostname): stopping TODAY BLOG $pid ... "
kill $pid

LOOPS=0
while (true); do
  gpid=$(get_pid "appName=today-blog" "$pid")
  if [ "$gpid" == "" ]; then
    echo "Oook! cost:$LOOPS"
    $(rm $pidfile)
    break
  fi
  let LOOPS=LOOPS+1
  sleep 1
done
