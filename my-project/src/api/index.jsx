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

// Get all designers
export function apiGetDesigner() {
    return newRequest({
        url: "/api/designer",
        method: "get",
    });
}

// Get one designer's profile by ID
export function apiGetDesignerById(id) {
    return newRequest({
        url: `/api/designer/${id}`,
        method: "get",
    });
}

// Get all Interior Design Projects (IDP)
export function apiGetProjects(params) {
    return newRequest({
        url: "/api/idp",
        method: "get",
        params,
    });
}

// Get one Interior Design Project by ID
export function apiGetProjectById(id) {
    return newRequest({
        url: `/api/idp/${id}`,
        method: "get",
    });
}