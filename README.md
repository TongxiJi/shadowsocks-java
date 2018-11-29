# shadowsocks-java
A  implementation of Shadowsocks in Java base on netty4 framework.

# Features
- [x] AEAD Ciphers support
- [x] TCP & UDP full support
- [x] DNS proxy optimization

# Environment
* JRE8

# Install
1. download shadowsocks-netty-x.x.x-bin.zip
2. unzip shadowsocks-netty-x.x.x-bin.zip
3. run
#### as ssserver
```
java -jar shadowsocks-netty-x.x.x.jar -s
```
#### as ssclient
```
java -jar shadowsocks-netty-x.x.x.jar -c
```

## Config file as python port
[Create configuration file and run](https://github.com/shadowsocks/shadowsocks/wiki/Configuration-via-Config-File)

# Build
1. import as maven project
2. maven package

## TODO
* [ ] ssr obfs features implementation(maybe no use,but for fun)
* [ ] performance optimization
* [ ] rate limit