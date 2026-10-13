import { Alert, Button, message } from "antd";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
    apiAddFavourite,
    apiGetFavourites,
    apiGetProjectById,
    apiRemoveFavourite,
} from "../../api";
import "./idpDetail.scss";

function IDPDetail() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [project, setProject] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);
    const [saved, setSaved] = useState(false);

    const user = JSON.parse(
        localStorage.getItem("userdata") || "null"
    );
    const customerId = user?.userid;

    useEffect(() => {
        async function loadProject() {
            try {
                setLoading(true);
                setError("");

                const res = await apiGetProjectById(id);
                const data = res?.data ?? res?.project ?? res;

                if (!data || !(data.id || data._id)) {
                    throw new Error("Project not found");
                }

                setProject(data);
            } catch (err) {
                console.error("Unable to load project:", err);
                setError(
                    "Unable to load this project. It may have been removed or is unavailable."
                );
            } finally {
                setLoading(false);
            }
        }

        if (id) {
            loadProject();
        } else {
            setError("Project ID is missing.");
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        if (!customerId || !id) {
            setSaved(false);
            return;
        }

        let active = true;

        async function checkFavourite() {
            try {
                const res = await apiGetFavourites(customerId);

                const list = Array.isArray(res)
                    ? res
                    : res?.data ?? res?.projects ?? [];

                if (!Array.isArray(list)) {
                    throw new Error("Unexpected favourites response format");
                }

                if (active) {
                    setSaved(
                        list.some(
                            (item) =>
                                String(item.id ?? item._id) === String(id)
                        )
                    );
                }
            } catch (err) {
                console.error("Unable to check favourite status:", err);
            }
        }

        checkFavourite();

        return () => {
            active = false;
        };
    }, [customerId, id]);

    const handleToggleFavourite = async () => {
        if (!customerId) {
            message.info("Please log in to manage your favourite projects.");
            navigate("/login");
            return;
        }

        if (!id || saving) {
            return;
        }

        try {
            setSaving(true);

            if (saved) {
                await apiRemoveFavourite(customerId, id);
                setSaved(false);
                message.success("Project removed from favourites.");
            } else {
                await apiAddFavourite(customerId, id);
                setSaved(true);
                message.success("Project added to favourites.");
            }
        } catch (err) {
            console.error("Unable to update favourite:", err);
            message.error(
                err.response?.data?.message ||
                "Unable to update your favourites. Please try again."
            );
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return null;
    }

    if (error || !project) {
        return (
            <main className="idp-detail-page">
                <Button onClick={() => navigate("/idp")}>
                    Back to Projects
                </Button>

                <Alert
                    type="error"
                    showIcon
                    title={error || "Project not found."}
                />
            </main>
        );
    }

    const title = project.title || "Untitled Project";
    const description =
        project.description || "No description available.";

    const images = Array.isArray(project.image)
        ? project.image
        : project.image
            ? [project.image]
            : project.imageUrl
                ? [project.imageUrl]
                : [];

    return (
        <main className="idp-detail-page">
            <div className="idp-detail-topbar">
                <Button onClick={() => navigate("/idp")}>
                    ← Back to Projects
                </Button>

                <Button onClick={() => navigate("/home")}>
                    Back to Home
                </Button>
            </div>

            <section className="idp-detail-card">
                <div className="idp-detail-heading">
                    <p className="idp-detail-eyebrow">
                        INTERIOR DESIGN PROJECT
                    </p>
                    <h1>{title}</h1>
                </div>

                {images.length > 0 ? (
                    <div className="idp-detail-gallery">
                        {images.map((image, index) => (
                            <img
                                key={`${image}-${index}`}
                                src={image}
                                alt={`${title} ${index + 1}`}
                            />
                        ))}
                    </div>
                ) : (
                    <div className="idp-detail-no-image">
                        No project images available
                    </div>
                )}

                <div className="idp-detail-information">
                    <h2>Project Description</h2>
                    <p>{description}</p>

                    <div className="idp-detail-meta">
                        <div>
                            <span>Project Status</span>
                            <strong>
                                {project.status === "past"
                                    ? "Past Project"
                                    : "Current Project"}
                            </strong>
                        </div>

                        {project.createTime && (
                            <div>
                                <span>Created</span>
                                <strong>
                                    {String(project.createTime).slice(0, 10)}
                                </strong>
                            </div>
                        )}

                        {project.updateTime && (
                            <div>
                                <span>Last Updated</span>
                                <strong>
                                    {String(project.updateTime).slice(0, 10)}
                                </strong>
                            </div>
                        )}
                    </div>
                </div>

                <div className="idp-detail-actions">
                    {project.designerId && (
                        <Link
                            className="idp-detail-designer-link"
                            to={`/designer/${project.designerId}`}
                            state={{ from: `/idp/${id}` }}
                        >
                            View Designer Profile
                        </Link>
                    )}

                    <Button
                        type={saved ? "default" : "primary"}
                        loading={saving}
                        onClick={handleToggleFavourite}
                    >
                        {saved ? "Removed" : "Add to Favourites"}
                    </Button>
                </div>
            </section>
        </main>
    );
}

export default IDPDetail;