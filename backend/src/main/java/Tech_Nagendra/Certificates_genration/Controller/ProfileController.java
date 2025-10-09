package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Dto.ProfileDto;
import Tech_Nagendra.Certificates_genration.Dto.ProfileStatsDto;
import Tech_Nagendra.Certificates_genration.Dto.UpdatePasswordDto;
import Tech_Nagendra.Certificates_genration.Service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;


    private String extractToken(String header) {
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : header;
    }

    // Get Profile
    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(@RequestHeader("Authorization") String token) {
        String jwt = extractToken(token);
        ProfileDto profile = profileService.getProfile(jwt);
        return ResponseEntity.ok(profile);
    }


    @PutMapping
    public ResponseEntity<ProfileDto> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody ProfileDto profileDto) {
        String jwt = extractToken(token);
        ProfileDto updated = profileService.updateProfile(jwt, profileDto);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdatePasswordDto passwordDto) {
        String jwt = extractToken(token);
        boolean success = profileService.updatePassword(jwt, passwordDto);
        return success
                ? ResponseEntity.ok("Password updated successfully")
                : ResponseEntity.badRequest().body("Current password is incorrect");
    }

    @GetMapping("/stats")
    public ResponseEntity<ProfileStatsDto> getStats(@RequestHeader("Authorization") String token) {
        String jwt = extractToken(token);
        ProfileStatsDto stats = profileService.getStats(jwt);
        return ResponseEntity.ok(stats);
    }
}
