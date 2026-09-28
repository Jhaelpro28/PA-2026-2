package proyecto_aula.PA.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import proyecto_aula.PA.modelo.Espacio;
import proyecto_aula.PA.modelo.Zona;

import java.util.List;


public interface  EspacioRepository extends JpaRepository<Espacio, Long>{
    List<Espacio> findByZonaAndOcupadoFalse(Zona zona);
}
