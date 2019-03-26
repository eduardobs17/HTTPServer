import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class HTTPServer extends Thread {

    private Socket client;
    private DataOutputStream outClient = null;
    private static final int puerto = 8080;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(puerto, 10, InetAddress.getByName("127.0.0.1"));
        System.out.println("HTTPServer iniciado en el puerto " +  puerto + '\n');

        while (true) {
            Socket socket = serverSocket.accept();
            HTTPServer httpServer = new HTTPServer(socket);
            httpServer.start();
        }
    }

    private HTTPServer(Socket cl) { client = cl; }

    public void run() {
        try {
            System.out.println("El cliente " + client.getInetAddress() + ":" + client.getPort() + " esta conectado.");

            BufferedReader inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outClient = new DataOutputStream(client.getOutputStream());

            String requestString = inClient.readLine();
            String headerLine = requestString;

            if (headerLine == null)
                return;

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            String httpMethod = tokenizer.nextToken();
            String httpQueryString = tokenizer.nextToken();

            System.out.println("La peticion HTTP es...");
            while (inClient.ready()) {
                /* Read the HTTP request until the end */
                System.out.println(requestString);
                requestString = inClient.readLine();
            }
            System.out.println('\n');

            if (httpMethod.equals("GET")) {
                if (httpQueryString.equals("/")) {
                    homePage();
                } else if (httpQueryString.startsWith("/hola")) {
                    helloPage(httpQueryString.substring(httpQueryString.lastIndexOf('/') + 1));
                } else {
                    sendResponse(404, "<b>No se encontró el recurso.</b>");
                }
            } else if (httpMethod.equals("HEAD")) {
                System.out.println("asdasdasdasdasdasd");
            } else if (httpMethod.equals("POST")) {
                System.out.println("asdasdasd");
            } else {
                sendResponse(404, "<b>No se encontró el recurso.</b>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to compose the response back to the client.
     */
    private void sendResponse(int statusCode, String responseString) throws Exception {
        String HTML_START = "<html><title>Mini servidor HTTP</title><body>";
        String HTML_END = "</body></html>";
        String NEW_LINE = "\r\n";

        String statusLine, contentLengthLine;
        String serverdetails = Headers.SERVER + ": Servidor Java";
        String contentTypeLine = Headers.CONTENT_TYPE + ": text/html" + NEW_LINE;

        if (statusCode == 200) {
            statusLine = Status.HTTP_200;
        } else {
            statusLine = Status.HTTP_404;
        }

        statusLine += NEW_LINE;
        responseString = HTML_START + responseString + HTML_END;
        contentLengthLine = Headers.CONTENT_LENGTH + responseString.length() + NEW_LINE;

        outClient.writeBytes(statusLine);
        outClient.writeBytes(serverdetails);
        outClient.writeBytes(contentTypeLine);
        outClient.writeBytes(contentLengthLine);
        outClient.writeBytes(Headers.CONNECTION + ": close" + NEW_LINE);

        /* adding the new line between header and body */
        outClient.writeBytes(NEW_LINE);
        outClient.writeBytes(responseString);
        outClient.close();
    }

    /**
     * Method used to compose the home page response to the client
     *
     */
    private void homePage() throws Exception {
        String responseBuffer = "<b>HTTP Server Home Page.</b><BR><BR>" +
                "<b>Para ver la pagina \"hola\", use: http://localhost:" + puerto + "/hola/nombre</b><BR>";
        sendResponse(200, responseBuffer);
    }

    /**
     * Method used to compose the hello page response to the client
     *
     */
    private void helloPage(String name) throws Exception {
        sendResponse(200, "<b>Hola:" + name + "</b><BR>");
    }

    /**
     * class used to store headers constants
     *
     */
    private static class Headers {
        static final String SERVER = "Server";
        static final String CONNECTION = "Connection";
        static final String CONTENT_LENGTH = "Content-Length";
        static final String CONTENT_TYPE = "Content-Type";
    }

    /**
     * class used to store status line constants
     *
     */
    private static class Status {
        static final String HTTP_200 = "HTTP/1.1 200 OK";
        static final String HTTP_404 = "HTTP/1.1 404 Not Found";
    }
}