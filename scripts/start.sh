#!/usr/bin/env bash

PROJECT_ROOT="/home/ubuntu/app"
JAR_FILE="$PROJECT_ROOT/spring-webapp.jar"

APP_LOG="$PROJECT_ROOT/application.log"
ERROR_LOG="$PROJECT_ROOT/error.log"
DEPLOY_LOG="$PROJECT_ROOT/deploy.log"

TIME_NOW=$(date +%c)

# 1. Java 설치 확인 및 자동 설치 로직 추가
if ! command -v java &> /dev/null
then
    echo "$TIME_NOW > Java가 설치되어 있지 않습니다. OpenJDK 17 설치를 시작합니다." >> $DEPLOY_LOG
    sudo apt-get update -y
    sudo apt-get install openjdk-17-jdk -y >> $DEPLOY_LOG 2>&1

    # 설치 후 재확인
    if ! command -v java &> /dev/null
    then
        echo "$TIME_NOW > Java 설치에 실패했습니다. 배포를 중단합니다." >> $DEPLOY_LOG
        exit 1
    fi
    echo "$TIME_NOW > Java 설치가 완료되었습니다." >> $DEPLOY_LOG
else
    echo "$TIME_NOW > Java 설치 확인됨: $(java -version 2>&1 | head -n 1)" >> $DEPLOY_LOG
fi

# 2. build 파일 복사
echo "$TIME_NOW > $JAR_FILE 파일 복사" >> $DEPLOY_LOG
cp $PROJECT_ROOT/build/libs/*.jar $JAR_FILE

# 3. jar 파일 실행
echo "$TIME_NOW > $JAR_FILE 파일 실행" >> $DEPLOY_LOG
# 에러 분석을 위해 2> $ERROR_LOG 설정을 유지합니다.
nohup java -jar $JAR_FILE > $APP_LOG 2> $ERROR_LOG &

# 4. 실행 확인
sleep 2 # 프로세스가 뜰 시간을 잠시 줍니다.
CURRENT_PID=$(pgrep -f $JAR_FILE)

if [ -z "$CURRENT_PID" ]; then
  echo "$TIME_NOW > 프로세스 실행 실패! $ERROR_LOG 를 확인하세요." >> $DEPLOY_LOG
else
  echo "$TIME_NOW > 실행된 프로세스 아이디 $CURRENT_PID 입니다." >> $DEPLOY_LOG
fi