import { Children, lazy } from "react";
import { Navigate } from "react-router-dom";
const Home = lazy(() => import("../pages/Home/home"));
const Login = lazy(() => import("../pages/Login/login"));



const Router = [
    {
        path: "/",
        element: <Navigate to="/home" />,
    },
    {
        path: "/home",
        stuff: {},
        element: <Home />,
    },
    {
        path: "/login",
        stuff: { title: "Login" },
        element: <Login />,
    },

    //   {
    //     path: "/profile/:id/*",
    //     stuff: { title: "User profile" },
    //     element: <Profile />,
    //     children: [
    //       {
    //         path: "Report",
    //         stuff: { title: "Report" },
    //         element: <Report />,
    //       },

    //     ],
    //   },

];
export default Router;