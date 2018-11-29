package cn.wowspeeder;

import cn.wowspeeder.config.Config;
import cn.wowspeeder.config.ConfigLoader;
import cn.wowspeeder.encryption.CryptAeadBase;
import cn.wowspeeder.encryption.CryptFactory;
import cn.wowspeeder.encryption.ICrypt;
import cn.wowspeeder.socks5.SocksServerHandler;
import cn.wowspeeder.socks5.SocksServerInitializer;
import cn.wowspeeder.ss.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.ServerDomainSocketChannel;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SSLocal {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(SSServer.class);

    private static final String CONFIG = "conf/config.json";

    private static EventLoopGroup bossGroup = new NioEventLoopGroup();
    private static EventLoopGroup workerGroup = new NioEventLoopGroup();

    private static SSLocal SSLocal = new SSLocal();

    public static SSLocal getInstance() {
        return SSLocal;
    }

    private SSLocal() {

    }

    public void start() throws Exception {
        final Config config = ConfigLoader.load(CONFIG);
        logger.info("load config !");

        for (Map.Entry<Integer, String> portPassword : config.getPortPassword().entrySet()) {
            startSingle(config.getLocalAddress(), config.getLocalPort(),
                    config.getServer(),
                    portPassword.getKey(),
                    portPassword.getValue(),
                    config.getMethod(),
                    config.getObfs(),
                    config.getObfsParam());
        }
    }

    private void startSingle(String socks5Server, Integer socks5Port, String server, Integer port, String password, String method, String obfs, String obfsparam) throws Exception {
        ServerBootstrap tcpBootstrap = new ServerBootstrap();
        tcpBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_RCVBUF, 32 * 1024)// 读缓冲区为32k
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ctx) throws Exception {
                        logger.debug("channel initializer");
                        ctx.pipeline()
                                //timeout
                                .addLast("timeout", new IdleStateHandler(0, 0, SSCommon.TCP_PROXY_IDEL_TIME, TimeUnit.SECONDS) {
                                    @Override
                                    protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
                                        ctx.close();
                                        return super.newIdleStateEvent(state, first);
                                    }
                                });

                        //socks5
                        ctx.pipeline()
//                                .addLast(new LoggingHandler(LogLevel.INFO))
                                .addLast(new SocksPortUnificationServerHandler())
                                .addLast(SocksServerHandler.INSTANCE)
                                .addLast(new SSLocalTcpProxyHandler(server, port, method, password, obfs, obfsparam));
                    }
                });

//            logger.info("TCP Start At Port " + config.get_localPort());
        tcpBootstrap.bind(socks5Server, socks5Port).sync();
        logger.info("listen at {}:{}", socks5Server, socks5Port);
    }

    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        logger.info("Stop Server!");
    }

    public static void main(String[] args) throws Exception {
        try {
            getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
            getInstance().stop();
            System.exit(-1);
        }
    }

}
