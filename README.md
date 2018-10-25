# shadowsocks-java
A  implementation of Shadowsocks in Java base on netty4 framework.

# Features
- [x] AEAD Ciphers support
- [x] TCP & UDP full support
- [x] DNS proxy optimization

# environment
* JRE8

# install
1. download shadowsocks-netty-server-x.x.x-bin.zip
2. unzip shadowsocks-netty-server-x.x.x-bin.zip
3. run
```
#unix like:
cd shadowsocks-netty-server-x.x.x
bash shadowsocks-netty-server.sh
```

```
#windows:
shadowsocks-netty-server.bat
```

# build
1. import as maven project
2. maven package

## TODO
* [ ] ss-local implementation
* [ ] ssr obfs features implementation(maybe no use,but for fun)
* [ ] performance optimization
* [ ] rate limit