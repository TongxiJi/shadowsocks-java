package cn.wowspeeder.ss;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.socks.SocksAddressType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.List;

public class SSProtocolEncoder extends MessageToMessageEncoder<ByteBuf> {
    private static Logger logger = LoggerFactory.getLogger(SSProtocolEncoder.class);


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
//        logger.debug("encode:" + msg.readableBytes());
        //组装ss协议
        //udp [target address][payload]
        //tcp only [payload]
        ByteBuf sendBuff = Unpooled.buffer(32 * 1024);
        boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();
        if (isUdp) {
            SSAddrRequest ssAddr = null;
            InetSocketAddress remoteSrc = ctx.channel().attr(SSCommon.REMOTE_SRC).get();
//        logger.info("remote addr:" + sender.getAddress().toString());
            if (remoteSrc.getAddress() instanceof Inet6Address) {
                ssAddr = new SSAddrRequest(SocksAddressType.IPv6, remoteSrc.getHostString(), remoteSrc.getPort());
            } else if (remoteSrc.getAddress() instanceof Inet4Address) {
                ssAddr = new SSAddrRequest(SocksAddressType.IPv4, remoteSrc.getHostString(), remoteSrc.getPort());
            } else {
                ssAddr = new SSAddrRequest(SocksAddressType.DOMAIN, remoteSrc.getHostString(), remoteSrc.getPort());
            }
            ssAddr.encodeAsByteBuf(sendBuff);
        }
        sendBuff.writeBytes(msg);
        out.add(sendBuff);
    }
}
