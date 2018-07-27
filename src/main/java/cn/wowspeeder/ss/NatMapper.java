package cn.wowspeeder.ss;

import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NatMapper {
    private static InternalLogger logger =  InternalLoggerFactory.getInstance(NatMapper.class);

    private static Map<InetSocketAddress, Channel> udpTable = new ConcurrentHashMap<>();

    static void putUdpChannel(InetSocketAddress udpTarget, Channel udpChannel) {
        udpTable.put(udpTarget, udpChannel);
    }

    static Channel getUdpChannel(InetSocketAddress udpTarget) {
        return udpTable.get(udpTarget);
    }

    private static Channel removeUdpMapping(InetSocketAddress udpTarget) {
        return udpTable.remove(udpTarget);
    }

    static void closeChannelGracefully(InetSocketAddress source) {
        Channel udpChannel = removeUdpMapping(source);
        if (udpChannel != null && udpChannel.isActive()) {
            logger.debug("\tProxy << Target \tDisconnect");
            udpChannel.close();
        }
    }
}
