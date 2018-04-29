package cn.wowspeeder.ss;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class SSCheckerSend extends ChannelOutboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(SSCheckerSend.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();
        if (isUdp) {
            InetSocketAddress client = ctx.channel().attr(SSCommon.CLIENT).get();
            msg = new DatagramPacket((ByteBuf) msg, client);
        }
        super.write(ctx,msg,promise);
    }
}
