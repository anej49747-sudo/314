import { useLocation, useNavigate } from "react-router-dom";
import "../dashboard/dashboard.scss";

function Reports() {
    const navigate = useNavigate();
    const location = useLocation();
    const userdata = location.state?.userdata || {};

    const goToManagerDashboard = () => {
        navigate("/dashboard", {
            state: {
                userdata: {
                    ...userdata,
                    role: "manager"
                }
            },
            replace: true
        });
    };

    return (
        <div className="dashboard-page">
            <div className="dashboard-background"></div>

            <button
                type="button"
                className="dashboard-back-btn"
                onClick={goToManagerDashboard}
            >
                <span className="dashboard-back-icon">←</span>
                <span>Back to Dashboard</span>
            </button>

            <div className="dashboard-container">
                <div className="dashboard-title-section">
                    <h1>
                        Platform Reports
                        <br />
                        Management
                    </h1>

                    <div className="dashboard-title-line"></div>
                </div>

                <div className="dashboard-cards">
                    <div className="dashboard-card">
                        <div className="dashboard-card-content">
                            <h2>Daily Reports</h2>
                            <p>View daily platform activity.</p>
                        </div>
                    </div>

                    <div className="dashboard-card">
                        <div className="dashboard-card-content">
                            <h2>Weekly Reports</h2>
                            <p>View weekly platform activity.</p>
                        </div>
                    </div>

                    <div className="dashboard-card">
                        <div className="dashboard-card-content">
                            <h2>Monthly Reports</h2>
                            <p>View monthly platform activity.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Reports;