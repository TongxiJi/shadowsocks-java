package cn.wowspeeder;

import cn.wowspeeder.config.Config;
import cn.wowspeeder.config.ConfigXmlLoader;
import cn.wowspeeder.ss.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SocksServer {

    private static Logger logger = LoggerFactory.getLogger(SocksServer.class);

    private static final String CONFIG = "conf/config.xml";

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;
    private ServerBootstrap tcpBootstrap = null;
    private Bootstrap udpBootstrap = null;
    private static SocksServer socksServer = new SocksServer();

    public static SocksServer getInstance() {
        return socksServer;
    }

    private SocksServer() {

    }

    public void start() {
        try {
            final Config config = ConfigXmlLoader.load(CONFIG);
            List<Channel> channels = new ArrayList<>();

            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            //tcp server
            tcpBootstrap = new ServerBootstrap();
            tcpBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_RCVBUF, 32 * 1024)// 读缓冲区为32k
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_LINGER, 1) //关闭时等待1s发送关闭
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ctx) throws Exception {
//                            ctx.pipeline().addLast(new SSTcpHandler(config));
                            ctx.pipeline()
                                    //timeout
                                    .addLast("timeout", new IdleStateHandler(0, 0, 2, TimeUnit.MINUTES) {
                                        @Override
                                        protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
                                            ctx.close();
                                            return super.newIdleStateEvent(state, first);
                                        }
                                    })
                                    // in
                                    .addLast("ssCheckerReceive", new SSCheckerReceive(config.get_method(), config.get_password()))
                                    .addLast("ssCipherDecoder", new SSCipherDecoder())
                                    .addLast("ssProtocolDecoder", new SSProtocolDecoder())
                                    //proxy
                                    .addLast("ssTcpProxy", new SSTcpProxyHandler())
                                    // out
                                    .addLast("ssCheckerSend", new SSCheckerSend())
                                    .addLast("ssCipherEncoder", new SSCipherEncoder())
                                    .addLast("ssProtocolEncoder", new SSProtocolEncoder())
                            ;
                        }
                    });

//            logger.info("TCP Start At Port " + config.get_localPort());
            channels.add(tcpBootstrap.bind(config.get_localPort()).sync().channel());

            //udp server
            udpBootstrap = new Bootstrap();
            udpBootstrap.group(bossGroup).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)// 支持广播
                    .option(ChannelOption.SO_RCVBUF, 64 * 1024)// 设置UDP读缓冲区为64k
                    .option(ChannelOption.SO_SNDBUF, 64 * 1024)// 设置UDP写缓冲区为64k
                    .handler(new ChannelInitializer<NioDatagramChannel>() {

                        @Override
                        protected void initChannel(NioDatagramChannel ctx) throws Exception {
                            ctx.pipeline()
                                    // in
                                    .addLast("ssCheckerReceive", new SSCheckerReceive(config.get_method(), config.get_password()))
                                    .addLast("ssCipherDecoder", new SSCipherDecoder())
                                    .addLast("ssProtocolDecoder", new SSProtocolDecoder())
                                    //proxy
                                    .addLast("ssUdpProxy", new SSUdpProxyHandler())
                                    // out
                                    .addLast("ssCheckerSend", new SSCheckerSend())
                                    .addLast("ssCipherEncoder", new SSCipherEncoder())
                                    .addLast("ssProtocolEncoder", new SSProtocolEncoder())
                            ;
                        }
                    })
            ;
//            logger.info("UDP Start At Port " + config.get_localPort());
            channels.add(udpBootstrap.bind(config.get_localPort()).sync().channel());

            for (int i = 0; i < channels.size(); i++) {
                Channel channel = channels.get(i);
                ChannelFuture channelFuture = channel.closeFuture();
                logger.info("start channel:" + channel.toString());
                if (i == channels.size() - 1) {
                    channelFuture.sync();
                }
            }
        } catch (Exception e) {
            logger.error("start error", e);
        } finally {
            stop();
        }
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

}
