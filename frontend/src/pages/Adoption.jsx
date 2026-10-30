import { ArrowLeft, ArrowRight, Heart } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useEffect, useRef, useState } from "react";
import { apiRequest, formatINR, normalizePet } from "../services/api.js";
import PetImage from "../components/PetImage.jsx";

export default function Adoption() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [pet, setPet] = useState(null);
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [adoptionId, setAdoptionId] = useState(null);
  const submitting = useRef(false);
  const checkoutResolved = useRef(false);

  useEffect(() => {
    apiRequest(`/pets/${id}`)
      .then(({ pet: detail }) => setPet(normalizePet(detail)))
      .catch((err) => setError(err.message));
  }, [id]);

  async function submitApplication(event) {
    event.preventDefault();
    if (submitting.current) return;
    submitting.current = true;
    setBusy(true);
    setError("");
    setMessage("");

    try {
      if (typeof window.Razorpay !== "function") {
        throw new Error("Razorpay Checkout could not be loaded. Refresh the page and try again.");
      }

      let currentAdoptionId = adoptionId;
      if (!currentAdoptionId) {
        const { adoption } = await apiRequest("/adoptions", {
          method: "POST",
          body: JSON.stringify({ petId: Number(id), notes }),
        });
        currentAdoptionId = adoption.id;
        setAdoptionId(currentAdoptionId);
      }

      const order = await apiRequest("/payments/razorpay/orders", {
        method: "POST",
        body: JSON.stringify({ adoptionId: currentAdoptionId }),
      });

      checkoutResolved.current = false;
      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: order.name,
        description: order.description,
        order_id: order.orderId,
        prefill: { email: order.email },
        theme: { color: "#e9785c" },
        handler: async (response) => {
          checkoutResolved.current = true;
          try {
            await apiRequest("/payments/razorpay/verify", {
              method: "POST",
              body: JSON.stringify({
                orderId: response.razorpay_order_id,
                paymentId: response.razorpay_payment_id,
                signature: response.razorpay_signature,
              }),
            });
            navigate("/dashboard?payment=success", { replace: true });
          } catch (err) {
            setError(err.message);
            setBusy(false);
            submitting.current = false;
          }
        },
        modal: {
          ondismiss: async () => {
            if (checkoutResolved.current) return;
            checkoutResolved.current = true;
            try {
              await apiRequest("/payments/razorpay/cancel", {
                method: "POST",
                body: JSON.stringify({ orderId: order.orderId }),
              });
              setMessage("Checkout was cancelled. Your application is saved; you can try again when you're ready.");
            } catch (err) {
              setError(err.message);
            } finally {
              setBusy(false);
              submitting.current = false;
            }
          },
        },
      });

      checkout.on("payment.failed", async (response) => {
        checkoutResolved.current = true;
        try {
          await apiRequest("/payments/razorpay/failure", {
            method: "POST",
            body: JSON.stringify({
              orderId: response.error.metadata?.order_id,
              paymentId: response.error.metadata?.payment_id,
            }),
          });
          setError("Payment was not completed. Your adoption application is saved; please try again.");
        } catch (err) {
          setError(err.message);
        } finally {
          setBusy(false);
          submitting.current = false;
        }
      });
      checkout.open();
    } catch (err) {
      setError(err.message);
      setBusy(false);
      submitting.current = false;
    }
  }

  if (error && !pet) return <div className="empty-state page-empty"><h2>We couldn't load this adoption.</h2><p>{error}</p><Link to="/pets">Back to pets</Link></div>;
  if (!pet) return <div className="page-loading">Getting your application ready…</div>;

  return (
    <section className="application-page section-wrap">
      <Link to={`/pets/${pet.id}`} className="back-link"><ArrowLeft size={16} /> Back to {pet.name}</Link>
      <div className="application-card">
        <div className="application-intro">
          <span className="eyebrow">THIS IS THE EXCITING PART</span>
          <h1>Let's make it<br /><em>official.</em></h1>
          <p>Tell us a little about the home you're offering {pet.name}. Once you submit, you can complete the adoption contribution securely.</p>
          <div className="application-pet">
            <PetImage pet={pet} alt="" />
            <span><strong>{pet.name}</strong><small>Adoption contribution · {formatINR(pet.adoption_fee)}</small></span>
            <Heart size={20} />
          </div>
        </div>
        <form className="form-stack application-form" onSubmit={submitApplication}>
          <label>A note for the adoption team
            <textarea
              rows="6"
              minLength={10}
              maxLength={1000}
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              placeholder="Tell us about your home, your experience with pets, and what made you fall for this one…"
              required
              disabled={Boolean(adoptionId) || busy}
            />
          </label>
          <p className="secure-note">Your application is reviewed by the rescue team. Payment is securely processed by Razorpay Test Mode.</p>
          {error && <p className="form-error" role="alert">{error}</p>}
          {message && <p className="notice-banner" role="status">{message}</p>}
          <button className="button button-coral button-full" disabled={busy}>
            {busy ? "Preparing secure checkout…" : "Adopt and pay securely"} <ArrowRight size={17} />
          </button>
          <Link className="cancel-link" to={`/pets/${pet.id}`}>I'll come back to this</Link>
        </form>
      </div>
    </section>
  );
}
