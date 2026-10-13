import axios from "axios";
import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./reports.scss";

const API_BASE_URL = (
    import.meta.env.VITE_API_BASE_URL ||
    "https://314-production.up.railway.app"
).replace(/\/$/, "");

const PERIODS = [
    { value: "daily", label: "Daily" },
    { value: "weekly", label: "Weekly" },
    { value: "monthly", label: "Monthly" },
];

const METRICS = [
    {
        key: "customers",
        totalKey: "totalCustomers",
        label: "New Customers",
        color: "#315b48",
        icon: "users",
    },
    {
        key: "idps",
        totalKey: "totalIdps",
        label: "New IDPs",
        color: "#789c83",
        icon: "projects",
    },
    {
        key: "favourites",
        totalKey: "totalFavourites",
        label: "New Favourites",
        color: "#b4cbb9",
        icon: "favourites",
    },
];

function formatDate(dateString, period) {
    if (!dateString) return "";

    const date = new Date(`${dateString}T00:00:00`);

    if (Number.isNaN(date.getTime())) {
        return dateString;
    }

    if (period === "monthly") {
        return date.toLocaleDateString("en-SG", {
            month: "short",
            year: "2-digit",
        });
    }

    return date.toLocaleDateString("en-SG", {
        day: "2-digit",
        month: "short",
    });
}

function MetricIcon({ type }) {
    if (type === "users") {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="10" cy="7" r="4" />
                <path d="M20 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
        );
    }

    if (type === "projects") {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
            </svg>
        );
    }

    return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z" />
        </svg>
    );
}

export default function Reports() {
    const navigate = useNavigate();
    const location = useLocation();

    const userdata = useMemo(() => {
        if (location.state?.userdata) {
            return location.state.userdata;
        }

        try {
            return JSON.parse(localStorage.getItem("userdata") || "{}");
        } catch {
            return {};
        }
    }, [location.state]);

    const [period, setPeriod] = useState("daily");
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");

    const trend = useMemo(() => {
        return Array.isArray(report?.trend) ? report.trend : [];
    }, [report]);

    const maxValue = useMemo(() => {
        const values = trend.flatMap((item) => [
            Number(item.customers) || 0,
            Number(item.idps) || 0,
            Number(item.favourites) || 0,
        ]);

        return Math.max(1, ...values);
    }, [trend]);

    const goToManagerDashboard = () => {
        navigate("/dashboard", {
            state: {
                userdata: {
                    ...userdata,
                    role: "manager",
                },
            },
            replace: true,
        });
    };

    useEffect(() => {
        let active = true;
        let errorTimer;

        const fetchReport = async () => {
            setLoading(true);
            setErrorMessage("");

            try {
                const response = await axios.get(
                    `${API_BASE_URL}/api/reports`,
                    {
                        params: { period },
                        timeout: 15000,
                    }
                );

                if (!active) return;

                setReport(response.data);
            } catch (error) {
                if (!active) return;

                const message =
                    error.response?.data?.message ||
                    error.response?.data?.error ||
                    "Unable to load report. Please try again.";

                setErrorMessage(
                    typeof message === "string"
                        ? message
                        : "Unable to load report. Please try again."
                );

                errorTimer = setTimeout(() => {
                    if (active) {
                        setErrorMessage("");
                    }
                }, 2000);
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        };

        fetchReport();

        return () => {
            active = false;
            clearTimeout(errorTimer);
        };
    }, [period]);

    const getTotal = (metric) => {
        if (report?.[metric.totalKey] !== undefined) {
            return Number(report[metric.totalKey]) || 0;
        }

        return trend.reduce(
            (total, item) =>
                total + (Number(item[metric.key]) || 0),
            0
        );
    };

    const selectedPeriodLabel =
        PERIODS.find((item) => item.value === period)?.label || "Daily";

    return (
        <main className="reports-page">
            <div className="reports-container">
                <button
                    type="button"
                    className="reports-back-button"
                    onClick={goToManagerDashboard}
                >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M19 12H5" />
                        <path d="m12 19-7-7 7-7" />
                    </svg>
                    <span>Back to Manager Dashboard</span>
                </button>

                <header className="reports-header">
                    <div className="reports-title-section">
                        <div className="reports-title-line">
                            <span className="reports-title-accent" />
                            <h1>Platform Reports</h1>
                        </div>

                        <p>
                            Review platform activity and monitor customer
                            engagement over time.
                        </p>
                    </div>

                    <div className="reports-period-selector">
                        {PERIODS.map((item) => (
                            <button
                                key={item.value}
                                type="button"
                                className={`reports-period-button ${period === item.value ? "active" : ""
                                    }`}
                                onClick={() => setPeriod(item.value)}
                                disabled={loading && period === item.value}
                            >
                                {item.label}
                            </button>
                        ))}
                    </div>
                </header>

                <section className="reports-overview">
                    <div className="reports-section-heading">
                        <div>
                            <h2>Activity Overview</h2>
                            <p>
                                {selectedPeriodLabel} report ·{" "}
                                {period === "daily"
                                    ? "Last 7 days"
                                    : period === "weekly"
                                        ? "Last 8 weeks"
                                        : "Last 6 months"}
                            </p>
                        </div>

                    </div>

                    {loading ? (
                        <div className="reports-loading">
                            <span className="reports-loading-spinner" />
                            <span>Loading...</span>
                        </div>
                    ) : (
                        <div className="reports-metric-grid">
                            {METRICS.map((metric) => (
                                <article
                                    className="reports-metric-card"
                                    key={metric.key}
                                >
                                    <div className="reports-metric-top">
                                        <div className="reports-metric-info">
                                            <span className="reports-metric-label">
                                                {metric.label}
                                            </span>
                                        </div>

                                        <div
                                            className="reports-metric-icon"
                                            style={{
                                                backgroundColor: `${metric.color}20`,
                                                color: metric.color,
                                            }}
                                        >
                                            <MetricIcon type={metric.icon} />
                                        </div>
                                    </div>

                                    <div className="reports-metric-value">
                                        {getTotal(metric).toLocaleString("en-SG")}
                                    </div>

                                    <div className="reports-metric-footer">
                                        <span className="reports-metric-dot" />
                                        <span>Recorded in selected period</span>
                                    </div>
                                </article>
                            ))}
                        </div>
                    )}
                </section>

                <section className="reports-chart-section">
                    <div className="reports-chart-header">
                        <div>
                            <h2>Activity Trends</h2>
                            <p>
                                Compare new customers, IDPs and favourites
                                across each period.
                            </p>
                        </div>

                    </div>

                    {loading ? (
                        <div className="reports-chart-loading">
                            <span className="reports-loading-spinner" />
                            <span>Loading...</span>
                        </div>
                    ) : trend.length === 0 ? (
                        <div className="reports-empty">
                            <div className="reports-empty-icon">
                                <MetricIcon type="projects" />
                            </div>
                            <h3>No activity data</h3>
                            <p>
                                No report records were returned for this period.
                            </p>
                        </div>
                    ) : (
                        <>
                            <div className="reports-chart-legend">
                                {METRICS.map((metric) => (
                                    <div
                                        className="reports-legend-item"
                                        key={metric.key}
                                    >
                                        <span
                                            className="reports-legend-color"
                                            style={{
                                                backgroundColor: metric.color,
                                            }}
                                        />
                                        <span>{metric.label}</span>
                                    </div>
                                ))}
                            </div>

                            <div className="reports-chart">
                                <div className="reports-y-axis">
                                    {[1, 0.75, 0.5, 0.25, 0].map(
                                        (fraction) => (
                                            <span key={fraction}>
                                                {Math.round(maxValue * fraction)}
                                            </span>
                                        )
                                    )}
                                </div>

                                <div className="reports-chart-content">
                                    <div className="reports-chart-grid">
                                        {[0, 1, 2, 3, 4].map((line) => (
                                            <span key={line} />
                                        ))}
                                    </div>

                                    <div className="reports-chart-columns">
                                        {trend.map((item, index) => (
                                            <div
                                                className="reports-chart-column"
                                                key={`${item.date}-${index}`}
                                            >
                                                <div className="reports-bar-group">
                                                    {METRICS.map((metric) => {
                                                        const value =
                                                            Number(item[metric.key]) ||
                                                            0;

                                                        const height =
                                                            (value / maxValue) *
                                                            100;

                                                        return (
                                                            <div
                                                                className="reports-bar-wrapper"
                                                                key={metric.key}
                                                                title={`${metric.label}: ${value}`}
                                                            >
                                                                <span className="reports-bar-value">
                                                                    {value > 0
                                                                        ? value
                                                                        : ""}
                                                                </span>

                                                                <div
                                                                    className={`reports-bar ${value === 0
                                                                        ? "zero"
                                                                        : ""
                                                                        }`}
                                                                    style={{
                                                                        height: `${height}%`,
                                                                        backgroundColor:
                                                                            metric.color,
                                                                    }}
                                                                />
                                                            </div>
                                                        );
                                                    })}
                                                </div>

                                                <span className="reports-chart-date">
                                                    {formatDate(item.date, period)}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            <div className="reports-chart-footer">
                                <span>
                                    {report?.startDate && report?.endDate
                                        ? `From ${report.startDate} to ${report.endDate}`
                                        : "Based on recorded creation timestamps"}
                                </span>
                                <span>{trend.length} periods</span>
                            </div>
                        </>
                    )}
                </section>
            </div>

            {errorMessage && (
                <div className="reports-toast" role="alert">
                    <span className="reports-toast-icon">!</span>
                    <span>{errorMessage}</span>
                </div>
            )}
        </main>
    );
}