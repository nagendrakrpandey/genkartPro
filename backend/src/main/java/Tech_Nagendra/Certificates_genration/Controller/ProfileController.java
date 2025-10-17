package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Dto.ProfileDto;
import Tech_Nagendra.Certificates_genration.Dto.ProfileStatsDto;
import Tech_Nagendra.Certificates_genration.Dto.UpdatePasswordDto;
import Tech_Nagendra.Certificates_genration.Service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class ProfileController {

    private final ProfileService profileService;

    private String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must be provided in the format 'Bearer <token>'");
        }
        return header.substring(7).trim();
    }

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String tokenHeader) {
        try {
            String token = extractToken(tokenHeader);
            ProfileDto profile = profileService.getProfile(token);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch profile: " + e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestHeader(value = "Authorization", required = false) String tokenHeader,
                                           @RequestBody ProfileDto profileDto) {
        try {
            String token = extractToken(tokenHeader);
            ProfileDto updated = profileService.updateProfile(token, profileDto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update profile: " + e.getMessage());
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestHeader(value = "Authorization", required = false) String tokenHeader,
                                            @RequestBody UpdatePasswordDto passwordDto) {
        try {
            String token = extractToken(tokenHeader);
            boolean success = profileService.updatePassword(token, passwordDto);
            if (success) {
                return ResponseEntity.ok("Password updated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Current password is incorrect");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update password: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader(value = "Authorization", required = false) String tokenHeader) {
        try {
            String token = extractToken(tokenHeader);
            ProfileStatsDto stats = profileService.getStats(token);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch stats: " + e.getMessage());
        }
    }
}
