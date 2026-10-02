import { apiLogin } from "../../api";
import "../Login/login.css"
import { Button, Checkbox, Form, Input, Alert } from 'antd';
import { useState } from "react";
import { useNavigate } from "react-router-dom";
function login() {
    const [alert, setAlert] = useState({ text: "", type: "" })
    const [showAlert, setShowAlert] = useState(false)
    const navigate = useNavigate()
    const onFinish = async (values) => {
        console.log('Success:', values);
        try {
            const res = await apiLogin(values);
            if (res.status == "success") {

                setAlert({ text: `用户${res.userdata.username}登陆成功`, type: "success" })
                setShowAlert(true)
                localStorage.setItem("userdata", JSON.stringify({
                    userid: res.userdata.id,
                    username: res.userdata.username
                }))
                setTimeout(() => {
                    navigate('/home')
                }, 1000);


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
    const login = () => {

    }
    return (
        <div className="login">
            {
                showAlert ? <Alert title={alert.text} type={alert.type} style={{ position: "fixed", alignSelf: "center", top: 10 }} /> : null
            }

            <div className="loginbox">
                <h1 className="login-title">Login</h1>
                <Form
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

                    <Form.Item className="remember-item" name="remember" valuePropName="checked" label="">
                        <Checkbox>Remember me</Checkbox>
                    </Form.Item>

                    <Form.Item label={null} wrapperCol={{ span: 24 }}>
                        <Button type="primary" htmlType="submit">
                            Submit
                        </Button>
                    </Form.Item>
                </Form>
            </div>

        </div >
    )
}
export default login;