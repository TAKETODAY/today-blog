import { Outlet, useAccess } from '@umijs/max';
import Forbidden from '@/pages/403';

export default function RequireLogin() {
  const access = useAccess();
  return access.isLoggedIn ? <Outlet /> : <Forbidden />;
}
