import axios from "axios";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./createdesigner.scss";

const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ||
    "https://314-production.up.railway.app";

const createEmptyDesigner = () => ({
    username: "",
    password: "",
    name: "",
    role: "designer",
    isEditing: false,
    isSaved: false,
});

function CreateDesigner() {
    const navigate = useNavigate();
    const location = useLocation();
    const userdata = location.state?.userdata || {};

    const [form, setForm] = useState(createEmptyDesigner());
    const [designers, setDesigners] = useState([]);
    const [creating, setCreating] = useState(false);
    const [deletingId, setDeletingId] = useState(null);
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("success");

    const showMessage = (text, type = "success", duration = 0) => {
        setMessage(text);
        setMessageType(type);

        if (duration > 0) {
            window.setTimeout(() => {
                setMessage((current) => (current === text ? "" : current));
            }, duration);
        }
    };

    // Load designers from the backend.
    const loadDesigners = async () => {
        try {
            const response = await axios.get(
                `${API_BASE_URL}/api/designer`
            );

            if (response.data.status === "success") {
                setDesigners(response.data.designers || []);
            } else {
                showMessage(
                    response.data.message || "Unable to load Designer List.",
                    "error"
                );
            }
        } catch (error) {
            console.error("Failed to load designers:", error);
            showMessage("Unable to load Designer List.", "error");
        }
    };

    useEffect(() => {
        loadDesigners();
    }, []);

    // Update editable form fields.
    const updateForm = (field, value) => {
        setForm((current) => ({
            ...current,
            [field]: value,
            isSaved: false,
        }));
    };

    // Edit
    const handleEdit = () => {
        setForm((current) => ({
            ...current,
            isEditing: true,
            isSaved: false,
        }));

        setMessage("");
    };

    // Save
    const handleSave = () => {
        if (
            !form.username.trim() ||
            !form.password ||
            !form.name.trim()
        ) {
            showMessage(
                "Please fill in data before saving.",
                "error",
                3000
            );
            return;
        }

        setForm((current) => ({
            ...current,
            username: current.username.trim(),
            name: current.name.trim(),
            role: "designer",
            isEditing: false,
            isSaved: true,
        }));

        showMessage("Form saved.", "success", 2000);
    };

    // Create the Designer account through the backend.
    const createDesigner = async () => {
        if (!form.isSaved || form.isEditing) {
            showMessage(
                "Please click Edit and Save before creating the account.",
                "error",
                3000
            );
            return;
        }

        if (
            !form.username.trim() ||
            !form.password ||
            !form.name.trim()
        ) {
            showMessage(
                "Please complete all fields before creating the account.",
                "error",
                3000
            );
            return;
        }

        setCreating(true);
        setMessage("");

        try {
            const response = await axios.post(
                `${API_BASE_URL}/api/admin/create-designer`,
                {
                    username: form.username.trim(),
                    password: form.password,
                    name: form.name.trim(),
                    role: "designer",
                }
            );

            if (response.data.status !== "success") {
                showMessage(
                    response.data.message ||
                    "Failed to create Designer account.",
                    "error",
                    3000
                );
                return;
            }

            showMessage(
                `Designer account "${form.username.trim()}" created!`,
                "success",
                2000
            );

            setForm(createEmptyDesigner());

            // Refresh the list after successful creation.
            await loadDesigners();
        } catch (error) {
            console.error("Failed to create Designer:", error);

            showMessage(
                error.response?.data?.message ||
                "Failed to create Designer account. Please check the backend API.",
                "error",
                4000
            );
        } finally {
            setCreating(false);
        }
    };

    // Delete a Designer account from the backend
    const removeDesigner = async (designer) => {
        if (!designer.id) {
            showMessage(
                "Cannot remove this Designer because the ID is missing.",
                "error",
                3000
            );
            return;
        }

        const confirmed = window.confirm(
            `Are you sure you want to remove "${designer.username}"? This action cannot be undone.`
        );

        if (!confirmed) {
            return;
        }

        setDeletingId(designer.id);
        setMessage("");

        try {
            const response = await axios.delete(
                `${API_BASE_URL}/api/designer/${encodeURIComponent(
                    designer.id
                )}`
            );

            if (response.data.status !== "success") {
                showMessage(
                    response.data.message || "Failed to remove Designer.",
                    "error",
                    3000
                );
                return;
            }

            showMessage(
                `Designer "${designer.username}" has been removed.`,
                "success",
                2500
            );

            // Refresh the list
            await loadDesigners();
        } catch (error) {
            console.error("Failed to remove Designer:", error);

            showMessage(
                error.response?.data?.message ||
                "Failed to remove Designer. Please check the backend API.",
                "error",
                4000
            );
        } finally {
            setDeletingId(null);
        }
    };

    return (
        <div className="create-designer-page">
            <header className="create-designer-header">
                <button
                    type="button"
                    className="create-designer-back"
                    onClick={() =>
                        navigate("/dashboard", {
                            state: { userdata },
                        })
                    }
                >
                    <span aria-hidden="true">←</span>
                    Back to Dashboard
                </button>
            </header>

            <main className="create-designer-layout">
                {/* Left column: Create Designer */}
                <section className="create-designer-section">
                    <div className="create-designer-card">
                        <div className="create-designer-card-header">
                            <h2>Create Designer</h2>
                        </div>

                        <div className="create-designer-fields">
                            <label>
                                Username
                                <input
                                    type="text"
                                    value={form.username}
                                    readOnly={!form.isEditing}
                                    onChange={(event) =>
                                        updateForm(
                                            "username",
                                            event.target.value
                                        )
                                    }
                                    placeholder="Enter username"
                                    autoComplete="off"
                                />
                            </label>

                            <label>
                                Password
                                <input
                                    type="password"
                                    value={form.password}
                                    readOnly={!form.isEditing}
                                    onChange={(event) =>
                                        updateForm(
                                            "password",
                                            event.target.value
                                        )
                                    }
                                    placeholder="Enter password"
                                    autoComplete="new-password"
                                />
                            </label>

                            <label>
                                Full Name
                                <input
                                    type="text"
                                    value={form.name}
                                    readOnly={!form.isEditing}
                                    onChange={(event) =>
                                        updateForm(
                                            "name",
                                            event.target.value
                                        )
                                    }
                                    placeholder="Enter full name"
                                />
                            </label>

                            <label>
                                Role
                                <input
                                    type="text"
                                    value="designer"
                                    disabled
                                />
                            </label>
                        </div>

                        <div className="create-designer-actions">
                            <button
                                type="button"
                                className="create-designer-edit-save"
                                onClick={
                                    form.isEditing ? handleSave : handleEdit
                                }
                                disabled={creating}
                            >
                                {form.isEditing ? "Save" : "Edit"}
                            </button>

                            <button
                                type="button"
                                className="create-designer-submit"
                                disabled={
                                    creating ||
                                    form.isEditing ||
                                    !form.isSaved
                                }
                                onClick={createDesigner}
                            >
                                {creating
                                    ? "Creating..."
                                    : "Create Designer"}
                                {!creating && (
                                    <span aria-hidden="true">→</span>
                                )}
                            </button>
                        </div>
                    </div>
                </section>

                {/* Right column: Designer List */}
                <section className="create-designer-list-section">
                    <div className="create-designer-list-heading">
                        <h2>Designer List</h2>
                        <span className="create-designer-count">
                            {designers.length} designers
                        </span>
                    </div>

                    <div className="create-designer-table-wrapper">
                        <table className="create-designer-table">
                            <thead>
                                <tr>
                                    <th>Username</th>
                                    <th>Full Name</th>
                                    <th>Role</th>
                                    <th>Remove</th>
                                </tr>
                            </thead>

                            <tbody>
                                {designers.length > 0 ? (
                                    designers.map((designer) => (
                                        <tr
                                            key={
                                                designer.id ||
                                                designer.username
                                            }
                                        >
                                            <td className="create-designer-username">
                                                {designer.username}
                                            </td>

                                            <td>
                                                {designer.name || "—"}
                                            </td>

                                            <td>
                                                <span className="create-designer-role">
                                                    {designer.role || "designer"}
                                                </span>
                                            </td>

                                            <td>
                                                <button
                                                    type="button"
                                                    className="create-designer-remove-button"
                                                    disabled={
                                                        deletingId ===
                                                        designer.id
                                                    }
                                                    onClick={() =>
                                                        removeDesigner(designer)
                                                    }
                                                >
                                                    {deletingId === designer.id
                                                        ? "Removing..."
                                                        : "Remove"}
                                                </button>
                                            </td>
                                        </tr>
                                    ))
                                ) : (
                                    <tr>
                                        <td
                                            colSpan="4"
                                            className="create-designer-empty"
                                        >
                                            No designers found.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </section>
            </main>

            {message && (
                <div
                    className={`create-designer-message ${messageType}`}
                    role="status"
                >
                    {message}
                </div>
            )}
        </div>
    );
}

export default CreateDesigner;
