import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiGetDesignerById } from "../../api";

function DesignerProfile() {
    const { id } = useParams();

    const [designer, setDesigner] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function loadDesigner() {
            try {
                setLoading(true);
                setError("");

                const res = await apiGetDesignerById(id);

                // Compatible with Axios responses and direct JSON responses
                const responseData = res?.data ?? res;

                // Your backend returns { designer: {...}, status: "success" }
                const data =
                    responseData?.designer ??
                    responseData?.userdata ??
                    responseData;

                if (
                    !data ||
                    typeof data !== "object" ||
                    responseData?.status === "error" ||
                    data.status === "error"
                ) {
                    throw new Error("Designer not found");
                }

                setDesigner(data);
            } catch (err) {
                console.error("Failed to load designer:", err);
                setError("Unable to load this designer's profile.");
            } finally {
                setLoading(false);
            }
        }

        if (id) {
            loadDesigner();
        } else {
            setError("Designer ID is missing.");
            setLoading(false);
        }
    }, [id]);

    if (loading) {
        return <p>Loading profile...</p>;
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
        return <p>Designer not found.</p>;
    }

    return (
        <main className="directory-page">
            <Link to="/designer">← Back to Designers</Link>

            <article className="directory-card">
                <h1>{designer.name || designer.username || "Designer"}</h1>

                <p>
                    <strong>Username:</strong>{" "}
                    {designer.username || "Not provided"}
                </p>

                <p>
                    <strong>Introduction:</strong>{" "}
                    {designer.description ||
                        designer.bio ||
                        "Not provided"}
                </p>

                {designer.specialization && (
                    <p>
                        <strong>Specialization:</strong>{" "}
                        {designer.specialization}
                    </p>
                )}

                {designer.email && (
                    <p>
                        <strong>Email:</strong> {designer.email}
                    </p>
                )}

                {designer.phone && (
                    <p>
                        <strong>Phone:</strong> {designer.phone}
                    </p>
                )}
            </article>
        </main>
    );
}

export default DesignerProfile;