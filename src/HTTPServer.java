import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/** Clase principal. */
public class HTTPServer extends Thread {

    private Socket client;
    private DataOutputStream outClient = null;
    private static final int puerto = 8080;
    private final static String docRaiz = "html";
    private final static String fichGET = "index.html";
    private final static String fichPOST = "usuario.html";
    private static String refiereHeader = "";

    /* Para la bitacora. */
    private List<ConsultaGenerada> bitacora = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(puerto, 10, InetAddress.getByName("127.0.0.1"));
        System.out.println("HTTP server iniciado en el puerto " +  puerto + '\n');

        while (true) {
            Socket socket = serverSocket.accept();
            HTTPServer httpServer = new HTTPServer(socket);
            httpServer.start();
        }
    }

    /** Constructor de la clase HTTPServer. */
    private HTTPServer(Socket cl) { client = cl; }

    /** Codigo que ejecuta cada hilo cuando se crea. */
    public void run() {
        try {
            BufferedReader inClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            outClient = new DataOutputStream(client.getOutputStream());

            /* String que guarda el encabezado de la peticion. */
            String requestString = inClient.readLine();
            String headerLine = requestString;

            if (headerLine == null)
                return;

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            /* String que guarda el metodo HTTP que se usa. */
            String httpMethod = tokenizer.nextToken();
            String httpQueryString = tokenizer.nextToken();

            System.out.println("La peticion HTTP es...");
            /* String que guarda la peticion. */
            StringBuilder peticion = new StringBuilder();
            int postData = 0;
            int contador = 0;
            boolean isPost = false;
            String accept = "";

            while (inClient.ready()) { //Aca es
                if (requestString.contains("Content-Length:")) { //Eliminar una lina innecesaria
                    postData = Integer.valueOf(requestString.substring(requestString.indexOf("Content-Length:") + 16));
                    isPost=true;
                }
                if (requestString.contains("Accept:")) {
                    accept = requestString.substring(requestString.indexOf("Accept:") + 7);
                }

                peticion.append(requestString).append("\n");
                if (requestString.equals("")) { break; }

                requestString = inClient.readLine();
            }

            System.out.println(peticion + "\n\n");

            /* Bitacora */
            String consultaBitacora = peticion.toString();
            StringBuilder datos = new StringBuilder(); //Se inicializa vacio

            if (isPost) { //Se debe recuperar la información adicional, el PostData es el numero en Bytes
                postData = postData; //Quitar el espacio
                char[] buffer= new char[postData];
                int read = inClient.read(buffer);
                // for each byte in the buffer
                for (int i = 0; i < postData; ++i) { datos.append(buffer[i]); }
            }

            switch (httpMethod) {
                case "GET":
                    //Verificar si trae parametros
                    String getURL = httpQueryString;
                    StringTokenizer token = new StringTokenizer(getURL, "?");

                    /*--URL--*/
                    String onlyURLGet = token.nextToken();

                    /*--Parametros--*/
                    if (token.hasMoreTokens()) { datos = new StringBuilder(token.nextToken()); }

                    httpQueryString = onlyURLGet; //Ya dividido, lo devuelve al httpQuery para mandarselo a la bitacora

                    /*Se envia la bitacora*/
                    consultaBitacora(consultaBitacora, httpQueryString, datos.toString());

                    sendResponse(200, httpQueryString, "GET", accept);
                    break;

                case "HEAD":
                    /*Se envia la bitacora*/
                    consultaBitacora(consultaBitacora, httpQueryString, datos.toString());
                    sendResponse(200, httpQueryString, "HEAD", accept);
                    break;

                case "POST":
                    /*Se envia la bitacora*/
                    consultaBitacora(consultaBitacora, httpQueryString, datos.toString());
                    sendResponse(200, httpQueryString, "POST", accept);
                    break;
                default:
                    sendResponse(501, "No se puede cumplir la petición en estos momentos.</b>", "ERROR", "");
                    break;
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Metodo usado para crear la respuesta para el cliente. */
    private void sendResponse (int statusCode, String responseString, String tipoPeticion, String acceptPeticion) {
        String NEW_LINE = "\r\n";

        /* Crea las variables de los encabezados. */
        String statusLine, contentLengthLine;
        String date = Headers.DATE + ": " + new Date().toString() + NEW_LINE;
        String serverdetails = Headers.SERVER + ": Servidor Java" + NEW_LINE;
        String contentTypeLine = Headers.CONTENT_TYPE + ": ";
        String accept = Headers.ACCEPT + ": " + acceptPeticion + NEW_LINE;

        /* Pone el codigo status de la respuesta. */
        if (statusCode == 200) {
            statusLine = Status.HTTP_200;
        } else if (statusCode == 404) {
            statusLine = Status.HTTP_404;
        } else if (statusCode == 500) {
            statusLine = Status.HTTP_500;
        } else {
            statusLine = Status.HTTP_501;
        }

        /* GET, HEAD o POST */
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
                /* Abre el archivo. */
                File f = new File(fich);

                if (!f.exists()) {
                    sendResponse(404, "<b>No se encontró el recurso.</b>", "ERROR", acceptPeticion);
                    return;
                }

                if (!f.canRead()) {
                    sendResponse(500, "<b>Error interno.</b>", "ERROR", acceptPeticion);
                     return;
                }

                /* Lee el fichero que se ha solicitado. */
                InputStream sin = new FileInputStream(f);
                /* Identifica el tipo de la peticion. */
                String tipoMime = MimeTypes.mimeTypeString(fich);
                if (!acceptPeticion.equals(" */*") && !acceptPeticion.contains(tipoMime)) {
                    statusLine = Status.HTTP_406;
                }
                contentTypeLine +=  tipoMime + NEW_LINE;
                int n = sin.available();

                statusLine += NEW_LINE;
                contentLengthLine = Headers.CONTENT_LENGTH + ": " + n + NEW_LINE;

                headers(NEW_LINE, statusLine, contentLengthLine, date, serverdetails, contentTypeLine, accept);

                /* Muestra los diferentes archivos en la pantalla del navegador. */
                if (!tipoPeticion.equals("HEAD")) {
                    byte[] buf = new byte[2048];
                    while ((n = sin.read(buf)) >= 0) {
                        outClient.write(buf, 0, n);
                    }
                }

                sin.close();
                outClient.close();
            } catch (IOException e) { System.out.println("ERROR"); }
        } else {
            try {
                statusLine += NEW_LINE;
                responseString = "<html><title>Mini servidor HTTP</title><body>" + "<b>ERROR " +
                        statusCode + ". </b>" + responseString + "</body></html>";
                contentLengthLine = Headers.CONTENT_LENGTH + ": " + responseString.length() + NEW_LINE;

                headers(NEW_LINE, statusLine, contentLengthLine, date, serverdetails, contentTypeLine, accept);
                outClient.writeBytes(responseString);
                outClient.close();
            } catch (IOException e) { System.out.println("ERROR"); }
        }
    }

    /** Metodo que escriba los encabezados. */
    private void headers(String NEW_LINE, String statusLine, String contentLengthLine, String date, String serverdetails, String contentTypeLine, String accept) throws IOException {
        outClient.writeBytes(statusLine);
        outClient.writeBytes(date);
        outClient.writeBytes(serverdetails);
        outClient.writeBytes(contentTypeLine);
        outClient.writeBytes(contentLengthLine);
        if (refiereHeader.equals("")) {
            outClient.writeBytes(Headers.REFIERE + ": localhost" + NEW_LINE);
        } else {
            outClient.writeBytes(Headers.REFIERE + ": " + refiereHeader + NEW_LINE);
        }
        outClient.writeBytes(accept);
        outClient.writeBytes(Headers.CONNECTION + ": close" + NEW_LINE);

        outClient.writeBytes(NEW_LINE);
    }

    /** Agrega la consulta a la bitácora. */
    private void consultaBitacora(String peticionCompleta, String url, String datos) throws IOException {
        /*Crea el objeto de la consulta*/
        ConsultaGenerada nuevaConsulta = new ConsultaGenerada();

        /*Llena los datos de la consulta*/
        nuevaConsulta.setDatos(datos);

        /*Obtiene el timestamp y lo inserta*/
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String tsTiempo = Long.toString(timestamp.getTime());
        nuevaConsulta.setEstampillaTiempo(tsTiempo);

         /*Inserta el url que obtiene desde el HTTPQueryString*/
        nuevaConsulta.setUrl(url);

        StringTokenizer tokensLinea=new StringTokenizer(peticionCompleta, "\n");

        /*--Primera linea--*/
        String strLinea = tokensLinea.nextToken();
       /*Inserta el metodo de la consulta en la bitacora*/
        StringTokenizer tokensEspacio = new StringTokenizer(strLinea, " ");
        String metodo = tokensEspacio.nextToken();
        nuevaConsulta.setMetodo(metodo);

        while (tokensLinea.hasMoreTokens()) {
        /*--Obtiene la siguiente linea--*/
            strLinea = tokensLinea.nextToken();
            tokensEspacio = new StringTokenizer(strLinea, " ");

        /*Se neccesita el segundo token*/
            String tag = tokensEspacio.nextToken(); //Obtiene el Tag

            if (tag.equals("Host:")) { //Si es el Host
                tag = tokensEspacio.nextToken();
               /*Se parcea el host+puerto*/
                StringTokenizer tokenHost = new StringTokenizer(tag, ":");
                String isHost = tokenHost.nextToken();
                //Lo inserta en el objeto
                nuevaConsulta.setServidor(isHost);
            } else {
                if (tag.equals("Referer:")) { //Obtiene el referer
                    tag = tokensEspacio.nextToken();
                    tag = tag.substring(7); //Se elimina el http://
                     /*Se parcea el refiere+puerto*/
                    StringTokenizer tokenRefiere = new StringTokenizer(tag, ":");
                    /*Se debe obtener el segundo, el primero es el http*/
                    String isRefiere = tokenRefiere.nextToken(); //Con '//'
                    /*Lo inserta en el objeto*/
                    nuevaConsulta.setRefiere(isRefiere);
                    refiereHeader = isRefiere;
                }
            }
        }

        /*Se encarga de manipular la nueva consulta*/
        escribirTexto(nuevaConsulta);
    }

    /** Añade la consulta a la bitacora de un txt. */
    private void escribirTexto(ConsultaGenerada nuevaConsulta) throws IOException {
        String fileNewLine = nuevaConsulta.getMetodo() + "\t" +
                nuevaConsulta.getEstampillaTiempo() + "\t" + nuevaConsulta.getServidor() + "\t" +
                nuevaConsulta.getRefiere() + "\t"+ nuevaConsulta.getUrl() + "\t" + nuevaConsulta.getDatos();
        bitacora.add(nuevaConsulta);

        BufferedWriter writer = new BufferedWriter(new FileWriter("bitacora.txt",true));
        writer.append(fileNewLine).append("\n");
        writer.close();
    }

    /** Clase que maneja los encabezados. */
    private static class Headers {
        static final String SERVER = "Server";
        static final String CONNECTION = "Connection";
        static final String CONTENT_LENGTH = "Content-Length";
        static final String CONTENT_TYPE = "Content-Type";
        static final String DATE = "Date";
        static final String ACCEPT = "Accept";
        static final String REFIERE = "Refiere";
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