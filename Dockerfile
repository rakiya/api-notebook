FROM openjdk:8-jre-alpine

RUN apk update --no-cache

## ディレクトリ作成
RUN mkdir /habanero
RUN mkdir /habanero/logs
RUN mkdir /habanero/conf

## リソースをイメージにコピー
WORKDIR /habanero
COPY ./build/libs/api-notebook.jar .
COPY ./conf/application.conf ./conf
COPY ./conf/logback.xml ./conf

## ボリュームを作成
VOLUME /habanero/logs

## ポートを開放
EXPOSE 80

## アプリケーションを実行
CMD java -server \
        -XX:+UnlockExperimentalVMOptions \
        -XX:+UseCGroupMemoryLimitForHeap \
        -XX:InitialRAMFraction=2 \
        -XX:MinRAMFraction=2 \
        -XX:MaxRAMFraction=2 \
        -XX:+UseG1GC \
        -XX:MaxGCPauseMillis=100 \
        -XX:+UseStringDeduplication \
        -jar api-notebook.jar