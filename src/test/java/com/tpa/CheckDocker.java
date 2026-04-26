package com.tpa;

public class CheckDocker {
    public static void main(String[] args) {
        System.out.println("TCP 2375: " + isTcpPortOpen("localhost", 2375));
        try {
            System.out.println("Testcontainers: " + org.testcontainers.DockerClientFactory.instance().isDockerAvailable());
        } catch (Exception e) {
            System.out.println("Testcontainers Error: " + e.getMessage());
        }
    }

    private static boolean isTcpPortOpen(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 800);
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }
}
