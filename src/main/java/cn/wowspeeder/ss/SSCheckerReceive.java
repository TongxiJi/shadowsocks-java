package cn.wowspeeder.ss;

import cn.wowspeeder.encryption.CryptFactory;
import cn.wowspeeder.encryption.ICrypt;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class SSCheckerReceive extends SimpleChannelInboundHandler<Object> {
    private static InternalLogger logger =  InternalLoggerFactory.getInstance(SSCheckerReceive.class);

    private String method;
    private String password;
    private boolean isForUDP = false;

    public SSCheckerReceive(String method, String password) {
        this(method, password, false);
    }

    public SSCheckerReceive(String method, String password, boolean isForUDP) {
        super(false);
        this.method = method;
        this.password = password;
        this.isForUDP = isForUDP;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ICrypt _crypt = CryptFactory.get(this.method, this.password);
        assert _crypt != null;
        _crypt.ivSetIgnore(isForUDP);
        ctx.channel().attr(SSCommon.CIPHER).set(_crypt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
//        logger.debug("channelRead0");
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
