import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { apiGetDesignerById } from "../../api";
import "./designerProfile.scss";

function DesignerProfile() {
    const { id: rawId } = useParams();
    const id = rawId?.trim();
    const location = useLocation();
    const backTo = location.state?.from || "/designer";
    const fromProject = Boolean(location.state?.from);

    const [designer, setDesigner] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadDesigner() {
            try {
                setLoading(true);
                setError("");

                if (!id) {
                    throw new Error("Designer ID is missing.");
                }

                const res = await apiGetDesignerById(id);
                const responseData = res?.data ?? res;

                const data =
                    responseData?.designer ??
                    responseData?.userdata ??
                    responseData?.user ??
                    responseData;

                if (
                    !data ||
                    typeof data !== "object" ||
                    Array.isArray(data) ||
                    responseData?.status === "error" ||
                    data.status === "error"
                ) {
                    throw new Error(
                        responseData?.message || "Designer not found."
                    );
                }

                if (!cancelled) {
                    setDesigner(data);
                }
            } catch (err) {
                console.error("Failed to load designer:", err);

                if (!cancelled) {
                    setError(
                        err?.response?.data?.message ||
                        err.message ||
                        "Unable to load this designer's profile."
                    );
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadDesigner();

        return () => {
            cancelled = true;
        };
    }, [id]);

    if (loading) {
        return (
            <main className="directory-page">
                <div className="profile-message">
                    Loading designer profile...
                </div>
            </main>
        );
    }

    if (error || !designer) {
        return (
            <main className="directory-page">
                <Link
                    className="back-link"
                    to={backTo}
                    state={fromProject ? { from: "/idp" } : undefined}
                >
                    <span aria-hidden="true">←</span>
                    {fromProject ? "Back to Project" : "Back to Designers"}
                </Link>

                <p className="profile-message">
                    {error || "Designer not found."}
                </p>
            </main>
        );
    }

    const designStyles = Array.isArray(designer.designStyles)
        ? designer.designStyles
        : typeof designer.designStyles === "string"
            ? designer.designStyles
                .split(",")
                .map((style) => style.trim())
                .filter(Boolean)
            : [];

    const information = [
        {
            label: "Username",
            value: designer.username,
            icon: "♙",
        },
        {
            label: "Name",
            value: designer.name || designer.fullName,
            icon: "✧",
        },
        {
            label: "Gender",
            value: designer.gender,
            icon: "♀",
        },
        {
            label: "Email",
            value: designer.email,
            icon: "✉",
        },
    ];

    const introduction =
        designer.bio || designer.description || "Not provided";

    return (
        <main className="directory-page">
            <Link
                className="back-link"
                to={backTo}
                state={fromProject ? { from: "/idp" } : undefined}
            >
                <span aria-hidden="true">←</span>
                {fromProject ? "Back to Project" : "Back to Designers"}
            </Link>

            <article className="directory-card">
                <div className="designer-content">
                    <section className="profile-section">
                        <h1 className="section-title">
                            {designer.name ||
                                designer.fullName ||
                                designer.username ||
                                "Designer"}
                        </h1>

                        <h2 className="subsection-title">
                            Personal Information
                        </h2>

                        <div className="designer-info-grid">
                            {information.map((item) => (
                                <div
                                    className="designer-info-item"
                                    key={item.label}
                                >
                                    <div
                                        className="info-icon"
                                        aria-hidden="true"
                                    >
                                        {item.icon}
                                    </div>

                                    <div className="info-text">
                                        <span className="info-label">
                                            {item.label}
                                        </span>

                                        <p className="info-value">
                                            {item.value || "Not provided"}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </section>

                    <section className="profile-section about-section">
                        <h2 className="section-title">
                            About the Designer
                        </h2>

                        <div className="designer-about">
                            <p className="about-text">{introduction}</p>
                        </div>
                    </section>

                    <section className="profile-section styles-section">
                        <h2 className="section-title">Design Styles</h2>

                        {designStyles.length > 0 ? (
                            <div className="designer-styles">
                                {designStyles.map((style, index) => (
                                    <span
                                        className="style-tag"
                                        key={`${style}-${index}`}
                                    >
                                        {style}
                                    </span>
                                ))}
                            </div>
                        ) : (
                            <p className="empty-styles">Not provided</p>
                        )}
                    </section>

                    <section className="profile-section company-section">
                        <h2 className="section-title">
                            Company Information
                        </h2>

                        <div className="designer-info-grid">
                            <div className="designer-info-item">
                                <div
                                    className="info-icon"
                                    aria-hidden="true"
                                >
                                    ⌂
                                </div>

                                <div className="info-text">
                                    <span className="info-label">
                                        COMPANY NAME
                                    </span>

                                    <p className="info-value">
                                        {designer.company?.trim() ||
                                            "Not provided"}
                                    </p>
                                </div>
                            </div>

                            <div className="designer-info-item">
                                <div
                                    className="info-icon"
                                    aria-hidden="true"
                                >
                                    ✉
                                </div>

                                <div className="info-text">
                                    <span className="info-label">
                                        COMPANY CONTACT
                                    </span>

                                    <p className="info-value">
                                        {designer.companyContact?.trim() ||
                                            "Not provided"}
                                    </p>
                                </div>
                            </div>

                            <div className="designer-info-item">
                                <div
                                    className="info-icon"
                                    aria-hidden="true"
                                >
                                    ⌖
                                </div>

                                <div className="info-text">
                                    <span className="info-label">
                                        COMPANY ADDRESS
                                    </span>

                                    <p className="info-value">
                                        {designer.companyAddress?.trim() ||
                                            "Not provided"}
                                    </p>
                                </div>
                            </div>
                        </div>

                        <div className="designer-about company-description">
                            <span className="info-label">
                                COMPANY DESCRIPTION
                            </span>

                            <p className="about-text">
                                {designer.companyDescription?.trim() ||
                                    "Not provided"}
                            </p>
                        </div>
                    </section>
                </div>
            </article>
        </main>
    );
}

export default DesignerProfile;