package cn.wowspeeder.ss;

import cn.wowspeeder.encryption.CryptUtil;
import cn.wowspeeder.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class SSCipherEncoder extends MessageToMessageEncoder<ByteBuf> {
    private static Logger logger = LoggerFactory.getLogger(SSCipherEncoder.class);


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
//        logger.debug("encode msg size:" + msg.readableBytes());
        ICrypt _crypt = ctx.channel().attr(SSCommon.CIPHER).get();
//        Boolean isUdp = ctx.channel().attr(SSCommon.IS_UDP).get();
        byte[] encryptedData = CryptUtil.encrypt(_crypt, msg);
//        logger.debug("encode after encryptedData size:{}",encryptedData.length);
        out.add(Unpooled.wrappedBuffer(encryptedData));
    }
}
