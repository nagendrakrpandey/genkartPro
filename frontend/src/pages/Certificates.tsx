"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/hooks/use-toast";
import { FileSpreadsheet, Image, Clock, CheckCircle, FileImage, Stamp, Plus, X } from "lucide-react";
import api from "@/Services/api";

interface TemplateType {
  id: number;
  templateName: string;
  imageType: number;
}

interface TemplateFiles {
  excel: File | null;
  zip: File | null;
  logo: File | null;
  sign: File | null;
}

interface UploadTemplateFiles {
  jrxml: File | null;
  images: File[];
}

function UploadCard({
  title,
  icon,
  accept,
  onChange,
  file,
  multiple = false,
  preview = false,
}: {
  title: string;
  icon: React.ReactNode;
  accept: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  file: File | File[] | null;
  multiple?: boolean;
  preview?: boolean;
}) {
  return (
    <div className="flex flex-col items-center bg-white rounded-xl shadow-md p-4 border border-gray-200 hover:shadow-xl transition-all duration-300">
      <div className="text-indigo-600 mb-2">{icon}</div>
      <label className="text-sm font-semibold mb-1">{title}</label>
      <Input type="file" accept={accept} onChange={onChange} multiple={multiple} className="mb-2 w-full max-w-xs" />
      {file && (
        <div className="flex flex-col items-center mt-2 w-full gap-2">
          {Array.isArray(file)
            ? file.map((f, idx) => (
                <div key={idx} className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg w-full justify-between">
                  <span className="truncate font-medium">{f.name}</span>
                  <Badge variant="secondary">Ready</Badge>
                  {preview && f.type.startsWith("image/") && (
                    <img src={URL.createObjectURL(f)} alt="preview" className="h-20 object-contain mt-2 rounded-md border" />
                  )}
                </div>
              ))
            : (
                <div className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg w-full justify-between">
                  <span className="truncate font-medium">{file.name}</span>
                  <Badge variant="secondary">Ready</Badge>
                  {preview && file.type.startsWith("image/") && (
                    <img src={URL.createObjectURL(file)} alt="preview" className="h-20 object-contain mt-2 rounded-md border" />
                  )}
                </div>
              )}
        </div>
      )}
    </div>
  );
}

export default function CertificatePage() {
  const [templates, setTemplates] = useState<TemplateType[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<TemplateType | null>(null);
  const [files, setFiles] = useState<TemplateFiles>({ excel: null, zip: null, logo: null, sign: null });
  const [isUploading, setIsUploading] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadFiles, setUploadFiles] = useState<UploadTemplateFiles>({ jrxml: null, images: [] });
  const { toast } = useToast();

  useEffect(() => {
    api.get("/templates")
      .then((res) => setTemplates(res.data))
      .catch((err) => toast({ title: "Error", description: err.response?.data || err.message, variant: "destructive" }));
  }, []);

  const requiredFields = selectedTemplate ? (() => {
    const type = selectedTemplate.imageType;
    const fields: (keyof TemplateFiles)[] = [];
    if (type >= 0) fields.push("excel");
    if (type >= 1) fields.push("zip");
    if (type >= 2) fields.push("logo");
    if (type >= 3) fields.push("sign");
    return fields;
  })() : [];

  const handleSelectTemplateByName = async (name: string) => {
    try {
      const res = await api.get(`/templates/name/${name}`);
      setSelectedTemplate(res.data);
      setFiles({ excel: null, zip: null, logo: null, sign: null });
    } catch (err: any) {
      toast({ title: "Error", description: err.response?.data || err.message, variant: "destructive" });
    }
  };

  const handleFileUpload = (type: keyof TemplateFiles, e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    if (file) setFiles(prev => ({ ...prev, [type]: file }));
  };

  const handleGenerate = async () => {
    if (!selectedTemplate) return;
    const missing = requiredFields.filter(f => !files[f]);
    if (missing.length > 0) {
      toast({ title: "Missing files", description: `Please upload: ${missing.join(", ")}`, variant: "destructive" });
      return;
    }

    const formData = new FormData();
    requiredFields.forEach(f => files[f] && formData.append(f, files[f]!));

    setIsUploading(true);
    try {
      const res = await api.post(`/generateCertificate/${selectedTemplate.id}/1/download`, formData, { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: "application/zip" }));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "Certificates.zip");
      document.body.appendChild(link);
      link.click();
      link.remove();
      setFiles({ excel: null, zip: null, logo: null, sign: null });
      setSelectedTemplate(null);
      toast({ title: "Success ✅", description: "Certificates downloaded successfully." });
    } catch (err: any) {
      toast({ title: "Error ⚠️", description: err.response?.data || err.message, variant: "destructive" });
    } finally {
      setIsUploading(false);
    }
  };

  const handleUploadTemplate = async () => {
    if (!uploadFiles.jrxml) return toast({ title: "JRXML required", variant: "destructive" });
    if (uploadFiles.images.length === 0) return toast({ title: "At least 1 image required", variant: "destructive" });

    const formData = new FormData();
    formData.append("jrxml", uploadFiles.jrxml);
    uploadFiles.images.forEach(img => formData.append("images", img));

    try {
      const res = await api.post("/templates/upload", formData);
      toast({ title: "Template uploaded ✅", description: res.data.templateName });
      setShowUploadModal(false);
      setUploadFiles({ jrxml: null, images: [] });
      const updated = await api.get("/templates");
      setTemplates(updated.data);
    } catch (err: any) {
      toast({ title: "Upload failed ⚠️", description: err.response?.data || err.message, variant: "destructive" });
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50 py-10 px-4 flex flex-col items-center gap-6">

      {/* Certificate Management Card */}
      <Card className="w-full max-w-5xl shadow-2xl rounded-3xl p-6 bg-white border border-gray-200">
        <CardHeader className="text-center mb-6">
          <CardTitle className="text-3xl md:text-4xl font-extrabold text-indigo-700">Certificate Management</CardTitle>
          <p className="text-gray-500 mt-2 text-lg">Manage your certificates easily with Nagendra Kumar Pandey</p>
        </CardHeader>
        <CardContent>
          <select className="w-full border p-3 rounded mb-4" value={selectedTemplate?.templateName ?? ""} onChange={(e) => handleSelectTemplateByName(e.target.value)}>
            <option value="">-- Select Template --</option>
            {templates.map(t => <option key={t.id} value={t.templateName}>{t.templateName}</option>)}
          </select>

          {selectedTemplate && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
              {requiredFields.includes("excel") && <UploadCard title="Upload Excel" icon={<FileSpreadsheet />} accept=".xlsx,.xls" onChange={(e) => handleFileUpload("excel", e)} file={files.excel} />}
              {requiredFields.includes("zip") && <UploadCard title="Upload ZIP" icon={<Image />} accept=".zip" onChange={(e) => handleFileUpload("zip", e)} file={files.zip} />}
              {requiredFields.includes("logo") && <UploadCard title="Upload Logo" icon={<FileImage />} accept="image/*" onChange={(e) => handleFileUpload("logo", e)} file={files.logo} preview />}
              {requiredFields.includes("sign") && <UploadCard title="Upload Signature" icon={<Stamp />} accept="image/*" onChange={(e) => handleFileUpload("sign", e)} file={files.sign} preview />}
            </div>
          )}

          {selectedTemplate && (
            <div className="mt-6 text-center">
              <Button onClick={handleGenerate} disabled={isUploading || requiredFields.some(f => !files[f])}
                className="bg-green-600 hover:bg-green-700 text-white font-bold px-6 py-3 rounded-full transition">
                {isUploading ? <><Clock className="animate-spin mr-2 inline-block" /> Uploading...</> : <><CheckCircle className="mr-2 inline-block" /> Generate Certificates</>}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
