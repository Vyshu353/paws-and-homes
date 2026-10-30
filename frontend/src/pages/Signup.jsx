import { ArrowLeft, Heart } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";

export default function Signup() {
  const { signUp } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await signUp(form);
      navigate("/dashboard");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return <section className="auth-page"><div className="auth-art"><img src="https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&w=1200&q=85" alt="A happy dog ready to meet its new family" /><div className="auth-art-copy"><span>Best friends<br />start right here.</span><Heart size={26} fill="currentColor" /></div></div><div className="auth-panel"><Link to="/" className="back-link"><ArrowLeft size={16} /> Back home</Link><div className="auth-form-wrap"><span className="eyebrow">LET'S MAKE A MATCH</span><h1>Make room<br />for <em>love.</em></h1><p className="auth-subtitle">Create an account and meet your new favorite hello.</p><form onSubmit={handleSubmit} className="form-stack"><label>Your name<input type="text" autoComplete="name" placeholder="Jamie Parker" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required minLength={2} maxLength={100} /></label><label>Email address<input type="email" autoComplete="email" placeholder="you@example.com" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label><label>Password<input type="password" autoComplete="new-password" placeholder="At least 8 characters" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={8} /></label>{error && <p className="form-error" role="alert">{error}</p>}<button className="button button-dark button-full" disabled={busy}>{busy ? "Creating your account…" : "Create account"} <Heart size={16} /></button></form><p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p></div></div></section>;
}
