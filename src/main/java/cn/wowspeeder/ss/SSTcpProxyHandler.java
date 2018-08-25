package cn.wowspeeder.ss;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

public class SSTcpProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static InternalLogger logger =  InternalLoggerFactory.getInstance(SSTcpProxyHandler.class);

    private Channel clientChannel;
    private Channel remoteChannel;
    private Bootstrap proxyClient;
    private List<ByteBuf> clientBuffs = new ArrayList<>();

    public SSTcpProxyHandler() {
    }


    @Override
    protected void channelRead0(ChannelHandlerContext clientCtx, ByteBuf msg) throws Exception {
        logger.debug("channel id {},readableBytes:{}", clientCtx.channel().id().toString(), msg.readableBytes());
        if (this.clientChannel == null) {
            this.clientChannel = clientCtx.channel();
        }
//        if (msg.readableBytes() == 0) return;
        proxy(clientCtx, msg);
    }

    private void proxy(ChannelHandlerContext clientCtx, ByteBuf msg) {
        logger.debug("channel id {},pc is null {},{}", clientCtx.channel().id().toString(), (remoteChannel == null), msg.readableBytes());
        if (remoteChannel == null && proxyClient == null) {
            proxyClient = new Bootstrap();//

            InetSocketAddress clientRecipient = clientCtx.channel().attr(SSCommon.REMOTE_DES).get();

            proxyClient.group(clientCtx.channel().eventLoop()).channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60 * 1000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_RCVBUF, 32 * 1024)// 读缓冲区为32k
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(
                            new ChannelInitializer<Channel>() {
                                @Override
                                protected void initChannel(Channel ch) throws Exception {
                                    ch.pipeline()
                                            .addLast("timeout", new IdleStateHandler(0, 0, 15, TimeUnit.MINUTES) {
                                                @Override
                                                protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
                                                    logger.debug("{} state:{}", clientRecipient.toString(), state.toString());
                                                    proxyChannelClose();
                                                    return super.newIdleStateEvent(state, first);
                                                }
                                            })
                                            .addLast("tcpProxy", new SimpleChannelInboundHandler<ByteBuf>() {
                                                @Override
                                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                                    clientCtx.channel().writeAndFlush(msg.retain());
                                                }

                                                @Override
                                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
//                                                    logger.debug("channelActive {}",msg.readableBytes());
                                                }

                                                @Override
                                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                                    super.channelInactive(ctx);
                                                    proxyChannelClose();
                                                }

                                                @Override
                                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//                                                    super.exceptionCaught(ctx, cause);
                                                    proxyChannelClose();
                                                }
                                            });
                                }
                            }
                    );
            try {
                proxyClient
                        .connect(clientRecipient)
                        .addListener((ChannelFutureListener) future -> {
                            try {
                                if (future.isSuccess()) {
                                    logger.debug("channel id {}, {}<->{}<->{} connect  {}", clientCtx.channel().id().toString(), clientCtx.channel().remoteAddress().toString(), future.channel().localAddress().toString(), clientRecipient.toString(), future.isSuccess());
                                    remoteChannel = future.channel();
                                    //write and flush cacheList
                                    writeAndFlushByteBufList();
                                } else {
                                    logger.error("channel id {}, {}<->{} connect {},cause {}", clientCtx.channel().id().toString(), clientCtx.channel().remoteAddress().toString(), clientRecipient.toString(), future.isSuccess(), future.cause());
                                    proxyChannelClose();
                                }
                            } catch (Exception e) {
                                proxyChannelClose();
                            }
                        });
            } catch (Exception e) {
                logger.error("connect internet error", e);
                proxyChannelClose();
                return;
            }
        }

        //put list and refresh cache
        clientBuffs.add(msg.retain());
        writeAndFlushByteBufList();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        proxyChannelClose();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx,cause);
        proxyChannelClose();
    }

    private void proxyChannelClose() {
//        logger.info("proxyChannelClose");
        try {
            if (clientBuffs != null) {
                clientBuffs.forEach(ReferenceCountUtil::release);
                clientBuffs = null;
            }
            if (remoteChannel != null) {
                remoteChannel.close();
                remoteChannel = null;
            }
            if (clientChannel != null) {
                clientChannel.close();
                clientChannel = null;
            }
        } catch (Exception e) {
//            logger.error("close channel error", e);
        }
    }
    /**
     * print ByteBufList to remote channel
     */
    private void writeAndFlushByteBufList() {
        if (remoteChannel != null && !clientBuffs.isEmpty()) {
            for (ByteBuf messageBuf : clientBuffs) {
                remoteChannel.write(messageBuf);
            }
            clientBuffs.clear();
            remoteChannel.flush();

            if(logger.isDebugEnabled()){
                logger.debug("channel id {},remote channel write", remoteChannel.id().toString());
            }
        }
    }
}
