import { ArrowLeft, ArrowRight, Heart, MapPin, ShieldCheck } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { apiRequest, formatINR, normalizePet } from "../services/api.js";
import { useAuth } from "../context/AuthContext.jsx";
import PetImage from "../components/PetImage.jsx";

export default function PetDetails() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [pet, setPet] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => {
    apiRequest(`/pets/${id}`)
      .then(({ pet: detail }) => setPet(normalizePet(detail)))
      .catch((err) => setError(err.message));
  }, [id]);

  function startAdoption() {
    if (user) navigate(`/adoption/${pet.id}`);
    else navigate("/login", { state: { from: `/adoption/${pet.id}` } });
  }

  if (error) return <div className="empty-state page-empty"><h2>We couldn't find this pet.</h2><p>{error}</p><Link to="/pets" className="button button-dark">Browse pets</Link></div>;
  if (!pet) return <div className="page-loading">Finding your friend…</div>;
  return <section className="pet-detail section-wrap"><Link to="/pets" className="back-link"><ArrowLeft size={16} /> All pets</Link><div className="detail-layout"><div className="detail-photo"><PetImage pet={pet} /></div><div className="detail-copy"><span className="eyebrow">{pet.species} looking for a home</span><h1>Meet <em>{pet.name}.</em></h1><p className="detail-location"><MapPin size={16} />{pet.location}</p><div className="pet-facts"><span><strong>{pet.age}</strong> {pet.age === 1 ? "year old" : "years old"}</span><span><strong>{pet.gender}</strong></span><span><strong>{pet.breed}</strong></span></div><p className="detail-description">{pet.description || `${pet.name} is a sweet ${pet.breed || pet.species.toLowerCase()} ready to bring a little more joy to your home. Come say hello and see if it's a match!`}</p><div className="detail-note"><ShieldCheck size={19} /><span>Health checked, vaccinated, and ready for a loving home.</span></div>{pet.available ? <button className="button button-coral button-full" onClick={startAdoption}>Start an adoption <ArrowRight size={17} /></button> : <div className="unavailable-note"><Heart size={17} /> This pet has found a home. Thank you for caring!</div>}<p className="fee-note">Adoption contribution: <strong>{formatINR(pet.adoption_fee)}</strong></p></div></div></section>;
}
