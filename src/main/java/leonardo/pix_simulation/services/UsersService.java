package leonardo.pix_simulation.services;

import leonardo.pix_simulation.entities.UsersEntity;
import leonardo.pix_simulation.exceptions.EmailOrCpfAlreadyRegisteredException;
import leonardo.pix_simulation.exceptions.UserNotFoundException;
import leonardo.pix_simulation.exceptions.InvalidCredentialsException;
import leonardo.pix_simulation.dtos.LoginRequest;
import leonardo.pix_simulation.dtos.LoginResponse;
import leonardo.pix_simulation.dtos.UserCreateRequest;
import leonardo.pix_simulation.dtos.UserUpdateRequest;
import leonardo.pix_simulation.repositories.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UsersService {
    
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsersService(UsersRepository usersRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsersEntity create(UserCreateRequest request) {
        UsersEntity user = new UsersEntity();
        user.setName(request.name()); user.setCpf(request.cpf()); user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
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
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            currentUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return usersRepository.save(currentUser);
    }

    @Transactional
    public UsersEntity update(UUID id, UserUpdateRequest request) {
        UsersEntity user = new UsersEntity();
        user.setName(request.name()); user.setCpf(request.cpf()); user.setEmail(request.email());
        user.setPassword(request.password());
        return update(id, user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UsersEntity user = usersRepository.findByCpfAndDeletedAtIsNull(request.cpf())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return new LoginResponse(jwtService.generateToken(user));
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
