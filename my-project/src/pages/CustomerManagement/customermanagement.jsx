import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import newRequest from "../../utils/request";
import "./customermanagement.scss";

function CustomerManagement() {
    const navigate = useNavigate();
    const location = useLocation();
    const userdata = location.state?.userdata || {};

    const [customers, setCustomers] = useState([]);
    const [username, setUsername] = useState("");
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState("");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    useEffect(() => {
        fetchCustomers();
    }, []);

    const fetchCustomers = async (searchUsername = "") => {
        setLoading(true);
        setError("");

        try {
            const searchValue = searchUsername.trim();

            const response = await newRequest({
                url: searchValue
                    ? "/api/admin/customers/search"
                    : "/api/admin/customers",
                method: "get",
                ...(searchValue ? { params: { username: searchValue } } : {}),
            });

            if (!response || response.status !== "success") {
                throw new Error(
                    response?.message || "Failed to load customer accounts."
                );
            }

            setCustomers(
                Array.isArray(response.customers) ? response.customers : []
            );
        } catch (err) {
            setCustomers([]);
            setError(
                err.response?.data?.message ||
                err.message ||
                "Unable to load customer accounts. Please try again."
            );
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async (event) => {
        event.preventDefault();
        setSuccess("");
        await fetchCustomers(username);
    };

    const handleStatusChange = async (customer) => {
        const isActive = customer.active !== false;
        const action = isActive ? "deactivate" : "reactivate";

        const confirmed = window.confirm(
            isActive
                ? `Are you sure you want to deactivate "${customer.username}"?`
                : `Are you sure you want to reactivate "${customer.username}"?`
        );

        if (!confirmed) return;

        const customerId = customer.id || customer._id;

        if (!customerId) {
            setError("Customer ID is missing. Unable to update this account.");
            return;
        }

        setActionLoading(customerId);
        setError("");
        setSuccess("");

        try {
            const response = await newRequest({
                url: `/api/admin/customers/${customerId}/${action}`,
                method: "put",
            });

            if (!response || response.status !== "success") {
                throw new Error(
                    response?.message || "Unable to update customer account status."
                );
            }

            await fetchCustomers(username);

            setSuccess(
                response.message ||
                (action === "deactivate"
                    ? "Customer account deactivated successfully."
                    : "Customer account reactivated successfully.")
            );
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.message ||
                "Unable to update customer account status. Please try again."
            );
        } finally {
            setActionLoading("");
        }
    };

    const getAccountStatus = (customer) => {
        return customer.active === false ? "Inactive" : "Active";
    };

    return (
        <div className="customer-management">
            <button
                type="button"
                className="back-dashboard-btn"
                onClick={() =>
                    navigate("/dashboard", {
                        state: { userdata },
                    })
                }
            >
                ← Back to Dashboard
            </button>

            <div className="customer-management-header">
                <div>
                    <p className="customer-management-label">USER ADMINISTRATION</p>
                    <h1>Customer Management</h1>
                </div>
            </div>

            <section className="customer-search-card">
                <form className="customer-search-form" onSubmit={handleSearch}>
                    <div className="customer-search-input-group">
                        <label htmlFor="customer-username">Search by Username</label>
                        <input
                            id="customer-username"
                            type="text"
                            value={username}
                            onChange={(event) => setUsername(event.target.value)}
                            placeholder="Enter customer username"
                        />
                    </div>

                    <div className="customer-search-actions">
                        <button
                            type="submit"
                            className="customer-search-btn"
                            disabled={loading}
                        >
                            {loading ? "Searching..." : "Search"}
                        </button>
                    </div>
                </form>
            </section>

            {success && (
                <div className="customer-success-message" role="status">
                    {success}
                </div>
            )}

            {error && (
                <div className="customer-error-message" role="alert">
                    {error}
                </div>
            )}

            <section className="customer-list-card">
                <div className="customer-list-header">
                    <div>
                        <h2>Customer List</h2>
                        <p>
                            {loading
                                ? "Loading customer accounts..."
                                : `${customers.length} customer${customers.length === 1 ? "" : "s"
                                } found`}
                        </p>
                    </div>
                </div>

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
                                    <td colSpan="6" className="customer-empty-message">
                                        Loading customer accounts...
                                    </td>
                                </tr>
                            ) : customers.length === 0 ? (
                                <tr>
                                    <td colSpan="6" className="customer-empty-message">
                                        No customer accounts found.
                                    </td>
                                </tr>
                            ) : (
                                customers.map((customer, index) => {
                                    const isActive = customer.active !== false;
                                    const customerId = customer.id || customer._id;
                                    const isUpdating = actionLoading === customerId;

                                    return (
                                        <tr key={customerId || customer.username || index}>
                                            <td>{index + 1}</td>
                                            <td>{customer.username || "-"}</td>
                                            <td>
                                                {customer.name ||
                                                    customer.fullName ||
                                                    customer.fullname ||
                                                    "-"}
                                            </td>
                                            <td>
                                                <span className="customer-role">
                                                    {customer.role || "customer"}
                                                </span>
                                            </td>
                                            <td>
                                                <span
                                                    className={`customer-status ${isActive
                                                        ? "customer-status-active"
                                                        : "customer-status-inactive"
                                                        }`}
                                                >
                                                    {getAccountStatus(customer)}
                                                </span>
                                            </td>
                                            <td>
                                                <button
                                                    type="button"
                                                    className={`customer-action-btn ${isActive
                                                        ? "customer-deactivate-btn"
                                                        : "customer-reactivate-btn"
                                                        }`}
                                                    onClick={() => handleStatusChange(customer)}
                                                    disabled={isUpdating || !customerId}
                                                >
                                                    {isUpdating
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
            </section>
        </div>
    );
}

export default CustomerManagement;