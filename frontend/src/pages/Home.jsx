import { ArrowDown, ArrowRight, Heart, PawPrint, ShieldCheck, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import PetCard from "../components/PetCard.jsx";
import { useEffect, useState } from "react";
import { apiRequest, normalizePet } from "../services/api.js";

const fallbackPets = [
  { id: 1, name: "Milo", species: "Dog", breed: "Golden retriever", age: 2, gender: "Male", location: "Brooklyn, NY", image_url: "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=900&q=85" },
  { id: 2, name: "Olive", species: "Cat", breed: "Tabby cat", age: 1, gender: "Female", location: "Queens, NY", image_url: "https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=900&q=85" },
  { id: 3, name: "Biscuit", species: "Dog", breed: "Corgi mix", age: 3, gender: "Male", location: "Jersey City, NJ", image_url: "https://images.unsplash.com/photo-1612536057832-2ff7ead58194?auto=format&fit=crop&w=900&q=85" },
];

export default function Home() {
  const [pets, setPets] = useState(fallbackPets);
  const [petsError, setPetsError] = useState("");
  useEffect(() => {
    apiRequest("/pets?limit=3").then(({ pets: availablePets }) => {
      setPets(availablePets.map(normalizePet));
    }).catch((error) => {
      setPetsError(error.message);
    });
  }, []);

  return (
    <>
      <section className="hero">
        <div className="hero-copy">
          <div className="eyebrow"><span className="eyebrow-dot" /> A little love goes a long way</div>
          <h1>Find your<br />forever <span>friend.</span></h1>
          <p className="hero-description">Big hearts. Wet noses. Happy homes. Meet the pet who’s been waiting for someone just like you.</p>
          <div className="hero-actions"><Link className="button button-coral" to="/pets">Meet the pets <ArrowRight size={17} /></Link><a className="text-link" href="#how-it-works">How adoption works <ArrowDown size={15} /></a></div>
          <div className="hero-proof"><div className="avatar-stack"><img src="https://i.pravatar.cc/80?img=47" alt="" /><img src="https://i.pravatar.cc/80?img=12" alt="" /><img src="https://i.pravatar.cc/80?img=44" alt="" /></div><div><strong>2,400+ happy families</strong><span>found their perfect match</span></div></div>
        </div>
        <div className="hero-visual">
          <div className="hero-image-frame"><img src="https://images.unsplash.com/photo-1551717743-49959800b1f6?auto=format&fit=crop&w=1200&q=90" alt="A happy dog enjoying the sunshine" /></div>
          <div className="floating-note"><span className="note-icon"><Heart size={17} fill="currentColor" /></span><div><strong>Love at first wag</strong><span>It happens every day</span></div></div>
          <div className="hero-scribble"><PawPrint size={38} /><span>Best day<br />ever!</span></div>
          <div className="hero-image-caption"><span>Meet your new<br />favorite hello.</span><span className="caption-line" /></div>
        </div>
        <span className="hero-sunburst">✳</span>
      </section>
      <section className="trust-strip"><span><Heart size={17} /> Rescue first</span><span><ShieldCheck size={17} /> Every pet vetted</span><span><Sparkles size={17} /> Support all the way</span></section>
      <section className="featured-section section-wrap">
        <div className="section-heading"><div><span className="eyebrow">YOUR NEW BEST FRIEND</span><h2>Somebody’s waiting<br />to meet <em>you.</em></h2></div><Link className="text-link" to="/pets">See all pets <ArrowRight size={16} /></Link></div>
        {petsError && <p className="form-error" role="status">Showing sample pets because the live list could not be loaded: {petsError}</p>}
        {pets.length > 0
          ? <div className="pet-grid">{pets.map((pet) => <PetCard key={pet.id} pet={pet} />)}</div>
          : <p className="empty-state">No pets are currently available. Please check back soon.</p>}
      </section>
      <section className="story-section" id="our-story"><div className="story-image"><img src="https://images.unsplash.com/photo-1601758228041-f3b2795255f1?auto=format&fit=crop&w=1000&q=85" alt="A family sharing a happy moment with their dog" /><span className="story-stamp">GOOD<br />DOGS<br />GOOD<br />PEOPLE</span></div><div className="story-copy"><span className="eyebrow">A BETTER KIND OF MATCH</span><h2>Adoption is<br />a <em>love story.</em></h2><p>We bring good people and incredible rescue pets together. From your first hello to the first night home, we make every step feel a little easier.</p><Link className="button button-dark" to="/pets">Find your match <ArrowRight size={16} /></Link></div></section>
      <section className="steps-section" id="how-it-works"><div className="section-heading"><div><span className="eyebrow">EASY AS ONE, TWO, WOOF</span><h2>Three steps to<br />your <em>happily ever after.</em></h2></div></div><div className="steps-grid"><article><span className="step-number">01</span><div className="step-icon peach"><PawPrint /></div><h3>Find your person</h3><p>Browse our lovely lineup and find a pet whose personality feels like home.</p></article><article><span className="step-number">02</span><div className="step-icon yellow"><Heart /></div><h3>Say hello</h3><p>Send an adoption application. Our friendly team will help you get to know each other.</p></article><article><span className="step-number">03</span><div className="step-icon green"><Sparkles /></div><h3>Bring them home</h3><p>We’ll help with the final details so you can focus on the best part: your new life together.</p></article></div></section>
      <section className="bottom-cta"><div><span className="eyebrow">YOUR COUCH IS CALLING</span><h2>Ready for a little<br />more <em>love?</em></h2></div><Link className="button button-coral" to="/pets">Meet the pets <ArrowRight size={17} /></Link><span className="cta-heart">♡</span></section>
    </>
  );
}
