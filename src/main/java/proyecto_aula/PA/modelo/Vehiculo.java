package proyecto_aula.PA.modelo;


import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Vehiculo {
    
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String placa;
    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipo;

    public Vehiculo(){}
    public Vehiculo(String placa, TipoVehiculo tipo){
        this.placa = placa;
        this.tipo = tipo;
    }
    public Long getId(){
        return id;
    }
    public String getPlaca(){
        return placa;
    }
    public void setPlaca(String placa){
        this.placa = placa;
    }
    public TipoVehiculo getTipo(){
        return tipo;
    }
    public void setTipo(TipoVehiculo tipo){
        this.tipo = tipo;
    }
}
