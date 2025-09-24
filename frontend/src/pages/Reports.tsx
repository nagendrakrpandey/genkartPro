import { useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/hooks/use-toast";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Download,
  FileText,
  Calendar,
  Search,
  Filter,
  CheckCircle,
  Clock,
  AlertCircle,
} from "lucide-react";

const certificateReports = [
  {
    id: 1,
    fileName: "course_completion_batch_1.xlsx",
    templateName: "Achievement Certificate",
    generatedBy: "John Doe",
    generatedCount: 45,
    downloadCount: 42,
    status: "completed",
    createdAt: "2024-01-15 14:30",
    lastDownload: "2024-01-16 09:15",
  },
  {
    id: 2,
    fileName: "workshop_participants.xlsx",
    templateName: "Participation Certificate",
    generatedBy: "Jane Smith",
    generatedCount: 23,
    downloadCount: 20,
    status: "completed",
    createdAt: "2024-01-14 11:20",
    lastDownload: "2024-01-15 16:45",
  },
  {
    id: 3,
    fileName: "excellence_awards_q1.xlsx",
    templateName: "Excellence Certificate",
    generatedBy: "Mike Johnson",
    generatedCount: 15,
    downloadCount: 0,
    status: "processing",
    createdAt: "2024-01-16 10:00",
    lastDownload: "-",
  },
  {
    id: 4,
    fileName: "training_completion.xlsx",
    templateName: "Achievement Certificate",
    generatedBy: "Sarah Wilson",
    generatedCount: 67,
    downloadCount: 65,
    status: "completed",
    createdAt: "2024-01-13 16:15",
    lastDownload: "2024-01-14 08:30",
  },
  {
    id: 5,
    fileName: "seminar_attendance.xlsx",
    templateName: "Participation Certificate",
    generatedBy: "David Brown",
    generatedCount: 0,
    downloadCount: 0,
    status: "failed",
    createdAt: "2024-01-12 13:45",
    lastDownload: "-",
  },
];

export default function Reports() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [selectedReports, setSelectedReports] = useState<number[]>([]);
  const { toast } = useToast();

  const filteredReports = certificateReports.filter((report) => {
    const matchesSearch = 
      report.fileName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      report.templateName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      report.generatedBy.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesStatus = statusFilter === "all" || report.status === statusFilter;
    
    return matchesSearch && matchesStatus;
  });

  const getStatusIcon = (status: string) => {
    switch (status) {
      case "completed":
        return <CheckCircle className="h-4 w-4 text-success" />;
      case "processing":
        return <Clock className="h-4 w-4 text-warning animate-pulse" />;
      case "failed":
        return <AlertCircle className="h-4 w-4 text-destructive" />;
      default:
        return null;
    }
  };

  const getStatusBadge = (status: string) => {
    const variants = {
      completed: "default",
      processing: "secondary",
      failed: "destructive",
    } as const;
    
    return (
      <Badge variant={variants[status as keyof typeof variants] || "secondary"}>
        {status}
      </Badge>
    );
  };

  const handleExportSelected = (format: "excel" | "pdf") => {
    if (selectedReports.length === 0) {
      toast({
        title: "No reports selected",
        description: "Please select at least one report to export",
        variant: "destructive",
      });
      return;
    }

    toast({
      title: `Exporting to ${format.toUpperCase()}`,
      description: `${selectedReports.length} reports will be exported`,
    });
  };

  const handleExportAll = (format: "excel" | "pdf") => {
    toast({
      title: `Exporting all reports to ${format.toUpperCase()}`,
      description: `${filteredReports.length} reports will be exported`,
    });
  };

  const toggleReportSelection = (reportId: number) => {
    setSelectedReports(prev => 
      prev.includes(reportId) 
        ? prev.filter(id => id !== reportId)
        : [...prev, reportId]
    );
  };

  const toggleAllReports = () => {
    setSelectedReports(prev => 
      prev.length === filteredReports.length 
        ? []
        : filteredReports.map(report => report.id)
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Reports</h1>
        <p className="text-muted-foreground">
          View and export detailed reports of all certificate generations
        </p>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Reports</CardTitle>
            <FileText className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{certificateReports.length}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Completed</CardTitle>
            <CheckCircle className="h-4 w-4 text-success" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {certificateReports.filter(r => r.status === "completed").length}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Processing</CardTitle>
            <Clock className="h-4 w-4 text-warning" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {certificateReports.filter(r => r.status === "processing").length}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Downloads</CardTitle>
            <Download className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {certificateReports.reduce((sum, report) => sum + report.downloadCount, 0)}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters and Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Certificate Generation Reports</CardTitle>
          <CardDescription>
            Detailed overview of all certificate generation activities
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div className="flex flex-col gap-2 md:flex-row md:items-center md:space-x-2">
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search reports..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-8 w-full md:w-[300px]"
                />
              </div>
              
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-full md:w-[150px]">
                  <Filter className="h-4 w-4 mr-2" />
                  <SelectValue placeholder="Filter by status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Status</SelectItem>
                  <SelectItem value="completed">Completed</SelectItem>
                  <SelectItem value="processing">Processing</SelectItem>
                  <SelectItem value="failed">Failed</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex flex-col gap-2 md:flex-row">
              <Button
                variant="outline"
                onClick={() => handleExportSelected("excel")}
                disabled={selectedReports.length === 0}
              >
                <Download className="h-4 w-4 mr-2" />
                Export Selected (Excel)
              </Button>
              <Button
                variant="outline"
                onClick={() => handleExportSelected("pdf")}
                disabled={selectedReports.length === 0}
              >
                <Download className="h-4 w-4 mr-2" />
                Export Selected (PDF)
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Reports Table */}
      <Card>
        <CardContent className="p-0">
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[50px]">
                    <input
                      type="checkbox"
                      checked={selectedReports.length === filteredReports.length && filteredReports.length > 0}
                      onChange={toggleAllReports}
                      className="rounded border-gray-300"
                    />
                  </TableHead>
                  <TableHead>File Name</TableHead>
                  <TableHead>Template</TableHead>
                  <TableHead>Generated By</TableHead>
                  <TableHead>Generated</TableHead>
                  <TableHead>Downloaded</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredReports.map((report) => (
                  <TableRow key={report.id} className="hover:bg-muted/50">
                    <TableCell>
                      <input
                        type="checkbox"
                        checked={selectedReports.includes(report.id)}
                        onChange={() => toggleReportSelection(report.id)}
                        className="rounded border-gray-300"
                      />
                    </TableCell>
                    <TableCell className="font-medium">
                      <div className="flex items-center space-x-2">
                        <FileText className="h-4 w-4 text-muted-foreground" />
                        <span>{report.fileName}</span>
                      </div>
                    </TableCell>
                    <TableCell>{report.templateName}</TableCell>
                    <TableCell>{report.generatedBy}</TableCell>
                    <TableCell>{report.generatedCount}</TableCell>
                    <TableCell>{report.downloadCount}</TableCell>
                    <TableCell>
                      <div className="flex items-center space-x-2">
                        {getStatusIcon(report.status)}
                        {getStatusBadge(report.status)}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center space-x-1">
                        <Calendar className="h-3 w-3 text-muted-foreground" />
                        <span className="text-sm">{report.createdAt}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex space-x-1">
                        {report.status === "completed" && (
                          <Button variant="outline" size="sm">
                            <Download className="h-3 w-3" />
                          </Button>
                        )}
                        <Button variant="outline" size="sm">
                          <FileText className="h-3 w-3" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          
          {filteredReports.length === 0 && (
            <div className="text-center py-8">
              <FileText className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
              <p className="text-muted-foreground">No reports found matching your criteria</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Bulk Export Actions */}
      {filteredReports.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Bulk Export</CardTitle>
            <CardDescription>
              Export all filtered reports at once
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex space-x-2">
              <Button onClick={() => handleExportAll("excel")} variant="outline">
                <Download className="h-4 w-4 mr-2" />
                Export All to Excel
              </Button>
              <Button onClick={() => handleExportAll("pdf")} variant="outline">
                <Download className="h-4 w-4 mr-2" />
                Export All to PDF
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}