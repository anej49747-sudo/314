import axios from "axios";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../dashboard/dashboard.scss";
import "./categories.scss";

const API_BASE_URL = (
    import.meta.env.VITE_API_BASE_URL ||
    "https://314-production.up.railway.app"
).replace(/\/$/, "");

function Categories() {
    const navigate = useNavigate();
    const location = useLocation();
    const userdata = location.state?.userdata || {};

    const [categories, setCategories] = useState([]);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [editingId, setEditingId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    useEffect(() => {
        if (!success && !error) return;

        const timer = window.setTimeout(() => {
            setSuccess("");
            setError("");
        }, 2500);

        return () => window.clearTimeout(timer);
    }, [success, error]);

    const loadCategories = async () => {
        setLoading(true);

        try {
            const response = await axios.get(
                `${API_BASE_URL}/api/categories`
            );
            setCategories(response.data);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load categories. Please check the backend connection."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadCategories();
    }, []);

    const resetForm = () => {
        setName("");
        setDescription("");
        setEditingId(null);
        setError("");
        setSuccess("");
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!name.trim()) {
            setError("Please enter a category name.");
            setSuccess("");
            return;
        }

        setSaving(true);
        setError("");
        setSuccess("");

        const categoryData = {
            name: name.trim(),
            description: description.trim()
        };

        try {
            if (editingId) {
                await axios.put(
                    `${API_BASE_URL}/api/categories/${editingId}`,
                    categoryData
                );
                setSuccess("Category updated successfully.");
            } else {
                await axios.post(
                    `${API_BASE_URL}/api/categories`,
                    categoryData
                );
                setSuccess("Category created successfully.");
            }

            setName("");
            setDescription("");
            setEditingId(null);

            await loadCategories();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to save category. Please try again."
            );
        } finally {
            setSaving(false);
        }
    };

    const handleEdit = (category) => {
        setEditingId(category.id);
        setName(category.name || "");
        setDescription(category.description || "");
        setError("");
        setSuccess("");

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    };

    const handleDelete = async (category) => {
        const confirmed = window.confirm(
            `Are you sure you want to delete "${category.name}"?`
        );

        if (!confirmed) return;

        setError("");
        setSuccess("");

        try {
            await axios.delete(
                `${API_BASE_URL}/api/categories/${category.id}`
            );

            setSuccess("Category deleted successfully.");

            if (editingId === category.id) {
                setName("");
                setDescription("");
                setEditingId(null);
            }

            await loadCategories();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to delete category. Please try again."
            );
        }
    };

    const goToManagerDashboard = () => {
        navigate("/dashboard", {
            state: {
                userdata: {
                    ...userdata,
                    role: "manager"
                }
            },
            replace: true
        });
    };

    return (
        <div className="dashboard-page categories-page">
            <div className="dashboard-background"></div>

            <button
                type="button"
                className="dashboard-back-btn"
                onClick={goToManagerDashboard}
            >
                <span className="dashboard-back-icon">←</span>
                <span>Back to Dashboard</span>
            </button>

            {(error || success) && (
                <div
                    className={`categories-toast ${error ? "categories-toast-error" : ""
                        }`}
                    role="status"
                    aria-live="polite"
                >
                    <span className="categories-toast-icon">
                        {error ? "!" : "✓"}
                    </span>
                    <span>{error || success}</span>
                </div>
            )}

            <div className="dashboard-container categories-container">
                <div className="dashboard-title-section">
                    <h1>IDP Categories Management</h1>
                    <div className="dashboard-title-line"></div>
                    <p className="categories-subtitle">
                        Create, update and manage interior design project categories.
                    </p>
                </div>

                <div className="categories-form-card">
                    <h2>{editingId ? "Edit Category" : "Create Category"}</h2>

                    <form onSubmit={handleSubmit}>
                        <label htmlFor="category-name">Category Name</label>
                        <input
                            id="category-name"
                            type="text"
                            value={name}
                            onChange={(event) => setName(event.target.value)}
                            placeholder="Enter category name"
                            maxLength={100}
                            required
                            onInvalid={(event) =>
                                event.target.setCustomValidity(
                                    "Please enter a category name."
                                )
                            }
                            onInput={(event) =>
                                event.target.setCustomValidity("")
                            }
                        />

                        <label htmlFor="category-description">Description</label>
                        <textarea
                            id="category-description"
                            value={description}
                            onChange={(event) =>
                                setDescription(event.target.value)
                            }
                            placeholder="Enter category description"
                            rows={4}
                            maxLength={500}
                        />

                        <div className="categories-form-actions">
                            <button
                                type="submit"
                                className="categories-primary-btn"
                                disabled={saving}
                            >
                                {saving
                                    ? "Saving..."
                                    : editingId
                                        ? "Save Changes"
                                        : "Create Category"}
                            </button>

                            {editingId && (
                                <button
                                    type="button"
                                    className="categories-secondary-btn"
                                    onClick={resetForm}
                                >
                                    Cancel
                                </button>
                            )}
                        </div>
                    </form>
                </div>

                <div className="categories-list-card">
                    <div className="categories-list-heading">
                        <div>
                            <h2>Category List</h2>
                            <p>Manage your existing IDP categories.</p>
                        </div>

                        <span className="categories-count">
                            {categories.length} categories
                        </span>
                    </div>

                    {loading ? (
                        <p className="categories-empty">
                            Loading categories...
                        </p>
                    ) : categories.length === 0 ? (
                        <p className="categories-empty">
                            No categories found. Create your first category above.
                        </p>
                    ) : (
                        <div className="categories-table-wrapper">
                            <table className="categories-table">
                                <thead>
                                    <tr>
                                        <th>Category Name</th>
                                        <th>Description</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {categories.map((category) => (
                                        <tr key={category.id}>
                                            <td className="categories-name">
                                                {category.name}
                                            </td>
                                            <td>
                                                {category.description || "No description"}
                                            </td>
                                            <td>
                                                <div className="categories-row-actions">
                                                    <button
                                                        type="button"
                                                        className="categories-edit-btn"
                                                        onClick={() => handleEdit(category)}
                                                    >
                                                        Edit
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="categories-delete-btn"
                                                        onClick={() => handleDelete(category)}
                                                    >
                                                        Delete
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default Categories;