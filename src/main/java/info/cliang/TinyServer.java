package info.cliang;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map.Entry;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public class TinyServer {
    private final ServerSocket serverSocket;

    private final String rootPath;

    public TinyServer(int portNumber, String rootPath) throws IOException {
        this.serverSocket = new ServerSocket(portNumber);
        this.rootPath = rootPath;
    }

    public void listen() {
        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                handleRequest(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        HttpRequest httpRequest = parseRequest(clientSocket);
        HttpResponse httpResponse = createResponse(httpRequest);
        httpResponse = addCommonServerHeaders(httpResponse);
        sendResponse(httpResponse, clientSocket);
    }

    private void sendResponse(HttpResponse httpResponse, Socket clientSocket) throws IOException {
        DataOutputStream writer = new DataOutputStream(clientSocket.getOutputStream());
        String statusLine = String.format("%s %s %s", httpResponse.getVersion(), httpResponse.getStatusCode(),
            httpResponse.getReasonPhrase());
        writer.writeBytes(statusLine + "\n");

        for (Entry<String, String> header : httpResponse.getHeaders().entrySet()) {
            writer.writeBytes(header.getKey() + ": " + header.getValue() + "\n");
        }

        writer.writeBytes("\n");
        writer.write(httpResponse.getBody());
    }

    private HttpRequest parseRequest(Socket clientSocket) throws IOException {
        BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        HttpRequest httpRequest = new HttpRequest();
        String requestLine = clientReader.readLine();
        String[] splits = requestLine.split(" ");
        httpRequest.setHttpMethod(splits[0]);
        httpRequest.setPath(splits[1]);
        httpRequest.setVersion(splits[2]);
        return httpRequest;
    }

    private HttpResponse createResponse(HttpRequest httpRequest) throws IOException {
        Path resourcePath = Paths.get(rootPath, httpRequest.getPath());

        if (Files.exists(resourcePath)) {
            return createSuccessResponse(resourcePath);
        } else {
            return create404Response();
        }
    }

    private HttpResponse createSuccessResponse(Path resourcePath) throws IOException {
        HttpResponse httpResponse = new HttpResponse();
        byte[] fileContent = Files.readAllBytes(resourcePath);

        httpResponse.setVersion("HTTP/1.1");
        httpResponse.setStatusCode("200");
        httpResponse.setReasonPhrase("OK");

        httpResponse.setBody(fileContent);
        httpResponse.addHeader("Content-Type", "text/html");
        httpResponse.addHeader("Content-Length", String.valueOf(fileContent.length));

        return httpResponse;
    }

    private HttpResponse create404Response() {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setVersion("HTTP/1.1");
        httpResponse.setStatusCode("404");
        httpResponse.setReasonPhrase("NOT FOUND");

        String body = "404 Not Found";
        httpResponse.setBody(body.getBytes());
        httpResponse.addHeader("Content-Type", "text/plain");
        httpResponse.addHeader("Content-Length", String.valueOf(body.length()));
        return httpResponse;
    }

    private HttpResponse addCommonServerHeaders(HttpResponse httpResponse) {
        String gmtDate = RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")));
        httpResponse.addHeader("Date", gmtDate);
        httpResponse.addHeader("Server", "TinyServer/0.1 (Mac OSX)");
        return httpResponse;
    }
}
