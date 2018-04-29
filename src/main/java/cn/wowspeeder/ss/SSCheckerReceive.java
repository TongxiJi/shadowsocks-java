package cn.wowspeeder.ss;

import cn.wowspeeder.encryption.CryptFactory;
import cn.wowspeeder.encryption.ICrypt;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSCheckerReceive extends SimpleChannelInboundHandler<Object> {
    private static Logger logger = LoggerFactory.getLogger(SSCheckerReceive.class);

    private final String method;
    private final String password;

    public SSCheckerReceive(String method, String password) {
        super(false);
        this.method = method;
        this.password = password;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
//        logger.debug("channelRead0");

        ICrypt _crypt = ctx.channel().attr(SSCommon.CIPHER).get();

        boolean isUdp = msg instanceof DatagramPacket;
//        logger.debug("channelRead0 isUdp:"+isUdp);
        if (_crypt == null || isUdp) {
            _crypt = CryptFactory.get(this.method, this.password);
            ctx.channel().attr(SSCommon.CIPHER).set(_crypt);
        }
        ctx.channel().attr(SSCommon.IS_UDP).set(isUdp);

        if (isUdp) {
            ctx.channel().attr(SSCommon.CLIENT).set(((DatagramPacket) msg).sender());
            ctx.fireChannelRead(((DatagramPacket) msg).content());
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
