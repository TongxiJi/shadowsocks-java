package cn.wowspeeder;

public class ServerStart {

    public static void main(String[] args) throws InterruptedException {
        try {
            SSServer.getInstance().start();
        } catch (Exception e) {
            e.printStackTrace();
            SSServer.getInstance().stop();
            System.exit(-1);
        }

        while (true) {
            Thread.sleep(1000);
        }
    }
}
