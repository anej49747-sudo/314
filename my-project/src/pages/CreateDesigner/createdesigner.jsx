import axios from "axios";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./createdesigner.scss";

const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ||
    "https://314-production.up.railway.app";

const createEmptyDesigner = () => ({
    formId: crypto.randomUUID(),
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

    const [forms, setForms] = useState([createEmptyDesigner()]);
    const [designers, setDesigners] = useState([]);
    const [loading, setLoading] = useState({});
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("success");

    // Load existing designers
    const loadDesigners = async () => {
        try {
            const response = await axios.get(
                `${API_BASE_URL}/api/designer`
            );

            if (response.data.status === "success") {
                setDesigners(response.data.designers || []);
            } else {
                setMessage(
                    response.data.message || "Unable to load Designer List."
                );
                setMessageType("error");
            }
        } catch (error) {
            console.error("Failed to load designers:", error);
            setMessage("Unable to load Designer List.");
            setMessageType("error");
        }
    };

    useEffect(() => {
        loadDesigners();
    }, []);

    // Update form fields
    const updateForm = (formId, field, value) => {
        setForms((currentForms) =>
            currentForms.map((form) =>
                form.formId === formId
                    ? {
                        ...form,
                        [field]: value,
                        isSaved: false,
                    }
                    : form
            )
        );
    };

    // Enable editing
    const handleEdit = (formId) => {
        setForms((currentForms) =>
            currentForms.map((form) =>
                form.formId === formId
                    ? {
                        ...form,
                        isEditing: true,
                        isSaved: false,
                    }
                    : form
            )
        );

        setMessage("");
    };

    // Save form locally
    const handleSave = (form) => {
        if (
            !form.username.trim() ||
            !form.password ||
            !form.name.trim() ||
            !form.role.trim()
        ) {
            setMessage(
                "Please fill in data before saving."
            );
            setMessageType("error");
            return;
        }

        setForms((currentForms) =>
            currentForms.map((item) =>
                item.formId === form.formId
                    ? {
                        ...item,
                        username: item.username.trim(),
                        name: item.name.trim(),
                        role: item.role.trim().toLowerCase(),
                        isEditing: false,
                        isSaved: true,
                    }
                    : item
            )
        );

        setMessage(
            "Form saved. "
        );
        setMessageType("success");
    };

    // Add another form
    const addDesignerForm = () => {
        setForms((currentForms) => [
            ...currentForms,
            createEmptyDesigner(),
        ]);

        setMessage("");
    };

    // Remove a form
    const removeDesignerForm = (formId) => {
        setForms((currentForms) => {
            const remaining = currentForms.filter(
                (form) => form.formId !== formId
            );

            return remaining.length > 0
                ? remaining
                : [createEmptyDesigner()];
        });

        setMessage("");
    };

    // Create account through backend
    const createDesigner = async (form) => {
        if (!form.isSaved || form.isEditing) {
            setMessage(
                "Please click Edit and Save before creating the account."
            );
            setMessageType("error");
            return;
        }

        if (
            !form.username.trim() ||
            !form.password ||
            !form.name.trim() ||
            !form.role.trim()
        ) {
            setMessage(
                "Please complete all fields before creating the account."
            );
            setMessageType("error");
            return;
        }

        setLoading((current) => ({
            ...current,
            [form.formId]: true,
        }));

        setMessage("");

        try {
            const response = await axios.post(
                `${API_BASE_URL}/api/admin/create-designer`,
                {
                    username: form.username.trim(),
                    password: form.password,
                    name: form.name.trim(),
                    role: form.role.trim().toLowerCase(),
                }
            );

            if (response.data.status !== "success") {
                setMessage(
                    response.data.message ||
                    "Failed to create Designer account."
                );
                setMessageType("error");
                return;
            }

            setMessage(
                `Designer account "${form.username.trim()}" created successfully.`
            );
            setMessageType("success");

            setForms((currentForms) => {
                const remaining = currentForms.filter(
                    (item) => item.formId !== form.formId
                );

                return remaining.length > 0
                    ? remaining
                    : [createEmptyDesigner()];
            });

            await loadDesigners();
        } catch (error) {
            console.error("Failed to create Designer:", error);

            setMessage(
                error.response?.data?.message ||
                "Failed to create Designer account. Please check the backend API."
            );
            setMessageType("error");
        } finally {
            setLoading((current) => ({
                ...current,
                [form.formId]: false,
            }));
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
                    <span>←</span>
                    Back to Dashboard
                </button>
            </header>

            {/* Create Designer cards */}
            <section className="create-designer-section">
                <div className="create-designer-forms">
                    {forms.map((form, index) => (
                        <div
                            className="create-designer-card"
                            key={form.formId}
                        >
                            <div className="create-designer-card-header">
                                <h2>Create Designer</h2>

                                <div className="create-designer-card-header-actions">
                                    {forms.length > 1 && (
                                        <button
                                            type="button"
                                            className="create-designer-remove"
                                            onClick={() =>
                                                removeDesignerForm(form.formId)
                                            }
                                        >
                                            Remove
                                        </button>
                                    )}

                                    <button
                                        type="button"
                                        className="create-designer-add"
                                        onClick={addDesignerForm}
                                        aria-label="Add another Designer"
                                        title="Add another Designer"
                                    >
                                        +
                                    </button>
                                </div>
                            </div>

                            {forms.length > 1 && (
                                <p className="create-designer-form-number">
                                    Designer form {index + 1}
                                </p>
                            )}

                            <div className="create-designer-fields">
                                <label>
                                    Username
                                    <input
                                        type="text"
                                        value={form.username}
                                        readOnly={!form.isEditing}
                                        onChange={(event) =>
                                            updateForm(
                                                form.formId,
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
                                                form.formId,
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
                                                form.formId,
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
                                        value={form.role}
                                        disabled
                                        readOnly={!form.isEditing}
                                        onChange={(event) =>
                                            updateForm(
                                                form.formId,
                                                "role",
                                                event.target.value
                                            )
                                        }
                                        placeholder="Enter role"
                                    />
                                </label>
                            </div>

                            <div className="create-designer-actions">
                                <button
                                    type="button"
                                    className="create-designer-edit-save"
                                    onClick={() =>
                                        form.isEditing
                                            ? handleSave(form)
                                            : handleEdit(form.formId)
                                    }
                                >
                                    {form.isEditing ? "Save" : "Edit"}
                                </button>

                                <button
                                    type="button"
                                    className="create-designer-submit"
                                    disabled={
                                        Boolean(loading[form.formId]) ||
                                        form.isEditing ||
                                        !form.isSaved
                                    }
                                    onClick={() => createDesigner(form)}
                                >
                                    {loading[form.formId]
                                        ? "Creating..."
                                        : "Create Designer"}
                                    {!loading[form.formId] && <span>→</span>}
                                </button>
                            </div>
                        </div>
                    ))}
                </div>

                {message && (
                    <div
                        className={`create-designer-message ${messageType}`}
                        role="status"
                    >
                        {message}
                    </div>
                )}
            </section>

            {/* Designer List: unchanged, three columns only */}
            <section className="create-designer-list-section">
                <div className="create-designer-list-heading">
                    <h2>Designer List</h2>
                </div>

                <div className="create-designer-table-wrapper">
                    <table className="create-designer-table">
                        <thead>
                            <tr>
                                <th>Username</th>
                                <th>Full Name</th>
                                <th>Role</th>
                            </tr>
                        </thead>

                        <tbody>
                            {designers.length > 0 ? (
                                designers.map((designer) => (
                                    <tr key={designer.id}>
                                        <td className="create-designer-username">
                                            {designer.username}
                                        </td>

                                        <td>{designer.name || "—"}</td>

                                        <td>
                                            <span className="create-designer-role">
                                                {designer.role || "designer"}
                                            </span>
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td
                                        colSpan="3"
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
        </div>
    );
}

export default CreateDesigner;