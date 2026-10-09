
import newRequest from "../utils/request";

// Login
export function apiLogin(data) {
    return newRequest({
        url: "/api/login",
        method: "post",
        data,
    });
}

// Register
export function apiRegistration(data) {
    return newRequest({
        url: "/api/register",
        method: "post",
        data,
    });
}

// Get user information
export function apiGetuser(userid) {
    return newRequest({
        url: "/api/getuser",
        method: "get",
        params: {
            userid,
        },
    });
}
