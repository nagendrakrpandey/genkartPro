package Tech_Nagendra.Certificates_genration.Service;
import Tech_Nagendra.Certificates_genration.Dto.ProfileDto;
import Tech_Nagendra.Certificates_genration.Dto.ProfileStatsDto;
import Tech_Nagendra.Certificates_genration.Dto.UpdatePasswordDto;
import Tech_Nagendra.Certificates_genration.Entity.UserProfile;
import Tech_Nagendra.Certificates_genration.Repository.ProfileRepository;
import Tech_Nagendra.Certificates_genration.Utility.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final JwtUtil jwtUtil;
    public ProfileDto getProfile(String token) {
        String username = jwtUtil.extractUsername(token);
        UserProfile user = profileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDto(user);
    }
    public ProfileDto updateProfile(String token, ProfileDto profileDto) {
        // Extract current user (modifier)
        String username = jwtUtil.extractUsername(token);
        UserProfile modifier = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Find target user
        UserProfile user = profileRepository.findById(profileDto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));
        System.out.println("User ID received: " + profileDto.getId());

        // Update fields
        if (profileDto.getName() != null) user.setName(profileDto.getName());
        if (profileDto.getUsername() != null) user.setUsername(profileDto.getUsername());
        if (profileDto.getEmail() != null) user.setEmail(profileDto.getEmail());

        // Save modifier’s ID (numeric)
        user.setModifiedBy(modifier.getId());

        UserProfile updated = profileRepository.save(user);
        return mapToDto(updated);



    }

    public boolean updatePassword(String token, UpdatePasswordDto dto) {
        String username = jwtUtil.extractUsername(token);
        UserProfile user = profileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(dto.getCurrentPassword())) {
            return false;
        }
        user.setPassword(dto.getNewPassword());
        profileRepository.save(user);
        return true;
    }
    public String uploadAvatar(String token, MultipartFile file) {
        String username = jwtUtil.extractUsername(token);
        UserProfile user = profileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        profileRepository.save(user);
        return "Avatar uploaded: " + file.getOriginalFilename();
    }

    public ProfileStatsDto getStats(String token) {
        return new ProfileStatsDto(0, 0);
    }
    private ProfileDto mapToDto(UserProfile user) {
        return new ProfileDto(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getModifiedAt(),
                user.getCreatedBy(),
                user.getModifiedBy()
        );
    }
}