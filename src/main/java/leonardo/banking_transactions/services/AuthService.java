package leonardo.banking_transactions.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import leonardo.banking_transactions.dtos.signin.SigninRequest;
import leonardo.banking_transactions.dtos.signin.SigninResponse;
import leonardo.banking_transactions.entities.UsersEntity;
import leonardo.banking_transactions.exceptions.validations.InvalidCredentialsException;
import leonardo.banking_transactions.repositories.UserTokenVersionsRepository;
import leonardo.banking_transactions.repositories.UsersRepository;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;
    private final JwtService jwtService;
    private final UserTokenVersionsRepository userTokenVersionsRepository;

    public AuthService(
            UsersRepository usersRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserTokenVersionsRepository userTokenVersionsRepository) {
        this.usersRepository = usersRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userTokenVersionsRepository = userTokenVersionsRepository;

    }

    @Transactional(readOnly = true)
    public SigninResponse signin(SigninRequest request) {
        UsersEntity user = usersRepository
                .findByCpfAndDeletedAtIsNull(request.cpf())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new InvalidCredentialsException();
        int tokenVersion = userTokenVersionsRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Token version is missing for user"))
                .getTokenVersion();
        return new SigninResponse(jwtService.generateToken(user, tokenVersion));
    }
}
