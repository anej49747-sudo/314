
import { Alert, Button, Form, Input } from "antd";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiLogin, apiRegistration } from "../../api";
import "../Login/login.css";

function Login() {
    const [alert, setAlert] = useState({
        text: "",
        type: "",
    });

    const [pagecontent, setPagecontent] = useState("login");
    const [showAlert, setShowAlert] = useState(false);

    const navigate = useNavigate();

    // Login
    const onFinish = async (values) => {
        setShowAlert(false);

        try {
            const res = await apiLogin(values);
            console.log("Login response:", res);

            if (res.status === "success") {
                setAlert({
                    text: `${res.userdata.username} Login Success!`,
                    type: "success",
                });
                setShowAlert(true);

                localStorage.setItem(
                    "userdata",
                    JSON.stringify({
                        userid: res.userdata.id,
                        username: res.userdata.username,
                        role: res.userdata.role,
                    })
                );

                setTimeout(() => {
                    navigate("/home");
                }, 1000);
            } else {
                setAlert({
                    text: res.message || "Login failed",
                    type: "error",
                });
                setShowAlert(true);
            }
        } catch (error) {
            console.error("Login failed:", error);

            setAlert({
                text:
                    error.response?.data?.message ||
                    "Login failed.",
                type: "error",
            });
            setShowAlert(true);
        }
    };

    // Form validation failure
    const onFinishFailed = (errorInfo) => {
        console.log("Form validation failed:", errorInfo);
    };

    // Switch to Register
    const register = () => {
        setPagecontent("register");
        setShowAlert(false);
    };

    // Switch to Login 1
    const login = () => {
        setPagecontent("login");
        setShowAlert(false);
    };

    // Register
    const onRegister = async (values) => {
        setShowAlert(false);

        try {
            const res = await apiRegistration(values);
            console.log("Register response:", res);

            if (res.status === "success") {
                setAlert({
                    text: "Registration successful! Please log in.",
                    type: "success",
                });
                setShowAlert(true);

                // Switch to Login after successful registration
                setPagecontent("login");
            } else {
                setAlert({
                    text: res.message || "Registration failed",
                    type: "error",
                });
                setShowAlert(true);
            }
        } catch (error) {
            console.error("Registration failed:", error);

            setAlert({
                text:
                    error.response?.data?.message ||
                    "Registration failed. Please try again.",
                type: "error",
            });
            setShowAlert(true);
        }
    };

    return (
        <div className="login">
            <button
                id="back"
                style={{
                    position: "fixed",
                    top: "40px",
                    left: "40px",
                }}
                onClick={() => navigate("/home")}
            >
                ← Back to home
            </button>

            {showAlert && (
                <Alert
                    title={alert.text}
                    type={alert.type}
                    showIcon
                    closable
                    onClose={() => setShowAlert(false)}
                    style={{
                        position: "fixed",
                        alignSelf: "center",
                        top: 10,
                        zIndex: 1000,
                    }}
                />
            )}

            <div className="loginbox">
                <li className="title">
                    <span
                        onClick={login}
                        style={{
                            color:
                                pagecontent === "login"
                                    ? "rgb(209, 175, 88)"
                                    : "",
                            cursor: "pointer",
                        }}
                    >
                        Login
                    </span>

                    <b>/</b>

                    <span
                        onClick={register}
                        style={{
                            color:
                                pagecontent === "register"
                                    ? "rgb(209, 175, 88)"
                                    : "",
                            cursor: "pointer",
                        }}
                    >
                        Register
                    </span>
                </li>

                {pagecontent === "login" ? (
                    <Form
                        colon={false}
                        name="loginForm"
                        labelCol={{ span: 8 }}
                        wrapperCol={{ span: 16 }}
                        style={{ maxWidth: 600 }}
                        onFinish={onFinish}
                        onFinishFailed={onFinishFailed}
                        autoComplete="off"
                    >
                        <Form.Item
                            label="Username"
                            name="username"
                            rules={[
                                {
                                    required: true,
                                    message: "This field is required.",
                                },
                            ]}
                        >
                            <Input />
                        </Form.Item>

                        <Form.Item
                            label="Password"
                            name="password"
                            rules={[
                                {
                                    required: true,
                                    message: "This field is required.",
                                },
                            ]}
                        >
                            <Input.Password />
                        </Form.Item>

                        <Form.Item
                            label={null}
                            wrapperCol={{ span: 24 }}
                        >
                            <Button
                                type="primary"
                                htmlType="submit"
                                block
                            >
                                Login
                            </Button>
                        </Form.Item>
                    </Form>
                ) : (
                    <Form
                        colon={false}
                        name="registerForm"
                        labelCol={{ span: 8 }}
                        wrapperCol={{ span: 16 }}
                        style={{ maxWidth: 600 }}
                        onFinish={onRegister}
                        onFinishFailed={onFinishFailed}
                        autoComplete="off"
                    >
                        <Form.Item
                            label="Username"
                            name="username"
                            rules={[
                                {
                                    required: true,
                                    whitespace: true,
                                    message: "Please enter a username.",
                                },
                            ]}
                        >
                            <Input />
                        </Form.Item>

                        <Form.Item
                            label="Password"
                            name="password"
                            rules={[
                                {
                                    required: true,
                                    message: "Please enter a password.",
                                },
                                {
                                    min: 6,
                                    max: 6,
                                    message:
                                        "Password must be exactly 6 characters.",
                                },
                            ]}
                        >
                            <Input.Password maxLength={6} />
                        </Form.Item>

                        <Form.Item
                            label={null}
                            wrapperCol={{ span: 24 }}
                        >
                            <Button
                                type="primary"
                                htmlType="submit"
                                block
                            >
                                Register
                            </Button>
                        </Form.Item>
                    </Form>
                )}
            </div>
        </div>
    );
}

export default Login;
