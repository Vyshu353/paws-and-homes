import { ArrowLeft, Heart } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";

export default function Login() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "", role: "USER" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await signIn(form);
      navigate(location.state?.from || "/dashboard", { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return <section className="auth-page"><div className="auth-art"><img src="https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=1200&q=85" alt="A friendly dog looking at the camera" /><div className="auth-art-copy"><span>There's always room<br />for one more.</span><Heart size={26} fill="currentColor" /></div></div><div className="auth-panel"><Link to="/" className="back-link"><ArrowLeft size={16} /> Back home</Link><div className="auth-form-wrap"><span className="eyebrow">WELCOME BACK</span><h1>Good to see<br />you <em>again.</em></h1><p className="auth-subtitle">Sign in to pick up where your new best friend is waiting.</p><form onSubmit={handleSubmit} className="form-stack"><label>Sign in as<select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}><option value="USER">Adopter</option><option value="ADMIN">Administrator</option></select></label><label>Email address<input type="email" autoComplete="email" placeholder="you@example.com" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label><label>Password<input type="password" autoComplete="current-password" placeholder="Your password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="button button-dark button-full" disabled={busy}>{busy ? "Signing you in…" : `Sign in as ${form.role === "ADMIN" ? "administrator" : "adopter"}`} <Heart size={16} /></button></form><p className="auth-switch">New around here? <Link to="/signup">Create an account</Link></p></div></div></section>;
}
