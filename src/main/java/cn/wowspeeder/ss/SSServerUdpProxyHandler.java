package cn.wowspeeder.ss;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class SSServerUdpProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(SSServerUdpProxyHandler.class);

    private static EventLoopGroup proxyBossGroup = new NioEventLoopGroup();

    public SSServerUdpProxyHandler() {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext clientCtx, ByteBuf msg) throws Exception {
//        logger.debug("readableBytes:" + msg.readableBytes());
        InetSocketAddress clientSender = clientCtx.channel().attr(SSCommon.RemoteAddr).get();
        InetSocketAddress clientRecipient = clientCtx.channel().attr(SSCommon.REMOTE_DES).get();
        proxy(clientSender, clientRecipient, clientCtx, msg.retain());
    }

    private void proxy(InetSocketAddress clientSender, InetSocketAddress clientRecipient, ChannelHandlerContext clientCtx, ByteBuf msg) {
        Channel pc = NatMapper.getUdpChannel(clientSender);
        if (pc == null) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(proxyBossGroup).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_RCVBUF, 64 * 1024)// 设置UDP读缓冲区为64k
                    .option(ChannelOption.SO_SNDBUF, 64 * 1024)// 设置UDP写缓冲区为64k
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            int proxyIdleTimeout
                                    = clientRecipient.getPort() != 53
                                    ? SSCommon.UDP_PROXY_IDEL_TIME
                                    : SSCommon.UDP_DNS_PROXY_IDEL_TIME;
                            ch.pipeline()
                                    .addLast("timeout", new IdleStateHandler(0, 0, proxyIdleTimeout, TimeUnit.SECONDS) {
                                        @Override
                                        protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
//                                            logger.debug("{} state:{}", clientSender.toString(), state.toString());
                                            NatMapper.closeChannelGracefully(clientSender);
                                            return super.newIdleStateEvent(state, first);
                                        }
                                    })
//                                    .addLast(new LoggingHandler(LogLevel.INFO))
                                    .addLast("udpProxy", new SimpleChannelInboundHandler<DatagramPacket>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
//                                            logger.debug("rc received message ");
                                            clientCtx.channel().attr(SSCommon.REMOTE_SRC).set(msg.sender());
                                            clientCtx.channel().writeAndFlush(msg.retain().content());
                                        }
                                    });
                        }
                    });
            try {
                pc = bootstrap
                        .bind(0)
                        .addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (future.isSuccess()) {
                                    logger.debug("channel id {}, {}<->{}<->{} connect  {}", clientCtx.channel().id().toString(), clientSender.toString(), future.channel().localAddress().toString(), clientRecipient.toString(), future.isSuccess());
                                    NatMapper.putUdpChannel(clientSender, future.channel());
                                }
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
            pc.writeAndFlush(new DatagramPacket(msg, clientRecipient));
        }
    }
}
