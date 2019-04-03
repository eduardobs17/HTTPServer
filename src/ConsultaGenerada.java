
public class ConsultaGenerada {

    private String metodo;
    private String estampillaTiempo;
    private String servidor;
    private String refiere;
    private String url;
    private String datos;

    public  ConsultaGenerada ()
    {
        refiere= "[vacio]";
        datos= "[vacio]";
    }

    public String getMetodo() {
        return metodo;
    }

    public String getEstampillaTiempo() {
        return estampillaTiempo;
    }

    public String getServidor() {
        return servidor;
    }

    public String getRefiere() {
        return refiere;
    }

    public String getUrl() {
        return url;
    }

    public String getDatos() {
        return datos;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public void setEstampillaTiempo(String estampillaTiempo) {
        this.estampillaTiempo = estampillaTiempo;
    }

    public void setServidor(String servidor) {
        this.servidor = servidor;
    }

    public void setRefiere(String refiere) {
        this.refiere = refiere;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDatos(String datos) {
        this.datos = datos;
    }
}
