import { ArrowUpRight, MapPin } from "lucide-react";
import { Link } from "react-router-dom";
import PetImage from "./PetImage.jsx";

export default function PetCard({ pet }) {
  return (
    <Link className="pet-card" to={`/pets/${pet.id}`}>
      <div className="pet-card-image-wrap">
        <PetImage pet={pet} className="pet-card-image" loading="lazy" />
        <span className="pet-type">{pet.breed || pet.species}</span>
        <span className="pet-arrow"><ArrowUpRight size={18} /></span>
      </div>
      <div className="pet-card-info">
        <div><h3>{pet.name}</h3><span>{pet.age} {pet.age === 1 ? "year" : "years"} old · {pet.gender}</span></div>
        <p><MapPin size={14} />{pet.location}</p>
      </div>
    </Link>
  );
}
