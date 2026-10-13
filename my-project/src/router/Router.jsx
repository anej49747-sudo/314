import { lazy } from "react";
import { Navigate } from "react-router-dom";

const Home = lazy(() => import("../pages/Home/home"));
const Login = lazy(() => import("../pages/Login/login"));
const Profile = lazy(() => import("../pages/profile/profile"));
const Dashboard = lazy(() => import("../pages/dashboard/dashboard.jsx"));

const Designer = lazy(() => import("../pages/Designer/designer"));
const DesignerProfile = lazy(
  () => import("../pages/Designer/designerProfile")
);

const IDP = lazy(() => import("../pages/IDP/idp"));
const IDPDetail = lazy(() => import("../pages/IDP/idpDetail"));

const CreateDesigner = lazy(
  () => import("../pages/CreateDesigner/createdesigner")
);

const CustomerManagement = lazy(
  () => import("../pages/CustomerManagement/customermanagement")
);

const CompanyInformation = lazy(
  () => import("../pages/CompanyInformation/companyinformation")
);

const DesignerIDP = lazy(() => import("../pages/Designer/IDP"));

const Favourites = lazy(
  () => import("../pages/Favourites/favourites")
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
    path: "/designer/company-information",
    element: <CompanyInformation />,
  },
  {
    path: "/designer/idp",
    element: <DesignerIDP />,
  },
  {
    path: "/idp",
    element: <IDP />,
  },
  {
    path: "/idp/:id",
    element: <IDPDetail />,
  },
  {
    path: "/admin/create-designer",
    element: <CreateDesigner />,
  },
  {
    path: "/customermanagement",
    element: <CustomerManagement />,
  },
  {
    path: "/favourites",
    element: <Favourites />,
  },
];

export default Router;