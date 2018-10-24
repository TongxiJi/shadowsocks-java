package cn.wowspeeder.ss.obfs.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link HttpServerCodec} smell nice!
 */
public class HttpSimpleHandler extends SimpleChannelInboundHandler<HttpRequest> {
    public static ByteBuf   HTTP_SIMPLE_DELIMITER = Unpooled.copiedBuffer("\r\n".getBytes());

    private static Logger logger = LoggerFactory.getLogger(HttpSimpleHandler.class);


    public static final String OBFS_NAME = "http_simple";


    public static List<ChannelHandler> getHandlers() {
        List<ChannelHandler> channels = new ArrayList<>();
        channels.add(new HttpServerCodec());
        channels.add(new HttpSimpleHandler());
        return channels;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        if (msg.decoderResult() != DecoderResult.SUCCESS) {
            logger.error("simple_http decode error ,pip close");
            ctx.channel().close();
            return;
        }
        logger.debug(msg.toString());
        String strHexData = msg.uri().replace("%", "").replace("/", "");
        ByteBuf encodeData = Unpooled.wrappedBuffer(Hex.decode(strHexData));
        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
        ctx.fireChannelRead(encodeData);
        ctx.pipeline().remove(HttpServerCodec.class);
        ctx.pipeline().remove(this);
    }
}
