"use client";

import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "@/Services/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/components/ui/use-toast";

interface ProfileData {
  name: string;
  username: string;
  email: string;
  role: string;
}

interface StatsData {
  totalCertificates: number;
  lastLogin: string;
}

const Profile: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("profile");
  const [profileData, setProfileData] = useState<ProfileData>({
    name: "",
    username: "",
    email: "",
    role: "",
  });
  const [stats, setStats] = useState<StatsData | null>(null);
  const [passwordData, setPasswordData] = useState({ currentPassword: "", newPassword: "" });
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const token = sessionStorage.getItem("authToken");
      const res = await api.get("/profile", { headers: { Authorization: `Bearer ${token}` } });
      setProfileData(res.data);
    } catch (err: any) {
      toast({ title: "Error", description: err.response?.data?.error || "Failed to load profile.", variant: "destructive" });
    } finally { setLoading(false); }
  };

  const fetchStats = async () => {
    setLoading(true);
    try {
      const token = sessionStorage.getItem("authToken");
      const res = await api.get("/profile/stats", { headers: { Authorization: `Bearer ${token}` } });
      setStats(res.data);
    } catch (err: any) {
      toast({ title: "Error", description: err.response?.data?.error || "Failed to load stats.", variant: "destructive" });
    } finally { setLoading(false); }
  };

  useEffect(() => {
    fetchProfile();
    fetchStats();
  }, []);

  const handleProfileUpdate = async () => {
    setLoading(true);
    try {
      const token = sessionStorage.getItem("authToken");
      const res = await api.put("/profile", { name: profileData.name, username: profileData.username, email: profileData.email }, { headers: { Authorization: `Bearer ${token}` } });
      setProfileData(res.data);
      toast({ title: "Success", description: "Profile updated successfully!" });
    } catch (err: any) {
      toast({ title: "Error", description: err.response?.data?.error || "Failed to update profile.", variant: "destructive" });
    } finally { setLoading(false); }
  };

  const handlePasswordUpdate = async () => {
    if (!passwordData.currentPassword || !passwordData.newPassword) {
      toast({ title: "Warning", description: "Please fill all fields." });
      return;
    }
    setLoading(true);
    try {
      const token = sessionStorage.getItem("authToken");
      await api.put("/profile/password", passwordData, { headers: { Authorization: `Bearer ${token}` } });
      toast({ title: "Success", description: "Password updated successfully!" });
      setPasswordData({ currentPassword: "", newPassword: "" });
    } catch (err: any) {
      toast({ title: "Error", description: err.response?.data?.error || "Current password incorrect.", variant: "destructive" });
    } finally { setLoading(false); }
  };

  return (
    <div className="p-4 sm:p-6 max-w-6xl mx-auto flex flex-col md:flex-row gap-6">
      {/* Sidebar */}
      <div className="md:w-1/4 w-full flex flex-col gap-3">
        <Button variant={activeTab === "profile" ? "default" : "outline"} className="w-full" onClick={() => setActiveTab("profile")}>Profile</Button>
        <Button variant={activeTab === "update" ? "default" : "outline"} className="w-full" onClick={() => setActiveTab("update")}>Update Profile</Button>
        <Button variant={activeTab === "password" ? "default" : "outline"} className="w-full" onClick={() => setActiveTab("password")}>Change Password</Button>
      </div>

      {/* Main Content */}
      <div className="md:w-3/4 w-full space-y-6">
        <Card className="shadow-lg rounded-lg">
          <CardHeader>
            <CardTitle>
              {activeTab === "profile" ? "Profile Details" : activeTab === "update" ? "Update Profile" : "Change Password"}
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6 space-y-6">
            {loading && <p className="text-gray-500 text-center">Loading...</p>}

            {/* Profile Tab */}
            {activeTab === "profile" && !loading && (
              <div className="space-y-4">
                <p><strong>Name:</strong> {profileData.name}</p>
                <p><strong>Email:</strong> {profileData.email}</p>
                <p><strong>Role:</strong> {profileData.role}</p>

                {stats && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-6">
                    <Card className="bg-blue-50 p-6 text-center rounded-lg shadow-sm hover:shadow-md transition">
                      <p className="text-2xl font-bold">{stats.totalCertificates}</p>
                      <p className="text-gray-600 mt-1">Total Certificates</p>
                    </Card>
                    <Card className="bg-purple-50 p-6 text-center rounded-lg shadow-sm hover:shadow-md transition">
                      <p className="text-sm">{new Date(stats.lastLogin).toLocaleString()}</p>
                      <p className="text-gray-600 mt-1">Last Certificates Generated</p>
                    </Card>
                  </div>
                )}
              </div>
            )}

            {/* Update Profile Tab */}
            {activeTab === "update" && (
              <div className="space-y-4">
                <Input type="text" placeholder="Full Name" value={profileData.name} onChange={(e) => setProfileData({ ...profileData, name: e.target.value })} />
                <Input type="text" readOnly placeholder="Username" value={profileData.username} className="bg-gray-100 cursor-not-allowed" />
                <Input type="email" placeholder="Email" value={profileData.email} onChange={(e) => setProfileData({ ...profileData, email: e.target.value })} />
                <Button onClick={handleProfileUpdate} className="w-full" disabled={loading}>Save Changes</Button>
              </div>
            )}

            {/* Change Password Tab */}
            {activeTab === "password" && (
              <div className="space-y-4">
                <Input type="password" placeholder="Current Password" value={passwordData.currentPassword} onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })} />
                <Input type="password" placeholder="New Password" value={passwordData.newPassword} onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })} />
                <Button onClick={handlePasswordUpdate} className="w-full bg-green-600 hover:bg-green-700 transition" disabled={loading}>Update Password</Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Profile;
