import { NavLink } from 'react-router-dom';

const links = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/new', label: 'Analyze an error' },
  { to: '/history', label: 'History' },
  { to: '/knowledge', label: 'Knowledge base' },
];

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand">FixFlow AI</div>
      <nav aria-label="Main">
        <ul className="nav-list">
          {links.map((link) => (
            <li key={link.to}>
              <NavLink
                to={link.to}
                end={link.end}
                className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              >
                {link.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <p className="sidebar-note">
        Local single-user demo. Do not expose this app publicly without access controls.
      </p>
    </aside>
  );
}