
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiGetDesigner } from "../../api";

function Designer() {
    const [designers, setDesigners] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function loadDesigners() {
            try {
                const res = await apiGetDesigner();

                console.log("Designer API response:", res);

                const list = Array.isArray(res)
                    ? res
                    : res?.designers ??
                    res?.data ??
                    res?.userdata ??
                    [];

                if (!Array.isArray(list)) {
                    throw new Error("Unexpected response format");
                }

                setDesigners(list);
            } catch (err) {
                console.error("Failed to load designers:", err);

                setError(
                    "Unable to load designers. Please check the backend API."
                );
            } finally {
                setLoading(false);
            }
        }

        loadDesigners();
    }, []);

    return (
        <main style={{ padding: "30px" }}>

            {/* Back to Home button */}
            <Link
                to="/home"
                style={{
                    display: "inline-block",
                    padding: "10px 18px",
                    marginBottom: "25px",
                    border: "1px solid #ccc",
                    borderRadius: "8px",
                    backgroundColor: "#fff",
                    color: "#333",
                    textDecoration: "none",
                    fontSize: "14px",
                    cursor: "pointer",
                    transition: "background-color 0.2s",
                }}
            >
                ← Back to Home
            </Link>

            <h1>Interior Designers</h1>

            {loading ? (
                <p>Loading designers...</p>
            ) : error ? (
                <p>{error}</p>
            ) : designers.length === 0 ? (
                <p>No designers found.</p>
            ) : (
                <div>
                    {designers.map((designer) => {
                        const id = designer.id ?? designer._id;

                        return (
                            <div
                                key={id ?? designer.username}
                                style={{
                                    border: "1px solid #ddd",
                                    borderRadius: "10px",
                                    padding: "20px",
                                    marginBottom: "15px",
                                }}
                            >
                                <h2>
                                    {designer.name ||
                                        designer.username ||
                                        "Designer"}
                                </h2>

                                <p>
                                    {designer.description ||
                                        designer.bio ||
                                        "No introduction available."}
                                </p>

                                {id && (
                                    <Link to={`/designer/${id}`}>
                                        View Profile
                                    </Link>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </main>
    );
}

export default Designer;
