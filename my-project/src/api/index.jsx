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

// Get users' information
export function apiGetuser(userid) {
    return newRequest({
        url: "/api/getuser",
        method: "get",
        params: {
            userId: userid,
        },
    });
}

// Get designers
export function apiGetDesigner() {
    return newRequest({
        url: "/api/designer",
        method: "get",
    });
}

// Get designers' profile
export function apiGetDesignerById(id) {
    return newRequest({
        url: `/api/designer/${id}`,
        method: "get",
    });
}

// Get IDP list
export function apiGetProjects(params) {
    return newRequest({
        url: "/api/idp",
        method: "get",
        params,
    });
}

// Get per IDP
export function apiGetProjectById(id) {
    return newRequest({
        url: `/api/idp/${id}`,
        method: "get",
    });
}

// Update designers' profile
export function apiUpdateDesignerProfile(id, data) {
    return newRequest({
        url: `/api/designer/${encodeURIComponent(id)}/profile`,
        method: "put",
        data,
    });
}

// for manager, customer, admin
export function apiUpdateUserProfile(id, data) {
    return newRequest({
        url: `/api/user/${id}/profile`,
        method: "put",
        data,
    });

    // Add IDP to favourites
    export function apiAddFavourite(customerId, idpId) {
        return newRequest({
            url: `/api/favourites/${encodeURIComponent(customerId)}/${encodeURIComponent(idpId)}`,
            method: "post",
        });
    }

    // Get customer's favourite IDPs
    export function apiGetFavourites(customerId) {
        return newRequest({
            url: `/api/favourites/${encodeURIComponent(customerId)}`,
            method: "get",
        });
    }

    // Remove IDP from favourites
    export function apiRemoveFavourite(customerId, idpId) {
        return newRequest({
            url: `/api/favourites/${encodeURIComponent(customerId)}/${encodeURIComponent(idpId)}`,
            method: "delete",
        });
    }

}