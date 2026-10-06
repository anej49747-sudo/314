import axios from "axios";
import cookie from "js-cookie";

const request = axios.create({
    baseURL: "http://localhost:8080",
    // baseURL: "http://127.0.0.1:5000",
    timeout: 20000,
    timeoutErrorMessage: "request timeout",
});
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("token") || cookie.get("token");
        if (token) {
            config.headers.authorization = `Bearer ${token}`;
        }
        return config;
    },
    (err) => {
        return Promise.reject(err);
    },
);
request.interceptors.response.use(
    (config) => {
        console.log(
            "!!! The interceptor got the original response from the backend:",
            // config.data,
        );
        return config.data;
    },
    (err) => {

        console.error("!!! the interceptor caught a network error:", err);
        const msg = err.response?.data?.msg || "server error";
        return Promise.reject(err);
    },
);
function newRequest(config) {
    const { url, method = "get", data = {}, params = {} } = config;
    return request({
        url,
        method,
        data,
        params,
    });
}
export default newRequest;