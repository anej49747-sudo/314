import { useNavigate } from "react-router-dom";
import { Alert } from 'antd';
import { useState, useEffect } from "react";
import "../Home/home.css"
function home() {
    const [showAlert, setShowAlert] = useState(false)
    const [loginStatus, setloginStatus] = useState(false)
    const user = JSON.parse(
        localStorage.getItem("userdata") || "null"
    );

    const navigate = useNavigate()
    useEffect(() => {
        if (user !== null) {
            setloginStatus(true)
        } else {
            setloginStatus(false)
        }
    }, [])

    const username = user?.username;
    // 登出
    const LogOut = () => {
        localStorage.removeItem("userdata")
        setShowAlert(true);

        setloginStatus(false)
        setTimeout(() => {
            setShowAlert(false);
        }, 1000);

    }
    return <div style={{ width: "100%" }}>
        {
            showAlert ? <Alert title={"登出成功"} type={"success"} style={{
                position: "fixed",
                top: 10,
                left: "50%",
                transform: "translateX(-50%)",
                zIndex: 1
            }} /> : null
        }
        <div className="header">
            {
                loginStatus ? <div style={{
                    width: "100%", height: "50px",
                }}>

                    <li style={{
                        fontSize: 20, width: "88px", height: "50px", backgroundColor: "white", position: "fixed", right: 0, display: "flex",
                        alignItems: "center",
                        justifyContent: "center"
                    }} onClick={() => LogOut()}>{username}   logout</li></div> : <div>
                    <li onClick={() => navigate('/login')} style={{
                        fontSize: 20, width: "88px", height: "50px", backgroundColor: "white", position: "fixed", right: 0, display: "flex",
                        alignItems: "center",
                        justifyContent: "center"
                    }}>login</li>
                </div>

            }

        </div>

        <span >

            home
        </span>
    </div>
}
export default home;