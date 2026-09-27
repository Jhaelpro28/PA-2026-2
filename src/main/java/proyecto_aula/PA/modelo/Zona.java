package proyecto_aula.PA.modelo;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


@Entity
public class Zona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String nombre;
    private String tipo;
    private int capacidad;

    public Zona(){
        //Constructor vacio para los get y set de los atributos
    }
    public Zona(String nombre, String tipo, int capacidad){
        this.nombre = nombre;
        this.tipo = tipo;
        this.capacidad = capacidad;
    }
    public Long getId(){
        return id;
    }
    public String getNombre(){
        return nombre;
    }
    public void setNombre(String nombre){
        this.nombre = nombre;
    }
    public String getTipo(){
        return tipo;
    }
    public void setTipo(String tipo){
        this.tipo = tipo;
    }
    public int getCapacidad(){
        return capacidad;
    }
    public void setCapacidad(int capacidad){
        this.capacidad = capacidad;
    }
}
