const nodemailer = require("nodemailer");

let transporter;

function getTransporter() {
  if (!process.env.SMTP_HOST || !process.env.SMTP_USER || !process.env.SMTP_PASS) {
    return null;
  }
  if (!transporter) {
    transporter = nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: Number(process.env.SMTP_PORT || 587),
      secure: process.env.SMTP_SECURE === "true",
      auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS },
    });
  }
  return transporter;
}

async function sendNotification({ to, subject, text }) {
  const mailer = getTransporter();
  if (!mailer) {
    console.info(`Email not sent to ${to}: SMTP is not configured.`);
    return { sent: false, reason: "smtp_not_configured" };
  }
  await mailer.sendMail({
    from: process.env.MAIL_FROM || process.env.SMTP_USER,
    to,
    subject,
    text,
  });
  return { sent: true };
}

module.exports = { sendNotification };
