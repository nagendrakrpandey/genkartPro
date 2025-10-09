package Tech_Nagendra.Certificates_genration.Repository;

import Tech_Nagendra.Certificates_genration.Entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByGeneratedBy(Long userId);

    long countByGeneratedOnBetween(Date startDate, Date endDate);
    List<Report> findByGeneratedBy(Long userId);
    List<Report> findBySid(String sid);
}