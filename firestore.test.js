const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_director_1";
const BOB_UID = "bob_director_2";
const SUPER_ADMIN_UID = "super_admin_adriel";
const SUPER_ADMIN_EMAIL = "adrielhk72@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function validSchoolPayload(ownerUid, name = "Lycee Moderne") {
  const ts = pastDate(10000);
  return {
    ownerUid,
    memberUids: [ownerUid],
    name,
    code: "LYM01",
    schoolType: "Lycee",
    city: "Yaounde",
    country: "Cameroun",
    subscriptionStatus: "TRIAL",
    subscriptionEndMillis: Date.now() + 86400000 * 30,
    monthlyFeeFcfa: 20000,
    supportAccessEnabled: false,
    motto: "Discipline - Travail - Succes",
    primaryColorHex: "#0B1528",
    secondaryColorHex: "#1E40AF",
    accentColorHex: "#0D9488",
    createdAt: ts,
    updatedAt: ts,
  };
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

test("Unauthenticated user: cannot read or write schools or subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("schools").get());
  await assertFails(unauthDb.collection("schools").doc("school_1").set(validSchoolPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("subscriptions").get());
  await assertFails(unauthDb.collection("platform_config").doc("global").get());
});

test("School isolation: Alice creates & queries her school; Bob cannot read, list, or mutate it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@school.cm", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@other.cm", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("schools").doc("school_alice").set(validSchoolPayload(ALICE_UID)));

  // Filtered query matching Kotlin repository succeeds
  await assertSucceeds(
    aliceDb.collection("schools").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Unfiltered list query is rejected by rules
  await assertFails(aliceDb.collection("schools").get());

  // Cross-tenant isolation: Bob cannot get or update Alice's school
  await assertFails(bobDb.collection("schools").doc("school_alice").get());
  await assertFails(bobDb.collection("schools").doc("school_alice").update({
    name: "Hijacked School",
    updatedAt: pastDate(1000),
  }));

  // Bob's filtered query returns only Bob's schools (succeeds safely with empty result)
  await assertSucceeds(
    bobDb.collection("schools").where("memberUids", "array-contains", BOB_UID).get()
  );
});

test("Shadow update, ghost fields, and @DocumentId rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@school.cm", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("schools").doc("school_alice").set(validSchoolPayload(ALICE_UID)));

  // Reject payload containing @DocumentId 'id' key
  await assertFails(
    aliceDb.collection("schools").doc("school_bad").set({
      id: "school_bad",
      ...validSchoolPayload(ALICE_UID),
    })
  );

  // Owner cannot self-upgrade subscriptionStatus or monthlyFeeFcfa
  await assertFails(
    aliceDb.collection("schools").doc("school_alice").update({
      subscriptionStatus: "ACTIVE",
      updatedAt: pastDate(1000),
    })
  );

  // Owner can update branding fields cleanly
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_alice").update({
      motto: "Excellence & Innovation",
      primaryColorHex: "#1E40AF",
      updatedAt: pastDate(1000),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("schools").doc("school_alice").update({
      motto: "Future Clock",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("School subcollections: students, grades, and payments enforce isolation and terminal locks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@school.cm", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@other.cm", email_verified: true }).firestore();
  const ts = pastDate(8000);

  await assertSucceeds(aliceDb.collection("schools").doc("school_alice").set(validSchoolPayload(ALICE_UID)));

  // Create student
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_alice").collection("students").doc("stu_1").set({
      schoolId: "school_alice",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID],
      matricule: "ASM-26-0001",
      firstName: "Amina",
      lastName: "Mbarga",
      gender: "F",
      classId: "cls_1",
      className: "6eme A",
      academicYearId: "yr_1",
      scholarshipDiscountFcfa: 0,
      enrollmentStatus: "ENROLLED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Create payment
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_alice").collection("payments").doc("pay_1").set({
      schoolId: "school_alice",
      ownerUid: ALICE_UID,
      memberUids: [ALICE_UID],
      receiptNumber: "REC-2026-0001",
      studentId: "stu_1",
      studentName: "Amina Mbarga",
      studentMatricule: "ASM-26-0001",
      classId: "cls_1",
      className: "6eme A",
      feeCategory: "TUITION",
      amountFcfa: 50000,
      method: "ORANGE_MONEY",
      paidAtMillis: Date.now() - 5000,
      status: "COMPLETED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Refund once (COMPLETED -> REFUNDED) succeeds
  await assertSucceeds(
    aliceDb.collection("schools").doc("school_alice").collection("payments").doc("pay_1").update({
      status: "REFUNDED",
      note: "Erreur de saisie",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: cannot modify a REFUNDED payment again
  await assertFails(
    aliceDb.collection("schools").doc("school_alice").collection("payments").doc("pay_1").update({
      status: "COMPLETED",
      updatedAt: pastDate(500),
    })
  );

  // Cross-school subcollection read/query denied for Bob
  await assertFails(
    bobDb.collection("schools").doc("school_alice").collection("students").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("schools").doc("school_alice").collection("payments").doc("pay_1").get()
  );
});

test("Super Admin privileges & email spoofing prevention", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@school.cm", email_verified: true }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", { email: SUPER_ADMIN_EMAIL, email_verified: false }).firestore();
  const superAdminDb = testEnv.authenticatedContext(SUPER_ADMIN_UID, { email: SUPER_ADMIN_EMAIL, email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("schools").doc("school_alice").set(validSchoolPayload(ALICE_UID)));

  // Spoofed unverified email CANNOT list all schools or activate subscriptions
  await assertFails(spoofAdminDb.collection("schools").get());
  await assertFails(spoofAdminDb.collection("schools").doc("school_alice").update({
    subscriptionStatus: "ACTIVE",
    updatedAt: pastDate(1000),
  }));

  // Verified Super Admin CAN list all schools and activate subscriptions
  await assertSucceeds(superAdminDb.collection("schools").get());
  await assertSucceeds(superAdminDb.collection("schools").doc("school_alice").update({
    subscriptionStatus: "ACTIVE",
    monthlyFeeFcfa: 20000,
    updatedAt: pastDate(1000),
  }));
});
