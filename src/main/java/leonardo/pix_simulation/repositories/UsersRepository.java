package leonardo.pix_simulation.repositories;

import leonardo.pix_simulation.entities.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<UsersEntity, UUID> {
    
    List<UsersEntity> findByDeletedAtIsNull();

    Optional<UsersEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpfAndIdNot(String cpf, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
