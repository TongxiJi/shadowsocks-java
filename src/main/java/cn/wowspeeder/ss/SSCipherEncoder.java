package cn.wowspeeder.ss;

import cn.wowspeeder.encryption.CryptUtil;
import cn.wowspeeder.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.List;

public class SSCipherEncoder extends MessageToMessageEncoder<ByteBuf> {
    private static InternalLogger logger =  InternalLoggerFactory.getInstance(SSCipherEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
//        logger.debug("encode msg size:" + msg.readableBytes());
        ICrypt _crypt = ctx.channel().attr(SSCommon.CIPHER).get();
//        Boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();
        byte[] encryptedData = CryptUtil.encrypt(_crypt, msg);
//        logger.debug("encode after encryptedData size:{}",encryptedData.length);
        out.add(msg.retain().clear().writeBytes(encryptedData));
    }
}
