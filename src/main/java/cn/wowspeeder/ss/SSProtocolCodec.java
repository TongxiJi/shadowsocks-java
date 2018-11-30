package cn.wowspeeder.ss;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
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
public class SSProtocolCodec extends MessageToMessageCodec<Object, Object> {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(SSProtocolCodec.class);
    private boolean isSSLocal = false;
    private boolean tcpAddressed = false;

    public SSProtocolCodec() {
        this(false);
    }

    public SSProtocolCodec(boolean isSSLocal) {
        super();
        this.isSSLocal = isSSLocal;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        ByteBuf buf;
        if (msg instanceof DatagramPacket) {
            buf = ((DatagramPacket) msg).content();
        } else if (msg instanceof ByteBuf) {
            buf = (ByteBuf) msg;
        } else {
            throw new Exception("unsupported msg type:" + msg.getClass());
        }

        logger.debug("encode " + buf.readableBytes());
        //组装ss协议
        //udp [target address][payload]
        //tcp only [payload]
        boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();

        InetSocketAddress addr = null;
        if (isUdp) {
            addr = ctx.channel().attr(
                    !isSSLocal ? SSCommon.REMOTE_SRC : SSCommon.REMOTE_DES
            ).get();
            logger.debug("addr {}", addr);
        } else if (isSSLocal && !tcpAddressed) {
            addr = ctx.channel().attr(SSCommon.REMOTE_DES).get();
            tcpAddressed = true;
        }

        if (addr == null) {
            buf.retain();
        } else {
            SSAddrRequest ssAddr;

//        logger.info("remote addr:" + sender.getAddress().toString());
            if (addr.getAddress() instanceof Inet6Address) {
                ssAddr = new SSAddrRequest(SocksAddressType.IPv6, addr.getHostString(), addr.getPort());
            } else if (addr.getAddress() instanceof Inet4Address) {
                ssAddr = new SSAddrRequest(SocksAddressType.IPv4, addr.getHostString(), addr.getPort());
            } else {
                ssAddr = new SSAddrRequest(SocksAddressType.DOMAIN, addr.getHostString(), addr.getPort());
            }

            ByteBuf addrBuff = Unpooled.buffer(128);
            ssAddr.encodeAsByteBuf(addrBuff);

            logger.debug("encode {} {}", buf.readableBytes());
            buf = Unpooled.wrappedBuffer(addrBuff, buf.retain());
            logger.debug("encode {} {}", buf.readableBytes());
        }

        if (msg instanceof DatagramPacket) {
            msg = ((DatagramPacket) msg).replace(buf);
        } else {
            msg = buf;
        }

        out.add(msg);
        logger.debug("encode done");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        ByteBuf buf;
        if (msg instanceof DatagramPacket) {
            buf = ((DatagramPacket) msg).content();
        } else if (msg instanceof ByteBuf) {
            buf = (ByteBuf) msg;
        } else {
            throw new Exception("unsupported msg type:" + msg.getClass());
        }

        if (buf.readableBytes() < 1 + 1 + 2) {// [1-byte type][variable-length host][2-byte port]
            return;
        }

        Boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();

        logger.debug("dataBuff readableBytes:" + buf.readableBytes());

        InetSocketAddress addr = null;
        if (isUdp || (!isSSLocal && !tcpAddressed)) {
            SSAddrRequest addrRequest = SSAddrRequest.getAddrRequest(buf);
            if (addrRequest == null) {
                logger.error("fail to get address request from {},pls check client's cipher setting", ctx.channel().remoteAddress());
                if (!ctx.channel().attr(SSCommon.IS_UDP).get()) {
                    ctx.close();
                }
                return;
            }
            logger.debug(ctx.channel().id().toString() + " addressType = " + addrRequest.addressType() + ",host = " + addrRequest.host() + ",port = " + addrRequest.port() + ",dataBuff = "
                    + buf.readableBytes());

            addr = new InetSocketAddress(addrRequest.host(), addrRequest.port());
            ctx.channel().attr(
                    !isSSLocal ? SSCommon.REMOTE_DES : SSCommon.REMOTE_SRC
            ).set(addr);

            if (!isUdp) {
                tcpAddressed = true;
            }
        }
        buf.retain();
        out.add(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        InetSocketAddress clientSender = ctx.channel().attr(SSCommon.RemoteAddr).get();
        logger.error("client {},error :{}", clientSender.toString(), cause.getMessage());
//        super.exceptionCaught(ctx, cause);
    }
}
