import { Children, lazy } from "react";
import { Navigate } from "react-router-dom";

const Home = lazy(() => import("../pages/Home/home"));
const Login = lazy(() => import("../pages/Login/login"));
const Profile = lazy(() => import("../pages/profile/profile"));
const Dashboard = lazy(() => import("../pages/dashboard/dashboard.jsx"));

//const Admin = lazy(() => import("../pages/Admin/admin.jsx"));
//const Customer = lazy(() => import("../pages/Customer/customer"));
//const Designer = lazy(() => import("../pages/Designer/designer"),);
//const Manager = lazy(() => import("../pages/Management/management"));

const Router = [
  {
    path: "/",
    element: <Navigate to="/home" replace />,
  },

  {
    path: "/home",
    stuff: { title: "Home" },
    element: <Home />,
  },

  {
    path: "/dashboard",
    element: <Dashboard />,
  },

  {
    path: "/login",
    stuff: { title: "Login" },
    element: <Login />,
  },

  {
    path: "/profile",
    element: <Profile />,
  },

  /* {
     path: "/admin",
     stuff: { title: "Admin" },
     element: <Admin />,
   },
 
   {
     path: "/designer",
     stuff: { title: "Designer" },
     element: <Designer />,
   },
 
   {
     path: "/customer",
     stuff: { title: "Customer" },
     element: <Customer />,
   },
 
   {
     path: "/manager",
     stuff: { title: "Manager" },
     element: <Manager />,
   },*/
];

export default Router;
