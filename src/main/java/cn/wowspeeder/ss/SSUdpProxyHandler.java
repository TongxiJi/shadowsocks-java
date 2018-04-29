package cn.wowspeeder.ss;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class SSUdpProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static Logger logger = LoggerFactory.getLogger(SSUdpProxyHandler.class);


    public SSUdpProxyHandler() {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext clientCtx, ByteBuf msg) throws Exception {
//        logger.debug("readableBytes:" + msg.readableBytes());
        InetSocketAddress clientSender = clientCtx.channel().attr(SSCommon.CLIENT).get();
        InetSocketAddress clientRecipient = clientCtx.channel().attr(SSCommon.REMOTE_DES).get();
        proxy(clientSender, clientRecipient, clientCtx, msg.retain());
    }

    private void proxy(InetSocketAddress clientSender, InetSocketAddress clientRecipient, ChannelHandlerContext clientCtx, ByteBuf msg) {
        Channel pc = NatMapper.getUdpChannel(clientSender);
        if (pc == null) {
            Bootstrap bootstrap = new Bootstrap();
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            bootstrap.group(bossGroup).channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline()
                                    .addLast("timeout", new IdleStateHandler(0, 0, 1, TimeUnit.MINUTES) {
                                        @Override
                                        protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
//                                            logger.debug("{} state:{}", clientSender.toString(), state.toString());
                                            NatMapper.closeChannelGracefully(clientSender);
                                            return super.newIdleStateEvent(state, first);
                                        }
                                    })
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
                                    logger.debug("channel id {}, {}<->{}<->{} connect  {}",clientCtx.channel().id().toString(), clientSender.toString(), future.channel().localAddress().toString(), clientRecipient.toString(), future.isSuccess());
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
