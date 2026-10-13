import { Button } from "antd";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { apiGetProjects } from "../../api";
import "./idp.scss";

function IDP() {
    const navigate = useNavigate();

    const [projects, setProjects] = useState([]);
    const [keyword, setKeyword] = useState("");
    const [category, setCategory] = useState("All");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        async function loadProjects() {
            try {
                const res = await apiGetProjects();

                const list = Array.isArray(res)
                    ? res
                    : res?.projects ?? res?.idp ?? res?.data ?? [];

                if (!Array.isArray(list)) {
                    throw new Error("Unexpected project response format");
                }

                setProjects(list);
            } catch (err) {
                console.error("Unable to load projects:", err);
                setError("Unable to load projects. Please try again later.");
            } finally {
                setLoading(false);
            }
        }

        loadProjects();
    }, []);

    const categories = useMemo(() => {
        const values = projects
            .map((project) => project.category)
            .filter(Boolean);

        return ["All", ...new Set(values)];
    }, [projects]);

    const filteredProjects = useMemo(() => {
        const searchText = keyword.trim().toLowerCase();

        return projects.filter((project) => {
            const title = project.title || project.name || "";
            const description = project.description || "";
            const projectCategory = project.category || "";

            const matchesKeyword =
                title.toLowerCase().includes(searchText) ||
                description.toLowerCase().includes(searchText);

            const matchesCategory =
                category === "All" || projectCategory === category;

            return matchesKeyword && matchesCategory;
        });
    }, [projects, keyword, category]);

    if (loading) {
        return (
            <main className="idp-page">
                <Button
                    className="idp-back-button"
                    onClick={() => navigate("/home")}
                >
                    ← Back to Home
                </Button>
                <p>Loading projects...</p>
            </main>
        );
    }

    if (error) {
        return (
            <main className="idp-page">
                <Button
                    className="idp-back-button"
                    onClick={() => navigate("/home")}
                >
                    ← Back to Home
                </Button>
                <p className="idp-error">{error}</p>
            </main>
        );
    }

    return (
        <main className="idp-page">
            <Button
                className="idp-back-button"
                onClick={() => navigate("/home")}
            >
                ← Back to Home
            </Button>

            <div className="idp-header">
                <h1>Interior Design Projects</h1>
            </div>

            <div className="idp-controls">
                <input
                    type="search"
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    placeholder="Search projects..."
                    aria-label="Search projects"
                />

                <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    aria-label="Filter by category"
                >
                    {categories.map((item) => (
                        <option value={item} key={item}>
                            {item === "All" ? "All Categories" : item}
                        </option>
                    ))}
                </select>
            </div>

            <p className="idp-result-count">
                {filteredProjects.length} project(s) found
            </p>

            {filteredProjects.length === 0 ? (
                <p className="idp-empty">
                    No matching projects found.
                </p>
            ) : (
                <div className="directory-grid">
                    {filteredProjects.map((project, index) => {
                        const id = project.id ?? project._id;
                        const title =
                            project.title ||
                            project.name ||
                            "Untitled Project";

                        const images = project.image;
                        const imageUrl =
                            project.imageUrl ||
                            (Array.isArray(images) ? images[0] : images);

                        return (
                            <article
                                className="directory-card"
                                key={id || index}
                            >
                                {imageUrl && (
                                    <img
                                        className="idp-image"
                                        src={imageUrl}
                                        alt={title}
                                    />
                                )}

                                <h2>{title}</h2>

                                <p>
                                    {project.description ||
                                        "No description available."}
                                </p>

                                {project.category && (
                                    <p className="idp-category">
                                        <strong>Category:</strong>{" "}
                                        {project.category}
                                    </p>
                                )}

                                {id && (
                                    <Link
                                        to={`/idp/${id}`}
                                        className="idp-view-link"
                                    >
                                        View Project →
                                    </Link>
                                )}
                            </article>
                        );
                    })}
                </div>
            )}
        </main>
    );
}

export default IDP;