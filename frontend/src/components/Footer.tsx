export function Footer() {
  return (
    <footer className="mt-16 border-t border-slate-200 py-8 text-sm text-slate-500 dark:border-slate-800 dark:text-slate-400">
      <div className="container-page flex flex-col items-center justify-between gap-2 sm:flex-row">
        <p>© {new Date().getFullYear()} EventHub. A full-stack portfolio project.</p>
        <p>Spring Boot · PostgreSQL · React</p>
      </div>
    </footer>
  );
}
