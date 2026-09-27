package proyecto_aula.PA.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity 
public class Registro {
    @Id 
    @GeneratedValue(strategy= GenerationType.IDENTITY)

    private Long id;

    @ManyToOne 
    private Vehiculo vehiculo;
    @ManyToOne 
    private Espacio espacio;

    private LocalDateTime horaEntrada;
    private LocalDateTime horaSalida;
    private Double valorPagado;


    public Registro(){}
    
    public Registro(Vehiculo vehiculo, Espacio espacio, LocalDateTime horaEntrada){
        this.vehiculo = vehiculo;
        this.espacio = espacio;
        this.horaEntrada = horaEntrada;
         }

         public Long getId(){
            return id;
         }
         public Vehiculo getVehiculo(){
            return vehiculo;
         }
         public void setVehiculo(Vehiculo vehiculo){
            this.vehiculo = vehiculo;
         }
         public Espacio getEspacio(){
            return espacio;
         }
         public void setEspacio(Espacio espacio){
            this.espacio = espacio;
         }
         public LocalDateTime getHoraEntrada(){
            return horaEntrada;
         }
         public void setHoraEntrada(LocalDateTime horaEntrada){
            this.horaEntrada= horaEntrada;
         }
         public LocalDateTime getHoraSalida(){
            return horaSalida;
         }
         public void setHoraSalida(LocalDateTime horaSalida){
            this.horaSalida=horaSalida;
         }
         public Double getValorPagado(){
            return valorPagado;
         }
         public void setValorPagado(Double valorPagado){
            this.valorPagado=valorPagado;
         }
}
