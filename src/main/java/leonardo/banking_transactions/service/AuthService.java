package leonardo.banking_transactions.service;

import io.jsonwebtoken.JwtException;
import leonardo.banking_transactions.dto.auth.RefreshTokenRequest;
import leonardo.banking_transactions.dto.auth.SigninRequest;
import leonardo.banking_transactions.dto.auth.SigninResponse;
import leonardo.banking_transactions.entity.UsersEntity;
import leonardo.banking_transactions.exception.validations.InvalidCredentialsException;
import leonardo.banking_transactions.exception.validations.InvalidJwtOrMissingException;
import leonardo.banking_transactions.repository.UserTokenVersionsRepository;
import leonardo.banking_transactions.repository.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;
    private final JwtService jwtService;
    private final UserTokenVersionsRepository userTokenVersionsRepository;

    public AuthService(UsersRepository usersRepository, JwtService jwtService, PasswordEncoder passwordEncoder,
            UserTokenVersionsRepository userTokenVersionsRepository) {
        this.usersRepository = usersRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userTokenVersionsRepository = userTokenVersionsRepository;
    }

    @Transactional(readOnly = true)
    public SigninResponse signin(SigninRequest request) {
        UsersEntity user = usersRepository.findByCpfAndDeletedAtIsNull(request.cpf())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new InvalidCredentialsException();
        int version = userTokenVersionsRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Token version is missing for user")).getTokenVersion();
        return new SigninResponse(jwtService.generateToken(user, version));
    }

    @Transactional(readOnly = true)
    public SigninResponse refresh(RefreshTokenRequest request) {
        try {
            UUID id = jwtService.extractUserId(request.token());
            int version = jwtService.extractTokenVersion(request.token());
            UsersEntity user = usersRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(InvalidJwtOrMissingException::new);
            int current = userTokenVersionsRepository.findById(id)
                    .orElseThrow(InvalidJwtOrMissingException::new).getTokenVersion();
            if (version != current)
                throw new InvalidJwtOrMissingException();
            return new SigninResponse(jwtService.generateToken(user, current));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtOrMissingException();
        }
    }
}