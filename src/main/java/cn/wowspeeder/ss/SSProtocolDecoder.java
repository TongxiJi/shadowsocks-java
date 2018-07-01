package cn.wowspeeder.ss;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;


/**
 * https://www.shadowsocks.org/en/spec/Protocol.html
 * [1-byte type][variable-length host][2-byte port]
 * The following address types are defined:
 * <p>
 * 0x01: host is a 4-byte IPv4 address.
 * 0x03: host is a variable length string, starting with a 1-byte length, followed by up to 255-byte domain name.
 * 0x04: host is a 16-byte IPv6 address.
 * The port number is a 2-byte big-endian unsigned integer.
 **/
public class SSProtocolDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static Logger logger = LoggerFactory.getLogger(SSProtocolDecoder.class);


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (msg.readableBytes() < 1 + 1 + 2) {// [1-byte type][variable-length host][2-byte port]
            return;
        }

        Boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();
        Boolean isFirstTcpPack = ctx.channel().attr(SSCommon.IS_FIRST_TCP_PACK).get();

//        logger.debug("dataBuff readableBytes:" + msg.readableBytes() + isFirstTcpPack);

        if (isUdp || (!isUdp && isFirstTcpPack != null && isFirstTcpPack)) {
            SSAddrRequest addrRequest = SSAddrRequest.getAddrRequest(msg);
            if (addrRequest == null) {
                logger.error("failed to get address request from {}", ctx.channel().attr(SSCommon.CLIENT).get().getHostString());
                if (!ctx.channel().attr(SSCommon.IS_UDP).get()) {
                    ctx.close();
                }
                return;
            }
            logger.debug(ctx.channel().id().toString() + " addressType = " + addrRequest.addressType() + ",host = " + addrRequest.host() + ",port = " + addrRequest.port() + ",dataBuff = "
                    + msg.readableBytes());
            ctx.channel().attr(SSCommon.REMOTE_DES).set(new InetSocketAddress(addrRequest.host(), addrRequest.port()));
            ctx.channel().attr(SSCommon.IS_FIRST_TCP_PACK).set(false);
        }
//        if (msg.readableBytes() == 0) {
//            return;
//        }
        out.add(msg.retain());
        //TODO test data
//        InetSocketAddress recipient = ctx.channel().attr(SSCommon.CLIENT).get();
//        ByteBuf testbuff = Unpooled.buffer();
//        testbuff.writeBytes(new byte[]{0x01, 0x02});
//        ctx.channel().writeAndFlush(new DatagramPacket(testbuff, recipient));
    }
}
