package cn.wowspeeder;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.cli.*;

public class Application {
    private static InternalLogger logger = InternalLoggerFactory.getInstance(Application.class);

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option option = new Option("s", "server", false, "server listen address");
        options.addOption(option);

        option = new Option("c", "client", false, "server connect address");
        options.addOption(option);

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);


        if (commandLine.hasOption("s")) {
            SSServer.getInstance().start();
        } else if (commandLine.hasOption("c")) {
            SSLocal.getInstance().start();
        } else {
            logger.error("not found run type");
        }

        logger.info("start success!");
    }
}
