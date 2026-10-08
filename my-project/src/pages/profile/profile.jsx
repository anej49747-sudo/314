import { useNavigate, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import { apiGetuser } from "../../api";
import "./profile.css";

function Profile() {
  const navigate = useNavigate();

  // 获取当前页面的地址/路由
  const location = useLocation();

  const userdata = location.state?.userdata || {};
  // ？.可选链。创建一个叫 userdata 的变量，如果 location.state 存在，就拿 userdata；如果没有，就给它一个空对象。
  const [currentUser, setCurrentUser] = useState(userdata);
  console.log("收到的个人数据:", location.state);

  // usestate让react保存一个会变化的数据
  const [formData, setFormData] = useState({
    name: currentUser.name || "",
    email: currentUser.email || "",
    phone: currentUser.phone || "",
    role: currentUser.role || "",
  });

  // async请求后端指令
  const getUser = async () => {
    if (!userdata?.userid)
      return;
    try {
      const res = await apiGetuser(userdata.userid);
      console.log(res);
      if (res.status == "success" && res.userdata) {
        const user = res.userdata;

        setCurrentUser(user);

        setFormData({
          name: user.name || "",
          email: user.email || "",
          phone: user.phone || "",
          role: user.role || "",
        });
      }
    } catch (error) {
      console.error("请求失败:", error);
    }
  };

  useEffect(() => {
    if (userdata?.userid) {
      getUser();
    }
  }, [userdata.userid]);
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value,
    });
  };
  const handleSubmit = () => {
    console.log("提交的所有表单数据：", formData);
    // 这里可以把 formData 发送给后端 API
  };
  const [name, setName] = useState(userdata.fullName || "");
  const [email, setEmail] = useState(userdata.email || "");
  const [phone, setPhone] = useState(userdata.phone || "");
  const [showedit, setshowedit] = useState(false);
  const [editingName, setEditingName] = useState(false);
  const [editingEmail, setEditingEmail] = useState(false);
  const [editingPhone, setEditingPhone] = useState(false);
  const edit = () => {
    setshowedit(true);
  };
  const handleSave = (field) => {
    const currentUser = JSON.parse(localStorage.getItem("userdata") || "{}");

    if (field === "name") {
      currentUser.fullName = name;

      localStorage.setItem("userdata", JSON.stringify(currentUser));

      setEditingName(false);
    }

    if (field === "email") {
      currentUser.email = email;

      localStorage.setItem("userdata", JSON.stringify(currentUser));

      setEditingEmail(false);
    }

    if (field === "phone") {
      currentUser.phone = phone;

      localStorage.setItem("userdata", JSON.stringify(currentUser));

      setEditingPhone(false);
    }
  };
  const submit = () => {
    console.log("保存的个人数据", formData);
    setshowedit(false)
  };
  return (
    <div className="profile-page">
      <div className="profile-background"></div>

      <div className="profile-navigation">
        <button className="profile-navigation-btn" onClick={() => navigate(-1)}>
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

            <div className="username">{userdata.username || "Username"}</div>
          </div>

          <div className="profile-divider"></div>
          <div className="right">
            <li>
              <span>Name:</span>{" "}
              <input
                type="text"
                className="name"
                name="name"
                placeholder="Enter your name"
                value={formData.name}
                onChange={handleChange}
                disabled={!showedit}
              />
            </li>

            <li>
              <span>Email:</span>{" "}
              <input
                type="text"
                className="email"
                name="email"
                placeholder="Enter email"
                value={formData.email}
                onChange={handleChange}
                disabled={!showedit}
              />
            </li>
            <li>
              <span>Phone:</span>{" "}
              <input
                type="text"
                className="phone"
                name="phone"
                placeholder="Enter phone number"
                value={formData.phone}
                onChange={handleChange}
                disabled={!showedit}
              />
            </li>
            {showedit ? (
              <li className="but">
                <button onClick={() => setshowedit(false)}>cancel</button>

                <button onClick={() => submit()}>submit</button>
              </li>
            ) : (
              <li className="but">
                <button onClick={() => edit()}>Edit</button>
              </li>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Profile;
