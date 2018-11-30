package cn.wowspeeder.socks5;


import cn.wowspeeder.ss.SSCommon;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.*;

@ChannelHandler.Sharable
public final class SocksServerHandler extends SimpleChannelInboundHandler<SocksMessage> {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(SocksServerHandler.class);

    public static final SocksServerHandler INSTANCE = new SocksServerHandler();

    private SocksServerHandler() {
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage socksRequest) throws Exception {
        switch (socksRequest.version()) {
            case SOCKS5:
                if (socksRequest instanceof Socks5InitialRequest) {
                    // auth support example
                    //ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
                    //ctx.write(new DefaultSocks5AuthMethodResponse(Socks5AuthMethod.PASSWORD));
                    ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                    ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
                } else if (socksRequest instanceof Socks5CommandRequest) {
                    Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
                    if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
//                        ctx.pipeline().addLast(new SocksServerConnectHandler());
                        ctx.pipeline().remove(this);
                        //ss-local just res SUCCESS
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.SUCCESS,
                                Socks5AddressType.IPv4,
                                "0.0.0.0",
                                0));

                        ctx.channel().attr(SSCommon.REMOTE_DES_SOCKS5).set(socks5CmdRequest);

//                        ctx.fireChannelRead(socksRequest);
                    } else if (socks5CmdRequest.type() == Socks5CommandType.UDP_ASSOCIATE) {
                        ctx.pipeline().remove(this);

                        InetSocketAddress bindAddr = (InetSocketAddress) ctx.channel().localAddress();
                        InetAddress bindId = bindAddr.getAddress();
                        Socks5AddressType bindAddrType = Socks5AddressType.IPv4;
                        if (bindId instanceof Inet4Address) {
                            bindAddrType = Socks5AddressType.IPv4;
                        } else if (bindId instanceof Inet6Address) {
                            bindAddrType = Socks5AddressType.IPv6;
                        }

                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.SUCCESS,
                                bindAddrType,
                                bindId.getHostAddress(),
                                bindAddr.getPort()));

//                       ctx.fireChannelRead(socksRequest);
                    } else {
                        ctx.close();
                    }
                } else {
                    ctx.close();
                }
                break;
            case UNKNOWN:
                ctx.close();
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocksServerUtils.closeOnFlush(ctx.channel());
    }
}