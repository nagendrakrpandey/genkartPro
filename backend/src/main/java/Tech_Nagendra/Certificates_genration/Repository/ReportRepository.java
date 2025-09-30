package Tech_Nagendra.Certificates_genration.Repository;

import Tech_Nagendra.Certificates_genration.Entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByGeneratedBy(Long userId);

    long countByGeneratedOnBetween(Date startDate, Date endDate);
}