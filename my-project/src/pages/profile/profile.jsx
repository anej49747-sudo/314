import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  apiGetuser,
  apiUpdateDesignerProfile,
} from "../../api";
import "./profile.css";

function Profile() {
  const navigate = useNavigate();
  const location = useLocation();

  // 获取当前用户信息，兼容路由传值和 localStorage
  const savedUser = JSON.parse(
    localStorage.getItem("userdata") || "{}"
  );

  const userdata = location.state?.userdata || savedUser;

  const userId =
    userdata.id ||
    userdata.userid ||
    savedUser.id ||
    savedUser.userid ||
    "";
  console.log("当前用户 ID:", userId);
  console.log("当前用户资料:", userdata);
  const [currentUser, setCurrentUser] = useState(userdata);

  const [formData, setFormData] = useState({
    name: userdata.name || userdata.fullName || "",
    gender: userdata.gender || "",
    email: userdata.email || "",
    company: userdata.company || "",
    bio: userdata.bio || "",
    designStyles: Array.isArray(userdata.designStyles)
      ? userdata.designStyles.join(", ")
      : "",
  });

  const [showedit, setshowedit] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  // 将后端返回的数据转换成表单格式
  const fillForm = (user) => {
    setFormData({
      name: user.name || "",
      gender: user.gender || "",
      email: user.email || "",
      company: user.company || "",
      bio: user.bio || "",
      designStyles: Array.isArray(user.designStyles)
        ? user.designStyles.join(", ")
        : "",
    });
  };

  // 获取最新用户资料
  useEffect(() => {
    const getUser = async () => {
      if (!userId) {
        setErrorMessage(
          "Cannot find your user ID. Please log in again."
        );
        return;
      }

      setLoading(true);
      setErrorMessage("");

      try {
        const res = await apiGetuser(userId);
        const result = res?.data ?? res;

        if (result.status === "success" && result.userdata) {
          const user = result.userdata;

          setCurrentUser(user);

          // 普通用户资料接口可能不包含设计师专属字段，
          // 因此保留路由或本地已有的设计师资料
          const mergedUser = {
            ...userdata,
            ...user,
            gender: user.gender ?? userdata.gender ?? "",
            company: user.company ?? userdata.company ?? "",
            bio: user.bio ?? userdata.bio ?? "",
            designStyles:
              user.designStyles ?? userdata.designStyles ?? [],
          };

          setCurrentUser(mergedUser);
          fillForm(mergedUser);

          localStorage.setItem(
            "userdata",
            JSON.stringify({
              ...savedUser,
              ...user,
            })
          );
        } else {
          setErrorMessage(
            result.message || "Unable to load your profile."
          );
        }
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
  }, [userId]);

  // 处理输入框变化
  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // 开始编辑
  const edit = () => {
    fillForm(currentUser);
    setErrorMessage("");
    setshowedit(true);
  };

  // 取消编辑，恢复原来的数据
  const handleCancel = () => {
    fillForm(currentUser);
    setErrorMessage("");
    setshowedit(false);
  };

  // 保存个人资料到后端
  const submit = async () => {
    if (!userId) {
      setErrorMessage("Cannot find your user ID. Please log in again.");
      return;
    }

    // 此接口只允许更新 designer 角色
    if (
      String(currentUser.role || userdata.role || "").toLowerCase()
      !== "designer"
    ) {
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

      if (result.status === "success") {
        const updatedUser = result.designer || {
          ...currentUser,
          ...payload,
        };

        setCurrentUser(updatedUser);
        fillForm(updatedUser);

        // 同步更新本地缓存
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
        alert("Profile updated successfully!");
      } else {
        setErrorMessage(
          result.message || "Failed to update profile."
        );
      }
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

  return (
    <div className="profile-page">
      <div className="profile-background"></div>

      <div className="profile-navigation">
        <button
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
          <div className="profile-title-line"></div>
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

            <div className="profile-role">
              {currentUser.role || userdata.role || ""}
            </div>
          </div>

          <div className="profile-divider"></div>

          <div className="right">
            {loading && <p>Loading profile...</p>}

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
                className="name"
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
                className="gender"
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
                className="email"
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
                className="company"
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
                className="bio"
                name="bio"
                placeholder="Introduce yourself and your experience"
                value={formData.bio}
                onChange={handleChange}
                disabled={!showedit || saving}
                rows={4}
              />
            </div>

            <div className="profile-field">
              <label htmlFor="profile-styles">Design Styles:</label>
              <input
                id="profile-styles"
                type="text"
                className="designStyles"
                name="designStyles"
                placeholder="e.g. Modern, Minimalist, Scandinavian"
                value={formData.designStyles}
                onChange={handleChange}
                disabled={!showedit || saving}
              />
              {showedit && (
                <small>
                  Separate each design style with a comma.
                </small>
              )}
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