package proyecto_aula.PA.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity 
public class Espacio {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY) 
    private Long id;
    private int numero;
    private boolean ocupado;

    @ManyToOne 
    private Zona zona;

    public Espacio(){

    }
    //Constructor de la clase con los get y setter de los atributos, y tambien zona con sus atributos
    public Espacio(int numero,boolean ocupado){
        this.numero = numero;
        this.ocupado = ocupado;
    }
    public Zona getZona(){
        return zona;
    }
    public void setZona(Zona zona){
        this.zona = zona;
    }
    public Long getId(){
        return id;
    }
    public int getNumero(){
        return numero;
    }
    public void setNumero(int numero){
        this.numero = numero;
    }
    public boolean isOcupado(){
        return ocupado;
    }
    public void setOcupado(boolean ocupado){
        this.ocupado = ocupado;
    }
}
