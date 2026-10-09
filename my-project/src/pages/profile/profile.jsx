import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  apiGetuser,
  apiGetDesignerById,
  apiUpdateDesignerProfile,
} from "../../api";

import "./profile.css";

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
    userdata.id || userdata.userid || savedUser.id || savedUser.userid || "";

  const [currentUser, setCurrentUser] = useState(userdata);

  const [formData, setFormData] = useState({
    name: userdata.name || userdata.fullName || "",
    gender: userdata.gender || "",
    email: userdata.email || "",
    company: userdata.company || "",
    bio: userdata.bio || "",
    designStyles: Array.isArray(userdata.designStyles)
      ? userdata.designStyles.join(", ")
      : userdata.designStyles || "",
  });

  const [showedit, setshowedit] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const fillForm = (user) => {
    setFormData({
      name: user.name || "",
      gender: user.gender || "",
      email: user.email || "",
      company: user.company || "",
      bio: user.bio || "",
      designStyles: Array.isArray(user.designStyles)
        ? user.designStyles.join(", ")
        : user.designStyles || "",
    });
  };

  useEffect(() => {
    const getUser = async () => {
      if (!userId) {
        setErrorMessage("Cannot find your user ID. Please log in again.");
        return;
      }

      setLoading(true);
      setErrorMessage("");

      try {
        const role = String(
          userdata.role || savedUser.role || ""
        ).toLowerCase();

        const res =
          role === "designer"
            ? await apiGetDesignerById(userId)
            : await apiGetuser(userId);

        const result = res?.data ?? res;

        if (result.status !== "success") {
          setErrorMessage(
            result.message || "Unable to load your profile."
          );
          return;
        }

        const user = result.designer || result.userdata;

        if (!user) {
          setErrorMessage("No profile data was returned.");
          return;
        }

        setCurrentUser(user);
        fillForm(user);

        localStorage.setItem(
          "userdata",
          JSON.stringify({
            ...savedUser,
            ...user,
            id: user.id || userId,
          })
        );
      } catch (error) {
        console.error("Failed to load profile:", error);

        setErrorMessage(
          error?.response?.data?.message ||
          "Failed to load profile. Please try again."
        );
      } finally {
        setLoading(false);
      }
    };

    getUser();

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

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

    if (role !== "designer") {
      setErrorMessage(
        "Only designer accounts can update designer profiles."
      );
      return;
    }

    setSaving(true);
    setErrorMessage("");

    try {
      const payload = {
        name: formData.name.trim(),
        gender: formData.gender.trim(),
        email: formData.email.trim(),
        company: formData.company.trim(),
        bio: formData.bio.trim(),
        designStyles: formData.designStyles
          .split(",")
          .map((style) => style.trim())
          .filter(Boolean),
      };

      const res = await apiUpdateDesignerProfile(userId, payload);
      const result = res?.data ?? res;

      if (result.status !== "success") {
        setErrorMessage(
          result.message || "Failed to update profile."
        );
        return;
      }

      const updatedUser = result.designer || {
        ...currentUser,
        ...payload,
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
          id: updatedUser.id || userId,
        })
      );

      setshowedit(false);
      alert("Profile updated successfully!");
    } catch (error) {
      console.error("Failed to update profile:", error);

      setErrorMessage(
        error?.response?.data?.message ||
        "Failed to save your profile. Please try again."
      );
    } finally {
      setSaving(false);
    }
  };

  const role =
    currentUser.role || userdata.role || savedUser.role || "";

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
              {currentUser.username ||
                userdata.username ||
                "Username"}
            </div>

            <div className="profile-role">{role}</div>
          </div>

          <div className="profile-divider" />

          <div className="right">
            {loading && (
              <p className="profile-status">
                Loading profile...
              </p>
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
                disabled={!showedit || saving}
              />
            </div>

            <div className="profile-field">
              <label htmlFor="profile-gender">Gender:</label>
              <select
                id="profile-gender"
                name="gender"
                value={formData.gender}
                onChange={handleChange}
                disabled={!showedit || saving}
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
                disabled={!showedit || saving}
              />
            </div>

            <div className="profile-field">
              <label htmlFor="profile-company">Company:</label>
              <input
                id="profile-company"
                type="text"
                name="company"
                placeholder="Enter company name or information"
                value={formData.company}
                onChange={handleChange}
                disabled={!showedit || saving}
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
                disabled={!showedit || saving}
                rows={3}
              />
            </div>

            <div className="profile-field">
              <label htmlFor="profile-styles">
                Design Styles:
              </label>
              <input
                id="profile-styles"
                type="text"
                name="designStyles"
                placeholder="e.g. Modern, Minimalist, Scandinavian"
                value={formData.designStyles}
                onChange={handleChange}
                disabled={!showedit || saving}
              />


            </div>

            <div className="but">
              {showedit ? (
                <>
                  <button
                    type="button"
                    onClick={handleCancel}
                    disabled={saving}
                  >
                    Cancel
                  </button>

                  <button
                    type="button"
                    onClick={submit}
                    disabled={saving || loading}
                  >
                    {saving ? "Saving..." : "Save"}
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  onClick={edit}
                  disabled={loading}
                >
                  Edit
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Profile;