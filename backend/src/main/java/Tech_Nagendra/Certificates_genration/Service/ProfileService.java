package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Dto.ProfileDto;
import Tech_Nagendra.Certificates_genration.Dto.ProfileStatsDto;
import Tech_Nagendra.Certificates_genration.Dto.UpdatePasswordDto;
import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Entity.UserProfile;
import Tech_Nagendra.Certificates_genration.Repository.ProfileRepository;
import Tech_Nagendra.Certificates_genration.Repository.ReportRepository;
import Tech_Nagendra.Certificates_genration.Utility.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ReportRepository reportRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserProfile findById(Long userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public ProfileDto getProfile(String token) {
        Long currentUserId = extractAndValidateToken(token);
        UserProfile user = findById(currentUserId);
        return mapToDto(user);
    }

    public ProfileDto updateProfile(String token, ProfileDto profileDto) {
        Long currentUserId = extractAndValidateToken(token);
        UserProfile currentUser = findById(currentUserId);

        Long targetUserId = profileDto.getId() != null ? profileDto.getId() : currentUserId;
        UserProfile targetUser = findById(targetUserId);

        if (profileDto.getName() != null) targetUser.setName(profileDto.getName());
        if (profileDto.getUsername() != null) targetUser.setUsername(profileDto.getUsername());
        if (profileDto.getEmail() != null) targetUser.setEmail(profileDto.getEmail());

        targetUser.setModifiedBy(currentUserId);

        UserProfile updated = profileRepository.save(targetUser);
        return mapToDto(updated);
    }

    public boolean updatePassword(String token, UpdatePasswordDto dto) {
        Long currentUserId = extractAndValidateToken(token);
        UserProfile user = findById(currentUserId);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        profileRepository.save(user);
        return true;
    }

    public ProfileStatsDto getStats(String token) {
        Long currentUserId = extractAndValidateToken(token);
        UserProfile user = findById(currentUserId);

        long totalCertificates = reportRepository.countByGeneratedBy_Id(currentUserId);
        long activeCertificates = reportRepository.countByGeneratedBy_IdAndStatus(currentUserId, "ACTIVE");
        Optional<Report> lastReport = reportRepository.findTopByGeneratedBy_IdOrderByGeneratedOnDesc(currentUserId);
        String lastGenerated = lastReport.map(r -> r.getGeneratedOn().toString()).orElse("N/A");

        return new ProfileStatsDto((int) totalCertificates, (int) activeCertificates, lastGenerated);
    }

    public void saveLoginToken(Long userId, String token) {
        UserProfile user = findById(userId);
        user.setLoginToken(token);
        profileRepository.save(user);
    }

    private Long extractAndValidateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization token required");
        }

        try {
            Long userId = jwtUtil.extractUserId(token);
            UserProfile user = findById(userId);
            if (user.getLoginToken() == null || !user.getLoginToken().equals(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
            }
            return userId;
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
        } catch (MalformedJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token format");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token validation failed: " + e.getMessage());
        }
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
