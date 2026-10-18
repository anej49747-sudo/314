import { ConfigProvider } from 'antd'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import AuthLayout from './components/AuthLayout.jsx'
import CustomerPage from './pages/CustomerPage.jsx'
import CreateDesignerPage from './pages/CreateDesignerPage.jsx'
import DesignerChangePasswordPage from './pages/DesignerChangePasswordPage.jsx'
import DesignerPage from './pages/DesignerPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'

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
            <Route index element={<Navigate to="/register" replace />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="customer" element={<CustomerPage />} />
            <Route path="designer" element={<DesignerPage />} />
            <Route path="designer/change-password" element={<DesignerChangePasswordPage />} />
            <Route path="admin/designers/new" element={<CreateDesignerPage />} />
            <Route path="*" element={<Navigate to="/register" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  )
}
