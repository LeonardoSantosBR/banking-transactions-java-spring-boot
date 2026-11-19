package leonardo.banking_transactions.service;

import leonardo.banking_transactions.dto.users.UserCreateRequest;
import leonardo.banking_transactions.dto.users.UserUpdateRequest;
import leonardo.banking_transactions.entity.UserTokenVersionsEntity;
import leonardo.banking_transactions.entity.UsersEntity;
import leonardo.banking_transactions.exception.users.UserDataConflictExistingRecord;
import leonardo.banking_transactions.exception.users.UserNotAllowedException;
import leonardo.banking_transactions.exception.users.UserNotFoundException;
import leonardo.banking_transactions.repository.UserTokenVersionsRepository;
import leonardo.banking_transactions.repository.UsersRepository;
import leonardo.banking_transactions.service.aws.S3Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UsersService {
    private final UsersRepository usersRepository;
    private final UserTokenVersionsRepository tokenVersionsRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;

    public UsersService(
            UsersRepository usersRepository,
            UserTokenVersionsRepository userTokenVersionsRepository,
            PasswordEncoder passwordEncoder,
            S3Service s3Service) {
        this.usersRepository = usersRepository;
        this.tokenVersionsRepository = userTokenVersionsRepository;
        this.passwordEncoder = passwordEncoder;
        this.s3Service = s3Service;
    }

    @Transactional
    public UsersEntity create(UserCreateRequest request, MultipartFile avatar) {
        UsersEntity user = new UsersEntity();
        user.setName(request.name());
        user.setCpf(request.cpf());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        if (avatar != null && !avatar.isEmpty()) {
            validateAvatar(avatar);
            String objectKey = "avatars/" + UUID.randomUUID() + "/" + fileExtension(avatar.getOriginalFilename());
            try {
                s3Service.upload(objectKey, avatar.getBytes(), avatar.getContentType());
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read avatar file", exception);
            }
            user.setAvatarUrl(objectKey);
        }
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
    public UsersEntity update(UUID id, UserUpdateRequest request) {
        return update(id, request, null);
    }

    @Transactional
    public UsersEntity update(UUID id, UserUpdateRequest request, MultipartFile avatar) {
        UsersEntity user = new UsersEntity();
        user.setName(request.name());
        user.setCpf(request.cpf());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(request.password());
        UsersEntity currentUser = findById(id);
        String previousAvatarUrl = currentUser.getAvatarUrl();
        updateUserEntity(currentUser, user);
        if (avatar != null && !avatar.isEmpty()) {
            validateAvatar(avatar);
            currentUser.setAvatarUrl(uploadAvatar(avatar));
        }
        UsersEntity savedUser = usersRepository.save(currentUser);
        if (avatar != null && !avatar.isEmpty() && previousAvatarUrl != null) {
            s3Service.delete(previousAvatarUrl);
        }
        return savedUser;
    }

    @Transactional
    private void updateUserEntity(UsersEntity currentUser, UsersEntity user) {
        validateUniqueFields(user, currentUser.getId());
        currentUser.setName(user.getName());
        currentUser.setCpf(user.getCpf());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhone(user.getPhone());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            currentUser.setPassword(passwordEncoder.encode(user.getPassword()));
            incrementTokenVersion(currentUser.getId());
        }
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

    private void validateAvatar(MultipartFile avatar) {
        if (avatar.getContentType() == null || !avatar.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image");
        }
        if (avatar.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Avatar must not exceed 5 MB");
        }
    }

    private String fileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "image";
        }
        return filename.substring(filename.lastIndexOf('.') + 1)
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
    }

    private String uploadAvatar(MultipartFile avatar) {
        String objectKey = "avatars/" + UUID.randomUUID() + "/" + fileExtension(avatar.getOriginalFilename());
        try {
            s3Service.upload(objectKey, avatar.getBytes(), avatar.getContentType());
            return objectKey;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read avatar file", exception);
        }
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
