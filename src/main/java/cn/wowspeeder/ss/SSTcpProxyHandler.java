package cn.wowspeeder.ss;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class SSTcpProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static Logger logger = LoggerFactory.getLogger(SSTcpProxyHandler.class);
    private Channel clientChannel;
    private Channel remoteChannel;
    private boolean httpClientCreated;
    private ByteBuf  clientBuff;

    public SSTcpProxyHandler() {
    }


    @Override
    protected void channelRead0(ChannelHandlerContext clientCtx, ByteBuf msg) throws Exception {
        logger.debug("readableBytes:" + msg.readableBytes());
        if (this.clientChannel == null) {
            this.clientChannel = clientCtx.channel();
        }
//        if (msg.readableBytes() == 0) return;
        InetSocketAddress clientRecipient = clientCtx.channel().attr(SSCommon.REMOTE_DES).get();
        proxy(clientRecipient, clientCtx, msg);
    }

    private void proxy(InetSocketAddress clientRecipient, ChannelHandlerContext clientCtx, ByteBuf msg) {
        logger.debug("pc is null {},{}", (remoteChannel == null) , msg.readableBytes());
        msg.retain();

        if (remoteChannel == null) {
            if (clientBuff  == null) {
                clientBuff = msg;
            } else {
                clientBuff.writeBytes(msg);
                msg.release();
            }

            if (httpClientCreated) {
                return;
            }
            httpClientCreated = true;
            Bootstrap bootstrap = new Bootstrap();//
            bootstrap.group(clientCtx.channel().eventLoop()).channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000).option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(
                            new ChannelInitializer<Channel>() {
                                @Override
                                protected void initChannel(Channel ch) throws Exception {
                                    ch.pipeline()
                                            .addLast("timeout", new IdleStateHandler(0, 0, 1, TimeUnit.MINUTES) {
                                                @Override
                                                protected IdleStateEvent newIdleStateEvent(IdleState state, boolean first) {
                                                    logger.info("{} state:{}", clientRecipient.toString(), state.toString());
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
                                                    ctx.writeAndFlush(clientBuff);
                                                }

                                                @Override
                                                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                                    super.channelInactive(ctx);
                                                    proxyChannelClose();
                                                }

                                                @Override
                                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                                    super.exceptionCaught(ctx, cause);
                                                    proxyChannelClose();
                                                    if (msg.refCnt() != 0) {
                                                        ReferenceCountUtil.release(msg);
                                                    }
                                                }
                                            });
                                }
                            }
                    );
            try {
                 bootstrap
                        .connect(clientRecipient)
                        .addListener((ChannelFutureListener) future -> {
                            if (future.isSuccess()) {
                                remoteChannel = future.channel();
                                logger.debug("channel id {}, {}<->{}<->{} connect  {}", clientCtx.channel().id().toString(), "client", future.channel().localAddress().toString(), clientRecipient.toString(), future.isSuccess());
                            } else {
                                proxyChannelClose();
                                logger.error("channel id {}, ->{} connect  {}", clientCtx.channel().id().toString(), "client", clientRecipient.toString(), future.isSuccess());
                            }
                        });
            } catch (Exception e) {
                logger.error("connect internet error", e);
                proxyChannelClose();
            }
        } else {
            logger.debug("write direct {}", msg.readableBytes());
            remoteChannel.writeAndFlush(msg);
        }
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
        try {
            if (remoteChannel != null) {
                remoteChannel.close();
                remoteChannel = null;
            }
            if (clientChannel != null) {
                clientChannel.close();
                clientChannel = null;
            }
        } catch (Exception e) {
            logger.error("close channel error", e);
        }
    }
}
