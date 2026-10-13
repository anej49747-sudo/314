
import axios from "axios";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./IDP.scss";

const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ||
    "https://314-production.up.railway.app";

const api = axios.create({
    baseURL: API_BASE_URL,
});

const emptyForm = {
    title: "",
    description: "",
    imageText: "",
    status: "current",
};

function IDP() {
    const navigate = useNavigate();
    const location = useLocation();

    const [userdata] = useState(() => {
        if (location.state?.userdata) {
            return location.state.userdata;
        }

        try {
            return JSON.parse(localStorage.getItem("userdata")) || {};
        } catch {
            return {};
        }
    });

    const designerId =
        userdata.id ||
        userdata._id ||
        userdata.userid ||
        userdata.userId;

    const [projects, setProjects] = useState([]);
    const [favouriteCounts, setFavouriteCounts] = useState({});
    const [form, setForm] = useState(emptyForm);
    const [editingId, setEditingId] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [filter, setFilter] = useState("all");
    const [sortByPopularity, setSortByPopularity] = useState(false);

    const loadProjects = async () => {
        if (!designerId) {
            setError("Designer ID was not found. Please log in again.");
            setLoading(false);
            return;
        }

        setLoading(true);
        setError("");

        try {
            const response = await api.get(
                `/api/idp/designer/${encodeURIComponent(designerId)}`
            );

            const projectList = Array.isArray(response.data)
                ? response.data
                : [];

            setProjects(projectList);

            const counts = await Promise.all(
                projectList.map(async (project) => {
                    const id = project.id || project._id;

                    if (!id) {
                        return [id, 0];
                    }

                    try {
                        const countResponse = await api.get(
                            `/api/favourites/count/${encodeURIComponent(id)}`
                        );

                        return [id, Number(countResponse.data) || 0];
                    } catch (countError) {
                        console.error(
                            `Unable to load favourite count for ${id}:`,
                            countError
                        );
                        return [id, 0];
                    }
                })
            );

            setFavouriteCounts(Object.fromEntries(counts));
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load projects. Please try again."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadProjects();
    }, [designerId]);

    const openCreateForm = () => {
        setForm(emptyForm);
        setEditingId(null);
        setShowForm(true);
        setError("");
        setMessage("");
    };

    const openEditForm = (project) => {
        setForm({
            title: project.title || "",
            description: project.description || "",
            imageText: Array.isArray(project.image)
                ? project.image.join("\n")
                : "",
            status: project.status || "current",
        });

        setEditingId(project.id || project._id);
        setShowForm(true);
        setError("");
        setMessage("");
    };

    const closeForm = () => {
        setShowForm(false);
        setEditingId(null);
        setForm(emptyForm);
    };

    const handleChange = (event) => {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!designerId) {
            setError("Designer ID was not found. Please log in again.");
            return;
        }

        const image = form.imageText
            .split(/[\n,]+/)
            .map((url) => url.trim())
            .filter(Boolean);

        const payload = {
            designerId,
            title: form.title.trim(),
            description: form.description.trim(),
            image,
            status: form.status,
        };

        setSaving(true);
        setError("");
        setMessage("");

        try {
            if (editingId) {
                await api.put(
                    `/api/idp/${encodeURIComponent(editingId)}`,
                    payload,
                    {
                        params: { designerId },
                    }
                );

                setMessage("Project updated successfully.");
            } else {
                await api.post("/api/idp", payload);
                setMessage("Project created successfully.");
            }

            closeForm();
            await loadProjects();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to save the project. Please try again."
            );
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (project) => {
        const projectId = project.id || project._id;

        if (
            !window.confirm(
                `Are you sure you want to delete "${project.title}"?`
            )
        ) {
            return;
        }

        setError("");
        setMessage("");

        try {
            await api.delete(
                `/api/idp/${encodeURIComponent(projectId)}`,
                {
                    params: { designerId },
                }
            );

            setProjects((previous) =>
                previous.filter(
                    (item) => (item.id || item._id) !== projectId
                )
            );

            setFavouriteCounts((previous) => {
                const updated = { ...previous };
                delete updated[projectId];
                return updated;
            });

            setMessage("Project deleted successfully.");
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to delete the project. Please try again."
            );
        }
    };

    const filteredProjects = projects
        .filter((project) => {
            if (filter === "all") {
                return true;
            }

            return project.status === filter;
        })
        .sort((a, b) => {
            if (!sortByPopularity) {
                return 0;
            }

            const aId = a.id || a._id;
            const bId = b.id || b._id;

            return (
                (favouriteCounts[bId] || 0) -
                (favouriteCounts[aId] || 0)
            );
        });

    const currentCount = projects.filter(
        (project) => project.status === "current"
    ).length;

    const completedCount = projects.filter(
        (project) => project.status === "completed"
    ).length;

    const totalFavourites = projects.reduce((total, project) => {
        const id = project.id || project._id;
        return total + (favouriteCounts[id] || 0);
    }, 0);

    const formatDate = (value) => {
        if (!value) {
            return "—";
        }

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) {
            return "—";
        }

        return date.toLocaleDateString();
    };

    return (
        <main className="idp-page">
            <div className="idp-background" />

            <div className="idp-container">
                <button
                    type="button"
                    className="idp-back-btn"
                    onClick={() =>
                        navigate("/dashboard", {
                            state: { userdata },
                        })
                    }
                >
                    <span>←</span>
                    Back to Designer Dashboard
                </button>

                <header className="idp-header">
                    <p className="idp-eyebrow">DESIGNER WORKSPACE</p>
                    <h1>My Portfolio &amp; Interest Tracking</h1>
                </header>

                {error && (
                    <div
                        className="idp-alert idp-alert-error"
                        role="alert"
                    >
                        {error}
                    </div>
                )}

                {message && (
                    <div
                        className="idp-alert idp-alert-success"
                        role="status"
                    >
                        {message}
                    </div>
                )}

                <section className="idp-stats">
                    <div className="idp-stat-card">
                        <span>All Projects</span>
                        <strong>{projects.length}</strong>
                    </div>

                    <div className="idp-stat-card">
                        <span>Current Projects</span>
                        <strong>{currentCount}</strong>
                    </div>

                    <div className="idp-stat-card">
                        <span>Completed Projects</span>
                        <strong>{completedCount}</strong>
                    </div>

                    <div className="idp-stat-card">
                        <span>Total Favourites</span>
                        <strong>{totalFavourites}</strong>
                    </div>
                </section>

                <section className="idp-toolbar">
                    <div>
                        <h2>My Projects</h2>
                        <p>
                            Manage your current and past interior design projects.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="idp-primary-btn"
                        onClick={openCreateForm}
                    >
                        + Create IDP
                    </button>
                </section>

                {showForm && (
                    <section className="idp-form-card">
                        <div className="idp-form-heading">
                            <h2>
                                {editingId ? "Edit Project" : "Create New IDP"}
                            </h2>

                            <button
                                type="button"
                                className="idp-close-btn"
                                onClick={closeForm}
                                aria-label="Close form"
                            >
                                ×
                            </button>
                        </div>

                        <form onSubmit={handleSubmit}>
                            <label htmlFor="idp-title">Project Title</label>
                            <input
                                id="idp-title"
                                name="title"
                                value={form.title}
                                onChange={handleChange}
                                placeholder="Enter project title"
                                maxLength={150}
                                required
                            />

                            <label htmlFor="idp-description">Description</label>
                            <textarea
                                id="idp-description"
                                name="description"
                                value={form.description}
                                onChange={handleChange}
                                placeholder="Describe your design concept and project details"
                                rows={4}
                                required
                            />

                            <label htmlFor="idp-image">Image URLs</label>
                            <textarea
                                id="idp-image"
                                name="imageText"
                                value={form.imageText}
                                onChange={handleChange}
                                placeholder="Enter one image URL per line"
                                rows={3}
                            />

                            <label htmlFor="idp-status">Project Status</label>
                            <select
                                id="idp-status"
                                name="status"
                                value={form.status}
                                onChange={handleChange}
                            >
                                <option value="current">Current</option>
                                <option value="completed">Completed</option>
                            </select>

                            <div className="idp-form-actions">
                                <button
                                    type="button"
                                    className="idp-secondary-btn"
                                    onClick={closeForm}
                                    disabled={saving}
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    className="idp-primary-btn"
                                    disabled={saving}
                                >
                                    {saving
                                        ? "Saving..."
                                        : editingId
                                            ? "Save Changes"
                                            : "Create Project"}
                                </button>
                            </div>
                        </form>
                    </section>
                )}

                <div className="idp-filter-row">
                    {[
                        { value: "all", label: "All Projects" },
                        { value: "current", label: "Current" },
                        { value: "completed", label: "Completed" },
                    ].map((option) => (
                        <button
                            key={option.value}
                            type="button"
                            className={
                                filter === option.value
                                    ? "idp-filter-btn active"
                                    : "idp-filter-btn"
                            }
                            onClick={() => setFilter(option.value)}
                        >
                            {option.label}
                        </button>
                    ))}

                    <button
                        type="button"
                        className={
                            sortByPopularity
                                ? "idp-filter-btn active"
                                : "idp-filter-btn"
                        }
                        onClick={() =>
                            setSortByPopularity((previous) => !previous)
                        }
                    >
                        {sortByPopularity
                            ? "High to Low"
                            : "Sort by Popularity"}
                    </button>
                </div>

                {loading ? (
                    <p>Loading projects and favourite counts...</p>
                ) : filteredProjects.length === 0 ? (
                    <div className="idp-empty-state">
                        <h3>No projects found</h3>
                        <p>
                            Create your first IDP to start showcasing your design work.
                        </p>
                    </div>
                ) : (
                    <section className="idp-project-grid">
                        {filteredProjects.map((project, index) => {
                            const projectId = project.id || project._id;
                            const favouriteCount =
                                favouriteCounts[projectId] || 0;

                            return (
                                <article
                                    className="idp-project-card"
                                    key={projectId}
                                >
                                    <div className="idp-project-image">
                                        {project.image?.[0] ? (
                                            <img
                                                src={project.image[0]}
                                                alt={project.title}
                                                onError={(event) => {
                                                    event.currentTarget.style.display =
                                                        "none";
                                                }}
                                            />
                                        ) : (
                                            <div className="idp-image-placeholder">
                                                <span>Interior Design Project</span>
                                            </div>
                                        )}

                                        <span
                                            className={`idp-status ${project.status === "completed"
                                                ? "completed"
                                                : "current"
                                                }`}
                                        >
                                            {project.status === "completed"
                                                ? "Completed"
                                                : "Current"}
                                        </span>
                                    </div>

                                    <div className="idp-project-content">
                                        <h3>{project.title}</h3>

                                        {sortByPopularity && (
                                            <p className="idp-popularity-rank">
                                                Popularity Rank: Top {index + 1}
                                            </p>
                                        )}

                                        <p className="idp-favourite-count">
                                            ♥ Favourited by {favouriteCount}{" "}
                                            {favouriteCount === 1
                                                ? "customer"
                                                : "customers"}
                                        </p>

                                        <p className="idp-project-description">
                                            {project.description}
                                        </p>

                                        {project.image?.length > 1 && (
                                            <p className="idp-image-count">
                                                {project.image.length} image URLs
                                            </p>
                                        )}

                                        <div className="idp-project-dates">
                                            <span>
                                                Created: {formatDate(project.createTime)}
                                            </span>

                                            <span>
                                                Updated: {formatDate(project.updateTime)}
                                            </span>
                                        </div>

                                        <div className="idp-project-actions">
                                            <button
                                                type="button"
                                                className="idp-edit-btn"
                                                onClick={() =>
                                                    openEditForm(project)
                                                }
                                            >
                                                Edit
                                            </button>

                                            <button
                                                type="button"
                                                className="idp-delete-btn"
                                                onClick={() =>
                                                    handleDelete(project)
                                                }
                                            >
                                                Delete
                                            </button>
                                        </div>
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

export default IDP;