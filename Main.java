import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        // Use a thread pool to handle concurrent connections
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try {
            ServerSocket serverSocket = new ServerSocket(4221);
            serverSocket.setReuseAddress(true);

            while (true) {
                // Accept a new client and pass it to the thread pool
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");

                // Use a thread to handle the connection
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            // Step 1: Parse the request line
            String requestLine = in.readLine();
            if (requestLine == null) {
                return;
            }
            System.out.println("Request: " + requestLine);
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String path = requestParts[1];

            // Step 2: Parse headers
            Map<String, String> headers = new HashMap<>();
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                String[] headerParts = line.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }

            // Step 3: Handle request body (for POST/PUT requests)
            StringBuilder body = new StringBuilder();
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
                char[] buffer = new char[contentLength];
                in.read(buffer, 0, contentLength);
                body.append(buffer);
            }

            // Step 4: Prepare the response based on the path
            String httpResponse;
            if ("/".equals(path)) {
                httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + "<html><body><h1>Welcome to the Home Page</h1></body></html>";
            } else if ("/file".equals(path)) {
                // Return a file
                File file = new File("index.html");
                if (file.exists()) {
                    httpResponse = "HTTP/1.1 200 OK\r\n\r\n" + readFile(file);
                } else {
                    httpResponse = "HTTP/1.1 404 Not Found\r\n\r\nFile Not Found";
                }
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n" + "<html><body><h1>404 Not Found</h1></body></html>";
            }

            // Step 5: Write the response to the client
            out.write(httpResponse.getBytes("UTF-8"));
            out.flush();

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Failed to close client socket");
            }
        }
    }

    // Utility method to read file contents
    private static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
