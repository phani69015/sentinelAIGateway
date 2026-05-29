import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import QueryPage from './pages/QueryPage';
import AuditLog from './pages/AuditLog';
import AuditDetail from './pages/AuditDetail';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="query" element={<QueryPage />} />
          <Route path="audit" element={<AuditLog />} />
          <Route path="audit/:id" element={<AuditDetail />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
