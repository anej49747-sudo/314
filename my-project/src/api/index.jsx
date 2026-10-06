import newRequest from "../utils/request";
export function apiLogin(data) {
    return newRequest({
        url: "/api/login",
        method: "post",
        data
    })
}
export function apiRegistration(data) {
    return newRequest({
        url: "/api/register",
        method: "post",
        data
    })
}

export function apiGetuser(data) {
    return newRequest({
        url: "/api/getuser",
        method: "get",
        data
    })
}