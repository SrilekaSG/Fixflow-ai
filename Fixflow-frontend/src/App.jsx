import { Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import NewDiagnosisPage from './pages/NewDiagnosisPage.jsx';
import HistoryPage from './pages/HistoryPage.jsx';
import DiagnosisDetailPage from './pages/DiagnosisDetailPage.jsx';
import KnowledgeBasePage from './pages/KnowledgeBasePage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="new" element={<NewDiagnosisPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route path="diagnoses/:id" element={<DiagnosisDetailPage />} />
        <Route path="knowledge" element={<KnowledgeBasePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}