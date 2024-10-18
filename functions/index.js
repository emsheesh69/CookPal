/* eslint-disable indent */
/* eslint-disable require-jsdoc */
const functions = require("firebase-functions");
const nodemailer = require("nodemailer");

// Access environment variables
const gmailEmail = functions.config().gmail.email;
const gmailPassword = functions.config().gmail.password;

// Set up Nodemailer transport
const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: gmailEmail,
    pass: gmailPassword,
  },
});

// Function to generate a 6-digit OTP
function generateOTP() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// Cloud Function to send OTP email
exports.sendOTPEmail = functions.https.onCall((data, context) => {
  const email = data.email;
  const otp = generateOTP();

  const mailOptions = {
    from: `CookPal App <${gmailEmail}>`,
    to: email,
    subject: "Your CookPal OTP",
    text: `Your one-time password (OTP) is ${otp}. It is valid for 10 minutes.`,
  };

  // Send email and return the OTP
  return transporter
    .sendMail(mailOptions)
    .then(() => {
      return {success: true, otp: otp};
    })
    .catch((error) => {
      console.error("Error sending email:", error);
      return {success: false, error: error.toString()};
    });
});
