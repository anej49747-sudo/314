import { lazy } from "react";
import { Navigate } from "react-router-dom";

const Home = lazy(() => import("../pages/Home/home"));
const Login = lazy(() => import("../pages/Login/login"));
const Profile = lazy(() => import("../pages/profile/profile"));
const Dashboard = lazy(() => import("../pages/dashboard/dashboard.jsx"));

const Designer = lazy(
  () => import("../pages/Designer/designer")
);

const DesignerProfile = lazy(
  () => import("../pages/Designer/designerProfile")
);

const IDP = lazy(
  () => import("../pages/IDP/idp")
);

const IDPDetail = lazy(
  () => import("../pages/IDP/idpDetail")
);

const Router = [
  {
    path: "/",
    element: <Navigate to="/home" replace />,
  },
  {
    path: "/home",
    element: <Home />,
  },
  {
    path: "/login",
    element: <Login />,
  },
  {
    path: "/dashboard",
    element: <Dashboard />,
  },
  {
    path: "/profile",
    element: <Profile />,
  },
  {
    path: "/designer",
    element: <Designer />,
  },
  {
    path: "/designer/:id",
    element: <DesignerProfile />,
  },
  {
    path: "/idp",
    element: <IDP />,
  },
  {
    path: "/idp/:id",
    element: <IDPDetail />,
  },
];

export default Router;