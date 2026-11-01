import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { Layout } from './components/Layout';
import { ScrollToTop } from './components/ScrollToTop';
import { Spinner } from './components/Spinner';
import EventDetail from './pages/EventDetail';
import Events from './pages/Events';
import Home from './pages/Home';
import Login from './pages/Login';
import MyOrders from './pages/MyOrders';
import NotFound from './pages/NotFound';
import Register from './pages/Register';

// The admin area is only needed by administrators, so it is a separate chunk loaded on demand.
const AdminLayout = lazy(() => import('./pages/admin/AdminLayout'));
const Dashboard = lazy(() => import('./pages/admin/Dashboard'));
const AdminEvents = lazy(() => import('./pages/admin/AdminEvents'));
const AdminCategories = lazy(() => import('./pages/admin/AdminCategories'));
const AdminOrders = lazy(() => import('./pages/admin/AdminOrders'));

export default function App() {
  return (
    <>
      <ScrollToTop />
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="events" element={<Events />} />
          <Route path="events/:id" element={<EventDetail />} />
          <Route path="login" element={<Login />} />
          <Route path="register" element={<Register />} />
          <Route
            path="orders"
            element={
              <ProtectedRoute>
                <MyOrders />
              </ProtectedRoute>
            }
          />
          <Route
            path="admin"
            element={
              <ProtectedRoute role="ADMIN">
                <Suspense fallback={<Spinner />}>
                  <AdminLayout />
                </Suspense>
              </ProtectedRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="events" element={<AdminEvents />} />
            <Route path="categories" element={<AdminCategories />} />
            <Route path="orders" element={<AdminOrders />} />
          </Route>
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </>
  );
}
