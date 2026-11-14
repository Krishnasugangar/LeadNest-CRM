import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout, RequireAdmin, RequireAuth } from './ui';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import LeadsPage from './pages/LeadsPage';
import LeadDetailPage from './pages/LeadDetailPage';
import DealsPage from './pages/DealsPage';
import FollowUpsPage from './pages/FollowUpsPage';
import AgentsPage from './pages/AgentsPage';
import ProfilePage from './pages/ProfilePage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<Layout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/leads" element={<LeadsPage />} />
          <Route path="/leads/:id" element={<LeadDetailPage />} />
          <Route path="/deals" element={<DealsPage />} />
          <Route path="/follow-ups" element={<FollowUpsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route element={<RequireAdmin />}>
            <Route path="/agents" element={<AgentsPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
