"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/hooks/use-toast";
import { FileSpreadsheet, Clock, CheckCircle, FileImage, Stamp } from "lucide-react";
import api from "@/Services/api";

interface TemplateType {
  id: number;
  templateName: string;
  imageType: number;
}

interface TemplateFiles {
  excel: File | null;
}

function UploadCard({
  title,
  icon,
  accept,
  onChange,
  file,
}: {
  title: string;
  icon: React.ReactNode;
  accept: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  file: File | null;
}) {
  return (
    <div className="flex flex-col items-center bg-white rounded-xl shadow-md p-4 border border-gray-200 hover:shadow-xl transition-all duration-300">
      <div className="text-indigo-600 mb-2">{icon}</div>
      <label className="text-sm font-semibold mb-1">{title}</label>
      <Input type="file" accept={accept} onChange={onChange} className="mb-2 w-full max-w-xs" />
      {file && (
        <div className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg w-full justify-between">
          <span className="truncate font-medium">{file.name}</span>
          <Badge variant="secondary">Ready</Badge>
        </div>
      )}
    </div>
  );
}

export default function CertificatePage() {
  const [templates, setTemplates] = useState<TemplateType[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<TemplateType | null>(null);
  const [files, setFiles] = useState<TemplateFiles>({ excel: null });
  const [isUploading, setIsUploading] = useState(false);
  const { toast } = useToast();

  useEffect(() => {
    api.get("/templates")
      .then((res) => setTemplates(res.data))
      .catch((err) => toast({ title: "Error", description: err.response?.data || err.message, variant: "destructive" }));
  }, []);

  const handleSelectTemplateByName = async (name: string) => {
    try {
      const res = await api.get(`/templates/name/${name}`);
      setSelectedTemplate(res.data);
      setFiles({ excel: null });
    } catch (err: any) {
      toast({ title: "Error", description: err.response?.data || err.message, variant: "destructive" });
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    if (file) setFiles({ excel: file });
  };

  const handleGenerate = async () => {
    if (!selectedTemplate || !files.excel) {
      return toast({ title: "Missing fields", description: "Please select template and upload Excel file", variant: "destructive" });
    }

    const formData = new FormData();
    formData.append("templateId", selectedTemplate.id.toString());
    formData.append("userId", "1"); // replace with actual userId
    formData.append("excelFile", files.excel);

    setIsUploading(true);
    try {
      const res = await api.post("/certificates/generate", formData, { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: "application/zip" }));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "Certificates.zip");
      document.body.appendChild(link);
      link.click();
      link.remove();

      toast({ title: "Success ✅", description: "Certificates downloaded successfully." });
      setFiles({ excel: null });
      setSelectedTemplate(null);
    } catch (err: any) {
      toast({ title: "Error ⚠️", description: err.response?.data || err.message, variant: "destructive" });
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50 py-10 px-4 flex flex-col items-center gap-6">
      <Card className="w-full max-w-3xl shadow-2xl rounded-3xl p-6 bg-white border border-gray-200">
        <CardHeader className="text-center mb-6">
          <CardTitle className="text-3xl md:text-4xl font-extrabold text-indigo-700">Certificate Management</CardTitle>
          <p className="text-gray-500 mt-2 text-lg">Manage your certificates easily</p>
        </CardHeader>
        <CardContent>
          <select className="w-full border p-3 rounded mb-4" value={selectedTemplate?.templateName ?? ""} onChange={(e) => handleSelectTemplateByName(e.target.value)}>
            <option value="">-- Select Template --</option>
            {templates.map(t => <option key={t.id} value={t.templateName}>{t.templateName}</option>)}
          </select>

          {selectedTemplate && (
            <UploadCard
              title="Upload Excel"
              icon={<FileSpreadsheet />}
              accept=".xlsx,.xls"
              onChange={handleFileUpload}
              file={files.excel}
            />
          )}

          {selectedTemplate && (
            <div className="mt-6 text-center">
              <Button onClick={handleGenerate} disabled={isUploading || !files.excel}
                className="bg-green-600 hover:bg-green-700 text-white font-bold px-6 py-3 rounded-full transition">
                {isUploading ? <><Clock className="animate-spin mr-2 inline-block" /> Generating...</> : <><CheckCircle className="mr-2 inline-block" /> Generate Certificates</>}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
