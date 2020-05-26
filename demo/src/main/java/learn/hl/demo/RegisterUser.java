/*
SPDX-License-Identifier: Apache-2.0
*/

package learn.hl.demo;

import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallet.Identity;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

public class RegisterUser {

	static {
		System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
	}

	public static void register() throws Exception {

		// Create a CA client for interacting with the CA.
		Properties props = new Properties();
		props.put("pemFile",
				"ca.artist.mediacoin.com-cert.pem");
		props.put("allowAllHostNames", "true");
		HFCAClient caClient = HFCAClient.createNewInstance("https://localhost:7054", props);
		CryptoSuite cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
		caClient.setCryptoSuite(cryptoSuite);

		// Create a wallet for managing identities
		Wallet wallet = Wallet.createFileSystemWallet(Paths.get("/home/ubuntu/wallet"));

		// Check to see if we've already enrolled the user.
		boolean userExists = wallet.exists("appUser3");
		if (userExists) {
			System.out.println("An identity for the user \"appUser3\" already exists in the wallet");
			return;
		}

		userExists = wallet.exists("admin");
		if (!userExists) {
			System.out.println("\"admin\" needs to be enrolled and added to the wallet first");
			return;
		}

		Identity adminIdentity = wallet.get("admin");
		User admin = new User() {

			@Override
			public String getName() {
				return "admin";
			}

			@Override
			public Set<String> getRoles() {
				return null;
			}

			@Override
			public String getAccount() {
				return null;
			}

			@Override
			public String getAffiliation() {
				return "artist.department1";
			}

			@Override
			public Enrollment getEnrollment() {
				return new Enrollment() {

					@Override
					public PrivateKey getKey() {
						return adminIdentity.getPrivateKey();
					}

					@Override
					public String getCert() {
						return adminIdentity.getCertificate();
					}
				};
			}

			@Override
			public String getMspId() {
				return "artistMSP";
			}

		};

//		Collection<HFCAIdentity> hfcaIdentities = caClient.getHFCAIdentities(admin);
//		((HFCAIdentity)hfcaIdentities[0])

		// Register the user, enroll the user, and import the new identity into the wallet.
		RegistrationRequest registrationRequest = new RegistrationRequest("appUser3");
		registrationRequest.setAffiliation("org1.department1");
		registrationRequest.setEnrollmentID("appUser3");
		String enrollmentSecret = caClient.register(registrationRequest, admin);

		Enrollment enrollment = caClient.enroll("appUser3", enrollmentSecret);
		Identity user = Identity.createIdentity("artistMSP", enrollment.getCert(), enrollment.getKey());
		wallet.put("appUser3", user);
		System.out.println("Successfully enrolled user \"appUser2\" and imported it into the wallet");
	}

}
