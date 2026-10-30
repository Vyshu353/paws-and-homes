import { Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import PetCard from "../components/PetCard.jsx";
import { apiRequest, normalizePet } from "../services/api.js";

export default function Pets() {
  const [pets, setPets] = useState([]);
  const [species, setSpecies] = useState("All pets");
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    apiRequest("/pets")
      .then(({ pets: availablePets }) => setPets(availablePets.map(normalizePet)))
      .catch((err) => setError(err.message));
  }, []);
  const filtered = useMemo(() => pets.filter((pet) =>
    (species === "All pets" || pet.species.toLowerCase() === species.toLowerCase()) &&
    `${pet.name} ${pet.breed} ${pet.location}`.toLowerCase().includes(search.toLowerCase())
  ), [pets, species, search]);

  return <section className="listing-page section-wrap"><div className="listing-heading"><span className="eyebrow">THE GOOD COMPANY</span><h1>Meet the <em>whole</em><br />lovable bunch.</h1><p>Every face belongs to a friend looking for their forever home.</p></div><div className="listing-controls"><div className="filter-tabs">{["All pets", "Dog", "Cat"].map((item) => <button key={item} className={species === item ? "filter-active" : ""} onClick={() => setSpecies(item)}>{item === "Dog" ? "Dogs" : item === "Cat" ? "Cats" : item}</button>)}</div><label className="search-field"><Search size={17} /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search by name or breed" aria-label="Search pets" /></label></div>{error ? <div className="empty-state"><h2>We couldn’t load the pets.</h2><p>{error}</p></div> : filtered.length ? <div className="pet-grid listing-grid">{filtered.map((pet) => <PetCard key={pet.id} pet={pet} />)}</div> : <div className="empty-state"><h2>No matches just yet.</h2><p>Try a different search or check back soon for new friends.</p></div>}</section>;
}
