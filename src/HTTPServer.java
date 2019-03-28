import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

/** Clase principal. */
public class HTTPServer extends Thread {

    private Socket client;
    private DataOutputStream outClient = null;
    private static final int puerto = 8080;
    private final static String docRaiz = "html";
    private final static String fichGET = "index.html";
    private final static String fichPOST = "post.html";

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(puerto, 10, InetAddress.getByName("127.0.0.1"));
        System.out.println("HTTP server iniciado en el puerto " +  puerto + '\n');

        while (true) {
            Socket socket = serverSocket.accept();
            HTTPServer httpServer = new HTTPServer(socket);
            httpServer.start();
        }
    }

    private HTTPServer(Socket cl) { client = cl; }

    public void run() {
        try {
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
            StringBuilder peticion = new StringBuilder();
            int postData = -1;
            while (inClient.ready()) {
                if (requestString.contains("Content-Length:")) {
                    postData = new Integer(requestString.substring(requestString.indexOf("Content-Length:") + 16));
                }
                peticion.append(requestString).append("\n");
                if (requestString.equals("")) { break; }
                requestString = inClient.readLine();
            }
            System.out.println(peticion + "\n\n");

            switch (httpMethod) {
                case "GET":
                    sendResponse(200, httpQueryString, "GET", "");
                    break;
                case "HEAD":
                    sendResponse(200, httpQueryString, "HEAD", "");
                    break;
                case "POST":
                    String postDataS = "";
                    if (postData > 0) {
                        char[] charArray = new char[postData];
                        inClient.read(charArray, 0, postData);
                        postDataS = new String(charArray);
                        postDataS = postDataS.substring(5);
                    }
                    sendResponse(200, httpQueryString, "POST", postDataS);
                    break;
                default:
                    sendResponse(501, "No se puede cumplir la petición en estos momentos.</b>", "ERROR", "");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to compose the response back to the client.
     * tipo = 0 => get o post; tipo = 1 => head
     */
    private void sendResponse (int statusCode, String responseString, String tipoPeticion, String userPost) {
        String HTML_START = "<html><title>Mini servidor HTTP</title><body>";
        String HTML_END = "</body></html>";
        String NEW_LINE = "\r\n";

        String statusLine, contentLengthLine;
        String date = Headers.DATE + ": " + new Date().toString() + NEW_LINE;
        String serverdetails = Headers.SERVER + ": Servidor Java" + NEW_LINE;
        String contentTypeLine = Headers.CONTENT_TYPE + ": ";

        if (statusCode == 200) {
            statusLine = Status.HTTP_200;
        } else if (statusCode == 404) {
            statusLine = Status.HTTP_404;
        } else if (statusCode == 4500) {
            statusLine = Status.HTTP_500;
        } else {
            statusLine = Status.HTTP_501;
        }

        if (!tipoPeticion.equals("ERROR")) {
            String fich;

            if (responseString.endsWith("/")) {
                fich = docRaiz + "/" + fichGET;
            } else if (!tipoPeticion.equals("POST")){
                fich = docRaiz + responseString;
            } else {
                fich = docRaiz + "/" + fichPOST;
            }

            try {
                File f = new File(fich);

                if (!f.exists()) {
                    sendResponse(404, "<b>No se encontró el recurso.</b>", "ERROR", "");
                    return;
                }

                if (!f.canRead()) {
                    sendResponse(500, "<b>Error interno.</b>", "ERROR", "");
                     return;
                }

                // Ahora lee el fichero que se ha solicitado
                InputStream sin = new FileInputStream(f);
                String tipoMime = MimeTypes.mimeTypeString(fich);
                contentTypeLine +=  tipoMime + NEW_LINE;
                int n = sin.available();

                statusLine += NEW_LINE;
                contentLengthLine = Headers.CONTENT_LENGTH + ": " + n + NEW_LINE;

                headers(NEW_LINE, statusLine, contentLengthLine, date, serverdetails, contentTypeLine);

                if (!tipoPeticion.equals("HEAD")) {
                    byte[] buf = new byte[2048];
                    while ((n = sin.read(buf)) >= 0) {
                        outClient.write(buf, 0, n);
                    }
                }

                sin.close();
                outClient.close();
            } catch (IOException e) {
                System.out.println("ERROR");
            }
        } else {
            try {
                statusLine += NEW_LINE;
                responseString = HTML_START + "<b>ERROR " + statusCode + ". </b>" + responseString + HTML_END;
                contentLengthLine = Headers.CONTENT_LENGTH + ": " + responseString.length() + NEW_LINE;

                headers(NEW_LINE, statusLine, contentLengthLine, date, serverdetails, contentTypeLine);
                outClient.writeBytes(responseString);
                outClient.close();
            } catch (IOException e) {
                System.out.println("ERROR");
            }
        }
    }

    /** Metodo que completa los encabezados. */
    private void headers(String NEW_LINE, String statusLine, String contentLengthLine, String date, String serverdetails, String contentTypeLine) throws IOException {
        outClient.writeBytes(statusLine);
        outClient.writeBytes(date);
        outClient.writeBytes(serverdetails);
        outClient.writeBytes(contentTypeLine);
        outClient.writeBytes(contentLengthLine);
        outClient.writeBytes(Headers.CONNECTION + ": close" + NEW_LINE);

        outClient.writeBytes(NEW_LINE);
    }

    /** Clase que maneja los encabezados. */
    private static class Headers {
        static final String SERVER = "Server";
        static final String CONNECTION = "Connection";
        static final String CONTENT_LENGTH = "Content-Length";
        static final String CONTENT_TYPE = "Content-Type";
        static final String DATE = "Date";
        static final String ACCEPT = "Accept";
    }

    /** Clase que maneja los codigos HTTP. */
    private static class Status {
        static final String HTTP_200 = "HTTP/1.1 200 OK";
        static final String HTTP_404 = "HTTP/1.1 404 Not Found";
        static final String HTTP_406 = "HTTP/1.1 406 Not Acceptable";
        static final String HTTP_500 = "HTTP/1.1 500 Internal Server Error";
        static final String HTTP_501 = "HTTP/1.1 501 Not Implemented";
    }

    /** Clase que maneja los mime types. */
    private static class MimeTypes {
        private final static String mime_text_plain = "text/plain";
        private final static String mime_text_html = "text/html";
        private final static String mime_image_gif = "image/gif";
        private final static String mime_image_jpg = "image/jpg";
        private final static String mime_image_png = "image/png";
        private final static String mime_audio_midi = "audio/midi";
        private final static String mime_audio_mpeg = "audio/mpeg";
        private final static String mime_app_os = "application/octet-stream";

        /** Devuelve el tipo MIME que corresponde a un nombre de archivo dado. */
        static String mimeTypeString(String fichero) {
            String tipo;

            if (fichero.endsWith(".html") || fichero.endsWith(".htm"))
                tipo = mime_text_html;
            else if (fichero.endsWith(".class"))
                tipo = mime_app_os;
            else if (fichero.endsWith(".gif"))
                tipo = mime_image_gif;
            else if (fichero.endsWith(".jpg") || fichero.endsWith(".jpeg"))
                tipo = mime_image_jpg;
            else if (fichero.endsWith(".png"))
                tipo = mime_image_png;
            else if (fichero.endsWith(".mid"))
                tipo = mime_audio_midi;
            else if (fichero.endsWith(".mpeg") || fichero.endsWith(".mpg") || fichero.endsWith(".mp1") || fichero.endsWith(".mp2") || fichero.endsWith(".mp3"))
                tipo = mime_audio_mpeg;
            else
                tipo = mime_text_plain;

            return tipo;
        }
    }
}