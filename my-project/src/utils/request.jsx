
import axios from "axios";
import cookie from "js-cookie";

const request = axios.create({
    baseURL: "https://314-production.up.railway.app",
    timeout: 20000,
    timeoutErrorMessage: "request timeout",
});

// Request interceptor
request.interceptors.request.use(
    (config) => {
        const token =
            localStorage.getItem("token") || cookie.get("token");

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (err) => Promise.reject(err)
);

// Response interceptor
request.interceptors.response.use(
    (response) => {
        console.log(
            "Backend response:",
            response.data
        );

        return response.data;
    },
    (err) => {
        console.error(
            "Request failed:",
            err.response?.data || err.message
        );

        return Promise.reject(err);
    }
);

// Unified request function
function newRequest(config) {
    const {
        url,
        method = "get",
        data,
        params,
    } = config;

    return request({
        url,
        method,
        ...(data !== undefined ? { data } : {}),
        ...(params !== undefined ? { params } : {}),
    });
}

export default newRequest;
