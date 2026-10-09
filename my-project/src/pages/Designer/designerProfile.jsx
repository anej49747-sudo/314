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
                const res = await apiGetDesignerById(id);

                const data = res?.userdata ?? res?.designer ?? res?.data ?? res;

                if (!data || typeof data !== "object" || data.status === "error") {
                    throw new Error("Designer not found");
                }

                setDesigner(data);
            } catch (err) {
                console.error(err);
                setError("Unable to load this designer's profile.");
            } finally {
                setLoading(false);
            }
        }

        loadDesigner();
    }, [id]);

    if (loading) return <p>Loading profile...</p>;
    if (error) return <p>{error}</p>;
    if (!designer) return <p>Designer not found.</p>;

    return (
        <main className="directory-page">
            <Link to="/designer">← Back to Designers</Link>

            <article className="directory-card">
                <h1>{designer.name || designer.username || "Designer"}</h1>

                <p>
                    <strong>Introduction:</strong>{" "}
                    {designer.description || designer.bio || "Not provided"}
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