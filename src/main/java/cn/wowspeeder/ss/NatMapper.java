package cn.wowspeeder.ss;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NatMapper {
    private static final InternalLogger log;

    static {
        log = InternalLoggerFactory.getInstance(NatMapper.class);
    }

    private static Map<InetSocketAddress, Channel> udpTable = new ConcurrentHashMap<>();
    private static Map<InetSocketAddress, Channel> tcpTable = new ConcurrentHashMap<>();

    static void putTcpChannel(InetSocketAddress udpTarget, Channel tcpChannel) {
        tcpTable.put(udpTarget, tcpChannel);
    }

    static void putUdpChannel(InetSocketAddress udpTarget, Channel udpChannel) {
        udpTable.put(udpTarget, udpChannel);
    }


    static Channel getTcpChannel(InetSocketAddress udpTarget) {
        return tcpTable.get(udpTarget);
    }

    static Channel getUdpChannel(InetSocketAddress udpTarget) {
        return udpTable.get(udpTarget);
    }

    private static Channel removeUdpMapping(InetSocketAddress udpTarget) {
        return udpTable.remove(udpTarget);
    }

    private static Channel removeTcpMapping(InetSocketAddress udpTarget) {
        return tcpTable.remove(udpTarget);
    }

    static void closeChannelGracefully(InetSocketAddress source) {
        Channel udpChannel = removeUdpMapping(source);
        Channel tcpChannel = removeTcpMapping(source);
        if (udpChannel != null && udpChannel.isActive()) {
            log.debug("\tProxy << Target \tDisconnect");
            udpChannel.close();
        }
        if (tcpChannel != null && tcpChannel.isActive()) {
            tcpChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            log.debug("\tClient << Proxy \tDisconnect");
        }
    }
}
