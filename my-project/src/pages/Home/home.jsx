import { Alert } from "antd";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../Home/home.scss";

function Home() {
  const [showAlert, setShowAlert] = useState(false);
  const [loginStatus, setLoginStatus] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const [user, setUser] = useState(
    JSON.parse(localStorage.getItem("userdata") || "null"),
  );

  const navigate = useNavigate();

  useEffect(() => {
    const currentUser = JSON.parse(
      localStorage.getItem("userdata") || "null",
    );

    setUser(currentUser);
    setLoginStatus(currentUser !== null);
  }, []);

  const username = user?.username;
  const role = user?.role;

  const LogOut = () => {
    localStorage.removeItem("userdata");

    setUser(null);
    setShowMenu(false);
    setShowAlert(true);
    setLoginStatus(false);

    setTimeout(() => {
      setShowAlert(false);
    }, 1000);
  };

  return (
    <div className="home">
      {showAlert && (
        <Alert
          title="Logout Success!"
          type="success"
          style={{
            position: "fixed",
            top: 10,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1001,
          }}
        />
      )}

      <header className="header">
        <nav className="header-nav">
          <button
            type="button"
            className="nav-link"
            onClick={() => navigate("/designer")}
          >
            Designers
          </button>
        </nav>

        <div className="header-actions">
          {loginStatus ? (
            <>
              <div className="user-dropdown">
                <button
                  type="button"
                  className="user-tag"
                  onClick={() => setShowMenu(!showMenu)}
                >
                  {role
                    ? role.charAt(0).toUpperCase() + role.slice(1)
                    : "User"}{" "}
                  : {username}
                  <span className="dropdown-arrow">
                    {showMenu ? "⌃" : "⌄"}
                  </span>
                </button>

                {showMenu && (
                  <div className="dropdown-menu">
                    <button
                      type="button"
                      className="dropdown-item"
                      onClick={() => {
                        setShowMenu(false);
                        navigate("/dashboard", {
                          state: { userdata: user },
                        });
                      }}
                    >
                      Dashboard
                    </button>
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={LogOut}
                className="login-register"
              >
                Logout
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={() => navigate("/login")}
              className="login-register"
            >
              Login/Register
            </button>
          )}
        </div>
      </header>

      <div className="homecontent">
        <section className="home-main">
          <div className="home-title">
            <h1>Interior Design Projects</h1>
            <div className="title-line"></div>
            <p>Discover and manage interior design projects.</p>
          </div>

          <div className="feature-section">
            <div className="feature-card feature-card-view-search">
              <div className="feature-icon">⌕</div>

              <div className="feature-content">
                <h2>View &amp; Search</h2>
                <p>
                  Search and explore different interior design projects.
                </p>
              </div>

              <button
                type="button"
                className="feature-button"
                onClick={() => navigate("/idp")}
              >
                <span>Explore Projects</span>
                <span>→</span>
              </button>
            </div>

            <div className="feature-card feature-card-save">
              <div className="feature-icon">♡</div>

              <div className="feature-content">
                <h2>Save</h2>
                <p>Save your favourite projects for later.</p>
              </div>

              <button
                type="button"
                className="feature-button"
                onClick={() => navigate("/favourites")}
              >
                <span>Saved Projects</span>
                <span>→</span>
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

export default Home;