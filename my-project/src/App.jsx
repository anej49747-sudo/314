import { Suspense } from "react";
import {
  BrowserRouter,
  useRoutes
} from "react-router-dom";
import Router from "./router/Router";
import "./App.css";

function AppContent() {
  const routes = useRoutes(Router);

  return (
    <div className="App">
      <Suspense fallback={<div>Loading...</div>}>
        {routes}
      </Suspense>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App;