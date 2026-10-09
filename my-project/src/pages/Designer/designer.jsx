import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiGetDesigner } from "../../api";
import "./designer.scss";

function Designer() {
    const [designers, setDesigners] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadDesigners() {
            try {
                setLoading(true);
                setError("");

                const res = await apiGetDesigner();
                console.log("Designer API response:", res);

                const responseData = res?.data ?? res;

                const list = Array.isArray(responseData)
                    ? responseData
                    : responseData?.designers ??
                    responseData?.data?.designers ??
                    responseData?.data ??
                    responseData?.userdata ??
                    [];

                if (!Array.isArray(list)) {
                    throw new Error("Unexpected response format");
                }

                if (!cancelled) {
                    setDesigners(list);
                }
            } catch (err) {
                console.error("Failed to load designers:", err);

                if (!cancelled) {
                    setError(
                        "Unable to load designers. Please check the backend API."
                    );
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadDesigners();

        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <main className="designers-page">
            <div className="designers-container">
                <Link className="designers-back-link" to="/home">
                    <span aria-hidden="true">←</span>
                    Back to Home
                </Link>

                <header className="designers-heading">
                    <h1>Interior Designers</h1>
                </header>

                {loading ? (
                    <div className="designers-message">
                        Loading designers...
                    </div>
                ) : error ? (
                    <div className="designers-message designers-error">
                        {error}
                    </div>
                ) : designers.length === 0 ? (
                    <div className="designers-empty">
                        <span className="empty-icon" aria-hidden="true">
                            ♧
                        </span>
                        <h2>No designers found</h2>
                        <p>There are no designers to display right now.</p>
                    </div>
                ) : (
                    <section className="designers-grid">
                        {designers.map((designer, index) => {
                            const id = String(designer.id ?? designer._id ?? "").trim();

                            const name =
                                designer.name ||
                                designer.fullName ||
                                designer.username ||
                                "Designer";

                            const introduction =
                                designer.description ||
                                designer.bio ||
                                "No introduction available yet.";

                            const styles = Array.isArray(designer.designStyles)
                                ? designer.designStyles
                                : typeof designer.designStyles === "string"
                                    ? designer.designStyles
                                        .split(",")
                                        .map((style) => style.trim())
                                        .filter(Boolean)
                                    : [];

                            return (
                                <article
                                    className="designer-card"
                                    key={id ?? designer.username ?? index}
                                >
                                    <div className="designer-card-top">
                                        <div className="designer-card-icon" aria-hidden="true">
                                            ♙
                                        </div>

                                        <span className="designer-card-label">
                                            INTERIOR DESIGNER
                                        </span>
                                    </div>

                                    <div className="designer-card-content">
                                        <h2 className="designer-card-name">{name}</h2>

                                        {designer.company && (
                                            <p className="designer-card-company">
                                                <span aria-hidden="true">⌂</span>
                                                {designer.company}
                                            </p>
                                        )}

                                        <p className="designer-card-description">
                                            {introduction}
                                        </p>

                                        {styles.length > 0 && (
                                            <div className="designer-card-styles">
                                                {styles.slice(0, 3).map((style, styleIndex) => (
                                                    <span
                                                        className="designer-style-tag"
                                                        key={`${style} -${styleIndex} `}
                                                    >
                                                        {style}
                                                    </span>
                                                ))}

                                                {styles.length > 3 && (
                                                    <span className="designer-style-more">
                                                        +{styles.length - 3}
                                                    </span>
                                                )}
                                            </div>
                                        )}
                                    </div>

                                    <div className="designer-card-footer">
                                        {id ? (
                                            <Link
                                                className="designer-view-link"
                                                to={`/designer/ ${id} `}
                                            >
                                                View Profile
                                                <span aria-hidden="true">→</span>
                                            </Link>
                                        ) : (
                                            <span className="designer-no-profile">
                                                Profile unavailable
                                            </span>
                                        )}
                                    </div>
                                </article>
                            );
                        })}
                    </section>
                )}
            </div>
        </main>
    );
}

export default Designer;