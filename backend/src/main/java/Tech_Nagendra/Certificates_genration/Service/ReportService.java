package Tech_Nagendra.Certificates_genration.Service;

import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    // Save single report
    public Report saveReport(Report report) {
        return reportRepository.save(report);
    }

    // Save list of reports
    public void saveAll(List<Report> list) {
        reportRepository.saveAll(list);
    }

    // Count certificates by user
    public Long countCertificatesByUser(Long userId) {
        Long cnt = reportRepository.countByGeneratedBy(userId);
        return cnt == null ? 0L : cnt;
    }

    // Count certificates generated in the current month
    public Long countCertificatesThisMonth() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate now = LocalDate.now(zone);
        ZonedDateTime startZ = now.withDayOfMonth(1).atStartOfDay(zone);
        ZonedDateTime endZ = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone);
        Date start = Date.from(startZ.toInstant());
        Date end = Date.from(endZ.toInstant());
        Long cnt = reportRepository.countByGeneratedOnBetween(start, end);
        return cnt == null ? 0L : cnt;
    }

    // Get all reports
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    // Get report by ID
    public Report getReportById(Long id) {
        return reportRepository.findById(id).orElse(null);
    }

    // Get reports by user ID
    public List<Report> getReportsByUser(Long userId) {
        return reportRepository.findByGeneratedBy(userId);
    }

    // Filter reports by status and search term
    public List<Report> getReportsByFilter(String status, String searchTerm) {
        List<Report> allReports = reportRepository.findAll();
        return allReports.stream()
                .filter(r -> {
                    boolean matchesStatus = status == null || status.equalsIgnoreCase("all") ||
                            (r.getGrade() != null && r.getGrade().equalsIgnoreCase(status)) ||
                            (r.getTemplateName() != null && r.getTemplateName().equalsIgnoreCase(status));
                    boolean matchesSearch = searchTerm == null || searchTerm.isEmpty() ||
                            (r.getTemplateName() != null && r.getTemplateName().toLowerCase().contains(searchTerm.toLowerCase())) ||
                            (r.getCourseName() != null && r.getCourseName().toLowerCase().contains(searchTerm.toLowerCase())) ||
                            (r.getBatchId() != null && r.getBatchId().toLowerCase().contains(searchTerm.toLowerCase()));
                    return matchesStatus && matchesSearch;
                })
                .collect(Collectors.toList());
    }

    // Save or update report by SID (handles multiple existing records)
    @Transactional
    public Report saveOrUpdateBySid(Report incomingReport) {
        if (incomingReport == null || incomingReport.getSid() == null) {
            throw new IllegalArgumentException("Report or SID cannot be null");
        }

        List<Report> existingList = reportRepository.findBySid(incomingReport.getSid());

        if (existingList != null && !existingList.isEmpty()) {
            // Update first record found
            Report existing = existingList.get(0);
            existing.setGeneratedOn(incomingReport.getGeneratedOn());
            existing.setGeneratedBy(incomingReport.getGeneratedBy());
            existing.setCourseName(incomingReport.getCourseName());
            existing.setBatchId(incomingReport.getBatchId());
            existing.setGrade(incomingReport.getGrade());
            existing.setTemplateName(incomingReport.getTemplateName());
            existing.setJobrole(incomingReport.getJobrole());
            existing.setLevel(incomingReport.getLevel());
            existing.setTemplateID(incomingReport.getTemplateID());
            existing.setTrainingPartner(incomingReport.getTrainingPartner());
            return reportRepository.save(existing);
        } else {
            // Save new report if SID does not exist
            return reportRepository.save(incomingReport);
        }
    }

    // Save or update a list of reports by SID
    @Transactional
    public void saveOrUpdateAllBySid(List<Report> reports) {
        if (reports == null || reports.isEmpty()) return;
        for (Report r : reports) {
            saveOrUpdateBySid(r);
        }
    }
}
