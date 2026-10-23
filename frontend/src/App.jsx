import { ConfigProvider } from 'antd'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import AuthLayout from './components/AuthLayout.jsx'
import HomePage from './pages/HomePage.jsx'
import AddPortfolioProjectPage from './pages/AddPortfolioProjectPage.jsx'
import CustomerPage from './pages/CustomerPage.jsx'
import CustomerAccountSettingsPage from './pages/CustomerAccountSettingsPage.jsx'
import CustomerFavouritesPage from './pages/CustomerFavouritesPage.jsx'
import CompareDesignersPage from './pages/CompareDesignersPage.jsx'
import CompanyInformationPage from './pages/CompanyInformationPage.jsx'
import CreateDesignerPage from './pages/CreateDesignerPage.jsx'
import AdminAccountSearchPage from './pages/AdminAccountSearchPage.jsx'
import AdminDesignerReportsPage from './pages/AdminDesignerReportsPage.jsx'
import DesignerChangePasswordPage from './pages/DesignerChangePasswordPage.jsx'
import DesignerPage from './pages/DesignerPage.jsx'
import DesignerAccountSettingsPage from './pages/DesignerAccountSettingsPage.jsx'
import DesignerPortfolioPage from './pages/DesignerPortfolioPage.jsx'
import DesignerDirectoryPage from './pages/DesignerDirectoryPage.jsx'
import DesignerProfilePage from './pages/DesignerProfilePage.jsx'
import './designer/designerProfile.css'
import EditPortfolioProjectPage from './pages/EditPortfolioProjectPage.jsx'
import PublicProjectDetailsPage from './pages/PublicProjectDetailsPage.jsx'
import ProjectSearchPage from './pages/ProjectSearchPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import ProjectCategoriesPage from './pages/ProjectCategoriesPage.jsx'
import ActivityReportsPage from './pages/ActivityReportsPage.jsx'
import ManagerProjectsPage from './pages/ManagerProjectsPage.jsx'
import ArticlesPage from './pages/ArticlesPage.jsx'
import ArticleDetailsPage from './pages/ArticleDetailsPage.jsx'
import ManagerArticlesPage from './pages/ManagerArticlesPage.jsx'
import ManagerRecognitionsPage from './pages/ManagerRecognitionsPage.jsx'

const theme = {
  token: {
    colorPrimary: '#365B82',
    colorText: '#1D2939',
    colorTextSecondary: '#526581',
    colorBorder: '#D0D8E2',
    colorError: '#B42318',
    colorSuccess: '#067647',
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    borderRadius: 8,
    controlHeight: 44,
  },
}

export default function App() {
  return (
    <ConfigProvider theme={theme}>
      <BrowserRouter>
        <Routes>
          <Route element={<AuthLayout />}>
            <Route index element={<HomePage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="designers" element={<DesignerDirectoryPage />} />
            <Route path="designers/:designerId" element={<DesignerProfilePage />} />
            <Route path="projects" element={<ProjectSearchPage />} />
            <Route path="projects/:projectId" element={<PublicProjectDetailsPage />} />
            <Route path="articles" element={<ArticlesPage />} />
            <Route path="articles/:articleId" element={<ArticleDetailsPage />} />
            <Route path="customer" element={<CustomerPage />} />
            <Route path="customer/account" element={<CustomerAccountSettingsPage />} />
            <Route path="customer/favourites" element={<CustomerFavouritesPage />} />
            <Route path="customer/compare" element={<CompareDesignersPage />} />
            <Route path="designer" element={<DesignerPage />} />
            <Route path="designer/account" element={<DesignerAccountSettingsPage />} />
            <Route path="designer/change-password" element={<DesignerChangePasswordPage />} />
            <Route path="designer/company-information" element={<CompanyInformationPage />} />
            <Route path="designer/portfolio" element={<DesignerPortfolioPage />} />
            <Route path="designer/portfolio/new" element={<AddPortfolioProjectPage />} />
            <Route path="designer/portfolio/:projectId/edit" element={<EditPortfolioProjectPage />} />
            <Route path="admin/designers/new" element={<CreateDesignerPage />} />
            <Route path="admin/accounts" element={<AdminAccountSearchPage />} />
            <Route path="admin/reports" element={<AdminDesignerReportsPage />} />
            <Route path="manager/categories" element={<ProjectCategoriesPage />} />
            <Route path="manager/projects" element={<ManagerProjectsPage />} />
            <Route path="manager/articles" element={<ManagerArticlesPage />} />
            <Route path="manager/recognitions" element={<ManagerRecognitionsPage />} />
            <Route path="manager/reports" element={<ActivityReportsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  )
}
