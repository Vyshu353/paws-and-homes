const Stripe = require("stripe");

let stripeClient;

function getStripe() {
  if (!process.env.STRIPE_SECRET_KEY) {
    const error = new Error("Payments are not configured. Add STRIPE_SECRET_KEY to the backend environment.");
    error.status = 503;
    throw error;
  }
  if (!stripeClient) stripeClient = new Stripe(process.env.STRIPE_SECRET_KEY);
  return stripeClient;
}

module.exports = { getStripe };
