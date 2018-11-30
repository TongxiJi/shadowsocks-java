package cn.wowspeeder.ss;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

public class SSServerCheckerSend extends ChannelOutboundHandlerAdapter {
    private static InternalLogger logger =  InternalLoggerFactory.getInstance(SSServerCheckerSend.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();
        if (isUdp) {
            InetSocketAddress client = ctx.channel().attr(SSCommon.RemoteAddr).get();
            msg = new DatagramPacket((ByteBuf) msg, client);
        }
        super.write(ctx,msg,promise);
    }
}
