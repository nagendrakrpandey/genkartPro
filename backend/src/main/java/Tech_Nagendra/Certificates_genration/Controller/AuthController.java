package Tech_Nagendra.Certificates_genration.Controller;
import Tech_Nagendra.Certificates_genration.Dto.AuthRequest;
import Tech_Nagendra.Certificates_genration.Dto.AuthResponse;
import Tech_Nagendra.Certificates_genration.Entity.UserProfile;
import Tech_Nagendra.Certificates_genration.Repository.ProfileRepository;
import Tech_Nagendra.Certificates_genration.Utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ProfileRepository profileRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        System.out.println("Login attempt for user: " + request.getUsername() + " with password: " + request.getPassword());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserProfile userProfile = profileRepository.findByUsername(userDetails.getUsername())
                    .orElse(null);
            String token = jwtUtil.generateToken(
                    userDetails.getUsername(),
                    userProfile != null ? userProfile.getEmail() : ""
            );
            System.out.println("Authentication successful. Token: " + token);
            return ResponseEntity.ok(new AuthResponse(
                    true,
                    200,
                    "Login successful",
                    userDetails.getUsername(),
                    userProfile != null ? userProfile.getEmail() : null,
                    token
            ));

        } catch (BadCredentialsException e) {
            System.out.println("Invalid credentials for user: " + request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, 401, "Invalid username or password", null, null, null));
        } catch (Exception e) {
            System.out.println("Error during login: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, 500, "Internal server error", null, null, null));
        }
    }
}
