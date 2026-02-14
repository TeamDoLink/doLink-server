#!/usr/bin/env bash

PROJECT_ROOT="/home/ubuntu/app"
JAR_FILE="$PROJECT_ROOT/spring-webapp.jar"

APP_LOG="$PROJECT_ROOT/application.log"
ERROR_LOG="$PROJECT_ROOT/error.log"
DEPLOY_LOG="$PROJECT_ROOT/deploy.log"

TIME_NOW=$(date +%c)

echo "--- 배포 시작: $TIME_NOW ---" >> $DEPLOY_LOG

# 1. Java 설치 확인 및 자동 설치
if ! command -v java &> /dev/null
then
    echo "$TIME_NOW > Java가 설치되어 있지 않습니다. OpenJDK 17 설치를 시작합니다." >> $DEPLOY_LOG
    sudo apt-get update -y
    sudo apt-get install openjdk-17-jdk -y >> $DEPLOY_LOG 2>&1

    if ! command -v java &> /dev/null
    then
        echo "$TIME_NOW > Java 설치에 실패했습니다. 배포를 중단합니다." >> $DEPLOY_LOG
        exit 1
    fi
    echo "$TIME_NOW > Java 설치 완료." >> $DEPLOY_LOG
else
    echo "$TIME_NOW > Java 설치 확인됨: $(java -version 2>&1 | head -n 1)" >> $DEPLOY_LOG
fi

# 2. 기존 프로세스 종료 (stop.sh 활용)
# CodeRabbitAI가 지적한 포트 충돌 문제를 이미 작성하신 stop.sh로 해결합니다.
echo "$TIME_NOW > 기존 프로세스 종료를 위해 stop.sh를 실행합니다." >> $DEPLOY_LOG
if [ -f "$PROJECT_ROOT/scripts/stop.sh" ]; then
    bash "$PROJECT_ROOT/scripts/stop.sh"
else
    echo "$TIME_NOW > stop.sh 파일이 없어 종료 로직을 건너뜁니다." >> $DEPLOY_LOG
fi

# 3. build 파일 복사
echo "$TIME_NOW > $JAR_FILE 파일 복사" >> $DEPLOY_LOG
cp $PROJECT_ROOT/build/libs/*.jar $JAR_FILE

# 4. jar 파일 실행
echo "$TIME_NOW > $JAR_FILE 신규 파일 실행" >> $DEPLOY_LOG
# nohup 실행 시 에러는 error.log에 기록됩니다.
nohup java -jar $JAR_FILE > $APP_LOG 2> $ERROR_LOG &

# 5. 실행 확인 (약간의 대기 후 PID 체크)
sleep 5
CURRENT_PID=$(pgrep -f $JAR_FILE)

if [ -z "$CURRENT_PID" ]; then
  echo "$TIME_NOW > [실패] 프로세스 실행 실패! $ERROR_LOG를 확인하세요." >> $DEPLOY_LOG
  exit 1
else
  echo "$TIME_NOW > [성공] 실행된 프로세스 아이디: $CURRENT_PID" >> $DEPLOY_LOG
fi