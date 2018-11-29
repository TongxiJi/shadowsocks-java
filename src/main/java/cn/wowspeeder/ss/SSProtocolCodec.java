package cn.wowspeeder.ss;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
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
public class SSProtocolCodec extends MessageToMessageCodec<ByteBuf,ByteBuf> {
    private static InternalLogger logger =  InternalLoggerFactory.getInstance(SSProtocolCodec.class);


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        logger.debug("encode " + msg.readableBytes());
        //组装ss协议
        //udp [target address][payload]
        //tcp only [payload]
        boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();

        if (isUdp) {
            ByteBuf addrBuff = Unpooled.buffer(128);
            SSAddrRequest ssAddr;
            InetSocketAddress remoteSrc = ctx.channel().attr(SSCommon.REMOTE_SRC).get();
//        logger.info("remote addr:" + sender.getAddress().toString());
            if (remoteSrc.getAddress() instanceof Inet6Address) {
                ssAddr = new SSAddrRequest(SocksAddressType.IPv6, remoteSrc.getHostString(), remoteSrc.getPort());
            } else if (remoteSrc.getAddress() instanceof Inet4Address) {
                ssAddr = new SSAddrRequest(SocksAddressType.IPv4, remoteSrc.getHostString(), remoteSrc.getPort());
            } else {
                ssAddr = new SSAddrRequest(SocksAddressType.DOMAIN, remoteSrc.getHostString(), remoteSrc.getPort());
            }
            ssAddr.encodeAsByteBuf(addrBuff);
            out.add(Unpooled.wrappedBuffer(addrBuff, msg.retain()));
        } else {
            out.add(msg.retain());
        }
        logger.debug("encode done");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (msg.readableBytes() < 1 + 1 + 2) {// [1-byte type][variable-length host][2-byte port]
            return;
        }

        Boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();
        Boolean isFirstTcpPack = ctx.channel().attr(SSCommon.IS_FIRST_TCP_PACK).get();

        logger.debug("dataBuff readableBytes:" + msg.readableBytes());

        if (isUdp || (isFirstTcpPack != null && isFirstTcpPack)) {
            SSAddrRequest addrRequest = SSAddrRequest.getAddrRequest(msg);
            if (addrRequest == null) {
                logger.error("fail to get address request from {},pls check client's cipher setting", ctx.channel().attr(SSCommon.CLIENT).get().getHostString());
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
        out.add(msg.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        InetSocketAddress clientSender = ctx.channel().attr(SSCommon.CLIENT).get();
        logger.error("client {},error :{}", clientSender.toString(), cause.getMessage());
//        super.exceptionCaught(ctx, cause);
    }
}
