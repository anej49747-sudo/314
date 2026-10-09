import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiGetDesignerById } from "../../api";

function DesignerProfile() {
    const { id } = useParams();

    const [designer, setDesigner] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadDesigner() {
            try {
                setLoading(true);
                setError("");

                const res = await apiGetDesignerById(id);
                const responseData = res?.data ?? res;

                // Support common backend response formats.
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
                    throw new Error("Designer not found");
                }

                if (!cancelled) {
                    setDesigner(data);
                }
            } catch (err) {
                console.error("Failed to load designer:", err);

                if (!cancelled) {
                    setError(
                        err?.response?.data?.message ||
                        "Unable to load this designer's profile."
                    );
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        if (id) {
            loadDesigner();
        } else {
            setError("Designer ID is missing.");
            setLoading(false);
        }

        return () => {
            cancelled = true;
        };
    }, [id]);

    if (loading) {
        return (
            <main className="directory-page">
                <p>Loading profile...</p>
            </main>
        );
    }

    if (error) {
        return (
            <main className="directory-page">
                <Link to="/designer">← Back to Designers</Link>
                <p>{error}</p>
            </main>
        );
    }

    if (!designer) {
        return (
            <main className="directory-page">
                <Link to="/designer">← Back to Designers</Link>
                <p>Designer not found.</p>
            </main>
        );
    }

    const designStyles = Array.isArray(designer.designStyles)
        ? designer.designStyles
        : typeof designer.designStyles === "string"
            ? designer.designStyles.split(",").map((style) => style.trim()).filter(Boolean)
            : [];

    return (
        <main className="directory-page">
            <Link to="/designer">← Back to Designers</Link>

            <article className="directory-card">
                <h1>{designer.name || designer.fullName || designer.username || "Designer"}</h1>

                <p>
                    <strong>Username:</strong>{" "}
                    {designer.username || "Not provided"}
                </p>

                <p>
                    <strong>Name:</strong>{" "}
                    {designer.name || designer.fullName || "Not provided"}
                </p>

                <p>
                    <strong>Gender:</strong>{" "}
                    {designer.gender || "Not provided"}
                </p>

                <p>
                    <strong>Email:</strong>{" "}
                    {designer.email || "Not provided"}
                </p>

                <p>
                    <strong>Company:</strong>{" "}
                    {designer.company || "Not provided"}
                </p>

                <p>
                    <strong>Introduction:</strong>{" "}
                    {designer.bio || designer.description || "Not provided"}
                </p>

                <section className="designer-styles">
                    <h2>Design Styles</h2>

                    {designStyles.length > 0 ? (
                        <ul>
                            {designStyles.map((style, index) => (
                                <li key={`${style}-${index}`}>{style}</li>
                            ))}
                        </ul>
                    ) : (
                        <p>Not provided</p>
                    )}
                </section>
            </article>
        </main>
    );
}

export default DesignerProfile;