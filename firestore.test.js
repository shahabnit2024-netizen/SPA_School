const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const ADMIN_EMAIL = "shahab.nit2024@gmail.com";
const MOD_2015_UID = "mod_2015_uid";
const MEMBER_2015_UID = "member_2015_uid";
const MEMBER_2018_UID = "member_2018_uid";
const PENDING_2015_UID = "pending_2015_uid";
const PENDING_2018_UID = "pending_2018_uid";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 5000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedUsersAndRoles() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const t = pastDate(20000);
    await db.collection("users").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: ADMIN_EMAIL,
      name: "Super Admin",
      phone: "+8801700000000",
      hidePhone: false,
      sscBatch: "2010",
      bloodGroup: "O+",
      profession: "Architect",
      city: "Comilla",
      country: "Bangladesh",
      role: "admin",
      status: "approved",
      visibleToUids: [ADMIN_UID],
      createdAt: t,
      updatedAt: t,
    });
    await db.collection("users").doc(MOD_2015_UID).set({
      uid: MOD_2015_UID,
      email: "mod2015@example.com",
      name: "Batch 2015 Moderator",
      phone: "+8801711111111",
      hidePhone: false,
      sscBatch: "2015",
      bloodGroup: "A+",
      profession: "Doctor",
      city: "Dhaka",
      country: "Bangladesh",
      role: "moderator",
      status: "approved",
      visibleToUids: [MOD_2015_UID],
      createdAt: t,
      updatedAt: t,
    });
    await db.collection("users").doc(MEMBER_2015_UID).set({
      uid: MEMBER_2015_UID,
      email: "member2015@example.com",
      name: "Verified Alumnus",
      phone: "Hidden (Privacy On)",
      hidePhone: true,
      sscBatch: "2015",
      bloodGroup: "B+",
      profession: "Engineer",
      city: "Comilla",
      country: "Bangladesh",
      role: "member",
      status: "approved",
      visibleToUids: [MEMBER_2015_UID, MOD_2015_UID],
      createdAt: t,
      updatedAt: t,
    });
    await db.collection("users").doc(MEMBER_2018_UID).set({
      uid: MEMBER_2018_UID,
      email: "member2018@example.com",
      name: "Batch 2018 Member",
      phone: "+8801722222222",
      hidePhone: false,
      sscBatch: "2018",
      bloodGroup: "AB+",
      profession: "Teacher",
      city: "Chattogram",
      country: "Bangladesh",
      role: "member",
      status: "approved",
      visibleToUids: [MEMBER_2018_UID],
      createdAt: t,
      updatedAt: t,
    });
    await db.collection("users").doc(PENDING_2015_UID).set({
      uid: PENDING_2015_UID,
      email: "pending2015@example.com",
      name: "Pending 2015 User",
      phone: "+8801733333333",
      hidePhone: false,
      sscBatch: "2015",
      bloodGroup: "O-",
      profession: "Banker",
      city: "Dhaka",
      country: "Bangladesh",
      role: "member",
      status: "pending",
      visibleToUids: [PENDING_2015_UID, MOD_2015_UID],
      createdAt: t,
      updatedAt: t,
    });
    await db.collection("users").doc(PENDING_2018_UID).set({
      uid: PENDING_2018_UID,
      email: "pending2018@example.com",
      name: "Pending 2018 User",
      phone: "+8801744444444",
      hidePhone: false,
      sscBatch: "2018",
      bloodGroup: "B-",
      profession: "Designer",
      city: "Sylhet",
      country: "Bangladesh",
      role: "member",
      status: "pending",
      visibleToUids: [PENDING_2018_UID],
      createdAt: t,
      updatedAt: t,
    });
    await db.collection("users").doc(MEMBER_2015_UID).collection("private").doc("contact").set({
      uid: MEMBER_2015_UID,
      phone: "+8801799999999",
      updatedAt: t,
    });
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedUsersAndRoles();
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").where("status", "==", "approved").get());
  await assertFails(unauth.collection("notices").where("audience", "==", "all_members").get());
  await assertFails(unauth.collection("blood_requests").where("status", "in", ["open", "fulfilled"]).get());
  await assertFails(unauth.collection("events").where("visibility", "==", "alumni").get());
  await assertFails(unauth.collection("funds").where("status", "in", ["active", "completed"]).get());
  await assertFails(unauth.collection("teachers").where("status", "in", ["current", "retired", "late"]).get());
});

test("Registration creates approved member directly, blocks role escalation, and rejects future timestamps", async () => {
  const newDb = testEnv.authenticatedContext("new_user_1", {
    email: "newuser@example.com",
    email_verified: true,
  }).firestore();
  const t = pastDate();

  // Valid member registration (approved immediately)
  await assertSucceeds(
    newDb.collection("users").doc("new_user_1").set({
      uid: "new_user_1",
      email: "newuser@example.com",
      name: "Rahim Uddin",
      phone: "+8801811111111",
      hidePhone: false,
      sscBatch: "2016",
      bloodGroup: "A+",
      profession: "Software Engineer",
      city: "Dhaka",
      country: "Bangladesh",
      role: "member",
      status: "approved",
      visibleToUids: ["new_user_1"],
      createdAt: t,
      updatedAt: t,
    })
  );

  // Self-escalation to admin must fail
  await assertFails(
    newDb.collection("users").doc("new_user_2").set({
      uid: "new_user_2",
      name: "Spoofer",
      phone: "+8801811111112",
      hidePhone: false,
      sscBatch: "2016",
      bloodGroup: "A+",
      profession: "Hacker",
      city: "Dhaka",
      country: "Bangladesh",
      role: "admin",
      status: "approved",
      createdAt: t,
    })
  );

  // Future timestamp must fail
  await assertFails(
    newDb.collection("users").doc("new_user_3").set({
      uid: "new_user_3",
      name: "Future User",
      phone: "+8801811111113",
      hidePhone: false,
      sscBatch: "2016",
      bloodGroup: "A+",
      profession: "Student",
      city: "Comilla",
      country: "Bangladesh",
      role: "member",
      status: "approved",
      createdAt: new Date(Date.now() + 60000),
    })
  );
});

test("Private phone contact subcollection is strictly isolated to owner and Super Admin", async () => {
  await seedUsersAndRoles();
  const ownerDb = testEnv.authenticatedContext(MEMBER_2015_UID).firestore();
  const otherDb = testEnv.authenticatedContext(MEMBER_2018_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(ownerDb.collection("users").doc(MEMBER_2015_UID).collection("private").doc("contact").get());
  await assertSucceeds(adminDb.collection("users").doc(MEMBER_2015_UID).collection("private").doc("contact").get());
  await assertFails(otherDb.collection("users").doc(MEMBER_2015_UID).collection("private").doc("contact").get());
});

test("Batch Moderator can query and approve/reject ONLY their own sscBatch pending users", async () => {
  await seedUsersAndRoles();
  const modDb = testEnv.authenticatedContext(MOD_2015_UID).firestore();

  // Moderator queries pending users in batch 2015 visible to them
  await assertSucceeds(
    modDb.collection("users")
      .where("sscBatch", "==", "2015")
      .where("status", "==", "pending")
      .where("visibleToUids", "array-contains", MOD_2015_UID)
      .get()
  );

  // Unfiltered pending query by moderator fails
  await assertFails(modDb.collection("users").where("status", "==", "pending").get());

  // Moderator approves pending user in batch 2015
  await assertSucceeds(
    modDb.collection("users").doc(PENDING_2015_UID).update({
      status: "approved",
      updatedAt: pastDate(1000),
    })
  );

  // Moderator CANNOT approve pending user in batch 2018
  await assertFails(
    modDb.collection("users").doc(PENDING_2018_UID).update({
      status: "approved",
      updatedAt: pastDate(1000),
    })
  );

  // Moderator CANNOT promote someone to admin or moderator
  await assertFails(
    modDb.collection("users").doc(PENDING_2015_UID).update({
      role: "moderator",
      status: "approved",
      updatedAt: pastDate(1000),
    })
  );
});

test("Super Admin can approve users, assign moderators, and manage notices, events, funds, transactions, and teachers", async () => {
  await seedUsersAndRoles();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const memberDb = testEnv.authenticatedContext(MEMBER_2015_UID).firestore();
  const t = pastDate(2000);

  // Spoofed email (email_verified: false) must NOT get bootstrap admin privileges
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("teachers").doc("t_spoof").set({
      name: "Fake",
      subject: "Math",
      tenure: "2000-2020",
      phone: "0170000000",
      status: "current",
      createdAt: t,
    })
  );

  // Super Admin creates notice, event, fund, transaction, teacher
  await assertSucceeds(
    adminDb.collection("notices").doc("n1").set({
      title: "Annual Reunion 2026",
      description: "Save the date at Sreepur Prasanna Academy campus.",
      category: "notice",
      isPinned: true,
      audience: "all_members",
      postedBy: ADMIN_UID,
      postedByName: "Super Admin",
      createdAt: t,
    })
  );

  await assertSucceeds(
    adminDb.collection("events").doc("e1").set({
      title: "Grand Alumni Reunion",
      date: "2026-12-20",
      location: "SPA Campus, Comilla",
      fee: 1000,
      description: "Full day reunion & cultural program.",
      visibility: "alumni",
      registeredUsers: [],
      createdBy: ADMIN_UID,
      createdAt: t,
    })
  );

  await assertSucceeds(
    adminDb.collection("funds").doc("f1").set({
      title: "Scholarship Fund 2026",
      targetAmount: 500000,
      collectedAmount: 0,
      spentAmount: 0,
      status: "active",
      createdBy: ADMIN_UID,
      createdAt: t,
    })
  );

  await assertSucceeds(
    adminDb.collection("funds").doc("f1").collection("transactions").doc("tx1").set({
      type: "in",
      amount: 10000,
      description: "Batch 2010 contribution",
      date: "2026-11-23",
      addedBy: ADMIN_UID,
      addedByName: "Super Admin",
      campaignVisibility: "alumni",
      createdAt: t,
    })
  );

  await assertSucceeds(
    adminDb.collection("teachers").doc("t1").set({
      name: "Md. Abdul Karim",
      subject: "Mathematics",
      tenure: "1995 - 2022",
      phone: "+8801755555555",
      status: "retired",
      createdAt: t,
    })
  );

  // Verified member can read all community collections with aligned queries
  await assertSucceeds(memberDb.collection("notices").where("audience", "==", "all_members").get());
  await assertSucceeds(memberDb.collection("events").where("visibility", "==", "alumni").get());
  await assertSucceeds(memberDb.collection("funds").where("status", "in", ["active", "completed"]).get());
  await assertSucceeds(memberDb.collection("funds").doc("f1").collection("transactions").where("campaignVisibility", "==", "alumni").get());
  await assertSucceeds(memberDb.collection("teachers").where("status", "in", ["current", "retired", "late"]).get());

  // Verified member can register & unregister for event, but cannot edit event title
  await assertSucceeds(
    memberDb.collection("events").doc("e1").update({
      registeredUsers: [MEMBER_2015_UID],
    })
  );
  await assertSucceeds(
    memberDb.collection("events").doc("e1").update({
      registeredUsers: [],
    })
  );
  await assertFails(
    memberDb.collection("events").doc("e1").update({
      title: "Hacked Event",
    })
  );

  // Verified member cannot create fund transaction or teacher
  await assertFails(
    memberDb.collection("funds").doc("f1").collection("transactions").doc("tx2").set({
      type: "out",
      amount: 5000,
      description: "Unauthorized",
      date: "2026-11-23",
      addedBy: MEMBER_2015_UID,
      campaignVisibility: "alumni",
      createdAt: t,
    })
  );
  await assertFails(
    memberDb.collection("teachers").doc("t2").set({
      name: "Unauthorized",
      subject: "Physics",
      tenure: "2020-Present",
      phone: "0170000000",
      status: "current",
      createdAt: t,
    })
  );
});

test("Verified members can create blood requests and mark their own fulfilled, but not others", async () => {
  await seedUsersAndRoles();
  const member15 = testEnv.authenticatedContext(MEMBER_2015_UID).firestore();
  const member18 = testEnv.authenticatedContext(MEMBER_2018_UID).firestore();
  const t = pastDate(2000);

  await assertSucceeds(
    member15.collection("blood_requests").doc("req1").set({
      patientName: "Kamal Hossain",
      bloodGroup: "O+",
      targetBloodGroups: ["O+", "O-"],
      hospitalLocation: "Comilla Medical College Hospital",
      contactPhone: "+8801712345678",
      bagsNeeded: 2,
      status: "open",
      postedBy: MEMBER_2015_UID,
      postedByName: "Verified Alumnus",
      createdAt: t,
    })
  );

  // Invalid blood group in targetBloodGroups is rejected
  await assertFails(
    member15.collection("blood_requests").doc("req_bad").set({
      patientName: "Kamal Hossain",
      bloodGroup: "O+",
      targetBloodGroups: ["INVALID"],
      hospitalLocation: "Comilla Medical College Hospital",
      contactPhone: "+8801712345678",
      bagsNeeded: 2,
      status: "open",
      postedBy: MEMBER_2015_UID,
      createdAt: t,
    })
  );

  // Owner can save private notification preferences; other member cannot read them
  await assertSucceeds(
    member15.collection("users").doc(MEMBER_2015_UID).collection("private").doc("notifications").set({
      uid: MEMBER_2015_UID,
      alertsEnabled: true,
      subscribedGroups: ["O+", "O-"],
      fcmToken: "test_fcm_token_abc123",
      updatedAt: t,
    })
  );
  await assertFails(
    member18.collection("users").doc(MEMBER_2015_UID).collection("private").doc("notifications").get()
  );

  // Other member cannot mark req1 fulfilled
  await assertFails(
    member18.collection("blood_requests").doc("req1").update({
      status: "fulfilled",
    })
  );

  // Poster can mark fulfilled
  await assertSucceeds(
    member15.collection("blood_requests").doc("req1").update({
      status: "fulfilled",
    })
  );
});

test("Super Admin (sahabuddinsagor@gmail.com) can update app logo/branding and edit any section", async () => {
  await seedUsersAndRoles();
  const sagorDb = testEnv.authenticatedContext("sagor_admin_uid", {
    email: "sahabuddinsagor@gmail.com",
    email_verified: true,
  }).firestore();
  const memberDb = testEnv.authenticatedContext(MEMBER_2015_UID).firestore();
  const t = pastDate(1000);

  // Super Admin can write app_config/branding with custom logo
  await assertSucceeds(
    sagorDb.collection("app_config").doc("branding").set({
      schoolNameBn: "শ্রীপুর প্রসন্ন একাডেমি, কুমিল্লা",
      schoolNameEn: "Sreepur Prasanna Academy, Comilla",
      subtitleBn: "প্রাক্তন ছাত্র-ছাত্রী সংসদ",
      subtitleEn: "Alumni Association",
      logoDataOrUrl: "https://example.com/spa-logo.png",
      visibility: "alumni",
      updatedBy: "sagor_admin_uid",
      updatedAt: t,
    })
  );

  // Approved member can read branding, but CANNOT write branding
  await assertSucceeds(memberDb.collection("app_config").doc("branding").get());
  await assertFails(
    memberDb.collection("app_config").doc("branding").set({
      schoolNameBn: "Hacked",
      schoolNameEn: "Hacked",
      visibility: "alumni",
      updatedBy: MEMBER_2015_UID,
      updatedAt: t,
    })
  );

  // Super Admin can edit any user profile fields
  await assertSucceeds(
    sagorDb.collection("users").doc(MEMBER_2015_UID).update({
      profession: "Senior Architect",
      updatedAt: t,
    })
  );

  // Super Admin can push app update info; verified members can read but NOT write
  await assertSucceeds(
    sagorDb.collection("app_config").doc("update_info").set({
      versionCode: 2,
      versionName: "1.1.0",
      releaseNotes: "Added live app update alerts and refreshed logo.",
      downloadUrl: "https://example.com/spa-alumni-v1.1.apk",
      mandatory: true,
      visibility: "alumni",
      pushedBy: "sagor_admin_uid",
      pushedByName: "Sahabuddin Sagor",
      updatedAt: t,
    })
  );
  await assertSucceeds(memberDb.collection("app_config").doc("update_info").get());
  await assertFails(
    memberDb.collection("app_config").doc("update_info").set({
      versionCode: 99,
      versionName: "9.9.9",
      releaseNotes: "Spoof update",
      mandatory: false,
      visibility: "alumni",
      pushedBy: MEMBER_2015_UID,
      updatedAt: t,
    })
  );
});

test("Members can update their own profile photo and upload pending batch photos that require admin approval", async () => {
  await seedUsersAndRoles();
  const member15 = testEnv.authenticatedContext(MEMBER_2015_UID).firestore();
  const member18 = testEnv.authenticatedContext(MEMBER_2018_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const t = pastDate(1000);

  // Member can update their own photoUrl with a compressed JPEG data URI
  const sampleAvatar = "data:image/jpeg;base64," + "A".repeat(2048);
  await assertSucceeds(
    member15.collection("users").doc(MEMBER_2015_UID).update({
      photoUrl: sampleAvatar,
      updatedAt: t,
    })
  );

  // Cross-user photoUrl update must fail
  await assertFails(
    member18.collection("users").doc(MEMBER_2015_UID).update({
      photoUrl: sampleAvatar,
      updatedAt: t,
    })
  );

  // Member CANNOT self-approve a batch gallery upload
  await assertFails(
    member15.collection("batch_gallery").doc("g_bad").set({
      sscBatch: "2015",
      caption: "Batch 2015 Iftar Get-Together",
      imageDataOrUrl: sampleAvatar,
      status: "approved",
      uploadedBy: MEMBER_2015_UID,
      uploadedByName: "Verified Alumnus",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Member can upload a pending batch group photo
  await assertSucceeds(
    member15.collection("batch_gallery").doc("g1").set({
      sscBatch: "2015",
      caption: "Batch 2015 Reunion at SPA Campus",
      imageDataOrUrl: sampleAvatar,
      status: "pending",
      uploadedBy: MEMBER_2015_UID,
      uploadedByName: "Verified Alumnus",
      createdAt: t,
      updatedAt: t,
    })
  );

  // Uploader can query their own uploads; other member cannot query pending gallery photos
  await assertSucceeds(
    member15.collection("batch_gallery").where("uploadedBy", "==", MEMBER_2015_UID).get()
  );
  await assertFails(
    member18.collection("batch_gallery").where("status", "==", "pending").get()
  );
  await assertFails(
    member15.collection("batch_gallery").doc("g1").update({
      status: "approved",
      reviewedBy: MEMBER_2015_UID,
      updatedAt: t,
    })
  );

  // Admin can query pending photos and approve them
  await assertSucceeds(
    adminDb.collection("batch_gallery").where("status", "==", "pending").get()
  );
  await assertSucceeds(
    adminDb.collection("batch_gallery").doc("g1").update({
      status: "approved",
      reviewedBy: ADMIN_UID,
      updatedAt: t,
    })
  );

  // Other verified members can now list approved batch photos
  await assertSucceeds(
    member18.collection("batch_gallery").where("status", "==", "approved").get()
  );
});

