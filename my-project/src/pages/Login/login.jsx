import { apiLogin, apiRegistration } from "../../api";
import "../Login/login.css"
import { Button, Checkbox, Form, Input, Alert } from 'antd';

import { useState } from "react";
import { useNavigate } from "react-router-dom";
function login() {
    const [alert, setAlert] = useState({ text: "", type: "" })
    const [pagecontent, setpagecontent] = useState("login")
    const [showAlert, setShowAlert] = useState(false)
    const navigate = useNavigate()
    const onFinish = async (values) => {
        console.log('Success:', values);
        try {
            const res = await apiLogin(values);
            console.log(res);
            if (res.status == "success") {
                setAlert({ text: ` ${res.userdata.username} Login Success!`, type: "success" })
                setShowAlert(true)
                localStorage.setItem("userdata", JSON.stringify({
                    userid: res.userdata.id,
                    username: res.userdata.username,
                    role: res.userdata.role,
                }))
                setTimeout(() => {
                    navigate('/home')
                }, 1000);


            } else {
                setAlert({ text: res.data.message, type: "error" })
            }
        } catch (error) {
            console.log(error)
            if (error) {
                console.log("Login failed")
            }
        }
    };
    const onFinishFailed = errorInfo => {
        console.log('Failed:', errorInfo);
    };
    const register = () => {
        console.log("regis")
        setpagecontent("register")
    }
    const login = () => {
        console.log("login")
        setpagecontent("login")
    }

    const onRegister = async (values) => {
        console.log('regis:', values);
        try {
            const res = await apiRegistration(values);
            console.log(res);

        } catch (error) {
            console.log(error)

        }
    };
    return (
        <div className="login">
            <button
                id="back"
                style={{ position: "fixed", top: "40px", left: "40px" }}
                onClick={() => navigate("/home")}
            >← Back to home</button>

            {
                showAlert ? <Alert title={alert.text} type={alert.type} style={{ position: "fixed", alignSelf: "center", top: 10 }} /> : null
            }

            <div className="loginbox">

                <li className="title"><span onClick={() => login()} style={{
                    color: pagecontent === "login"
                        ? "rgb(209, 175, 88)"
                        : ""
                }}>Login</span><b>/</b><span onClick={() => register()} style={{
                    color: pagecontent === "register"
                        ? "rgb(209, 175, 88)"
                        : ""
                }}>Register</span></li>
                {
                    pagecontent == "login" ? <Form
                        colon={false}
                        name="basic"
                        labelCol={{ span: 8 }}
                        wrapperCol={{ span: 16 }}
                        style={{ maxWidth: 600 }}
                        initialValues={{ remember: true }}
                        onFinish={onFinish}
                        onFinishFailed={onFinishFailed}
                        autoComplete="off"
                    >
                        <Form.Item
                            label="Username"
                            name="username"
                            rules={[{ required: true, message: 'This field is required.' }]}
                        >
                            <Input />
                        </Form.Item>

                        <Form.Item
                            label="Password"
                            name="password"
                            rules={[{ required: true, message: 'This field is required.' }]}
                        >
                            <Input.Password />
                        </Form.Item>



                        <Form.Item label={null} wrapperCol={{ span: 24 }}>
                            <Button type="primary" htmlType="submit">
                                Login
                            </Button>
                        </Form.Item>
                    </Form>
                        : <Form
                            colon={false}
                            name="basic"
                            labelCol={{ span: 8 }}
                            wrapperCol={{ span: 16 }}
                            style={{ maxWidth: 600 }}
                            initialValues={{ remember: true }}
                            onFinish={onRegister}
                            onFinishFailed={onFinishFailed}
                            autoComplete="off"
                        >
                            <Form.Item
                                label="Username"
                                name="username"
                                rules={[{ required: true, message: 'This field is required.' }]}
                            >
                                <Input />
                            </Form.Item>
                            {/* <Form.Item
                                label="Email"
                                name="email"
                                rules={[{ required: true, message: 'This field is required.' }]}
                            >
                                <Input />
                            </Form.Item> */}

                            <Form.Item
                                label="Password"
                                name="password"
                                rules={[{ required: true, message: 'This field is required.' }, {
                                    min: 6,
                                    max: 6,
                                    message: "Password must be exactly 6 characters."
                                }]}
                            >
                                <Input.Password />
                            </Form.Item>



                            <Form.Item label={null} wrapperCol={{ span: 24 }}>
                                <Button type="primary" htmlType="submit">
                                    Register
                                </Button>
                            </Form.Item>
                        </Form>

                }
            </div>

        </div >
    )
}
export default login;