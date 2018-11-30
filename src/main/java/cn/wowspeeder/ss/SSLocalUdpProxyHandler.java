package cn.wowspeeder.ss;

import cn.wowspeeder.encryption.CryptFactory;
import cn.wowspeeder.encryption.ICrypt;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

public class SSLocalUdpProxyHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(SSLocalUdpProxyHandler.class);

    private static byte[] SOCKS5_ADDRESS_PREFIX = new byte[]{0, 0, 0};

    private static EventLoopGroup proxyBossGroup = new NioEventLoopGroup();
    private final ICrypt crypt;
    private final InetSocketAddress ssServer;

    public SSLocalUdpProxyHandler(String server, Integer port, String method, String password, String obfs, String obfsparam) {
        crypt = CryptFactory.get(method, password, true);
        ssServer = new InetSocketAddress(server, port);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext clientCtx, DatagramPacket msg) throws Exception {
        logger.debug("readableBytes:" + msg.content().readableBytes());
        InetSocketAddress clientSender = msg.sender();

        msg.content().skipBytes(3);//skip [5, 0, 0]
        SSAddrRequest addrRequest = SSAddrRequest.getAddrRequest(msg.content());
        InetSocketAddress clientRecipient = new InetSocketAddress(addrRequest.host(), addrRequest.port());
        proxy(clientSender, msg.content(),clientRecipient);
    }

    private void proxy(InetSocketAddress clientSender, ByteBuf msg,InetSocketAddress clientRecipient) throws InterruptedException {
        Channel pc = NatMapper.getUdpChannel(clientSender);
        if (pc == null) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(proxyBossGroup).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_RCVBUF, 64 * 1024)// 设置UDP读缓冲区为64k
                    .option(ChannelOption.SO_SNDBUF, 64 * 1024)// 设置UDP写缓冲区为64k
                    .handler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.attr(SSCommon.IS_UDP).set(true);
                            ch.attr(SSCommon.CIPHER).set(crypt);
                            ch.pipeline()
                                    .addLast(new LoggingHandler(LogLevel.INFO))
                                    .addLast("ssCipherCodec", new SSCipherCodec())
                                    .addLast("ssProtocolCodec", new SSProtocolCodec(true))

                                    .addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
//                                          logger.debug("rc received message ");
                                            ByteBuf content = Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(SOCKS5_ADDRESS_PREFIX), msg.content().retain());
                                            ctx.channel().writeAndFlush(new DatagramPacket(content, clientSender)).addListener(new GenericFutureListener<Future<? super Void>>() {
                                                @Override
                                                public void operationComplete(Future<? super Void> future) throws Exception {
                                                    logger.debug("operationComplete {} {}", future.isSuccess(), future.cause());
                                                }
                                            });
                                        }
                                    })
                            ;
                        }
                    })
            ;
            try {
                pc = bootstrap
                        .bind("0.0.0.0", 0)
                        .addListener((ChannelFutureListener) future -> {
                            if (future.isSuccess()) {
                                NatMapper.putUdpChannel(clientSender, future.channel());
                            }
                        })
                        .sync()
                        .channel();
            } catch (Exception e) {
                logger.error("connect intenet error", e);
                return;
            }
        } else {
//            logger.debug("pc {} already exist", clientSender.toString());
        }

        if (pc != null) {
            pc.attr(SSCommon.REMOTE_DES).set(clientRecipient);
            pc.writeAndFlush(new DatagramPacket(msg.retain(), ssServer)).addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    logger.debug("operationComplete {} {}",future.isSuccess(),future.cause());
                }
            });
        }
    }


}
