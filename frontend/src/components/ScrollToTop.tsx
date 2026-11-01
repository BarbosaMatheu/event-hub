import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/** New page => start at the top (the browser keeps the old scroll position in a SPA). */
export function ScrollToTop() {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
}
