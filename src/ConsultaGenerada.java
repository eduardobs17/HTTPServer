class ConsultaGenerada {

    private String metodo;
    private String estampillaTiempo;
    private String servidor;
    private String refiere;
    private String url;
    private String datos;

    /** Clase que simula las consultas del server. */
    ConsultaGenerada() {
        refiere= "[vacio]";
        datos= "[vacio]";
    }

    String getMetodo() { return metodo; }

    String getEstampillaTiempo() { return estampillaTiempo; }

    String getServidor() { return servidor; }

    String getRefiere() { return refiere; }

    String getUrl() { return url; }

    String getDatos() { return datos; }

    void setMetodo(String metodo) { this.metodo = metodo; }

    void setEstampillaTiempo(String estampillaTiempo) { this.estampillaTiempo = estampillaTiempo; }

    void setServidor(String servidor) { this.servidor = servidor; }

    void setRefiere(String refiere) { this.refiere = refiere; }

    void setUrl(String url) { this.url = url; }

    void setDatos(String datos) { this.datos = datos; }
}
