"use client";
import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/hooks/use-toast";
import { FileSpreadsheet, Image, Clock, CheckCircle, FileImage, Stamp } from "lucide-react";
import axios from "axios";

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

function UploadCard({
  title,
  icon,
  accept,
  onChange,
  file,
  preview,
}: {
  title: string;
  icon: React.ReactNode;
  accept: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  file: File | null;
  preview?: boolean;
}) {
  return (
    <div className="flex flex-col items-center bg-white rounded-xl shadow-md p-4 border border-gray-200 hover:shadow-lg transition w-full">
      <div className="text-indigo-600 mb-2">{icon}</div>
      <label className="text-sm font-medium mb-1">{title}</label>
      <Input type="file" accept={accept} onChange={onChange} className="mb-2 w-full max-w-xs" />
      {file && (
        <div className="flex flex-col items-center mt-2 w-full">
          <div className="flex items-center gap-2 bg-gray-100 p-2 rounded-lg w-full justify-between">
            <span className="truncate">{file.name}</span>
            <Badge variant="secondary">Ready</Badge>
          </div>
          {preview && file.type.startsWith("image/") && (
            <img
              src={URL.createObjectURL(file)}
              alt="preview"
              className="h-20 object-contain mt-2 rounded-md border"
            />
          )}
        </div>
      )}
    </div>
  );
}

export default function CertificatePage() {
  const [templates, setTemplates] = useState<TemplateType[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
  const [templateImages, setTemplateImages] = useState<string[]>([]);
  const [files, setFiles] = useState<TemplateFiles>({ excel: null, zip: null, logo: null, sign: null });
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [role, setRole] = useState<string>(""); // admin/user
  const { toast } = useToast();

  const token = typeof window !== "undefined" ? sessionStorage.getItem("authToken") : null;

  // Fetch templates based on role
  useEffect(() => {
    if (!token) {
      toast({
        title: "Unauthorized",
        description: "No JWT token found. Please login again.",
        variant: "destructive",
      });
      return;
    }

    // Decode token locally to extract role (optional, if backend sends role separately you can skip)
    try {
      const payload = JSON.parse(atob(token.split(".")[1]));
      setRole(payload.role || "user");
    } catch (e) {
      console.error("Failed to parse JWT token", e);
    }

    
    axios
      .get("http://localhost:8086/templates", {
        headers: { Authorization: `Bearer ${token}` },
      })
      .then((res) => setTemplates(res.data || []))
      .catch((err) => {
        console.error("Error fetching templates:", err);
        toast({
          title: "Error loading templates",
          description: err.response?.data || err.message,
          variant: "destructive",
        });
      });
  }, [token]);

  // Fetch selected template images
  useEffect(() => {
    if (!selectedTemplateId || !token) {
      setTemplateImages([]);
      return;
    }

    axios
      .get<string[]>(`http://localhost:8086/templates/${selectedTemplateId}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      .then((res) => setTemplateImages(res.data || []))
      .catch((err) => {
        console.error("Error fetching template images:", err);
        toast({
          title: "Error loading images",
          description: err.response?.data || err.message,
          variant: "destructive",
        });
      });
  }, [selectedTemplateId, token]);

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
      toast({ title: `${type.toUpperCase()} uploaded`, description: file.name });
    }
  };

  const handleGenerate = async () => {
    if (!selectedTemplate || !token) return;

    const missing = requiredFields.filter((f) => !files[f]);
    if (missing.length > 0) {
      toast({
        title: "Missing files",
        description: `Please upload: ${missing.join(", ")}`,
        variant: "destructive",
      });
      return;
    }

    const formData = new FormData();
    const fieldMap: Record<keyof TemplateFiles, string> = {
      excel: "excel",
      zip: "zipImage",
      logo: "logo",
      sign: "sign",
    };

    requiredFields.forEach((f) => {
      const file = files[f];
      if (file) formData.append(fieldMap[f], file);
    });

    setIsUploading(true);
    try {
      const res = await axios.post(
        `http://localhost:8086/certificates/generate-zip/${selectedTemplate.id}`,
        formData,
        {
          responseType: "blob",
          headers: {
            "Content-Type": "multipart/form-data",
            Authorization: `Bearer ${token}`,
          },
        }
      );

      const url = window.URL.createObjectURL(new Blob([res.data], { type: "application/zip" }));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "Certificates.zip");
      document.body.appendChild(link);
      link.click();
      link.remove();

      toast({ title: "Certificates generated successfully!" });
      setFiles({ excel: null, zip: null, logo: null, sign: null });
      setSelectedTemplateId(null);
      setTemplateImages([]);
    } catch (err: any) {
      console.error(err);
      setErrorMessage("An error occurred while generating certificates.");
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50 py-10 px-4 flex justify-center">
      <Card className="w-full max-w-6xl shadow-2xl rounded-3xl p-6 bg-white border border-gray-200">
        <CardHeader className="text-center mb-6">
          <CardTitle className="text-3xl md:text-4xl font-extrabold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
            Certificate Management
          </CardTitle>
          <p className="text-sm text-gray-500 mt-1">Role: {role}</p>
        </CardHeader>

        <CardContent className="space-y-6">
          <div className="flex flex-col items-center gap-2">
            <select
              className="w-full md:w-[85%] border rounded-lg p-3 text-sm focus:ring focus:ring-indigo-400"
              value={selectedTemplateId ?? ""}
              onChange={(e) => setSelectedTemplateId(Number(e.target.value))}
            >
              <option value="">-- Select Certificate Template --</option>
              {templates.length > 0 ? (
                templates.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.templateName}
                  </option>
                ))
              ) : (
                <option disabled>No templates found</option>
              )}
            </select>
          </div>

          {templateImages.length > 0 && (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 mt-4">
              {templateImages.map((img, idx) => (
                <img
                  key={idx}
                  src={img}
                  alt={`Template Image ${idx + 1}`}
                  className="h-28 object-contain rounded-md border"
                />
              ))}
            </div>
          )}

          {selectedTemplate && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
              {requiredFields.includes("excel") && (
                <UploadCard
                  title="Upload Excel"
                  icon={<FileSpreadsheet className="h-6 w-6 text-green-600" />}
                  accept=".xlsx,.xls"
                  onChange={(e) => handleFileUpload("excel", e)}
                  file={files.excel}
                />
              )}
              {requiredFields.includes("zip") && (
                <UploadCard
                  title="Upload Images (ZIP)"
                  icon={<Image className="h-6 w-6 text-purple-600" />}
                  accept=".zip"
                  onChange={(e) => handleFileUpload("zip", e)}
                  file={files.zip}
                />
              )}
              {requiredFields.includes("logo") && (
                <UploadCard
                  title="Upload Logo"
                  icon={<FileImage className="h-6 w-6 text-blue-600" />}
                  accept="image/*"
                  onChange={(e) => handleFileUpload("logo", e)}
                  file={files.logo}
                  preview
                />
              )}
              {requiredFields.includes("sign") && (
                <UploadCard
                  title="Upload Signature"
                  icon={<Stamp className="h-6 w-6 text-amber-600" />}
                  accept="image/*"
                  onChange={(e) => handleFileUpload("sign", e)}
                  file={files.sign}
                  preview
                />
              )}
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
                  <>
                    <Clock className="h-5 w-5 mr-2 animate-spin" /> Processing...
                  </>
                ) : (
                  <>
                    <CheckCircle className="h-5 w-5 mr-2" /> Generate Certificates
                  </>
                )}
              </Button>
              {errorMessage && <p className="text-red-600 mt-2 font-medium">{errorMessage}</p>}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
