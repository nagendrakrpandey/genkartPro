"use client";

import React, { useEffect, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
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
  avatarUrl?: string;
}

interface StatsData {
  totalCertificates: number;
  activeCertificates: number;
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
    avatarUrl: "",
  });
  const [stats, setStats] = useState<StatsData | null>(null);
  const [passwordData, setPasswordData] = useState({
    currentPassword: "",
    newPassword: "",
  });
  const [loading, setLoading] = useState(false);

  const { toast } = useToast();

  // Fetch profile
  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await api.get("/profile");
      setProfileData(res.data);
    } catch (err: any) {
      console.error("Error fetching profile:", err);
      toast({
        title: "Error",
        description: err.response?.data?.error || err.message || "Failed to load profile.",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // Fetch stats
  const fetchStats = async () => {
    setLoading(true);
    try {
      const res = await api.get("/profile/stats");
      setStats(res.data);
    } catch (err: any) {
      console.error("Error fetching stats:", err);
      toast({
        title: "Error",
        description: err.response?.data?.error || err.message || "Failed to load stats.",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
    fetchStats();
  }, []);

  // Avatar upload
  const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    const file = e.target.files[0];
    const formData = new FormData();
    formData.append("file", file);

    setLoading(true);
    try {
      const reader = new FileReader();
      reader.onload = () =>
        setProfileData((prev) => ({ ...prev, avatarUrl: reader.result as string }));
      reader.readAsDataURL(file);

      const res = await api.post("/profile/avatar", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setProfileData((prev) => ({ ...prev, avatarUrl: res.data.avatarUrl }));
      toast({ title: "Success", description: "Avatar uploaded successfully!" });
    } catch (err: any) {
      toast({
        title: "Error",
        description: err.response?.data?.error || "Failed to upload avatar.",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };




 // Update profile
const handleProfileUpdate = async () => {
  setLoading(true);
  try {
    const token = sessionStorage.getItem("authToken"); // JWT from session
    const wasUsernameChanged = profileData.username !== sessionStorage.getItem("username");
    const res = await api.put(
      "/profile",
      {
        name: profileData.name,
        username: profileData.username,
        email: profileData.email,
      },
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    setProfileData(res.data);

    toast({ title: "Success", description: "Profile updated successfully!" });

    if (wasUsernameChanged) {
      setTimeout(() => {
        sessionStorage.removeItem("authToken");
        navigate("/login");
      }, 1000);
    } else {
      fetchProfile(); 
    }
  } catch (err: any) {
    console.error("Update Profile Error:", err);

    const errorMessage =
      err.response?.data?.message ||
      err.response?.data?.error ||
      err.message ||
      "Access Denied to update profile.";

    toast({
      title: "Access Denied",
      description: errorMessage,
      variant: "destructive",
    });
  } finally {
    setLoading(false);
  }
};


  // Update password
  const handlePasswordUpdate = async () => {
    if (!passwordData.currentPassword || !passwordData.newPassword) {
      toast({ title: "Warning", description: "Please fill all fields." });
      return;
    }
    setLoading(true);
    try {
      await api.put("/profile/password", passwordData);
      toast({ title: "Success", description: "Password updated successfully!" });
      setPasswordData({ currentPassword: "", newPassword: "" });
    } catch (err: any) {
      toast({
        title: "Error",
        description: err.response?.data?.error || "Current password incorrect.",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 max-w-6xl mx-auto flex flex-col md:flex-row gap-6">
      {/* Sidebar */}
      <div className="md:w-1/4 w-full bg-white shadow-md rounded-lg p-4 space-y-3">
        <Button
          variant={activeTab === "profile" ? "default" : "outline"}
          className="w-full"
          onClick={() => setActiveTab("profile")}
        >
          Profile
        </Button>
        <Button
          variant={activeTab === "update" ? "default" : "outline"}
          className="w-full"
          onClick={() => setActiveTab("update")}
        >
          Update Profile
        </Button>
        <Button
          variant={activeTab === "password" ? "default" : "outline"}
          className="w-full"
          onClick={() => setActiveTab("password")}
        >
          Change Password
        </Button>
      </div>

      {/* Content */}
      <div className="md:w-3/4 w-full">
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle>
              {activeTab === "profile"
                ? "Profile Details"
                : activeTab === "update"
                  ? "Update Profile"
                  : "Change Password"}
            </CardTitle>
          </CardHeader>
          <CardContent className="p-6 space-y-6">
            {loading && <p className="text-gray-500">Loading...</p>}

            {/* Profile View */}
            {activeTab === "profile" && !loading && (
              <div className="space-y-4">
                {profileData.avatarUrl && (
                  <img
                    src={profileData.avatarUrl}
                    alt="Avatar"
                    className="w-24 h-24 rounded-full object-cover border shadow"
                  />
                )}
                <div className="flex justify-between" style={{borderRadius: "50%"}}>
                  <div className="space-y-4">
                    <p>
                      <strong>Name:</strong> {profileData.name}
                    </p>
                    <p>
                      <strong>Email:</strong> {profileData.email}
                    </p>
                    <p>
                      <strong>Role:</strong> {profileData.role}
                    </p>
                  </div>
                  <div style={{position: "relative", top: "-70px"}}>
                    <img src="public\images.jpg" alt="" />
                  </div>
                </div>
                {stats && (
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-4">
                    <Card className="bg-blue-50 p-4 text-center">
                      <p className="text-lg font-bold">{stats.totalCertificates}</p>
                      <p className="text-gray-600 text-sm">Total Certificates</p>
                    </Card>
                    <Card className="bg-green-50 p-4 text-center">
                      <p className="text-lg font-bold">{stats.activeCertificates}</p>
                      <p className="text-gray-600 text-sm">Active Certificates</p>
                    </Card>
                    <Card className="bg-purple-50 p-4 text-center">
                      <p className="text-sm">{new Date(stats.lastLogin).toLocaleString()}</p>
                      <p className="text-gray-600 text-sm">Last Login</p>
                    </Card>
                  </div>
                )}
              </div>
            )}

            {activeTab === "update" && (
              <div className="space-y-3">
                <Input
                  type="text"
                  placeholder="Full Name"
                  value={profileData.name}
                  onChange={(e) => setProfileData({ ...profileData, name: e.target.value })}
                />
                <Input
                  type="text"
                  readOnly
                  placeholder="Username"
                  value={profileData.username}
                  onChange={(e) => setProfileData({ ...profileData, username: e.target.value })}
                />
                <Input
                  type="email"
                  placeholder="Email"
                  value={profileData.email}
                  onChange={(e) => setProfileData({ ...profileData, email: e.target.value })}
                />
                <Button onClick={handleProfileUpdate} className="w-full" disabled={loading}>
                  Save Changes
                </Button>
              </div>
            )}

            {/* Change Password */}
            {activeTab === "password" && (
              <div className="space-y-3">
                <Input
                  type="password"
                  placeholder="Current Password"
                  value={passwordData.currentPassword}
                  onChange={(e) =>
                    setPasswordData({ ...passwordData, currentPassword: e.target.value })
                  }
                />
                <Input
                  type="password"
                  placeholder="New Password"
                  value={passwordData.newPassword}
                  onChange={(e) =>
                    setPasswordData({ ...passwordData, newPassword: e.target.value })
                  }
                />
                <Button
                  onClick={handlePasswordUpdate}
                  className="w-full bg-green-600 hover:bg-green-700"
                  disabled={loading}
                >
                  Update Password
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Profile;
