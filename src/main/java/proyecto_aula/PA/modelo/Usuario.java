package proyecto_aula.PA.modelo;
import jakarta.persistence.*;
@Entity
public class Usuario {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String documento;
    private String password;
    private String username;

    public Usuario(){}


    public Long getId(){
        return id;
    }

    public String getNombre(){
        return nombre;
    }
    public String getDocumento(){
        return documento;
    }
    public String getPassword(){
        return password;
    }
    public String getUsername(){
        return username;
    }
    public void setNombre(String nombre){
        this.nombre = nombre;
    }
    public void setId(Long id){
        this.id = id;
    }
    public void setDocumento(String documento){
        this.documento = documento;
    }
    public void setPassword (String password){
        this.password = password;
    }
    public void setUsername(String username){
        this.username = username;
    }
}
