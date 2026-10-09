package leonardo.banking_transactions.services;

import leonardo.banking_transactions.entities.UsersEntity;
import leonardo.banking_transactions.entities.UserTokenVersionsEntity;
import leonardo.banking_transactions.exceptions.users.UserDataConflictExistingRecord;
import leonardo.banking_transactions.exceptions.users.UserNotAllowedException;
import leonardo.banking_transactions.exceptions.users.UserNotFoundException;
import leonardo.banking_transactions.exceptions.validation.InvalidCredentialsException;
import leonardo.banking_transactions.dtos.login.LoginRequest;
import leonardo.banking_transactions.dtos.login.LoginResponse;
import leonardo.banking_transactions.dtos.users.UserCreateRequest;
import leonardo.banking_transactions.dtos.users.UserUpdateRequest;
import leonardo.banking_transactions.repositories.UsersRepository;
import leonardo.banking_transactions.repositories.UserTokenVersionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UsersService {
    private final UsersRepository usersRepository;
    private final UserTokenVersionsRepository tokenVersionsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsersService(
            UsersRepository usersRepository,
            UserTokenVersionsRepository tokenVersionsRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usersRepository = usersRepository;
        this.tokenVersionsRepository = tokenVersionsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsersEntity create(UserCreateRequest request) {
        UsersEntity user = new UsersEntity();
        user.setName(request.name());
        user.setCpf(request.cpf());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        validateUniqueFields(user, null);
        UsersEntity savedUser = usersRepository.save(user);
        tokenVersionsRepository.save(new UserTokenVersionsEntity(savedUser.getId(), 0));
        return savedUser;
    }

    @Transactional(readOnly = true)
    public List<UsersEntity> findAllActive() {
        return usersRepository.findByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public UsersEntity findById(UUID id) {
        return usersRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UsersEntity update(UUID id, UsersEntity user) {
        UsersEntity currentUser = findById(id);
        validateUniqueFields(user, id);
        currentUser.setName(user.getName());
        currentUser.setCpf(user.getCpf());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhone(user.getPhone());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            currentUser.setPassword(passwordEncoder.encode(user.getPassword()));
            incrementTokenVersion(id);
        }
        return usersRepository.save(currentUser);
    }

    @Transactional
    public UsersEntity update(UUID id, UserUpdateRequest request) {
        UsersEntity user = new UsersEntity();
        user.setName(request.name());
        user.setCpf(request.cpf());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(request.password());
        return update(id, user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UsersEntity user = usersRepository
                .findByCpfAndDeletedAtIsNull(request.cpf())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new InvalidCredentialsException();
        int tokenVersion = tokenVersionsRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Token version is missing for user"))
                .getTokenVersion();
        return new LoginResponse(jwtService.generateToken(user, tokenVersion));
    }

    @Transactional
    public void delete(UUID authenticatedUserId, UUID id) {
        if (!id.equals(authenticatedUserId))
            throw new UserNotAllowedException();
        UsersEntity user = findById(id);
        user.setDeletedAt(OffsetDateTime.now());
        incrementTokenVersion(id);
        usersRepository.save(user);
    }

    private void incrementTokenVersion(UUID userId) {
        var tokenVersion = tokenVersionsRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Token version is missing for user"));
        tokenVersion.setTokenVersion(tokenVersion.getTokenVersion() + 1);
        tokenVersionsRepository.save(tokenVersion);
    }

    private void validateUniqueFields(UsersEntity user, UUID currentUserId) {
        boolean cpfAlreadyExists = currentUserId == null
                ? usersRepository.existsByCpf(user.getCpf())
                : usersRepository.existsByCpfAndIdNot(user.getCpf(), currentUserId);
        boolean emailAlreadyExists = currentUserId == null
                ? usersRepository.existsByEmail(user.getEmail())
                : usersRepository.existsByEmailAndIdNot(user.getEmail(), currentUserId);
        if (cpfAlreadyExists)
            throw new UserDataConflictExistingRecord();
        if (emailAlreadyExists)
            throw new UserDataConflictExistingRecord();
    }
}
