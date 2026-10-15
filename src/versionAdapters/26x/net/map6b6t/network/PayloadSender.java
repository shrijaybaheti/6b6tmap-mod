package net.map6b6t.network;

public class PayloadSender {
    public static int getSuccessCount() {
        return UploadService.get().stats().uploaded();
    }
}
