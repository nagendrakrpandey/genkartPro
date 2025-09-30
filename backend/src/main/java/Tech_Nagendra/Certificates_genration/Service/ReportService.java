package Tech_Nagendra.Certificates_genration.Service;


import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Date;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    public Report saveReport(Report report) {
        return reportRepository.save(report);
    }

    public Long countCertificatesByUser(Long userId) {
        Long cnt = reportRepository.countByGeneratedBy(userId);
        return cnt == null ? 0L : cnt;
    }

    public Long countCertificatesThisMonth() {
        // compute start of month (inclusive) and start of next month (exclusive)
        ZoneId zone = ZoneId.systemDefault();
        LocalDate now = LocalDate.now(zone);
        ZonedDateTime startZ = now.withDayOfMonth(1).atStartOfDay(zone);
        ZonedDateTime endZ = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone);

        Date start = Date.from(startZ.toInstant());
        Date end = Date.from(endZ.toInstant());

        Long cnt = reportRepository.countByGeneratedOnBetween(start, end);
        return cnt == null ? 0L : cnt;
    }

    // convenience: bulk save
    public void saveAll(List<Report> list) {
        reportRepository.saveAll(list);
    }
}
