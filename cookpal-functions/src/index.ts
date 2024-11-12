import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

// Define the expected shape of the data
interface ResetPasswordData {
  email: string;
  newPassword: string;
}

// Define the function with the correct parameter type
export const resetPassword = functions.https.onCall(
  async (
    data: functions.https.CallableRequest<ResetPasswordData>
  ): Promise<{ success: boolean; message: string }> => {
    const {email, newPassword} = data.data; // Access the data with `.data`

    try {
      // Fetch the user by email
      const user = await admin.auth().getUserByEmail(email);
      // Update the user's password
      await admin.auth().updateUser(user.uid, {password: newPassword});

      return {success: true, message: "Password reset successfully."};
    } catch (error: unknown) {
      // Explicitly handle 'unknown' type for error
      console.error("Error resetting password:", error);
      if (error instanceof Error) {
        return {success: false, message: error.message};
      } else {
        return {success: false, message: "An unknown error occurred."};
      }
    }
  }
);
