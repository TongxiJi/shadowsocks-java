package cn.wowspeeder.ss;

import cn.wowspeeder.encryption.CryptUtil;
import cn.wowspeeder.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SSCipherDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static Logger logger = LoggerFactory.getLogger(SSCipherDecoder.class);


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> list) throws Exception {
        ICrypt _crypt = ctx.channel().attr(SSCommon.CIPHER).get();
        byte[] data = CryptUtil.decrypt(_crypt, msg);
        if (data == null) {
            ctx.channel().close();
        }
        list.add(msg.retain().clear().writeBytes(data));//
    }
}
