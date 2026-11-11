import { Navigate, Route, Routes } from 'react-router-dom';
import AppShell from './components/AppShell';
import CandidatesScreen from './screens/CandidatesScreen';
import CatalogScreen from './screens/CatalogScreen';
import ImportScreen from './screens/ImportScreen';
import LoginScreen from './screens/LoginScreen';
import NoticesScreen from './screens/NoticesScreen';
import PeopleScreen from './screens/PeopleScreen';
import PersonScreen from './screens/PersonScreen';
import PostFormScreen from './screens/PostFormScreen';
import PostScreen from './screens/PostScreen';
import PostsScreen from './screens/PostsScreen';
import ProjectScreen from './screens/ProjectScreen';
import RecommendationsScreen from './screens/RecommendationsScreen';

/**
 * The routes the prototype's chrome holds. The board at `/posts` is where the app lands: the
 * projects, the posts and one post's detail in one view. A post's own screen edits it, its
 * candidate search reads the pool, and the project screen keeps the end-of-project deep link.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginScreen />} />
      <Route element={<AppShell />}>
        <Route path="/" element={<Navigate to="/posts" replace />} />
        <Route path="/posts" element={<PostsScreen />} />
        <Route path="/posts/:id/candidates" element={<CandidatesScreen />} />
        <Route path="/candidates" element={<CandidatesScreen />} />
        <Route path="/posts/:postId" element={<PostScreen />} />
        <Route path="/recommendations" element={<RecommendationsScreen />} />
        <Route path="/projects" element={<Navigate to="/posts" replace />} />
        <Route path="/projects/:projectId" element={<ProjectScreen />} />
        <Route path="/projects/:projectId/posts/new" element={<PostFormScreen />} />
        <Route path="/imports" element={<ImportScreen />} />
        <Route path="/people" element={<PeopleScreen />} />
        <Route path="/people/:employeeNo" element={<PersonScreen />} />
        <Route path="/catalogs" element={<CatalogScreen />} />
        <Route path="/notices" element={<NoticesScreen />} />
      </Route>
      <Route path="*" element={<Navigate to="/posts" replace />} />
    </Routes>
  );
}
