import java.io.IOException;
import java.io.InputStream;

class ServerListener extends Thread {
    private InputStream inputStream;
    private boolean isFirstMessage;

    public ServerListener(InputStream inputStream) {
        this.inputStream = inputStream;
        this.isFirstMessage = true;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytesRead;

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String message = new String(buffer, 0, bytesRead);
                String[] messages = message.split("\\n");

                for (String msg : messages) {
                    if (isFirstMessage) {
                        System.out.println(msg.trim());
                        isFirstMessage = false;
                    } else {
                        System.out.println(msg.trim());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
