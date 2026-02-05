#!/usr/bin/env bash

# set -e: 오류 발생 시 즉시 중단
# set -u: 미정의 변수 사용 시 오류
PROJECT_ROOT="/home/ubuntu/app"
JAR_FILE="$PROJECT_ROOT/spring-webapp.jar"
DEPLOY_LOG="$PROJECT_ROOT/deploy.log"

TIME_NOW=$(date +%c)

# 1. 현재 구동 중인 모든 PID 확인 (따옴표 사용 및 에러 핸들링)
CURRENT_PIDS=$(pgrep -f "$JAR_FILE" || true)

if [ -z "$CURRENT_PIDS" ]; then
  echo "$TIME_NOW > 현재 실행 중인 애플리케이션이 없습니다." >> "$DEPLOY_LOG"
else
  echo "$TIME_NOW > 실행 중인 프로세스($CURRENT_PIDS) 종료를 시도합니다." >> "$DEPLOY_LOG"

  # 2. 모든 PID에 SIGTERM(-15) 송신
  kill -15 $CURRENT_PIDS

  # 3. 프로세스가 완전히 종료될 때까지 대기 (최대 30초)
  MAX_RETRIES=30
  COUNT=0

  while [ $COUNT -lt $MAX_RETRIES ]; do
    # 아직 살아있는 프로세스가 있는지 확인
    STILL_ALIVE=$(pgrep -f "$JAR_FILE" || true)

    if [ -z "$STILL_ALIVE" ]; then
      echo "$TIME_NOW > 애플리케이션이 성공적으로 종료되었습니다." >> "$DEPLOY_LOG"
      break
    fi

    echo "$TIME_NOW > 종료 대기 중... (${COUNT}/${MAX_RETRIES})" >> "$DEPLOY_LOG"
    sleep 1
    COUNT=$((COUNT + 1))
  done

  # 4. 30초 후에도 살아있다면 강제 종료(SIGKILL)
  if [ -n "$(pgrep -f "$JAR_FILE" || true)" ]; then
    echo "$TIME_NOW > 애플리케이션이 정상적으로 종료되지 않아 강제 종료(SIGKILL)를 수행합니다." >> "$DEPLOY_LOG"
    kill -9 $(pgrep -f "$JAR_FILE")
    sleep 2
  fi
fi