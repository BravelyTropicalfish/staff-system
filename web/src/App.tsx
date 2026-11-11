import { Link, Navigate, Route, Routes } from 'react-router-dom';
import CandidatesScreen from './screens/CandidatesScreen';
import CatalogScreen from './screens/CatalogScreen';
import ImportScreen from './screens/ImportScreen';
import NoticesScreen from './screens/NoticesScreen';
import PeopleScreen from './screens/PeopleScreen';
import PersonScreen from './screens/PersonScreen';
import PostFormScreen from './screens/PostFormScreen';
import PostScreen from './screens/PostScreen';
import ProjectScreen from './screens/ProjectScreen';
import ProjectsScreen from './screens/ProjectsScreen';

function Shell() {
  return (
    <main className="mx-auto max-w-5xl p-8">
      <h1 className="text-2xl font-semibold tracking-tight">Staffing</h1>
      <p className="mt-2 text-sm text-slate-600">
        The workbook, the catalogs, the projects, the notices and the post screens are reachable
        from here.
      </p>
      <ul className="mt-4 space-y-1 text-sm">
        <li>
          <Link className="text-slate-800 underline" to="/imports">
            Import the workbook
          </Link>
        </li>
        <li>
          <Link className="text-slate-800 underline" to="/people">
            The people pool
          </Link>
        </li>
        <li>
          <Link className="text-slate-800 underline" to="/projects">
            Projects and their posts
          </Link>
        </li>
        <li>
          <Link className="text-slate-800 underline" to="/catalogs">
            Roles and skills
          </Link>
        </li>
        <li>
          <Link className="text-slate-800 underline" to="/notices">
            Notices
          </Link>
        </li>
      </ul>
    </main>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Shell />} />
      <Route path="/imports" element={<ImportScreen />} />
      <Route path="/people" element={<PeopleScreen />} />
      <Route path="/people/:employeeNo" element={<PersonScreen />} />
      <Route path="/projects" element={<ProjectsScreen />} />
      <Route path="/projects/:projectId" element={<ProjectScreen />} />
      <Route path="/catalogs" element={<CatalogScreen />} />
      <Route path="/posts/:id/candidates" element={<CandidatesScreen />} />
      <Route path="/posts/:postId" element={<PostScreen />} />
      <Route path="/projects/:projectId/posts/new" element={<PostFormScreen />} />
      <Route path="/notices" element={<NoticesScreen />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
