package cn.wowspeeder.ss;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

public class SSCheckerReceive extends SimpleChannelInboundHandler<Object> {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(SSCheckerReceive.class);

    public SSCheckerReceive() {
        super(false);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean isUdp = msg instanceof DatagramPacket;
        ctx.channel().attr(SSCommon.IS_UDP).set(isUdp);

        if (isUdp) {
            DatagramPacket udpRaw = ((DatagramPacket) msg);
            if (udpRaw.content().readableBytes() < 4) { //no cipher, min size = 1 + 1 + 2 ,[1-byte type][variable-length host][2-byte port]
                return;
            }
            ctx.channel().attr(SSCommon.CLIENT).set(udpRaw.sender());
            ctx.fireChannelRead(udpRaw.content());
        } else {
            ctx.channel().attr(SSCommon.CLIENT).set((InetSocketAddress) ctx.channel().remoteAddress());
            ctx.channel().attr(SSCommon.IS_FIRST_TCP_PACK).set(true);
            ctx.channel().pipeline().remove(this);
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
