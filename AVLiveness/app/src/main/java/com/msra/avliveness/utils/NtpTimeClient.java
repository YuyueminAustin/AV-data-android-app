package com.msra.avliveness.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public final class NtpTimeClient {
    private static final int NTP_PORT = 123;
    private static final int NTP_PACKET_SIZE = 48;
    private static final long NTP_TO_UNIX_EPOCH_DELTA = 2208988800L;

    private NtpTimeClient() {}

    public static double queryUnixTimeSeconds(String server, int timeoutMs) throws IOException {
        byte[] buffer = new byte[NTP_PACKET_SIZE];
        buffer[0] = 0x1B; // LI=0, VN=3, Mode=3 (client)

        InetAddress address = InetAddress.getByName(server);
        DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);
        DatagramPacket response = new DatagramPacket(new byte[NTP_PACKET_SIZE], NTP_PACKET_SIZE);

        DatagramSocket socket = new DatagramSocket();
        try {
            socket.setSoTimeout(timeoutMs);
            socket.send(request);
            socket.receive(response);
        } catch (SocketTimeoutException e) {
            throw new IOException("NTP request timeout", e);
        } finally {
            socket.close();
        }

        byte[] data = response.getData();
        long seconds = readUnsignedInt(data, 40);
        long fraction = readUnsignedInt(data, 44);
        double fractionSeconds = fraction / 4294967296.0; // 2^32
        return (seconds - NTP_TO_UNIX_EPOCH_DELTA) + fractionSeconds;
    }

    private static long readUnsignedInt(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | ((long) (data[offset + 3] & 0xFF));
    }
}
