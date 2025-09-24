"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/hooks/use-toast";
import { FileSpreadsheet, Image, Clock, CheckCircle, FileImage, Stamp, Plus } from "lucide-react";
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
    <div className="flex flex-col items-center bg-white rounded-xl shadow-md p-4 border border-gray-200 hover:shadow-lg transition">
      <div className="text-indigo-600 mb-2">{icon}</div>
      <label className="text-sm font-medium mb-1">{title}</label>
      <Input type="file" accept={accept} onChange={onChange} multiple={multiple} className="mb-2 w-full max-w-xs" />
      {file && (
        <div className="flex flex-col items-center mt-2 w-full gap-2">
          {Array.isArray(file) ? (
            file.map((f, idx) => (
              <div key={idx} className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg w-full justify-between">
                <span className="truncate">{f.name}</span>
                <Badge variant="secondary">Ready</Badge>
                {preview && f.type.startsWith("image/") && (
                  <img src={URL.createObjectURL(f)} alt="preview" className="h-20 object-contain mt-2 rounded-md border" />
                )}
              </div>
            ))
          ) : (
            <div className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg w-full justify-between">
              <span className="truncate">{file.name}</span>
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
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
  const [files, setFiles] = useState<TemplateFiles>({ excel: null, zip: null, logo: null, sign: null });
  const [isUploading, setIsUploading] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadFiles, setUploadFiles] = useState<UploadTemplateFiles>({ jrxml: null, images: [] });
  const { toast } = useToast();

  // Fetch templates
  useEffect(() => {
    api.get("/templates")
      .then((res) => setTemplates(res.data))
      .catch((err) => toast({ title: "Error", description: err.response?.data || err.message, variant: "destructive" }));
  }, []);

  const selectedTemplate = templates.find((t) => t.id === selectedTemplateId);

  const requiredFields = (() => {
    if (!selectedTemplate) return [];
    const type = selectedTemplate.imageType;
    const fields: (keyof TemplateFiles)[] = [];
    if (type >= 0) fields.push("excel");
    if (type >= 1) fields.push("zip");
    if (type >= 2) fields.push("logo");
    if (type >= 3) fields.push("sign");
    return fields;
  })();

  const handleFileUpload = (type: keyof TemplateFiles, e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    if (file) {
      setFiles((prev) => ({ ...prev, [type]: file }));
      toast({ title: `${type.toUpperCase()} uploaded ✅`, description: file.name });
    }
  };

  const handleGenerate = async () => {
    if (!selectedTemplate) return;
    const missing = requiredFields.filter((f) => !files[f]);
    if (missing.length > 0) {
      toast({ title: "Missing files", description: `Please upload: ${missing.join(", ")}`, variant: "destructive" });
      return;
    }
    const formData = new FormData();
    requiredFields.forEach((f) => {
      const file = files[f];
      if (file) formData.append(f, file);
    });

    setIsUploading(true);
    try {
      const res = await api.post(`/generateCertificate/${selectedTemplate.id}/1/download`, formData, {
        responseType: "blob",
        headers: { "Content-Type": "multipart/form-data" },
      });

      const url = window.URL.createObjectURL(new Blob([res.data], { type: "application/zip" }));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "Certificates.zip");
      document.body.appendChild(link);
      link.click();
      link.remove();

      toast({ title: "Success ✅", description: "Certificates downloaded successfully." });
      setFiles({ excel: null, zip: null, logo: null, sign: null });
      setSelectedTemplateId(null);
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
    uploadFiles.images.forEach((img) => formData.append("images", img));

    try {
      const res = await api.post("/templates", formData, { headers: { "Content-Type": "multipart/form-data" } });
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
      {/* Always show Upload Template button */}
      <Button onClick={() => setShowUploadModal(true)} className="flex items-center gap-2 bg-indigo-600 text-white hover:bg-indigo-700 transition">
        <Plus className="h-5 w-5" /> Upload Template
      </Button>

      {showUploadModal && (
        <div className="fixed inset-0 z-50 flex justify-center items-center overflow-auto bg-black/30 p-4">
          <Card className="w-full max-w-2xl p-6 relative">
            <Button onClick={() => setShowUploadModal(false)} className="absolute top-3 right-3 text-red-500">X</Button>
            <CardHeader>
              <CardTitle className="text-xl font-bold">Upload Template</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <UploadCard
                title="JRXML File"
                icon={<FileSpreadsheet className="h-6 w-6 text-green-600" />}
                accept=".jrxml,.xml"
                onChange={(e) => setUploadFiles((prev) => ({ ...prev, jrxml: e.target.files?.[0] || null }))}
                file={uploadFiles.jrxml}
              />
              <UploadCard
                title="Template Images (Max 10)"
                icon={<Image className="h-6 w-6 text-purple-600" />}
                accept="image/*"
                multiple
                onChange={(e) => {
                  const filesArr = Array.from(e.target.files || []).slice(0, 10);
                  setUploadFiles((prev) => ({ ...prev, images: filesArr }));
                }}
                file={uploadFiles.images}
                preview
              />
              <Button onClick={handleUploadTemplate} className="bg-indigo-600 text-white mt-4">
                Upload
              </Button>
            </CardContent>
          </Card>
        </div>
      )}

      <Card className="w-full max-w-5xl shadow-2xl rounded-3xl p-6 bg-white border border-gray-200">
        <CardHeader className="text-center mb-6">
          <CardTitle className="text-3xl md:text-4xl font-extrabold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
            Certificate Management
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex flex-col items-center gap-2">
            <select
              className="w-full md:w-[85%] border rounded-lg p-3 text-sm focus:ring focus:ring-indigo-400"
              value={selectedTemplateId ?? ""}
              onChange={(e) => {
                setSelectedTemplateId(Number(e.target.value));
                setFiles({ excel: null, zip: null, logo: null, sign: null });
              }}
            >
              <option value="">-- Select Certificate Template --</option>
              {templates.map((t) => <option key={t.id} value={t.id}>{t.templateName}</option>)}
            </select>
          </div>

          {selectedTemplate && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
              {requiredFields.includes("excel") && <UploadCard title="Upload Excel" icon={<FileSpreadsheet className="h-6 w-6 text-green-600" />} accept=".xlsx,.xls" onChange={(e) => handleFileUpload("excel", e)} file={files.excel} />}
              {requiredFields.includes("zip") && <UploadCard title="Upload Images (ZIP)" icon={<Image className="h-6 w-6 text-purple-600" />} accept=".zip" onChange={(e) => handleFileUpload("zip", e)} file={files.zip} />}
              {requiredFields.includes("logo") && <UploadCard title="Upload Logo" icon={<FileImage className="h-6 w-6 text-blue-600" />} accept="image/*" onChange={(e) => handleFileUpload("logo", e)} file={files.logo} preview />}
              {requiredFields.includes("sign") && <UploadCard title="Upload Signature" icon={<Stamp className="h-6 w-6 text-amber-600" />} accept="image/*" onChange={(e) => handleFileUpload("sign", e)} file={files.sign} preview />}
            </div>
          )}

          {selectedTemplate && (
            <div className="text-center mt-6">
              <Button
                onClick={handleGenerate}
                disabled={isUploading || requiredFields.some((f) => !files[f])}
                className="px-8 py-3 text-lg rounded-full bg-gradient-to-r from-indigo-600 to-purple-600 hover:shadow-xl text-white disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isUploading ? (
                  <><Clock className="h-5 w-5 mr-2 animate-spin" /> Uploading...</>
                ) : (
                  <><CheckCircle className="h-5 w-5 mr-2" /> Generate Certificates</>
                )}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}







