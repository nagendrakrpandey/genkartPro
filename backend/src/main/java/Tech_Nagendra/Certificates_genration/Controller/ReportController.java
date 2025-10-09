package Tech_Nagendra.Certificates_genration.Controller;

import Tech_Nagendra.Certificates_genration.Entity.Report;
import Tech_Nagendra.Certificates_genration.Service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/all")
    public ResponseEntity<List<Report>> getAllReports() {
        List<Report> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReportById(@PathVariable Long id) {
        Report report = reportService.getReportById(id);
        if (report != null) {
            return ResponseEntity.ok(report);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Report>> getReportsByUser(@PathVariable Long userId) {
        List<Report> reports = reportService.getReportsByUser(userId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Report>> getReportsByFilter(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchTerm) {
        List<Report> reports = reportService.getReportsByFilter(status, searchTerm);
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/save")
    public ResponseEntity<Report> saveReport(@RequestBody Report report) {
        Report savedReport = reportService.saveOrUpdateBySid(report);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedReport);
    }

    @PostMapping("/saveAll")
    public ResponseEntity<Void> saveAllReports(@RequestBody List<Report> reports) {
        reportService.saveOrUpdateAllBySid(reports);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/count/user/{userId}")
    public ResponseEntity<Long> countCertificatesByUser(@PathVariable Long userId) {
        Long count = reportService.countCertificatesByUser(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/month")
    public ResponseEntity<Long> countCertificatesThisMonth() {
        Long count = reportService.countCertificatesThisMonth();
        return ResponseEntity.ok(count);
    }
}
