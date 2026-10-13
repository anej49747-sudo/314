import { Alert, Button } from "antd";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
    apiGetFavourites,
    apiRemoveFavourite,
} from "../../api";
import "./favourites.scss";

function Favourites() {
    const navigate = useNavigate();

    const [projects, setProjects] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [removingId, setRemovingId] = useState("");

    const user = JSON.parse(
        localStorage.getItem("userdata") || "null"
    );
    const customerId = user?.userid;

    useEffect(() => {
        if (!customerId) {
            setLoading(false);
            return;
        }

        async function loadFavourites() {
            try {
                const res = await apiGetFavourites(customerId);

                const list = Array.isArray(res)
                    ? res
                    : res?.data ?? res?.projects ?? [];

                if (!Array.isArray(list)) {
                    throw new Error("Unexpected response format");
                }

                setProjects(list);
            } catch (err) {
                console.error("Unable to load favourites:", err);
                setError("Unable to load saved projects. Please try again.");
            } finally {
                setLoading(false);
            }
        }

        loadFavourites();
    }, [customerId]);

    const handleRemove = async (id) => {
        if (!customerId || !id) return;

        try {
            setRemovingId(id);
            await apiRemoveFavourite(customerId, id);

            setProjects((current) =>
                current.filter(
                    (project) => (project.id ?? project._id) !== id
                )
            );
        } catch (err) {
            console.error("Unable to remove favourite:", err);
            setError("Unable to remove this project from favourites.");
        } finally {
            setRemovingId("");
        }
    };

    return (
        <main className="favourites-page">
            <Button
                className="back-home-button"
                onClick={() => navigate("/home")}
            >
                ← Back to Home
            </Button>

            <div className="favourites-header">
                <h1>Saved Projects</h1>
            </div>

            {!customerId ? (
                <Alert
                    type="info"
                    showIcon
                    title="Log in to view your saved projects."
                    action={
                        <Button
                            size="small"
                            onClick={() => navigate("/login")}
                        >
                            Login
                        </Button>
                    }
                />
            ) : loading ? (
                null
            ) : (
                <>
                    {error && (
                        <Alert
                            type="error"
                            showIcon
                            title={error}
                            closable
                            onClose={() => setError("")}
                        />
                    )}

                    <p className="favourites-count">
                        {projects.length} saved project(s)
                    </p>

                    {projects.length === 0 ? (
                        <div className="favourites-empty">
                            <h2>No saved projects yet</h2>

                            <Button
                                type="primary"
                                onClick={() => navigate("/idp")}
                            >
                                View Projects
                            </Button>
                        </div>
                    ) : (
                        <div className="favourites-grid">
                            {projects.map((project) => {
                                const id = project.id ?? project._id;
                                const title =
                                    project.title ||
                                    project.name ||
                                    "Untitled Project";

                                const images = project.image;
                                const imageUrl =
                                    project.imageUrl ||
                                    (Array.isArray(images)
                                        ? images[0]
                                        : images);

                                return (
                                    <article
                                        className="favourite-card"
                                        key={id}
                                    >
                                        {imageUrl ? (
                                            <img
                                                src={imageUrl}
                                                alt={title}
                                                className="favourite-image"
                                            />
                                        ) : (
                                            <div className="favourite-no-image">
                                                No image available
                                            </div>
                                        )}

                                        <h2>{title}</h2>

                                        <p>
                                            {project.description ||
                                                "No description available."}
                                        </p>

                                        {id && (
                                            <Link
                                                to={`/idp/${id}`}
                                                className="favourite-view-link"
                                            >
                                                View Project
                                            </Link>
                                        )}

                                        <Button
                                            danger
                                            loading={removingId === id}
                                            disabled={
                                                !id || Boolean(removingId)
                                            }
                                            onClick={() => handleRemove(id)}
                                        >
                                            Remove from Saved
                                        </Button>
                                    </article>
                                );
                            })}
                        </div>
                    )}
                </>
            )}
        </main>
    );
}

export default Favourites;