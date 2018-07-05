@echo off
java -Dio.netty.maxDirectMemory=0 -Dio.netty.leakDetectionLevel=advanced -jar shadowsocks-netty-server-1.0.0.jar
pause