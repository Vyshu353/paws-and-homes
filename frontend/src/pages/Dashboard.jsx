import { ArrowRight, Heart, PawPrint } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { useEffect, useState } from "react";
import { apiRequest } from "../services/api.js";

export default function Dashboard() {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const [adoptions, setAdoptions] = useState([]);
  const [error, setError] = useState("");
  useEffect(() => {
    apiRequest("/adoptions/mine").then(({ adoptions: mine }) => setAdoptions(mine)).catch((err) => setError(err.message));
  }, []);
  return <section className="dashboard-page section-wrap"><div className="dashboard-welcome"><div><span className="eyebrow">YOUR LITTLE CORNER</span><h1>Hey, {user.name.split(" ")[0]}<em>.</em></h1><p>Here’s where your next chapter begins.</p></div><div className="welcome-paw"><PawPrint size={38} /></div></div>{searchParams.get("payment") === "cancelled" && <div className="notice-banner">No worries — your adoption is still here. You can try checkout again when you're ready.</div>}{searchParams.get("payment") === "success" && <div className="notice-banner">Payment received! Your adoption is being confirmed. Check your email for updates.</div>}<div className="dashboard-section-title"><div><h2>Your adoption journey</h2><p>Every great friendship starts somewhere.</p></div><Link className="text-link" to="/pets">Meet more pets <ArrowRight size={16} /></Link></div>{error ? <div className="empty-state"><p>{error}</p></div> : adoptions.length ? <div className="adoption-list">{adoptions.map((item) => <article className="adoption-row" key={item.id}><img src={item.image_url} alt={item.pet_name} /><div className="adoption-row-info"><strong>{item.pet_name}</strong><span>Application submitted {new Date(item.created_at).toLocaleDateString()}</span></div><span className={`status-pill status-${item.status}`}>{item.status}</span></article>)}</div> : <div className="dashboard-empty"><div className="empty-heart"><Heart size={26} /></div><h3>Your story is waiting to begin.</h3><p>Take a peek at the lovely pets looking for a person just like you.</p><Link to="/pets" className="button button-dark">Find your friend <ArrowRight size={16} /></Link></div>}</section>;
}
