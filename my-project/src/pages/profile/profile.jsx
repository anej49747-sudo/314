import axios from "axios";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import {
  apiGetDesignerById,
  apiGetuser,
  apiUpdateDesignerProfile,
  apiUpdateUserProfile,
} from "../../api";

import "./profile.css";

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ||
  "https://314-production.up.railway.app"
).replace(/\/+$/, "");

function Profile() {
  const navigate = useNavigate();
  const location = useLocation();

  const [savedUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("userdata") || "{}");
    } catch {
      return {};
    }
  });

  const userdata = location.state?.userdata || savedUser;

  const userId =
    userdata.id ||
    userdata.userid ||
    savedUser.id ||
    savedUser.userid ||
    "";

  const roleForFetch = String(
    userdata.role || savedUser.role || ""
  ).toLowerCase();

  const [currentUser, setCurrentUser] = useState(userdata);

  const [formData, setFormData] = useState({
    name: userdata.name || userdata.fullName || "",
    gender: userdata.gender || "",
    email: userdata.email || "",
    bio: userdata.bio || "",
    designStyles: Array.isArray(userdata.designStyles)
      ? userdata.designStyles.join(", ")
      : userdata.designStyles || "",
  });

  const [showedit, setshowedit] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const fillForm = (user) => {
    setFormData({
      name: user.name || user.fullName || "",
      gender: user.gender || "",
      email: user.email || "",
      bio: user.bio || "",
      designStyles: Array.isArray(user.designStyles)
        ? user.designStyles.join(", ")
        : user.designStyles || "",
    });
  };

  useEffect(() => {
    let cancelled = false;

    const getUser = async () => {
      if (!userId) {
        setErrorMessage("Cannot find your user ID. Please log in again.");
        return;
      }

      setLoading(true);
      setErrorMessage("");

      try {
        const res =
          roleForFetch === "designer"
            ? await apiGetDesignerById(userId)
            : await apiGetuser(userId);

        const result = res?.data ?? res;

        if (result?.status !== "success") {
          throw new Error(
            result?.message || "Unable to load your profile."
          );
        }

        const user = result.designer || result.userdata;

        if (!user) {
          throw new Error("No profile data was returned.");
        }

        if (cancelled) return;

        const completeUser = {
          ...userdata,
          ...user,
          id: user.id || userId,
        };

        setCurrentUser(completeUser);
        fillForm(completeUser);

        const latestSavedUser = JSON.parse(
          localStorage.getItem("userdata") || "{}"
        );

        localStorage.setItem(
          "userdata",
          JSON.stringify({
            ...latestSavedUser,
            ...completeUser,
          })
        );
      } catch (error) {
        if (!cancelled) {
          console.error("Failed to load profile:", error);
          setErrorMessage(
            error?.response?.data?.message ||
            error.message ||
            "Whoops! Something went wrong!"
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    getUser();

    return () => {
      cancelled = true;
    };
  }, [userId, roleForFetch]);

  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const edit = () => {
    fillForm(currentUser);
    setErrorMessage("");
    setshowedit(true);
  };

  const handleCancel = () => {
    fillForm(currentUser);
    setErrorMessage("");
    setshowedit(false);
  };

  const submit = async () => {
    if (!userId) {
      setErrorMessage("Cannot find your user ID. Please log in again.");
      return;
    }

    const role = String(
      currentUser.role || userdata.role || savedUser.role || ""
    ).toLowerCase();

    setSaving(true);
    setErrorMessage("");

    try {
      const payload = {
        name: formData.name.trim(),
        gender: formData.gender.trim(),
        email: formData.email.trim(),
        bio: formData.bio.trim(),
        designStyles: formData.designStyles
          .split(",")
          .map((style) => style.trim())
          .filter(Boolean),
      };

      const res =
        role === "designer"
          ? await apiUpdateDesignerProfile(userId, payload)
          : await apiUpdateUserProfile(userId, {
            name: payload.name,
            gender: payload.gender,
            email: payload.email,
            bio: payload.bio,
          });

      const result = res?.data ?? res;

      if (result?.status !== "success") {
        throw new Error(
          result?.message || "Failed to update profile."
        );
      }

      const updatedUser = {
        ...currentUser,
        ...(result.userdata || {}),
        ...(result.designer || {}),
        ...payload,
        id: result.userdata?.id || result.designer?.id || userId,
      };

      setCurrentUser(updatedUser);
      fillForm(updatedUser);

      const latestSavedUser = JSON.parse(
        localStorage.getItem("userdata") || "{}"
      );

      localStorage.setItem(
        "userdata",
        JSON.stringify({
          ...latestSavedUser,
          ...updatedUser,
        })
      );

      setshowedit(false);
      window.alert("Profile updated successfully!");
    } catch (error) {
      console.error("Failed to update profile:", error);
      setErrorMessage(
        error?.response?.data?.message ||
        error.message ||
        "Failed to save your profile. Please try again."
      );
    } finally {
      setSaving(false);
    }
  };

  const role = String(
    currentUser.role || userdata.role || savedUser.role || ""
  ).toLowerCase();

  const isDesigner = role === "designer";
  const canDeleteAccount =
    role === "customer" || role === "designer";

  const handleDeleteAccount = async () => {
    if (!userId) {
      setErrorMessage("Cannot find your user ID. Please log in again.");
      return;
    }

    if (!canDeleteAccount) {
      setErrorMessage(
        "Admin and Manager cannot delete their own accounts."
      );
      return;
    }

    const confirmed = window.confirm(
      "Are you sure you want to permanently delete your account? This action cannot be undone."
    );

    if (!confirmed) return;

    setDeleting(true);
    setErrorMessage("");

    try {
      const response = await axios.delete(
        `${API_BASE_URL}/api/user/${encodeURIComponent(userId)}/account`
      );

      const result = response?.data ?? response;

      if (result.status !== "success") {
        throw new Error(
          result.message || "Failed to delete your account."
        );
      }

      localStorage.removeItem("userdata");
      sessionStorage.clear();

      window.alert("Your account has been deleted successfully.");
      navigate("/login", { replace: true });
    } catch (error) {
      console.error("Failed to delete account:", error);
      setErrorMessage(
        error?.response?.data?.message ||
        error.message ||
        "Failed to delete your account. Please try again."
      );
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="profile-page">
      <div className="profile-background" />

      <div className="profile-navigation">
        <button
          type="button"
          className="profile-navigation-btn"
          onClick={() => navigate(-1)}
        >
          <span className="navigation-icon">←</span>
          <span>Return to Dashboard</span>
        </button>
      </div>

      <div className="profile-container">
        <div className="profile-header">
          <h1>Personal Profile</h1>
          <div className="profile-title-line" />
        </div>

        <div className="profile-content">
          <div className="profile-left">
            <div className="avatar-wrapper">
              <div className="default-avatar">
                <span>👤</span>
              </div>
            </div>

            <div className="username">
              {currentUser.username || userdata.username || "Username"}
            </div>

            <div className="profile-role">{role}</div>
          </div>

          <div className="profile-divider" />

          <div className="right">
            {loading && (
              <p className="profile-status">Loading profile...</p>
            )}

            {errorMessage && (
              <p className="profile-error" role="alert">
                {errorMessage}
              </p>
            )}

            <div className="profile-field">
              <label htmlFor="profile-name">Name:</label>
              <input
                id="profile-name"
                type="text"
                name="name"
                placeholder="Enter your name"
                value={formData.name}
                onChange={handleChange}
                disabled={!showedit || saving || deleting}
              />
            </div>

            <div className="profile-field">
              <label htmlFor="profile-gender">Gender:</label>
              <select
                id="profile-gender"
                name="gender"
                value={formData.gender}
                onChange={handleChange}
                disabled={!showedit || saving || deleting}
              >
                <option value="">Select gender</option>
                <option value="Female">Female</option>
                <option value="Male">Male</option>
                <option value="Other">Other</option>
                <option value="Prefer not to say">
                  Prefer not to say
                </option>
              </select>
            </div>

            <div className="profile-field">
              <label htmlFor="profile-email">Email:</label>
              <input
                id="profile-email"
                type="email"
                name="email"
                placeholder="Enter email"
                value={formData.email}
                onChange={handleChange}
                disabled={!showedit || saving || deleting}
              />
            </div>

            <div className="profile-field">
              <label htmlFor="profile-bio">Introduction:</label>
              <textarea
                id="profile-bio"
                name="bio"
                placeholder="Introduce yourself and your experience"
                value={formData.bio}
                onChange={handleChange}
                disabled={!showedit || saving || deleting}
                rows={3}
              />
            </div>

            {isDesigner && (
              <div className="profile-field">
                <label htmlFor="profile-styles">Design Styles:</label>
                <input
                  id="profile-styles"
                  type="text"
                  name="designStyles"
                  placeholder="e.g. Modern, Minimalist, Scandinavian"
                  value={formData.designStyles}
                  onChange={handleChange}
                  disabled={!showedit || saving || deleting}
                />
              </div>
            )}

            <div className="but">
              {showedit ? (
                <>
                  <button
                    type="button"
                    onClick={handleCancel}
                    disabled={saving || deleting}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    onClick={submit}
                    disabled={saving || loading || deleting}
                  >
                    {saving ? "Saving..." : "Save"}
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  onClick={edit}
                  disabled={loading || deleting}
                >
                  Edit
                </button>
              )}
            </div>
          </div>
        </div>

        {canDeleteAccount && (
          <div className="profile-danger-zone">
            <p>
              Permanently delete your account. This action cannot be
              undone.
            </p>

            <button
              type="button"
              className="profile-delete-button"
              onClick={handleDeleteAccount}
              disabled={loading || saving || deleting}
            >
              {deleting ? "Deleting..." : "Delete My Account"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default Profile;