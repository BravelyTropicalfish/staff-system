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
import ProjectScreen from './screens/ProjectScreen';
import ProjectsScreen from './screens/ProjectsScreen';

/** Where the shell lands: what the app is for, and what the sidebar holds. */
function Overview() {
  return (
    <>
      <div className="page-head">
        <div>
          <h1>Staffing</h1>
          <div className="lede">
            Match people to project posts: import the people master report, search who passes the
            gates, recommend them to the project lead, and keep the gap numbers honest.
          </div>
        </div>
      </div>
      <div className="grid-3">
        <section className="card">
          <div className="card-head">
            <h2>Delivery</h2>
          </div>
          <div className="card-body stack">
            <p className="note">
              Projects list their posts, the gap view sums seats chosen against seats needed, and the
              project lead ends a project once — which releases everyone on it.
            </p>
          </div>
        </section>
        <section className="card">
          <div className="card-head">
            <h2>People</h2>
          </div>
          <div className="card-body stack">
            <p className="note">
              The pool is read-only: person data arrives by import, bands are named rather than
              scored, and a search lists everyone who passes the gates by name.
            </p>
          </div>
        </section>
        <section className="card">
          <div className="card-head">
            <h2>Evidence</h2>
          </div>
          <div className="card-body stack">
            <p className="note">
              Every screen reads what the server answers, and the five CI checks are the merge
              condition — no screen decides eligibility on its own.
            </p>
          </div>
        </section>
      </div>
    </>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginScreen />} />
      <Route element={<AppShell />}>
        <Route path="/" element={<Overview />} />
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
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
