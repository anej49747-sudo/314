import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import newRequest from "../../utils/request";
import "./companyinformation.scss";

function CompanyInformation() {
    const navigate = useNavigate();
    const location = useLocation();

    const [userdata, setUserdata] = useState(() => {
        const savedUser = localStorage.getItem("userdata");

        try {
            return location.state?.userdata || JSON.parse(savedUser || "{}");
        } catch {
            return location.state?.userdata || {};
        }
    });

    const [formData, setFormData] = useState({
        company: "",
        companyDescription: "",
        companyContact: "",
        companyAddress: "",
    });

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const userId = userdata.id || userdata.userid || "";

    useEffect(() => {
        if (!userId) {
            setError("Designer account information is missing. Please log in again.");
            setLoading(false);
            return;
        }

        let cancelled = false;

        const loadCompanyInformation = async () => {
            try {
                const response = await newRequest({
                    url: `/api/designer/${userId}`,
                    method: "GET",
                });

                if (cancelled) return;

                if (response?.status !== "success" || !response?.designer) {
                    throw new Error(
                        response?.message || "Failed to load company information."
                    );
                }

                const designer = response.designer;

                setUserdata((previous) => {
                    const updated = { ...previous, ...designer };
                    localStorage.setItem("userdata", JSON.stringify(updated));
                    return updated;
                });

                setFormData({
                    company: designer.company || "",
                    companyDescription: designer.companyDescription || "",
                    companyContact: designer.companyContact || "",
                    companyAddress: designer.companyAddress || "",
                });
            } catch (err) {
                if (!cancelled) {
                    setError(
                        err.response?.data?.message ||
                        err.message ||
                        "Unable to load company information."
                    );
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        };

        loadCompanyInformation();

        return () => {
            cancelled = true;
        };
    }, [userId]);

    const handleChange = (event) => {
        const { name, value } = event.target;

        setFormData((previous) => ({
            ...previous,
            [name]: value,
        }));

        setMessage("");
        setError("");
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (!userId) {
            setError("Designer ID is missing.");
            return;
        }

        setSaving(true);
        setMessage("");
        setError("");

        try {
            const response = await newRequest({
                url: `/api/designer/${userId}/company-information`,
                method: "PUT",
                data: formData,
            });

            if (response?.status !== "success") {
                throw new Error(
                    response?.message || "Failed to save company information."
                );
            }

            const updatedDesigner = response.designer || {
                ...userdata,
                ...formData,
            };

            const updatedUserdata = {
                ...userdata,
                ...updatedDesigner,
            };

            setUserdata(updatedUserdata);
            localStorage.setItem("userdata", JSON.stringify(updatedUserdata));

            setFormData({
                company: updatedDesigner.company || "",
                companyDescription: updatedDesigner.companyDescription || "",
                companyContact: updatedDesigner.companyContact || "",
                companyAddress: updatedDesigner.companyAddress || "",
            });

            setMessage("Company information saved successfully.");

            window.setTimeout(() => {
                setMessage("");
            }, 2000);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.message ||
                "Unable to save company information."
            );
        } finally {
            setSaving(false);
        }
    };

    const handleCancel = () => {
        navigate("/dashboard", {
            state: { userdata },
        });
    };

    return (
        <div className="company-information-page">
            <div className="company-information-background"></div>

            <button
                type="button"
                className="company-information-back"
                onClick={handleCancel}
            >
                <span>←</span>
                Back to Dashboard
            </button>

            <main className="company-information-container">
                <header className="company-information-header">
                    <p className="company-information-eyebrow">DESIGNER DASHBOARD</p>

                    <h1>Company Information</h1>

                    <p className="company-information-subtitle">
                        Create and manage your company profile so customers can know your
                        company data.
                    </p>
                </header>

                <section className="company-information-card">
                    <div className="company-information-card-header">
                        <div>
                            <h2>Company Profile</h2>
                            <p>Enter your company's information below.</p>
                        </div>

                    </div>

                    {loading ? (
                        <div className="company-information-status">
                            Loading company information...
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit}>
                            <div className="company-information-fields">
                                <div className="company-information-field">
                                    <label htmlFor="company">Company Name</label>
                                    <input
                                        id="company"
                                        name="company"
                                        type="text"
                                        value={formData.company}
                                        onChange={handleChange}
                                        placeholder="Enter your company name"
                                        maxLength={150}
                                    />
                                </div>

                                <div className="company-information-field">
                                    <label htmlFor="companyContact">
                                        Company Contact Details
                                    </label>
                                    <input
                                        id="companyContact"
                                        name="companyContact"
                                        type="text"
                                        value={formData.companyContact}
                                        onChange={handleChange}
                                        placeholder="Phone number or business email"
                                        maxLength={200}
                                    />
                                </div>

                                <div className="company-information-field company-information-field-full">
                                    <label htmlFor="companyAddress">Company Address</label>
                                    <input
                                        id="companyAddress"
                                        name="companyAddress"
                                        type="text"
                                        value={formData.companyAddress}
                                        onChange={handleChange}
                                        placeholder="Enter your company's operating address"
                                        maxLength={300}
                                    />
                                    <span className="company-information-hint">
                                        This address can help customers identify your business
                                        location.
                                    </span>
                                </div>

                                <div className="company-information-field company-information-field-full">
                                    <label htmlFor="companyDescription">
                                        Company Description
                                    </label>
                                    <textarea
                                        id="companyDescription"
                                        name="companyDescription"
                                        value={formData.companyDescription}
                                        onChange={handleChange}
                                        placeholder="Introduce your company, services, experience and design approach..."
                                        rows={6}
                                        maxLength={3000}
                                    />
                                    <span className="company-information-hint">
                                        {formData.companyDescription.length}/3000 characters
                                    </span>
                                </div>
                            </div>

                            <div className="company-information-actions">
                                <div className="company-information-feedback" aria-live="polite">
                                    {error && (
                                        <span className="company-information-message error">
                                            {error}
                                        </span>
                                    )}

                                    {message && !error && (
                                        <span className="company-information-message success">
                                            {message}
                                        </span>
                                    )}
                                </div>

                                <div className="company-information-buttons">
                                    <button
                                        type="button"
                                        className="company-information-cancel"
                                        onClick={handleCancel}
                                        disabled={saving}
                                    >
                                        Cancel
                                    </button>

                                    <button
                                        type="submit"
                                        className="company-information-save"
                                        disabled={saving}
                                    >
                                        {saving ? "Saving..." : "Save"}
                                    </button>
                                </div>
                            </div>
                        </form>
                    )}
                </section>

            </main>
        </div>
    );
}

export default CompanyInformation;