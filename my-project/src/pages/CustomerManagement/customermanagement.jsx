
import { useEffect, useState } from "react";
import newRequest from "../../utils/request";
import "./customermanagement.scss";

const CustomerManagement = () => {
    const [customers, setCustomers] = useState([]);
    const [searchUsername, setSearchUsername] = useState("");
    const [loading, setLoading] = useState(false);
    const [actionId, setActionId] = useState(null);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");

    // Load all customers
    const fetchCustomers = async (username = "") => {
        setLoading(true);
        setError("");
        setMessage("");

        try {
            let response;

            if (username.trim()) {
                response = await newRequest.get(
                    "/api/admin/customers/search",
                    {
                        params: { username: username.trim() },
                    }
                );
            } else {
                response = await newRequest.get("/api/admin/customers");
            }

            const data = response.data;

            if (data.status !== "success") {
                throw new Error(data.message || "Failed to load customers.");
            }

            setCustomers(data.customers || []);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.message ||
                "Unable to load customers. Please try again."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCustomers();
    }, []);

    // Search by username
    const handleSearch = (event) => {
        event.preventDefault();
        fetchCustomers(searchUsername);
    };

    // Reset search
    const handleReset = () => {
        setSearchUsername("");
        fetchCustomers("");
    };

    // Deactivate or reactivate customer
    const handleStatusChange = async (customer) => {
        const isActive = customer.active !== false;

        const action = isActive ? "deactivate" : "reactivate";
        const actionLabel = isActive ? "deactivate" : "reactivate";

        const confirmed = window.confirm(
            `Are you sure you want to ${actionLabel} the account "${customer.username}"?`
        );

        if (!confirmed) return;

        setActionId(customer.id);
        setError("");
        setMessage("");

        try {
            const response = await newRequest.put(
                `/api/admin/customers/${customer.id}/${action}`
            );

            const data = response.data;

            if (data.status !== "success") {
                throw new Error(data.message || "Unable to update account status.");
            }

            setMessage(
                data.message ||
                `Customer account ${action === "deactivate" ? "deactivated" : "reactivated"} successfully.`
            );

            // Reload the list to show the latest status
            await fetchCustomers(searchUsername);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.message ||
                "Unable to update account status."
            );
        } finally {
            setActionId(null);
        }
    };

    return (
        <div className="customer-management">
            <div className="customer-page-header">
                <div>
                    <p className="customer-eyebrow">USER ADMINISTRATION</p>
                    <h1>Customer Management</h1>
                    <p className="customer-subtitle">
                        View, search and manage registered customer accounts.
                    </p>
                </div>

                <button
                    type="button"
                    className="customer-refresh-button"
                    onClick={() => fetchCustomers(searchUsername)}
                    disabled={loading}
                >
                    ↻ Refresh
                </button>
            </div>

            <section className="customer-summary">
                <div className="customer-summary-icon">👥</div>
                <div>
                    <p className="customer-summary-label">Customers Found</p>
                    <h2>{customers.length}</h2>
                </div>
            </section>

            <section className="customer-list-card">
                <div className="customer-list-heading">
                    <div>
                        <h2>Customer List</h2>
                        <p>Search accounts and manage their access.</p>
                    </div>
                </div>

                <form className="customer-search" onSubmit={handleSearch}>
                    <div className="customer-search-input-wrapper">
                        <span className="customer-search-icon">⌕</span>
                        <input
                            type="text"
                            placeholder="Search by username..."
                            value={searchUsername}
                            onChange={(event) =>
                                setSearchUsername(event.target.value)
                            }
                            aria-label="Search customer by username"
                        />
                    </div>

                    <button type="submit" disabled={loading}>
                        Search
                    </button>

                    <button
                        type="button"
                        className="customer-reset-button"
                        onClick={handleReset}
                        disabled={loading}
                    >
                        Reset
                    </button>
                </form>

                {error && (
                    <div className="customer-alert customer-alert-error" role="alert">
                        {error}
                    </div>
                )}

                {message && (
                    <div
                        className="customer-alert customer-alert-success"
                        role="status"
                    >
                        {message}
                    </div>
                )}

                <div className="customer-table-wrapper">
                    <table className="customer-table">
                        <thead>
                            <tr>
                                <th>NO.</th>
                                <th>USERNAME</th>
                                <th>FULL NAME</th>
                                <th>ROLE</th>
                                <th>ACCOUNT STATUS</th>
                                <th>ACTION</th>
                            </tr>
                        </thead>

                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan="6" className="customer-table-empty">
                                        Loading customers...
                                    </td>
                                </tr>
                            ) : customers.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="customer-table-empty">
                                        No customer accounts found.
                                    </td>
                                </tr>
                            ) : (
                                customers.map((customer, index) => {
                                    const isActive = customer.active !== false;

                                    return (
                                        <tr key={customer.id || customer.username}>
                                            <td>{index + 1}</td>

                                            <td className="customer-username">
                                                {customer.username || "-"}
                                            </td>

                                            <td>{customer.name || "-"}</td>

                                            <td>
                                                <span className="customer-role">
                                                    {customer.role || "customer"}
                                                </span>
                                            </td>

                                            <td>
                                                <span
                                                    className={`customer-status ${isActive ? "status-active" : "status-inactive"
                                                        }`}
                                                >
                                                    <span className="status-dot" />
                                                    {isActive ? "Active" : "Deactivated"}
                                                </span>
                                            </td>

                                            <td>
                                                <button
                                                    type="button"
                                                    className={`customer-action-button ${isActive
                                                            ? "deactivate-button"
                                                            : "reactivate-button"
                                                        }`}
                                                    onClick={() => handleStatusChange(customer)}
                                                    disabled={
                                                        actionId === customer.id || !customer.id
                                                    }
                                                >
                                                    {actionId === customer.id
                                                        ? "Processing..."
                                                        : isActive
                                                            ? "Deactivate"
                                                            : "Reactivate"}
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>

                <div className="customer-table-footer">
                    Showing {customers.length} customer
                    {customers.length === 1 ? "" : "s"}
                </div>
            </section>
        </div>
    );
};

export default CustomerManagement;
