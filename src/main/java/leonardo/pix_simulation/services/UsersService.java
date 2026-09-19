package leonardo.pix_simulation.services;

import leonardo.pix_simulation.entities.UsersEntity;
import leonardo.pix_simulation.exceptions.EmailOrCpfAlreadyRegisteredException;
import leonardo.pix_simulation.exceptions.UserNotFoundException;
import leonardo.pix_simulation.repositories.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UsersService {
    
    private final UsersRepository usersRepository;

    public UsersService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Transactional
    public UsersEntity create(UsersEntity user) {
        validateUniqueFields(user, null);
        return usersRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UsersEntity> findAllActive() {
        return usersRepository.findByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public UsersEntity findById(UUID id) {
        return usersRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UsersEntity update(UUID id, UsersEntity user) {
        UsersEntity currentUser = findById(id);
        validateUniqueFields(user, id);

        currentUser.setName(user.getName());
        currentUser.setCpf(user.getCpf());
        currentUser.setEmail(user.getEmail());

        return usersRepository.save(currentUser);
    }

    @Transactional
    public void delete(UUID id) {
        UsersEntity user = findById(id);
        user.setDeletedAt(OffsetDateTime.now());
        usersRepository.save(user);
    }

    private void validateUniqueFields(UsersEntity user, UUID currentUserId) {
        boolean cpfAlreadyExists = currentUserId == null
                ? usersRepository.existsByCpf(user.getCpf())
                : usersRepository.existsByCpfAndIdNot(user.getCpf(), currentUserId);

        boolean emailAlreadyExists = currentUserId == null
                ? usersRepository.existsByEmail(user.getEmail())
                : usersRepository.existsByEmailAndIdNot(user.getEmail(), currentUserId);

        if (cpfAlreadyExists) {
            throw new EmailOrCpfAlreadyRegisteredException("CPF", user.getCpf());
        }

        if (emailAlreadyExists) {
            throw new EmailOrCpfAlreadyRegisteredException("EMAIL", user.getEmail());
        }
    }
}
