package firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;

import java.io.FileInputStream;
import java.io.IOException;

public class Firebase {
    private static FirebaseApp server;
    public static void getServer() {
        try{
            server = FirebaseApp.getInstance();
        }catch(IllegalStateException e){
            FileInputStream serviceAccount;
            try {
                serviceAccount = new FileInputStream("resources/google-services.json");
                FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
                server = FirebaseApp.initializeApp(options);
            } catch (IOException ex) {
                //e.printStackTrace();
            }
        }
    }
    public static String getProfilePicture(String email) {
        getServer();

        try{
            FirebaseAuth auth=FirebaseAuth.getInstance(server);
            UserRecord user=auth.getUserByEmail(email);
            return user.getPhotoUrl();

        }catch(FirebaseAuthException e){
            //e.printStackTrace();
            return null;
        }
    }
    public static String verifyToken(String token, String uid) {
        getServer();

        try{
            FirebaseAuth auth= FirebaseAuth.getInstance(server);
            FirebaseToken ftoken = auth.verifyIdToken(token, true);
            String tokenUid = ftoken.getUid();

            if(tokenUid.equals(uid))
                return ftoken.getEmail();
            else
                return null;

        }catch(FirebaseAuthException e){
            //e.printStackTrace();
            return null;
        }
    }

    public static boolean isAuthServiceReachable() {
        getServer();

        try {
            FirebaseAuth auth = FirebaseAuth.getInstance(server);
            // Attempt to fetch a non-existent user to check service availability
            auth.getUserByEmail("test@example.com");
            return true;
        } catch (FirebaseAuthException e) {
            // Handle FirebaseAuthException indicating the service is reachable
            return true;
        } catch (Exception e) {
            // If another exception occurs, it might indicate connectivity issues
            e.printStackTrace();
            return false;
        }
    }

}
