import { useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Upload, ImageIcon, FileUp, Loader2 } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
// Optional import: uncomment only if framer-motion installed
// import { motion } from "framer-motion";

export default function UploadTemplate() {
  const [templateName, setTemplateName] = useState("");
  const [imageType, setImageType] = useState<number | string>("");
  const [jrxmlFiles, setJrxmlFiles] = useState<File[]>([]);
  const [images, setImages] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const token = sessionStorage.getItem("authToken");

  const handleJRXMLUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) setJrxmlFiles(Array.from(e.target.files));
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files);
      setImages(files);
      setImagePreviews(files.map((file) => URL.createObjectURL(file)));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!templateName.trim()) {
      toast({ title: "Template name required", variant: "destructive" });
      return;
    }
    if (!imageType) {
      toast({ title: "Image type required", variant: "destructive" });
      return;
    }
    if (jrxmlFiles.length === 0) {
      toast({ title: "No JRXML file selected", variant: "destructive" });
      return;
    }

    setLoading(true);
    const formData = new FormData();
    formData.append("templateName", templateName);
    formData.append("imageType", String(imageType));

    jrxmlFiles.forEach((file) => formData.append("jrxml", file));
    images.forEach((file) => formData.append("images", file));

    try {
      const res = await fetch("http://localhost:8086/templates", {
        method: "POST",
        body: formData,
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (res.ok) {
        toast({
          title: "Upload successful",
          description: "Template & images have been saved.",
        });
        setTemplateName("");
        setImageType("");
        setJrxmlFiles([]);
        setImages([]);
        setImagePreviews([]);
      } else {
        const errText = await res.text();
        toast({
          title: "Upload failed",
          description: errText || "Server error occurred.",
          variant: "destructive",
        });
      }
    } catch (error: any) {
      toast({
        title: "Network Error",
        description: error.message || "Unable to connect to server.",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-100 via-white to-slate-50 flex items-center justify-center p-6">
      <Card className="w-full max-w-3xl bg-white border border-gray-200 shadow-lg rounded-2xl">
        <CardHeader className="text-center border-b border-gray-100 pb-6">
          <CardTitle className="text-3xl font-semibold text-gray-800 tracking-tight">
            Upload Certificate Template
          </CardTitle>
          <p className="text-gray-500 text-sm mt-2">
            Managed by <span className="font-medium text-indigo-600">Nagendra Kumar Pandey</span>
          </p>
        </CardHeader>

        <CardContent>
          <form
            onSubmit={handleSubmit}
            className="space-y-6 mt-6"
            encType="multipart/form-data"
          >
            {/* Template Name */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Template Name
              </label>
              <Input
                type="text"
                placeholder="Enter a descriptive name"
                value={templateName}
                onChange={(e) => setTemplateName(e.target.value)}
                className="border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
              />
            </div>

            {/* Image Type */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Image Type (Numeric)
              </label>
              <Input
                type="number"
                placeholder="e.g. 1, 2, 3"
                value={imageType}
                onChange={(e) => setImageType(e.target.value)}
                className="border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
              />
            </div>

            {/* JRXML Upload */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                <FileUp size={18} className="text-indigo-500" /> Upload JRXML File(s)
              </label>
              <Input
                type="file"
                accept=".jrxml"
                multiple
                onChange={handleJRXMLUpload}
                className="cursor-pointer border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
              />
              {jrxmlFiles.length > 0 && (
                <p className="text-xs text-gray-500 mt-1">
                  {jrxmlFiles.length} file(s) selected
                </p>
              )}
            </div>

            {/* Image Upload */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                <ImageIcon size={18} className="text-indigo-500" /> Upload Image(s)
              </label>
              <Input
                type="file"
                accept="image/*"
                multiple
                onChange={handleImageUpload}
                className="cursor-pointer border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
              />

              {imagePreviews.length > 0 && (
                <div className="grid grid-cols-3 sm:grid-cols-4 gap-3 mt-3">
                  {imagePreviews.map((src, index) => (
                    <div
                      key={index}
                      className="relative rounded-lg overflow-hidden border border-gray-200 hover:shadow-md transition-shadow"
                    >
                      <img
                        src={src}
                        alt={`preview-${index}`}
                        className="object-cover w-full h-24"
                      />
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Submit Button */}
            <div className="pt-6 text-center">
              <Button
                type="submit"
                disabled={loading}
                className="bg-indigo-600 hover:bg-indigo-700 text-white px-8 py-3 rounded-lg font-medium shadow-md hover:shadow-lg transition-all"
              >
                {loading ? (
                  <>
                    <Loader2 className="animate-spin h-5 w-5 mr-2" />
                    Uploading...
                  </>
                ) : (
                  <>
                    <Upload size={18} className="mr-2" />
                    Upload Template
                  </>
                )}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
