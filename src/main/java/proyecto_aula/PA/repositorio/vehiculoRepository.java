package proyecto_aula.PA.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import proyecto_aula.PA.modelo.Vehiculo;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long>{

    public Vehiculo findByPlaca(String placa);
    
}
