import { Link, NavLink, useNavigate } from "react-router-dom";
import { Heart, Menu, PawPrint } from "lucide-react";
import { useAuth } from "../context/AuthContext.jsx";

export default function Layout({ children }) {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  function handleSignOut() {
    signOut();
    navigate("/");
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <Link className="brand" to="/" aria-label="Paws and Homes home">
          <span className="brand-mark"><PawPrint size={21} fill="currentColor" /></span>
          <span>paws<span className="brand-light">&</span>homes</span>
        </Link>
        <nav className="main-nav" aria-label="Main navigation">
          <NavLink to="/pets">Find a pet</NavLink>
          <a href="/#our-story">Our story</a>
          <a href="/#how-it-works">How it works</a>
        </nav>
        <div className="header-actions">
          {user ? (
            <>
              <Link className="header-dashboard" to="/dashboard">Hi, {user.name.split(" ")[0]}</Link>
              <button className="button button-dark button-small" onClick={handleSignOut}>Sign out</button>
            </>
          ) : (
            <>
              <Link className="header-login" to="/login">Log in</Link>
              <Link className="button button-dark button-small" to="/signup">Get started <Heart size={14} /></Link>
            </>
          )}
        </div>
        <button className="mobile-menu" aria-label="Open menu" onClick={() => navigate("/pets")}><Menu /></button>
      </header>
      <main>{children}</main>
      <footer className="site-footer">
        <Link className="brand footer-brand" to="/"><span className="brand-mark"><PawPrint size={19} fill="currentColor" /></span><span>paws<span className="brand-light">&</span>homes</span></Link>
        <p>Every pet deserves a place to call home.</p>
        <span>© {new Date().getFullYear()} Paws & Homes</span>
      </footer>
    </div>
  );
}
