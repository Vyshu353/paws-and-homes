const { pool } = require("../config/db");
const { getStripe } = require("../services/paymentService");
const { sendNotification } = require("../services/emailService");

async function createCheckout(req, res, next) {
  const adoptionId = Number(req.body.adoptionId);
  if (!Number.isInteger(adoptionId) || adoptionId < 1) {
    return res.status(400).json({ message: "A valid adoption application is required." });
  }
  try {
    const { rows } = await pool.query(
      `SELECT a.id, a.status, p.name, p.adoption_fee
       FROM adoptions a JOIN pets p ON p.id = a.pet_id
       WHERE a.id = $1 AND a.user_id = $2`,
      [adoptionId, req.user.id],
    );
    const adoption = rows[0];
    if (!adoption) return res.status(404).json({ message: "Adoption application not found." });
    if (adoption.status !== "pending") return res.status(409).json({ message: "This adoption application has already been processed." });

    const stripe = getStripe();
    const session = await stripe.checkout.sessions.create({
      mode: "payment",
      customer_email: req.user.email,
      line_items: [{
        quantity: 1,
        price_data: {
          currency: "usd",
          unit_amount: Math.round(Number(adoption.adoption_fee) * 100),
          product_data: { name: `Adoption contribution — ${adoption.name}` },
        },
      }],
      metadata: { adoptionId: String(adoption.id), userId: String(req.user.id) },
      success_url: `${process.env.FRONTEND_URL}/dashboard?payment=success`,
      cancel_url: `${process.env.FRONTEND_URL}/dashboard?payment=cancelled`,
    });
    await pool.query("UPDATE adoptions SET stripe_session_id = $1 WHERE id = $2", [session.id, adoption.id]);
    return res.json({ url: session.url });
  } catch (error) {
    return next(error);
  }
}

async function handleWebhook(req, res) {
  let stripe;
  try {
    stripe = getStripe();
    if (!process.env.STRIPE_WEBHOOK_SECRET) {
      return res.status(503).json({ message: "Stripe webhook verification is not configured." });
    }
  } catch (error) {
    return res.status(error.status || 500).json({ message: error.message });
  }

  let event;
  try {
    event = stripe.webhooks.constructEvent(
      req.body,
      req.headers["stripe-signature"],
      process.env.STRIPE_WEBHOOK_SECRET,
    );
  } catch (error) {
    return res.status(400).send(`Webhook signature verification failed: ${error.message}`);
  }

  if (event.type === "checkout.session.completed") {
    const session = event.data.object;
    const adoptionId = Number(session.metadata?.adoptionId);
    if (adoptionId && session.payment_status === "paid") {
      const client = await pool.connect();
      try {
        await client.query("BEGIN");
        const { rows } = await client.query(
          `UPDATE adoptions SET status = 'paid'
           WHERE id = $1 AND stripe_session_id = $2 AND status = 'pending'
           RETURNING id, user_id, pet_id`,
          [adoptionId, session.id],
        );
        if (rows[0]) {
          await client.query("UPDATE pets SET available = FALSE WHERE id = $1", [rows[0].pet_id]);
          const { rows: users } = await client.query("SELECT name, email FROM users WHERE id = $1", [rows[0].user_id]);
          const { rows: pets } = await client.query("SELECT name FROM pets WHERE id = $1", [rows[0].pet_id]);
          await client.query("COMMIT");
          try {
            await sendNotification({
              to: users[0].email,
              subject: `Payment received for ${pets[0].name}`,
              text: `Hi ${users[0].name},\n\nYour adoption contribution for ${pets[0].name} was received. Our rescue team will be in touch with the next steps. Thank you for giving a pet a home!\n\nThe Paws & Homes team`,
            });
          } catch (emailError) {
            console.error("Payment confirmation email delivery failed:", emailError);
          }
        } else {
          await client.query("COMMIT");
        }
      } catch (error) {
        await client.query("ROLLBACK");
        console.error("Could not finalize paid adoption:", error);
        return res.status(500).json({ message: "Could not update adoption after payment." });
      } finally {
        client.release();
      }
    }
  }
  return res.json({ received: true });
}

module.exports = { createCheckout, handleWebhook };
