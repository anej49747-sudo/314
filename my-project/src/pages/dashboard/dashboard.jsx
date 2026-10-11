import { useLocation, useNavigate } from "react-router-dom";
import "../dashboard/dashboard.scss";

function Dashboard() {
  const navigate = useNavigate();
  const location = useLocation();

  const userdata = location.state?.userdata || {};

  console.log("Received data:", location.state);

  return (
    <div className="dashboard-page">
      <div className="dashboard-background"></div>

      <button
        className="dashboard-back-btn"
        onClick={() => navigate("/home")}
      >
        <span className="dashboard-back-icon">←</span>
        <span>Back to Home</span>
      </button>

      <div className="dashboard-container">

        <div className="dashboard-title-section">
          <h1>
            {userdata.role == "designer"
              ? "Designer"
              : userdata.role == "customer"
                ? "Customer"
                : userdata.role == "admin"
                  ? "Admin"
                  : userdata.role == "manager"
                    ? "Manager"
                    : ""}
            <br />
            Dashboard
          </h1>

          <div className="dashboard-title-line"></div>
        </div>

        {userdata.role == "designer" ? (

          <div className="dashboard-cards">

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>♙</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>Personal Profile</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                  onClick={() =>
                    navigate("/profile", {
                      state: {
                        userdata: userdata,
                      },
                    })
                  }
                >
                  <span>View Profile</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>⌂</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>Company Information</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                >
                  <span>Manage Company Information</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>▣</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>My Portfolio &amp; Interest Tracking</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                >
                  <span>Manage Portfolio &amp; View Interest</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

          </div>

        ) : userdata.role == "admin" ? (

          <div className="dashboard-cards">

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>♙</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>Personal Profile</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                  onClick={() =>
                    navigate("/profile", {
                      state: {
                        userdata: userdata,
                      },
                    })
                  }
                >
                  <span>View Profile</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>⚙</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>User Management</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                  onClick={() => navigate("/admin")}
                >
                  <span>Manage Users</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>♙</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>Create Designer Account</h2>


                <button
                  type="button"
                  className="dashboard-manager-btn"
                  onClick={() =>
                    navigate("/admin/create-designer", {
                      state: { userdata },
                    })
                  }
                >
                  <span>Create Designer Account</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>

              </div>
            </div>

          </div>

        ) : userdata.role == "customer" ? (

          <div className="dashboard-cards">

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>♙</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>Personal Profile</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                  onClick={() =>
                    navigate("/profile", {
                      state: {
                        userdata: userdata,
                      },
                    })
                  }
                >
                  <span>View Profile</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

          </div>

        ) : userdata.role == "manager" ? (

          <div className="dashboard-cards">

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>♙</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>Personal Profile</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                  onClick={() =>
                    navigate("/profile", {
                      state: {
                        userdata: userdata,
                      },
                    })
                  }
                >
                  <span>View Profile</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>❖</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>IDP Categories</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                  onClick={() => navigate("/management/categories")}
                >
                  <span>Manage IDP Categories</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

            <div className="dashboard-card">
              <div className="dashboard-card-icon">
                <span>◫</span>
              </div>

              <div className="dashboard-card-divider"></div>

              <div className="dashboard-card-content">
                <h2>Reports</h2>

                <button
                  type="button"
                  className="dashboard-manager-btn"
                  onClick={() => navigate("/management/reports")}
                >
                  <span>Daily / Weekly / Monthly Reports</span>
                  <span className="dashboard-button-arrow">→</span>
                </button>
              </div>
            </div>

          </div>

        ) : null}

      </div>
    </div>
  );
}

export default Dashboard;