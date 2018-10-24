package cn.wowspeeder.ss;

import cn.wowspeeder.encryption.CryptUtil;
import cn.wowspeeder.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class SSCipherDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(SSCipherDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> list) throws Exception {
        logger.debug("decode msg size:" + msg.readableBytes());
        ICrypt _crypt = ctx.channel().attr(SSCommon.CIPHER).get();
        byte[] data = CryptUtil.decrypt(_crypt, msg);
        if (data == null || data.length == 0) {
            return;
        }
        logger.debug((ctx.channel().attr(SSCommon.IS_UDP).get() ? "(UDP)" : "(TCP)") + " decode after:" + data.length);
//        logger.debug("channel id:{}  decode text:{}", ctx.channel().id(), new String(data, Charset.forName("gbk")));
        list.add(msg.retain().clear().writeBytes(data));//
    }
}
